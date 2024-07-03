package cn.homecredit.printserverextraction.service;

import cn.homecredit.printserverextraction.dao.ContractRepository;
import cn.homecredit.printserverextraction.dao.FailedContractRepository;
import cn.homecredit.printserverextraction.dao.ReportRepository;
import cn.homecredit.printserverextraction.dao.ShardStatusRepository;
import cn.homecredit.printserverextraction.exception.ContractProcessFailedException;
import cn.homecredit.printserverextraction.model.Contract;
import cn.homecredit.printserverextraction.model.FailedContract;
import cn.homecredit.printserverextraction.model.Report;
import cn.homecredit.printserverextraction.model.ShardStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;


@Service
@Slf4j
public class ContractProcessingService {
    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ShardStatusRepository shardStatusRepository;

    @Autowired
    private FailedContractRepository failedContractRepository;
    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private HDSSApiService HdssApiService;

    @Autowired
    private ShardStatusService shardStatusService;
    @Value("${ps.immig.task.contractprocessing.statuses}")
    private String contractStatuses;

    private List<String> contractStatusList;

    public ScheduledFuture<?> getScheduledFuture() {
        return scheduledFuture;
    }


    public void setScheduledFuture(ScheduledFuture<?> scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }

    private ScheduledFuture<?> scheduledFuture;

    List<String> docTypes = Arrays.asList(new String[]{"AL", "AF", "ST", "TAC", "AFS", "TUP", "BAL", "IL", "DS", "GDS", "DD", "FR", "TCB", "TCA", "DDAL", "FLC", "PTC", "TCN", "TCC", "FI", "PTR", "RTC"});
    String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

    @PostConstruct
    public void init() {
        // 将逗号分隔的字符串转换为列表
        this.contractStatusList = Arrays.asList(contractStatuses.split(","));
    }

    @Async
    public void processShard(Boolean isRepair) {
        long threadId = Thread.currentThread().getId();
        String editor= pid+":"+threadId;


        ShardStatus shard = shardStatusService.getSuspendOrPendingShardAndUpdateProcessing(editor);
        if (shard == null) {
            log.info("No shard need to process");
            if (scheduledFuture.isCancelled()) {
                log.warn("Task be canceled: " + ",editor: " + editor);
                return;
            }
            return;
        }

            //PENDING状态必然为null,SUSPEND状态则CurrentId必然有值
            Long startId = shard.getCurrentId()!=null?shard.getCurrentId():shard.getStartId();
            Long endId = shard.getEndId();

            List<Contract> dataList;
            if (isRepair) {
                dataList = contractRepository.findFAILEDDataInShard(startId, endId);
            }else {
                dataList = contractRepository.findPendingDataInShard(startId, endId,contractStatusList);
            }

            if (dataList.isEmpty()) {
                log.info("No contract need to process");
                shard.setStatus("PROCESSED");
                shardStatusRepository.save(shard);
                return;
            }

            for (Contract data : dataList) {


                shard.setCurrentId(data.getId());
                shardStatusRepository.save(shard);


                if (scheduledFuture.isCancelled()) {
                    log.warn("Task be canceled, shardId: " + shard.getShardId() +",editor: " + editor);

                    shard.setStatus("SUSPEND");
                    shardStatusRepository.save(shard);

                    return;
                }


                List<FailedContract> failedContractList = failedContractRepository.findFailedContractsByOriginalContractId(data.getId());

                try {
                    String statusNow=data.getStatus();
                    data.setStatus("PROCESSING");
                    contractRepository.save(data);
                    if (statusNow.equals("FAILED")){
                        FailedContract failedContract = failedContractList.get(0);
                        List<String> docTypes = failedContract.getDocTypes();
                        // 删除失败记录
                        failedContractRepository.delete(failedContract);
                        processData(data,docTypes);
                    }else{//PENDING
                        processData(data,new ArrayList<>());
                    }
                    data.setStatus("PROCESSED");
                    contractRepository.save(data);
                } catch (Exception e) {
                    // 处理失败，将数据状态重置为FAILED
                    data.setStatus("FAILED");
                    contractRepository.save(data);
                    // 处理失败，将数据存入失败表
                    FailedContract newFailedContract = new FailedContract();
                    newFailedContract.setOriginalContractId(data.getId());

                    //failedContract.setReason(); // 更新失败原因
                    log.error(e.getMessage(),e);
                    if (e instanceof ContractProcessFailedException){
                        ContractProcessFailedException contractProcessFailedException= (ContractProcessFailedException) e;
                        newFailedContract.setDocTypes(contractProcessFailedException.getFailedDocTypes());
                    }
                    failedContractRepository.save(newFailedContract);

                }


            }


        shard.setStatus("PROCESSED");
        shardStatusRepository.save(shard);
    }


    public long getFailedDataCount() {
        return failedContractRepository.count();
    }


    private void processData(Contract contract,List<String> docTypes) throws IOException, ContractProcessFailedException {
        //调用hdss
        Long contractNo = contract.getContractNo();
        if (docTypes.isEmpty()){
            docTypes=this.docTypes;
        }

        List<String> failedDocTypes=new ArrayList();
        for (String docType : docTypes) {

            try {
                File file = HdssApiService.callExternalApi(contractNo, docType);
                Report report = new Report();
                report.setReportSize(file.length());
                report.setContractNo(contractNo);
                report.setReportPath(file.getCanonicalPath());
                report.setReportType(docType);
                report.setContract(contract);
                reportRepository.save(report);
                log.warn("Doc file exist, ContractNo: " + contractNo +",docType: " + docType);
            }

        catch (HttpClientErrorException ex) {
            // 根据不同的响应码判断异常类型并抛出
            int statusCode = ex.getRawStatusCode();
            if (statusCode == 404) {
                // 404 响应码属于正常情况，文件不存在而已
                log.warn("Doc file not exist, ContractNo: " + contractNo +",docType: " + docType);
            } else {
                // 其他响应码为异常情况，应记录下来，后续进行批量重试
                log.error("Failed to get doc file, ContractNo: " + contractNo +",docType: " + docType);
                failedDocTypes.add(docType);
            }
        }

        }

        if(!failedDocTypes.isEmpty()){ throw new ContractProcessFailedException("process contract:{} failed",failedDocTypes);
        }

        log.info("process contract:{} totally successfully",contractNo);
    }


}

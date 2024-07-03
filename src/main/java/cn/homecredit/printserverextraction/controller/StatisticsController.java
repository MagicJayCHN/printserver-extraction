package cn.homecredit.printserverextraction.controller;

import cn.homecredit.printserverextraction.controller.dto.RateResponse;
import cn.homecredit.printserverextraction.dao.ShardStatusRepository;
import cn.homecredit.printserverextraction.service.ContractProcessingService;
import cn.homecredit.printserverextraction.service.HDSSApiService;
import cn.homecredit.printserverextraction.service.dto.TaskProgress;
import io.fabric8.kubernetes.api.model.Pod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static cn.homecredit.printserverextraction.controller.TaskSchedulerController.RUNNING;

@RestController
@RequestMapping("/api/statistics")
@Slf4j
public class StatisticsController {

    @Autowired
    private ContractProcessingService contractProcessingService;

    @Autowired
    private HDSSApiService hdssApiService;
    @Autowired
    private TaskSchedulerController taskSchedulerController;
    @Autowired
    private ShardStatusRepository shardStatusRepository;
    @Autowired
    private RestTemplate restTemplate;
    @GetMapping("/failed-contract-count")
    public long getFailedDataCount() {
        return contractProcessingService.getFailedDataCount();
    }


    @GetMapping("/task-progress")
    public TaskProgress getTaskProgress() {

        Long totalContracts = shardStatusRepository.getTotalSize();
        Long processedContracts = shardStatusRepository.getTotalprocessedSize();
        Double processedContractsPercentage = processedContracts == null ? 0 : (double) processedContracts / totalContracts * 100;

        Long totalShards =shardStatusRepository.count();
        Long shardProcessedSize = shardStatusRepository.getShardProcessedSize();
        Double processedShardsPercentage =shardProcessedSize == null ? 0 : (double) shardProcessedSize / totalShards * 100;


        BigDecimal contractProcessVelocityPerSecond = BigDecimal.ZERO;
        BigDecimal hdssApiVelocityPerSecond=BigDecimal.ZERO;

        Map<String, List<Pod>> podsGroupByStatus = taskSchedulerController.getPodsGroupByStatus();
        List<Pod> runningTaskPods = podsGroupByStatus.get(RUNNING);

        for (Pod pod : runningTaskPods) {
            log.info("retrieve the pod：{}", pod.getMetadata().getName());
            String podIp = pod.getStatus().getPodIP();
            String url = String.format("http://%s:8080/api/statistics/rate", podIp);

            RateResponse rateResponse = restTemplate.getForObject(url, RateResponse.class);

            contractProcessVelocityPerSecond.add(rateResponse.getActualContractRate());
            hdssApiVelocityPerSecond.add(rateResponse.getActualHdssRate());
        }
        BigDecimal estimatedRemainingSeconcd=new BigDecimal((processedContracts == null ? totalContracts : (totalContracts - processedContracts))).divide(contractProcessVelocityPerSecond) ;
        BigDecimal estimatedRemainingHour=estimatedRemainingSeconcd.divide(BigDecimal.valueOf(3600));

        return new TaskProgress(BigDecimal.valueOf(totalContracts),BigDecimal.valueOf(processedContracts),BigDecimal.valueOf(processedContractsPercentage), BigDecimal.valueOf(totalShards),BigDecimal.valueOf(shardProcessedSize),BigDecimal.valueOf(processedShardsPercentage),contractProcessVelocityPerSecond,hdssApiVelocityPerSecond,estimatedRemainingSeconcd,estimatedRemainingHour);

    }

    @GetMapping("/rate")
    public RateResponse getRate() {
        return new RateResponse(new BigDecimal(getActualHdssRate()).setScale(3, RoundingMode.HALF_UP),new BigDecimal(getActualContractRate()).setScale(3, RoundingMode.HALF_UP));
    }


    private double getActualHdssRate() {
        long currentTime = System.currentTimeMillis();
        Queue<Long> requestTimestamps = hdssApiService.getRequestTimestamps();
        // 移除超过1分钟的时间戳
        while (!requestTimestamps.isEmpty() && currentTime - requestTimestamps.peek() > TimeUnit.MINUTES.toMillis(1)) {
            requestTimestamps.poll();
        }
        return (double) requestTimestamps.size() / TimeUnit.MINUTES.toSeconds(1); // 请求数除以60秒
    }
    private double getActualContractRate() {
        long currentTime = System.currentTimeMillis();
        Queue<Long> requestTimestamps = contractProcessingService.getRequestTimestamps();
        // 移除超过1分钟的时间戳
        while (!requestTimestamps.isEmpty() && currentTime - requestTimestamps.peek() > TimeUnit.MINUTES.toMillis(1)) {
            requestTimestamps.poll();
        }
        return (double) requestTimestamps.size() / TimeUnit.MINUTES.toSeconds(1); // 请求数除以60秒
    }
}


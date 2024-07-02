package cn.homecredit.printserverextraction.service;

import cn.homecredit.printserverextraction.service.dto.TaskProgress;
import cn.homecredit.printserverextraction.dao.ContractRepository;
import cn.homecredit.printserverextraction.dao.ShardStatusRepository;
import cn.homecredit.printserverextraction.model.ShardStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class ShardStatusService {

    @Autowired
    private ShardStatusRepository shardStatusRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private HDSSApiService hdssApiService;



    @Transactional(propagation = Propagation.REQUIRES_NEW)//新建一个事务来支持findPendingShardForUpdate的@Lock，并且创建上层事务（因为上层需要实时更新contract，而不能最后一次性提交）
    public ShardStatus getSuspendOrPendingShardAndUpdateProcessing(String editor) {
        List<ShardStatus> shardList = shardStatusRepository.findSuspendOrPendingShardForUpdate( PageRequest.of(0, 1));
        if (shardList.isEmpty()) {
            return null;
        }

        ShardStatus shard = shardList.get(0);
        shard.setStatus("PROCESSING");


        shard.setEditor(editor);
        shardStatusRepository.save(shard);
        return shard;
    }

    public void initializeShards(int shardSize) {
        long minId = contractRepository.findMinId();
        long maxId = contractRepository.findMaxId();
        long totalDataSize = contractRepository.count();
        long shardCount = (totalDataSize  / (shardSize+1))+1;

        shardStatusRepository.deleteAll();

        long startId = minId ;
        for (long i = 0; i < shardCount; i++) {

            long endId = startId + shardSize;

            ShardStatus shard = new ShardStatus();
            shard.setStartId(startId);
            shard.setEndId(Math.min(endId, maxId));
            shard.setStatus("PENDING");
            startId=endId+1;

            shardStatusRepository.save(shard);
        }
    }

    @Transactional
    public void resetShardStatus() {
        shardStatusRepository.resetShardStatus();
    }


    public TaskProgress getTaskProgress() {

        Long totalSize = shardStatusRepository.getTotalSize();
        Long totalprocessedSize = shardStatusRepository.getTotalprocessedSize();
        Long shardProcessedSize = shardStatusRepository.getShardProcessedSize();

        double totalPercentage = totalprocessedSize == null ? 0 : (double) totalprocessedSize / totalSize * 100;
        double roundedTotalPercentage = new BigDecimal(totalPercentage).setScale(3, RoundingMode.HALF_UP).doubleValue();

        long seconds = getSecondsSinceEarliestProcessedEdate();
        double totalVelocity = totalprocessedSize == null ? 0 : (double) totalprocessedSize / seconds;
        double roundedTotalVelocity = new BigDecimal(totalVelocity).setScale(3, RoundingMode.HALF_UP).doubleValue();

        double restTime = 0;
        if (totalVelocity != 0) {
            restTime = (totalprocessedSize == null ? totalSize : (totalSize - totalprocessedSize)) / totalVelocity;
        }
        double roundedRestTime = new BigDecimal(restTime).setScale(3, RoundingMode.HALF_UP).doubleValue();

        double shardPercentage = shardProcessedSize == null ? 0 : (double) shardProcessedSize / totalSize * 100;
        double roundedShardPercentage = new BigDecimal(shardPercentage).setScale(3, RoundingMode.HALF_UP).doubleValue();

        double ratePerNode = getActualRequestRate();

        return new TaskProgress(totalSize, roundedTotalPercentage, roundedTotalVelocity, roundedRestTime, roundedRestTime / 3600, roundedShardPercentage, ratePerNode);
    }
    private long getSecondsSinceEarliestProcessedEdate() {
        Date earliestEdate = contractRepository.findEarliestProcessedEdate();
        if (earliestEdate == null) {
            return -1; // 或者返回一个特定的值表示没有找到记录
        }
        long currentTimeMillis = System.currentTimeMillis();
        long earliestEdateMillis = earliestEdate.getTime();
        return (currentTimeMillis - earliestEdateMillis) / 1000; // 计算时间差，以秒为单位
    }
    public double getActualRequestRate() {
        long currentTime = System.currentTimeMillis();
        Queue<Long> requestTimestamps = hdssApiService.getRequestTimestamps();
        // 移除超过1分钟的时间戳
        while (!requestTimestamps.isEmpty() && currentTime - requestTimestamps.peek() > TimeUnit.MINUTES.toMillis(1)) {
            requestTimestamps.poll();
        }
        return (double) requestTimestamps.size() / TimeUnit.MINUTES.toSeconds(1); // 请求数除以60秒
    }
}


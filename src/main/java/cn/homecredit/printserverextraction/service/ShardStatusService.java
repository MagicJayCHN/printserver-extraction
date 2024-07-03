package cn.homecredit.printserverextraction.service;

import cn.homecredit.printserverextraction.dao.ContractRepository;
import cn.homecredit.printserverextraction.dao.ShardStatusRepository;
import cn.homecredit.printserverextraction.model.ShardStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;



@Service
@Slf4j
public class ShardStatusService {

    @Autowired
    private ShardStatusRepository shardStatusRepository;

    @Autowired
    private ContractRepository contractRepository;



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

}


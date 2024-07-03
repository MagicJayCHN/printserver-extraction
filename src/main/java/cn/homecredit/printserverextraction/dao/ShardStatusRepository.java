package cn.homecredit.printserverextraction.dao;

import cn.homecredit.printserverextraction.model.ShardStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

import java.util.List;

public interface ShardStatusRepository extends JpaRepository<ShardStatus, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)//如果分片很多的话，可能
    @Query("SELECT s FROM ShardStatus s WHERE s.status in( 'PENDING','SUSPEND') ORDER BY s.status DESC,s.shardId ASC")
    List<ShardStatus> findSuspendOrPendingShardForUpdate(Pageable pageable);


    @Modifying
    @Query("UPDATE ShardStatus s SET s.status = 'PENDING', s.currentId = NULL,s.editor='SYSTEM'")
    void resetShardStatus();

    @Query("SELECT SUM(s.endId+1 - s.startId) FROM ShardStatus s")
    Long getTotalSize();


    //总处理进度
    @Query("SELECT SUM(s.currentId - s.startId) FROM ShardStatus s WHERE s.currentId IS NOT NULL")
    Long getTotalprocessedSize();

    //分片处理进度
    @Query("SELECT COUNT(s.shardId) FROM ShardStatus s WHERE s.status = 'PROCESSED'")
    Long getShardProcessedSize();




}

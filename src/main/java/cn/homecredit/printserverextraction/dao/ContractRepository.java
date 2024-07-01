package cn.homecredit.printserverextraction.dao;

import cn.homecredit.printserverextraction.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    @Query("SELECT d FROM Contract d WHERE d.id >= :startId AND d.id <= :endId AND d.status = 'PENDING' AND d.contractStatus IN :contractStatuses ORDER BY d.id ASC")
    List<Contract> findPendingDataInShard(@Param("startId") Long startId, @Param("endId") Long endId,@Param("contractStatuses") List<String> contractStatuses);

    @Query("SELECT d FROM Contract d WHERE d.id >= :startId AND d.id <= :endId AND d.status = 'FAILED' ORDER BY d.id ASC")
    List<Contract> findFAILEDDataInShard(@Param("startId") Long startId, @Param("endId") Long endId);

    // 查询最小 ID
    @Query("SELECT MIN(d.id) FROM Contract d")
    Long findMinId();

    // 查询最大 ID
    @Query("SELECT MAX(d.id) FROM Contract d")
    Long findMaxId();

    @Query("SELECT MIN(s.edate) FROM Contract s WHERE s.status = 'PROCESSED'")
    Date findEarliestProcessedEdate();
}


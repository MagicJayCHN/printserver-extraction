package cn.homecredit.printserverextraction.dao;

import cn.homecredit.printserverextraction.model.FailedContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FailedContractRepository extends JpaRepository<FailedContract, Long> {
    // 可以根据需要添加自定义的查询方法
    @Query("SELECT f FROM FailedContract f WHERE f.originalContractId = :originalContractId")
    List<FailedContract> findFailedContractsByOriginalContractId(@Param("originalContractId") Long originalContractId);

}

package cn.homecredit.printserverextraction.dao;


import cn.homecredit.printserverextraction.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}


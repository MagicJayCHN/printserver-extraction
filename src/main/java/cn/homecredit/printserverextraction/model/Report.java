package cn.homecredit.printserverextraction.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.Date;

@Entity
@Data
public class Report extends Auditable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long reportSize;
    private String reportPath;
    private String reportType;
    private Long contractNo;


    @ManyToOne
    private Contract contract;

    // Getters and Setters
}

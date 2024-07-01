package cn.homecredit.printserverextraction.model;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Data
public class Contract extends Auditable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status;
    private Long contractNo;
    private String contractStatus;

    @OneToMany(mappedBy = "contract")
    private List<Report> reports;

    // Getters and Setters
}

package cn.homecredit.printserverextraction.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;


@MappedSuperclass
@Data
public abstract class Auditable {

    @Temporal(TemporalType.TIMESTAMP)
    private Date cdate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date edate;

    private String creator;

    private String editor;

    @PrePersist
    protected void onCreate() {
        cdate = new Date();
        edate = new Date();
        creator = "SYSTEM";
        editor = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        edate = new Date();
    }

    // Getters and Setters
}


package cn.homecredit.printserverextraction.model;

import cn.homecredit.printserverextraction.util.JsonUtils;
import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
public class FailedContract extends Auditable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long originalContractId; // 原始数据的ID
    private String reason; // 失败原因

    @Column(columnDefinition = "TEXT")
    private String docTypes; // 存储为JSON字符串

    @Transient
    public List<String> getDocTypes() {
        return docTypes != null ? JsonUtils.json2list(docTypes,String.class) : new ArrayList<>();
    }
    // Getters and setters
    public void setDocTypes(List<String> docTypes) {
        String str = JsonUtils.obj2json(docTypes);
        this.docTypes = str;
    }

    // Getters and Setters
}
package cn.homecredit.printserverextraction.service.dto;

import cn.homecredit.printserverextraction.model.ShardStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TaskProgress {

    private double totalSize;
    private double totalPercentage;
    private double shardPercentage;
    private double totalVelocity;
    private double ratePerNode;

    // Getters and Setters
}


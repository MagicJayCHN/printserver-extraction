package cn.homecredit.printserverextraction.service.dto;

import cn.homecredit.printserverextraction.model.ShardStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TaskProgress {

    private double totalContract;
    private double finishContractPercentage;
    private double estimatedRemainingTime;

    private double finishShardPercentage;
    private double contractProcessVelocityPerSecond;
    private double hdssApiQpsPerNode;

    // Getters and Setters
}


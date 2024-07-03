package cn.homecredit.printserverextraction.service.dto;

import cn.homecredit.printserverextraction.model.ShardStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class TaskProgress {

    private BigDecimal totalContracts;
    private BigDecimal processedContracts;
    private BigDecimal processedContractsPercentage;
    private BigDecimal totalShards;
    private BigDecimal processedShards;
    private BigDecimal processedShardsPercentage;


    private BigDecimal contractProcessVelocityPerSecond;
    private BigDecimal hdssApiVelocityPerSecond;

    private BigDecimal estimatedRemainingSeconcd;
    private BigDecimal estimatedRemainingHour;

    // Getters and Setters
}


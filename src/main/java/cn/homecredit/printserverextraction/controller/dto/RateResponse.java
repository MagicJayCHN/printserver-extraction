package cn.homecredit.printserverextraction.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class RateResponse {
    private BigDecimal actualHdssRate;
    private BigDecimal actualContractRate;
    // Getters and Setters
}


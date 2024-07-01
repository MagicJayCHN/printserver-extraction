package cn.homecredit.printserverextraction.controller.dto;


import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class ShardInitializationRequest {

    //每个分片处理的记录数，即每个线程每次拿走的contract数据记录数，这个数不能太大，否则全量查询MySQL中数据load到jvm中会导致内存溢出
    //1个亿的数据，则会产生10w的分片
    @NotNull(message = "shardSize is required")
    @Min(value = 100, message = "shardSize must be greater than 100")
    private int shardSize=1000;

    // Getters and Setters
}


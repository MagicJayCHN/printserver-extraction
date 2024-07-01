package cn.homecredit.printserverextraction.controller;

import cn.homecredit.printserverextraction.service.ContractProcessingService;
import cn.homecredit.printserverextraction.service.ShardStatusService;
import cn.homecredit.printserverextraction.service.dto.TaskProgress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private ContractProcessingService contractProcessingService;
    @Autowired
    private ShardStatusService shardStatusService;


    @GetMapping("/failed-contract-count")
    public long getFailedDataCount() {
        return contractProcessingService.getFailedDataCount();
    }
    @GetMapping("/task-progress")
    public TaskProgress getTaskProgress() {
        return shardStatusService.getTaskProgress();
    }
}


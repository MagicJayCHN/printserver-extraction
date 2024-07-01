package cn.homecredit.printserverextraction.scheduler;

import cn.homecredit.printserverextraction.service.ContractProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

//@Component
//public class ScheduledTasks {
//    @Autowired
//    private ContractProcessingService contractProcessingService;
//
//    @Scheduled(fixedRate = 10000) // 每10秒运行一次
//    public void runDataProcessing() {
//        contractProcessingService.processShard();
//    }
//}

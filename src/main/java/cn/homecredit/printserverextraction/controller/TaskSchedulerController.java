package cn.homecredit.printserverextraction.controller;


import cn.homecredit.printserverextraction.controller.dto.SchedulerStartRequest;
import cn.homecredit.printserverextraction.controller.dto.SchedulerStopRequest;
import cn.homecredit.printserverextraction.controller.dto.ShardInitializationRequest;
import cn.homecredit.printserverextraction.service.ContractProcessingService;
import cn.homecredit.printserverextraction.service.ShardStatusService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.concurrent.ScheduledFuture;

@RestController
@RequestMapping("/api/task")
@Slf4j
public class TaskSchedulerController {

    @Autowired
    private ContractProcessingService contractProcessingService;
    @Autowired
    private ShardStatusService shardStatusService;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Value("${ps.immig.task.contractprocessing.interval}")
    private long interval;




    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/initializeShards")
    public String initializeShards(@Valid @RequestBody ShardInitializationRequest request) {

        shardStatusService.initializeShards(request.getShardSize());
        return "Shards initialized";
    }


    @PostMapping("/start")
    public String startTask(@RequestBody SchedulerStartRequest request, HttpServletRequest httpRequest) {
        String requestUrl = getFullRequestUrl(httpRequest);
        ScheduledFuture scheduledFuture = contractProcessingService.getScheduledFuture();
        log.info("Pod be invoked start, all pods:{},current pods:{} " , request.getAllPods() , request.getCurrentPod() );
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            if (request.getCurrentPod() < request.getAllPods()) {
                // 已经启动过，并且发送次数(启动节点个数)不满足，则重新发起 HTTP 请求给别的节点
                request.setTotalTimes(request.getTotalTimes() + 1);
                restTemplate.postForObject(requestUrl, request, String.class);
            }

            return "Start task is already running";
        } else {


            scheduledFuture = taskScheduler.scheduleAtFixedRate(() -> contractProcessingService.processShard(false), interval);
            contractProcessingService.setScheduledFuture(scheduledFuture);
            request.setCurrentPod(request.getCurrentPod() + 1);

            if (request.getCurrentPod() < request.getAllPods()) {
                // 未启动任务，并且发送次数(启动节点个数)不满足，则+1并启动，并重新发起 HTTP 请求
                request.setTotalTimes(request.getTotalTimes() + 1);

                restTemplate.postForObject(requestUrl, request, String.class);
            } else if (request.getCurrentPod() >= request.getAllPods()) {
                log.info("All " + request.getAllPods() + " pod start" + ",extra route " + request.getTotalTimes() + " times");
            }

            return "Start task begin";
        }
    }


    @PostMapping("/stop")
    public String stopTask(@RequestBody SchedulerStopRequest request, HttpServletRequest httpRequest) {
        String requestUrl = getFullRequestUrl(httpRequest);
        ScheduledFuture scheduledFuture = contractProcessingService.getScheduledFuture();
        log.info("Pod be invoked stop, all pods:{},current pods:{} " , request.getAllPods() , request.getCurrentPod() );
        if (scheduledFuture != null&&!scheduledFuture.isCancelled()) {

            request.setCurrentPod(request.getCurrentPod() + 1);
            //已经开始的任务（执行过程中判断iscancel()可以自行中断），应该开始但没开始的任务（等待的任务，没分配到线程，则直接不开始了），还没开始的任务（直接就不开始了）
            scheduledFuture.cancel(true);

            if (request.getCurrentPod() < request.getAllPods()) {
                // 未停止任务，则+1并启动，并重新发起 HTTP 请求
                request.setTotalTimes(request.getTotalTimes() + 1);

                restTemplate.postForObject(requestUrl, request, String.class);
            } else if (request.getCurrentPod() >= request.getAllPods()) {
                log.info("All " + request.getAllPods() + " pod stop" + ",extra route " + request.getTotalTimes() + " times");
            }

            return "Stop task begin";
        } else {
            if (request.getCurrentPod() < request.getAllPods()) {
                // 已经停止过，则重新发起 HTTP 请求给别的节点
                request.setTotalTimes(request.getTotalTimes() + 1);

                restTemplate.postForObject(requestUrl, request, String.class);
            }

            return "No running task to stop";
        }


    }


    @PostMapping("/reprocess")//全部结束，才可以处理错误，不可以暂停之后直接处理错误，会数据错乱
    public String reprocessTask(@RequestBody SchedulerStartRequest request, HttpServletRequest httpRequest) {
        String requestUrl = getFullRequestUrl(httpRequest);
        log.info("Pod be invoked reprocess, all pods:{},current pods:{} " , request.getAllPods() , request.getCurrentPod() );
        ScheduledFuture scheduledFuture = contractProcessingService.getScheduledFuture();
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {

            if (request.getCurrentPod() < request.getAllPods()) {
                // 已经启动过，则重新发起 HTTP 请求给别的节点
                request.setTotalTimes(request.getTotalTimes() + 1);

                restTemplate.postForObject(requestUrl, request, String.class);
            }
            return "Reprocess task is already running";
        } else {

            //第一个启动的节点，应该对shard_status进行初始化
            if (request.getCurrentPod() == 0) {
                shardStatusService.resetShardStatus();
            }

            scheduledFuture = taskScheduler.scheduleAtFixedRate(() -> contractProcessingService.processShard(true), interval);
            contractProcessingService.setScheduledFuture(scheduledFuture);

            request.setCurrentPod(request.getCurrentPod() + 1);

            if (request.getCurrentPod() < request.getAllPods()) {
                // 未启动任务，则+1并启动，并重新发起 HTTP 请求

                request.setTotalTimes(request.getTotalTimes() + 1);

                restTemplate.postForObject(requestUrl, request, String.class);
            } else if (request.getCurrentPod() >= request.getAllPods()) {
                log.info("All " + request.getAllPods() + " pod reprocess" + ",extra route " + request.getTotalTimes() + " times");
            }


            return "Reprocess task begin";
        }
    }


    private String getFullRequestUrl(HttpServletRequest request) {
        StringBuffer requestUrl = request.getRequestURL();
        String queryString = request.getQueryString();
        if (queryString == null) {
            return requestUrl.toString();
        } else {
            return requestUrl.append('?').append(queryString).toString();
        }
    }
}

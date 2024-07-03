package cn.homecredit.printserverextraction.controller;


import cn.homecredit.printserverextraction.controller.dto.SchedulerStartRequest;

import cn.homecredit.printserverextraction.controller.dto.ShardInitializationRequest;
import cn.homecredit.printserverextraction.service.ContractProcessingService;
import cn.homecredit.printserverextraction.service.ShardStatusService;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private KubernetesClient kubernetesClient = new DefaultKubernetesClient();

    private final String STOPPED = "stopped";
    private final String RUNNING = "running";


    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/initializeShards")
    public String initializeShards(@Valid @RequestBody ShardInitializationRequest request) {

        shardStatusService.initializeShards(request.getShardSize());
        return "Shards initialized";
    }


    @PostMapping("/adjust")
    public String adjust(@RequestParam int taskCount) {
        int t = adjustTask(taskCount);
        if (t == -1) {
            return "taskCount must be between 0 and podsNumber";
        } else {
            return "adjust successfully";
        }

    }

    @GetMapping("/status")
    public String getTaskStatus() {
        ScheduledFuture scheduledFuture = contractProcessingService.getScheduledFuture();
        if (scheduledFuture == null || scheduledFuture.isCancelled()) {
            return STOPPED;
        } else {//即scheduledFuture != null && !scheduledFuture.isCancelled()
            return RUNNING;
        }

    }

    @PostMapping("/start")//该接口提供给单机使用，该接口再k8s中不能直接调用，因为不知道会路由到哪个节点来启动。k8s中的启停通过/adjust接口
    public String startTask(@RequestBody SchedulerStartRequest request) {

        ScheduledFuture scheduledFuture = contractProcessingService.getScheduledFuture();

        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {

            log.info("task is already running");

        } else {

            scheduledFuture = taskScheduler.scheduleAtFixedRate(() -> contractProcessingService.processShard(request.getIsRepair()), interval);
            contractProcessingService.setScheduledFuture(scheduledFuture);
            log.info("task begin to start");

        }
        return RUNNING;
    }


    @PostMapping("/stop")//该接口提供给单机使用，该接口再k8s中不能直接调用，因为不知道会路由到哪个节点来启动。k8s中的启停通过/adjust接口
    public String stopTask() {

        ScheduledFuture scheduledFuture = contractProcessingService.getScheduledFuture();
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {

            //已经开始的任务（执行过程中判断iscancel()可以自行中断），应该开始但没开始的任务（等待的任务，没分配到线程，则直接不开始了），还没开始的任务（直接就不开始了）
            scheduledFuture.cancel(true);
            log.info("task begin to stop");

        } else {

            log.info("task is already stopped");
        }

        return STOPPED;

    }


    @PostMapping("/repair")
    public String repairTask() {

        Map<String, List<Pod>> podsGroupByStatus = getPodsGroupByStatus();
        List<Pod> runningTaskPods = podsGroupByStatus.get(RUNNING);
        int runningTasks = runningTaskPods == null ? 0 : runningTaskPods.size();
        List<Pod> stoppedTaskPods = podsGroupByStatus.get(STOPPED);

        if (runningTasks > 0) {//全部结束，才可以处理错误，不可以暂停之后直接处理错误，会数据错乱
            return "existing running task:{" + runningTasks + "},please finish all running task and begin to reprocess";
        }

        shardStatusService.resetShardStatus();

        for (Pod pod : stoppedTaskPods) {

            String url = String.format("http://%s:8080/api/task/start", pod.getStatus().getPodIP());
            SchedulerStartRequest request = new SchedulerStartRequest();
            request.setIsRepair(true);
            restTemplate.postForObject(url, request, String.class);
        }
        log.info("task begin to reprocess");

        return "reprocessing";
    }


    private int adjustTask(int taskCount) {
        Map<String, List<Pod>> podsGroupByStatus = getPodsGroupByStatus();
        List<Pod> runningTaskPods = podsGroupByStatus.get(RUNNING);
        int runningTasks = runningTaskPods == null ? 0 : runningTaskPods.size();
        List<Pod> stoppedTaskPods = podsGroupByStatus.get(STOPPED);
        int stoppedTasks = stoppedTaskPods == null ? 0 : stoppedTaskPods.size();

        if (taskCount > runningTasks + stoppedTasks || taskCount < 0) {
            return -1;
        }

        if (taskCount > runningTasks) {
            // Start additional tasks
            if (stoppedTasks > 0) {
                for (int i = 0; i < taskCount - runningTasks; i++) {
                    String url = String.format("http://%s:8080/api/task/start", stoppedTaskPods.get(i % stoppedTasks).getStatus().getPodIP());
                    restTemplate.postForObject(url, null, String.class);
                    log.info("existing running task:{},stopped task：{},so start task: {}", runningTasks, stoppedTasks, taskCount - runningTasks);
                }
            }
        } else if (taskCount < runningTasks) {
            // Stop some tasks
            if (runningTasks > 0) {
                for (int i = 0; i < runningTasks - taskCount; i++) {
                    String url = String.format("http://%s:8080/api/task/stop", runningTaskPods.get(i % runningTasks).getStatus().getPodIP());
                    SchedulerStartRequest request=new SchedulerStartRequest();
                    restTemplate.postForObject(url, request, String.class);
                    log.info("existing running task:{},stopped task：{},so stop task: {}", runningTasks, stoppedTasks, runningTasks - taskCount);
                }
            }
        }

        return taskCount;
    }

    private Map<String, List<Pod>> getPodsGroupByStatus() {
        List<Pod> pods = kubernetesClient.pods().inNamespace("technical-tool").withLabel("artifactId", "printserver-extraction").list().getItems();
        Map<String, List<Pod>> statusMap = new HashMap<>();

        for (Pod pod : pods) {
            log.info("retrieve the pod：{}", pod.getMetadata().getName());
            String podIp = pod.getStatus().getPodIP();
            String url = String.format("http://%s:8080/api/task/status", podIp);

            String taskStatus = restTemplate.getForObject(url, String.class);

            statusMap.computeIfAbsent(taskStatus, k -> new ArrayList<>()).add(pod);
        }

        return statusMap;
    }
}

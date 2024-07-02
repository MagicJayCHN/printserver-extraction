package cn.homecredit.printserverextraction.controller.dto;

import lombok.Data;

@Data
public class SchedulerStartRequest {
    private int currentPod =0;//
    private int allPods =3;//currentPod=已启动节点数，allPods=目标节点数，如想启动3个节点，应该currentPod=0,allPods=3,想继续扩容到5个节点，应该currentPod=3，allPods=5）
    private int totalTimes=0;

    // Getters and Setters
}


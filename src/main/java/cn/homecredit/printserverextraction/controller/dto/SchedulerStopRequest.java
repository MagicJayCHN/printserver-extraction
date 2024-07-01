package cn.homecredit.printserverextraction.controller.dto;

import lombok.Data;

@Data
public class SchedulerStopRequest {

    private int currentPod =0;//想启动3个节点，应该设置为0
    private int allPods =3;
    private int totalTimes=0;


    // Getters and Setters
}


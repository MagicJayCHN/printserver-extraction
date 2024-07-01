package cn.homecredit.printserverextraction.exception;

import java.util.ArrayList;
import java.util.List;

public class ContractProcessFailedException extends Exception {

    public List<String> getFailedDocTypes() {
        return failedDocTypes;
    }

    private List<String> failedDocTypes=new ArrayList();
    public ContractProcessFailedException(String message,List<String> failedDocTypes) {
        super(message);
        this.failedDocTypes= failedDocTypes;
    }
}

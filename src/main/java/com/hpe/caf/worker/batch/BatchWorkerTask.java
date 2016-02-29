package com.hpe.caf.worker.batch;

import java.util.Map;

/**
 * Created by gibsodom on 22/02/2016.
 */
public class BatchWorkerTask {
    private String batchDefinition;
    private String batchType;
    private String taskMessageType;
    private Map<String,String> taskMessageParams;
    private String targetPipe;

    public BatchWorkerTask(){

    }

    public String getBatchDefinition() {
        return batchDefinition;
    }

    public void setBatchDefinition(String batchDefinition) {
        this.batchDefinition = batchDefinition;
    }

    public String getBatchType() {
        return batchType;
    }

    public void setBatchType(String batchType) {
        this.batchType = batchType;
    }

    public String getTaskMessageType() {
        return taskMessageType;
    }

    public void setTaskMessageType(String taskMessageType) {
        this.taskMessageType = taskMessageType;
    }

    public Map<String, String> getTaskMessageParams() {
        return taskMessageParams;
    }

    public void setTaskMessageParams(Map<String, String> taskMessageParams) {
        this.taskMessageParams = taskMessageParams;
    }

    public String getTargetPipe() {
        return targetPipe;
    }

    public void setTargetPipe(String targetPipe) {
        this.targetPipe = targetPipe;
    }
}


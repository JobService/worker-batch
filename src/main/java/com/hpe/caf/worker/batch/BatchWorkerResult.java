package com.hpe.caf.worker.batch;

public class BatchWorkerResult {
    private String batchTask;

    public BatchWorkerResult(){
    }

    public BatchWorkerResult(String batchTask) {
        this.batchTask = batchTask;
    }

    public String getBatchTask() {
        return batchTask;
    }

    public void setBatchTask(String batchTask) {
        this.batchTask = batchTask;
    }
}

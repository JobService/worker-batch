package com.hpe.caf.worker.batch;

/**
 * Created by gibsodom on 22/02/2016.
 */
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

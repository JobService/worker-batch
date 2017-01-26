package com.hpe.caf.worker.batch;

import com.hpe.caf.api.worker.WorkerConfiguration;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class BatchWorkerConfiguration extends WorkerConfiguration {

    @NotNull
    @Size(min = 1)
    private String outputQueue;

    @Min(1)
    @Max(20)
    private int threads;

    @Min(1)
    private int cacheExpireTime;

    private ReturnValueBehaviour returnValueBehaviour;

    public String getOutputQueue() {
        return outputQueue;
    }

    public void setOutputQueue(String outputQueue) {
        this.outputQueue = outputQueue;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public int getCacheExpireTime() {
        return cacheExpireTime;
    }

    public void setCacheExpireTime(int cacheExpireTime) {
        this.cacheExpireTime = cacheExpireTime;
    }

    public ReturnValueBehaviour getReturnValueBehaviour() {
        return returnValueBehaviour;
    }

    public void setReturnValueBehaviour(ReturnValueBehaviour returnValueBehaviour) {
        this.returnValueBehaviour = returnValueBehaviour;
    }
}

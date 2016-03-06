package com.hpe.caf.worker.batch;

import java.util.Map;

public class BatchWorkerTask {

    public String batchDefinition;

    public String batchType;

    public String taskMessageType;

    public Map<String,String> taskMessageParams;

    public String targetPipe;
}

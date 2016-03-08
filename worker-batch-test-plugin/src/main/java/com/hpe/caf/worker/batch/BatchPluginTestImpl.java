package com.hpe.caf.worker.batch;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by gibsodom on 26/02/2016.
 * A very simple implementation of the batch worker plugin. Will split a comma separated list into subtasks.
 */
public class BatchPluginTestImpl implements BatchWorkerPlugin {

    public static String identifier = "BATCH_WORKER_TEST_IMPL";

    @Override
    public void processBatch(BatchWorkerServices batchWorkerServices, String batchDefinition, String taskMessageType, Map<String, String> taskMessageParams) {
        List<String> items = Arrays.asList(batchDefinition.split("\\s*,\\s*"));
        for(String subItem:items) {
            batchWorkerServices.registerItemSubtask("Test task", 1, subItem);
        }
    }



}

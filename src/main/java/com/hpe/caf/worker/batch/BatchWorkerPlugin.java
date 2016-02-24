package com.hpe.caf.worker.batch;

import java.util.Map;

/**
 * The Batch Worker Plugin processes batch definitions by splitting them into further batch definitions and passes those
 * split batch definitions to the Batch Worker Services for further processing. The Batch Worker Plugin also constructs
 * a task data object of the given task message type for each task item which is passed to the Batch Worker Services
 * before serialisation.
 */
public interface BatchWorkerPlugin {

    /**
     * Splits batch definition up into batch definitions for further batch definition refinement and constructs task
     * message parameters for the given task message type.
     *
     * @param batchWorkerServices Object containing batch worker services
     * @param batchDefinition String containing the batch definition
     * @param taskMessageType String containing the task message type as which to base the construction of the task data
     *                        on
     * @param taskMessageParams Map containing additional task message parameters
     */
    void processBatch(BatchWorkerServices batchWorkerServices, String batchDefinition, String taskMessageType,
                      Map<String, String> taskMessageParams);
}

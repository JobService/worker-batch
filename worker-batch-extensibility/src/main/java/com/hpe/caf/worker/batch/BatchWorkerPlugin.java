/*
 * Copyright 2016-2021 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
     * 
     * @throws BatchDefinitionException thrown if the batch definition is invalid
     * @throws BatchWorkerTransientException thrown if the batch could not be processed because of a transient error 
     *                                       and indicates the batch processing can be retried
     */
    void processBatch(BatchWorkerServices batchWorkerServices, String batchDefinition, String taskMessageType,
                      Map<String, String> taskMessageParams) throws BatchDefinitionException, BatchWorkerTransientException;
}

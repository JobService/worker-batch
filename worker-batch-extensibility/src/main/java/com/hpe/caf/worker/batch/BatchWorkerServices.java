/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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

/**
 * The Batch Worker Services is used to register processed batch definitions for further batch defining. The class is
 * also used to register a task message's parameters before serialisation.
 */
public interface BatchWorkerServices {

    /**
     * Registers a processed a sub batch for publishing to the Batch Worker's input queue
     *
     * @param batchDefinition   String containing the new batch definition
     */
    void registerBatchSubtask(String batchDefinition);

    /**
     * Registers a task message's parameters for serialisation and publishing to an input queue
     *
     * @param taskClassifier String containing the name of the Classifier the task data is intended for
     * @param taskApiVersion Integer containing the version number of the task api
     * @param taskData       Object containing the constructed task data for task message type
     */
    void registerItemSubtask(String taskClassifier, int taskApiVersion, Object taskData);

    /**
     * Returns the specified service, or {@code null} if the service has not been registered.
     *
     * @param <S> the type of the service to be returned
     * @param service the interface or abstract class representing the service
     * @return the service provider
     */
    <S> S getService(Class<S> service);
}

/*
 * Copyright 2016-2024 Open Text.
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
package com.github.jobservice.worker.batch;


import com.github.cafapi.common.api.Codec;
import com.github.cafapi.common.api.CodecException;
import com.github.workerframework.worker.api.TaskFailedException;
import com.github.workerframework.worker.api.TaskStatus;
import com.github.workerframework.worker.api.WorkerResponse;
import com.github.workerframework.worker.api.WorkerTaskData;

import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of the BatchWorkerServices interface.
 * This is passed to the BatchWorkerPlugin to provide call back methods to register new subtasks.
 */
public class BatchWorkerServicesImpl implements BatchWorkerServices {

    private final Codec codec;
    private final String inputQueue;
    private final BatchWorkerTask currentTask;
    private final WorkerTaskData workerTaskData;

    private boolean hasSubtasks;

    private final Map<Class, Object> serviceMap;

    /**
     * Constructor for BatchWorkerServiceImpl.
     *
     * @param task The current BatchWorkerTask.
     * @param codec A Codec implementation.
     * @param inputQueue The input pipe for batch messages
     * @param workerTaskData The worker task data to operate on when issuing responses
     */
    public BatchWorkerServicesImpl(final BatchWorkerTask task, final Codec codec, final String inputQueue,
                                   final WorkerTaskData workerTaskData)
    {
        this.codec = codec;
        this.currentTask = task;
        this.inputQueue = inputQueue;
        this.hasSubtasks = false;
        this.serviceMap = new HashMap<>();
        this.workerTaskData = workerTaskData;
    }

    /**
     * A Callback method for the BatchWorkerPlugin to register a new sub-batch of tasks processed.
     *
     * @param batchDefinition String containing the new batch definition
     */
    @Override
    public void registerBatchSubtask(final String batchDefinition) {
        try {
            issueResponse(inputQueue, TaskStatus.NEW_TASK, codec.serialise(createBatchWorkerTask(batchDefinition)),
                                               BatchWorkerConstants.WORKER_NAME, BatchWorkerConstants.WORKER_API_VERSION);
            hasSubtasks = true;
        } catch (CodecException e) {
            throw new TaskFailedException("Failed to serialize", e);
        } catch (Throwable e) { //Catch everything else.
            throw new TaskFailedException(e.getMessage(), e);
        }
    }

    /**
     * A Callback method for the BatchWorkerPlugin to register a new subtask to be published.
     *
     * @param taskClassifier String containing the name of the Classifier the task data is intended for.
     * @param taskApiVersion Integer containing the version number of the task api.
     * @param taskData       Object containing the constructed task data for the task message type.
     */
    @Override
    public void registerItemSubtask(final String taskClassifier, final int taskApiVersion, final Object taskData) {
        try {
            issueResponse(currentTask.targetPipe, TaskStatus.NEW_TASK, codec.serialise(taskData), taskClassifier,
                                               taskApiVersion);
           hasSubtasks = true;
        } catch (CodecException e) {
            throw new TaskFailedException("Failed to serialize", e);
        } catch (Throwable e) { //Catch everything else.
            throw new TaskFailedException(e.getMessage(), e);
        }
    }

    /**
     * A get method for retrieving a service registered with BatchWorkerServices
     *
     * @param service the interface or abstract class representing the service
     * @return the service provider
     */
    @Override
    public <S> S getService(Class<S> service) {
        return (S) serviceMap.get(service);
    }

    /**
     * A method for registering a service with BatchWorkerServices
     *
     * @param service the interface or abstract class representing the service
     * @param serviceProvider the instance of the service to register
     */
    public <S> void register(final Class<S> service, final S serviceProvider) {
        serviceMap.put(service, serviceProvider);
    }

    /**
     * Creates a new BatchWorkerTask for a sub-batch.
     *
     * @param batchDefinition The sub-batch definition to process.
     * @return A constructed BatchWorkerTask.
     */
    private BatchWorkerTask createBatchWorkerTask(String batchDefinition) {
        BatchWorkerTask batchWorkerTask = new BatchWorkerTask();
        batchWorkerTask.batchDefinition = batchDefinition;
        batchWorkerTask.batchType = currentTask.batchType;
        batchWorkerTask.targetPipe = currentTask.targetPipe;
        batchWorkerTask.taskMessageParams = currentTask.taskMessageParams;
        batchWorkerTask.taskMessageType = currentTask.taskMessageType;
        return batchWorkerTask;
    }

    /**
     * Return whether this batch has sub tasks.
     * If registerBatchSubtask() or registerItemSubtask() is called, the subtaskCount will be incremented (if tracking info is present).
     *
     * @return whether the batch has sub tasks.
     */
    public boolean hasSubtasks() {
        return hasSubtasks;
    }
    
    /**
     * Issues a response for the current subtask to the target queue.
     *
     * @param targetPipe The queue to publish to
     * @param taskStatus Status of task being published
     * @param taskData Task's data represented by a byte[]
     * @param taskClassifier Task's classifier
     * @param taskApiVersion Tasks API version
     */
    private void issueResponse(final String targetPipe, final TaskStatus taskStatus, final byte[] taskData, final String taskClassifier,
                                final int taskApiVersion)
    {
        final WorkerResponse response = new WorkerResponse(targetPipe, taskStatus, taskData, taskClassifier, taskApiVersion, null);
        workerTaskData.addResponse(response, false);
    }
}

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

import com.google.common.cache.LoadingCache;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskFailedException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * An implementation of the BatchWorkerServices interface.
 * This is passed to the BatchWorkerPlugin to provide call back methods to register new subtasks.
 */
public class BatchWorkerServicesImpl implements BatchWorkerServices {

    private static final Logger logger = LoggerFactory.getLogger(BatchWorkerServicesImpl.class);

    private final Connection conn;
    private final LoadingCache<String, Channel> channelCache;
    private final Codec codec;
    private final String inputQueue;
    private final BatchWorkerTask currentTask;
    private final TrackingInfo tracking;
    private final BatchWorkerPublisher batchWorkerPublisher;

    private int subtaskCount;
    private boolean hasSubtasks;

    private final Map<Class, Object> serviceMap;

    /**
     * Constructor for BatchWorkerServiceImpl.
     *
     * @param task                 The current BatchWorkerTask.
     * @param codec                A Codec implementation.
     * @param channelCache         A cache of Rabbitmq Channels to send subtasks to.
     * @param conn                 The Connection to the  Rabbitmq Server.
     * @param inputQueue           The BatchWorker's input queue.
     * @param trackingInfo         The Job Serivce TrackingInfo to be passed to all subtasks.
     * @param batchWorkerPublisher A utility object that handles that actual publishing of subtasks.
     */
    public BatchWorkerServicesImpl(BatchWorkerTask task, Codec codec, LoadingCache<String, Channel> channelCache, Connection conn, String inputQueue, TrackingInfo trackingInfo, BatchWorkerPublisher batchWorkerPublisher) {
        this.conn = conn;
        this.channelCache = channelCache;
        this.codec = codec;
        this.currentTask = task;
        this.inputQueue = inputQueue;
        this.subtaskCount = 0;
        this.tracking = trackingInfo;
        this.batchWorkerPublisher = batchWorkerPublisher;
        this.hasSubtasks = false;
        this.serviceMap = new HashMap<>();
    }

    /**
     * A Callback method for the BatchWorkerPlugin to register a new sub-batch of tasks processed.
     *
     * @param batchDefinition String containing the new batch definition
     */
    @Override
    public void registerBatchSubtask(String batchDefinition) {
        try {
            String currentTaskId = incrementTaskId();
            BatchWorkerTask subtask = createBatchWorkerTask(batchDefinition);
            byte[] serializedTask = codec.serialise(subtask);
            TaskMessage taskMessage = new TaskMessage(currentTaskId, BatchWorkerConstants.WORKER_NAME,
                    BatchWorkerConstants.WORKER_API_VERSION, serializedTask, TaskStatus.NEW_TASK, new HashMap<>(), inputQueue, getTracking(currentTaskId));
            batchWorkerPublisher.storeInMessageBuffer(inputQueue, taskMessage);
        } catch (ExecutionException e) {
            throw new TaskFailedException("Failed to retrieve or load queue channel from cache", e);
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
    public void registerItemSubtask(String taskClassifier, int taskApiVersion, Object taskData) {
        try {
            String currentTaskId = incrementTaskId();
            TaskMessage message = new TaskMessage(currentTaskId, taskClassifier, taskApiVersion,
                    codec.serialise(taskData), TaskStatus.NEW_TASK, new HashMap<>(), currentTask.targetPipe, getTracking(currentTaskId));
            batchWorkerPublisher.storeInMessageBuffer(currentTask.targetPipe, message);
        } catch (ExecutionException e) {
            throw new TaskFailedException("Failed to retrieve or load queue channel from cache", e);
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
     * Increments the current TaskId for each subtask. If the batch was not sent by the Job Service (i.e no trackingInfo)
     * a UUID is used instead.
     *
     * @return The TaskId for the subtask.
     */
    private String incrementTaskId() {
        hasSubtasks = true;
        if (tracking == null) {
            return UUID.randomUUID().toString();
        }
        subtaskCount++;
        return tracking.getJobTaskId() + "." + subtaskCount;
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
     * Constructs the TrackingInfo for the current subtask.
     *
     * @param currentTaskId The current subtask's Id.
     * @return TrackingInfo.
     */
    private TrackingInfo getTracking(String currentTaskId) {
        if (tracking == null) {
            return null;
        }
        TrackingInfo trackingInfo = deepCopyTracking(tracking);
        trackingInfo.setJobTaskId(currentTaskId);
        return trackingInfo;
    }

    /**
     * Method to deep copy the TrackingInfo for each subtask to avoid overwriting any buffered subtasks TrackingInfo.
     *
     * @param trackingInfoToCopy
     * @return
     */
    private TrackingInfo deepCopyTracking(TrackingInfo trackingInfoToCopy) {
        TrackingInfo trackingInfo = new TrackingInfo();
        trackingInfo.setJobTaskId(trackingInfoToCopy.getJobTaskId());
        trackingInfo.setStatusCheckTime(trackingInfoToCopy.getStatusCheckTime());
        trackingInfo.setStatusCheckUrl(trackingInfoToCopy.getStatusCheckUrl());
        trackingInfo.setTrackingPipe(trackingInfoToCopy.getTrackingPipe());
        trackingInfo.setTrackTo(trackingInfoToCopy.getTrackTo());
        return trackingInfo;
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
}

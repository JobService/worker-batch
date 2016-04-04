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
     * Increments the current TaskId for each subtask. If the batch was not sent by the Job Service (i.e no trackingInfo)
     * a UUID is used instead.
     *
     * @return The TaskId for the subtask.
     */
    private String incrementTaskId() {
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
}

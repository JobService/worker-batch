package com.hpe.caf.worker.batch;

import com.google.common.cache.LoadingCache;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskFailedException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class BatchWorkerServicesImpl implements BatchWorkerServices {

    private static final Logger logger = Logger.getLogger(BatchWorkerServicesImpl.class);

    private final Connection conn;
    private final LoadingCache<String, Channel> channelCache;
    private final Codec codec;
    private final String inputQueue;
    private final BatchWorkerTask currentTask;

    private int subtaskCount;

    public BatchWorkerServicesImpl(BatchWorkerTask task, Codec codec, LoadingCache<String, Channel> channelCache, Connection conn, String inputQueue) {
        this.conn = conn;
        this.channelCache = channelCache;
        this.codec = codec;
        this.currentTask = task;
        this.inputQueue = inputQueue;
        this.subtaskCount = 0;
    }

    @Override
    public void registerBatchSubtask(String batchDefinition) {
        try {
            String currentTaskId = incrementTaskId();
            BatchWorkerTask subtask = createBatchWorkerTask(batchDefinition);
            byte[] serializedTask = codec.serialise(subtask);
            TaskMessage taskMessage = new TaskMessage(currentTaskId, BatchWorkerConstants.WORKER_NAME,
                    BatchWorkerConstants.WORKER_API_VERSION, serializedTask, TaskStatus.NEW_TASK, new HashMap<>());
            publishMessage(inputQueue, taskMessage);
        } catch (ExecutionException e) {
            throw new TaskFailedException("Failed to retrieve or load queue channel from cache", e);
        } catch (CodecException e) {
            throw new TaskFailedException("Failed to serialize", e);
        } catch (Exception e) { //All other exceptions
            throw new TaskFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void registerItemSubtask(String taskClassifier, int taskApiVersion, Object taskData) {
        try {
            String currentTaskId = incrementTaskId();
            TaskMessage message = new TaskMessage(currentTaskId, taskClassifier, taskApiVersion,
                    codec.serialise(taskData), TaskStatus.NEW_TASK, new HashMap<>());
            publishMessage(currentTask.targetPipe, message);
        } catch (ExecutionException e) {
            throw new TaskFailedException("Failed to retrieve or load queue channel from cache", e);
        } catch (CodecException e) {
            throw new TaskFailedException("Failed to serialize", e);
        } catch (Exception e) { //All other exceptions
            throw new TaskFailedException(e.getMessage(), e);
        }
    }

    private String incrementTaskId() {
        subtaskCount++;
        //todo When tracking data is added (CAF-599), change this to increment the current taskid
//        return taskId+"."+subtaskCount;
        return UUID.randomUUID().toString();
    }

    private BatchWorkerTask createBatchWorkerTask(String batchDefinition) {
        BatchWorkerTask batchWorkerTask = new BatchWorkerTask();
        batchWorkerTask.batchDefinition = batchDefinition;
        batchWorkerTask.batchType = currentTask.batchType;
        batchWorkerTask.targetPipe = currentTask.targetPipe;
        batchWorkerTask.taskMessageParams = currentTask.taskMessageParams;
        batchWorkerTask.taskMessageType = currentTask.taskMessageType;
        return batchWorkerTask;
    }

    private void publishMessage(String targetPipe, TaskMessage taskMessage) throws CodecException, ExecutionException, IOException {
        logger.debug("Loading channel for " + targetPipe);
        Channel channel = channelCache.get(targetPipe);
        logger.debug("Queueing new task with id " + taskMessage.getTaskId() + " on " + targetPipe);
        channel.basicPublish("", targetPipe, MessageProperties.PERSISTENT_TEXT_PLAIN, codec.serialise(taskMessage));
        logger.debug("Successfully published task" + taskMessage.getTaskId() + " to " + targetPipe);
    }
}

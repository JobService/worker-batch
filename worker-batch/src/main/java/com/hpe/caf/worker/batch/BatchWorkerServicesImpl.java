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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Created by gibsodom on 22/02/2016.
 */
public class BatchWorkerServicesImpl implements BatchWorkerServices {

    private Connection conn;
    private LoadingCache<String, Channel> channelCache;
    private Codec codec;
    private int subtaskCount;
    private String inputQueue;
    private BatchWorkerTask currentTask;
    private static Logger logger = Logger.getLogger(BatchWorkerServicesImpl.class);

    public BatchWorkerServicesImpl(BatchWorkerTask task, Codec codec, LoadingCache<String, Channel> channelCache, Connection conn, String inputQueue) {
        this.conn = conn;
        this.channelCache = channelCache;
        this.codec = codec;
        this.currentTask = task;
        this.inputQueue = inputQueue;
    }

    @Override
    public void registerBatchSubtask(String batchDefinition) {
        try {
            String currentTaskId = incrementTaskId();
            byte[] serializedTask = codec.serialise(createBatchWorkerTask(batchDefinition, currentTask.batchType, currentTask.taskMessageType, currentTask.taskMessageParams, currentTask.targetPipe));
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

    private BatchWorkerTask createBatchWorkerTask(String batchDefinition, String batchType, String taskMessageType, Map<String, String> taskMessageParams, String targetPipe) {
        BatchWorkerTask batchWorkerTask = new BatchWorkerTask();
        batchWorkerTask.batchDefinition = batchDefinition;
        batchWorkerTask.batchType = batchType;
        batchWorkerTask.targetPipe = targetPipe;
        batchWorkerTask.taskMessageParams = taskMessageParams;
        batchWorkerTask.taskMessageType = taskMessageType;
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

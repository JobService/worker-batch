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
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TrackingInfo;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Object that publishes task messages from a buffer.
 */
public class BatchWorkerPublisher {

    private static final Logger logger = LoggerFactory.getLogger(BatchWorkerPublisher.class);
    private LoadingCache<String, Channel> channelCache;
    private Codec codec;

    //Maps target pipe and task message
    private Map.Entry<String, TaskMessage> currentTaskMessage = null;


    public BatchWorkerPublisher(LoadingCache<String, Channel> channelCache, Codec codec) {
        this.channelCache = channelCache;
        this.codec = codec;
    }

    /**
     * Publishes the current message in the buffer (if present) and saves this task message to the buffer.
     *
     * @param targetPipe  the queue to publish the method to.
     * @param taskMessage the message to publish.
     * @throws ExecutionException
     * @throws CodecException
     * @throws IOException
     */
    public void storeInMessageBuffer(String targetPipe, TaskMessage taskMessage) throws ExecutionException, CodecException, IOException {
        if (currentTaskMessage != null) {
            logger.debug("Message already in buffer, publishing this message before storing new message.");
            publishMessage(currentTaskMessage.getKey(), currentTaskMessage.getValue());
        }
        currentTaskMessage = new AbstractMap.SimpleEntry<>(targetPipe, taskMessage);
    }

    /**
     * Publishes the current task message in the buffer to the target queue.
     *
     * @param targetPipe  The queue to publish to.
     * @param taskMessage The message to publish.
     * @throws CodecException
     * @throws ExecutionException
     * @throws IOException
     */
    private void publishMessage(String targetPipe, TaskMessage taskMessage) throws CodecException, ExecutionException, IOException {
        logger.debug("Loading channel for " + targetPipe);
        Channel channel = channelCache.get(targetPipe);
        logger.debug("Setting new task's destination as " + targetPipe);
        taskMessage.setTo(targetPipe);
        String outputQueue = getOutputQueue(taskMessage.getTracking(), targetPipe);
        logger.debug("Queueing new task with id " + taskMessage.getTaskId() + " on " + outputQueue);
        channel.basicPublish("", outputQueue, MessageProperties.PERSISTENT_TEXT_PLAIN, codec.serialise(taskMessage));
        logger.debug("Successfully published task" + taskMessage.getTaskId() + " to " + outputQueue);
    }

    /**
     * Publishes the current message in the buffer, appending an * to the TaskId to signify it's the final subtask.
     *
     * @throws ExecutionException
     * @throws CodecException
     * @throws IOException
     */
    public void publishLastMessage() throws ExecutionException, CodecException, IOException {
        if (currentTaskMessage == null) {
            return;
        }
        logger.debug("About to publish last subtask. Marking message with Id " + currentTaskMessage.getValue().getTaskId() + " as the last subtask.");
        TrackingInfo trackingInfo = currentTaskMessage.getValue().getTracking();
        if (trackingInfo != null) {
            trackingInfo.setJobTaskId(trackingInfo.getJobTaskId() + "*");
        }
        publishMessage(currentTaskMessage.getKey(), currentTaskMessage.getValue());
        currentTaskMessage = null;
    }

    /**
     * Checks if the buffer is empty.
     *
     * @return True if the buffer is empty.
     */
    public boolean isTaskMessageBufferEmpty() {
        if (currentTaskMessage == null) {
            return true;
        }
        return false;
    }

    private String getOutputQueue(TrackingInfo trackingInfo, String targetPipe) {
        if (trackingInfo == null) {
            return targetPipe;
        }
        return trackingInfo.getTrackingPipe() == null || trackingInfo.getTrackingPipe().equalsIgnoreCase(targetPipe) ? targetPipe : trackingInfo.getTrackingPipe();
    }
}

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
import com.github.workerframework.util.rabbitmq.RabbitUtil;
import com.github.workerframework.worker.api.TaskMessage;
import com.github.workerframework.worker.configs.RabbitConfiguration;
import com.github.workerframework.worker.queues.rabbit.RabbitWorkerQueueConfiguration;
import com.github.workerframework.worker.testing.SettingNames;
import com.github.workerframework.worker.testing.util.SettingsProvider;
import com.github.workerframework.worker.testing.util.WorkerServices;
import com.rabbitmq.client.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class BatchTargetQueueRetriever {

    private static BatchTargetQueueRetriever instance;
    private static Connection connection;
    private Channel channel;
    private Codec codec;
    private static Logger logger = Logger.getLogger(BatchTargetQueueRetriever.class);
    private boolean debugEnabled;

    protected BatchTargetQueueRetriever() throws Exception {
        RabbitWorkerQueueConfiguration rabbitWorkerQueueConfiguration = WorkerServices.getDefault().getConfigurationSource().getConfiguration(RabbitWorkerQueueConfiguration.class);
        createRabbitConnection(rabbitWorkerQueueConfiguration);
        codec = WorkerServices.getDefault().getCodec();
        debugEnabled = SettingsProvider.defaultProvider.getBooleanSetting(SettingNames.createDebugMessage,false);
    }

    public static BatchTargetQueueRetriever getInstance() throws Exception {
        if (instance == null) {
            instance = new BatchTargetQueueRetriever();
        }
        return instance;
    }

    public List<TaskMessage> retrieveMessages(String targetQueue) throws IOException, InterruptedException, CodecException {
        if (channel == null) {
            channel = connection.createChannel();
            channel.queueDeclare(targetQueue, true, false, false, new HashMap<>());
        }
        List<TaskMessage> messages = new ArrayList<>();
        QueueConsumer consumer = new QueueConsumer(channel);

        // Avoid ShutdownSignalException where targetQueue has yet to be initialised and populated.
        final int numberOfRetries = 5;
        final long timeToWait = 1000;
        for (int i=0; i<numberOfRetries; i++) {
            try {
                channel.basicConsume(targetQueue, false, consumer);
                break;
            } catch (IOException ioe) {
                //  Log exception.
                logger.error(ioe.getMessage());

                if (i < numberOfRetries) {
                    //  Wait until targetQueue has been initialised and populated.
                    Thread.sleep(timeToWait);
                } else {
                    //  If retry count has exceeded, then re-throw.
                    throw ioe;
                }
            }
        }

        //get first message from queue
        QueueConsumer.Delivery delivery = consumer.nextDelivery(100);
        while (delivery != null) {
            //deserialse message and add to list
            messages.add(codec.deserialise(delivery.getBody(), TaskMessage.class));
            //retrieve next message or time out and stop loop
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            delivery = consumer.nextDelivery(100);
        }
        if(debugEnabled){
            publishDebugMessages(messages,targetQueue);
        }
        //Assume queue no longer needed
        purgeQueue(targetQueue);
        return messages;
    }

    private void createRabbitConnection(RabbitWorkerQueueConfiguration rabbitWorkerConfiguration) throws IOException, TimeoutException,
            URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        //Check if environment override specified
        String rabbitHost = SettingsProvider.defaultProvider.getSetting(SettingNames.dockerHostAddress);
        Integer rabbitPort = Integer.parseInt(SettingsProvider.defaultProvider.getSetting(SettingNames.rabbitmqNodePort));
        rabbitWorkerConfiguration.getRabbitConfiguration().setRabbitHost(rabbitHost != null ? rabbitHost : rabbitWorkerConfiguration.getRabbitConfiguration().getRabbitHost());
        rabbitWorkerConfiguration.getRabbitConfiguration().setRabbitPort(rabbitPort != null ? rabbitPort : rabbitWorkerConfiguration.getRabbitConfiguration().getRabbitPort());

        RabbitConfiguration rabbitConfiguration = rabbitWorkerConfiguration.getRabbitConfiguration();
        connection = RabbitUtil.createRabbitConnection(rabbitConfiguration);
    }

    private void purgeQueue(String targetQueue) {
        try {
            channel.queuePurge(targetQueue);
            channel.close();
            channel = null;
        } catch (IOException e) {
            logger.error("Failed to delete queue " + targetQueue, e);
        } catch (TimeoutException e) {
            logger.error("Failed to close channel", e);
        }
    }

    private void publishDebugMessages(List<TaskMessage> messages, String targetQueue) throws IOException, CodecException {
        String debugQueue = targetQueue + "-debug";
        RabbitUtil.declareWorkerQueue(channel, debugQueue);
        for (TaskMessage taskMessage : messages) {
            channel.basicPublish("", debugQueue, MessageProperties.PERSISTENT_TEXT_PLAIN, codec.serialise(taskMessage));
        }
    }
}

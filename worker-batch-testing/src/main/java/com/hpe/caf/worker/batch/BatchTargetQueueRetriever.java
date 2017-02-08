package com.hpe.caf.worker.batch;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.configs.RabbitConfiguration;
import com.hpe.caf.util.rabbitmq.RabbitUtil;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;
import com.hpe.caf.worker.testing.SettingNames;
import com.hpe.caf.worker.testing.SettingsProvider;
import com.hpe.caf.worker.testing.WorkerServices;
import com.rabbitmq.client.*;
import net.jodah.lyra.ConnectionOptions;
import net.jodah.lyra.config.Config;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
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
        }
        List<TaskMessage> messages = new ArrayList<>();
        QueueingConsumer consumer = new QueueingConsumer(channel);

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
        QueueingConsumer.Delivery delivery = consumer.nextDelivery(100);
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

    private void createRabbitConnection(RabbitWorkerQueueConfiguration rabbitWorkerConfiguration) throws IOException, TimeoutException {
        //Check if environment override specified
        String rabbitHost = SettingsProvider.defaultProvider.getSetting(SettingNames.dockerHostAddress);
        Integer rabbitPort = Integer.parseInt(SettingsProvider.defaultProvider.getSetting(SettingNames.rabbitmqNodePort));
        rabbitWorkerConfiguration.getRabbitConfiguration().setRabbitHost(rabbitHost != null ? rabbitHost : rabbitWorkerConfiguration.getRabbitConfiguration().getRabbitHost());
        rabbitWorkerConfiguration.getRabbitConfiguration().setRabbitPort(rabbitPort != null ? rabbitPort : rabbitWorkerConfiguration.getRabbitConfiguration().getRabbitPort());

        RabbitConfiguration rabbitConfiguration = rabbitWorkerConfiguration.getRabbitConfiguration();
        ConnectionOptions lyraOpts = RabbitUtil.createLyraConnectionOptions(rabbitConfiguration.getRabbitHost(),
                rabbitConfiguration.getRabbitPort(), rabbitConfiguration.getRabbitUser(), rabbitConfiguration.getRabbitPassword());
        Config lyraConfig = RabbitUtil.createLyraConfig(rabbitConfiguration.getBackoffInterval(),
                rabbitConfiguration.getMaxBackoffInterval(), -1);
        connection = RabbitUtil.createRabbitConnection(lyraOpts, lyraConfig);
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

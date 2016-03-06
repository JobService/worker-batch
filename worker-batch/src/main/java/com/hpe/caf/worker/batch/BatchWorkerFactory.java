package com.hpe.caf.worker.batch;

import com.google.common.cache.*;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.configs.RabbitConfiguration;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.rabbitmq.RabbitUtil;
import com.hpe.caf.worker.AbstractWorkerFactory;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import net.jodah.lyra.ConnectionOptions;
import net.jodah.lyra.config.Config;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BatchWorkerFactory extends AbstractWorkerFactory<BatchWorkerConfiguration, BatchWorkerTask> {

    private LoadingCache<String, Channel> channelCache;
    private Connection conn;
    private static Logger logger = Logger.getLogger(BatchWorkerFactory.class);
    private String inputQueue;
    private Map<String, BatchWorkerPlugin> registeredPlugins = new HashMap<>();

    /**
     * Instantiates a new DefaultWorkerFactory.
     *
     * @param configSource       the worker configuration source
     * @param dataStore          the external data store
     * @param codec              the codec used in serialisation
     * @param configurationClass the worker configuration class
     * @param taskClass          the worker task class
     * @throws WorkerException if the factory cannot be instantiated
     */
    public BatchWorkerFactory(ConfigurationSource configSource, DataStore dataStore, Codec codec, Class<BatchWorkerConfiguration> configurationClass, Class<BatchWorkerTask> taskClass) throws WorkerException {
        super(configSource, dataStore, codec, configurationClass, taskClass);
        try {
            RabbitWorkerQueueConfiguration rabbitWorkerQueueConfiguration = configSource.getConfiguration(RabbitWorkerQueueConfiguration.class);
            createRabbitConnection(rabbitWorkerQueueConfiguration);
            inputQueue = rabbitWorkerQueueConfiguration.getInputQueue();
            createChannelCache();
        } catch (Exception e) {
            throw new WorkerException("Failed to create worker factory", e);
        }
        registerPlugins();
    }

    @Override
    protected String getWorkerName() {
        return BatchWorkerConstants.WORKER_NAME;
    }

    @Override
    protected int getWorkerApiVersion() {
        return BatchWorkerConstants.WORKER_API_VERSION;
    }

    @Override
    protected Worker createWorker(BatchWorkerTask task) throws TaskRejectedException, InvalidTaskException {
        return new BatchWorker(task, getConfiguration().getOutputQueue(), getCodec(), channelCache, conn, inputQueue, registeredPlugins);
    }

    @Override
    public String getInvalidTaskQueue() {
        return getConfiguration().getOutputQueue();
    }

    @Override
    public int getWorkerThreads() {
        return getConfiguration().getThreads();
    }

    @Override
    public HealthResult healthCheck() {
        return HealthResult.RESULT_HEALTHY;
    }

    @Override
    public void shutdown() {
        channelCache.invalidateAll();
        channelCache = null;
        try {
            conn.close();
            conn = null;
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private void createRabbitConnection(RabbitWorkerQueueConfiguration rabbitWorkerConfiguration) throws IOException, TimeoutException {
        RabbitConfiguration rabbitConfiguration = rabbitWorkerConfiguration.getRabbitConfiguration();
        ConnectionOptions lyraOpts = RabbitUtil.createLyraConnectionOptions(rabbitConfiguration.getRabbitHost(),
                rabbitConfiguration.getRabbitPort(), rabbitConfiguration.getRabbitUser(), rabbitConfiguration.getRabbitPassword());
        Config lyraConfig = RabbitUtil.createLyraConfig(rabbitConfiguration.getBackoffInterval(),
                rabbitConfiguration.getMaxBackoffInterval(), -1);
        conn = RabbitUtil.createRabbitConnection(lyraOpts, lyraConfig);
    }

    private void createChannelCache() {
        CacheLoader<String, Channel> cacheLoader = new CacheLoader<String, Channel>() {
            public Channel load(String key) throws IOException {
                Channel channel = conn.createChannel();
                RabbitUtil.declareWorkerQueue(channel, key);
                return channel;
            }
        };
        RemovalListener<String, Channel> removalListener = new RemovalListener<String, Channel>() {
            public void onRemoval(RemovalNotification<String, Channel> removal) {
                Channel channel = removal.getValue();
                try {
                    channel.close();
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        };
        channelCache = CacheBuilder.newBuilder().concurrencyLevel(getWorkerThreads())
                .maximumSize(100).expireAfterAccess(getConfiguration().getCacheExpireTime(), TimeUnit.SECONDS).removalListener(removalListener).build(
                        cacheLoader
                );
    }

    private void registerPlugins() {
        List<BatchWorkerPlugin> pluginList = ModuleLoader.getServices(BatchWorkerPlugin.class);
        for (BatchWorkerPlugin plugin : pluginList) {
            registeredPlugins.put(plugin.getIdentifier(), plugin);
        }
    }
}

/*
 * Copyright 2016-2020 Micro Focus or one of its affiliates.
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

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.worker.AbstractWorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BatchWorkerFactory extends AbstractWorkerFactory<BatchWorkerConfiguration, BatchWorkerTask> {
    private static final Logger logger = LoggerFactory.getLogger(BatchWorkerFactory.class);
    private final Map<String, BatchWorkerPlugin> registeredPlugins = new HashMap<>();

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
    public BatchWorkerFactory(final ConfigurationSource configSource, final DataStore dataStore, final Codec codec,
                              final Class<BatchWorkerConfiguration> configurationClass,
                              final Class<BatchWorkerTask> taskClass) throws WorkerException {
        super(configSource, dataStore, codec, configurationClass, taskClass);
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
    protected Worker createWorker(final BatchWorkerTask task, final WorkerTaskData workerTaskData) throws TaskRejectedException,
            InvalidTaskException {
        return new BatchWorker(task, getConfiguration(), getCodec(), registeredPlugins, getDataStore(), workerTaskData);
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

    private void registerPlugins() {
        final List<BatchWorkerPlugin> pluginList = ModuleLoader.getServices(BatchWorkerPlugin.class);
        for (final BatchWorkerPlugin plugin : pluginList) {
            registeredPlugins.put(plugin.getClass().getSimpleName(), plugin);
        }
    }
}

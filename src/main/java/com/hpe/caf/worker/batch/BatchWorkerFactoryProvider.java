package com.hpe.caf.worker.batch;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.api.worker.WorkerFactoryProvider;

public class BatchWorkerFactoryProvider implements WorkerFactoryProvider {

    @Override
    public WorkerFactory getWorkerFactory(ConfigurationSource configSource, DataStore dataStore, Codec codec) throws WorkerException {
        return new BatchWorkerFactory(configSource,dataStore,codec,BatchWorkerConfiguration.class, BatchWorkerTask.class);
    }
}

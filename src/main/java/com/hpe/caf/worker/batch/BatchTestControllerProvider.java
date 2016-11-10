package com.hpe.caf.worker.batch;

import com.hpe.caf.worker.testing.*;
import com.hpe.caf.worker.testing.execution.AbstractTestControllerProvider;

public class BatchTestControllerProvider extends AbstractTestControllerProvider<BatchWorkerConfiguration, BatchWorkerTask, BatchWorkerResult, BatchTestInput, BatchTestExpectation> {

    public BatchTestControllerProvider() {
        super(BatchWorkerConstants.WORKER_NAME, BatchWorkerConfiguration::getOutputQueue, BatchWorkerConfiguration.class, BatchWorkerTask.class, BatchWorkerResult.class, BatchTestInput.class, BatchTestExpectation.class);
    }

    @Override
    protected WorkerTaskFactory<BatchWorkerTask, BatchTestInput, BatchTestExpectation> getTaskFactory(TestConfiguration<BatchWorkerTask, BatchWorkerResult, BatchTestInput, BatchTestExpectation> configuration) throws Exception {
        return new BatchTaskFactory(configuration);
    }

    @Override
    protected ResultProcessor getTestResultProcessor(TestConfiguration<BatchWorkerTask, BatchWorkerResult, BatchTestInput, BatchTestExpectation> configuration, WorkerServices workerServices) {
        return new BatchResultValidationProcessor();
    }

    @Override
    protected TestItemProvider getDataPreparationItemProvider(TestConfiguration<BatchWorkerTask, BatchWorkerResult, BatchTestInput, BatchTestExpectation> configuration) {
        return new BatchResultPreparationProvider(configuration);
    }

    @Override
    protected ResultProcessor getDataPreparationResultProcessor(TestConfiguration<BatchWorkerTask, BatchWorkerResult, BatchTestInput, BatchTestExpectation> configuration, WorkerServices workerServices) {
        return new BatchSaveResultProcessor(configuration, workerServices.getCodec());
    }
}

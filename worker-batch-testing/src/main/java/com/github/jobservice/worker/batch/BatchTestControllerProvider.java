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

import com.github.workerframework.worker.testing.ResultProcessor;
import com.github.workerframework.worker.testing.TestConfiguration;
import com.github.workerframework.worker.testing.TestItemProvider;
import com.github.workerframework.worker.testing.WorkerTaskFactory;
import com.github.workerframework.worker.testing.execution.AbstractTestControllerProvider;
import com.github.workerframework.worker.testing.util.WorkerServices;

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
        return new BatchResultValidationProcessor(workerServices.getCodec(), workerServices.getDataStore());
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

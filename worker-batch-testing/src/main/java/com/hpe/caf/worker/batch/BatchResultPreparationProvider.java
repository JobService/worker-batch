/*
 * Copyright 2016-2021 Micro Focus or one of its affiliates.
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

import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;
import com.hpe.caf.worker.testing.preparation.PreparationItemProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class BatchResultPreparationProvider extends PreparationItemProvider<BatchWorkerTask, BatchWorkerResult, BatchTestInput, BatchTestExpectation> {

    TestConfiguration configuration;

    public BatchResultPreparationProvider(TestConfiguration<BatchWorkerTask, BatchWorkerResult, BatchTestInput, BatchTestExpectation> configuration) {
        super(configuration);
        this.configuration = configuration;
    }

    @Override
    protected TestItem createTestItem(Path inputFile, Path expectedFile) throws Exception {
        TestItem<BatchTestInput,BatchTestExpectation> testItem = super.createTestItem(inputFile,expectedFile);
        BatchWorkerTask task = getTaskTemplate();
        if(task == null){
            task = new BatchWorkerTask();
            task.targetPipe = UUID.randomUUID().toString();
            task.batchDefinition = new String(Files.readAllBytes(inputFile));
            task.batchType = "com.hpe.caf.worker.batch.BatchPluginTestImpl";
        } else {
            task.batchDefinition = new String(Files.readAllBytes(inputFile));
        }
        testItem.getInputData().setTask(task);
        return testItem;
    }
}

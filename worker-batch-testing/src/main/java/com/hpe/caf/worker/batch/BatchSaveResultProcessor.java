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
package com.hpe.caf.worker.batch;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;
import com.hpe.caf.worker.testing.preparation.PreparationResultProcessor;

import java.util.List;

public class BatchSaveResultProcessor extends PreparationResultProcessor<BatchWorkerTask,BatchWorkerResult,BatchTestInput,BatchTestExpectation> {

    protected BatchSaveResultProcessor(TestConfiguration<BatchWorkerTask, BatchWorkerResult, BatchTestInput, BatchTestExpectation> configuration, Codec codec) {
        super(configuration, codec);
    }

    @Override
    protected byte[] getOutputContent(BatchWorkerResult batchWorkerResult, TaskMessage message, TestItem<BatchTestInput, BatchTestExpectation> testItem) throws Exception {
        List<TaskMessage> subTasks = BatchTargetQueueRetriever.getInstance().retrieveMessages(testItem.getInputData().getTask().targetPipe);
        testItem.getExpectedOutputData().setSubTasks(subTasks);
        return super.getOutputContent(batchWorkerResult, message, testItem);
    }
}

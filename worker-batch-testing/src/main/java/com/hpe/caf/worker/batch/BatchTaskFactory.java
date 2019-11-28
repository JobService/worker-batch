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

import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.testing.FileInputWorkerTaskFactory;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;

import java.util.Date;

public class BatchTaskFactory extends FileInputWorkerTaskFactory<BatchWorkerTask, BatchTestInput, BatchTestExpectation> {

    public BatchTaskFactory(TestConfiguration configuration) throws Exception {
        super(configuration);
    }
    @Override
    protected BatchWorkerTask createTask(TestItem<BatchTestInput, BatchTestExpectation> testItem, ReferencedData sourceData) {
        BatchWorkerTask task = testItem.getInputData().getTask();
        return task;
    }

    @Override
    public String getWorkerName() {
        return BatchWorkerConstants.WORKER_NAME;
    }

    @Override
    public int getApiVersion() {
        return BatchWorkerConstants.WORKER_API_VERSION;
    }

    @Override
    public TrackingInfo createTrackingInfo(TestItem<BatchTestInput, BatchTestExpectation> testItem) {
        return new TrackingInfo(testItem.getTag(), new Date(), null, null, null);
    }
}

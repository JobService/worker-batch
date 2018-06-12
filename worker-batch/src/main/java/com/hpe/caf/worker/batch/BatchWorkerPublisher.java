/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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

import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.api.worker.WorkerTaskData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Object that publishes task messages from a buffer.
 */
public class BatchWorkerPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchWorkerPublisher.class);
    private final WorkerTaskData workerTaskData;

    public BatchWorkerPublisher(final WorkerTaskData workerTaskData) {
        this.workerTaskData = workerTaskData;
    }

    /**
     * Issues a response for the current subtask to the target queue.
     *
     * @param targetPipe The queue to publish to
     * @param taskStatus Status of task being published
     * @param taskData Task's data represented by a byte[]
     * @param taskClassifier Task's classifier
     * @param taskApiVersion Tasks API version
     */
    public void issueResponse(final String targetPipe, final TaskStatus taskStatus, final byte[] taskData, final String taskClassifier,
                                final int taskApiVersion)
    {
        final WorkerResponse response = new WorkerResponse(targetPipe, taskStatus, taskData, taskClassifier, taskApiVersion, null);
        workerTaskData.addResponse(response, false);
    }
}

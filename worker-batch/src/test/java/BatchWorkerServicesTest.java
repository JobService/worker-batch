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
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.worker.batch.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.file.Path;
import java.util.*;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BatchWorkerServicesTest
{

    @Mock
    WorkerTaskData workerTaskData;

    private final static Codec CODEC = new JsonCodec();
    private final String inputQueue = "input";
    private final String outputQueue = "output";
    private BatchWorkerServicesImpl services;
    private byte[] taskData;
    private BatchWorkerTask task;

    @Before
    public void setup() throws Exception
    {
        task = new BatchWorkerTask();
        task.targetPipe = "TestingPipe";
        this.workerTaskData = Mockito.mock(WorkerTaskData.class);
        doNothing().when(this.workerTaskData).addResponse(eq(any(WorkerResponse.class)), false);
        task.targetPipe = outputQueue;
        taskData = CODEC.serialise(task);
        services = new BatchWorkerServicesImpl(task, CODEC, "testPipe", workerTaskData);
    }

    @Test
    public void testGetRegisteredServiceFromBatchWorkerServices() throws Exception
    {
        // Mock DataStore and register it with the BatchWorkerServices
        final String mockRefId = "mockRefId";
        final DataStore mockDataStore = mock(DataStore.class);
        when(mockDataStore.store(any(Path.class), any(String.class))).thenReturn(mockRefId);
        services.register(DataStore.class, mockDataStore);

        // Assert that the DataStore stored as a service can be called from the service register
        Path mockFilePath = null;
        Assert.assertEquals(mockRefId, services.getService(DataStore.class).store(mockFilePath, "mockDsRef"));
    }

    @Test
    public void testNoOutputSwitch() throws Exception
    {
        // Set up some mocked method classes and calls
        final String taskId = "J1";

        // Set up our task for this test
        final BatchWorkerTask localTask = new BatchWorkerTask();
        localTask.targetPipe = outputQueue;
        localTask.batchDefinition = "";
        localTask.batchType = "com.hpe.caf.worker.batch.BatchPluginTestImpl";

        // Set up tracking info for this test
        final TrackingInfo trackingInfo = new TrackingInfo();
        trackingInfo.setJobTaskId(taskId);

        // Can be empty as we will be defaulting to the test plugin when no match is found
        final Map<String, BatchWorkerPlugin> plugins = new HashMap<>();
        final BatchWorkerConfiguration configuration = new BatchWorkerConfiguration();
        configuration.setReturnValueBehaviour(ReturnValueBehaviour.RETURN_ONLY_IF_ZERO_SUBTASKS);
        BatchWorker batchWorker = new BatchWorker(localTask, configuration, CODEC, plugins, null, workerTaskData);

        // We can assert the data of the response WITH ZERO SUBTASKS is not empty as we do want to return a result here
        final WorkerResponse workerResponseZeroSubtasks = batchWorker.doWork();
        Assert.assertTrue(workerResponseZeroSubtasks.getTaskStatus().equals(TaskStatus.RESULT_SUCCESS));
        Assert.assertTrue(workerResponseZeroSubtasks.getData().length > 0); // there is output data

        // We can assert the data of the response WITH SUBTASKS is empty as we don't want to return a result here
        configuration.setReturnValueBehaviour(ReturnValueBehaviour.RETURN_NONE);
        localTask.batchDefinition = "abc";
        batchWorker = new BatchWorker(localTask, configuration, CODEC, plugins, null, workerTaskData);
        final WorkerResponse workerResponseReturnNone = batchWorker.doWork();
        Assert.assertTrue(workerResponseReturnNone.getTaskStatus().equals(TaskStatus.RESULT_SUCCESS));
        Assert.assertTrue(workerResponseReturnNone.getData().length == 0); // there is no output data

        // We can assert the data of the response WITH SUBTASKS is empty as we don't want to return a result here
        configuration.setReturnValueBehaviour(ReturnValueBehaviour.RETURN_ALL);
        localTask.batchDefinition = "abc";
        batchWorker = new BatchWorker(localTask, configuration, CODEC, plugins, null, workerTaskData);
        final WorkerResponse workerResponseReturnAll = batchWorker.doWork();
        Assert.assertTrue(workerResponseReturnAll.getTaskStatus().equals(TaskStatus.RESULT_SUCCESS));
        Assert.assertTrue(workerResponseReturnAll.getData().length > 0); // there is no output data
    }

    @Test
    public void testCreateTask() throws Exception
    {
        services.registerItemSubtask(inputQueue, 0, taskData);
        Mockito.verify(workerTaskData, Mockito.times(1)).addResponse(any(WorkerResponse.class), eq(false));
    }

    @Test
    public void testCreateSubBatch() throws Exception
    {
       services.registerBatchSubtask("testBatchDefinition");
       Mockito.verify(workerTaskData, Mockito.times(1)).addResponse(any(WorkerResponse.class), eq(false));
    }

    @Test(expected= TaskFailedException.class)
    public void testSerializeFailure() throws Exception
    {
        final BatchWorkerTask localTask = new BatchWorkerTask();
        localTask.targetPipe = outputQueue;
        //Mock codec and recreate services localTask to used the new mocked codec.
        final Codec codec = Mockito.mock(Codec.class);
        services = new BatchWorkerServicesImpl(localTask, codec, "testPipe", workerTaskData);
        Mockito.when(codec.serialise(localTask)).thenThrow(CodecException.class);
        services.registerItemSubtask(BatchWorkerConstants.WORKER_NAME, BatchWorkerConstants.WORKER_API_VERSION, localTask);
    }
}

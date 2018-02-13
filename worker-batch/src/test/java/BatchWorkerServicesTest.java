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
import com.google.common.cache.LoadingCache;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.worker.batch.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.impl.ChannelN;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BatchWorkerServicesTest {

    @Mock
    private Connection connection;

    @Mock
    LoadingCache<String, Channel> channelCache;

    @Mock
    ChannelN channel;

    @Mock
    ChannelN outputChannel;

    private Codec codec = new JsonCodec();
    private BatchWorkerServicesImpl services;
    private byte[] taskData;
    private byte[] taskMessageSerialized;
    private BatchWorkerTask task;
    private final String inputQueue = "input";
    private final String outputQueue = "output";
    private BatchWorkerPublisher batchWorkerPublisher;

    @Before
    public void setup() throws CodecException, IOException, ExecutionException {
        task = new BatchWorkerTask();
        task.targetPipe = outputQueue;
        batchWorkerPublisher = new BatchWorkerPublisher(channelCache, codec);
        services = new BatchWorkerServicesImpl(task, codec, channelCache, connection, inputQueue, null, batchWorkerPublisher);
        taskData = codec.serialise(task);
        taskMessageSerialized = codec.serialise(new TaskMessage(UUID.randomUUID().toString(), BatchWorkerConstants.WORKER_NAME,
                BatchWorkerConstants.WORKER_API_VERSION, taskData, TaskStatus.NEW_TASK, new HashMap<>(),outputQueue,null));
    }

    @Test
    public void testGetRegisteredServiceFromBatchWorkerServices() throws DataStoreException {
        // Mock DataStore and register it with the BatchWorkerServices
        String mockRefId = "mockRefId";
        DataStore mockDataStore = mock(DataStore.class);
        when(mockDataStore.store(any(Path.class), any(String.class)))
                .thenReturn(mockRefId);
        services.register(DataStore.class, mockDataStore);

        // Assert that the DataStore stored as a service can be called from the service register
        Path mockFilePath = null;
        Assert.assertEquals(mockRefId, services.getService(DataStore.class).store(mockFilePath, "mockDsRef"));
    }

    @Test
    public void testNoOutputSwitch() throws IOException, ExecutionException, CodecException, BatchDefinitionException, InvalidTaskException, InterruptedException, 
                                     TaskRejectedException
    {
        // Set up some mocked method classes and calls
        String taskId = "J1";
        Mockito.when(connection.createChannel()).thenReturn(outputChannel);
        Mockito.when(channelCache.get(outputQueue)).thenReturn(outputChannel);
        BatchWorkerPublisher batchWorkerPublisher = Mockito.mock(BatchWorkerPublisher.class);
        PublishAnswer answer = new PublishAnswer();
        Mockito.doAnswer(answer).when(batchWorkerPublisher).storeInMessageBuffer(Mockito.anyString(), Mockito.any());

        // Set up our task for this test
        BatchWorkerTask localTask = new BatchWorkerTask();
        localTask.targetPipe = outputQueue;
        localTask.batchDefinition = "";
        localTask.batchType = "com.hpe.caf.worker.batch.BatchPluginTestImpl";

        // Set up tracking info for this test
        TrackingInfo trackingInfo = new TrackingInfo();
        trackingInfo.setJobTaskId(taskId);

        // Can be empty as we will be defaulting to the test plugin when no match is found
        Map<String, BatchWorkerPlugin> plugins = new HashMap<>();
        BatchWorkerConfiguration configuration = new BatchWorkerConfiguration();
        configuration.setReturnValueBehaviour(ReturnValueBehaviour.RETURN_ONLY_IF_ZERO_SUBTASKS);
        BatchWorker batchWorker = new BatchWorker(localTask, trackingInfo, configuration, codec, channelCache, connection, inputQueue, plugins, null);

        // We can assert the data of the response WITH ZERO SUBTASKS is not empty as we do want to return a result here
        WorkerResponse workerResponseZeroSubtasks = batchWorker.doWork();
        Assert.assertTrue(workerResponseZeroSubtasks.getTaskStatus().equals(TaskStatus.RESULT_SUCCESS));
        Assert.assertTrue(workerResponseZeroSubtasks.getData().length > 0); // there is output data

        // We can assert the data of the response WITH SUBTASKS is empty as we don't want to return a result here
        configuration.setReturnValueBehaviour(ReturnValueBehaviour.RETURN_NONE);
        localTask.batchDefinition = "abc";
        batchWorker = new BatchWorker(localTask, trackingInfo, configuration, codec, channelCache, connection, inputQueue, plugins, null);
        WorkerResponse workerResponseReturnNone = batchWorker.doWork();
        Assert.assertTrue(workerResponseReturnNone.getTaskStatus().equals(TaskStatus.RESULT_SUCCESS));
        Assert.assertTrue(workerResponseReturnNone.getData().length == 0); // there is no output data

        // We can assert the data of the response WITH SUBTASKS is empty as we don't want to return a result here
        configuration.setReturnValueBehaviour(ReturnValueBehaviour.RETURN_ALL);
        localTask.batchDefinition = "abc";
        batchWorker = new BatchWorker(localTask, trackingInfo, configuration, codec, channelCache, connection, inputQueue, plugins, null);
        WorkerResponse workerResponseReturnAll = batchWorker.doWork();
        Assert.assertTrue(workerResponseReturnAll.getTaskStatus().equals(TaskStatus.RESULT_SUCCESS));
        Assert.assertTrue(workerResponseReturnAll.getData().length > 0); // there is no output data
    }

    @Test
    public void testCreateTask() throws ExecutionException, IOException, CodecException {
        Mockito.when(connection.createChannel()).thenReturn(outputChannel);
        Mockito.when(channelCache.get(outputQueue)).thenReturn(outputChannel);
        BatchWorkerTask localTask = new BatchWorkerTask();
        localTask.targetPipe = outputQueue;
        services.registerItemSubtask(BatchWorkerConstants.WORKER_NAME, BatchWorkerConstants.WORKER_API_VERSION, localTask);
        //Store a new message in the buffer to publish our actual message.
        batchWorkerPublisher.storeInMessageBuffer(inputQueue, new TaskMessage());
        Mockito.verify(outputChannel).basicPublish(Mockito.eq(""), Mockito.eq(outputQueue), Mockito.eq(MessageProperties.PERSISTENT_TEXT_PLAIN), Mockito.argThat(new TaskMessageMatcher(taskMessageSerialized)));
    }

    @Test
    public void testCreateSubBatch() throws IOException, ExecutionException, CodecException {
        Mockito.when(connection.createChannel()).thenReturn(channel);
        Mockito.when(channelCache.get(outputQueue)).thenReturn(outputChannel);
        Mockito.when(channelCache.get(inputQueue)).thenReturn(channel);
        services.registerBatchSubtask(null);
        //Store a new message in the buffer to publish our actual message.
        batchWorkerPublisher.storeInMessageBuffer(inputQueue, new TaskMessage());
        //overwrite default expected TaskMessage to change the "to" field to the Worker's input queue.
        taskMessageSerialized = codec.serialise(new TaskMessage(UUID.randomUUID().toString(), BatchWorkerConstants.WORKER_NAME,
                BatchWorkerConstants.WORKER_API_VERSION, taskData, TaskStatus.NEW_TASK, new HashMap<>(),inputQueue,null));
        Mockito.verify(channel).basicPublish(Mockito.eq(""), Mockito.eq(inputQueue), Mockito.eq(MessageProperties.PERSISTENT_TEXT_PLAIN), Mockito.argThat(new TaskMessageMatcher(taskMessageSerialized)));
    }

    @Test
    public void testCacheFailure() throws ExecutionException, IOException, CodecException {
        //put Task message into the buffer before tests.
        batchWorkerPublisher.storeInMessageBuffer(outputQueue, new TaskMessage());
        Mockito.when(connection.createChannel()).thenReturn(outputChannel);
        Mockito.when(channelCache.get(outputQueue)).thenThrow(ExecutionException.class);
        BatchWorkerTask localTask = new BatchWorkerTask();
        localTask.targetPipe = outputQueue;
        Boolean exceptionThrown = false;
        try {
            services.registerItemSubtask(BatchWorkerConstants.WORKER_NAME, BatchWorkerConstants.WORKER_API_VERSION, localTask);
        } catch (TaskFailedException e) {
            exceptionThrown = true;
            Assert.assertEquals("Failed to retrieve or load queue channel from cache", e.getMessage());
        }
        Assert.assertTrue("Task Failed Exception should have been thrown", exceptionThrown);
    }

    @Test
    public void testSerializeFailure() throws ExecutionException, IOException, CodecException {
        //put Task message into the buffer before tests.
        batchWorkerPublisher.storeInMessageBuffer(outputQueue, new TaskMessage());
        Mockito.when(connection.createChannel()).thenReturn(outputChannel);
        Mockito.when(channelCache.get(outputQueue)).thenReturn(outputChannel);
        BatchWorkerTask localTask = new BatchWorkerTask();
        localTask.targetPipe = outputQueue;
        //Mock codec and recreate services localTask to used the new mocked codec.
        Codec codec = Mockito.mock(Codec.class);
        services = new BatchWorkerServicesImpl(localTask, codec, channelCache, connection, inputQueue, null, batchWorkerPublisher);
        Mockito.when(codec.serialise(localTask)).thenThrow(CodecException.class);
        Boolean exceptionThrown = false;
        try {
            services.registerItemSubtask(BatchWorkerConstants.WORKER_NAME, BatchWorkerConstants.WORKER_API_VERSION, localTask);
        } catch (TaskFailedException e) {
            exceptionThrown = true;
            Assert.assertEquals("Failed to serialize", e.getMessage());
        }
        Assert.assertTrue("Task Failed Exception should have been thrown", exceptionThrown);
    }

    @Test
    public void testGeneralException() throws ExecutionException, IOException, CodecException {
        //put Task message into the buffer before tests.
        batchWorkerPublisher.storeInMessageBuffer(outputQueue, new TaskMessage());
        Mockito.when(connection.createChannel()).thenReturn(outputChannel);
        Mockito.when(channelCache.get(outputQueue)).thenThrow(Exception.class);
        BatchWorkerTask localTask = new BatchWorkerTask();
        localTask.targetPipe = outputQueue;
        Boolean exceptionThrown = false;
        try {
            services.registerItemSubtask(BatchWorkerConstants.WORKER_NAME, BatchWorkerConstants.WORKER_API_VERSION, localTask);
        } catch (Exception e) {
            exceptionThrown = true;
        }
        Assert.assertTrue("Task Failed Exception should have been thrown", exceptionThrown);
    }

    @Test
    public void testTaskIds_subTasks() throws ExecutionException, CodecException, IOException {
        String taskId = "J1";
        Mockito.when(connection.createChannel()).thenReturn(outputChannel);
        Mockito.when(channelCache.get(outputQueue)).thenReturn(outputChannel);
        BatchWorkerPublisher batchWorkerPublisher = Mockito.mock(BatchWorkerPublisher.class);
        PublishAnswer answer = new PublishAnswer();
        Mockito.doAnswer(answer).when(batchWorkerPublisher).storeInMessageBuffer(Mockito.anyString(), Mockito.any());
        BatchWorkerTask localTask = new BatchWorkerTask();
        localTask.targetPipe = outputQueue;
        TrackingInfo trackingInfo = new TrackingInfo();
        trackingInfo.setJobTaskId(taskId);
        trackingInfo.setTrackTo(inputQueue);
        trackingInfo.setTrackingPipe(outputQueue);
        Date trackingStatusCheckTime = new Date();
        trackingInfo.setStatusCheckTime(trackingStatusCheckTime);
        BatchWorkerServices services = new BatchWorkerServicesImpl(localTask, codec, channelCache, connection, inputQueue, trackingInfo, batchWorkerPublisher);
        for (int i = 0; i < 10; i++) {
            services.registerItemSubtask(BatchWorkerConstants.WORKER_NAME, BatchWorkerConstants.WORKER_API_VERSION, localTask);
        }
        batchWorkerPublisher.publishLastMessage();
        List<TaskMessage> messages = answer.messageList;
        Assert.assertEquals("Should have sent 10 messages", 10, messages.size());
        for (int currentMessage = 0; currentMessage < messages.size(); currentMessage++) {
            String currentTaskId = taskId + "." + (currentMessage + 1);
            TaskMessage message = messages.get(currentMessage);
            Assert.assertEquals("Task Ids should match", currentTaskId, message.getTaskId());
            Assert.assertEquals("Task Ids on tracking should match", currentTaskId, message.getTracking().getJobTaskId());
            Assert.assertEquals("Tracking Status check time should match", trackingStatusCheckTime.getTime(), message.getTracking().getStatusCheckTime().getTime());
            Assert.assertEquals("TrackTo should match", trackingInfo.getTrackTo(), message.getTracking().getTrackTo());
            Assert.assertEquals("TrackingPipe should match", trackingInfo.getTrackingPipe(), message.getTracking().getTrackingPipe());
        }
    }

    @Test
    public void testTaskIds_subBatch() throws ExecutionException, CodecException, IOException {
        String taskId = "J1";
        Mockito.when(connection.createChannel()).thenReturn(outputChannel);
        Mockito.when(channelCache.get(outputQueue)).thenReturn(outputChannel);
        BatchWorkerPublisher batchWorkerPublisher = Mockito.mock(BatchWorkerPublisher.class);
        PublishAnswer answer = new PublishAnswer();
        Mockito.doAnswer(answer).when(batchWorkerPublisher).storeInMessageBuffer(Mockito.anyString(), Mockito.any());
        BatchWorkerTask localTask = new BatchWorkerTask();
        localTask.targetPipe = outputQueue;
        TrackingInfo trackingInfo = new TrackingInfo();
        trackingInfo.setJobTaskId(taskId);
        trackingInfo.setTrackTo(inputQueue);
        trackingInfo.setTrackingPipe(outputQueue);
        Date trackingStatusCheckTime = new Date();
        trackingInfo.setStatusCheckTime(trackingStatusCheckTime);
        BatchWorkerServices services = new BatchWorkerServicesImpl(localTask, codec, channelCache, connection, inputQueue, trackingInfo, batchWorkerPublisher);
        for (int i = 0; i < 5; i++) {
            services.registerBatchSubtask(null);
        }
        List<TaskMessage> messages = answer.messageList;
        for (int currentMessage = 0; currentMessage < messages.size(); currentMessage++) {
            String currentTaskId = taskId + "." + (currentMessage + 1);
            TaskMessage message = messages.get(currentMessage);
            Assert.assertEquals("Task Ids should match", currentTaskId, message.getTaskId());
            Assert.assertEquals("Task Ids on tracking should match", currentTaskId, message.getTracking().getJobTaskId());
            Assert.assertEquals("Tracking Status check time should match", trackingStatusCheckTime.getTime(), message.getTracking().getStatusCheckTime().getTime());
            Assert.assertEquals("TrackTo should match", trackingInfo.getTrackTo(), message.getTracking().getTrackTo());
            Assert.assertEquals("TrackingPipe should match", trackingInfo.getTrackingPipe(), message.getTracking().getTrackingPipe());
        }
    }

    @Test
    public void testPublishToTrackingPipe() throws ExecutionException, IOException, CodecException {
        Mockito.when(connection.createChannel()).thenReturn(outputChannel);
        Mockito.when(channelCache.get(outputQueue)).thenReturn(outputChannel);
        BatchWorkerTask localTask = new BatchWorkerTask();
        localTask.targetPipe = outputQueue;
        TrackingInfo trackingInfo = new TrackingInfo();
        String trackingQueue = "tracking-queue";
        trackingInfo.setTrackingPipe(trackingQueue);
        trackingInfo.setJobTaskId("J1");
        BatchWorkerServices services = new BatchWorkerServicesImpl(localTask, codec, channelCache, connection, inputQueue, trackingInfo, batchWorkerPublisher);
        services.registerItemSubtask(BatchWorkerConstants.WORKER_NAME, BatchWorkerConstants.WORKER_API_VERSION, localTask);
        //Create expected TaskMessage with TrackingInfo.
        byte[] taskMessageSerialized = codec.serialise(new TaskMessage(UUID.randomUUID().toString(), BatchWorkerConstants.WORKER_NAME,
                BatchWorkerConstants.WORKER_API_VERSION, taskData, TaskStatus.NEW_TASK, new HashMap<>(), outputQueue, trackingInfo));
        //Store a new message in the buffer to publish our actual message.
        batchWorkerPublisher.storeInMessageBuffer(inputQueue, new TaskMessage());
        Mockito.verify(outputChannel).basicPublish(Mockito.eq(""), Mockito.eq(trackingQueue), Mockito.eq(MessageProperties.PERSISTENT_TEXT_PLAIN), Mockito.argThat(new TaskMessageMatcher(taskMessageSerialized)));
    }
}

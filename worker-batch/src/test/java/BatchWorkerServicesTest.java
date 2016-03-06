import com.google.common.cache.LoadingCache;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskFailedException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.worker.batch.BatchWorkerConstants;
import com.hpe.caf.worker.batch.BatchWorkerServicesImpl;
import com.hpe.caf.worker.batch.BatchWorkerTask;
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
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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

    @Before
    public void setup() throws CodecException {
        task = new BatchWorkerTask();
        task.targetPipe = outputQueue;
        services = new BatchWorkerServicesImpl(task, codec, channelCache, connection, inputQueue);
        taskData = codec.serialise(task);
        taskMessageSerialized = codec.serialise(new TaskMessage(UUID.randomUUID().toString(), BatchWorkerConstants.WORKER_NAME,
                BatchWorkerConstants.WORKER_API_VERSION, taskData, TaskStatus.NEW_TASK, new HashMap<>()));
    }

    @Test
    public void testCreateTask() throws ExecutionException, IOException, CodecException {
        Mockito.when(connection.createChannel()).thenReturn(outputChannel);
        Mockito.when(channelCache.get(outputQueue)).thenReturn(outputChannel);
        BatchWorkerTask localTask = new BatchWorkerTask();
        localTask.targetPipe = outputQueue;
        services.registerItemSubtask(BatchWorkerConstants.WORKER_NAME, BatchWorkerConstants.WORKER_API_VERSION, localTask);
        Mockito.verify(outputChannel).basicPublish(Mockito.eq(""), Mockito.eq(outputQueue), Mockito.eq(MessageProperties.PERSISTENT_TEXT_PLAIN), Mockito.argThat(new TaskMessageMatcher(taskMessageSerialized)));
    }

    @Test
    public void testCreateSubBatch() throws IOException, ExecutionException, CodecException {
        Mockito.when(connection.createChannel()).thenReturn(channel);
        Mockito.when(channelCache.get(outputQueue)).thenReturn(outputChannel);
        Mockito.when(channelCache.get(inputQueue)).thenReturn(channel);
        services.registerBatchSubtask(null);
        Mockito.verify(channel).basicPublish(Mockito.eq(""), Mockito.eq(inputQueue), Mockito.eq(MessageProperties.PERSISTENT_TEXT_PLAIN), Mockito.argThat(new TaskMessageMatcher(taskMessageSerialized)));
    }

    @Test
    public void testCacheFailure() throws ExecutionException, IOException, CodecException {
        Mockito.when(connection.createChannel()).thenReturn(outputChannel);
        Mockito.when(channelCache.get(outputQueue)).thenThrow(ExecutionException.class);
        BatchWorkerTask localTask = new BatchWorkerTask();
        localTask.targetPipe = outputQueue;
        Boolean exceptionThrown = false;
        try {
            services.registerItemSubtask(BatchWorkerConstants.WORKER_NAME, BatchWorkerConstants.WORKER_API_VERSION, localTask);
        } catch (TaskFailedException e){
            exceptionThrown = true;
            Assert.assertEquals(e.getMessage(),"Failed to retrieve or load queue channel from cache");
        }
        Assert.assertTrue("Task Failed Exception should have been thrown", exceptionThrown);
    }

    @Test
    public void testSerializeFailure() throws ExecutionException, IOException, CodecException {
        Mockito.when(connection.createChannel()).thenReturn(outputChannel);
        Mockito.when(channelCache.get(outputQueue)).thenReturn(outputChannel);
        BatchWorkerTask localTask = new BatchWorkerTask();
        localTask.targetPipe = outputQueue;
        //Mock codec and recreate services localTask to used the new mocked codec.
        Codec codec = Mockito.mock(Codec.class);
        services = new BatchWorkerServicesImpl(task, codec, channelCache, connection, inputQueue);
        Mockito.when(codec.serialise(localTask)).thenThrow(CodecException.class);
        Boolean exceptionThrown = false;
        try {
            services.registerItemSubtask(BatchWorkerConstants.WORKER_NAME, BatchWorkerConstants.WORKER_API_VERSION, localTask);
        } catch (TaskFailedException e){
            exceptionThrown = true;
            Assert.assertEquals(e.getMessage(),"Failed to serialize");
        }
        Assert.assertTrue("Task Failed Exception should have been thrown",exceptionThrown);
    }

    @Test
    public void testGeneralException() throws ExecutionException, IOException {
        Mockito.when(connection.createChannel()).thenReturn(outputChannel);
        Mockito.when(channelCache.get(outputQueue)).thenThrow(Exception.class);
        BatchWorkerTask localTask = new BatchWorkerTask();
        localTask.targetPipe = outputQueue;
        Boolean exceptionThrown = false;
        try {
            services.registerItemSubtask(BatchWorkerConstants.WORKER_NAME, BatchWorkerConstants.WORKER_API_VERSION, localTask);
        } catch (Exception e){
            exceptionThrown = true;
        }
        Assert.assertTrue("Task Failed Exception should have been thrown", exceptionThrown);
    }
}

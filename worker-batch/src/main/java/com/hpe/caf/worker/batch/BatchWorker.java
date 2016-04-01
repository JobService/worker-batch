package com.hpe.caf.worker.batch;

import com.google.common.cache.LoadingCache;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskFailedException;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.worker.AbstractWorker;
import com.rabbitmq.client.Connection;

import java.util.Map;

public class BatchWorker extends AbstractWorker<BatchWorkerTask, BatchWorkerResult> {

    private final BatchWorkerServices batchWorkerServices;
    private final Map<String, BatchWorkerPlugin> registeredPlugins;
    private final TrackingInfo tracking;

    /**
     * Create a Worker. The input task will be validated.
     *
     * @param task        the input task for this Worker to operate on
     * @param tracking    additional fields used in tracking task messages
     * @param resultQueue the reference to the queue that should take results from this type of Worker
     * @param codec       used to serialising result data
     * @throws InvalidTaskException if the input task does not validate successfully
     */
    public BatchWorker(BatchWorkerTask task, TrackingInfo tracking, String resultQueue, Codec codec, LoadingCache channelCache, Connection conn, String inputQueue, Map<String, BatchWorkerPlugin> plugins) throws InvalidTaskException {
        super(task, resultQueue, codec);
        this.tracking = tracking;
        batchWorkerServices = new BatchWorkerServicesImpl(task, getCodec(), channelCache, conn, inputQueue);
        registeredPlugins = plugins;
    }

    @Override
    public WorkerResponse doWork() throws InterruptedException {
        try {
            checkIfInterrupted();
            BatchWorkerTask task = getTask();
            BatchWorkerPlugin batchWorkerPlugin = registeredPlugins.get(task.batchType);
            //If plugin not registered, check if full class name has been specified.
            if (batchWorkerPlugin == null) {
                Class pluginClass = ClassLoader.getSystemClassLoader().loadClass(task.batchType);
                batchWorkerPlugin = (BatchWorkerPlugin) pluginClass.newInstance();
            }
            batchWorkerPlugin.processBatch(batchWorkerServices, task.batchDefinition, task.taskMessageType, task.taskMessageParams);

        } catch (ReflectiveOperationException e) {
            throw new TaskFailedException("Invalid batch type  " + getTask().batchType);
        }
        catch(BatchDefinitionException e){
            throw new TaskFailedException("Failed to process batch", e);
        }

        //todo When tracking info added, set to taskId
        BatchWorkerResult result = new BatchWorkerResult();
        result.batchTask = getTask().targetPipe;
        return createSuccessResult(result);
    }

    @Override
    public String getWorkerIdentifier() {
        return BatchWorkerConstants.WORKER_NAME;
    }

    @Override
    public int getWorkerApiVersion() {
        return BatchWorkerConstants.WORKER_API_VERSION;
    }
}

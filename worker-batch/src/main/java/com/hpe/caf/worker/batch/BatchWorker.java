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

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.worker.AbstractWorker;

import java.util.Map;

public class BatchWorker extends AbstractWorker<BatchWorkerTask, BatchWorkerResult> {

    private final BatchWorkerServicesImpl batchWorkerServices;
    private final Map<String, BatchWorkerPlugin> registeredPlugins;
    private final BatchWorkerPublisher messagePublisher;
    private final BatchWorkerConfiguration configuration;
    private String jobId;

    /**
     * Create a Worker. The input task will be validated.
     *
     * @param task the input task for this Worker to operate on
     * @param configuration the batch worker configuration
     * @param codec used to serialising result data
     * @param plugins Map of plugins to use during processing
     * @param dataStore Workers datastore.
     * @param workerTaskData The task data to operate on
     * @throws InvalidTaskException if the input task does not validate successfully
     */
    public BatchWorker(final BatchWorkerTask task, final BatchWorkerConfiguration configuration, final Codec codec,
                       final Map<String, BatchWorkerPlugin> plugins, final DataStore dataStore, final WorkerTaskData workerTaskData)
        throws InvalidTaskException
    {
        super(task, configuration.getOutputQueue(), codec);
        this.configuration = configuration;
        this.messagePublisher = new BatchWorkerPublisher(workerTaskData);
        batchWorkerServices = new BatchWorkerServicesImpl(task, getCodec(), messagePublisher, workerTaskData.getTo());
        batchWorkerServices.register(DataStore.class, dataStore);
        registeredPlugins = plugins;
        try {
            final TrackingInfo trackingInfo = workerTaskData.getTrackingInfo();
            jobId = trackingInfo.getJobId();
        } catch (final Exception ex) {
            jobId = task.targetPipe;
        }
    }

    /**
     * Process the BatchWorkerTask to create and publish sub-tasks.
     *
     * @return WorkerResponse Response at the end of worker processing. The response content can change based on the return behaviour config.
     * @throws TaskRejectedException Any exception thrown during processing is wrapped in a TaskRejectedException before being thrown
     */
    @Override
    public WorkerResponse doWork() throws TaskRejectedException {
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

            final BatchWorkerResult result = new BatchWorkerResult();
            result.batchTask = jobId;

            // Read configuration entry for return value behaviour
            if(configuration.getReturnValueBehaviour()==null) {
                return createSuccessResult(result);
            }

            switch(configuration.getReturnValueBehaviour())
            {
                case RETURN_ALL:
                    // We return all results to the output queue
                    return createSuccessResult(result);
                case RETURN_NONE:
                    //  Return nothing to the output queue.
                    if (batchWorkerServices.hasSubtasks()) {
                        return createSuccessNoOutputToQueue();
                    } else {
                        //  Given no sub tasks, ensure the job is marked as completed.
                        return createTaskCompleteResponse();
                    }
                case RETURN_ONLY_IF_ZERO_SUBTASKS:
                    // We only return a result to the output queue if there were zero sub files with the batch
                    if(batchWorkerServices.hasSubtasks()) {
                        return createSuccessNoOutputToQueue();
                    } else {
                        return createSuccessResult(result);
                    }
                default:
                    return createSuccessResult(result);
            }
        } catch (final ReflectiveOperationException e) {
            throw new TaskFailedException("Invalid batch type  " + getTask().batchType);
        } catch (final BatchDefinitionException e) {
            throw new TaskFailedException("Failed to process batch", e);
        } catch(final BatchWorkerTransientException e) {
            throw new TaskRejectedException("Failed to process batch", e);
        } catch (final Throwable e) {
            throw new TaskFailedException("Failed to process batch", e);
        }
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

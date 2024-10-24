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


import com.github.cafapi.common.api.Codec;
import com.github.workerframework.worker.AbstractWorker;
import com.github.workerframework.worker.api.DataStore;
import com.github.workerframework.worker.api.InvalidTaskException;
import com.github.workerframework.worker.api.TaskFailedException;
import com.github.workerframework.worker.api.TaskRejectedException;
import com.github.workerframework.worker.api.TrackingInfo;
import com.github.workerframework.worker.api.WorkerResponse;
import com.github.workerframework.worker.api.WorkerTaskData;

import java.util.Map;

public class BatchWorker extends AbstractWorker<BatchWorkerTask, BatchWorkerResult> {

    private final BatchWorkerServicesImpl batchWorkerServices;
    private final Map<String, BatchWorkerPlugin> registeredPlugins;
    private final BatchWorkerConfiguration configuration;
    private final String jobId;

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
        super(task, configuration.getOutputQueue(), codec, workerTaskData);
        this.configuration = configuration;
        batchWorkerServices = new BatchWorkerServicesImpl(task, getCodec(), workerTaskData.getTo(), workerTaskData);
        batchWorkerServices.register(DataStore.class, dataStore);
        registeredPlugins = plugins;
        jobId = getJobId(workerTaskData.getTrackingInfo(), task);
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
                return createSuccessAndCompleteResponse(result);
            }

            switch(configuration.getReturnValueBehaviour())
            {
                case RETURN_ALL:
                    // We return all results to the output queue
                    return createSuccessAndCompleteResponse(result);
                case RETURN_NONE:
                    return createTaskCompleteResponse();
                case RETURN_ONLY_IF_ZERO_SUBTASKS:
                    // We only return a result to the output queue if there were zero sub files with the batch
                    if(batchWorkerServices.hasSubtasks()) {
                        return createTaskCompleteResponse();
                    } else {
                        return createSuccessAndCompleteResponse(result);
                    }
                default:
                    return createSuccessAndCompleteResponse(result);
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

    private static String getJobId(final TrackingInfo trackingInfo, final BatchWorkerTask task)
    {
        try {
            return trackingInfo.getJobId();
        } catch (final Exception ex) {
            return task.targetPipe;
        }
    }
}

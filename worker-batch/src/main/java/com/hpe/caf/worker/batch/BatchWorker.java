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

import com.google.common.cache.LoadingCache;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.worker.AbstractWorker;
import com.rabbitmq.client.Connection;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class BatchWorker extends AbstractWorker<BatchWorkerTask, BatchWorkerResult> {

    private final BatchWorkerServicesImpl batchWorkerServices;
    private final Map<String, BatchWorkerPlugin> registeredPlugins;
    private final TrackingInfo tracking;
    private final BatchWorkerPublisher messagePublisher;
    private final BatchWorkerConfiguration configuration;

    /**
     * Create a Worker. The input task will be validated.
     *
     * @param task the input task for this Worker to operate on
     * @param tracking additional fields used in tracking task messages
     * @param configuration the batch worker configuration
     * @param codec used to serialising result data
     * @throws InvalidTaskException if the input task does not validate successfully
     */
    public BatchWorker(BatchWorkerTask task, TrackingInfo tracking, BatchWorkerConfiguration configuration, Codec codec,
                       LoadingCache channelCache, Connection conn, String inputQueue, Map<String, BatchWorkerPlugin> plugins,
                       DataStore dataStore)
        throws InvalidTaskException
    {
        super(task, configuration.getOutputQueue(), codec);
        this.configuration = configuration;
        this.tracking = tracking;
        this.messagePublisher = new BatchWorkerPublisher(channelCache, codec);
        batchWorkerServices = new BatchWorkerServicesImpl(task, getCodec(), channelCache, conn, inputQueue, tracking, messagePublisher);
        batchWorkerServices.register(DataStore.class, dataStore);
        registeredPlugins = plugins;
    }

    /**
     * Process the BatchWorkerTask to create and publish sub-tasks.
     *
     * @return WorkerResponse.
     * @throws InterruptedException
     */
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
            if (!messagePublisher.isTaskMessageBufferEmpty()) {
                messagePublisher.publishLastMessage();
            }

            BatchWorkerResult result = new BatchWorkerResult();
            result.batchTask = tracking == null ? task.targetPipe : tracking.getJobId();

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
                    //  If there are no errors then no output needs to be output.
                    //  Also reset the 'trackTo' field on the tracking information but only if there are no
                    //  subtasks.
                    if (batchWorkerServices.hasSubtasks()) {
                        return createSuccessNoOutputToQueue(tracking == null ? StringUtils.EMPTY : tracking.getTrackTo());
                    } else {
                        return createSuccessNoOutputToQueue(null);
                    }
                case RETURN_ONLY_IF_ZERO_SUBTASKS:
                    // We only return a result to the output queue if there were zero subfiles with the batch
                    if(batchWorkerServices.hasSubtasks()) {
                        // If there are sub tasks, don't send to output queue
                        return createSuccessNoOutputToQueue(tracking == null ? StringUtils.EMPTY : tracking.getTrackTo());
                    } else {
                        // If there are no sub tasks, sent to output queue
                        return createSuccessResult(result);
                    }
                default:
                    return createSuccessResult(result);
            }
        } catch (ReflectiveOperationException e) {
            throw new TaskFailedException("Invalid batch type  " + getTask().batchType);
        } catch (BatchDefinitionException e) {
            throw new TaskFailedException("Failed to process batch", e);
        } catch (Throwable e) {
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

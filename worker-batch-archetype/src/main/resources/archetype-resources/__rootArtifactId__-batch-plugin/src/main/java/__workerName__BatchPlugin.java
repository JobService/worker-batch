/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.worker.batch.BatchDefinitionException;
import com.hpe.caf.worker.batch.BatchWorkerPlugin;
import com.hpe.caf.worker.batch.BatchWorkerServices;
import com.hpe.caf.worker.document.DocumentWorkerConstants;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import com.hpe.caf.worker.document.DocumentWorkerTask;

import java.io.IOException;
import java.util.*;

/**
 * Plugin that expects a batch definition as a JSON array of asset IDs and will create tasks from these IDs.
 */
public class ${workerName}BatchPlugin implements BatchWorkerPlugin
{
    public static ObjectMapper mapper = new ObjectMapper();

    @Override
    public void processBatch(final BatchWorkerServices batchWorkerServices, final String batchDefinition,
                             final String taskMessageType, final Map<String, String> taskMessageParams)
        throws BatchDefinitionException
    {
        // Expecting batch definition to be in the form of a serialized json array of string values.
        final String[] contents;
        try {
            contents = mapper.readValue(batchDefinition, String[].class);
        } catch (IOException e) {
            // Failed to process the batch
            throw new BatchDefinitionException("Unable to convert batch definition to array of contents.", e);
        }
        if (contents == null || contents.length == 0) {
            throw new BatchDefinitionException("No contents passed in on batch definition.");
        }

        if (!Objects.equals(taskMessageType, "DocumentMessage")) {
            throw new BatchDefinitionException("Unknown Task Message Type: " + taskMessageType);
        }

        //  Get content length as we intend to split the list into sub-batches.
        final int contentsLength = contents.length;

        if (contentsLength > 1) {

            //  Split the list and create sub-batches.
            final String[] subBatchArrayA = Arrays.copyOfRange(contents, 0, contentsLength/2);
            final String[] subBatchArrayB = Arrays.copyOfRange(contents, (contentsLength/2), contentsLength);

            final String subBatchArrayAString;
            final String subBatchArrayBString;
            try {
                subBatchArrayAString = mapper.writeValueAsString(subBatchArrayA);
                subBatchArrayBString = mapper.writeValueAsString(subBatchArrayB);
            } catch (JsonProcessingException e) {
                throw new BatchDefinitionException("Failed to serialize the sub-batch list as a string.", e);
            }

            batchWorkerServices.registerBatchSubtask(subBatchArrayAString);
            batchWorkerServices.registerBatchSubtask(subBatchArrayBString);
        } else {
            for (final String content : contents) {
                // Build a Document Worker Task Message that contains taskData 
                // containing a field with the content to the item
                final DocumentWorkerTask taskData = new DocumentWorkerTask();
                taskData.fields = new HashMap<>();

                final DocumentWorkerFieldValue contentFieldValue = new DocumentWorkerFieldValue();
                contentFieldValue.data = content;
                contentFieldValue.encoding = DocumentWorkerFieldEncoding.utf8;

                String contentFieldName = taskMessageParams.get("contentFieldName");
                // If the name of the field was not specified in the task message params, default to CONTENT
                if (contentFieldName == null) {
                    contentFieldName = "CONTENT";
                }

                taskData.fields.put(contentFieldName, Arrays.asList(contentFieldValue));

                // Register item to send with BatchWorkerServices
                batchWorkerServices.registerItemSubtask(DocumentWorkerConstants.WORKER_NAME,
                        DocumentWorkerConstants.WORKER_API_VER, taskData);
            }
        }
    }
}

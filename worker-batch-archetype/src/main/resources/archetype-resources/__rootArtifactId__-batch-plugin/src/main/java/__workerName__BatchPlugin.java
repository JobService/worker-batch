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
        final String[] references;
        try {
            references = mapper.readValue(batchDefinition, String[].class);
        } catch (IOException e) {
            // Failed to process the batch
            throw new BatchDefinitionException("Unable to convert batch definition to array of references.", e);
        }
        if (references == null || references.length == 0) {
            throw new BatchDefinitionException("No asset ids passed in on batch definition.");
        }

        if (!Objects.equals(taskMessageType, "DocumentMessage")) {
            throw new BatchDefinitionException("Unknown Task Message Type: " + taskMessageType);
        }

        //  Get reference length as we intend to split the list into sub-batches.
        final int referencesLength = references.length;

        if (referencesLength > 1) {

            //  Split the list and create sub-batches.
            final String[] subBatchArrayA = Arrays.copyOfRange(references, 0, referencesLength/2);
            final String[] subBatchArrayB = Arrays.copyOfRange(references, (referencesLength/2), referencesLength);

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
            for (final String reference : references) {
                // Build a Document Worker Task Message that contains taskData 
                // containing a field with the reference to the item
                final DocumentWorkerTask taskData = new DocumentWorkerTask();
                taskData.fields = new HashMap<>();

                final DocumentWorkerFieldValue referenceFieldValue = new DocumentWorkerFieldValue();
                referenceFieldValue.data = reference;
                referenceFieldValue.encoding = DocumentWorkerFieldEncoding.utf8;

                String referenceFieldName = taskMessageParams.get("referenceFieldName");
                // If the name of the field was not specified in the task message params, default to STORAGE_REFERENCE
                if (referenceFieldName == null) {
                    referenceFieldName = "storageReference";
                }

                taskData.fields.put(referenceFieldName, Arrays.asList(referenceFieldValue));

                // Register item to send with BatchWorkerServices
                batchWorkerServices.registerItemSubtask(DocumentWorkerConstants.WORKER_NAME,
                        DocumentWorkerConstants.WORKER_API_VER, taskData);
            }
        }
    }
}

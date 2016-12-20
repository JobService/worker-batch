package com.hpe.caf.worker.batch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the batch worker plugin. Will take in an array of string arrays. Each string comprises a comma separated list
 * which will be split into subtasks.
 */
public class BatchComplexPluginTestImpl implements BatchWorkerPlugin
{
    public static ObjectMapper mapper = new ObjectMapper();

    static class BatchDefinitionReferences
    {
        public List<List<String>> stringValueList;

        @JsonCreator
        public BatchDefinitionReferences(final List<List<String>> stringValueList)
        {
            this.stringValueList = stringValueList;
        }
    }

    @Override
    public void processBatch(BatchWorkerServices batchWorkerServices, String batchDefinition, String taskMessageType,
                             Map<String, String> taskMessageParams) throws BatchDefinitionException
    {
        //  Expecting batch definition to be in the form of a serialized json array of string array values.
        BatchDefinitionReferences references;
        try {
            references = mapper.readValue(batchDefinition, BatchDefinitionReferences.class);
        } catch (IOException e) {
            //failed to process the batch
            throw new BatchDefinitionException("Unable to convert batch definition to array of string array references.", e);
        }

        if (references.stringValueList.size() > 1) {
            //  Array comprises a number of string arrays.
            //  Split these up and register new sub batches for them.
            for (List<String> refValues : references.stringValueList) {
                String subBatchDefinition = "";
                for (String s : refValues) {
                    subBatchDefinition = subBatchDefinition + "\"" + s + "\",";
                }
                batchWorkerServices.registerBatchSubtask("[[" + subBatchDefinition.replaceAll(",$", "") + "]]");
            }
        } else {
            //  Array comprises a single string array.
            List<String> refValues = references.stringValueList.get(0);
            if (refValues.size() > 1) {
                //  String array has been passed a number of string values.
                //  Split these up and register new sub batches for them.
                for (String s : refValues) {
                    batchWorkerServices.registerBatchSubtask("[[\"" + s + "\"]]");
                }
            } else {
                //  A single string has been providied.
                //  Split the comma separated list into subtasks.
                List<String> items = Arrays.asList(refValues.get(0).split("\\s*,\\s*"));
                for (String subItem : items) {
                    batchWorkerServices.registerItemSubtask("BatchComplexPluginTest", 1, subItem);
                }
            }
        }
    }
}

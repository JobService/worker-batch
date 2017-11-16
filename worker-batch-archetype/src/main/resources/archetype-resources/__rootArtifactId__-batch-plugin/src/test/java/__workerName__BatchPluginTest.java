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
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.messagebuilder.TaskMessage;
import com.hpe.caf.worker.batch.BatchDefinitionException;
import com.hpe.caf.worker.batch.BatchWorkerServices;
import com.hpe.caf.worker.document.DocumentWorkerConstants;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.document.DocumentWorkerTask;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Testing the ${workerName} Batch plugin class.
 */
public class ${workerName}BatchPluginTest
{
    ObjectMapper mapper = new ObjectMapper();
    BatchWorkerServices testWorkerServices = null;
    String taskMessageType = null;
    Map<String, String> testTaskMessageParams = null;
    int subBatchCount = 0;

    /**
     * Testing that a simple batch definition of "aa", "bb", "cc", "esgesges/adawf" ends up constructing 4 tasks and
     * registering each with the BatchWorkerServicesImpl provided. The task sent should also have been constructed
     * correctly using DocumentMessage specified via taskMessageType.
     */
    @Test
    public void testSimpleBatch() throws JsonProcessingException, BatchDefinitionException
    {
        Collection<TaskMessage> constructedTaskMessages = new ArrayList<>();
        ArrayList<String> testContents = createContentsList("aa", "bb", "cc", "esgesges/adawf");
        int expectedSubBatchCount = 6;

        testWorkerServices = createTestBatchWorkerServices(constructedTaskMessages);

        String contentFieldNameParamKey = "contentFieldName";
        String contentFieldNameParamValue = "CONTENT";
        testTaskMessageParams = createTaskMessageParams(
                new AbstractMap.SimpleEntry<>(contentFieldNameParamKey, contentFieldNameParamValue));
        String batchDefinition = mapper.writeValueAsString(testContents);
        taskMessageType = "DocumentMessage";

        ${workerName}BatchPlugin plugin = new ${workerName}BatchPlugin();
        plugin.processBatch(testWorkerServices, batchDefinition, taskMessageType, testTaskMessageParams);

        // Verify that expected number of sub batches were registered
        Assert.assertEquals("Expecting sub-batches to be created.",
                expectedSubBatchCount, subBatchCount);

        // Verify that expected number of messages were registered
        Assert.assertEquals("Expecting same number of task messages generated as contents we had in batch " +
                "definition.", testContents.size(), constructedTaskMessages.size());

        for(TaskMessage returnedMessage: constructedTaskMessages){
            checkClassifierAndApiVersion(returnedMessage);

            DocumentWorkerTask returnedTaskData = (DocumentWorkerTask) returnedMessage.getTaskData();
            Assert.assertNotNull("Expecting task data returned to not be null.", returnedTaskData);

            String returnedContentFieldDocumentWorkerFieldValueData =
                    returnedTaskData.fields.get(contentFieldNameParamValue).get(0).data;
            DocumentWorkerFieldEncoding returnedContentFieldDocumentWorkerFieldValueEncoding =
                    returnedTaskData.fields.get(contentFieldNameParamValue).get(0).encoding;

            Assert.assertEquals("Expected encoding to be set to utf8", DocumentWorkerFieldEncoding.utf8,
                    returnedContentFieldDocumentWorkerFieldValueEncoding);
            Assert.assertTrue("Expecting the content value to be the content on the batch definition we passed " +
                    "in.", testContents.contains(returnedContentFieldDocumentWorkerFieldValueData));
        }
    }

    private void checkClassifierAndApiVersion(TaskMessage returnedMessage)
    {
        Assert.assertEquals("Expecting task api version to be that defined in Test builder.",
                DocumentWorkerConstants.WORKER_API_VER, returnedMessage.getTaskApiVersion());
        Assert.assertEquals("Expecting task classifier to be that defined in Test builder.",
                DocumentWorkerConstants.WORKER_NAME, returnedMessage.getTaskClassifier());
    }

    private ArrayList<String> createContentsList(String... contents)
    {
        ArrayList<String> testContents = new ArrayList<>();
        for(String content: contents){
        testContents.add(content);
        }
        return testContents;
    }

    @SafeVarargs
    private final Map<String, String> createTaskMessageParams(Map.Entry<String, String>... entries)
    {
        Map<String, String> testTaskMessageParams = new HashMap<>();
        for(Map.Entry<String, String> entry: entries){
            testTaskMessageParams.put(entry.getKey(), entry.getValue());
        }
        return testTaskMessageParams;
    }

    private BatchWorkerServices createTestBatchWorkerServices(Collection<TaskMessage> constructedTaskMessages)
    {
        return new BatchWorkerServices()
        {
            @Override
            public void registerBatchSubtask(String batchDefinition)
            {
                ${workerName}BatchPlugin plugin = new ${workerName}BatchPlugin();
                try {
                    plugin.processBatch(testWorkerServices, batchDefinition, taskMessageType, testTaskMessageParams);
                    subBatchCount = subBatchCount + 1;
                } catch (BatchDefinitionException e) {
                    throw new RuntimeException(e.getMessage(),e.getCause());
                }
            }

            @Override
            public void registerItemSubtask(String taskClassifier, int taskApiVersion, Object taskData)
            {
                // Store this as a task message so we can refer to it when verifying results
                TaskMessage message = new TaskMessage();
                message.setTaskApiVersion(taskApiVersion);
                message.setTaskClassifier(taskClassifier);
                message.setTaskData(taskData);
                constructedTaskMessages.add(message);
            }

            @Override
            public <S> S getService(Class<S> aClass)
            {
                return null;
            }
        };
    }
}

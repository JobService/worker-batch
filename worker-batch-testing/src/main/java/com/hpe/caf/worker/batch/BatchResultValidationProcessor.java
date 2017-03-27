package com.hpe.caf.worker.batch;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import com.hpe.caf.worker.document.DocumentWorkerTask;
import com.hpe.caf.worker.testing.ResultProcessor;
import com.hpe.caf.worker.testing.TestItem;
import org.apache.commons.io.IOUtils;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BatchResultValidationProcessor implements ResultProcessor {

    private final Codec codec;

    private final DataStore dataStore;

    public BatchResultValidationProcessor(final Codec codec, final DataStore dataStore) {
        this.codec = codec;
        this.dataStore = dataStore;
    }

    @Override
    public boolean process(final TestItem testItem, final TaskMessage resultMessage) throws Exception {
        if (resultMessage.getTaskStatus() == TaskStatus.RESULT_SUCCESS) {
            final BatchTestInput testInput = (BatchTestInput) testItem.getInputData();
            final BatchTestExpectation testOuput = (BatchTestExpectation) testItem.getExpectedOutputData();
            final BatchWorkerTask inputTask = testInput.getTask();
            final List<TaskMessage> subTasks =
                    BatchTargetQueueRetriever.getInstance().retrieveMessages(inputTask.targetPipe);
            if (subTasks.size() != testOuput.getSubTasks().size()) {
                System.out.println("Sub tasks count mismatch - actual (" + subTasks.size() + ") : expected ("
                        + testOuput.getSubTasks().size() + ")");
                return false;
            }
            compareSubTasks(testOuput.getSubTasks(), subTasks);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getInputIdentifier(final TaskMessage message) {
        return message.getTaskId();
    }

    private void compareSubTasks(final List<TaskMessage>expectedSubTasks, final List<TaskMessage> actualSubTasks)
            throws CodecException {
        for (int i = 0; i < actualSubTasks.size(); i++) {
            final String actualSubTaskClassifier = actualSubTasks.get(i).getTaskClassifier();
            assertEquals(actualSubTaskClassifier, expectedSubTasks.get(i).getTaskClassifier(),
                    "Subtasks classifier should match");
            assertEquals(actualSubTasks.get(i).getTaskApiVersion(), expectedSubTasks.get(i).getTaskApiVersion(),
                    "Subtasks task API version should match");
            assertEquals(actualSubTasks.get(i).getTaskStatus(), expectedSubTasks.get(i).getTaskStatus(),
                    "Subtasks task status should match");
            final byte[] expectedTaskData = expectedSubTasks.get(i).getTaskData();
            final byte[] actualTaskData = actualSubTasks.get(i).getTaskData();
            final boolean taskDataMatched = Arrays.equals(expectedTaskData, actualTaskData);
            if (!taskDataMatched && actualSubTaskClassifier.equals("DocumentWorker")) {
                // Expected and Actual taskData byte arrays do not match so perform additional validation
                compareDocumentWorkerTaskData(expectedTaskData, actualTaskData);
            } else {
                assertTrue(taskDataMatched, "Task data should match");
            }
        }
    }

    /**
     * Deserialise and compare that byte arrays of expected and actual taskDatas match.
     *
     * @param expectedTaskData the expected taskData byte array to match against
     * @param actualTaskData the actual taskData byte array to match against
     */
    public void compareDocumentWorkerTaskData(final byte[] expectedTaskData,
                                              final byte[] actualTaskData) throws CodecException {
        // Use the codec to deserialise the taskDatas into DocumentWorkerTask objects
        final DocumentWorkerTask expectedDocumentWorkerTaskData = codec.deserialise(expectedTaskData,
                DocumentWorkerTask.class);
        final DocumentWorkerTask actualDocumentWorkerTaskData = codec.deserialise(actualTaskData,
                DocumentWorkerTask.class);
        // Validate that the expected and actual task data custom data match
        validateTaskDataCustomData(expectedDocumentWorkerTaskData, actualDocumentWorkerTaskData);
        // Validate that the expected and actual task data fields match
        validateTaskDataFields(expectedDocumentWorkerTaskData, actualDocumentWorkerTaskData);
    }

    /**
     * Validate that the expected and actual DocumentWorkerTaskData customDatas match.
     *
     * @param expectedDocumentWorkerTaskData the expected DocumentWorkerTask containing customData to match against
     * @param actualDocumentWorkerTaskData the actual DocumentWorkerTask containing customData to match against
     */
    private void validateTaskDataCustomData(final DocumentWorkerTask expectedDocumentWorkerTaskData,
                                          final DocumentWorkerTask actualDocumentWorkerTaskData) {
        final Map<String, String> actualDocumentWorkerTaskDataCustomData = actualDocumentWorkerTaskData.customData;
        final Map<String, String> expectedDocumentWorkerTaskDataCustomData = expectedDocumentWorkerTaskData.customData;

        assertEquals(actualDocumentWorkerTaskDataCustomData, expectedDocumentWorkerTaskDataCustomData,
                "Expected DocumentWorkerTask TaskData Custom Data should contain Actual DocumentWorkerTask TaskData " +
                        "Custom Data under test");
    }

    /**
     * Validate that the expected and actual DocumentWorkerTaskData fields match.
     *
     * @param expectedDocumentWorkerTaskData the expected DocumentWorkerTask containing fields to match against
     * @param actualDocumentWorkerTaskData the actual DocumentWorkerTask containing fields to match against
     */
    private void validateTaskDataFields(final DocumentWorkerTask expectedDocumentWorkerTaskData,
                                      final DocumentWorkerTask actualDocumentWorkerTaskData) {
        final Map<String, List<DocumentWorkerFieldValue>> actualDocumentWorkerTaskDataFields =
                actualDocumentWorkerTaskData.fields;
        final Map<String, List<DocumentWorkerFieldValue>> expectedDocumentWorkerTaskDataFields =
                expectedDocumentWorkerTaskData.fields;
        // Compare expected with actual
        validateDocumentWorkerTaskDataFields(expectedDocumentWorkerTaskDataFields, actualDocumentWorkerTaskDataFields);
        // Compare actual with expected
        validateDocumentWorkerTaskDataFields(actualDocumentWorkerTaskDataFields, expectedDocumentWorkerTaskDataFields);
    }

    /**
     * Validated a String Key to List<DocumentWorkerFieldValue> Value pair Map with another String Key to
     * List<DocumentWorkerFieldValue> Value pair Map
     * @param lHSDocumentWorkerTaskDataFields left hand side Map of fields of DocumentWorkerFieldValues
     * @param rHSDocumentWorkerTaskDataFields right hand side Map of fields of DocumentWorkerFieldValues
     */
    private void validateDocumentWorkerTaskDataFields(
            final Map<String, List<DocumentWorkerFieldValue>> lHSDocumentWorkerTaskDataFields,
            final Map<String, List<DocumentWorkerFieldValue>> rHSDocumentWorkerTaskDataFields) {
        lHSDocumentWorkerTaskDataFields.entrySet().stream().forEach(stringListEntry -> {
            final List<DocumentWorkerFieldValue> listOfLHSDocumentWorkerFieldValues = stringListEntry.getValue();
            final List<DocumentWorkerFieldValue> listOfRHSDocumentWorkerFieldValues =
                    rHSDocumentWorkerTaskDataFields.get(stringListEntry.getKey());
            assertNotNull(listOfRHSDocumentWorkerFieldValues, "Map of right handside fields should contain key from " +
                    "left handside map.");
            compareListsOfDocumentWorkerFieldValues(listOfLHSDocumentWorkerFieldValues,
                    listOfRHSDocumentWorkerFieldValues);
        });
    }

    /**
     * Compare that lists of actual and expected DocumentWorkerFieldValues match.
     *
     * @param expectedListOfDocumentWorkerFieldValues list of expected DocumentWorkerFieldValues containing data to
     *                                                match against
     * @param actualListOfDocumentWorkerFieldValues list of actual DocumentWorkerFieldValues containing data to match
     *                                              against
     */
    private void compareListsOfDocumentWorkerFieldValues(
            final List<DocumentWorkerFieldValue> expectedListOfDocumentWorkerFieldValues,
            final List<DocumentWorkerFieldValue> actualListOfDocumentWorkerFieldValues) {

        assertEquals(actualListOfDocumentWorkerFieldValues.size(), expectedListOfDocumentWorkerFieldValues.size(),
                "DocumentWorkerFieldValue Lists should be of the same length");

        final LinkedList<DocumentWorkerFieldValue> expectedListOfDocumentWorkerFieldValuesLinkedList =
                new LinkedList<>(expectedListOfDocumentWorkerFieldValues);

        for (final DocumentWorkerFieldValue actualDocumentWorkerFieldValue : actualListOfDocumentWorkerFieldValues) {
            boolean result;
            Iterator<DocumentWorkerFieldValue> expectedListOfDocumentWorkerFieldValuesIter =
                    expectedListOfDocumentWorkerFieldValuesLinkedList.iterator();

            while(expectedListOfDocumentWorkerFieldValuesIter.hasNext()) {
                DocumentWorkerFieldValue expectedDocumentWorkerFieldValue =
                        expectedListOfDocumentWorkerFieldValuesIter.next();
                // Compare the expected and actual DocumentWorkerFieldValues in byte array form
                try {
                    byte[] expectedDocumentWorkerFieldValueBytes = getBytes(expectedDocumentWorkerFieldValue);
                    byte[] actualDocumentWorkerFieldValueBytes = getBytes(actualDocumentWorkerFieldValue);

                    result = Arrays.equals(expectedDocumentWorkerFieldValueBytes, actualDocumentWorkerFieldValueBytes);
                    // If there was a match remove that element from the linked list else if there are no more elements
                    // to compare against in the linked list assert the result
                    if (result) {
                        expectedListOfDocumentWorkerFieldValuesIter.remove();
                        break;
                    } else if (!expectedListOfDocumentWorkerFieldValuesIter.hasNext()) {
                        assertTrue(result, "Expected DocumentWorkerFieldValue to actual DocumentWorkerFieldValue " +
                                "mismatch");
                    }
                } catch (DataStoreException e) {
                    System.out.println("DataStoreException thrown: " + e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    System.out.println("IOException thrown: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private byte[] getBytes(final DocumentWorkerFieldValue value) throws DataStoreException, IOException {
        return getBytes(value.encoding, value.data);
    }

    private byte[] getBytes(final DocumentWorkerFieldEncoding encoding, final String data) throws DataStoreException,
            IOException {
        final String nonNullData = nullToEmpty(data);
        switch (nullToUtf8(encoding)) {
            case storage_ref:
                return IOUtils.toByteArray(dataStore.retrieve(nonNullData));
            case base64:
                return Base64.getDecoder().decode(nonNullData);
            default: //utf8
                return nonNullData.getBytes(StandardCharsets.UTF_8);
        }
    }

    private static String nullToEmpty(final String str)
    {
        return (str != null) ? str : "";
    }

    private static DocumentWorkerFieldEncoding nullToUtf8(final DocumentWorkerFieldEncoding encoding)
    {
        return (encoding != null) ? encoding : DocumentWorkerFieldEncoding.utf8;
    }
}

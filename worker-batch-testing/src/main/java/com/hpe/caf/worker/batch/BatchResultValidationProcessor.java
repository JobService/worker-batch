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
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BatchResultValidationProcessor implements ResultProcessor {

    private final Codec codec;

    private static Logger logger = Logger.getLogger(BatchResultValidationProcessor.class);

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
            return compareSubTasks(testOuput.getSubTasks(), subTasks);
        } else {
            return false;
        }
    }

    @Override
    public String getInputIdentifier(final TaskMessage message) {
        return message.getTaskId();
    }

    public boolean compareSubTasks(final List<TaskMessage> expectedSubTasks, final List<TaskMessage> actualSubTasks)
            throws CodecException {

        Iterator<TaskMessage> actualListOfTaskMessagesLinkedListIter = actualSubTasks.iterator();

        while (actualListOfTaskMessagesLinkedListIter.hasNext()) {
            final TaskMessage actualTaskMessage = actualListOfTaskMessagesLinkedListIter.next();
            Iterator<TaskMessage> expectedListOfTaskMessagesLinkedListIter = expectedSubTasks.iterator();
            while (expectedListOfTaskMessagesLinkedListIter.hasNext()) {
                final TaskMessage expectedTaskMessage = expectedListOfTaskMessagesLinkedListIter.next();

                // Validate the task message classifier
                final String actualSubTaskClassifier = actualTaskMessage.getTaskClassifier();
                boolean taskMessageVariablesMatched =
                        actualSubTaskClassifier.equals(expectedTaskMessage.getTaskClassifier());
                if (!taskMessageVariablesMatched && expectedListOfTaskMessagesLinkedListIter.hasNext()) {
                    logger.debug("Actual and Expected Task Message Task Classifiers mismatched. Attempting to match " +
                            "with the next expected Task Message.");
                    continue;
                } else if (!taskMessageVariablesMatched && !expectedListOfTaskMessagesLinkedListIter.hasNext()) {
                    logger.debug("Actual and Expected Task Message Task Classifiers mismatched. There are no more " +
                            "expected Task Messages to attempt to validate against.");
                    return false;
                }

                // Validate the task message api version
                taskMessageVariablesMatched =
                        actualTaskMessage.getTaskApiVersion() == expectedTaskMessage.getTaskApiVersion();
                if (!taskMessageVariablesMatched && expectedListOfTaskMessagesLinkedListIter.hasNext()) {
                    logger.debug("Actual and Expected Task Message Task API Versions mismatched. Attempting to match " +
                            "with the next expected Task Message.");
                    continue;
                } else if (!taskMessageVariablesMatched && !expectedListOfTaskMessagesLinkedListIter.hasNext()) {
                    logger.debug("Actual and Expected Task Message Task API Versions mismatched. There are no more " +
                            "expected Task Messages to attempt to validate against.");
                    return false;
                }

                // Validate the task message task status
                taskMessageVariablesMatched =
                        actualTaskMessage.getTaskStatus().equals(expectedTaskMessage.getTaskStatus());
                if (!taskMessageVariablesMatched && expectedListOfTaskMessagesLinkedListIter.hasNext()) {
                    logger.debug("Actual and Expected Task Message Task Statuses mismatched. Attempting to match " +
                            "with the next expected Task Message.");
                    continue;
                } else if (!taskMessageVariablesMatched && !expectedListOfTaskMessagesLinkedListIter.hasNext()) {
                    logger.debug("Actual and Expected Task Message Task Statuses mismatched. There are no more " +
                            "expected Task Messages to attempt to validate against.");
                    return false;
                }

                final byte[] expectedTaskData = expectedTaskMessage.getTaskData();
                final byte[] actualTaskData = actualTaskMessage.getTaskData();
                boolean taskDataMatched = Arrays.equals(expectedTaskData, actualTaskData);

                // Expected and Actual taskData byte arrays do not match so perform additional validation
                if (!taskDataMatched && actualSubTaskClassifier.equals("DocumentWorker")) {
                    taskDataMatched = compareDocumentWorkerTaskData(expectedTaskData, actualTaskData);
                }

                // If the taskData did not match and there is still another expected TaskMessage to match against
                // continue to the next expected message for matching.
                if (!taskDataMatched && expectedListOfTaskMessagesLinkedListIter.hasNext()) {
                    logger.debug("Task Message returned from worker did not match with Expected Task Message. " +
                            "Attempting to match with the next expected Task Message.");
                    continue;
                }

                // If actual and expected task messages matched, remove them from their lists and break out of the loop
                if (taskDataMatched) {
                    logger.info("Task Message returned from worker matched with Expected Task Message.");
                    actualListOfTaskMessagesLinkedListIter.remove();
                    expectedListOfTaskMessagesLinkedListIter.remove();
                    break;
                }
            }
        }

        // If there are no more actual and expected subtasks, validation succeeded, return true
        return actualSubTasks.size() == 0 && expectedSubTasks.size() == 0;
    }

    /**
     * Deserialise and compare that byte arrays of expected and actual taskDatas match.
     *
     * @param expectedTaskData the expected taskData byte array to match against
     * @param actualTaskData the actual taskData byte array to match against
     * @return result of comparison
     */
    public boolean compareDocumentWorkerTaskData(final byte[] expectedTaskData,
                                              final byte[] actualTaskData) throws CodecException {
        // Use the codec to deserialise the taskDatas into DocumentWorkerTask objects
        final DocumentWorkerTask expectedDocumentWorkerTaskData = codec.deserialise(expectedTaskData,
                DocumentWorkerTask.class);
        final DocumentWorkerTask actualDocumentWorkerTaskData = codec.deserialise(actualTaskData,
                DocumentWorkerTask.class);
        // Validate that the expected and actual task data custom data match
        if (validateTaskDataCustomData(expectedDocumentWorkerTaskData, actualDocumentWorkerTaskData)) {
            // Validate that the expected and actual task data fields match
            return validateTaskDataFields(expectedDocumentWorkerTaskData, actualDocumentWorkerTaskData);
        }

        return false;
    }

    /**
     * Validate that the expected and actual DocumentWorkerTaskData customDatas match.
     *
     * @param expectedDocumentWorkerTaskData the expected DocumentWorkerTask containing customData to match against
     * @param actualDocumentWorkerTaskData the actual DocumentWorkerTask containing customData to match against
     * @return result of comparison
     */
    private boolean validateTaskDataCustomData(final DocumentWorkerTask expectedDocumentWorkerTaskData,
                                          final DocumentWorkerTask actualDocumentWorkerTaskData) {
        final Map<String, String> actualDocumentWorkerTaskDataCustomData = actualDocumentWorkerTaskData.customData;
        final Map<String, String> expectedDocumentWorkerTaskDataCustomData = expectedDocumentWorkerTaskData.customData;

        boolean customDataCompareResult =
                expectedDocumentWorkerTaskDataCustomData.equals(actualDocumentWorkerTaskDataCustomData);

        if (!customDataCompareResult) {
            logger.debug("Custom Data Field mismatching between Actual and Expected DocumentWorkerTaskData");
        }

        return customDataCompareResult;
    }

    /**
     * Validate that the expected and actual DocumentWorkerTaskData fields match.
     *
     * @param expectedDocumentWorkerTaskData the expected DocumentWorkerTask containing fields to match against
     * @param actualDocumentWorkerTaskData the actual DocumentWorkerTask containing fields to match against
     * @return result of comparison
     */
    private boolean validateTaskDataFields(final DocumentWorkerTask expectedDocumentWorkerTaskData,
                                      final DocumentWorkerTask actualDocumentWorkerTaskData) {
        final Map<String, List<DocumentWorkerFieldValue>> actualDocumentWorkerTaskDataFields =
                actualDocumentWorkerTaskData.fields;
        final Map<String, List<DocumentWorkerFieldValue>> expectedDocumentWorkerTaskDataFields =
                expectedDocumentWorkerTaskData.fields;
        // Compare expected with actual and actual with expected and return the result
        return validateDocumentWorkerTaskDataFields(expectedDocumentWorkerTaskDataFields,
                actualDocumentWorkerTaskDataFields)
                && validateDocumentWorkerTaskDataFields(actualDocumentWorkerTaskDataFields,
                expectedDocumentWorkerTaskDataFields);
    }

    /**
     * Validated a String Key to List<DocumentWorkerFieldValue> Value pair Map with another String Key to
     * List<DocumentWorkerFieldValue> Value pair Map
     * @param lHSDocumentWorkerTaskDataFields left hand side Map of fields of DocumentWorkerFieldValues
     * @param rHSDocumentWorkerTaskDataFields right hand side Map of fields of DocumentWorkerFieldValues
     * @return result of comparison
     */
    private boolean validateDocumentWorkerTaskDataFields(
            final Map<String, List<DocumentWorkerFieldValue>> lHSDocumentWorkerTaskDataFields,
            final Map<String, List<DocumentWorkerFieldValue>> rHSDocumentWorkerTaskDataFields) {
        boolean result = false;
        for (final Map.Entry<String, List<DocumentWorkerFieldValue>> lHSDocumentWorkerTaskDataField :
                lHSDocumentWorkerTaskDataFields.entrySet()) {
            final List<DocumentWorkerFieldValue> listOfLHSDocumentWorkerFieldValues =
                    lHSDocumentWorkerTaskDataField.getValue();
            final List<DocumentWorkerFieldValue> listOfRHSDocumentWorkerFieldValues =
                    rHSDocumentWorkerTaskDataFields.get(lHSDocumentWorkerTaskDataField.getKey());
            // Map of right handside fields should contain key from left handside map, if so compare them.
            if (listOfRHSDocumentWorkerFieldValues != null) {
                result = compareListsOfDocumentWorkerFieldValues(listOfLHSDocumentWorkerFieldValues,
                        listOfRHSDocumentWorkerFieldValues);
            } else {
                logger.debug("Map of right handside fields does not contain key from left handside map.");
                return false;
            }
        }
        return result;
    }

    /**
     * Compare that lists of actual and expected DocumentWorkerFieldValues match.
     *
     * @param expectedListOfDocumentWorkerFieldValues list of expected DocumentWorkerFieldValues containing data to
     *                                                match against
     * @param actualListOfDocumentWorkerFieldValues list of actual DocumentWorkerFieldValues containing data to match
     *                                              against
     * @return result of comparison
     */
    private boolean compareListsOfDocumentWorkerFieldValues(
            final List<DocumentWorkerFieldValue> expectedListOfDocumentWorkerFieldValues,
            final List<DocumentWorkerFieldValue> actualListOfDocumentWorkerFieldValues) {

        boolean result = false;

        if (expectedListOfDocumentWorkerFieldValues.size() != actualListOfDocumentWorkerFieldValues.size()) {
            logger.debug("Expected and Actual DocumentWorkerFieldValue Lists are not of the same length");
            return result;
        }

        final LinkedList<DocumentWorkerFieldValue> expectedListOfDocumentWorkerFieldValuesLinkedList =
                new LinkedList<>(expectedListOfDocumentWorkerFieldValues);

        for (final DocumentWorkerFieldValue actualDocumentWorkerFieldValue : actualListOfDocumentWorkerFieldValues) {
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
                    // to compare against in the linked list return false
                    if (result) {
                        expectedListOfDocumentWorkerFieldValuesIter.remove();
                        result = true;
                        break;
                    } else if (!expectedListOfDocumentWorkerFieldValuesIter.hasNext()) {
                        logger.warn("Expected DocumentWorkerFieldValue (data: " + expectedDocumentWorkerFieldValue.data
                                + " encoding: " + expectedDocumentWorkerFieldValue.encoding
                                + ") mismatched actual DocumentWorkerFieldValue (data: "
                                + actualDocumentWorkerFieldValue.data + " encoding: "
                                + actualDocumentWorkerFieldValue.encoding + ")");
                        return false;
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
        return result;
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

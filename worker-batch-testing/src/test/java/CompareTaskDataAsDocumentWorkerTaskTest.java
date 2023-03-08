/*
 * Copyright 2016-2023 Open Text.
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
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.worker.batch.BatchResultValidationProcessor;
import static org.testng.Assert.fail;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.*;

public class CompareTaskDataAsDocumentWorkerTaskTest {

    DataStore mockDataStore;
    InputStream mockBinaryStream;
    InputStream mockBinaryStream2;
    InputStream mockBinaryStream3;
    InputStream mockBinaryStream4;

    final String binaryStorageReference = "test-datastore-partial-reference/a5475686-70f2-497d-8356-a9c8d473b599";
    final String mockBinary = "mockBinary";

    @BeforeTest
    public void setupMockDataStore() throws DataStoreException, IOException {
        mockDataStore = mock(DataStore.class);
    }

    /**
     * This test verifies that list of DocumentWorkerTask taskData fields and customData are validated correctly when
     * they are held unordered
     */
    @Test
    public void compareUnorderedListsOfExpectedAndActualTaskMessages() throws DataStoreException {
        mockBinaryStream = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream2 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream3 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream4 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        when(mockDataStore.retrieve(binaryStorageReference)).thenReturn(mockBinaryStream,
                mockBinaryStream2, mockBinaryStream3, mockBinaryStream4);

        String expectedAndActualTaskDataString1 = "{\"fields\":{" +
                "\"aNewField\":[{\"data\":\"aNewFieldValue\"},{\"data\":\"aNewFieldValue2\"}]," +
                "\"CUSTOM_CONTENT\":[{" +
                "\"data\":\"" + binaryStorageReference + "\"," +
                "\"encoding\":\"storage_ref\"}]," +
                "\"CUSTOM_FILE_NAME\":[{" +
                "\"data\":\"FileToTestWithinSubDir.txt\"}]}," +
                "\"customData\":{\"aCustomDataField\":\"aCustomDataFieldValue\"}}";
        String expectedAndActualTaskDataString2 = "{\"fields\":{" +
                "\"aNewField\":[{\"data\":\"aNewFieldValue\"},{\"data\":\"aNewFieldValue2\"}]," +
                "\"CUSTOM_CONTENT\":[{" +
                "\"data\":\"" + binaryStorageReference + "\"," +
                "\"encoding\":\"storage_ref\"}]," +
                "\"CUSTOM_FILE_NAME\":[{" +
                "\"data\":\"AnotherFileToTestWithinSubDir.txt\"}]}," +
                "\"customData\":{\"aCustomDataField\":\"aCustomDataFieldValue\"}}";

        byte[] expectedAndActualTaskData1AsByteArray = expectedAndActualTaskDataString1.getBytes();
        byte[] expectedAndActualTaskData2AsByteArray = expectedAndActualTaskDataString2.getBytes();

        List<TaskMessage> expectedSubTasks = new ArrayList<>();
        TaskMessage expectedTaskMessage1 = new TaskMessage("TaskMessage1", "DocumentWorker", 1,
                expectedAndActualTaskData1AsByteArray, TaskStatus.RESULT_SUCCESS, new HashMap<>());
        TaskMessage expectedTaskMessage2 = new TaskMessage("TaskMessage2", "DocumentWorker", 1,
                expectedAndActualTaskData2AsByteArray, TaskStatus.RESULT_SUCCESS, new HashMap<>());
        expectedSubTasks.add(expectedTaskMessage1);
        expectedSubTasks.add(expectedTaskMessage2);

        List<TaskMessage> actualSubTasks = new ArrayList<>();
        TaskMessage actualTaskMessage1 = new TaskMessage("TaskMessage1", "DocumentWorker", 1,
                expectedAndActualTaskData1AsByteArray, TaskStatus.RESULT_SUCCESS, new HashMap<>());
        TaskMessage actualTaskMessage2 = new TaskMessage("TaskMessage2", "DocumentWorker", 1,
                expectedAndActualTaskData2AsByteArray, TaskStatus.RESULT_SUCCESS, new HashMap<>());
        actualSubTasks.add(actualTaskMessage2);
        actualSubTasks.add(actualTaskMessage1);

        BatchResultValidationProcessor batchResultValidationProcessor = new BatchResultValidationProcessor(
                new JsonCodec(), mockDataStore);

        try {
            assertTrue(batchResultValidationProcessor.compareSubTasks(expectedSubTasks, actualSubTasks),
                    "Unordered SubTasks should match");
        } catch (CodecException e) {
            e.printStackTrace();
            fail("Test failed to deserialise with the Codec");
        }
    }

    /**
     * This test verifies that list of DocumentWorkerTask taskData fields and customData are validated correctly when
     * they are held unordered and one of the actual task messages does not match any of the expected task messages.
     */
    @Test
    public void compareExpectedAndActualTaskMessagesMismatchedVariables() throws DataStoreException {
        mockBinaryStream = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream2 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream3 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream4 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        when(mockDataStore.retrieve(binaryStorageReference)).thenReturn(mockBinaryStream,
                mockBinaryStream2, mockBinaryStream3, mockBinaryStream4);

        String expectedAndActualTaskDataString1 = "{\"fields\":{" +
                "\"aNewField\":[{\"data\":\"aNewFieldValue\"},{\"data\":\"aNewFieldValue2\"}]," +
                "\"CUSTOM_CONTENT\":[{" +
                "\"data\":\"" + binaryStorageReference + "\"," +
                "\"encoding\":\"storage_ref\"}]," +
                "\"CUSTOM_FILE_NAME\":[{" +
                "\"data\":\"FileToTestWithinSubDir.txt\"}]}," +
                "\"customData\":{\"aCustomDataField\":\"aCustomDataFieldValue\"}}";
        String expectedAndActualTaskDataString2 = "{\"fields\":{" +
                "\"aNewField\":[{\"data\":\"aNewFieldValue\"},{\"data\":\"aNewFieldValue2\"}]," +
                "\"CUSTOM_CONTENT\":[{" +
                "\"data\":\"" + binaryStorageReference + "\"," +
                "\"encoding\":\"storage_ref\"}]," +
                "\"CUSTOM_FILE_NAME\":[{" +
                "\"data\":\"AnotherFileToTestWithinSubDir.txt\"}]}," +
                "\"customData\":{\"aCustomDataField\":\"aCustomDataFieldValue\"}}";

        byte[] expectedAndActualTaskData1AsByteArray = expectedAndActualTaskDataString1.getBytes();
        byte[] expectedAndActualTaskData2AsByteArray = expectedAndActualTaskDataString2.getBytes();

        List<TaskMessage> expectedSubTasks = new ArrayList<>();
        TaskMessage expectedTaskMessage1 = new TaskMessage("TaskMessage1", "DocumentWorker", 1,
                expectedAndActualTaskData1AsByteArray, TaskStatus.RESULT_SUCCESS, new HashMap<>());
        TaskMessage expectedTaskMessage2 = new TaskMessage("TaskMessage2", "DocumentWorker", 1,
                expectedAndActualTaskData2AsByteArray, TaskStatus.RESULT_SUCCESS, new HashMap<>());
        expectedSubTasks.add(expectedTaskMessage1);
        expectedSubTasks.add(expectedTaskMessage2);

        List<TaskMessage> actualSubTasks = new ArrayList<>();
        TaskMessage actualTaskMessage1 = new TaskMessage("TaskMessage1", "DocumentWorker", 1,
                expectedAndActualTaskData1AsByteArray, TaskStatus.RESULT_SUCCESS, new HashMap<>());
        TaskMessage actualTaskMessage2 = new TaskMessage("TaskMessage2", "DocumentWorker2", 2,
                expectedAndActualTaskData2AsByteArray, TaskStatus.RESULT_SUCCESS, new HashMap<>());
        actualSubTasks.add(actualTaskMessage2);
        actualSubTasks.add(actualTaskMessage1);

        BatchResultValidationProcessor batchResultValidationProcessor = new BatchResultValidationProcessor(
                new JsonCodec(), mockDataStore);

        try {
            assertFalse(batchResultValidationProcessor.compareSubTasks(expectedSubTasks, actualSubTasks),
                    "Expected and Actual Task Message Variables should mismatch.");
        } catch (CodecException e) {
            e.printStackTrace();
            fail("Test failed to deserialise with the Codec");
        }
    }

    /**
     * This test verifies that DocumentWorkerTask taskData fields and customData are validated correctly.
     * Fields that use encoding of storage_ref need to ignore the UUID part of the storage reference during validation.
     */
    @Test
    public void compareByteArrayTaskDataThatTransformsToDocumentWorkerTask() throws DataStoreException {
        mockBinaryStream = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream2 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream3 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream4 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        when(mockDataStore.retrieve(binaryStorageReference)).thenReturn(mockBinaryStream,
                mockBinaryStream2, mockBinaryStream3, mockBinaryStream4);

        String expectedTaskDataString = "{\"fields\":{" +
                "\"aNewField\":[{\"data\":\"aNewFieldValue\"},{\"data\":\"aNewFieldValue2\"}]," +
                "\"CUSTOM_CONTENT\":[{" +
                    "\"data\":\"" + binaryStorageReference + "\"," +
                    "\"encoding\":\"storage_ref\"}]," +
                "\"CUSTOM_FILE_NAME\":[{" +
                    "\"data\":\"AnotherFileToTestWithinSubDir.txt\"}]}," +
                "\"customData\":{\"aCustomDataField\":\"aCustomDataFieldValue\"}}";
        String actualTaskDataString = "{\"fields\":{" +
                "\"aNewField\":[{\"data\":\"aNewFieldValue2\"},{\"data\":\"aNewFieldValue\"}]," +
                "\"CUSTOM_CONTENT\":[{" +
                    "\"data\":\"" + binaryStorageReference + "\"," +
                    "\"encoding\":\"storage_ref\"}]," +
                "\"CUSTOM_FILE_NAME\":[{" +
                    "\"data\":\"AnotherFileToTestWithinSubDir.txt\"}]}," +
                "\"customData\":{\"aCustomDataField\":\"aCustomDataFieldValue\"}}";

        byte[] expectedTaskDataAsByteArray = expectedTaskDataString.getBytes();
        byte[] actualTaskDataAsByteArray = actualTaskDataString.getBytes();

        BatchResultValidationProcessor batchResultValidationProcessor = new BatchResultValidationProcessor(
                new JsonCodec(), mockDataStore);

        try {
            assertTrue(batchResultValidationProcessor.compareDocumentWorkerTaskData(expectedTaskDataAsByteArray,
                    actualTaskDataAsByteArray), "Comparison of TaskData should fail as the actual is missing a field.");
        } catch (CodecException e) {
            e.printStackTrace();
            fail("Test failed to deserialise with the Codec");
        }
    }

    /**
     * This test verifies that the batchResultValidationProcessor.compareDocumentWorkerTaskData() throws an error when
     * there is a field within the expected taskData that is missing from the actual TaskData.
     */
    @Test
    public void compareByteArrayTaskDataThatTransformsToDocumentWorkerTaskWithActualTaskDataMissingExpectedField()
            throws DataStoreException {
        mockBinaryStream = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream2 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream3 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream4 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        when(mockDataStore.retrieve(binaryStorageReference)).thenReturn(mockBinaryStream,
                mockBinaryStream2, mockBinaryStream3, mockBinaryStream4);
        String expectedTaskDataString = "{\"fields\":{" +
                "\"anExpectedFieldMissingFromTheActual\":[{\"data\":\"anExpectedFieldMissingFromTheActualValue\"}]," +
                "\"aNewField\":[{\"data\":\"aNewFieldValue\"}]," +
                "\"CUSTOM_CONTENT\":[{" +
                "\"data\":\"" + binaryStorageReference + "\"," +
                "\"encoding\":\"storage_ref\"}]," +
                "\"CUSTOM_FILE_NAME\":[{" +
                "\"data\":\"AnotherFileToTestWithinSubDir.txt\"}]}," +
                "\"customData\":{\"aCustomDataField\":\"aCustomDataFieldValue\"}}";
        String actualTaskDataString = "{\"fields\":{" +
                "\"aNewField\":[{\"data\":\"aNewFieldValue\"}]," +
                "\"CUSTOM_CONTENT\":[{" +
                "\"data\":\"" + binaryStorageReference + "\"," +
                "\"encoding\":\"storage_ref\"}]," +
                "\"CUSTOM_FILE_NAME\":[{" +
                "\"data\":\"AnotherFileToTestWithinSubDir.txt\"}]}," +
                "\"customData\":{\"aCustomDataField\":\"aCustomDataFieldValue\"}}";

        byte[] expectedTaskDataAsByteArray = expectedTaskDataString.getBytes();
        byte[] actualTaskDataAsByteArray = actualTaskDataString.getBytes();

        BatchResultValidationProcessor batchResultValidationProcessor = new BatchResultValidationProcessor(
                new JsonCodec(), mockDataStore);
        try {
            boolean result = batchResultValidationProcessor.compareDocumentWorkerTaskData(expectedTaskDataAsByteArray,
                    actualTaskDataAsByteArray);
            assertFalse(result, "Comparison of TaskData should fail as the actual is missing a field.");
        } catch (CodecException e) {
            e.printStackTrace();
            fail("Test failed to deserialise with the Codec");
        }
    }

    /**
     * This test verifies that the batchResultValidationProcessor.compareDocumentWorkerTaskData() throws an error when
     * there is a field within the actual taskData that is missing from the expected TaskData.
     */
    @Test
    public void compareByteArrayTaskDataThatTransformsToDocumentWorkerTaskWithExpectedTaskDataMissingActualField()
            throws DataStoreException {
        mockBinaryStream = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream2 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream3 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream4 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        when(mockDataStore.retrieve(binaryStorageReference)).thenReturn(mockBinaryStream,
                mockBinaryStream2, mockBinaryStream3, mockBinaryStream4);
        String expectedTaskDataString = "{\"fields\":{" +
                "\"aNewField\":[{\"data\":\"aNewFieldValue\"}]," +
                "\"CUSTOM_CONTENT\":[{" +
                "\"data\":\"" + binaryStorageReference + "\"," +
                "\"encoding\":\"storage_ref\"}]," +
                "\"CUSTOM_FILE_NAME\":[{" +
                "\"data\":\"AnotherFileToTestWithinSubDir.txt\"}]}," +
                "\"customData\":{\"aCustomDataField\":\"aCustomDataFieldValue\"}}";
        String actualTaskDataString = "{\"fields\":{" +
                "\"anActualFieldMissingFromTheExpected\":[{\"data\":\"anActualFieldMissingFromTheExpectedValue\"}]," +
                "\"aNewField\":[{\"data\":\"aNewFieldValue\"}]," +
                "\"CUSTOM_CONTENT\":[{" +
                "\"data\":\"" + binaryStorageReference + "\"," +
                "\"encoding\":\"storage_ref\"}]," +
                "\"CUSTOM_FILE_NAME\":[{" +
                "\"data\":\"AnotherFileToTestWithinSubDir.txt\"}]}," +
                "\"customData\":{\"aCustomDataField\":\"aCustomDataFieldValue\"}}";

        byte[] expectedTaskDataAsByteArray = expectedTaskDataString.getBytes();
        byte[] actualTaskDataAsByteArray = actualTaskDataString.getBytes();

        BatchResultValidationProcessor batchResultValidationProcessor = new BatchResultValidationProcessor(
                new JsonCodec(), mockDataStore);

        try {
            boolean result = batchResultValidationProcessor.compareDocumentWorkerTaskData(expectedTaskDataAsByteArray,
                    actualTaskDataAsByteArray);
            assertFalse(result, "Comparison of TaskData should fail as the expected is missing a field.");
        } catch (CodecException e) {
            e.printStackTrace();
            fail("Test failed to deserialise with the Codec");
        }
    }

    /**
     * This test verifies that the batchResultValidationProcessor.compareDocumentWorkerTaskData() throws an error when
     * there is a customData within the expected taskData that is missing from the actual TaskData customData.
     */
    @Test
    public void compareByteArrayTaskDataThatTransformsToDocumentWorkerTaskWithActualTaskDataMissingCustomDataField()
            throws DataStoreException {
        mockBinaryStream = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream2 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream3 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream4 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        when(mockDataStore.retrieve(binaryStorageReference)).thenReturn(mockBinaryStream,
                mockBinaryStream2, mockBinaryStream3, mockBinaryStream4);
        String expectedTaskDataString = "{\"fields\":{" +
                "\"aNewField\":[{\"data\":\"aNewFieldValue\"}]," +
                "\"CUSTOM_CONTENT\":[{" +
                "\"data\":\"" + binaryStorageReference + "\"," +
                "\"encoding\":\"storage_ref\"}]," +
                "\"CUSTOM_FILE_NAME\":[{" +
                "\"data\":\"AnotherFileToTestWithinSubDir.txt\"}]}," +
                "\"customData\":{\"aCustomDataField\":\"aCustomDataFieldValue\"," +
                "\"anExpectedCustomDataFieldMissingFromTheActual\":\"anExpectedCustomDataFieldValue\"}}";
        String actualTaskDataString = "{\"fields\":{" +
                "\"aNewField\":[{\"data\":\"aNewFieldValue\"}]," +
                "\"CUSTOM_CONTENT\":[{" +
                "\"data\":\"" + binaryStorageReference + "\"," +
                "\"encoding\":\"storage_ref\"}]," +
                "\"CUSTOM_FILE_NAME\":[{" +
                "\"data\":\"AnotherFileToTestWithinSubDir.txt\"}]}," +
                "\"customData\":{\"aCustomDataField\":\"aCustomDataFieldValue\"}}";

        byte[] expectedTaskDataAsByteArray = expectedTaskDataString.getBytes();
        byte[] actualTaskDataAsByteArray = actualTaskDataString.getBytes();

        BatchResultValidationProcessor batchResultValidationProcessor = new BatchResultValidationProcessor(
                new JsonCodec(), mockDataStore);
        try {
            boolean result = batchResultValidationProcessor.compareDocumentWorkerTaskData(expectedTaskDataAsByteArray,
                    actualTaskDataAsByteArray);
            assertFalse(result, "Comparison of TaskData should fail as the actual is missing a customData " +
                    "Field");
        } catch (CodecException e) {
            e.printStackTrace();
            fail("Test failed to deserialise with the Codec");
        }
    }

    /**
     * This test verifies that the batchResultValidationProcessor.compareDocumentWorkerTaskData() throws an error when
     * there is a customData within the actual taskData that is missing from the expected TaskData customData.
     */
    @Test
    public void compareByteArrayTaskDataThatTransformsToDocumentWorkerTaskWithExpectedTaskDataMissingCustomDataField()
            throws DataStoreException {
        mockBinaryStream = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream2 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream3 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        mockBinaryStream4 = new ByteArrayInputStream(mockBinary.getBytes(StandardCharsets.UTF_8));
        when(mockDataStore.retrieve(binaryStorageReference)).thenReturn(mockBinaryStream,
                mockBinaryStream2, mockBinaryStream3, mockBinaryStream4);

        String expectedTaskDataString = "{\"fields\":{" +
                "\"aNewField\":[{\"data\":\"aNewFieldValue\"}]," +
                "\"CUSTOM_CONTENT\":[{" +
                "\"data\":\"" + binaryStorageReference + "\"," +
                "\"encoding\":\"storage_ref\"}]," +
                "\"CUSTOM_FILE_NAME\":[{" +
                "\"data\":\"AnotherFileToTestWithinSubDir.txt\"}]}," +
                "\"customData\":{\"aCustomDataField\":\"aCustomDataFieldValue\"}}";
        String actualTaskDataString = "{\"fields\":{" +
                "\"aNewField\":[{\"data\":\"aNewFieldValue\"}]," +
                "\"CUSTOM_CONTENT\":[{" +
                "\"data\":\"" + binaryStorageReference + "\"," +
                "\"encoding\":\"storage_ref\"}]," +
                "\"CUSTOM_FILE_NAME\":[{" +
                "\"data\":\"AnotherFileToTestWithinSubDir.txt\"}]}," +
                "\"customData\":{\"aCustomDataField\":\"aCustomDataFieldValue\"," +
                "\"anExpectedCustomDataFieldMissingFromTheActual\":\"anExpectedCustomDataFieldValue\"}}";

        byte[] expectedTaskDataAsByteArray = expectedTaskDataString.getBytes();
        byte[] actualTaskDataAsByteArray = actualTaskDataString.getBytes();

        BatchResultValidationProcessor batchResultValidationProcessor = new BatchResultValidationProcessor(
                new JsonCodec(), mockDataStore);

        try {
            boolean result = batchResultValidationProcessor.compareDocumentWorkerTaskData(expectedTaskDataAsByteArray,
                    actualTaskDataAsByteArray);
            assertFalse(result, "Comparison of TaskData should fail as the expected is missing a customData " +
                    "Field");
        } catch (CodecException e) {
            e.printStackTrace();
            fail("Test failed to deserialise with the Codec");
        }
    }
}

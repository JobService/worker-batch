import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.worker.batch.BatchResultValidationProcessor;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
            batchResultValidationProcessor.compareDocumentWorkerTaskData(expectedTaskDataAsByteArray,
                    actualTaskDataAsByteArray);
        } catch (CodecException e) {
            e.printStackTrace();
            Assert.fail("Test failed to deserialise with the Codec");
        }
    }

    /**
     * This test verifies that the batchResultValidationProcessor.compareDocumentWorkerTaskData() throws an error when
     * there is a field within the expected taskData that is missing from the actual TaskData.
     */
    @Test(expectedExceptions = AssertionError.class)
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
            batchResultValidationProcessor.compareDocumentWorkerTaskData(expectedTaskDataAsByteArray,
                    actualTaskDataAsByteArray);
        } catch (CodecException e) {
            e.printStackTrace();
            Assert.fail("Test failed to deserialise with the Codec");
        }
    }

    /**
     * This test verifies that the batchResultValidationProcessor.compareDocumentWorkerTaskData() throws an error when
     * there is a field within the actual taskData that is missing from the expected TaskData.
     */
    @Test(expectedExceptions = AssertionError.class)
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
            batchResultValidationProcessor.compareDocumentWorkerTaskData(expectedTaskDataAsByteArray,
                    actualTaskDataAsByteArray);
        } catch (CodecException e) {
            e.printStackTrace();
            Assert.fail("Test failed to deserialise with the Codec");
        }
    }

    /**
     * This test verifies that the batchResultValidationProcessor.compareDocumentWorkerTaskData() throws an error when
     * there is a customData within the expected taskData that is missing from the actual TaskData customData.
     */
    @Test(expectedExceptions = AssertionError.class)
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
            batchResultValidationProcessor.compareDocumentWorkerTaskData(expectedTaskDataAsByteArray,
                    actualTaskDataAsByteArray);
        } catch (CodecException e) {
            e.printStackTrace();
            Assert.fail("Test failed to deserialise with the Codec");
        }
    }

    /**
     * This test verifies that the batchResultValidationProcessor.compareDocumentWorkerTaskData() throws an error when
     * there is a customData within the actual taskData that is missing from the expected TaskData customData.
     */
    @Test(expectedExceptions = AssertionError.class)
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
            batchResultValidationProcessor.compareDocumentWorkerTaskData(expectedTaskDataAsByteArray,
                    actualTaskDataAsByteArray);
        } catch (CodecException e) {
            e.printStackTrace();
            Assert.fail("Test failed to deserialise with the Codec");
        }
    }
}

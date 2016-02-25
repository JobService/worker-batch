package com.hpe.caf.worker.batch;

import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;
import com.hpe.caf.worker.testing.preparation.PreparationItemProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Created by gibsodom on 25/02/2016.
 */
public class BatchResultPreparationProvider extends PreparationItemProvider<BatchWorkerTask, BatchWorkerResult, BatchTestInput, BatchTestExpectation> {

    TestConfiguration configuration;

    public BatchResultPreparationProvider(TestConfiguration<BatchWorkerTask, BatchWorkerResult, BatchTestInput, BatchTestExpectation> configuration) {
        super(configuration);
        this.configuration = configuration;
    }

    @Override
    protected TestItem createTestItem(Path inputFile, Path expectedFile) throws Exception {
        TestItem<BatchTestInput,BatchTestExpectation> testItem = super.createTestItem(inputFile,expectedFile);
        BatchWorkerTask task = getTaskTemplate();
        if(task == null){
            task = new BatchWorkerTask();
            task.setTargetPipe(UUID.randomUUID().toString());
            task.setBatchDefinition(new String(Files.readAllBytes(inputFile)));
            task.setBatchType("com.hpe.caf.worker.batch.BatchPluginTestImpl");
        } else {
            task.setBatchDefinition(new String(Files.readAllBytes(inputFile)));
        }
        testItem.getInputData().setTask(task);
        return testItem;
    }

}

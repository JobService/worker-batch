package com.hpe.caf.worker.batch;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;
import com.hpe.caf.worker.testing.preparation.PreparationResultProcessor;

import java.util.List;

/**
 * Created by gibsodom on 25/02/2016.
 */
public class BatchSaveResultProcessor extends PreparationResultProcessor<BatchWorkerTask,BatchWorkerResult,BatchTestInput,BatchTestExpectation> {

    protected BatchSaveResultProcessor(TestConfiguration<BatchWorkerTask, BatchWorkerResult, BatchTestInput, BatchTestExpectation> configuration, Codec codec) {
        super(configuration, codec);
    }

    @Override
    protected byte[] getOutputContent(BatchWorkerResult batchWorkerResult, TaskMessage message, TestItem<BatchTestInput, BatchTestExpectation> testItem) throws Exception {
        List<TaskMessage> subTasks = BatchTargetQueueRetriever.getInstance().retrieveMessages(testItem.getInputData().getTask().getTargetPipe());
        testItem.getExpectedOutputData().setSubTasks(subTasks);
        return super.getOutputContent(batchWorkerResult, message, testItem);
    }
}

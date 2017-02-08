package com.hpe.caf.worker.batch;

import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.testing.FileInputWorkerTaskFactory;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;

import java.util.Date;

public class BatchTaskFactory extends FileInputWorkerTaskFactory<BatchWorkerTask, BatchTestInput, BatchTestExpectation> {

    public BatchTaskFactory(TestConfiguration configuration) throws Exception {
        super(configuration);
    }
    @Override
    protected BatchWorkerTask createTask(TestItem<BatchTestInput, BatchTestExpectation> testItem, ReferencedData sourceData) {
        BatchWorkerTask task = testItem.getInputData().getTask();
        return task;
    }

    @Override
    public String getWorkerName() {
        return BatchWorkerConstants.WORKER_NAME;
    }

    @Override
    public int getApiVersion() {
        return BatchWorkerConstants.WORKER_API_VERSION;
    }

    @Override
    public TrackingInfo createTrackingInfo(TestItem<BatchTestInput, BatchTestExpectation> testItem) {
        return new TrackingInfo(testItem.getTag(), new Date(), null, null, null);
    }
}

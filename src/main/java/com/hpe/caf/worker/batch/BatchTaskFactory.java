package com.hpe.caf.worker.batch;

import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.testing.FileInputWorkerTaskFactory;
import com.hpe.caf.worker.testing.TestItem;
import com.hpe.caf.worker.testing.WorkerServices;

public class BatchTaskFactory extends FileInputWorkerTaskFactory<BatchWorkerTask, BatchTestInput, BatchTestExpectation> {

    public BatchTaskFactory(WorkerServices workerServices, String containerId, String testFilesFolder) {
        super(workerServices, containerId, testFilesFolder);
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
}

package com.hpe.caf.worker.batch;

import com.hpe.caf.worker.batch.BatchWorkerTask;
import com.hpe.caf.worker.testing.FileTestInputData;

/**
 * Created by gibsodom on 25/02/2016.
 */
public class BatchTestInput extends FileTestInputData {
    private BatchWorkerTask task;

    public BatchWorkerTask getTask() {
        return task;
    }

    public void setTask(BatchWorkerTask task) {
        this.task = task;
    }
}

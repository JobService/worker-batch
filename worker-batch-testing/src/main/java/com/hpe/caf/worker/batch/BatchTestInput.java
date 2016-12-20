package com.hpe.caf.worker.batch;

import com.hpe.caf.worker.testing.FileTestInputData;

public class BatchTestInput extends FileTestInputData {
    private BatchWorkerTask task;

    public BatchWorkerTask getTask() {
        return task;
    }

    public void setTask(BatchWorkerTask task) {
        this.task = task;
    }
}

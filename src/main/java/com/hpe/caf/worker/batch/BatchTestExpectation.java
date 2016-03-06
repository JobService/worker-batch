package com.hpe.caf.worker.batch;

import com.hpe.caf.api.worker.TaskMessage;

import java.util.List;

public class BatchTestExpectation {
    private List<TaskMessage> subTasks;

    public List<TaskMessage> getSubTasks() {
        return subTasks;
    }

    public void setSubTasks(List<TaskMessage> subTasks) {
        this.subTasks = subTasks;
    }
}

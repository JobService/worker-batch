package com.hpe.caf.worker.batch;

import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.worker.testing.ResultProcessor;
import com.hpe.caf.worker.testing.TestItem;
import org.junit.Assert;

import java.util.Arrays;
import java.util.List;

public class BatchResultValidationProcessor implements ResultProcessor {

    @Override
    public boolean process(TestItem testItem, TaskMessage resultMessage) throws Exception {
        if (resultMessage.getTaskStatus() == TaskStatus.RESULT_SUCCESS) {
            BatchTestInput testInput = (BatchTestInput) testItem.getInputData();
            BatchTestExpectation testOuput = (BatchTestExpectation) testItem.getExpectedOutputData();
            BatchWorkerTask inputTask = testInput.getTask();
            List<TaskMessage> subTasks = BatchTargetQueueRetriever.getInstance().retrieveMessages(inputTask.targetPipe);
            if (subTasks.size() != testOuput.getSubTasks().size()) {
                return false;
            }
            compareSubTasks(testOuput.getSubTasks(),subTasks);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getInputIdentifier(TaskMessage message) {
        return message.getTaskId();
    }

    private void compareSubTasks(List<TaskMessage>expectedSubTasks, List<TaskMessage> actualSubTasks){
        for (int i = 0; i < actualSubTasks.size(); i++) {
            Assert.assertEquals("Subtasks classifier should match", expectedSubTasks.get(i).getTaskClassifier(), actualSubTasks.get(i).getTaskClassifier());
            Assert.assertEquals("Subtasks task API version should match", expectedSubTasks.get(i).getTaskApiVersion(), actualSubTasks.get(i).getTaskApiVersion());
            Assert.assertEquals("Subtasks task status should match", expectedSubTasks.get(i).getTaskStatus(), actualSubTasks.get(i).getTaskStatus());
            Assert.assertTrue("Task data should match", Arrays.equals(expectedSubTasks.get(i).getTaskData(),actualSubTasks.get(i).getTaskData()));
        }
    }
}

import com.hpe.caf.api.worker.TaskMessage;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gibsodom on 04/04/2016.
 */
public class PublishAnswer implements Answer {

    public List<TaskMessage> messageList = new ArrayList<>();

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
        messageList.add((TaskMessage) invocation.getArguments()[1]);
        return invocation.getArguments()[1];
    }
}

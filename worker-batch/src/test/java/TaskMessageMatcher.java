import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.DecodeMethod;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.codec.JsonCodec;
import org.mockito.ArgumentMatcher;

public class TaskMessageMatcher extends ArgumentMatcher<byte[]> {

    private JsonCodec codec = new JsonCodec();
    private TaskMessage expected;

    public TaskMessageMatcher(byte[] expected) throws CodecException {

        this.expected = codec.deserialise(expected, TaskMessage.class, DecodeMethod.LENIENT);
    }

    @Override
    public boolean matches(Object argument) {
        byte[] argBytes = (byte[]) argument;
        TaskMessage message = null;
        try {
            message = codec.deserialise(argBytes, TaskMessage.class);
        } catch (CodecException e) {
            e.printStackTrace();
        }
        if (message.getTaskApiVersion() != expected.getTaskApiVersion()) {
            return false;
        }
        if (!new String(message.getTaskData()).equalsIgnoreCase(new String(expected.getTaskData()))) {
            return false;
        }
        if (message.getContext().size() != expected.getContext().size()) {
            return false;
        }
        if (message.getTaskStatus() != expected.getTaskStatus()) {
            return false;
        }
        if(!message.getTo().equalsIgnoreCase(expected.getTo())){
            return false;
        }
        if ((message.getTracking() == null && expected.getTracking() != null) || (message.getTracking() != null && expected.getTracking() == null)) {
            return false;
        }
        return true;
    }
}

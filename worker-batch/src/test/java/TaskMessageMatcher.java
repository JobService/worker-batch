/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

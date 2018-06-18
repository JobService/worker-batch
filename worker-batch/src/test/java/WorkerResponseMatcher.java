/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.codec.JsonCodec;
import org.mockito.ArgumentMatcher;

public class WorkerResponseMatcher extends ArgumentMatcher<byte[]>
{

    private JsonCodec codec = new JsonCodec();
    private WorkerResponse expected;

    public WorkerResponseMatcher(WorkerResponse expected) throws CodecException
    {

        this.expected = expected;
    }

    @Override
    public boolean matches(Object argument)
    {
        final byte[] argBytes = (byte[]) argument;
        WorkerResponse message = null;
        try {
            message = codec.deserialise(argBytes, WorkerResponse.class);
        } catch (final CodecException e) {
            e.printStackTrace();
        }
        if (message.getApiVersion() != expected.getApiVersion()) {
            return false;
        }
        if (!new String(message.getData()).equalsIgnoreCase(new String(expected.getData()))) {
            return false;
        }
        if (message.getContext().length != expected.getContext().length) {
            return false;
        }
        if (message.getTaskStatus() != expected.getTaskStatus()) {
            return false;
        }
        if (!message.getTrackTo().equalsIgnoreCase(expected.getTrackTo())) {
            return false;
        }
        return true;
    }
}

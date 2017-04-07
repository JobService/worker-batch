/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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

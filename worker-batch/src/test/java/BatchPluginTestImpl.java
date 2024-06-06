/*
 * Copyright 2016-2024 Open Text.
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
import com.hpe.caf.worker.batch.BatchWorkerPlugin;
import com.hpe.caf.worker.batch.BatchWorkerServices;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by gibsodom on 26/02/2016.
 * A very simple implementation of the batch worker plugin. Will split a comma separated list into subtasks.
 */
public class BatchPluginTestImpl implements BatchWorkerPlugin {
    @Override
    public void processBatch(BatchWorkerServices batchWorkerServices, String batchDefinition, String taskMessageType,
                             Map<String, String> taskMessageParams) {
        List<String> items = Arrays.asList(batchDefinition.split("\\s*,\\s*"));
        for(String subItem : items) {
            if(!subItem.equals("")){
                batchWorkerServices.registerItemSubtask("Test task", 1, subItem);
            }
        }
    }
}

/*
 * Copyright 2016-2023 Open Text.
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
package com.hpe.caf.worker.batch;

import java.util.Map;

public class BatchWorkerTask {

    /**
     * This is the definition of the batch.  For example, it might be a string
     * like "workbook == 5".  The definition string will be interpreted by the
     * type specified to the {@link #batchType} field.
     */
    public String batchDefinition;

    /**
     * This is the type that is used to interpret the batch definition string.
     * The Batch Worker will need to create an instance of this class to
     * interpret the definition string, so the specified class must be made
     * available on the Batch Worker's classpath.
     */
    public String batchType;

    /**
     * This is a factory type that is used to construct the TaskMessage for each
     * item of the batch.  Like the type specified in the {@link #batchType}
     * field, it must be available on the Batch Worker's classpath.
     * <p>
     * Obviously this type is highly tied to the {@link #targetPipe} field, in
     * that the messages produced by this object must be compatible with the
     * workers they are being sent to.
     */
    public String taskMessageType;

    /**
     * This is a set of named parameters to be passed to the specified
     * TaskMessage builder (i.e. the factory type specified by the
     * {@link #taskMessageType} parameter).  Their meaning is dependant on the
     * type specified.
     */
    public Map<String,String> taskMessageParams;

    /**
     * A message is constructed for each item of the batch.  This field
     * specifies the pipe (channel or queue) where these per-item messages are
     * to be forwarded to.
     */
    public String targetPipe;
}

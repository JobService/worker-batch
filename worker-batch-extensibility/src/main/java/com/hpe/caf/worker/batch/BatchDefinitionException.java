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
package com.hpe.caf.worker.batch;

/**
 * Exception thrown when a plugin is unable to process a batch definition
 */
public class BatchDefinitionException extends Exception {
    /**
     * Create a new BatchDefinitionException when the issue was another exception thrown.
     * @param message the message indicating the problem
     * @param cause the exception cause
     */
    public BatchDefinitionException(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Create a new BatchDefinitionException.
     * @param message the message indicating the problem
     */
    public BatchDefinitionException(final String message)
    {
        super(message);
    }
}

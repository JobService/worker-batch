/*
 * Copyright 2016-2020 Micro Focus or one of its affiliates.
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
 * Exception indicates that a transient failure has occurred; the operation might be able to succeed if it is
 * retried at a later time.
 */
public class BatchWorkerTransientException extends Exception {
    /**
     * Constructs a BatchWorkerTransientException with a given reason.
     *
     * @param message a description of the exception
     */
    public BatchWorkerTransientException(final String message)
    {
        super(message);
    }

    /**
     * Constructs a BatchWorkerTransientException with a given reason and cause.
     *
     * @param message a description of the exception
     * @param cause the underlying reason for this exception
     */
    public BatchWorkerTransientException(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Constructs a BatchWorkerTransientException with a given cause.
     *
     * @param cause the underlying reason for this exception
     */
    public BatchWorkerTransientException(final Throwable cause)
    {
        super(cause);
    }
}

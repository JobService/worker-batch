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

package com.hpe.caf.worker.batch;

/**
 * The Batch Worker Services is used to register processed batch definitions for further batch defining. The class is
 * also used to register a task message's parameters before serialisation.
 */
public interface BatchWorkerServices {

    /**
     * Registers a processed a sub batch for publishing to the Batch Worker's input queue
     *
     * @param batchDefinition   String containing the new batch definition
     */
    void registerBatchSubtask(String batchDefinition);

    /**
     * Registers a task message's parameters for serialisation and publishing to an input queue
     *
     * @param taskClassifier String containing the name of the Classifier the task data is intended for
     * @param taskApiVersion Integer containing the version number of the task api
     * @param taskData       Object containing the constructed task data for task message type
     */
    void registerItemSubtask(String taskClassifier, int taskApiVersion, Object taskData);

    /**
     * Returns the specified service, or {@code null} if the service has not been registered.
     *
     * @param <S> the type of the service to be returned
     * @param service the interface or abstract class representing the service
     * @return the service provider
     */
    <S> S getService(Class<S> service);
}

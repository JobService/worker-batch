# Batch Worker Container

This is a docker container for the Batch Worker. It consists of the Batch Worker which can be ran by passing in the required configuration files to the container. It uses the 'java:8' base image.

## Configuration

### Configuration Files
The worker requires configuration files to be passed through for;

* BatchWorkerConfiguration
* RabbitWorkerQueueConfiguration
* StorageServiceDataStoreConfiguration

### Environment Variables
##### CAF\_CONFIG\_PATH
The location of the configuration files to be used by the worker. Common to all workers.

##### DROPWIZARD\_CONFIG\_PATH
The full path of the dropwizard configuration file that the worker should use. This can be used to control various dropwizard options such as logging output level. This is optional and the worker shall default to the configuration file included in the image if this is not provided.
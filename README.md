## worker-batch

This is the aggregated repositories of the batch worker. This repository contains all projects relating to the batch worker and builds them all as part of its build process.

The projects contained within this repository are as follows:

### worker-batch-shared
- This is the shared library defining public classes that constitute the worker interface to be used by consumers of the Batch Worker.

More information on the functioning of the Batch Worker is available [here](https://github.hpe.com/caf/worker-batch/tree/develop/worker-batch-shared).

### worker-batch
- This project contains the implementation of the batch worker api.
- More information on this project can be found [here](https://github.hpe.com/caf/worker-batch/tree/develop/worker-batch)

### worker-batch-container

- This is a docker container for the Batch Worker. It consists of the Batch Worker which can be ran by passing in the required configuration files to the container. It uses the 'java:8' base image.

More information on the functioning of the Batch Worker is available [here](https://github.hpe.com/caf/worker-batch/tree/develop/worker-batch).

##### Container Configuration

Configuration details can be found [here](https://github.hpe.com/caf/chateau/tree/develop/services/batch-worker/configuration-files).

##### Feature Testing
The testing for the Batch Worker is defined [here](https://github.hpe.com/caf/worker-batch/tree/develop/testcases)

### worker-batch-testing
- This project contains service specific testing implementations to allow automated testing of the batch worker.

### worker-batch-extensibility
- The Batch Worker Plugin processes batch definitions by splitting them into further batch definitions and passes those split batch definitions to the Batch Worker Services for further processing. The Batch Worker Plugin also constructs a task data object of the given task message type for each task item which is passed to the Batch Worker Services before serialisation.

- The Batch Worker Services is used to register processed batch definitions for further batch defining. The class is also used to register a task message's parameters before serialisation.

### worker-batch-plugins
- Collects plugins and message builder implementations for use with worker-batch together into a single aggregated tar.gz.

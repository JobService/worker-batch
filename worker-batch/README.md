# Batch Worker

This worker is used to create and send multiple task messages to a specified worker. 

## Overview
When the Batch Worker receives a batch to process, it constructs an instance of the type that is specified by the `batchType` field, and then uses this to interpret the batch definition.  It splits the batch up into individual items, generates task messages for each of the items, and dispatches them to the pipe specified by the targetPipe field.

## Configuration 

This worker is configured by setting the following environment variables:

 - `CAF_BATCH_WORKER_CACHE_EXPIRE_TIME`  
    Default: `120`  
    Length of time, in seconds, the connection to a queue is maintained

 - `CAF_BATCH_WORKER_ERROR_QUEUE`  
    Default: `batch-worker-err`  
    Sets the queue name where errors are reported

 - `CAF_WORKER_THREADS`  
    Default: `1`  
    Specifies the number of threads used for parallel processing

# Batch Worker API

## Input Task Message Format

The `BatchWorkerTask` has the following properties: 

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
    <tr>
        <td>batchDefinition</td>
        <td>String</td>
        <td>This is the definition of the batch. For example, it might be a string like "workbook == 5". The definition string will be interpreted by the type specified to the 'batchType' field.</td>
    </tr>
    <tr>
        <td>batchType</td>
        <td>String</td>
        <td>This is the type that is used to interpret the batch definition string. The Batch Worker will need to create an instance of this class to interpret the definition string, so the specified class must be made available on the Batch Worker's classpath.</td>
    </tr>
    <tr>
        <td>taskMessageType</td>
        <td>String</td>
        <td>This is a factory type that is used to construct the TaskMessage for each item of the batch.  Like the type specified in the 'batchType' field, it must be available on the Batch Worker's classpath.</td>
    </tr>
    <tr>
        <td>taskMessageParams</td>
        <td>Map&lt;String,&nbsp;String&gt;</td>
        <td>This is a set of named parameters to be passed to the specified TaskMessage builder (i.e. the factory type specified by the 'taskMessageType' parameter). Their meaning is dependant on the type specified.</td>
    </tr>
    <tr>
        <td>targetPipe</td>
        <td>String</td>
        <td>This field specifies the pipe (channel or queue) where these per-item messages are to be forwarded to.</td>
    </tr>
</table>


## Output Task Message Format
The `BatchWorkerResponse` has the following properties:

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
    <tr>
        <td>batchTask</td>
        <td>String</td>
        <td>The Id of the now completed batch</td>
    </tr>
</table>

## Health Check

This worker has no dependency on external components and so does not supply any additional health checks beyond the one supplied by the CAF framework.

## Failure Modes

*   Configuration Errors: these will cause the worker to fail on start up. The cause should be output in the logs.

#### Maintainers

The following people are contacts for developing and maintaining this module:

*   Dominic Gibson (Belfast, UK, dominic.joh.gibson@hpe.com)

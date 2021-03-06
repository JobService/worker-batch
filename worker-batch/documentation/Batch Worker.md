# Batch Worker

This worker is used to create and send multiple task messages to a specified worker. 

## Overview
When the Batch Worker receives a batch to process, it constructs an instance of the type that is specified by the `batchType` field, and then uses this to interpret the batch definition.  It splits the batch up into individual items, generates task messages for each of the items, and dispatches them to the pipe specified by the targetPipe field.

## Configuration 

This worker uses the standard `caf-api` system of `ConfigurationSource` only. The configuration is contained in the `com.hpe.caf.worker.batch.BatchWorkerConfiguration`
class with the following options:

*   `workerVersion`: the version number of the worker
*   `outputQueue`: the name of the queue to put results on. _(Required)_
*   `threads`: the maximum number of threads the worker can run. _(Required)_
*   `cacheExpireTime`: the time in seconds for the worker to maintain a channel to a queue. _(Required)_
*   `returnValueBehaviour`: enumeration defining the worker response behaviour
    * `RETURN_ALL`: all results are returned to the output queue.
    * `RETURN_NONE`: no results except exceptions are returned to the output queue.
    * `RETURN_ONLY_IF_ZERO_SUBTASKS`: returns no results unless an exception or zero sub tasks are present for the batch definition.

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

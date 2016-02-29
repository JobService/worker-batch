# Batch Worker

This worker is used to create and send multiple task messages to a specified worker. 

## General Overview
When the Batch Worker receives a batch to process, it first constructs an instance of the type that is specified by the batchType field. This type should implement the BatchWorkerPlugin interface.
It then constructs its own internal BatchWorkerServicesImpl object - an object which implements the BatchWorkerServices interface.
It calls processBatch(), passing in the services object and the other parameters.
The implementation of the processBatch() method will interpret the batch definition string and split it up into either:

1. a set of batch definitions representing smaller batches
2. a set of items

If it determines to split the batch into a set of smaller batches, then it will make a series of calls to the registerBatchSubtask() method. The Batch Worker will construct messages which are to be directed back towards itself and dispatch them to the input pipe that the Batch Worker itself is listening on (not to the pipe specified by the targetPipe field).

If it instead determines to split the batch into a set of items, then it will first construct an instance of the type that is specified by the taskMessageFactory field, and then use it to generate task messages that are appropriate to be sent to the worker listening on the targetPipe. It will call the registerItemSubtask() method for each item, and the Batch Worker will dispatch the messages to the pipe specified by the targetPipe field.

## Configuration 

This worker uses the standard `caf-api` system of `ConfigurationSource` only. The configuration is contained in the `com.hpe.caf.worker.batch.BatchWorkerConfiguration`
class with the following options:

*   `outputQueue`: the name of the queue to put results on. _(Required)_
*   `threads`: the maximum number of threads the worker can run. _(Required)_
*   `cacheExpireTime`: the time in seconds for the worker to maintain a channel to a queue up to a maximum of 10 minutes (600 seconds). _(Required)_

# Batch Worker API

##Input Task Message Format

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
        <td>This is the type that is used to interpret the batch definition string. The Batch Worker will need to create an instance of this class to interpret the definition string, so the specified class must be made available on the Batch Worker's classpath. We should be able to make these types available as a mounted tar, in the same way that we make the Policy Handlers and Converters available to the Policy Worker.</td>
    </tr>
    <tr>
        <td>taskMessageType</td>
        <td>String</td>
        <td>This is a factory type that is used to construct the TaskMessage for each item of the batch. This string will be passed to the instance of the 'batchType' object and it will need to be able to create an instance of this class, so only types which have been deployed may be specified.</td>
    </tr>
    <tr>
        <td>taskMessageParams</td>
        <td>Map&lt;String,String&gt;</td>
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

##Health Check

This worker has no dependency on external components and so does not supply any additional health checks beyond the one supplied by the CAF framework.

##Failure Modes

*   Configuration Errors: these will cause the worker to fail on start up. The cause should be output in the logs.

####Maintainers

The following people are contacts for developing and maintaining this module:

*   Dominic Gibson (Belfast, UK, dominic.joh.gibson@hpe.com)

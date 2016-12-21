# Worker Batch Extensibility

The Batch Worker Plugin processes batch definitions by splitting them into further batch definitions and passes those split batch definitions to the Batch Worker Services for further processing. The Batch Worker Plugin also constructs a task data object of the given task message type for each task item which is passed to the Batch Worker Services before serialisation.

The Batch Worker Services is used to register processed batch definitions for further batch defining. The class is also used to register a task message's parameters before serialisation.
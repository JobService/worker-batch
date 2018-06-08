!not-ready-for-release!

#### Version Number
${version-number}

#### New Features

#### Known Issues

#### Breaking Changes
- [SCMOD-4072](https://jira.autonomy.com/browse/SCMOD-4072): Updated to use latest version of worker framework
   Latest version of worker framework now brings in dropwizard components at version 1.3.2
- [SCMOD-3525](https://jira.autonomy.com/browse/SCMOD-4072): Updated to send messages directly to output pipe
   Perviously the worker assessed the tracking info to determine where it should send a dispatching message, 
   i.e. if a message was to be tracked then it would send the message to the tracking pipe and allow it to forward the message. 
   The tracking of a message is now dispatched by the framework so the worker will now always forward its messages to the output pipe. 
   Due to this change Version 3.0.0 batch workers will no longer be compatible with pre 2.5.0 versions of the Job Service. 

## CAF_8014 - Update Batch Worker to pass Job Tracking info on creating Task Messages ##

Verify that the Batch Worker passes Job Tracking information when creating Task Messages

**Test Steps**

Post the message attached to CAF-927 onto the Batch Worker's input queue

- Do this through Rabbit UI
- Click on the queue and select Publish message
- Copy the message into the ""Payload"" field

**Test Data**

N/A

**Expected Result**

3 new messages will be on the tracking queue, 2 Example Worker messages with the taskIds of J1.1 and J1.2 with tracking information contained. The last taskId of the batch (in this case J1.2) will have an asterisk on the end e.g. J1.2*. The 3rd message will be the Batch Worker response message. 

**JIRA Link** - [CAF-927](https://jira.autonomy.com/browse/CAF-927)

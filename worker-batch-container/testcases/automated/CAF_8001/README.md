## CAF_8001 - Batch Worker - generate input messages from a comma separated list ##

Verify that the batch worker will take a comma separated list and create a message for each item and send this to a defined queue

**Test Steps**

1. Point the Batch Worker at a number of files that contain comma separated lists of small and large numbers of items
2. Verify that the output has a matching number of messages as to items in the input file lists

**Test Data**

Variety of text files with different numbers of items in comma separated lists

**Expected Result**

The files are processed and the correct number of messages are output to the queue for each item in the comma separated lists.

**JIRA Link** - [CAF-597](https://jira.autonomy.com/browse/CAF-597)


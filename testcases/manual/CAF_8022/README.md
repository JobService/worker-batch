## CAF_8022 - Tag processed items with Worker name and version ##

Verify that when an item is processed by the Batch worker it gets tagged with the name and version of the worker

**Test Steps**

1. Set up system to perform Batch using the debug parameter
2. Examine the output messages from the Batch worker

**Test Data**

Variety of text files with different numbers of items in comma separated lists

**Expected Result**

The output task message will contain a "sourceInfo" section that has the name and version of the Batch worker

**JIRA Link** - [CAF-188](https://jira.autonomy.com/browse/CAF-188)

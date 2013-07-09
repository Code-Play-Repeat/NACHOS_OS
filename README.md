Simple Operating System for group 2 of CSE150 University of California, Merced class
====================================================================================

+ Documentation for running the files is included.
+ Documentation on our implementations are included in the design documents.
- As a side note our group wasn't responsible for implementing the security part of an O.S. The main topics we covered and implemented in this O.S. project are thread system, multiprogramming, and networks & distributed systems.

Parts of the Project I was Responsible for
--------------------------------------------

Implementation of the following classes:
----------------------------------------

+ UserKernel.java
+ Alarm.java
+ NetKernel.java
+ NetProcess.java
+ I was responsible for the following methods in the PriorityScheduler.java:
* pickNextThread()
* nextThread()
* getEffectivePriority()
* setPriority(int priority)
* waitForAccess (KThread thread)
* acquire(KThread thread)
* increasePriority()
* decreasePriority()
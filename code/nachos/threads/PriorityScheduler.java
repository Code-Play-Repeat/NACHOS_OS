package nachos.threads;

import nachos.machine.*;
import java.util.ArrayList;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param    transferPriority    <tt>true</tt> if this queue should
     *   				 transfer priority from waiting threads
     *   				 to the owning thread.
     * @return    a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
   	 return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
   	 Lib.assertTrue(Machine.interrupt().disabled());

   	 return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
   	 Lib.assertTrue(Machine.interrupt().disabled());

   	 return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
   	 Lib.assertTrue(Machine.interrupt().disabled());

   	 Lib.assertTrue(priority >= priorityMinimum &&
   			 priority <= priorityMaximum);

   	 getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
   	 boolean intStatus = Machine.interrupt().disable();

   	 KThread thread = KThread.currentThread();

   	 int priority = getPriority(thread);
   	 if (priority == priorityMaximum)
   		 return false;

   	 setPriority(thread, priority+1);

   	 Machine.interrupt().restore(intStatus);
   	 return true;
    }

    public boolean decreasePriority() {
   	 boolean intStatus = Machine.interrupt().disable();

   	 KThread thread = KThread.currentThread();

   	 int priority = getPriority(thread);
   	 if (priority == priorityMinimum)
   		 return false;

   	 setPriority(thread, priority-1);

   	 Machine.interrupt().restore(intStatus);
   	 return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param    thread    the thread whose scheduling state to return.
     * @return    the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
   	 if (thread.schedulingState == null)
   		 thread.schedulingState = new ThreadState(thread);

   	 return (ThreadState) thread.schedulingState;
    }


    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {

   	 protected ArrayList threadList = new ArrayList();    	// Creates a list of threads for the priority scheduler
   	 protected KThread owner = null;   			// Makes a single thread the owner

   	 PriorityQueue(boolean transferPriority) {
   		 this.transferPriority = transferPriority;   	// Transfers the priority to the current thread.
   	 }

   	 public void waitForAccess(KThread thread) {
   		 Lib.assertTrue(Machine.interrupt().disabled());// Disable interrupts for current thread

   		 KThread threadTmp = null;
   		 for (int i = 0; i < threadList.size(); i++) {  // Begins a for-loop that will span the size of the thread list stored in threadList
   			 threadTmp = (KThread)threadList.get(i);// Gets the thread at i placed into threadTmp, a temporary thread holder.
   			 getThreadState(thread).invalidatePriority();// It resets the priority of the effective priority
   		 }
   		 
   		 threadList.add(thread);   			// Adds a passed in thread to the threadList
   		 getThreadState(thread).waitForAccess(this);   	// The thread is waiting for this to finish before getting the thread state.
   	 }

   	 public void acquire(KThread thread) {
   		 Lib.assertTrue(Machine.interrupt().disabled());// Disable interrupts for current thread
   		 owner = thread;   				// The owner acquires the current thread and all of its ownerships
   		 getThreadState(thread).acquire(this);   	// The thread can acquire this thread's state
   	 }

   	 /**
   	  * Will choose the next thread and place it into owner, returning it.
   	  *
   	  * @see nachos.threads.PriorityScheduler#nextThread
   	  * @return The new owner
   	  */
   	 public KThread nextThread() {   			// This thread takes the next thread, removing it from the waiting thread list. Pick next thread just chooses the next.
   		 boolean intStatus = Machine.interrupt().disable();// Disable interrupts for current thread

   		 if (owner != null){   				// Checks to see if there is a current value in owner
   			 getThreadState(owner).removeQueue(this);// If there is, remove it from the queue. It is just a check to see if its a idleThread
   		 }

   		 /**
   		  * This checks to see if there is anything on the waiting to run thread list.
   		  * If there is, it will skip as isEmpty() will return false, otherwise this will
   		  * run, exiting this current method.
   		  */
   		 if (isEmpty()) {   				// If there is nothing in the list
   			 owner = null;   			// Deallocated the owner, removing all attributes from it
   			 Machine.interrupt().restore(intStatus);// The interrupts are re-enabled
   			 return null;   			// Return null to exit nextThread
   		 }

   		 owner = pickNextThread().thread;  // If it reaches here, there is something on the list, and the owner becomes the next thing on that list.

   		 if (owner != null) {   			// If owner is not null, aka if it is not idle
   			 getThreadState(owner).invalidatePriority();// Resets effectivePriority
   			 threadList.remove(owner);   		// Removes the owner from the thread list
   			 acquire(owner);   			// The owner acquires access
   		 }
   		 Machine.interrupt().restore(intStatus);   	// Restores interrupts
   		 return owner;   				// Returns the owner
   	 }

   	 /**
   	  * Checks to see if the thread list is empty or not. This will be used to
   	  * determine if there is anything waiting to be run.
   	  *
   	  * @see nachos.threads.PriorityScheduler#isEmpty
   	  * @return <tt>True</tt> if empty, <tt>false</tt> is not
   	  */
   	 public boolean isEmpty() {   				// Checks to see if the threadList is empty
   		 return threadList.isEmpty();   		// True if empty, false if not.
   	 }

   	 /**
   	  * Return the next thread that <tt>nextThread()</tt> would return,
   	  * without modifying the state of this queue.
   	  *
   	  * @return    the next thread that <tt>nextThread()</tt> would
   	  *   	 return.
   	  */
   	 protected ThreadState pickNextThread() {   		// Looks to see what the next thread is, a peek function
   		 if (isEmpty())   				// If there is nothing on the thread list
   			 return null;   			// Returns nothing

   		 int position = 0;   				// Position of the thread in the list
   		 int max = 0;   				// Max priority
   		 int current = 0;   				// current priority
   		 long currentTime = System.currentTimeMillis(); // Gets the current time, right now
   		 long waitedTime = 0;   			// The time the thread was put onto the waiting queue

   		 // Checks the priorities of all waiting threads. If equivalent priorities, takes longest time.
   		 for (int i = 0; i < threadList.size(); i++) {    // Goes through the list of threads waiting
   			 KThread thread = (KThread) threadList.get(i);// gets the value in the list

   			 if (transferPriority)   		// true if this queue should transfer priority from waiting threads to the owning thread.
   				 current = getThreadState(thread).getEffectivePriority();// Gets the effective priority
   			 else   				// this queue should not transfer from waiting thread to owning thread
   				 current = getThreadState(thread).getPriority();// gets the priority

   			 if (current > max) {   		// As long as current is more than 1, there will be a new value of max the first time, after that it will take the max.
   				 max = current;   		// If current is larger, max becomes current
   				 position = i;   		// The position the the thread is saved
   				 currentTime = waitedTime;   	// The clock is reset
   			 }
   			 else if (current == max) {   		// If they have the same priority
   				 waitedTime = getThreadState(thread).getWaitingTime();// Gets how long the current thread has been waiting
   				 if (waitedTime < currentTime) {// Compares the current time to the other time the smaller the number, the longer it has been waiting,
   					 currentTime = waitedTime;// The current time gets updated to be replaced with the oldest waiting position/time
   					 position = i;   	// Gets the new position
   				 }
   			 }
   		 }
   		 
   		 return getThreadState((KThread) threadList.get(position));// returns the thread that you will be choosing at position
   	 }

   	 /**
   	  * Pretty useless. We do not call it.
   	  *
   	  * @see nachos.threads.PriorityScheduler#print
   	  */
   	 public void print() {
   		 Lib.assertTrue(Machine.interrupt().disabled());
   		 // Don't really want to <3
   	 }

   	 /**
   	  * <tt>true</tt> if this queue should transfer priority from waiting
   	  * threads to the owning thread.
   	  */
   	 public boolean transferPriority;   // Just a boolean, nothing to see >.>

   	 /**
   	  * Returns the list of threads that are being used in the priority
   	  * scheduler.
   	  *
   	  * @see nachos.threads.PriorityScheduler#getThreadList
   	  */
   	 public ArrayList getThreadList() { // Just a function call to get the list of threads
   		 return threadList;   	    // Yup...
   	 }
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see    nachos.threads.KThread#schedulingState
     */
    protected class ThreadState implements Comparable {

   	 protected ArrayList acquiredList = new ArrayList();    // Creates a new list, later adding the wait queue to the list
   	 protected long waitingTime = System.currentTimeMillis();// Gets the current time
   	 protected int effectivePriority = -1;   		// Will change to the donated priority
   	 protected int recalcPriority = -1;   			// Will always stay -1

   	 /**
   	  * Allocate a new <tt>ThreadState</tt> object and associate it with the
   	  * specified thread.
   	  *
   	  * @param    thread    the thread this state belongs to.
   	  */
   	 public ThreadState(KThread thread) {
   		 this.thread = thread;   			// Passes in a thread and makes it this thread
   		 setPriority(priorityDefault);   		// Makes it the default priority
   	 }

   	 /**
   	  * Return the priority of the associated thread.
   	  *
   	  * @return    the priority of the associated thread.
   	  */
   	 public int getPriority() {
   		 return priority;   				// Returning the priority
   	 }

   	 /**
   	  * Return the effective priority of the associated thread.
   	  *
   	  * @return    the effective priority of the associated thread.
   	  */
   	 public int getEffectivePriority() {   			// Where it starts to get fun....
   		 /**
   		  * This will be used for all the waiting threads.
   		  */
   		 PriorityQueue waitQueue = null;   		// Starts the wait queue at nothing, the wait queue will be used for all of the waiting threads
   		 ArrayList threads = null;   			// Same with the list of arrays, will be used for the list of threads
   		 int max = priority;   				// Saves the current priority as the max
   		 int current = 0;   				// Current is the priority after scheduling, just initializing right now

   		 /**
   		  * Because effectivePriority is being recalculated often and the number is being saved to priority,
   		  * it will calculate it and save it to effective priority. If it has not been reset, it will have
   		  * its value, a number that is not -1. Because it is not -1, it can not be changed in this method
   		  * and will therefore return its current value.
   		  */
   		 if (effectivePriority == recalcPriority) {   	 // Static for the effective priority
   			 for (int i = 0; i < acquiredList.size(); i++) {// For loop for the size of the list
   				 waitQueue = (PriorityQueue) acquiredList.get(i);// Passes in the thread at point i to waitQueue
   				 threads = waitQueue.getThreadList();    // Passes the array of threads into threads
   				 for (int j = 0; j < threads.size(); j++) {// Just a for-loop at the size of the list of threads
   					 current = ((ThreadState)((KThread)threads.get(j)).schedulingState).getEffectivePriority();// Gets the current priority after scheduling
   					 if (current > max) {    // If the current priority is greater than the previous highest,
   						 max = current;  // Saves the newest highest priority to max
   						 effectivePriority = max; // Saves the highest priority to effective priority
   					 }
   				 }    
   			 }   	 
   		 }
   		 
   		 /**
   		  * Can do this double check because it will change the effective priority in the previous section.
   		  * If it is changed previously, it will not do anything here, but if it is not changed, it will
   		  * get its priority.
   		  */
   		 if (effectivePriority == -1)   			// If the effective priority has not changed,
   			 effectivePriority = getPriority();   		// It gets the current priority of the thread
   		 return effectivePriority;   				// Then it returns what the new effective priority is
   	 }

   	 /**
   	  * Set the priority of the associated thread to the specified value. Will also
   	  * check to make sure priorities are not at max or min values.
   	  *
   	  * @param    priority    the new priority.
   	  */
   	 public void setPriority(int priority) {
   		 if (this.priority == priority)   			// If the priorities are equal, nothing to be done so exits
   			 return;   					// In this case, an exit call
   		 else if (priority > PriorityScheduler.priorityMaximum) // If the new priority will be above the max priority,
   			 this.priority = PriorityScheduler.priorityMaximum;// It gets the max value (7)
   		 else if (priority < PriorityScheduler.priorityMinimum) // If it is under the minimum,
   			 this.priority = PriorityScheduler.priorityMinimum;// It will be set as the minimum (0)
   		 else
   			 this.priority = priority;   			// Otherwise, the priority will get the priority
   		 effectivePriority = recalcPriority;   			// Resets effective priority to -1

   	 }

   	 /**
   	  * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
   	  * the associated thread) is invoked on the specified priority queue.
   	  * The associated thread is therefore waiting for access to the
   	  * resource guarded by <tt>waitQueue</tt>. This method is only called
   	  * if the associated thread cannot immediately obtain access.
   	  *
   	  * @param    waitQueue    the queue that the associated thread is
   	  *   			 now waiting on.
   	  *
   	  * @see    nachos.threads.ThreadQueue#waitForAccess
   	  */
   	 public void waitForAccess(PriorityQueue waitQueue) {
   		 waitingTime = System.currentTimeMillis();   	 // Passes in the current time to waiting time
   	 }

   	 /**
   	  * Called when the associated thread has acquired access to whatever is
   	  * guarded by <tt>waitQueue</tt>. This can occur either as a result of
   	  * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
   	  * <tt>thread</tt> is the associated thread), or as a result of
   	  * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
   	  *
   	  * @see    nachos.threads.ThreadQueue#acquire
   	  * @see    nachos.threads.ThreadQueue#nextThread
   	  */
   	 public void acquire(PriorityQueue waitQueue) {
   		 acquiredList.add(waitQueue);

   		 effectivePriority = recalcPriority;
   	 }    

   	 /**
   	  * Called when the associated thread wishes to know when an item goes
   	  * onto the stack.
   	  *
   	  * @see nachos.threads.ThreadQueue#getWaitingTime
   	  */
   	 public long getWaitingTime() {   				// Returns the waiting time
   		 return waitingTime;
   	 }

   	 /**
   	  * Called when a queue is to be removed from a list
   	  *
   	  * @see nachos.threads.threadQueue#removeQueue
   	  */
   	 public void removeQueue(PriorityQueue queue) {   		// Removes the queue from the list
   		 acquiredList.remove(queue);   				// This removes an entire queue
   	 }

   	 /**
   	  * When called, invalidatePriority will reset the <tt>effectivePriority</tt>.
   	  *
   	  * @see nachos.threads.threadQueue#invalidatePriority
   	  */
   	 public void invalidatePriority() {   				// Resets the priority
   		 effectivePriority = recalcPriority;
   	 }
   	 
   	 /** The thread with which this object is associated. */       
   	 protected KThread thread;
   	 /** The priority of the associated thread. */
   	 protected int priority;

   	 /**
   	  * Compares an object passed into <tt>compareTo()</tt> to the current object
   	  * checking the priotity's to each other, and if the object's priority is
   	  * greater than the current, or they are the same and the waiting time is longer,
   	  * an integer value is returned, -1, 0, or 1. No explicit calls in this class.
   	  * It, however, is essential to the entire class's implementation.
   	  *
   	  * @see nachos.threads.threadQueue#compareTo
   	  */
   	 public int compareTo(Object obj) {   				 // Compares an object to the current object
   		 long waitingTime1 = (this.waitingTime);   		 // Gets and saves the current object's waiting time
   		 long waitingTime2 = (((ThreadState)obj).waitingTime);   // Saves the object passed in's waiting time
   		 long priority1 = (this.priority);   			 // Gets the priority of the current object
   		 long priority2 = (((ThreadState)obj).priority);   	 // Gets the priority of the object passed in
   		 if (priority1 > priority2)   				 // If the current thread's priority is greater than the object's priority
   			 return -1;   					 // return -1
   		 else if (priority1 < priority2)   			 // If its priority is less than the passed in object
   			 return 1;   					 // return 1
   		 else{   						 // If the priorities are equal
   			 if(waitingTime1 < waitingTime2)    		 // compare the waiting times, if the waiting time of the current thread is less than that of the new object return -1.
   				 return -1;   			 	 // This is true because you want the lower number. (it does not go by actual waiting time, just the starting time)
   			 else if (waitingTime1 > waitingTime2)   	 // If its waiting time is higher, return 1
   				 return 1;
   			 else
   				 return 0;   				 // Otherwise they have been waiting the exact same time and are equal.
   		 }
   	 }

   	 /**
   	  * Passes in an <tt>object</tt> and checks it against the current object
   	  *
   	  * @see nachos.threads.threadQueue#equals
   	  */
   	 public boolean equals(Object obj) {   				 // Checks to
   		 if (this.waitingTime == ((ThreadState)obj).waitingTime && this.priority == ((ThreadState)obj).priority)
   			 return true;
   		 return false;
   	 }
    }
}

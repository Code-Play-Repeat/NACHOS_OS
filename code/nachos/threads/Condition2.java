package nachos.threads;
import java.util.*;
import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	this.conditionLock = conditionLock;
	//Create linkedlist conditionQueue
	conditionQueue = new LinkedList<KThread>();
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	boolean intStatus = Machine.interrupt().disable();
	//Store the current thread to Linked List
	KThread current = KThread.currentThread();
	conditionQueue.push(current);
	conditionLock.release();
	//Put the current thread to sleep
	KThread.sleep();
	conditionLock.acquire();
	Machine.interrupt().restore(intStatus);
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	boolean intStatus = Machine.interrupt().disable();
	//Check if there is any threads in conditionQueue, and remove the thread from the list and ready it.  
	if(!conditionQueue.isEmpty()) {
	KThread remove = conditionQueue.pop();
	remove.ready();
	}
	Machine.interrupt().restore(intStatus);

    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	boolean intStatus = Machine.interrupt().disable();
	//While there are threads in the conditionQueue, you remove it and ready it.
	while(conditionQueue.size() >0) 
		wake();
	Machine.interrupt().restore(intStatus);
    }

    private Lock conditionLock;
    private LinkedList<KThread> conditionQueue;
}

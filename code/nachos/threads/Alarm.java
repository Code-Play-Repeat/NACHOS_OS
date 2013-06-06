package nachos.threads;

import nachos.machine.*;
import java.util.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
		// sleeping thread queue to keep track of sleeping threads
		private PriorityQueue<SleepingThread> STQueue;
		private static long wakeTime;

    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
		// creates an empty priority queue of type sleeping threads that uses
		// the thread comparator class
		// its initial size is 11 and its capacity grows automatically
		// as elements are added to the queue
		Comparator<SleepingThread> comparator = new ThreadComparator();
		STQueue = new PriorityQueue<SleepingThread>(11,comparator);
    }

	


	/*
	*	A private class to store a sleeping thread and the amount of time for
  *   the thread to wait until waking
	*/
	private class SleepingThread {
		public SleepingThread (KThread thread, long time) {
			this.st = thread; // stores sleeping thread
			this.time = time; // stores the amount of time this thread waits until waking
		}
		public KThread st;
		public long time;
	}

/* a ThreadComparator class to compare the waiting times amongst two threads 
*		The order is based off of the tiem threads require for waiting
*   Threads that have shorter wait times would be placed in front whereas
*   threads with longer wait times would be placed in back
*/

	private class ThreadComparator implements Comparator<SleepingThread>
  { 
		public int compare (SleepingThread thread1, SleepingThread thread2)
		{
			if(thread1.time > thread2.time)
				return 1;
			else if (thread1.time < thread2.time)
				return -1;
			else
				return 0;
		}
			
  }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
	KThread.currentThread().yield();
		


		// head of queue
		SleepingThread head = STQueue.peek();

		// first checks to see if head is not null and  if the time has
	  // passed for waiting.
		// If the time has passed it loops through the priority queue
		// until it reaches a thread that still has time leftover to wait.
		while ((head != null) && (head.time <= Machine.timer().getTime()))
		{
			// acquire lock
			boolean status = Machine.interrupt().disable();
			// place head of queue onto the ready queue
			head.st.ready();
			// remove the head of the queue from the priority queue
			// this is because the head of the queue is no longer sleeping
			STQueue.remove(head);
			// advance head variable to the next head of the queue
			head = STQueue.peek();
			// release lock
			Machine.interrupt().restore(status);
		}



			
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
		
    public void waitUntil(long x) 
		{
		// for now, cheat just to get something working (busy waiting is bad)
		// while (wakeTime > Machine.timer().getTime())
		//    KThread.yield();
		// long wakeTime = Machine.timer().getTime() + x;
		
			wakeTime = Machine.timer().getTime() + x;
			// checks to see if wakeTime did not pass
			if (wakeTime > Machine.timer().getTime())
			{
					boolean status = Machine.interrupt().disable();
	
					// Creates new sleeping thread that stores currentThread information and wake time
					// waketime = current time + x (amount of time requested by thread to add to the 	current time)
					SleepingThread thread = new SleepingThread(KThread.currentThread(),wakeTime);
		
					// insert sleeping thread onto priority queue
					STQueue.add(thread);
			
					// put thread to sleep
					KThread.currentThread().sleep();
					// release lock
					Machine.interrupt().restore(status);

			}
		}	
}

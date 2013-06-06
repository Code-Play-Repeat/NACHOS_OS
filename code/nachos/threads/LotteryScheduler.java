package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.PriorityQueue;
import nachos.threads.PriorityScheduler.ThreadState;

import java.util.*;

/**
* A scheduler that chooses threads using a lottery.
*
* <p>
* A lottery scheduler associates a number of tickets with each thread. When a
* thread needs to be dequeued, a random lottery is held, among all the tickets
* of all the threads waiting to be dequeued. The thread that holds the winning
* ticket is chosen.
*
* <p>
* Note that a lottery scheduler must be able to handle a lot of tickets
* (sometimes billions), so it is not acceptable to maintain state for every
* ticket.
*
* <p>
* A lottery scheduler must partially solve the priority inversion problem; in
* particular, tickets must be transferred through locks, and through joins.
* Unlike a priority scheduler, these tickets add (as opposed to just taking the
* maximum).
*/
public class LotteryScheduler extends PriorityScheduler {
   /**
    * Allocate a new lottery scheduler.
    */
   public LotteryScheduler() {
   }
   
   public static final int priorityDefault = 1;
   /**
    * The minimum priority that a thread can have. Do not change this value.
    */
   public static final int priorityMinimum = 1;
   /**
    * The maximum priority that a thread can have. Do not change this value.
    */
   public static final int priorityMaximum = Integer.MAX_VALUE;
   
   public static ThreadState schedulingState = null;
   
   public KThread nextThread() {
       boolean intStatus = Machine.interrupt().disable();
       if(owner != null)
           owner = null;

       if (isEmpty()) {
           owner = null;
           Machine.interrupt().restore(intStatus);
           return null;
       }

       owner = pickNextThread().thread;

       if(owner != null) {
           threadList.remove(owner);
           acquire(owner);
       }
       Machine.interrupt().restore(intStatus);
       return owner;
   }

   private void acquire(KThread thread) {
       acquiredList.add(thread);
   }

   public void removeQueue(LotteryScheduler lottery) {            // Removes the queue from the list
       acquiredList.remove(lottery);                            // This removes an entire queue
   }

   protected ThreadState pickNextThread() {
       Random rand = new Random();
       if(isEmpty())
           return null;

       int position = 0;
       int current = 0;
       int tickets;
       int randNum;
       
       tickets = sum();
       randNum = rand.nextInt(tickets) + 1;
       
       if(randNum == 1)
           return getThreadState((KThread) threadList.get(0));

       for(int i = 1; i < threadList.size(); i++) {
           KThread thread = (KThread) threadList.get(i);
           current += getThreadState(thread).getPriority();
           if(current > randNum) {
               position = i;
               break;
           }
       }
       return getThreadState((KThread) threadList.get(position));
   }

   protected int sum() {
       if(isEmpty())
           return 0;
       
       int current = 0;

       for(int i = 0; i < threadList.size(); i++) {
           KThread thread = (KThread) threadList.get(i);
           current += getThreadState(thread).getPriority();
           if(current > priorityMaximum) {
               current = priorityMaximum;
           }
       }
       return current;
   }
   
   public boolean isEmpty() {
       return threadList.isEmpty();
   }
   
   protected ArrayList threadList = new ArrayList();
   protected ArrayList acquiredList = new ArrayList();
   protected KThread owner = null;
}

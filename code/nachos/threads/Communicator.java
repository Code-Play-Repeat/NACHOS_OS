package nachos.threads;
import java.util.LinkedList;
import java.lang.Integer;
import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
        private Lock commLock;
        private int word;
        private boolean listenerBusy;
        private boolean hasSpeaker;
	private boolean wordReady;
	private Condition2 thereIsWord;
	private Condition2 speakerLeft;
	private Condition2 listenerReady;
	private Condition2 wordTaken;


        /**
         * Allocate a new communicator.
         */
        public Communicator() {
		commLock = new Lock();
                speakerLeft = new Condition2(commLock);
		listenerReady = new Condition2(commLock);
		wordTaken = new Condition2(commLock);
		thereIsWord = new Condition2(commLock);			           

		word = 0;

                listenerBusy = false;
                hasSpeaker = false;
		wordReady = false;
		
        }

        /**
         * Wait for a thread to listen through this communicator, and then transfer
         * <i>word</i> to the listener.
         *
         * <p>
         * Does not return until this thread is paired up with a listening thread.
         * Exactly one listener should receive <i>word</i>.
         *
         * @param       word    the integer to transfer.
         */
        public void speak(int word) {

		                
		

                commLock.acquire();
		while (hasSpeaker){		//check if already has speaker
			speakerLeft.sleep();	//if has speaker next speaker sleeps
		}
		
		hasSpeaker = true;
		this.word = word;
		wordReady = true;
		thereIsWord.wake();		// if listener runs first, wakes the function so that there is now a word ready to take.

		while(wordReady){		//the check if the word is gone
			wordTaken.sleep();
		}
		
		hasSpeaker = false;
		speakerLeft.wake();		//lets the next speaker speak

                commLock.release();
		
        }

        /**
         * Wait for a thread to speak through this communicator, and then return
         * the <i>word</i> that thread passed to <tt>speak()</tt>.
         *
         * @return      the integer transferred.
         */    
        public int listen() {

		int word = 0;
		commLock.acquire();
		while(listenerBusy){			//checks for another listener
			listenerReady.sleep();
		}
		listenerBusy = true;
		while(!wordReady){			//check if speaker has spoken yet
			thereIsWord.sleep();
		}		
		word = this.word;			//takes word
		wordReady = false;			//
		wordTaken.wake();			//tells speaker the the word has been taken
		
		listenerBusy = false;
		
		listenerReady.wake();			//lets the next listener listen
		commLock.release();
		return word;
		
                
        }


}

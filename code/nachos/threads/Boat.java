package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat 
{
        static BoatGrader bg;
        static Lock submarineLock;             
        static boolean oneOnSub;         
        static Condition2 molokaiChild;     
        static Condition2 oahuWaitChild; 
	static Semaphore childAtMolokai; 
        static Semaphore submarineReserve;   
        static Semaphore submarineReserveForTwoSlots; 
        static boolean oneChildLeft = false;  
        static int crossedChildren = 0, crossedAdults = 0; //Number of children and adults who have crossed at a point of time

        public static void selfTest(int adults, int children) 	//self test
	{
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
        }

        public static void begin(int adults,int children,BoatGrader b)
	{
                bg = b;
                childAtMolokai = new Semaphore(0);
                submarineReserve = new Semaphore(1);
                submarineReserveForTwoSlots = new Semaphore(2); 
                submarineLock = new Lock();
                oneOnSub = false;
                molokaiChild = new Condition2(submarineLock);
                oahuWaitChild = new Condition2(submarineLock);
                oneChildLeft = false;
                crossedChildren = 0;
                crossedAdults = 0;
                for (int i = 0; i < adults; i++) //Create the required number of adult threads
		{
                        Runnable r = new Runnable() 
			{
                                public void run() 
				{
                                        AdultItinerary();
                                }
                        };
                        KThread t = new KThread(r);
                        t.setName("The adult No." + i);
                        t.fork();
                }

                for (int i = 0; i < children; i++)  //Create the required number of Child threads
		{
                        Runnable r = new Runnable() 
			{
                                public void run() 
				{
                                        ChildItinerary();
                                }
                        };
                        KThread t = new KThread(r);
                        t.setName("The child No." + i);
                        t.fork();
                }
		
                while (!oneChildLeft)		//keeps running untill the number of adults and childeren
		{				//crossed equal the number we started with
                        oneChildLeft = ((crossedAdults == adults) && (crossedChildren == children - 1));
                        KThread.yield();
                }
		
                bg.ChildRowToMolokai(); 	//rows last child to Molokai

        }

        /**
         * The algorithm is pretty simple.One child keeps going back and forth between Molokai and Oahu.
	 * If an adult is on Oahu he takes the boat and rows to Molokai given that there is at least one
	 * child already there. Once and adult is transported he stays at Molokai and a child brings the boat
	 * back to Oahu to see if anybody is remaining on Oahu.All trips from Oahu to Molokai are two-child
         * or one adult trips and all trips from Molokai to Oahu are single child trips.
         */

        static void AdultItinerary() 
	{
                childAtMolokai.P(); //wait for at least one child to be present at Molokai 
                submarineLock.acquire(); 
                bg.AdultRowToMolokai(); //Pilots the sub to Molokai
                molokaiChild.wake();  //Wake the child so he can pilot back the submarine 
                crossedAdults++;        
                crossedChildren--;
                submarineLock.release();
        }

        static void ChildItinerary() 
	{
                boolean atMolokai = false; //True if child is at Molokai.Child starts on Oahu
                while (!oneChildLeft)          
		{
                        if (atMolokai)     //If child is at molokai, do the following (Ride to Oahu)
			{
                                bg.ChildRowToOahu(); //Pilots the sub to Oahu
                                atMolokai = false;   
                        } 
			else //Else if child is at oahu, do the following (Ride to Moloaki)
			{
                                submarineReserveForTwoSlots.P(); 
                                submarineLock.acquire();
                                if (oneOnSub)       //checks if the other child is already on the sub
				{
                                        oneOnSub = false;      
                                        oahuWaitChild.wake();  
                                        bg.ChildRowToMolokai(); //Pilots the sub to Molokai
                                        atMolokai = true;       
                                        crossedChildren++;
                                        molokaiChild.sleep();  
                                } 
				else   //or Two children
				{
                                        oneOnSub = true;       
                                        oahuWaitChild.sleep();
                                        bg.ChildRideToMolokai(); //Ride to Molokai
                                        bg.ChildRowToOahu();     //Pilot the sub back to Oahu
					childAtMolokai.V();  
                                        submarineReserveForTwoSlots.V();    
                                        submarineReserveForTwoSlots.V();    
                                }
                                submarineLock.release();
                        }
                }
        }
}

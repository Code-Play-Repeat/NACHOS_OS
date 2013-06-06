package nachos.userprog;
import java.util.*;
import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
	
    pid=pidNums++;
	fileTable[0] = UserKernel.console.openForReading();
	fileTable[1] = UserKernel.console.openForWriting();
	
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
    
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	
	
	myThread=new UThread(this).setName(name);
	
	myThread.fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);
	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// for now, just assume that virtual addresses equal physical addresses
	/*
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;

	*/
	
	int byteNum = 0;
	do{
		// calculating page index from virtual address and byte number
		int pageIndex = Processor.pageFromAddress(vaddr+ byteNum);
		// checking the bounds of the page index to make sure that it is within the proper bounds
		if (pageIndex < 0 || pageIndex >= pageTable.length) return 0;
		// calculating page offset using virtual address and byte number
		int pageOffset = Processor.offsetFromAddress(vaddr+ byteNum);
		// calculating number of bytes left in the page which is the page size - the page offset
		int amountLeftInPage = pageSize - pageOffset;
		// calculating number of bytes to read which is hte min of the amount left and the length - byte number
		int amountToRead = Math.min(amountLeftInPage, length - byteNum);
		// physical address calculated from page table entry's ppn * page size + page offset.
		int physicalAddr =  pageTable[pageIndex].ppn*pageSize + pageOffset;
		// transfering data from this process's virtual memory to all of the specified array
		System.arraycopy(memory, physicalAddr, data, offset + byteNum, amountToRead);
		byteNum += amountToRead;
	}while(byteNum < length);
	
	
	
	return byteNum;

    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
	byte[] memory = Machine.processor().getMemory();
	
	// for now, just assume that virtual addresses equal physical addresses
	
	//check to see if the write could succeed before trying
	int byteNum = 0;
	do{
		int pageIndex = Processor.pageFromAddress(vaddr + byteNum);
		if (pageIndex < 0 || pageIndex >= pageTable.length || pageTable[pageIndex].readOnly) return 0;
		int pageOffset = Processor.offsetFromAddress(vaddr+ byteNum);
		int amountLeftInPage = pageSize - pageOffset;
		int amountToWrite = Math.min(amountLeftInPage, length - byteNum);

		byteNum += amountToWrite;
	}while(byteNum < length);
	
	//do the write
	byteNum = 0;
	do{
		// same calculations as in readVirtualMemory
		// except array copy is different
		int pageIndex = Processor.pageFromAddress(vaddr + byteNum);
		int pageOffset = Processor.offsetFromAddress(vaddr+ byteNum);
		int amountLeftInPage = pageSize - pageOffset;
		int amountToWrite = Math.min(amountLeftInPage, length - byteNum);
		int physicalAddr =  pageTable[pageIndex].ppn*pageSize + pageOffset;
		// this time it transfers data from the specified array to this process's virtual memory
		System.arraycopy(data, offset + byteNum, memory, physicalAddr, amountToWrite);
		byteNum += amountToWrite;
	}while(byteNum < length);

	return byteNum;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
		System.out.println("fail1");
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    System.out.println("fail1");
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		System.out.println("fail2");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    System.out.println("fail3");
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;

	if (!loadSections())
	{System.out.println("fail4");
	    return false;
	}

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {

    	UserKernel.availablePageLock.acquire();
		// check if there is enough physical memory to load programs
		if (numPages > UserKernel.availablePages.size()) {
		    coff.close();
		    Lib.debug(dbgProcess, "\tinsufficient physical memory");
		    UserKernel.availablePageLock.release();
		    return false;
		}
		// initilize a new page table and place the available pages from user kernel into the page table
		pageTable = new TranslationEntry[numPages];
		for (int i=0; i< numPages; i++)
    	    pageTable[i] = new TranslationEntry(i,UserKernel.availablePages.remove(), true,false,false,false);
		UserKernel.availablePageLock.release();
		// load sections
		for (int s=0; s<coff.getNumSections(); s++) {
		    CoffSection section = coff.getSection(s);
		    
		    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
			      + " section (" + section.getLength() + " pages)");
	
				// goes through each section and updates the page table entries to read only
				// also loads the pages from the page table
		    for (int i=0; i<section.getLength(); i++) {
			int vpn = section.getFirstVPN()+i;

			//int ppn = UserKernel.availablePages.remove().ppn;
			pageTable[vpn].readOnly = section.isReadOnly();

			// for now, just assume virtual addresses=physical addresses
			section.loadPage(i, pageTable[vpn].ppn);
		    }
		}
		
		return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    	UserKernel.availablePageLock.acquire();
			// adds the pages from the page table back to the User Kernel and clears the page table contents
			// this effectively releases any resources allocated by loadSections()
    	for (int i=0; i< numPages; i++)
    	{
    		UserKernel.availablePages.add(pageTable[i].ppn);
    		pageTable[i] = null;
    	}
    	UserKernel.availablePageLock.release();
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {
    	
    if (pid==0)
    	Machine.halt();
	
	//Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }

	private int handleExit(int st) {
		coff.close();
		for (int i =0; i <fileTable.length;i++)
		{
			if (fileTable[i]!=null) 
				{
				fileTable[i].close();
				fileTable[i]=null;
				}
		}
		//System.out.println("checking");
		this.status=st;
		goodExit=true;
		if (parent!=null)
		{
			parent.children.remove(this);
		}
		unloadSections();
		KThread.finish();
		
		return 0;
	}

	private int handleExec(int namePtr,int argc, int argv) {
		if (argc < 0) return -1;
		
		String fileName = readVirtualMemoryString(namePtr, maxStrLength);

		String[] args = new String[argc];
		for(int i=0;i < argc;i++)
		{
			byte[] memLocation = new byte[4];
			if(this.readVirtualMemory(argv + i*4, memLocation) > 0){
				args[i] = readVirtualMemoryString(Lib.bytesToInt(memLocation, 0), maxStrLength);
			}
		}
		UserProcess process = UserProcess.newUserProcess();
		if(!process.execute(fileName, args)) return -1;
		children.add(process);
		process.parent = this;
		return process.pid;
	}

	private int handleJoin(int pid, int statusAddress) {
		UserProcess child=null;       

		for (int i=0;i<children.size();i++)
		{
			if (children.get(i).pid == pid)
			{
				child=children.get(i);
				break;				
			}
		}
		if (child==null || child.myThread==null) return -1;
		child.myThread.join();
		
		byte[] stat = new byte[4];
		Lib.bytesFromInt(stat, 0, child.status);
		int bytesWritten = writeVirtualMemory(statusAddress, stat);
		return (child.goodExit && bytesWritten == 4) ? 1 : 0;
	}

	
	private int handleCreate(int a0) {
		if (readVirtualMemoryString(a0, 256)==null) 
			return -1;
		
		int fd = findOpening();
		if (fd!=-1)
		{
		fileTable[fd]=ThreadedKernel.fileSystem.open(readVirtualMemoryString(a0, 256),true);
			if (fileTable[fd]==null) 
				return -1;	
		return fd;
		}
		else return -1;
	}


       private int handleOpen(int a0) {
		if (readVirtualMemoryString(a0, 256)==null) 
			return -1;
		
		int fd = findOpening();
		if (fd!=-1)
		{
		fileTable[fd]=ThreadedKernel.fileSystem.open(readVirtualMemoryString(a0, 256),false);
			if (fileTable[fd]==null) 
				return -1;
		return fd;
		}
		else return -1;
	}

	private int handleRead(int a0,int a1,int a2) {
		if (a0>15 || a0 < 0 || fileTable[a0]==null) 
			return 0;
		byte[] a = new byte[a2];
		int amountRead = fileTable[a0].read(a, 0, a2);
		if(amountRead <=0) 
			return 0;
		return this.writeVirtualMemory(a1, a,0,amountRead);

	}

	private int handleWrite(int a0,int a1,int a2) {
		if (a0>15 || a0 < 0 || fileTable[a0]==null) 
			return 0;
		byte[] dataToWrite = new byte[a2];
		int amountRead = readVirtualMemory(a1,dataToWrite,0, a2);
		if(amountRead != a2) 
			return 0;
		return fileTable[a0].write(dataToWrite, 0, a2);
	}

       private int handleClose(int a0) {		
		if (a0>15 || a0 < 0) 
			return -1;
		OpenFile f = fileTable[a0];
		if (f==null) 
			return -1;		
		f.close();
		fileTable[a0]=null;
		return 0;
	}

	private int handleUnlink(int a0) {
    	String s = readVirtualMemoryString(a0,256);
    	if (ThreadedKernel.fileSystem.remove(s))
    	{
		return 0;
    	}
    	else return -1;
	}

	
	public int findOpening()
	{		
		for (int i=0;i<fileTable.length;i++)
		{
			if (fileTable[i]==null) return i;
		}
		return -1;
	}


    private static final int
        	syscallHalt = 0,
		syscallExit = 1,
		syscallExec = 2,
		syscallJoin = 3,
		syscallCreate = 4,
		syscallOpen = 5,
		syscallRead = 6,
		syscallWrite = 7,
		syscallClose = 8,
		syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
    	//System.out.println("dd");
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
	case syscallExit:
		return handleExit(a0);
	case syscallExec:
		return handleExec(a0,a1,a2);
	case syscallJoin:
		return handleJoin(a0,a1);
	case syscallCreate:
		return handleCreate(a0);
	case syscallOpen:
		return handleOpen(a0);
	case syscallRead:
		return handleRead(a0,a1,a2);
	case syscallWrite:
		return handleWrite(a0,a1,a2);
	case syscallClose:
		return handleClose(a0);
	case syscallUnlink:
		return handleUnlink(a0);
		
	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }

    

	/**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	  //  System.out.println(result);
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				  
	case Processor.exceptionReadOnly:
		System.out.println("exception: Read Only");
		handleExit(1);
		break;
	case Processor.exceptionAddressError:
		System.out.println("exception: Address Error");
		handleExit(1);
		break;
	case Processor.exceptionPageFault:
		System.out.println("exception: Page Fault");
		handleExit(1);
		break;
	case Processor.exceptionIllegalInstruction:
		System.out.println("exception: Illegal Instruction");
		handleExit(1);
		break;
	case Processor.exceptionOverflow:
		System.out.println("exception: Overflow");
		handleExit(1);
		break;
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static int maxStrLength=256;
    
    public  OpenFile[] fileTable = new OpenFile[16];
  
    private int pid;
    public static int pidNums=0;
    public KThread myThread;
    public int status = 0;
    public boolean goodExit=false;
    private ArrayList<UserProcess> children = new ArrayList<UserProcess>();
    public UserProcess parent=null;
}


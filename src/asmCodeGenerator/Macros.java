package asmCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.runtime.RunTime;

public class Macros {
	
	public static void addITo(ASMCodeFragment frag, String location) {
		loadIFrom(frag, location);
		frag.add(Add);
		storeITo(frag, location);
	}
	public static void incrementInteger(ASMCodeFragment frag, String location) {
		frag.add(PushI, 1);
		addITo(frag, location);
	}
	public static void decrementInteger(ASMCodeFragment frag, String location) {
		frag.add(PushI, -1);
		addITo(frag, location);
	}
	
	public static void loadIFrom(ASMCodeFragment frag, String location) {
		frag.add(PushD, location);
		frag.add(LoadI);
	}
	public static void storeITo(ASMCodeFragment frag, String location) {
		frag.add(PushD, location);
		frag.add(Exchange);
		frag.add(StoreI);
	}
	public static void declareI(ASMCodeFragment frag, String variableName) {
		frag.add(DLabel, variableName);
		frag.add(DataZ, 4);
	}
	
	/** [... baseLocation] -> [... intValue]
	 * @param frag ASMCodeFragment to add code to
	 * @param offset amount to add to the base location before reading
	 */
	public static void readIOffset(ASMCodeFragment frag, int offset) {
		frag.add(PushI, offset);	// [base offset]
		frag.add(Add);				// [base+off]
		frag.add(LoadI);			// [*(base+off)]
	}
	/** [... baseLocation] -> [... charValue]
	 * @param frag ASMCodeFragment to add code to
	 * @param offset amount to add to the base location before reading
	 */
	public static void readCOffset(ASMCodeFragment frag, int offset) {
		frag.add(PushI, offset);	// [base offset]
		frag.add(Add);				// [base+off]
		frag.add(LoadC);			// [*(base+off)]
	}
	/** [... intToWrite baseLocation] -> [...]
	 * @param frag ASMCodeFragment to add code to
	 * @param offset amount to add to the base location before writing 
	 */
	public static void writeIOffset(ASMCodeFragment frag, int offset) {
		frag.add(PushI, offset);	// [datum base offset]
		frag.add(Add);				// [datum base+off]
		frag.add(Exchange);			// [base+off datum]
		frag.add(StoreI);			// []
	}
	
	/** [... charToWrite baseLocation] -> [...]
	 * @param frag ASMCodeFragment to add code to
	 * @param offset amount to add to the base location before writing 
	 */
	public static void writeCOffset(ASMCodeFragment frag, int offset) {
		frag.add(PushI, offset);	// [datum base offset]
		frag.add(Add);				// [datum base+off]
		frag.add(Exchange);			// [base+off datum]
		frag.add(StoreC);			// []
	}
	
	
	////////////////////////////////////////////////////////////////////
    // debugging aids

	// does not disturb stack.  Takes a format string - no %'s!
	public static void printString(ASMCodeFragment code, String format) {
		String stringLabel = ASMCodeGenerator.getLabeller().newLabel("pstring-", "");
		code.add(DLabel, stringLabel);
		code.add(DataS, format);
		code.add(PushD, stringLabel);
		code.add(Printf);
	}
	// does not disturb stack.  Takes a format string
	public static void printStackTop(ASMCodeFragment code, String format) {
		code.add(Duplicate);
		String stringLabel = ASMCodeGenerator.getLabeller().newLabel("ptop-", "");
		code.add(DLabel, stringLabel);
		code.add(DataS, format);
		code.add(PushD, stringLabel);
		code.add(Printf);
	}
	public static void printStack(ASMCodeFragment code, String string) {
		String stringLabel = ASMCodeGenerator.getLabeller().newLabel("pstack-", "");
		code.add(DLabel, stringLabel);
		code.add(DataS, string + " ");
		code.add(PushD, stringLabel);
		code.add(Printf);
		code.add(PStack);
	}
	
	// [... ptr] -> [... ptr]
	public static void printPtrAndRefcount(ASMCodeFragment code, String prefix) {
		printStackTop(code, prefix + " ptr: %d ");		// ptr
		code.add(Duplicate);	// [... ptr ptr]
		readCOffset(code, 8);
		printStackTop(code, "refcount: <%d>\n");
		code.add(Pop);			// [... ptr]
	}
	
	public static void printPtrAndStatusCode(ASMCodeFragment code, String prefix) {
		printStackTop(code, prefix + " ptr: %d ");		// ptr
		code.add(Duplicate);	// [... ptr ptr]
		readIOffset(code, 4);
		printStackTop(code, "status: <%d>\n");
		code.add(Pop);			// [... ptr]
	}
	
	public static void printPtrAndTypeId(ASMCodeFragment code, String prefix) {
		printStackTop(code, prefix + " ptr: %d ");		// ptr
		code.add(Duplicate);	// [... ptr ptr]
		code.add(LoadI);
		printStackTop(code, "typeId: <%d>\n");
		code.add(Pop);			// [... ptr]
	}
	
	
	
	// [...] -> [...]
	public static void printFramePointer(ASMCodeFragment code, String prefix) {
		code.add(PushD, RunTime.FRAME_POINTER);
		code.add(LoadI);
		printString(code, prefix + " FramePointer: ");
		printStackTop(code, "%d");
		code.add(Pop);
		printString(code, "\n");

	}
	
	public static void printStackPointer(ASMCodeFragment code, String prefix) {
		code.add(PushD, RunTime.STACK_POINTER);
		code.add(LoadI);
		printString(code, prefix + " StackPointer: ");
		printStackTop(code, "%d");
		code.add(Pop);
		printString(code, "\n");
	}
	
	public static void printPointer(ASMCodeFragment code, String location, String prefix) {
		code.add(PushD, location);
		code.add(LoadI);
		printString(code, prefix + ":");
		printStackTop(code, "%d");
		code.add(Pop);
		printString(code, "\n");
	}
	
}

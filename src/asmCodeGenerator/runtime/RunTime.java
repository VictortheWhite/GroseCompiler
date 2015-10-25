package asmCodeGenerator.runtime;
import static asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
public class RunTime {
	public static final String EAT_LOCATION_ZERO      = "$eat-location-zero";		// helps us distinguish null pointers from real ones.
	public static final String INTEGER_PRINT_FORMAT   = "$print-format-integer";
	public static final String FLOATING_PRINT_FORMAT  = "$print-format-float";
	public static final String BOOLEAN_PRINT_FORMAT   = "$print-format-boolean";
	public static final String CHARACTER_PRINT_FORMAT = "$print-format-character";
	public static final String STRING_PRINT_FORMAT 	  = "$print-format-string";
	public static final String NEWLINE_PRINT_FORMAT   = "$print-format-newline";
	public static final String SEPARATOR_PRINT_FORMAT = "$print-format-separator";
	public static final String BOOLEAN_TRUE_STRING    = "$boolean-true-string";
	public static final String BOOLEAN_FALSE_STRING   = "$boolean-false-string";
	public static final String GLOBAL_MEMORY_BLOCK    = "$global-memory-block";
	public static final String USABLE_MEMORY_START    = "$usable-memory-start";
	public static final String MAIN_PROGRAM_LABEL     = "$$main";
	
	public static final String STRING_CONCA_ARG1	  = "$string-concatenation-arg1";
	public static final String STRING_CONCA_ARG2	  = "$string-concatenation-arg2";
	public static final String STRING_CONCATENATION  = "$$string-concatenation-start";
	
	public static final String GENERAL_RUNTIME_ERROR = "$$general-runtime-error";
	public static final String INTEGER_DIVIDE_BY_ZERO_RUNTIME_ERROR = "$$i-divide-by-zero";
	public static final String FLOATING_DIVIDE_BY_ZERO_RUNTIME_ERROR = "$$f-divide-by-zero";
	public static final String ARRAY_INDEXING_OUT_BOUND_ERROR = "$$array-indexing-index-out-bound";
	public static final String ARRAY_EMPTY_CREATION_SIZE_NEGATIVE_ERROR ="$$array-negative-size-creation";

	private ASMCodeFragment environmentASM() {
		ASMCodeFragment result = new ASMCodeFragment(GENERATES_VOID);
		result.append(jumpToMain());
		result.append(stringsForPrintf());
		result.append(runtimeErrors());
		result.append(stringConcatenationSubRoutine());
		result.add(DLabel, USABLE_MEMORY_START);
		return result;
	}
	
	private ASMCodeFragment jumpToMain() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		frag.add(Jump, MAIN_PROGRAM_LABEL);
		return frag;
	}
	
	private ASMCodeFragment stringsForPrintf() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		frag.add(DLabel, EAT_LOCATION_ZERO);
		frag.add(DataZ, 8);
		frag.add(DLabel, INTEGER_PRINT_FORMAT);
		frag.add(DataS, "%d");
		frag.add(DLabel, FLOATING_PRINT_FORMAT);
		frag.add(DataS, "%g");
		frag.add(DLabel, CHARACTER_PRINT_FORMAT);
		frag.add(DataS, "%c");
		frag.add(DLabel, STRING_PRINT_FORMAT);
		frag.add(DataS, "%s");
		frag.add(DLabel, BOOLEAN_PRINT_FORMAT);
		frag.add(DataS, "%s");
		frag.add(DLabel, NEWLINE_PRINT_FORMAT);
		frag.add(DataS, "\n");
		frag.add(DLabel, SEPARATOR_PRINT_FORMAT);
		frag.add(DataS, " ");
		frag.add(DLabel, BOOLEAN_TRUE_STRING);
		frag.add(DataS, "true");
		frag.add(DLabel, BOOLEAN_FALSE_STRING);
		frag.add(DataS, "false");
		
		return frag;
	}
	
	private ASMCodeFragment stringConcatenationSubRoutine() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VALUE);
		String arg1_Start = "string-concatenation-arg1-loop-start";
		String arg1_End = "string-concatenation-arg1-loop-end";
		String arg2_Start = "string-concatenation-arg2-loop-start";
		String arg2_End = "string-concatenation-arg2-loop-end";
		frag.add(DLabel, STRING_CONCA_ARG1);		// memory location reserved for arg1
		frag.add(DataI, 0);
		frag.add(DLabel, STRING_CONCA_ARG2);		// memory location reserved for arg2
		frag.add(DataI, 0);
		
		
		frag.add(Label, STRING_CONCATENATION);		// [...R]	concatenation start (R being return address)
		
		// calculate length(C) 
		frag.add(PushD, STRING_CONCA_ARG1);
		frag.add(LoadI);							// [...R A]	A is arg1
		frag.add(PushI, 9);
		frag.add(Add);
		frag.add(LoadI); 							// [...R len(A)]
		frag.add(PushD, STRING_CONCA_ARG2);
		frag.add(LoadI); 							// [...R len(A) B]
		frag.add(PushI, 9); 		
		frag.add(Add);
		frag.add(LoadI); 							// [...R len(A) len(B)]
		frag.add(Add); 								// string length of C

		// allocate memory
		frag.add(PushI, 13+1 );						// header + terminator
		frag.add(Add);								// [...R size(C)]
		frag.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE); 	// [...R C]


		// set header of C
		frag.add(Duplicate);
		frag.add(Duplicate); 						// [...R C C C]
		frag.add(PushI, 10);						// [...R C C C 10]
		frag.add(StoreI);							// [...R C C]	Type-identifier added
		frag.add(PushI, 4);
		frag.add(Add); 								// [...R C C*]	( C* denotes currentAddr being processed)
		frag.add(Duplicate); 						// [...R C C* C*]
		frag.add(PushI, 1);							// [...R C C* C* 1]		status 001 
		frag.add(StoreI); 							// [...R C C*]
		frag.add(PushI, 4); 		
		frag.add(Add); 								// [...R C C*]		C* += 4
		frag.add(Duplicate); 						// [...R C C* C*]
		frag.add(PushI, 0); 						// [...R C C* C* 0]
		frag.add(StoreC); 							// [...R C C*]
		frag.add(PushI, 1); 								
		frag.add(Add); 								// [...R C C*]		C*+=1
		frag.add(Duplicate);						// [...R C C* C*]
		// calculate length(C)
		// may be redundant
		frag.add(PushD, STRING_CONCA_ARG1);
		frag.add(LoadI);							
		frag.add(PushI, 9);
		frag.add(Add);
		frag.add(LoadI); 							
		frag.add(PushD, STRING_CONCA_ARG2);
		frag.add(LoadI); 							
		frag.add(PushI, 9); 		
		frag.add(Add);
		frag.add(LoadI); 							
		frag.add(Add);
		// end of calculate length(C)				   [...R C C* C* len(C)]
		frag.add(StoreI); 							// [...R C C*]
		frag.add(PushI, 4);
		frag.add(Add); 								// [...R C C*]	C*+=4 header finished

		
		
		// copy A to C
		frag.add(PushD, STRING_CONCA_ARG1);
		frag.add(LoadI);
		frag.add(PushI, 13);
		frag.add(Add); 								// [...R C C* A*]	A* is now string start
		frag.add(PushD, STRING_CONCA_ARG1); 		// [...R C C* A* arg1]
		frag.add(Exchange); 						// [...R C C* arg1 A*]
		frag.add(StoreI); 							// [...R C C*]		
		frag.add(Label, arg1_Start);				// loop start
		frag.add(Duplicate); 						// [...R C C* C*]
		frag.add(PushD, STRING_CONCA_ARG1);
		frag.add(LoadI); 							// [...R C C* C* A*]
		frag.add(LoadC); 							// [...R C C* C* A[i]]
		frag.add(Duplicate); 						// [...R C C* C* A[i] A[i]]
		frag.add(JumpFalse, arg1_End);				// [...R C C* C* A[i]]
		frag.add(StoreC); 							// [...R C C*]
		frag.add(PushI, 1);
		frag.add(Add); 								// [...R C C*] 		C* += 1
		frag.add(PushD, STRING_CONCA_ARG1);
		frag.add(Duplicate);						// [...R C C* arg1 arg1]
		frag.add(LoadI);
		frag.add(PushI, 1);
		frag.add(Add);								// [...R C C* arg1 A*] 	A* +=1
		frag.add(StoreI);
		frag.add(Jump, arg1_Start);
		frag.add(Label, arg1_End);	
		frag.add(Pop);
		frag.add(Pop); 								// [...R C C*]
		

		// copy B to C
		frag.add(PushD, STRING_CONCA_ARG2);
		frag.add(LoadI);
		frag.add(PushI, 13);
		frag.add(Add); 								// [...R C C* B*]	B* is now string start
		frag.add(PushD, STRING_CONCA_ARG2); 		// [...R C C* B* arg2]
		frag.add(Exchange); 						// [...R C C* arg2 B*]
		frag.add(StoreI); 							// [...R C C*]
		frag.add(Label, arg2_Start);				// loop start
		frag.add(Duplicate); 						// [...R C C* C*]
		frag.add(PushD, STRING_CONCA_ARG2);
		frag.add(LoadI); 							// [...R C C* C* B*]
		frag.add(LoadC); 							// [...R C C* C* B[i]]
		frag.add(Duplicate); 						// [...R C C* C* B[i] B[i]]
		frag.add(JumpFalse, arg2_End);				// [...R C C* C* B[i]]
		frag.add(StoreC); 							// [...R C C*]
		frag.add(PushI, 1);
		frag.add(Add); 								// [...R C C*] 		C* += 1
		frag.add(PushD, STRING_CONCA_ARG2);
		frag.add(Duplicate);						// [...R C C* arg2 arg2]
		frag.add(LoadI);
		frag.add(PushI, 1);
		frag.add(Add);								// [...R C C* arg2 B*] 	B* +=1
		frag.add(StoreI);
		frag.add(Jump, arg2_Start);
		frag.add(Label, arg2_End);
		frag.add(Pop);
		frag.add(Pop); 								// [...R C C*]
		

		
		// add terminator
		frag.add(PushI, 0);
		frag.add(StoreC); 							// [...R C]
		frag.add(Exchange); 						// [...C R]
		frag.add(PopPC);							// return
		return frag;
	}
	
	private ASMCodeFragment runtimeErrors() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		
		generalRuntimeError(frag);
		integerDivideByZeroError(frag);
		floatingDivideByZeroError(frag);
		arrayIndexingOutBoundError(frag);
		arrayNegativeSizeCreationError(frag);
		
		return frag;
	}
	private ASMCodeFragment generalRuntimeError(ASMCodeFragment frag) {
		String generalErrorMessage = "$errors-general-message";

		frag.add(DLabel, generalErrorMessage);
		frag.add(DataS, "Runtime error: %s\n");
		
		frag.add(Label, GENERAL_RUNTIME_ERROR);
		frag.add(PushD, generalErrorMessage);
		frag.add(Printf);
		frag.add(Halt);
		return frag;
	}
	private void integerDivideByZeroError(ASMCodeFragment frag) {
		String intDivideByZeroMessage = "$errors-int-divide-by-zero";
		
		frag.add(DLabel, intDivideByZeroMessage);
		frag.add(DataS, "integer divide by zero");
		
		frag.add(Label, INTEGER_DIVIDE_BY_ZERO_RUNTIME_ERROR);
		frag.add(PushD, intDivideByZeroMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}
	private void floatingDivideByZeroError(ASMCodeFragment frag) {
		String floatingDivideByZeroMessage = "$errors-floating-divide-by-zero";
		
		frag.add(DLabel, floatingDivideByZeroMessage);
		frag.add(DataS, "floating divide by zero");
		
		frag.add(Label, FLOATING_DIVIDE_BY_ZERO_RUNTIME_ERROR);
		frag.add(PushD, floatingDivideByZeroMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}
	private void arrayIndexingOutBoundError(ASMCodeFragment frag) {
		String arrayIndexingOutBoundMessage = "$errors-array-indexing-out-of-bound";
		
		frag.add(DLabel, arrayIndexingOutBoundMessage);
		frag.add(DataS, "array indexing out of bound");
		
		frag.add(Label, ARRAY_INDEXING_OUT_BOUND_ERROR);
		frag.add(PushD, arrayIndexingOutBoundMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}
	
	private void arrayNegativeSizeCreationError(ASMCodeFragment frag) {
		String arrayNegativeSizeCreationErrorMessage = "$errors-array-negative-creation";
		
		frag.add(DLabel, arrayNegativeSizeCreationErrorMessage);
		frag.add(DataS, "array size cannnot be negative");
		
		frag.add(Label, ARRAY_EMPTY_CREATION_SIZE_NEGATIVE_ERROR);
		frag.add(PushD, arrayNegativeSizeCreationErrorMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}

	
	
	
	public static ASMCodeFragment getEnvironment() {
		RunTime rt = new RunTime();
		return rt.environmentASM();
	}
}

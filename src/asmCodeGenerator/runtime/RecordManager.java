package asmCodeGenerator.runtime;

import static asmCodeGenerator.Macros.*;
import static asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import java.util.ArrayList;
import java.util.List;

import semanticAnalyzer.SemanticAnalyzer;
import semanticAnalyzer.types.TupleType;
import semanticAnalyzer.types.Type;
import symbolTable.Binding;
import symbolTable.FunctionBinding;
import symbolTable.Scope;
import symbolTable.SymbolTable;
import symbolTable.TupleBinding;
import asmCodeGenerator.ASMCodeGenerator;
import asmCodeGenerator.Labeller;
import asmCodeGenerator.Macros;
import asmCodeGenerator.codeStorage.ASMCodeFragment;

public class RecordManager {
	/*
	 * this recorder manager has methods to maintain reference count
	 * and initialization of tuple arrtributes table
	 * and to-be-checked list
	 * and real deallocate module
	 */
	private static final String TUPLE_MASTER_TABLE = "$tuple-attribute-table-address-table";				//store off
	private static final String TO_BE_CHECKED_LIST = "$to-be-checked-list-table-start";
	private static final String TO_BE_CHECKED_LIST_SIZE = "$to-be-ckecked-list-table-size";					// in terms of 4byte
	private static final String DEALLOCATE_CHECKLIST = "deallocate-checked-list-table";
	
	private static Labeller labeller = ASMCodeGenerator.getLabeller();
	
	
	// initialization
	public static ASMCodeFragment codeForInitialization() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		
		frag.append(initializeTupleAttributeTable());
		frag.append(initializeToBeCheckedTable());
		frag.append(deallocateSubRoutine());
		return frag;
	}
	
	private static ASMCodeFragment initializeTupleAttributeTable() {
		/*
		 * master table stores address of different Tables
		 * tuple Attribute table entry = $master-table + (typeID-100)*4
		 * each tuple attribute table stores the offset of reference type, -1 as terminator
		 */
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		List<Binding> tupleBindings = getTupleBindings();
		if(tupleBindings.size() == 0 ) {
			return frag;
		}
		
		// construct master table
		frag.add(DLabel, TUPLE_MASTER_TABLE);
		for(Binding binding: tupleBindings) {
			TupleType type = (TupleType)binding.getType();
			frag.add(DataD, type.getAttributeTableLabel());
		}
		
		// construct tables for each tuple
		for(Binding binding: tupleBindings) {
			TupleType type = (TupleType)binding.getType();
			frag.add(DLabel, type.getAttributeTableLabel());
			for(Binding attributeBinding: type.getSymbolTable().values()) {
				if(attributeBinding.getType().isReferenceType()) {
					int offset = attributeBinding.getMemoryLocation().getOffset();
					frag.add(DataI, offset);
				}
			}
			frag.add(DataI, -1);	// terminator
		}
			
		return frag;
	}

	private static ASMCodeFragment initializeToBeCheckedTable() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);

		frag.add(DLabel, TO_BE_CHECKED_LIST_SIZE);
		frag.add(DataI, 0);
		
		frag.add(DLabel, TO_BE_CHECKED_LIST);
		frag.add(DataZ, 600);					// to-be-checked list capacity is 150
		
		return frag;
	}
	
	
	// Deallocate Module
	private static ASMCodeFragment deallocateSubRoutine() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		String ItrLocation = "$record-manager-deallocate-itr-adr";
		String checkListLoopStart = "$record-manager-deallocate-checklist-loop-start";
		String checkListLoopContinue = "$record-manager-deallocate-chekclist-loop-continue";
		String checkListLoopEnd = "$record-manager-deallocate-checklist-loop-end";
		
		String deallocateStringLabel = "$record-manager-deallocate-string";
		String deallocateArrayLabel = "$record-manager-deallocate-array";
		String deallocateTupleLabel = "$record-manager-deallocate-tuple";
		
		String deallocateTupleItrLabel = "$record-manager-deallocate-tuple-itr";

		
		frag.add(DLabel, ItrLocation);
		frag.add(PushI, 0);
		frag.add(DLabel, deallocateTupleItrLabel);
		frag.add(PushI, 0);
		
		frag.add(Label, DEALLOCATE_CHECKLIST);
		
		frag.add(Label, checkListLoopStart);
		Macros.loadIFrom(frag, TO_BE_CHECKED_LIST_SIZE);
		Macros.loadIFrom(frag, ItrLocation);
		frag.add(Subtract);
		frag.add(JumpFalse, checkListLoopEnd);		// if size - itr ==0 jump end
		// real looping start
		frag.add(PushD, TO_BE_CHECKED_LIST);
		Macros.loadIFrom(frag, ItrLocation);
		frag.add(PushI, 4);
		frag.add(Multiply);							// record location: tableHead + 4*itr
		frag.add(LoadI);							// [...adr]
		// jump to continue if do-not-dispose
		frag.add(Duplicate);
		Macros.readCOffset(frag, 4);
		frag.add(PushI, 4);
		frag.add(BTAnd);
		frag.add(JumpTrue, checkListLoopContinue);
		// jump to continue if refcount > 0
		frag.add(Duplicate);
		Macros.readCOffset(frag, 8);
		frag.add(JumpTrue, checkListLoopContinue);
		// jump to string deallocation
		frag.add(Duplicate);
		frag.add(LoadI);							// typeId
		frag.add(PushI, 10);
		frag.add(Subtract);
		frag.add(JumpFalse, deallocateStringLabel);
		// jump to array deallocation
		frag.add(Duplicate);
		frag.add(LoadI);
		frag.add(PushI, 9);
		frag.add(Subtract);
		frag.add(JumpFalse, deallocateArrayLabel);
		// jump to tuple deallocation
		frag.add(Duplicate);
		frag.add(LoadI);
		frag.add(PushI, 100);
		frag.add(JumpNeg, deallocateTupleLabel);
		// gives error if not string, array or tuple
		frag.add(Call, RunTime.GENERAL_RUNTIME_ERROR); 
		
		// deallocate String
		frag.add(Label, deallocateStringLabel);
		deallocateRecord(frag);
		frag.add(Jump, checkListLoopContinue);
		
		// deallocate Array
		frag.add(Label, deallocateArrayLabel);
		deallocateArray(frag);
		frag.add(Jump, checkListLoopContinue);
		
		// deallocate Tuple
		frag.add(Label, deallocateTupleLabel);
		deallocateTuple(frag, deallocateTupleItrLabel);
		frag.add(Jump, checkListLoopContinue);
		
		// loop continue
		frag.add(Label, checkListLoopContinue);		// adr
		frag.add(Pop);								
		Macros.incrementInteger(frag, ItrLocation);	// [...] itr++
		frag.add(Jump, checkListLoopStart);
		// loop end
		frag.add(Label, checkListLoopEnd);
		
		// reset size and itr to 0
		frag.add(PushI, 0);
		Macros.storeITo(frag, TO_BE_CHECKED_LIST_SIZE);
		frag.add(PushI, 0);
		Macros.storeITo(frag, ItrLocation);
		
		return frag;
	}
	
	private static void deallocateArray(ASMCodeFragment frag) {
		String subElementLoopStart = "record-manager-deallocate-array-subelement-start";
		String subElementLoopEnd = "record-manager-deallocate-array-subelement-end";
		
		// [...adr]
		
		// if subType is not reference type
		frag.add(Duplicate);
		Macros.readIOffset(frag, 4);		// status code
		frag.add(PushI, 2);
		frag.add(BTAnd);
		frag.add(JumpFalse, subElementLoopEnd);
		
		// initialize loop invariant
		frag.add(Duplicate);
		frag.add(Duplicate);				
		Macros.readIOffset(frag, 13);				// [...adr adr n]
		frag.add(Exchange);
		frag.add(PushI, 17);
		frag.add(Add);
		frag.add(Exchange);							// [...adr adr* n]
		
		// loop start
		frag.add(Label, subElementLoopStart);
		frag.add(Duplicate);
		frag.add(JumpFalse, subElementLoopEnd);		// if n == 0, jump end
		frag.add(Exchange);							// [...adr n adr*]
		// decrement refcount of subElement
		frag.add(Duplicate);						// [...adr n adr* adr*]
		frag.add(LoadI);							// [...adr n adr* subElement]
		decrementRefcount(frag);
		frag.add(Pop);								// [...adr n adr*]
		
		// maintain loop invariant
		frag.add(PushI, 4);
		frag.add(Add);
		frag.add(Exchange);
		frag.add(PushI, -1);
		frag.add(Add);								// [...adr adr* n]	n--, adr* += 4
		frag.add(Jump, subElementLoopStart);
		
		// loop end
		frag.add(Label, subElementLoopEnd);
		// [...adr n adr*]
		frag.add(Pop);
		frag.add(Pop);
		
		
		// deallocate
		// [...adr]
		deallocateRecord(frag);
	}
	
	private static void deallocateTuple(ASMCodeFragment frag, String itrLabel) {
		String attributeLoopStart = "record-manager-deallocate-tuple-attribute-start";
		String attributeLoopEnd = "record-manager-deallocate-tuple-attribute-end";
		
		// [...adr]
		
		// jump to loop end subType is not reference type
		frag.add(Duplicate);
		Macros.readIOffset(frag, 4);
		frag.add(PushI, 2);
		frag.add(BTAnd);
		frag.add(JumpFalse, attributeLoopEnd);
		
		// initialize loop invariant
		frag.add(PushD, itrLabel);
		frag.add(PushI, 0);
		frag.add(StoreI);
		
		// loop start
		frag.add(Label, attributeLoopStart);
		// get offset
		// [...adr]
		frag.add(Duplicate);
		frag.add(Duplicate);				// [...adr adr]
		frag.add(LoadI);					// [...adr adr typeId]
		frag.add(PushI, 100);
		frag.add(Subtract);
		frag.add(PushI, 4);
		frag.add(Multiply);
		frag.add(PushD, TUPLE_MASTER_TABLE);
		frag.add(Add);						
		frag.add(LoadI);					// [...adr adr attriTableStart]
		Macros.loadIFrom(frag, itrLabel);
		frag.add(PushI, 4);
		frag.add(Multiply);
		frag.add(Add);
		frag.add(LoadI);					// [...adr adr offset]
		// if offset == -1. jump end
		frag.add(Duplicate);
		frag.add(PushI, -1);
		frag.add(Subtract);
		frag.add(JumpFalse, attributeLoopEnd);
		// decrement refcount of attir
		frag.add(Add);						// [...adr attriAdr]
		frag.add(LoadI);					// [...adr attir]
		decrementRefcount(frag);
		// increment itr
		Macros.incrementInteger(frag, itrLabel);
		frag.add(Jump, attributeLoopStart);
		
		frag.add(Label, attributeLoopEnd);	
		
		// deallocate Record
		deallocateRecord(frag);
		
		// [...adr]
		
	}
	
	private static void deallocateRecord(ASMCodeFragment frag) {
		frag.add(Duplicate);	
		frag.add(PushI, 0);		
		frag.add(StoreI);	// erase typeId
		frag.add(Call, MemoryManager.MEM_MANAGER_DEALLOCATE);
	}
	
	
	//------------------------------------------------------------
	// Macros for reference counting
	
	
	
	// Macros on referenceCounting
	// compile time
	public static void incrementRefcount(ASMCodeFragment code) {
		/* 
		 * increment refcount
		 * if refcount reaches 128
		 * (skip if ref is null)
		 */
		
		String endLabel = labeller.newLabel("record-manager-increment-refcount-end", "");
		// jump to end if null
		code.add(Duplicate);
		code.add(JumpFalse, endLabel);
		
		// [...ref]
		// increment refcount
		code.add(Duplicate);
		code.add(Duplicate);
		code.add(PushI, 8);
		code.add(Add);
		code.add(LoadC);		// [...ref ref refcount]
		code.add(PushI, 1);
		code.add(Add);
		code.add(StoreC);		
		
		// [...ref]
		// set do-not-dispose bit to one when needed
		code.add(Duplicate);
		Macros.readCOffset(code, 8);
		code.add(PushI, 128);
		code.add(Subtract);
		code.add(JumpFalse, endLabel);
		// if refcount is 128, do following
		setDoNotDiopose(code);
		
		// end label
		code.add(Label, endLabel);
	}
	private static void setDoNotDiopose(ASMCodeFragment code) {
		// [...ref]
		code.add(Duplicate);
		code.add(Duplicate);
		Macros.readIOffset(code, 4);	//[...ref ref statusCode]
		code.add(PushI, 4);
		code.add(BTOr);
	}
	
	public static void decrementRefcount(ASMCodeFragment code) {
		/* decrement refcount by 1
		 * add to checklist if refcount is 0
		 * (skip if ref is null)
		 */
		
		String endLabel = labeller.newLabel("record-manager-decrement-refount-end", "");
		
		// jump to end if null
		code.add(Duplicate);
		code.add(JumpFalse, endLabel);
		
		// [...ref]
		// decrement refcount
		code.add(Duplicate);
		code.add(Duplicate);			// [...ref ref ref]
		Macros.readCOffset(code, 8);	// [...ref ref refcount]
		code.add(StoreC);				
		
		// [...ref]
		// add to checklist if needed
		code.add(Duplicate);
		Macros.readCOffset(code, 8);	// [...ref refcount]
		code.add(JumpTrue, endLabel);
		// if refcount is 0, do fllowing
		addToCheckList(code);
		
		// end label
		code.add(Label, endLabel);
		
	}
	public static void addToCheckList(ASMCodeFragment code) {
		/* [...ref]
		 * add to checklist
		 * increment checklist size by 1
		 */
		code.add(Duplicate);
		code.add(PushD, TO_BE_CHECKED_LIST);
		Macros.loadIFrom(code, TO_BE_CHECKED_LIST_SIZE);
		code.add(PushI, 4);
		code.add(Add);										//[...ref ref adr]
		code.add(Exchange);									//[...ref adr ref]
		code.add(StoreI);
		// increment size by 1
		Macros.incrementInteger(code, TO_BE_CHECKED_LIST_SIZE);
	}
	
	
	
	
	
	
	// helper methods for initializion
	private static List<Binding> getTupleBindings() {
		Scope globalScope = SemanticAnalyzer.getGlobalScope();
		SymbolTable globalSymbolTable = globalScope.getSymbolTable();
		
		List<Binding> bindings = new ArrayList<Binding>(globalSymbolTable.values());
		List<Binding> tupleTypes = new ArrayList<Binding>();
		
		for(Binding binding: bindings) {
			if((binding instanceof TupleBinding) || (binding instanceof FunctionBinding)) {
				if(binding.getType() instanceof TupleType) {
					tupleTypes.add(binding);
				}
			}
		}
		return tupleTypes;
	}
	
	
}

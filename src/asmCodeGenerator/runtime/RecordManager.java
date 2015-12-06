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
	
	private static Labeller labeller = ASMCodeGenerator.getLabeller();
	
	
	// initialization
	public static ASMCodeFragment codeForInitialization() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		
		frag.append(initializeTupleAttributeTable());
		frag.append(initializeToBeCheckedTable());
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
		
		return frag;
	}
	
	
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
	private static void addToCheckList(ASMCodeFragment code) {
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

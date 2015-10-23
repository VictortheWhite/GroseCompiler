package asmCodeGenerator;

import parseTree.nodeTypes.StringConstantNode;
import parseTree.nodeTypes.PopulatedArrayNode;
import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.PrimitiveType;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;

public class Header {
		
	// string header
	public void addHeader(ASMCodeFragment code, StringConstantNode node) {
		code.add(ASMOpcode.DataI, 10);						// 4bytes, Type Identifier
		code.add(ASMOpcode.DataI, getStatusCode(node));		// 4bytes, Status
		code.add(ASMOpcode.DataC, 0);  						// 1bytes, Refcount (not used in grouse-2)
		code.add(ASMOpcode.DataI, node.getValue().length());// 4bytes, length
	}
	
	private int getStatusCode(StringConstantNode node) {
		int StatusCode = 0;
		StatusCode += 1;			//set immutability to 1
		StatusCode += 0 * 2;		//set subtype-is-reference to 0
		StatusCode += 1 * 4;		//set do-not-dispose to 1
		
		return StatusCode;
	}
	
	// array header
	// this function operates when the address of memory allocated already on top of the stack
	// [...adr]
	public void addHeader(ASMCodeFragment code, PopulatedArrayNode node) {
		code.add(ASMOpcode.Duplicate);
		
		code.add(ASMOpcode.Duplicate);						// [...adr adr]
		code.add(ASMOpcode.PushI, 9);						// 4bytes, Type Identifier
		code.add(ASMOpcode.StoreI);							
		code.add(ASMOpcode.PushI, 4);
		code.add(ASMOpcode.Add);							// [...adr adr+4]
		
		code.add(ASMOpcode.Duplicate);
		code.add(ASMOpcode.PushI, getStatusCode(node));		// 4bytes, Status
		code.add(ASMOpcode.StoreI);							
		code.add(ASMOpcode.PushI, 4);
		code.add(ASMOpcode.Add);							//	[...adr adr+8]
		
		code.add(ASMOpcode.Duplicate);
		code.add(ASMOpcode.PushI, 0);						// 1bytes, Refcount (not used in grouse-2)
		code.add(ASMOpcode.StoreC);
		code.add(ASMOpcode.PushI, 1);
		code.add(ASMOpcode.Add);							// [...adr adr+9]
		
		code.add(ASMOpcode.Duplicate);
		code.add(ASMOpcode.PushI, ((ArrayType)node.getType()).getSubTypeSize());	// 4bytes, size of subType
		code.add(ASMOpcode.StoreI);
		code.add(ASMOpcode.PushI, 4);
		code.add(ASMOpcode.Add);							// [...adr adr+13]
		
		code.add(ASMOpcode.PushI, node.nChildren());		// 4bytes, length( the number of elements of the array)
		code.add(ASMOpcode.StoreI);							// [...adr]
	}
	private int getStatusCode(PopulatedArrayNode node) {
		int StatusCode = 0;
		int isSubTypeReference = 0;
		
		if(node.getType() == PrimitiveType.STRING) 
			isSubTypeReference = 1;
		if(node.getType() instanceof ArrayType)
			isSubTypeReference = 1;
		
		StatusCode += 0;						// may be mutated
		StatusCode += isSubTypeReference * 2;	// set to 1 if subtype is reference type, otherwise 0
		StatusCode += 0 * 4;					// set do-not-dispose to 1
		
		return StatusCode;
	}
	
	
	
}

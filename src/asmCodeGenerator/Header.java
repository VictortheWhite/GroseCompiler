package asmCodeGenerator;

import parseTree.nodeTypes.FreshArrayNode;
import parseTree.nodeTypes.StringConstantNode;
import parseTree.nodeTypes.PopulatedArrayNode;
import semanticAnalyzer.types.*;
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
		code.add(ASMOpcode.PushI, getStatusCode(node.getType()));		// 4bytes, Status
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
	public void addHeader(ASMCodeFragment code, FreshArrayNode node, String lengthLabel, ArrayType nodeType) {
		code.add(ASMOpcode.Duplicate);
		
		code.add(ASMOpcode.Duplicate);						// [...adr adr]
		code.add(ASMOpcode.PushI, 9);						// 4bytes, Type Identifier
		code.add(ASMOpcode.StoreI);							
		code.add(ASMOpcode.PushI, 4);
		code.add(ASMOpcode.Add);							// [...adr adr+4]
		
		code.add(ASMOpcode.Duplicate);
		code.add(ASMOpcode.PushI, getStatusCode(nodeType));		// 4bytes, Status
		code.add(ASMOpcode.StoreI);							
		code.add(ASMOpcode.PushI, 4);
		code.add(ASMOpcode.Add);							//	[...adr adr+8]
		
		code.add(ASMOpcode.Duplicate);
		code.add(ASMOpcode.PushI, 0);						// 1bytes, Refcount (not used in grouse-2)
		code.add(ASMOpcode.StoreC);
		code.add(ASMOpcode.PushI, 1);
		code.add(ASMOpcode.Add);							// [...adr adr+9]
		
		code.add(ASMOpcode.Duplicate);
		code.add(ASMOpcode.PushI, nodeType.getSubTypeSize());	// 4bytes, size of subType
		code.add(ASMOpcode.StoreI);
		code.add(ASMOpcode.PushI, 4);
		code.add(ASMOpcode.Add);							// [...adr adr+13]
		
		code.add(ASMOpcode.PushD, lengthLabel);				
		code.add(ASMOpcode.LoadI);							// 4bytes, length( the number of elements of the array)
		code.add(ASMOpcode.StoreI);							// [...adr]
	}
 	
	public void addTupleHeader(ASMCodeFragment code, TupleType tupleType) {
		// [...adr]
		code.add(ASMOpcode.Duplicate);	// [...adr adr]
		code.add(ASMOpcode.PushI, tupleType.getTypeId());
		code.add(ASMOpcode.StoreI);
		
		code.add(ASMOpcode.Duplicate);
		code.add(ASMOpcode.PushI, 4);
		code.add(ASMOpcode.Add);
		code.add(ASMOpcode.PushI, getStatusCode(tupleType));
		code.add(ASMOpcode.StoreI);
		
		code.add(ASMOpcode.Duplicate);
		code.add(ASMOpcode.PushI, 8);
		code.add(ASMOpcode.Add);
		code.add(ASMOpcode.PushI, 0);
		code.add(ASMOpcode.StoreC);
	}
	public int getStatusCode(Type type) {
		int StatusCode = 0;
		int isMutable = 0;
		int isSubTypeReference = 0;
		int doNotDispose = 1;
		
		if(type == PrimitiveType.STRING) {
			isMutable = 1;
			isSubTypeReference = 1;
			doNotDispose = 0;
		}
		if(type instanceof ArrayType)
			isSubTypeReference = 1;
		if(type instanceof TupleType) {
			if(((TupleType)type).subTypeIsReference())
				isSubTypeReference = 1;
		}
		
		
		StatusCode += isMutable;				// may be mutated
		StatusCode += isSubTypeReference * 2;	// set to 1 if subtype is reference type, otherwise 0
		StatusCode += doNotDispose * 4;					// set do-not-dispose to 1
		
		return StatusCode;
	}
	
	
	
}

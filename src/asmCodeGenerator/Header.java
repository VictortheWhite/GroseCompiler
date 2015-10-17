package asmCodeGenerator;

import parseTree.nodeTypes.StringConstantNode;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;

public class Header {
		
	public void addHeader(ASMCodeFragment code, StringConstantNode node) {
		code.add(ASMOpcode.DataI, 10);						// 4bytes, Type Identifier
		code.add(ASMOpcode.DataI, getStatusCode(node));		// 4bytes, Status
		code.add(ASMOpcode.DataC, 0);  						// 1bytes, Refcount (not used in grouse-2)
		code.add(ASMOpcode.DataI, node.getValue().length());// 4bytes, length
	}
	
	
	private int getStatusCode(StringConstantNode node) {
		int StatusCode = 0;
		StatusCode += 0;		//set immutability to 0
		StatusCode += 0 * 2;		//set subtype-is-reference to 0
		StatusCode += 1 * 4;		//set do-not-diopose to 1
		
		return StatusCode;
	}
	
	
	
}

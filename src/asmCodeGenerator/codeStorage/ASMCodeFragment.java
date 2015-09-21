package asmCodeGenerator.codeStorage;

import java.util.ArrayList;
import java.util.List;

public class ASMCodeFragment {
	private List<ASMCodeChunk> chunks;
	
	// these names refer to what the code in the fragment
	// leaves on top of the stack: either nothing, a value
	// (like 5 or 17.4) or an address where the "result" can
	// be found.
	public enum CodeType {
		GENERATES_VOID,
		GENERATES_VALUE,
		GENERATES_ADDRESS;
	}
	CodeType codeType;
	
	public ASMCodeFragment(CodeType codeType) {
		chunks = new ArrayList<ASMCodeChunk>();
		this.codeType = codeType;
	}
	
	public void markAsVoid() {
		codeType = CodeType.GENERATES_VOID;
	}
	public void markAsValue() {
		codeType = CodeType.GENERATES_VALUE;
	}
	public void markAsAddress() {
		codeType = CodeType.GENERATES_ADDRESS;
	}
	public boolean isAddress() {
		return codeType == CodeType.GENERATES_ADDRESS;
	}
	public boolean isValue() {
		return codeType == CodeType.GENERATES_VALUE;
	}
	public boolean isVoid() {
		return codeType == CodeType.GENERATES_VOID;
	}
	
	/** Append all instructions in the argument to this code fragment.
	 *  This does not change the type of this code fragment; you must
	 *  call markAsXXX afterwards if you need that to happen.
	 * @param fragment
	 */
	public void append(ASMCodeFragment fragment) {
		chunks.addAll(fragment.chunks);
	}
	
	public void add(ASMOpcode opcode, int operand, String comment) {
		lastChunk().add(opcode, operand, comment);
	}
	public void add(ASMOpcode opcode, int operand) {
		lastChunk().add(opcode, operand);
	}
	public void add(ASMOpcode opcode, double operand, String comment) {
		lastChunk().add(opcode, operand, comment);
	}
	public void add(ASMOpcode opcode, double operand) {
		lastChunk().add(opcode, operand);
	}
	public void add(ASMOpcode opcode, String operand, String comment) {
		lastChunk().add(opcode, operand, comment);
	}
	public void add(ASMOpcode opcode, String operand) {
		lastChunk().add(opcode, operand);
	}
	public void add(ASMOpcode opcode) {
		lastChunk().add(opcode);
	}

	private ASMCodeChunk lastChunk() {
		if(chunks.size() == 0) {
			newChunk();
		}
		return chunks.get(chunks.size() - 1);
	}

	private void newChunk() {
		ASMCodeChunk chunk = new ASMCodeChunk();
		chunks.add(chunk);
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for(ASMCodeChunk chunk: chunks) {
			buffer.append(chunk.toString());
		}
		return buffer.toString();
	}

}

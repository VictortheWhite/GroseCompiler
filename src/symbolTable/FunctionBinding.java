package symbolTable;

import jdk.nashorn.internal.codegen.FunctionSignature;
import inputHandler.TextLocation;
import semanticAnalyzer.types.Type;

public class FunctionBinding extends Binding{

	private FunctionSignature signature;
	
	public FunctionBinding(Type type, TextLocation location,
			MemoryLocation memoryLocation, String lexeme) {
		super(type, location, memoryLocation, lexeme);
		this.signature = null;
	}
	
	public void setSignature(FunctionSignature funcSignature) {
		this.signature = funcSignature;
	}
	
	public FunctionSignature getSignature() {
		return this.signature;
	}

}
package symbolTable;

import inputHandler.TextLocation;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.types.Type;

public class FunctionBinding extends Binding{

	private FunctionSignature signature;
	private String funcStartLabel;
	private String returnLabel;
	
	public FunctionBinding(Type type, TextLocation location,
			MemoryLocation memoryLocation, String lexeme) {
		super(type, location, memoryLocation, lexeme);
		this.signature = null;
		this.funcStartLabel = "$$function-start-label-" + lexeme;
		this.returnLabel = "$$function-return-label-" + lexeme;
	}
	
	public String getFunctionStartLabel() {
		return this.funcStartLabel;
	}
	
	public String getRetureLabel() {
		return this.returnLabel;
	}
	
	public void setSignature(FunctionSignature functionSignature) {
		this.signature = functionSignature;
	}
	
	public FunctionSignature getSignature() {
		return this.signature;
	}

}
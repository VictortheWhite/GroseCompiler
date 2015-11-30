package symbolTable;

import inputHandler.TextLocation;
import semanticAnalyzer.types.Type;

public class TupleBinding extends Binding{

	public TupleBinding(Type type, TextLocation location,
			MemoryLocation memoryLocation, String lexeme) {
		super(type, location, memoryLocation, lexeme);
	}

}
package symbolTable;
import inputHandler.TextLocation;
import semanticAnalyzer.types.Type;

public class StaticBinding extends Binding{
	
	private String isInitializedLabel;
	private static int n = 0;
	
	public StaticBinding(Type type, TextLocation location,
			MemoryLocation memoryLocation, String lexeme) {
		super(type, location, memoryLocation, lexeme);
		
		this.isInitializedLabel = "$static-variable-isInitialized-" + n++;
	}
	
	public String getIndicatorLabel() {
		return this.isInitializedLabel;
	}

}

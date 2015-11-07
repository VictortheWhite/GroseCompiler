package semanticAnalyzer.types;
import parseTree.nodeTypes.*;
import symbolTable.SymbolTable;

public class TupleType implements Type{
	
	private SymbolTable SymbolTable;					// SymbolTable of tuple sub-Elements if defined by parameterList
	private String refferedName;						// Name of reffered tuple if defined by another tuple
	
	public TupleType() {
		this.SymbolTable = null;
		this.refferedName = null;
	}
	
	public void initialize(TupleDefinitionNode node) {
		this.SymbolTable = node.getScope().getSymbolTable();
	}
	public void initialize(IdentifierNode node) {
		this.refferedName = node.getToken().getLexeme();
	}
	
	
	
	public int getSize() {
		return 4;
	}
	
	public String infoString() {
		if(SymbolTable != null)
			return "TupleType " + SymbolTable.toString();
		if(refferedName!=null)
			return "TupleType " + refferedName;
		return "Not-initialized";
	}
	
}


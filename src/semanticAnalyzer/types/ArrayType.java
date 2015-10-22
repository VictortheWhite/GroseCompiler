package semanticAnalyzer.types;

public class ArrayType implements Type{

	private Type subType;

	public ArrayType(Type subtype) {
		this.subType = subtype;
	}
	public ArrayType() {
		this.subType = null;
	}
	
	

	public Type getSubType() {
		return subType;
	}
	
	public int getSize() {
		return 4;		
	}
	public String infoString() {
		return "Array of " + subType.toString();
	}
}

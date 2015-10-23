package semanticAnalyzer.types;

public class ArrayType implements Type{

	private Type subType;

	public ArrayType(Type subtype) {
		this.subType = subtype;
	}
	public ArrayType() {
		this.subType = null;
	}
	
	@Override
	public boolean equals(Object otherType) {
		assert otherType instanceof Type;
		
		if(!(otherType instanceof ArrayType) || otherType == null) {
			return false;
		} else 
			return subType.equals(((ArrayType)otherType).subType);
	}
	
	public Type getSubType() {
		return subType;
	}
	
	public int getSize() {
		return 4;		
	}
	public int getSubTypeSize() {
		return subType.getSize();
	}
	public String infoString() {
		if(subType instanceof ArrayType) {
			return "Array of " + ((ArrayType)subType).infoString();
		}
		else
			return "Array of " + subType.toString();
	}
}

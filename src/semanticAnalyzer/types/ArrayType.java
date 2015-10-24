package semanticAnalyzer.types;

public class ArrayType implements Type{

	public class TypeVariable {

	}
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
		assert subType != null;
				
		if(otherType instanceof PrimitiveType || otherType == null)
			return false;
		else if(otherType instanceof ArrayType)
			return subType.equals(((ArrayType)otherType).subType);
		else 	//Type Variable
			return otherType.equals(this);	
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

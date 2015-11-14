package semanticAnalyzer.types;

public class TupleTypeVariable extends TypeVariable implements Type{
	
	public TupleTypeVariable(String name) {
		super(name);
	}
	
	@Override
	public boolean equals(Object otherType) {
		assert otherType instanceof Type;
		if(this.type == null) {
			if(!(otherType instanceof TupleType)) {
				return false;
			}
			this.type = (TupleType)otherType;
			return true;
		}
		
		return otherType.equals(this.type);
	}
	
}

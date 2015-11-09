package semanticAnalyzer.types;


public enum PrimitiveType implements Type {
	BOOLEAN(1),
	INTEGER(4),
	FLOATING(8),
	CHARACTER(1),
	STRING(4),
	VOID(0),
	ERROR(0),			// use as a value when a syntax error has occurred
	NO_TYPE(0, "");		// use as a value when no type has been assigned.
	
	private int sizeInBytes;
	private String infoString;
	
	private PrimitiveType(int size) {
		this.sizeInBytes = size;
		this.infoString = toString();
	}
	private PrimitiveType(int size, String infoString) {
		this.sizeInBytes = size;
		this.infoString = infoString;
	}
	
	////////////////////////////////////////////
	// type comparasion
	public boolean equals(TupleType otherType) {
		return otherType.equals(this);
	}
	////////////////////////////////////////////
	public int getSize() {
		return sizeInBytes;
	}
	public String infoString() {
		return infoString;
	}
}

package semanticAnalyzer.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import logging.GrouseLogger;
import parseTree.ParseNode;
import parseTree.nodeTypes.*;
import symbolTable.Binding;
import symbolTable.MemoryAccessMethod;
import symbolTable.PositiveMemoryAllocator;
import symbolTable.SymbolTable;

public class TupleType implements Type{
	
	private String TupleName;
	private SymbolTable symbolTable;					// SymbolTable of tuple sub-Elements if defined by parameterList
	private TupleType refferedType;						// Name of reffered tuple if defined by another tuple
	private String tupleAttributeTable;
	private int typeId;
	
	private static int TypeIdCount = 100;
	
	
	public TupleType(String name) {
		this.TupleName = name;
		this.symbolTable = null;
		this.refferedType = null;
		this.typeId = -1;
		this.tupleAttributeTable = "tuple-" + this.TupleName 
									+ "-attributeTable";
	}
	
	
	// equals
	public boolean equals(Object otherType) {
		if(!(otherType instanceof TupleType)) {
			return false;
		}
		
		if(this == otherType) {
			return true;
		}
		
		if(this.symbolTable == null || ((TupleType)otherType).symbolTable == null) {
			// for path compression
			return this == otherType;
		}
		
		List<Type> otherTupleArgList = ((TupleType)otherType).getParameterList();
		
		return this.checkArugments(otherTupleArgList);
	}
	
	////////////////////////////////////////////////////////////
	// initialize tupleType with symbolTable or other tupleType
	public void initialize(ParseNode node) {
		if(node instanceof ParameterListNode) {
			this.symbolTable = node.getScope().getSymbolTable();
		}
		if(node instanceof TypeNode) {
			assert node.getType() instanceof TupleType;
			this.refferedType = (TupleType)node.getType();
		}
	}
	
	// check Arguments
	public boolean checkArugments(List<Type> arguments) {
		
		assert this.symbolTable != null;
		
		List<Binding> TupleArgsBindings= new ArrayList<Binding>(this.symbolTable.values());
		if(arguments.size() != TupleArgsBindings.size()) {
			return false;
		}
		
		for(int i=0;i<arguments.size();i++) {		
			if(!arguments.get(i).equals(TupleArgsBindings.get(i).getType())) {
				return false;
			}
		}
		
		
		return true;
	}

	// path compression
	public void compressPath() {		
		TupleType current = this;
		List<TupleType> Path= new Vector<TupleType>();
		
		while(current.refferedType != null) {		
			if(Path.contains(current)) {
				CircledTupleDefinitionError();
				return;
			}
			Path.add(current);
			current = current.refferedType;
		}
		this.symbolTable = current.symbolTable;
		assert symbolTable != null;
		
		if(!this.isTrivial()) {
			this.typeId = TypeIdCount++;
		}
	}
	
	// eliminate TrivialTupleType in symbolTable
	public void eliminateTrivialTupleTypeInSymbolTable() {
		boolean needToReallocate = false;
		for(String tupleName : this.symbolTable.keySet()) {
			Binding binding = this.symbolTable.lookup(tupleName);

			if(binding.getType() instanceof ArrayType) {
				((ArrayType)binding.getType()).eliminateTrivialTuple();
			}
			
			if(!(binding.getType() instanceof TupleType)) {
				continue;
			}
			
			needToReallocate = true;
			TupleType type = (TupleType)binding.getType();
			if(type.isTrivial()) {
				binding.setType(type.getTirvialEquvalenceType());
			}			
		}
		if(needToReallocate) {
			this.symbolTable.reallocateMemory(
				new PositiveMemoryAllocator(MemoryAccessMethod.GENERATE_OFFSET_ONLY, 9
				));
		}		
	}
	
	// print symbolTable
	public void printSymbolTable() {
		System.out.println(this.TupleName + " " +this.symbolTable);
	}
	
	
	/////////////////////////////////////////////////////////////
	// attributes
	public int getTypeId() {
		return this.typeId;
	}
	
	public Binding lookup(String name) {
		return this.symbolTable.lookup(name);
	}
	
	public List<Type> getParameterList() {
		List<Binding> TupleArgsBindings= new ArrayList<Binding>(this.symbolTable.values());
		List<Type> result = new ArrayList<Type>();
		
		for(Binding current: TupleArgsBindings) {
			result.add(current.getType());
		}
		return result;
	}
	
	public List<Binding> getBindings() {
		return new ArrayList<Binding>(this.symbolTable.values());
	}
	
	public int getLength() {
		return getParameterList().size();
	}
	
	public int getBytesNeeded() {
		List<Type> typeList = getParameterList();
		int bytes = 9;	// header size
		for(Type type: typeList	) {
			bytes += type.getSize();
		}
		return bytes;
	}

	public boolean isTrivial() {
		return symbolTable.keySet().size() <= 1;
	}
	
	public boolean subTypeIsReference() {
				
		List<Type> typeList = getParameterList();
		for(Type type: typeList	) {
			if(type instanceof ArrayType)
				return true;
			if(type instanceof TupleType)
				return true;
		}	
		return false;
	}
	public Type getTirvialEquvalenceType() {
		assert isTrivial();	
		List<Type> types= getParameterList();
		if(types.isEmpty()) {
			return PrimitiveType.VOID;
		}
		Type resultType = types.get(0);
		
		if(resultType instanceof TupleType) {
			if(((TupleType)resultType).isTrivial()) {
				return ((TupleType)resultType).getTirvialEquvalenceType();
			}
		} 
		/*
		 * not needed since it is undefined
		if(resultType instanceof ArrayType) {
			((ArrayType)resultType).eliminateTrivialTuple();
		}
		*/
		return resultType;
	}

	public SymbolTable getSymbolTable() {
		return this.symbolTable;
	}
	
	public static int getTypeIDCount() {
		return TypeIdCount;
	}
	
	public String getAttributeTableLabel() {
		return this.tupleAttributeTable;
	}
	
	////////////////////////////////////////////////////////////
	// error logging
	public void CircledTupleDefinitionError() {
		GrouseLogger log = GrouseLogger.getLogger("compiler.semanticAnalyzer.identifierNode");
		log.severe("Circled definition of tuple Type: " + this.infoString());
	}
	
	////////////////////////////////////////////////////////////
	// type info
	public boolean isReferenceType() {
		return true;
	}
	
	public int getSize() {
		return 4;
	}
	
	public String infoString() {
		
		/*
		if(this.symbolTable != null)
			return "TupleType[" + this.TupleName + "]\n"+ this.symbolTable.toString();
		if(refferedType!=null)
			return "TupleType" + "[" + this.TupleName + "] "+ refferedType.TupleName;
		
		return "Not-initialized";
		*/
		return "Tuple[" + this.TupleName + "]";
	}
	
}


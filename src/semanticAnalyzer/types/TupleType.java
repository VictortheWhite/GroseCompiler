package semanticAnalyzer.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import logging.GrouseLogger;
import parseTree.ParseNode;
import parseTree.nodeTypes.*;
import symbolTable.Binding;
import symbolTable.SymbolTable;

public class TupleType implements Type{
	
	private String TupleName;
	private SymbolTable symbolTable;					// SymbolTable of tuple sub-Elements if defined by parameterList
	private TupleType refferedType;						// Name of reffered tuple if defined by another tuple
	private int typeId;
	
	private static int TypeIdCount = 100;
	
	
	public TupleType(String name) {
		this.TupleName = name;
		this.symbolTable = null;
		this.refferedType = null;
		this.typeId = TypeIdCount++;
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
	
	public boolean checkArugments(List<Type> arguments) {
		if(!compressPath()) {
			return false;
		}
		
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
	
	
	/////////////////////////////////////////////////////////////
	// attributes
	public int getTypeId() {
		return this.typeId;
	}
	
	public List<Type> getParameterList() {
		List<Binding> TupleArgsBindings= new ArrayList<Binding>(this.symbolTable.values());
		List<Type> result = new ArrayList<Type>();
		
		for(Binding current: TupleArgsBindings) {
			result.add(current.getType());
		}
		return result;
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
			Type realType = type;
			if(type instanceof TupleType)  {
				if(((TupleType)type).isTrivial()) {
					realType = ((TupleType)type).getTirvialEquvalenceType();
				}
			}
			if(realType instanceof ArrayType)
				return false;
			if(realType instanceof TupleType)
				return false;
		}	
		return true;
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
		return resultType;
	}
	//////////////////////////////////////////////////////////////
	// path compression
	private boolean compressPath() {		
		TupleType current = this;
		List<TupleType> Path= new Vector<TupleType>();
		
		while(current.refferedType != null) {		
			if(Path.contains(current)) {
				CircledTupleDefinitionError();
				return false;
			}
			Path.add(current);
			current = current.refferedType;
		}
		this.symbolTable = current.symbolTable;
		return true;
	}
	
	
	////////////////////////////////////////////////////////////
	// error logging
	public void CircledTupleDefinitionError() {
		GrouseLogger log = GrouseLogger.getLogger("compiler.semanticAnalyzer.identifierNode");
		log.severe("Circled definition of tuple Type: " + this.infoString());
	}
	
	////////////////////////////////////////////////////////////
	// type info
	public int getSize() {
		return 4;
	}
	
	public String infoString() {
		/*
		if(SymbolTable != null)
			return "TupleType[" + this.TupleName + "]\n"+ SymbolTable.toString();
		if(refferedName!=null)
			return "TupleType" + "[" + this.TupleName + "] "+ refferedName;
		return "Not-initialized";
		*/
		return "Tuple[" + this.TupleName + "]";
	}
	
}


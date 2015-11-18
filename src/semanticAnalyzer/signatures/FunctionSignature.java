package semanticAnalyzer.signatures;

import java.util.List;

import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.TupleType;
import semanticAnalyzer.types.Type;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;

//immutable
public class FunctionSignature {
	private static final boolean ALL_TYPES_ACCEPT_ERROR_TYPES = true;
	private Type resultType;
	private Type[] paramTypes;
	Object whichVariant;
	
	
	public void printArgs() {
		for(int i=0;i<paramTypes.length;i++)
			System.out.println(paramTypes[i].infoString());
		System.out.println("\n\n");
	}
	
	///////////////////////////////////////////////////////////////
	// construction
	
	public FunctionSignature(Object whichVariant, Type ...types) {
		assert(types.length >= 1);
		storeParamTypes(types);
		resultType = types[types.length-1];
		this.whichVariant = whichVariant;
	}
	private void storeParamTypes(Type[] types) {
		paramTypes = new Type[types.length-1];
		for(int i=0; i<types.length-1; i++) {
			paramTypes[i] = types[i];
		}
	}
	
	//////////////////////////////////////////////////////////////
	// eliminate trivial tuple
	public void eliminateTrivialTuple() {
		for(int i = 0; i< paramTypes.length; i++) {
			if(paramTypes[i] instanceof TupleType) {
				if(((TupleType)paramTypes[i]).isTrivial())
					paramTypes[i] = ((TupleType)paramTypes[i]).getTirvialEquvalenceType();
			}
			/*
			 * not needed since it is undefined
			if(paramTypes[i] instanceof ArrayType) {
				((ArrayType)paramTypes[i]).eliminateTrivialTuple();
			}
			*/
		}
		
		if(resultType instanceof TupleType) {
			if(((TupleType)resultType).isTrivial())
				resultType = ((TupleType)resultType).getTirvialEquvalenceType();
		}
		/*
		 * not needed since it is undefined
		if(resultType instanceof ArrayType) {
			((ArrayType)resultType).eliminateTrivialTuple();
		}
		*/
		
	}
	
	///////////////////////////////////////////////////////////////
	// accessors
	
	public Object getVariant() {
		return whichVariant;
	}
	public Type resultType() {
		return resultType;
	}
	public boolean isNull() {
		return false;
	}
	
	
	///////////////////////////////////////////////////////////////
	// main query

	public boolean accepts(List<Type> types) {
		if(types.size() != paramTypes.length) {
			return false;
		}
		
		for(int i=0; i<paramTypes.length; i++) {
			if(!assignableTo(paramTypes[i], types.get(i))) {
				return false;
			}
		}		
		return true;
	}
	private boolean assignableTo(Type variableType, Type valueType) {
		if(valueType == PrimitiveType.ERROR && ALL_TYPES_ACCEPT_ERROR_TYPES) {
			return true;
		}	
		// cannot be valueType.equals(variableType)
		// otherwise PrimitiveType.equals(TypeVariable) will return false
		return variableType.equals(valueType);
	}
	
	// Null object pattern
	private static FunctionSignature neverMatchedSignature = new FunctionSignature(1, PrimitiveType.ERROR) {
		public boolean accepts(List<Type> types) {
			return false;
		}
		public boolean isNull() {
			return true;
		}
	};
	public static FunctionSignature nullInstance() {
		return neverMatchedSignature;
	}
	
	///////////////////////////////////////////////////////////////////
	// Signatures for grouse-0 operators
	// this section will probably disappear in grouse-1 (in favor of FunctionSignatures)
	
	private static FunctionSignature addSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER);
	private static FunctionSignature multiplySignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER);
	private static FunctionSignature greaterSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN);
	private static FunctionSignature divideSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER);
	
	// the switch here is ugly compared to polymorphism.  This should perhaps be a method on Lextant.
	public static FunctionSignature signatureOf(Lextant lextant) {
		assert(lextant instanceof Punctuator);	
		Punctuator punctuator = (Punctuator)lextant;
		
		switch(punctuator) {
		case ADD:		return addSignature;
		case MULTIPLY:	return multiplySignature;
		case DIVIDE:	return divideSignature;
		case GREATER:	return greaterSignature;

		default:
			return neverMatchedSignature;
		}
	}

}
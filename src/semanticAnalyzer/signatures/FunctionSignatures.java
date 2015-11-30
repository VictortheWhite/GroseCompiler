package semanticAnalyzer.signatures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Punctuator;
import semanticAnalyzer.types.TupleTypeVariable;
import semanticAnalyzer.types.Type;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.TypeVariable;
import asmCodeGenerator.codeStorage.ASMOpcode;



public class FunctionSignatures extends ArrayList<FunctionSignature> {
	private static final long serialVersionUID = -4907792488209670697L;
	private static Map<Object, FunctionSignatures> signaturesForKey = new HashMap<Object, FunctionSignatures>();
	
	Object key;
	
	public FunctionSignatures(Object key, FunctionSignature ...functionSignatures) {
		this.key = key;
		for(FunctionSignature functionSignature: functionSignatures) {
			add(functionSignature);
		}
		signaturesForKey.put(key, this);
	}
	
	public Object getKey() {
		return key;
	}
	public boolean hasKey(Object key) {
		return this.key.equals(key);
	}
	
	public FunctionSignature acceptingSignature(List<Type> types) {
		for(FunctionSignature functionSignature: this) {
			if(functionSignature.accepts(types)) {
				return functionSignature;
			}
		}
		return FunctionSignature.nullInstance();
	}
	public boolean accepts(List<Type> types) {
		return !acceptingSignature(types).isNull();
	}

	
	/////////////////////////////////////////////////////////////////////////////////
	// access to FunctionSignatures by key object.
	
	public static FunctionSignatures nullSignatures = new FunctionSignatures(0, FunctionSignature.nullInstance());

	public static FunctionSignatures signaturesOf(Object key) {
		if(signaturesForKey.containsKey(key)) {
			return signaturesForKey.get(key);
		}
		return nullSignatures;
	}
	public static FunctionSignature signature(Object key, List<Type> types) {
		FunctionSignatures signatures = FunctionSignatures.signaturesOf(key);
		return signatures.acceptingSignature(types);
	}
	


	
	
	/////////////////////////////////////////////////////////////////////////////////
	// Put the signatures for operators in the following static block.
	private static TypeVariable TypeVar = new TypeVariable("ArrayTypeVar");
	private static TupleTypeVariable TupleTypeVar = new TupleTypeVariable("TupleTypeVar");
	public static void resetTypeVar() {
		TypeVar.reset();
		TupleTypeVar.reset();
	}

	static {
		// here's one example to get you started with FunctionSignatures: the signatures for addition.		
		// for this to work, you should statically import PrimitiveType.*
		
		new FunctionSignatures(Punctuator.ADD,
		    new FunctionSignature(ASMOpcode.Add, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
		    new FunctionSignature(ASMOpcode.FAdd, PrimitiveType.FLOATING, PrimitiveType.FLOATING, PrimitiveType.FLOATING),
		    new FunctionSignature(1 /*String concatenation*/, PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.STRING)	
		);
		
		new FunctionSignatures(Punctuator.MULTIPLY,
			new FunctionSignature(ASMOpcode.Multiply, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
			new FunctionSignature(ASMOpcode.FMultiply, PrimitiveType.FLOATING, PrimitiveType.FLOATING, PrimitiveType.FLOATING)
		);
		
		new FunctionSignatures(Punctuator.DIVIDE,
				new FunctionSignature(ASMOpcode.Divide, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
				new FunctionSignature(ASMOpcode.FDivide, PrimitiveType.FLOATING, PrimitiveType.FLOATING, PrimitiveType.FLOATING)
			);
		
		new FunctionSignatures(Punctuator.SUB,
				new FunctionSignature(ASMOpcode.Subtract, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
				new FunctionSignature(ASMOpcode.FSubtract, PrimitiveType.FLOATING, PrimitiveType.FLOATING, PrimitiveType.FLOATING)

			);
		
		new FunctionSignatures(Punctuator.GREATER,
				new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.FLOATING, PrimitiveType.FLOATING, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN)
			);
		
		new FunctionSignatures(Punctuator.LESS,
				new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.FLOATING, PrimitiveType.FLOATING, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN)
			);
		
		new FunctionSignatures(Punctuator.GREATEROFEQUAL,
				new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.FLOATING, PrimitiveType.FLOATING, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN)
			);
		
		new FunctionSignatures(Punctuator.LESSOFEQUAL,
				new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.FLOATING, PrimitiveType.FLOATING, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN)
			);
		
		new FunctionSignatures(Punctuator.EQUAL,
				new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.FLOATING, PrimitiveType.FLOATING, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, new ArrayType(TypeVar), new ArrayType(TypeVar), PrimitiveType.BOOLEAN),
				new FunctionSignature(1, TupleTypeVar, TupleTypeVar, PrimitiveType.BOOLEAN)
			);
		
		new FunctionSignatures(Punctuator.NOTEQUAL,
				new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.FLOATING, PrimitiveType.FLOATING, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, new ArrayType(TypeVar), new ArrayType(TypeVar), PrimitiveType.BOOLEAN),
				new FunctionSignature(1, TupleTypeVar, TupleTypeVar, PrimitiveType.BOOLEAN)
			);
		
		new FunctionSignatures(Punctuator.BOOLEANAND,
				new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN)
			);
		
		new FunctionSignatures(Punctuator.BOOLEANOR,
				new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN)
			);
		
		new FunctionSignatures(Punctuator.BOOLEANCOMPLIMENT,
				new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN)
			);
		
		// refcount
		new FunctionSignatures(Punctuator.SHARP,
				new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.INTEGER),
				new FunctionSignature(1, new ArrayType(TypeVar), PrimitiveType.INTEGER),
				new FunctionSignature(1, TupleTypeVar, PrimitiveType.INTEGER)
			);
		
		// record number
		new FunctionSignatures(Punctuator.DOLLERSIGN,
				new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.INTEGER),
				new FunctionSignature(1, new ArrayType(TypeVar), PrimitiveType.INTEGER),
				new FunctionSignature(1, TupleTypeVar, PrimitiveType.INTEGER)
			);
		
		
		// length
		new FunctionSignatures(Punctuator.BAR,											
				new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.INTEGER),
				new FunctionSignature(1, new ArrayType(TypeVar), PrimitiveType.INTEGER),
				new FunctionSignature(1, TupleTypeVar, PrimitiveType.INTEGER)
			);
		
		// array indexing
		new FunctionSignatures(Punctuator.OPEN_SQUARE_BRACKET,
				new FunctionSignature(1, new ArrayType(TypeVar), PrimitiveType.INTEGER, TypeVar)
			);
		
		// copy--------------------------------------------------------------------------------------
		new FunctionSignatures(Keyword.COPY,
				new FunctionSignature(1, new ArrayType(TypeVar), new ArrayType(TypeVar)),
				new FunctionSignature(1, TupleTypeVar, TupleTypeVar)
		);
		
		// casting------------------------------------------------------------------------------------------
		new FunctionSignatures(Punctuator.CAST,
				new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
				new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER),
				new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.FLOATING, PrimitiveType.FLOATING),
				new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.FLOATING, PrimitiveType.FLOATING, PrimitiveType.FLOATING),
				new FunctionSignature(1, PrimitiveType.FLOATING, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
				new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER),
				new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
				new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN),
				new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.STRING),
				new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN)
			);
		

		
		// First, we use the operator itself (in this case the Punctuator ADD) as the key.
		// Then, we give that key two signatures: one an (INT x INT -> INT) and the other
		// a (FLOAT x FLOAT -> FLOAT).  Each signature has a "whichVariant" parameter where
		// I'm placing the instruction (ASMOpcode) that needs to be executed.
		//
		// I'll follow the convention that if a signature has an ASMOpcode for its whichVariant,
		// then to generate code for the operation, one only needs to generate the code for
		// the operands (in order) and then add to that the Opcode.  For instance, the code for
		// floating addition should look like:
		//
		//		(generate argument 1)	: may be many instructions
		//		(generate argument 2)   : ditto
		//		FAdd					: just one instruction
		//
		// If the code that an operator should generate is more complicated than this, then
		// I will not use an ASMOpcode for the whichVariant.  In these cases I typically use
		// a small object with one method (the "Command" design pattern) that generates the
		// required code.

	}
	
}

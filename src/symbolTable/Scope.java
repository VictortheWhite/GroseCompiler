package symbolTable;

import inputHandler.TextLocation;
import logging.GrouseLogger;
import parseTree.nodeTypes.IdentifierNode;
import semanticAnalyzer.types.Type;
import tokens.Token;

public class Scope {
	private Scope baseScope;
	private MemoryAllocator allocator;
	private SymbolTable symbolTable;
	
//////////////////////////////////////////////////////////////////////
// factories

	public static Scope createProgramScope() {
		return new Scope(programScopeAllocator(), nullInstance());
	}
	public static Scope createTupleScope() {
		return new Scope(tupleScopeAllocator(), nullInstance());
	}
	public static Scope createParameterScope() {
		return new Scope(parameterScopeAllocator(), nullInstance());
	}
	public static Scope createProcedureScope() {
		return new Scope(procedureScopeAllocator(), nullInstance());
	}
	public Scope createSubscope() {
		return new Scope(allocator, this);
	}
	
	private static MemoryAllocator programScopeAllocator() {
		return new PositiveMemoryAllocator(
				MemoryAccessMethod.DIRECT_ACCESS_BASE, 
				MemoryLocation.GLOBAL_VARIABLE_BLOCK
				);
	}
	
	private static MemoryAllocator tupleScopeAllocator() {
		return new PositiveMemoryAllocator(
				MemoryAccessMethod.GENERATE_OFFSET_ONLY,
				9	// offset for the tuple header
				);
	}
	
	private static MemoryAllocator parameterScopeAllocator() {
		return new ParameterMemoryAllocator(
				MemoryAccessMethod.INDIRECT_ACCESS_BASE,
				MemoryLocation.FRAME_POINTER
				);
	}
	
	private static MemoryAllocator procedureScopeAllocator() {
		return new NegativeMemoryAllocator(
				MemoryAccessMethod.INDIRECT_ACCESS_BASE,
				MemoryLocation.FRAME_POINTER,
				-8
				);
	}
	
//////////////////////////////////////////////////////////////////////
// private constructor.	
	private Scope(MemoryAllocator allocator, Scope baseScope) {
		super();
		this.baseScope = (baseScope == null) ? this : baseScope;
		this.symbolTable = new SymbolTable();
		
		this.allocator = allocator;
		//allocator.saveState();			// seperate saveState() from enterScope; Scope can be created without entered.
	}
	
///////////////////////////////////////////////////////////////////////
//  basic queries	
	public Scope getBaseScope() {
		return baseScope;
	}
	public MemoryAllocator getAllocationStrategy() {
		return allocator;
	}
	public SymbolTable getSymbolTable() {
		return symbolTable;
	}	
	public void enterScope() {
		this.allocator.saveState();
	}
	
///////////////////////////////////////////////////////////////////////
//memory allocation
	// must call leave() when destroying/leaving a scope.
	public void leave() {
		allocator.restoreState();
	}
	public int getAllocatedSize() {
		return allocator.getMaxAllocatedSize();
	}

///////////////////////////////////////////////////////////////////////
//bindings
	public Binding createBinding(IdentifierNode identifierNode, Type type) {
		Token token = identifierNode.getToken();
		symbolTable.errorIfAlreadyDefined(token);
				
		String lexeme = token.getLexeme();
		Binding binding = allocateNewBinding(type, token.getLocation(), lexeme);	
		symbolTable.install(lexeme, binding);

		return binding;
	}
	public Binding createBinding(String lexeme, Type type) {
		symbolTable.errorIfAlreadyDefined(lexeme);
				
		Binding binding = allocateNewBinding(type, new TextLocation("currentTest.grouse", -1, -1), lexeme);	
		symbolTable.install(lexeme, binding);

		return binding;
	}
	private Binding allocateNewBinding(Type type, TextLocation textLocation, String lexeme) {
		MemoryLocation memoryLocation = allocator.allocate(type.getSize());
		return new Binding(type, textLocation, memoryLocation, lexeme);
	}
	
// return variable bindings
// accepts a memory location
	public Binding createReturnVariableBinding(Type type, String lexeme, MemoryLocation memLocation) {
		symbolTable.errorIfAlreadyDefined(lexeme);
		
		Binding binding = new Binding(type, new TextLocation("input_file", -1, -1), memLocation, lexeme);
		symbolTable.install(lexeme, binding);
		
		return binding;
	}
// function bindings
// takes 0 bytes in globalSymbolTable
 	public Binding createFunctionBinding(IdentifierNode identifierNode, Type type) {
		Token token = identifierNode.getToken();
		symbolTable.errorIfAlreadyDefined(token);
				
		String lexeme = token.getLexeme();
		Binding binding = allocateNewFunctionBinding(type, token.getLocation(), lexeme);	
		symbolTable.install(lexeme, binding);

		return binding;
	}
	
	private Binding allocateNewFunctionBinding(Type type, TextLocation textLocation, String lexeme) {
		MemoryLocation memoryLocation = allocator.allocate(0);
		return new FunctionBinding(type, textLocation, memoryLocation, lexeme);
	}
	
// tuple bindings	
// takes 0 bytes in globalSymbolTable
	public Binding createTupleBinding(IdentifierNode identifierNode, Type type) {
		Token token = identifierNode.getToken();
		symbolTable.errorIfAlreadyDefined(token);
				
		String lexeme = token.getLexeme();
		Binding binding = allocateNewTupleBinding(type, token.getLocation(), lexeme);	
		symbolTable.install(lexeme, binding);

		return binding;
	}
	private Binding allocateNewTupleBinding(Type type, TextLocation textLocation, String lexeme) {
		MemoryLocation memoryLocation = allocator.allocate(0);
		return new Binding(type, textLocation, memoryLocation, lexeme);
	}
	
	
	
///////////////////////////////////////////////////////////////////////
//toString
	public String toString() {
		String result = "scope: ";
		result += " hash "+ hashCode() + "\n";
		result += symbolTable;
		return result;
	}

////////////////////////////////////////////////////////////////////////////////////
//Null Scope object - lazy singleton (Lazy Holder) implementation pattern
	public static Scope nullInstance() {
		return NullScope.instance;
	}
	private static class NullScope extends Scope {
		private static NullScope instance = new NullScope();

		private NullScope() {
			super(	new PositiveMemoryAllocator(MemoryAccessMethod.NULL_ACCESS, "", 0),
					null);
		}
		public String toString() {
			return "scope: the-null-scope";
		}
		@Override
		public Binding createBinding(IdentifierNode identifierNode, Type type) {
			unscopedIdentifierError(identifierNode.getToken());
			return super.createBinding(identifierNode, type);
		}
		// subscopes of null scope need their own strategy.  Assumes global block is static.
		public Scope createSubscope() {
			return new Scope(programScopeAllocator(), this);
		}
	}


///////////////////////////////////////////////////////////////////////
//error reporting
	private static void unscopedIdentifierError(Token token) {
		GrouseLogger log = GrouseLogger.getLogger("compiler.scope");
		log.severe("variable " + token.getLexeme() + 
				" used outside of any scope at " + token.getLocation());
	}

}

package symbolTable;

import java.util.ArrayList;
import java.util.Collection;
//import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logging.GrouseLogger;
import semanticAnalyzer.types.Type;
import tokens.Token;

public class SymbolTable {
	private Map<String, Binding> table;
	
	public SymbolTable() {
		//talbe = new HashMap<StringBinding>
		table = new LinkedHashMap<String, Binding>();
	}
	
	
	////////////////////////////////////////////////////////////////
	// installation and lookup of identifiers

	public Binding install(String identifier, Binding binding) {
		table.put(identifier, binding);
		return binding;
	}
	public Binding lookup(String identifier) {
		return table.getOrDefault(identifier, Binding.nullInstance());
	}
	
	////////////////////////////////////////////////////////////////////
	// reallocate memory, for trivial type replacement for parameterList of Tuples
	// accepts a memory allocator, and reallocates memory
	public void reallocateMemory(MemoryAllocator allocator) {
		List<Binding> bindings = new ArrayList<Binding>(values());
		for(Binding binding: bindings) {
			Type bindingType = binding.getType();
			binding.setMemoryLocation(allocator.allocate(bindingType.getSize()));
		}
	}
	
	///////////////////////////////////////////////////////////////////////
	// Map delegates	
	
	public boolean containsKey(String identifier) {
		return table.containsKey(identifier);
	}
	public Set<String> keySet() {
		return table.keySet();
	}
	public Collection<Binding> values() {
		return table.values();
	}
	
	///////////////////////////////////////////////////////////////////////
	//error reporting

	public void errorIfAlreadyDefined(Token token) {
		if(containsKey(token.getLexeme())) {		
			multipleDefinitionError(token);
		}
	}
	protected static void multipleDefinitionError(Token token) {
		GrouseLogger log = GrouseLogger.getLogger("compiler.symbolTable");
		log.severe("variable \"" + token.getLexeme() + 
				          "\" multiply defined at " + token.getLocation());
	}

	///////////////////////////////////////////////////////////////////////
	// toString

	public String toString() {
		StringBuffer result = new StringBuffer("    symbol table: \n");
		table.entrySet().forEach((entry) -> {
			result.append("        " + entry + "\n");
		});
		return result.toString();
	}
}

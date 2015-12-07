package semanticAnalyzer;

import parseTree.*;

import java.util.ArrayList;
import java.util.List;

import symbolTable.MemoryAccessMethod;
import symbolTable.MemoryAllocator;
import symbolTable.MemoryLocation;
import symbolTable.PositiveMemoryAllocator;
import symbolTable.Scope;
import symbolTable.Binding;

//import semanticAnalyzer.types.TupleType;


public class SemanticAnalyzer {
	ParseNode ASTree;
	
	private static Scope globalScope;
	private static List<Binding> staticBindings;
	private static MemoryAllocator staticAllocator;
	
	public static ParseNode analyze(ParseNode ASTree) {
		SemanticAnalyzer analyzer = new SemanticAnalyzer(ASTree);
		return analyzer.analyze();
	}
	public SemanticAnalyzer(ParseNode ASTree) {
		this.ASTree = ASTree;
		staticBindings = new ArrayList<Binding>();
		staticAllocator = new PositiveMemoryAllocator(
				MemoryAccessMethod.DIRECT_ACCESS_BASE, 
				MemoryLocation.STATIC_VARIABLE_BLOCK,
				0);
	}
	
	public ParseNode analyze() {		
		ASTree.accept(new SemanticAnalysisTupleCollectingVisitor());
		ASTree.accept(new TupleInitializationAndFunctionSignatureVisitor());
		ASTree.accept(new SemanticAnalysisVisitor());
		
		/*
		System.out.println("Global "+globalScope.getSymbolTable());		
		for(String tupleName : globalScope.getSymbolTable().keySet()) {
			Binding binding = globalScope.getSymbolTable().lookup(tupleName);
			if(!(binding.getType() instanceof TupleType)) {
				continue;
			}
			TupleType type = (TupleType)binding.getType();
			type.printSymbolTable();
		}
		*/
		
		
		
		
		
		return ASTree;
	}
	
	public static void setGlobalScope(Scope global) {
		globalScope = global;
	}
	
	public static Scope getGlobalScope() {
		return globalScope;
	}
	
	public static List<Binding> getStaticBindings() {
		return staticBindings;
	}
	
	public static MemoryAllocator getStaticAllocator() {
		return staticAllocator;
	}
	
}

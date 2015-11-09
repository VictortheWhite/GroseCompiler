package semanticAnalyzer;

import parseTree.*;
import symbolTable.Scope;


public class SemanticAnalyzer {
	ParseNode ASTree;
	
	private static Scope globalScope;
	
	public static ParseNode analyze(ParseNode ASTree) {
		SemanticAnalyzer analyzer = new SemanticAnalyzer(ASTree);
		return analyzer.analyze();
	}
	public SemanticAnalyzer(ParseNode ASTree) {
		this.ASTree = ASTree;
	}
	
	public ParseNode analyze() {
		ASTree.accept(new SemanticAnalysisTupleCollectingVisitor());
		ASTree.accept(new TupleInitializationAndFunctionSignatureVisitor());
		ASTree.accept(new SemanticAnalysisVisitor());
		
		return ASTree;
	}
	
	public static void setGlobalScope(Scope global) {
		globalScope = global;
	}
	
	public static Scope getGlobalScope() {
		return globalScope;
	}
}

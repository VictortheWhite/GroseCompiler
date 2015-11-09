package semanticAnalyzer;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import parseTree.nodeTypes.*;
import semanticAnalyzer.types.TupleType;
import semanticAnalyzer.types.Type;
import symbolTable.Binding;
import symbolTable.Scope;



public class SemanticAnalysisTupleCollectingVisitor extends ParseNodeVisitor.Default{
	
	// this visitor creates all the scopes without entering it
	// scopes created includes program, block, for, tuple def
	// this visitor collects all the names of global definition
	// and creates binding of initialized tuple type
	
		
	@Override
	public void visitLeave(ParseNode node) {
		throw new RuntimeException("Node class unimplemented in SemanticAnalysisVisitor: " + node.getClass());
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	// constructs larger than statements
	@Override
	public void visitEnter(ProgramNode node) {
		CreateProgramScope(node);
	}
	public void visitEnter(MainBlockNode node) {
		CreateSubScope(node);
	}
	@Override
	public void visitEnter(BlockNode node) {
		CreateSubScope(node);
	}
	
	//////////////////////////////////////////////////////////////////
	// GlobalDefinition
	
	// Tuple Definition
	@Override
	public void visitEnter(TupleDefinitionNode node) {
		CreateSubScope(node);
	}
	@Override
	public void visitLeave(TupleDefinitionNode node) {
		IdentifierNode TupleTypeNode = (IdentifierNode) node.child(0); 
		Type type = new TupleType(TupleTypeNode.getToken().getLexeme());
		
		TupleTypeNode.setType(type);
		addBinding(TupleTypeNode, type);
		
		TupleTypeNode.getBinding().setImmutablity(true);
		TupleTypeNode.getBinding().setShadow(false);
	}
	
	// ParameterList
	
	@Override
	public void visitEnter(ParameterListNode node) {
		CreateSubScope(node);
	}
	
	//////////////////////////////////////////////////////////////////
	// create Scopes for statements
	@Override
	public void visitEnter(ForStatementNode node) {
		CreateSubScope(node);
	}
		
	//////////////////////////////////////////////////////////////////
	// scoping methods for scoping
	
	private void CreateProgramScope(ProgramNode node) {
		Scope scope = Scope.createProgramScope();
		SemanticAnalyzer.setGlobalScope(scope);
		node.setScope(scope);
	}
	
	private void CreateSubScope(ParseNode node) {
		Scope baseScope = node.getLocalScope();
		Scope scope = baseScope.createSubscope();
		node.setScope(scope);
	}
	
	// helper methods for bindings
	private void addBinding(IdentifierNode identifierNode, Type type) {
		if(!identifierNode.canBeShadowed()) {
			identifierNode.setBinding(Binding.nullInstance());
		}
		Binding binding = SemanticAnalyzer.getGlobalScope().createBinding(identifierNode, type);
		identifierNode.setBinding(binding);
	}
}

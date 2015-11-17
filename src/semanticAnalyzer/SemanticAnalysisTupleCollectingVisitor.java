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
		if(node.getParent() instanceof FunctionDefinitionNode) {
			CreateProcedureScope(node);
		} else {
			CreateSubScope(node);
		}
	}
	//////////////////////////////////////////////////////////////////
	// create scopes
	// ParameterList
	
	@Override
	public void visitEnter(ParameterListNode node) {
		CreateTupleScope(node);
	}
	
	// statements
	@Override
	public void visitEnter(ForStatementNode node) {
		CreateSubScope(node);
	}
	
	// function Definition
		@Override
	public void visitEnter(FunctionDefinitionNode node) {
		CreateParameterScope(node);
	}
	
	
	//////////////////////////////////////////////////////////////////
	// GlobalDefinition
	
	// Tuple Definition
	@Override
	public void visitLeave(TupleDefinitionNode node) {
		IdentifierNode TupleTypeNode = (IdentifierNode) node.child(0); 
		Type type = new TupleType(TupleTypeNode.getToken().getLexeme());
		
		TupleTypeNode.setType(type);
		addTupleBinding(TupleTypeNode, type);
		
		TupleTypeNode.getBinding().setImmutablity(true);
		TupleTypeNode.getBinding().setShadow(false);
	}
	
	
	@Override
	public void visitLeave(FunctionDefinitionNode node) {
		IdentifierNode funcName = (IdentifierNode) node.child(0);
		ParseNode returnTuple = node.child(2);
		Type type = new TupleType(funcName.getToken().getLexeme());
		
		funcName.setType(type);
		returnTuple.setType(type);
		addFunctionBinding(funcName, type);
		
		funcName.getBinding().setImmutablity(true);
		funcName.getBinding().setShadow(false);
	}
	
	
		
	//////////////////////////////////////////////////////////////////
	// scoping methods for scoping
	
	private void CreateProgramScope(ProgramNode node) {
		Scope scope = Scope.createProgramScope();
		SemanticAnalyzer.setGlobalScope(scope);
		node.setScope(scope);
	}
	
	private void CreateTupleScope(ParseNode node) {
		Scope scope = Scope.createTupleScope();
		node.setScope(scope);
	}
	
	private void CreateParameterScope(ParseNode node) {
		Scope scope = Scope.createParameterScope();
		node.setScope(scope);
	}
	
	private void CreateProcedureScope(ParseNode node) {
		Scope scope = Scope.createProcedureScope();
		node.setScope(scope);
	}
	
	private void CreateSubScope(ParseNode node) {
		Scope baseScope = node.getLocalScope();
		Scope scope = baseScope.createSubscope();
		node.setScope(scope);
	}
	
	
	// helper methods for bindings
	private void addFunctionBinding(IdentifierNode identifierNode, Type type) {
		if(!identifierNode.canBeShadowed()) {
			identifierNode.setBinding(Binding.nullInstance());
		}
		Binding binding = SemanticAnalyzer.getGlobalScope().createFunctionBinding(identifierNode, type);
		identifierNode.setBinding(binding);
	}
	private void addTupleBinding(IdentifierNode identifierNode, Type type) {
		if(!identifierNode.canBeShadowed()) {
			identifierNode.setBinding(Binding.nullInstance());
		}
		Binding binding = SemanticAnalyzer.getGlobalScope().createTupleBinding(identifierNode, type);
		identifierNode.setBinding(binding);
	}
}

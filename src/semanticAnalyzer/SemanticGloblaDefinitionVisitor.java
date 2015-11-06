package semanticAnalyzer;

import java.util.Arrays;
import java.util.List;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import logging.GrouseLogger;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import parseTree.nodeTypes.*;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.signatures.FunctionSignatures;
import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import symbolTable.Binding;
import symbolTable.Scope;
import tokens.IdentifierToken;
import tokens.LextantToken;
import tokens.Token;

class SemanticGloblaDefinitionVisitor extends ParseNodeVisitor.Default {
	@Override
	public void visitLeave(ParseNode node) {
		throw new RuntimeException("Node class unimplemented in SemanticGloblaDefinitionVisitor: " + node.getClass());
	}
	
	///////////////////////////////////////////////////////////////////////////
	// create ProgramScope for globalDefinition
	@Override
	public void visitEnter(ProgramNode node) {
		enterProgramScope(node);
	}
		
	
	///////////////////////////////////////////////////////////////////////////
	// helper methods for scoping.
	private void enterProgramScope(ParseNode node) {
		Scope scope = Scope.createProgramScope();
		node.setScope(scope);
	}
	
	////////////////////////////////////////////////////////////////////////////
	// deal with global definitions
	
	// tuple definition
	@Override
	public void visitLeave(TupleDefinitionNode node) {
		IdentifierNode identifier = (IdentifierNode) node.child(0);
		ParameterTupleNode paraTup = (ParameterTupleNode) node.child(1);
		
		Type TupleType = paraTup.getType();
		identifier.setType(TupleType);
		addBinding(identifier, TupleType);
		
		// make it immutable and unable to be shadowed for now
		identifier.getBinding().setImmutablity(true);
		identifier.getBinding().setShadow(false);
		
	}
	
	// parameter tuple
	@Override
	public void visitLeave(ParameterTupleNode node) {
		if(node.child(0) instanceof IdentifierNode) {
			
		} else {
			
		}
	}
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// IdentifierNodes, with helper methods
	/*
	@Override
	public void visit(IdentifierNode node) {
		
		if(!isBeingDeclared(node)) {		
			Binding binding = node.findVariableBinding();
			node.setType(binding.getType());
			node.setBinding(binding);
		}
		
		// else parent DeclarationNode does the processing.
	}
	
	private boolean isBeingDeclared(IdentifierNode node) {
		ParseNode parent = node.getParent();
		return (parent instanceof TupleDefinitionNode) ;
	}
	*/
	
	private void addBinding(IdentifierNode identifierNode, Type type) {
		if(!identifierNode.canBeShadowed()) {
			identifierNode.setBinding(Binding.nullInstance());
		}
		Scope scope = identifierNode.getLocalScope();
		Binding binding = scope.createBinding(identifierNode, type);
		identifierNode.setBinding(binding);
	}
}
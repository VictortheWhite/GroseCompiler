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
import semanticAnalyzer.types.TupleType;
import semanticAnalyzer.types.Type;
import symbolTable.Binding;
import symbolTable.Scope;
import tokens.IdentifierToken;
import tokens.LextantToken;
import tokens.Token;

public class TupleInitializationAndFunctionSignatureVisitor extends ParseNodeVisitor.Default{

	// this visitor inilized tupleType
	// and add function signatures for functions
	
	@Override
	public void visitLeave(TupleDefinitionNode node) {
		IdentifierNode TupleName = (IdentifierNode) node.child(0);
		ParseNode initializer = node.child(1);
		
		
		if(!(initializer instanceof ParameterListNode || initializer.getType() instanceof TupleType)) {
			logError("Ilegal initializer for tuple Type: paraList or tupleType expected " + node.getToken().getLocation());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		
		assert TupleName.getType() instanceof TupleType;
		TupleType type= (TupleType)TupleName.getType();
		
		type.initialize(initializer);
		//node.setType(type);
	}
	
	// parameterList, dealing
	@Override
	public void visitLeave(ParameterListNode node) {
		if(!(node.getParent() instanceof TupleDefinitionNode)) {
			return;
		}
		
		Scope parameterScope = node.getScope();
		
		for(ParseNode paraSpec : node.getChildren()) {
			TypeNode typeNode = (TypeNode)	paraSpec.child(0);
			IdentifierNode id = (IdentifierNode) paraSpec.child(1);
			createBinding(parameterScope, id, typeNode.getType());
		}
	}
	
	////////////////////////////////////////////////////////////////////
	// Type Node
	
	public void visitLeave(TypeNode node) {
		assert node.nChildren() == 1;
		assert node.getToken().isLextant(Punctuator.OPEN_SQUARE_BRACKET);
		
		node.setType(new ArrayType(node.child(0).getType()));
	}
	
	@Override
	public void visit(TypeNode node) {
		node.setType(node.getTerminalType());
	}
	
	/////////////////////////////////////////////////////////////////////
	// creat Binding
	private void createBinding(Scope scope, IdentifierNode identifierNode, Type type) {
		if(!identifierNode.canBeShadowed()) {
			identifierNode.setBinding(Binding.nullInstance());
		}
		
		Binding binding = scope.createBinding(identifierNode, type);
		identifierNode.setBinding(binding);
	}
	
	///////////////////////////////////////////////////////////////////////
	// Error loging
	
	private void logError(String message) {
		GrouseLogger log = GrouseLogger.getLogger("compiler.semanticAnalyzer");
		log.severe(message);
	}
	
}

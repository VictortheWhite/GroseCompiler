package semanticAnalyzer;

import java.util.ArrayList;
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
import symbolTable.FunctionBinding;
import symbolTable.MemoryAccessMethod;
import symbolTable.MemoryAllocator;
import symbolTable.MemoryLocation;
import symbolTable.PositiveMemoryAllocator;
import symbolTable.Scope;
import tokens.LextantToken;
import tokens.Token;

public class SemanticAnalysisGlobalVariableVisitor extends ParseNodeVisitor.Default {
	
	/*
	 * the accept method of Block nodes has been changed
	 * so that it doesn't let a globalVarVisitor visit its children
	 * thus this will only deal with global variable declarations
	 */
	
	
	@Override
	public void visitLeave(ParseNode node) {
		throw new RuntimeException("Node class unimplemented in SemanticAnalysisVisitor: " + node.getClass());
	}
	
	///////////////////////////////////////////////////////////////////////////
	// global variable declaration
	@Override
	public void visitLeave(DeclarationNode node) {
		assert node.getParent() instanceof ProgramNode;
		IdentifierNode identifier = (IdentifierNode) node.child(0);
		ParseNode initializer = node.child(1);
		
		Type declarationType = initializer.getType();
		node.setType(declarationType);
		
		identifier.setType(declarationType);
		
		if(node.isStatic()) 
			addGlobalStaticBinding(identifier, declarationType);
		else
			addGlobalVariableBinding(identifier, declarationType);
		
		if(node.getToken().isLextant(Keyword.IMMUTABLE)) {
			identifier.getBinding().setImmutablity(true);
		}
		else if(node.getToken().isLextant(Keyword.VARIABLE)) {
			identifier.getBinding().setImmutablity(false);
		}
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	// expressions
	@Override
	public void visitLeave(BinaryOperatorNode node) {
		assert node.nChildren() == 2;
		ParseNode left  = node.child(0);
		ParseNode right = node.child(1);
		List<Type> childTypes = Arrays.asList(left.getType(), right.getType());
				
		Lextant operator = operatorFor(node);
		FunctionSignature signature = FunctionSignatures.signature(operator, childTypes );
		
		if(signature.accepts(childTypes)) {
			node.setType(signature.resultType());
		}
		else {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
	}
	@Override
	public void visitLeave(UnaryOperatorNode node) {
		assert node.nChildren()==1;
		
		// @target
		if(node.getToken().isLextant(Punctuator.AT)) {
			ParseNode child = node.child(0);
			if(!isTargetNode(child)) {
				logError("target required for @: " + node.getToken().getLocation());
				node.setType(PrimitiveType.ERROR);
				return;
			}
			node.setType(PrimitiveType.INTEGER);
			return;
		}
		
		// unary operators other than @
		ParseNode SingleChild = node.child(0);
		List<Type> childTypes = Arrays.asList(SingleChild.getType());
		
		Lextant operator = operatorFor(node);
		FunctionSignature signature = FunctionSignatures.signature(operator, childTypes);
				
		if(signature.accepts(childTypes)) {
			node.setType(signature.resultType());
		}
		else {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
	}
	
	private boolean isTargetNode(ParseNode node) {
		return (node instanceof IdentifierNode) 
				|| (node instanceof ArrayIndexingNode)
				|| (node instanceof TupleEntryNode);
	}
	
	@Override
	public void visitLeave(LengthOperatorNode node) {
		assert node.nChildren()	==1;
		ParseNode SingleChild = node.child(0);
		List<Type> childTypes = Arrays.asList(SingleChild.getType());
		
		Lextant operator = operatorFor(node); 
		FunctionSignature signature = FunctionSignatures.signature(operator, childTypes);
		
		if(signature.accepts(childTypes)) {
			node.setType(signature.resultType());
		} 
		else {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
		
		// not needed anymore because node.setType did that
		// FunctionSignatures.resetTypeVar();
	}
	private Lextant operatorFor(ParseNode node) {
		LextantToken token = (LextantToken) node.getToken();
		return token.getLextant();
	}
	
	// populated array creation
	@Override
	public void visitLeave(PopulatedArrayNode node) {
		if(node.nChildren() == 0) {
			logError("Cannnot create empty populated array: " + node.getToken().getLocation());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		Type childType = node.child(0).getType();
		if(childType instanceof TupleType) {				// tuple type has to be the same type( not equivalent one)
			for(ParseNode child : node.getChildren()) {
				if(child.getType() != childType) {
					populatedArrayTypeCheckError(node, childType);
					node.setType(PrimitiveType.ERROR);
					return;
				}
			}
		}
		else {												// use equals() for other types
 			for(ParseNode child : node.getChildren()) {
				if(!child.getType().equals(childType)) {
					populatedArrayTypeCheckError(node, childType);
					node.setType(PrimitiveType.ERROR);
					return;
				}
			}
		}
		ArrayType arrayType = new ArrayType(childType);
		node.setType(arrayType);
	}
	
	// empty array creation
	@Override
	public void visitLeave(FreshArrayNode node) {
		assert node.nChildren() == 2;
		Type nodeType = node.child(0).getType();
		if(!(nodeType instanceof ArrayType || nodeType instanceof TupleType)) {
			logError("Ilegal Type for fresh, arrayType or tupleType expected" + node.getToken().getLocation());
			node.setType(PrimitiveType.ERROR);
		}
		
		if(nodeType instanceof ArrayType) {
			if(node.child(1).getType()!= PrimitiveType.INTEGER) {
				logError("Ilegal Type for fresh, int expected for array Length" + node.getToken().getLocation());
			}
		}
		if(nodeType instanceof TupleType) {
			List<Type> childTypes = collectChildrenTypes(node.child(1));
			if(!((TupleType) nodeType).checkArugments(childTypes)) {
				argumentsListNotMatchedError(node,nodeType);
				node.setType(PrimitiveType.ERROR);
			}
		}
		
		node.setType(nodeType);
	}
	
	// array concatenation
	@Override
	public void visitLeave(ArrayConcatenationNode node) {
		if(node.nChildren() < 2) {
			logError("Array Concatenation needs at least 2 args" + node.getToken().getLocation());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		List<Type> childTypes = collectChildrenTypes(node);
		Type nodeType = childTypes.get(0);
		if(!(nodeType instanceof ArrayType)) {
			logError("arrayType needed for array conca"
					+ node.getToken().getLocation());
		}
		for(Type childType : childTypes) {
			if(!childType.equals(nodeType)) {
				logError("array conca type cheking error: " + childTypes 
						+ node.getToken().getLocation());
				node.setType(PrimitiveType.ERROR);
				return;
			}
		}
		
		node.setType(nodeType);
	}
	
	private List<Type> collectChildrenTypes(ParseNode node) {
		List<Type> childrenTypes = new ArrayList<Type>();
		for(ParseNode child : node.getChildren()) {
			childrenTypes.add(child.getType());
		}
		
		return childrenTypes;
	}
	
	
	@Override
	public void visitLeave(FunctionInvocationNode node) {
		assert node.child(0) instanceof IdentifierNode;
		IdentifierNode functionNode = (IdentifierNode) node.child(0);
		ParseNode argList = node.child(1);
				
		if(!(functionNode.getBinding() instanceof FunctionBinding)) {
			logError("No function "+node.getToken().getLexeme() +node.getToken().getLocation());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		FunctionBinding funcBinding = (FunctionBinding)functionNode.getBinding();
		FunctionSignature signature = funcBinding.getSignature();
				
		List<Type> exprTypesList = new ArrayList<Type>();
		for(ParseNode current : argList.getChildren()) {
			exprTypesList.add(current.getType());
		}
		
		if(signature.accepts(exprTypesList)) {
			node.setType(signature.resultType());
		}
		else {
			typeCheckError(node, exprTypesList);
			node.setType(PrimitiveType.ERROR);
		}
		
		if(node.getType() == PrimitiveType.VOID) {
			if(!(node.getParent() instanceof FunctionCallNode)) {
				logError("void function can only be invocated by 'CALL'" + node.getToken().getLocation());
				node.setType(PrimitiveType.ERROR);
			}
		}
		
	}
	
	
	// array indexing
	@Override
	public void visitLeave(ArrayIndexingNode node) {
		assert node.nChildren() == 2;
		ParseNode left  = node.child(0);
		ParseNode right = node.child(1);
		List<Type> childTypes = Arrays.asList(left.getType(), right.getType());
				
		Lextant operator = operatorFor(node);
		FunctionSignature signature = FunctionSignatures.signature(operator, childTypes );
		
		if(signature.accepts(childTypes)) {
			node.setType(signature.resultType());	//setType deals with type variable correctly and reset typeVar
		}
		else {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
		
		// not needed anymore cuz node.setType() resets typeVar
		// FunctionSignatures.resetTypeVar();		
	}
	
	// expression -- tuple entry
	@Override
	public void visitLeave(TupleEntryNode node) {
		if(!(node.child(0).getType() instanceof TupleType)) {
			logError("Expected tupleType in tuple Entry: " + node.getToken().getLocation());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		
		Type nodeType = node.child(1).getType();
		node.setType(nodeType);
	}
	
	// expression -- null reference
	@Override
	public void visitLeave(NullReferenceNode node) {
		assert node.nChildren() == 1;
		Type nodeType = node.child(0).getType();
		if(nodeType instanceof PrimitiveType) {
			if(nodeType != PrimitiveType.STRING) {
				invalidNullReferenceTypeError(node);
				node.setType(PrimitiveType.ERROR);
				return;
			}
		}	
		node.setType(nodeType);
	}
	
	///////////////////////////////////////////////////////////////////
	// type node
	public void visitLeave(TypeNode node) {
		assert node.nChildren() == 1;
		assert node.getToken().isLextant(Punctuator.OPEN_SQUARE_BRACKET);
		
		node.setType(new ArrayType(node.child(0).getType()));
	}
	
	@Override
	public void visit(TypeNode node) {
		node.setType(node.getTerminalType());
	}


	///////////////////////////////////////////////////////////////////////////
	// simple leaf nodes
	@Override
	public void visit(BooleanConstantNode node) {
		node.setType(PrimitiveType.BOOLEAN);
	}
	@Override
	public void visit(ErrorNode node) {
		node.setType(PrimitiveType.ERROR);
	}
	@Override
	public void visit(IntegerConstantNode node) {
		node.setType(PrimitiveType.INTEGER);
	}
	@Override
	public void visit(FloatingConstantNode node) {
		node.setType(PrimitiveType.FLOATING);
	}
	@Override
	public void visit(CharacterConstantNode node) {
		node.setType(PrimitiveType.CHARACTER);
	}
	@Override
	public void visit(StringConstantNode node) {
		node.setType(PrimitiveType.STRING);
	}
	@Override
	public void visit(NewlineNode node) {
		node.setType(PrimitiveType.NO_TYPE);
	}
	@Override
	public void visit(SeparatorNode node) {
		node.setType(PrimitiveType.NO_TYPE);
	}
	
	///////////////////////////////////////////////////////////////////////////
	// IdentifierNodes, with helper methods
	@Override
	public void visit(IdentifierNode node) {
		if(isTupleSubElement(node)) {
			Binding binding = findSubelementBinding(node); 
			if(binding == Binding.nullInstance()) {
				logError("No such entry in TupleType : " + node.getToken().getLocation());
			}
			node.setType(binding.getType());
			node.setBinding(binding);
			return;
		}
		
		if(isFunctionOrTupleIdentifier(node)) {
			Binding binding = findTupleOrFunctionBinding(node);
			node.setType(binding.getType());
			node.setBinding(binding);
			return;
		}
		
		if(!isBeingDeclared(node)) {		
			Binding binding = node.findVariableBinding();
			node.setType(binding.getType());
			node.setBinding(binding);
		}
		// else parent DeclarationNode does the processing.
	}
	private boolean isBeingDeclared(IdentifierNode node) {
		ParseNode parent = node.getParent();
		return (parent instanceof DeclarationNode) && (node == parent.child(0)) 
				||(parent instanceof ForStatementNode) && (node == parent.child(0))
				||(parent instanceof ForControlPhraseNode) && (node == parent.child(0))
				/* second identifier of pair for control*/
				||(parent instanceof ForControlPhraseNode) && (node == parent.child(1)) && (parent.getToken().isLextant(Keyword.PAIR))
				||(parent instanceof ParameterSpecificationNode);
	}
	private boolean isTupleSubElement(IdentifierNode node) {
		ParseNode parent = node.getParent();
		return (parent instanceof TupleEntryNode) && (node == parent.child(1));
	}
	private boolean isFunctionOrTupleIdentifier(IdentifierNode node) {
		ParseNode parent = node.getParent();
		return (parent instanceof FunctionInvocationNode) && (node == parent.child(0)) 
				|| (parent instanceof FunctionDefinitionNode) && (node == parent.child(0)) 
				|| (parent instanceof TupleDefinitionNode) && (node == parent.child(0));
	}
	// finding binding of tuple Subelement
	private Binding findSubelementBinding(IdentifierNode node) {
		if(!(node.getParent().child(0).getType() instanceof TupleType)) {
			return Binding.nullInstance();
		}
		
		TupleType type = (TupleType)node.getParent().child(0).getType();
		return type.lookup(node.getToken().getLexeme());
	}
	// finding binding for tuple or function Identifier 
	private Binding findTupleOrFunctionBinding(IdentifierNode node) {
		return SemanticAnalyzer.getGlobalScope().getSymbolTable().lookup(node.getToken().getLexeme());
	}
	////////////////////////////////////////////////////////////////////////
	private void addGlobalVariableBinding(IdentifierNode identifierNode, Type type) {
		if(!identifierNode.canBeShadowed()) {
			identifierNode.setBinding(Binding.nullInstance());
		}
		Scope scope = SemanticAnalyzer.getGlobalScope();
		Binding binding = scope.createBinding(identifierNode, type);
		identifierNode.setBinding(binding);
	}
	
	// for static variables
	// add binding to staticScope
	// and its local scope
	private void addGlobalStaticBinding(IdentifierNode identifierNode, Type type) {
		if(!identifierNode.canBeShadowed()) {
			identifierNode.setBinding(Binding.nullInstance());
		}
		Scope staticScope = SemanticAnalyzer.getStaticVariableScope();
		Scope localScope = SemanticAnalyzer.getGlobalScope();
		
		Binding realBinding = staticScope.createStaticBinding(identifierNode, type);
		Binding binding = localScope.createBinding(realBinding);
			
		identifierNode.setBinding(binding);
	}

	///////////////////////////////////////////////////////////////////////////
	// error logging/printing

	private void typeCheckError(ParseNode node, List<Type> operandTypes) {
		Token token = node.getToken();
				
		logError("operator " + token.getLexeme() + " not defined for types " 
				 + operandTypes  + " at " + token.getLocation());	
	}
	private void populatedArrayTypeCheckError(ParseNode node, Type type) {
		Token token = node.getToken();
		String errorMessage = "populated array creation requires same types in expression list: ";
		
		logError(errorMessage + type.infoString() + token.getLocation());
	}

	private void argumentsListNotMatchedError(ParseNode node, Type type) {
		Token token = node.getToken();
		String errorMessage = "ArrgumentList does not mathch: ";
		
		logError(errorMessage + type.infoString() + token.getLocation());
	}
	private void invalidNullReferenceTypeError(ParseNode node) {
		Token token = node.getToken();
		String errorMessage = "Reference Type needed for null reference: " + token.getLocation();
		logError(errorMessage);
	}
	private void logError(String message) {
		GrouseLogger log = GrouseLogger.getLogger("compiler.semanticAnalyzer");
		log.severe(message);
	}
}
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
import symbolTable.Scope;
import tokens.LextantToken;
import tokens.Token;

class SemanticAnalysisVisitor extends ParseNodeVisitor.Default {
	@Override
	public void visitLeave(ParseNode node) {
		throw new RuntimeException("Node class unimplemented in SemanticAnalysisVisitor: " + node.getClass());
	}
	
	///////////////////////////////////////////////////////////////////////////
	// constructs larger than statements
	@Override
	public void visitEnter(ProgramNode node) {
		enterScope(node);
	}
	@Override
	public void visitLeave(ProgramNode node) {
		leaveScope(node);
	}
	public void visitEnter(MainBlockNode node) {
		enterScope(node);
	}
	public void visitLeave(MainBlockNode node) {
		leaveScope(node);
	}
	@Override
	public void visitEnter(BlockNode node) {
		enterScope(node);
	}
	@Override
	public void visitLeave(BlockNode node) {
		leaveScope(node);
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	// helper methods for scoping.	
	private void enterScope(ParseNode node) {
		Scope scope = node.getScope();
		scope.enterScope();
	}
	private void leaveScope(ParseNode node) {
		node.getScope().leave();
	}
	
	///////////////////////////////////////////////////////////////////////////
	// global definition
	@Override
	public void visitEnter(ParameterListNode node) {
		enterScope(node);
	}
	public void visitLeave(ParameterListNode node) {
		leaveScope(node);
	}
	
	///////////////////////////////////////////////////////////////////////////
	// statements, declarations and reassignments
	@Override
	public void visitLeave(PrintStatementNode node) {
	}
	@Override
	public void visitLeave(DeclarationNode node) {
		IdentifierNode identifier = (IdentifierNode) node.child(0);
		ParseNode initializer = node.child(1);
		
		Type declarationType = initializer.getType();
		node.setType(declarationType);
		
		identifier.setType(declarationType);
		addBinding(identifier, declarationType);
		
		if(node.getToken().isLextant(Keyword.IMMUTABLE)) {
			identifier.getBinding().setImmutablity(true);
		}
		else if(node.getToken().isLextant(Keyword.VARIABLE)) {
			identifier.getBinding().setImmutablity(false);
		}
	}
	@Override
	public void visitLeave(ReassignmentNode node) {
		ParseNode target = null;
		ParseNode expr = node.child(1);
		if(node.child(0) instanceof ArrayIndexingNode) {
			target = node.child(0);
		} else if(node.child(0) instanceof TupleEntryNode) {
			target = node.child(0);
		}
		else if(node.child(0) instanceof IdentifierNode){
			IdentifierNode identifier = (IdentifierNode) node.child(0);
		
			if(identifier.getBinding().getImmutablity()) {
				reassignImmutableError(node, identifier.getToken().getLexeme());
			}
			target = identifier;		
			} 
		else {
			exprNotTargetableError(node, node.child(0));
			node.setType(PrimitiveType.ERROR);
			return;
		}
		
		
		if(!target.getType().equals(expr.getType())) {
			typeCheckError(node, Arrays.asList(expr.getType(),expr.getType()));
			node.setType(PrimitiveType.ERROR);
			return;
		}
		node.setType(target.getType());

		
	}
	/////////////////////////////////////////////////////////////////////////////
	//control flow statement
	@Override
	public void visitLeave(IfStatementNode node) {
		ParseNode condition = node.child(0);
		if(condition.getType() != PrimitiveType.BOOLEAN) {
			logError("Expected boolean type for if (condition)");
			node.setType(PrimitiveType.ERROR);
		} else
			node.setType(condition.getType());
	}
	@Override
	public void visitLeave(WhileStatementNode node) {
		ParseNode condition = node.child(0);
		
		if(condition instanceof ForControlPhraseNode) {
			assert condition.getToken().isLextant(Keyword.EVER);	// while(ever) { }
			node.setType(PrimitiveType.NO_TYPE);
			return;
		}
		
		if(condition.getType() != PrimitiveType.BOOLEAN) {
			logError("Expected boolean type for if (condition)");
			node.setType(PrimitiveType.ERROR);
		} else
			node.setType(condition.getType());
	}
	
	@Override 
	public void visitEnter(ForStatementNode node) {
		enterScope(node);		
	}
	@Override
	public void visitLeave(ForStatementNode node) {

		leaveScope(node);
	}

	@Override
	public void visitLeave(ForControlPhraseNode node) {
		// type check array
		if(node.getToken().isLextant(Keyword.INDEX, Keyword.ELEMENT)) {
			ParseNode arrayExprNode = node.child(1);
			
			if(!(arrayExprNode.getType() instanceof ArrayType)) {
				logError("Expected array type in for(index id of array)");
				node.setType(PrimitiveType.ERROR);
			} 
		
		}
		
		// check exprs in count has type of Int
		if(node.getToken().isLextant(Keyword.COUNT)) {
			for(int i = 1; i< node.nChildren(); i++)
				if(node.child(i).getType()!=PrimitiveType.INTEGER) {
					logError("Expected integer type for count lower and upper bound");
					node.setType(PrimitiveType.ERROR);
				}
		}
		
		// create binding for itr
		if(node.getToken().isLextant(Keyword.EVER)) {
			return;
		} else {
			assert node.nChildren() ==2 || node.nChildren() == 3;
			
			Type itrType = null;
			if(node.getToken().isLextant(Keyword.ELEMENT)) {
				itrType = ((ArrayType)node.child(1).getType()).getSubType();
			} else
				itrType = PrimitiveType.INTEGER;
			
			IdentifierNode itrIdentifierNode = (IdentifierNode)node.child(0);
			itrIdentifierNode.setType(itrType);
			// add identifier to binding
			addBinding(itrIdentifierNode, itrType);
			
			// set itr to be immutable
			itrIdentifierNode.getBinding().setImmutablity(true);
			// itr cannot be shadowed
			itrIdentifierNode.getBinding().setShadow(false);
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
	
	private List<Type> collectChildrenTypes(ParseNode node) {
		List<Type> childrenTypes = new ArrayList<Type>();
		for(ParseNode child : node.getChildren()) {
			childrenTypes.add(child.getType());
		}
		
		return childrenTypes;
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
				||(parent instanceof ParameterSpecificationNode);
	}
	private boolean isTupleSubElement(IdentifierNode node) {
		ParseNode parent = node.getParent();
		return (parent instanceof TupleEntryNode) && (node == parent.child(1));
	}
	// finding binding of tuple Subelement
	private Binding findSubelementBinding(IdentifierNode node) {
		if(!(node.getParent().child(0).getType() instanceof TupleType)) {
			return Binding.nullInstance();
		}
		
		TupleType type = (TupleType)node.getParent().child(0).getType();
		return type.lookup(node.getToken().getLexeme());
	}
	
	////////////////////////////////////////////////////////////////////////
	private void addBinding(IdentifierNode identifierNode, Type type) {
		if(!identifierNode.canBeShadowed()) {
			identifierNode.setBinding(Binding.nullInstance());
		}
		Scope scope = identifierNode.getLocalScope();
		Binding binding = scope.createBinding(identifierNode, type);
		identifierNode.setBinding(binding);
	}
	
	///////////////////////////////////////////////////////////////////////////
	// error logging/printing

	private void typeCheckError(ParseNode node, List<Type> operandTypes) {
		Token token = node.getToken();
		
		logError("operator " + token.getLexeme() + " not defined for types " 
				 + operandTypes  + " at " + token.getLocation());	
	}
	private void reassignImmutableError(ParseNode node, String identifier) {		
		logError("Immutable identifier (" + identifier +") cannot be reassigned");
	}
	private void populatedArrayTypeCheckError(ParseNode node, Type type) {
		Token token = node.getToken();
		String errorMessage = "populated array creation requires same types in expression list: ";
		
		logError(errorMessage + type.infoString() + token.getLocation());
	}
	private void exprNotTargetableError(ParseNode node, ParseNode child) {
		Token token = node.getToken();
		String errorMessage = "targetable expr must be identifier or array indexing: ";
		
		logError(errorMessage + token.getLocation() + child);
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
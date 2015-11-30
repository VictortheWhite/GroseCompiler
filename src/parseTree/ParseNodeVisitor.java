package parseTree;

import parseTree.nodeTypes.*;

// Visitor pattern with pre- and post-order visits
public interface ParseNodeVisitor {
	
	// non-leaf nodes: visitEnter and visitLeave
	void visitEnter(ParseNode node);
	void visitLeave(ParseNode node);
	
	void visitEnter(MainBlockNode node);
	void visitLeave(MainBlockNode node);

	void visitEnter(BlockNode node);
	void visitLeave(BlockNode node);
	
	void visitEnter(DeclarationNode node);
	void visitLeave(DeclarationNode node);

	void visitEnter(ReassignmentNode node);
	void visitLeave(ReassignmentNode node);
	
	void visitEnter(IfStatementNode node);
	void visitLeave(IfStatementNode node);
	
	void visitEnter(WhileStatementNode node);
	void visitLeave(WhileStatementNode node);
	
	void visitEnter(ForStatementNode node);
	void visitLeave(ForStatementNode node);
	
	void visitEnter(ForControlPhraseNode node);
	void visitLeave(ForControlPhraseNode node);
		
	void visitEnter(PrintStatementNode node);
	void visitLeave(PrintStatementNode node);
	
	void visitEnter(FunctionCallNode node);
	void visitLeave(FunctionCallNode node);
	
	void visitEnter(ProgramNode node);
	void visitLeave(ProgramNode node);
	
	void visitEnter(BinaryOperatorNode node);
	void visitLeave(BinaryOperatorNode node);
	
	void visitEnter(UnaryOperatorNode node);
	void visitLeave(UnaryOperatorNode node);
	
	void visitEnter(LengthOperatorNode node);
	void visitLeave(LengthOperatorNode node);

	void visitEnter(PopulatedArrayNode node);
	void visitLeave(PopulatedArrayNode node);
	
	void visitEnter(FreshArrayNode node);
	void visitLeave(FreshArrayNode node);
	
	void visitEnter(ArrayIndexingNode node);
	void visitLeave(ArrayIndexingNode node);

	void visitEnter(ArrayConcatenationNode node);
	void visitLeave(ArrayConcatenationNode node);
	
	void visitEnter(TupleDefinitionNode node);
	void visitLeave(TupleDefinitionNode node);

	void visitEnter(TupleEntryNode node);
	void visitLeave(TupleEntryNode node);
	
	void visitEnter(ParameterListNode node);
	void visitLeave(ParameterListNode node);
	
	void visitEnter(ParameterSpecificationNode node);
	void visitLeave(ParameterSpecificationNode node);
	
	void visitEnter(ExpressionListNode node);
	void visitLeave(ExpressionListNode node);
	
	void visitEnter(NullReferenceNode node);
	void visitLeave(NullReferenceNode node);
	
	void visitEnter(FunctionDefinitionNode node);
	void visitLeave(FunctionDefinitionNode node);
	
	void visitEnter(FunctionInvocationNode node);
	void visitLeave(FunctionInvocationNode node);
	
	void visitEnter(TypeNode node);
	void visitLeave(TypeNode node);
	
	// leaf nodes: visitLeaf only
	void visit(BooleanConstantNode node);
	void visit(ErrorNode node);
	void visit(IdentifierNode node);
	void visit(IntegerConstantNode node);
	void visit(FloatingConstantNode node);
	void visit(CharacterConstantNode node);
	void visit(StringConstantNode node);
	void visit(NewlineNode node);
	void visit(SeparatorNode node);
	void visit(TypeNode node);
	void visit(BreakContinueStatementNode node);
	void visit(FunctionReturnNode node);

	
	public static class Default implements ParseNodeVisitor
	{
		public void defaultVisit(ParseNode node) {	}
		public void defaultVisitEnter(ParseNode node) {
			defaultVisit(node);
		}
		public void defaultVisitLeave(ParseNode node) {
			defaultVisit(node);
		}		
		public void defaultVisitForLeaf(ParseNode node) {
			defaultVisit(node);
		}
		public void visitEnter(DeclarationNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(DeclarationNode node) {
			defaultVisitLeave(node);
		}					
		public void visitEnter(ReassignmentNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ReassignmentNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(FunctionCallNode node) {
			defaultVisitLeave(node);
		}
		public void visitLeave(FunctionCallNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(MainBlockNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(MainBlockNode node) {
			defaultVisitLeave(node);
		}				
		public void visitEnter(BlockNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(BlockNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ParseNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ParseNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(IfStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(IfStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(WhileStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(WhileStatementNode node) {
			defaultVisitLeave(node);
		}	
		public void visitEnter(ForStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ForStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ForControlPhraseNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ForControlPhraseNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(PrintStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(PrintStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ProgramNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ProgramNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(BinaryOperatorNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(BinaryOperatorNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(UnaryOperatorNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(UnaryOperatorNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(LengthOperatorNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(LengthOperatorNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(PopulatedArrayNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(PopulatedArrayNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(FreshArrayNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(FreshArrayNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ArrayIndexingNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ArrayIndexingNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ArrayConcatenationNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ArrayConcatenationNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(TupleDefinitionNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(TupleDefinitionNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(TupleEntryNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(TupleEntryNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ParameterListNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ParameterListNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ParameterSpecificationNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ParameterSpecificationNode node) {
			defaultVisitLeave(node);
		}		
		public void visitEnter(ExpressionListNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ExpressionListNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(NullReferenceNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(NullReferenceNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(FunctionDefinitionNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(FunctionDefinitionNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(FunctionInvocationNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(FunctionInvocationNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(TypeNode node) {
			defaultVisitLeave(node);
		}
		public void visitLeave(TypeNode node) {
			defaultVisitLeave(node);
		}
		
		// leaf nodes
		public void visit(BooleanConstantNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(ErrorNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(IdentifierNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(IntegerConstantNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(FloatingConstantNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(CharacterConstantNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(StringConstantNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(NewlineNode node) {
			defaultVisitForLeaf(node);
		}	
		public void visit(SeparatorNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(TypeNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(BreakContinueStatementNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(FunctionReturnNode node) {
			defaultVisitForLeaf(node);
		}

	}




}

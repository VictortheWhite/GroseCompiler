package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.SemanticAnalysisGlobalVariableVisitor;
import tokens.Token;

public class BlockNode extends ParseNode {

	public BlockNode(Token token) {
		super(token);
	}
	public BlockNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// no attributes

	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
	
	public void accept(SemanticAnalysisGlobalVariableVisitor visitor) {
		/*
		 * do nothing
		 * cause we don't do anything to expression inside anyblock
		 * nor statements other than global variable declarection
		 * thus just do nothing
		 */
	}
}

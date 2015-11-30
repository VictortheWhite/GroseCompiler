package parseTree.nodeTypes;

import lexicalAnalyzer.Punctuator;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class ArrayConcatenationNode extends ParseNode {

	public ArrayConcatenationNode(Token token) {
		super(token);
		assert token.isLextant(Punctuator.OPEN_SQUARE_BRACKET);
	}
	public ArrayConcatenationNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// attributes

	
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}

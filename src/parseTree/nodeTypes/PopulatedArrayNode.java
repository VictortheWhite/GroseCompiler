package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Punctuator;
import tokens.Token;

public class PopulatedArrayNode extends ParseNode {
	
	public PopulatedArrayNode(Token token) {
		super(token);
		assert(token.isLextant(Punctuator.OPEN_SQUARE_BRACKET));
	}

	public PopulatedArrayNode(ParseNode node) {
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
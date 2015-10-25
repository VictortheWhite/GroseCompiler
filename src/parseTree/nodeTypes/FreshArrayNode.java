package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import tokens.Token;

public class FreshArrayNode extends ParseNode {

	public FreshArrayNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.FRESH));
	}

	public FreshArrayNode(ParseNode node) {
		super(node);
	}
	
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static FreshArrayNode withChildren(Token token, ParseNode left, ParseNode right) {
		FreshArrayNode node = new FreshArrayNode(token);
		node.appendChild(left);
		node.appendChild(right);
		return node;
	}
	
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}

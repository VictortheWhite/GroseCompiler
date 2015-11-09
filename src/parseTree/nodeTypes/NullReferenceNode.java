package parseTree.nodeTypes;

import lexicalAnalyzer.Keyword;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class NullReferenceNode extends ParseNode {

	public NullReferenceNode(Token token) {
		super(token);
		assert token.isLextant(Keyword.NULL);
	}
	public NullReferenceNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// convenience factory
	public static NullReferenceNode withChildren(Token token, ParseNode left) {
		NullReferenceNode node = new NullReferenceNode(token);
		node.appendChild(left);
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
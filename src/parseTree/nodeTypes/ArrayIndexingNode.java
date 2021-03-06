package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;

public class ArrayIndexingNode extends ParseNode {

	public ArrayIndexingNode(Token token) {
		super(token);
		assert(token instanceof LextantToken);
	}

	public ArrayIndexingNode(ParseNode node) {
		super(node);
	}
	
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static ArrayIndexingNode withChildren(Token token, ParseNode left, ParseNode right) {
		ArrayIndexingNode node = new ArrayIndexingNode(token);
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

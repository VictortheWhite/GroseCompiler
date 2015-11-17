package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class ExpressionListNode extends ParseNode {

	public ExpressionListNode(Token token) {
		super(token);
	}
	public ExpressionListNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// attributes

	public int getExprListSize() {
		int size = 0;
		for(ParseNode child : this.getChildren()) {
			size += child.getType().getSize();
		}
		return size;
	}
	
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
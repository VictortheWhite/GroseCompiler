package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class ParameterListNode extends ParseNode {

	public ParameterListNode(Token token) {
		super(token);
	}
	public ParameterListNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// attributes
	public int getExprListSize() {
		int size = 0;
		for(ParseNode child : this.getChildren()) {
			size += child.child(0).getType().getSize();
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
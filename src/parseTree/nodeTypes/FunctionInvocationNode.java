package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.IdentifierToken;
import tokens.Token;

public class FunctionInvocationNode extends ParseNode {
	
	public FunctionInvocationNode(Token token) {
		super(token);
		assert(token instanceof IdentifierToken);
	}

	public FunctionInvocationNode(ParseNode node) {
		super(node);
	}
	
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
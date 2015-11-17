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
	
	
	////////////////////////////////////////////////////////////
	// convenience factory

	public static FunctionInvocationNode withChildren(Token token, ParseNode funcName, ParseNode exprList) {
		FunctionInvocationNode node = new FunctionInvocationNode(token);
		node.appendChild(funcName);
		node.appendChild(exprList);
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
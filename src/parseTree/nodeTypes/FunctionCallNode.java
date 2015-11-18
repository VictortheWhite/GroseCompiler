package parseTree.nodeTypes;

import lexicalAnalyzer.Keyword;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class FunctionCallNode extends ParseNode {
	
	public FunctionCallNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.CALL));
	}

	public FunctionCallNode(ParseNode node) {
		super(node);
	}
	
	
	////////////////////////////////////////////////////////////
	// convenience factory

	public static FunctionCallNode withChildren(Token token, ParseNode funcInvocationNOde) {
		FunctionCallNode node = new FunctionCallNode(token);
		node.appendChild(funcInvocationNOde);
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

package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Lextant;
import tokens.LextantToken;
import tokens.Token;

public class UnaryOperatorNode extends ParseNode {

	public UnaryOperatorNode(Token token) {
		super(token);
		assert(token instanceof LextantToken);
	}

	public UnaryOperatorNode(ParseNode node) {
		super(node);
	}
	
	
	////////////////////////////////////////////////////////////
	// attributes
	
	public Lextant getOperator() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}	
	
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static UnaryOperatorNode addAndReturnChildren(Token token, ParseNode currentNode) {
		UnaryOperatorNode node = new UnaryOperatorNode(token);
		//node.appendChild(left);
		currentNode.appendChild(node);
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
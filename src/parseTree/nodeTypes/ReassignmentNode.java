package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import tokens.Token;

public class ReassignmentNode extends ParseNode {

	public ReassignmentNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.LET));
	}

	public ReassignmentNode(ParseNode node) {
		super(node);
	}
	

	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static ReassignmentNode withChildren(Token token, ParseNode declaredName, ParseNode initializer) {
		ReassignmentNode node = new ReassignmentNode(token);
		node.appendChild(declaredName);
		node.appendChild(initializer);
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


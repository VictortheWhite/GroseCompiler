package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class ParameterSpecificationNode extends ParseNode {

	public ParameterSpecificationNode(Token token) {
		super(token);
	}

	public ParameterSpecificationNode(ParseNode node) {
		super(node);
	}
	

	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static ParameterSpecificationNode withChildren(Token token, ParseNode type, ParseNode identifierName) {
		ParameterSpecificationNode node = new ParameterSpecificationNode(token);
		node.appendChild(type);
		node.appendChild(identifierName);
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

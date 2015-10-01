package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.StringToken;
import tokens.Token;

public class StringConstantNode extends ParseNode {
	public StringConstantNode(Token token) {
		super(token);
		assert(token instanceof StringToken);
	}
	public StringConstantNode(ParseNode node) {
		super(node);
	}

////////////////////////////////////////////////////////////
// attributes
	
	public String getValue() {
		return StringToken().getValue();
	}

	public StringToken StringToken() {
		return (StringToken)token;
	}	

///////////////////////////////////////////////////////////
// accept a visitor
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}

}

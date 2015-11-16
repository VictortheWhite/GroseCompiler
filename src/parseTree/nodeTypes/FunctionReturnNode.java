package parseTree.nodeTypes;

import lexicalAnalyzer.Keyword;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class FunctionReturnNode extends ParseNode {
	
	public FunctionReturnNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.RETURN));
	}

	public FunctionReturnNode(ParseNode node) {
		super(node);
	}
	
	
	///////////////////////////////////////////////////////////
	// accept 
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}
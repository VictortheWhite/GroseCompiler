package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import tokens.Token;

public class ForStatementNode extends ParseNode {

	public ForStatementNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.FOR));
	}

	public ForStatementNode(ParseNode node) {
		super(node);
	}
	
	
	////////////////////////////////////////////////////////////
	// attributes
	
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static ForStatementNode withChildren(Token token, ParseNode itr, ParseNode array, ParseNode block) {
		ForStatementNode node = new ForStatementNode(token);
		node.appendChild(itr);
		node.appendChild(array);
		node.appendChild(block);
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
package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import tokens.Token;

public class WhileStatementNode extends ParseNode {

	private String startLoopLabel;
	private String endLoopLabel;
	
	public WhileStatementNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.WHILE));
		startLoopLabel = "";
		endLoopLabel = "";
	}

	public WhileStatementNode(ParseNode node) {
		super(node);
		startLoopLabel = "";
		endLoopLabel = "";
	}
	
	
	////////////////////////////////////////////////////////////
	// attributes
	
	public void setStartLabel(String startLabel) {
		this.startLoopLabel = startLabel;
	}
	public void setEndLabel(String endLabel) {
		this.endLoopLabel = endLabel;
	}
	
	public String getStartLabel() {
		return this.startLoopLabel;
	}
	public String getEndLabel() {
		return this.endLoopLabel;
	}
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static WhileStatementNode withChildren(Token token, ParseNode expression, ParseNode block) {
		WhileStatementNode node = new WhileStatementNode(token);
		node.appendChild(expression);
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

package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import tokens.Token;

public class ForStatementNode extends ParseNode {

	private String startLoopLabel;
	private String endLoopLabel;
	private String continueLabel;
	
	public ForStatementNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.FOR));
		startLoopLabel = "";
		endLoopLabel = "";
	}

	public ForStatementNode(ParseNode node) {
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
	public void setContinuelabel(String continueLabel) {
		this.continueLabel = continueLabel;
	}
	
	public String getStartLabel() {
		return this.startLoopLabel;
	}
	public String getEndLabel() {
		return this.endLoopLabel;
	}
	public String getContinueLabel() {
		return this.continueLabel;
	}
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static ForStatementNode withChildren(Token token, ParseNode forCtl, ParseNode block) {
		ForStatementNode node = new ForStatementNode(token);
		node.appendChild(forCtl);
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
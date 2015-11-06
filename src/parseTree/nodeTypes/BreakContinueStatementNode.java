package parseTree.nodeTypes;

import lexicalAnalyzer.Keyword;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class BreakContinueStatementNode extends ParseNode {

	public BreakContinueStatementNode(Token token) {
		super(token);
		assert token.isLextant(Keyword.BREAK, Keyword.CONTINUE);
	}
	public BreakContinueStatementNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// attributes
	
	public boolean isLeagal() {			// whether it is inside a loop
		return findClosestLoopNode() != null;
	}
	
	public String getJumpLabel() {
		ParseNode LoopNode = findClosestLoopNode();
		if(token.isLextant(Keyword.BREAK)) {
			if(LoopNode instanceof ForStatementNode) {
				return ((ForStatementNode)LoopNode).getEndLabel();
			} else if(LoopNode instanceof WhileStatementNode) {
				return ((WhileStatementNode)LoopNode).getEndLabel();
			}
		} else if(token.isLextant(Keyword.CONTINUE)) {
			if(LoopNode instanceof ForStatementNode) {
				return ((ForStatementNode)LoopNode).getContinueLabel();
			} else if(LoopNode instanceof WhileStatementNode) {
				return ((WhileStatementNode)LoopNode).getStartLabel();
			}
		}
		
		assert false;
		return "";
	}
	
	private ParseNode findClosestLoopNode() {
		ParseNode loopNode = null;
		for(ParseNode current: pathToRoot()) {
			if(current instanceof ForStatementNode || current instanceof WhileStatementNode) {
				loopNode = current;
				break;
			}
		}
		return loopNode;
	}

	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}
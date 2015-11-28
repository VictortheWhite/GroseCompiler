package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import tokens.Token;

public class ForControlPhraseNode extends ParseNode {

	private boolean isLowerBoundIncludedInLoop;		// lowerbound expr in Count
	private boolean isUpperBoundIncludedInLoop;		// upperbound
	
	public ForControlPhraseNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.INDEX, Keyword.ELEMENT, Keyword.EVER, Keyword.COUNT, Keyword.PAIR));
	}

	public ForControlPhraseNode(ParseNode node) {
		super(node);
	}
	
	//////////////////////////////////////////////////////////
	public void setLowerBoundIncluded(boolean b) {
		this.isLowerBoundIncludedInLoop = b;
	}
	public void setUpperBoundIncluded(boolean b) {
		this.isUpperBoundIncludedInLoop = b;
	}
	public boolean isLowerBoundIncluded() {
		return this.isLowerBoundIncludedInLoop;
	}
	public boolean isUpperBoundIncluded() {
		return this.isUpperBoundIncludedInLoop;
	}
	
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
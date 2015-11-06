package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import tokens.Token;

public class ForControlPhraseNode extends ParseNode {

	public ForControlPhraseNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.INDEX, Keyword.ELEMENT, Keyword.EVER, Keyword.COUNT));
	}

	public ForControlPhraseNode(ParseNode node) {
		super(node);
	}
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
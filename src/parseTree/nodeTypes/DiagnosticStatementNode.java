package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import tokens.Token;

public class DiagnosticStatementNode extends ParseNode {
	
	public DiagnosticStatementNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.DIAGNOSTIC));
	}

	public DiagnosticStatementNode(ParseNode node) {
		super(node);
	}

	
	////////////////////////////////////////////////////////////
	// attributes
	
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
		
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}

}
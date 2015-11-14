package parseTree.nodeTypes;

import lexicalAnalyzer.Punctuator;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class TupleEntryNode extends ParseNode {

	public TupleEntryNode(Token token) {
		super(token);
		assert(token.isLextant(Punctuator.DOT));
	}

	public TupleEntryNode(ParseNode node) {
		super(node);
	}
	
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static TupleEntryNode withChildren(Token token, ParseNode left, ParseNode right) {
		TupleEntryNode node = new TupleEntryNode(token);
		node.appendChild(left);
		node.appendChild(right);
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

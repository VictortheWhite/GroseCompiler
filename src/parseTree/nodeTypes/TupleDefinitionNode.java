package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import tokens.Token;

public class TupleDefinitionNode extends ParseNode {

	public TupleDefinitionNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.TUPLE));
	}

	public TupleDefinitionNode(ParseNode node) {
		super(node);
	}
	

	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static TupleDefinitionNode withChildren(Token token, ParseNode tupleName, ParseNode parameterTuple) {
		TupleDefinitionNode node = new TupleDefinitionNode(token);
		node.appendChild(tupleName);
		node.appendChild(parameterTuple);
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

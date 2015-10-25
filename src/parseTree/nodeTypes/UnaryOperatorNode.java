
package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import lexicalAnalyzer.Keyword;
import semanticAnalyzer.types.*;
import tokens.LextantToken;
import tokens.Token;

public class UnaryOperatorNode extends ParseNode {

	public UnaryOperatorNode(Token token) {
		super(token);
		assert(token.isLextant(Punctuator.BOOLEANCOMPLIMENT, Keyword.COPY));
	}

	public UnaryOperatorNode(ParseNode node) {
		super(node);
	}
	
	
	////////////////////////////////////////////////////////////
	// attributes
	
	@Override
	public void setType(Type type) {
		if(type instanceof ArrayType) {
			assert ((ArrayType)type).getSubType() instanceof TypeVariable;
			TypeVariable var = (TypeVariable)((ArrayType)type).getSubType();
			super.setType(new ArrayType(var.getType())); 
		} else 
			super.setType(type);
	}
	
	public Lextant getOperator() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}	
	
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static UnaryOperatorNode addAndReturnChildren(Token token, ParseNode currentNode) {
		UnaryOperatorNode node = new UnaryOperatorNode(token);
		//node.appendChild(left);
		currentNode.appendChild(node);
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
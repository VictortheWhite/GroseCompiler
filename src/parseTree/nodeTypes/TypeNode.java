package parseTree.nodeTypes;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;
import semanticAnalyzer.types.*;

public class TypeNode extends ParseNode {
	public TypeNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.INT, Keyword.FLOAT, Keyword.CHAR, Keyword.STRING, Keyword.BOOL));
	}
	public TypeNode(ParseNode node) {
		super(node);
	}

////////////////////////////////////////////////////////////
// attributes
	
	public Lextant getLextant() {
		return LextantToken().getLextant();
	}

	public LextantToken LextantToken() {
		return (LextantToken)token;
	}	
	
	public Type getPrimitiveType() {
		assert(token.isLextant(Keyword.INT, Keyword.FLOAT, Keyword.CHAR, Keyword.STRING, Keyword.BOOL));

		if(token.isLextant(Keyword.INT)) {
			return PrimitiveType.INTEGER;
		} else if(token.isLextant(Keyword.FLOAT)) {
			return PrimitiveType.FLOATING;
		} else if(token.isLextant(Keyword.CHAR)) {
			return PrimitiveType.CHARACTER;
		} else if(token.isLextant(Keyword.STRING)) {
			return PrimitiveType.STRING;
		} else if(token.isLextant(Keyword.BOOL)) {
			return PrimitiveType.BOOLEAN;
		}
		return null;
	}

///////////////////////////////////////////////////////////
// accept a visitor
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}

}

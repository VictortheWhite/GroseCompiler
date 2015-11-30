package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import tokens.LextantToken;
import tokens.Token;

public class DeclarationNode extends ParseNode {

	private boolean isStatic;
	
	public DeclarationNode(Token token, boolean isStatic) {
		super(token);
		this.isStatic = isStatic;
		assert(token.isLextant(Keyword.IMMUTABLE) || token.isLextant(Keyword.VARIABLE));
	}

	public DeclarationNode(ParseNode node) {
		super(node);
	}
	
	
	////////////////////////////////////////////////////////////
	// attributes
	
	public Lextant getDeclarationType() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}	
	
	public boolean isStatic() {
		return this.isStatic;
	}
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static DeclarationNode withChildren(Token token, ParseNode declaredName, ParseNode initializer, boolean isStatic) {
		DeclarationNode node = new DeclarationNode(token, isStatic);
		node.appendChild(declaredName);
		node.appendChild(initializer);
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

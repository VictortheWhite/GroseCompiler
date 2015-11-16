package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import tokens.Token;

public class FunctionDefinitionNode extends ParseNode {

	private String FunctionStartLabel;
	
	public FunctionDefinitionNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.FUNCTION));
	}

	public FunctionDefinitionNode(ParseNode node) {
		super(node);
	}
	
	public String getFunctionStartLable() {
		return this.FunctionStartLabel;
	}
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static FunctionDefinitionNode withChildren(Token token, ParseNode funcName, ParseNode argumentList, ParseNode returnTuple, ParseNode block) {
		FunctionDefinitionNode node = new FunctionDefinitionNode(token);
		node.appendChild(funcName);
		node.appendChild(argumentList);
		node.appendChild(returnTuple);
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
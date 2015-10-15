package parseTree.nodeTypes;
import java.util.List;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import tokens.Token;

public class IfStatementNode extends ParseNode {

	public IfStatementNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.IF));
	}

	public IfStatementNode(ParseNode node) {
		super(node);
	}
	
	
	////////////////////////////////////////////////////////////
	// attributes
	
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static IfStatementNode withChildren(Token token, ParseNode condition, List<ParseNode> blocks) {
		assert blocks.size() >= 1 && blocks.size() <= 2;
		
		IfStatementNode node = new IfStatementNode(token);
		node.appendChild(condition);
		for(ParseNode blockNode: blocks) {
			node.appendChild(blockNode);
		}
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

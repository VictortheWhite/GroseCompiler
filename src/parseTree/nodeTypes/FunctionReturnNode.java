package parseTree.nodeTypes;

import lexicalAnalyzer.Keyword;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import symbolTable.FunctionBinding;
import tokens.Token;

public class FunctionReturnNode extends ParseNode {
	
	public FunctionReturnNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.RETURN));
	}

	public FunctionReturnNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// attributes
	
	public boolean isLeagalReturn() {			// whether it is inside a loop
		return findFunctionDefNode() != null;
	}
	
	public String getReturnLabel() {
		ParseNode FunctionInvocationNode = findFunctionDefNode();
		IdentifierNode funcName = (IdentifierNode)FunctionInvocationNode.child(0);
		FunctionBinding binding = (FunctionBinding)funcName.getBinding();
		
		return binding.getRetureLabel();
	}
	
	private ParseNode findFunctionDefNode() {
		ParseNode funcDefNode = null;
		for(ParseNode current: pathToRoot()) {
			if(current instanceof FunctionDefinitionNode) {
				funcDefNode = current;
				break;
			}
		}
		return funcDefNode;
	}
	
	
	///////////////////////////////////////////////////////////
	// accept 
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}
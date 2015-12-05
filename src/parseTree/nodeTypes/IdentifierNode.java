package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import logging.GrouseLogger;
import symbolTable.Binding;
import symbolTable.FunctionBinding;
import symbolTable.Scope;
import symbolTable.StaticBinding;
import symbolTable.TupleBinding;
import tokens.IdentifierToken;
import tokens.Token;

public class IdentifierNode extends ParseNode {
	private Binding binding;
	private Scope declarationScope;

	public IdentifierNode(Token token) {
		super(token);
		assert(token instanceof IdentifierToken);
		this.binding = null;
	}
	public IdentifierNode(ParseNode node) {
		super(node);
		
		if(node instanceof IdentifierNode) {
			this.binding = ((IdentifierNode)node).binding;
		}
		else {
			this.binding = null;
		}
	}
	
////////////////////////////////////////////////////////////
// attributes
	
	public IdentifierToken identifierToken() {
		return (IdentifierToken)token;
	}

	public void setBinding(Binding binding) {
		this.binding = binding;
	}
	public Binding getBinding() {
		return binding;
	}
	
	public boolean isStatic() {
		return this.binding instanceof StaticBinding;
	}
	
	public String getIsDeclaraedIndicatorLabel() {
		assert this.binding instanceof StaticBinding;
		return ((StaticBinding)this.binding).getIndicatorLabel();
	}
	
////////////////////////////////////////////////////////////
// Speciality functions

	public Binding findVariableBinding() {
		String identifier = token.getLexeme();

		for(ParseNode current : pathToRoot()) {	
			if(current.containsBindingOf(identifier)) {
				declarationScope = current.getScope();
				Binding binding = current.bindingOf(identifier);
				if((binding instanceof TupleBinding) || (binding instanceof FunctionBinding)) {
					break;
				}
				return current.bindingOf(identifier);
			}
		}
		useBeforeDefineError();
		return Binding.nullInstance();
	}
	
	public boolean canBeShadowed() {
		String identifier = token.getLexeme();
		
		for(ParseNode current : pathToRoot()) {
			if(current.containsBindingOf(identifier)) {
				if(!current.bindingOf(identifier).canBeShadowed()) {
					cannotBeShadowedError();
					return false;
				}
			}
		}
		
		return true;
	}

	public Scope getDeclarationScope() {
		findVariableBinding();
		return declarationScope;
	}
	
	/////////////////////////////////////////////////////////////////////////
	// error logging
	public void useBeforeDefineError() {
		GrouseLogger log = GrouseLogger.getLogger("compiler.semanticAnalyzer.identifierNode");
		Token token = getToken();
		log.severe("identifier " + token.getLexeme() + " used before defined at " + token.getLocation());
	}
	
	public void cannotBeShadowedError() {
		GrouseLogger log = GrouseLogger.getLogger("compiler.semanticAnalyzer.identifierNode");
		Token token = getToken();
		log.severe("identifier " + token.getLexeme() + " cannot be shadowed at " + token.getLocation());
	}
	
///////////////////////////////////////////////////////////
// accept a visitor
		
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}

package parseTree.nodeTypes;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import logging.GrouseLogger;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.IdentifierToken;
import tokens.LextantToken;
import tokens.Token;
import semanticAnalyzer.SemanticAnalyzer;
import semanticAnalyzer.types.*;
import symbolTable.Binding;
import symbolTable.Scope;
import symbolTable.SymbolTable;

public class TypeNode extends ParseNode {
	
	
	public TypeNode(Token token) {
		super(token);
		assert(token.isLextant(Punctuator.OPEN_SQUARE_BRACKET, Keyword.INT, Keyword.FLOAT, Keyword.CHAR, Keyword.STRING, Keyword.BOOL) ||
				token instanceof IdentifierToken);
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
	
	public Type getTerminalType() {
		if(token instanceof IdentifierToken)
			return getTupleType();
		else
			return getPrimitiveType();
	}
	
	private Type getPrimitiveType() {
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
	
	private Type getTupleType() {
		Scope globalScope = SemanticAnalyzer.getGlobalScope();
		SymbolTable globalSymbolTable = globalScope.getSymbolTable();
		String tupleName = token.getLexeme();
		
		if(!globalSymbolTable.containsKey(tupleName)) {
			useBeforeDefineError();
		}
		
		Binding binding = globalSymbolTable.lookup(tupleName);
		return binding.getType();
	}

	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static TypeNode withChildren(Token token, ParseNode left) {
		TypeNode node = new TypeNode(token);
		node.appendChild(left);
		return node;
	}
	
	///////////////////////////////////////////////////////////////
	// Error Logger
	public void useBeforeDefineError() {
		GrouseLogger log = GrouseLogger.getLogger("compiler.semanticAnalyzer.TypeNode");
		Token token = getToken();
		log.severe("TupleType " + token.getLexeme() + " used before defined at " + token.getLocation());
	}
	
	///////////////////////////////////////////////////////////
	// accept a visitor
	
	public void accept(ParseNodeVisitor visitor) {
		if(this.nChildren() == 0) {
			visitor.visit(this);
		} 
		else {
			visitor.visitEnter(this);
			visitChildren(visitor);
			visitor.visitLeave(this);
		}		
	}
}

package lexicalAnalyzer;

import tokens.LextantToken;
import tokens.Token;


public enum Punctuator implements Lextant {
	ADD("+"),
	SUB("-"),
	MULTIPLY("*"),
	DIVIDE("/"),
	GREATER(">"),
	LESS("<"),
	EQUAL("=="),
	NOTEQUAL("!="),
	LESSOFEQUAL("<="),
	GREATEROFEQUAL(">="),
	BOOLEANAND("&&"),
	BOOLEANOR("||"),
	BOOLEANCOMPLIMENT("!"),
	ASSIGN(":="),
	CAST(":"),
	SEPARATOR(","),
	TERMINATOR(";"), 
	BAR("|"),
	DOT("."),
	SHARP("#"),
	AT("@"),
	DOLLERSIGN("$"),
	OPEN_PARENTHESIS("("),
	CLOSE_PARENTHESIS(")"),
	OPEN_SQUARE_BRACKET("["),
	CLOSE_SQUARE_BRACKET("]"),
	ARROW("->"),
	OPEN_BRACE("{"),
	CLOSE_BRACE("}"),
	NULL_PUNCTUATOR("");

	private String lexeme;
	private Token prototype;
	
	private Punctuator(String lexeme) {
		this.lexeme = lexeme;
		this.prototype = LextantToken.make(null, lexeme, this);
	}
	public String getLexeme() {
		return lexeme;
	}
	public Token prototype() {
		return prototype;
	}
	
	
	public static Punctuator forLexeme(String lexeme) {
		for(Punctuator punctuator: values()) {
			if(punctuator.lexeme.equals(lexeme)) {
				return punctuator;
			}
		}
		return NULL_PUNCTUATOR;
	}
	
/*
	//   the following hashtable lookup can replace the implementation of forLexeme above. It is faster but less clear. 
	private static LexemeMap<Punctuator> lexemeToPunctuator = new LexemeMap<Punctuator>(values(), NULL_PUNCTUATOR);
	public static Punctuator forLexeme(String lexeme) {
		   return lexemeToPunctuator.forLexeme(lexeme);
	}
*/
	
}



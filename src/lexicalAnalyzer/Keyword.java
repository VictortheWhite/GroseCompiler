package lexicalAnalyzer;

import tokens.LextantToken;
import tokens.Token;


public enum Keyword implements Lextant {
	IMMUTABLE("imm"),
	VARIABLE("var"),
	STATIC("static"),
	FRESH("fresh"),
	LET("let"),
	PRINT("print"),
	COPY("copy"),
	NEWLINE("nl"),
	INT("int"),
	FLOAT("float"),
	CHAR("char"),
	STRING("string"),
	BOOL("bool"),
	ARRAY("array"),
	TUPLE("tuple"),
	NULL("null"),
	TRUE("true"),
	FALSE("false"),
	IF("if"),
	ELSE("else"),
	WHILE("while"),
	FOR("for"),
	INDEX("index"),
	ELEMENT("element"),
	PAIR("pair"),
	OF("of"),
	EVER("ever"),
	COUNT("count"),
	BREAK("break"),
	CONTINUE("continue"),
	FUNCTION("func"),
	RETURN("return"),
	CALL("call"),
	MAIN("main"),
	NULL_KEYWORD("");

	private String lexeme;
	private Token prototype;
	
	
	private Keyword(String lexeme) {
		this.lexeme = lexeme;
		this.prototype = LextantToken.make(null, lexeme, this);
	}
	public String getLexeme() {
		return lexeme;
	}
	public Token prototype() {
		return prototype;
	}
	
	public static Keyword forLexeme(String lexeme) {
		for(Keyword keyword: values()) {
			if(keyword.lexeme.equals(lexeme)) {
				return keyword;
			}
		}
		return NULL_KEYWORD;
	}
	public static boolean isAKeyword(String lexeme) {
		return forLexeme(lexeme) != NULL_KEYWORD;
	}
	
	/*   the following hashtable lookup can replace the serial-search implementation of forLexeme above. It is faster but less clear. 
	private static LexemeMap<Keyword> lexemeToKeyword = new LexemeMap<Keyword>(values(), NULL_KEYWORD);
	public static Keyword forLexeme(String lexeme) {
		return lexemeToKeyword.forLexeme(lexeme);
	}
	*/
}

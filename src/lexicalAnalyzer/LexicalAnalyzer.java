package lexicalAnalyzer;


import logging.GrouseLogger;
import inputHandler.InputHandler;
import inputHandler.LocatedChar;
import inputHandler.LocatedCharStream;
import inputHandler.PushbackCharStream;
import inputHandler.TextLocation;
import tokens.FloatingToken;
import tokens.IdentifierToken;
import tokens.IntegerToken;
import tokens.LextantToken;
import tokens.NullToken;
import tokens.Token;

import static lexicalAnalyzer.PunctuatorScanningAids.*;

public class LexicalAnalyzer extends ScannerImp implements Scanner {
	
	public static LexicalAnalyzer make(String filename) {
		InputHandler handler = InputHandler.fromFilename(filename);
		PushbackCharStream charStream = PushbackCharStream.make(handler);
		return new LexicalAnalyzer(charStream);
	}

	public LexicalAnalyzer(PushbackCharStream input) {
		super(input);
	}

	
	//////////////////////////////////////////////////////////////////////////////
	// Token-finding main dispatch	

	@Override
	protected Token findNextToken() {
		LocatedChar ch = nextNonWhitespaceChar();
		
		if(isNumberStart(ch)) {
			return scanNumber(ch);
		}
		else if(isIdentifierStart(ch)) {
			return scanIdentifier(ch);
		}
		else if(isPunctuatorStart(ch)) {
			//return PunctuatorScanner.scan(ch, input);
			return ScanPunctuatorAndComments(ch, input);
		}
		else if(isEndOfInput(ch)) {
			return NullToken.make(ch.getLocation());
		}
		else {
			lexicalError(ch);
			return findNextToken();
		}
	}


	private LocatedChar nextNonWhitespaceChar() {
		LocatedChar ch = input.next();
		while(ch.isWhitespace()) {
			ch = input.next();
		}
		return ch;
	}
	
	
	//////////////////////////////////////////////////////////////////////////////
	// NumberStart
	private boolean isNumberStart(LocatedChar ch) {
		return ch.isDigit() || isMinusNumberStart(ch);
	}
	
	private boolean isMinusNumberStart(LocatedChar ch) {
		if(ch.getCharacter() == '-') {
			LocatedChar nextChar = input.next();
			input.pushback(nextChar);
			if(nextChar.isDigit()) 
				return true;
		}
		
		return false;
	}
	
	// Integer lexical analysis	

	
	private Token scanNumber(LocatedChar firstChar) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(firstChar.getCharacter());
		//appendSubsequentDigits(buffer);
		if(CompleteNumber(buffer))	//true for floating, false for integer
			return FloatingToken.make(firstChar.getLocation(), buffer.toString());
		else
			return IntegerToken.make(firstChar.getLocation(), buffer.toString());			
	}
	
	private boolean CompleteNumber(StringBuffer buffer) {
		appendSubsequentDigits(buffer);
		LocatedChar c = input.next();
		if(c.getCharacter() == '.') {
			LocatedChar nextChar = input.next();
			if(nextChar.isDigit()) {
				buffer.append(c.getCharacter());
				buffer.append(nextChar.getCharacter());
				appendSubsequentDigits(buffer);
				scanExponentOfFloating(buffer);
				return true;
			} else {
				input.pushback(nextChar);
			}
		}
		
		input.pushback(c);
		
		return false;
	}
	
	private void appendSubsequentDigits(StringBuffer buffer) {
		LocatedChar c = input.next();
		while(c.isDigit()) {
			buffer.append(c.getCharacter());
			c = input.next();
		}
		input.pushback(c);
	}
	
	private void scanExponentOfFloating(StringBuffer buffer) {
		LocatedChar c = input.next();
		if(c.getCharacter()=='e') {
			LocatedChar nextChar = input.next();
			if(isNumberStart(nextChar)) {
				buffer.append(c.getCharacter());
				buffer.append(nextChar.getCharacter());
				appendSubsequentDigits(buffer);
				return;
			} else 
				input.pushback(nextChar);
		}
		
		input.pushback(c);
	}
	
	
	//////////////////////////////////////////////////////////////////////////////
	// Identifier and keyword lexical analysis	
	
	private boolean isIdentifierStart(LocatedChar ch) {
		if(ch.isLowerCase())
			return true;
		else if(ch.isUpperCase())
			return true;
		else
			return false;
	}
	
	private Token scanIdentifier(LocatedChar firstChar) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(firstChar.getCharacter());
		//appendSubsequentLowercase(buffer);
		findWholeIdentifier(buffer);

		String lexeme = buffer.toString();
		
		//whether identifier length exceeds 32
		if(lexeme.length() > 32) {
			//which char to issue error is to be considered
			lexicalError(input.next());
		}
		
		if(Keyword.isAKeyword(lexeme)) {
			return LextantToken.make(firstChar.getLocation(), lexeme, Keyword.forLexeme(lexeme));
		}
		else {
			return IdentifierToken.make(firstChar.getLocation(), lexeme);
		}
	}
	
	private void findWholeIdentifier(StringBuffer buffer) {
		LocatedChar c = input.next();
		while(isIdentifierChar(c)) {
			buffer.append(c.getCharacter());
			c = input.next();
		}
		input.pushback(c);
	}
	
	private boolean isIdentifierChar(LocatedChar ch) {
		if(ch.isLowerCase())
			return true;
		else if(ch.isUpperCase())
			return true;
		else if(ch.isDigit())
			return true;
		else if(ch.getCharacter()=='_' || ch.getCharacter()=='~')
			return true;
		else 
			return false;
	}
	
	@SuppressWarnings("unused")
	// old method to find whole identifier
	private void appendSubsequentLowercase(StringBuffer buffer) {
		LocatedChar c = input.next();
		while(c.isLowerCase()) {
			buffer.append(c.getCharacter());
			c = input.next();
		}
		input.pushback(c);
	}
	
	
	//////////////////////////////////////////////////////////////////////////////
	// Judge whether there's comments starter
	
	private Token ScanPunctuatorAndComments(LocatedChar ch, PushbackCharStream input)
	{
		LocatedChar tempC = input.next();
		if(tempC.getCharacter() == '/'){		// double '/' = comments starter
			return eliminateComments(tempC, input);
		} else {
			input.pushback(tempC);
			return PunctuatorScanner.scan(ch, input);
		}
	}
	
	
	//////////////////////////////////////////////////////////////////////////////
	// Punctuator lexical analysis	
	// old method left in to show a simple scanning method.
	// current method is the algorithm object PunctuatorScanner.java

	@SuppressWarnings("unused")
	private Token oldScanPunctuator(LocatedChar ch) {
		TextLocation location = ch.getLocation();
		
		switch(ch.getCharacter()) {
		case '*':
			return LextantToken.make(location, "*", Punctuator.MULTIPLY);
		case '+':
			return LextantToken.make(location, "+", Punctuator.ADD);
		/*case '/':{
			LocatedChar c_temp = input.next();
			if(c_temp.getCharacter()=='/'){
				System.out.println("shit");
				return eliminateComments(location,c_temp);
			} else {
				//push new char back
				input.pushback(c_temp);
				return LextantToken.make(location, "/" , Punctuator.DIVIDE);
			}
		}*/
		case '>':
			return LextantToken.make(location, ">", Punctuator.GREATER);
		case ':':
			if(ch.getCharacter()=='=') {
				return LextantToken.make(location, ":=", Punctuator.ASSIGN);
			}
			else {
				throw new IllegalArgumentException("found : not followed by = in scanOperator");
			}
		case ',':
			return LextantToken.make(location, ",", Punctuator.SEPARATOR);
		case ';':
			return LextantToken.make(location, ";", Punctuator.TERMINATOR);
		default:
			throw new IllegalArgumentException("bad LocatedChar " + ch + "in scanOperator");
		}
	}

	

	//////////////////////////////////////////////////////////////////////////////
	// Character-classification routines specific to Grouse scanning.	

	private boolean isPunctuatorStart(LocatedChar lc) {
		char c = lc.getCharacter();
		return isPunctuatorStartingCharacter(c);
	}

	private boolean isEndOfInput(LocatedChar lc) {
		return lc == LocatedCharStream.FLAG_END_OF_INPUT;
	}
	
	
	//////////////////////////////////////////////////////////////////////////////
	// Error-reporting	

	private void lexicalError(LocatedChar ch) {
		GrouseLogger log = GrouseLogger.getLogger("compiler.lexicalAnalyzer");
		log.severe("Lexical error: invalid character " + ch);
	}
	
	private Token eliminateComments(LocatedChar ch, PushbackCharStream input){
		
		//System.out.println("comments found here");
		LocatedChar ch1 = ch;
		LocatedChar ch2 = input.next();
		
		if(ch2.getCharacter()!='\n'){	
			while(true){
				ch1 = ch2;
				ch2 = input.next();
				if((ch1.getCharacter()=='/' && ch2.getCharacter()=='/') || ch2.getCharacter() == '\n')
					break;
			}
		}
		return findNextToken();
	}
	
}

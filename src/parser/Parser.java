package parser;

import java.util.Arrays;

import logging.GrouseLogger;
import parseTree.*;
import parseTree.nodeTypes.*;
import tokens.*;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import lexicalAnalyzer.Scanner;


public class Parser {
	private Scanner scanner;
	private Token nowReading;
	private Token previouslyRead;
	
	public static ParseNode parse(Scanner scanner) {
		Parser parser = new Parser(scanner);
		return parser.parse();
	}
	public Parser(Scanner scanner) {
		super();
		this.scanner = scanner;
	}
	
	public ParseNode parse() {
		readToken();
		return parseProgram();
	}

	////////////////////////////////////////////////////////////
	// "program" is the start symbol S
	// S -> MAIN mainBlock
	
	private ParseNode parseProgram() {
		if(!startsProgram(nowReading)) {
			return syntaxErrorNode("program");
		}
		ParseNode program = new ProgramNode(nowReading);
		
		expect(Keyword.MAIN);
		ParseNode mainBlock = parseMainBlock();
		program.appendChild(mainBlock);
		
		if(!(nowReading instanceof NullToken)) {
			return syntaxErrorNode("end of program");
		}
		
		return program;
	}
	private boolean startsProgram(Token token) {
		return token.isLextant(Keyword.MAIN);
	}
	
	
	///////////////////////////////////////////////////////////
	// mainBlock
	
	// mainBlock -> { statement* }
	private ParseNode parseMainBlock() {
		if(!startsMainBlock(nowReading)) {
			return syntaxErrorNode("mainBlock");
		}
		ParseNode mainBlock = new MainBlockNode(nowReading);
		expect(Punctuator.OPEN_BRACE);
		
		while(startsStatement(nowReading)) {
			ParseNode statement = parseStatement();
			mainBlock.appendChild(statement);
		}
		expect(Punctuator.CLOSE_BRACE);
		return mainBlock;
	}
	private boolean startsMainBlock(Token token) {
		return token.isLextant(Punctuator.OPEN_BRACE);
	}
	
	
	///////////////////////////////////////////////////////////
	// statements
	
	// statement-> declaration | printStmt
	private ParseNode parseStatement() {
		if(!startsStatement(nowReading)) {
			return syntaxErrorNode("statement");
		}
		if(startsDeclaration(nowReading)) {
			return parseDeclaration();
		}
		if(startsReassignment(nowReading)) {
			return parseReassignment();
		}
		if(startsPrintStatement(nowReading)) {
			return parsePrintStatement();
		}
		assert false : "bad token " + nowReading + " in parseStatement()";
		return null;
	}
	private boolean startsStatement(Token token) {
		return startsPrintStatement(token) ||
			   startsDeclaration(token) ||
			   startsReassignment(token);
	}
	
	// printStmt -> PRINT printExpressionList ;
	private ParseNode parsePrintStatement() {
		if(!startsPrintStatement(nowReading)) {
			return syntaxErrorNode("print statement");
		}
		PrintStatementNode result = new PrintStatementNode(nowReading);
		
		readToken();
		result = parsePrintExpressionList(result);
		
		expect(Punctuator.TERMINATOR);
		return result;
	}
	private boolean startsPrintStatement(Token token) {
		return token.isLextant(Keyword.PRINT);
	}	

	// This adds the printExpressions it parses to the children of the given parent
	// printExpressionList -> printExpression*   (note that this is nullable)

	private PrintStatementNode parsePrintExpressionList(PrintStatementNode parent) {
		while(startsPrintExpression(nowReading)) {
			parsePrintExpression(parent);
		}
		return parent;
	}
	

	// This adds the printExpression it parses to the children of the given parent
	// printExpression -> expr? ,? nl? 
	
	private void parsePrintExpression(PrintStatementNode parent) {
		if(startsExpression(nowReading)) {
			ParseNode child = parseExpression();
			parent.appendChild(child);
		}
		if(nowReading.isLextant(Punctuator.SEPARATOR)) {
			readToken();
			ParseNode child = new SeparatorNode(previouslyRead);
			parent.appendChild(child);
		}
		if(nowReading.isLextant(Keyword.NEWLINE)) {
			readToken();
			ParseNode child = new NewlineNode(previouslyRead);
			parent.appendChild(child);
		}
	}
	private boolean startsPrintExpression(Token token) {
		return startsExpression(token) || token.isLextant(Punctuator.SEPARATOR, Keyword.NEWLINE) ;
	}
	
	// declaration -> IMMUTABLE identifier := expression ;
	private ParseNode parseDeclaration() {
		if(!startsDeclaration(nowReading)) {
			return syntaxErrorNode("declaration");
		}
		Token declarationToken = nowReading;
		readToken();
		
		ParseNode identifier = parseIdentifier();
		expect(Punctuator.ASSIGN);
		ParseNode initializer = parseExpression();
		expect(Punctuator.TERMINATOR);
		
		return DeclarationNode.withChildren(declarationToken, identifier, initializer);
	}
	private boolean startsDeclaration(Token token) {
		return token.isLextant(Keyword.IMMUTABLE) || token.isLextant(Keyword.VARIABLE);
	}
	
	// reassignment -> let identifier := expression;
	private ParseNode parseReassignment() {
		if(!startsReassignment(nowReading)) {
			return syntaxErrorNode("reassignment");
		}
		Token reassignmentToken = nowReading;
		readToken();
		ParseNode identifier = parseIdentifier();
		expect(Punctuator.ASSIGN);
		ParseNode initializer = parseExpression();
		expect(Punctuator.TERMINATOR);
		
		return ReassignmentNode.withChildren(reassignmentToken, identifier, initializer);
	}
	
	private boolean startsReassignment(Token token) {
		return token.isLextant(Keyword.LET);
	}

	
	///////////////////////////////////////////////////////////
	// expressions
	// expr  -> expr0
	// expr0 -> expr1 [ && expr1 | ||expr1]*
	// expr1 -> expr2 [> expr2]*
	// expr2 -> expr3 [+ expr3]*  (left-assoc)
	// expr3 -> expr4 [MULT expr4]*  (left-assoc)
	// expr3.5 -> expr4:type 
	// expr4 -> literal
	//			parenthesis
	// literal -> intNumber | floatingNumber | identifier | booleanConstant | characterConstant | stringConstant

	// expr  -> expr1
	private ParseNode parseExpression() {		
		if(!startsExpression(nowReading)) {
			return syntaxErrorNode("expression");
		}
		return parseExpression0();
	}
	private boolean startsExpression(Token token) {
		return startsExpression0(token);
	}

	// expr0 -> expr1 [ && expr1 | ||expr1]*
	private ParseNode parseExpression0() {
		if(!startsExpression0(nowReading)) {
			return syntaxErrorNode("expression<0>");
		}
		ParseNode left = parseExpression1();
		while(isLextantBooleanOperator(nowReading)) {
			Token booleanOperatorToken = nowReading;
			readToken();
			ParseNode right = parseExpression1();
			
			left = BinaryOperatorNode.withChildren(booleanOperatorToken, left, right);
		}
		return left;
	}
	
	private boolean isLextantBooleanOperator(Token nowReading) {
		return nowReading.isLextant(Punctuator.BOOLEANAND) || nowReading.isLextant(Punctuator.BOOLEANOR);
	}
	
	private boolean startsExpression0(Token nowReading) {
		return startsExpression1(nowReading);
	}
	
	
	// expr1 -> expr2 [> expr2]?
	private ParseNode parseExpression1() {
		if(!startsExpression1(nowReading)) {
			return syntaxErrorNode("expression<1>");
		}
		
		ParseNode left = parseExpression2();
		while(isLextantComparisonOperator(nowReading)) {
			Token compareToken = nowReading;
			readToken();
			ParseNode right = parseExpression2();
			
			left = BinaryOperatorNode.withChildren(compareToken, left, right);
		}
		return left;

	}
	private boolean isLextantComparisonOperator(Token nowReading) {
		return nowReading.isLextant(Punctuator.GREATER) || nowReading.isLextant(Punctuator.LESS) || nowReading.isLextant(Punctuator.EQUAL) 
				|| nowReading.isLextant(Punctuator.NOTEQUAL) || nowReading.isLextant(Punctuator.GREATEROFEQUAL) || nowReading.isLextant(Punctuator.LESSOFEQUAL);
	}
	private boolean startsExpression1(Token token) {
		return startsExpression2(token);
	}

	// expr2 -> expr3 [+ expr3]*  (left-assoc)
	private ParseNode parseExpression2() {
		if(!startsExpression2(nowReading)) {
			return syntaxErrorNode("expression<2>");
		}
		
		ParseNode left = parseExpression3();
		while(true) {
			if(nowReading.isLextant(Punctuator.ADD)) {
				Token additiveToken = nowReading;
				readToken();
				ParseNode right = parseExpression3();
				
				left = BinaryOperatorNode.withChildren(additiveToken, left, right);
			} else if(nowReading.isLextant(Punctuator.SUB)) {
				Token subtractiveToken = nowReading;
				readToken();
				ParseNode right = parseExpression3();
				
				left = BinaryOperatorNode.withChildren(subtractiveToken, left, right);
			} else 
				break;
		}
		return left;
	}
	private boolean startsExpression2(Token token) {
		//return startsLiteral(token);
		return startsExpression3(token);
	}	

	// expr3 -> expr4 [MULT expr4]*  (left-assoc)
	private ParseNode parseExpression3() {
		if(!startsExpression3(nowReading)) {
			return syntaxErrorNode("expression<3>");
		}
		
		ParseNode left = parseExpression7over2();
		/*
		while(nowReading.isLextant(Punctuator.MULTIPLY)) {
			Token multiplicativeToken = nowReading;
			readToken();
			ParseNode right = parseExpression4();
			
			left = BinaryOperatorNode.withChildren(multiplicativeToken, left, right);
		}*/
		while(true){
			if(nowReading.isLextant(Punctuator.MULTIPLY)) {
				Token multiplicativeToken = nowReading;
				readToken();
				ParseNode right = parseExpression7over2();
				
				left = BinaryOperatorNode.withChildren(multiplicativeToken, left, right);
			} else if(nowReading.isLextant(Punctuator.DIVIDE)) {
				Token divisiveToken = nowReading;
				readToken();
				ParseNode right = parseExpression7over2();
				
				left = BinaryOperatorNode.withChildren(divisiveToken, left, right);
			} else {
				break;
			}
		}
		
		return left;
	}
	private boolean startsExpression3(Token token) {
		return startsExpression7over2(token);
	}
	
	// expr3.5 -> expr4:type
	
	private ParseNode parseExpression7over2() {
		if(!startsExpression4(nowReading)) {
			return syntaxErrorNode("expression<3.5>");
		}
		
		ParseNode left = parseExpression4();
		while(nowReading.isLextant(Punctuator.CAST)) {
			Token castingToken = nowReading;
			ParseNode right = null;
			readToken();
			if(nowReading.isLextant(Keyword.INT, Keyword.FLOAT, Keyword.CHAR, Keyword.STRING, Keyword.BOOL)) {
				right = new TypeNode(nowReading);
			} else {
				return syntaxErrorNode("expression casting: expected type");
			}
			
			left = BinaryOperatorNode.withChildren(castingToken, left, right);
			readToken();
		}
		
		return left;
		
	}
	private boolean startsExpression7over2(Token token) {
		return startsExpression4(token);
	}
	// expr4 -> literal | parenthesis
	private ParseNode parseExpression4() {
		if(!startsExpression4(nowReading)) {
			return syntaxErrorNode("expression<4>");
		}
		if(startsLiteral(nowReading))
			return parseLiteral();
		else if(startsParenthesis(nowReading)) {
			return parseParenthesis();
		}
		return syntaxErrorNode("expression<4>");
	}
	private boolean startsExpression4(Token token) {
		return startsLiteral(token) || startsParenthesis(token);
	}

	// Parenthesis
	private ParseNode parseParenthesis() {
		readToken();
		ParseNode right = parseExpression();
		expect(Punctuator.CLOSE_PARENTHESIS);
		return right;
	}
	private boolean startsParenthesis(Token token) {
		return token.isLextant(Punctuator.OPEN_PARENTHESIS);
	}
	
	// literal -> number | identifier | booleanConstant
	private ParseNode parseLiteral() {
		if(!startsLiteral(nowReading)) {
			return syntaxErrorNode("literal");
		}
		
		if(startsIntNumber(nowReading)) {
			return parseIntNumber();
		}
		if(startsFloatingNumber(nowReading)) {
			return parseFloatingNumber();
		}
		/*if(startsIntNumber(nowReading)) {
			return parseFloatingNumber();
		}*/
		if(startsIdentifier(nowReading)) {
			return parseIdentifier();
		}
		if(startsBooleanConstant(nowReading)) {
			return parseBooleanConstant();
		}
		if(startsCharacterConstant(nowReading)) {
			return parseCharacterConstant();
		}
		if(startsStringConstant(nowReading)) {
			return parseStringConstant();
		}
		assert false : "bad token " + nowReading + " in parseLiteral()";
		return null;
	}
	private boolean startsLiteral(Token token) {
		return startsIntNumber(token) || startsFloatingNumber(token) || startsIdentifier(token) || startsBooleanConstant(token)
				|| startsCharacterConstant(token) || startsStringConstant(token);
	}

	// number (terminal)
	private ParseNode parseIntNumber() {
		if(!startsIntNumber(nowReading)) {
			return syntaxErrorNode("integer constant");
		}
		readToken();
		return new IntegerConstantNode(previouslyRead);
	}
	private ParseNode parseFloatingNumber() {
		if(!startsFloatingNumber(nowReading)) {
			return syntaxErrorNode("floating constant");
		}
		readToken();
		return new FloatingConstantNode(previouslyRead);
		
	}
	private boolean startsIntNumber(Token token) {
		return token instanceof IntegerToken;//NumberToken;
	}
	
	private boolean startsFloatingNumber(Token token) {
		return token instanceof FloatingToken;
	}

	// identifier (terminal)
	private ParseNode parseIdentifier() {
		if(!startsIdentifier(nowReading)) {
			return syntaxErrorNode("identifier");
		}
		readToken();
		return new IdentifierNode(previouslyRead);
	}
	private boolean startsIdentifier(Token token) {
		return token instanceof IdentifierToken;
	}

	// boolean constant (terminal)
	private ParseNode parseBooleanConstant() {
		if(!startsBooleanConstant(nowReading)) {
			return syntaxErrorNode("boolean constant");
		}
		readToken();
		return new BooleanConstantNode(previouslyRead);
	}
	private boolean startsBooleanConstant(Token token) {
		return token.isLextant(Keyword.TRUE, Keyword.FALSE);
	}
	
	// Character and String (terminal)
	private ParseNode parseCharacterConstant() {
		if(!startsCharacterConstant(nowReading)) {
			return syntaxErrorNode("character constant");
		}
		readToken();
		return new CharacterConstantNode(previouslyRead);
	}
	
	private ParseNode parseStringConstant() {
		if(!startsStringConstant(nowReading)) {
			return syntaxErrorNode("string constant");
		}
		readToken();
		return new StringConstantNode(previouslyRead);
	}
	
	private boolean startsCharacterConstant(Token token) {
		return token instanceof CharacterToken;
	}
	
	private boolean startsStringConstant(Token token) {
		return token instanceof StringToken;
	}
	
	//-------------------------------------------
	private void readToken() {
		previouslyRead = nowReading;
		nowReading = scanner.next();
	}	
	
	// if the current token is one of the given lextants, read the next token.
	// otherwise, give a syntax error and read next token (to avoid endless looping).
	private void expect(Lextant ...lextants ) {
		if(!nowReading.isLextant(lextants)) {
			syntaxError(nowReading, "expecting " + Arrays.toString(lextants));
		}
		readToken();
	}	
	private ErrorNode syntaxErrorNode(String expectedSymbol) {
		syntaxError(nowReading, "expecting " + expectedSymbol);
		ErrorNode errorNode = new ErrorNode(nowReading);
		readToken();
		return errorNode;
	}
	private void syntaxError(Token token, String errorDescription) {
		String message = "" + token.getLocation() + " " + errorDescription;
		error(message);
	}
	private void error(String message) {
		GrouseLogger log = GrouseLogger.getLogger("compiler.Parser");
		log.severe("syntax error: " + message);
	}	
}


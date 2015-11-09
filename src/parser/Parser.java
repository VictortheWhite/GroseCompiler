package parser;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

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
	// S -> globalDefinition* MAIN mainBlock
	
	private ParseNode parseProgram() {
		if(!startsProgram(nowReading)) {
			return syntaxErrorNode("program");
		}
		ParseNode program = new ProgramNode(nowReading);
		
		while(startsGlobalDefinition(nowReading)) {
			ParseNode globalDef = parseGlobalDefinition();
			program.appendChild(globalDef);
		}
		
		expect(Keyword.MAIN);
		ParseNode mainBlock = parseMainBlock();
		program.appendChild(mainBlock);
		
		if(!(nowReading instanceof NullToken)) {
			return syntaxErrorNode("end of program");
		}
		
		return program;
	}
	private boolean startsProgram(Token token) {
		return token.isLextant(Keyword.MAIN) || startsGlobalDefinition(token);
	}
	
	///////////////////////////////////////////////////////////
	// globalDefinition
	
	// globalDefinition -> tupleDefinition
	//					   functionDefinition
	private ParseNode parseGlobalDefinition() {
		if(!startsGlobalDefinition(nowReading)) {
			return syntaxErrorNode("Global Definiton");
		}
		
		if(startsTupleDefinition(nowReading))
			return parseTupleDefinition();
		
		assert false : "bad token " + nowReading + " in parseGlobalDefinition()";
		return null;		
	}
	
	private boolean startsGlobalDefinition(Token token) {
		return startsTupleDefinition(token);
	}
	
	// tupleDefinition
	// tupleDefinition -> tuple identifier parameterTuple
	private ParseNode parseTupleDefinition() {
		if(!startsTupleDefinition(nowReading)) {
			return syntaxErrorNode("TupleDefinition");
		}
		Token tupleDefToken = nowReading;
		readToken();
		
		ParseNode tupleName = parseIdentifier();
		ParseNode parameterTuple = parseParameterTuple();
		expect(Punctuator.TERMINATOR);
		return TupleDefinitionNode.withChildren(tupleDefToken, tupleName, parameterTuple);
	}
	
	private boolean startsTupleDefinition(Token token) {
		return token.isLextant(Keyword.TUPLE);
	}
	
	// parameterTuple -> ( parameterList )
	//					| identifier
	
	private ParseNode parseParameterTuple() {
		if(nowReading.isLextant(Punctuator.OPEN_PARENTHESIS)) {
			ParameterListNode result = new ParameterListNode(nowReading);
			readToken();
			result = parseParameterList(result);
			expect(Punctuator.CLOSE_PARENTHESIS);
			
			return result;
		} else {
			return parseType();
		}
	}
	
	// parameterList -> [ parameterSpecification [, parameterSpecification]* ]  
	// zero or more comma-seperated parameterSpecifications
	
	private ParameterListNode parseParameterList(ParameterListNode parent) {
		if(startsParameterSpecification(nowReading)) {
			ParseNode paraSpecNode = parseParameterSpecification();
			parent.appendChild(paraSpecNode);
			
			while(nowReading.isLextant(Punctuator.SEPARATOR)) {
				readToken();
				paraSpecNode = parseParameterSpecification();
				parent.appendChild(paraSpecNode);
			}
		}
		
		return parent;
	}
	// parameterSpecification -> type identifier
	private ParseNode parseParameterSpecification() {
		if(!startsParameterSpecification(nowReading)) {
			return syntaxErrorNode("Parameter Specification");
		}
		
		Token ParaSpecToken = nowReading;
		ParseNode type = parseType();
		ParseNode attributeName = parseIdentifier();
		
		return ParameterSpecificationNode.withChildren(ParaSpecToken, type, attributeName);
	}
	
	private boolean startsParameterSpecification(Token token) {
		return startsType(token);
	}
	
	///////////////////////////////////////////////////////////
	// MainBlock
	
	// MainBlock -> { statement* }
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
	// Block
	// Block -> { statement* }
	private ParseNode parseBlock() {
		if(!startsBlock(nowReading)) {
			return syntaxErrorNode("Block");
		}
		ParseNode Block = new BlockNode(nowReading);
		expect(Punctuator.OPEN_BRACE);
		
		while(startsStatement(nowReading)) {
			ParseNode statement = parseStatement();
			Block.appendChild(statement);
		}
		expect(Punctuator.CLOSE_BRACE);
		return Block;
	}
	
	private boolean startsBlock(Token token) {
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
		if(startsIfStatement(nowReading)) {
			return parseIfStatement();
		}
		if(startsWhileStatement(nowReading)) {
			return parseWhileStatement();
		}
		if(startsForStatement(nowReading)) {
			return parseForStatement();
		}
		if(startsBreakContinue(nowReading)) {
			return parseBreakContinue();
		}
		if(startsBlock(nowReading)) {
			return parseBlock();
		}
		assert false : "bad token " + nowReading + " in parseStatement()";
		return null;
	}
	private boolean startsStatement(Token token) {
		return startsPrintStatement(token) ||
			   startsDeclaration(token) ||
			   startsReassignment(token) ||
			   startsIfStatement(token) ||
			   startsWhileStatement(token) ||
			   startsForStatement(token) ||
			   startsBlock(token) ||
			   startsBreakContinue(token);
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
	
	// reassignment -> let target := expression;
	private ParseNode parseReassignment() {
		if(!startsReassignment(nowReading)) {
			return syntaxErrorNode("reassignment");
		}
		Token reassignmentToken = nowReading;
		readToken();
		ParseNode target = parseExpression();
		expect(Punctuator.ASSIGN);
		ParseNode initializer = parseExpression();
		expect(Punctuator.TERMINATOR);
		
		return ReassignmentNode.withChildren(reassignmentToken, target, initializer);
	}

	private boolean startsReassignment(Token token) {
		return token.isLextant(Keyword.LET);
	}
	
	// ifStmt -> if (expr) block (else block)?
	private ParseNode parseIfStatement() {
		if(!startsIfStatement(nowReading)) {
			return syntaxErrorNode("IfStatement");
		}
		Token ifStatementToken = nowReading;
		readToken();
		expect(Punctuator.OPEN_PARENTHESIS);
		ParseNode condition = parseExpression();
		expect(Punctuator.CLOSE_PARENTHESIS);
		ParseNode IfBlock = parseBlock();
		List<ParseNode> blocks = new Vector<ParseNode>();
		blocks.add(IfBlock);
		
		//deal with "else"
		if(nowReading.isLextant(Keyword.ELSE)) {
			ParseNode elseBlock = parseElseStatement();
			blocks.add(elseBlock);
		}
		return IfStatementNode.withChildren(ifStatementToken, condition, blocks);
		
	}
	private ParseNode parseElseStatement() {
		if(!nowReading.isLextant(Keyword.ELSE)) {
			return syntaxErrorNode("ElseStatement");
		}
		readToken();
		return parseBlock();
	}
	
	private boolean startsIfStatement(Token token) {
		return token.isLextant(Keyword.IF);
	}
	
	// whileStmt -> while (expr) block
	private ParseNode parseWhileStatement() {
		if(!startsWhileStatement(nowReading)) {
			return syntaxErrorNode("whileStatement");
		}
		Token WhileStatementToken = nowReading;
		readToken();
		expect(Punctuator.OPEN_PARENTHESIS);
		ParseNode condition = parseWhileConditon();
		expect(Punctuator.CLOSE_PARENTHESIS);
		ParseNode block = parseBlock();
		return WhileStatementNode.withChildren(WhileStatementToken, condition, block);
	}
	private boolean startsWhileStatement(Token token) {
		return token.isLextant(Keyword.WHILE);
	}
	
	private ParseNode parseWhileConditon() {
		if(!startsWhileCondition(nowReading)) {
			return syntaxErrorNode("while condition");
		}
		
		if(startsExpression(nowReading)) 
			return parseExpression();
		if(nowReading.isLextant(Keyword.EVER)) {
			readToken();
			return new ForControlPhraseNode(previouslyRead);
		}
		
		assert false;
		return null;
	}
	private boolean startsWhileCondition(Token token) {
		return startsExpression(token) || token.isLextant(Keyword.EVER);
	}
	// forStem -> for ( forControlPhrase ) block
	private ParseNode parseForStatement() {
		if(!startsForStatement(nowReading)) {
			return syntaxErrorNode("forStatement");
		}
		Token forStatementToken = nowReading;
		readToken();
		expect(Punctuator.OPEN_PARENTHESIS);
		ParseNode forCtlPhrase = parseForControlPhrase();
		expect(Punctuator.CLOSE_PARENTHESIS);
		ParseNode block = parseBlock();
		return ForStatementNode.withChildren(forStatementToken, forCtlPhrase, block);
	}
	private boolean startsForStatement(Token token) {
		return token.isLextant(Keyword.FOR);
	}
	
	// forControlPhrase ->	index id of expr
	//						element id of exp
	//						ever
	//						count (expression lessOp)? identifier lessOp expression
	
	private ParseNode parseForControlPhrase() {
		if(!startsForControlPhrase(nowReading)) {
			return syntaxErrorNode("For Control Phrase");
		}
		ForControlPhraseNode result = new ForControlPhraseNode(nowReading);
		if(nowReading.isLextant(Keyword.INDEX, Keyword.ELEMENT)) {	
			readToken();
			ParseNode itr = parseIdentifier();
			expect(Keyword.OF);
			ParseNode array = parseExpression();
			
			result.appendChild(itr);
			result.appendChild(array);
		}
		if(nowReading.isLextant(Keyword.COUNT)){
			readToken();
			ParseNode expr1 = parseExpression2();
			Token lessOp1 = nowReading;
			if(!lessOp1.isLextant(Punctuator.LESS, Punctuator.LESSOFEQUAL)) {
				return syntaxErrorNode("count needs lessOp");
			}
			

			
			readToken();
			ParseNode expr2 = parseExpression2();
			if(nowReading.isLextant(Punctuator.LESS, Punctuator.LESSOFEQUAL)) {
				if(!(expr2 instanceof IdentifierNode)) {
					return syntaxErrorNode("ForControl count expected identifier");
				}
				Token lessOp2 = nowReading;
				
				if(lessOp1.isLextant(Punctuator.LESS))
					result.setLowerBoundIncluded(false);
				if (lessOp1.isLextant(Punctuator.LESSOFEQUAL))
					result.setLowerBoundIncluded(true);
				if (lessOp2.isLextant(Punctuator.LESS))
					result.setUpperBoundIncluded(false);
				if(lessOp2.isLextant(Punctuator.LESSOFEQUAL))
					result.setUpperBoundIncluded(true);
				
				
				readToken();
				ParseNode expr3 = parseExpression2();
				result.appendChild(expr2);	// first child is the itr
				result.appendChild(expr1);
				result.appendChild(expr3);
			} else {
				if(!(expr1 instanceof IdentifierNode)) {
					return syntaxErrorNode("ForControl count expected identifier");
				}
				if(lessOp1.isLextant(Punctuator.LESS)) 
					result.setUpperBoundIncluded(false);
				if(lessOp1.isLextant(Punctuator.LESSOFEQUAL))
					result.setUpperBoundIncluded(true);
				result.appendChild(expr1);	// first child itr
				result.appendChild(expr2);
			}
			
		}
		if(nowReading.isLextant(Keyword.EVER))
			readToken();
		
		return result;

	}
	
	private boolean startsForControlPhrase(Token token) {
		return token.isLextant(Keyword.INDEX, Keyword.ELEMENT, Keyword.EVER, Keyword.COUNT);
	}
	
	// break and continue stmt
	// BreakContinueStmt -> [break | continue] ;
	
	private ParseNode parseBreakContinue() {
		if(!startsBreakContinue(nowReading)) {
			return syntaxErrorNode("break-continue");
		}
		Token breakContinueToken = nowReading;
		readToken();
		expect(Punctuator.TERMINATOR);
		return new BreakContinueStatementNode(breakContinueToken);
	}
	
	private boolean startsBreakContinue(Token token) {
		return token.isLextant(Keyword.CONTINUE, Keyword.BREAK);
	}
	
	///////////////////////////////////////////////////////////
	// expressions
	// expr  -> expr0or
	// expr0or -> expr1 [|| expr0and]*
	// expr0and -> expr1 [&& expr1]*
	// expr1 -> expr2 [> expr2]*
	// expr2 -> expr3 [+ expr3]*  (left-assoc)
	// expr3 -> expr3.5 [MULT expr3.5]*  (left-assoc)
	// expr3.5 -> exprUnaryOp [:type]* 
	// exprUnaryOp -> [!|copy]* exprArrayIndexing 
	// exprArrayIndexing -> expr4 [ [expr] ]*
	// expr4 -> literal
	//			parenthesis
	//			[exprList]
	//			fresh[type](expr)
	//			length
	// literal -> intNumber | floatingNumber | identifier | booleanConstant | characterConstant | stringConstant

	// expr  -> expr0
	private ParseNode parseExpression() {		
		if(!startsExpression(nowReading)) {
			return syntaxErrorNode("expression");
		}
		return parseExpression0or();
	}
	private boolean startsExpression(Token token) {
		return startsExpression0or(token);
	}

	// expr0or -> expr1 [||expr0and]*
	private ParseNode parseExpression0or() {
		if(!startsExpression0or(nowReading)) {
			return syntaxErrorNode("expression<0or>");
		}
		ParseNode left = parseExpression0and();
		while(nowReading.isLextant(Punctuator.BOOLEANOR)) {
			Token booleanOperatorToken = nowReading;
			readToken();
			ParseNode right = parseExpression0and();
			
			left = BinaryOperatorNode.withChildren(booleanOperatorToken, left, right);
		}
		return left;
	}
	
	private boolean startsExpression0or(Token nowReading) {
		return startsExpression0and(nowReading);
	}
	
	// expr0and -> expr1 [&& expr1]*
	private ParseNode parseExpression0and() {
		if(!startsExpression0and(nowReading)) {
			return syntaxErrorNode("Expression<0and>");
		}
		ParseNode left = parseExpression1();
		while(nowReading.isLextant(Punctuator.BOOLEANAND)) {
			Token booleanOperatorToken = nowReading;
			readToken();
			ParseNode right = parseExpression1();
			
			left = BinaryOperatorNode.withChildren(booleanOperatorToken, left, right);
		}
		return left;
		
	}
	private boolean startsExpression0and(Token nowReading) {
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
	
	// expr3.5 -> exprUnaryOp:type
	
	private ParseNode parseExpression7over2() {
		if(!startsExpression7over2(nowReading)) {
			return syntaxErrorNode("expression<3.5>");
		}
		
		ParseNode left = parseExpressionUnaryOp();
		while(nowReading.isLextant(Punctuator.CAST)) {
			Token castingToken = nowReading;
			ParseNode right = null;
			readToken();
			if(startsType(nowReading)) {
				right = parseType();
			} else {
				return syntaxErrorNode("expression casting: expected type");
			}
			
			left = BinaryOperatorNode.withChildren(castingToken, left, right);
		}
		
		return left;
		
	}
	private boolean startsExpression7over2(Token token) {
		return startsExpressionUnaryOp(token);
	}
	// Unary operator
	// exprUnaryOp -> [!|copy]* expr4 right-assoc
	//				  null type
	private ParseNode parseExpressionUnaryOp() {
		if(!startsExpressionUnaryOp(nowReading)) {
			return syntaxErrorNode("expression UnaryOp");
		}
		if(nowReading.isLextant(Keyword.NULL)) {
			Token nullToken = nowReading;
			readToken();
			ParseNode typeNode = parseType();
			return NullReferenceNode.withChildren(nullToken, typeNode);
		}
		
		ParseNode UnaryRoot = null;
		ParseNode currentNode = null;
		if(!isUnaryOperator(nowReading)) {
			return parseExpressionArrayIndexing();
		} else {
			UnaryRoot = new UnaryOperatorNode(nowReading);
			currentNode = UnaryRoot;
			readToken();
		}

		while(isUnaryOperator(nowReading)) {
			Token unaryOperatorToken = nowReading;
			currentNode = UnaryOperatorNode.addAndReturnChildren(unaryOperatorToken, currentNode);	
			readToken();
		}
		currentNode.appendChild(parseExpressionArrayIndexing());
		return UnaryRoot;

		
	}
	private boolean startsExpressionUnaryOp(Token token) {
		return isUnaryOperator(token) || startsExpressionArrayIndexing(token) || token.isLextant(Keyword.NULL);
	}
	private boolean isUnaryOperator(Token token) {
		return token.isLextant(Punctuator.BOOLEANCOMPLIMENT, Keyword.COPY);
	}
	
	// exprArrayIndexing -> expr4[ [expr] ]*
	private ParseNode parseExpressionArrayIndexing() {
		if(!startsExpressionArrayIndexing(nowReading)) {
			return syntaxErrorNode("expression-ArrayIndexing");
		}
		
		ParseNode left = parseExpression4();
		while(true){
			if(nowReading.isLextant(Punctuator.OPEN_SQUARE_BRACKET)) {
				Token ArrayIndexingToken = nowReading;
				readToken();
				ParseNode right = parseExpression();
				expect(Punctuator.CLOSE_SQUARE_BRACKET);
				
				left = ArrayIndexingNode.withChildren(ArrayIndexingToken, left, right);
			} else
				break;
			
		}
		
		return left;
	}
	private boolean startsExpressionArrayIndexing(Token token) {
		return startsExpression4(token);
	}
	
	// expr4 -> literal 
	//			parenthesis 
	//			length 
	//			populated_Array 
	//			fresh[type](expr)
	private ParseNode parseExpression4() {
		if(!startsExpression4(nowReading)) {
			return syntaxErrorNode("expression<4>");
		}
		if(startsLiteral(nowReading))
			return parseLiteral();
		else if(startsParenthesis(nowReading))
			return parseParenthesis();
		else if(startsPopulatedArray(nowReading)) 
			return parsePopulatedArray();
		else if(startsFreshArray(nowReading))
			return parseFreshArray();
		else if(startsLengthOperation(nowReading))
			return parseLength();
		
		return syntaxErrorNode("expression<4>");
	}
	private boolean startsExpression4(Token token) {
		return startsLiteral(token) || startsParenthesis(token) 
				|| startsLengthOperation(token) || startsPopulatedArray(token) 
				|| startsFreshArray(token);
	}

	// Parenthesis
	private ParseNode parseParenthesis() {
		if(!startsParenthesis(nowReading)) {
			return syntaxErrorNode("Parenthesis");
		}
		readToken();
		ParseNode right = parseExpression();
		expect(Punctuator.CLOSE_PARENTHESIS);
		return right;
	}
	private boolean startsParenthesis(Token token) {
		return token.isLextant(Punctuator.OPEN_PARENTHESIS);
	}
	// populated array creation
	private ParseNode parsePopulatedArray() {
		if(!startsPopulatedArray(nowReading)) {
			return syntaxErrorNode("populated array creation");
		}
		PopulatedArrayNode result = new PopulatedArrayNode(nowReading);
		
		readToken();
		result = (PopulatedArrayNode)parseExpressionList(result);
		
		expect(Punctuator.CLOSE_SQUARE_BRACKET);
		return result;
		
		
	}
	private ParseNode parseExpressionList(ParseNode node) {
		
		if(!startsExpression(nowReading)) {
			return node;
		}
		
		ParseNode newExpr = parseExpression();
		node.appendChild(newExpr);
		while(nowReading.isLextant(Punctuator.SEPARATOR)) {
			readToken();
			newExpr = parseExpression();
			node.appendChild(newExpr);
		}
		
		return node;
	}
	
	private boolean startsPopulatedArray(Token token) {
		return token.isLextant(Punctuator.OPEN_SQUARE_BRACKET);
	}
	
	// fresh array
	private ParseNode parseFreshArray() {
		if(!startsFreshArray(nowReading)) {
			return syntaxErrorNode("empty array creation(fresh)");
		}
		Token freshToken = nowReading;
		readToken();
		
		Token typeToken = nowReading;
		ParseNode type = parseType();
		ParseNode expr = null;
		expect(Punctuator.OPEN_PARENTHESIS);
		if(typeToken instanceof IdentifierToken)
			expr = parseExpressionList(new ExpressionListNode(previouslyRead));
		else
			expr = parseExpression();
		expect(Punctuator.CLOSE_PARENTHESIS);
		return FreshArrayNode.withChildren(freshToken, type, expr);
	}
	private boolean startsFreshArray(Token token) {
		return token.isLextant(Keyword.FRESH);
	}
	
	// type
	private ParseNode parseType() {
		if(startsParenthesis(nowReading)) {
			readToken();
			ParseNode type = parseType();
			expect(Punctuator.CLOSE_PARENTHESIS);
			return type;
		}
		
		if(!startsType(nowReading)) {
			return syntaxErrorNode("unvalid type");
		}
		
		if(nowReading.isLextant(Punctuator.OPEN_SQUARE_BRACKET)) {
			Token arrayToken = nowReading;
			readToken();
			ParseNode child = parseType();
			expect(Punctuator.CLOSE_SQUARE_BRACKET);
			return TypeNode.withChildren(arrayToken, child);
		} else {
			Token typeToken = nowReading;
			readToken();
			return new TypeNode(typeToken);
		}
	}
	private boolean startsType(Token token) {
		return token.isLextant(Punctuator.OPEN_SQUARE_BRACKET, Keyword.INT, Keyword.FLOAT, Keyword.CHAR, Keyword.STRING, Keyword.BOOL)
				|| startsIdentifier(token);
	}
	
	
	
	// length
	private ParseNode parseLength() {
		if(!startsLengthOperation(nowReading)) {
			return syntaxErrorNode("Length opreation");
		}
		Token lengthOpreator = nowReading;
		readToken();
		ParseNode left = parseExpression();
		expect(Punctuator.BAR);
		return LengthOperatorNode.withChildren(lengthOpreator, left);
	}
	private boolean startsLengthOperation(Token token) {
		return token.isLextant(Punctuator.BAR);
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


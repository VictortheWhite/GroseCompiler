package asmCodeGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.runtime.MemoryManager;
import asmCodeGenerator.runtime.RecordManager;
import asmCodeGenerator.runtime.RunTime;
import asmCodeGenerator.Header;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import parseTree.*;
import parseTree.nodeTypes.ArrayConcatenationNode;
import parseTree.nodeTypes.ArrayIndexingNode;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BlockNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.BreakContinueStatementNode;
import parseTree.nodeTypes.CharacterConstantNode;
import parseTree.nodeTypes.DiagnosticStatementNode;
import parseTree.nodeTypes.ExpressionListNode;
import parseTree.nodeTypes.ForControlPhraseNode;
import parseTree.nodeTypes.ForStatementNode;
import parseTree.nodeTypes.FreshArrayNode;
import parseTree.nodeTypes.FunctionCallNode;
import parseTree.nodeTypes.FunctionDefinitionNode;
import parseTree.nodeTypes.FunctionInvocationNode;
import parseTree.nodeTypes.FunctionReturnNode;
import parseTree.nodeTypes.LengthOperatorNode;
import parseTree.nodeTypes.MainBlockNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.FloatingConstantNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.IntegerConstantNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.NullReferenceNode;
import parseTree.nodeTypes.ParameterListNode;
import parseTree.nodeTypes.PopulatedArrayNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.SeparatorNode;
import parseTree.nodeTypes.StringConstantNode;
import parseTree.nodeTypes.TupleDefinitionNode;
import parseTree.nodeTypes.TupleEntryNode;
import parseTree.nodeTypes.UnaryOperatorNode;
import parseTree.nodeTypes.WhileStatementNode;
import parseTree.nodeTypes.ReassignmentNode;
import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.TupleType;
import semanticAnalyzer.types.Type;
import symbolTable.Binding;
import symbolTable.FunctionBinding;
import symbolTable.Scope;
import symbolTable.StaticBinding;
import static asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;

// do not call the code generator if any errors have occurred during analysis.
public class ASMCodeGenerator {
	private static Labeller labeller = new Labeller();
	private static Header header = new Header();

	ParseNode root;

	public static ASMCodeFragment generate(ParseNode syntaxTree) {
		ASMCodeGenerator codeGenerator = new ASMCodeGenerator(syntaxTree);
		return codeGenerator.makeASM();
	}
	public ASMCodeGenerator(ParseNode root) {
		super();
		this.root = root;
	}
	public static Labeller getLabeller() {
		return labeller;
	}
	
	public ASMCodeFragment makeASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		
		code.append( MemoryManager.codeForInitialization());
		code.append( RunTime.getEnvironment() );
		code.append( RecordManager.codeForInitialization());
		code.append( globalVariableBlockASM() );
		code.append( programASM() );
		code.append( MemoryManager.codeForAfterApplication() );
		
		return code;
	}
	private ASMCodeFragment globalVariableBlockASM() {
		assert root.hasScope();
		Scope scope = root.getScope();
		int globalBlockSize = scope.getAllocatedSize();
		
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		code.add(DLabel, RunTime.GLOBAL_MEMORY_BLOCK);
		code.add(DataZ, globalBlockSize);
		return code;
	}
	private ASMCodeFragment programASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		
		//code.add(    Label, RunTime.MAIN_PROGRAM_LABEL);
		code.append( programCode());
		code.add(    Halt );
		
		return code;
	}
	private ASMCodeFragment programCode() {
		CodeVisitor visitor = new CodeVisitor();
		root.accept(visitor);
		return visitor.removeRootCode(root);
	}


	private class CodeVisitor extends ParseNodeVisitor.Default {
		private Map<ParseNode, ASMCodeFragment> codeMap;
		ASMCodeFragment code;
		
		public CodeVisitor() {
			codeMap = new HashMap<ParseNode, ASMCodeFragment>();
		}


		////////////////////////////////////////////////////////////////////
        // Make the field "code" refer to a new fragment of different sorts.
		private void newAddressCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_ADDRESS);
			codeMap.put(node, code);
		}
		private void newValueCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_VALUE);
			codeMap.put(node, code);
		}
		private void newVoidCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_VOID);
			codeMap.put(node, code);
		}

	    ////////////////////////////////////////////////////////////////////
        // Get code from the map.
		private ASMCodeFragment getAndRemoveCode(ParseNode node) {
			ASMCodeFragment result = codeMap.get(node);
			codeMap.remove(result);
			return result;
		}
	    public  ASMCodeFragment removeRootCode(ParseNode tree) {
			return getAndRemoveCode(tree);
		}		
		private ASMCodeFragment removeValueCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			makeFragmentValueCode(frag, node);
			return frag;
		}		
		private ASMCodeFragment removeAddressCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			assert frag.isAddress();
			return frag;
		}		
		private ASMCodeFragment removeVoidCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			assert frag.isVoid();
			return frag;
		}
		
	    ////////////////////////////////////////////////////////////////////
        // convert code to value-generating code.
		private void makeFragmentValueCode(ASMCodeFragment code, ParseNode node) {
			assert !code.isVoid();
			
			if(code.isAddress()) {
				turnAddressIntoValue(code, node);
			}	
		}
		private void turnAddressIntoValue(ASMCodeFragment code, ParseNode node) {
			if(node.getType() == PrimitiveType.INTEGER) {
				code.add(LoadI);
			}	
			else if(node.getType() == PrimitiveType.BOOLEAN) {
				code.add(LoadC);
			}	
			else if(node.getType() == PrimitiveType.FLOATING) {
				code.add(LoadF);
			}
			else if(node.getType() == PrimitiveType.CHARACTER) {
				code.add(LoadC);
			}
			else if(node.getType() == PrimitiveType.STRING) {
				code.add(LoadI);
			}
			else if(node.getType() instanceof ArrayType) {
				code.add(LoadI);
			} 
			else if(node.getType() instanceof TupleType) {
				code.add(LoadI);
			}
			else {
				assert false : "node " + node;
			}
			code.markAsValue();
		}
		
		
	    ////////////////////////////////////////////////////////////////////
        // ensures all types of ParseNode in given AST have at least a visitLeave	
		public void visitLeave(ParseNode node) {
			assert false : "node " + node + " not handled in ASMCodeGenerator";
		}
		
		///////////////////////////////////////////////////////////////////////////
		// constructs larger than statements
		public void visitLeave(ProgramNode node) {
			ASMCodeFragment GlobalVariableDeclaraction = new ASMCodeFragment(GENERATES_VOID);
			newVoidCode(node);
			for(ParseNode child : node.getChildren()) {
				if(child instanceof TupleDefinitionNode)
					continue;
				if(child instanceof DeclarationNode) {
					GlobalVariableDeclaraction.append(removeVoidCode(child));
				}
				if(child instanceof FunctionDefinitionNode) {
					code.append(removeVoidCode(child));
				}
				if(child instanceof MainBlockNode) {
					code.add(Label, RunTime.MAIN_PROGRAM_LABEL);
					code.append(GlobalVariableDeclaraction);
					code.append(removeVoidCode(child));
				}
			}
		}
		public void visitLeave(MainBlockNode node) {
			newVoidCode(node);
			for(ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
				
				// deallocate after each statement ends
				code.add(Call, RecordManager.DEALLOCATE_CHECKLIST);
			}

		}
		public void visitLeave(BlockNode node)	{
			newVoidCode(node);
			for(ParseNode child: node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
				
				// deallocate memory when a statements ends( includidng a block)
				code.add(Call, RecordManager.DEALLOCATE_CHECKLIST);
			}
			
			// add codes to decrement refcount of variables in scoope
			// when leaves the scope
			Scope localScope = node.getScope();
			decrementRefcountInScope(code, localScope);
			code.add(Call, RecordManager.DEALLOCATE_CHECKLIST);
			
		}
		
		private void decrementRefcountInScope(ASMCodeFragment code, Scope scope) {
			for(Binding binding: scope.getSymbolTable().values()) {
				if(binding instanceof StaticBinding) {
					continue;
				}
				if(!binding.getType().isReferenceType()) {
					continue;
				}
				ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
				binding.generateAddress(frag);			// [...adr]
				frag.add(LoadI);						// [...record]
				RecordManager.decrementRefcount(frag);
				frag.add(Pop);
				
				code.append(frag);
			}
		}
		
		
		//////////////////////////////////////////////////////////////////////////
		// function definition
		public void visitLeave(FunctionDefinitionNode node) {
			IdentifierNode funcName = (IdentifierNode)node.child(0);
			ParameterListNode exprList = (ParameterListNode)node.child(1);
			ASMCodeFragment userCodeBody = removeVoidCode(node.child(3));
			FunctionBinding funcBinding = (FunctionBinding)funcName.getBinding();
			String StartLabel = funcBinding.getFunctionStartLabel();
			String returnLabel = funcBinding.getRetureLabel();
			Scope procedureScope = node.child(3).getScope();
			
			
			int frameSize = procedureScope.getAllocatedSize() + 8;
			
			newVoidCode(node);
			code.add(Label, StartLabel);
			
			//-------------------------------------------
			// function setup
			
			// [...R]
			// push old FP and returnAdr onto FrameStack			
			// decrement SP
			code.add(PushD, RunTime.STACK_POINTER);
			code.add(LoadI);
			code.add(PushI, -4);
			code.add(Add);					// [...R sp-4]
			// store Old FP
			code.add(Duplicate);
			code.add(PushD, RunTime.FRAME_POINTER);
			code.add(LoadI);
			code.add(StoreI);				// [...R sp-4]
			// decrement SP
			code.add(PushI, -4);
			code.add(Add);					// [...R sp-8]
			// store Return Address
			code.add(Exchange);
			code.add(StoreI);
			
			
			// [...]
			// set FP to SP
			code.add(PushD, RunTime.FRAME_POINTER);	//[...fpAdr]
			code.add(PushD, RunTime.STACK_POINTER);
			code.add(LoadI);						// [...fpAdr sp]
			code.add(StoreI);
			
			// [...]
			// decrement sp by frame size
			code.add(PushD, RunTime.STACK_POINTER);	
			code.add(Duplicate);					// [...spAdr spAdr]
			code.add(LoadI);
			code.add(PushI, -frameSize);
			code.add(Add);							// [...spAdr sp]
			code.add(StoreI);						// [...]
			
			
			//-------------------------------------------
			// userCode
			code.append(userCodeBody);
			
			//-------------------------------------------
			// function teardown
			
			// return label
			code.add(Label, returnLabel);
			
			// load return address onto ASM Stack
			// change fp to old fp
			code.add(PushD, RunTime.FRAME_POINTER);
			code.add(LoadI);						// [...fp]
			code.add(Duplicate);
			code.add(PushI, -8);
			code.add(Add);							// [...fp fp-8]
			code.add(LoadI);						// [...fp R]
			code.add(Exchange);
			code.add(PushI, -4);
			code.add(Add);
			code.add(LoadI);						// [...R oldFP]
			code.add(PushD, RunTime.FRAME_POINTER);
			code.add(Exchange);
			code.add(StoreI);						// [...R]		
			
			// increment sp by frameSize
			code.add(PushD, RunTime.STACK_POINTER);
			code.add(Duplicate);
			code.add(LoadI);
			code.add(PushI, frameSize);
			code.add(Add);
			code.add(StoreI);						// [...R]		
			
			Type returnType = funcName.getType();			
			if(returnType != PrimitiveType.VOID) {
				ASMOpcode loadOpcode = LoadArrayOpcode(returnType);
				ASMOpcode storeOpcode = opcodeForStore(returnType);
				//[...R]
				// increment sp by argumentsSize
				code.add(PushD, RunTime.STACK_POINTER);
				code.add(LoadI);						// [...R sp]
				code.add(loadOpcode);						// [...R returnVar]
				code.add(PushD, RunTime.STACK_POINTER);
				code.add(LoadI);						// [...R returnVar sp]
				code.add(PushI, exprList.getExprListSize());
				code.add(Add);							// [...R returnVar sp*]
				code.add(Duplicate);
				code.add(PushD, RunTime.STACK_POINTER); 
				code.add(Exchange);
				code.add(StoreI);						// [...R returnVar sp]
				code.add(Exchange);
				code.add(storeOpcode);
			
				// [...R]
				// push returnVar onto ASM stack
				// increment sp by returnVar type size
				code.add(PushD, RunTime.STACK_POINTER);
				code.add(LoadI);
				code.add(loadOpcode); // [...R returnVar]
				code.add(PushD, RunTime.STACK_POINTER);
				code.add(Duplicate);
				code.add(LoadI);
				code.add(PushI, funcName.getType().getSize());
				code.add(Add);
				code.add(StoreI);						// [...R returnVar]
			} else {
				Macros.loadIFrom(code, RunTime.STACK_POINTER);
				code.add(PushI, exprList.getExprListSize());
				code.add(Add);
				Macros.storeITo(code, RunTime.STACK_POINTER);
			}
			
			
			// stack pointer points to old frame of prevously procedure now
			// return
			if(returnType != PrimitiveType.VOID) {
				code.add(Exchange);
			}				
			code.add(Return);

		}
		

		///////////////////////////////////////////////////////////////////////////
		// statements and declarations

		public void visitLeave(PrintStatementNode node) {
			newVoidCode(node);

			for(ParseNode child : node.getChildren()) {
				if(child instanceof NewlineNode || child instanceof SeparatorNode) {
					ASMCodeFragment childCode = removeVoidCode(child);
					code.append(childCode);
				}
				else {
					if(child.getType() instanceof ArrayType)
						appendPrintArrayCode(child);
					else if(child.getType() instanceof TupleType)
						appendPrintTupleCode(child);
					else
						appendPrintCode(child);
				}
			}
		}
		
		public void visit(NewlineNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.NEWLINE_PRINT_FORMAT);
			code.add(Printf);
		}
		public void visit(SeparatorNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.SEPARATOR_PRINT_FORMAT);
			code.add(Printf);
		}
		private void appendPrintCode(ParseNode node) {

			code.append(removeValueCode(node));			
			printPrimitiveType(node.getType());
		}
		
		private void printPrimitiveType(Type type) {
			String format = printFormat(type);
			String notPrintingLabel = labeller.newLabel("jump-print-null-pointer", "");

			printNullIfNullPointer(type, notPrintingLabel);
			convertToStringIfBoolean(type);
			addAddressOffestIfString(type);
			code.add(PushD, format);
			code.add(Printf);
			code.add(Label, notPrintingLabel);
		}
		
		// print array
		private void appendPrintArrayCode(ParseNode node) {
			ArrayType nodeType = (ArrayType)node.getType();
			code.append(removeValueCode(node));			// [...adr]
			printArray(nodeType);
		}
		private void printArray(ArrayType type) {
			ASMOpcode loadOpcode = LoadArrayOpcode(type.getSubType());
			int subTypeSize = type.getSubTypeSize();
			String startLoopLabel = labeller.newLabel("printing-array-loop-start", "");
			String endLoopLabel = labeller.newLabelSameNumber("printing-array-loop-end", "");
			String notPrintingLabel = labeller.newLabelSameNumber("jump-print-null-pointer", "");
			printNullIfNullPointer(type, notPrintingLabel);
			
			code.add(PushI, 91);
			code.add(PushD, RunTime.CHARACTER_PRINT_FORMAT);
			code.add(Printf);							// print '['
			
			code.add(Duplicate);				
			code.add(PushI, 13);
			code.add(Add);
			code.add(LoadI);							// [...adr n]
			code.add(Exchange);
			code.add(PushI,17);
			code.add(Add);
			code.add(Exchange);							// [...adr* n]	adr* is now address of element to print
			
			// if length ==0 , just let go
			code.add(Duplicate);
			code.add(JumpFalse, endLoopLabel);
			//else
			code.add(Label, startLoopLabel);
			code.add(Exchange);							// [..n adr*]
			code.add(Duplicate);						
			code.add(loadOpcode);						// [...n adr* val]

			/* if subType is array
			 * 		recursively print array
			 * else
			 * 		print element
			 */
			if(type.getSubType() instanceof ArrayType) {
				printArray((ArrayType)type.getSubType());
			} else if(type.getSubType() instanceof TupleType) {
				printTuple((TupleType)type.getSubType());
			}
			else {
				printPrimitiveType(type.getSubType());
			}	
			code.add(PushI, subTypeSize);
			code.add(Add);								// adr* += subTypeSize
			code.add(Exchange);
			code.add(PushI, -1);
			code.add(Add);								// [...adr* n]		n--
			code.add(Duplicate);
			code.add(JumpFalse, endLoopLabel);
			code.add(PushI, 44);						// print ','
			code.add(PushD, RunTime.CHARACTER_PRINT_FORMAT);
			code.add(Printf);
			code.add(PushI, 32);						// print space
			code.add(PushD, RunTime.CHARACTER_PRINT_FORMAT);
			code.add(Printf);							
			code.add(Jump, startLoopLabel);				
			code.add(Label,endLoopLabel);				// [...adr* n]
			code.add(Pop);
			code.add(Pop);
			
			code.add(PushI, 93);
			code.add(PushD, RunTime.CHARACTER_PRINT_FORMAT);
			code.add(Printf);
			
			code.add(Label, notPrintingLabel);
		}
		
		// print tuple
		private void appendPrintTupleCode(ParseNode node) {
			TupleType tupleType = (TupleType)node.getType();
			code.append(removeValueCode(node));	// [...adr]
			printTuple(tupleType);
			
		}
		private void printTuple(TupleType type) {
			List<Type> subElementTypes = type.getParameterList();
			String notPrintingLabel = labeller.newLabel("jump-print-null-pointer", "");

			printNullIfNullPointer(type, notPrintingLabel);
			
			code.add(PushI, 40);
			code.add(PushD, RunTime.CHARACTER_PRINT_FORMAT);
			code.add(Printf);							// print '('
			
			// [...adr ]
			code.add(PushI, 9);
			code.add(Add);
			
			int i = 0;
			while(i < subElementTypes.size()) {
				Type subType = subElementTypes.get(i);
				ASMOpcode loadOpcode = LoadArrayOpcode(subType);
				
				code.add(Duplicate);	// [...adr* adr*]
				code.add(loadOpcode);	// [...adr* ele]
				if(subType instanceof ArrayType) {
					printArray((ArrayType)subType);
				} else if(subType instanceof TupleType) {
					printTuple((TupleType)subType);
				}
				else {
					printPrimitiveType(subType);
				}
				code.add(PushI, subType.getSize());
				code.add(Add);
				
				i++;
				if(i == subElementTypes.size())
					break;
				else {	
					code.add(PushI, 44);						// print ','
					code.add(PushD, RunTime.CHARACTER_PRINT_FORMAT);
					code.add(Printf);
					code.add(PushI, 32);						// print space
					code.add(PushD, RunTime.CHARACTER_PRINT_FORMAT);
					code.add(Printf);	
				}
			}
			code.add(Pop);
			
			code.add(PushI, 41);
			code.add(PushD, RunTime.CHARACTER_PRINT_FORMAT);
			code.add(Printf);							// print ')'
			
			code.add(Label, notPrintingLabel);
		}
		private void convertToStringIfBoolean(Type type) {
			if(type != PrimitiveType.BOOLEAN) {
				return;
			}
			
			String trueLabel = labeller.newLabel("-print-boolean-true", "");
			String endLabel = labeller.newLabelSameNumber("-print-boolean-join", "");

			code.add(JumpTrue, trueLabel);
			code.add(PushD, RunTime.BOOLEAN_FALSE_STRING);
			code.add(Jump, endLabel);
			code.add(Label, trueLabel);
			code.add(PushD, RunTime.BOOLEAN_TRUE_STRING);
			code.add(Label, endLabel);
		}		
		private void addAddressOffestIfString(Type type) {
			if(type != PrimitiveType.STRING) {
				return;
			}
			
			code.add(PushI, 13);
			code.add(Add);
		}
		
		private void printNullIfNullPointer(Type type, String notPrintingLabel) {
			if((type instanceof PrimitiveType) && (type != PrimitiveType.STRING)) {
				return;
			}
			String printNullLabel = labeller.newLabel("print-null-pointer", "");
			String pointerNotNullLabel = labeller.newLabelSameNumber("pointer-not-null", "");
			code.add(Duplicate);
			code.add(JumpFalse, printNullLabel);
			code.add(Jump, pointerNotNullLabel);
			
			code.add(Label, printNullLabel);
			code.add(Pop);
			code.add(PushD, RunTime.NULL_POINTER_STRING);
			code.add(PushD, RunTime.STRING_PRINT_FORMAT);
			code.add(Printf);
			code.add(Jump, notPrintingLabel);
			
			code.add(Label, pointerNotNullLabel);
		}
		
		private String printFormat(Type type) {		
			assert type instanceof PrimitiveType;
			switch((PrimitiveType)type) {
			case INTEGER:	return RunTime.INTEGER_PRINT_FORMAT;
			case BOOLEAN:	return RunTime.BOOLEAN_PRINT_FORMAT;
			case FLOATING:	return RunTime.FLOATING_PRINT_FORMAT;
			case CHARACTER:	return RunTime.CHARACTER_PRINT_FORMAT;
			case STRING:	return RunTime.STRING_PRINT_FORMAT;
			default:		
				assert false : "Type " + type + " unimplemented in ASMCodeGenerator.printFormat()";
				return "";
			}
		}
		private ASMOpcode LoadArrayOpcode(Type type) {
			if(type instanceof ArrayType) {
				return LoadI;
			}
			if(type instanceof TupleType) {
				return LoadI;
			}
			
			assert type instanceof PrimitiveType;
			switch((PrimitiveType)type) {
			case INTEGER:	return LoadI;
			case BOOLEAN:	return LoadC;
			case FLOATING:	return LoadF;
			case CHARACTER:	return LoadC;
			case STRING:	return LoadI;
			case VOID:		return Pop;	 // pop the load address
			default:		
				assert false : "Type " + type + " unimplemented in ASMCodeGenerator.LoadArrayOpcode()";
				return null;
			}
		}
		//-------------------------------------------------------
		// Declaration and Reassignment
		public void visitLeave(DeclarationNode node) {
			IdentifierNode identifier = (IdentifierNode)node.child(0);
			String endOfDeclarationLabel = labeller.newLabel("declaracion-end", "");
			
			ASMCodeFragment lvalue = removeAddressCode(node.child(0));	
			ASMCodeFragment rvalue = removeValueCode(node.child(1));
			
			newVoidCode(node);
			
			jumpOverIfDeclaredStaticVariable(identifier, endOfDeclarationLabel);
			code.append(lvalue);
			code.append(rvalue);

			Type type = node.getType();
			
			// if reference type, increment refcount
			if(type.isReferenceType()) {
				RecordManager.incrementRefcount(code);
			}
			// store
			code.add(opcodeForStore(type));
			addEndLabelIfStaticDeclaration(identifier, endOfDeclarationLabel);
			

		}
		
		private void jumpOverIfDeclaredStaticVariable(IdentifierNode identifier, String endLabel) {
			if(!identifier.isStatic()) {
				return;
			}
			
			code.add(PushD, identifier.getIsDeclaraedIndicatorLabel());
			code.add(LoadC);
			code.add(JumpTrue, endLabel);
			// set isDeclaredBit to 1
			code.add(PushD, identifier.getIsDeclaraedIndicatorLabel());
			code.add(PushI, 1);
			code.add(StoreC);
		}
		
		private void addEndLabelIfStaticDeclaration(IdentifierNode identifier, String endLabel) {
			if(!identifier.isStatic()) {
				return;
			}		
			code.add(Label, endLabel);
		}
		
		public void visitLeave(ReassignmentNode node) {
			newVoidCode(node);
			ASMCodeFragment lvalue = removeAddressCode(node.child(0));
			ASMCodeFragment rvalue = removeValueCode(node.child(1));
			
			code.append(lvalue);
			// decrement refcount of lvalue if reference type
			if(node.getType().isReferenceType()) {
				code.add(Duplicate);
				code.add(LoadI);
				RecordManager.decrementRefcount(code);
				code.add(Pop);
			}
			
			code.append(rvalue);
			// increment refcount of rvalue if reference type
			if(node.getType().isReferenceType()) {
				RecordManager.incrementRefcount(code);
			}
			
			
			Type type = node.getType();
			code.add(opcodeForStore(type));
			

		}
		private ASMOpcode opcodeForStore(Type type) {
			if(type == PrimitiveType.INTEGER) {
				return StoreI;
			}
			if(type == PrimitiveType.BOOLEAN) {
				return StoreC;
			}
			if(type == PrimitiveType.FLOATING) {
				return StoreF;
			}
			if(type == PrimitiveType.CHARACTER) {
				return StoreC;
			}
			if(type == PrimitiveType.STRING) {
				return StoreI;
			}
			if(type instanceof ArrayType) {
				return StoreI;
			}
			if(type instanceof TupleType) {
				assert !((TupleType)type).isTrivial();
				return StoreI;
			}
			assert false: "Type " + type + " unimplemented in opcodeForStore()";
			return null;
		}
		
		///////////////////////////////////////////////////////////////////////////
		// function call
		public void visitLeave(FunctionCallNode node) {
			newVoidCode(node);
			code.append(removeValueCode(node.child(0)));

			if(node.child(0).getType() != PrimitiveType.VOID) {
				code.add(Pop);
			}
		}
		
		///////////////////////////////////////////////////////////////////////////
		// control flows statements------ if while and for
		public void visitLeave(IfStatementNode node) {
			newVoidCode(node);
			ASMCodeFragment condition = removeValueCode(node.child(0));
			ASMCodeFragment TrueBlock = removeVoidCode(node.child(1));
			
			String startsElseBlockLabel = labeller.newLabel("else-block-start", ""); 
			String endIfElseStatementLabel = labeller.newLabelSameNumber("if-else-stmt-end", "");

			
			code.append(condition);
			code.add(JumpFalse, startsElseBlockLabel);
			code.append(TrueBlock);
			code.add(Jump, endIfElseStatementLabel);
			code.add(Label, startsElseBlockLabel);
			if(node.getChildren().size()==3) {
				ASMCodeFragment ElseBlock = removeVoidCode(node.child(2));
				code.append(ElseBlock);
			}				
			code.add(Label, endIfElseStatementLabel);

		}
		public void visitEnter(WhileStatementNode node) {
			node.setStartLabel(labeller.newLabel("while-loop-start", ""));
			node.setEndLabel(labeller.newLabelSameNumber("while-loop-end", ""));
		}
		public void visitLeave(WhileStatementNode node) {
			newVoidCode(node);
			
			String startsLoopLabel = node.getStartLabel(); 
			String endsLoopLabel = node.getEndLabel();
					
			ASMCodeFragment Block = removeVoidCode(node.child(1));
			
			if(node.child(0) instanceof ForControlPhraseNode) {		// while(ever) { }
				code.add(Label, startsLoopLabel);
				code.append(Block);
				code.add(Jump, startsLoopLabel);
				code.add(Label, endsLoopLabel);
			} else {												// while(expr) { }
				ASMCodeFragment condition = removeValueCode(node.child(0));

				code.add(Label, startsLoopLabel);
				code.append(condition);
				code.add(JumpFalse, endsLoopLabel);
				code.append(Block);
				code.add(Jump, startsLoopLabel);
				code.add(Label, endsLoopLabel);
			}
			
			
		}
		
		public void visitEnter(ForStatementNode node) {
			node.setStartLabel(labeller.newLabel("for-loop-start", ""));
			node.setEndLabel(labeller.newLabelSameNumber("for-loop-end", ""));
			node.setContinuelabel(labeller.newLabelSameNumber("for-loop-continue", ""));
		}
		public void visitLeave(ForStatementNode node) {
			
			String startLoopLabel = node.getStartLabel();
			String endLoopLabel = node.getEndLabel();
			String continueLoopLabel = node.getContinueLabel();
			
			ForControlPhraseNode forCtlNode = (ForControlPhraseNode)node.child(0);
			ASMCodeFragment block = removeVoidCode(node.child(1));
			
			newVoidCode(node);
			if(forCtlNode.getToken().isLextant(Keyword.EVER)) {
				code.add(Label, startLoopLabel);			// loop forever
				code.append(block);
				code.add(Label, continueLoopLabel);
				code.add(Jump, startLoopLabel);
				code.add(Label, endLoopLabel);
			} else if(forCtlNode.getToken().isLextant(Keyword.ELEMENT)) {
				
				String elementAdrPtr = labeller.newLabel("$for-loop-element-pointer", "");
				String itrAdr = labeller.newLabelSameNumber("$for-loop-itr-adr", "");
				
				Type subTypeOfArray = ((ArrayType)forCtlNode.child(1).getType()).getSubType();
				ASMOpcode loadElementOpcode = LoadArrayOpcode(subTypeOfArray);
				ASMOpcode storeElementOpcode = opcodeForStore(subTypeOfArray);
				
				ASMCodeFragment elementAdr = removeAddressCode(forCtlNode.child(0));
				ASMCodeFragment arrayExpr = removeValueCode(forCtlNode.child(1));
				
				
				code.add(DLabel, elementAdrPtr);
				code.add(DataI, 0);
				code.add(DLabel, itrAdr);
				code.add(DataI, 0);
				// store eleAdr to elementAdrPtr
				code.append(elementAdr);			// [...eleAdr]
				code.add(PushD, elementAdrPtr);		
				code.add(Exchange);
				code.add(StoreI);				// [...]	*elePtr = ele;
				// initialize i to 0
				code.add(PushD, itrAdr);
				code.add(PushI, 0);
				code.add(StoreI);			// i = 0
				code.append(arrayExpr);		// [...arrayStart]
				// increment refcount of arrayExpr
				RecordManager.incrementRefcount(code);
				// load array-length n
				code.add(Duplicate);		// [...arrayStart arrayStart]
				code.add(PushI, 13);
				code.add(Add);
				code.add(LoadI);			// [...arrayStart n]
				
				// loop start
				code.add(Label, startLoopLabel);
				code.add(Duplicate);		// [...arrayStart n n]
				code.add(PushD,itrAdr);
				code.add(LoadI);			// [...arrayStart n n i]
				code.add(Subtract);			// [...arrayStart n n-i]
				code.add(JumpFalse,endLoopLabel);	// if i < n, continue 	(n-i>0)		
				// load a[i] to ele before block starts
				code.add(Exchange);
				code.add(Duplicate);		// [...n arrayStart arrayStart]
				code.add(PushD, elementAdrPtr);
				code.add(LoadI);			// [...n arrayStart arrayStart eleAdr]
				code.add(Exchange);			// [...n arrayStart eleAdr arrayStart]
				code.add(PushD, itrAdr);
				code.add(LoadI);
				code.add(PushI, subTypeOfArray.getSize());
				code.add(Multiply);
				code.add(PushI, 17);
				code.add(Add);
				code.add(Add);				// 13 + i*size
				code.add(loadElementOpcode);// [...n arrayStart eleAdr a[0]]
				code.add(storeElementOpcode);//[...n arrayStart]
				code.add(Exchange);			// [...arrayStart n]
				// block start
				code.append(block);
				code.add(Label, continueLoopLabel);	// continue Label
				// i++
				code.add(PushD,itrAdr);
				code.add(Duplicate);		// [...arrayStart n itrAdr itrAdr]
				code.add(LoadI);			// [...arrayStart n itrAdr i]
				code.add(PushI, 1);
				code.add(Add);				
				code.add(StoreI);			// [...arrayStart n]	i++
				code.add(Jump, startLoopLabel);
				code.add(Label, endLoopLabel);	// [...arrayStart n]
				code.add(Pop);				// [...arrayStart]
				RecordManager.decrementRefcount(code);
				code.add(Pop);
				
			} else if(forCtlNode.getToken().isLextant(Keyword.PAIR)) {
				String itrAdrPtr = labeller.newLabel("$for-loop-itr-pointer", "");
				String eleAdrPtr = labeller.newLabelSameNumber("$for-loop-ele-pointer", "");
				
				Type subTypeOfArray = ((ArrayType)forCtlNode.child(2).getType()).getSubType();
				ASMOpcode loadElementOpcode = LoadArrayOpcode(subTypeOfArray);
				ASMOpcode storeElementOpcode = opcodeForStore(subTypeOfArray);
				
				ASMCodeFragment itrAdr = removeAddressCode(forCtlNode.child(0));
				ASMCodeFragment eleAdr = removeAddressCode(forCtlNode.child(1));
				ASMCodeFragment arrayExpr = removeValueCode(forCtlNode.child(2));
				
				// itrAdrPtrs
				Macros.declareI(code, itrAdrPtr);
				Macros.declareI(code, eleAdrPtr);
				
				// store itrAdrs
				code.append(itrAdr);
				Macros.storeITo(code, itrAdrPtr);
				code.append(eleAdr);
				Macros.storeITo(code, eleAdrPtr);
				
				// initialize itr
				Macros.loadIFrom(code, itrAdrPtr);	//[...itrAdr]
				code.add(PushI, 0);
				code.add(StoreI);
				
				// put arrayExpr on stack
				code.append(arrayExpr);
				// increment refcount of arrayExpr
				RecordManager.incrementRefcount(code);
				// load array length
				code.add(Duplicate);
				Macros.readIOffset(code, 13);		// [...arr n]
				
				// loop start
				code.add(Label, startLoopLabel);
				// if itr == n, jump
				code.add(Duplicate);				// [...arr n n]
				Macros.loadIFrom(code, itrAdrPtr);	
				code.add(LoadI);					// [...arr n n itr]
				code.add(Subtract);
				code.add(JumpFalse, endLoopLabel);	// [...arr n]
				// load element
				code.add(Exchange);					// [...n arr];
				code.add(Duplicate);
				code.add(PushI, subTypeOfArray.getSize());
		        Macros.loadIFrom(code, itrAdrPtr);
		        code.add(LoadI);					// [...n arr arr size itr]
		        code.add(Multiply);
		        code.add(PushI, 17);
		        code.add(Add);						// [...n arr arr offset]
		        code.add(Add);
		        code.add(loadElementOpcode);		// [...n arr ele]
		        Macros.loadIFrom(code, eleAdrPtr);	// [...n arra ele eleAdr]
		        code.add(Exchange);
		        code.add(storeElementOpcode);
		        // user block
		        code.append(block);
		        code.add(Label, continueLoopLabel);
		        
		        // itr++
		        Macros.loadIFrom(code, itrAdrPtr);
		        code.add(Duplicate);
		        code.add(LoadI);
		        code.add(PushI, 1);
		        code.add(Add);
		        code.add(StoreI);
		        // exchange stack
		        code.add(Exchange);					// [...n arr] => [...arr n]
				// jump start
		        code.add(Jump, startLoopLabel);
				code.add(Label, endLoopLabel);		// [...arr n]
				code.add(Pop);						// [...n arr]
				// decrement refcount of arrayExpr
				RecordManager.decrementRefcount(code);
				code.add(Pop);
				
			} else if(forCtlNode.getToken().isLextant(Keyword.INDEX)) {
				String itrAdrPtr = labeller.newLabel("$for-loop-itr-pointer", "");
				ASMCodeFragment itrAdr = removeAddressCode(forCtlNode.child(0));
				ASMCodeFragment arrayExpr = removeValueCode(forCtlNode.child(1));
		
				code.add(DLabel, itrAdrPtr);
				code.add(DataI, 0);
			
				code.append(itrAdr);			// [...itrAdr]
				code.add(Duplicate);
				code.add(PushD, itrAdrPtr);
				code.add(Exchange);
				code.add(StoreI);				// [...itrAdr]	*p = i;
				code.add(PushI, 0);
				code.add(StoreI);			// i = 0
				code.append(arrayExpr);		
				code.add(PushI, 13);
				code.add(Add);
				code.add(LoadI);			// [...n]
			
				code.add(Label, startLoopLabel);
				code.add(Duplicate);		// [...n n]
				code.add(PushD,itrAdrPtr);
				code.add(LoadI);
				code.add(LoadI);			// [...n n i]
				code.add(Subtract);			// [...n n-i]
				code.add(JumpFalse,endLoopLabel);	// if i < n, continue 	(n-i>0)	
				code.append(block);
				code.add(Label, continueLoopLabel);	// continuelabel
				code.add(PushD,itrAdrPtr);
				code.add(LoadI);			// [...n itrAdr]
				code.add(Duplicate);
				code.add(LoadI);			// [...n itrAdr i]
				code.add(PushI, 1);
				code.add(Add);				
				code.add(StoreI);			// [...n]	i++
				code.add(Jump, startLoopLabel);
				code.add(Label, endLoopLabel);
				code.add(Pop);
				
			} else if(forCtlNode.getToken().isLextant(Keyword.COUNT)) {								
				ASMCodeFragment itrAdr = removeAddressCode(forCtlNode.child(0));
				ASMCodeFragment lowerBound = null;
				ASMCodeFragment upperBound = null;
				
				String upperBoundLocation = labeller.newLabel("for-loop-count-upperbound-location", "");
				
				ASMOpcode JumpOpcode = null;
				if(forCtlNode.isUpperBoundIncluded())
					JumpOpcode = JumpNeg;
				else
					JumpOpcode = JumpFalse;
				
				if(forCtlNode.nChildren() == 2)
					upperBound = removeValueCode(forCtlNode.child(1));
				else if(forCtlNode.nChildren() == 3) {
					upperBound = removeValueCode(forCtlNode.child(2));
					lowerBound = removeValueCode(forCtlNode.child(1));
				}
				
				Macros.declareI(code, upperBoundLocation);
				code.add(PushD, upperBoundLocation);
				code.append(upperBound);
				code.add(StoreI);
				
				code.append(itrAdr);
				code.add(Duplicate);
				if(forCtlNode.nChildren() == 2) {
					code.add(PushI, 0);
				} else if(forCtlNode.nChildren() == 3) {
					code.append(lowerBound);
					if(!forCtlNode.isLowerBoundIncluded()) {
						code.add(PushI, 1);
						code.add(Add);
					} 
				}
				code.add(StoreI);			// [...itrAdr]
				
				code.add(Label, startLoopLabel);
				code.add(Duplicate);		// [...itrAdr]
				code.add(LoadI);			// [...itrAdr itr]
				// load upper bound
				Macros.loadIFrom(code, upperBoundLocation);
				code.add(Exchange);			// [...itrAdr upperBound itr]
				code.add(Subtract);
				code.add(JumpOpcode, endLoopLabel);	// [...itrAdr]
				code.append(block);
				code.add(Label, continueLoopLabel);
				code.add(Duplicate);
				code.add(Duplicate);		// [...itrAdr itrAdr itrAdr]
				code.add(LoadI);
				code.add(PushI, 1);
				code.add(Add);
				code.add(StoreI);			// [...itrAdr]
				code.add(Jump, startLoopLabel);
				code.add(Label, endLoopLabel);	// [..itrAdr]
				code.add(Pop);
			}
			
		}

		public void visitLeave(DiagnosticStatementNode node) {
			ParseNode IntExpr = node.child(0);
			
			newVoidCode(node);
			if(node.nChildren() == 1) { 
				code.append(removeValueCode(IntExpr));
			}
			if(node.nChildren() == 2) {
				ExpressionListNode exprList = (ExpressionListNode)node.child(1);
				int size = exprList.getExprListSize() + 4;
				String argsLocation = labeller.newLabel("$diagnostic-statement-args-location", "");
				int []offsets = new int[exprList.nChildren()+1];				
				
				
				code.add(Label, argsLocation);
				code.add(DataZ, size);
				
				// store intExpr
				code.add(PushD, argsLocation);
				code.append(removeValueCode(IntExpr));
				code.add(StoreI);
				offsets[0] = 0;
				
				int i  = 1;
				int offset = 4;
				for(ParseNode expr: exprList.getChildren()) {
					ASMOpcode storeOpcode = opcodeForStore(expr.getType());
					code.append(removeValueCode(expr));		// [...expr]
					code.add(PushD, argsLocation);
					code.add(PushI, offset);
					code.add(Add);							// [...expr adr]
					code.add(Exchange);
					code.add(storeOpcode);
									
					offsets[i] = offset;
					offset += expr.getType().getSize();
					i++;
				}
				
				// place it on asm stack in reverse order
				for(int j = exprList.nChildren(); j > 0; j--) {
					ASMOpcode loadOpcode = LoadArrayOpcode(exprList.child(j-1).getType());
					code.add(PushD, argsLocation);
					code.add(PushI, offsets[j]);
					code.add(Add);
					code.add(loadOpcode);
				}
				code.add(PushD, argsLocation);
				code.add(LoadI);
				
			}
			
			code.add(Call, MemoryManager.MEM_MANAGER_DIAGNOSTICS);
			
			
		}
		
		
		
		///////////////////////////////////////////////////////////////////////////
		// expressions --------Binary operator
		public void visitLeave(BinaryOperatorNode node) {
			Lextant operator = node.getOperator();

			if(isComparisonOperator(operator)) {
				visitComparisonOperatorNode(node, operator);
			} else if(isBooleanOperator(operator)) {
				visitBooleanOperatorNode(node,operator);
			} else if(isStringConcatenation(node,operator)) {
				visitStringConcatenationNode(node,operator);
			}
			else if(operator==Punctuator.CAST) {
				visitCastingOperatorNode(node);
			}
			else {
				visitNormalBinaryOperatorNode(node);
			}
		}
				
		private boolean isComparisonOperator(Lextant operator) {
			return operator==Punctuator.GREATER || operator==Punctuator.LESS || operator==Punctuator.GREATEROFEQUAL 
					|| operator==Punctuator.LESSOFEQUAL || operator==Punctuator.EQUAL || operator==Punctuator.NOTEQUAL;
		}
		
		// comparison operator
		private void visitComparisonOperatorNode(BinaryOperatorNode node, Lextant operator) {
			
			
			
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			Type childType = node.child(0).getType();

			String startLabel = labeller.newLabel("-compare-arg1-", "");
			String arg2Label  = labeller.newLabelSameNumber("-compare-arg2-", "");
			String subLabel   = labeller.newLabelSameNumber("-compare-sub-", "");
			String trueLabel  = labeller.newLabelSameNumber("-compare-true-", "");
			String falseLabel = labeller.newLabelSameNumber("-compare-false-", "");
			String joinLabel  = labeller.newLabelSameNumber("-compare-join-", "");
			
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(Label, arg2Label);
			code.append(arg2);
			code.add(Label, subLabel);
						
			addSubstractionAndJumpInstruction(childType, operator, trueLabel, falseLabel);
			
			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);

		}
		//comparasion aids: returns the jump instruction needed
		private void addSubstractionAndJumpInstruction(Type childType, Lextant operator, String trueLabel, String falseLabel) {
		
			if(childType == PrimitiveType.INTEGER || childType == PrimitiveType.CHARACTER) {
				code.add(Subtract);

				if(operator == Punctuator.LESS) {
					code.add(JumpNeg, trueLabel);
					code.add(Jump, falseLabel);
				} else if(operator == Punctuator.GREATER) {
					code.add(JumpPos, trueLabel);
					code.add(Jump, falseLabel);
				} else if(operator == Punctuator.EQUAL) {
					code.add(JumpFalse, trueLabel);
					code.add(Jump, falseLabel);
				} else if(operator == Punctuator.NOTEQUAL) {
					code.add(JumpTrue, trueLabel);
					code.add(Jump, falseLabel);
				} else if(operator == Punctuator.LESSOFEQUAL) {
					code.add(JumpPos, falseLabel);
					code.add(Jump, trueLabel);
				} else if(operator == Punctuator.GREATEROFEQUAL) {
					code.add(JumpNeg, falseLabel);
					code.add(Jump, trueLabel);
				}
			} else if(childType == PrimitiveType.FLOATING) {
				code.add(FSubtract);
				
				if(operator == Punctuator.LESS) {
					code.add(JumpFNeg, trueLabel);
					code.add(Jump, falseLabel);
				} else if(operator == Punctuator.GREATER) {
					code.add(JumpFPos, trueLabel);
					code.add(Jump, falseLabel);
				} else if(operator == Punctuator.EQUAL) {
					code.add(JumpFZero, trueLabel);
					code.add(Jump, falseLabel);
				} else if(operator == Punctuator.NOTEQUAL) {
					code.add(JumpFZero, falseLabel);
					code.add(Jump, trueLabel);
				} else if(operator == Punctuator.LESSOFEQUAL) {
					code.add(JumpFPos, falseLabel);
					code.add(Jump, trueLabel);
				} else if(operator == Punctuator.GREATEROFEQUAL) {
					code.add(JumpFNeg, falseLabel);
					code.add(Jump, trueLabel);
				}
			} else if(childType == PrimitiveType.BOOLEAN || childType == PrimitiveType.STRING || childType instanceof ArrayType || childType instanceof TupleType) {
				code.add(Subtract);
				if(operator == Punctuator.EQUAL) {
					code.add(JumpFalse, trueLabel);
					code.add(Jump, falseLabel);
				} else if(operator == Punctuator.NOTEQUAL) {
					code.add(JumpTrue, trueLabel);
					code.add(Jump, falseLabel);
				}
			} 
		}
	
		// boolean operator
		private boolean isBooleanOperator(Lextant operator) {
			return operator==Punctuator.BOOLEANAND || operator==Punctuator.BOOLEANOR;
		}
		private void visitBooleanOperatorNode(BinaryOperatorNode node, Lextant operator) {
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			String falseLabel = labeller.newLabel("boolean-binary-operator-false", "");
			String trueLabel = labeller.newLabelSameNumber("boolean-binary-operator-true", "");
			String joinLabel = labeller.newLabelSameNumber("boolean-binary-operator-join", "");
			newValueCode(node);
			
			//examine arg1
			code.append(arg1);
			if(operator == Punctuator.BOOLEANAND) {
				code.add(JumpFalse, falseLabel);
			} else if(operator == Punctuator.BOOLEANOR) {
				code.add(JumpTrue, trueLabel);
			}			
			
			//examine arg2
			code.append(arg2);
			if(operator == Punctuator.BOOLEANAND) {
				code.add(JumpFalse, falseLabel);
			} else if(operator == Punctuator.BOOLEANOR) {
				code.add(JumpTrue, trueLabel);
				code.add(Jump, falseLabel);
			}
			
			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);

			

		}
			
		// strin concatenation
		private boolean isStringConcatenation(BinaryOperatorNode node, Lextant operator) {
			return operator == Punctuator.ADD && node.getType() == PrimitiveType.STRING;
		}
		private void visitStringConcatenationNode(BinaryOperatorNode node, Lextant operator) {
			assert isStringConcatenation(node, operator);
			newValueCode(node);
			
			ASMCodeFragment arg1 = removeValueCode(node.child(0));	
			ASMCodeFragment arg2 = removeValueCode(node.child(1));	
			
			code.append(arg1);									// [...arg1]
			code.append(arg2);									// [...arg1 arg2]

			code.add(PushD, RunTime.STRING_CONCA_ARG2);			// [...arg1 arg2 arg2_addr]
			code.add(Exchange); 								// [...arg1 arg2_addr arg2]
			code.add(StoreI); 									// [...arg1]
			code.add(PushD, RunTime.STRING_CONCA_ARG1);			// [...arg1 arg1_addr]
			code.add(Exchange); 								// [...arg1_addr arg1]
			code.add(StoreI); 									// [...]
			code.add(Call, RunTime.STRING_CONCATENATION);		// [...R] 	R being return address(current PC +1)
		}

		
		// nomal binary operator
		private void visitNormalBinaryOperatorNode(BinaryOperatorNode node) {
			newValueCode(node);
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			code.append(arg1);
			code.append(arg2);
			
			ASMOpcode opcode = opcodeForOperator(node.getOperator(),node.getType());
			AddRuntimeErrorForZeroDivision(opcode);
			code.add(opcode);							// type-dependent!
		}
		private ASMOpcode opcodeForOperator(Lextant lextant, Type nodeType) {
			assert(lextant instanceof Punctuator);
			assert(nodeType instanceof Type);
			Punctuator punctuator = (Punctuator)lextant;
			if(nodeType == PrimitiveType.INTEGER) {
				switch(punctuator) {
				case ADD: 	   		return Add;				// type-dependent!
				case MULTIPLY: 		return Multiply;		// type-dependent!
				case DIVIDE:		return Divide;			// type-dependent!
				case SUB:			return Subtract;
			
				default:
					assert false : "unimplemented operator in opcodeForOperator";
				}
			} else if(nodeType == PrimitiveType.FLOATING) {
				switch(punctuator) {
				case ADD:			return FAdd;
				case MULTIPLY: 		return FMultiply;
				case DIVIDE:		return FDivide;
				case SUB:			return FSubtract;
				
				default:
					assert false : "unimplemented operator in opcodeForOperator for floating type";
				}
			} else {
				assert false: "unimplemented type of node";
			}
			return null;
		}
		private void AddRuntimeErrorForZeroDivision(ASMOpcode opcode) {
			if(opcode == ASMOpcode.Divide) {
				code.add(Duplicate);
				code.add(JumpFalse,RunTime.INTEGER_DIVIDE_BY_ZERO_RUNTIME_ERROR );
			} else if(opcode == ASMOpcode.FDivide) {
				code.add(Duplicate);
				code.add(JumpFZero,RunTime.FLOATING_DIVIDE_BY_ZERO_RUNTIME_ERROR);
			} else
				return;
		}		
		
		// casting
		private void visitCastingOperatorNode(BinaryOperatorNode node) {
			newValueCode(node);
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			code.append(arg1);
			
			Type childType = node.child(0).getType();
			Type targetType = node.child(1).getType();
			if(childType == PrimitiveType.INTEGER) {
				if(targetType == PrimitiveType.CHARACTER) {
					code.add(PushI, 127);
					code.add(BTAnd);
				} else if(targetType == PrimitiveType.FLOATING) {
					code.add(ConvertF);
				} else if(targetType == PrimitiveType.BOOLEAN) {
					String trueLabel  = labeller.newLabel("-cast-boolean-true-", "");
					String falseLabel = labeller.newLabelSameNumber("-cast-boolean-false-", "");
					String joinLabel  = labeller.newLabelSameNumber("-cast-boolean-join-", "");
					
					code.add(JumpTrue, trueLabel);
					code.add(Jump, falseLabel);
					code.add(Label, trueLabel);
					code.add(PushI, 1);
					code.add(Jump, joinLabel);
					code.add(Label, falseLabel);
					code.add(PushI, 0);
					code.add(Jump, joinLabel);
					code.add(Label, joinLabel);
				} else
					return;
			} else if(childType == PrimitiveType.FLOATING) {
				if(targetType == PrimitiveType.INTEGER) {
					code.add(ConvertI);
				} else
					return;
			} else if(childType == PrimitiveType.CHARACTER) {
				if(targetType == PrimitiveType.INTEGER) {
					return;		//do nothing
				} else if(targetType == PrimitiveType.BOOLEAN) {
					String trueLabel  = labeller.newLabel("-cast-boolean-true-", "");
					String falseLabel = labeller.newLabelSameNumber("-cast-boolean-false-", "");
					String joinLabel  = labeller.newLabelSameNumber("-cast-boolean-join-", "");
					
					code.add(JumpTrue, trueLabel);
					code.add(Jump, falseLabel);
					code.add(Label, trueLabel);
					code.add(PushI, 1);
					code.add(Jump, joinLabel);
					code.add(Label, falseLabel);
					code.add(PushI, 0);
					code.add(Jump, joinLabel);
					code.add(Label, joinLabel);
				}
			}
		}
		
		// array indexing
		public void visitLeave(ArrayIndexingNode node) {
			newAddressCode(node);
			Type type = node.getType();	//get subtype of the array
			ASMCodeFragment array = removeValueCode(node.child(0));
			ASMCodeFragment index = removeValueCode(node.child(1));
			
			code.append(array);						// [...adr]
			code.add(Duplicate);
			code.add(PushI, 13);
			code.add(Add);
			code.add(LoadI);						// [...adr len]
			code.add(Duplicate);					// [...adr len len]
			code.append(index);						// [...adr len len i]
			code.add(Duplicate);
			code.add(JumpNeg, RunTime.ARRAY_INDEXING_OUT_BOUND_ERROR);	// [...adr len len i]
			code.add(Subtract);						// [...adr len len-i]
			code.add(Duplicate);
			code.add(PushI, -1);
			code.add(Add);							// [...adr len len-i len-i-1]
			code.add(JumpNeg, RunTime.ARRAY_INDEXING_OUT_BOUND_ERROR);	// i >= len-1  => len-i-1 >= 0		[...adr len len-i]
			code.add(Subtract);						// [...adr i]	len-(len-i) = i
			code.add(PushI, type.getSize());
			code.add(Multiply);						// [...adr i*subTypeSize]
			code.add(PushI, 17);
			code.add(Add);							
			code.add(Add);							// [...adr+17+i*subTypeSize]
			
		}
		

		// tuple entry
		public void visitLeave(TupleEntryNode node) {
			ASMCodeFragment tupleVarAdr = removeValueCode(node.child(0));
			ASMCodeFragment subElement = removeAddressCode(node.child(1));
			
			newAddressCode(node);
			code.append(tupleVarAdr);
			code.append(subElement);
			code.add(Add);
		}
		
		// expressions --------Binary operator
		public void visitLeave(UnaryOperatorNode node) {
			Lextant operator = node.getOperator();
			if(operator == Punctuator.BOOLEANCOMPLIMENT) {
				visitBooleanComplimentNode(node, operator);
			}
			if(operator == Punctuator.AT) {
				visitAddressOfNode(node, operator);
			}
			if(operator == Punctuator.SHARP) {
				visitReferenceCountNode(node, operator);
			}
			if(operator == Punctuator.DOLLERSIGN) {
				visitRecordNumberNode(node, operator);
			}
			if(operator == Keyword.COPY) {
				if(node.getType() instanceof TupleType) 
					visitTupleCopyNode(node, operator);
				if(node.getType() instanceof ArrayType)
					visitArrayCopyNode(node, operator);
			}
		}
		private void visitBooleanComplimentNode(UnaryOperatorNode node, Lextant operator) {
			assert operator == Punctuator.BOOLEANCOMPLIMENT;
			
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			String falseLabel = labeller.newLabel("boolean-binary-operator-false", "");
			String trueLabel = labeller.newLabelSameNumber("boolean-binary-operator-true", "");
			String joinLabel = labeller.newLabelSameNumber("boolean-binary-operator-join", "");
			
			newValueCode(node);
			
			code.append(arg1);
			code.add(JumpTrue, falseLabel);
			
			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);
		}
		
		private void visitArrayCopyNode(UnaryOperatorNode node, Lextant operator) {
			assert operator == Keyword.COPY;
			
			ASMCodeFragment arg = removeValueCode(node.child(0));
			
			newValueCode(node);
			
			code.append(arg);
			code.add(PushD, RunTime.ARRAY_COPY_ARG);
			code.add(Exchange);
			code.add(StoreI);
			
			// this copy module does a byte to byte copy
			// doesn't do anything with refcount
			// but does header stuff
			code.add(Call, RunTime.ARRAY_COPY);
			
			// increase refcount of subElements if reference type
			if(node.getType().isReferenceType()) {
				incrementRefcountOfArraySubElement();
			}
			
		}
		private void incrementRefcountOfArraySubElement() {
			String startLoopLabel = labeller.newLabel("$incremnt-array-subelement-refcount-loop-start", "");
			String endLoopLabel = labeller.newLabelSameNumber("$increment-array-subelement-refcount-loop-end", "");
			
			// [...adr]
			code.add(Duplicate);
			code.add(Duplicate);			// [...adr adr adr]
			Macros.readIOffset(code, 13);	// [...adr adr n]
			
			code.add(Exchange);
			code.add(PushI, 17);
			code.add(Add);
			code.add(Exchange);				// [...adr adr* n]
			
			// loop start
			code.add(Label, startLoopLabel);
			// if n == 0, jump end
			code.add(Duplicate);
			code.add(JumpFalse, endLoopLabel);
			// increment refcout
			code.add(Exchange);				// [...adr n adr*]
			code.add(Duplicate);
			code.add(LoadI);				// [...adr n adr* record]
			RecordManager.incrementRefcount(code);
			code.add(Pop);					// [...adr n adr*]
			// increment loop invariant
			code.add(PushI, 4);
			code.add(Add);
			code.add(Exchange);
			code.add(PushI, -1);
			code.add(Add);					// [...adr adr* n] n--, adr* +=4
			// jump start
			code.add(Jump, startLoopLabel);
			
			// loop end
			code.add(Label, endLoopLabel);	// [...adr adr* n]
			code.add(Pop);
			code.add(Pop);
			
		}
		
		private void visitTupleCopyNode(UnaryOperatorNode node, Lextant operator) {
			assert node.getType() instanceof TupleType;
			assert operator == Keyword.COPY;
			ASMCodeFragment arg = removeValueCode(node.child(0));
			TupleType type = (TupleType)node.getType();
			
			newValueCode(node);
			
			code.add(PushD, RunTime.TUPLE_COPY_ARG);
			code.append(arg);
			code.add(StoreI);

			code.add(PushI, type.getBytesNeeded());
			// [...n]
			code.add(Call, RunTime.TUPLE_COPY);
			
			// [...record]
			// increment refcount of attributes
			if(node.getType().isReferenceType()) {
				incrementRefcountOfTupleAttribute((TupleType)node.getType());
			}
		}
		private void incrementRefcountOfTupleAttribute(TupleType type) {
			for(Binding binding: type.getSymbolTable().values()) {
				if(binding.getType().isReferenceType()) {
					int offset = binding.getMemoryLocation().getOffset();
					
					code.add(Duplicate);				// [...adr adr]
					Macros.readIOffset(code, offset);	// [...adr record]
					RecordManager.incrementRefcount(code);
				}
			}
		}
		
		private void visitAddressOfNode(UnaryOperatorNode node, Lextant operator) {
			assert operator == Punctuator.AT;
			ASMCodeFragment targetAddress = removeAddressCode(node.child(0));
			newValueCode(node);
			code.append(targetAddress);
		}
		
		private void visitReferenceCountNode(UnaryOperatorNode node, Lextant operator) {
			assert operator == Punctuator.SHARP;
			ASMCodeFragment expr = removeValueCode(node.child(0));
			
			newValueCode(node);
			code.append(expr);
			Macros.readCOffset(code, 8);
		}

		private void visitRecordNumberNode(UnaryOperatorNode node, Lextant operator) {
			assert operator == Punctuator.DOLLERSIGN;
			ASMCodeFragment expr = removeValueCode(node.child(0));
			
			newValueCode(node);
			code.append(expr);
				
				//Macros.printStack(code, "ptr: ");
			
			code.add(Call, MemoryManager.MEM_MANAGER_GET_ID);
		}
		
		// expression ------------------- length
		public void visitLeave(LengthOperatorNode node) {
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			
			
			int offset;
			if(node.child(0).getType() instanceof ArrayType) {
				offset = 13;
			} else {
				offset = 9;
			}
			newValueCode(node);
			
			if(node.child(0).getType() instanceof TupleType) {
				TupleType childType = (TupleType)node.child(0).getType();
				code.add(PushI, childType.getLength());
				return;
			}

			
			code.append(arg1);
			code.add(PushI, offset);
			code.add(Add);
			code.add(LoadI);
		}
		
		// expression -----------Populated Array Creation
		public void visitLeave(PopulatedArrayNode node) {
			int subTypeSize = ((ArrayType)node.getType()).getSubTypeSize();
			int length = node.nChildren();
			ASMOpcode storeOpcode = null;
			switch(subTypeSize) {
				case 1 : storeOpcode = ASMOpcode.StoreC; break;
				case 4 : storeOpcode = ASMOpcode.StoreI; break;
				case 8 : storeOpcode = ASMOpcode.StoreF; break;
			}
			assert storeOpcode != null;
			
			newValueCode(node);
			
			code.add(PushI, subTypeSize * length + 17);			// bytes of array
			code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE); // allocate memory	[...adr]
			
			header.addHeader(code, node);				// this method adds header to array, when returns, the adr still on the top of stack [...adr]
			
			// add record to TO_BE_CHECKED_LIST
			//RecordManager.addToCheckList(code);
			
			addElementsToArray(code, storeOpcode, subTypeSize, node);
			
		}
		
		private void addElementsToArray(ASMCodeFragment code, ASMOpcode storeOpcode, int subTypeSize, ParseNode node) {
			code.add(Duplicate);	// [adr adr]
			code.add(PushI, 17);	
			code.add(Add);			// [adr adr*]	adr* = adr + 17
			
			// add element start
			ASMCodeFragment childCode = null;
			for(ParseNode child : node.getChildren()) {
				childCode = removeValueCode(child);
				code.add(Duplicate);
				code.append(childCode);
				// if reference type, incremnt refcount
				if(child.getType().isReferenceType()) {
					RecordManager.incrementRefcount(code);
				}
				code.add(storeOpcode);
				code.add(PushI, subTypeSize);
				code.add(Add);
			}
			
			code.add(Pop);
			
			
		}
		
		// expression -----------Array Concatenation
		public void visitLeave(ArrayConcatenationNode node) {
			ASMCodeFragment firstArray = removeValueCode(node.child(0));
			
			newValueCode(node);
			code.append(firstArray);
			for(int i = 1; i < node.nChildren(); i++) {
				// store arg1
				// [...arg1]
				Macros.storeITo(code, RunTime.ARRAY_CONCA_ARG1);
				// store arg2
				code.append(removeValueCode(node.child(i)));
				Macros.storeITo(code, RunTime.ARRAY_CONCA_ARG2);
								
				// call array-concatenation-subroutine
				code.add(Call, RunTime.ARRAY_CONCATENATION);
			}
		}
		
		// expression -----------Empty Array Creation
		public void visitLeave(FreshArrayNode node) {
			if(node.getType() instanceof ArrayType) {
				addFreshArray(node);
			}
			if(node.getType() instanceof TupleType) {
				addFreshTuple(node);
			}
													// [...adr]
		}
		
		private void addFreshArray(FreshArrayNode node) {
			// logic is comparatively complicated, so putting everything including header here
			ArrayType nodeType = (ArrayType)node.getType();
			int subTypeSize = nodeType.getSubTypeSize();
			String startLoopLabel = labeller.newLabel("empty-array-initialization-loop-start", "");
			String endLoopLabel = labeller.newLabelSameNumber("empty-array-initialization-loop-end", "");
			String lengthVariableLabel = labeller.newLabelSameNumber("temporary-variable-for-length-in-fresh", "");
								
			ASMCodeFragment lengthCode = removeValueCode(node.child(1));
						
			newValueCode(node);
						
			code.add(DLabel, lengthVariableLabel);
			code.add(DataI, 0);

			code.append(lengthCode);
			code.add(Duplicate);									// [...len len]
			code.add(PushD, lengthVariableLabel);
			code.add(Exchange);
			code.add(StoreI);	
						
			// add RuntimeError
			code.add(Duplicate);
			code.add(JumpNeg, RunTime.ARRAY_EMPTY_CREATION_SIZE_NEGATIVE_ERROR);
						
						
			code.add(PushI, subTypeSize);
			code.add(Multiply);
			code.add(PushI, 17);
			code.add(Add);											// [...size]
			code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);		// [...adr]
			// add header
			header.addHeader(code, node, lengthVariableLabel, nodeType);// [...adr]
			
			// add to TO_BE_CHECKED_LIST
			RecordManager.addToCheckList(code);
			
			code.add(Duplicate);
			code.add(PushI, 17);
			code.add(Add);											// [...adr adr+17]
			code.add(PushD, lengthVariableLabel);
			code.add(LoadI);										// [...adr adr* n]
			code.add(PushI, subTypeSize);
			code.add(Multiply);										// [...adr adr* n]
			code.add(Label, startLoopLabel);
			code.add(Duplicate);
						
			code.add(JumpFalse, endLoopLabel);						// [...adr adr* n]
			code.add(Exchange);										// [...adr n adr*]
			code.add(Duplicate);
			code.add(PushI, 0);
			code.add(StoreC);										
			code.add(PushI, 1);
			code.add(Add);											// adr*++
			code.add(Exchange);
			code.add(PushI, -1);
			code.add(Add);											// n--
			code.add(Jump, startLoopLabel);
						
			code.add(Label, endLoopLabel);
			code.add(Pop);
			code.add(Pop);	
		}
		
		private void addFreshTuple(FreshArrayNode node) {
			TupleType nodeType= (TupleType)node.getType();
			ParseNode expressionList = node.child(1);
			
			newValueCode(node);
			
			
			code.add(PushI, nodeType.getBytesNeeded());
			code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
			header.addTupleHeader(code, nodeType);
			
			// add to TO_BE_CHECKED_LIST
			RecordManager.addToCheckList(code);
			
			// [...adr]		
			code.add(Duplicate);	
			code.add(PushI, 9);
			code.add(Add);		// [...adr adr*]

			for(ParseNode child : expressionList.getChildren()) {
				Type type = child.getType();
				ASMOpcode storeOpcode = opcodeForStore(type);
				ASMCodeFragment childValue = removeValueCode(child);
				
				code.add(Duplicate);	// [...adr adr* adr*]
				code.append(childValue);// [...adr adr* adr* val]
				// if reference type, increment refcount of childValue
				if(type.isReferenceType()) {
					RecordManager.incrementRefcount(code);
				}
				code.add(storeOpcode);	// [...adr adr*]
				code.add(PushI, type.getSize());
				code.add(Add);			// adr* += typeSize
										// [...adr adr*]
			}
			code.add(Pop);				// [...adr]
		}
		
		
		// expression -- null reference
		@Override
		public void visitLeave(NullReferenceNode node) {
			newValueCode(node);
			code.add(PushI, 0);
		}
		
		// expression -- function invocation
		@Override
		public void visitLeave(FunctionInvocationNode node) {
			Type returnType = node.getType();
			IdentifierNode funcName = (IdentifierNode) node.child(0);
			ParseNode exprList = node.child(1);
			FunctionBinding funcBinding = (FunctionBinding) funcName.getBinding();
			String JumpFunctionLabel = funcBinding.getFunctionStartLabel();
			
			newValueCode(node);
			

			// push arguments onto frame stack
			// decrement stack pointer accordingly
			for(ParseNode expr : exprList.getChildren()) {
				Type exprType = expr.getType();
				ASMOpcode storeOpcode = opcodeForStore(exprType);
				code.add(PushD, RunTime.STACK_POINTER);
				code.add(LoadI);							// [...sp]
				code.add(PushI, -exprType.getSize());
				code.add(Add);								// [...sp*]
				code.add(Duplicate);
				code.add(PushD, RunTime.STACK_POINTER);
				code.add(Exchange);
				code.add(StoreI);							// [...sp ] sp -= typeSize
				code.append(removeValueCode(expr));
				code.add(storeOpcode);
			}
			
			
				
			// push return variable onto frame stack
			// decrement stack pointer accordingly
				
			// decrement stack pointer
			code.add(PushD, RunTime.STACK_POINTER);
			code.add(LoadI);							// [...sp]
			code.add(PushI, -returnType.getSize());
			code.add(Add);								// [...sp*]
			code.add(PushD, RunTime.STACK_POINTER);
			code.add(Exchange);
			code.add(StoreI);							// [... ] sp -= typeSize
			
			if(returnType != PrimitiveType.VOID) {
				ASMOpcode storeOpcode = opcodeForStore(returnType);
				Macros.loadIFrom(code, RunTime.STACK_POINTER);
				// push returnVariable onto ASM stack
				if(returnType instanceof TupleType) {
					TupleType returnTupleType = (TupleType)returnType;
					code.add(PushI, returnTupleType.getBytesNeeded());
					code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
					header.addTupleHeader(code, returnTupleType);
				} else {
					if(returnType == PrimitiveType.FLOATING) 
						code.add(PushF, 0.0);
					else
						code.add(PushI, 0);
				}
				// store returnVarible to FRAME stack
				code.add(storeOpcode);
			}
			
			
			
			// args and returnVar on FrameStack
			// Stack Pointer adjusted
			// Call function
			code.add(Call, JumpFunctionLabel);
			
			if(returnType.isReferenceType()) {
				if(returnType instanceof TupleType) 
					RecordManager.addToCheckList(code);
				else	// string and array
					RecordManager.decrementRefcount(code);
			}

		}
			
		///////////////////////////////////////////////////////////////////////////
		// leaf nodes (ErrorNode not necessary)
		public void visit(BooleanConstantNode node) {
			newValueCode(node);
			code.add(PushI, node.getValue() ? 1 : 0);
		}
		public void visit(IdentifierNode node) {
			newAddressCode(node);
			Binding binding = node.getBinding();
			
			binding.generateAddress(code);
		}		
		public void visit(IntegerConstantNode node) {
			newValueCode(node);
			
			code.add(PushI, node.getValue());
		}
		public void visit(FloatingConstantNode node) {
			newValueCode(node);
			
			code.add(PushF, node.getValue());
		}
		public void visit(CharacterConstantNode node) {
			newValueCode(node);
			
			code.add(PushI, node.getValue());
		}
		public void visit(StringConstantNode node) {
			newValueCode(node);
			
			String stringLabel = labeller.newLabel("$string-constant", "");
			
			code.add(DLabel, stringLabel);
			header.addHeader(code, node);
			code.add(DataS, node.getValue());
			code.add(PushD, stringLabel);
		}
		public void visit(BreakContinueStatementNode node) {
			newVoidCode(node);			
			
			// decrement refcount of scopes it jumps out
			for(ParseNode current : node.pathToRoot()) {
				if((current instanceof WhileStatementNode) || (current instanceof ForStatementNode)) {
					break;
				}
				if(!current.hasScope()) {
					continue;
				}
				
				Scope localScope = current.getScope();
				decrementRefcountInScope(code, localScope);
			}
			
			code.add(Jump, node.getJumpLabel());
		}
		public void visit(FunctionReturnNode node) {
			newVoidCode(node);
			String returnLabel = node.getReturnLabel();
			code.add(Jump, returnLabel);
		}
	}

}

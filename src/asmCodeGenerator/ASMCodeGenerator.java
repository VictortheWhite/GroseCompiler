package asmCodeGenerator;

import java.util.HashMap;
import java.util.Map;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.runtime.MemoryManager;
import asmCodeGenerator.runtime.RunTime;
import asmCodeGenerator.Header;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import parseTree.*;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BlockNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.CharacterConstantNode;
import parseTree.nodeTypes.MainBlockNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.FloatingConstantNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.IntegerConstantNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.SeparatorNode;
import parseTree.nodeTypes.StringConstantNode;
import parseTree.nodeTypes.UnaryOperatorNode;
import parseTree.nodeTypes.WhileStatementNode;
import parseTree.nodeTypes.ReassignmentNode;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import symbolTable.Binding;
import symbolTable.Scope;
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
		
		code.add(    Label, RunTime.MAIN_PROGRAM_LABEL);
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
			newVoidCode(node);
			for(ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}
		}
		public void visitLeave(MainBlockNode node) {
			newVoidCode(node);
			for(ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}
		}
		public void visitLeave(BlockNode node)	{
			newVoidCode(node);
			for(ParseNode child: node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}
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
			String format = printFormat(node.getType());

			code.append(removeValueCode(node));
			convertToStringIfBoolean(node);
			addAddressOffestIfString(node);
			code.add(PushD, format);
			code.add(Printf);
		}
		private void convertToStringIfBoolean(ParseNode node) {
			if(node.getType() != PrimitiveType.BOOLEAN) {
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
		private void addAddressOffestIfString(ParseNode node) {
			if(node.getType() != PrimitiveType.STRING) {
				return;
			}
			
			code.add(PushI, 13);
			code.add(Add);
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
		// Declaration and Reassignment
		public void visitLeave(DeclarationNode node) {
			newVoidCode(node);
			ASMCodeFragment lvalue = removeAddressCode(node.child(0));	
			ASMCodeFragment rvalue = removeValueCode(node.child(1));
			
			code.append(lvalue);
			code.append(rvalue);
			
			Type type = node.getType();
			code.add(opcodeForStore(type));
		}
		public void visitLeave(ReassignmentNode node) {
			newVoidCode(node);
			ASMCodeFragment lvalue = removeAddressCode(node.child(0));
			ASMCodeFragment rvalue = removeValueCode(node.child(1));
			
			code.append(lvalue);
			code.append(rvalue);
			
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
			assert false: "Type " + type + " unimplemented in opcodeForStore()";
			return null;
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
		
		public void visitLeave(WhileStatementNode node) {
			newVoidCode(node);
			ASMCodeFragment condition = removeValueCode(node.child(0));
			ASMCodeFragment Block = removeVoidCode(node.child(1));
			
			String startsLoopLabel = labeller.newLabel("while-loop-start", ""); 
			String endsLoopLabel = labeller.newLabelSameNumber("while-loop-end", "");
			
			code.add(Label, startsLoopLabel);
			code.append(condition);
			code.add(JumpFalse, endsLoopLabel);
			code.append(Block);
			code.add(Jump, startsLoopLabel);
			code.add(Label, endsLoopLabel);
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
			

			
//			code.add(JumpPos, trueLabel);
			//OldAddJumpInstruction(childType, operator, trueLabel, falseLabel);
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
			} else if(childType == PrimitiveType.BOOLEAN || childType == PrimitiveType.STRING) {
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
		@SuppressWarnings("unused")
		private void OldAddJumpInstruction(Type childType, Lextant operator, String trueLabel, String falseLabel) {
			
			if(childType == PrimitiveType.INTEGER || childType == PrimitiveType.CHARACTER || childType == PrimitiveType.BOOLEAN)
				code.add(Subtract);
			else if(childType == PrimitiveType.FLOATING)
				code.add(FSubtract);
			
			if(operator == Punctuator.GREATEROFEQUAL) {
				code.add(ASMOpcode.Duplicate);
				String compareGreater = labeller.newLabelSameNumber("-compare-greater-for-GREATEREQUAL-Compare-", "");
				if(childType == PrimitiveType.INTEGER) {
					code.add(JumpPos, compareGreater);
					code.add(Pop);
					code.add(Jump, falseLabel);

					code.add(Label,compareGreater);
					code.add(ASMOpcode.JumpFalse, trueLabel);	//jump when arg1 == arg2	
				} else if(childType == PrimitiveType.FLOATING) {
					
						code.add(JumpFPos, compareGreater);
						code.add(Pop);
						code.add(Jump, falseLabel);

						code.add(Label,compareGreater);
						code.add(ASMOpcode.JumpFZero, trueLabel);	//jump when arg1 == arg2	
				}
				
			} else if(operator == Punctuator.LESSOFEQUAL) {
				code.add(ASMOpcode.Duplicate);
				String compareEqual = labeller.newLabelSameNumber("-compare-equal-for-LESSEQUAL-Compare-", "");
				String popDuplicate = labeller.newLabelSameNumber("compare-pop-duplicate-for-LESSEQUAL", "");
				if(childType == PrimitiveType.INTEGER) {
					code.add(JumpNeg, popDuplicate);

					code.add(Label,compareEqual);
					code.add(ASMOpcode.JumpFalse, trueLabel);	//jump when arg1 == arg2	
					code.add(Label, popDuplicate);
					code.add(Pop);
					code.add(Jump,trueLabel);
				} else if(childType == PrimitiveType.FLOATING) {
					
					code.add(JumpFNeg, popDuplicate);

					code.add(Label,compareEqual);
					code.add(ASMOpcode.JumpFZero, trueLabel);	//jump when arg1 == arg2	
					code.add(Label, popDuplicate);
					code.add(Pop);
					code.add(Jump,trueLabel);	
				}
				
				
			} else if(operator == Punctuator.NOTEQUAL && childType == PrimitiveType.FLOATING) {
				code.add(ASMOpcode.JumpFZero, falseLabel);
				code.add(ASMOpcode.Jump, trueLabel);
			} else {
				ASMOpcode Opcode = null;
				if(childType == PrimitiveType.INTEGER) {
					if(operator == Punctuator.GREATER)
						Opcode = ASMOpcode.JumpPos;
					else if(operator == Punctuator.LESS)
						Opcode = ASMOpcode.JumpNeg;
					else if(operator == Punctuator.EQUAL)
						Opcode = ASMOpcode.JumpFalse;
					else if(operator == Punctuator.NOTEQUAL)
						Opcode = ASMOpcode.JumpTrue;
				} else if (childType == PrimitiveType.FLOATING) {
					if(operator == Punctuator.GREATER)
						Opcode = ASMOpcode.JumpFPos;
					else if(operator == Punctuator.LESS)
						Opcode = ASMOpcode.JumpFNeg;
					else if(operator == Punctuator.EQUAL)
						Opcode = ASMOpcode.JumpFZero;
				} 
				code.add(Opcode, trueLabel);
				
			}
			code.add(Jump, falseLabel);

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
		
		// expressions --------Binary operator
		public void visitLeave(UnaryOperatorNode node) {
			Lextant operator = node.getOperator();
			if(operator == Punctuator.BOOLEANCOMPLIMENT) {
				visitBooleanComplimentNode(node, operator);
			}
		}
		public void visitBooleanComplimentNode(UnaryOperatorNode node, Lextant operator) {
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
	}

}

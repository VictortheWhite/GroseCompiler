package asmCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.Label;
import asmCodeGenerator.codeStorage.ASMCodeFragment;


public class Labeller {
	public int labelSequenceNumber;

	public Labeller() {
		this.labelSequenceNumber = 0;
	}

	public void setLabelSequenceNumber(int labelSequenceNumber) {
		this.labelSequenceNumber = labelSequenceNumber;
	}

	public String newLabel(String prefix, String postfix) {
		labelSequenceNumber++;
		return prefix + labelSequenceNumber + postfix;
	}

	public String newLabelSameNumber(String prefix, String postfix) {
		return prefix + labelSequenceNumber + postfix;
	}

	public String addNewLabel(ASMCodeFragment code, String prefix, String postfix) {
		String label = newLabel(prefix, postfix);
		code.add(Label, label);
		return label;
	}
}
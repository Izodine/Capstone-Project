package com.syncedsoftware.iassembly.iasm_base.registers;

public class SubsetRegister extends Register{
	
	public SubsetRegister(String name) {
		super(name);
	}

	public int getl(){
		return (super.rValue() << 24) >>> 24;
	}
	
	public int geth(){
		return (super.rValue() << 16) >>> 16 >> 8;
	}
	
}

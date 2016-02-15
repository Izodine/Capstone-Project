package com.syncedsoftware.iassembly.iasm_base.registers;

import android.os.Handler;

import java.util.HashMap;
import java.util.Map;

public final class RegisterManager {
	
	private Map<String, Register> registers;

	/**
	 * Initializes an instance of RegisterManager with the 8 General Purpose Registers 
	 * (EAX, EBX, ECX, EDX, ESP, EBP, ESI, EDI).
	 */
	public RegisterManager(){
		registers = new HashMap<>(8);
		
		for(String registerName: RegisterNames.list){

			if( ! Register.isExtendedRegister(registerName)) continue;
			
			switch(registerName){
			
			case RegisterNames.eax:
			case RegisterNames.ebx:
			case RegisterNames.ecx:
			case RegisterNames.edx:
				
				registers.put(registerName, new SubsetRegister(registerName));
				
				break;
				
			default:
				registers.put(registerName, new Register(registerName));
			
			}
				
		}

		registers.put(RegisterNames.ax, registers.get(RegisterNames.eax));
		registers.put(RegisterNames.bx, registers.get(RegisterNames.ebx));
		registers.put(RegisterNames.cx, registers.get(RegisterNames.ecx));
		registers.put(RegisterNames.dx, registers.get(RegisterNames.edx));

	}

	public void addListenerToAll(Register.RegisterListener listener, Handler handler){
		for(Map.Entry<String, Register> entry: registers.entrySet()){
			entry.getValue().addListener(listener, handler);
		}
	}
	
	public Register getRegister(String name){
		name = name.toLowerCase();
		if(registers.containsKey(name)){
			return registers.get(name);
		}
		else{
			return null;
		}
	}

	public Register getRegisterFromSubset(String name){
		String identifier = "e"+name.toLowerCase();

		return registers.get(identifier);
	}

	public Map<String, Register> getAllRegisters(){
		return registers;
	}
	
//	public void printRegisterValues(){
//		for(String registerName: RegisterNames.list){
//			Register register = registers.get(registerName);
//			System.out.println(registerName + " :Full Value: " + register.rValue());
//			System.out.println(registerName + " :Low 16: " + register.getx());
//
//			if(register instanceof SubsetRegister){
//				System.out.println(registerName + " low: " + ((SubsetRegister)register).getl());
//				System.out.println(registerName + " high: " + ((SubsetRegister)register).geth());
//			}
//			System.out.println("--------------");
//		}
//	}

}

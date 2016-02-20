package com.syncedsoftware.iassembly.iasm_base.instructions;

import com.syncedsoftware.iassembly.iasm_base.interpreter.Interpreter;
import com.syncedsoftware.iassembly.iasm_base.registers.Register;
import com.syncedsoftware.iassembly.iasm_base.registers.RegisterManager;
import com.syncedsoftware.iassembly.iasm_base.registers.SubsetRegister;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InstructionSet {
	
	private InstructionSet(){}
	
	public final static class Names{
		
		private Names(){}
		
		public static final String and       = "AND";
		public static final String mov       = "MOV";
		public static final String interrupt = "INT";
		public static final String xor       = "XOR";
		public static final String or        = "OR";
		public static final String add       = "ADD";
		public static final String mul       = "MUL";
		public static final String sub       = "SUB";
		public static final String nop       = "NOP";
		public static final String inc       = "INC";
		public static final String dec       = "DEC";
//		public static final String push      = "PUSH";
//		public static final String pop       = "POP";
//		public static final String call      = "CALL";
//		public static final String ret       = "RET"; // Reserved for big Functions update

		public static final String[] array = {
				and, mov, interrupt, xor,
				add, mul, sub, nop, inc,
				dec
		};

	}
	
	public final static class Keywords{
		
		private Keywords(){}
		
		public static final String resb = "RESB";  // 1 byte
		public static final String resw = "RESW";  // 2 bytes
		public static final String resd = "RESD";  // 4 bytes
		public static final String resq = "RESQ";  // 8 bytes
		public static final String rest = "REST";  // 10 bytes
		public static final String resdq = "RESDQ";// 16 bytes
		public static final String resy = "RESY";  // 32 bytes
		public static final String resz = "RESZ";  // 64 bytes

		public static final String db = "DB";      // 1 byte
		public static final String dw = "DW";      // 2 bytes
		public static final String dd = "DD";      // 4 bytes
		public static final String dq = "DQ";      // 8 bytes
		public static final String dt = "DT";      // 10 bytes
		public static final String dow = "DO";     // 16 bytes
		public static final String dy = "DY";      // 32 bytes
		public static final String dz = "DZ";      // 64 bytes

        public static final String equ = "EQU";    // Constant

		public static final String section = "SECTION";
		public static final String segment = "SEGMENT";
		public static final String data = "DATA";
		public static final String bss = "BSS";
		public static final String text = "TEXT";
//		public static final String global    = "GLOBAL"; // Reserved for big functions update

		public static final String[] array ={
				resb, resw, resq, resq,
				rest, resdq, resy, resz,
				db, dw, dd, dq,
				dt, dow, dy, dz,
				section, segment, data, bss,
				text , equ};

	}
    /*
    MOV  register, register - Supported
    MOV  register, immediate - Supported
    MOV  register, memory    - Supported
    MOV  memory, register   - Coming Soon
    MOV  memory, immediate  - Coming Soon
    */

	public static <D,S> void resolveInstruction(D destination, S source, String parts[]){

		switch(parts[0].toUpperCase()){

			case Names.mov:
				mov(destination, source);
				break;

			case Names.and:
				and(destination, source);
				break;

			case Names.xor:
				xor(destination, source);
				break;

			case Names.or:
				or(destination, source);
				break;

			case Names.add:
				add(destination, source);
				break;

			case Names.sub:
				sub(destination, source);
				break;

			case Names.mul:
				mul(destination, source);
				break;

		}

	}

    public static void resolveInstruction(Interpreter interpreter, String[] parts){

        switch(parts[0].toUpperCase()){

            case Names.interrupt:
                interrupt(interpreter, parts);
                break;

			case Names.inc:
				inc(interpreter.InternalRegisterManager.getRegister(parts[1]));
				break;

			case Names.dec:
				dec(interpreter.InternalRegisterManager.getRegister(parts[1]));
				break;

        }

    }

    // Only works for sys_write and sys_exit
    private static void interrupt(Interpreter interpreter, String[] parts) {
        RegisterManager manager = interpreter.InternalRegisterManager;
        int sysCall        = manager.getRegister("EAX").rValue();
        int descriptor     = manager.getRegister("EBX").rValue();
        int ptr            = manager.getRegister("ECX").rValue();
        int len            = manager.getRegister("EDX").rValue();

		switch (sysCall) {

			case 4:

				switch (descriptor) {

					case 1:
						byte[] bytes = interpreter.InternalMemory.getRawBytes(ptr, len);
						if (len > interpreter.InternalMemory.getCompressedMemorySize()) {
							interpreter.reportError("Segmentation Fault.");
						} else {
							interpreter.sendOutput(new String(bytes));
						}
						break;

				}
				break;

			case 1:

				interpreter.sysExitInterrupt();

				break;


		}

    }


    public static <D,S> void mov(D destination, S source) {

        if(destination instanceof SubsetRegister){

            if(source instanceof SubsetRegister){
                ((SubsetRegister) destination).write(((SubsetRegister) source).xValue());
                return;
            }

            if(source instanceof Number){
                ((SubsetRegister) destination).write(((Number) source).shortValue());
                return;
            }

        }

		if(destination instanceof  Register){

            if(source instanceof Register){
                ((Register) destination).write(((Register) source).rValue());
                return;
            }

            if(source instanceof Number){
                ((Register) destination).write(((Number) source).intValue());
                return;
            }

        }
//        // memory immediate
//        // memory register
//        if(destination instanceof Memory){
//
//            if(source instanceof Number){
//
//            }
//
//			if(source instanceof Register){
//
//			}
//
//        }

	}

	public static <D,S> void add(D destination, S source) {

		if(destination instanceof SubsetRegister){

			if(source instanceof SubsetRegister){
				((SubsetRegister) destination).write(((SubsetRegister) destination).xValue() + ((SubsetRegister) source).xValue());
				return;
			}

			if(source instanceof Number){
				((SubsetRegister) destination).write(((SubsetRegister) destination).xValue() + ((Number) source).shortValue());
				return;
			}

		}

		if(destination instanceof  Register){

			if(source instanceof Register){
				((Register) destination).write(((Register) source).rValue());
				return;
			}

			if(source instanceof Number){
				((Register) destination).write(((Number) source).intValue());
				return;
			}

		}

	}

	public static <D,S> void sub(D destination, S source) {

		if(destination instanceof SubsetRegister){

			if(source instanceof SubsetRegister){
				((SubsetRegister) destination).write(((SubsetRegister) destination).xValue() - ((SubsetRegister) source).xValue());
				return;
			}

			if(source instanceof Number){
				((SubsetRegister) destination).write(((SubsetRegister) destination).xValue() - ((Number) source).shortValue());
				return;
			}

		}

		if(destination instanceof  Register){

			if(source instanceof Register){
				((Register) destination).write(((Register) destination).rValue() - ((Register) source).rValue());
				return;
			}

			if(source instanceof Number){
				((Register) destination).write(((Register) destination).rValue() -((Number) source).intValue());
				return;
			}

		}

	}

	public static <D,S> void and(D destination, S source) {

		if(destination instanceof SubsetRegister){

			if(source instanceof SubsetRegister){
				((SubsetRegister) destination).write(((SubsetRegister) destination).xValue() & ((SubsetRegister) source).xValue());
				return;
			}

			if(source instanceof Number){
				((SubsetRegister) destination).write(((SubsetRegister) destination).xValue() & ((Number) source).shortValue());
				return;
			}

		}

		if(destination instanceof  Register){

			if(source instanceof Register){
				((Register) destination).write(((Register) destination).rValue() & ((Register) source).rValue());
				return;
			}

			if(source instanceof Number){
				((Register) destination).write(((Register) destination).rValue() & ((Number) source).intValue());
				return;
			}

		}

	}

	public static <D,S> void xor(D destination, S source) {

		if(destination instanceof SubsetRegister){

			if(source instanceof SubsetRegister){
				((SubsetRegister) destination).write(((SubsetRegister) destination).xValue() ^ ((SubsetRegister) source).xValue());
				return;
			}

			if(source instanceof Number){
				((SubsetRegister) destination).write(((SubsetRegister) destination).xValue() ^ ((Number) source).shortValue());
				return;
			}

		}

		if(destination instanceof  Register){

			if(source instanceof Register){
				((Register) destination).write(((Register) destination).rValue() ^ ((Register) source).rValue());
				return;
			}

			if(source instanceof Number){
				((Register) destination).write(((Register) destination).rValue() ^ ((Number) source).intValue());
				return;
			}

		}

	}

	public static <D,S> void or(D destination, S source) {

		if(destination instanceof SubsetRegister){

			if(source instanceof SubsetRegister){
				((SubsetRegister) destination).write(((SubsetRegister) destination).xValue() | ((SubsetRegister) source).xValue());
				return;
			}

			if(source instanceof Number){
				((SubsetRegister) destination).write(((SubsetRegister) destination).xValue() | ((Number) source).shortValue());
				return;
			}

		}

		if(destination instanceof  Register){

			if(source instanceof Register){
				((Register) destination).write(((Register) destination).rValue() | ((Register) source).rValue());
				return;
			}

			if(source instanceof Number){
				((Register) destination).write(((Register) destination).rValue() | ((Number) source).intValue());
				return;
			}

		}

	}

	public static <S> void inc(S source) {

		if(source instanceof Register){

			if(source instanceof SubsetRegister){
				((SubsetRegister) source).write(((SubsetRegister) source).xValue()+1);
				return;
			}
			((Register) source).write(((Register) source).rValue()+1);
		}

	}

	public static <S> void dec(S source) {

		if(source instanceof Register){

			if(source instanceof SubsetRegister){
				((SubsetRegister) source).write(((SubsetRegister) source).xValue()-1);
				return;
			}
			((Register) source).write(((Register) source).rValue()-1);
		}

	}

	public static <D,S> void mul(D destination, S source) {

		if(destination instanceof SubsetRegister){

			if(source instanceof SubsetRegister){
				((SubsetRegister) destination).write(((SubsetRegister) destination).xValue() * ((SubsetRegister) source).xValue());
				return;
			}

			if(source instanceof Number){
				((SubsetRegister) destination).write(((SubsetRegister) destination).xValue() * ((Number) source).shortValue());
				return;
			}

		}

		if(destination instanceof  Register) {

			if (source instanceof Register) {
				((Register) destination).write(((Register) destination).rValue() * ((Register) source).rValue());
				return;
			}

			if (source instanceof Number) {
				((Register) destination).write(((Register) destination).rValue() * ((Number) source).intValue());
				return;
			}

		}

	}

	public static int operandCount(String name){

		switch(name.toUpperCase()){

			case Names.and:
			case Names.mov:
			case Names.xor:
			case Names.sub:
			case Names.or:
			case Names.mul:
				return 2;

			case Names.interrupt:
			case Names.inc:
			case Names.dec:
			//case Names.call:
				return 1;

			case Names.nop:
			//case Names.ret:
			default:
				return 0;

		}

	}

	public static boolean contains(String name){
		
		for(String instruction: Names.array){
			if(name.equalsIgnoreCase(instruction)) return true;
		}
		
		return false;
	}

	public static String[] allKeywords(){

		List<String> keywordList = new ArrayList<>(20);

        Collections.addAll(keywordList, Keywords.array);

		return keywordList.toArray(new String[keywordList.size()]);

	}

	public static String[] allInstructions(){

		List<String> keywordList = new ArrayList<>(20);

        Collections.addAll(keywordList, Names.array);

		return keywordList.toArray(new String[keywordList.size()]);

	}

	public static boolean containsKeyword(String name){
		
		for(String keyword: Keywords.array){
			if(keyword.equalsIgnoreCase(name)){
				return true;
			}
		}
		
		return false;
	}
	
}

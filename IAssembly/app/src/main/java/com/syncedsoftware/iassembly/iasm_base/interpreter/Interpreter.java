package com.syncedsoftware.iassembly.iasm_base.interpreter;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.syncedsoftware.iassembly.R;
import com.syncedsoftware.iassembly.iasm_base.Simulation;
import com.syncedsoftware.iassembly.iasm_base.instructions.InstructionSet;
import com.syncedsoftware.iassembly.iasm_base.memory.Memory;
import com.syncedsoftware.iassembly.iasm_base.registers.Register;
import com.syncedsoftware.iassembly.iasm_base.registers.RegisterManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Interpreter {

	public static final int CALLBACKS_ONSTARTSIMULATION = 0;
	public static final int CALLBACKS_ONSENDOUTPUT = 1;
	public static final int CALLBACKS_ONSENDERROROUTPUT = 2;
	public static final int CALLBACKS_ONENDSIMULATION = 3;
	/**
	 * Internal memory. Cannot be reassigned to a new memory object.
	 */
	public final Memory InternalMemory = new Memory();

    /**
     * Internal RegisterManager. Cannot be reassigned a new RegisterManager object.
     */
    public final RegisterManager InternalRegisterManager = new RegisterManager();

	private List<Simulation.SimulationListener> _callbacks;

    /**
     * Monitor for wait/notify operations
     */
    private static final Object lock = new Object();

    /**
     * A list to hold forward references
     */
    private final List<String> _awaitingSymbolTable = new ArrayList<>();

	private String _mode = Mode.IDLE;
    private boolean sysExitInterrupt = false;
	private int _lineNumber = 0;
	private List<String> _statements = new ArrayList<>();
	private Handler _handler;
    private Context context;

	private void processInstruction(String[] parts){

        if(parts[0].trim().toUpperCase().equals(InstructionSet.Names.nop)){
            return;
        }

        switch(resolveOperandOrder(parts)){

            case OperandOrder.REGISTER_REGISTER:
                InstructionSet.resolveInstruction(InternalRegisterManager.getRegister(parts[1]),
                        InternalRegisterManager.getRegister(parts[2]), parts);
                break;

            case OperandOrder.REGISTER_IMMEDIATE:
                InstructionSet.resolveInstruction(InternalRegisterManager.getRegister(parts[1]),
                        radixParse(parts[2]), parts);
                break;

            case OperandOrder.REGISTER_MEMORY:
                // Value
                Memory.Symbol symbol = InternalMemory.getSymbol(parts[2]);
                if(parts[2].contains("[") || symbol.isEqu){
                    if(symbol.isEqu && parts[2].contains("[")){
                        postToUiThread(CALLBACKS_ONSENDERROROUTPUT, context.getString(R.string.ERR_6));
                        return;
                    }
                    InstructionSet.resolveInstruction(InternalRegisterManager.getRegister(parts[1]),
                            InternalMemory.getSymbolValueAsNumber(parts[2]), parts);
                }
                else{
                    InstructionSet.resolveInstruction(InternalRegisterManager.getRegister(parts[1]),
                            InternalMemory.getSymbol(parts[2]).rootAddress, parts);
                }
                break;

            case OperandOrder.MEMORY_REGISTER:

                break;

            case OperandOrder.MEMORY_IMMEDIATE:

                break;

            case OperandOrder.ONE_OPERAND:
                    InstructionSet.resolveInstruction(this, parts);
                break;

        }

	}

    private Integer radixParse(String operand) {
        operand = operand.toUpperCase();
        if(operand.contains("H")) return Integer.parseInt(operand.replace("H",""), 16);
        if(operand.contains("0X")) return Integer.parseInt(operand.replace("X",""), 16);
        return Integer.parseInt(operand);
    }

    private int resolveOperandOrder(String[] parts){

        if(parts.length < 3) return OperandOrder.ONE_OPERAND;
				/*
        MOV  register, register 0
        MOV  register, immediate 1
        MOV  register, memory 3
        MOV  memory, register
		MOV  memory, immediate
         */
        String op1 = parts[1].replaceAll("[\\[|\\]]", "");
        String op2 = parts[2].replaceAll("[\\[|\\]]", "");

		boolean op1IsRegister = Register.isRegister(op1),
				op2IsRegister = Register.isRegister(op2),
				op1isMemoryLabel = InternalMemory.symbolIsDefined(op1),
                op2isMemoryLabel = InternalMemory.symbolIsDefined(op2);

        if(op1IsRegister){

            // Reg, reg
            if(op2IsRegister) return OperandOrder.REGISTER_REGISTER;
            // reg, immed
            if(isNumeric(op2)) return OperandOrder.REGISTER_IMMEDIATE;
            // red, mem
            if(op2isMemoryLabel) return OperandOrder.REGISTER_MEMORY;

        }

        if(op1isMemoryLabel){

            if(op2IsRegister) return OperandOrder.MEMORY_REGISTER;
            if(isNumeric(op2)) return OperandOrder.MEMORY_IMMEDIATE;

        }

		return OperandOrder.UNDEFINED;

	}

	private boolean isNumeric(String operand) {
        operand = operand.toUpperCase();
        if(operand.contains("0X")){
            return operand.matches("0[xX][0-9a-fA-F]+");
        }
        if(operand.contains("H")){
            return operand.matches("\\d+[h|H]+");
        }
		return operand.matches("\\d+");

	}

	public void interpret(){

        InternalMemory.compress();
		postToUiThread(CALLBACKS_ONSTARTSIMULATION, null);

        synchronized (lock){
            if (_statements.isEmpty()){
				postToUiThread(CALLBACKS_ONSENDERROROUTPUT, context.getString(R.string.ERR_5));
				return;
            }

            for (String line : _statements) {
                if(sysExitInterrupt) break;
                if (executeLine(line)) return;
            }
            String returnRegister;
            if(sysExitInterrupt){
                returnRegister = "EBX";
            }
            else{
                returnRegister = "EAX";
            }
            postToUiThread(CALLBACKS_ONSENDOUTPUT, context.getString(R.string.program_return_statement) + InternalRegisterManager.getRegister(returnRegister).rValue());
			postToUiThread(CALLBACKS_ONENDSIMULATION, null);
        }
        sysExitInterrupt = false;
        _statements.clear();
    }

    public void sysExitInterrupt(){
        sysExitInterrupt = true;
    }

	public void postToUiThread(final int callbackId, final String msg){
		_handler.post(new Runnable() {
            @Override
            public void run() {

                switch (callbackId) {

                    case CALLBACKS_ONSTARTSIMULATION:
                        for(Simulation.SimulationListener listener: _callbacks)
                            listener.onStartSimulation();
                        break;

                    case CALLBACKS_ONSENDOUTPUT:
                        for(Simulation.SimulationListener listener: _callbacks)
                             listener.onSendOutput(msg);
                        break;

                    case CALLBACKS_ONSENDERROROUTPUT:
                        for(Simulation.SimulationListener listener: _callbacks)
                            listener.onSendErrorOutput(msg);
                        break;

                    case CALLBACKS_ONENDSIMULATION:
                        for(Simulation.SimulationListener listener: _callbacks)
                            listener.onEndSimulation();
                        break;
                }

            }
        });
	}

	public void interpretStep(){

        InternalMemory.compress();
        postToUiThread(CALLBACKS_ONSTARTSIMULATION, null);

        synchronized (lock){
            if (_statements.isEmpty()){
                postToUiThread(CALLBACKS_ONSENDERROROUTPUT, context.getString(R.string.ERR_4));
                return;
            }

            for (String line : _statements) {
                postToUiThread(CALLBACKS_ONSENDOUTPUT, context.getString(R.string.executing_label) + line);
                if(sysExitInterrupt) break;
                if (executeLine(line)) break;
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            String returnRegister;
            if(sysExitInterrupt){
                returnRegister = "EBX";
            }
            else{
                returnRegister = "EAX";
            }
            postToUiThread(CALLBACKS_ONSENDOUTPUT, context.getString(R.string.ERR_3) + InternalRegisterManager.getRegister(returnRegister).rValue());
            postToUiThread(CALLBACKS_ONENDSIMULATION, null);
        }
        sysExitInterrupt = false;
        _statements.clear();
	}

    public void step(){
        synchronized (lock){
            lock.notify();
        }
    }

    private boolean executeLine(String line) {
        String[] parts = line.replaceAll(",", " ").split("\\s+");

        if( !InstructionSet.contains(parts[0])){
            postToUiThread(CALLBACKS_ONSENDERROROUTPUT, parts[0] + context.getString(R.string.ERR_1));
            return true;
        }

        // Reserved switch for future addition
        switch (parts.length) {

        case 1:
            //Log.v("Izodine","Mnemonic = " + parts[0]);
            break;

        case 2:
            //System.out.print("Mnemonic = " + parts[0]);
            //Log.v("Izodine"," Source reg = " + parts[1]);
            break;

        case 3:

            break;

        default:

            postToUiThread(CALLBACKS_ONSENDERROROUTPUT, context.getString(R.string.ERR_0) + _lineNumber);
            postToUiThread(CALLBACKS_ONSENDERROROUTPUT, context.getString(R.string.program_exiting_label));

            return true;
        }

        processInstruction(parts);
        return false;
    }

	/**
	 * Loads the given lines into the Interpreter.
	 * @param lines
	 * @return Returns false if there was malformed code or was unable to load.
	 */
	public boolean load(ArrayList<String> lines, List<Simulation.SimulationListener> listeners, Handler handler, Context context) {
        this.context = context;
		_callbacks = listeners;
		_handler = handler;

        InternalMemory.clear();
		_lineNumber = 0;
		for(String line: lines) {
			++_lineNumber;

			// Determine which mode to use
			if (line.contains("segment") || line.contains("section")) {
				if (line.contains(".data")) {
					//Log.v("Izodine", "Switching to .data mode.");
					_mode = Mode.DATA;
					continue;
				}

				if (line.contains(".bss")) {
					//Log.v("Izodine", "Switching to .bss mode.");
					_mode = Mode.BSS;
					continue;
				}

				if (line.contains(".text")) {
					//Log.v("Izodine", "Switching to .text mode.");
					_mode = Mode.TEXT;
					continue;
				}
			}

			// Get rid of assembly comments and extra space;
			String strippedLine = line.replaceAll("[;].*", "").trim();
            if(strippedLine.isEmpty()) continue;

			String[] parts = parse(strippedLine);


			if(lineHasErrors(strippedLine, parts)){
				postToUiThread(CALLBACKS_ONSENDERROROUTPUT, "Line " + _lineNumber + " has errors.");
				return false;
			}

			switch (_mode) {

			case Mode.TEXT:

				switch (parts.length) {

                    // Using 1 and 2 for Functions update.
				case 1:
//                    if(parts[0].trim().equalsIgnoreCase(InstructionSet.Names.ret)){
//                        functionBuilder.commitFunction();
//                    }
//                    else{
//                        functionBuilder.addFunction(parts[0]);
//                    }
//
					break;

				case 2:
//                    if(currentFunction != null){
//                        postToUiThread(CALLBACKS_ONSENDERROROUTPUT, "Cannot set global twice! at " + _lineNumber);
//                        return false;
//                    }
//                    if(parts[0].trim().toUpperCase().equalsIgnoreCase(InstructionSet.Keywords.global)) {
//                        Log.v("Izodine", "SETTING GLOBAL TO " + parts[1]);
//                        currentFunction = parts[1];
//                    }
					break;

				case 3:
                case 4:
                    parts[1] = parts[1].toUpperCase();
                    parts[2] = parts[2].toUpperCase();

                    if(parts[1].contains("[")) {
                        if (isNumeric(parts[1].replaceAll("[\\[|\\]]", ""))) {
                            postToUiThread(CALLBACKS_ONSENDERROROUTPUT, context.getString(R.string.ERR_10) + _lineNumber);
                            return false;
                        }

                    }

                    if(parts[2].contains("[")){

                        if (isNumeric(parts[2].replaceAll("[\\[|\\]]", ""))) {
                            postToUiThread(CALLBACKS_ONSENDERROROUTPUT,context.getString(R.string.ERR_10) + _lineNumber);
                            return false;
                        }
                    }

                    if(! Register.isRegister(parts[1])){

                        if(InstructionSet.containsKeyword(parts[1]) || InstructionSet.contains(parts[1])){
                            postToUiThread(CALLBACKS_ONSENDERROROUTPUT, context.getString(R.string.ERR_10) + _lineNumber);
                            return false;
                        }
                        if(! parts[1].contains("[")){
                            if( !parts[1].contains("]")) {
                                postToUiThread(CALLBACKS_ONSENDERROROUTPUT, context.getString(R.string.ERR_10) + _lineNumber);
                                return false;
                            }
                        }
                        if(! InternalMemory.symbolIsDefined(parts[1])) {
                            addAwaitingSymbol(parts[1]);
                        }
                    }

                    // Make sure you can't move **x into *x and vice verse
                    if(Register.isRegister(parts[1]) && Register.isRegister(parts[2])) {
                        if (parts[1].contains("E")) {
                            if (!parts[2].contains("E")) {
                                postToUiThread(CALLBACKS_ONSENDERROROUTPUT, context.getString(R.string.ERR_10) + _lineNumber);
                                return false;
                            }
                        }
                        // ^^
                        if (parts[2].contains("E")) {
                            if (!parts[1].contains("E")) {
                                postToUiThread(CALLBACKS_ONSENDERROROUTPUT, context.getString(R.string.ERR_10) + _lineNumber);
                                return false;
                            }
                        }
                    }
					break;

				default:
                    postToUiThread(CALLBACKS_ONSENDERROROUTPUT, context.getString(R.string.ERR_9) + _lineNumber);
                    return false;
				}

                _statements.add(strippedLine);

				break;

			case Mode.DATA:

				if(parts.length >= 3) {

                    parts = consolidateArguments(parts);

                    if(_awaitingSymbolTable.contains(parts[0]))
                        _awaitingSymbolTable.remove(_awaitingSymbolTable.indexOf(parts[0]));

                    if(parts[1].toUpperCase().matches(InstructionSet.Keywords.equ)){
                        if(parts[2].contains("$") && parts[2].contains("-")){
                            String targetSymbol = parts[2].replaceAll("\\s+","").replace("$-","");
                            parts[2] = Integer.toString(InternalMemory.getEquSize(targetSymbol));
                        }
                    }

                    if(InternalMemory.writeData(parts[1], parts[2], parts[0])){
                        postToUiThread(CALLBACKS_ONSENDERROROUTPUT, parts[0] + " is already defined.");
                    }
                }
                else {
                    return false;
                }

				break;

			case Mode.BSS:
				switch (parts.length) {

                    // Reserved cases for future use
				case 1:
					//Log.v("Izodine","Mnemonic = " + parts[0]);
					break;

				case 2:
					// System.out.print("Mnemonic = " + parts[0]);
                    // Log.v("Izodine"," Source reg = " + parts[1]);
					break;

				case 3:

                    if(_awaitingSymbolTable.contains(parts[0]))
                        _awaitingSymbolTable.remove(_awaitingSymbolTable.indexOf(parts[0]));

                    if (InternalMemory.writeBss(parts[1], Integer.parseInt(parts[2]), parts[0])){
                         postToUiThread(CALLBACKS_ONSENDERROROUTPUT, parts[0] + " is already defined.");
                     }
					break;

				default:
					return false;
				}
				break;

			}

		};
        //this.functionTable = functionBuilder.build();
		return true;
	}

    private String[] consolidateArguments(String[] parts) {

        StringBuilder sb = new StringBuilder();
        for(int i = 2; i < parts.length; i++){
            sb.append(parts[i]);
        }
        String[] consolidatedArr = new String[3];
        System.arraycopy(parts,0,consolidatedArr,0,2);
        consolidatedArr[2] = sb.toString();
        return consolidatedArr;
    }

    private void addAwaitingSymbol(String name){

        _awaitingSymbolTable.add(name);

    }

	private boolean lineHasErrors(String strippedLine, String[] parts) {
//		if(numberOfOccurances(strippedLine, '\'') % 2 != 0)
//			return true;
//
//		if(numberOfOccurances(strippedLine, '\"') % 2 != 0)
//			return true;
//		// Check if keyword exists in set
//		if( ! InstructionSet.contains(parts[1])){
//			_callbacks.onSendErrorOutput(parts[1] + " is not a recognized instruction.");
//			return true;
//		}
//
        if(InstructionSet.contains(parts[0])){
            if(parts.length < (InstructionSet.operandCount(parts[0]) + 1) ){
               postToUiThread(CALLBACKS_ONSENDERROROUTPUT, context.getString(R.string.ERR_7) + _lineNumber);
                return true;
            }
        }

		if(parts.length > 2 && parts[2].contains(".")){
			if(!parts[1].equalsIgnoreCase(InstructionSet.Keywords.dd )
					&& !parts[1].equalsIgnoreCase(InstructionSet.Keywords.dq)){
                postToUiThread(CALLBACKS_ONSENDERROROUTPUT, context.getString(R.string.ERR_8));
				return true;
			}
		}


		return false;
	}

	/** Extracts the parts of the line into a string array. Assumes the line is properly formatted.
	 * @param strippedLine
     */
    private String[] parse(String strippedLine) {
        if(strippedLine.contains("\"") || strippedLine.contains("'")) {
            List<String> list = new ArrayList<>();
            String[] parts;

            strippedLine = strippedLine.replaceAll("\\s+", " ");
            parts = strippedLine.split("(?<=,)(?=([^\"|\']*\"|\'[^\"|\']*\"|\')*[^\"|\']*$)");

            for (String part : parts) {
                if( ! (part.contains("\"") || part.contains("\'")))
                    part = part.replaceAll(",", " ");
                list.addAll(Arrays.asList(part.split("(?<=\\s)")));
            }

            //Log.v("Izodine", "PARSE " + list.toString());
            String[] parsedParts = new String[list.size()];

            return list.toArray(parsedParts);
        }
        else{
            return strippedLine.split("\\s*,\\s*|\\s+");
        }
    }

	private final class Mode {

		private Mode(){};

		public static final String IDLE = "IDLE";
		public static final String BSS = "BSS";
		public static final String TEXT = "TEXT";
		public static final String DATA = "DATA";
	}

//    public static final class Function{
//
//        public final String identifier;
//        public final List<String> statements;
//
//        private int lastExecutedStatement = 0;
//
//        public Function(String identifier, List<String> statements) throws IllegalArgumentException {
//
//            if(identifier == null || statements == null)
//                throw new IllegalArgumentException("Function Constructor parameters cannot be null.");
//
//            this.identifier = identifier;
//            this.statements = statements;
//
//        }
//
//        public String getNextLine(){
//            return statements.get(lastExecutedStatement++);
//        }
//
//    }
//
//    public static final class FunctionMapBuilder {
//
//        private String identifier;
//        private List<String> curStatements;
//        private Map<String, Function> functionMap = new HashMap<>();
//
//        public FunctionMapBuilder addLine(String line){
//            if(identifier == null) throw new NullPointerException("New function not initialized.");
//            curStatements.add(line);
//            return this;
//        }
//
//        public FunctionMapBuilder addFunction(String identifier){
//            curStatements = new ArrayList<>();
//            this.identifier = identifier;
//            addLine(identifier);
//            return this;
//        }
//
//        public FunctionMapBuilder commitFunction(){
//            addLine("ret");
//            functionMap.put(identifier, new Function(identifier, curStatements));
//            curStatements = new ArrayList<>();
//            identifier = null;
//            return this;
//        }
//
//        public FunctionMapBuilder removeLine(String line){
//            if(identifier == null) throw new NullPointerException("New function not initialized.");
//            if(curStatements.contains(line)){
//                curStatements.remove(curStatements.indexOf(line));
//            }
//            return this;
//        }
//
//        public FunctionMapBuilder removeLine(int index){
//            if(identifier == null) throw new NullPointerException("New function not initialized.");
//            curStatements.remove(0);
//            return this;
//        }
//
//        public Map<String, Function> build(){
//            if(identifier == null) throw new NullPointerException("New function not initialized.");
//            return functionMap;
//        }
//
//    }

	public static final class OperandOrder{

        /*
        MOV  register, register
        MOV  register, immediate
        MOV  register, memory
        MOV  memory, immediate
        MOV  memory, register
         */
        private OperandOrder(){}

		public static final int UNDEFINED = -1;

		public static final int REGISTER_REGISTER = 0;

		public static final int REGISTER_IMMEDIATE = 1;

		public static final int MEMORY_IMMEDIATE = 2;

		public static final int REGISTER_MEMORY = 3;

		public static final int MEMORY_REGISTER = 4;

        public static final int ONE_OPERAND = 5;

	}

}

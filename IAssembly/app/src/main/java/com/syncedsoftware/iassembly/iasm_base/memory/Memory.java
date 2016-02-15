package com.syncedsoftware.iassembly.iasm_base.memory;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import com.syncedsoftware.iassembly.iasm_base.instructions.InstructionSet;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents memory for the simulation. Contains simulated BSS and Data segments. Every value gets written to
 * a ByteBuffer. When the simulation starts, the values of each segment get compressed into a single
 * unit starting with the data values. This output  is the compressed version of the memory. Like actual
 * memory, it is possible to corrupt values by performing operations that go out of bounds of the
 * original size of the symbol.
 */
public final class Memory {
	
	private static final int MAX_MEMORY_LIMIT = 8192;
    private static final int CALLBACK_ONMEMORYCHANGED = 0;
    private static final int CALLBACK_ONMEMORYCOMPRESSED = 1;

    private int compressedMemorySize = 0;
    private Handler handler;
    private MemoryListener listener;

	private ByteBuffer bssMemory = ByteBuffer.allocate(MAX_MEMORY_LIMIT);
	private ByteBuffer dataMemory = ByteBuffer.allocate(MAX_MEMORY_LIMIT);
    private ByteBuffer compressedSegments;
	
	/*
	 * Used for uncompressed memory. This is used to store the values before they get
	 * compressed into a single block of memory.
	 */
	private Map<String, Symbol> memorySymbolTable = new HashMap<>();

    /*
     * Used for compressed memory.
     */
    private Map<String, Symbol> compressedMemorySymbolTable = new HashMap<>();

    private List<String> bssMemoryList = new ArrayList<>(12);
    private List<String> dataMemoryList = new ArrayList<>(12);
	
	public Memory(){ }

    /**
     * Attaches a listener to this block of memory. Note that this is a listener for the compressed
     * version of memory, not for the allocation buffers.
     * @param handler A handler to communicate with the UI thread.
     * @param listener The listener.
     */
    public void addListener(Handler handler, MemoryListener listener){
        this.handler = handler;
        this.listener = listener;
    }

    public int getCompressedMemorySize(){
        return this.compressedMemorySize;
    }

    /**
     * Checks if a symbol is defined.
     * @param name The symbol identifier to check.
     * @return True if defined, false otherwise.
     */
	public boolean symbolIsDefined(String name){
		return memorySymbolTable.containsKey(name);
	}

    /**
     * Reserves space in the BSS segment. Note that this is not the actual representation of memory
     * in the simulation. This method is for adding a value to be added to memory when the
     * simulation starts.
     * @param byteCount The amount of bytes to allocate.
     * @param symbolIdentifier The identifier of the symbol.
     * @return True if the symbol identifier has already been defined, false otherwise.
     */
	public boolean writeBss(int byteCount, String symbolIdentifier){

        symbolIdentifier = formatSymbolIdentifier(symbolIdentifier);

        if(memorySymbolTable.containsKey(symbolIdentifier)) return true;

		memorySymbolTable.put(symbolIdentifier, createSymbol(symbolIdentifier, bssMemory.position(), byteCount, false));

		for(int position = 0; position < byteCount; position++){
			bssMemory.put((byte) 0);
		}
        bssMemoryList.add(symbolIdentifier);

		return false;
	}

    public int getEquSize(String target){
		target = formatSymbolIdentifier(target);
//		Log.v("Izodine", "EQU TARGET " + memorySymbolTable.get(target));
//		Log.v("Izodine", "POSITION " + (dataMemory.position() - memorySymbolTable.get(target).rootAddress));
		return dataMemory.position() - memorySymbolTable.get(target).rootAddress;
    }

	public boolean writeBss(String resKeyword, int count, String symbolIdentifier){
		return writeBss(keywordOperandToInt(resKeyword, count), symbolIdentifier);
	}

    public void writeToCompressed(int address, String arguments, String dataType){
        int top = compressedSegments.position();
        compressedSegments.position(address);

        String[] argumentParts = splitArguments(arguments);
        boolean isString = arguments.contains("\'") || arguments.contains("\"");

        putBytes(dataType, argumentParts, compressedSegments);

        compressedSegments.position(top);

        callback(CALLBACK_ONMEMORYCHANGED);
    }

    /**
     * Returns the compressed memory segment. The segment has the data segment values aligned first,
     * followed by the bss segment values.
     */
    public byte[] getCompressedMemory(){
        return compressedSegments.array();
    }

    /**
     * Compresses each internal segment into a single segment. This is the actual representation of
     * memory that will be used by the simulator. This also initializes the stack pointer to the
     * top of the stack.
     */
    public void compress(){
		int len = bssMemory.position() + dataMemory.position();

        this.compressedMemorySize = len;
		compressedSegments = ByteBuffer.allocate(len);

        for(String symbol: dataMemoryList){
            Symbol symbolWrapper = memorySymbolTable.get(symbol);
            int rootAddress =  symbolWrapper.rootAddress;
            int totalBytes = symbolWrapper.len;

			compressedMemorySymbolTable.put(symbol,
                    createSymbol(symbol, compressedSegments.position(), totalBytes, symbolWrapper.isEqu));

            for(int loc = rootAddress; loc < (rootAddress + totalBytes); loc++){
                byte b = dataMemory.get(loc);
                compressedSegments.put(b);
            }
        }

        for(String symbol: bssMemoryList){
            Symbol symbolWrapper = memorySymbolTable.get(symbol);
            int rootAddress =  symbolWrapper.rootAddress;
            int totalBytes = symbolWrapper.len;

			compressedMemorySymbolTable.put(symbol,
                    createSymbol(symbol,compressedSegments.position(), totalBytes, symbolWrapper.isEqu));

            for(int loc = rootAddress; loc < (rootAddress + totalBytes); loc++){
                compressedSegments.put(bssMemory.get(loc));
            }
        }

        callback(CALLBACK_ONMEMORYCOMPRESSED);
    }

	public Symbol getSymbol(String name){
		name = name.replaceAll("[\\[|\\]]", "");
		name = name.replace(":", "");
		if(compressedMemorySymbolTable.containsKey(name)) {
			return compressedMemorySymbolTable.get(name);
		}
		else{
			return null;
		}
	}

	public Number getSymbolValueAsNumber(String name){

		name = name.replaceAll("[\\[|\\]]", "");

		Symbol symbol = getSymbol(name);
		if(symbol == null) return 0;

		byte[] symBytes = new byte[symbol.len];

		compressedSegments.position(symbol.rootAddress);

		for(int i = 0; i < symBytes.length; i++){
			symBytes[i] = 0;
		}

		for(int i = 0; i < symBytes.length; i++){
			symBytes[i] = compressedSegments.get();
		}

		compressedSegments.clear();

		return new BigInteger(symBytes);
	}

	public byte[] getSymbolValueAsArray(String name){

		name = name.replaceAll("[\\[|\\]]", "");
		Symbol symbol = getSymbol(name);
		if(symbol == null) return new byte[0];

		compressedSegments.flip();
		compressedSegments.position(symbol.rootAddress);

		byte[] symBytes = new byte[symbol.len];

		for(int i = 0; i < symBytes.length; i++){
			symBytes[i] = compressedSegments.get();
		}

		return ByteBuffer.wrap(symBytes).array();

	}

    private void callback(final int callbackId) {

        final Memory callbackMemory = this;

        if(handler == null) return;

        switch (callbackId) {

            case CALLBACK_ONMEMORYCOMPRESSED:
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        listener.onMemoryCompressed(callbackMemory);
                    }
                });
                break;

            case CALLBACK_ONMEMORYCHANGED:
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        listener.onMemoryChanged(callbackMemory.getCompressedMemory());
                    }
                });
                break;

        }
    }

    public void clear(){
		bssMemory = ByteBuffer.allocate(MAX_MEMORY_LIMIT);
		dataMemory = ByteBuffer.allocate(MAX_MEMORY_LIMIT);
	}
	
	public static int keywordOperandToInt(String resKeyword, int count) {
		int total = 0;
		switch (resKeyword.trim().toUpperCase()) {

		// Allocate 1 byte
		case InstructionSet.Keywords.resb:
		case InstructionSet.Keywords.db:
		case InstructionSet.Keywords.equ:

			total = count;
			break;
			
		case InstructionSet.Keywords.resw:
		case InstructionSet.Keywords.dw:
			
			total = 2 * count;
			break;
			
		case InstructionSet.Keywords.resd:
		case InstructionSet.Keywords.dd:
			
			total = 4 * count;
			break;
			
		case InstructionSet.Keywords.resq:
		case InstructionSet.Keywords.dq:
			
			total = 8 * count;
			break;
			
		case InstructionSet.Keywords.rest:
		case InstructionSet.Keywords.dt:
			
			total = 10 * count;
			break;
			
		case InstructionSet.Keywords.resdq:
		case InstructionSet.Keywords.dow:
			
			total = 16 * count;
			break;
			
		case InstructionSet.Keywords.resy:
		case InstructionSet.Keywords.dy:
			
			total = 32 * count;
			break;
			
		case InstructionSet.Keywords.resz:
		case InstructionSet.Keywords.dz:
			
			total = 64 * count;
			break;
			
		default:
			System.out.println("Warning: " + resKeyword + " is not a recognized keyword.");
			
		}
		
		return total;
	}
	
	public static Symbol createSymbol(String symbolIdentifier, Integer rootAddress, Integer totalBytes, boolean isEqu){
		return new Symbol(symbolIdentifier,new Pair<>(rootAddress, totalBytes), isEqu);
	}
	
	public static byte[] numberToByteArray(Number num){
		if(num instanceof Short){
			return ByteBuffer.allocate(2).putShort(num.shortValue()).array();
		}
		if(num instanceof Integer){
			return ByteBuffer.allocate(4).putInt(num.intValue()).array();
		}
		if(num instanceof Long){
			return ByteBuffer.allocate(8).putLong(num.longValue()).array();
		}
		if(num instanceof Float){
			return ByteBuffer.allocate(4).putFloat(num.floatValue()).array();
		}
		if(num instanceof Double){
			return ByteBuffer.allocate(8).putDouble(num.doubleValue()).array();
		}
		return new byte[0];
	}

    /**
     * Writes a value to the data segment. Note this is not the actual memory segment used in the simulation.
     * This method is for defining a data value to be used for the simulation.
     * @param dataType The type of data. (DB, DW, etc.)
     * @param arguments The data to be stored.
     * @param symbolIdentifier The identifier for the symbol.
     * @return True is the symbol identifier is already declared. False otherwise.
     */
	public boolean writeData(String dataType, String arguments, String symbolIdentifier) {

        symbolIdentifier = formatSymbolIdentifier(symbolIdentifier);

        if(memorySymbolTable.containsKey(symbolIdentifier)) return true;

		String[] argumentParts = splitArguments(arguments);

		//Log.v("izodine", Arrays.toString(argumentParts));

		int rootAddress = dataMemory.position();
		// For each argument copy the arguments to memoryBox according to dataTypeSize
        int bytesWritten = putBytes(dataType, argumentParts, dataMemory);

		boolean isEqu = false;
		if(dataType.trim().toUpperCase().equals(InstructionSet.Keywords.equ)){
			isEqu = true;
		}
        memorySymbolTable.put(symbolIdentifier, createSymbol(symbolIdentifier, rootAddress, bytesWritten, isEqu));
        dataMemoryList.add(symbolIdentifier);

        return false;
		
//		byte[] test = readData(symbolIdentifier);
//		System.out.println(Float.intBitsToFloat(ByteBuffer.wrap(test).getInt()));
//		System.out.println(Arrays.toString(test));
		
	}

    @NonNull
    private String[] splitArguments(String arguments) {
        return arguments.split("\\s+(?=(?:[^'\"]*['\"][^'\"]*['\"])*[^'\"]*$)|,(?=(?:[^\'\"]*[\'\"][^\'\"]*[\'\"])*[^\'\"]*$)");
    }

//	public Symbol findSymbolWithPtr(int ptr){
//		for(Map.Entry<String, Pair<Integer, Integer>> entry : compressedMemorySymbolTable.entrySet()){
//			if(entry.getValue().elementOne == ptr){
//				return new Symbol(entry.getKey(), entry.getValue());
//			}
//		}
//		return null;
//	}

	public byte[] getRawBytes(int rootAddr, int len){
		byte[] buffer = new byte[len];

        int count = 0;
		for(int i = rootAddr; i < (rootAddr + len); i++){
            if((count) > compressedSegments.capacity() - 1){
                return buffer;
            }
			buffer[count++] = (compressedSegments.get(i));
		}

		return buffer;
	}

//    public Symbol getUncompressedSymbol(String name){
//        if(memorySymbolTable.containsKey(name)){
//            Pair<Integer, Integer> pair = memorySymbolTable.get(name);
//            return new Symbol(name, pair);
//        }
//        return null;
//    }

    private int putBytes(String dataType, String[] argumentParts, ByteBuffer buffer) {
        int bytesWritten = 0;
        for (String argumentPart : argumentParts) {

            byte[] argBytes = encodeValue(argumentPart, dataType);
					//isString ? encodeValue(argumentPart, dataType)
                    //: encodeValue(getNumberValue(argumentPart, dataType), dataType);
            // Discard anything bigger than the data type size
            for (byte argByte : argBytes) {
               // Log.v("Izodine", "ARGBYTE "+ argByte);
                buffer.put(argByte);
                bytesWritten++;
            }

        }

        return bytesWritten;
    }

    private String formatSymbolIdentifier(String symbolIdentifier) {
        return symbolIdentifier.replace(":", "").trim();
    }

    private static Number getNumberValue(String value, String dataType) {
		if(value.contains(".")){

			switch(dataType.toUpperCase()){

			case InstructionSet.Keywords.dd:
				return Float.valueOf(value);

			case InstructionSet.Keywords.dq:
				return Double.valueOf(value);

			default:
				// Only 32 and 64 bit floats are supported right now. Return a double.
				return Double.valueOf(value);
			}
		}
		if(value.contains("0x") || value.contains("0X")){
			return new BigInteger(value.toUpperCase().replace("0X", ""), 16);
		}
		if(value.contains("h") || value.contains("H")){
			return new BigInteger( value.toUpperCase().replace("H","") , 16);
		}
		return new BigInteger(value);
	}

	/** Calculates the correct block size such that the block size takes into account
	 * the data type. For example, if the datatype is a dword and the value is 1 byte, it
	 * will make sure the block size is 2 bytes.
	 * @param totalArgBytes The total amount of bytes.
	 * @param dataTypeSize The size of the data type.
	 * @return The calculated block size.
	 */
	private static int memoryBlockSize(int totalArgBytes, int dataTypeSize) {
		return (dataTypeSize == 1) 
				? totalArgBytes
				: ((totalArgBytes - ((totalArgBytes % dataTypeSize))) + dataTypeSize);
	}

	/**
	 * Strips out any unnecessary symbols or data.
	 * @param value The value to be formatted.
	 * @return The formatted string
	 */
	private static String formatData(String value) {
		if (value.contains("\"") || value.contains("\'")) {
			value = value.replaceAll("\"|\'", "");
		}
		return value;
	}

	/**
	 * Encodes a segment of a string into a byte array using utf-8.
	 * @param value The value to be encoded
	 * @return The encoded byte array.
	 */
	private byte[] encodeValue(String value, String dataType) {
		boolean isString = value.contains("\"") || value.contains("'");
		byte[] encodedBlock, formattedBlock;

		// Encode Strings
		if (isString) {
			value = formatData(value);
			formattedBlock = value.getBytes(Charset.forName("utf-8"));
			encodedBlock = new byte[memoryBlockSize(value.length(), keywordOperandToInt(dataType, 1))];
			System.arraycopy(
					formattedBlock,
					0,
					encodedBlock,
					0,
					formattedBlock.length);

			return encodedBlock;
		} else {
			Number numValue = getNumberValue(value, dataType);
			if (numValue instanceof Float || numValue instanceof Double) {

				switch (dataType.toUpperCase()) {
					case InstructionSet.Keywords.dd:
						return numberToByteArray(Float.floatToRawIntBits(numValue.floatValue()));

					case InstructionSet.Keywords.dq:
						return numberToByteArray(Double.doubleToRawLongBits(numValue.doubleValue()));
				}
			}
			if (numValue instanceof BigInteger) {
				formattedBlock = ((BigInteger) numValue).toByteArray();
				encodedBlock = new byte[memoryBlockSize(formattedBlock.length, keywordOperandToInt(dataType, 1))];
				System.arraycopy(formattedBlock, 0, encodedBlock, 0, formattedBlock.length);
				return encodedBlock;
			}
		}
		return new byte[0];
	}

	public static final class Symbol{

		public final String identifier;
		public final int rootAddress;
		public final int len;
		public final boolean isEqu;

		public Symbol(String identifier, Pair<Integer, Integer> addrPair, boolean isEqu){
			this.identifier = identifier;
			this.rootAddress = addrPair.elementOne;
			this.len = addrPair.elementTwo;
			this.isEqu = isEqu;
		}

	}

    public interface MemoryListener{
        void onMemoryChanged(byte[] compressedMemory);
        void onMemoryCompressed(Memory memory);
    }

}

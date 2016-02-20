package com.syncedsoftware.iassembly.iasm_base.registers;

import android.os.Handler;
import java.nio.ByteBuffer;

public class Register {

    public final String name;

	// 32 bit register value
	private int rValue = 0;

    private RegisterListener listener;
	private Handler handler;

	public Register(String name){
		this.name = name;
	}

    public void addListener(RegisterListener listener, Handler handler){
        this.listener = listener;
		this.handler = handler;
    }

	public void write(int value) {
		rValue = value;
        if(listener != null) handler.post(new Runnable(){
			@Override
			public void run() {
				listener.onRegisterChanged(name, rValue);
			}
		}) ;
	}

	public void write(short value){
		byte[] lowBytes = ByteBuffer.allocate(2).putShort(value).array();
		byte[] rBytes = ByteBuffer.allocate(4).putInt(rValue).array();

		rBytes[2] = lowBytes[0];
		rBytes[3] = lowBytes[1];
		rValue = ByteBuffer.wrap(rBytes).getInt();
		if(listener != null) handler.post(new Runnable(){
			@Override
			public void run() {
				listener.onRegisterChanged(name, rValue);
			}
		}) ;
	}

	public short xValue() {
		return (short)rValue();
		//(rValue << 16) >>> 16;
	}

	public int rValue() {
		return rValue;
	}
	
	public static boolean isSubset(Register register){
		return register instanceof SubsetRegister;
	}
	
	public static boolean isRegister(String value){
		for(String string: RegisterNames.list){
			if(value.equalsIgnoreCase(string) || value.equalsIgnoreCase(string.substring(1, string.length()))){
				return true;
			}
		}
		return false;
	}

	public static boolean isExtendedRegister(String value){
		return isRegister(value) && (value.contains("E") || value.contains("e"));
	}

    public interface RegisterListener{
        // Called when a register value has changed
        void onRegisterChanged(String regName, int value);
    }

}

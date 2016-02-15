package com.syncedsoftware.iassembly.iasm_base.registers;

public final class RegisterNames {
	
	private RegisterNames(){}
	public static final String[] list;
	
	public static final String eax = "eax";
	public static final String ebx = "ebx";
	public static final String ecx = "ecx";
	public static final String edx = "edx";
	
	public static final String esp = "esp";
	public static final String ebp = "ebp";
	
	public static final String esi = "esi";
	public static final String edi = "edi";

	// Subsets
	public static final String ax = "ax";
	public static final String bx = "bx";
	public static final String cx = "cx";
	public static final String dx = "dx";

	static{
		list = new String[]{
				eax,
				ebx,
				ecx,
				edx,
				esp,
				ebp,
				esi,
				edi,
				ax,
				bx,
				cx,
				dx
			};
	}

}

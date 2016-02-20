package com.syncedsoftware.iassembly.iasm_base.utils;

import java.math.BigInteger;

/**
 * Created by Anthony M. Santiago on 2/19/16.
 */
public final class IASMUtils {
    public static boolean isNumeric(String operand) {
        operand = operand.toUpperCase();
        if(operand.contains("0X")){
            return operand.matches("0[xX][0-9a-fA-F]+");
        }
        if(operand.contains("H")){
            return operand.matches("\\d+[h|H]+");
        }

        return operand.matches("\\d+");

    }

    public static Integer radix16Parse(String operand) {

        if(operand.trim().isEmpty()){ return 0;  }

        operand = operand.toUpperCase();
        if(operand.contains("H")) return bigIntStringToInt(operand.replace("H", ""), 16);
        if(operand.contains("0X")) return bigIntStringToInt(operand.replace("X", ""), 16);

        return bigIntStringToInt(operand, 16);

    }

    public static Integer bigIntStringToInt(String integer, int radix){
        return new BigInteger(integer, radix).intValue();
    }
}

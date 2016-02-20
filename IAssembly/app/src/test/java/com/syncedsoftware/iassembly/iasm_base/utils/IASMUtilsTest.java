package com.syncedsoftware.iassembly.iasm_base.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Anthony M. Santiago on 2/19/16.
 */
public class IASMUtilsTest {

    @Test
    public void testIsNumeric_WITH_CONTAINS_H() throws Exception {
        assertEquals(Integer.valueOf(0x5), IASMUtils.radix16Parse("5h"));
    }

    @Test
    public void testIsNumeric_WITH_CONTAINS_0X() throws Exception {
        assertEquals(Integer.valueOf(0x5), IASMUtils.radix16Parse("0x5"));
    }

    @Test
    public void testIsNumeric_WITH_EMPTYSTRING() throws Exception {
        assertEquals(Integer.valueOf(0x0), IASMUtils.radix16Parse(""));
    }

    @Test
    public void testIsNumeric_WITH_EDGE_CASE_INTMAX() throws Exception {
        IASMUtils.radix16Parse("999999999999999");
    }

    @Test
    public void testIsNumeric_WITH_EDGE_CASE_INTMIN() throws Exception {
        IASMUtils.radix16Parse("-999999999999999");
    }
}
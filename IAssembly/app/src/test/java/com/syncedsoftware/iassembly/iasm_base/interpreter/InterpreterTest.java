package com.syncedsoftware.iassembly.iasm_base.interpreter;

import android.test.suitebuilder.annotation.MediumTest;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Anthony M. Santiago on 2/19/16.
 */
public class InterpreterTest implements Interpreter.InterpreterListener{

    Interpreter interpreter;

    @Before
    public void setup(){
        interpreter = new Interpreter();
        interpreter.addCallback(this);
    }

    /**
     * WITH_CONTEXT has to be tested in a connected test.
     * @throws Exception
     */
    @Test
    public void testLoad_WITH_VALID_LINES_WITH_NULL_CONTEXT() throws Exception {
        List<String> testLines = new ArrayList<>();
        testLines.add("section .text");
        testLines.add("xor eax,eax");
        testLines.add("section .data");
        testLines.add("testOp: db \"Hello\"");
        testLines.add("section .bss");
        testLines.add("testOp2: resb 50");
        assertEquals(true, interpreter.load(testLines, null));
    }

    @Test
    public void testLoad_WITH_EMPTY_LINES_WITH_NULL_CONTEXT() throws Exception {
        List<String> testLines = new ArrayList<>();
        assertEquals(false, interpreter.load(testLines, null));
    }

    @Test
    public void testLoad_WITH_VALID_TEXT_SECTION__WITH_NULL_CONTEXT() throws Exception {
        List<String> testLines = new ArrayList<>();
        testLines.add("section .text");
        testLines.add("xor eax,eax");
        assertEquals(true, interpreter.load(testLines, null));
    }

    @Test
    public void testLoad_WITH_VALID_DATA_SECTION_WITH_NULL_CONTEXT() throws Exception {
        List<String> testLines = new ArrayList<>();
        testLines.add("section .data");
        testLines.add("testOp4: db \"Hello\"");
        assertEquals(false, interpreter.load(testLines, null));
    }

    @Test
    public void testLoad_WITH_VALID_BSS_SECTION_WITH_NULL_CONTEXT() throws Exception {
        List<String> testLines = new ArrayList<>();
        testLines.add("section .bss");
        testLines.add("testOp3: resb 50");
        assertEquals(false, interpreter.load(testLines, null));
    }

    @Test
    public void testLoad_WITH_NOT_VALID_LINES_WITH_NULL_CONTEXT() throws Exception {
        List<String> testLines = new ArrayList<>();
        testLines.add("section .text");
        testLines.add("xor eax");
        testLines.add("section .bss");
        testLines.add("testOps: resb 50");
        assertEquals(false, interpreter.load(testLines, null));
    }

    @Test
    public void testLoad_WITH_NOT_VALID_TEXT_SECTION__WITH_NULL_CONTEXT() throws Exception {
        List<String> testLines = new ArrayList<>();
        testLines.add("section .text");
        testLines.add("xor eax");
        assertEquals(false, interpreter.load(testLines, null));
    }

    @Test
    public void testLoad_WITH_NOT_VALID_DATA_SECTION_WITH_NULL_CONTEXT() throws Exception {
        List<String> testLines = new ArrayList<>();
        testLines.add("section .data");
        testLines.add("testOp:");
        assertFalse(interpreter.load(testLines, null));
    }

    @Test
    public void testLoad_WITH_NOT_VALID_BSS_SECTION_WITH_NULL_CONTEXT() throws Exception {
        List<String> testLines = new ArrayList<>();
        testLines.add("section .bss");
        testLines.add("testOp2:");
        assertEquals(false, interpreter.load(testLines, null));
    }

    @Test
    public void testInterpret_WITH_VALID_CODE() throws Exception {
        List<String> testLines = new ArrayList<>();
        testLines.add("section .text");
        testLines.add("xor eax,eax");
        testLines.add("section .data");
        testLines.add("testOp: db \"Hello\"");
        testLines.add("section .bss");
        testLines.add("testOp2: resb 50");
        interpreter.load(testLines, null);

        assertTrue(interpreter.interpret());
    }

    @Test
    public void testInterpret_WITH_INVALID_CODE() throws Exception {
        List<String> testLines = new ArrayList<>();
        testLines.add("section .text");
        testLines.add("xor eax");
        testLines.add("section .data");
        testLines.add("testOp: db \"Hello\"");
        testLines.add("section .bss");
        testLines.add("testOp2: resb 50");
        interpreter.load(testLines, null);

        assertFalse(interpreter.interpret());
    }

    @Test
    public void testInterpret_WITH_EMPTY_CODE() throws Exception {
        List<String> testLines = new ArrayList<>();
        interpreter.load(testLines, null);

        assertFalse(interpreter.interpret());
    }

    @Override
    public void onStartSimulation() {

    }

    @Override
    public void onSendOutput(String msg) {
        System.out.println(msg);
    }

    @Override
    public void onSendErrorOutput(String msg) {
        System.err.println(msg);
    }

    @Override
    public void onEndSimulation() {

    }
}
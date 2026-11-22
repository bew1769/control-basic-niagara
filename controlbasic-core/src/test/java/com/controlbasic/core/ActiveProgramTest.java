package com.controlbasic.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ActiveProgramTest {

    @Test
    void badEditKeepsLastGoodProgramRunning() {
        ActiveProgram active = new ActiveProgram();
        Compilation good = ControlBasic.compile("10 A = A + 1");
        assertTrue(active.offer(good));
        assertFalse(active.offer(ControlBasic.compile("10 A = (A + ")));
        assertSame(good, active.running());
        String status = active.statusText(null);
        assertTrue(status.startsWith("Line 1"), status);
        assertTrue(status.endsWith(ActiveProgram.STILL_RUNNING), status);

        RuntimeState rt = new RuntimeState();
        ControlBasic.run(active.running(), rt, 1000);
        ControlBasic.run(active.running(), rt, 1000);
        assertEquals(2, rt.getVar("A"), 0, "the last good program must keep scanning");
    }

    @Test
    void badProgramWithNothingEarlierHasNothingToRun() {
        ActiveProgram active = new ActiveProgram();
        assertFalse(active.offer(ControlBasic.compile("10 GOTO 99")));
        assertNull(active.running());
        assertFalse(active.statusText(null).contains(ActiveProgram.STILL_RUNNING));
    }

    @Test
    void goodEditClearsCompileError() {
        ActiveProgram active = new ActiveProgram();
        active.offer(ControlBasic.compile("10 A = 1"));
        active.offer(ControlBasic.compile("10 A = "));
        Compilation fixed = ControlBasic.compile("10 A = 2");
        assertTrue(active.offer(fixed));
        assertSame(fixed, active.running());
        assertNull(active.compileError());
        assertEquals("", active.statusText(null));
        assertEquals("Line 1: boom", active.statusText("Line 1: boom"));
    }

    @Test
    void compileErrorWinsOverScanError() {
        ActiveProgram active = new ActiveProgram();
        active.offer(ControlBasic.compile("10 A = 1 / B"));
        active.offer(ControlBasic.compile("10 A = 1 /"));
        assertTrue(active.statusText("Line 1: Division by zero").endsWith(ActiveProgram.STILL_RUNNING));
    }
}

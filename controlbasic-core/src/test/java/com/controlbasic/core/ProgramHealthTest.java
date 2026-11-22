package com.controlbasic.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Block status, reliability and alarm transitions (the "bad edit" message). */
class ProgramHealthTest {

    private static ActiveProgram good() {
        ActiveProgram a = new ActiveProgram();
        a.offer(ControlBasic.compile("10 A = 1"));
        return a;
    }

    @Test
    void badEditRaisesFaultAndGoodEditClearsIt() {
        ProgramHealth h = new ProgramHealth();
        ActiveProgram a = good();
        assertEquals(ProgramHealth.Transition.NONE, h.update(true, a, null, null));
        assertEquals(ProgramHealth.State.RUNNING, h.state());
        assertEquals(PointStatus.OK, h.statusBits());

        a.offer(ControlBasic.compile("10 A = (1 +"));
        assertEquals(ProgramHealth.Transition.FAULT_RAISED, h.update(true, a, null, null));
        assertEquals(ProgramHealth.State.RUNNING, h.state(), "the last good program keeps running");
        assertEquals(ProgramHealth.Reliability.PROCESS_ERROR, h.reliability());
        assertEquals(PointStatus.FAULT, h.statusBits());
        assertTrue(h.fault().startsWith("Compile error: Line 1:"), h.fault());
        assertTrue(h.fault().endsWith(ActiveProgram.STILL_RUNNING), h.fault());

        assertEquals(ProgramHealth.Transition.NONE, h.update(true, a, null, null), "no repeat alarm each scan");

        a.offer(ControlBasic.compile("10 A = 2\n20 B = FOO(1)"));
        assertEquals(ProgramHealth.Transition.FAULT_CHANGED, h.update(true, a, null, null));
        assertTrue(h.fault().contains("Line 2"), h.fault());

        a.offer(ControlBasic.compile("10 A = 2"));
        assertEquals(ProgramHealth.Transition.FAULT_CLEARED, h.update(true, a, null, null));
        assertNull(h.fault());
        assertEquals(PointStatus.OK, h.statusBits());
        assertEquals(ProgramHealth.Reliability.NO_FAULT_DETECTED, h.reliability());
    }

    @Test
    void runtimeErrorFaultsUntilACleanScan() {
        ProgramHealth h = new ProgramHealth();
        ActiveProgram a = good();
        h.update(true, a, null, null);
        assertEquals(ProgramHealth.Transition.FAULT_RAISED, h.update(true, a, "Line 3: Division by zero", null));
        assertEquals("Runtime error: Line 3: Division by zero", h.fault());
        assertEquals(ProgramHealth.Transition.FAULT_CLEARED, h.update(true, a, null, null));
    }

    @Test
    void haltIsHaltedAndFaulted() {
        ProgramHealth h = new ProgramHealth();
        ActiveProgram a = good();
        assertEquals(ProgramHealth.Transition.FAULT_RAISED, h.update(true, a, null, "Low pressure"));
        assertEquals(ProgramHealth.State.HALTED, h.state());
        assertEquals("Halted: Low pressure", h.fault());
        h.update(true, a, null, null);
        assertEquals(ProgramHealth.State.RUNNING, h.state());
        assertEquals(ProgramHealth.Transition.FAULT_RAISED, h.update(true, a, null, ""));
        assertEquals("Halted by HALT", h.fault());
    }

    @Test
    void disabledIsIdleAndClearsTheFault() {
        ProgramHealth h = new ProgramHealth();
        ActiveProgram a = good();
        h.update(true, a, "Line 1: x", null);
        assertEquals(ProgramHealth.Transition.FAULT_CLEARED, h.update(false, a, "Line 1: x", null));
        assertEquals(ProgramHealth.State.IDLE, h.state());
        assertEquals(PointStatus.DISABLED, h.statusBits());
    }

    @Test
    void nothingCompiledYetIsIdleWithFault() {
        ProgramHealth h = new ProgramHealth();
        ActiveProgram a = new ActiveProgram();
        a.offer(ControlBasic.compile("10 A = "));
        assertEquals(ProgramHealth.Transition.FAULT_RAISED, h.update(true, a, null, null));
        assertEquals(ProgramHealth.State.IDLE, h.state());
        assertTrue(h.fault().startsWith("Compile error: Line 1"), h.fault());
        assertTrue(!h.fault().contains("still running"));
    }
}

package com.controlbasic.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ControlBasicTest {
    @Test
    void arithmeticPrecedenceAndIntegerDivision() {
        RuntimeState rt = run(
                "10 A = 1 + 2 * 3\n"
                        + "20 B = 2 ^ 3\n"
                        + "30 C = -2 ^ 2\n"
                        + "40 D = 13 \\ 5\n"
                        + "50 E = 13 MOD 5\n"
                        + "60 F = (NOT 0) = 1\n");
        assertEquals(7, rt.getVar("A"), 0);
        assertEquals(8, rt.getVar("B"), 0);
        assertEquals(-4, rt.getVar("C"), 0);
        assertEquals(2, rt.getVar("D"), 0);
        assertEquals(3, rt.getVar("E"), 0);
        assertEquals(1, rt.getVar("F"), 0);
    }

    @Test
    void singleLineAndBlockIf() {
        RuntimeState rt = run(
                "10 IF 1 THEN A = 5 ELSE A = 9\n"
                        + "20 IF 0 THEN B = 1 ELSE B = 2\n"
                        + "30 IF A > 4 THEN\n"
                        + "40 C = 1\n"
                        + "50 ELSE\n"
                        + "60 C = 2\n"
                        + "70 ENDIF\n"
                        + "80 IF 0 THEN\n"
                        + "90 D = 1\n"
                        + "100 ELSEIF 1 THEN\n"
                        + "110 D = 7\n"
                        + "120 ELSE\n"
                        + "130 D = 9\n"
                        + "140 END IF\n");
        assertEquals(5, rt.getVar("A"), 0);
        assertEquals(2, rt.getVar("B"), 0);
        assertEquals(1, rt.getVar("C"), 0);
        assertEquals(7, rt.getVar("D"), 0);
    }

    @Test
    void gotoGosubForAndOn() {
        RuntimeState rt = run(
                "10 GOTO 40\n"
                        + "20 A = 1\n"
                        + "30 GOTO 50\n"
                        + "40 GOSUB 200\n"
                        + "50 FOR I = 1 TO 3\n"
                        + "60 S = S + I\n"
                        + "70 NEXT I\n"
                        + "80 ON 2 GOTO 100, 110, 120\n"
                        + "90 GOTO 130\n"
                        + "100 B = 1\n"
                        + "105 GOTO 130\n"
                        + "110 B = 2\n"
                        + "115 GOTO 130\n"
                        + "120 B = 3\n"
                        + "130 END\n"
                        + "140 B = 9\n"
                        + "200 A = 4\n"
                        + "210 RETURN\n");
        assertEquals(4, rt.getVar("A"), 0);
        assertEquals(6, rt.getVar("S"), 0);
        assertEquals(2, rt.getVar("B"), 0);
        assertEquals(4, rt.getVar("I"), 0);
    }

    @Test
    void ifPlusFiresOnce() {
        FakeClock clock = new FakeClock();
        Compilation program = compile(
                "10 IF+ AI1 > 5 THEN N = N + 1\n");
        RuntimeState rt = new RuntimeState();
        rt.time = clock;
        MapPoints points = new MapPoints();
        points.values.put("AI1", Double.valueOf(0));
        rt.points = points;
        ControlBasic.run(program, rt, 1000);
        assertEquals(0, rt.getVar("N"), 0);
        points.values.put("AI1", Double.valueOf(9));
        clock.plusSeconds(1);
        ControlBasic.run(program, rt, 1000);
        assertEquals(1, rt.getVar("N"), 0);
        clock.plusSeconds(1);
        ControlBasic.run(program, rt, 1000);
        assertEquals(1, rt.getVar("N"), 0);
    }

    @Test
    void timeOnAndTimeLiteral() {
        FakeClock clock = new FakeClock();
        clock.zoned = ZonedDateTime.of(2026, 11, 21, 0, 6, 0, 0, ZoneId.of("UTC"));
        Compilation program = compile(
                "10 IF BI1 THEN START BO1 ELSE STOP BO1\n"
                        + "20 ELAPSED = TIMEON(BO1)\n"
                        + "30 IF TIME > 00:07:00 THEN OUT1 = 1 ELSE OUT1 = 0\n");
        RuntimeState rt = new RuntimeState();
        rt.time = clock;
        MapPoints points = new MapPoints();
        points.values.put("BI1", Double.valueOf(1));
        rt.points = points;
        ControlBasic.run(program, rt, 1000);
        assertEquals(0, points.values.get("OUT1").doubleValue(), 0);
        assertEquals(0, rt.getVar("ELAPSED"), 0);
        clock.plusSeconds(120);
        clock.zoned = clock.zoned.plusSeconds(120);
        ControlBasic.run(program, rt, 1000);
        assertEquals(120, rt.getVar("ELAPSED"), 0.001);
        clock.zoned = ZonedDateTime.of(2026, 11, 21, 0, 8, 0, 0, ZoneId.of("UTC"));
        ControlBasic.run(program, rt, 1000);
        assertEquals(1, points.values.get("OUT1").doubleValue(), 0);
    }

    @Test
    void variablesPersistAndFirstScan() {
        Compilation program = compile(
                "10 IF FIRST THEN A = 1 ELSE A = A + 1\n"
                        + "20 B = SCANS\n");
        RuntimeState rt = new RuntimeState();
        ControlBasic.run(program, rt, 1000);
        ControlBasic.run(program, rt, 1000);
        assertEquals(2, rt.getVar("A"), 0);
        assertEquals(2, rt.getVar("B"), 0);
    }

    @Test
    void startStopAndShortCircuit() {
        RuntimeState rt = run(
                "10 START FAN\n"
                        + "20 IF FAN AND 1 / FAN = 1 THEN OK = 1\n"
                        + "30 STOP FAN\n"
                        + "40 IF FAN AND 1 / 0 = 1 THEN BAD = 1 ELSE GOOD = 1\n");
        assertEquals(0, rt.getVar("FAN"), 0);
        assertEquals(1, rt.getVar("OK"), 0);
        assertEquals(1, rt.getVar("GOOD"), 0);
        assertEquals(0, rt.getVar("BAD"), 0);
    }

    @Test
    void mathHelpersTableAndScale() {
        RuntimeState rt = run(
                "10 A = ABS(-3.2)\n"
                        + "20 B = INT(-1.2)\n"
                        + "30 C = LIMIT(150, 0, 100)\n"
                        + "40 D = SCALE(70, 68, 76, 0, 100)\n"
                        + "50 E = TABLE(15, 0, 0, 10, 100, 20, 50)\n"
                        + "60 F = MIN(3, 1, 8)\n"
                        + "70 G = MAX(3, 1, 8)\n"
                        + "80 DIM P(3)\n"
                        + "90 P(1) = 4\n"
                        + "100 P(2) = P(1) + 1\n");
        assertEquals(3.2, rt.getVar("A"), 0.0001);
        assertEquals(-2, rt.getVar("B"), 0);
        assertEquals(100, rt.getVar("C"), 0);
        assertEquals(25, rt.getVar("D"), 0.0001);
        assertEquals(75, rt.getVar("E"), 0.0001);
        assertEquals(1, rt.getVar("F"), 0);
        assertEquals(8, rt.getVar("G"), 0);
        assertEquals(5, rt.arrays.get("P")[2], 0);
    }

    @Test
    void instructionLimitAndRuntimeLine() {
        Compilation loop = compile("10 GOTO 10\n");
        Scan limited = ControlBasic.run(loop, new RuntimeState(), 50);
        assertFalse(limited.ok());
        assertTrue(limited.error.format().contains("Exceeded"));
        assertEquals(1, limited.error.line);

        Compilation div = compile("10 A = 1\n20 B = A / 0\n");
        Scan boom = ControlBasic.run(div, new RuntimeState(), 1000);
        assertEquals(2, boom.error.line);
        assertTrue(boom.error.message.contains("Division by zero"));
    }

    @Test
    void parseErrorKeepsLine() {
        Compilation bad = ControlBasic.compile("10 A = 1\n20 A =\n30 B = 2\n");
        assertFalse(bad.ok());
        assertEquals(2, bad.diagnostics().get(0).line);
        assertTrue(bad.slots().isEmpty());
    }

    @Test
    void slotsFromUsagePrefixAndDeclaration() {
        Compilation program = ControlBasic.compile(
                "INPUT RoomTemp\n"
                        + "OUTPUT Damper\n"
                        + "BINPUT Occupied\n"
                        + "10 AO1 = AI1 + 1\n"
                        + "20 IF Occupied THEN START BO1 ELSE STOP BO1\n"
                        + "30 VAR1 = VAR1 + RoomTemp\n"
                        + "40 FOR I = 1 TO 3\n"
                        + "50 NEXT I\n"
                        + "60 Damper = AI2\n");
        assertTrue(program.ok(), program.diagnostics().toString());
        Map<String, SlotSpec> by = new LinkedHashMap<String, SlotSpec>();
        for (SlotSpec spec : program.slots()) {
            by.put(spec.canonical, spec);
        }
        assertEquals(SlotDirection.INPUT, by.get("ROOMTEMP").direction);
        assertEquals(SlotDirection.OUTPUT, by.get("DAMPER").direction);
        assertEquals(SlotDataType.BOOLEAN, by.get("OCCUPIED").dataType);
        assertEquals(SlotDirection.INPUT, by.get("OCCUPIED").direction);
        assertEquals(SlotDirection.OUTPUT, by.get("AO1").direction);
        assertEquals(SlotDirection.INPUT, by.get("AI1").direction);
        assertEquals(SlotDataType.BOOLEAN, by.get("BO1").dataType);
        assertEquals(SlotDirection.OUTPUT, by.get("BO1").direction);
        assertEquals(SlotDirection.VARIABLE, by.get("VAR1").direction);
        assertEquals(SlotDirection.INPUT, by.get("AI2").direction);
        assertFalse(by.containsKey("I"));
        assertFalse(by.containsKey("TIME"));
    }

    @Test
    void slotPlanPreservesMatchingSlots() {
        List<SlotPlan.Existing> existing = new ArrayList<SlotPlan.Existing>();
        existing.add(new SlotPlan.Existing("AI1", false, true));
        existing.add(new SlotPlan.Existing("AO1", false, true));
        existing.add(new SlotPlan.Existing("GONE", false, true));
        existing.add(new SlotPlan.Existing("UserNote", false, false));
        List<SlotSpec> desired = new ArrayList<SlotSpec>();
        desired.add(new SlotSpec("AI1", SlotDirection.INPUT, SlotDataType.NUMERIC));
        desired.add(new SlotSpec("AO1", SlotDirection.OUTPUT, SlotDataType.BOOLEAN));
        desired.add(new SlotSpec("BO1", SlotDirection.OUTPUT, SlotDataType.BOOLEAN));
        SlotPlan plan = SlotPlan.diff(existing, desired);
        assertEquals(2, plan.remove.size());
        assertTrue(plan.remove.contains("GONE"));
        assertTrue(plan.remove.contains("AO1"));
        assertFalse(plan.remove.contains("UserNote"));
        assertFalse(plan.remove.contains("AI1"));
        assertEquals(2, plan.add.size());
    }

    @Test
    void pointsRoundTripInputsAndOutputs() {
        Compilation program = compile(Samples.DAMPER);
        RuntimeState rt = new RuntimeState();
        MapPoints points = new MapPoints();
        points.values.put("AI1", Double.valueOf(80));
        points.values.put("VAR1", Double.valueOf(90));
        rt.points = points;
        Scan scan = ControlBasic.run(program, rt, 10000);
        assertTrue(scan.ok(), scan.error == null ? "" : scan.error.format());
        assertEquals(100, points.values.get("AO1").doubleValue(), 0.001);
        assertEquals(1, points.values.get("BO1").doubleValue(), 0);
        assertEquals(80, points.values.get("AI1").doubleValue(), 0);
        assertFalse(points.values.containsKey("FIRST"));
    }

    @Test
    void endHaltsTheRestOfTheScanOnly() {
        Compilation program = compile("10 A = 1\n20 END\n30 A = 5\n");
        RuntimeState rt = new RuntimeState();
        ControlBasic.run(program, rt, 1000);
        assertEquals(1, rt.getVar("A"), 0);
        assertFalse(rt.isHalted());
        Compilation halt = compile("10 HALT\n20 A = 1\n");
        RuntimeState stopped = new RuntimeState();
        Scan first = ControlBasic.run(halt, stopped, 1000);
        assertTrue(first.halted);
        Scan second = ControlBasic.run(halt, stopped, 1000);
        assertTrue(second.halted);
        assertEquals(0, second.instructions);
        assertEquals(0, stopped.getVar("A"), 0);
    }

    @Test
    void commentsCaseAndMultiStatement() {
        RuntimeState rt = run(
                "REM header\n"
                        + "10 a = 1 : b = a + 2 ' tail\n"
                        + "20 // whole line\n"
                        + "30 PRINT \"A=\"; A\n");
        assertEquals(1, rt.getVar("a"), 0);
        assertEquals(3, rt.getVar("B"), 0);
        assertTrue(rt.console().contains("A="));
        assertTrue(rt.console().contains("1"));
    }

    @Test
    void highlightKindsAndErrorLine() {
        List<Highlight.Run> runs = Highlight.colorLine("10 REM note", 1, -1);
        assertTrue(runs.stream().anyMatch(r -> r.style == Highlight.NUMBER));
        assertTrue(runs.stream().anyMatch(r -> r.style == Highlight.COMMENT));
        List<Highlight.Run> bad = Highlight.colorLine("A = 1", 4, 4);
        assertEquals(1, bad.size());
        assertEquals(Highlight.ERROR, bad.get(0).style);
    }

    @Test
    void sampleProgramCompiles() {
        Compilation program = ControlBasic.compile(Samples.DAMPER);
        assertTrue(program.ok(), program.diagnostics().toString());
        assertNull(program.diagnostics().isEmpty() ? null : program.diagnostics().get(0));
    }

    @Test
    void dowSundayIsOne() {
        FakeClock clock = new FakeClock();
        clock.zoned = ZonedDateTime.of(2026, 11, 22, 12, 0, 0, 0, ZoneId.of("UTC"));
        Compilation program = compile("10 D = DOW\n20 M = DOM\n");
        RuntimeState rt = new RuntimeState();
        rt.time = clock;
        ControlBasic.run(program, rt, 100);
        assertEquals(1, rt.getVar("D"), 0);
        assertEquals(22, rt.getVar("M"), 0);
    }

    private static RuntimeState run(String source) {
        Compilation program = compile(source);
        RuntimeState state = new RuntimeState();
        Scan scan = ControlBasic.run(program, state, 10000);
        assertTrue(scan.ok(), scan.error == null ? "" : scan.error.format());
        return state;
    }

    private static Compilation compile(String source) {
        Compilation program = ControlBasic.compile(source);
        assertTrue(program.ok(), program.diagnostics().toString());
        return program;
    }

    static final class FakeClock implements TimeSource {
        long millis = Instant.parse("2026-11-21T00:00:00Z").toEpochMilli();
        ZonedDateTime zoned = ZonedDateTime.of(2026, 11, 21, 0, 0, 0, 0, ZoneId.of("UTC"));

        void plusSeconds(long seconds) {
            millis += seconds * 1000L;
        }

        @Override
        public long nowMillis() {
            return millis;
        }

        @Override
        public ZonedDateTime now() {
            return zoned;
        }
    }

    static final class MapPoints implements PointImage {
        final Map<String, Double> values = new LinkedHashMap<String, Double>();

        @Override
        public double get(String canonicalName) {
            Double v = values.get(canonicalName);
            return v == null ? 0 : v.doubleValue();
        }

        @Override
        public void set(String canonicalName, double value) {
            values.put(canonicalName, Double.valueOf(value));
        }
    }
}

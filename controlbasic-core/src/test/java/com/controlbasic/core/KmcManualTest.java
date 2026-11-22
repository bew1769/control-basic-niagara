package com.controlbasic.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Behavior taken from the KMC manuals. Page numbers are from the KMC Connect
 * Installation and Reference Guide, Rev N, unless marked "Module Rev E" (KMC
 * BACnet Module for NiagaraAX, Rev E).
 */
class KmcManualTest {

    private static RuntimeState scan(String program) {
        Compilation c = ControlBasic.compile(program);
        assertTrue(c.ok(), program + " -> " + c.diagnostics());
        RuntimeState rt = new RuntimeState();
        Scan s = ControlBasic.run(c, rt, 1000);
        assertNull(s.error, program + " -> " + (s.error == null ? "" : s.error.format()));
        return rt;
    }

    private static double value(String expr) {
        return scan("10 A = " + expr).getVar("A");
    }

    // ---- Illustration 28-2, p.223: Order of operator precedence ----------

    @TestFactory
    List<DynamicTest> precedencePerIllustration28_2() {
        Object[][] cases = {
            // NOT is above ^
            {"NOT 2 ^ 2", 0.0},
            {"NOT 0 + 1", 2.0},
            // ^ above * and /
            {"2 ^ 3 * 2", 16.0},
            {"2 * 3 ^ 2", 18.0},
            // * and / above \
            {"7 \\ 2 * 2", 1.0},
            {"9 \\ 2 / 2", 9.0},
            // \ above MOD
            {"9 MOD 4 \\ 2", 1.0},
            // MOD above + and -
            {"1 + 7 MOD 3", 2.0},
            {"10 - 7 MOD 4", 7.0},
            // + and - above relational
            {"1 + 2 > 2", 1.0},
            {"2 < 1 + 2", 1.0},
            // relational above AND
            {"0 = 0 AND 0", 0.0},
            {"1 < 2 AND 3 > 2", 1.0},
            // AND above OR
            {"1 OR 1 AND 0", 1.0},
            {"0 AND 1 OR 1", 1.0},
            // OR above XOR (XOR is lowest)
            {"1 XOR 1 OR 1", 0.0},
            {"1 OR 0 XOR 1", 0.0},
            // same level left to right
            {"8 / 4 / 2", 1.0},
            {"2 ^ 3 ^ 2", 64.0},
            {"10 - 4 - 3", 3.0},
            {"2 * 6 / 3", 4.0},
            // parentheses first
            {"NOT (2 ^ 2)", 0.0},
            {"(1 XOR 1) OR 1", 1.0},
            // Table 28-4, p.225
            {"13 \\ 5", 2.0},
            {"13 MOD 5", 3.0},
            {"10 / 4", 2.5},
            {"2 * 4", 8.0},
            // Table 28-3, p.224
            {"5 = 2", 0.0},
            {"5 <> 2", 1.0},
            {"5 < 2", 0.0},
            {"5 > 2", 1.0},
            {"5 <= 2", 0.0},
            {"5 >= 2", 1.0},
        };
        List<DynamicTest> tests = new ArrayList<DynamicTest>();
        for (Object[] c : cases) {
            final String expr = (String) c[0];
            final double expected = ((Double) c[1]).doubleValue();
            tests.add(dynamicTest(expr + " = " + expected, () -> assertEquals(expected, value(expr), 1e-9, expr)));
        }
        return tests;
    }

    // ---- functions -------------------------------------------------------

    @Test
    void rndIsAnIntegerFromZeroToNMinusOne() {
        // p.316: "a random number between 0 and expression-1"
        Compilation c = ControlBasic.compile("10 A = RND(3)");
        RuntimeState rt = new RuntimeState();
        boolean[] seen = new boolean[3];
        for (int i = 0; i < 300; i++) {
            ControlBasic.run(c, rt, 100);
            double a = rt.getVar("A");
            assertEquals(Math.floor(a), a, 0);
            assertTrue(a >= 0 && a <= 2, "RND(3) gave " + a);
            seen[(int) a] = true;
        }
        assertTrue(seen[0] && seen[1] && seen[2]);
    }

    @Test
    void scansIsScansPerSecond() {
        // p.316: SCANS "returns the rate ... in scans per second"
        ControlBasicTest.FakeClock clock = new ControlBasicTest.FakeClock();
        Compilation c = ControlBasic.compile("10 R = SCANS\n20 N = SCANCOUNT\n30 S = S + 1 / SCANS");
        RuntimeState rt = new RuntimeState();
        rt.time = clock;
        rt.scanIntervalSeconds = 0.5;
        ControlBasic.run(c, rt, 100);
        assertEquals(2, rt.getVar("R"), 1e-9);
        for (int i = 0; i < 4; i++) {
            clock.plusMillis(250);
            ControlBasic.run(c, rt, 100);
        }
        assertEquals(4, rt.getVar("R"), 1e-9);
        assertEquals(5, rt.getVar("N"), 0);
        // KMC's own example: A = 1/SCANS : B = A + B counts seconds.
        assertEquals(0.5 + 4 * 0.25, rt.getVar("S"), 1e-9);
    }

    @Test
    void dowMondayOneSundaySevenAndDayConstants() {
        // Module Rev E p.49: "Monday is day 1, Sunday is day 7"; Connect p.272: IF D = MON
        ControlBasicTest.FakeClock clock = new ControlBasicTest.FakeClock();
        clock.zoned = ZonedDateTime.of(2026, 11, 23, 12, 0, 0, 0, ZoneId.of("UTC"));
        Compilation c = ControlBasic.compile("10 D = DOW\n20 IF DOW = MON THEN M = 1\n30 W = SUN");
        RuntimeState rt = new RuntimeState();
        rt.time = clock;
        ControlBasic.run(c, rt, 100);
        assertEquals(1, rt.getVar("D"), 0);
        assertEquals(1, rt.getVar("M"), 0);
        assertEquals(7, rt.getVar("W"), 0);
    }

    @Test
    void doyCountsFebruaryAs29Days() {
        // p.273: March 1 is always day 61, December 31 is day 366
        assertEquals(1, Evaluator.kmcDayOfYear(1, 1));
        assertEquals(59, Evaluator.kmcDayOfYear(2, 28));
        assertEquals(60, Evaluator.kmcDayOfYear(2, 29));
        assertEquals(61, Evaluator.kmcDayOfYear(3, 1));
        assertEquals(366, Evaluator.kmcDayOfYear(12, 31));
        ControlBasicTest.FakeClock clock = new ControlBasicTest.FakeClock();
        clock.zoned = ZonedDateTime.of(2027, 3, 1, 12, 0, 0, 0, ZoneId.of("UTC"));
        Compilation c = ControlBasic.compile("10 D = DOY");
        RuntimeState rt = new RuntimeState();
        rt.time = clock;
        ControlBasic.run(c, rt, 100);
        assertEquals(61, rt.getVar("D"), 0, "2027 is not a leap year; KMC still calls March 1 day 61");
    }

    @Test
    void psychrometricsTakeHumidityFirst() {
        // pp.270-271, p.277: DEWPOINT(OAH, OAT) in F, DEWPOINTSI in C; ENTHALPY Btu/lb, ENTHALPYSI kJ/kg
        assertEquals(9.26, value("DEWPOINTSI(50, 20)"), 0.05);
        assertEquals(48.67, value("DEWPOINT(50, 68)"), 0.1);
        assertEquals(38.5, value("ENTHALPYSI(50, 20)"), 0.3);
        assertEquals(24.23, value("ENTHALPY(50, 68)"), 0.1);
        assertEquals(value("DEWPOINTSI(40, 25)"), value("DEW-POINT-SI(40, 25)"), 1e-9);
        assertNotNull(compileError("10 A = DEWPOINTF(68, 50)"));
    }

    @Test
    void avgOfExpressions() {
        // p.258
        assertEquals(4, value("AVG(2, 4, 6)"), 1e-9);
        assertEquals(5, value("AVG(5)"), 1e-9);
    }

    @Test
    void incAndDecStatements() {
        // p.286 INC(point[, step]); p.270 DEC
        RuntimeState rt = scan("10 A = 5\n20 INC(A)\n30 INC(A, 2.5)\n40 B = 1\n50 DEC(B)\n60 DEC(B, 3)");
        assertEquals(8.5, rt.getVar("A"), 1e-9);
        assertEquals(-3, rt.getVar("B"), 1e-9);
    }

    @Test
    void haltWithMessage() {
        // p.283 HALT "Message": state Halted, message in Description of Halt
        Compilation c = ControlBasic.compile("10 N = N + 1\n20 HALT \"Low pressure\"\n30 N = 99");
        RuntimeState rt = new RuntimeState();
        Scan s = ControlBasic.run(c, rt, 100);
        assertTrue(s.halted);
        assertEquals("Low pressure", rt.haltMessage());
        ControlBasic.run(c, rt, 100);
        assertEquals(1, rt.getVar("N"), 0);
    }

    @Test
    void onErrorRedirectsWhenThePreviousLineFails() {
        // p.305: "redirects the program to the line specified by location when an
        // error is detected in the previous line"
        RuntimeState rt = scan("10 A = 1 / 0\n20 ONERROR 100\n30 B = 1\n40 END\n100 C = 1");
        assertEquals(0, rt.getVar("B"), 0);
        assertEquals(1, rt.getVar("C"), 0);
        assertTrue(rt.handledError().contains("Division by zero"), rt.handledError());
        // No error: ONERROR falls through.
        rt = scan("10 A = 1 / 1\n20 ONERROR 100\n30 B = 1\n40 END\n100 C = 1");
        assertEquals(1, rt.getVar("B"), 0);
        assertEquals(0, rt.getVar("C"), 0);
        // KMC's example puts ONERROR after a one-line IF.
        rt = scan("10 IF 1 THEN A = 1 / 0\n20 ONERROR 100\n30 END\n100 C = 1");
        assertEquals(1, rt.getVar("C"), 0);
    }

    @Test
    void onErrorOnlyCoversThePreviousLine() {
        Compilation c = ControlBasic.compile("10 A = 1 / 0\n20 B = 1\n30 ONERROR 100\n40 END\n100 C = 1");
        Scan s = ControlBasic.run(c, new RuntimeState(), 100);
        assertNotNull(s.error, "an error two lines above ONERROR is not handled");
        assertEquals(1, s.error.line, "physical line of 10 A = 1 / 0");
    }

    @Test
    void errorLevelReportsThePreviousStatement() {
        // p.278: "returns a non zero value when an error is detected in the previous statement"
        RuntimeState rt = scan("10 A = SQR(-1)\n20 E1 = ERRORLEVEL\n30 B = 1\n40 E2 = ERRORLEVEL");
        assertEquals(1, rt.getVar("E1"), 0);
        assertEquals(0, rt.getVar("E2"), 0);
        rt = scan("10 A = SQR(-1)\n20 IF ERRORLEVEL THEN A = -1");
        assertEquals(-1, rt.getVar("A"), 0);
    }

    @Test
    void nanCanBeStoredAndTested() {
        // p.296: "Use NAN to set a variable ... or to test if ... equal to Not A Number"
        RuntimeState rt = scan("10 A = NAN\n20 IF A = NAN THEN B = 1\n30 IF A <> NAN THEN C = 1\n40 D = ISNAN(A)\n50 E = A + 1");
        assertTrue(Double.isNaN(rt.getVar("A")));
        assertEquals(1, rt.getVar("B"), 0);
        assertEquals(0, rt.getVar("C"), 0);
        assertEquals(1, rt.getVar("D"), 0);
        assertTrue(Double.isNaN(rt.getVar("E")), "NAN propagates");
        rt = scan("10 P = POS_INFINITY\n20 N = NEG-INFINITY\n30 Q = P > 1000000");
        assertEquals(Double.POSITIVE_INFINITY, rt.getVar("P"), 0);
        assertEquals(Double.NEGATIVE_INFINITY, rt.getVar("N"), 0);
        assertEquals(1, rt.getVar("Q"), 0);
    }

    @Test
    void finiteMathThatProducesNanIsStillAnError() {
        for (String expr : new String[] {"0 ^ -1", "(-8) ^ 0.5", "10 ^ 200 * 10 ^ 200", "SQR(-1)", "ARCSIN(2)"}) {
            Compilation c = ControlBasic.compile("10 A = " + expr);
            Scan s = ControlBasic.run(c, new RuntimeState(), 100);
            assertNotNull(s.error, expr);
        }
    }

    // ---- status flags --------------------------------------------------

    private static ControlBasicTest.MapPoints points() {
        return new ControlBasicTest.MapPoints();
    }

    @Test
    void inputStatusFlagsAreReadable() {
        ControlBasicTest.MapPoints pts = points();
        pts.values.put("AI1", 10.0);
        pts.values.put("AI2", 20.0);
        pts.values.put("AI3", 30.0);
        pts.values.put("AI4", 40.0);
        pts.statuses.put("AI1", PointStatus.FAULT);
        pts.statuses.put("AI2", PointStatus.OVERRIDDEN);
        pts.statuses.put("AI3", PointStatus.ALARM | PointStatus.DISABLED);
        pts.statuses.put("AI4", PointStatus.DOWN | PointStatus.STALE);
        Compilation c = ControlBasic.compile(
                "10 AV1 = AI1.FAULT\n20 AV2 = AI2.OVERRIDDEN\n30 AV3 = AI3.IN_ALARM\n40 AV4 = AI3.OUT_OF_SERVICE\n"
                        + "50 AV5 = AI4.DOWN + AI4.STALE * 2\n60 AV6 = AI1.STATUS\n70 AV7 = AI2.PV\n80 AV8 = AI2.FAULT\n"
                        + "90 AV9 = AI4");
        assertTrue(c.ok(), c.diagnostics().toString());
        RuntimeState rt = new RuntimeState();
        rt.points = pts;
        assertNull(ControlBasic.run(c, rt, 100).error);
        assertEquals(1, pts.values.get("AV1"), 0);
        assertEquals(1, pts.values.get("AV2"), 0);
        assertEquals(1, pts.values.get("AV3"), 0);
        assertEquals(1, pts.values.get("AV4"), 0);
        assertEquals(3, pts.values.get("AV5"), 0);
        assertEquals(PointStatus.FAULT, pts.values.get("AV6"), 0);
        assertEquals(20, pts.values.get("AV7"), 0);
        assertEquals(0, pts.values.get("AV8"), 0);
        assertEquals(40, pts.values.get("AV9"), 0, "a down point keeps its last value (KMC ISNAN)");
    }

    @Test
    void nullInputReadsAsNanAndOutputsGoNull() {
        ControlBasicTest.MapPoints pts = points();
        pts.values.put("AI1", 55.0);
        pts.statuses.put("AI1", PointStatus.NULL);
        Compilation c = ControlBasic.compile(
                "10 AO1 = AI1 * 2\n20 AV1 = ISNAN(AI1)\n30 AV2 = AI1.NULL\n40 IF ISNAN(AI1) THEN AO2 = 0 ELSE AO2 = AI1");
        RuntimeState rt = new RuntimeState();
        rt.points = pts;
        assertNull(ControlBasic.run(c, rt, 100).error);
        assertTrue(Double.isNaN(pts.values.get("AO1")));
        assertEquals(PointStatus.NULL, pts.statuses.get("AO1").intValue());
        assertEquals(1, pts.values.get("AV1"), 0);
        assertEquals(1, pts.values.get("AV2"), 0);
        assertEquals(0, pts.values.get("AO2"), 0);
        assertEquals(PointStatus.OK, pts.statuses.get("AO2").intValue());
        // The input comes back: everything is ok again.
        pts.statuses.put("AI1", PointStatus.OK);
        assertNull(ControlBasic.run(c, rt, 100).error);
        assertEquals(110, pts.values.get("AO1"), 0);
        assertEquals(PointStatus.OK, pts.statuses.get("AO1").intValue());
    }

    @Test
    void runtimeErrorFaultsOutputsAndKeepsValues() {
        ControlBasicTest.MapPoints pts = points();
        pts.values.put("AI1", 2.0);
        Compilation c = ControlBasic.compile("10 AO1 = 10 / AI1\n20 AO2 = 1 / (AI1 - 2)");
        RuntimeState rt = new RuntimeState();
        rt.points = pts;
        pts.values.put("AI1", 5.0);
        assertNull(ControlBasic.run(c, rt, 100).error);
        assertEquals(2, pts.values.get("AO1"), 0);
        pts.values.put("AI1", 2.0);
        Scan bad = ControlBasic.run(c, rt, 100);
        assertNotNull(bad.error);
        assertEquals(2, pts.values.get("AO1"), 0, "values are not written on a failed scan");
        assertEquals(PointStatus.FAULT, pts.statuses.get("AO1").intValue());
        assertEquals(PointStatus.FAULT, pts.statuses.get("AO2").intValue());
        pts.values.put("AI1", 4.0);
        assertNull(ControlBasic.run(c, rt, 100).error);
        assertEquals(PointStatus.OK, pts.statuses.get("AO1").intValue());
    }

    @Test
    void haltFaultsOutputs() {
        ControlBasicTest.MapPoints pts = points();
        Compilation c = ControlBasic.compile("10 AO1 = 3\n20 HALT");
        RuntimeState rt = new RuntimeState();
        rt.points = pts;
        assertTrue(ControlBasic.run(c, rt, 100).halted);
        assertEquals(3, pts.values.get("AO1"), 0);
        assertEquals(PointStatus.FAULT, pts.statuses.get("AO1").intValue());
    }

    @Test
    void propertyReadCreatesAnInputSlotAndIsReadOnly() {
        Compilation c = ControlBasic.compile("10 AV1 = BI1.FAULT");
        assertTrue(c.ok());
        boolean found = false;
        for (SlotSpec s : c.slots()) {
            if (s.canonical.equals("BI1")) {
                found = s.direction == SlotDirection.INPUT;
            }
        }
        assertTrue(found, c.slots().toString());
        assertTrue(compileError("10 AI1.FAULT = 1").contains("read-only"));
        assertTrue(compileError("10 A = AI1.BOGUS").contains("Unknown property"));
        // .PV may be written: it is the value.
        RuntimeState rt = scan("10 AV1.PV = 4\n20 B = AV1");
        assertEquals(4, rt.getVar("B"), 0);
    }

    @Test
    void outputStatusRule() {
        assertEquals(PointStatus.OK, ControlBasic.outputStatus(1, new Scan(null, false, 1)));
        assertEquals(PointStatus.NULL, ControlBasic.outputStatus(Double.NaN, new Scan(null, false, 1)));
        assertEquals(PointStatus.FAULT,
                ControlBasic.outputStatus(1, new Scan(new Diagnostic(1, 1, "x"), false, 1)));
        assertEquals(PointStatus.FAULT, ControlBasic.outputStatus(1, new Scan(null, true, 1)));
        assertEquals("{fault,overridden}", PointStatus.describe(PointStatus.FAULT | PointStatus.OVERRIDDEN));
    }

    private static String compileError(String program) {
        Compilation c = ControlBasic.compile(program);
        assertFalse(c.ok(), "expected a compile error: " + program);
        return c.diagnostics().get(0).format();
    }
}

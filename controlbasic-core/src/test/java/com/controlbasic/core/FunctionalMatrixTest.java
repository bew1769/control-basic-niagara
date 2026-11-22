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
 * Expected-vs-actual matrix over the language in docs/LANGUAGE.md. Each row
 * runs one program for one scan and checks one variable.
 */
class FunctionalMatrixTest {

    private static final double EPS = 1e-9;

    private static final class Row {
        final String name;
        final String program;
        final String var;
        final double expected;
        final double tol;

        Row(String name, String program, String var, double expected, double tol) {
            this.name = name;
            this.program = program;
            this.var = var;
            this.expected = expected;
            this.tol = tol;
        }
    }

    private static Row r(String name, String program, String var, double expected) {
        return new Row(name, program, var, expected, EPS);
    }

    private static Row r(String name, String program, String var, double expected, double tol) {
        return new Row(name, program, var, expected, tol);
    }

    private static List<Row> rows() {
        List<Row> rows = new ArrayList<Row>();
        // Arithmetic and precedence
        rows.add(r("mul before add", "10 A = 2 + 3 * 4", "A", 14));
        rows.add(r("parentheses", "10 A = (2 + 3) * 4", "A", 20));
        rows.add(r("real divide", "10 A = 10 / 4", "A", 2.5));
        rows.add(r("integer divide", "10 A = 10 \\ 4", "A", 2));
        rows.add(r("integer divide toward zero", "10 A = -7 \\ 2", "A", -3));
        rows.add(r("MOD toward zero", "10 A = -7 MOD 3", "A", -1));
        rows.add(r("MOD below *", "10 A = 2 * 3 MOD 4", "A", 2));
        rows.add(r("MOD above +", "10 A = 7 MOD 3 + 1", "A", 2));
        rows.add(r("unary minus before ^", "10 A = -2 ^ 2", "A", -4));
        rows.add(r("double negative", "10 A = 3 - -2", "A", 5));
        rows.add(r("power", "10 A = 2 ^ 10", "A", 1024));
        rows.add(r("decimals", "10 A = 0.1 + 0.2", "A", 0.3, 1e-12));
        rows.add(r("add before compare", "10 A = 1 + 2 = 3", "A", 1));
        rows.add(r("relational before equality", "10 A = 2 < 3 = 1", "A", 1));
        // Comparisons
        rows.add(r("gt", "10 A = 5 > 3", "A", 1));
        rows.add(r("lt false", "10 A = 5 < 3", "A", 0));
        rows.add(r("ge equal", "10 A = 5 >= 5", "A", 1));
        rows.add(r("le false", "10 A = 5 <= 4", "A", 0));
        rows.add(r("eq", "10 A = 5 = 5", "A", 1));
        rows.add(r("ne", "10 A = 5 <> 5", "A", 0));
        // Logic
        rows.add(r("AND", "10 A = 1 AND 0", "A", 0));
        rows.add(r("OR", "10 A = 1 OR 0", "A", 1));
        rows.add(r("XOR", "10 A = 1 XOR 1", "A", 0));
        rows.add(r("NOT 0", "10 A = NOT 0", "A", 1));
        rows.add(r("NOT nonzero", "10 A = NOT 5", "A", 0));
        rows.add(r("logic yields 1", "10 A = 2 AND 3", "A", 1));
        rows.add(r("NOT binds tighter than +", "10 X = 0\n20 A = NOT X + 1", "A", 2));
        rows.add(r("negative exponent", "10 A = 2 ^ -1", "A", 0.5));
        rows.add(r("negative exponent then multiply", "10 A = 2 ^ -1 * 4", "A", 2));
        rows.add(r("leading minus on power", "10 A = -2 ^ 2", "A", -4));
        rows.add(r("power right-assoc", "10 A = 2 ^ 3 ^ 2", "A", 512));
        rows.add(r("AND binds tighter than OR", "10 A = 1 OR 1 AND 0", "A", 1));
        rows.add(r("OR binds tighter than XOR", "10 A = 1 XOR 1 OR 1", "A", 0));
        rows.add(r("XOR left to right", "10 A = 1 XOR 1 XOR 1", "A", 1));
        rows.add(r("AND short-circuits divide by zero",
                "10 F = 0\n20 IF F AND 1 / F > 0 THEN A = 1 ELSE A = 2", "A", 2));
        // IF / GOTO / loops
        rows.add(r("IF THEN ELSE true", "10 IF 3 > 2 THEN A = 1 ELSE A = 2", "A", 1));
        rows.add(r("IF THEN ELSE false", "10 IF 3 < 2 THEN A = 1 ELSE A = 2", "A", 2));
        rows.add(r("nested block IF",
                "10 X = 5\n20 IF X > 0 THEN\n30 IF X > 3 THEN\n40 A = 2\n50 ELSE\n60 A = 1\n70 ENDIF\n80 ELSE\n90 A = 0\n100 ENDIF",
                "A", 2));
        rows.add(r("THEN line-number goto", "10 IF 1 THEN 40\n20 A = 1\n30 END\n40 A = 2", "A", 2));
        rows.add(r("GOTO loop with counter", "10 I = 0\n20 I = I + 1\n30 IF I < 5 THEN GOTO 20", "I", 5));
        rows.add(r("label GOTO", "10 GOTO DONE\n20 A = 1\nDONE: A = A + 7", "A", 7));
        rows.add(r("FOR sum", "10 FOR I = 1 TO 10\n20 S = S + I\n30 NEXT I", "S", 55));
        rows.add(r("FOR negative STEP", "10 FOR I = 10 TO 1 STEP -3\n20 S = S + I\n30 NEXT I", "S", 22));
        rows.add(r("FOR zero iterations", "10 S = 0\n20 FOR I = 5 TO 1\n30 S = S + 1\n40 NEXT I", "S", 0));
        rows.add(r("FOR index after loop", "10 FOR I = 1 TO 3\n20 NEXT I", "I", 4));
        rows.add(r("nested FOR", "10 FOR I = 1 TO 3\n20 FOR J = 1 TO 2\n30 S = S + I * J\n40 NEXT J\n50 NEXT I", "S", 18));
        rows.add(r("GOSUB / RETURN", "10 GOSUB 100\n20 A = A + 1\n30 END\n100 A = 10\n110 RETURN", "A", 11));
        rows.add(r("nested GOSUB", "10 GOSUB 100\n20 END\n100 GOSUB 200\n110 A = A * 2\n120 RETURN\n200 A = 3\n210 RETURN", "A", 6));
        rows.add(r("ON n GOSUB", "10 ON 3 GOSUB 100, 200, 300\n20 END\n100 A = 1\n110 RETURN\n200 A = 2\n210 RETURN\n300 A = 3\n310 RETURN", "A", 3));
        rows.add(r("ON n out of range falls through", "10 A = 9\n20 ON 4 GOTO 100, 200\n30 END\n100 A = 1\n200 A = 2", "A", 9));
        rows.add(r("DIM array", "10 DIM T(3)\n20 FOR I = 1 TO 3\n30 T(I) = I * I\n40 NEXT I\n50 A = T(1) + T(2) + T(3)", "A", 14));
        rows.add(r("multi-statement line", "10 A = 1 : B = A + 1 : C = B * 3", "C", 6));
        rows.add(r("unassigned variable reads 0", "10 A = Q + 1", "A", 1));
        rows.add(r("case-insensitive names", "10 abc = 4\n20 A = ABC * 2", "A", 8));
        rows.add(r("END stops the scan", "10 A = 1\n20 END\n30 A = 2", "A", 1));
        // Functions
        rows.add(r("ABS", "10 A = ABS(-3.5)", "A", 3.5));
        rows.add(r("SGN", "10 A = SGN(-2)", "A", -1));
        rows.add(r("INT floors", "10 A = INT(-1.2)", "A", -2));
        rows.add(r("FIX truncates", "10 A = FIX(-1.7)", "A", -1));
        rows.add(r("FRAC", "10 A = FRAC(2.75)", "A", 0.75));
        rows.add(r("ROUND half away from zero", "10 A = ROUND(2.5)", "A", 3));
        rows.add(r("ROUND negative half", "10 A = ROUND(-2.5)", "A", -3));
        rows.add(r("ROUND below half", "10 A = ROUND(2.49)", "A", 2));
        rows.add(r("ROUND digits half", "10 A = ROUND(0.125, 2)", "A", 0.13, 1e-12));
        rows.add(r("ROUND digits", "10 A = ROUND(1.2345, 2)", "A", 1.23, 1e-12));
        rows.add(r("SQR", "10 A = SQR(16)", "A", 4));
        rows.add(r("EXP/LN", "10 A = LN(EXP(2))", "A", 2, 1e-12));
        rows.add(r("LOG base 10", "10 A = LOG(1000)", "A", 3, 1e-12));
        rows.add(r("SIN/COS", "10 A = SIN(0) + COS(0)", "A", 1));
        rows.add(r("ATN", "10 A = ATN(1) * 4", "A", Math.PI, 1e-12));
        rows.add(r("DEG/RAD", "10 A = DEG(RAD(90))", "A", 90, 1e-9));
        rows.add(r("MIN 3 args", "10 A = MIN(3, 1, 2)", "A", 1));
        rows.add(r("MAX 3 args", "10 A = MAX(3, 1, 2)", "A", 3));
        rows.add(r("LIMIT high", "10 A = LIMIT(5, 0, 3)", "A", 3));
        rows.add(r("LIMIT low", "10 A = LIMIT(-1, 0, 3)", "A", 0));
        rows.add(r("SCALE", "10 A = SCALE(5, 0, 10, 0, 100)", "A", 50));
        rows.add(r("SCALE does not clamp", "10 A = SCALE(15, 0, 10, 0, 100)", "A", 150));
        rows.add(r("SCALE reverse", "10 A = SCALE(74, 68, 76, 100, 0)", "A", 25));
        rows.add(r("TABLE interpolates", "10 A = TABLE(5, 0, 0, 10, 100)", "A", 50));
        rows.add(r("TABLE flat above", "10 A = TABLE(20, 0, 0, 10, 100)", "A", 100));
        rows.add(r("TABLE flat below", "10 A = TABLE(-5, 0, 0, 10, 100)", "A", 0));
        rows.add(r("HOURS/MINUTES", "10 A = HOURS + MINUTES + SECONDS", "A", 3661));
        rows.add(r("time literal HH:MM", "10 A = 8:30", "A", 30600));
        rows.add(r("time literal HH:MM:SS", "10 A = 00:07:00", "A", 420));
        rows.add(r("DEWPOINT 20C 50%", "10 A = DEWPOINT(20, 50)", "A", 9.26, 0.05));
        rows.add(r("ENTHALPYSI 20C 50%", "10 A = ENTHALPYSI(20, 50)", "A", 38.5, 0.3));
        rows.add(r("PID proportional", "10 A = PID(70, 72, 2, 0, 0)", "A", 4));
        rows.add(r("PID clamps", "10 A = PID(60, 72, 20, 0, 0, 0, 100)", "A", 100));
        rows.add(r("PID no derivative kick on first scan", "10 A = PID(70, 72, 2, 0, 1)", "A", 4));
        rows.add(r("RND range", "10 A = RND(10) >= 0 AND RND(10) <= 10", "A", 1));
        rows.add(r("FIRST on first scan", "10 A = FIRST", "A", 1));
        rows.add(r("SCANS on first scan", "10 A = SCANS", "A", 1));
        return rows;
    }

    @TestFactory
    List<DynamicTest> matrix() {
        List<DynamicTest> tests = new ArrayList<DynamicTest>();
        for (final Row row : rows()) {
            tests.add(dynamicTest(row.name, () -> {
                Compilation c = ControlBasic.compile(row.program);
                assertTrue(c.ok(), () -> row.name + ": " + c.diagnostics());
                RuntimeState rt = new RuntimeState();
                rt.time = clock();
                Scan s = ControlBasic.run(c, rt, 10000);
                assertNull(s.error, () -> row.name + ": " + (s.error == null ? "" : s.error.format()));
                assertEquals(row.expected, rt.getVar(row.var), row.tol, row.name);
            }));
        }
        return tests;
    }

    private static ControlBasicTest.FakeClock clock() {
        ControlBasicTest.FakeClock c = new ControlBasicTest.FakeClock();
        c.zoned = ZonedDateTime.of(2026, 11, 22, 12, 0, 0, 0, ZoneId.of("America/New_York"));
        return c;
    }

    // ---- errors -------------------------------------------------------

    private static String compileError(String program) {
        Compilation c = ControlBasic.compile(program);
        assertFalse(c.ok(), "expected a compile error for: " + program);
        return c.diagnostics().get(0).format();
    }

    private static Scan scanOnce(String program, RuntimeState rt) {
        Compilation c = ControlBasic.compile(program);
        assertTrue(c.ok(), c.diagnostics().toString());
        return ControlBasic.run(c, rt, 1000);
    }

    @Test
    void syntaxErrorReportsLine() {
        String e = compileError("10 A = 1\n20 B = (1 + \n30 C = 2");
        assertTrue(e.contains("2"), e);
    }

    @Test
    void gotoMissingLineIsCompileError() {
        String e = compileError("10 GOTO 99");
        assertTrue(e.toLowerCase().contains("99"), e);
    }

    @Test
    void unknownFunctionIsCompileError() {
        String e = compileError("10 A = FOO(1)");
        assertTrue(e.contains("FOO"), e);
    }

    @Test
    void arrayAssignedWithoutDimStillCompiles() {
        // Arrays can be created by assignment, so B(1) is not an unknown function.
        String e = compileError("10 B(1) = 5\n20 A = B(1)\n30 C = FOO(2)");
        assertTrue(e.contains("FOO") && !e.contains(" B"), e);
    }

    @Test
    void nextWithoutForIsAnError() {
        Compilation c = ControlBasic.compile("10 NEXT I");
        if (c.ok()) {
            Scan s = ControlBasic.run(c, new RuntimeState(), 100);
            assertNotNull(s.error);
        }
    }

    @Test
    void unsupportedWaitIsAClearError() {
        String e = compileError("10 WAIT 5");
        assertTrue(e.toUpperCase().contains("WAIT"), e);
    }

    @Test
    void divideByZeroIsRuntimeErrorAndKeepsOutputs() {
        ControlBasicTest.MapPoints pts = new ControlBasicTest.MapPoints();
        pts.values.put("AO1", 42.0);
        RuntimeState rt = new RuntimeState();
        rt.points = pts;
        Scan s = scanOnce("10 AO1 = 5\n20 A = 1 / 0", rt);
        assertNotNull(s.error);
        assertTrue(s.error.format().contains("Division by zero"), s.error.format());
        assertTrue(s.error.format().contains("2"), s.error.format());
        assertEquals(42.0, pts.values.get("AO1"), 0, "a failed scan must not write outputs");
    }

    @Test
    void integerDivideAndModByZero() {
        assertNotNull(scanOnce("10 A = 1 \\ 0", new RuntimeState()).error);
        assertNotNull(scanOnce("10 A = 1 MOD 0", new RuntimeState()).error);
    }

    @Test
    void runawayLoopIsCutOffAndNextScanRuns() {
        RuntimeState rt = new RuntimeState();
        Compilation c = ControlBasic.compile("10 N = N + 1\n20 GOTO 10");
        Scan s = ControlBasic.run(c, rt, 500);
        assertNotNull(s.error);
        assertTrue(s.error.format().contains("Exceeded 500"), s.error.format());
        Scan s2 = ControlBasic.run(c, rt, 500);
        assertNotNull(s2.error);
        assertEquals(2, rt.scanCount());
    }

    @Test
    void returnWithoutGosub() {
        assertNotNull(scanOnce("10 RETURN", new RuntimeState()).error);
    }

    @Test
    void arrayIndexOutOfRange() {
        assertNotNull(scanOnce("10 DIM T(2)\n20 T(3) = 1", new RuntimeState()).error);
    }

    @Test
    void mathDomainErrorsDoNotReachOutputsAsNaN() {
        for (String expr : new String[] {"SQR(-1)", "LN(-1)", "LOG(0)", "ARCSIN(2)", "0 ^ -1", "(-8) ^ 0.5"}) {
            ControlBasicTest.MapPoints pts = new ControlBasicTest.MapPoints();
            pts.values.put("AO1", 7.0);
            RuntimeState rt = new RuntimeState();
            rt.points = pts;
            Scan s = scanOnce("10 AO1 = " + expr, rt);
            double out = pts.values.get("AO1");
            assertFalse(Double.isNaN(out) || Double.isInfinite(out),
                    expr + " wrote " + out + " to an output (error=" + (s.error == null ? "none" : s.error.format()) + ")");
        }
    }

    @Test
    void onErrorHandlerRecoversEveryScan() {
        String program = "10 ONERROR GOTO 100\n20 A = 1 / D\n30 END\n100 E = E + 1\n110 A = 1 / D\n120 END";
        RuntimeState rt = new RuntimeState();
        Compilation c = ControlBasic.compile(program);
        assertTrue(c.ok(), c.diagnostics().toString());
        // Scan 1: the handler itself fails. Scan 2 must still enter the handler.
        ControlBasic.run(c, rt, 1000);
        double afterFirst = rt.getVar("E");
        ControlBasic.run(c, rt, 1000);
        assertEquals(afterFirst + 1, rt.getVar("E"), 0, "ONERROR handler not entered on the next scan");
    }

    @Test
    void onErrorHandlerCatchesDivideByZero() {
        RuntimeState rt = new RuntimeState();
        Scan s = scanOnce("10 ONERROR GOTO 100\n20 A = 1 / 0\n30 END\n100 B = 5", rt);
        assertNull(s.error);
        assertEquals(5, rt.getVar("B"), 0);
    }

    // ---- multi-scan behavior ------------------------------------------

    @Test
    void timeonCountsWallClockSeconds() {
        ControlBasicTest.FakeClock clock = new ControlBasicTest.FakeClock();
        Compilation c = ControlBasic.compile("10 IF FIRST THEN X = 1\n20 T = TIMEON(X)\n30 U = TIMEOFF(X)");
        RuntimeState rt = new RuntimeState();
        rt.time = clock;
        ControlBasic.run(c, rt, 100);
        clock.plusSeconds(5);
        ControlBasic.run(c, rt, 100);
        assertEquals(5, rt.getVar("T"), 0);
        assertEquals(0, rt.getVar("U"), 0);
    }

    @Test
    void intervalFiresOnFirstCallThenAfterPeriod() {
        ControlBasicTest.FakeClock clock = new ControlBasicTest.FakeClock();
        Compilation c = ControlBasic.compile("10 IF INTERVAL(10) THEN N = N + 1");
        RuntimeState rt = new RuntimeState();
        rt.time = clock;
        for (int i = 0; i < 25; i++) {
            ControlBasic.run(c, rt, 100);
            clock.plusSeconds(1);
        }
        // t = 0, 10, 20
        assertEquals(3, rt.getVar("N"), 0);
    }

    @Test
    void pidDerivativeStartsOnSecondScan() {
        ControlBasicTest.FakeClock clock = clock();
        ControlBasicTest.MapPoints pts = new ControlBasicTest.MapPoints();
        Compilation c = ControlBasic.compile("10 AO1_Out = PID(AI1_Pv, 72, 0, 0, 1)");
        assertTrue(c.ok(), c.diagnostics().toString());
        RuntimeState rt = new RuntimeState();
        rt.points = pts;
        rt.time = clock;
        pts.values.put("AI1_PV", 70.0);
        ControlBasic.run(c, rt, 100);
        assertEquals(0, pts.values.get("AO1_OUT"), EPS, "first scan: no derivative kick");
        clock.plusSeconds(1);
        pts.values.put("AI1_PV", 71.0);
        ControlBasic.run(c, rt, 100);
        // error 2 -> 1 over 1 s: derivative -1
        assertEquals(-1, pts.values.get("AO1_OUT"), EPS);
    }

    @Test
    void sensorOnAndIfPlusFireOnRisingEdgeOnly() {
        ControlBasicTest.MapPoints pts = new ControlBasicTest.MapPoints();
        Compilation c = ControlBasic.compile(
                "10 IF SENSORON(BI1_Occ) THEN AV1_Rise = AV1_Rise + 1\n20 IF+ BI1_Occ THEN AV2_Plus = AV2_Plus + 1");
        assertTrue(c.ok(), c.diagnostics().toString());
        RuntimeState rt = new RuntimeState();
        rt.points = pts;
        double[] seq = {0, 1, 1, 0, 1, 1};
        for (double v : seq) {
            pts.values.put("BI1_OCC", v);
            pts.values.put("BI1_Occ", v);
            ControlBasic.run(c, rt, 100);
        }
        assertEquals(2, rt.getVar("AV1_Rise"), 0);
        assertEquals(2, rt.getVar("AV2_Plus"), 0);
    }

    @Test
    void variablesPersistAcrossScansAndFirstClears() {
        Compilation c = ControlBasic.compile("10 IF FIRST THEN N = 100\n20 N = N + 1\n30 F = FIRST");
        RuntimeState rt = new RuntimeState();
        for (int i = 0; i < 3; i++) {
            ControlBasic.run(c, rt, 100);
        }
        assertEquals(103, rt.getVar("N"), 0);
        assertEquals(0, rt.getVar("F"), 0);
        assertEquals(3, rt.scanCount());
        rt.reset();
        ControlBasic.run(c, rt, 100);
        assertEquals(101, rt.getVar("N"), 0);
    }

    @Test
    void haltStopsLaterScans() {
        Compilation c = ControlBasic.compile("10 N = N + 1\n20 HALT");
        RuntimeState rt = new RuntimeState();
        ControlBasic.run(c, rt, 100);
        Scan s = ControlBasic.run(c, rt, 100);
        assertTrue(s.halted);
        assertEquals(1, rt.getVar("N"), 0);
    }

    @Test
    void scanRuntimeStaysSmallForTypicalProgram() {
        Compilation c = ControlBasic.compile(Samples.DAMPER);
        RuntimeState rt = new RuntimeState();
        rt.points = new ControlBasicTest.MapPoints();
        long t0 = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            ControlBasic.run(c, rt, 10000);
        }
        long micros = (System.nanoTime() - t0) / 1000 / 1000;
        assertTrue(micros < 2000, "average scan " + micros + " us");
    }
}

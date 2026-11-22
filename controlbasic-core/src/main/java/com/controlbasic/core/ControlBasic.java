package com.controlbasic.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Public entry point for the Control Basic interpreter.
 * Niagara code should call only this type plus {@link Compilation},
 * {@link RuntimeState}, {@link SlotSpec}, and {@link Highlight}.
 */
public final class ControlBasic {
    private ControlBasic() {
    }

    public static Compilation compile(String source) {
        try {
            List<Token> tokens = Lexer.tokenize(source == null ? "" : source);
            for (int i = 0; i < tokens.size(); i++) {
                Token t = tokens.get(i);
                if (t.type == TokenType.ERROR) {
                    return failed(new Diagnostic(t.line, t.column, t.text));
                }
            }
            Parser parser = new Parser(tokens);
            List<Stmt> statements = parser.parseProgram();
            SlotAnalyzer slots = new SlotAnalyzer();
            List<SlotSpec> specs = slots.analyze(statements);
            if (!slots.errors().isEmpty()) {
                return failed(slots.errors().get(0));
            }
            Compiler compiler = new Compiler();
            Instr[] code = compiler.compile(statements);
            if (!compiler.errors().isEmpty()) {
                return failed(compiler.errors().get(0));
            }
            return new Compilation(code, specs, new ArrayList<Diagnostic>());
        } catch (Parser.ParseException ex) {
            return failed(new Diagnostic(ex.line, ex.column, ex.getMessage()));
        }
    }

    public static Scan run(Compilation program, RuntimeState state, int maxInstructions) {
        if (program == null || !program.ok()) {
            Diagnostic d = program == null || program.diagnostics().isEmpty()
                    ? new Diagnostic(0, 1, "Program has errors")
                    : program.diagnostics().get(0);
            return new Scan(d, false, 0);
        }
        if (state == null) {
            state = new RuntimeState();
        }
        if (state.halted) {
            return new Scan(null, true, 0);
        }
        state.scans++;
        long now = state.time.nowMillis();
        if (state.lastScanMillis >= 0 && now > state.lastScanMillis) {
            state.scanRate = 1000.0 / (now - state.lastScanMillis);
        } else {
            state.scanRate = state.scanIntervalSeconds > 0 ? 1.0 / state.scanIntervalSeconds : 1.0;
        }
        state.lastScanMillis = now;
        state.statuses.clear();
        if (state.points != null) {
            List<SlotSpec> slots = program.slots();
            for (int i = 0; i < slots.size(); i++) {
                SlotSpec spec = slots.get(i);
                double value = state.points.get(spec.canonical);
                if (spec.direction == SlotDirection.INPUT) {
                    int bits = state.points.status(spec.canonical);
                    state.statuses.put(spec.canonical, Integer.valueOf(bits));
                    // KMC: a value that is not available reads as NAN (ISNAN);
                    // a down (offline) point keeps its last value (Connect Rev N, ISNAN).
                    if ((bits & PointStatus.NULL) != 0) {
                        value = Double.NaN;
                    }
                }
                state.vars.put(spec.canonical, Double.valueOf(value));
            }
        }
        Scan result = Runner.execute(program, state, maxInstructions);
        if (state.points != null) {
            writeOutputs(program, state, result);
        }
        return result;
    }

    /**
     * After a clean scan every output and variable slot gets its value and the
     * status ok, or null when the value is NAN. A runtime error leaves the
     * values alone and marks them fault; a HALT writes the values and marks
     * them fault, since they will not change again until a reset.
     */
    private static void writeOutputs(Compilation program, RuntimeState state, Scan result) {
        List<SlotSpec> slots = program.slots();
        for (int i = 0; i < slots.size(); i++) {
            SlotSpec spec = slots.get(i);
            if (spec.direction == SlotDirection.INPUT) {
                continue;
            }
            double value = state.getVar(spec.canonical);
            if (result.error == null) {
                state.points.set(spec.canonical, value);
            }
            state.points.setStatus(spec.canonical, outputStatus(value, result));
        }
    }

    /** Status bits an output carries after {@code result}. */
    public static int outputStatus(double value, Scan result) {
        int bits = Double.isNaN(value) ? PointStatus.NULL : PointStatus.OK;
        if (result != null && (result.error != null || result.halted)) {
            bits |= PointStatus.FAULT;
        }
        return bits;
    }

    private static Compilation failed(Diagnostic diagnostic) {
        List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
        diagnostics.add(diagnostic);
        return new Compilation(new Instr[0], new ArrayList<SlotSpec>(), diagnostics);
    }
}

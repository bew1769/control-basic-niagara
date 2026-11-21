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
        if (state.points != null) {
            List<SlotSpec> slots = program.slots();
            for (int i = 0; i < slots.size(); i++) {
                SlotSpec spec = slots.get(i);
                state.vars.put(spec.canonical, Double.valueOf(state.points.get(spec.canonical)));
            }
        }
        Scan result = Runner.execute(program, state, maxInstructions);
        if (result.error == null && state.points != null) {
            List<SlotSpec> slots = program.slots();
            for (int i = 0; i < slots.size(); i++) {
                SlotSpec spec = slots.get(i);
                if (spec.direction != SlotDirection.INPUT) {
                    state.points.set(spec.canonical, state.getVar(spec.canonical));
                }
            }
        }
        return result;
    }

    private static Compilation failed(Diagnostic diagnostic) {
        List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
        diagnostics.add(diagnostic);
        return new Compilation(new Instr[0], new ArrayList<SlotSpec>(), diagnostics);
    }
}

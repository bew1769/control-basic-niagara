package com.controlbasic.core;

import java.util.ArrayDeque;

/** Executes one scan of a compiled program. */
final class Runner {
    private Runner() {
    }

    static Scan execute(Compilation program, RuntimeState state, int maxInstructions) {
        Instr[] code = program.code();
        Evaluator ev = new Evaluator(state);
        int ip = 0;
        int steps = 0;
        int limit = maxInstructions <= 0 ? 10000 : maxInstructions;
        try {
            while (ip >= 0 && ip < code.length) {
                if (++steps > limit) {
                    throw new Evaluator.Fail(code[Math.min(ip, code.length - 1)].line, 1,
                            "Exceeded " + limit + " instructions in one scan");
                }
                Instr in = code[ip];
                try {
                    ip = step(in, ip, code, ev, state);
                } catch (Evaluator.Fail fail) {
                    if (state.onErrorIp >= 0 && !state.inHandler) {
                        state.inHandler = true;
                        ip = state.onErrorIp;
                        continue;
                    }
                    throw fail;
                }
            }
            state.inHandler = false;
            ev.commitLevels();
            return new Scan(null, state.halted, steps);
        } catch (Evaluator.Fail fail) {
            return new Scan(new Diagnostic(fail.line, fail.column, fail.getMessage()), false, steps);
        }
    }

    private static int step(Instr in, int ip, Instr[] code, Evaluator ev, RuntimeState state) {
        switch (in.op) {
            case Instr.ASSIGN:
                ev.writeVar(in.name, ev.eval(in.expr), in.expr);
                return ip + 1;
            case Instr.ARR:
                ev.writeIndex(in.name, ev.eval(in.expr), ev.eval(in.expr2), in.expr2);
                return ip + 1;
            case Instr.IF:
                return branch(in, ip, ev, state);
            case Instr.GOTO:
                return in.a;
            case Instr.GOSUB:
                if (ev.gosub().size() >= 32) {
                    throw new Evaluator.Fail(in.line, in.column, "GOSUB nested too deeply");
                }
                ev.gosub().push(Integer.valueOf(ip + 1));
                return in.a;
            case Instr.RETURN:
                if (ev.gosub().isEmpty()) {
                    throw new Evaluator.Fail(in.line, in.column, "RETURN without GOSUB");
                }
                return ev.gosub().pop().intValue();
            case Instr.FOR:
                return enterFor(in, ip, ev);
            case Instr.NEXT:
                return nextFor(in, ev);
            case Instr.ON:
                return onBranch(in, ip, ev);
            case Instr.ONERR:
                state.onErrorIp = in.a;
                state.inHandler = false;
                return ip + 1;
            case Instr.SET:
                ev.writeVar(in.name, in.a == 1 ? 1 : 0, new Expr.Num(0, in.line, in.column));
                return ip + 1;
            case Instr.END:
                return code.length;
            case Instr.HALT:
                state.halted = true;
                return code.length;
            case Instr.PRINT:
                print(in, ev, state);
                return ip + 1;
            case Instr.DIM:
                ev.dim(in.name, ev.eval(in.expr), in.expr);
                return ip + 1;
            default:
                throw new Evaluator.Fail(in.line, in.column, "Bad instruction");
        }
    }

    private static int branch(Instr in, int ip, Evaluator ev, RuntimeState state) {
        boolean raw = Evaluator.truth(ev.eval(in.expr));
        boolean[] prev = state.edges.get(Integer.valueOf(ip));
        if (prev == null) {
            prev = new boolean[] {false};
            state.edges.put(Integer.valueOf(ip), prev);
        }
        boolean cond = raw;
        if (in.b == 1) {
            cond = raw && !prev[0];
        } else if (in.b == 2) {
            cond = !raw && prev[0];
        }
        prev[0] = raw;
        return cond ? in.a : in.c;
    }

    private static int enterFor(Instr in, int ip, Evaluator ev) {
        double start = ev.eval(in.expr);
        double end = ev.eval(in.expr2);
        double step = in.expr3 == null ? 1 : ev.eval(in.expr3);
        if (step == 0) {
            throw new Evaluator.Fail(in.line, in.column, "FOR step is zero");
        }
        ev.writeVar(in.name, start, in.expr);
        RuntimeState.ForFrame frame = new RuntimeState.ForFrame(
                Functions.canon(in.name), end, step, in.a, ip + 1);
        if (finished(start, end, step)) {
            return in.a;
        }
        ev.fors().push(frame);
        return ip + 1;
    }

    private static int nextFor(Instr in, Evaluator ev) {
        if (ev.fors().isEmpty()) {
            throw new Evaluator.Fail(in.line, in.column, "NEXT without FOR");
        }
        RuntimeState.ForFrame frame = ev.fors().pop();
        if (in.name != null && !frame.name.equals(Functions.canon(in.name))) {
            throw new Evaluator.Fail(in.line, in.column, "NEXT " + in.name + " does not match FOR");
        }
        double value = ev.eval(new Expr.Var(frame.name, in.line, in.column)) + frame.step;
        if (finished(value, frame.end, frame.step)) {
            ev.writeVar(frame.name, value, new Expr.Num(0, in.line, in.column));
            return frame.afterIp;
        }
        ev.writeVar(frame.name, value, new Expr.Num(0, in.line, in.column));
        ev.fors().push(frame);
        return frame.bodyIp;
    }

    private static boolean finished(double value, double end, double step) {
        if (step >= 0) {
            return value > end + 1e-9;
        }
        return value < end - 1e-9;
    }

    private static int onBranch(Instr in, int ip, Evaluator ev) {
        double raw = ev.eval(in.expr);
        int index = (int) raw;
        if (index < 1 || in.jumps == null || index > in.jumps.length) {
            return ip + 1;
        }
        int target = in.jumps[index - 1];
        if (in.b == 1) {
            if (ev.gosub().size() >= 32) {
                throw new Evaluator.Fail(in.line, in.column, "GOSUB nested too deeply");
            }
            ev.gosub().push(Integer.valueOf(ip + 1));
        }
        return target;
    }

    private static void print(Instr in, Evaluator ev, RuntimeState state) {
        StringBuilder line = new StringBuilder();
        if (in.items != null) {
            for (int i = 0; i < in.items.size(); i++) {
                if (i > 0) {
                    line.append('\t');
                }
                line.append(ev.evalPrint(in.items.get(i)));
            }
        }
        line.append('\n');
        state.appendConsole(line.toString());
    }
}

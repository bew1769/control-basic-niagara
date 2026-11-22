package com.controlbasic.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lowers statements to a flat instruction list and resolves GOTO/GOSUB targets. */
final class Compiler {
    private final List<Instr> code = new ArrayList<Instr>();
    private final Map<Integer, Integer> lines = new HashMap<Integer, Integer>();
    private final Map<String, Integer> labels = new HashMap<String, Integer>();
    private final List<int[]> forStack = new ArrayList<int[]>();
    private final List<Diagnostic> errors = new ArrayList<Diagnostic>();

    List<Diagnostic> errors() {
        return errors;
    }

    Instr[] compile(List<Stmt> statements) {
        try {
            emitAll(statements);
        } catch (Stop stop) {
            return new Instr[0];
        }
        if (!forStack.isEmpty()) {
            errors.add(new Diagnostic(0, 1, "FOR without NEXT"));
        }
        resolve();
        if (!errors.isEmpty()) {
            return new Instr[0];
        }
        return code.toArray(new Instr[code.size()]);
    }

    private void emitAll(List<Stmt> statements) {
        for (int i = 0; i < statements.size(); i++) {
            emit(statements.get(i));
        }
    }

    private void emit(Stmt s) {
        if (s instanceof Stmt.Marker) {
            Stmt.Marker m = (Stmt.Marker) s;
            if (m.basicLine >= 0) {
                if (lines.containsKey(Integer.valueOf(m.basicLine))) {
                    fail(s, "Duplicate line number " + m.basicLine);
                }
                lines.put(Integer.valueOf(m.basicLine), Integer.valueOf(code.size()));
            }
            if (m.label != null) {
                String key = Functions.canon(m.label);
                if (labels.containsKey(key)) {
                    fail(s, "Duplicate label " + m.label);
                }
                labels.put(key, Integer.valueOf(code.size()));
            }
            return;
        }
        if (s instanceof Stmt.Assign) {
            Stmt.Assign a = (Stmt.Assign) s;
            checkAssign(a.name, s);
            checkExpr(a.expr);
            Instr in = instr(Instr.ASSIGN, s);
            in.name = a.name;
            in.expr = a.expr;
            code.add(in);
            return;
        }
        if (s instanceof Stmt.ArrAssign) {
            Stmt.ArrAssign a = (Stmt.ArrAssign) s;
            checkExpr(a.index);
            checkExpr(a.expr);
            Instr in = instr(Instr.ARR, s);
            in.name = a.name;
            in.expr = a.index;
            in.expr2 = a.expr;
            code.add(in);
            return;
        }
        if (s instanceof Stmt.If) {
            emitIf((Stmt.If) s);
            return;
        }
        if (s instanceof Stmt.Goto) {
            Instr in = instr(Instr.GOTO, s);
            in.name = ((Stmt.Goto) s).target;
            code.add(in);
            return;
        }
        if (s instanceof Stmt.Gosub) {
            Instr in = instr(Instr.GOSUB, s);
            in.name = ((Stmt.Gosub) s).target;
            code.add(in);
            return;
        }
        if (s instanceof Stmt.Return) {
            code.add(instr(Instr.RETURN, s));
            return;
        }
        if (s instanceof Stmt.For) {
            Stmt.For f = (Stmt.For) s;
            checkExpr(f.start);
            checkExpr(f.end);
            if (f.step != null) {
                checkExpr(f.step);
            }
            Instr in = instr(Instr.FOR, s);
            in.name = f.name;
            in.expr = f.start;
            in.expr2 = f.end;
            in.expr3 = f.step;
            in.a = -1;
            code.add(in);
            forStack.add(new int[] {code.size() - 1});
            return;
        }
        if (s instanceof Stmt.Next) {
            if (forStack.isEmpty()) {
                fail(s, "NEXT without FOR");
            }
            int forIp = forStack.remove(forStack.size() - 1)[0];
            Instr next = instr(Instr.NEXT, s);
            next.name = ((Stmt.Next) s).name;
            next.a = forIp;
            code.add(next);
            code.get(forIp).a = code.size();
            return;
        }
        if (s instanceof Stmt.OnBranch) {
            Stmt.OnBranch o = (Stmt.OnBranch) s;
            checkExpr(o.expr);
            Instr in = instr(Instr.ON, s);
            in.expr = o.expr;
            in.b = o.gosub ? 1 : 0;
            in.texts = o.targets.toArray(new String[o.targets.size()]);
            code.add(in);
            return;
        }
        if (s instanceof Stmt.OnError) {
            Instr in = instr(Instr.ONERR, s);
            in.name = ((Stmt.OnError) s).target;
            code.add(in);
            return;
        }
        if (s instanceof Stmt.SetPoint) {
            Stmt.SetPoint p = (Stmt.SetPoint) s;
            Instr in = instr(Instr.SET, s);
            in.name = p.name;
            in.a = p.on ? 1 : 0;
            code.add(in);
            return;
        }
        if (s instanceof Stmt.EndScan) {
            code.add(instr(Instr.END, s));
            return;
        }
        if (s instanceof Stmt.Halt) {
            Instr in = instr(Instr.HALT, s);
            in.text = ((Stmt.Halt) s).message;
            code.add(in);
            return;
        }
        if (s instanceof Stmt.Print) {
            Stmt.Print p = (Stmt.Print) s;
            for (Expr e : p.items) {
                checkExpr(e);
            }
            Instr in = instr(Instr.PRINT, s);
            in.items = p.items;
            code.add(in);
            return;
        }
        if (s instanceof Stmt.Dim) {
            Stmt.Dim d = (Stmt.Dim) s;
            checkExpr(d.size);
            Instr in = instr(Instr.DIM, s);
            in.name = d.name;
            in.expr = d.size;
            code.add(in);
            return;
        }
        if (s instanceof Stmt.Declare) {
            return;
        }
        fail(s, "Unsupported statement");
    }

    private void emitIf(Stmt.If s) {
        checkExpr(s.cond);
        Instr branch = instr(Instr.IF, s);
        branch.expr = s.cond;
        branch.b = s.edge == Stmt.Edge.RISING ? 1 : s.edge == Stmt.Edge.FALLING ? 2 : 0;
        code.add(branch);
        int ifIp = code.size() - 1;
        emitAll(s.thenBody);
        Instr jump = instr(Instr.GOTO, s);
        jump.a = -1;
        jump.name = null;
        code.add(jump);
        int elseIp = code.size();
        emitAll(s.elseBody);
        int endIp = code.size();
        code.get(ifIp).a = ifIp + 1;
        code.get(ifIp).c = elseIp;
        jump.a = endIp;
    }

    private void resolve() {
        for (int i = 0; i < code.size(); i++) {
            Instr in = code.get(i);
            in.recovers = in.op == Instr.ONERR || readsErrorLevel(in.expr) || readsErrorLevel(in.expr2)
                    || readsErrorLevel(in.expr3);
            if (in.items != null) {
                for (Expr item : in.items) {
                    in.recovers |= readsErrorLevel(item);
                }
            }
            if (in.op == Instr.GOTO && in.name != null) {
                in.a = resolveTarget(in.name, in);
            } else if (in.op == Instr.GOSUB) {
                in.a = resolveTarget(in.name, in);
            } else if (in.op == Instr.ONERR) {
                in.a = in.name == null ? -1 : resolveTarget(in.name, in);
            } else if (in.op == Instr.ON && in.texts != null) {
                in.jumps = new int[in.texts.length];
                for (int t = 0; t < in.texts.length; t++) {
                    in.jumps[t] = resolveTarget(in.texts[t], in);
                }
            }
        }
    }

    private int resolveTarget(String target, Instr at) {
        if (target == null) {
            failAt(at, "Missing target");
            return 0;
        }
        boolean numeric = true;
        for (int i = 0; i < target.length(); i++) {
            if (!Character.isDigit(target.charAt(i))) {
                numeric = false;
                break;
            }
        }
        if (numeric && target.length() > 0) {
            Integer ip = lines.get(Integer.valueOf(Integer.parseInt(target)));
            if (ip == null) {
                failAt(at, "Unknown line " + target);
                return 0;
            }
            return ip.intValue();
        }
        Integer ip = labels.get(Functions.canon(target));
        if (ip == null) {
            failAt(at, "Unknown label " + target);
            return 0;
        }
        return ip.intValue();
    }

    private static boolean readsErrorLevel(Expr e) {
        if (e == null) {
            return false;
        }
        if (e instanceof Expr.Var) {
            return "ERRORLEVEL".equals(Functions.canon(((Expr.Var) e).name));
        }
        if (e instanceof Expr.Call) {
            Expr.Call c = (Expr.Call) e;
            if ("ERRORLEVEL".equals(Functions.canon(c.name))) {
                return true;
            }
            for (Expr arg : c.args) {
                if (readsErrorLevel(arg)) {
                    return true;
                }
            }
            return false;
        }
        if (e instanceof Expr.Unary) {
            return readsErrorLevel(((Expr.Unary) e).expr);
        }
        if (e instanceof Expr.Binary) {
            return readsErrorLevel(((Expr.Binary) e).left) || readsErrorLevel(((Expr.Binary) e).right);
        }
        if (e instanceof Expr.Index) {
            return readsErrorLevel(((Expr.Index) e).index);
        }
        return false;
    }

    private void checkAssign(String name, Stmt at) {
        String key = Functions.canon(name);
        if (Functions.isConstant(key) || Functions.isNoArg(key)) {
            fail(at, "Cannot assign " + name);
        }
    }

    private void checkExpr(Expr e) {
        if (e == null) {
            return;
        }
        if (e instanceof Expr.Call) {
            Expr.Call c = (Expr.Call) e;
            String err = Functions.arityError(c.name, c.args.size());
            if (err != null) {
                errors.add(new Diagnostic(c.line, c.column, err));
                throw new Stop();
            }
            for (Expr arg : c.args) {
                checkExpr(arg);
            }
            return;
        }
        if (e instanceof Expr.Unary) {
            checkExpr(((Expr.Unary) e).expr);
            return;
        }
        if (e instanceof Expr.Binary) {
            checkExpr(((Expr.Binary) e).left);
            checkExpr(((Expr.Binary) e).right);
            return;
        }
        if (e instanceof Expr.Index) {
            checkExpr(((Expr.Index) e).index);
        }
    }

    private Instr instr(int op, Stmt s) {
        Instr in = new Instr();
        in.op = op;
        in.line = s.line;
        in.column = s.column;
        return in;
    }

    private void fail(Stmt s, String message) {
        errors.add(new Diagnostic(s.line, s.column, message));
        throw new Stop();
    }

    private void failAt(Instr in, String message) {
        errors.add(new Diagnostic(in.line, in.column, message));
    }

    private static final class Stop extends RuntimeException {
    }
}

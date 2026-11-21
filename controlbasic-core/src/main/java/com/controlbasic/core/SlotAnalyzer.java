package com.controlbasic.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Decides which identifiers become wire-sheet slots. */
final class SlotAnalyzer {
    private static final String[] PREFIXES = {
            "BINPUT", "BOUTPUT", "BVAR", "BOUT", "BIN", "OUT", "VAR",
            "AI", "AO", "AV", "BI", "BO", "IN", "BV"
    };

    private final Map<String, Use> uses = new LinkedHashMap<String, Use>();
    private final List<Diagnostic> errors = new ArrayList<Diagnostic>();
    private int order;

    List<Diagnostic> errors() {
        return errors;
    }

    List<SlotSpec> analyze(List<Stmt> statements) {
        walk(statements);
        List<SlotSpec> specs = new ArrayList<SlotSpec>();
        for (Use use : uses.values()) {
            if (use.array) {
                continue;
            }
            if (use.loop && !use.assigned && use.declared == null && prefix(use.canonical) == null) {
                continue;
            }
            if (Functions.isReserved(use.canonical)) {
                if (use.declared != null || use.assigned) {
                    errors.add(new Diagnostic(use.line, use.column,
                            use.spelling + " is reserved and cannot be assigned"));
                }
                continue;
            }
            if (blocked(use.canonical) && (use.declared != null || use.assigned || use.read)) {
                errors.add(new Diagnostic(use.line, use.column,
                        use.spelling + " conflicts with a block property"));
                continue;
            }
            SlotDirection direction;
            SlotDataType type;
            Kind prefixed = prefix(use.canonical);
            if (use.declared != null) {
                direction = directionOf(use.declared);
                type = booleanDecl(use.declared) ? SlotDataType.BOOLEAN : SlotDataType.NUMERIC;
            } else if (prefixed != null) {
                direction = prefixed.direction;
                type = prefixed.bool ? SlotDataType.BOOLEAN : SlotDataType.NUMERIC;
            } else if (!use.assigned && use.read) {
                direction = SlotDirection.INPUT;
                type = use.boolWrite && !use.numeric ? SlotDataType.BOOLEAN : SlotDataType.NUMERIC;
            } else if (use.assigned && !use.read) {
                direction = SlotDirection.OUTPUT;
                type = use.boolWrite && !use.numeric ? SlotDataType.BOOLEAN : SlotDataType.NUMERIC;
            } else if (use.assigned || use.read) {
                direction = SlotDirection.VARIABLE;
                type = use.boolWrite && !use.numeric ? SlotDataType.BOOLEAN : SlotDataType.NUMERIC;
            } else {
                continue;
            }
            specs.add(new SlotSpec(use.spelling, direction, type));
        }
        if (specs.size() > 128) {
            errors.add(new Diagnostic(0, 1, "A program can expose at most 128 slots"));
            return specs.subList(0, 128);
        }
        return specs;
    }

    private void walk(List<Stmt> statements) {
        for (int i = 0; i < statements.size(); i++) {
            Stmt s = statements.get(i);
            if (s instanceof Stmt.Assign) {
                Stmt.Assign a = (Stmt.Assign) s;
                write(a.name, false, a.line, a.column);
                walkExpr(a.expr, false);
            } else if (s instanceof Stmt.ArrAssign) {
                Stmt.ArrAssign a = (Stmt.ArrAssign) s;
                markArray(a.name, a.line, a.column);
                walkExpr(a.index, true);
                walkExpr(a.expr, true);
            } else if (s instanceof Stmt.If) {
                Stmt.If iff = (Stmt.If) s;
                walkExpr(iff.cond, false);
                walk(iff.thenBody);
                walk(iff.elseBody);
            } else if (s instanceof Stmt.For) {
                Stmt.For f = (Stmt.For) s;
                use(f.name, f.line, f.column).loop = true;
                walkExpr(f.start, true);
                walkExpr(f.end, true);
                if (f.step != null) {
                    walkExpr(f.step, true);
                }
            } else if (s instanceof Stmt.Next) {
                if (((Stmt.Next) s).name != null) {
                    use(((Stmt.Next) s).name, s.line, s.column).loop = true;
                }
            } else if (s instanceof Stmt.OnBranch) {
                walkExpr(((Stmt.OnBranch) s).expr, true);
            } else if (s instanceof Stmt.SetPoint) {
                write(((Stmt.SetPoint) s).name, true, s.line, s.column);
            } else if (s instanceof Stmt.Print) {
                for (Expr e : ((Stmt.Print) s).items) {
                    walkExpr(e, false);
                }
            } else if (s instanceof Stmt.Dim) {
                Stmt.Dim d = (Stmt.Dim) s;
                markArray(d.name, d.line, d.column);
                walkExpr(d.size, true);
            } else if (s instanceof Stmt.Declare) {
                Stmt.Declare d = (Stmt.Declare) s;
                for (int n = 0; n < d.names.size(); n++) {
                    Use u = use(d.names.get(n), d.line, d.column);
                    u.declared = d.kind;
                    if (booleanDecl(d.kind)) {
                        u.boolWrite = true;
                    }
                }
            }
        }
    }

    private void walkExpr(Expr e, boolean numeric) {
        if (e == null) {
            return;
        }
        if (e instanceof Expr.Var) {
            Use u = use(((Expr.Var) e).name, e.line, e.column);
            u.read = true;
            if (numeric) {
                u.numeric = true;
            }
            return;
        }
        if (e instanceof Expr.Index) {
            Expr.Index ix = (Expr.Index) e;
            markArray(ix.name, ix.line, ix.column);
            walkExpr(ix.index, true);
            return;
        }
        if (e instanceof Expr.Unary) {
            Expr.Unary u = (Expr.Unary) e;
            walkExpr(u.expr, u.op != TokenType.NOT || numeric);
            return;
        }
        if (e instanceof Expr.Binary) {
            Expr.Binary b = (Expr.Binary) e;
            boolean math = b.op != TokenType.AND && b.op != TokenType.OR && b.op != TokenType.XOR
                    && b.op != TokenType.EQ && b.op != TokenType.NE && b.op != TokenType.LT
                    && b.op != TokenType.GT && b.op != TokenType.LE && b.op != TokenType.GE;
            walkExpr(b.left, numeric || math);
            walkExpr(b.right, numeric || math);
            return;
        }
        if (e instanceof Expr.Call) {
            Expr.Call c = (Expr.Call) e;
            String name = Functions.canon(c.name);
            boolean level = name.equals("TIMEON") || name.equals("TIMEOFF")
                    || name.equals("SENSORON") || name.equals("SENSOROFF");
            for (int i = 0; i < c.args.size(); i++) {
                walkExpr(c.args.get(i), !level);
            }
        }
    }

    private void write(String name, boolean boolWrite, int line, int column) {
        Use u = use(name, line, column);
        u.write = true;
        u.assigned = true;
        if (boolWrite) {
            u.boolWrite = true;
        } else {
            u.numeric = true;
        }
    }

    private void markArray(String name, int line, int column) {
        use(name, line, column).array = true;
    }

    private Use use(String spelling, int line, int column) {
        String key = Functions.canon(spelling);
        Use u = uses.get(key);
        if (u == null) {
            u = new Use(spelling, line, column, order++);
            uses.put(key, u);
        }
        return u;
    }

    private static boolean booleanDecl(Stmt.DeclKind kind) {
        return kind == Stmt.DeclKind.BINPUT || kind == Stmt.DeclKind.BOUTPUT || kind == Stmt.DeclKind.BVAR;
    }

    private static SlotDirection directionOf(Stmt.DeclKind kind) {
        if (kind == Stmt.DeclKind.INPUT || kind == Stmt.DeclKind.BINPUT) {
            return SlotDirection.INPUT;
        }
        if (kind == Stmt.DeclKind.OUTPUT || kind == Stmt.DeclKind.BOUTPUT) {
            return SlotDirection.OUTPUT;
        }
        return SlotDirection.VARIABLE;
    }

    static Kind prefix(String canonical) {
        String name = canonical.toUpperCase(Locale.ROOT);
        for (int i = 0; i < PREFIXES.length; i++) {
            String p = PREFIXES[i];
            if (!name.startsWith(p)) {
                continue;
            }
            String rest = name.substring(p.length());
            if (!(rest.isEmpty() || digits(rest) || (rest.charAt(0) == '_' && rest.length() > 1))) {
                continue;
            }
            return kindForPrefix(p);
        }
        return null;
    }

    private static Kind kindForPrefix(String prefix) {
        if (prefix.equals("BINPUT") || prefix.equals("BIN") || prefix.equals("BI") || prefix.equals("IN") && false) {
            return new Kind(SlotDirection.INPUT, true);
        }
        if (prefix.equals("BINPUT") || prefix.equals("BIN") || prefix.equals("BI")) {
            return new Kind(SlotDirection.INPUT, true);
        }
        if (prefix.equals("BOUTPUT") || prefix.equals("BOUT") || prefix.equals("BO")) {
            return new Kind(SlotDirection.OUTPUT, true);
        }
        if (prefix.equals("BVAR") || prefix.equals("BV")) {
            return new Kind(SlotDirection.VARIABLE, true);
        }
        if (prefix.equals("AI") || prefix.equals("IN")) {
            return new Kind(SlotDirection.INPUT, false);
        }
        if (prefix.equals("AO") || prefix.equals("OUT")) {
            return new Kind(SlotDirection.OUTPUT, false);
        }
        return new Kind(SlotDirection.VARIABLE, false);
    }

    private static boolean digits(String rest) {
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return rest.length() > 0;
    }

    static boolean blocked(String canonical) {
        switch (canonical) {
            case "PROGRAM":
            case "SCANINTERVAL":
            case "MAXINSTRUCTIONS":
            case "ENABLED":
            case "LASTERROR":
            case "CONSOLE":
            case "SCANCOUNT":
            case "SLOTSUMMARY":
            case "TABLE1":
            case "TABLE2":
            case "TABLE3":
            case "TABLE4":
            case "STATUS":
            case "DISPLAYNAME":
                return true;
            default:
                return false;
        }
    }

    static final class Kind {
        final SlotDirection direction;
        final boolean bool;

        Kind(SlotDirection direction, boolean bool) {
            this.direction = direction;
            this.bool = bool;
        }
    }

    static final class Use {
        final String spelling;
        final String canonical;
        final int line;
        final int column;
        boolean read;
        boolean write;
        boolean assigned;
        boolean boolWrite;
        boolean numeric;
        boolean loop;
        boolean array;
        Stmt.DeclKind declared;

        Use(String spelling, int line, int column, int order) {
            this.spelling = spelling;
            this.canonical = Functions.canon(spelling);
            this.line = line;
            this.column = column;
        }
    }
}

package com.controlbasic.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Statement nodes produced by the parser, before jump targets are resolved. */
public abstract class Stmt {
    public final int line;
    public final int column;

    protected Stmt(int line, int column) {
        this.line = line;
        this.column = column;
    }

    /** Records a Basic line number and/or a label. Emits no code. */
    public static final class Marker extends Stmt {
        public final int basicLine;
        public final String label;

        public Marker(int basicLine, String label, int line, int column) {
            super(line, column);
            this.basicLine = basicLine;
            this.label = label;
        }
    }

    public static final class Assign extends Stmt {
        public final String name;
        public final Expr expr;

        public Assign(String name, Expr expr, int line, int column) {
            super(line, column);
            this.name = name;
            this.expr = expr;
        }
    }

    public static final class ArrAssign extends Stmt {
        public final String name;
        public final Expr index;
        public final Expr expr;

        public ArrAssign(String name, Expr index, Expr expr, int line, int column) {
            super(line, column);
            this.name = name;
            this.index = index;
            this.expr = expr;
        }
    }

    public enum Edge {
        NONE, RISING, FALLING
    }

    public static final class If extends Stmt {
        public final Edge edge;
        public final Expr cond;
        public final List<Stmt> thenBody;
        public final List<Stmt> elseBody;

        public If(Edge edge, Expr cond, List<Stmt> thenBody, List<Stmt> elseBody, int line, int column) {
            super(line, column);
            this.edge = edge;
            this.cond = cond;
            this.thenBody = thenBody == null ? Collections.<Stmt>emptyList() : thenBody;
            this.elseBody = elseBody == null ? Collections.<Stmt>emptyList() : elseBody;
        }
    }

    public static final class Goto extends Stmt {
        public final String target;

        public Goto(String target, int line, int column) {
            super(line, column);
            this.target = target;
        }
    }

    public static final class Gosub extends Stmt {
        public final String target;

        public Gosub(String target, int line, int column) {
            super(line, column);
            this.target = target;
        }
    }

    public static final class Return extends Stmt {
        public Return(int line, int column) {
            super(line, column);
        }
    }

    public static final class For extends Stmt {
        public final String name;
        public final Expr start;
        public final Expr end;
        public final Expr step;

        public For(String name, Expr start, Expr end, Expr step, int line, int column) {
            super(line, column);
            this.name = name;
            this.start = start;
            this.end = end;
            this.step = step;
        }
    }

    public static final class Next extends Stmt {
        public final String name;

        public Next(String name, int line, int column) {
            super(line, column);
            this.name = name;
        }
    }

    public static final class OnBranch extends Stmt {
        public final Expr expr;
        public final boolean gosub;
        public final List<String> targets;

        public OnBranch(Expr expr, boolean gosub, List<String> targets, int line, int column) {
            super(line, column);
            this.expr = expr;
            this.gosub = gosub;
            this.targets = targets;
        }
    }

    public static final class OnError extends Stmt {
        /** Null clears the handler. */
        public final String target;

        public OnError(String target, int line, int column) {
            super(line, column);
            this.target = target;
        }
    }

    public static final class SetPoint extends Stmt {
        public final String name;
        public final boolean on;

        public SetPoint(String name, boolean on, int line, int column) {
            super(line, column);
            this.name = name;
            this.on = on;
        }
    }

    public static final class EndScan extends Stmt {
        public EndScan(int line, int column) {
            super(line, column);
        }
    }

    public static final class Halt extends Stmt {
        /** KMC {@code HALT "Message"}; null when no message was given. */
        public final String message;

        public Halt(String message, int line, int column) {
            super(line, column);
            this.message = message;
        }
    }

    public static final class Print extends Stmt {
        public final List<Expr> items;

        public Print(List<Expr> items, int line, int column) {
            super(line, column);
            this.items = items == null ? new ArrayList<Expr>() : items;
        }
    }

    public static final class Dim extends Stmt {
        public final String name;
        public final Expr size;

        public Dim(String name, Expr size, int line, int column) {
            super(line, column);
            this.name = name;
            this.size = size;
        }
    }

    public enum DeclKind {
        INPUT, OUTPUT, BINPUT, BOUTPUT, VAR, BVAR
    }

    public static final class Declare extends Stmt {
        public final DeclKind kind;
        public final List<String> names;

        public Declare(DeclKind kind, List<String> names, int line, int column) {
            super(line, column);
            this.kind = kind;
            this.names = names;
        }
    }
}

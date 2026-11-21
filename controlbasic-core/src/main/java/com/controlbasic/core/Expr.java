package com.controlbasic.core;

import java.util.ArrayList;
import java.util.List;

/** Expression nodes. Evaluation lives in {@link Evaluator}. */
public abstract class Expr {
    public final int line;
    public final int column;

    protected Expr(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public static final class Num extends Expr {
        public final double value;

        public Num(double value, int line, int column) {
            super(line, column);
            this.value = value;
        }
    }

    public static final class Str extends Expr {
        public final String value;

        public Str(String value, int line, int column) {
            super(line, column);
            this.value = value == null ? "" : value;
        }
    }

    public static final class Var extends Expr {
        public final String name;

        public Var(String name, int line, int column) {
            super(line, column);
            this.name = name;
        }
    }

    public static final class Index extends Expr {
        public final String name;
        public final Expr index;

        public Index(String name, Expr index, int line, int column) {
            super(line, column);
            this.name = name;
            this.index = index;
        }
    }

    public static final class Unary extends Expr {
        public final TokenType op;
        public final Expr expr;

        public Unary(TokenType op, Expr expr, int line, int column) {
            super(line, column);
            this.op = op;
            this.expr = expr;
        }
    }

    public static final class Binary extends Expr {
        public final TokenType op;
        public final Expr left;
        public final Expr right;

        public Binary(TokenType op, Expr left, Expr right, int line, int column) {
            super(line, column);
            this.op = op;
            this.left = left;
            this.right = right;
        }
    }

    /** {@code site} identifies a stateful call (PID, INTERVAL, IF edges are separate). */
    public static final class Call extends Expr {
        public final String name;
        public final List<Expr> args;
        public final int site;

        public Call(String name, List<Expr> args, int site, int line, int column) {
            super(line, column);
            this.name = name;
            this.args = args;
            this.site = site;
        }
    }

    public static List<Expr> list(Expr... items) {
        List<Expr> out = new ArrayList<Expr>();
        for (Expr e : items) {
            out.add(e);
        }
        return out;
    }
}

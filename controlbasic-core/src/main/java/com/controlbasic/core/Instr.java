package com.controlbasic.core;

import java.util.List;

/** One executable step. Fields are filled by {@link Compiler}. */
final class Instr {
    static final int ASSIGN = 1;
    static final int ARR = 2;
    static final int IF = 3;
    static final int GOTO = 4;
    static final int GOSUB = 5;
    static final int RETURN = 6;
    static final int FOR = 7;
    static final int NEXT = 8;
    static final int ON = 9;
    static final int ONERR = 10;
    static final int SET = 11;
    static final int END = 12;
    static final int HALT = 13;
    static final int PRINT = 14;
    static final int DIM = 15;

    int op;
    int line;
    int column;
    String name;
    Expr expr;
    Expr expr2;
    Expr expr3;
    List<Expr> items;
    int a;
    int b;
    int c;
    int[] jumps;
    String[] texts;
}

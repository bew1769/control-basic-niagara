package com.controlbasic.core;

/** Result of a single scan. */
public final class Scan {
    public final Diagnostic error;
    public final boolean halted;
    public final int instructions;

    public Scan(Diagnostic error, boolean halted, int instructions) {
        this.error = error;
        this.halted = halted;
        this.instructions = instructions;
    }

    public boolean ok() {
        return error == null;
    }
}

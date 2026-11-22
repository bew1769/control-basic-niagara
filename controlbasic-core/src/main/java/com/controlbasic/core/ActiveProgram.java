package com.controlbasic.core;

/**
 * The program a block is executing. A source that fails to compile does not
 * replace a program that compiled earlier, so a typo saved while the station
 * is running leaves the last good program in control instead of freezing
 * every output. The compile error is still reported.
 */
public final class ActiveProgram {
    /** Appended to the compile error while an older program keeps running. */
    public static final String STILL_RUNNING = " (still running the last program that compiled)";

    private volatile Compilation running;
    private volatile String compileError;

    /**
     * Offers a new compile result.
     *
     * @return true when it compiled and is now the running program
     */
    public synchronized boolean offer(Compilation next) {
        if (next != null && next.ok()) {
            running = next;
            compileError = null;
            return true;
        }
        compileError = next == null || next.diagnostics().isEmpty()
                ? "Compile failed" : next.diagnostics().get(0).format();
        return false;
    }

    /** The program to execute, or null when nothing has compiled yet. */
    public Compilation running() {
        return running;
    }

    /** The latest compile error, or null when the latest source compiled. */
    public String compileError() {
        return compileError;
    }

    /**
     * Text for the block's Last Error. A compile error wins over a scan error
     * from the older program; otherwise the scan error, or "" when clean.
     */
    public String statusText(String scanError) {
        String ce = compileError;
        if (ce != null) {
            return running == null ? ce : ce + STILL_RUNNING;
        }
        return scanError == null ? "" : scanError;
    }
}

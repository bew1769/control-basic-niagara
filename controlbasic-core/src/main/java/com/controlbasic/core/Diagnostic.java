package com.controlbasic.core;

/** A parse or runtime problem. {@code line} is 1-based, or 0 when unknown. */
public final class Diagnostic {
    public final int line;
    public final int column;
    public final String message;

    public Diagnostic(int line, int column, String message) {
        this.line = line;
        this.column = column;
        this.message = message == null ? "" : message;
    }

    public String format() {
        if (line > 0) {
            return "Line " + line + ": " + message;
        }
        return message;
    }

    @Override
    public String toString() {
        return format();
    }
}

package com.controlbasic.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Token runs for a single source line. Styles match the order used by the
 * Workbench syntax adapter ({@code PLAIN}, {@code KEYWORD}, {@code COMMENT},
 * {@code STRING}, {@code NUMBER}, {@code ERROR}).
 */
public final class Highlight {
    public static final int PLAIN = 0;
    public static final int KEYWORD = 1;
    public static final int COMMENT = 2;
    public static final int STRING = 3;
    public static final int NUMBER = 4;
    public static final int ERROR = 5;

    public static final class Run {
        public final int start;
        public final int end;
        public final int style;

        public Run(int start, int end, int style) {
            this.start = start;
            this.end = end;
            this.style = style;
        }
    }

    private Highlight() {
    }

    /**
     * Color one physical line. {@code errorLine} is 1-based; when it matches
     * {@code lineNumber} the whole line is {@link #ERROR}.
     */
    public static List<Run> colorLine(String line, int lineNumber, int errorLine) {
        if (line == null) {
            line = "";
        }
        if (lineNumber == errorLine && errorLine > 0) {
            return Collections.singletonList(new Run(0, line.length(), ERROR));
        }
        List<Token> tokens = Lexer.tokenize(line);
        List<Run> runs = new ArrayList<Run>();
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.type == TokenType.EOF || t.type == TokenType.NEWLINE) {
                continue;
            }
            int start = Math.max(0, t.column - 1);
            int end = start;
            if (t.type == TokenType.STRING) {
                end = Math.min(line.length(), start + t.text.length() + 2);
            } else if (t.type == TokenType.ERROR) {
                end = Math.min(line.length(), start + Math.max(1, t.text.length()));
            } else {
                end = Math.min(line.length(), start + t.text.length());
            }
            if (end < start) {
                end = start;
            }
            runs.add(new Run(start, end, styleOf(t.type)));
        }
        return runs;
    }

    public static int styleOf(TokenType type) {
        if (type == null || type == TokenType.IDENT) {
            return PLAIN;
        }
        if (type == TokenType.COMMENT || type == TokenType.REM) {
            return COMMENT;
        }
        if (type == TokenType.STRING) {
            return STRING;
        }
        if (type == TokenType.NUMBER) {
            return NUMBER;
        }
        if (type == TokenType.ERROR) {
            return ERROR;
        }
        if (type.ordinal() >= TokenType.REM.ordinal()) {
            return KEYWORD;
        }
        return PLAIN;
    }
}

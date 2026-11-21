package com.controlbasic.core;

/** One lexeme. {@code line} and {@code column} are 1-based. */
public final class Token {
    public final TokenType type;
    public final String text;
    public final int line;
    public final int column;
    public final double number;

    public Token(TokenType type, String text, int line, int column, double number) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.column = column;
        this.number = number;
    }

    public static Token of(TokenType type, String text, int line, int column) {
        return new Token(type, text, line, column, 0);
    }

    public static Token number(String text, double number, int line, int column) {
        return new Token(TokenType.NUMBER, text, line, column, number);
    }
}

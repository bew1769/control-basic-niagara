package com.controlbasic.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Line-oriented lexer. Comments ({@code REM}, {@code '} and {@code //}) are
 * tokens so the editor highlighter and the parser share one scan.
 */
public final class Lexer {
    private static final Map<String, TokenType> KEYWORDS = new HashMap<String, TokenType>();

    static {
        kw("REM", TokenType.REM);
        kw("LET", TokenType.LET);
        kw("IF", TokenType.IF);
        kw("THEN", TokenType.THEN);
        kw("ELSE", TokenType.ELSE);
        kw("ELSEIF", TokenType.ELSEIF);
        kw("ENDIF", TokenType.ENDIF);
        kw("END", TokenType.END);
        kw("GOTO", TokenType.GOTO);
        kw("GOSUB", TokenType.GOSUB);
        kw("RETURN", TokenType.RETURN);
        kw("FOR", TokenType.FOR);
        kw("TO", TokenType.TO);
        kw("STEP", TokenType.STEP);
        kw("NEXT", TokenType.NEXT);
        kw("ON", TokenType.ON);
        kw("ONERROR", TokenType.ONERROR);
        kw("START", TokenType.START);
        kw("STOP", TokenType.STOP);
        kw("ENABLE", TokenType.ENABLE);
        kw("DISABLE", TokenType.DISABLE);
        kw("OPEN", TokenType.OPEN);
        kw("CLOSE", TokenType.CLOSE);
        kw("HALT", TokenType.HALT);
        kw("PRINT", TokenType.PRINT);
        kw("DIM", TokenType.DIM);
        kw("INPUT", TokenType.INPUT);
        kw("OUTPUT", TokenType.OUTPUT);
        kw("BINPUT", TokenType.BINPUT);
        kw("BOUTPUT", TokenType.BOUTPUT);
        kw("BOOLINPUT", TokenType.BINPUT);
        kw("BOOLOUTPUT", TokenType.BOUTPUT);
        kw("VAR", TokenType.VAR);
        kw("BVAR", TokenType.BVAR);
        kw("BOOL", TokenType.BVAR);
        kw("AND", TokenType.AND);
        kw("OR", TokenType.OR);
        kw("XOR", TokenType.XOR);
        kw("NOT", TokenType.NOT);
        kw("MOD", TokenType.MOD);
        kw("OFF", TokenType.OFF);
        kw("INTEGER", TokenType.INTEGER);
        kw("REAL", TokenType.REAL);
        kw("STRING", TokenType.STRING_KW);
        kw("WAIT", TokenType.WAIT);
        kw("ALARM", TokenType.ALARM);
        kw("DALARM", TokenType.ALARM);
        kw("FUNCTION", TokenType.FUNCTION);
        kw("PROCEDURE", TokenType.PROCEDURE);
        kw("CALL", TokenType.CALL);
        kw("COV", TokenType.COV);
        kw("NETPOINT", TokenType.NETPOINT);
        kw("NETPOINTCACHE", TokenType.NETPOINT);
    }

    private Lexer() {
    }

    private static void kw(String name, TokenType type) {
        KEYWORDS.put(name, type);
    }

    public static List<Token> tokenize(String source) {
        if (source == null) {
            source = "";
        }
        List<Token> out = new ArrayList<Token>();
        int line = 1;
        int col = 1;
        int i = 0;
        int n = source.length();
        while (i < n) {
            char c = source.charAt(i);
            if (c == '\n') {
                out.add(Token.of(TokenType.NEWLINE, "\n", line, col));
                i++;
                line++;
                col = 1;
                continue;
            }
            if (c == '\r') {
                out.add(Token.of(TokenType.NEWLINE, "\n", line, col));
                i++;
                if (i < n && source.charAt(i) == '\n') {
                    i++;
                }
                line++;
                col = 1;
                continue;
            }
            if (c == ' ' || c == '\t') {
                i++;
                col++;
                continue;
            }
            if (c == '\'') {
                int startCol = col;
                int start = i;
                i++;
                col++;
                while (i < n && source.charAt(i) != '\n' && source.charAt(i) != '\r') {
                    i++;
                    col++;
                }
                out.add(Token.of(TokenType.COMMENT, source.substring(start, i), line, startCol));
                continue;
            }
            if (c == '/' && i + 1 < n && source.charAt(i + 1) == '/') {
                int startCol = col;
                int start = i;
                i += 2;
                col += 2;
                while (i < n && source.charAt(i) != '\n' && source.charAt(i) != '\r') {
                    i++;
                    col++;
                }
                out.add(Token.of(TokenType.COMMENT, source.substring(start, i), line, startCol));
                continue;
            }
            if (c == '"') {
                int startCol = col;
                int startLine = line;
                i++;
                col++;
                StringBuilder sb = new StringBuilder();
                boolean closed = false;
                while (i < n) {
                    char d = source.charAt(i);
                    if (d == '\n' || d == '\r') {
                        break;
                    }
                    if (d == '"') {
                        i++;
                        col++;
                        closed = true;
                        break;
                    }
                    if (d == '\\' && i + 1 < n) {
                        char e = source.charAt(i + 1);
                        if (e == '"' || e == '\\') {
                            sb.append(e);
                            i += 2;
                            col += 2;
                            continue;
                        }
                    }
                    sb.append(d);
                    i++;
                    col++;
                }
                if (!closed) {
                    out.add(Token.of(TokenType.ERROR, "Unterminated string", startLine, startCol));
                } else {
                    out.add(Token.of(TokenType.STRING, sb.toString(), startLine, startCol));
                }
                continue;
            }
            if (isDigit(c) || (c == '.' && i + 1 < n && isDigit(source.charAt(i + 1)))) {
                int startCol = col;
                int start = i;
                i++;
                col++;
                while (i < n && isDigit(source.charAt(i))) {
                    i++;
                    col++;
                }
                if (i < n && source.charAt(i) == '.') {
                    i++;
                    col++;
                    while (i < n && isDigit(source.charAt(i))) {
                        i++;
                        col++;
                    }
                }
                if (i < n && (source.charAt(i) == 'e' || source.charAt(i) == 'E')) {
                    int ePos = i;
                    int eCol = col;
                    i++;
                    col++;
                    if (i < n && (source.charAt(i) == '+' || source.charAt(i) == '-')) {
                        i++;
                        col++;
                    }
                    int expStart = i;
                    while (i < n && isDigit(source.charAt(i))) {
                        i++;
                        col++;
                    }
                    if (i == expStart) {
                        i = ePos;
                        col = eCol;
                    }
                }
                String text = source.substring(start, i);
                try {
                    out.add(Token.number(text, Double.parseDouble(text), line, startCol));
                } catch (NumberFormatException ex) {
                    out.add(Token.of(TokenType.ERROR, "Bad number " + text, line, startCol));
                }
                continue;
            }
            if (isIdentStart(c)) {
                int startCol = col;
                int start = i;
                i++;
                col++;
                while (i < n && isIdentPart(source.charAt(i))) {
                    i++;
                    col++;
                }
                // KMC object.property syntax (AO8.RD, TL1.EN): keep it one token.
                if (i + 1 < n && source.charAt(i) == '.' && isIdentStart(source.charAt(i + 1))) {
                    i++;
                    col++;
                    while (i < n && isIdentPart(source.charAt(i))) {
                        i++;
                        col++;
                    }
                }
                String text = source.substring(start, i);
                String upper = text.toUpperCase(Locale.ROOT);
                TokenType kw = KEYWORDS.get(upper);
                if (kw == TokenType.IF && i < n && (source.charAt(i) == '+' || source.charAt(i) == '-')) {
                    char sign = source.charAt(i);
                    i++;
                    col++;
                    out.add(Token.of(sign == '+' ? TokenType.IF_PLUS : TokenType.IF_MINUS, text + sign, line, startCol));
                    continue;
                }
                if (kw == TokenType.REM) {
                    out.add(Token.of(TokenType.REM, text, line, startCol));
                    int cStart = i;
                    int cCol = col;
                    while (i < n && source.charAt(i) != '\n' && source.charAt(i) != '\r') {
                        i++;
                        col++;
                    }
                    if (i > cStart) {
                        out.add(Token.of(TokenType.COMMENT, source.substring(cStart, i), line, cCol));
                    }
                    continue;
                }
                if (kw != null) {
                    out.add(Token.of(kw, text, line, startCol));
                } else {
                    out.add(Token.of(TokenType.IDENT, text, line, startCol));
                }
                continue;
            }
            int startCol = col;
            if (c == '<' && i + 1 < n && source.charAt(i + 1) == '>') {
                out.add(Token.of(TokenType.NE, "<>", line, startCol));
                i += 2;
                col += 2;
                continue;
            }
            if (c == '<' && i + 1 < n && source.charAt(i + 1) == '=') {
                out.add(Token.of(TokenType.LE, "<=", line, startCol));
                i += 2;
                col += 2;
                continue;
            }
            if (c == '>' && i + 1 < n && source.charAt(i + 1) == '=') {
                out.add(Token.of(TokenType.GE, ">=", line, startCol));
                i += 2;
                col += 2;
                continue;
            }
            TokenType sym = symbol(c);
            if (sym != null) {
                out.add(Token.of(sym, String.valueOf(c), line, startCol));
                i++;
                col++;
                continue;
            }
            out.add(Token.of(TokenType.ERROR, "Unexpected character '" + c + "'", line, startCol));
            i++;
            col++;
        }
        out.add(Token.of(TokenType.EOF, "", line, col));
        return out;
    }

    private static TokenType symbol(char c) {
        switch (c) {
            case '+': return TokenType.PLUS;
            case '-': return TokenType.MINUS;
            case '*': return TokenType.STAR;
            case '/': return TokenType.SLASH;
            case '\\': return TokenType.BACKSLASH;
            case '^': return TokenType.CARET;
            case '=': return TokenType.EQ;
            case '<': return TokenType.LT;
            case '>': return TokenType.GT;
            case '(': return TokenType.LPAREN;
            case ')': return TokenType.RPAREN;
            case '[': return TokenType.LBRACK;
            case ']': return TokenType.RBRACK;
            case ',': return TokenType.COMMA;
            case ':': return TokenType.COLON;
            case ';': return TokenType.SEMI;
            default: return null;
        }
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isIdentStart(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
    }

    private static boolean isIdentPart(char c) {
        return isIdentStart(c) || isDigit(c);
    }
}

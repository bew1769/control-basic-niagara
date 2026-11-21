package com.controlbasic.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Recursive-descent parser for line-numbered and label-style Control Basic.
 * The first error aborts the parse; the diagnostic carries the source line.
 */
final class Parser {
    private final List<Token> tokens;
    private int i;
    private int nextSite = 1;

    private static final Map<String, String> ALIASES = new HashMap<String, String>();

    static {
        alias("TIME-ON", "TIMEON");
        alias("TIME-OFF", "TIMEOFF");
        alias("SIN-1", "ARCSIN");
        alias("COS-1", "ARCCOS");
        alias("TAN-1", "ARCTAN");
        alias("DEW-POINT", "DEWPOINT");
        alias("DEW-POINT-SI", "DEWPOINT");
        alias("ENTHALPY-SI", "ENTHALPYSI");
        alias("LN-1", "INVLN");
        alias("SENSOR-ON", "SENSORON");
        alias("SENSOR-OFF", "SENSOROFF");
        alias("ON-ERROR", "ONERROR");
    }

    private static void alias(String from, String to) {
        ALIASES.put(from, to);
    }

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parseProgram() {
        List<Stmt> out = new ArrayList<Stmt>();
        parseBlockInto(out, false);
        return out;
    }

    private void parseBlockInto(List<Stmt> out, boolean stopOnCloser) {
        while (!eof()) {
            skipLineBreaks();
            if (eof()) {
                break;
            }
            if (stopOnCloser && isBlockStop()) {
                return;
            }
            parseLineInto(out, stopOnCloser);
        }
    }

    private void parseLineInto(List<Stmt> out, boolean stopOnCloser) {
        int line = peek().line;
        int col = peek().column;
        int basic = -1;
        if (isLineNumber()) {
            basic = (int) peek().number;
            advance();
            if (check(TokenType.COLON)) {
                advance();
            }
        }
        String label = null;
        if (check(TokenType.IDENT) && checkAt(1, TokenType.COLON)) {
            label = peek().text;
            advance();
            advance();
        }
        if (basic >= 0 || label != null) {
            out.add(new Stmt.Marker(basic, label, line, col));
        }
        if (stopOnCloser && isBlockStop()) {
            return;
        }
        if (nlOrEof() || check(TokenType.COMMENT)) {
            skipComments();
            if (check(TokenType.NEWLINE)) {
                advance();
            }
            return;
        }
        parseStatementsRestOfLine(out, stopOnCloser);
    }

    private void parseStatementsRestOfLine(List<Stmt> out, boolean stopOnCloser) {
        while (!nlOrEof()) {
            skipComments();
            if (nlOrEof()) {
                break;
            }
            if (stopOnCloser && isBlockStop()) {
                return;
            }
            out.add(parseStatement());
            skipComments();
            if (check(TokenType.COLON)) {
                advance();
                continue;
            }
            break;
        }
        if (check(TokenType.NEWLINE)) {
            advance();
        }
    }

    private Stmt parseStatement() {
        Token t = peek();
        if (t.type == TokenType.ERROR) {
            throw error(t, t.text);
        }
        if (t.type == TokenType.REM) {
            advance();
            skipComments();
            return new Stmt.Marker(-1, null, t.line, t.column);
        }
        if (t.type == TokenType.IF || t.type == TokenType.IF_PLUS || t.type == TokenType.IF_MINUS) {
            return parseIf();
        }
        if (t.type == TokenType.GOTO) {
            advance();
            return new Stmt.Goto(parseTarget(), t.line, t.column);
        }
        if (t.type == TokenType.GOSUB) {
            advance();
            return new Stmt.Gosub(parseTarget(), t.line, t.column);
        }
        if (t.type == TokenType.RETURN) {
            advance();
            return new Stmt.Return(t.line, t.column);
        }
        if (t.type == TokenType.FOR) {
            return parseFor(t);
        }
        if (t.type == TokenType.NEXT) {
            advance();
            String name = null;
            if (check(TokenType.IDENT)) {
                name = advance().text;
            }
            return new Stmt.Next(name, t.line, t.column);
        }
        if (t.type == TokenType.ONERROR || isHyphenOnError()) {
            return parseOnError(t);
        }
        if (t.type == TokenType.ON) {
            return parseOn(t);
        }
        if (t.type == TokenType.START || t.type == TokenType.ENABLE || t.type == TokenType.OPEN) {
            advance();
            return new Stmt.SetPoint(parseName(), true, t.line, t.column);
        }
        if (t.type == TokenType.STOP || t.type == TokenType.DISABLE || t.type == TokenType.CLOSE) {
            advance();
            return new Stmt.SetPoint(parseName(), false, t.line, t.column);
        }
        if (t.type == TokenType.HALT) {
            advance();
            return new Stmt.Halt(t.line, t.column);
        }
        if (t.type == TokenType.END) {
            if (checkAt(1, TokenType.IF)) {
                throw error(t, "END IF without IF");
            }
            advance();
            return new Stmt.EndScan(t.line, t.column);
        }
        if (t.type == TokenType.ENDIF) {
            throw error(t, "ENDIF without IF");
        }
        if (t.type == TokenType.PRINT) {
            return parsePrint(t);
        }
        if (t.type == TokenType.DIM) {
            return parseDim(t);
        }
        if (t.type == TokenType.LET) {
            advance();
            return parseAssign();
        }
        if (isDecl(t.type)) {
            return parseDecl(t);
        }
        if (t.type == TokenType.INTEGER || t.type == TokenType.REAL) {
            return parseDecl(t, t.type == TokenType.INTEGER ? Stmt.DeclKind.VAR : Stmt.DeclKind.VAR);
        }
        if (t.type == TokenType.STRING_KW) {
            throw error(t, "STRING variables are not supported");
        }
        if (t.type == TokenType.WAIT) {
            throw error(t, "WAIT is not supported (it would block the scan)");
        }
        if (t.type == TokenType.ALARM) {
            throw error(t, "ALARM is not supported");
        }
        if (t.type == TokenType.FUNCTION || t.type == TokenType.PROCEDURE) {
            throw error(t, t.text.toUpperCase(Locale.ROOT) + " is not supported");
        }
        if (t.type == TokenType.CALL) {
            throw error(t, "CALL is not supported");
        }
        if (t.type == TokenType.COV || t.type == TokenType.NETPOINT) {
            throw error(t, t.text.toUpperCase(Locale.ROOT) + " is not supported");
        }
        if (t.type == TokenType.IDENT) {
            return parseAssign();
        }
        throw error(t, "Expected a statement");
    }

    private Stmt parseIf() {
        Token t = advance();
        Stmt.Edge edge = Stmt.Edge.NONE;
        if (t.type == TokenType.IF_PLUS) {
            edge = Stmt.Edge.RISING;
        } else if (t.type == TokenType.IF_MINUS) {
            edge = Stmt.Edge.FALLING;
        }
        Expr cond = parseExpr();
        if (!check(TokenType.THEN)) {
            throw error(peek(), "Expected THEN");
        }
        advance();
        if (check(TokenType.NEWLINE) || check(TokenType.EOF)) {
            if (check(TokenType.NEWLINE)) {
                advance();
            }
            List<Stmt> thenBody = new ArrayList<Stmt>();
            parseBlockInto(thenBody, true);
            List<Stmt> elseBody = new ArrayList<Stmt>();
            if (check(TokenType.ELSEIF)) {
                advance();
                elseBody.add(parseIfFromCondition(Stmt.Edge.NONE, peek()));
                return new Stmt.If(edge, cond, thenBody, elseBody, t.line, t.column);
            } else if (check(TokenType.ELSE)) {
                Token el = advance();
                if (check(TokenType.IF) || check(TokenType.IF_PLUS) || check(TokenType.IF_MINUS)) {
                    elseBody.add(parseIf());
                } else if (check(TokenType.NEWLINE) || check(TokenType.EOF)) {
                    if (check(TokenType.NEWLINE)) {
                        advance();
                    }
                    parseBlockInto(elseBody, true);
                } else {
                    parseStatementsRestOfLine(elseBody, true);
                }
                if (el.type == TokenType.ERROR) {
                    throw error(el, el.text);
                }
            }
            expectEndIf();
            return new Stmt.If(edge, cond, thenBody, elseBody, t.line, t.column);
        }
        List<Stmt> thenBody = new ArrayList<Stmt>();
        if (check(TokenType.NUMBER) && lineTargetFollows(1)) {
            thenBody.add(new Stmt.Goto(integerTarget(advance()), t.line, t.column));
        } else {
            parseSingleLineClause(thenBody);
        }
        List<Stmt> elseBody = new ArrayList<Stmt>();
        if (check(TokenType.ELSE)) {
            advance();
            if (check(TokenType.NUMBER) && lineTargetFollows(1)) {
                elseBody.add(new Stmt.Goto(integerTarget(advance()), t.line, t.column));
            } else if (!nlOrEof() && !check(TokenType.COLON)) {
                parseSingleLineClause(elseBody);
            }
        }
        return new Stmt.If(edge, cond, thenBody, elseBody, t.line, t.column);
    }

    /** ELSEIF already consumed; {@code mark} is the first token of the condition. */
    private Stmt parseIfFromCondition(Stmt.Edge edge, Token mark) {
        Expr cond = parseExpr();
        if (!check(TokenType.THEN)) {
            throw error(peek(), "Expected THEN");
        }
        advance();
        if (check(TokenType.NEWLINE) || check(TokenType.EOF)) {
            if (check(TokenType.NEWLINE)) {
                advance();
            }
            List<Stmt> thenBody = new ArrayList<Stmt>();
            parseBlockInto(thenBody, true);
            List<Stmt> elseBody = new ArrayList<Stmt>();
            if (check(TokenType.ELSEIF)) {
                advance();
                elseBody.add(parseIfFromCondition(Stmt.Edge.NONE, peek()));
                return new Stmt.If(edge, cond, thenBody, elseBody, mark.line, mark.column);
            } else if (check(TokenType.ELSE)) {
                advance();
                if (check(TokenType.IF) || check(TokenType.IF_PLUS) || check(TokenType.IF_MINUS)) {
                    elseBody.add(parseIf());
                } else if (check(TokenType.NEWLINE) || check(TokenType.EOF)) {
                    if (check(TokenType.NEWLINE)) {
                        advance();
                    }
                    parseBlockInto(elseBody, true);
                } else {
                    parseStatementsRestOfLine(elseBody, true);
                }
            }
            expectEndIf();
            return new Stmt.If(edge, cond, thenBody, elseBody, mark.line, mark.column);
        }
        List<Stmt> thenBody = new ArrayList<Stmt>();
        parseSingleLineClause(thenBody);
        List<Stmt> elseBody = new ArrayList<Stmt>();
        if (check(TokenType.ELSE)) {
            advance();
            if (!nlOrEof() && !check(TokenType.COLON)) {
                parseSingleLineClause(elseBody);
            }
        }
        return new Stmt.If(edge, cond, thenBody, elseBody, mark.line, mark.column);
    }

    private void parseSingleLineClause(List<Stmt> out) {
        while (!nlOrEof() && !check(TokenType.ELSE) && !check(TokenType.COLON) && !isBlockStop()) {
            out.add(parseStatement());
            skipComments();
            if (check(TokenType.COLON) && !checkAt(1, TokenType.ELSE)) {
                // A colon inside a single-line THEN continues that clause.
                // A colon that introduces the next statement of the outer line
                // is left for the caller when the next token would end the IF.
                Token after = tokens.get(Math.min(i + 1, tokens.size() - 1));
                if (after.type == TokenType.ELSE || after.type == TokenType.NEWLINE || after.type == TokenType.EOF) {
                    break;
                }
                advance();
                continue;
            }
            break;
        }
    }

    private void expectEndIf() {
        if (check(TokenType.ENDIF)) {
            advance();
            return;
        }
        if (check(TokenType.END) && checkAt(1, TokenType.IF)) {
            advance();
            advance();
            return;
        }
        throw error(peek(), "Expected ENDIF");
    }

    private Stmt parseFor(Token t) {
        advance();
        String name = parseName();
        if (!check(TokenType.EQ)) {
            throw error(peek(), "Expected = in FOR");
        }
        advance();
        Expr start = parseExpr();
        if (!check(TokenType.TO)) {
            throw error(peek(), "Expected TO");
        }
        advance();
        Expr end = parseExpr();
        Expr step = null;
        if (check(TokenType.STEP)) {
            advance();
            step = parseExpr();
        }
        return new Stmt.For(name, start, end, step, t.line, t.column);
    }

    private Stmt parseOn(Token t) {
        advance();
        Expr expr = parseExpr();
        boolean gosub;
        if (check(TokenType.GOTO)) {
            gosub = false;
            advance();
        } else if (check(TokenType.GOSUB)) {
            gosub = true;
            advance();
        } else {
            throw error(peek(), "Expected GOTO or GOSUB after ON");
        }
        List<String> targets = new ArrayList<String>();
        targets.add(parseTarget());
        while (check(TokenType.COMMA)) {
            advance();
            targets.add(parseTarget());
        }
        return new Stmt.OnBranch(expr, gosub, targets, t.line, t.column);
    }

    private Stmt parseOnError(Token t) {
        if (t.type == TokenType.ONERROR) {
            advance();
        } else {
            // ON - ERROR already shaped by isHyphenOnError; consume three tokens.
            advance();
            advance();
            advance();
        }
        if (check(TokenType.OFF)) {
            advance();
            return new Stmt.OnError(null, t.line, t.column);
        }
        if (check(TokenType.GOTO)) {
            advance();
        }
        return new Stmt.OnError(parseTarget(), t.line, t.column);
    }

    private Stmt parsePrint(Token t) {
        advance();
        List<Expr> items = new ArrayList<Expr>();
        if (nlOrEof() || check(TokenType.COLON)) {
            return new Stmt.Print(items, t.line, t.column);
        }
        items.add(parsePrintItem());
        while (check(TokenType.SEMI) || check(TokenType.COMMA)) {
            advance();
            if (nlOrEof() || check(TokenType.COLON)) {
                break;
            }
            items.add(parsePrintItem());
        }
        return new Stmt.Print(items, t.line, t.column);
    }

    private Expr parsePrintItem() {
        if (check(TokenType.STRING)) {
            Token s = advance();
            return new Expr.Str(s.text, s.line, s.column);
        }
        return parseExpr();
    }

    private Stmt parseDim(Token t) {
        advance();
        String name = parseName();
        Expr size;
        if (check(TokenType.LPAREN) || check(TokenType.LBRACK)) {
            Token open = advance();
            size = parseExpr();
            TokenType close = open.type == TokenType.LPAREN ? TokenType.RPAREN : TokenType.RBRACK;
            if (!check(close)) {
                throw error(peek(), "Expected closing bracket in DIM");
            }
            advance();
        } else {
            throw error(peek(), "Expected DIM name(size)");
        }
        return new Stmt.Dim(name, size, t.line, t.column);
    }

    private Stmt parseDecl(Token t) {
        return parseDecl(t, declKind(t.type));
    }

    private Stmt parseDecl(Token t, Stmt.DeclKind kind) {
        advance();
        List<String> names = new ArrayList<String>();
        names.add(parseName());
        while (check(TokenType.COMMA)) {
            advance();
            names.add(parseName());
        }
        return new Stmt.Declare(kind, names, t.line, t.column);
    }

    private Stmt parseAssign() {
        Token nameTok = peek();
        String name = parseName();
        if (check(TokenType.LPAREN) || check(TokenType.LBRACK)) {
            Token open = advance();
            Expr index = parseExpr();
            TokenType close = open.type == TokenType.LPAREN ? TokenType.RPAREN : TokenType.RBRACK;
            if (!check(close)) {
                throw error(peek(), "Expected closing bracket");
            }
            advance();
            if (!check(TokenType.EQ)) {
                throw error(peek(), "Expected =");
            }
            advance();
            Expr rhs = parseExpr();
            return new Stmt.ArrAssign(name, index, rhs, nameTok.line, nameTok.column);
        }
        if (!check(TokenType.EQ)) {
            throw error(peek(), "Expected =");
        }
        advance();
        return new Stmt.Assign(name, parseExpr(), nameTok.line, nameTok.column);
    }

    private Expr parseExpr() {
        return parseXor();
    }

    private Expr parseXor() {
        Expr left = parseOr();
        while (check(TokenType.XOR)) {
            Token op = advance();
            left = new Expr.Binary(op.type, left, parseOr(), op.line, op.column);
        }
        return left;
    }

    private Expr parseOr() {
        Expr left = parseAnd();
        while (check(TokenType.OR)) {
            Token op = advance();
            left = new Expr.Binary(op.type, left, parseAnd(), op.line, op.column);
        }
        return left;
    }

    private Expr parseAnd() {
        Expr left = parseEq();
        while (check(TokenType.AND)) {
            Token op = advance();
            left = new Expr.Binary(op.type, left, parseEq(), op.line, op.column);
        }
        return left;
    }

    private Expr parseEq() {
        Expr left = parseRel();
        while (check(TokenType.EQ) || check(TokenType.NE)) {
            Token op = advance();
            left = new Expr.Binary(op.type, left, parseRel(), op.line, op.column);
        }
        return left;
    }

    private Expr parseRel() {
        Expr left = parseAdd();
        while (check(TokenType.LT) || check(TokenType.GT) || check(TokenType.LE) || check(TokenType.GE)) {
            Token op = advance();
            left = new Expr.Binary(op.type, left, parseAdd(), op.line, op.column);
        }
        return left;
    }

    private Expr parseAdd() {
        Expr left = parseMod();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            Token op = advance();
            left = new Expr.Binary(op.type, left, parseMod(), op.line, op.column);
        }
        return left;
    }

    private Expr parseMod() {
        Expr left = parseIdiv();
        while (check(TokenType.MOD)) {
            Token op = advance();
            left = new Expr.Binary(op.type, left, parseIdiv(), op.line, op.column);
        }
        return left;
    }

    private Expr parseIdiv() {
        Expr left = parseMul();
        while (check(TokenType.BACKSLASH)) {
            Token op = advance();
            left = new Expr.Binary(op.type, left, parseMul(), op.line, op.column);
        }
        return left;
    }

    private Expr parseMul() {
        Expr left = parseUnary();
        while (check(TokenType.STAR) || check(TokenType.SLASH)) {
            Token op = advance();
            left = new Expr.Binary(op.type, left, parseUnary(), op.line, op.column);
        }
        return left;
    }

    private Expr parseUnary() {
        if (check(TokenType.MINUS)) {
            Token op = advance();
            return new Expr.Unary(TokenType.MINUS, parseUnary(), op.line, op.column);
        }
        if (check(TokenType.PLUS)) {
            advance();
            return parseUnary();
        }
        return parsePow();
    }

    private Expr parsePow() {
        Expr left = parseNot();
        if (check(TokenType.CARET)) {
            Token op = advance();
            return new Expr.Binary(op.type, left, parsePow(), op.line, op.column);
        }
        return left;
    }

    private Expr parseNot() {
        if (check(TokenType.NOT)) {
            Token op = advance();
            return new Expr.Unary(TokenType.NOT, parseNot(), op.line, op.column);
        }
        return parsePrimary();
    }

    private Expr parsePrimary() {
        Token t = peek();
        if (t.type == TokenType.ERROR) {
            throw error(t, t.text);
        }
        if (t.type == TokenType.NUMBER) {
            advance();
            return parseTimeOrNumber(t);
        }
        if (t.type == TokenType.ON || t.type == TokenType.OFF) {
            advance();
            return new Expr.Num(t.type == TokenType.ON ? 1 : 0, t.line, t.column);
        }
        if (t.type == TokenType.STRING) {
            advance();
            return new Expr.Str(t.text, t.line, t.column);
        }
        if (t.type == TokenType.LPAREN) {
            advance();
            Expr inner = parseExpr();
            if (!check(TokenType.RPAREN)) {
                throw error(peek(), "Expected )");
            }
            advance();
            return inner;
        }
        if (t.type == TokenType.IDENT || isCallableKeyword(t.type)) {
            String name = readFunctionName();
            Token nameTok = t;
            if (check(TokenType.LPAREN)) {
                advance();
                List<Expr> args = new ArrayList<Expr>();
                if (!check(TokenType.RPAREN)) {
                    args.add(parseExpr());
                    while (check(TokenType.COMMA)) {
                        advance();
                        args.add(parseExpr());
                    }
                }
                if (!check(TokenType.RPAREN)) {
                    throw error(peek(), "Expected )");
                }
                advance();
                if (Functions.isFunction(name)) {
                    return new Expr.Call(name, args, nextSite++, nameTok.line, nameTok.column);
                }
                if (args.size() != 1) {
                    throw error(nameTok, "Unknown function " + name);
                }
                return new Expr.Index(name, args.get(0), nameTok.line, nameTok.column);
            }
            if (check(TokenType.LBRACK)) {
                advance();
                Expr index = parseExpr();
                if (!check(TokenType.RBRACK)) {
                    throw error(peek(), "Expected ]");
                }
                advance();
                return new Expr.Index(name, index, nameTok.line, nameTok.column);
            }
            return new Expr.Var(name, nameTok.line, nameTok.column);
        }
        throw error(t, "Expected an expression");
    }

    private Expr parseTimeOrNumber(Token first) {
        if (!(check(TokenType.COLON) && checkAt(1, TokenType.NUMBER))) {
            return new Expr.Num(first.number, first.line, first.column);
        }
        advance();
        Token mm = advance();
        double seconds = first.number * 3600.0 + mm.number * 60.0;
        if (check(TokenType.COLON) && checkAt(1, TokenType.NUMBER)) {
            advance();
            Token ss = advance();
            seconds += ss.number;
        }
        return new Expr.Num(seconds, first.line, first.column);
    }

    private String readFunctionName() {
        Token first = advance();
        if (first.type != TokenType.IDENT && !isCallableKeyword(first.type)) {
            throw error(first, "Expected a name");
        }
        StringBuilder spelling = new StringBuilder(first.text);
        StringBuilder hyphen = new StringBuilder(first.text.toUpperCase(Locale.ROOT));
        while (check(TokenType.MINUS) && checkAt(1, TokenType.IDENT)) {
            String candidate = hyphen + "-" + tokens.get(i + 1).text.toUpperCase(Locale.ROOT);
            if (!couldBeAlias(candidate)) {
                break;
            }
            advance();
            Token part = advance();
            spelling.append('-').append(part.text);
            hyphen.append('-').append(part.text.toUpperCase(Locale.ROOT));
            String resolved = ALIASES.get(hyphen.toString());
            if (resolved != null && !couldBeLongerAlias(hyphen.toString())) {
                return resolved;
            }
        }
        String resolved = ALIASES.get(hyphen.toString());
        return resolved == null ? spelling.toString() : resolved;
    }

    private static boolean couldBeAlias(String partial) {
        for (String key : ALIASES.keySet()) {
            if (key.startsWith(partial)) {
                return true;
            }
        }
        return false;
    }

    private static boolean couldBeLongerAlias(String value) {
        String longer = value + "-";
        for (String key : ALIASES.keySet()) {
            if (key.startsWith(longer)) {
                return true;
            }
        }
        return false;
    }

    private String parseName() {
        Token t = peek();
        if (t.type != TokenType.IDENT) {
            throw error(t, "Expected a name");
        }
        return advance().text;
    }

    private String parseTarget() {
        Token t = peek();
        if (t.type == TokenType.NUMBER) {
            if (t.text.indexOf('.') >= 0 || t.text.indexOf('e') >= 0 || t.text.indexOf('E') >= 0) {
                throw error(t, "Line target must be an integer");
            }
            advance();
            return integerTarget(t);
        }
        if (t.type == TokenType.IDENT) {
            return advance().text;
        }
        throw error(t, "Expected a line number or label");
    }

    private static String integerTarget(Token t) {
        return Long.toString((long) t.number);
    }

    private boolean isLineNumber() {
        if (!check(TokenType.NUMBER)) {
            return false;
        }
        Token t = peek();
        if (t.text.indexOf('.') >= 0 || t.text.indexOf('e') >= 0 || t.text.indexOf('E') >= 0) {
            return false;
        }
        TokenType next = tokens.get(Math.min(i + 1, tokens.size() - 1)).type;
        return next != TokenType.PLUS && next != TokenType.MINUS && next != TokenType.STAR
                && next != TokenType.SLASH && next != TokenType.BACKSLASH && next != TokenType.CARET
                && next != TokenType.EQ && next != TokenType.NE && next != TokenType.LT
                && next != TokenType.GT && next != TokenType.LE && next != TokenType.GE
                && next != TokenType.LPAREN && next != TokenType.RPAREN;
    }

    private boolean lineTargetFollows(int ahead) {
        TokenType next = tokens.get(Math.min(i + ahead, tokens.size() - 1)).type;
        return next == TokenType.ELSE || next == TokenType.NEWLINE || next == TokenType.EOF
                || next == TokenType.COLON || next == TokenType.COMMENT;
    }

    private boolean isBlockStop() {
        if (check(TokenType.ELSE) || check(TokenType.ELSEIF) || check(TokenType.ENDIF)) {
            return true;
        }
        return check(TokenType.END) && checkAt(1, TokenType.IF);
    }

    private boolean isHyphenOnError() {
        return check(TokenType.ON) && checkAt(1, TokenType.MINUS) && checkAt(2, TokenType.IDENT)
                && "ERROR".equalsIgnoreCase(tokens.get(i + 2).text);
    }

    private static boolean isDecl(TokenType type) {
        return type == TokenType.INPUT || type == TokenType.OUTPUT || type == TokenType.BINPUT
                || type == TokenType.BOUTPUT || type == TokenType.VAR || type == TokenType.BVAR;
    }

    private static Stmt.DeclKind declKind(TokenType type) {
        if (type == TokenType.INPUT) {
            return Stmt.DeclKind.INPUT;
        }
        if (type == TokenType.OUTPUT) {
            return Stmt.DeclKind.OUTPUT;
        }
        if (type == TokenType.BINPUT) {
            return Stmt.DeclKind.BINPUT;
        }
        if (type == TokenType.BOUTPUT) {
            return Stmt.DeclKind.BOUTPUT;
        }
        if (type == TokenType.BVAR) {
            return Stmt.DeclKind.BVAR;
        }
        return Stmt.DeclKind.VAR;
    }

    private static boolean isCallableKeyword(TokenType type) {
        return false;
    }

    private boolean nlOrEof() {
        skipComments();
        return check(TokenType.NEWLINE) || check(TokenType.EOF);
    }

    private boolean eof() {
        skipComments();
        return check(TokenType.EOF);
    }

    private void skipLineBreaks() {
        while (true) {
            skipComments();
            if (check(TokenType.NEWLINE)) {
                advance();
                continue;
            }
            break;
        }
    }

    private void skipComments() {
        while (check(TokenType.COMMENT)) {
            advance();
        }
    }

    private boolean check(TokenType type) {
        return peek().type == type;
    }

    private boolean checkAt(int ahead, TokenType type) {
        int idx = i + ahead;
        if (idx < 0 || idx >= tokens.size()) {
            return false;
        }
        return tokens.get(idx).type == type;
    }

    private Token peek() {
        return tokens.get(i);
    }

    private Token advance() {
        Token t = tokens.get(i);
        if (t.type != TokenType.EOF && i + 1 < tokens.size()) {
            i++;
        }
        return t;
    }

    private ParseException error(Token t, String message) {
        return new ParseException(t.line, t.column, message);
    }

    static final class ParseException extends RuntimeException {
        final int line;
        final int column;

        ParseException(int line, int column, String message) {
            super(message);
            this.line = line;
            this.column = column;
        }
    }

}

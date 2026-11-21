package com.controlbasic.core;

import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Expression evaluation and the built-in math / time functions. */
final class Evaluator {
    private final RuntimeState rt;
    private final ArrayDeque<RuntimeState.ForFrame> fors = new ArrayDeque<RuntimeState.ForFrame>();
    private final ArrayDeque<Integer> gosub = new ArrayDeque<Integer>();

    Evaluator(RuntimeState rt) {
        this.rt = rt;
    }

    ArrayDeque<RuntimeState.ForFrame> fors() {
        return fors;
    }

    ArrayDeque<Integer> gosub() {
        return gosub;
    }

    double eval(Expr e) {
        if (e instanceof Expr.Num) {
            return ((Expr.Num) e).value;
        }
        if (e instanceof Expr.Str) {
            throw fail(e, "String used as a number");
        }
        if (e instanceof Expr.Var) {
            return readVar(((Expr.Var) e).name, e);
        }
        if (e instanceof Expr.Index) {
            Expr.Index ix = (Expr.Index) e;
            return readIndex(ix.name, eval(ix.index), e);
        }
        if (e instanceof Expr.Unary) {
            Expr.Unary u = (Expr.Unary) e;
            double v = eval(u.expr);
            if (u.op == TokenType.MINUS) {
                return -v;
            }
            if (u.op == TokenType.NOT) {
                return truth(v) ? 0 : 1;
            }
            throw fail(e, "Bad operator");
        }
        if (e instanceof Expr.Binary) {
            return binary((Expr.Binary) e);
        }
        if (e instanceof Expr.Call) {
            return call((Expr.Call) e);
        }
        throw fail(e, "Bad expression");
    }

    String evalPrint(Expr e) {
        if (e instanceof Expr.Str) {
            return ((Expr.Str) e).value;
        }
        return format(eval(e));
    }

    static String format(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return Double.toString(v);
        }
        if (v == Math.rint(v) && Math.abs(v) < 1e15) {
            return Long.toString((long) v);
        }
        return Double.toString(v);
    }

    static boolean truth(double v) {
        return v != 0 && !Double.isNaN(v);
    }

    private double binary(Expr.Binary b) {
        if (b.op == TokenType.AND) {
            if (!truth(eval(b.left))) {
                return 0;
            }
            return truth(eval(b.right)) ? 1 : 0;
        }
        if (b.op == TokenType.OR) {
            if (truth(eval(b.left))) {
                return 1;
            }
            return truth(eval(b.right)) ? 1 : 0;
        }
        double l = eval(b.left);
        double r = eval(b.right);
        switch (b.op) {
            case XOR:
                return truth(l) ^ truth(r) ? 1 : 0;
            case PLUS:
                return l + r;
            case MINUS:
                return l - r;
            case STAR:
                return l * r;
            case SLASH:
                if (r == 0) {
                    throw fail(b, "Division by zero");
                }
                return l / r;
            case BACKSLASH:
                if (r == 0) {
                    throw fail(b, "Division by zero");
                }
                return (long) (l / r);
            case MOD:
                if (r == 0) {
                    throw fail(b, "Division by zero");
                }
                return l - r * (long) (l / r);
            case CARET:
                return Math.pow(l, r);
            case EQ:
                return l == r ? 1 : 0;
            case NE:
                return l != r ? 1 : 0;
            case LT:
                return l < r ? 1 : 0;
            case GT:
                return l > r ? 1 : 0;
            case LE:
                return l <= r ? 1 : 0;
            case GE:
                return l >= r ? 1 : 0;
            default:
                throw fail(b, "Bad operator");
        }
    }

    private double readVar(String name, Expr at) {
        String key = Functions.canon(name);
        if (Functions.isConstant(key)) {
            return Functions.constant(key);
        }
        if (key.equals("ON")) {
            return 1;
        }
        if (key.equals("OFF")) {
            return 0;
        }
        if (Functions.isNoArg(key)) {
            return noArg(key, at);
        }
        if (rt.arrays.containsKey(key)) {
            throw fail(at, "Array " + name + " needs an index");
        }
        Double v = rt.vars.get(key);
        return v == null ? 0 : v.doubleValue();
    }

    double writeVar(String name, double value, Expr at) {
        String key = Functions.canon(name);
        if (Functions.isConstant(key) || Functions.isNoArg(key) || key.equals("ON") || key.equals("OFF")) {
            throw fail(at == null ? new Expr.Num(0, 0, 0) : at, "Cannot assign " + name);
        }
        rt.vars.put(key, Double.valueOf(value));
        return value;
    }

    double readIndex(String name, double index, Expr at) {
        String key = Functions.canon(name);
        double[] arr = rt.arrays.get(key);
        int idx = (int) index;
        if (arr == null) {
            throw fail(at, "Array " + name + " is not dimensioned");
        }
        if (idx < 1 || idx >= arr.length) {
            throw fail(at, "Index out of range for " + name);
        }
        return arr[idx];
    }

    void writeIndex(String name, double index, double value, Expr at) {
        String key = Functions.canon(name);
        double[] arr = rt.arrays.get(key);
        int idx = (int) index;
        if (arr == null) {
            if (idx < 1 || idx > 256) {
                throw fail(at, "Index out of range for " + name);
            }
            arr = new double[idx + 1];
            rt.arrays.put(key, arr);
        }
        if (idx < 1 || idx >= arr.length) {
            throw fail(at, "Index out of range for " + name);
        }
        arr[idx] = value;
    }

    void dim(String name, double size, Expr at) {
        int n = (int) size;
        if (n < 1 || n > 10000) {
            throw fail(at, "DIM size out of range");
        }
        rt.arrays.put(Functions.canon(name), new double[n + 1]);
    }

    private double call(Expr.Call c) {
        String name = Functions.canon(c.name);
        String arity = Functions.arityError(name, c.args.size());
        if (arity != null) {
            throw fail(c, arity);
        }
        double[] a = new double[c.args.size()];
        boolean delay = name.equals("TIMEON") || name.equals("TIMEOFF") || name.equals("SENSORON")
                || name.equals("SENSOROFF");
        if (!delay) {
            for (int i = 0; i < c.args.size(); i++) {
                a[i] = eval(c.args.get(i));
            }
        }
        switch (name) {
            case "ABS":
                return Math.abs(a[0]);
            case "INT":
                return Math.floor(a[0]);
            case "FIX":
                return (long) a[0];
            case "SQR":
            case "SQRT":
                if (a[0] < 0) {
                    throw fail(c, "Square root of a negative number");
                }
                return Math.sqrt(a[0]);
            case "EXP":
            case "INVLN":
                return Math.exp(a[0]);
            case "LN":
                return requirePositive(c, a[0], Math.log(a[0]));
            case "LOG":
            case "LOG10":
                return requirePositive(c, a[0], Math.log10(a[0]));
            case "SIN":
                return Math.sin(a[0]);
            case "COS":
                return Math.cos(a[0]);
            case "TAN":
                return Math.tan(a[0]);
            case "ATN":
            case "ARCTAN":
                return Math.atan(a[0]);
            case "ARCSIN":
                return Math.asin(a[0]);
            case "ARCCOS":
                return Math.acos(a[0]);
            case "SGN":
                return Math.signum(a[0]);
            case "FRAC":
                return a[0] - Math.floor(a[0]);
            case "DEG":
                return Math.toDegrees(a[0]);
            case "RAD":
                return Math.toRadians(a[0]);
            case "MIN":
                return minmax(a, false);
            case "MAX":
                return minmax(a, true);
            case "ROUND":
                return round(a);
            case "MOD":
                if (a[1] == 0) {
                    throw fail(c, "Division by zero");
                }
                return a[0] - a[1] * (long) (a[0] / a[1]);
            case "LIMIT":
            case "CLAMP":
                return clamp(a[0], a[1], a[2]);
            case "SCALE":
                return scale(c, a);
            case "TABLE":
                return table(c, a);
            case "TBL":
                return tbl(c, a[0], (int) a[1]);
            case "RND":
                if (a.length == 0) {
                    return rt.random.nextDouble();
                }
                return rt.random.nextDouble() * a[0];
            case "TIMEON":
                return elapsed(c, true);
            case "TIMEOFF":
                return elapsed(c, false);
            case "SENSORON":
                return edge(c, true);
            case "SENSOROFF":
                return edge(c, false);
            case "INTERVAL":
                return interval(c, a[0]);
            case "PID":
                return pid(c, a);
            case "DEWPOINT":
                return dewC(a[0], a[1]);
            case "DEWPOINTF":
                return dewC((a[0] - 32.0) * 5.0 / 9.0, a[1]) * 9.0 / 5.0 + 32.0;
            case "ENTHALPYSI":
                return enthalpy(a[0], a[1]);
            case "ENTHALPY":
                return enthalpy(a[0], a[1]) * 0.4299226;
            case "TIME":
            case "DOW":
            case "DOM":
            case "DOY":
            case "MONTH":
            case "YEAR":
            case "SCAN":
            case "SCANS":
            case "FIRST":
                return noArg(name, c);
            default:
                throw fail(c, "Unknown function " + name);
        }
    }

    private double elapsed(Expr.Call c, boolean wantOn) {
        if (!(c.args.get(0) instanceof Expr.Var)) {
            throw fail(c, "TIMEON/TIMEOFF argument must be a variable");
        }
        String key = Functions.canon(((Expr.Var) c.args.get(0)).name);
        boolean on = truth(readVar(key, c));
        long now = rt.time.nowMillis();
        RuntimeState.TimerMark mark = rt.timers.get(key);
        if (mark == null) {
            rt.timers.put(key, new RuntimeState.TimerMark(on, now));
            return 0;
        }
        if (mark.on != on) {
            mark.on = on;
            mark.since = now;
            return 0;
        }
        if (on != wantOn) {
            return 0;
        }
        return Math.max(0, (now - mark.since) / 1000.0);
    }

    private double edge(Expr.Call c, boolean rising) {
        if (!(c.args.get(0) instanceof Expr.Var)) {
            throw fail(c, "SENSORON/SENSOROFF argument must be a variable");
        }
        String key = Functions.canon(((Expr.Var) c.args.get(0)).name);
        boolean on = truth(readVar(key, c));
        Boolean prev = rt.levelPrev.get(key);
        boolean was = prev != null && prev.booleanValue();
        // Updated at the end of the scan so two calls in one scan agree.
        rt.levelPrev.put(key + "#pending", Boolean.valueOf(on));
        if (rising) {
            return on && !was ? 1 : 0;
        }
        return !on && was ? 1 : 0;
    }

    void commitLevels() {
        Map<String, Boolean> pending = new HashMap<String, Boolean>();
        Iterator<Map.Entry<String, Boolean>> it = rt.levelPrev.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Boolean> e = it.next();
            if (e.getKey().endsWith("#pending")) {
                pending.put(e.getKey().substring(0, e.getKey().length() - 8), e.getValue());
                it.remove();
            }
        }
        rt.levelPrev.putAll(pending);
    }

    private double interval(Expr at, double period) {
        if (period < 0) {
            throw fail(at, "INTERVAL period must be >= 0");
        }
        long now = rt.time.nowMillis();
        Integer site = Integer.valueOf(siteOf(at));
        Long last = rt.intervals.get(site);
        if (last == null || now - last.longValue() >= (long) (period * 1000.0)) {
            rt.intervals.put(site, Long.valueOf(now));
            return 1;
        }
        return 0;
    }

    private int siteOf(Expr at) {
        return at instanceof Expr.Call ? ((Expr.Call) at).site : 0;
    }

    private double pid(Expr.Call c, double[] a) {
        double pv = a[0];
        double sp = a[1];
        double kp = a[2];
        double ki = a[3];
        double kd = a[4];
        double lo = a.length == 7 ? a[5] : Double.NEGATIVE_INFINITY;
        double hi = a.length == 7 ? a[6] : Double.POSITIVE_INFINITY;
        if (lo > hi) {
            double s = lo;
            lo = hi;
            hi = s;
        }
        RuntimeState.PidMark m = rt.pids.get(Integer.valueOf(c.site));
        long now = rt.time.nowMillis();
        if (m == null) {
            m = new RuntimeState.PidMark();
            m.lastMillis = now;
            rt.pids.put(Integer.valueOf(c.site), m);
        }
        double dt = (now - m.lastMillis) / 1000.0;
        if (!m.started || dt < 1e-6) {
            dt = rt.scanIntervalSeconds > 0 ? rt.scanIntervalSeconds : 1;
        }
        if (dt > 60) {
            dt = 60;
        }
        m.started = true;
        m.lastMillis = now;
        double err = sp - pv;
        m.integral += err * dt;
        double deriv = (err - m.prevError) / dt;
        m.prevError = err;
        double out = kp * err + ki * m.integral + kd * deriv;
        if (out > hi) {
            out = hi;
            m.integral -= err * dt;
        } else if (out < lo) {
            out = lo;
            m.integral -= err * dt;
        }
        return out;
    }

    private double noArg(String name, Expr at) {
        ZonedDateTime z = rt.time.now();
        switch (name) {
            case "TIME":
                return z.getHour() * 3600.0 + z.getMinute() * 60.0 + z.getSecond() + z.getNano() / 1e9;
            case "DOW":
                // Sunday = 1 ... Saturday = 7. java DayOfWeek Monday = 1.
                int dow = z.getDayOfWeek().getValue();
                return dow == 7 ? 1 : dow + 1;
            case "DOM":
                return z.getDayOfMonth();
            case "DOY":
                return z.getDayOfYear();
            case "MONTH":
                return z.getMonthValue();
            case "YEAR":
                return z.getYear();
            case "SCAN":
                return rt.scanIntervalSeconds;
            case "SCANS":
                return rt.scans;
            case "FIRST":
                return rt.scans == 1 ? 1 : 0;
            default:
                throw fail(at, "Unknown value " + name);
        }
    }

    private static double minmax(double[] a, boolean max) {
        double v = a[0];
        for (int i = 1; i < a.length; i++) {
            if (max) {
                if (a[i] > v) {
                    v = a[i];
                }
            } else if (a[i] < v) {
                v = a[i];
            }
        }
        return v;
    }

    private static double round(double[] a) {
        if (a.length == 1) {
            return Math.rint(a[0]);
        }
        double p = Math.pow(10, a[1]);
        return Math.rint(a[0] * p) / p;
    }

    private static double clamp(double v, double lo, double hi) {
        if (lo > hi) {
            double s = lo;
            lo = hi;
            hi = s;
        }
        if (v < lo) {
            return lo;
        }
        if (v > hi) {
            return hi;
        }
        return v;
    }

    private double scale(Expr at, double[] a) {
        double span = a[2] - a[1];
        if (span == 0) {
            throw fail(at, "SCALE input span is zero");
        }
        double t = (a[0] - a[1]) / span;
        return a[3] + t * (a[4] - a[3]);
    }

    private double table(Expr at, double[] a) {
        double x = a[0];
        int pairs = (a.length - 1) / 2;
        double x0 = a[1];
        double y0 = a[2];
        if (x <= x0) {
            return y0;
        }
        for (int i = 1; i < pairs; i++) {
            double x1 = a[1 + i * 2];
            double y1 = a[2 + i * 2];
            if (x <= x1) {
                double dx = x1 - x0;
                if (dx == 0) {
                    return y1;
                }
                return y0 + (x - x0) * (y1 - y0) / dx;
            }
            x0 = x1;
            y0 = y1;
        }
        return y0;
    }

    private double tbl(Expr at, double x, int index) {
        double[][] t = rt.tables.get(Integer.valueOf(index));
        if (t == null) {
            throw fail(at, "Table " + index + " is empty");
        }
        double[] packed = new double[1 + t[0].length * 2];
        packed[0] = x;
        for (int i = 0; i < t[0].length; i++) {
            packed[1 + i * 2] = t[0][i];
            packed[2 + i * 2] = t[1][i];
        }
        return table(at, packed);
    }

    private double requirePositive(Expr at, double x, double value) {
        if (x <= 0) {
            throw fail(at, "Logarithm of a non-positive number");
        }
        return value;
    }

    /** Magnus dew point, degrees Celsius, relative humidity in percent. */
    static double dewC(double tempC, double rh) {
        double bounded = rh;
        if (bounded < 0.1) {
            bounded = 0.1;
        }
        if (bounded > 100) {
            bounded = 100;
        }
        double a = 17.625;
        double b = 243.04;
        double gamma = Math.log(bounded / 100.0) + (a * tempC) / (b + tempC);
        return (b * gamma) / (a - gamma);
    }

    /** Moist-air enthalpy, kJ/kg dry air, at 101.325 kPa. */
    static double enthalpy(double tempC, double rh) {
        double es = 0.61094 * Math.exp((17.625 * tempC) / (tempC + 243.04));
        double e = Math.max(0, Math.min(100, rh)) / 100.0 * es;
        double p = 101.325;
        if (e >= p) {
            e = p * 0.99;
        }
        double w = 0.62198 * e / (p - e);
        return 1.006 * tempC + w * (2501 + 1.86 * tempC);
    }

    static Fail fail(Expr e, String message) {
        int line = e == null ? 0 : e.line;
        int column = e == null ? 0 : e.column;
        return new Fail(line, column, message);
    }

    static final class Fail extends RuntimeException {
        final int line;
        final int column;

        Fail(int line, int column, String message) {
            super(message);
            this.line = line;
            this.column = column;
        }
    }

}

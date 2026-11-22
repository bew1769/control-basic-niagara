package com.controlbasic.core;

import java.util.Locale;

/** Built-in function names and arity checks. Evaluation is in {@link Evaluator}. */
final class Functions {
    private Functions() {
    }

    static String canon(String name) {
        return name == null ? "" : name.toUpperCase(Locale.ROOT);
    }

    static boolean isFunction(String name) {
        return arityError(canon(name), -1) == null || known(canon(name));
    }

    static boolean known(String name) {
        switch (name) {
            case "ABS":
            case "INT":
            case "FIX":
            case "SQR":
            case "SQRT":
            case "EXP":
            case "LN":
            case "LOG":
            case "LOG10":
            case "SIN":
            case "COS":
            case "TAN":
            case "ATN":
            case "ARCTAN":
            case "ARCSIN":
            case "ARCCOS":
            case "SGN":
            case "FRAC":
            case "INVLN":
            case "DEG":
            case "RAD":
            case "MIN":
            case "MAX":
            case "AVG":
            case "ISNAN":
            case "ROUND":
            case "MOD":
            case "LIMIT":
            case "CLAMP":
            case "SCALE":
            case "TABLE":
            case "TBL":
            case "TIMEON":
            case "TIMEOFF":
            case "SENSORON":
            case "SENSOROFF":
            case "RND":
            case "TIME":
            case "DOW":
            case "DOM":
            case "DOY":
            case "MONTH":
            case "YEAR":
            case "SCAN":
            case "SCANS":
            case "SCANCOUNT":
            case "ERRORLEVEL":
            case "FIRST":
            case "INTERVAL":
            case "PID":
            case "DEWPOINT":
            case "DEWPOINTSI":
            case "ENTHALPY":
            case "ENTHALPYSI":
                return true;
            default:
                return false;
        }
    }

    /** {@code argc < 0} only asks whether the name is a function. */
    static String arityError(String name, int argc) {
        name = canon(name);
        if (!known(name)) {
            return argc < 0 ? "unknown" : null;
        }
        if (argc < 0) {
            return null;
        }
        switch (name) {
            case "MIN":
            case "MAX":
                if (argc < 2) {
                    return name + " expects at least 2 arguments";
                }
                return null;
            case "AVG":
                if (argc < 1) {
                    return "AVG expects at least 1 argument";
                }
                return null;
            case "ROUND":
            case "RND":
                if (name.equals("ROUND") && (argc < 1 || argc > 2)) {
                    return "ROUND expects 1 or 2 arguments";
                }
                if (name.equals("RND") && argc > 1) {
                    return "RND expects 0 or 1 arguments";
                }
                return null;
            case "TABLE":
                if (argc < 5 || (argc % 2) == 0) {
                    return "TABLE expects x, x1, y1, x2, y2, ...";
                }
                return null;
            case "PID":
                if (argc != 5 && argc != 7) {
                    return "PID expects 5 or 7 arguments";
                }
                return null;
            case "TIME":
            case "DOW":
            case "DOM":
            case "DOY":
            case "MONTH":
            case "YEAR":
            case "SCAN":
            case "SCANS":
            case "SCANCOUNT":
            case "ERRORLEVEL":
            case "FIRST":
                if (argc != 0) {
                    return name + " does not take arguments";
                }
                return null;
            default:
                int expect = expected(name);
                if (expect >= 0 && argc != expect) {
                    return name + " expects " + expect + " argument" + (expect == 1 ? "" : "s");
                }
                return null;
        }
    }

    private static int expected(String name) {
        switch (name) {
            case "MOD":
            case "TBL":
            case "DEWPOINT":
            case "DEWPOINTSI":
            case "ENTHALPY":
            case "ENTHALPYSI":
                return 2;
            case "LIMIT":
            case "CLAMP":
                return 3;
            case "SCALE":
                return 5;
            case "INTERVAL":
            case "ISNAN":
            case "ABS":
            case "INT":
            case "FIX":
            case "SQR":
            case "SQRT":
            case "EXP":
            case "LN":
            case "LOG":
            case "LOG10":
            case "SIN":
            case "COS":
            case "TAN":
            case "ATN":
            case "ARCTAN":
            case "ARCSIN":
            case "ARCCOS":
            case "SGN":
            case "FRAC":
            case "INVLN":
            case "DEG":
            case "RAD":
            case "TIMEON":
            case "TIMEOFF":
            case "SENSORON":
            case "SENSOROFF":
                return 1;
            default:
                return -1;
        }
    }

    static boolean isNoArg(String name) {
        switch (canon(name)) {
            case "TIME":
            case "DOW":
            case "DOM":
            case "DOY":
            case "MONTH":
            case "YEAR":
            case "SCAN":
            case "SCANS":
            case "SCANCOUNT":
            case "ERRORLEVEL":
            case "FIRST":
                return true;
            default:
                return false;
        }
    }

    static boolean isConstant(String name) {
        switch (canon(name)) {
            case "HOURS":
            case "HOUR":
            case "MINUTES":
            case "SECONDS":
            case "SECOND":
            case "PI":
            case "TRUE":
            case "FALSE":
            case "YES":
            case "NO":
            case "NAN":
            case "POS_INFINITY":
            case "NEG_INFINITY":
            case "MON":
            case "TUE":
            case "WED":
            case "THU":
            case "FRI":
            case "SAT":
            case "SUN":
                return true;
            default:
                return false;
        }
    }

    static double constant(String name) {
        switch (canon(name)) {
            case "HOURS":
            case "HOUR":
                return 3600;
            case "MINUTES":
                return 60;
            case "SECONDS":
            case "SECOND":
                return 1;
            case "PI":
                return Math.PI;
            case "TRUE":
            case "YES":
                return 1;
            case "FALSE":
            case "NO":
                return 0;
            case "NAN":
                return Double.NaN;
            case "POS_INFINITY":
                return Double.POSITIVE_INFINITY;
            case "NEG_INFINITY":
                return Double.NEGATIVE_INFINITY;
            // KMC: Monday is day 1, Sunday is day 7 (KMC BACnet Module Rev E, DOW).
            case "MON":
                return 1;
            case "TUE":
                return 2;
            case "WED":
                return 3;
            case "THU":
                return 4;
            case "FRI":
                return 5;
            case "SAT":
                return 6;
            case "SUN":
                return 7;
            default:
                return 0;
        }
    }

    static boolean isReserved(String name) {
        String n = canon(name);
        return isFunction(n) || isConstant(n) || isNoArg(n);
    }
}

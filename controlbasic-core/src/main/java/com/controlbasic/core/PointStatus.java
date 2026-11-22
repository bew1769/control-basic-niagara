package com.controlbasic.core;

import java.util.Locale;

/**
 * Point status bits. The values are Niagara's {@code javax.baja.status.BStatus}
 * bits so the block can pass them through unchanged; the core has no Niagara
 * dependency.
 *
 * <p>KMC Control Basic reads BACnet objects, whose Status Flags are IN ALARM,
 * FAULT, OVERRIDDEN and OUT OF SERVICE (KMC Connect Reference, Rev N, Table
 * 35-5). A program reads them here as {@code point.IN_ALARM},
 * {@code point.FAULT}, {@code point.OVERRIDDEN} and
 * {@code point.OUT_OF_SERVICE}; the Niagara-only flags are available too.
 */
public final class PointStatus {
    public static final int OK = 0;
    public static final int DISABLED = 0x01;
    public static final int FAULT = 0x02;
    public static final int DOWN = 0x04;
    public static final int ALARM = 0x08;
    public static final int STALE = 0x10;
    public static final int OVERRIDDEN = 0x20;
    public static final int NULL = 0x40;
    public static final int UNACKED_ALARM = 0x80;

    private PointStatus() {
    }

    /**
     * Bit mask a {@code .PROPERTY} suffix tests, -1 for {@code .STATUS} (the
     * raw bits), -2 for {@code .PV} (the value), or 0 when unknown.
     */
    static int propertyMask(String property) {
        switch (property.toUpperCase(Locale.ROOT)) {
            case "PV":
            case "PRESENT_VALUE":
                return -2;
            case "STATUS":
                return -1;
            case "IN_ALARM":
            case "ALARM":
                return ALARM | UNACKED_ALARM;
            case "FAULT":
                return FAULT;
            case "OVERRIDDEN":
                return OVERRIDDEN;
            case "OUT_OF_SERVICE":
            case "OOS":
            case "DISABLED":
                return DISABLED;
            case "DOWN":
                return DOWN;
            case "STALE":
                return STALE;
            case "NULL":
                return NULL;
            default:
                return 0;
        }
    }

    /** Readable flags, for messages and tests: "{fault,overridden}". */
    public static String describe(int bits) {
        if (bits == 0) {
            return "{ok}";
        }
        StringBuilder sb = new StringBuilder("{");
        String[] names = {"disabled", "fault", "down", "alarm", "stale", "overridden", "null", "unackedAlarm"};
        for (int i = 0; i < names.length; i++) {
            if ((bits & (1 << i)) != 0) {
                if (sb.length() > 1) {
                    sb.append(',');
                }
                sb.append(names[i]);
            }
        }
        return sb.append('}').toString();
    }
}

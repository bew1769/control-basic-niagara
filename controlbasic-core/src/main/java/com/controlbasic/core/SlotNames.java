package com.controlbasic.core;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wire-sheet names. A point is a KMC object address plus an optional SEBA
 * descriptor: {@code AI1} or {@code AI1_RmTmp}. See docs/LANGUAGE.md.
 */
public final class SlotNames {
    /**
     * Prefix, instance, optional descriptor. The descriptor is camel-case
     * SEBA text. An underscore inside it is only the enumeration form
     * {@code CO2_1}.
     */
    private static final Pattern POINT = Pattern.compile(
            "^(AI|AO|AV|BI|BO|BV)(\\d+)(?:_([A-Za-z][A-Za-z0-9]*(?:_\\d+)*))?$",
            Pattern.CASE_INSENSITIVE);

    private SlotNames() {
    }

    public static final class Point {
        public final String prefix;
        public final int number;
        public final String descriptor;
        public final SlotDirection direction;
        public final SlotDataType dataType;

        Point(String prefix, int number, String descriptor, SlotDirection direction, SlotDataType dataType) {
            this.prefix = prefix;
            this.number = number;
            this.descriptor = descriptor;
            this.direction = direction;
            this.dataType = dataType;
        }
    }

    /** @return null when {@code spelling} is an internal variable, not a point */
    public static Point parse(String spelling) {
        if (spelling == null) {
            return null;
        }
        Matcher matcher = POINT.matcher(spelling);
        if (!matcher.matches()) {
            return null;
        }
        String prefix = matcher.group(1).toUpperCase(Locale.ROOT);
        int number;
        try {
            number = Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException ex) {
            return null;
        }
        String descriptor = matcher.group(3);
        SlotDirection direction;
        SlotDataType type;
        if ("AI".equals(prefix)) {
            direction = SlotDirection.INPUT;
            type = SlotDataType.NUMERIC;
        } else if ("AO".equals(prefix)) {
            direction = SlotDirection.OUTPUT;
            type = SlotDataType.NUMERIC;
        } else if ("AV".equals(prefix)) {
            direction = SlotDirection.VARIABLE;
            type = SlotDataType.NUMERIC;
        } else if ("BI".equals(prefix)) {
            direction = SlotDirection.INPUT;
            type = SlotDataType.BOOLEAN;
        } else if ("BO".equals(prefix)) {
            direction = SlotDirection.OUTPUT;
            type = SlotDataType.BOOLEAN;
        } else if ("BV".equals(prefix)) {
            direction = SlotDirection.VARIABLE;
            type = SlotDataType.BOOLEAN;
        } else {
            return null;
        }
        return new Point(prefix, number, descriptor, direction, type);
    }

    /**
     * Niagara slot escape, the same rules as {@code javax.baja.naming.SlotPath.escape}
     * and {@code TextUtil}: letters, digits, and underscore, and the first character
     * must be a letter or underscore. Anything else is {@code $} plus two lowercase
     * hex digits, or {@code $u} plus four hex digits above Latin-1.
     * A name that is already valid is returned unchanged.
     */
    public static String escape(String name) {
        if (name == null || name.length() == 0) {
            return "";
        }
        if (isValid(name)) {
            return name;
        }
        StringBuilder out = new StringBuilder(name.length() + 8);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (isValidChar(c, i == 0)) {
                out.append(c);
                continue;
            }
            if (c <= 0xFF) {
                out.append('$');
                String hex = Integer.toHexString(c);
                if (hex.length() == 1) {
                    out.append('0');
                }
                out.append(hex);
            } else {
                out.append("$u");
                String hex = Integer.toHexString(c);
                while (hex.length() < 4) {
                    hex = "0" + hex;
                }
                out.append(hex);
            }
        }
        return out.toString();
    }

    public static boolean isValid(String name) {
        if (name == null || name.length() == 0) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            if (!isValidChar(name.charAt(i), i == 0)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidChar(char c, boolean first) {
        if (c >= 'A' && c <= 'Z') {
            return true;
        }
        if (c >= 'a' && c <= 'z') {
            return true;
        }
        if (c == '_') {
            return true;
        }
        return !first && c >= '0' && c <= '9';
    }
}

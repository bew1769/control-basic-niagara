package javax.baja.util;

import javax.baja.sys.BValue;

/** Compile-only stand-in. The real class lives in baja.jar. */
public final class BFormat extends BValue {
    public static BFormat make(String pattern) {
        return new BFormat();
    }
}

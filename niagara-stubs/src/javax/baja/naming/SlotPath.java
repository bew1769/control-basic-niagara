package javax.baja.naming;

/**
 * Compile-only stand-in. The real class lives in baja.jar.
 * Escape rules match {@code com.controlbasic.core.SlotNames}.
 */
public final class SlotPath {
    private SlotPath() {
    }

    public static String escape(String name) {
        return com.controlbasic.core.SlotNames.escape(name);
    }
}

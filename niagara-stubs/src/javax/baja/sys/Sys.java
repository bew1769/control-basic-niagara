package javax.baja.sys;

public final class Sys {
    private Sys() {
    }

    public static boolean atSteadyState() {
        return true;
    }

    public static Type loadType(Class<?> type) {
        return new Type();
    }
}

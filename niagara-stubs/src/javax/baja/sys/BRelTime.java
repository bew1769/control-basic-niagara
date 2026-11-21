package javax.baja.sys;

public class BRelTime extends BValue {
    private final long millis;

    public BRelTime(long millis) {
        this.millis = millis;
    }

    public static BRelTime makeSeconds(int seconds) {
        return new BRelTime(seconds * 1000L);
    }

    public static BRelTime make(long millis) {
        return new BRelTime(millis);
    }

    public long getMillis() {
        return millis;
    }
}

package javax.baja.sys;

public final class BBoolean extends BValue {
    public static final BBoolean TRUE = new BBoolean(true);
    public static final BBoolean FALSE = new BBoolean(false);

    private final boolean value;

    private BBoolean(boolean value) {
        this.value = value;
    }

    public boolean getBoolean() {
        return value;
    }
}

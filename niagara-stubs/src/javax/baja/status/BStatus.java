package javax.baja.status;

import javax.baja.sys.BValue;

/** Compile-only stand-in. The real class lives in baja.jar. */
public final class BStatus extends BValue {
    public static final int DISABLED = 0x01;
    public static final int FAULT = 0x02;
    public static final int DOWN = 0x04;
    public static final int ALARM = 0x08;
    public static final int STALE = 0x10;
    public static final int OVERRIDDEN = 0x20;
    public static final int NULL = 0x40;
    public static final int UNACKED_ALARM = 0x80;

    public static final BStatus ok = new BStatus(0);
    public static final BStatus fault = new BStatus(FAULT);
    public static final BStatus disabled = new BStatus(DISABLED);
    public static final BStatus nullStatus = new BStatus(NULL);

    private final int bits;

    private BStatus(int bits) {
        this.bits = bits;
    }

    public static BStatus make(int bits) {
        return new BStatus(bits);
    }

    public int getBits() {
        return bits;
    }
}

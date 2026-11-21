package javax.baja.sys;

public final class Clock {
    private Clock() {
    }

    public interface Ticket {
        void cancel();
    }

    public static Ticket schedulePeriodically(BRelTime interval, Runnable callback, BComponent worker) {
        return new Ticket() {
            @Override
            public void cancel() {
            }
        };
    }
}

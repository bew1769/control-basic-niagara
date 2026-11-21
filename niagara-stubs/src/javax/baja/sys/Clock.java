package javax.baja.sys;

public final class Clock {
    private Clock() {
    }

    public interface Ticket {
        void cancel();
    }

    /**
     * Niagara 4.15 shape. There is no {@code Runnable} overload on {@code Clock}.
     * The block's hidden {@code scanTick} action is the callback.
     */
    public static Ticket schedulePeriodically(BComponent worker, BRelTime interval, Action action, BValue arg) {
        return new Ticket() {
            @Override
            public void cancel() {
            }
        };
    }
}

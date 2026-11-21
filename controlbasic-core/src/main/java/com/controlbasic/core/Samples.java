package com.controlbasic.core;

/** Programs used as the component default and as test fixtures. */
public final class Samples {
    private Samples() {
    }

    public static final String DAMPER =
            "REM Scaled damper. Fan proves on, then the damper is capped.\n"
                    + "10 IF FIRST THEN VAR1 = 72\n"
                    + "20 AO1 = LIMIT(SCALE(AI1, 68, 76, 0, 100), 0, 100)\n"
                    + "30 IF AI1 > VAR1 THEN START BO1 ELSE STOP BO1\n"
                    + "40 IF TIMEON(BO1) > 2 * MINUTES THEN AO1 = MIN(AO1, 50)\n"
                    + "50 END\n";
}

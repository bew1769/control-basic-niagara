package com.controlbasic.core;

/** Programs used as the component default and as test fixtures. */
public final class Samples {
    private Samples() {
    }

    public static final String DAMPER =
            "REM Scaled damper. Fan proves on, then the damper command is capped.\n"
                    + "10 IF FIRST THEN AV1_ZnTmpSpt = 72\n"
                    + "20 AO1_DprCmd = LIMIT(SCALE(AI1_RmTmp, 68, 76, 0, 100), 0, 100)\n"
                    + "30 IF AI1_RmTmp > AV1_ZnTmpSpt THEN START BO1_FanCmd ELSE STOP BO1_FanCmd\n"
                    + "40 IF TIMEON(BO1_FanCmd) > 2 * MINUTES THEN AO1_DprCmd = MIN(AO1_DprCmd, 50)\n"
                    + "50 END\n";
}

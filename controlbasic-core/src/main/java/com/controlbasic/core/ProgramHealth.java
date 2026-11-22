package com.controlbasic.core;

/**
 * The block's own health, modelled on the KMC / BACnet Program object
 * (KMC Connect Reference Rev N, section 35, Program object): Program State,
 * Reliability, Description Of Halt, and the FAULT status flag, which is set
 * whenever Reliability is not NO_FAULT_DETECTED.
 *
 * <p>The block feeds it the result of every compile and scan. It reports the
 * Niagara status bits for the block and whether the fault alarm has to be
 * raised, re-raised with a new message, or returned to normal. It has no
 * Niagara dependency so it can be unit tested.
 */
public final class ProgramHealth {
    /** BACnet Program State values used here (Connect Rev N, Table 35-20). */
    public enum State { IDLE, RUNNING, HALTED }

    /** BACnet Reliability values used here (Connect Rev N, Program object). */
    public enum Reliability { NO_FAULT_DETECTED, PROCESS_ERROR }

    /** What the block must do with its fault alarm after an update. */
    public enum Transition { NONE, FAULT_RAISED, FAULT_CHANGED, FAULT_CLEARED }

    private State state = State.IDLE;
    private Reliability reliability = Reliability.NO_FAULT_DETECTED;
    private String fault;
    private boolean enabled = true;

    /**
     * @param enabled      the block's Enabled property
     * @param active       compile state (last good program and latest compile error)
     * @param scanError    the last scan's runtime error, already formatted, or null
     * @param haltMessage  non-null when the program has executed HALT ("" when HALT had no message)
     */
    public synchronized Transition update(boolean enabled, ActiveProgram active, String scanError, String haltMessage) {
        this.enabled = enabled;
        String next;
        if (!enabled) {
            state = State.IDLE;
            next = null;
        } else {
            String compileError = active == null ? null : active.compileError();
            boolean hasProgram = active != null && active.running() != null;
            if (haltMessage != null && hasProgram) {
                state = State.HALTED;
            } else {
                state = hasProgram ? State.RUNNING : State.IDLE;
            }
            if (compileError != null) {
                next = "Compile error: " + compileError + (hasProgram ? ActiveProgram.STILL_RUNNING : "");
            } else if (haltMessage != null && hasProgram) {
                next = haltMessage.length() == 0 ? "Halted by HALT" : "Halted: " + haltMessage;
            } else if (scanError != null && hasProgram) {
                next = "Runtime error: " + scanError;
            } else {
                next = null;
            }
        }
        reliability = next == null ? Reliability.NO_FAULT_DETECTED : Reliability.PROCESS_ERROR;
        String before = fault;
        fault = next;
        if (before == null && next != null) {
            return Transition.FAULT_RAISED;
        }
        if (before != null && next == null) {
            return Transition.FAULT_CLEARED;
        }
        if (before != null && !before.equals(next)) {
            return Transition.FAULT_CHANGED;
        }
        return Transition.NONE;
    }

    public synchronized State state() {
        return state;
    }

    public synchronized Reliability reliability() {
        return reliability;
    }

    /** The current fault text (the alarm message), or null when healthy. */
    public synchronized String fault() {
        return fault;
    }

    /** Niagara status bits for the block itself. */
    public synchronized int statusBits() {
        if (!enabled) {
            return PointStatus.DISABLED;
        }
        return fault == null ? PointStatus.OK : PointStatus.FAULT;
    }
}

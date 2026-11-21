package com.controlbasic.core;

/** Direction of a linkable slot created from a program. */
public enum SlotDirection {
    /** Read by the program. Links write this slot. The program does not publish it. */
    INPUT,
    /** Written by the program. Shown on the wire sheet for outbound links. */
    OUTPUT,
    /** Read and written. Seeded from the slot each scan and published back. */
    VARIABLE
}

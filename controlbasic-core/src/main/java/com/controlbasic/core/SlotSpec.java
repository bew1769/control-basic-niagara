package com.controlbasic.core;

/**
 * One dynamic slot the program needs. {@code name} keeps the first spelling
 * from the source. {@code canonical} is the case-insensitive key.
 */
public final class SlotSpec {
    /** Program spelling, first seen in the source. */
    public final String name;
    /** Case-insensitive key. The runtime reads and writes points with this. */
    public final String canonical;
    /** Name passed to {@code SlotPath.escape} / {@link SlotNames#escape}. */
    public final String niagaraName;
    public final SlotDirection direction;
    public final SlotDataType dataType;

    public SlotSpec(String name, SlotDirection direction, SlotDataType dataType) {
        this.name = name;
        this.canonical = Functions.canon(name);
        this.niagaraName = SlotNames.escape(name);
        this.direction = direction;
        this.dataType = dataType;
    }

    @Override
    public String toString() {
        return name + " " + direction + " " + dataType;
    }
}

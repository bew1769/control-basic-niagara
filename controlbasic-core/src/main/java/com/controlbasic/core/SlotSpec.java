package com.controlbasic.core;

/**
 * One dynamic slot the program needs. {@code name} keeps the first spelling
 * from the source. {@code canonical} is the case-insensitive key.
 */
public final class SlotSpec {
    public final String name;
    public final String canonical;
    public final SlotDirection direction;
    public final SlotDataType dataType;

    public SlotSpec(String name, SlotDirection direction, SlotDataType dataType) {
        this.name = name;
        this.canonical = Functions.canon(name);
        this.direction = direction;
        this.dataType = dataType;
    }

    @Override
    public String toString() {
        return name + " " + direction + " " + dataType;
    }
}

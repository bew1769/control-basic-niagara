package com.controlbasic.core;

/**
 * Where linkable point values live during a scan. The Niagara block
 * implements this with dynamic slots. Tests use a map.
 */
public interface PointImage {
    double get(String canonicalName);

    void set(String canonicalName, double value);

    /** {@link PointStatus} bits of an input slot. */
    default int status(String canonicalName) {
        return PointStatus.OK;
    }

    /** Sets the {@link PointStatus} bits of an output or variable slot. */
    default void setStatus(String canonicalName, int bits) {
    }
}

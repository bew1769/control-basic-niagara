package com.controlbasic.core;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Clock the interpreter reads. Tests supply a fixed or stepped clock. */
public interface TimeSource {
    long nowMillis();

    ZonedDateTime now();

    TimeSource SYSTEM = new TimeSource() {
        @Override
        public long nowMillis() {
            return System.currentTimeMillis();
        }

        @Override
        public ZonedDateTime now() {
            return ZonedDateTime.now(ZoneId.systemDefault());
        }
    };
}

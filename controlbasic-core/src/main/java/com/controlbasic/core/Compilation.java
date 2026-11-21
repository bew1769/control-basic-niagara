package com.controlbasic.core;

import java.util.Collections;
import java.util.List;

/** A compiled program, or the diagnostics that stopped it compiling. */
public final class Compilation {
    private final Instr[] code;
    private final List<SlotSpec> slots;
    private final List<Diagnostic> diagnostics;

    Compilation(Instr[] code, List<SlotSpec> slots, List<Diagnostic> diagnostics) {
        this.code = code == null ? new Instr[0] : code;
        this.slots = slots == null ? Collections.<SlotSpec>emptyList() : slots;
        this.diagnostics = diagnostics == null ? Collections.<Diagnostic>emptyList() : diagnostics;
    }

    public boolean ok() {
        return diagnostics.isEmpty();
    }

    public List<Diagnostic> diagnostics() {
        return diagnostics;
    }

    public List<SlotSpec> slots() {
        return slots;
    }

    Instr[] code() {
        return code;
    }
}

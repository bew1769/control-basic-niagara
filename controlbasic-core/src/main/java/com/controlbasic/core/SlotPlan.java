package com.controlbasic.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Diff between the slots a component already has and the slots a program
 * needs. Kept slots are omitted so existing links stay put.
 */
public final class SlotPlan {
    public final List<String> remove;
    public final List<SlotSpec> add;

    public SlotPlan(List<String> remove, List<SlotSpec> add) {
        this.remove = remove;
        this.add = add;
    }

    public static final class Existing {
        public final String name;
        public final String canonical;
        public final boolean boolType;
        public final boolean managed;

        public Existing(String name, boolean boolType, boolean managed) {
            this.name = name;
            this.canonical = Functions.canon(name);
            this.boolType = boolType;
            this.managed = managed;
        }
    }

    public static SlotPlan diff(List<Existing> existing, List<SlotSpec> desired) {
        List<String> remove = new ArrayList<String>();
        List<SlotSpec> add = new ArrayList<SlotSpec>();
        List<Existing> managed = new ArrayList<Existing>();
        if (existing != null) {
            for (int i = 0; i < existing.size(); i++) {
                Existing slot = existing.get(i);
                if (slot.managed) {
                    managed.add(slot);
                }
            }
        }
        if (desired == null) {
            desired = new ArrayList<SlotSpec>();
        }
        for (int i = 0; i < managed.size(); i++) {
            Existing slot = managed.get(i);
            SlotSpec spec = find(desired, slot.canonical);
            if (spec == null || spec.dataType == SlotDataType.BOOLEAN != slot.boolType) {
                remove.add(slot.name);
            }
        }
        for (int i = 0; i < desired.size(); i++) {
            SlotSpec spec = desired.get(i);
            Existing slot = findExisting(managed, spec.canonical);
            if (slot == null || slot.boolType != (spec.dataType == SlotDataType.BOOLEAN)) {
                add.add(spec);
            }
        }
        return new SlotPlan(remove, add);
    }

    private static SlotSpec find(List<SlotSpec> desired, String canonical) {
        for (int i = 0; i < desired.size(); i++) {
            if (desired.get(i).canonical.equals(canonical)) {
                return desired.get(i);
            }
        }
        return null;
    }

    private static Existing findExisting(List<Existing> managed, String canonical) {
        for (int i = 0; i < managed.size(); i++) {
            if (managed.get(i).canonical.equals(canonical)) {
                return managed.get(i);
            }
        }
        return null;
    }
}

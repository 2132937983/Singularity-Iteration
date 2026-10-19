// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.reactor.audit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * 0.1.10.12: the flight recorder of one nuclear reactor. The reactor writes one {@link Sample} per
 * 20-tick cycle (thermal balance, coolant, layout counts, columns, events) and the layout after the
 * cycle; the recorder keeps the last {@value #SAMPLES} cycles (40 seconds) and the components that
 * melted in that time. It lives only in memory (it starts empty after a load). When the reactor fails
 * or is removed while hot, {@link ReactorIncidents} turns it into a saved incident.
 */
public final class ReactorBlackbox {
    public static final int SAMPLES = 40;
    /** Slot flags. */
    public static final int FLAG_MELTED = 1, FLAG_EXPIRED = 2, FLAG_DEPLETED = 4, FLAG_OUTSIDE = 8;

    /**
     * One cycle. Heat in HU per cycle; {@code emitted} is the heat that vents moved into the air (or the
     * coolant), {@code excess} the extra hull heat of a feed rate above 1. {@code unloadedNeighbours}:
     * neighbour positions (of 6) whose chunk was not loaded (a chamber there cannot count).
     */
    public record Sample(long gameTime, long hullBefore, long hullAfter, long maxHull, long generated, long emitted,
                         long excess, double euPerTick, int feeds, int columns, int unloadedNeighbours, boolean enabled,
                         boolean fluidMode, int coldIn, int hotOut, int fluidCapacity, int converted,
                         long coolBudget, long coolUsed, int coolCold, int coolHot,
                         long componentHeat, long componentCapacity, int fuelRods, int coolingParts,
                         int melted, int expired, int dropped, int automationIn, int automationOut) {
        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putLong("T", gameTime); t.putLong("Hb", hullBefore); t.putLong("Ha", hullAfter); t.putLong("Hm", maxHull);
            t.putLong("G", generated); t.putLong("E", emitted); t.putLong("X", excess); t.putDouble("Eu", euPerTick);
            t.putInt("F", feeds); t.putInt("C", columns); t.putInt("U", unloadedNeighbours); t.putBoolean("On", enabled);
            t.putBoolean("Fl", fluidMode); t.putInt("Ci", coldIn); t.putInt("Ho", hotOut); t.putInt("Fc", fluidCapacity);
            t.putInt("Cv", converted); t.putLong("Lb", coolBudget); t.putLong("Lu", coolUsed); t.putInt("Lc", coolCold);
            t.putInt("Lh", coolHot); t.putLong("Ph", componentHeat); t.putLong("Pc", componentCapacity);
            t.putInt("Fr", fuelRods); t.putInt("Cp", coolingParts); t.putInt("Me", melted); t.putInt("Ex", expired);
            t.putInt("Dr", dropped); t.putInt("Ai", automationIn); t.putInt("Ao", automationOut);
            return t;
        }

        static Sample load(CompoundTag t) {
            return new Sample(t.getLong("T"), t.getLong("Hb"), t.getLong("Ha"), t.getLong("Hm"), t.getLong("G"), t.getLong("E"),
                t.getLong("X"), t.getDouble("Eu"), t.getInt("F"), t.getInt("C"), t.getInt("U"), t.getBoolean("On"),
                t.getBoolean("Fl"), t.getInt("Ci"), t.getInt("Ho"), t.getInt("Fc"), t.getInt("Cv"), t.getLong("Lb"),
                t.getLong("Lu"), t.getInt("Lc"), t.getInt("Lh"), t.getLong("Ph"), t.getLong("Pc"), t.getInt("Fr"),
                t.getInt("Cp"), t.getInt("Me"), t.getInt("Ex"), t.getInt("Dr"), t.getInt("Ai"), t.getInt("Ao"));
        }
    }

    /**
     * One reactor slot: item id, component kind ({@code ReactorCycle.Kind} ordinal, -1 none), stored heat
     * or wear and its capacity, remaining and maximum fuel cycles, {@code FLAG_*} flags.
     */
    public record Slot(String item, int kind, int stored, int capacity, int remaining, int maxRemaining, int flags) {
        public static final Slot EMPTY = new Slot("", -1, 0, 0, 0, 0, 0);
        public boolean empty() { return item.isEmpty(); }
        public Slot withFlags(int more) { return new Slot(item, kind, stored, capacity, remaining, maxRemaining, flags | more); }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            if (empty() && flags == 0) return t;
            t.putString("I", item); t.putInt("K", kind); t.putInt("S", stored); t.putInt("C", capacity);
            t.putInt("R", remaining); t.putInt("M", maxRemaining); t.putInt("F", flags);
            return t;
        }

        static Slot load(CompoundTag t) {
            if (!t.contains("I", Tag.TAG_STRING)) return EMPTY;
            return new Slot(t.getString("I"), t.getInt("K"), t.getInt("S"), t.getInt("C"), t.getInt("R"), t.getInt("M"), t.getInt("F"));
        }

        /** The slot of {@code stack} with its cycle state {@code part} (null: not a reactor component). */
        public static Slot of(net.minecraft.world.item.ItemStack stack, com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Part part) {
            if (stack.isEmpty()) return EMPTY;
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            int flags = id.contains("deplete") ? FLAG_DEPLETED : 0;
            if (part == null) return new Slot(id, -1, 0, 0, 0, 0, flags);
            int max = 0;
            if (part.profile().kind() == com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Kind.FUEL) {
                var durability = stack.get(com.miophas.singularity_iteration.common.registry.mio_icif_data_components.FUEL_ROD_DURABILITY.get());
                max = durability == null ? 0 : durability.maxUses();
            }
            return new Slot(id, part.profile().kind().ordinal(), part.stored(), part.profile().capacity(), part.remaining(), max, flags);
        }
    }

    /** A component that melted (or a reflector that wore out): the slot, the component before it went, the time. */
    public record Melt(int slot, Slot component, long gameTime) {
        CompoundTag save() { CompoundTag t = component.save(); t.putInt("Slot", slot); t.putLong("T", gameTime); return t; }
        static Melt load(CompoundTag t) { return new Melt(t.getInt("Slot"), Slot.load(t), t.getLong("T")); }
    }

    private final ArrayDeque<Sample> samples = new ArrayDeque<>();
    private final List<Melt> melts = new ArrayList<>();
    private Slot[] layout = filled();
    private int automationIn, automationOut, dropped;

    private static Slot[] filled() { Slot[] s = new Slot[54]; java.util.Arrays.fill(s, Slot.EMPTY); return s; }

    /** Counts an item that automation put in or took out (counted into the next sample). */
    public void automation(boolean insert) { if (insert) automationIn++; else automationOut++; }
    /** Counts components dropped out of the reactor (columns lost). */
    public void dropped(int count) { dropped += count; }
    public int pendingAutomationIn() { return automationIn; }
    public int pendingAutomationOut() { return automationOut; }
    public int pendingDropped() { return dropped; }

    /** Records one cycle: the sample (with the pending counters already in it), the layout after it, its melts. */
    public void record(Sample sample, Slot[] after, List<Melt> newMelts) {
        if (samples.size() == SAMPLES) samples.removeFirst();
        samples.addLast(sample);
        layout = after.clone();
        melts.addAll(newMelts);
        long oldest = samples.getFirst().gameTime();
        melts.removeIf(m -> m.gameTime() < oldest);
        automationIn = 0; automationOut = 0; dropped = 0;
    }

    public List<Sample> samples() { return List.copyOf(samples); }
    public List<Melt> melts() { return List.copyOf(melts); }
    public Slot[] layout() { return layout.clone(); }
    public boolean empty() { return samples.isEmpty(); }

    // ------------------------------------------------------------------ NBT (for the incident)

    public static ListTag saveSamples(List<Sample> samples) {
        ListTag list = new ListTag();
        for (var s : samples) list.add(s.save());
        return list;
    }

    public static List<Sample> loadSamples(ListTag list) {
        var out = new ArrayList<Sample>();
        for (int i = 0; i < list.size(); i++) out.add(Sample.load(list.getCompound(i)));
        return out;
    }

    public static ListTag saveLayout(Slot[] layout) {
        ListTag list = new ListTag();
        for (var s : layout) list.add(s.save());
        return list;
    }

    public static Slot[] loadLayout(ListTag list) {
        Slot[] out = filled();
        for (int i = 0; i < Math.min(54, list.size()); i++) out[i] = Slot.load(list.getCompound(i));
        return out;
    }

    public static ListTag saveMelts(List<Melt> melts) {
        ListTag list = new ListTag();
        for (var m : melts) list.add(m.save());
        return list;
    }

    public static List<Melt> loadMelts(ListTag list) {
        var out = new ArrayList<Melt>();
        for (int i = 0; i < list.size(); i++) out.add(Melt.load(list.getCompound(i)));
        return out;
    }
}

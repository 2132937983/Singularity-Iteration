package com.singularity_iteration.mio_icif.Blocks.entity.slot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.singularity_iteration.mio_icif.api.machine.ISlotLayout;
import com.singularity_iteration.mio_icif.api.machine.ISlotType;

import java.util.*;

@SuppressWarnings("null")
public class SlotLayout implements ISlotLayout {

    private final ImmutableList<SlotRange> ranges;
    private final ImmutableMap<SlotType, ImmutableList<SlotRange>> rangesByType;
    private final int totalSlots;

    private SlotLayout(Builder builder) {
        ImmutableList.Builder<SlotRange> listBuilder = ImmutableList.builder();
        ImmutableMap.Builder<SlotType, ImmutableList<SlotRange>> mapBuilder = ImmutableMap.builder();
        Map<SlotType, ImmutableList.Builder<SlotRange>> typeBuilders = new EnumMap<>(SlotType.class);

        int index = 0;
        for (Builder.Entry entry : builder.entries) {
            SlotRange range = new SlotRange(entry.type, index, entry.count);
            listBuilder.add(range);
            typeBuilders.computeIfAbsent(entry.type, k -> ImmutableList.builder()).add(range);
            index += entry.count;
        }

        this.ranges = listBuilder.build();
        this.totalSlots = index;

        for (Map.Entry<SlotType, ImmutableList.Builder<SlotRange>> e : typeBuilders.entrySet()) {
            mapBuilder.put(e.getKey(), e.getValue().build());
        }
        this.rangesByType = mapBuilder.build();
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public SlotRange getRange(SlotType type) {
        ImmutableList<SlotRange> list = rangesByType.get(type);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    public ImmutableList<SlotRange> getRanges(SlotType type) {
        ImmutableList<SlotRange> list = rangesByType.get(type);
        return list != null ? list : ImmutableList.of();
    }

    public int getCount(SlotType type) {
        int total = 0;
        ImmutableList<SlotRange> list = rangesByType.get(type);
        if (list != null) {
            for (SlotRange range : list) {
                total += range.count;
            }
        }
        return total;
    }

    public int getStart(SlotType type) {
        SlotRange range = getRange(type);
        return range != null ? range.start : -1;
    }

    public SlotType getInternalSlotType(int slot) {
        return getTypeInternal(slot);
    }

    private SlotType getTypeInternal(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= totalSlots) return null;
        for (SlotRange range : ranges) {
            if (range.contains(slotIndex)) return range.type;
        }
        return null;
    }

    public boolean isType(int slot, SlotType type) {
        ImmutableList<SlotRange> list = rangesByType.get(type);
        if (list == null) return false;
        for (SlotRange range : list) {
            if (range.contains(slot)) return true;
        }
        return false;
    }

    public int[] getSlotsOfType(SlotType type) {
        ImmutableList<SlotRange> list = rangesByType.get(type);
        if (list == null || list.isEmpty()) return new int[0];
        int total = 0;
        for (SlotRange range : list) {
            total += range.count;
        }
        int[] result = new int[total];
        int i = 0;
        for (SlotRange range : list) {
            for (int j = 0; j < range.count; j++) {
                result[i++] = range.start + j;
            }
        }
        return result;
    }

    public ImmutableList<SlotRange> getAllRanges() {
        return ranges;
    }

    public boolean hasType(SlotType type) {
        return rangesByType.containsKey(type);
    }

    // ISlotLayout API convenience methods
    @Override
    public int getCount(ISlotType type) {
        if (type instanceof SlotType slotType) {
            return getCount(slotType);
        }
        return 0;
    }

    @Override
    public int[] getSlotsOfType(ISlotType type) {
        if (type instanceof SlotType slotType) {
            return getSlotsOfType(slotType);
        }
        return new int[0];
    }

    @Override
    public ISlotType getType(int slotIndex) {
        return getTypeInternal(slotIndex);
    }

    @Override
    public int getTotalCount() {
        return getTotalSlots();
    }

    @Override
    public int[] getInputSlots() {
        return getSlotsOfType(SlotType.INPUT);
    }

    @Override
    public int[] getOutputSlots() {
        return getSlotsOfType(SlotType.OUTPUT);
    }

    @Override
    public int[] getBatterySlots() {
        return getSlotsOfType(SlotType.BATTERY);
    }

    @Override
    public int[] getUpgradeSlots() {
        return getSlotsOfType(SlotType.UPGRADE);
    }

    @Override
    public int getInputCount() {
        return getCount(SlotType.INPUT);
    }

    @Override
    public int getOutputCount() {
        return getCount(SlotType.OUTPUT);
    }

    @Override
    public int getBatteryCount() {
        return getCount(SlotType.BATTERY);
    }

    @Override
    public int getUpgradeCount() {
        return getCount(SlotType.UPGRADE);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SlotLayout fromISlotLayout(ISlotLayout layout) {
        if (layout instanceof SlotLayout sl) return sl;
        Builder builder = builder();
        int inputCount = layout.getInputCount();
        if (inputCount > 0) builder.input(inputCount);
        int outputCount = layout.getOutputCount();
        if (outputCount > 0) builder.output(outputCount);
        if (layout.getBatteryCount() > 0) builder.battery();
        int upgradeCount = layout.getUpgradeCount();
        if (upgradeCount > 0) builder.upgrade(upgradeCount);
        return builder.build();
    }

    public static class Builder implements ISlotLayout.Builder {
        private final List<Entry> entries = new ArrayList<>();

        private static class Entry {
            final SlotType type;
            final int count;

            Entry(SlotType type, int count) {
                this.type = type;
                this.count = count;
            }
        }

        public Builder input(int count) {
            entries.add(new Entry(SlotType.INPUT, count));
            return this;
        }

        public Builder output(int count) {
            entries.add(new Entry(SlotType.OUTPUT, count));
            return this;
        }

        public Builder battery() {
            entries.add(new Entry(SlotType.BATTERY, 1));
            return this;
        }

        public Builder fluidInput(int count) {
            entries.add(new Entry(SlotType.FLUID_INPUT, count));
            return this;
        }

        public Builder fluidOutput(int count) {
            entries.add(new Entry(SlotType.FLUID_OUTPUT, count));
            return this;
        }

        public Builder upgrade(int count) {
            entries.add(new Entry(SlotType.UPGRADE, count));
            return this;
        }

        public Builder extra(int count) {
            entries.add(new Entry(SlotType.EXTRA, count));
            return this;
        }

        public Builder rotor() {
            entries.add(new Entry(SlotType.ROTOR, 1));
            return this;
        }

        public Builder turbine() {
            entries.add(new Entry(SlotType.TURBINE, 1));
            return this;
        }

        public Builder reactor(int count) {
            entries.add(new Entry(SlotType.REACTOR, count));
            return this;
        }

        public Builder coil() {
            return coil(1);
        }

        public Builder coil(int count) {
            entries.add(new Entry(SlotType.COIL, count));
            return this;
        }

        public Builder heating() {
            entries.add(new Entry(SlotType.HEATING, 1));
            return this;
        }

        public Builder fuel() {
            entries.add(new Entry(SlotType.FUEL, 1));
            return this;
        }

        public Builder fuel(int count) {
            entries.add(new Entry(SlotType.FUEL, count));
            return this;
        }

        public Builder memory() {
            entries.add(new Entry(SlotType.MEMORY, 1));
            return this;
        }

        public Builder tool() {
            entries.add(new Entry(SlotType.TOOL, 1));
            return this;
        }

        public Builder nuclear(int count) {
            entries.add(new Entry(SlotType.NUCLEAR, count));
            return this;
        }

        public Builder drill() {
            entries.add(new Entry(SlotType.DRILL, 1));
            return this;
        }

        public Builder scanner() {
            entries.add(new Entry(SlotType.SCANNER, 1));
            return this;
        }

        public Builder miningPipe() {
            entries.add(new Entry(SlotType.MINING_PIPE, 1));
            return this;
        }

        public Builder filter(int count) {
            entries.add(new Entry(SlotType.FILTER, count));
            return this;
        }

        public Builder heatConductor() {
            return heatConductor(1);
        }

        public Builder heatConductor(int count) {
            entries.add(new Entry(SlotType.HEAT_CONDUCTOR, count));
            return this;
        }

        public Builder rtgPellet() {
            entries.add(new Entry(SlotType.RTG_PELLET, 1));
            return this;
        }

        public Builder rtgPellet(int count) {
            entries.add(new Entry(SlotType.RTG_PELLET, count));
            return this;
        }

        public SlotLayout build() {
            return new SlotLayout(this);
        }
    }

    public static class SlotRange {
        public final SlotType type;
        public final int start;
        public final int count;

        public SlotRange(SlotType type, int start, int count) {
            this.type = type;
            this.start = start;
            this.count = count;
        }

        public boolean contains(int slot) {
            return slot >= start && slot < start + count;
        }

        public int get(int index) {
            if (index < 0 || index >= count) return -1;
            return start + index;
        }

        @Override
        public String toString() {
            return type + "[" + start + ".." + (start + count - 1) + "]";
        }
    }
}
package com.miophas.singularity_iteration.core.runtime.energy;

import net.minecraft.world.inventory.ContainerData;

/** Lossless meter synchronization, including vanilla's signed 16-bit data packets. */
public final class MeterReadings implements com.miophas.singularity_iteration.core.api.menu.WordContainerData {
    public static final int SLOT_COUNT = 15;
    private final int[] words = new int[SLOT_COUNT];

    @Override public int getCount() { return SLOT_COUNT; }
    @Override public int get(int index) { return words[index]; }
    @Override public void set(int index, int value) { words[index] = value & 0xffff; }

    public void update(int mode, double average, double minimum, double maximum, int count) {
        words[0] = mode;
        putDouble(1, average);
        putDouble(5, minimum);
        putDouble(9, maximum);
        words[13] = count & 0xffff;
        words[14] = (count >>> 16) & 0xffff;
    }

    public int mode() { return words[0]; }
    public double average() { return readDouble(1); }
    public double minimum() { return readDouble(5); }
    public double maximum() { return readDouble(9); }
    public int samples() { return words[13] | (words[14] << 16); }

    private void putDouble(int offset, double value) {
        long bits = Double.doubleToRawLongBits(value);
        for (int i = 0; i < 4; i++) words[offset + i] = (int) ((bits >>> (16 * i)) & 0xffff);
    }

    private double readDouble(int offset) {
        long bits = 0;
        for (int i = 0; i < 4; i++) bits |= (long) words[offset + i] << (16 * i);
        return Double.longBitsToDouble(bits);
    }
}

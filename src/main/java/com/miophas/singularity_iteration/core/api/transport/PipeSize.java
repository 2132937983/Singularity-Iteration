package com.miophas.singularity_iteration.core.api.transport;

import net.minecraft.util.StringRepresentable;

/** IC2 fluid pipe geometry and capacity multipliers; unrelated to item stack limits. */
public enum PipeSize implements StringRepresentable {
    TINY("tiny", 4, 1, 6), SMALL("small", 6, 1, 3),
    MEDIUM("medium", 8, 1, 1), LARGE("large", 10, 2, 1);

    private final String name;
    private final int pixels, numerator, denominator;

    PipeSize(String name, int pixels, int numerator, int denominator) {
        this.name = name;
        this.pixels = pixels;
        this.numerator = numerator;
        this.denominator = denominator;
    }

    @Override public String getSerializedName() { return name; }
    public int diameterPixels() { return pixels; }
    public int capacity(int materialRate) { return materialRate * numerator / denominator; }
}

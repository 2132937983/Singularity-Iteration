package com.miophas.singularity_iteration.core.api.transport;

import net.minecraft.util.StringRepresentable;

/** Material parameters from IC2 2.8.221 PipeType. */
public enum PipeGrade implements StringRepresentable {
    BRONZE("bronze", 2400, 0xAE5111), STEEL("steel", 4800, 0x808080);

    private final String name;
    private final int materialRate, color;

    PipeGrade(String name, int materialRate, int color) {
        this.name = name;
        this.materialRate = materialRate;
        this.color = color;
    }

    @Override public String getSerializedName() { return name; }
    public int color() { return 0xFF000000 | color; }
    public int capacity(PipeSize size) { return size.capacity(materialRate); }
    /** Aggregate output to non-pipe neighbours in mB/t. */
    public int transferPerTick(PipeSize size) { return capacity(size) / 20; }
}

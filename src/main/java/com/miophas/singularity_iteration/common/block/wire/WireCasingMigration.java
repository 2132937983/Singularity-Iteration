// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.block.wire;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * 0.1.11.33 (cable audit W4): converts the cable block states of saves before 0.1.11.33 (the booleans foamlogged,
 * foam_reinforced, foam_hardened, disguised) into the {@code casing} property ({@link WireCasing}). The block-state
 * codec drops properties it does not know, so without this every cable of an old world would lose its scaffold, foam or
 * wall. It works on the NBT before it is decoded: the palettes of a chunk ({@code ChunkSerializer.read}) and one state
 * tag ({@code NbtUtils.readBlockState}: structures, falling blocks and the like). Pure and idempotent.
 */
public final class WireCasingMigration {
    private WireCasingMigration() { }

    private static final String[] OLD = {"foamlogged", "foam_reinforced", "foam_hardened", "disguised"};

    /** Converts one {Name, Properties} state tag in place; true when it changed. Only states with "foamlogged" (the cables). */
    public static boolean migrateState(CompoundTag state) {
        if (!state.contains("Properties", Tag.TAG_COMPOUND)) return false;
        CompoundTag properties = state.getCompound("Properties");
        if (!properties.contains("foamlogged", Tag.TAG_STRING) || !state.getString("Name").startsWith("mio_icif:")) return false;
        var casing = WireCasing.of(flag(properties, "foamlogged"), flag(properties, "foam_reinforced"),
            flag(properties, "foam_hardened"), flag(properties, "disguised"));
        for (String key : OLD) properties.remove(key);
        properties.putString("casing", casing.getSerializedName());
        return true;
    }

    /** Converts the block palettes of one chunk tag in place; returns the number of states changed. */
    public static int migrateChunk(CompoundTag chunk) {
        if (!chunk.contains("sections", Tag.TAG_LIST)) return 0;
        int changed = 0;
        ListTag sections = chunk.getList("sections", Tag.TAG_COMPOUND);
        for (int i = 0; i < sections.size(); i++) {
            CompoundTag blockStates = sections.getCompound(i).getCompound("block_states");
            if (!blockStates.contains("palette", Tag.TAG_LIST)) continue;
            ListTag palette = blockStates.getList("palette", Tag.TAG_COMPOUND);
            for (int j = 0; j < palette.size(); j++) if (migrateState(palette.getCompound(j))) changed++;
        }
        return changed;
    }

    private static boolean flag(CompoundTag properties, String key) { return "true".equals(properties.getString(key)); }
}

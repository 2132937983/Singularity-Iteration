// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * A loaded blueprint (0.1.8.0): the non-air blocks of a schematic, relative to its minimum
 * corner. Immutable. Sources: {@link BlueprintReader}.
 *
 * @param name    display name (file name or schematic name)
 * @param entries the blocks, relative positions 0..size-1
 * @param size    the size of the bounding box
 */
public record Blueprint(String name, List<Entry> entries, Vec3i size) {
    /** Hard limit of blocks in one blueprint (memory and save size guard). */
    public static final int MAX_BLOCKS = 262_144;

    public record Entry(BlockPos pos, BlockState state) { }

    /**
     * The world positions and states for a placement at {@code origin} (minimum corner after the
     * transform) with {@code rotation} and {@code mirror}. The transform keeps the minimum corner
     * at {@code origin}.
     */
    public List<Entry> place(BlockPos origin, Rotation rotation, Mirror mirror) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        BlockPos[] moved = new BlockPos[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            BlockPos p = StructureTemplate.transform(entries.get(i).pos(), mirror, rotation, BlockPos.ZERO);
            moved[i] = p;
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
        }
        java.util.ArrayList<Entry> out = new java.util.ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            BlockPos p = moved[i].offset(-minX, -minY, -minZ).offset(origin);
            out.add(new Entry(p, entries.get(i).state().mirror(mirror).rotate(rotation)));
        }
        return out;
    }

    /** The world positions for a placement exactly as Create places it: anchor + transform, no shift. */
    public List<Entry> placeAnchored(BlockPos anchor, Rotation rotation, Mirror mirror) {
        java.util.ArrayList<Entry> out = new java.util.ArrayList<>(entries.size());
        for (Entry e : entries) {
            BlockPos p = StructureTemplate.transform(e.pos(), mirror, rotation, BlockPos.ZERO).offset(anchor);
            out.add(new Entry(p, e.state().mirror(mirror).rotate(rotation)));
        }
        return out;
    }
}

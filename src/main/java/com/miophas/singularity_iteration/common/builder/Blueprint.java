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
    /**
     * 0.1.11.187: no fixed limit of blocks any more (0.1.11.186: 1,048,576; before 262,144, a larger blueprint was cut). A
     * blueprint is refused only when the free memory cannot hold it with its plan, {@link #BYTES_PER_BLOCK} a block, checked
     * while it is read ({@link Budget}): then {@link TooLarge}, and the builder says so instead of the game running out of
     * memory.
     */
    public static final long BYTES_PER_BLOCK = 256;

    /** Thrown while reading a blueprint the free memory cannot hold (0.1.11.187). */
    public static final class TooLarge extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public TooLarge(String message) { super(message, null, false, false); }
    }

    /** The memory the game may still take: the heap limit less what is in use (garbage counts as in use until collected). */
    public static long free() {
        Runtime rt = Runtime.getRuntime();
        return rt.maxMemory() - (rt.totalMemory() - rt.freeMemory());
    }

    /** Whether {@code bytes} more fit in the memory; before saying no it collects the garbage once and looks again. */
    public static boolean affordable(long bytes) {
        if (bytes <= free()) return true;
        System.gc();
        return bytes <= free();
    }

    /**
     * Counts the blocks of a blueprint being read and refuses it ({@link TooLarge}) when the memory free at the start cannot
     * hold them, every 65,536 blocks. The garbage is collected at most once a read (a full collection pauses the game).
     */
    static final class Budget {
        private long blocks, free = free();
        private boolean collected;

        void add() {
            if ((++blocks & 0xFFFF) == 0) check();
        }

        private void check() {
            if (blocks * BYTES_PER_BLOCK <= free) return;
            if (!collected) {
                collected = true;
                System.gc();
                free = free() + blocks * 64;   // what the blocks read so far take is in use now
            }
            if (blocks * BYTES_PER_BLOCK > free)
                throw new TooLarge(blocks + " blocks read, " + (free >> 20) + " MB free");
        }
    }

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
            out.add(new Entry(p, turn(entries.get(i).state(), rotation, mirror)));
        }
        return out;
    }

    private static final java.util.Map<net.minecraft.world.level.block.Block, Boolean> OURS = new java.util.concurrent.ConcurrentHashMap<>();

    /** 0.1.11.188: whether {@code state} is a block of this mod. */
    public static boolean ours(BlockState state) {
        return OURS.computeIfAbsent(state.getBlock(), b -> com.miophas.singularity_iteration.common.Singularity_Iteration.MOD_ID
            .equals(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getNamespace()));
    }

    /**
     * 0.1.11.188: a state turned and mirrored with the blueprint. Most blocks of this mod (machines, pipes, cables) do not
     * turn themselves, so a turned blueprint kept them as drawn: a machine faced the old way and a pipe or cable joined the
     * old sides. Such a block is turned here: its facing, its axis and its four side connections go round with the blueprint.
     */
    public static BlockState turn(BlockState s, Rotation rotation, Mirror mirror) {
        BlockState own = s.mirror(mirror).rotate(rotation);
        if (own != s || rotation == Rotation.NONE && mirror == Mirror.NONE || !ours(s)) return own;
        BlockState out = s;
        var definition = s.getBlock().getStateDefinition();
        for (net.minecraft.world.level.block.state.properties.Property<?> p : s.getProperties()) {
            if (p instanceof net.minecraft.world.level.block.state.properties.DirectionProperty d) {
                net.minecraft.core.Direction to = rotation.rotate(mirror.mirror(s.getValue(d)));
                if (d.getPossibleValues().contains(to)) out = out.setValue(d, to);
            } else if (p instanceof net.minecraft.world.level.block.state.properties.EnumProperty<?> e
                && e.getValueClass() == net.minecraft.core.Direction.Axis.class
                && (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90)) {
                out = swapAxis(out, e);
            }
        }
        // the four side connections (only when the block has all four: they are swapped round)
        java.util.Map<net.minecraft.core.Direction, net.minecraft.world.level.block.state.properties.BooleanProperty> sides = new java.util.EnumMap<>(net.minecraft.core.Direction.class);
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL)
            if (definition.getProperty(d.getSerializedName()) instanceof net.minecraft.world.level.block.state.properties.BooleanProperty b) sides.put(d, b);
        if (sides.size() == 4)
            for (var side : sides.entrySet()) out = out.setValue(sides.get(rotation.rotate(mirror.mirror(side.getKey()))), s.getValue(side.getValue()));
        return out;
    }

    @SuppressWarnings("unchecked")
    private static BlockState swapAxis(BlockState state, net.minecraft.world.level.block.state.properties.EnumProperty<?> property) {
        var axis = (net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.core.Direction.Axis>) property;
        net.minecraft.core.Direction.Axis v = state.getValue(axis);
        net.minecraft.core.Direction.Axis to = v == net.minecraft.core.Direction.Axis.X ? net.minecraft.core.Direction.Axis.Z
            : v == net.minecraft.core.Direction.Axis.Z ? net.minecraft.core.Direction.Axis.X : v;
        return axis.getPossibleValues().contains(to) ? state.setValue(axis, to) : state;
    }

    /** The world positions for a placement exactly as Create places it: anchor + transform, no shift. */
    public List<Entry> placeAnchored(BlockPos anchor, Rotation rotation, Mirror mirror) {
        java.util.ArrayList<Entry> out = new java.util.ArrayList<>(entries.size());
        for (Entry e : entries) {
            BlockPos p = StructureTemplate.transform(e.pos(), mirror, rotation, BlockPos.ZERO).offset(anchor);
            out.add(new Entry(p, turn(e.state(), rotation, mirror)));
        }
        return out;
    }
}

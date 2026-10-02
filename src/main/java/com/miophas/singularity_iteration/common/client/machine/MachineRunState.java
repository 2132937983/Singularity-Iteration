package com.miophas.singularity_iteration.common.client.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Client-side view of a machine's running state, read straight from the block state the
 * server already syncs ({@code lit} for processing machines, {@code active} for generators).
 * Property lookups are resolved once per block and cached, so a check is one map lookup
 * plus one state value read - cheap enough for per-frame and per-tick use.
 */
@OnlyIn(Dist.CLIENT)
public final class MachineRunState {
    private static final Map<Block, BooleanProperty> RUNNING = new IdentityHashMap<>();
    private static final Map<Block, DirectionProperty> FACING = new IdentityHashMap<>();
    private static final BooleanProperty NONE = BooleanProperty.create("si_none");
    private static final DirectionProperty NO_FACING = DirectionProperty.create("si_none_facing");

    /** Animation and sound cycle length shared by parts, lamps and loops (2 s). */
    public static final int CYCLE_TICKS = 40;

    private MachineRunState() {}

    public static boolean isRunning(BlockState state) {
        BooleanProperty p = RUNNING.computeIfAbsent(state.getBlock(), MachineRunState::findRunning);
        return p != NONE && state.getValue(p);
    }

    public static boolean hasRunState(BlockState state) {
        return RUNNING.computeIfAbsent(state.getBlock(), MachineRunState::findRunning) != NONE;
    }

    /** Horizontal facing (north when the block has none). */
    public static Direction facing(BlockState state) {
        DirectionProperty p = FACING.computeIfAbsent(state.getBlock(), MachineRunState::findFacing);
        if (p == NO_FACING) return Direction.NORTH;
        Direction d = state.getValue(p);
        return d.getAxis().isHorizontal() ? d : Direction.NORTH;
    }

    /** Per-machine phase offset in ticks, so neighbouring machines do not move in lock-step. */
    public static int phaseOffset(BlockPos pos) {
        long h = pos.asLong() * 0x9E3779B97F4A7C15L;
        h ^= (h >>> 29);
        return (int) Math.floorMod(h, CYCLE_TICKS);
    }

    private static BooleanProperty findRunning(Block block) {
        BooleanProperty lit = null;
        for (Property<?> p : block.getStateDefinition().getProperties()) {
            if (p instanceof BooleanProperty bp) {
                if (p.getName().equals("active")) return bp;
                if (p.getName().equals("lit")) lit = bp;
            }
        }
        return lit != null ? lit : NONE;
    }

    private static DirectionProperty findFacing(Block block) {
        for (Property<?> p : block.getStateDefinition().getProperties()) {
            if (p instanceof DirectionProperty dp && p.getName().equals("facing")) return dp;
        }
        return NO_FACING;
    }
}

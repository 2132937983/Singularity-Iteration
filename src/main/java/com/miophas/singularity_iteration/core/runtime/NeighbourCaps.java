// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 0.1.10.10 (D3): the capabilities of the neighbours of a block entity, kept between ticks.
 *
 * <p>Heat and kinetic machines asked {@code level.getCapability} for their neighbours every tick:
 * the neighbour's state and block entity are read, then the provider runs (several providers make a
 * new wrapper object each time). Here the answer is kept per face and reused while the neighbour has
 * the same block state object and the same block entity, for at most {@link #RECHECK} ticks (a
 * provider whose answer depends on data inside its block entity is asked again at least that often).
 *
 * <p>An unloaded neighbour gives {@code null}; the lookup never loads a chunk (a plain
 * {@code getCapability} on an unloaded position loads it). Off the server thread every call asks the
 * level directly.
 */
public final class NeighbourCaps {
    private NeighbourCaps() { }

    /** Longest reuse of one answer, in game ticks. */
    public static final int RECHECK = 100;

    private static final class Slot {
        BlockCapability<?, ?> kind;
        BlockState state;
        BlockEntity tile;
        Object answer;
        long askedAt;
    }

    /** Per owner: two slots per face (two kinds of capability per face). Server thread only. */
    private static final Map<BlockEntity, Slot[]> SLOTS = new WeakHashMap<>();

    /**
     * The capability of the block on face {@code face} of {@code owner}, asked with the context
     * {@code face.getOpposite()} (the side of the neighbour that touches the owner), or {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static <T> @Nullable T get(BlockEntity owner, BlockCapability<T, Direction> capability, Direction face) {
        if (!(owner.getLevel() instanceof ServerLevel level) || owner.isRemoved()) return null;
        BlockPos at = owner.getBlockPos().relative(face);
        if (!level.isLoaded(at)) return null;
        if (!level.getServer().isSameThread()) return level.getCapability(capability, at, face.getOpposite());
        PerfCounters.add(PerfCounters.Kind.CAP_LOOKUPS);
        BlockState state = level.getBlockState(at);
        BlockEntity tile = state.hasBlockEntity() ? level.getBlockEntity(at) : null;
        Slot[] slots = SLOTS.computeIfAbsent(owner, ignored -> new Slot[12]);
        int first = face.ordinal(), second = 6 + face.ordinal();
        Slot slot = slots[first] != null && slots[first].kind == capability ? slots[first]
            : slots[second] != null && slots[second].kind == capability ? slots[second] : null;
        long now = level.getGameTime();
        if (slot != null && slot.state == state && slot.tile == tile && now - slot.askedAt < RECHECK && now >= slot.askedAt) {
            PerfCounters.add(PerfCounters.Kind.CAP_CACHED);
            return (T) slot.answer;
        }
        T answer = level.getCapability(capability, at, state, tile, face.getOpposite());
        if (slot == null) {
            int index = slots[first] == null ? first : slots[second] == null ? second : first;
            slot = slots[index] = new Slot();
        }
        slot.kind = capability; slot.state = state; slot.tile = tile; slot.answer = answer; slot.askedAt = now;
        return answer;
    }

    /** Forgets every kept answer of {@code owner} (for example after the owner turned). */
    public static void forget(BlockEntity owner) {
        SLOTS.remove(owner);
    }
}

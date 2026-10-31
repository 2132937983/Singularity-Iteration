// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;

/** Shared front-face HU output, without loading adjacent chunks. */
public final class ThermalOutput {
    private ThermalOutput() { }
    public static boolean enabled() { return com.miophas.singularity_iteration.core.runtime.energy.engine.IndependentEnergyMode.feature("thermalGenerators"); }
    public static IMioIcifCapabilities.IHeatStorage front(BlockEntity owner) {
        if (!(owner.getLevel() instanceof ServerLevel level) || !level.getServer().isSameThread()
                || owner.isRemoved() || !owner.getBlockState().hasProperty(BlockStateProperties.FACING)) return null;
        var facing = owner.getBlockState().getValue(BlockStateProperties.FACING);
        var pos = owner.getBlockPos().relative(facing);
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return null;
        var target = com.miophas.singularity_iteration.core.runtime.NeighbourCaps.get(owner, IMioIcifCapabilities.HEAT_STORAGE_BLOCK, facing);
        if (target != null) return target;
        var tile = chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
        return tile instanceof IMioIcifCapabilities.IHeatStorage heat && !declaresHeat(level, pos) ? heat : null;
    }

    /** Whether the block at {@code pos} registers a heat capability (it then decides itself which faces are open). */
    public static boolean declaresHeat(Level level, BlockPos pos) {
        return level.getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, pos, null) != null;
    }

    /**
     * 0.1.10.42: the block entity at {@code pos} as heat storage, only when its block registers no heat capability at all (a block
     * of another mod that implements the interface); else null. A block whose capability refuses a face keeps that face closed:
     * before, this fallback reached the block entity past the capability and heat went in or out on every face.
     */
    public static IMioIcifCapabilities.IHeatStorage undeclared(Level level, BlockPos pos) {
        if (declaresHeat(level, pos)) return null;
        return com.miophas.singularity_iteration.core.api.MioIcifAPI.instance().getCapabilities().adaptHeatStorage(level.getBlockEntity(pos));
    }
    public static long room(IMioIcifCapabilities.IHeatStorage target) {
        if (target == null || !target.canReceiveHeat()) return 0;
        return Math.max(0, Math.max(0, target.getMaxHeatStored()) - Math.max(0, target.getHeatStored()));
    }
    public static long offer(IMioIcifCapabilities.IHeatStorage target, long requested) {
        long offered = Math.min(Math.max(0, requested), room(target));
        if (offered == 0) return 0;
        long accepted = target.receiveHeat(offered, false);
        if (accepted < 0 || accepted > offered) throw new IllegalStateException("Heat receiver violated its accepted-amount contract");
        if (accepted > 0 && target instanceof BlockEntity tile) ContainerToTank.markUnsaved(tile);
        return accepted;
    }
    /** The source is owned storage. Rejected heat is restored through its internal path. */
    public static long move(IMioIcifCapabilities.IHeatStorage source, IMioIcifCapabilities.IHeatStorage target, long requested) {
        if (source == null || source == target) return 0;
        long offered = Math.min(Math.max(0, requested), Math.min(Math.max(0, source.getMaxExtract()),
            Math.min(Math.max(0, source.getHeatStored()), room(target))));
        if (offered == 0) return 0;
        long extracted = source.consumeHeatInternal(offered, false);
        long accepted = offer(target, extracted);
        long refund = extracted - accepted;
        if (refund > 0 && source.generateHeatInternal(refund, false) != refund)
            throw new IllegalStateException("Owned heat storage could not restore rejected transfer");
        return accepted;
    }
}

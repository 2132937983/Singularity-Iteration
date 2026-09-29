// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import net.minecraft.server.level.ServerLevel;
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
        var target = level.getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, pos, facing.getOpposite());
        if (target != null) return target;
        var tile = chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
        return tile instanceof IMioIcifCapabilities.IHeatStorage heat ? heat : null;
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

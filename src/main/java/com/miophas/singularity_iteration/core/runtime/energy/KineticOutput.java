// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;

/** Loaded-neighbor KU access; callers own a shared source budget across faces. */
public final class KineticOutput {
    private KineticOutput() { }
    public static boolean enabled() { return com.miophas.singularity_iteration.core.runtime.energy.engine.IndependentEnergyMode.feature("kineticGenerators"); }
    public static IMioIcifCapabilities.IKineticStorage front(BlockEntity owner) {
        return owner.getBlockState().hasProperty(BlockStateProperties.FACING)
            ? at(owner, owner.getBlockState().getValue(BlockStateProperties.FACING)) : null;
    }
    public static IMioIcifCapabilities.IKineticStorage at(BlockEntity owner, Direction side) {
        if (!(owner.getLevel() instanceof ServerLevel level) || !level.getServer().isSameThread() || owner.isRemoved()) return null;
        var pos = owner.getBlockPos().relative(side);
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return null;
        var target = level.getCapability(IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, pos, side.getOpposite());
        if (target != null) return target;
        var tile = chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
        return tile instanceof IMioIcifCapabilities.IKineticStorage storage ? storage : null;
    }
    public static long room(IMioIcifCapabilities.IKineticStorage target, int sourceRPM) {
        if (target == null || !target.canReceiveKinetic() || sourceRPM <= target.getRPM()) return 0;
        return Math.max(0, Math.max(0, target.getMaxKineticStored()) - Math.max(0, target.getKineticStored()));
    }
    public static long offer(IMioIcifCapabilities.IKineticStorage target, long available, int sourceRPM) {
        long wanted = Math.min(Math.max(0, available), room(target, sourceRPM));
        if (wanted == 0) return 0;
        long accepted = target.receiveKinetic(wanted, false);
        if (accepted < 0 || accepted > wanted) throw new IllegalStateException("Kinetic receiver violated its accepted-amount contract");
        if (accepted > 0 && target instanceof BlockEntity tile) ContainerToTank.markUnsaved(tile);
        return accepted;
    }
}

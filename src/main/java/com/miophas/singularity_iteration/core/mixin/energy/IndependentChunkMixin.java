// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.mixin.energy;

import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentCableFactory;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerFactory;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value=LevelChunk.class,remap=false)
public abstract class IndependentChunkMixin {
    @Inject(method="setBlockState",at=@At("RETURN"),require=1)
    private void scexBlockHistory(BlockPos position,BlockState state,boolean moving,CallbackInfoReturnable<BlockState> callback) {
        BlockState before=callback.getReturnValue();
        if(com.miophas.singularity_iteration.core.runtime.energy.engine.IndependentEnergyMode.enabled() && before!=null && before.getBlock()!=state.getBlock()
                && ((LevelChunk)(Object)this).getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            com.miophas.singularity_iteration.core.platform.neoforge.energy.PlatformTopology.physicalBlockChanged(level,position,before,state);
        }
    }
    @Shadow private void removeBlockEntityTicker(BlockPos position) { throw new AssertionError(); }
    @WrapOperation(method={"setBlockState","createBlockEntity","promotePendingBlockEntity"},
        at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/EntityBlock;newBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/entity/BlockEntity;"),require=3)
    private BlockEntity scexCreate(EntityBlock block,BlockPos position,net.minecraft.world.level.block.state.BlockState state,Operation<BlockEntity> original) {
        var replacement=IndependentTransformerFactory.placed(position,state);
        if(replacement==null)replacement=IndependentCableFactory.create(null,position,state);
        return replacement!=null ? replacement : original.call(block,position,state);
    }
    @Inject(method="updateBlockEntityTicker",at=@At("HEAD"),cancellable=true,require=1)
    private void scexTicker(BlockEntity entity,CallbackInfo callback) {
        if(IndependentTransformerFactory.suppressTicker(entity) || IndependentCableFactory.suppressTicker(entity)) {
            removeBlockEntityTicker(entity.getBlockPos());callback.cancel();
        }
    }
}

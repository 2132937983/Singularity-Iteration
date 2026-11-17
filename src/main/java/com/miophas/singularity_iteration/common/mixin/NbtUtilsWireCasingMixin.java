// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.mixin;

import com.miophas.singularity_iteration.common.block.wire.WireCasingMigration;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 0.1.11.33 (cable audit W4): an old cable state read from NBT (structures, falling blocks) gets the casing property. */
@Mixin(NbtUtils.class)
public abstract class NbtUtilsWireCasingMixin {
    @Inject(method = "readBlockState", at = @At("HEAD"))
    private static void mio_icif$migrateWireCasing(HolderGetter<Block> blocks, CompoundTag tag, CallbackInfoReturnable<BlockState> cir) {
        WireCasingMigration.migrateState(tag);
    }
}

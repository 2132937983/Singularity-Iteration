// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.mixin;

import com.miophas.singularity_iteration.common.block.wire.WireCasingMigration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 0.1.11.33 (cable audit W4): the cable states of a loaded chunk get the casing property before the palettes decode. */
@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerWireCasingMixin {
    @Inject(method = "read", at = @At("HEAD"))
    private static void mio_icif$migrateWireCasing(ServerLevel level, PoiManager poi, RegionStorageInfo info, ChunkPos pos,
                                                  CompoundTag tag, CallbackInfoReturnable<ProtoChunk> cir) {
        WireCasingMigration.migrateChunk(tag);
    }
}

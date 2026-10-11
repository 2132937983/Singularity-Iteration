// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Changes the serialization Codec of DataComponents.MAX_STACK_SIZE.
 * It lets the maximum stack size in the data component go above 64.
 */
@Mixin(DataComponents.class)
@SuppressWarnings("null")
public class DataComponentMaxStackMixin {
    
    @ModifyExpressionValue(
        method = "lambda$static$1(Lnet/minecraft/core/component/DataComponentType$Builder;)Lnet/minecraft/core/component/DataComponentType$Builder;",
        at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/util/ExtraCodecs;intRange(II)Lcom/mojang/serialization/Codec;"
        )
    )
    private static Codec<Integer> replaceMaxStackCodec(Codec<Integer> original) {
        // Change the serialization range of the MAX_STACK_SIZE data component from 0-64 to 0-9999.
        return Codec.intRange(0, 9999);
    }
}



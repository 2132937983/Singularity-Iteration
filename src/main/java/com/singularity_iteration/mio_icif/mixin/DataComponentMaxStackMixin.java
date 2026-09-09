package com.singularity_iteration.mio_icif.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 修改 DataComponents.MAX_STACK_SIZE 的序列化 Codec
 * 允许数据组件中的最大堆叠数超过 64
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
        // �?MAX_STACK_SIZE 数据组件的序列化范围�?0-64 改为 0-9999
        return Codec.intRange(0, 9999);
    }
}



package com.singularity_iteration.mio_icif.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 修改 ItemStack 的物品数量序列化 Codec
 * 允许网络同步时传输大�?64 的堆叠数
 */
@Mixin(ItemStack.class)
@SuppressWarnings("null")
public class ItemStackMaxStackMixin {
    
    @ModifyExpressionValue(
        method = "lambda$static$3(Lcom/mojang/serialization/codecs/RecordCodecBuilder$Instance;)Lcom/mojang/datafixers/kinds/App;",
        at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/util/ExtraCodecs;intRange(II)Lcom/mojang/serialization/Codec;"
        )
    )
    private static Codec<Integer> replaceItemStackCodec(Codec<Integer> original) {
        // 将物品数量的序列化范围从 0-64 改为 0-9999
        return ExtraCodecs.intRange(0, 9999);
    }
}



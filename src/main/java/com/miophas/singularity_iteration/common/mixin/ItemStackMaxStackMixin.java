// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Changes the Codec that serializes the item count of an ItemStack.
 * This lets network sync send stack sizes larger than 64.
 *
 * <p>0.1.10.16: kept as a compatibility range only. It lets a stack above 99 be saved and loaded (an SI
 * coin stack of up to 9,999 in any container, and the stacks of worlds saved before); it makes no stack
 * larger. Which items may stack above 99 is decided by {@code LargeStacks} (SI large-stack items only);
 * the former global container mixin was removed ({@code DataComponentMaxStackMixin} stays, also as a
 * compatibility range only).
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



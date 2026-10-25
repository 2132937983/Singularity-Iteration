// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.miophas.singularity_iteration.common.item.LargeStacks;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 0.1.10.16: the slot side of {@link LargeStackContainerMixin}. A menu slot asks its own limit (the
 * container's no-argument limit), so without this a coin stack of more than 99 could be picked up but
 * not placed into a slot. Only SI large-stack items, only for slots with the default limit whose
 * container keeps large stacks ({@code LargeStacks.keepsLargeStacks}).
 */
@Mixin(Slot.class)
public abstract class LargeStackSlotMixin {
    @ModifyReturnValue(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), require = 0)
    private int mio_icif$largeStackLimit(int original, @Local(argsOnly = true) ItemStack stack) {
        return LargeStacks.slotLimit((Slot) (Object) this, stack, original);
    }
}

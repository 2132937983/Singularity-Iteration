// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.miophas.singularity_iteration.common.item.LargeStacks;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 0.1.10.16: replaces the global {@code ContainerMaxStackMixin} (every container that used the vanilla
 * limit of 99 reported 9,999, for every item whose own limit was above 99). Only an SI large-stack item
 * (the coin) gets more than 99, and only where the container uses the default limit and keeps large
 * stacks (vanilla containers, SI {@code LargeStackContainer}s); see {@link LargeStacks}. Optional ({@code require = 0}):
 * {@link LargeStacks#selfCheck()} reports it if the target ever changes.
 */
@Mixin(Container.class)
public interface LargeStackContainerMixin {
    @ModifyReturnValue(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), require = 0)
    default int mio_icif$largeStackLimit(int original, @Local(argsOnly = true) ItemStack stack) {
        return LargeStacks.containerLimit((Container) (Object) this, stack, original);
    }
}

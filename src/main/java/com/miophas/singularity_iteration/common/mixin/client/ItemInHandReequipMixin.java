package com.miophas.singularity_iteration.common.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Charging held electric items made the first-person hand "twitch": every energy
 * update replaces the stack's {@code battery_energy} component, the hand renderer sees
 * a different stack and replays the re-equip animation each time. An energy-only
 * change is not a new item, so it no longer triggers the animation (all items,
 * including addons and IC2-style batteries/tools; real item swaps still do).
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandReequipMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/neoforged/neoforge/client/ClientHooks;shouldCauseReequipAnimation(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)Z"),
        require = 0)
    private boolean si$ignoreEnergyOnlyChanges(ItemStack from, ItemStack to, int slot, Operation<Boolean> original) {
        boolean result = original.call(from, to, slot);
        return result && !com.miophas.singularity_iteration.common.item.EnergyStackDiff.energyOnly(from, to);
    }
}

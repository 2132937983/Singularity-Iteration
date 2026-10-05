package com.miophas.singularity_iteration.common.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Entity ESP (0.1.7.33): tagged creatures get the see-through outline pass. */
@Mixin(Minecraft.class)
public abstract class FcsGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true, require = 0)
    private void si$fcsOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (com.miophas.singularity_iteration.common.client.suit.FcsState.shouldGlow(entity)) cir.setReturnValue(true);
    }
}

package com.miophas.singularity_iteration.common.mixin.client;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Entity ESP (0.1.7.33): outline colour by category / threat level (client only). */
@Mixin(Entity.class)
public abstract class FcsTeamColorMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true, require = 0)
    private void si$fcsColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (!self.level().isClientSide) return;
        int color = com.miophas.singularity_iteration.common.client.suit.FcsState.glowColor(self);
        if (color != -1) cir.setReturnValue(color);
    }
}

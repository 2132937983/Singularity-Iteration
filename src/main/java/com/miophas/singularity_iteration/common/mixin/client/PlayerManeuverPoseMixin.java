// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.mixin.client;

import com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Special Maneuver Mode: limb pose of a flyer (arms swept back, lead arm forward at speed). */
@Mixin(HumanoidModel.class)
public abstract class PlayerManeuverPoseMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void mio_icif$maneuverPose(T entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player) ManeuverModeClient.applyLimbPose((HumanoidModel<?>) (Object) this, player);
    }
}

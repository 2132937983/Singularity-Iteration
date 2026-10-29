package com.miophas.singularity_iteration.common.mixin.client;

import com.miophas.singularity_iteration.common.client.render.BakedArmorModel;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 0.1.10.36: worn baked suit armor (nano, quantum) draws its enchantment glint at the scale of its 128x128 texture
 * ({@link SiRenderTypes#SUIT_ARMOR_GLINT}); every other armor model keeps the vanilla glint.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerGlintMixin {
    @Inject(method = "renderGlint(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void si$suitGlint(PoseStack pose, MultiBufferSource buffers, int light, Model model, CallbackInfo ci) {
        if (!(model instanceof BakedArmorModel)) return;
        model.renderToBuffer(pose, buffers.getBuffer(SiRenderTypes.SUIT_ARMOR_GLINT), light, OverlayTexture.NO_OVERLAY);
        ci.cancel();
    }
}

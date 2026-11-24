// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Emissive hard-light core, tactical visor, spine conduit, and joint nodes rendered over the
 * Wingman's dark mechanical endoskeleton. Tinted dynamically by tactical stance and suit tier
 * (with breathing pulse and combat surge).
 */
@OnlyIn(Dist.CLIENT)
public final class WingmanCoreLayer extends RenderLayer<WingmanEntity, WingmanRenderer.Model> {
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(
        Singularity_Iteration.MOD_ID, "textures/entity/wingman_endoskeleton_glow.png");

    public WingmanCoreLayer(RenderLayerParent<WingmanEntity, WingmanRenderer.Model> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, WingmanEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float age, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;
        float[] col = WingmanFx.modeColour(entity.mode());
        float r = col[0], g = col[1], b = col[2];

        // Subtle tint shift when wearing Living Nano Lining (emerald) or Quantum suit (violet-cyan)
        boolean lined = false, quantum = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = entity.getItemBySlot(slot);
            if (s.isEmpty()) continue;
            if (SuitModules.hasLivingLining(s)) lined = true;
            if (SuitModules.isQuantumPiece(s)) quantum = true;
        }
        if (lined) {
            r = Mth.lerp(0.30F, r, 0.22F);
            g = Mth.lerp(0.30F, g, 1.0F);
            b = Mth.lerp(0.30F, b, 0.62F);
        } else if (quantum) {
            r = Mth.lerp(0.22F, r, 0.72F);
            g = Mth.lerp(0.22F, g, 0.55F);
            b = Mth.lerp(0.22F, b, 1.0F);
        }

        // Breathing pulse + bright surge on combat/shot/swing
        float pulse = 0.78F + 0.22F * Mth.sin(age * 0.14F + entity.getId() * 0.7F);
        float surge = Math.max(entity.recoil(partialTick) * 0.35F, entity.getAttackAnim(partialTick) > 0.02F ? 0.35F : 0F);
        float intensity = Mth.clamp(pulse + surge, 0.55F, 1.0F);

        int color = FastColor.ARGB32.color(
            255,
            Mth.clamp((int) (r * intensity * 255), 0, 255),
            Mth.clamp((int) (g * intensity * 255), 0, 255),
            Mth.clamp((int) (b * intensity * 255), 0, 255)
        );
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(GLOW));
        getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
    }
}

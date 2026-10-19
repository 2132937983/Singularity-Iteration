// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * Wingman renderer (0.1.8.0). The Wingman is an empty suit: the body is not drawn, only the
 * armor layer (with the custom armor models of other mods and of this mod, the palette and the
 * glint) and the held items. In flight the suit leans into its travel direction (up to
 * {@link #MAX_LEAN} degrees) and the arms trail along the body, like the suit flight pose.
 *
 * <p>0.1.10.11:
 * <ul>
 *   <li>The lean and the flight pose follow the smoothed flight factor of the entity, read with the
 *       partial tick (no more jumps at every tick: the shaking suit and hands).</li>
 *   <li>Landed (idle landing) the suit stands upright with its legs straight.</li>
 *   <li>With a SlashBlade: Resharped blade the arms hold a still ready pose (the blade is drawn by
 *       SlashBlade's own layer from its motion, not from the arm, so a swaying arm looked like a
 *       twitching hand), and every slash of the Wingman plays a cut on the arm: a diagonal cut for a
 *       combo, a flat sweep for the dash, an overhead two-handed cut for a Slash Art.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class WingmanRenderer extends HumanoidMobRenderer<WingmanEntity, WingmanRenderer.Model> {
    /** Forward lean at full speed, degrees. */
    static final float MAX_LEAN = 62.0F;
    private static final ResourceLocation UNUSED = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    public WingmanRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModelLayers.PLAYER)), 0.3F);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
        // 0.1.9.0: frame core attachments of the worn helmet and chestplate
        addLayer(new com.miophas.singularity_iteration.common.client.suit.CoreAttachmentLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(WingmanEntity entity) { return UNUSED; }

    /** No body: the layers draw the suit. */
    @Override
    @Nullable
    protected RenderType getRenderType(WingmanEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        return null;
    }

    @Override
    protected void setupRotations(WingmanEntity entity, PoseStack pose, float bob, float bodyYaw, float partialTick, float scale) {
        super.setupRotations(entity, pose, bob, bodyYaw, partialTick, scale);
        float lean = MAX_LEAN * flightFactor(entity, partialTick);
        if (lean <= 0.01F) return;
        // pivot at the waist; negative x rotation tips the head forward (the model faces -z)
        pose.translate(0, 0.9F * scale, 0);
        pose.mulPose(Axis.XP.rotationDegrees(-lean));
        pose.translate(0, -0.9F * scale, 0);
    }

    /** 0 (hover or landed) .. 1 (full speed), smoothed by the entity and interpolated. */
    static float flightFactor(WingmanEntity entity, float partialTick) {
        return entity.flight(partialTick) * (1 - entity.landBlend(partialTick));
    }

    /** Humanoid model with the flight pose, the landed pose and the blade pose. */
    public static final class Model extends HumanoidModel<WingmanEntity> {
        public Model(ModelPart root) { super(root); }

        @Override
        public void setupAnim(WingmanEntity entity, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, age, netHeadYaw, headPitch);
            float partial = Mth.clamp(age - entity.tickCount, 0, 1);
            float f = flightFactor(entity, partial);
            float landed = entity.landBlend(partial);
            float air = 1 - landed;
            float sway = Mth.sin(age * 0.08F) * 0.05F * air;
            boolean blade = SlashBladeResharpedBridge.isBlade(entity.getMainHandItem());
            if (blade) {
                bladePose(entity, partial);
            } else if (attackTime <= 0) {
                // arms trail along the body, palms back (stabilisers); standing on the ground: as is
                rightArm.xRot = Mth.lerp(air, rightArm.xRot, Mth.lerp(f, rightArm.xRot * 0.3F + sway, 0.45F));
                leftArm.xRot = Mth.lerp(air, leftArm.xRot, Mth.lerp(f, leftArm.xRot * 0.3F - sway, 0.45F));
                rightArm.zRot = Mth.lerp(air, rightArm.zRot, Mth.lerp(f, 0.10F, 0.22F));
                leftArm.zRot = Mth.lerp(air, leftArm.zRot, -Mth.lerp(f, 0.10F, 0.22F));
            }
            // legs straight and together; a small hover sway; straight under the suit when landed
            rightLeg.xRot = Mth.lerp(f, sway, 0.10F) * air;
            leftLeg.xRot = Mth.lerp(f, -sway, 0.10F) * air;
            rightLeg.yRot = 0;
            leftLeg.yRot = 0;
            rightLeg.zRot = 0.03F * air + 0.01F;
            leftLeg.zRot = -rightLeg.zRot;
            // the head looks forward in fast flight
            head.xRot -= f * Mth.DEG_TO_RAD * MAX_LEAN * 0.6F;
            hat.copyFrom(head);
        }

        /** Ready pose (still) and the cut of the current slash. */
        private void bladePose(WingmanEntity entity, float partial) {
            // ready: the right hand near the hilt in front of the left hip, the left arm relaxed
            float rx = -0.42F, ry = 0.32F, rz = 0.04F;
            float lx = -0.12F, ly = 0.0F, lz = -0.10F;
            float p = entity.slashProgress(partial);
            if (p >= 0) {
                switch (entity.slashKind()) {
                    case WingmanEntity.SLASH_DASH -> {          // flat sweep, right to left
                        rx = keys(p, rx, -1.45F, -1.45F, -1.30F, rx);
                        ry = keys(p, ry, -1.05F, 0.95F, 0.80F, ry);
                        rz = keys(p, rz, 0.10F, -0.10F, -0.05F, rz);
                    }
                    case WingmanEntity.SLASH_ARTS -> {          // overhead, two hands
                        rx = keys(p, rx, -2.95F, -0.45F, -0.55F, rx);
                        ry = keys(p, ry, -0.12F, 0.10F, 0.10F, ry);
                        lx = keys(p, lx, -2.85F, -0.50F, -0.60F, lx);
                        ly = keys(p, ly, 0.30F, 0.40F, 0.35F, ly);
                        lz = keys(p, lz, 0.05F, 0.05F, 0.0F, lz);
                    }
                    default -> {                                // diagonal cut, high right to low left
                        rx = keys(p, rx, -2.55F, -0.50F, -0.65F, rx);
                        ry = keys(p, ry, -0.55F, 0.75F, 0.60F, ry);
                        rz = keys(p, rz, 0.15F, -0.05F, 0.0F, rz);
                    }
                }
            }
            rightArm.xRot = rx; rightArm.yRot = ry; rightArm.zRot = rz;
            leftArm.xRot = lx; leftArm.yRot = ly; leftArm.zRot = lz;
        }

        /**
         * Four phases over 0..1: wind-up (to {@code wind} at 0.25), cut (to {@code cut} at 0.55),
         * follow-through (to {@code follow} at 0.75), back to {@code end} at 1. Smoothstep in between.
         */
        private static float keys(float p, float start, float wind, float cut, float follow, float end) {
            if (p < 0.25F) return Mth.lerp(ease(p / 0.25F), start, wind);
            if (p < 0.55F) return Mth.lerp(ease((p - 0.25F) / 0.30F), wind, cut);
            if (p < 0.75F) return Mth.lerp(ease((p - 0.55F) / 0.20F), cut, follow);
            return Mth.lerp(ease((p - 0.75F) / 0.25F), follow, end);
        }

        private static float ease(float t) {
            t = Mth.clamp(t, 0, 1);
            return t * t * (3 - 2 * t);
        }
    }
}

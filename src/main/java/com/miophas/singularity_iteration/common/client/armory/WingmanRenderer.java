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
        float lean = MAX_LEAN * flightFactor(entity);
        // pivot at the waist; negative x rotation tips the head forward (the model faces -z)
        pose.translate(0, 0.9F * scale, 0);
        pose.mulPose(Axis.XP.rotationDegrees(-lean));
        pose.translate(0, -0.9F * scale, 0);
    }

    /** 0 (hover) .. 1 (full speed), from the horizontal speed of the last tick. */
    static float flightFactor(WingmanEntity entity) {
        double dx = entity.getX() - entity.xo, dz = entity.getZ() - entity.zo;
        return (float) Mth.clamp(Math.sqrt(dx * dx + dz * dz) * 1.6, 0, 1);
    }

    /** Humanoid model with the flight pose. */
    public static final class Model extends HumanoidModel<WingmanEntity> {
        public Model(ModelPart root) { super(root); }

        @Override
        public void setupAnim(WingmanEntity entity, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, age, netHeadYaw, headPitch);
            float f = flightFactor(entity);
            float sway = Mth.sin(age * 0.08F) * 0.05F;
            if (attackTime <= 0) {
                // arms trail along the body, palms back (stabilisers)
                rightArm.xRot = Mth.lerp(f, rightArm.xRot * 0.3F + sway, 0.45F);
                leftArm.xRot = Mth.lerp(f, leftArm.xRot * 0.3F - sway, 0.45F);
                rightArm.zRot = Mth.lerp(f, 0.10F, 0.22F);
                leftArm.zRot = -rightArm.zRot;
            }
            // legs straight and together; a small hover sway
            rightLeg.xRot = Mth.lerp(f, sway, 0.10F);
            leftLeg.xRot = Mth.lerp(f, -sway, 0.10F);
            rightLeg.zRot = 0.03F;
            leftLeg.zRot = -0.03F;
            // the head looks forward in fast flight
            head.xRot -= f * Mth.DEG_TO_RAD * MAX_LEAN * 0.6F;
            hat.copyFrom(head);
        }
    }
}

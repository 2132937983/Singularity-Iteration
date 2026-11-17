// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.armory.WingmanPose;
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
 *
 * <p>0.1.10.13: the arms of every held item follow {@link WingmanPose} (see {@link Model}).
 */
@OnlyIn(Dist.CLIENT)
public class WingmanRenderer extends HumanoidMobRenderer<WingmanEntity, WingmanRenderer.Model> {
    /** Forward lean at full speed, degrees. */
    static final float MAX_LEAN = WingmanPose.MAX_LEAN;
    private static final ResourceLocation UNUSED = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    public WingmanRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModelLayers.PLAYER)), 0.3F);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
        // 0.1.9.0: frame core attachments of the worn helmet and chestplate
        addLayer(new com.miophas.singularity_iteration.common.client.suit.CoreAttachmentLayer<>(this));
        // 0.1.10.30: the light of a worn nano or quantum suit
        addLayer(new com.miophas.singularity_iteration.common.client.suit.SuitGlowLayer<>(this));
        addLayer(new com.miophas.singularity_iteration.common.client.suit.SuitRippleLayer<>(this));     // 0.1.10.32 hit ripple
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
        // 0.1.11.44 (motion audit M-4): the tilt follows the flight direction (pitch forward or back, roll to the side)
        // 0.1.11.47 (M4): the hover float and the dip of a touchdown
        float air = 1 - entity.landBlend(partialTick);
        float drop = WingmanPose.hoverBob(entity.tickCount + partialTick, air, flightFactor(entity, partialTick))
            - 0.12F * WingmanPose.landingCrouch(entity.sinceLanding(partialTick));
        if (drop != 0) pose.translate(0, drop * scale, 0);
        float[] lean = entity.lean(partialTick);
        if (Math.abs(lean[0]) + Math.abs(lean[1]) <= 0.01F) return;
        // pivot at the waist; negative x rotation tips the head forward, negative z rotation tips it to the right
        pose.translate(0, 0.9F * scale, 0);
        pose.mulPose(Axis.XP.rotationDegrees(-lean[0]));
        pose.mulPose(Axis.ZP.rotationDegrees(-lean[1]));
        pose.translate(0, -0.9F * scale, 0);
    }

    /** 0 (hover or landed) .. 1 (full speed), smoothed by the entity and interpolated. */
    static float flightFactor(WingmanEntity entity, float partialTick) {
        return entity.flight(partialTick) * (1 - entity.landBlend(partialTick));
    }

    /**
     * Humanoid model with the flight pose, the landed pose, the weapon poses and the blade pose.
     *
     * <p>0.1.10.13: the arms come from {@link WingmanPose} for every held item (the jitter of the
     * nanosaber, the quantum sword, guns and tools): no walking swing (the suit flies), no switch to
     * the vanilla pose during a swing, a fail-safe smoothing of the pose per Wingman, and the cut, the
     * recoil and the blade slash added on top (all zero at their start and end).
     */
    public static final class Model extends HumanoidModel<WingmanEntity> {
        /** Smoothed arm pose per Wingman (6 angles and the time), see {@link WingmanPose#smooth}. */
        private final java.util.Map<WingmanEntity, float[]> poses = new java.util.WeakHashMap<>();

        public Model(ModelPart root) { super(root); }

        @Override
        public void setupAnim(WingmanEntity entity, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
            // no walking swing and no vanilla attack pose: both are replaced below
            float attack = attackTime;
            attackTime = 0;
            super.setupAnim(entity, 0, 0, age, netHeadYaw, headPitch);
            attackTime = attack;
            float partial = Mth.clamp(age - entity.tickCount, 0, 1);
            float f = flightFactor(entity, partial);
            float landed = entity.landBlend(partial);
            float air = 1 - landed;
            float sway = Mth.sin(age * 0.08F) * 0.05F * air;
            boolean leftHanded = entity.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT;
            boolean blade = SlashBladeResharpedBridge.isBlade(entity.getMainHandItem());
            WingmanPose.Hold hold = WingmanPose.holdOf(entity.getMainHandItem(), blade);
            float pitch = headPitch * Mth.DEG_TO_RAD, yaw = netHeadYaw * Mth.DEG_TO_RAD;
            float leanPitch = entity.lean(partial)[0];
            float combat = entity.combat(partial);
            // 0.1.11.46 (motion audit M2): the melee wind-up grows as the target comes within 5.5 blocks; a shield in the
            // off hand is raised in escort
            float windup = hold == WingmanPose.Hold.HELD ? Mth.clamp((5.5F - entity.targetDistance()) / 3.0F, 0, 1) : 0;
            float shield = entity.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem
                && entity.mode() == com.miophas.singularity_iteration.common.armory.WingmanMode.ESCORT ? 1 : 0;
            float[] pose = WingmanPose.base(hold, leftHanded, f, air, combat, sway, pitch, yaw,
                Math.max(0, leanPitch) * Mth.DEG_TO_RAD, windup, shield);
            WingmanPose.smooth(poses.computeIfAbsent(entity, e -> new float[7]), pose, age);
            WingmanPose.proneArms(pose, entity.proneBlend(partial), combat);   // 0.1.11.51 (F2): arms along the body at speed
            float twist = 0;
            float shotAge = entity.shotAge(partial);
            int cooldown = entity.shotCooldown();
            switch (hold) {
                case BLADE -> WingmanPose.slash(pose, entity.slashKind(), entity.slashProgress(partial));
                case RANGED -> {
                    WingmanPose.recoil(pose, leftHanded, entity.recoil(partial));
                    twist = WingmanPose.swing(pose, leftHanded, attack, pitch);
                }
                case BOW -> {   // the release at the shot, then the draw over the cooldown
                    WingmanPose.draw(pose, leftHanded, Mth.clamp((shotAge - 2) / Math.max(1, cooldown - 4), 0, 1), combat);
                    WingmanPose.recoil(pose, leftHanded, entity.recoil(partial) * 0.4F);
                }
                case CROSSBOW -> {
                    WingmanPose.reload(pose, leftHanded, shotAge / cooldown, combat);
                    WingmanPose.recoil(pose, leftHanded, entity.recoil(partial));
                }
                case SPEAR -> WingmanPose.spearThrow(pose, leftHanded, shotAge, combat);
                case HELD -> twist = WingmanPose.cut(pose, leftHanded, attack, windup);
                default -> twist = WingmanPose.swing(pose, leftHanded, attack, pitch);
            }
            rightArm.xRot = pose[WingmanPose.RX]; rightArm.yRot = pose[WingmanPose.RY]; rightArm.zRot = pose[WingmanPose.RZ];
            leftArm.xRot = pose[WingmanPose.LX]; leftArm.yRot = pose[WingmanPose.LY]; leftArm.zRot = pose[WingmanPose.LZ];
            if (hold != WingmanPose.Hold.BLADE) {
                // the small idle bob of the arms (continuous in time), as vanilla
                net.minecraft.client.model.AnimationUtils.bobModelPart(rightArm, age, 1.0F);
                net.minecraft.client.model.AnimationUtils.bobModelPart(leftArm, age, -1.0F);
            }
            // the body turns with a cut, and the shoulders with it (as vanilla)
            body.yRot = twist;
            rightArm.z = Mth.sin(twist) * 5.0F;
            rightArm.x = -Mth.cos(twist) * 5.0F;
            leftArm.z = -Mth.sin(twist) * 5.0F;
            leftArm.x = Mth.cos(twist) * 5.0F;
            // 0.1.11.45 (M1): the legs trail with speed, spread in a slide or bank, tuck in a boost; straight when landed
            float[] legs = WingmanPose.proneLegs(WingmanPose.legs(f, air, sway, entity.lean(partial)[1], entity.boostAge(partial)),
                entity.proneBlend(partial));
            rightLeg.xRot = legs[WingmanPose.LEG_RX];
            leftLeg.xRot = legs[WingmanPose.LEG_LX];
            rightLeg.yRot = 0;
            leftLeg.yRot = 0;
            rightLeg.zRot = legs[WingmanPose.LEG_RZ];
            leftLeg.zRot = legs[WingmanPose.LEG_LZ];
            // 0.1.11.48 (M3): with a SlashBlade blade, the arms and legs follow SlashBlade's own player motion of the combo
            // (the blade layer draws the blade from the same combo); without the bridge the key frames above stay
            if (hold == WingmanPose.Hold.BLADE) {
                Object motion = SlashBladeMotion.update(entity);
                if (motion != null) SlashBladeMotion.apply(motion, partial, rightArm, leftArm, rightLeg, leftLeg);
            }
            // the head looks forward in fast flight
            head.xRot -= leanPitch * Mth.DEG_TO_RAD * 0.6F;   // 0.1.11.44: the head keeps looking ahead (forward and back)
            // 0.1.11.47 (M4): a crouch on touchdown, a push-off at take-off, a slow look around when idle
            float crouch = WingmanPose.landingCrouch(entity.sinceLanding(partial));
            float push = WingmanPose.takeoffPush(entity.sinceTakeoff(partial));
            if (crouch > 0 || push > 0) {
                body.xRot += 0.35F * crouch - 0.08F * push;
                rightArm.xRot += 0.25F * crouch - 0.35F * push;
                leftArm.xRot += 0.25F * crouch - 0.35F * push;
                rightLeg.xRot += -0.25F * crouch + 0.45F * push;
                leftLeg.xRot += -0.20F * crouch + 0.40F * push;
            }
            head.yRot += WingmanPose.idleLook(age, (1 - entity.combat(partial)) * (1 - f));
            hat.copyFrom(head);
        }
    }
}

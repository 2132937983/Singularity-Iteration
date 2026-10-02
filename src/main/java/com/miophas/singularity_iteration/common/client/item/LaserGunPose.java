// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Two-handed gun handling for the mining laser.
 * <p>Third person: a low-ready carry (both hands on the gun, muzzle forward-down) that snaps up to a
 * shouldered aim along the line of sight when firing, stays raised briefly, then eases back down.
 * Each shot kicks the arms up. First person: a steady gun hold with a short backward/upward recoil
 * instead of the melee swing.
 */
public final class LaserGunPose implements IClientItemExtensions {
    /** Enum extension parameters for {@code HumanoidModel.ArmPose.MIO_ICIF_LASER_AIM} (see enumextensions.json). */
    public static final EnumProxy<HumanoidModel.ArmPose> ARM_POSE = new EnumProxy<>(HumanoidModel.ArmPose.class,
        true, (IArmPoseTransformer) LaserGunPose::transform);

    private static final int HOLD_AIM = 30, LOWER = 10;
    private static final Map<LivingEntity, Long> LAST_SHOT = new WeakHashMap<>();

    public static final LaserGunPose INSTANCE = new LaserGunPose();

    private LaserGunPose() {}

    private static float partial() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    }

    /** 1 right after a shot, falling to 0 as the 4-tick cooldown runs out. */
    static float recoil(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player p) || stack.isEmpty()) return 0;
        float c = p.getCooldowns().getCooldownPercent(stack.getItem(), partial());
        if (c > 0) LAST_SHOT.put(entity, entity.level().getGameTime());
        return c * c;
    }

    /** 1 = shouldered aim, 0 = low ready. */
    static float aim(LivingEntity entity) {
        Long last = LAST_SHOT.get(entity);
        if (entity.isUsingItem()) return 1;
        if (last == null) return 0;
        float since = entity.level().getGameTime() - last + partial();
        if (since <= HOLD_AIM) return 1;
        float t = Mth.clamp((since - HOLD_AIM) / LOWER, 0, 1);
        return 1 - t * t * (3 - 2 * t);
    }

    private static void transform(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        boolean right = arm == HumanoidArm.RIGHT;
        ItemStack stack = right == (entity.getMainArm() == HumanoidArm.RIGHT) ? entity.getMainHandItem() : entity.getOffhandItem();
        float kick = recoil(entity, stack);
        float aim = aim(entity);
        ModelPart main = right ? model.rightArm : model.leftArm;
        ModelPart off = right ? model.leftArm : model.rightArm;
        // shouldered aim (crossbow-style, both arms along the line of sight)
        AnimationUtils.animateCrossbowHold(model.rightArm, model.leftArm, model.head, right);
        float aimMainX = main.xRot, aimOffX = off.xRot, aimMainY = main.yRot, aimOffY = off.yRot;
        // low ready: muzzle forward and down, support hand under the body
        float lowMainX = -0.95F + model.head.xRot * 0.25F, lowOffX = -1.05F + model.head.xRot * 0.25F;
        float lowMainY = (right ? -0.12F : 0.12F), lowOffY = (right ? 0.55F : -0.55F);
        main.xRot = Mth.lerp(aim, lowMainX, aimMainX) - kick * 0.42F;
        off.xRot = Mth.lerp(aim, lowOffX, aimOffX) - kick * 0.30F;
        main.yRot = Mth.lerp(aim, lowMainY, aimMainY);
        off.yRot = Mth.lerp(aim, lowOffY, aimOffY);
        main.zRot = 0;
        off.zRot = 0;
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return ARM_POSE.getValue();
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float c = player.getCooldowns().getCooldownPercent(stack.getItem(), partialTick);
        float kick = c * c;
        pose.translate(side * 0.50F, -0.50F + equipProcess * -0.6F, -0.70F);
        // recoil: the gun jumps back toward the shoulder and the muzzle climbs, then settles
        pose.translate(side * 0.01F * kick, 0.035F * kick, 0.13F * kick);
        pose.mulPose(Axis.XP.rotationDegrees(9.0F * kick));
        pose.mulPose(Axis.ZP.rotationDegrees(side * -2.0F * kick));
        return true;
    }
}

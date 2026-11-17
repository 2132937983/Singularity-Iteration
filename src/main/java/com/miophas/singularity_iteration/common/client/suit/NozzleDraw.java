// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.suit.QuantumNozzles;
import com.miophas.singularity_iteration.common.suit.core.FrameCores;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 0.1.11.66 (quantum flight phase R0): puts a draw at a nozzle of {@link QuantumNozzles}: the part of the posed model, the
 * port on it and the exhaust direction turned onto +y, so a flame drawn along +y leaves the real port the real way.
 */
public final class NozzleDraw {
    private NozzleDraw() {}

    private static final ResourceLocation BOOTS = ResourceLocation.fromNamespaceAndPath(
        com.miophas.singularity_iteration.common.Singularity_Iteration.MOD_ID, "armor/item_armor_quantum_boots");

    /** The nozzle set of the chestplate an entity wears. */
    public static QuantumNozzles.Variant variant(LivingEntity e) {
        return QuantumNozzles.Variant.of(FrameCores.core(e.getItemBySlot(EquipmentSlot.CHEST)));
    }

    /** True when the entity wears the quantum boots. */
    public static boolean boots(LivingEntity e) {
        var s = e.getItemBySlot(EquipmentSlot.FEET);
        return !s.isEmpty() && BOOTS.equals(BuiltInRegistries.ITEM.getKey(s.getItem()));
    }

    /** The nozzles of what an entity wears: the chestplate set and, with the quantum boots, the boot set. */
    public static List<QuantumNozzles.Nozzle> worn(LivingEntity e) {
        QuantumNozzles.Variant v = variant(e);
        List<QuantumNozzles.Nozzle> out = new ArrayList<>(QuantumNozzles.chest(v));
        if (boots(e)) out.addAll(QuantumNozzles.boots(v));
        return out;
    }

    public static ModelPart part(HumanoidModel<?> model, QuantumNozzles.Part p) {
        return switch (p) {
            case BODY -> model.body;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_LEG -> model.rightLeg;
            case LEFT_LEG -> model.leftLeg;
        };
    }

    /**
     * Pushes the pose to nozzle {@code n} of the posed model, +y along its exhaust. False (and nothing pushed) when the part
     * is hidden.
     */
    public static boolean push(PoseStack pose, HumanoidModel<?> model, QuantumNozzles.Nozzle n) {
        ModelPart part = part(model, n.part());
        if (!part.visible) return false;
        pose.pushPose();
        part.translateAndRotate(pose);
        pose.translate(n.x() / 16F, n.y() / 16F, n.z() / 16F);
        pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), new Vector3f(n.dx(), n.dy(), n.dz())));
        return true;
    }

    /** The classic graviton field at the pose origin around +y: two violet rings, the outer one growing out on a one-second beat. */
    public static void field(VertexConsumer vc, PoseStack pose, float size, float power, float seconds, boolean classic) {
        if (power <= 0.02F) return;
        float r = 0.16F * size * (0.7F + 0.3F * power), phase = (seconds + size) % 1F;
        Matrix4f m = pose.last().pose();
        com.miophas.singularity_iteration.common.client.render.PhaseFx.ringAt(vc, m, 0, r, 0.035F * size, new float[]{0.72F, 0.55F, 1F},
            0.9F * Math.min(1, power + 0.3F));
        com.miophas.singularity_iteration.common.client.render.PhaseFx.ringAt(vc, m, 0.02F, r * (1.2F + 1.3F * phase), 0.02F * size,
            new float[]{0.85F, 0.75F, 1F}, 0.85F * power * (1 - phase));
    }

    /**
     * 0.1.11.70 (phase warp W0): a graviton field at the pose origin around +y, in light: a violet ring and a cyan one that
     * grows out of it and fades on a one-second beat.
     */
    public static void fieldPhase(VertexConsumer vc, PoseStack pose, float size, float power, float seconds) {
        if (power <= 0.02F) return;
        float r = 0.16F * size * (0.7F + 0.3F * power), phase = (seconds + size) % 1F;
        Matrix4f m = pose.last().pose();
        com.miophas.singularity_iteration.common.client.render.PhaseFx.ringAt(vc, m, 0, r, 0.03F * size,
            com.miophas.singularity_iteration.common.suit.PhaseRules.VIOLET, 0.9F * Math.min(1, power + 0.3F));
        com.miophas.singularity_iteration.common.client.render.PhaseFx.ringAt(vc, m, 0.02F, r * (1.2F + 1.3F * phase), 0.02F * size,
            com.miophas.singularity_iteration.common.suit.PhaseRules.CYAN, 0.85F * power * (1 - phase));
    }

    /** 0.1.11.70: the flare of a nozzle at the origin of {@code m} (a glow facing the camera). */
    public static void flare(VertexConsumer vc, Matrix4f m, float radius, float alpha) {
        org.joml.Vector3f o = m.transformPosition(0, 0, 0, new org.joml.Vector3f());
        com.miophas.singularity_iteration.common.client.render.PhaseFx.blob(vc, new Vec3(o.x, o.y, o.z), radius,
            com.miophas.singularity_iteration.common.suit.PhaseRules.CYAN, alpha);
    }

    /**
     * 0.1.11.70: the blink of an attitude jet in the nozzle frame {@code m} during a pulse: a point of light, a ring across
     * the exhaust that grows and fades over two ticks, and a short cone.
     */
    public static void blink(VertexConsumer vc, Matrix4f m, float size, int tick, float partial, float seconds, float rate) {
        float frac = ((tick & 1) + partial) / 2F;
        float[] b = com.miophas.singularity_iteration.common.suit.PhaseRules.blink(frac);
        flare(vc, m, 0.09F * size + 0.03F, b[1]);
        com.miophas.singularity_iteration.common.client.render.PhaseFx.ringAt(vc, m, 0.02F, b[0] * (0.6F + 0.6F * size), 0.015F,
            com.miophas.singularity_iteration.common.suit.PhaseRules.CYAN, b[1]);
        com.miophas.singularity_iteration.common.client.render.PhaseFx.cone(vc, m, 0.22F * (0.6F + 0.4F * size), 0.08F * size + 0.02F,
            seconds, rate, 0F, 0.8F);
    }
}

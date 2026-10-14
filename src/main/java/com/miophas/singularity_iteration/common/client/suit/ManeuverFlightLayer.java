// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix4f;

/**
 * Special Maneuver Mode, body effects (0.1.8.0).
 *
 * <ul>
 *   <li><b>Thrusters.</b> A glow disc and a short flame at each palm and sole. The layer reads the
 *       posed limbs of the player model, so each flame points along its limb, as the thrust vector
 *       of {@link ManeuverModeClient#applyLimbPose}. The output of each limb comes from
 *       {@link ManeuverModeClient#thrusterOutput}.</li>
 *   <li><b>Shock diamonds</b> (0.1.10.2): at full burn the plume grows longer and four bright Mach
 *       nodes (double cones) stand in it, flickering. Each flame is also recorded in world space
 *       for the heat haze of {@code ThrusterFx}.</li>
 *   <li><b>Aerodynamic deflection field.</b> In cruise, a streamlined paraboloid shell of energy in
 *       front of the head and along the body. Flow bands run from the nose to the tail. The shell
 *       follows the body pose (the body lies along the path in cruise). The field also lowers the
 *       flight power cost on the server ({@code ManeuverMode.costPerTick}).</li>
 * </ul>
 *
 * <p>Model space: 1 unit = 1 block after the 1/16 scale of the parts; y points down, so the head is
 * at negative y. The layer draws with the additive {@link SiRenderTypes#FIELD_GLOW} render type.
 * It draws nothing when the player is not in maneuver flight. The local player in first person is
 * not rendered by vanilla, so this layer does not run then (see {@code ManeuverModeClient.fieldRim}).
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ManeuverFlightLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public ManeuverFlightLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) renderer.addLayer(new ManeuverFlightLayer(renderer));
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partial, float age, float headYaw, float headPitch) {
        if (!ManeuverModeClient.inManeuver(player)) return;
        float[] output = ManeuverModeClient.thrusterOutput(player);
        if (output == null) return;
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        float time = ManeuverModeClient.visualTime(player);
        // 0.1.10.2: full burn (throttle above 0.72) lengthens the plume and lights the shock diamonds
        float burn = net.minecraft.util.Mth.clamp((ManeuverModeClient.throttleOf(player, partial) - 0.72F) / 0.2F, 0, 1);
        int id = player.getId();
        // limb tip in the limb frame (pixels): arms 10 px below the shoulder pivot, legs 12 px below the hip
        thruster(pose, vc, model.rightArm, -1F / 16F, 10.5F / 16F, output[0], time, 0, burn, id);
        thruster(pose, vc, model.leftArm, 1F / 16F, 10.5F / 16F, output[1], time, 1, burn, id);
        thruster(pose, vc, model.rightLeg, 0, 12.2F / 16F, output[2], time, 2, burn, id);
        thruster(pose, vc, model.leftLeg, 0, 12.2F / 16F, output[3], time, 3, burn, id);
        float field = ManeuverModeClient.deflectionField(player, partial);
        if (field > 0.02F) {
            pose.pushPose();
            model.body.translateAndRotate(pose);
            deflectionField(pose.last().pose(), vc, field, time);
            pose.popPose();
        }
    }

    /** Glow disc at the tip of one limb and a flame along the limb axis (+y of the limb). */
    private static void thruster(PoseStack pose, VertexConsumer vc, ModelPart part, float tipX, float tipY,
                                 float power, float time, int seed, float burn, int owner) {
        if (power <= 0.02F || !part.visible) return;
        pose.pushPose();
        part.translateAndRotate(pose);
        pose.translate(tipX, tipY, 0);
        Matrix4f m = pose.last().pose();
        float flicker = 0.85F + 0.15F * (float) Math.sin(time * 37 + seed * 1.7) * (float) Math.sin(time * 23 + seed);
        float p = power * flicker;
        float radius = 0.06F + 0.05F * p, length = 0.12F + 0.42F * p + 0.5F * burn * power;
        int seg = 10;
        for (int s = 0; s < seg; s++) {
            double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
            float x0 = (float) Math.cos(a0) * radius, z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius, z1 = (float) Math.sin(a1) * radius;
            // disc: bright white-blue centre, cyan edge
            put(vc, m, 0, 0, 0, 0.9F, 0.97F, 1F, 0.85F * p);
            put(vc, m, x0, 0, z0, 0.35F, 0.75F, 1F, 0.35F * p);
            put(vc, m, x1, 0, z1, 0.35F, 0.75F, 1F, 0.35F * p);
            put(vc, m, 0, 0, 0, 0.9F, 0.97F, 1F, 0.85F * p);
            // flame: a cone that narrows and fades along +y (out of the palm / sole)
            put(vc, m, x0 * 0.9F, 0.01F, z0 * 0.9F, 0.5F, 0.8F, 1F, 0.55F * p);
            put(vc, m, x1 * 0.9F, 0.01F, z1 * 0.9F, 0.5F, 0.8F, 1F, 0.55F * p);
            put(vc, m, x1 * 0.2F, length, z1 * 0.2F, 0.2F, 0.45F, 1F, 0);
            put(vc, m, x0 * 0.2F, length, z0 * 0.2F, 0.2F, 0.45F, 1F, 0);
        }
        boolean fx = FcsClientConfig.thrusterFx();
        if (burn > 0.02F && fx) {
            for (int i = 0; i < 4; i++) {
                float y = length * (0.36F + 0.19F * i);
                float rr = radius * (0.62F - 0.09F * i);
                float al = burn * power * (0.9F - 0.18F * i) * (0.72F + 0.28F * (float) Math.sin(time * 41 + i * 1.3F + seed));
                shockDiamond(vc, m, y, rr, rr * 1.5F, al);
            }
        }
        // 0.1.10.2: the flame in world space for the heat haze (the layer pose is relative to the camera)
        if (fx) {
            org.joml.Vector3f a = m.transformPosition(new org.joml.Vector3f(0, 0, 0)), b = m.transformPosition(new org.joml.Vector3f(0, length, 0));
            net.minecraft.world.phys.Vec3 cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            com.miophas.singularity_iteration.common.client.suit.nano.ThrusterFx.jet(owner * 8L + seed,
                new net.minecraft.world.phys.Vec3(a.x + cam.x, a.y + cam.y, a.z + cam.z), new net.minecraft.world.phys.Vec3(b.x + cam.x, b.y + cam.y, b.z + cam.z),
                p * (0.7F + 0.5F * burn));
        }
        pose.popPose();
    }

    /** One Mach node: two cones base to base around {@code y} on the limb axis, a white-violet core. */
    private static void shockDiamond(VertexConsumer vc, Matrix4f m, float y, float radius, float half, float alpha) {
        int seg = 8;
        for (int s = 0; s < seg; s++) {
            double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
            float x0 = (float) Math.cos(a0) * radius, z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius, z1 = (float) Math.sin(a1) * radius;
            put(vc, m, 0, y - half, 0, 0.8F, 0.75F, 1F, 0);
            put(vc, m, x0, y, z0, 1F, 0.95F, 1F, alpha);
            put(vc, m, x1, y, z1, 1F, 0.95F, 1F, alpha);
            put(vc, m, 0, y - half, 0, 0.8F, 0.75F, 1F, 0);
            put(vc, m, x0, y, z0, 1F, 0.95F, 1F, alpha);
            put(vc, m, 0, y + half, 0, 0.8F, 0.75F, 1F, 0);
            put(vc, m, 0, y + half, 0, 0.8F, 0.75F, 1F, 0);
            put(vc, m, x1, y, z1, 1F, 0.95F, 1F, alpha);
        }
    }

    /**
     * Paraboloid shell in body space. The apex is 0.45 blocks beyond the top of the head (y =
     * -0.95); the shell opens towards the feet and reaches a radius of 0.9 blocks at the hips.
     * The depth (z) is 75 % of the width (x), around the flat torso.
     */
    private static void deflectionField(Matrix4f m, VertexConsumer vc, float field, float time) {
        float apex = -0.95F, tail = 0.80F;
        int rings = 9, seg = 24;
        for (int r = 0; r < rings; r++) {
            float d0 = (tail - apex) * r / rings, d1 = (tail - apex) * (r + 1) / rings;
            float rad0 = 0.9F * (float) Math.sqrt(d0 / (tail - apex)), rad1 = 0.9F * (float) Math.sqrt(d1 / (tail - apex));
            float y0 = apex + d0, y1 = apex + d1;
            // brighter at the nose, fading towards the tail; flow bands move from nose to tail
            float flow0 = 0.35F + 0.65F * (float) Math.max(0, Math.sin(d0 * 7.5 - time * 9));
            float flow1 = 0.35F + 0.65F * (float) Math.max(0, Math.sin(d1 * 7.5 - time * 9));
            float fade0 = 1 - 0.85F * d0 / (tail - apex), fade1 = 1 - 0.85F * d1 / (tail - apex);
            float a0base = 0.16F * field * fade0 * flow0, a1base = 0.16F * field * fade1 * flow1;
            for (int s = 0; s < seg; s++) {
                double t0 = s * Math.PI * 2 / seg, t1 = (s + 1) * Math.PI * 2 / seg;
                float c0 = (float) Math.cos(t0), s0 = (float) Math.sin(t0), c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
                // hexagonal shimmer: a little brighter on 6 meridians
                float hex0 = 0.75F + 0.25F * (float) Math.pow(Math.abs(Math.cos(t0 * 3)), 8);
                float hex1 = 0.75F + 0.25F * (float) Math.pow(Math.abs(Math.cos(t1 * 3)), 8);
                put(vc, m, c0 * rad0, y0, s0 * rad0 * 0.75F, 0.35F, 0.8F, 1F, a0base * hex0);
                put(vc, m, c1 * rad0, y0, s1 * rad0 * 0.75F, 0.35F, 0.8F, 1F, a0base * hex1);
                put(vc, m, c1 * rad1, y1, s1 * rad1 * 0.75F, 0.35F, 0.8F, 1F, a1base * hex1);
                put(vc, m, c0 * rad1, y1, s0 * rad1 * 0.75F, 0.35F, 0.8F, 1F, a1base * hex0);
            }
        }
    }

    private static void put(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r, float g, float b, float a) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, Math.max(0, Math.min(1, a)));
    }
}

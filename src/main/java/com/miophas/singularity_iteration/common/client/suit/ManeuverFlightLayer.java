// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.suit.QuantumNozzles;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
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
 *   <li><b>Nozzles</b> (0.1.11.66, quantum flight phase R0): the flames leave the ports of the worn suit, from
 *       {@link QuantumNozzles} (back pods or propulsion boosters, crown and tail ports, forearm fins, palms, soles and heels),
 *       each along its own exhaust; a graviton core shows field rings instead of flames. 0.1.11.67 (R1): the outputs come
 *       from {@link com.miophas.singularity_iteration.common.suit.RcsAllocator}: the attitude jets burn in hard pulses.</li>
 *   <li><b>Shock diamonds</b> (0.1.10.2): at full burn the plume grows longer and four bright Mach
 *       nodes (double cones) stand in it, flickering. Each flame is also recorded in world space
 *       for the heat haze of {@code ThrusterFx}.</li>
 *   <li><b>Flame look</b> (0.1.10.31, S-5): the disc and the cone take the colour of the thrust (white, blue, violet,
 *       {@link FlameRules}) and an animated flame strip lies over each cone ({@link ThrusterFlame}); at full thrust the
 *       nozzles throw sparks.</li>
 *   <li><b>Aerodynamic deflection field.</b> In cruise, a streamlined paraboloid shell of energy in
 *       front of the head and along the body. Flow bands run from the nose to the tail. The shell
 *       follows the body pose (the body lies along the path in cruise). The field also lowers the
 *       flight power cost on the server ({@code ManeuverMode.costPerTick}).</li>
 * </ul>
 *
 * <p>Model space: 1 unit = 1 block after the 1/16 scale of the parts; y points down, so the head is
 * at negative y. The layer draws with the additive {@link SiRenderTypes#FIELD_GLOW} render type.
 * It draws nothing when the player is not in maneuver flight. The local player in first person is
 * not rendered by vanilla, so this layer does not run then (0.1.11.5: and nothing stands in for it).
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class ManeuverFlightLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public ManeuverFlightLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** 0.1.10.32 (P-3): detail tier of the player being drawn (render thread only). */
    private static int lodTier;

    /** 0.1.10.31: the flame strips of one player, drawn after the glow pass. */
    private final ThrusterFlame.Strips strips = new ThrusterFlame.Strips();

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
        LightTrails.samplePlayer(pose, model, player, partial);   // 0.1.11.122: the light trails leave the back nozzles
        if (!FcsClientConfig.classicFlightFx()) {
            phase(pose, buffers, player, model, output, partial);
            return;
        }
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        float time = ManeuverModeClient.visualTime(player);
        // 0.1.10.2: full burn (throttle above 0.72) lengthens the plume and lights the shock diamonds
        float thr = ManeuverModeClient.throttleOf(player, partial);
        lodTier = com.miophas.singularity_iteration.common.client.render.FxBudget.tier(player);                    // 0.1.10.32 (P-3)
        float burn = net.minecraft.util.Mth.clamp((thr - 0.72F) / 0.2F, 0, 1);
        int id = player.getId();
        strips.clear();
        // 0.1.11.66 (R0): every flame leaves a port of the worn suit (QuantumNozzles), the way the port looks
        java.util.List<QuantumNozzles.Nozzle> nozzles = NozzleDraw.worn(player);
        float[] outs = ManeuverModeClient.nozzleOutput(player, nozzles, partial);   // 0.1.11.67 (R1): the reaction control
        float kickClassic = ManeuverModeClient.boostKick(player);                       // 0.1.11.68 (R2)
        for (int i = 0; outs != null && i < outs.length; i++) {
            QuantumNozzles.Nozzle n = nozzles.get(i);
            float p = outs[i];
            if (p <= 0.02F || !NozzleDraw.push(pose, model, n)) continue;
            if (n.kind() == QuantumNozzles.Kind.FIELD) NozzleDraw.field(vc, pose, n.size(), p, time, true);
            else if (n.kind() == QuantumNozzles.Kind.RCS) thruster(pose, vc, 0.7F * p, n.size(), time, i, 0, id, thr, strips);
            else thruster(pose, vc, n.kind() == QuantumNozzles.Kind.MAIN ? p * (1 + 0.6F * kickClassic) : p, n.size(), time,
                n.name().hashCode() & 7, burn, id, thr, strips);
            pose.popPose();
        }
        float field = ManeuverModeClient.deflectionField(player, partial);
        if (field > 0.02F) {
            pose.pushPose();
            model.body.translateAndRotate(pose);
            deflectionField(pose.last().pose(), vc, field, time);
            pose.popPose();
        }
        // 0.1.10.31: the textured flames last (taking their buffer ends the glow batch of an immediate source)
        strips.draw(buffers);
    }

    /**
     * 0.1.11.70 (phase warp style phase W0): the phase maneuver flight. Every lit port of the worn suit burns a cone of light
     * along its exhaust (white core, quantum cyan to violet, rings of compressed space sliding out; violet with the burn),
     * with a flare at the nozzle; an attitude jet blinks (a point of light, a ring that grows across its exhaust and a
     * short cone); a graviton core shows its field rings. By day a soft dark bed under the suit lets the light read on a
     * bright sky. 0.1.11.71 (W1): a boost throws a ring of light off each main nozzle and turns its cone violet for a
     * moment; a hard brake burns its jets as steady hot cones; the star streams round a fast flyer are {@code StarStreams}.
     */
    private void phase(PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model,
                       float[] output, float partial) {
        float thr = ManeuverModeClient.throttleOf(player, partial);
        float burn = net.minecraft.util.Mth.clamp((thr - 0.72F) / 0.2F, 0, 1);
        float seconds = (player.tickCount + partial) / 20F, rate = com.miophas.singularity_iteration.common.suit.PhaseRules.rate(thr);
        // the day bed first, flushed, so the light lies on it
        pose.pushPose();
        model.body.translateAndRotate(pose);
        org.joml.Vector3f chest = pose.last().pose().transformPosition(0, 0.45F, 0, new org.joml.Vector3f());
        pose.popPose();
        com.miophas.singularity_iteration.common.client.render.PhaseFx.underlay(buffers.getBuffer(SiRenderTypes.FLAT),
            new net.minecraft.world.phys.Vec3(chest.x, chest.y, chest.z), 1.1F, 1F);
        if (buffers instanceof MultiBufferSource.BufferSource bs) bs.endBatch(SiRenderTypes.FLAT);
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        java.util.List<QuantumNozzles.Nozzle> nozzles = NozzleDraw.worn(player);
        float[] outs = ManeuverModeClient.nozzleOutput(player, nozzles, partial);   // 0.1.11.67 (R1): the reaction control
        float kick = ManeuverModeClient.boostKick(player);
        boolean hardBrake = ManeuverModeClient.brake(player) >= com.miophas.singularity_iteration.common.suit.RcsAllocator.HARD_BRAKE;
        for (int i = 0; outs != null && i < outs.length; i++) {
            QuantumNozzles.Nozzle n = nozzles.get(i);
            float p = outs[i];
            if (p <= 0.02F || !NozzleDraw.push(pose, model, n)) continue;
            Matrix4f m = pose.last().pose();
            if (n.kind() == QuantumNozzles.Kind.RCS && hardBrake) {
                // 0.1.11.71 (W1): a hard brake burns its jets together and steady: hot cones, no blink
                float w = com.miophas.singularity_iteration.common.suit.PhaseRules.width(1F, n.size()) * 1.2F;
                com.miophas.singularity_iteration.common.client.render.PhaseFx.cone(vc, m, 0.5F * (0.6F + 0.4F * n.size()), w, seconds + i * 0.13F, rate * 1.5F, 1F, 1F);
                NozzleDraw.flare(vc, m, w * 1.3F, 0.95F);
                pose.popPose();
                continue;
            }
            if (n.kind() == QuantumNozzles.Kind.FIELD) NozzleDraw.fieldPhase(vc, pose, n.size(), p, seconds);
            else if (n.kind() == QuantumNozzles.Kind.RCS) NozzleDraw.blink(vc, m, n.size(), player.tickCount, partial, seconds, rate);
            else {
                float length = com.miophas.singularity_iteration.common.suit.PhaseRules.length(p, burn, n.size());
                if (n.kind() == QuantumNozzles.Kind.MAIN) length *= 1 + 0.9F * kick;   // 0.1.11.68 (R2): the boost stretch
                float width = com.miophas.singularity_iteration.common.suit.PhaseRules.width(p, n.size());
                boolean main = n.kind() == QuantumNozzles.Kind.MAIN;
                com.miophas.singularity_iteration.common.client.render.PhaseFx.cone(vc, m, length, width, seconds + i * 0.13F, rate,
                    main ? Math.max(burn, kick) : burn, 0.9F);
                NozzleDraw.flare(vc, m, width * (1.1F + (main ? 0.8F * kick : 0F)), 0.55F + 0.4F * p);
                if (main && kick > 0.02F)   // 0.1.11.71 (W1): the boost: a ring of light thrown off the nozzle
                    com.miophas.singularity_iteration.common.client.render.PhaseFx.ringAt(vc, m, 0.04F + 0.25F * (1 - kick),
                        width * (0.6F + 2.4F * (1 - kick)), 0.025F, com.miophas.singularity_iteration.common.suit.PhaseRules.CYAN, kick);
            }
            pose.popPose();
        }
    }

    /** Glow disc and a flame along +y of the posed nozzle (0.1.11.66: at a port of {@link QuantumNozzles}, scaled by its size). */
    private static void thruster(PoseStack pose, VertexConsumer vc, float power, float size, float time, int seed, float burn, int owner,
                                 float throttle, ThrusterFlame.Strips strips) {
        Matrix4f m = pose.last().pose();
        float flicker = 0.85F + 0.15F * (float) Math.sin(time * 37 + seed * 1.7) * (float) Math.sin(time * 23 + seed);
        float p = power * flicker;
        float radius = (0.06F + 0.05F * p) * size, length = (0.12F + 0.42F * p + 0.5F * burn * power) * (0.6F + 0.4F * size);
        // 0.1.10.31 (S-5): disc and cone in the colour of the thrust, and the animated flame strip
        float colour = Math.max(throttle, 0.5F * power);
        ThrusterFlame.glow(vc, m, radius, length, power, colour, flicker, time, owner * 4 + seed,
            lodTier == com.miophas.singularity_iteration.common.client.render.FxLodRules.FAR ? null : strips, com.miophas.singularity_iteration.common.client.render.FxLodRules.segments(10, lodTier));
        boolean fx = FcsClientConfig.thrusterFx() && lodTier == com.miophas.singularity_iteration.common.client.render.FxLodRules.NEAR;
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
                p * (0.7F + 0.5F * burn), Math.max(throttle, 0.5F * power));
        }
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
            // 0.1.11.5: slow, shallow flow (was a hard pulse at 9 rad/s that made the shell throb) and a lower alpha
            float flow0 = 0.7F + 0.3F * (float) (0.5 + 0.5 * Math.cos(d0 * 4.0 - time * 2.4));
            float flow1 = 0.7F + 0.3F * (float) (0.5 + 0.5 * Math.cos(d1 * 4.0 - time * 2.4));
            float fade0 = 1 - 0.85F * d0 / (tail - apex), fade1 = 1 - 0.85F * d1 / (tail - apex);
            float a0base = 0.10F * field * fade0 * flow0, a1base = 0.10F * field * fade1 * flow1;
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

// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.ArmoryRegistry;
import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.armory.WingmanMode;
import com.miophas.singularity_iteration.common.client.render.FxDraw;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Wingman effects (0.1.9.1), drawn after the particles with additive light.
 *
 * <ul>
 *   <li><b>Lock-on.</b> When a Wingman takes a target, a scan laser runs from its visor to the
 *       target and a scan ring sweeps the target from the feet to the head. While the lock holds,
 *       a hologram marker stays on the target: three rotating corner brackets that snap in from
 *       twice their size, a diamond above the head and a ground ring with ticks, in the colour of
 *       the Wingman's mode (offensive red, dispersal amber, escort cyan, Fulton green), and a faint
 *       tether with a travelling pulse.</li>
 *   <li><b>Muzzle flash.</b> Each shot: a lens star, a hot disc and a short flame cone at the gun.</li>
 *   <li><b>Thruster boost.</b> An evasive boost: long jets against the boost direction and an
 *       expanding shock ring behind the drone; 0.1.10.2: shock diamonds stand in the jets, and the
 *       jets and the boot thrusters shimmer with heat haze ({@code ThrusterFx}).</li>
 *   <li><b>Attitude jets.</b> Small jets at the shoulders and hips fire against the drone's
 *       acceleration, so every turn and stop shows.</li>
 *   <li><b>Slash arcs.</b> A melee or blade swing draws a crescent across the front of the suit.</li>
 * </ul>
 * Client option {@code suit_visuals.wingman_fx}; flashes follow the reduced-flashes option.
 */
public final class WingmanFx {
    private WingmanFx() {}

    private static final class State {
        int shots = Integer.MIN_VALUE, boosts = Integer.MIN_VALUE, target = -1;
        int lockTick = -1000, shotTick = -1000, boostTick = -1000;
        Vec3 boostDir = Vec3.ZERO, prevVel = Vec3.ZERO, accel = Vec3.ZERO;
        int seen;
    }

    private static final Int2ObjectOpenHashMap<State> STATES = new Int2ObjectOpenHashMap<>();
    private static int ticks;

    /** Client tick: reads the synced counters, starts effects, spawns the particle parts. */
    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) { STATES.clear(); return; }
        ticks++;
        if (!FcsClientConfig.wingmanFx()) return;
        for (WingmanEntity w : mc.level.getEntitiesOfClass(WingmanEntity.class, mc.player.getBoundingBox().inflate(96))) {
            State s = STATES.computeIfAbsent(w.getId(), k -> new State());
            s.seen = ticks;
            boolean classic = FcsClientConfig.classicFlightFx();
            if (s.shots != Integer.MIN_VALUE && w.shotCount() != s.shots) {
                s.shotTick = ticks;
                Vec3 muzzle = muzzle(w, 1.0F);
                Vec3 look = w.getViewVector(1.0F);
                for (int i = 0; classic && i < 6; i++) {
                    mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, muzzle.x, muzzle.y, muzzle.z,
                        look.x * 0.3 + w.getRandom().nextGaussian() * 0.06, look.y * 0.3 + w.getRandom().nextGaussian() * 0.06,
                        look.z * 0.3 + w.getRandom().nextGaussian() * 0.06);
                }
            }
            if (s.boosts != Integer.MIN_VALUE && w.boostCount() != s.boosts) {
                s.boostTick = ticks;
                Vec3 v = w.getDeltaMovement();
                s.boostDir = v.lengthSqr() > 1.0E-4 ? v.normalize() : w.getViewVector(1.0F).scale(-1);
                for (int i = 0; classic && i < 10; i++) {
                    mc.level.addParticle(ArmoryRegistry.THRUST.get(), w.getX(), w.getY() + 0.9, w.getZ(),
                        -s.boostDir.x * 0.5 + w.getRandom().nextGaussian() * 0.08, -s.boostDir.y * 0.5 + w.getRandom().nextGaussian() * 0.08,
                        -s.boostDir.z * 0.5 + w.getRandom().nextGaussian() * 0.08);
                }
            }
            if (w.lockedTargetId() != s.target) {
                s.target = w.lockedTargetId();
                if (s.target >= 0) s.lockTick = ticks;
            }
            s.shots = w.shotCount();
            s.boosts = w.boostCount();
            Vec3 v = w.getDeltaMovement();
            s.accel = s.accel.scale(0.6).add(v.subtract(s.prevVel).scale(0.4));
            s.prevVel = v;
        }
        STATES.int2ObjectEntrySet().removeIf(e -> ticks - e.getValue().seen > 40);
    }

    /** Muzzle point: in front of the right hand, along the look. */
    static Vec3 muzzle(WingmanEntity w, float partial) {
        Vec3 look = w.getViewVector(partial);
        double yaw = Math.toRadians(Mth.lerp(partial, w.yBodyRotO, w.yBodyRot));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        return w.getPosition(partial).add(0, 1.3, 0).add(right.scale(0.36)).add(look.scale(0.85));
    }

    static float[] modeColour(WingmanMode mode) {
        return switch (mode) {
            case OFFENSIVE -> new float[]{1.0F, 0.32F, 0.22F};
            case DISPERSAL -> new float[]{1.0F, 0.7F, 0.2F};
            case ESCORT -> new float[]{0.3F, 0.9F, 1.0F};
        };
    }

    /** Render pass (AFTER_PARTICLES). */
    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || STATES.isEmpty() || !FcsClientConfig.wingmanFx()) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float now = ticks + partial;
        Vec3 cam = event.getCamera().getPosition();
        Vector3f left = event.getCamera().getLeftVector(), up = event.getCamera().getUpVector();
        float flash = FcsClientConfig.flashScale();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean classic = FcsClientConfig.classicFlightFx();
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        for (var entry : STATES.int2ObjectEntrySet()) {
            Entity e = mc.level.getEntity(entry.getIntKey());
            if (!(e instanceof WingmanEntity w) || !w.isAlive()) continue;
            State s = entry.getValue();
            float[] col = modeColour(w.mode());
            Vec3 pos = w.getPosition(partial);
            // lock-on: scan laser, sweep ring, hologram marker, tether
            Entity t = s.target >= 0 ? mc.level.getEntity(s.target) : null;
            if (t instanceof LivingEntity target && target.isAlive()) {
                float lockAge = now - s.lockTick;
                Vec3 eye = pos.add(0, 1.55, 0);
                Vec3 tc = target.getPosition(partial).add(0, target.getBbHeight() * 0.55, 0);
                if (lockAge < 14) {
                    float a = (1 - lockAge / 14F) * flash;
                    FxDraw.beam(vc, m, eye, tc, cam, 0.035F, col[0], col[1], col[2], 0.9F * a, 0.9F * a);
                    // the scan ring climbs the target
                    float k = Mth.clamp(lockAge / 10F, 0, 1);
                    Vec3 ringAt = target.getPosition(partial).add(0, target.getBbHeight() * k, 0);
                    FxDraw.ring(vc, m, ringAt, new Vec3(0, 1, 0), target.getBbWidth() * 0.75F, 0.12F, col[0], col[1], col[2], 0.9F * a, 28);
                } else {
                    // faint tether with a pulse running to the target
                    FxDraw.line(vc, m, eye, tc, cam, 0.012F, col[0], col[1], col[2], 0.14F);
                    float u = (now * 0.06F) % 1F;
                    Vec3 dot = eye.add(tc.subtract(eye).scale(u));
                    FxDraw.disc(vc, m, dot, tc.subtract(eye), 0.08F, col[0], col[1], col[2], 0.5F, 8);
                }
                marker(vc, m, target, tc, cam, left, up, col, lockAge, now, partial);
            }
            if (!classic) continue;   // 0.1.11.62: the constructivist pass below draws the flight and the strikes
            // muzzle flash
            float shotAge = now - s.shotTick;
            if (shotAge < 4) {
                float a = (1 - shotAge / 4F) * flash;
                Vec3 mz = muzzle(w, partial);
                Vec3 look = w.getViewVector(partial);
                FxDraw.star(vc, m, mz, left, up, 0.55F, 1.0F, 0.86F, 0.55F, 0.9F * a);
                FxDraw.cone(vc, m, mz, look, 0.7F, 0.12F, 1.0F, 0.75F, 0.35F, 0.8F * a, 10);
            }
            // thruster boost
            float boostAge = now - s.boostTick;
            boolean thrusterFx = FcsClientConfig.thrusterFx();
            if (boostAge < 9) {
                float a = 1 - boostAge / 9F;
                Vec3 back = s.boostDir.scale(-1);
                for (int side = -1; side <= 1; side += 2) {
                    Vec3 sideV = FxDraw.perp(s.boostDir).scale(0.18 * side);
                    Vec3 nozzle = pos.add(0, 0.95, 0).add(sideV);
                    float len = 1.6F * a + 0.3F;
                    FxDraw.cone(vc, m, nozzle, back, len, 0.16F, 0.5F, 0.85F, 1.0F, 0.85F * a, 10);
                    if (thrusterFx) {
                        // 0.1.10.2: shock diamonds in the boost jet, and its heat haze
                        for (int i = 0; i < 3; i++) {
                            Vec3 node = nozzle.add(back.scale(len * (0.3 + 0.22 * i)));
                            float na = 0.8F * a * (1 - i * 0.25F);
                            FxDraw.cone(vc, m, node, back, 0.12F, 0.07F - i * 0.012F, 1F, 0.95F, 1F, na, 8);
                            FxDraw.cone(vc, m, node, s.boostDir, 0.12F, 0.07F - i * 0.012F, 1F, 0.95F, 1F, na, 8);
                        }
                        com.miophas.singularity_iteration.common.client.suit.nano.ThrusterFx.jet(w.getId() * 8L + 5 + side, nozzle,
                            nozzle.add(back.scale(len)), a);
                    }
                }
                FxDraw.ring(vc, m, pos.add(0, 0.95, 0).add(back.scale(0.5 + boostAge * 0.12)), s.boostDir, 0.25F + boostAge * 0.17F, 0.2F,
                    0.65F, 0.9F, 1.0F, 0.7F * a, 32);
            }
            // 0.1.10.2: heat haze under the boot thrusters, stronger when the drone climbs or moves fast
            // 0.1.11.51 (F2): the boots follow the pitch of the suit (it rotates round its waist), and at high speed the
            // boot thrusters burn a visible jet behind it, longer with the speed
            Vec3 v = w.getDeltaMovement();
            double yaw = Math.toRadians(Mth.lerp(partial, w.yBodyRotO, w.yBodyRot));
            Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw)), ahead = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
            double pitch = Math.toRadians(w.lean(partial)[0]);
            Vec3 feet = pos.add(0, 0.9, 0).add(ahead.scale(-0.85 * Math.sin(pitch))).add(0, -0.85 * Math.cos(pitch), 0);
            Vec3 aft = ahead.scale(-Math.sin(pitch)).add(0, -Math.cos(pitch), 0);   // out of the soles
            float prone = w.proneBlend(partial);
            for (int side = -1; side <= 1; side += 2) {
                Vec3 boot = feet.add(right.scale(0.13 * side));
                if (thrusterFx) {
                    float thrust = (float) Math.min(1, 0.3 + v.length() * 1.2 + Math.max(0, v.y) * 1.5);
                    com.miophas.singularity_iteration.common.client.suit.nano.ThrusterFx.jet(w.getId() * 8L + 2 + side, boot,
                        boot.add(-v.x * 1.5, -0.55 - Math.max(0, v.y), -v.z * 1.5).add(aft.scale(0.2 * prone)), thrust * 0.6F);
                }
                if (prone > 0.05F) {
                    float len = (float) (0.5 + v.length() * 0.9) * prone;
                    FxDraw.cone(vc, m, boot, aft, len, 0.11F, 0.5F, 0.85F, 1.0F, 0.75F * prone, 10);
                    FxDraw.cone(vc, m, boot, aft, len * 0.45F, 0.06F, 1.0F, 0.97F, 0.9F, 0.85F * prone, 8);
                }
            }
            // attitude jets against the acceleration
            double acc = s.accel.length();
            if (acc > 0.015) {
                Vec3 jet = s.accel.normalize().scale(-1);
                float a = (float) Math.min(1, acc * 9);
                Vec3[] ports = {pos.add(right.scale(0.36)).add(0, 1.38, 0), pos.add(right.scale(-0.36)).add(0, 1.38, 0),
                    pos.add(right.scale(0.2)).add(0, 0.78, 0), pos.add(right.scale(-0.2)).add(0, 0.78, 0)};
                for (Vec3 p : ports) FxDraw.cone(vc, m, p, jet, 0.18F + 0.45F * a, 0.05F, 0.6F, 0.9F, 1.0F, 0.75F * a, 8);
            }
            // slash arc of a melee or blade swing
            float swing = w.getAttackAnim(partial);
            if (swing > 0.02F && !isRanged(w)) slashArc(vc, m, w, pos, swing, partial);
        }
        buffers.endBatch(SiRenderTypes.FIELD_GLOW);
        if (!classic) {
            VertexConsumer light = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
            for (var entry : STATES.int2ObjectEntrySet()) {
                Entity e = mc.level.getEntity(entry.getIntKey());
                if (e instanceof WingmanEntity w && w.isAlive()) phase(light, w, entry.getValue(), now, partial, cam);
            }
            buffers.endBatch(SiRenderTypes.FIELD_GLOW);
        }
        pose.popPose();
    }

    /**
     * 0.1.11.74 (phase warp style phase W4): the flight and the strikes of one Wingman in the phase warp style, in light
     * (camera-relative, additive; {@link com.miophas.singularity_iteration.common.client.render.PhaseFx}): a phase cone out
     * of each sole, longer with speed and in the high-speed pose; the star streams of a fast flyer; a boost is a long hot
     * cone and a ring of light thrown off; attitude jets blink; a shot is a flash and a ring at the muzzle; a melee or blade
     * swing is an arc of light fading from its edge. (0.1.11.62: the constructivist wedges, bars and red band.)
     */
    private static void phase(VertexConsumer vc, WingmanEntity w, State s, float now, float partial, Vec3 cam) {
        Vec3 pos = w.getPosition(partial), v = w.getDeltaMovement();
        float speed = (float) v.length(), seconds = (w.tickCount + partial) / 20F;
        double yaw = Math.toRadians(Mth.lerp(partial, w.yBodyRotO, w.yBodyRot));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw)), ahead = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        double pitch = Math.toRadians(w.lean(partial)[0]);
        Vec3 feet = pos.add(0, 0.9, 0).add(ahead.scale(-0.85 * Math.sin(pitch))).add(0, -0.85 * Math.cos(pitch), 0);
        Vec3 aft = ahead.scale(-Math.sin(pitch)).add(0, -Math.cos(pitch), 0);
        float prone = w.proneBlend(partial);
        float rate = com.miophas.singularity_iteration.common.suit.PhaseRules.rate(Math.min(1F, speed));
        if (com.miophas.singularity_iteration.common.client.legacy.flight.LegacyFlightFx.ENABLED && !w.landed()) {   // 0.1.11.124 (E1): the sole cones are archived (electromagnetic flight)
            float len = com.miophas.singularity_iteration.common.armory.WingmanPose.thrustLength(speed, prone, ticks) * 0.9F;
            for (int side = -1; side <= 1; side += 2) {
                Vec3 sole = feet.add(right.scale(0.13 * side)).subtract(cam);
                com.miophas.singularity_iteration.common.client.render.PhaseFx.cone(vc, frame(sole, aft), len, 0.14F, seconds + side * 0.3F, rate, prone * 0.6F, 0.9F);
                com.miophas.singularity_iteration.common.client.render.PhaseFx.blob(vc, sole, 0.14F, com.miophas.singularity_iteration.common.suit.PhaseRules.CYAN, 0.7F);
            }
        }
        com.miophas.singularity_iteration.common.client.render.StarStreams.drawFor(vc, w, v, partial, cam);
        float boostAge = now - s.boostTick, ring = com.miophas.singularity_iteration.common.armory.WingmanPose.boostRing(boostAge);
        if (ring > 0) {
            float k = boostAge / com.miophas.singularity_iteration.common.armory.WingmanPose.BOOST_CUT;
            Vec3 centre = pos.add(0, 0.95, 0).subtract(cam);
            if (com.miophas.singularity_iteration.common.client.legacy.flight.LegacyFlightFx.ENABLED)   // 0.1.11.124 (E1): the boost cone is archived; its ring of light stays
                com.miophas.singularity_iteration.common.client.render.PhaseFx.cone(vc, frame(centre, s.boostDir.scale(-1)), 2.2F * (1 - k) + 0.4F, 0.4F,
                    seconds, rate * 2, 1F, 1F - 0.6F * k);
            com.miophas.singularity_iteration.common.client.render.PhaseFx.ring(vc, centre, s.boostDir, ring, 0.07F,
                com.miophas.singularity_iteration.common.suit.PhaseRules.CYAN, 1F - k);
        }
        double acc = s.accel.length();
        if (com.miophas.singularity_iteration.common.client.legacy.flight.LegacyFlightFx.ENABLED && acc > 0.03 && ((w.tickCount >> 1) & 1) == 0) {   // attitude jets blink on a pulse (0.1.11.124, E1: archived)
            Vec3 jet = s.accel.normalize().scale(-1);
            float a = (float) Math.min(1, acc * 9);
            Vec3[] ports = {pos.add(right.scale(0.36)).add(0, 1.38, 0), pos.add(right.scale(-0.36)).add(0, 1.38, 0)};
            for (Vec3 p : ports) {
                Vec3 at = p.subtract(cam);
                com.miophas.singularity_iteration.common.client.render.PhaseFx.cone(vc, frame(at, jet), 0.18F + 0.3F * a, 0.08F, seconds, rate, 0F, 0.8F);
                com.miophas.singularity_iteration.common.client.render.PhaseFx.blob(vc, at, 0.1F, com.miophas.singularity_iteration.common.suit.PhaseRules.CYAN, 0.8F);
            }
        }
        float shotAge = now - s.shotTick;
        if (shotAge < 4) {
            Vec3 mz = muzzle(w, partial).subtract(cam), look = w.getViewVector(partial);
            float a = 1 - shotAge / 4F;
            com.miophas.singularity_iteration.common.client.render.PhaseFx.blob(vc, mz, 0.35F, com.miophas.singularity_iteration.common.suit.PhaseRules.CYAN, a);
            com.miophas.singularity_iteration.common.client.render.PhaseFx.ring(vc, mz, look, 0.15F + shotAge * 0.12F, 0.04F,
                com.miophas.singularity_iteration.common.suit.PhaseRules.WHITE, a);
        }
        float swing = w.getAttackAnim(partial);
        if (swing > 0.02F && !isRanged(w)) slashLight(vc, pos.subtract(cam), yaw, swing);
    }

    /** A frame at {@code at} (camera-relative) with +y along {@code dir}. */
    private static Matrix4f frame(Vec3 at, Vec3 dir) {
        Vec3 d = dir.lengthSqr() < 1.0E-8 ? new Vec3(0, -1, 0) : dir.normalize();
        return new Matrix4f().translate((float) at.x, (float) at.y, (float) at.z)
            .rotate(new org.joml.Quaternionf().rotationTo(0, 1, 0, (float) d.x, (float) d.y, (float) d.z));
    }

    /** 0.1.11.74 (W4): the cut in light: an arc over the swept sweep, bright at its leading edge and fading behind it. */
    private static void slashLight(VertexConsumer vc, Vec3 pos, double yaw, float swing) {
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 centre = pos.add(0, 1.15, 0);
        Matrix4f id = new Matrix4f();
        float[] cyan = com.miophas.singularity_iteration.common.suit.PhaseRules.CYAN, white = com.miophas.singularity_iteration.common.suit.PhaseRules.WHITE;
        int seg = 16;
        for (int i = 0; i < seg; i++) {
            float k0 = i / (float) seg, k1 = (i + 1F) / seg;
            if (k1 > swing) break;
            float behind = swing - k1;
            if (behind > 0.5F) continue;
            float a = 1 - behind / 0.5F;
            double a0 = Math.toRadians(-80 + 160 * k0), a1 = Math.toRadians(-80 + 160 * k1);
            Vec3 d0 = fwd.scale(Math.cos(a0)).add(right.scale(Math.sin(a0))).add(0, -0.35 * Math.sin(a0), 0);
            Vec3 d1 = fwd.scale(Math.cos(a1)).add(right.scale(Math.sin(a1))).add(0, -0.35 * Math.sin(a1), 0);
            FxDraw.vertex(vc, id, centre.add(d0.scale(1.0)), cyan[0], cyan[1], cyan[2], 0F);
            FxDraw.vertex(vc, id, centre.add(d0.scale(1.65)), white[0], white[1], white[2], 0.9F * a);
            FxDraw.vertex(vc, id, centre.add(d1.scale(1.65)), white[0], white[1], white[2], 0.9F * a);
            FxDraw.vertex(vc, id, centre.add(d1.scale(1.0)), cyan[0], cyan[1], cyan[2], 0F);
        }
    }

    /** 0.1.10.13: the same test as the combat logic and the arm pose ({@link com.miophas.singularity_iteration.common.armory.WingmanPose#ranged}). */
    private static boolean isRanged(WingmanEntity w) {
        return com.miophas.singularity_iteration.common.armory.WingmanPose.ranged(w.getMainHandItem());
    }

    /** Hologram marker on a locked target. */
    private static void marker(VertexConsumer vc, Matrix4f m, LivingEntity target, Vec3 tc, Vec3 cam, Vector3f leftV, Vector3f upV,
                               float[] col, float lockAge, float now, float partial) {
        float size = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.62F + 0.25F;
        float snap = lockAge < 6 ? 1 + (1 - lockAge / 6F) * 1.2F : 1;
        float alpha = lockAge < 6 ? 0.4F + 0.6F * (lockAge / 6F) : 0.55F + 0.15F * Mth.sin(now * 0.3F);
        Vec3 l = new Vec3(leftV.x(), leftV.y(), leftV.z()), u = new Vec3(upV.x(), upV.y(), upV.z());
        // three corner brackets at 120 degrees, turning in the camera plane
        float spin = now * 0.04F;
        for (int i = 0; i < 3; i++) {
            double a = spin + i * Math.PI * 2 / 3;
            Vec3 radial = l.scale(Math.cos(a)).add(u.scale(Math.sin(a)));
            Vec3 tangent = l.scale(-Math.sin(a)).add(u.scale(Math.cos(a)));
            Vec3 corner = tc.add(radial.scale(size * snap));
            Vec3 arm1 = corner.subtract(radial.scale(size * 0.32)).add(tangent.scale(size * 0.18));
            Vec3 arm2 = corner.subtract(radial.scale(size * 0.32)).subtract(tangent.scale(size * 0.18));
            FxDraw.line(vc, m, corner, arm1, cam, 0.03F, col[0], col[1], col[2], alpha);
            FxDraw.line(vc, m, corner, arm2, cam, 0.03F, col[0], col[1], col[2], alpha);
        }
        // diamond above the head
        Vec3 top = target.getPosition(partial).add(0, target.getBbHeight() + 0.45 + 0.08 * Mth.sin(now * 0.2F), 0);
        float d = 0.13F;
        Vec3 n = top.add(u.scale(d)), s = top.subtract(u.scale(d)), e = top.add(l.scale(d * 0.7)), w = top.subtract(l.scale(d * 0.7));
        FxDraw.line(vc, m, n, e, cam, 0.025F, col[0], col[1], col[2], alpha);
        FxDraw.line(vc, m, e, s, cam, 0.025F, col[0], col[1], col[2], alpha);
        FxDraw.line(vc, m, s, w, cam, 0.025F, col[0], col[1], col[2], alpha);
        FxDraw.line(vc, m, w, n, cam, 0.025F, col[0], col[1], col[2], alpha);
        // ground ring with ticks, turning the other way
        Vec3 foot = target.getPosition(partial).add(0, 0.04, 0);
        float rr = target.getBbWidth() * 0.85F + 0.2F;
        FxDraw.ring(vc, m, foot, new Vec3(0, 1, 0), rr, 0.06F, col[0], col[1], col[2], alpha * 0.8F, 36);
        for (int i = 0; i < 6; i++) {
            double a = -spin * 1.5 + i * Math.PI / 3;
            Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
            FxDraw.line(vc, m, foot.add(dir.scale(rr + 0.05)), foot.add(dir.scale(rr + 0.3)), cam, 0.03F, col[0], col[1], col[2], alpha);
        }
    }

    /** Crescent across the front of the suit, swept by the swing progress. */
    private static void slashArc(VertexConsumer vc, Matrix4f m, WingmanEntity w, Vec3 pos, float swing, float partial) {
        double yaw = Math.toRadians(Mth.lerp(partial, w.yBodyRotO, w.yBodyRot));
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 centre = pos.add(0, 1.15, 0);
        boolean blade = com.miophas.singularity_iteration.common.armory.SlashBladeCombat.isBlade(w.getMainHandItem());
        float r = blade ? 1.0F : 0.85F, g = blade ? 0.75F : 0.95F, b = 1.0F;
        int seg = 18;
        float head = swing;                                   // 0..1 across the arc
        for (int i = 0; i < seg; i++) {
            float k0 = i / (float) seg, k1 = (i + 1F) / seg;
            if (k1 > head) break;
            float fade0 = Mth.clamp(1 - (head - k0) * 2.2F, 0, 1), fade1 = Mth.clamp(1 - (head - k1) * 2.2F, 0, 1);
            double a0 = Math.toRadians(-80 + 160 * k0), a1 = Math.toRadians(-80 + 160 * k1);
            Vec3 d0 = fwd.scale(Math.cos(a0)).add(right.scale(Math.sin(a0))).add(0, -0.35 * Math.sin(a0), 0);
            Vec3 d1 = fwd.scale(Math.cos(a1)).add(right.scale(Math.sin(a1))).add(0, -0.35 * Math.sin(a1), 0);
            Vec3 in0 = centre.add(d0.scale(0.7)), out0 = centre.add(d0.scale(1.9));
            Vec3 in1 = centre.add(d1.scale(0.7)), out1 = centre.add(d1.scale(1.9));
            FxDraw.vertex(vc, m, in0, r, g, b, 0);
            FxDraw.vertex(vc, m, out0, r, g, b, 0.85F * fade0);
            FxDraw.vertex(vc, m, out1, r, g, b, 0.85F * fade1);
            FxDraw.vertex(vc, m, in1, r, g, b, 0);
        }
    }
}

// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit.nano;

import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Propulsion effects (0.1.10.2, client option {@code flight.thruster_fx}).
 *
 * <ul>
 *   <li><b>Heat haze.</b> Every thruster flame of this frame (the palms and soles of a suit in
 *       Special Maneuver flight, the boots and boost jets of the Wingmen) is recorded with its
 *       world position; the refraction pass of the nano effects draws a ribbon of heat haze along
 *       and past each flame, wider and stronger with the thrust. A record lives 150 ms, so a flame
 *       drawn after the refraction pass still shows from the next frame.</li>
 *   <li><b>Landing.</b> When a suit in maneuver flight touches down, a dust ring of the ground block
 *       and cloud puffs runs outward, a ring of haze and a soft light ring spread over the ground,
 *       with a low thump.</li>
 * </ul>
 * The shock diamonds sit in the flames themselves ({@code ManeuverFlightLayer}, {@code WingmanFx}).
 */
public final class ThrusterFx {
    private ThrusterFx() {}

    private record Jet(Vec3 a, Vec3 b, float power, long at) { }
    private record Landing(Vec3 pos, float strength, int born) { }

    private static final Long2ObjectOpenHashMap<Jet> JETS = new Long2ObjectOpenHashMap<>();
    private static final List<Landing> LANDINGS = new ArrayList<>();
    private static final long LIFE_MS = 150;

    /** Records a flame from {@code a} (the nozzle) to {@code b} (the flame tip) for this frame; {@code key} is unique per nozzle. */
    public static void jet(long key, Vec3 a, Vec3 b, float power) {
        if (!FcsClientConfig.thrusterFx() || power <= 0.05F) return;
        JETS.put(key, new Jet(a, b, power, System.currentTimeMillis()));
    }

    /** A suit lands from maneuver flight. */
    public static void landing(Entity e, float strength) {
        if (!FcsClientConfig.thrusterFx()) return;
        var level = e.level();
        Vec3 at = e.position();
        LANDINGS.add(new Landing(at, strength, NanoClient.ticks));
        if (LANDINGS.size() > 6) LANDINGS.remove(0);
        var state = level.getBlockState(BlockPos.containing(at).below());
        int n = 36;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            double c = Math.cos(a), s = Math.sin(a);
            if (!state.isAir()) level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x + c * 0.5, at.y + 0.05, at.z + s * 0.5,
                c * 0.4 * strength, 0.08, s * 0.4 * strength);
            if (i % 2 == 0) level.addParticle(ParticleTypes.CLOUD, at.x + c * 0.4, at.y + 0.1, at.z + s * 0.4, c * 0.25 * strength, 0.01, s * 0.25 * strength);
        }
        level.playLocalSound(at.x, at.y, at.z, mio_icif_sounds.THRUSTER_LAND.get(), SoundSource.PLAYERS, 0.5F + 0.5F * strength,
            0.9F + level.random.nextFloat() * 0.2F, false);
    }

    static boolean hazeActive() {
        prune();
        return !JETS.isEmpty() || !LANDINGS.isEmpty();
    }

    static boolean glowActive() {
        return !LANDINGS.isEmpty();
    }

    private static void prune() {
        long now = System.currentTimeMillis();
        JETS.values().removeIf(j -> now - j.at > LIFE_MS);
        LANDINGS.removeIf(l -> NanoClient.ticks - l.born > 16);
    }

    /** Refraction pass: heat haze along each flame and the landing haze rings. */
    static void haze(VertexConsumer vc, Matrix4f m, Vec3 cam, float partial, Minecraft mc) {
        for (Jet j : JETS.values()) {
            Vec3 axis = j.b.subtract(j.a);
            if (axis.lengthSqr() < 1.0E-6) continue;
            if (j.a.distanceToSqr(cam) < 1.2 * 1.2) continue;                  // not over the camera
            // the haze starts at the nozzle and runs on past the flame tip, widening
            Vec3 far = j.b.add(axis.scale(1.6));
            float s = Mth.clamp(j.power, 0, 1);
            NanoFx.ribbon(vc, m, j.a, j.b, cam, 0.16F + 0.18F * s, 1.0F, 0.6F, 0.4F, 0.55F * s, 0.75F * s, true);
            NanoFx.ribbon(vc, m, j.b, far, cam, 0.3F + 0.3F * s, 1.0F, 0.6F, 0.4F, 0.75F * s, 0, true);
        }
        for (Landing l : LANDINGS) {
            float age = NanoClient.ticks - l.born + partial;
            float k = Mth.clamp(age / 16F, 0, 1);
            NanoFx.ringFlat(vc, m, l.pos.add(0, 0.08, 0), 0.4F + 3.2F * (1 - (1 - k) * (1 - k)) * l.strength, 0.6F, 1F, 0.6F, 0.4F,
                0.9F * (1 - k) * l.strength, 40, true);
        }
    }

    /** Glow pass: the soft light ring of a landing. */
    static void glow(VertexConsumer vc, Matrix4f m, Vec3 cam, float partial) {
        for (Landing l : LANDINGS) {
            float age = NanoClient.ticks - l.born + partial;
            float k = Mth.clamp(age / 12F, 0, 1);
            NanoFx.ringFlat(vc, m, l.pos.add(0, 0.05, 0), 0.3F + 2.4F * k * l.strength, 0.14F, 0.45F, 0.8F, 1F, 0.6F * (1 - k), 48, false);
        }
    }

    static void clear() {
        JETS.clear();
        LANDINGS.clear();
    }
}

// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.render.ConstructBursts;
import com.miophas.singularity_iteration.common.suit.FlightImpact;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 0.1.11.68 (quantum flight phase R2): the weight of the quantum flight on the client. The events come from
 * {@link ManeuverModeClient}; the rules from {@link FlightImpact}.
 * <ul>
 *   <li><b>Hard landing</b>: the ground cracks (a star of black wedges with red cores) under a heavy double ring, a deep
 *       smash, and the camera shakes (also for a viewer nearby, less with the distance).</li>
 *   <li><b>Take-off</b>: a short star of white wedges blasted into the ground, a light shake.</li>
 *   <li><b>Boost</b>: the view widens at once and settles; the main wedges of the flyer stretch for a moment (flight
 *       layer, {@link ManeuverModeClient#boostKick}).</li>
 *   <li><b>Hard brake</b>: the camera nods forward and the view narrows; the brake jets fire together.</li>
 *   <li><b>Wall hit</b> at speed: cracks in the wall, a ring out of it, a blast and a heavy shake.</li>
 * </ul>
 * Options {@code impact_shake} and {@code impact_fov} (0..2). The classic style keeps its dust rings in place of the
 * cracks and stars; the shake and the kicks are in both styles.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class FlightImpactClient {
    private FlightImpactClient() {}

    private static float trauma;
    private static long boostAt = Long.MIN_VALUE / 2, brakeAt = Long.MIN_VALUE / 2;
    private static float brakeSize;
    private static long gatherAt = Long.MIN_VALUE / 2, jumpAt = Long.MIN_VALUE / 2;

    private static long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    private static boolean local(Player p) { return p == Minecraft.getInstance().player; }

    /** Adds trauma for the local view: full for the own event, less with the distance for another flyer's (none past 16). */
    private static void shakeFrom(Player p, float amount) {
        Player me = Minecraft.getInstance().player;
        if (me == null) return;
        float k = local(p) ? 1F : (float) Math.max(0, 1 - me.distanceTo(p) / 16.0) * 0.6F;
        if (k > 0) trauma = FlightImpact.add(trauma, amount * k);
    }

    public static void hardLanding(Player p, float strength) {
        Vec3 at = p.position();
        if (!FcsClientConfig.classicFlightFx()) ConstructBursts.hardLanding(at, strength);
        p.level().playLocalSound(at.x, at.y, at.z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS,
            0.6F + 0.6F * strength, 0.75F + p.level().random.nextFloat() * 0.1F, false);
        shakeFrom(p, 0.35F + 0.55F * strength);
    }

    public static void takeoff(Player p) {
        if (!FcsClientConfig.classicFlightFx()) ConstructBursts.takeoffBurst(p.position());
        shakeFrom(p, 0.22F);
    }

    public static void wallHit(Player p, Vec3 at, Vec3 normal, float strength) {
        if (!FcsClientConfig.classicFlightFx()) ConstructBursts.wallHit(at, normal, strength);
        p.level().playLocalSound(at.x, at.y, at.z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.0F + strength, 0.6F, false);
        p.level().playLocalSound(at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.35F + 0.4F * strength, 0.65F, false);
        shakeFrom(p, 0.6F + 0.4F * strength);
    }

    public static void boost(Player p) {
        if (local(p)) boostAt = now();
    }

    public static void brake(Player p, float size) {
        if (!local(p)) return;
        brakeAt = now();
        brakeSize = Math.max(0F, Math.min(1F, size));
    }

    /** 0.1.11.72 (W2): the gather before a warp: the own view narrows a little. */
    public static void gather(Player p) {
        if (!local(p)) return;
        gatherAt = now();
    }

    /** 0.1.11.72 (W2): the warp at the barrier: the own view widens hard and shakes a little; a viewer nearby feels it. */
    public static void jump(Player p) {
        if (local(p)) jumpAt = now();
        shakeFrom(p, 0.3F);
    }

    /** The trauma of the local view now (for tests and the HUD). */
    public static float trauma() { return trauma; }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        if (trauma > 0) trauma = FlightImpact.decay(trauma);
    }

    @SubscribeEvent
    static void camera(ViewportEvent.ComputeCameraAngles event) {
        float scale = FcsClientConfig.impactShake();
        if (scale <= 0) return;
        float partial = (float) event.getPartialTick();
        if (trauma > 0) {
            float[] a = FlightImpact.shake(trauma, (now() + partial) / 20F, scale);
            event.setYaw(event.getYaw() + a[0]);
            event.setPitch(event.getPitch() + a[1]);
            event.setRoll(event.getRoll() + a[2]);
        }
        float nod = FlightImpact.fovKick(now() - brakeAt + partial, 3.5F * brakeSize, scale);
        if (nod != 0) event.setPitch(event.getPitch() + nod);
    }

    @SubscribeEvent
    static void fov(ComputeFovModifierEvent event) {
        float scale = FcsClientConfig.impactFov();
        if (scale <= 0) return;
        float kick = FlightImpact.fovKick(now() - boostAt, FlightImpact.BOOST_KICK, scale)
            + FlightImpact.fovKick(now() - brakeAt, FlightImpact.BRAKE_KICK * brakeSize, scale)
            + FlightImpact.fovKick((now() - gatherAt) / 1.5F, -0.04F, scale)          // 0.1.11.72: the gather draws the view in
            + FlightImpact.fovKick((now() - jumpAt) / 1.6F, 0.16F, scale);            // and the warp throws it wide
        if (kick != 0) event.setNewFovModifier(event.getNewFovModifier() * (1 + kick));
    }
}

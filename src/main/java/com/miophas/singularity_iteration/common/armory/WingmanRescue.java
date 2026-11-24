// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.56 (gaze and Iron Man phase P4, decisions A / A / A): emergency rescue. Every tick the server checks each owner
 * of Wingmen for two dangers:
 * <ul>
 *   <li><b>A lethal fall</b>: falling (not on the ground, not gliding or flying, no Slow Falling or Levitation, not in a
 *       fluid, sinking faster than {@link #FALL_SPEED}) towards ground whose fall damage ({@link #fallDamage}) would take
 *       all the owner's health, or into the void.</li>
 *   <li><b>Near death</b>: health at most {@link #LOW_HEALTH} of the maximum while wearing fewer than two armour
 *       pieces.</li>
 * </ul>
 * On a danger the nearest Wingman that can help (active, not swapping or rescuing, out of its {@link #COOLDOWN}) is sent:
 * in a fall it races to catch the owner (stops the fall, Slow Falling as a safety net) and then swaps suits when the owner
 * wears less armour than it; near death it comes to swap suits at once. Config {@code Wingmen.emergencyRescue}.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class WingmanRescue {
    private WingmanRescue() {}

    public enum Danger { NONE, FALL, LOW_HEALTH }

    /** Sinking speed (blocks per tick) from which a fall is checked. */
    public static final double FALL_SPEED = 0.5;
    /** Fraction of the maximum health counted as near death. */
    public static final float LOW_HEALTH = 0.25F;
    /** Ticks between two rescues of one Wingman. */
    public static final int COOLDOWN = 1200;
    /** Farthest a rescuing Wingman may be, blocks. */
    public static final double RANGE = 64;
    /** Blocks below the owner searched for ground; beyond it the fall counts as this deep. */
    static final double PROBE = 64;

    /** Fall damage of a fall of {@code distance} blocks with the given safe distance and multiplier (vanilla's rule). */
    public static float fallDamage(double distance, double safe, double multiplier) {
        return (float) Math.max(0, Math.ceil((distance - safe) * multiplier));
    }

    /** The danger the owner is in now. */
    public static Danger danger(ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator() || p.isCreative()) return Danger.NONE;
        if (falling(p) && lethalFall(p)) return Danger.FALL;
        if (p.getHealth() <= p.getMaxHealth() * LOW_HEALTH && armourPieces(p) < 2) return Danger.LOW_HEALTH;
        return Danger.NONE;
    }

    static boolean falling(ServerPlayer p) {
        return !p.onGround() && !p.isFallFlying() && !p.getAbilities().flying && !p.isInLiquid() && !p.isPassenger()
            && !p.hasEffect(MobEffects.SLOW_FALLING) && !p.hasEffect(MobEffects.LEVITATION) && sinkSpeed(p) < -FALL_SPEED;
    }

    /** Last position of each owner, and the vertical move of the last tick (the server does not keep a player's velocity). */
    private static final java.util.Map<java.util.UUID, Vec3> LAST = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, Double> MOVED_Y = new java.util.HashMap<>();

    /** The owner's vertical speed: the measured move of the last tick, or the server velocity when that is lower (tests). */
    static double sinkSpeed(ServerPlayer p) {
        return Math.min(MOVED_Y.getOrDefault(p.getUUID(), 0.0), p.getDeltaMovement().y);
    }

    static boolean lethalFall(ServerPlayer p) {
        Vec3 from = p.position();
        BlockHitResult hit = p.level().clip(new ClipContext(from, from.add(0, -PROBE, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, p));
        double drop;
        if (hit.getType() == HitResult.Type.MISS) {
            if (from.y - PROBE < p.level().getMinBuildHeight()) return true;   // the void
            drop = PROBE;
        } else {
            if (!p.level().getFluidState(hit.getBlockPos()).isEmpty()) return false;   // water (or another fluid) below
            drop = from.y - hit.getLocation().y;
        }
        float damage = fallDamage(p.fallDistance + drop, p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE),
            p.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
        return damage >= p.getHealth() + p.getAbsorptionAmount();
    }

    static int armourPieces(ServerPlayer p) {
        int n = 0;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
            if (!p.getItemBySlot(s).isEmpty()) n++;
        return n;
    }

    /** The Wingman sent to {@code p} for {@code danger}, or null (none can help, or one is on the way). */
    @Nullable
    public static WingmanEntity check(ServerPlayer p) {
        if (!WingmanConfig.emergencyRescue()) return null;
        Danger danger = danger(p);
        if (danger == Danger.NONE) return null;
        WingmanEntity best = null;
        double bestD = RANGE * RANGE;
        long now = p.level().getGameTime();
        for (WingmanEntity w : WingmanService.of(p)) {
            if (w.rescuing() || w.swapPhase() != WingmanEntity.SWAP_NONE) return null;   // one is on the way (or swapping suits)
            if (!w.canRescue(now, danger) || w.level() != p.level()) continue;
            double d = w.distanceToSqr(p);
            if (d < bestD) { bestD = d; best = w; }
        }
        if (best != null) best.startRescue(danger, p);
        return best;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!WingmanService.hasAny(p)) { LAST.remove(p.getUUID()); MOVED_Y.remove(p.getUUID()); return; }
        Vec3 now = p.position(), last = LAST.put(p.getUUID(), now);
        double dy = last == null ? 0 : now.y - last.y;
        MOVED_Y.put(p.getUUID(), Math.abs(dy) > 12 ? 0 : dy);   // a teleport is no fall
        check(p);
    }

    @SubscribeEvent
    static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
        MOVED_Y.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        LAST.clear();
        MOVED_Y.clear();
    }
}

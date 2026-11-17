// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 0.1.11.69 (quantum flight phase R3, decision four B: push only): the shockwave of a hard landing. A player who flew in
 * Special Maneuver Mode (armed quantum chestplate, flying) a moment ago and touches the ground now, after a fall of at
 * least {@link FlightImpact#HARD_FALL} blocks in one of the last two ticks, pushes the creatures around away from the touchdown, by
 * {@link FlightImpact#shove} (less with their knockback resistance). Nothing is hurt and no block is touched; players,
 * armour stands, spectators and the lander's own Wingmen stay put. The fall is measured from the server positions (the
 * motion of a server player is not reliable). The client ends the flight a tick before the touchdown reaches the
 * server, so a touchdown counts up to {@link #GRACE} ticks after the last tick of flight.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class LandingShockwave {
    private LandingShockwave() {}

    /** The last tick of a player: height, on the ground, the fall of that tick (blocks, + down), the tick of the last flight. */
    private record Last(double y, boolean ground, double fall, int flewAt) { }

    private static final Map<UUID, Last> LAST = new HashMap<>();

    /** Ticks after the last tick of flight in which a touchdown still counts (the end of the flight reaches the server first). */
    public static final int GRACE = 3;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Last last = LAST.get(player.getUUID());
        boolean flying = ManeuverMode.isArmed(player) && player.getAbilities().flying && !player.onGround() && !player.isSpectator();
        double fall = last == null ? 0 : last.y() - player.getY();
        int flewAt = flying ? player.tickCount : last == null ? Integer.MIN_VALUE / 2 : last.flewAt();
        LAST.put(player.getUUID(), new Last(player.getY(), player.onGround(), fall, flewAt));
        if (last == null) return;
        float strength = touchdown(player.tickCount - last.flewAt(), last.ground(), player.onGround(), Math.max(fall, last.fall()));
        if (strength > 0) shockwave(player, strength);
    }

    /**
     * The strength of a touchdown (0: none): on the ground now and not in the last tick, at most {@link #GRACE} ticks
     * after flying, after a hard fall (the larger of the last two ticks, blocks per tick, + down; {@link FlightImpact#landing}).
     */
    public static float touchdown(int ticksSinceFlight, boolean wasGround, boolean onGround, double fall) {
        if (!onGround || wasGround || ticksSinceFlight < 0 || ticksSinceFlight > GRACE || fall <= 0) return 0F;
        return FlightImpact.landing((float) fall);
    }

    /** Pushes the creatures around {@code player} for a landing of {@code strength} (0..1); returns how many moved. */
    public static int shockwave(ServerPlayer player, float strength) {
        Vec3 at = player.position();
        float r = FlightImpact.shoveRadius(strength);
        int moved = 0;
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r, 2.5, r),
                e -> e != player && e.isAlive() && !(e instanceof Player) && !(e instanceof ArmorStand) && !e.isSpectator()
                    && !(e instanceof WingmanEntity w && player.getUUID().equals(w.owner())))) {
            Vec3 away = e.position().subtract(at);
            Vec3 flat = new Vec3(away.x, 0, away.z);
            float d = (float) flat.length();
            float[] push = FlightImpact.shove(d, strength);
            if (push[0] <= 0) continue;
            double resist = Math.max(0, Math.min(1, e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
            Vec3 dir = d < 1.0E-3 ? new Vec3(1, 0, 0) : flat.scale(1 / d);
            e.setDeltaMovement(e.getDeltaMovement().add(dir.scale(push[0] * (1 - resist))).add(0, push[1] * (1 - resist), 0));
            e.hasImpulse = true;
            e.hurtMarked = true;
            moved++;
        }
        return moved;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }
}

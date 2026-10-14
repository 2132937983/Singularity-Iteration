// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Smart Trajectory Guidance, server side (0.1.8.0).
 *
 * <h2>Specification</h2>
 * <ul>
 *   <li>The client sends the ids of the creatures locked in its HUD frame ({@link Locks}, at most
 *       {@link #MAX_LOCKS}). The server checks each target when a projectile uses it: alive, same
 *       level, within {@link #RANGE} blocks, not the shooter, not allied, not a pet of the
 *       shooter, and a player only with PvP on.</li>
 *   <li>Each new projectile of the wearer (arrows, tridents, thrown items, energy bolts, the
 *       projectiles of other mods: any {@link Projectile} with the wearer as owner) takes the next
 *       locked target in turn (multishot spreads over the locks) and costs {@link #GUIDED_EU} EU
 *       from the suit. Without energy the projectile flies unguided.</li>
 *   <li>At launch the projectile turns to the lead point of its target (pre-lock: an energy bolt
 *       flies straight to it). In flight it turns at most {@link #TURN_DEGREES} degrees per tick
 *       to the lead point and keeps its speed (the turn also cancels gravity). It stops homing
 *       after {@link #LIFETIME} ticks, when it stops (hit, stuck) or when the target dies.</li>
 *   <li>Vernier thruster sparks show behind a guided projectile.</li>
 * </ul>
 *
 * <p>Cost: one map entry per guided projectile, steered once per server tick. Entries go away
 * with their projectile; the maps are cleared on logout and server stop.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class SmartGuidance {
    private SmartGuidance() {}

    public static final long GUIDED_EU = 300;
    public static final int MAX_LOCKS = 6, LIFETIME = 200;
    public static final double RANGE = 96, TURN_DEGREES = 14;
    /** 0.1.10.1: most locks any wearer can hold (Mk.II 10 + Predictive Guidance 2): the packet limit. */
    public static final int LOCK_LIMIT = 12;

    private static final Map<UUID, int[]> LOCKS = new HashMap<>();
    private static final Map<UUID, Integer> NEXT = new HashMap<>();
    private record Guided(Projectile projectile, int target, int born, double turn, boolean predictive) { }
    private static final List<Guided> GUIDED = new ArrayList<>();

    static void setLocks(ServerPlayer player, int[] ids) {
        if (ids.length == 0) LOCKS.remove(player.getUUID());
        else LOCKS.put(player.getUUID(), ids.length > maxLocks(player) ? java.util.Arrays.copyOf(ids, maxLocks(player)) : ids);
    }

    /**
     * 0.1.10.1: locks the wearer may hold: {@link #MAX_LOCKS}, 10 with a Mk.II unit, +2 with the
     * Predictive Guidance synergy. Both sides use it (the client lock frame, the server check).
     */
    public static int maxLocks(Player player) {
        int base = SuitModules.tier(player, SuitModuleType.SMART_GUIDANCE) >= 2 ? 10 : MAX_LOCKS;
        return base + (com.miophas.singularity_iteration.common.suit.quantum.QuantumLinks.active(player,
            com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergy.PREDICTIVE_GUIDANCE) ? 2 : 0);
    }

    /** 0.1.10.1: degrees a guided shot may turn per tick: {@link #TURN_DEGREES}, 20 with Mk.II, +4 with Predictive Guidance. */
    public static double turnDegrees(Player player) {
        double base = SuitModules.tier(player, SuitModuleType.SMART_GUIDANCE) >= 2 ? 20 : TURN_DEGREES;
        return base + (com.miophas.singularity_iteration.common.suit.quantum.QuantumLinks.active(player,
            com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergy.PREDICTIVE_GUIDANCE) ? 4 : 0);
    }

    /** 0.1.10.0: the entity ids the client of {@code player} has locked (a copy; may hold stale ids: check each with {@link #valid}). */
    public static int[] locks(ServerPlayer player) {
        int[] ids = LOCKS.get(player.getUUID());
        return ids == null ? new int[0] : ids.clone();
    }

    /** True when {@code target} may be a guidance target of {@code player}. */
    public static boolean valid(ServerPlayer player, @Nullable Entity target) {
        if (!(target instanceof LivingEntity living) || !living.isAlive() || target == player || target.isSpectator()) return false;
        if (target.level() != player.level() || target.distanceToSqr(player) > RANGE * RANGE) return false;
        if (player.isAlliedTo(target)) return false;
        if (target instanceof OwnableEntity own && player.getUUID().equals(own.getOwnerUUID())) return false;
        if (target instanceof Player other && (!player.server.isPvpAllowed() || other.isCreative())) return false;
        // 0.1.8.5: a nano EMP breaks the locks on its wearer for a few seconds
        if (target instanceof Player other && com.miophas.singularity_iteration.common.suit.nano.NanoSuit.lockJammed(other)) return false;
        // 0.1.8.6: a worn jammer keeps the lock off its wearer
        if (target instanceof Player other && com.miophas.singularity_iteration.common.suit.nano.NanoSuit.jammerBlocksLock(other)) return false;
        return true;
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk() || !(event.getEntity() instanceof Projectile p)) return;
        if (!(p.getOwner() instanceof ServerPlayer player)) return;
        int[] locks = LOCKS.get(player.getUUID());
        if (locks == null || locks.length == 0 || !SuitModules.isActive(player, SuitModuleType.SMART_GUIDANCE)) return;
        // 0.1.8.5: a nano EMP in range scrambles the guidance of other players for a few seconds
        if (com.miophas.singularity_iteration.common.suit.nano.NanoSuit.guidanceJammed(player)) return;
        LivingEntity target = null;
        int start = NEXT.getOrDefault(player.getUUID(), 0);
        for (int k = 0; k < locks.length && target == null; k++) {
            int index = Math.floorMod(start + k, locks.length);
            Entity e = player.level().getEntity(locks[index]);
            if (valid(player, e)) { target = (LivingEntity) e; NEXT.put(player.getUUID(), index + 1); }
        }
        if (target == null) return;
        var piece = SuitModules.wornPieceWith(player, SuitModuleType.SMART_GUIDANCE);
        if (piece.isEmpty() || SuitModules.extract(player, piece, GUIDED_EU) < GUIDED_EU) return;
        // pre-lock: the projectile leaves already pointed at the lead point
        Vec3 v = p.getDeltaMovement();
        double speed = v.length();
        if (speed > 0.05) {
            Vec3 aim = lead(target, p.position(), speed, predictive(player)).subtract(p.position());
            if (aim.lengthSqr() > 1.0E-4) p.setDeltaMovement(aim.normalize().scale(speed));
        }
        GUIDED.add(new Guided(p, target.getId(), p.tickCount, turnDegrees(player), predictive(player)));
        if (GUIDED.size() > 512) GUIDED.remove(0);
    }

    @SubscribeEvent
    static void onTick(ServerTickEvent.Post event) {
        if (GUIDED.isEmpty()) return;
        for (Iterator<Guided> it = GUIDED.iterator(); it.hasNext(); ) {
            Guided g = it.next();
            Projectile p = g.projectile();
            if (p.isRemoved() || p.tickCount - g.born() > LIFETIME || !(p.level() instanceof ServerLevel level)) { it.remove(); continue; }
            Entity e = level.getEntity(g.target());
            Vec3 v = p.getDeltaMovement();
            double speed = v.length();
            if (!(e instanceof LivingEntity target) || !target.isAlive() || speed < 0.05) { it.remove(); continue; }
            Vec3 want = lead(target, p.position(), speed, g.predictive()).subtract(p.position());
            double dist = want.length();
            if (dist < 1.0E-3) continue;
            Vec3 now = v.scale(1 / speed), goal = want.scale(1 / dist);
            double angle = Math.acos(Math.max(-1, Math.min(1, now.dot(goal))));
            double max = Math.toRadians(g.turn());
            Vec3 dir = angle <= max ? goal : now.scale(1 - max / angle).add(goal.scale(max / angle)).normalize();
            p.setDeltaMovement(dir.scale(speed));
            p.hurtMarked = true;                                        // send the new velocity to the clients
            if (p.tickCount % 2 == 0) {
                Vec3 back = p.position().subtract(dir.scale(0.35));
                level.sendParticles(com.miophas.singularity_iteration.common.armory.ArmoryRegistry.CORE.get(), back.x, back.y, back.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    /** Lead point: the target centre after the time of flight at the current speed. */
    static Vec3 lead(LivingEntity target, Vec3 from, double speed) {
        Vec3 centre = target.getBoundingBox().getCenter();
        double t = from.distanceTo(centre) / Math.max(0.05, speed);
        Vec3 vel = target.getDeltaMovement();
        if (target.onGround()) vel = new Vec3(vel.x, 0, vel.z);
        return centre.add(vel.scale(Math.min(t, 40)));
    }

    /** 0.1.10.1: the lead point; with Predictive Guidance the predicted one ({@link #predictiveLead}). */
    static Vec3 lead(LivingEntity target, Vec3 from, double speed, boolean predictive) {
        return predictive ? predictiveLead(target, from, speed) : lead(target, from, speed);
    }

    static boolean predictive(Player player) {
        return com.miophas.singularity_iteration.common.suit.quantum.QuantumLinks.active(player,
            com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergy.PREDICTIVE_GUIDANCE);
    }

    /**
     * 0.1.10.1 Predictive Guidance: the intercept point solved in three passes (the time of flight
     * to the predicted point, then the point at that time, and so on). A creature that walks a
     * path is predicted along its path nodes at its current ground speed, so the shot gets ahead
     * of it round corners; other targets keep the straight-line prediction.
     */
    static Vec3 predictiveLead(LivingEntity target, Vec3 from, double speed) {
        Vec3 centre = target.getBoundingBox().getCenter();
        double lift = centre.y - target.getY();
        Vec3 aim = centre;
        for (int i = 0; i < 3; i++) {
            double t = Math.min(40, from.distanceTo(aim) / Math.max(0.05, speed));
            aim = predictedFeet(target, t).add(0, lift, 0);
        }
        return aim;
    }

    /** Feet position of {@code target} after {@code ticks}: along its navigation path when it walks one, else by its velocity. */
    static Vec3 predictedFeet(LivingEntity target, double ticks) {
        Vec3 vel = target.getDeltaMovement();
        double ground = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (target instanceof net.minecraft.world.entity.Mob mob && ground > 0.01) {
            var path = mob.getNavigation().getPath();
            if (path != null && !path.isDone()) {
                Vec3 at = target.position();
                double left = ground * ticks;
                for (int i = path.getNextNodeIndex(); i < path.getNodeCount() && left > 0; i++) {
                    Vec3 node = Vec3.atBottomCenterOf(path.getNodePos(i));
                    double d = node.distanceTo(at);
                    if (d >= left) return at.add(node.subtract(at).scale(left / d));
                    left -= d;
                    at = node;
                }
                return at;
            }
        }
        if (target.onGround()) vel = new Vec3(vel.x, 0, vel.z);
        return target.position().add(vel.scale(ticks));
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LOCKS.remove(event.getEntity().getUUID());
        NEXT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onStopped(ServerStoppedEvent event) {
        LOCKS.clear();
        NEXT.clear();
        GUIDED.clear();
    }

    // ------------------------------------------------------------------ packet
    /**
     * Client to server: the locked target ids of the HUD frame (0.1.8.0). Sent on change.
     * Fields (client values, not trusted): at most {@link #MAX_LOCKS} entity ids. The server
     * checks every id when it uses it ({@link #valid}). Rate limit:
     * {@code PacketRateLimit.SMART_GUIDANCE}.
     */
    public record Locks(int[] ids) implements CustomPacketPayload {
        public static final Type<Locks> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "guidance_locks"));
        public static final StreamCodec<ByteBuf, Locks> CODEC = StreamCodec.of((b, p) -> {
            FriendlyByteBuf f = new FriendlyByteBuf(b);
            int n = Math.min(LOCK_LIMIT, p.ids.length);
            f.writeVarInt(n);
            for (int i = 0; i < n; i++) f.writeVarInt(p.ids[i]);
        }, b -> {
            FriendlyByteBuf f = new FriendlyByteBuf(b);
            int n = f.readVarInt();
            if (n < 0 || n > LOCK_LIMIT) throw new io.netty.handler.codec.DecoderException("too many guidance locks: " + n);
            int[] ids = new int[n];
            for (int i = 0; i < n; i++) ids[i] = f.readVarInt();
            return new Locks(ids);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}

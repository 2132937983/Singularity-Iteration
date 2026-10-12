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

    private static final Map<UUID, int[]> LOCKS = new HashMap<>();
    private static final Map<UUID, Integer> NEXT = new HashMap<>();
    private record Guided(Projectile projectile, int target, int born) { }
    private static final List<Guided> GUIDED = new ArrayList<>();

    static void setLocks(ServerPlayer player, int[] ids) {
        if (ids.length == 0) LOCKS.remove(player.getUUID());
        else LOCKS.put(player.getUUID(), ids);
    }

    /** True when {@code target} may be a guidance target of {@code player}. */
    public static boolean valid(ServerPlayer player, @Nullable Entity target) {
        if (!(target instanceof LivingEntity living) || !living.isAlive() || target == player || target.isSpectator()) return false;
        if (target.level() != player.level() || target.distanceToSqr(player) > RANGE * RANGE) return false;
        if (player.isAlliedTo(target)) return false;
        if (target instanceof OwnableEntity own && player.getUUID().equals(own.getOwnerUUID())) return false;
        if (target instanceof Player other && (!player.server.isPvpAllowed() || other.isCreative())) return false;
        return true;
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk() || !(event.getEntity() instanceof Projectile p)) return;
        if (!(p.getOwner() instanceof ServerPlayer player)) return;
        int[] locks = LOCKS.get(player.getUUID());
        if (locks == null || locks.length == 0 || !SuitModules.isActive(player, SuitModuleType.SMART_GUIDANCE)) return;
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
            Vec3 aim = lead(target, p.position(), speed).subtract(p.position());
            if (aim.lengthSqr() > 1.0E-4) p.setDeltaMovement(aim.normalize().scale(speed));
        }
        GUIDED.add(new Guided(p, target.getId(), p.tickCount));
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
            Vec3 want = lead(target, p.position(), speed).subtract(p.position());
            double dist = want.length();
            if (dist < 1.0E-3) continue;
            Vec3 now = v.scale(1 / speed), goal = want.scale(1 / dist);
            double angle = Math.acos(Math.max(-1, Math.min(1, now.dot(goal))));
            double max = Math.toRadians(TURN_DEGREES);
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
            int n = Math.min(MAX_LOCKS, p.ids.length);
            f.writeVarInt(n);
            for (int i = 0; i < n; i++) f.writeVarInt(p.ids[i]);
        }, b -> {
            FriendlyByteBuf f = new FriendlyByteBuf(b);
            int n = f.readVarInt();
            if (n < 0 || n > MAX_LOCKS) throw new io.netty.handler.codec.DecoderException("too many guidance locks: " + n);
            int[] ids = new int[n];
            for (int i = 0; i < n; i++) ids[i] = f.readVarInt();
            return new Locks(ids);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}

// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.suit.SuitSensorData;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Per-tick sensor picture shared by the ESP, predictor and blast units: who is around, how they
 * move, what they are about to do, and which explosives are counting down.
 */
public final class SensorTracker {
    private SensorTracker() {}

    public static final double ESP_RANGE = 32;
    public static final double PREDICT_RANGE = 24;
    public static final double BLAST_RANGE = 28;
    public static final int PREDICT_TICKS = 20;
    private static final int HISTORY = 6;

    public enum Kind { HOSTILE, NEUTRAL, PASSIVE, PLAYER }

    /** One tracked creature. {@code path} is filled only when the predictor unit is active. */
    public record Track(LivingEntity entity, Kind kind, Vec3 velocity, List<Vec3> path, @Nullable String intent, boolean intentHot) { }

    public record Blast(Entity entity, float seconds, float radius, String label, float fuse) {
        /** Burnt part of the fuse, 0 (just lit) .. 1 (detonation). */
        public float burnt() { return fuse <= 0 ? 1F : Math.max(0F, Math.min(1F, 1F - seconds / fuse)); }
    }

    private static final Int2ObjectOpenHashMap<ArrayDeque<Vec3>> history = new Int2ObjectOpenHashMap<>();
    private static List<Track> tracks = List.of();
    private static List<Blast> blasts = List.of();
    private static ClientLevel boundLevel;

    public static List<Track> tracks() { return tracks; }
    public static List<Blast> blasts() { return blasts; }

    public static void clear() {
        history.clear();
        tracks = List.of();
        blasts = List.of();
    }

    public static Kind kind(Entity e) {
        if (e instanceof Player) return Kind.PLAYER;
        if (e instanceof Enemy) return Kind.HOSTILE;
        if (e instanceof NeutralMob) return Kind.NEUTRAL;
        return Kind.PASSIVE;
    }

    public static int color(Kind kind) {
        return switch (kind) {
            case HOSTILE -> 0xFF4A3D;
            case NEUTRAL -> 0xFFD23F;
            case PASSIVE -> 0x7CFFB2;
            case PLAYER -> 0x6FE6FF;
        };
    }

    public static void tick(ClientLevel level, LocalPlayer player, boolean esp, boolean predict, boolean blast) {
        if (level != boundLevel) { clear(); boundLevel = level; }
        if (!esp && !predict) {
            tracks = List.of();
            history.clear();
        } else {
            double range = esp ? ESP_RANGE : PREDICT_RANGE;
            List<Track> out = new ArrayList<>();
            Int2ObjectOpenHashMap<ArrayDeque<Vec3>> seen = new Int2ObjectOpenHashMap<>();
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                    e -> e != player && e.isAlive() && !e.isSpectator())) {
                if (e.isInvisible() && !e.isCurrentlyGlowing()) continue;
                ArrayDeque<Vec3> h = history.get(e.getId());
                if (h == null) h = new ArrayDeque<>(HISTORY + 1);
                h.addLast(e.position());
                while (h.size() > HISTORY) h.removeFirst();
                seen.put(e.getId(), h);
                Vec3 v = velocity(h);
                boolean near = e.distanceToSqr(player) <= PREDICT_RANGE * PREDICT_RANGE;
                List<Vec3> path = predict && near ? path(level, e, v) : List.of();
                String intent = predict && near ? intent(e, player) : null;
                boolean hot = intent != null && (SuitSensorData.threatLevel(e.getId()) == 2 || intent.equals("DET"));
                out.add(new Track(e, kind(e), v, path, intent, hot));
                if (out.size() >= 64) break;
            }
            history.clear();
            history.putAll(seen);
            Vec3 eye = player.position();
            out.sort((a, b) -> Double.compare(a.entity.distanceToSqr(eye), b.entity.distanceToSqr(eye)));
            tracks = out;
        }
        blasts = blast ? scanBlasts(level, player) : List.of();
    }

    private static Vec3 velocity(ArrayDeque<Vec3> h) {
        if (h.size() < 2) return Vec3.ZERO;
        Vec3[] p = h.toArray(new Vec3[0]);
        int n = Math.min(3, p.length - 1);
        Vec3 sum = Vec3.ZERO;
        for (int i = p.length - n; i < p.length; i++) sum = sum.add(p[i].subtract(p[i - 1]));
        return sum.scale(1.0 / n);
    }

    /** Dead reckoning for one second: constant horizontal speed, gravity when airborne, stop at walls. */
    private static List<Vec3> path(ClientLevel level, LivingEntity e, Vec3 v) {
        if (v.horizontalDistanceSqr() < 0.0004 && Math.abs(v.y) < 0.02) return List.of();
        List<Vec3> pts = new ArrayList<>(PREDICT_TICKS / 2 + 1);
        Vec3 p = e.position();
        Vec3 vel = v;
        boolean grounded = e.onGround();
        pts.add(p);
        for (int t = 1; t <= PREDICT_TICKS; t++) {
            if (!grounded && !e.isNoGravity()) vel = vel.subtract(0, 0.08, 0).scale(0.98);
            Vec3 next = p.add(vel);
            BlockPos at = BlockPos.containing(next.x, next.y + 0.3, next.z);
            if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty()) break;
            if (!grounded && vel.y < 0 && !level.getBlockState(BlockPos.containing(next.x, next.y - 0.05, next.z)).isAir()) {
                grounded = true;
                vel = new Vec3(vel.x, 0, vel.z);
            }
            p = next;
            if (t % 2 == 0) pts.add(p);
        }
        return pts;
    }

    /** Short attack-intent code from the synced animation state, or null. */
    @Nullable
    public static String intent(LivingEntity e, Player wearer) {
        if (e instanceof Creeper creeper && creeper.getSwelling(1.0F) > 0.02F) return "DET";
        if (e instanceof Ghast ghast && ghast.isCharging()) return "FIRE";
        if (e instanceof Guardian guardian && guardian.hasActiveAttackTarget()) return "BEAM";
        if (e instanceof SpellcasterIllager caster && caster.isCastingSpell()) return "CAST";
        if (e instanceof Vex vex && vex.isCharging()) return "DIVE";
        if (e instanceof Pillager pillager && pillager.isChargingCrossbow()) return "AIM";
        if (e instanceof EnderMan enderman && enderman.isCreepy()) return "AGGRO";
        if (e.isUsingItem()) return "AIM";
        if (e instanceof Mob mob && mob.isAggressive()) {
            double d = mob.distanceToSqr(wearer);
            return d < 9 ? "ATK" : "ENGAGE";
        }
        return null;
    }

    private static List<Blast> scanBlasts(ClientLevel level, LocalPlayer player) {
        List<Blast> out = new ArrayList<>();
        for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(BLAST_RANGE))) {
            if (e instanceof PrimedTnt tnt) out.add(new Blast(e, tnt.getFuse() / 20F, 4F, "TNT", 4F));
            else if (e instanceof MinecartTNT cart && cart.isPrimed()) out.add(new Blast(e, cart.getFuse() / 20F, 4F, "TNT-CART", 4F));
            else if (e instanceof Creeper creeper && creeper.getSwelling(1.0F) > 0.02F) {
                float left = (1F - creeper.getSwelling(1.0F)) * 30F / 20F;
                out.add(new Blast(e, left, creeper.isPowered() ? 6F : 3F, creeper.isPowered() ? "CREEPER+" : "CREEPER", 1.5F));
            }
        }
        out.sort((a, b) -> Float.compare(a.seconds, b.seconds));
        return out;
    }
}

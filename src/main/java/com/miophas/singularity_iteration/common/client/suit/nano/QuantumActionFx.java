// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit.nano;

import com.miophas.singularity_iteration.common.client.render.FxDraw;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * World effects of the quantum actions (0.1.10.0), drawn inside the nano effect passes of
 * {@link NanoFx} (scene copy refraction, hologram grid, additive glow) with one visual language,
 * "quantum interference": violet and cyan light that beats between the two, thin lattice lines,
 * lensing rims.
 *
 * <ul>
 *   <li><b>Quantum blink</b>: the body collapses into a violet point at the start (hologram
 *       afterimages along the path), a lensing streak joins the two points, the arrival implodes and
 *       then throws a shock ring over the ground; a lens star flares.</li>
 *   <li><b>Phase shift</b>: the body dissolves into a hologram that beats between magenta and cyan
 *       and refracts the scene; scan rings run up the body and leave magenta echoes. Mk.II ends with
 *       a violet shock wave and arcs to every creature it hits.</li>
 *   <li><b>Force field</b>: a lattice dome grows from the wearer; its rim bends the scene (Fresnel
 *       lensing) and brightens where you look along it; each stopped projectile sends ripples over
 *       the dome and hangs in a stasis ring; the dome flickers out when it falls.</li>
 *   <li><b>Time dilation</b>: a lensing bubble moves with the wearer, a clock dial turns on the
 *       ground with two slow hands, the projectiles inside leave long violet trails, the slowed
 *       creatures carry a time-lock ring, and slow motes drift inside.</li>
 *   <li><b>Entangled volley</b>: double-helix threads run from the wearer to every Wingman, the
 *       muzzles charge, each Wingman launches three bolts on curved paths that all strike at the
 *       same tick, under a shrinking countdown ring on the target.</li>
 *   <li><b>Decoherence</b>: red static crawls over the body.</li>
 * </ul>
 * Client option {@code quantum_fx_intensity}; {@code reduced_flashes} dims the flashes.
 */
final class QuantumActionFx {
    private QuantumActionFx() {}

    static final float[] VIOLET = NanoFx.Q_VIOLET, MAGENTA = NanoFx.Q_MAGENTA, WHITE = NanoFx.Q_WHITE, CYAN = NanoFx.CYAN;
    static final float[] FIELD_BLUE = {0.36F, 0.72F, 1.0F}, TIME = {0.78F, 0.49F, 1.0F}, GOLD = {1.0F, 0.82F, 0.48F}, RED = {1.0F, 0.25F, 0.2F};
    private static final Random RNG = new Random();

    // ------------------------------------------------------------------ state

    private record Blink(int entity, Vec3 from, Vec3 to, int born, int tier) { }
    private static final class Phase { int on, off = -1, max, tier; }
    private record Shock(Vec3 c, float radius, int born, int[] targets, boolean heavy) { }
    private static final class Field { int on, off = -1, refresh, tier; float radius; }
    private record Ripple(int entity, Vec3 normal, int born) { }
    private record Held(Vec3 at, int born, int owner) { }
    private static final class Dilation { int on, off = -1, refresh, left, max; float radius; }
    private static final class Bolt {
        final int wingman, target, fireAt, impactAt, bolts, seed;
        boolean fired, hit;
        Bolt(int wingman, int target, int fireAt, int impactAt, int bolts, int seed) {
            this.wingman = wingman; this.target = target; this.fireAt = fireAt; this.impactAt = impactAt; this.bolts = bolts; this.seed = seed;
        }
    }
    private static final class Volley { int owner, born, end; final List<Bolt> bolts = new ArrayList<>(); }
    private record Impact(Vec3 at, int born) { }
    private record Static(int entity, int born) { }
    // 0.1.10.1 synergies and combos
    private record Combo(int entity, int kind, int born) { }
    private record Burst(Vec3 c, float radius, float damage, int born, int[] targets) { }
    private static final class Decoy { Vec3 pos; int born, until; int[] drawn; boolean ended; int endAt; }
    private static final class Reflected { final int id; final int born; final ArrayDeque<Vec3> path = new ArrayDeque<>(); Reflected(int id, int born) { this.id = id; this.born = born; } }

    private static final List<Blink> BLINKS = new ArrayList<>();
    private static final Int2ObjectOpenHashMap<Phase> PHASES = new Int2ObjectOpenHashMap<>();
    private static final List<Shock> SHOCKS = new ArrayList<>();
    private static final Int2ObjectOpenHashMap<Field> FIELDS = new Int2ObjectOpenHashMap<>();
    private static final List<Ripple> RIPPLES = new ArrayList<>();
    private static final List<Held> HELD = new ArrayList<>();
    private static final Int2ObjectOpenHashMap<Dilation> DILATIONS = new Int2ObjectOpenHashMap<>();
    /** Recent positions of projectiles inside a dilation field (violet trails). */
    private static final Int2ObjectOpenHashMap<ArrayDeque<Vec3>> TRAILS = new Int2ObjectOpenHashMap<>();
    private static final List<Volley> VOLLEYS = new ArrayList<>();
    private static final List<Impact> IMPACTS = new ArrayList<>();
    private static final List<Static> STATICS = new ArrayList<>();
    private static final List<Combo> COMBOS = new ArrayList<>();
    private static final List<Burst> BURSTS = new ArrayList<>();
    private static final Int2ObjectOpenHashMap<Decoy> DECOYS = new Int2ObjectOpenHashMap<>();
    private static final List<Reflected> REFLECTED = new ArrayList<>();
    private static final List<Vec3> PENDING_REFLECT = new ArrayList<>();
    private static int now;

    static void clear() {
        BLINKS.clear();
        PHASES.clear();
        SHOCKS.clear();
        FIELDS.clear();
        RIPPLES.clear();
        HELD.clear();
        DILATIONS.clear();
        TRAILS.clear();
        VOLLEYS.clear();
        IMPACTS.clear();
        STATICS.clear();
        COMBOS.clear();
        BURSTS.clear();
        DECOYS.clear();
        REFLECTED.clear();
        PENDING_REFLECT.clear();
    }

    static float fx() {
        return FcsClientConfig.quantumFx();
    }

    private static float flash() {
        return FcsClientConfig.quantumFx() * FcsClientConfig.flashScale();
    }

    static boolean hazeActive() {
        return !BLINKS.isEmpty() || !FIELDS.isEmpty() || !DILATIONS.isEmpty() || !SHOCKS.isEmpty();
    }

    static boolean glowActive() {
        return !BLINKS.isEmpty() || !PHASES.isEmpty() || !SHOCKS.isEmpty() || !FIELDS.isEmpty() || !HELD.isEmpty() || !DILATIONS.isEmpty()
            || !VOLLEYS.isEmpty() || !IMPACTS.isEmpty() || !STATICS.isEmpty()
            || !COMBOS.isEmpty() || !BURSTS.isEmpty() || !DECOYS.isEmpty() || !REFLECTED.isEmpty() || synergyGlow();
    }

    /** 0.1.10.1: something of the link threads, the datalink marks or the guided paths is to draw. */
    private static boolean synergyGlow() {
        return !QuantumClient.DATALINK.isEmpty() || QuantumClient.datalinkMarked >= 0 || !QuantumClient.activeLinks.isEmpty() || !THREADS.isEmpty();
    }

    // ------------------------------------------------------------------ packet entry points

    static void blink(NanoPackets.Fx fx, boolean self) {
        Vec3 from = new Vec3(fx.x(), fx.y(), fx.z()), to = new Vec3(fx.a(), fx.b(), fx.c());
        int tier = fx.targets().length > 0 ? fx.targets()[0] : 1;
        BLINKS.add(new Blink(fx.entityId(), from, to, now, tier));
        // afterimages: the start and three points along the path
        for (float k : new float[]{0F, 0.3F, 0.55F, 0.8F}) NanoFx.ghost(fx.entityId(), from.lerp(to, k), NanoFx.QUANTUM);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (int i = 0; i < 26 * fx(); i++) {
            mc.level.addParticle(ParticleTypes.REVERSE_PORTAL, from.x + RNG.nextGaussian() * 0.35, from.y + 0.2 + RNG.nextDouble() * 1.7,
                from.z + RNG.nextGaussian() * 0.35, 0, 0, 0);
        }
        for (int i = 0; i < 22 * fx(); i++) {
            double a = RNG.nextDouble() * Math.PI * 2;
            mc.level.addParticle(ParticleTypes.END_ROD, to.x, to.y + 0.9, to.z, Math.cos(a) * 0.22, (RNG.nextDouble() - 0.4) * 0.12, Math.sin(a) * 0.22);
        }
    }

    static void phaseOn(int entity, int ticks, int tier) {
        Phase p = new Phase();
        p.on = now;
        p.max = Math.max(1, ticks);
        p.tier = tier;
        PHASES.put(entity, p);
    }

    static void phaseOff(NanoPackets.Fx fx) {
        Phase p = PHASES.get(fx.entityId());
        if (p != null && p.off < 0) p.off = now;
        // c > 0: the Phase Counter synergy put stored damage into the wave (0.1.10.1): a heavier, redder wave
        if (fx.b() > 0) SHOCKS.add(new Shock(new Vec3(fx.x(), fx.y() + 0.9, fx.z()), fx.b(), now, fx.targets(), fx.c() > 0));
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (int i = 0; i < (fx.b() > 0 ? 36 : 12) * fx(); i++) {
            double a = RNG.nextDouble() * Math.PI * 2;
            double s = fx.b() > 0 ? 0.6 : 0.2;
            mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, fx.x(), fx.y() + 0.9, fx.z(), Math.cos(a) * s, (RNG.nextDouble() - 0.5) * 0.2, Math.sin(a) * s);
        }
    }

    static void fieldOn(int entity, float radius, int tier, boolean fresh) {
        Field f = FIELDS.get(entity);
        if (f == null || f.off >= 0) {
            f = new Field();
            f.on = fresh ? now : now - 20;
            FIELDS.put(entity, f);
        }
        f.radius = Math.max(1, radius);
        f.tier = tier;
        f.refresh = now;
    }

    static void fieldOff(int entity, int reason) {
        Field f = FIELDS.get(entity);
        if (f != null && f.off < 0) f.off = now;
        HELD.removeIf(h -> h.owner == entity);
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null ? null : mc.level.getEntity(entity);
        if (e == null || f == null) return;
        Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0);
        for (int i = 0; i < 30 * fx(); i++) {
            Vec3 d = new Vec3(RNG.nextGaussian(), RNG.nextGaussian() * 0.7, RNG.nextGaussian()).normalize();
            Vec3 p = c.add(d.scale(f.radius));
            mc.level.addParticle(reason == 2 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD, p.x, p.y, p.z, d.x * 0.05, d.y * 0.05, d.z * 0.05);
        }
    }

    static void fieldCatch(NanoPackets.Fx fx) {
        Vec3 c = new Vec3(fx.x(), fx.y(), fx.z());
        float[] p = fx.points();
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i + 2 < p.length; i += 3) {
            Vec3 at = new Vec3(p[i], p[i + 1], p[i + 2]);
            Vec3 n = at.subtract(c);
            RIPPLES.add(new Ripple(fx.entityId(), n.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : n.normalize(), now));
            HELD.add(new Held(at, now, fx.entityId()));
            if (mc.level == null) continue;
            for (int k = 0; k < 8; k++) mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z,
                RNG.nextGaussian() * 0.12, RNG.nextGaussian() * 0.12, RNG.nextGaussian() * 0.12);
        }
        if (HELD.size() > 96) HELD.subList(0, HELD.size() - 96).clear();
    }

    static void dilationOn(int entity, float radius, int left, int max, boolean fresh) {
        Dilation d = DILATIONS.get(entity);
        if (d == null || d.off >= 0) {
            d = new Dilation();
            d.on = fresh ? now : now - 12;
            DILATIONS.put(entity, d);
        }
        d.radius = Math.max(1, radius);
        d.left = left;
        d.max = Math.max(1, max);
        d.refresh = now;
    }

    static void dilationOff(int entity) {
        Dilation d = DILATIONS.get(entity);
        if (d != null && d.off < 0) d.off = now;
    }

    static void volley(NanoPackets.Fx fx, boolean self) {
        Volley v = new Volley();
        v.owner = fx.entityId();
        v.born = now;
        int[] ids = fx.targets();
        float[] t = fx.points();
        int end = now;
        for (int i = 0, j = 0; i + 1 < ids.length && j + 2 < t.length; i += 2, j += 3) {
            Bolt b = new Bolt(ids[i], ids[i + 1], now + Math.round(t[j]), now + Math.round(t[j + 1]), Math.max(1, Math.round(t[j + 2])), ids[i] * 31 + i);
            v.bolts.add(b);
            end = Math.max(end, b.impactAt);
        }
        v.end = end + 16;
        VOLLEYS.add(v);
    }

    static void decohere(int entity) {
        STATICS.add(new Static(entity, now));
    }

    // ---- 0.1.10.1 synergies and combos

    static void combo(int entity, int kind) {
        COMBOS.add(new Combo(entity, kind, now));
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null ? null : mc.level.getEntity(entity);
        if (e == null) return;
        for (int i = 0; i < 24 * fx(); i++) {
            double a = RNG.nextDouble() * Math.PI * 2;
            mc.level.addParticle(kind == 1 ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 1.0, e.getZ(),
                Math.cos(a) * 0.3, (RNG.nextDouble() - 0.3) * 0.2, Math.sin(a) * 0.3);
        }
    }

    static void resonance(NanoPackets.Fx fx) {
        BURSTS.add(new Burst(new Vec3(fx.x(), fx.y(), fx.z()), fx.a(), fx.b(), now, fx.targets()));
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (int i = 0; i < 40 * fx(); i++) {
            double a = RNG.nextDouble() * Math.PI * 2;
            mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, fx.x(), fx.y() + 0.2, fx.z(), Math.cos(a) * 0.7, 0.05, Math.sin(a) * 0.7);
        }
    }

    static void decoy(NanoPackets.Fx fx) {
        Decoy d = new Decoy();
        d.pos = new Vec3(fx.x(), fx.y(), fx.z());
        d.born = now;
        d.until = now + Math.max(1, (int) fx.a());
        d.drawn = fx.targets();
        DECOYS.put(fx.entityId(), d);
    }

    static void decoyEnd(int entity) {
        Decoy d = DECOYS.get(entity);
        if (d == null || d.ended) return;
        d.ended = true;
        d.endAt = now;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (int i = 0; i < 20 * fx(); i++) mc.level.addParticle(ParticleTypes.REVERSE_PORTAL, d.pos.x + RNG.nextGaussian() * 0.3,
            d.pos.y + 0.2 + RNG.nextDouble() * 1.7, d.pos.z + RNG.nextGaussian() * 0.3, 0, 0, 0);
    }

    static void barrage(NanoPackets.Fx fx) {
        float[] p = fx.points();
        for (int i = 0; i + 2 < p.length; i += 3) PENDING_REFLECT.add(new Vec3(p[i], p[i + 1], p[i + 2]));
    }

    // ------------------------------------------------------------------ tick

    static void tick(Minecraft mc, int ticks) {
        now = ticks;
        if (mc.level == null) return;
        BLINKS.removeIf(b -> now - b.born > 16);
        SHOCKS.removeIf(s -> now - s.born > 18);
        RIPPLES.removeIf(r -> now - r.born > 14);
        IMPACTS.removeIf(i -> now - i.born > 14);
        STATICS.removeIf(s -> now - s.born > 18);
        HELD.removeIf(h -> now - h.born > 600 || !FIELDS.containsKey(h.owner));
        PHASES.int2ObjectEntrySet().removeIf(e -> {
            Phase p = e.getValue();
            return p.off >= 0 ? now - p.off > 8 : now - p.on > p.max + 40;
        });
        FIELDS.int2ObjectEntrySet().removeIf(e -> {
            Field f = e.getValue();
            return f.off >= 0 ? now - f.off > 12 : now - f.refresh > 45;
        });
        DILATIONS.int2ObjectEntrySet().removeIf(e -> {
            Dilation d = e.getValue();
            return d.off >= 0 ? now - d.off > 14 : now - d.refresh > 45;
        });
        // phase echoes: a magenta afterimage every third tick
        for (var e : PHASES.int2ObjectEntrySet()) {
            Phase p = e.getValue();
            Entity ent = mc.level.getEntity(e.getIntKey());
            if (ent == null || p.off >= 0) continue;
            if ((now - p.on) % 3 == 1) NanoFx.ghost(ent.getId(), ent.position(), NanoFx.PHASE_ECHO);
            if (RNG.nextFloat() < 0.6F * fx()) mc.level.addParticle(ParticleTypes.REVERSE_PORTAL, ent.getRandomX(0.6), ent.getRandomY(), ent.getRandomZ(0.6), 0, 0.02, 0);
        }
        // time dilation: slow motes inside, trails of the projectiles inside
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (var e : DILATIONS.int2ObjectEntrySet()) {
            Dilation d = e.getValue();
            Entity ent = mc.level.getEntity(e.getIntKey());
            if (ent == null || d.off >= 0) continue;
            Vec3 c = ent.position().add(0, ent.getBbHeight() * 0.5, 0);
            for (int i = 0; i < 3 * fx(); i++) {
                Vec3 p = c.add(new Vec3(RNG.nextGaussian(), RNG.nextGaussian() * 0.5, RNG.nextGaussian()).normalize().scale(RNG.nextDouble() * d.radius));
                mc.level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, 0, 0.004, 0);
            }
            for (Projectile pr : mc.level.getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(d.radius), x -> x.distanceToSqr(c) <= d.radius * d.radius)) {
                ArrayDeque<Vec3> trail = TRAILS.computeIfAbsent(pr.getId(), k -> new ArrayDeque<>());
                trail.addLast(pr.position());
                if (trail.size() > 24) trail.removeFirst();
                seen.add(pr.getId());
            }
        }
        TRAILS.int2ObjectEntrySet().removeIf(e -> !seen.contains(e.getIntKey()));
        // volley: muzzle sparks at each shot, a burst at the impact
        for (Iterator<Volley> it = VOLLEYS.iterator(); it.hasNext(); ) {
            Volley v = it.next();
            if (now > v.end) { it.remove(); continue; }
            for (Bolt b : v.bolts) {
                if (!b.fired && now >= b.fireAt) {
                    b.fired = true;
                    Entity w = mc.level.getEntity(b.wingman);
                    if (w != null) for (int k = 0; k < 10 * fx(); k++) mc.level.addParticle(ParticleTypes.END_ROD, w.getX(), w.getEyeY(), w.getZ(),
                        RNG.nextGaussian() * 0.08, RNG.nextGaussian() * 0.08, RNG.nextGaussian() * 0.08);
                }
                if (!b.hit && now >= b.impactAt) {
                    b.hit = true;
                    Entity t = mc.level.getEntity(b.target);
                    if (t == null) continue;
                    Vec3 at = t.position().add(0, t.getBbHeight() * 0.55, 0);
                    IMPACTS.add(new Impact(at, now));
                    mc.level.addParticle(ParticleTypes.FLASH, at.x, at.y, at.z, 0, 0, 0);
                    for (int k = 0; k < 18 * fx(); k++) mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z,
                        RNG.nextGaussian() * 0.35, RNG.nextGaussian() * 0.35, RNG.nextGaussian() * 0.35);
                }
            }
        }
        // decoherence: red static on the body
        for (Static s : STATICS) {
            Entity e = mc.level.getEntity(s.entity);
            if (e != null && RNG.nextInt(2) == 0) mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, e.getRandomX(0.7), e.getRandomY(), e.getRandomZ(0.7), 0, 0.03, 0);
        }
        tickSynergies(mc);
    }

    // ------------------------------------------------------------------ 0.1.10.1 synergy timeline

    /** Link threads on a body: the heights (share of the body height) of the two linked pieces and the synergy colour. */
    private record LinkThread(float h0, float h1, int color, int index) { }
    private static final Int2ObjectOpenHashMap<List<LinkThread>> THREADS = new Int2ObjectOpenHashMap<>();

    private static void tickSynergies(Minecraft mc) {
        COMBOS.removeIf(c -> now - c.born > 18);
        BURSTS.removeIf(b -> now - b.born > 20);
        DECOYS.int2ObjectEntrySet().removeIf(e -> {
            Decoy d = e.getValue();
            return d.ended ? now - d.endAt > 10 : now > d.until + 20;
        });
        // the decoy hologram: a flickering afterimage of its owner at the decoy point
        for (var e : DECOYS.int2ObjectEntrySet()) {
            Decoy d = e.getValue();
            if (!d.ended && (now - d.born) % 3 == 0 && RNG.nextFloat() < 0.85F) NanoFx.ghost(e.getIntKey(), d.pos, NanoFx.QUANTUM);
        }
        // reflected shots: find the projectiles at the reported points, then follow them for a second
        if (!PENDING_REFLECT.isEmpty()) {
            for (Vec3 at : PENDING_REFLECT) {
                for (Projectile p : mc.level.getEntitiesOfClass(Projectile.class, new AABB(at, at).inflate(1.2))) {
                    REFLECTED.add(new Reflected(p.getId(), now));
                    break;
                }
            }
            PENDING_REFLECT.clear();
        }
        for (Iterator<Reflected> it = REFLECTED.iterator(); it.hasNext(); ) {
            Reflected r = it.next();
            Entity e = mc.level.getEntity(r.id);
            if (e == null || now - r.born > 30) { if (now - r.born > 40 || e == null && r.path.isEmpty()) it.remove(); continue; }
            r.path.addLast(e.position());
            if (r.path.size() > 16) r.path.removeFirst();
        }
        // link threads of the players near (every 5 ticks)
        if (now % 5 == 0 && mc.player != null) {
            THREADS.clear();
            int n = 0;
            for (Player p : mc.level.players()) {
                if (p.distanceToSqr(mc.player) > 48 * 48 || p.isInvisible()) continue;
                List<String> keys = p == mc.player ? QuantumClient.activeLinks
                    : com.miophas.singularity_iteration.common.suit.quantum.QuantumLinks.activeKeys(p);
                if (keys.isEmpty()) continue;
                List<LinkThread> list = new ArrayList<>();
                int i = 0;
                for (String key : keys) {
                    String[] u = com.miophas.singularity_iteration.common.suit.quantum.QuantumLinks.units(key);
                    if (u == null) continue;
                    var s = com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergy.byId(key);
                    list.add(new LinkThread(height(p, u[0]), height(p, u[1]), s == null ? 0x9EF0FF : s.color, i++));
                }
                THREADS.put(p.getId(), list);
                if (++n >= 8) break;
            }
        }
    }

    /** Height share of the worn piece that carries a unit: helmet 0.9, chestplate 0.62, leggings 0.38, boots 0.1. */
    private static float height(Player p, String key) {
        var unit = com.miophas.singularity_iteration.common.suit.UnitRef.byKey(key);
        if (unit == null) return 0.62F;
        var piece = com.miophas.singularity_iteration.common.suit.SuitModules.wornPieceWith(p, unit);
        var slot = com.miophas.singularity_iteration.common.suit.SuitModules.slotOf(piece);
        if (slot == null) return 0.62F;
        return switch (slot) {
            case HEAD -> 0.9F;
            case LEGS -> 0.38F;
            case FEET -> 0.1F;
            default -> 0.62F;
        };
    }

    // ------------------------------------------------------------------ player rendering (phase shift)

    /** Phase-shift look of a player: [0] body visibility, [1] grid strength, [2] refraction; null when not phased. */
    static float[] phaseLook(Player player, float partial) {
        Phase p = PHASES.get(player.getId());
        if (p == null) return null;
        float t = now + partial;
        float body, grid, refract;
        if (p.off < 0) {
            float k = Mth.clamp((t - p.on) / 4F, 0, 1);
            body = 1 - 0.7F * NanoFx.smooth(0, 1, k);
            grid = 0.5F + 0.3F * (float) Math.sin(t * 1.3F) + (1 - k) * 0.7F;
            refract = 0.75F * k;
        } else {
            float k = Mth.clamp((t - p.off) / 6F, 0, 1);
            body = NanoFx.smooth(0, 1, k);
            grid = (1 - k) * 0.9F;
            refract = (1 - k) * 0.6F;
        }
        return new float[]{body, grid * fx(), refract};
    }

    // ------------------------------------------------------------------ haze pass (scene refraction)

    static void haze(VertexConsumer vc, Matrix4f m, Vec3 cam, float partial, Minecraft mc) {
        float f = fx();
        for (Blink b : BLINKS) {
            float age = now - b.born + partial;
            float s = Mth.clamp(1 - age / 12F, 0, 1) * f;
            if (s <= 0.01F) continue;
            NanoFx.ribbon(vc, m, b.from.add(0, 0.9, 0), b.to.add(0, 0.9, 0), cam, 1.1F, 0.6F, 0.6F, 1.0F, s * 0.7F, s, true);
            float r = age < 3 ? 1.6F * (1 - age / 3F) + 0.2F : 0.3F + (age - 3) * 0.28F;
            NanoFx.ringFlat(vc, m, b.to.add(0, 0.05, 0), r, 0.6F, 0.6F, 0.6F, 1.0F, s, 36, true);
        }
        for (Shock s : SHOCKS) {
            float age = now - s.born + partial;
            float k = Mth.clamp(age / 8F, 0, 1);
            float a = (age < 8 ? 1 : Mth.clamp(1 - (age - 8) / 8F, 0, 1)) * f;
            if (a > 0.01F) NanoFx.ringBand(vc, m, s.c, Math.max(0.2F, s.radius * NanoFx.easeOut(k)), 1.0F, 0.6F, 0.6F, 1.0F, a, 48, true);
        }
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        for (var e : FIELDS.int2ObjectEntrySet()) {
            Entity ent = mc.level.getEntity(e.getIntKey());
            if (ent == null || firstPerson && ent == mc.player) continue;           // the rim cannot be seen from inside
            Field fl = e.getValue();
            float grow = NanoFx.easeOut(Mth.clamp((now - fl.on + partial) / 10F, 0, 1));
            float fade = fl.off >= 0 ? Mth.clamp(1 - (now - fl.off + partial) / 10F, 0, 1) : 1;
            shellHaze(vc, m, center(ent, partial), fl.radius * grow, cam, 0.75F * fade * f);
        }
        for (var e : DILATIONS.int2ObjectEntrySet()) {
            Entity ent = mc.level.getEntity(e.getIntKey());
            if (ent == null) continue;
            Dilation d = e.getValue();
            float in = Mth.clamp((now - d.on + partial) / 12F, 0, 1), out = d.off >= 0 ? Mth.clamp(1 - (now - d.off + partial) / 14F, 0, 1) : 1;
            float r = d.radius * NanoFx.easeOut(in) * (d.off >= 0 ? 1 + 0.15F * (1 - out) : 1);
            if (firstPerson && ent == mc.player) {
                // from inside: a few slow lensing bands on the ground instead of the rim
                for (int k = 0; k < 2; k++) {
                    float u = ((now + partial) * 0.006F + k * 0.5F) % 1F;
                    NanoFx.ringFlat(vc, m, ent.getPosition(partial).add(0, 0.06, 0), r * u, 0.8F, 0.6F, 0.5F, 1.0F, 0.45F * (1 - u) * out * f, 48, true);
                }
                continue;
            }
            shellHaze(vc, m, center(ent, partial), r, cam, 0.95F * out * f);
        }
    }

    /** A sphere of refraction whose strength rises at the rim (where the view runs along the surface). */
    private static void shellHaze(VertexConsumer vc, Matrix4f m, Vec3 c, float r, Vec3 cam, float strength) {
        if (r < 0.2F || strength <= 0.01F) return;
        int lat = 12, lon = 28;
        for (int i = 0; i < lat; i++) {
            double t0 = -Math.PI / 2 + i * Math.PI / lat, t1 = t0 + Math.PI / lat;
            for (int j = 0; j < lon; j++) {
                double p0 = j * Math.PI * 2 / lon, p1 = p0 + Math.PI * 2 / lon;
                Vec3 a = sphere(c, r, t0, p0), b = sphere(c, r, t0, p1), d = sphere(c, r, t1, p1), e = sphere(c, r, t1, p0);
                NanoFx.quad(vc, m, a, b, d, e, 0.6F, 0.6F, 1.0F, rim(a, c, cam) * strength, rim(b, c, cam) * strength,
                    rim(d, c, cam) * strength, rim(e, c, cam) * strength);
            }
        }
    }

    private static Vec3 sphere(Vec3 c, float r, double lat, double lon) {
        return c.add(Math.cos(lat) * Math.cos(lon) * r, Math.sin(lat) * r, Math.cos(lat) * Math.sin(lon) * r);
    }

    /** Fresnel-like weight: 1 at the silhouette, 0 face-on. */
    private static float rim(Vec3 p, Vec3 c, Vec3 cam) {
        Vec3 n = p.subtract(c).normalize(), v = p.subtract(cam).normalize();
        float x = 1 - (float) Math.abs(n.dot(v));
        return x * x * x;
    }

    private static Vec3 center(Entity e, float partial) {
        return e.getPosition(partial).add(0, e.getBbHeight() * 0.5, 0);
    }

    // ------------------------------------------------------------------ glow pass (additive light)

    static void glow(VertexConsumer vc, Matrix4f m, Vec3 cam, float partial, Minecraft mc, RenderLevelStageEvent event) {
        float t = now + partial;
        for (Blink b : BLINKS) blinkGlow(vc, m, b, cam, t, event);
        for (var e : PHASES.int2ObjectEntrySet()) {
            Entity ent = mc.level.getEntity(e.getIntKey());
            if (ent != null) phaseGlow(vc, m, ent, e.getValue(), cam, t, partial, mc);
        }
        for (Shock s : SHOCKS) shockGlow(vc, m, s, cam, t, partial, mc);
        for (var e : FIELDS.int2ObjectEntrySet()) {
            Entity ent = mc.level.getEntity(e.getIntKey());
            if (ent != null) fieldGlow(vc, m, ent, e.getValue(), cam, t, partial, mc);
        }
        for (Held h : HELD) heldGlow(vc, m, h, cam, t, event);
        for (var e : DILATIONS.int2ObjectEntrySet()) {
            Entity ent = mc.level.getEntity(e.getIntKey());
            if (ent != null) dilationGlow(vc, m, ent, e.getValue(), cam, t, partial, mc);
        }
        for (var e : TRAILS.int2ObjectEntrySet()) trailGlow(vc, m, e.getValue(), cam);
        for (Volley v : VOLLEYS) volleyGlow(vc, m, v, cam, t, partial, mc);
        for (Impact i : IMPACTS) impactGlow(vc, m, i, cam, t, event);
        for (Static s : STATICS) staticGlow(vc, m, s, cam, t, partial, mc);
        // 0.1.10.1 synergies and combos
        for (Combo c : COMBOS) comboGlow(vc, m, c, cam, t, partial, mc, event);
        for (Burst b : BURSTS) burstGlow(vc, m, b, cam, t, partial, mc);
        for (var e : DECOYS.int2ObjectEntrySet()) decoyGlow(vc, m, e.getValue(), cam, t, partial, mc);
        for (Reflected r : REFLECTED) reflectedGlow(vc, m, r, cam, t);
        threadsGlow(vc, m, cam, t, partial, mc);
        datalinkGlow(vc, m, cam, t, partial, mc);
        guidedPathsGlow(vc, m, cam, t, partial, mc);
    }

    private static void blinkGlow(VertexConsumer vc, Matrix4f m, Blink b, Vec3 cam, float t, RenderLevelStageEvent event) {
        float age = t - b.born;
        float f = fx(), fl = flash();
        float s = Mth.clamp(1 - age / 10F, 0, 1);
        Vec3 a0 = b.from.add(0, 0.9, 0), a1 = b.to.add(0, 0.9, 0);
        float[] core = b.tier >= 2 ? MAGENTA : VIOLET;
        // the streak: a bright core and cyan side lines at the feet and the head
        if (s > 0.01F) {
            FxDraw.beam(vc, m, a0, a1, cam, 0.22F * s, core[0], core[1], core[2], 0.5F * s * f, 0.95F * s * f);
            for (float h : new float[]{0.25F, 1.6F}) {
                NanoFx.ribbon(vc, m, b.from.add(0, h, 0), b.to.add(0, h, 0), cam, 0.05F, CYAN[0], CYAN[1], CYAN[2], 0.15F * s * f, 0.55F * s * f, false);
            }
        }
        // the start collapses into a point
        if (age < 7) {
            float k = age / 7F;
            NanoFx.ball(vc, m, a0, 0.9F * (1 - k), core, 0.8F * (1 - k) * f, cam);
            FxDraw.ring(vc, m, a0, new Vec3(0, 1, 0), 0.9F * (1 - k), 0.12F, core[0], core[1], core[2], 0.7F * (1 - k) * f, 32);
        }
        // the arrival: implosion, then the shock ring over the ground and a lens star
        if (age < 3) {
            float k = age / 3F;
            FxDraw.ring(vc, m, a1, new Vec3(0, 1, 0), 1.6F * (1 - k) + 0.15F, 0.2F, WHITE[0], WHITE[1], WHITE[2], 0.9F * f, 40);
        } else if (age < 15) {
            float k = (age - 3) / 12F;
            NanoFx.ringFlat(vc, m, b.to.add(0, 0.05, 0), 0.3F + k * 3.2F, 0.16F, core[0], core[1], core[2], 0.8F * (1 - k) * f, 48, false);
            NanoFx.ringFlat(vc, m, b.to.add(0, 0.07, 0), 0.2F + k * 2.0F, 0.07F, CYAN[0], CYAN[1], CYAN[2], 0.6F * (1 - k) * f, 40, false);
        }
        if (age < 6) {
            float k = 1 - age / 6F;
            FxDraw.star(vc, m, a1, event.getCamera().getLeftVector(), event.getCamera().getUpVector(), 1.3F * k + 0.3F, WHITE[0], WHITE[1], WHITE[2], k * fl);
        }
    }

    private static void phaseGlow(VertexConsumer vc, Matrix4f m, Entity e, Phase p, Vec3 cam, float t, float partial, Minecraft mc) {
        if (e == mc.player && mc.options.getCameraType().isFirstPerson()) return;
        float on = p.off >= 0 ? Mth.clamp(1 - (t - p.off) / 6F, 0, 1) : Mth.clamp((t - p.on) / 3F, 0, 1);
        if (on <= 0.01F) return;
        float f = fx();
        Vec3 base = e.getPosition(partial);
        float h = e.getBbHeight(), r = e.getBbWidth() * 0.5F + 0.25F;
        // scan rings running up the body, alternating magenta and cyan
        for (int i = 0; i < 5; i++) {
            float u = ((t * 0.06F) + i / 5F) % 1F;
            float[] c = i % 2 == 0 ? MAGENTA : CYAN;
            NanoFx.ringFlat(vc, m, base.add(0, u * h, 0), r, 0.05F, c[0], c[1], c[2], 0.65F * on * f * (float) Math.sin(u * Math.PI), 24, false);
        }
        // two interference helices
        for (int strand = 0; strand < 2; strand++) {
            Vec3 prev = null;
            for (int i = 0; i <= 24; i++) {
                float u = i / 24F;
                double ang = u * Math.PI * 4 + t * 0.25 + strand * Math.PI;
                Vec3 pt = base.add(Math.cos(ang) * r * 1.1, u * h, Math.sin(ang) * r * 1.1);
                float[] c = strand == 0 ? MAGENTA : CYAN;
                if (prev != null) NanoFx.ribbon(vc, m, prev, pt, cam, 0.03F, c[0], c[1], c[2], 0.5F * on * f, 0.5F * on * f, false);
                prev = pt;
            }
        }
    }

    private static void shockGlow(VertexConsumer vc, Matrix4f m, Shock s, Vec3 cam, float t, float partial, Minecraft mc) {
        float age = t - s.born;
        float f = fx();
        float k = Mth.clamp(age / 8F, 0, 1);
        float fade = age < 8 ? 1 : Mth.clamp(1 - (age - 8) / 10F, 0, 1);
        float r = Math.max(0.2F, s.radius * NanoFx.easeOut(k));
        if (age < 4) NanoFx.ball(vc, m, s.c, 0.4F + age * 0.3F, WHITE, (1 - age / 4F) * flash(), cam);
        if (s.heavy) NanoFx.ringBand(vc, m, s.c, r * 0.92F, 0.7F, RED[0], 0.35F, 0.6F, 0.8F * fade * f, 56, false);
        NanoFx.ringBand(vc, m, s.c, r, 0.3F, MAGENTA[0], MAGENTA[1], MAGENTA[2], 0.75F * fade * f, 56, false);
        NanoFx.ringFlat(vc, m, s.c.add(0, -0.88, 0), r, 0.16F, VIOLET[0], VIOLET[1], VIOLET[2], 0.6F * fade * f, 56, false);
        for (int i = 0; i < 3; i++) NanoFx.meridian(vc, m, s.c, r, (float) (i * Math.PI / 3 + age * 0.06F), 0.04F, MAGENTA, 0.3F * fade * f, cam);
        if (age < 12) {
            Random rng = new Random(s.born * 17L + (long) (age / 2));
            for (int id : s.targets) {
                Entity e = mc.level.getEntity(id);
                if (e == null) continue;
                NanoFx.arc(vc, m, s.c, e.getPosition(partial).add(0, e.getBbHeight() * 0.6, 0), cam, rng, 0.05F, (1 - age / 12F) * f, MAGENTA);
            }
        }
    }

    private static void fieldGlow(VertexConsumer vc, Matrix4f m, Entity e, Field fl, Vec3 cam, float t, float partial, Minecraft mc) {
        float f = fx();
        float grow = NanoFx.easeOut(Mth.clamp((t - fl.on) / 10F, 0, 1));
        float fade = fl.off >= 0 ? Mth.clamp(1 - (t - fl.off) / 10F, 0, 1) : 1;
        if (fade <= 0.01F) return;
        // flicker as it falls
        if (fl.off >= 0 && NanoFx.hash(e.getId(), (int) (t * 3)) % 3 == 0) fade *= 0.3F;
        boolean inside = e == mc.player && mc.options.getCameraType().isFirstPerson();
        float self = inside ? 0.45F : 1F;
        Vec3 c = center(e, partial);
        float r = fl.radius * grow * (fl.off >= 0 ? 1 + 0.12F * (1 - fade) : 1);
        float[] col = fl.tier >= 2 ? FIELD_BLUE : FIELD_BLUE;
        // lattice: latitude rings and meridians; each segment brightens at the rim and where the hex wave passes
        int lat = 9, lon = 18, steps = 36;
        for (int i = 1; i < lat; i++) {
            double th = -Math.PI / 2 + i * Math.PI / lat;
            Vec3 prev = null;
            for (int j = 0; j <= steps; j++) {
                double ph = j * Math.PI * 2 / steps + (i % 2) * Math.PI / steps;
                Vec3 p = sphere(c, r, th, ph);
                if (prev != null) segment(vc, m, prev, p, c, cam, col, t, i * 7 + j, 0.42F * fade * self * f);
                prev = p;
            }
        }
        for (int j = 0; j < lon; j++) {
            double ph = j * Math.PI * 2 / lon + t * 0.004;
            Vec3 prev = null;
            for (int i = 0; i <= lat * 2; i++) {
                double th = -Math.PI / 2 + i * Math.PI / (lat * 2);
                Vec3 p = sphere(c, r, th, ph);
                if (prev != null) segment(vc, m, prev, p, c, cam, col, t, j * 5 + i, 0.3F * fade * self * f);
                prev = p;
            }
        }
        // deploy wave front and the ground contact ring
        float age = t - fl.on;
        if (age < 12) {
            float k = age / 12F;
            NanoFx.ringFlat(vc, m, e.getPosition(partial).add(0, 0.05, 0), r * k + 0.2F, 0.25F, WHITE[0], WHITE[1], WHITE[2], 0.8F * (1 - k) * f, 56, false);
        }
        NanoFx.ringFlat(vc, m, e.getPosition(partial).add(0, 0.04, 0), (float) Math.sqrt(Math.max(0, r * r - Math.pow(c.y - e.getPosition(partial).y, 2))),
            0.12F, col[0], col[1], col[2], 0.55F * fade * f, 56, false);
        // impact ripples over the dome
        for (Ripple rp : RIPPLES) {
            if (rp.entity != e.getId()) continue;
            float ra = t - rp.born;
            float theta = 0.08F + ra * 0.07F, a = Mth.clamp(1 - ra / 14F, 0, 1) * f;
            domeCircle(vc, m, c, r * 1.01F, rp.normal, theta, cam, WHITE, a);
            domeCircle(vc, m, c, r * 1.01F, rp.normal, theta * 0.6F, cam, col, a * 0.8F);
        }
    }

    /** One lattice segment: brighter at the rim (Fresnel) and under a travelling hex wave. */
    private static void segment(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 cam, float[] col, float t, int index, float alpha) {
        Vec3 mid = a.add(b).scale(0.5);
        float rim = rim(mid, c, cam);
        float wave = 0.5F + 0.5F * (float) Math.sin(index * 0.9F + t * 0.18F);
        float al = alpha * (0.15F + 0.85F * rim) * (0.55F + 0.45F * wave);
        if (al < 0.015F) return;
        NanoFx.ribbon(vc, m, a, b, cam, 0.035F, col[0], col[1], col[2], al, al, false);
    }

    /** A circle on the dome around {@code axis} at angular radius {@code theta}. */
    private static void domeCircle(VertexConsumer vc, Matrix4f m, Vec3 c, float r, Vec3 axis, float theta, Vec3 cam, float[] col, float a) {
        if (a <= 0.01F) return;
        Vec3 t1 = FxDraw.perp(axis), t2 = axis.cross(t1).normalize();
        Vec3 prev = null;
        for (int i = 0; i <= 28; i++) {
            double ph = i * Math.PI * 2 / 28;
            Vec3 dir = axis.scale(Math.cos(theta)).add(t1.scale(Math.sin(theta) * Math.cos(ph))).add(t2.scale(Math.sin(theta) * Math.sin(ph)));
            Vec3 p = c.add(dir.scale(r));
            if (prev != null) NanoFx.ribbon(vc, m, prev, p, cam, 0.06F, col[0], col[1], col[2], a, a, false);
            prev = p;
        }
    }

    private static void heldGlow(VertexConsumer vc, Matrix4f m, Held h, Vec3 cam, float t, RenderLevelStageEvent event) {
        float f = fx();
        float pulse = 0.6F + 0.4F * (float) Math.sin(t * 0.5F + h.born);
        Vec3 view = h.at.subtract(cam);
        if (view.lengthSqr() < 1.0E-4) return;
        FxDraw.ring(vc, m, h.at, view.normalize(), 0.22F + 0.04F * pulse, 0.05F, FIELD_BLUE[0], FIELD_BLUE[1], FIELD_BLUE[2], 0.7F * pulse * f, 20);
        FxDraw.ring(vc, m, h.at, new Vec3(0, 1, 0), 0.3F, 0.03F, WHITE[0], WHITE[1], WHITE[2], 0.35F * f, 16);
        float age = t - h.born;
        if (age < 5) FxDraw.star(vc, m, h.at, event.getCamera().getLeftVector(), event.getCamera().getUpVector(), 0.5F * (1 - age / 5F) + 0.1F,
            0.7F, 0.9F, 1.0F, (1 - age / 5F) * flash());
    }

    private static void dilationGlow(VertexConsumer vc, Matrix4f m, Entity e, Dilation d, Vec3 cam, float t, float partial, Minecraft mc) {
        float f = fx();
        float in = Mth.clamp((t - d.on) / 12F, 0, 1), out = d.off >= 0 ? Mth.clamp(1 - (t - d.off) / 14F, 0, 1) : 1;
        float a = in * out * f;
        if (a <= 0.01F) return;
        float r = d.radius * NanoFx.easeOut(in);
        Vec3 feet = e.getPosition(partial);
        Vec3 c = center(e, partial);
        boolean inside = e == mc.player && mc.options.getCameraType().isFirstPerson();
        // boundary: slow latitude rings and meridians
        if (!inside) {
            for (int i = -2; i <= 2; i++) {
                double lat = i * Math.PI / 6;
                NanoFx.ringFlat(vc, m, c.add(0, Math.sin(lat) * r, 0), (float) (Math.cos(lat) * r), 0.05F, TIME[0], TIME[1], TIME[2], 0.28F * a, 64, false);
            }
            for (int j = 0; j < 6; j++) NanoFx.meridian(vc, m, c, r, (float) (j * Math.PI / 6 + t * 0.002F), 0.035F, TIME, 0.16F * a, cam);
        }
        // the clock dial on the ground: rim, 60 ticks (every fifth long), two slow hands
        Vec3 g = feet.add(0, 0.05, 0);
        NanoFx.ringFlat(vc, m, g, r, 0.1F, TIME[0], TIME[1], TIME[2], 0.6F * a, 72, false);
        NanoFx.ringFlat(vc, m, g, r * 0.62F, 0.05F, GOLD[0], GOLD[1], GOLD[2], 0.35F * a, 64, false);
        float spin = t * 0.004F;
        for (int k = 0; k < 60; k++) {
            double ang = k * Math.PI / 30 + spin;
            float len = k % 5 == 0 ? 0.9F : 0.35F;
            Vec3 p0 = g.add(Math.cos(ang) * (r - len), 0, Math.sin(ang) * (r - len)), p1 = g.add(Math.cos(ang) * r, 0, Math.sin(ang) * r);
            NanoFx.ribbon(vc, m, p0, p1, cam, k % 5 == 0 ? 0.08F : 0.04F, TIME[0], TIME[1], TIME[2], 0.5F * a, 0.7F * a, false);
        }
        double hand1 = t * 0.03, hand2 = t * 0.0025;
        NanoFx.ribbon(vc, m, g, g.add(Math.cos(hand1) * r * 0.85, 0, Math.sin(hand1) * r * 0.85), cam, 0.09F, GOLD[0], GOLD[1], GOLD[2], 0.75F * a, 0.2F * a, false);
        NanoFx.ribbon(vc, m, g, g.add(Math.cos(hand2) * r * 0.55, 0, Math.sin(hand2) * r * 0.55), cam, 0.14F, TIME[0], TIME[1], TIME[2], 0.8F * a, 0.3F * a, false);
        // time-lock rings on the hostile creatures inside
        for (LivingEntity x : mc.level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r), x -> x != e && x instanceof Enemy && x.distanceToSqr(c) <= r * r)) {
            Vec3 xf = x.getPosition(partial).add(0, 0.08, 0);
            float rr = x.getBbWidth() * 0.7F + 0.2F;
            for (int k = 0; k < 6; k++) {
                double a0 = k * Math.PI / 3 + t * 0.01, a1 = a0 + Math.PI / 5;
                NanoFx.ribbon(vc, m, xf.add(Math.cos(a0) * rr, 0, Math.sin(a0) * rr), xf.add(Math.cos(a1) * rr, 0, Math.sin(a1) * rr), cam, 0.05F,
                    TIME[0], TIME[1], TIME[2], 0.65F * a, 0.65F * a, false);
            }
        }
    }

    private static void trailGlow(VertexConsumer vc, Matrix4f m, ArrayDeque<Vec3> trail, Vec3 cam) {
        if (trail.size() < 2) return;
        float f = fx();
        Vec3 prev = null;
        int i = 0, n = trail.size();
        for (Vec3 p : trail) {
            if (prev != null) {
                float a0 = (i - 1F) / n, a1 = (float) i / n;
                NanoFx.ribbon(vc, m, prev, p, cam, 0.09F, TIME[0], TIME[1], TIME[2], 0.6F * a0 * f, 0.6F * a1 * f, false);
            }
            prev = p;
            i++;
        }
    }

    private static void volleyGlow(VertexConsumer vc, Matrix4f m, Volley v, Vec3 cam, float t, float partial, Minecraft mc) {
        float f = fx();
        Entity owner = mc.level.getEntity(v.owner);
        float age = t - v.born;
        java.util.Set<Integer> threaded = new java.util.HashSet<>();
        for (Bolt b : v.bolts) {
            Entity w = mc.level.getEntity(b.wingman), tg = mc.level.getEntity(b.target);
            // entanglement threads from the wearer to each Wingman
            if (owner != null && w != null && age < 30 && threaded.add(b.wingman)) {
                helix(vc, m, center(owner, partial).add(0, 0.3, 0), center(w, partial), cam, t, (1 - age / 30F) * f);
            }
            if (tg == null) continue;
            Vec3 aim = tg.getPosition(partial).add(0, tg.getBbHeight() * 0.55, 0);
            // countdown ring over the target, shrinking to the body at impact
            if (t < b.impactAt) {
                float total = Math.max(1, b.impactAt - v.born), left = Mth.clamp((b.impactAt - t) / total, 0, 1);
                float rr = tg.getBbWidth() * 0.6F + 0.15F + 1.6F * left;
                float[] col = left < 0.25F ? RED : GOLD;
                FxDraw.ring(vc, m, aim, new Vec3(0, 1, 0), rr, 0.06F, col[0], col[1], col[2], 0.8F * f, 40);
                for (int k = 0; k < 4; k++) {
                    double ang = k * Math.PI / 2 + t * 0.12;
                    Vec3 p = aim.add(Math.cos(ang) * rr, 0, Math.sin(ang) * rr);
                    NanoFx.ribbon(vc, m, p.add(0, -0.25, 0), p.add(0, 0.25, 0), cam, 0.07F, col[0], col[1], col[2], 0.9F * f, 0.9F * f, false);
                }
            }
            if (w == null) continue;
            Vec3 muzzle = w.getEyePosition(partial);
            // charge at the muzzle before the shot
            if (t < b.fireAt) {
                float k = Mth.clamp((t - v.born) / Math.max(1, b.fireAt - v.born), 0, 1);
                NanoFx.ball(vc, m, muzzle, 0.08F + 0.3F * k, CYAN, (0.4F + 0.5F * k) * f, cam);
                continue;
            }
            if (t > b.impactAt) continue;
            float u = Mth.clamp((t - b.fireAt) / Math.max(1, b.impactAt - b.fireAt), 0, 1);
            Vec3 side = aim.subtract(muzzle).cross(new Vec3(0, 1, 0));
            side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
            for (int k = 0; k < b.bolts; k++) {
                float spread = k - (b.bolts - 1) / 2F;
                Vec3 p1 = muzzle.add(0, 2.2 + Math.abs(spread) * 0.8, 0).add(side.scale(spread * 2.4));
                Vec3 p2 = aim.add(0, 2.6, 0).add(side.scale(spread * 1.1));
                Vec3 head = bezier(muzzle, p1, p2, aim, u);
                Vec3 prev = head;
                for (int s = 1; s <= 8; s++) {
                    float us = Math.max(0, u - s * 0.035F);
                    Vec3 q = bezier(muzzle, p1, p2, aim, us);
                    float a0 = (1 - (s - 1) / 8F) * f, a1 = (1 - s / 8F) * f;
                    float[] col = k % 2 == 0 ? CYAN : VIOLET;
                    NanoFx.ribbon(vc, m, prev, q, cam, 0.12F, col[0], col[1], col[2], 0.8F * a0, 0.8F * a1, false);
                    prev = q;
                    if (us <= 0) break;
                }
                NanoFx.ball(vc, m, head, 0.14F, WHITE, 0.95F * f, cam);
            }
        }
    }

    private static Vec3 bezier(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float u) {
        float v = 1 - u;
        return p0.scale(v * v * v).add(p1.scale(3 * v * v * u)).add(p2.scale(3 * v * u * u)).add(p3.scale(u * u * u));
    }

    /** Two strands winding around the line from a to b (an entanglement thread). */
    private static void helix(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 cam, float t, float alpha) {
        if (alpha <= 0.01F) return;
        Vec3 axis = b.subtract(a);
        double len = axis.length();
        if (len < 0.5) return;
        Vec3 n1 = FxDraw.perp(axis.normalize()), n2 = axis.normalize().cross(n1).normalize();
        int steps = Math.max(12, (int) (len * 3));
        for (int strand = 0; strand < 2; strand++) {
            float[] col = strand == 0 ? CYAN : VIOLET;
            Vec3 prev = null;
            for (int i = 0; i <= steps; i++) {
                float u = (float) i / steps;
                double ph = u * len * 2.4 - t * 0.45 + strand * Math.PI;
                double amp = 0.16 * Math.sin(u * Math.PI);
                Vec3 p = a.add(axis.scale(u)).add(n1.scale(Math.cos(ph) * amp)).add(n2.scale(Math.sin(ph) * amp));
                if (prev != null) NanoFx.ribbon(vc, m, prev, p, cam, 0.04F, col[0], col[1], col[2], 0.7F * alpha, 0.7F * alpha, false);
                prev = p;
            }
        }
    }

    private static void impactGlow(VertexConsumer vc, Matrix4f m, Impact i, Vec3 cam, float t, RenderLevelStageEvent event) {
        float age = t - i.born;
        float k = Mth.clamp(age / 12F, 0, 1);
        float fl = flash();
        if (age < 4) NanoFx.ball(vc, m, i.at, 0.5F + age * 0.25F, WHITE, (1 - age / 4F) * fl, cam);
        FxDraw.ring(vc, m, i.at, new Vec3(0, 1, 0), 0.3F + k * 2.6F, 0.18F, CYAN[0], CYAN[1], CYAN[2], 0.8F * (1 - k) * fx(), 40);
        if (age < 5) FxDraw.star(vc, m, i.at, event.getCamera().getLeftVector(), event.getCamera().getUpVector(), 1.4F * (1 - age / 5F) + 0.2F,
            0.8F, 0.95F, 1.0F, (1 - age / 5F) * fl);
    }

    private static void staticGlow(VertexConsumer vc, Matrix4f m, Static s, Vec3 cam, float t, float partial, Minecraft mc) {
        Entity e = mc.level.getEntity(s.entity);
        if (e == null || e == mc.player && mc.options.getCameraType().isFirstPerson()) return;
        float age = t - s.born;
        float a = Mth.clamp(1 - age / 18F, 0, 1) * fx();
        Random r = new Random(s.born * 7919L + (long) (age / 2));
        AABB box = e.getBoundingBox().move(e.getPosition(partial).subtract(e.position()));
        for (int k = 0; k < 3; k++) {
            Vec3 p = new Vec3(Mth.lerp(r.nextDouble(), box.minX, box.maxX), Mth.lerp(r.nextDouble(), box.minY, box.maxY), Mth.lerp(r.nextDouble(), box.minZ, box.maxZ));
            Vec3 q = new Vec3(Mth.lerp(r.nextDouble(), box.minX, box.maxX), Mth.lerp(r.nextDouble(), box.minY, box.maxY), Mth.lerp(r.nextDouble(), box.minZ, box.maxZ));
            NanoFx.arc(vc, m, p, q, cam, r, 0.025F, a, RED);
        }
        NanoFx.ringFlat(vc, m, e.getPosition(partial).add(0, 0.05, 0), 0.5F + age * 0.06F, 0.06F, RED[0], RED[1], RED[2], 0.6F * a, 28, false);
    }

    // ------------------------------------------------------------------ 0.1.10.1 synergy and combo geometry

    /**
     * Combos: Quantum Tunnel = a violet vortex of three shrinking rings around the body;
     * Frozen Volley = a gold "time-stop" sphere flash with clock ticks; Static Barrage = a blue
     * burst ring and radial sparks.
     */
    private static void comboGlow(VertexConsumer vc, Matrix4f m, Combo c, Vec3 cam, float t, float partial, Minecraft mc, RenderLevelStageEvent event) {
        Entity e = mc.level.getEntity(c.entity);
        if (e == null) return;
        float age = t - c.born, k = Mth.clamp(age / 18F, 0, 1), a = (1 - k) * fx();
        if (a <= 0.01F) return;
        Vec3 ctr = center(e, partial);
        boolean self = e == mc.player && mc.options.getCameraType().isFirstPerson();
        switch (c.kind) {
            case 0 -> {
                for (int i = 0; i < 3; i++) {
                    float r = (1.6F - i * 0.35F) * (1 - k * 0.7F);
                    Vec3 at = ctr.add(0, (i - 1) * 0.55, 0);
                    FxDraw.ring(vc, m, at, new Vec3(Math.sin(t * 0.3 + i), 1, Math.cos(t * 0.3 + i)), r, 0.12F, VIOLET[0], VIOLET[1], VIOLET[2], 0.8F * a, 36);
                }
            }
            case 1 -> {
                if (!self) NanoFx.ball(vc, m, ctr, 0.6F + 2.4F * k, GOLD, 0.5F * a, cam);
                Vec3 feet = e.getPosition(partial).add(0, 0.06, 0);
                NanoFx.ringFlat(vc, m, feet, 1.2F + 3.5F * k, 0.14F, GOLD[0], GOLD[1], GOLD[2], 0.9F * a, 48, false);
                for (int i = 0; i < 12; i++) {
                    double ang = i * Math.PI / 6;
                    float r0 = 1.0F + 3.5F * k, r1 = r0 + 0.5F;
                    NanoFx.ribbon(vc, m, feet.add(Math.cos(ang) * r0, 0, Math.sin(ang) * r0), feet.add(Math.cos(ang) * r1, 0, Math.sin(ang) * r1), cam, 0.08F,
                        GOLD[0], GOLD[1], GOLD[2], 0.9F * a, 0.9F * a, false);
                }
            }
            default -> {
                FxDraw.ring(vc, m, ctr, new Vec3(0, 1, 0), 0.8F + 4F * k, 0.25F, FIELD_BLUE[0], FIELD_BLUE[1], FIELD_BLUE[2], 0.8F * a, 48);
                if (age < 5) FxDraw.star(vc, m, ctr, event.getCamera().getLeftVector(), event.getCamera().getUpVector(), 1.6F * (1 - age / 5F) + 0.3F,
                    0.7F, 0.9F, 1.0F, (1 - age / 5F) * flash());
            }
        }
    }

    /** Resonant Deflection burst: standing-wave rings that run out over the ground, a violet column, arcs to every creature hit. */
    private static void burstGlow(VertexConsumer vc, Matrix4f m, Burst b, Vec3 cam, float t, float partial, Minecraft mc) {
        float age = t - b.born, k = Mth.clamp(age / 10F, 0, 1);
        float a = (age < 10 ? 1 : Mth.clamp(1 - (age - 10) / 10F, 0, 1)) * fx();
        if (a <= 0.01F) return;
        float[] col = {0.61F, 0.49F, 1.0F};
        for (int i = 0; i < 4; i++) {
            float r = b.radius * NanoFx.easeOut(Mth.clamp(k - i * 0.12F, 0, 1));
            if (r <= 0.05F) continue;
            NanoFx.ringFlat(vc, m, b.c.add(0, -0.25 + 0.02 * i, 0), r, 0.18F - 0.03F * i, col[0], col[1], col[2], 0.85F * a * (1 - i * 0.2F), 56, false);
        }
        NanoFx.ringBand(vc, m, b.c, 0.6F + 0.4F * k, 2.4F * (1 - k * 0.5F), col[0], col[1], col[2], 0.5F * a, 24, false);
        if (age < 12) {
            Random rng = new Random(b.born * 13L + (long) (age / 2));
            for (int id : b.targets) {
                Entity e = mc.level.getEntity(id);
                if (e != null) NanoFx.arc(vc, m, b.c, e.getPosition(partial).add(0, e.getBbHeight() * 0.5, 0), cam, rng, 0.05F, (1 - age / 12F) * fx(), col);
            }
        }
    }

    /** Afterimage Decoy: a pulsing ground ring and chevrons over the decoy, red tethers from it to the creatures it draws. */
    private static void decoyGlow(VertexConsumer vc, Matrix4f m, Decoy d, Vec3 cam, float t, float partial, Minecraft mc) {
        float a = (d.ended ? Mth.clamp(1 - (t - d.endAt) / 10F, 0, 1) : 1F) * fx();
        if (a <= 0.01F) return;
        float pulse = 0.6F + 0.4F * (float) Math.sin(t * 0.5F);
        NanoFx.ringFlat(vc, m, d.pos.add(0, 0.05, 0), 0.7F + 0.1F * pulse, 0.1F, VIOLET[0], VIOLET[1], VIOLET[2], 0.8F * a, 32, false);
        for (int i = 0; i < 2; i++) {
            Vec3 top = d.pos.add(0, 2.3 + i * 0.3 + 0.1 * Math.sin(t * 0.2), 0);
            NanoFx.ribbon(vc, m, top.add(-0.25, 0.2, 0), top, cam, 0.06F, VIOLET[0], VIOLET[1], VIOLET[2], 0.9F * a, 0.9F * a, false);
            NanoFx.ribbon(vc, m, top, top.add(0.25, 0.2, 0), cam, 0.06F, VIOLET[0], VIOLET[1], VIOLET[2], 0.9F * a, 0.9F * a, false);
        }
        if (d.ended || d.drawn == null) return;
        for (int id : d.drawn) {
            Entity e = mc.level.getEntity(id);
            if (e == null) continue;
            Vec3 from = e.getPosition(partial).add(0, e.getBbHeight() * 0.7, 0), to = d.pos.add(0, 1.0, 0);
            Vec3 step = to.subtract(from).scale(1 / 12.0);
            for (int i = 0; i < 12; i += 2) {
                Vec3 p0 = from.add(step.scale(i)), p1 = from.add(step.scale(i + 1));
                NanoFx.ribbon(vc, m, p0, p1, cam, 0.035F, RED[0], 0.4F, 0.5F, 0.55F * a * pulse, 0.55F * a * pulse, false);
            }
        }
    }

    /** Static Barrage: the thrown-back shots trail blue light. */
    private static void reflectedGlow(VertexConsumer vc, Matrix4f m, Reflected r, Vec3 cam, float t) {
        if (r.path.size() < 2) return;
        float fade = Mth.clamp(1 - (t - r.born - 30) / 10F, 0, 1) * fx();
        Vec3 prev = null;
        int i = 0, n = r.path.size();
        for (Vec3 p : r.path) {
            if (prev != null) NanoFx.ribbon(vc, m, prev, p, cam, 0.12F, FIELD_BLUE[0], FIELD_BLUE[1], FIELD_BLUE[2],
                0.8F * fade * (i - 1F) / n, 0.8F * fade * i / n, false);
            prev = p;
            i++;
        }
    }

    /**
     * Link threads: for each working synergy, a thread of light in its colour winds around the body
     * from one linked piece to the other (third person and other players).
     */
    private static void threadsGlow(VertexConsumer vc, Matrix4f m, Vec3 cam, float t, float partial, Minecraft mc) {
        if (THREADS.isEmpty()) return;
        float f = fx();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        for (var e : THREADS.int2ObjectEntrySet()) {
            Entity p = mc.level.getEntity(e.getIntKey());
            if (p == null || p == mc.player && firstPerson) continue;
            Vec3 base = p.getPosition(partial);
            float h = p.getBbHeight(), r = p.getBbWidth() * 0.5F + 0.22F;
            for (LinkThread th : e.getValue()) {
                float[] col = FxDraw.rgb(th.color());
                float y0 = th.h0() * h, y1 = th.h1() * h;
                if (Math.abs(y1 - y0) < 0.05F) y1 = y0 + 0.35F;             // two units on one piece: a short loop
                Vec3 prev = null;
                for (int i = 0; i <= 18; i++) {
                    float u = i / 18F;
                    double ang = th.index() * 2.1 + u * Math.PI * 2.2 + t * 0.08;
                    double y = Mth.lerp(u, y0, y1);
                    double rr = r + 0.08 * Math.sin(u * Math.PI);
                    Vec3 q = base.add(Math.cos(ang) * rr, y, Math.sin(ang) * rr);
                    if (prev != null) {
                        float pulse = 0.35F + 0.35F * (float) Math.sin(u * 12 - t * 0.6F + th.index());
                        NanoFx.ribbon(vc, m, prev, q, cam, 0.035F, col[0], col[1], col[2], pulse * f, pulse * f, false);
                    }
                    prev = q;
                }
            }
        }
    }

    /** Fleet Datalink: a cyan diamond over each creature the Wingmen see, a turning double ring on the marked one. */
    private static void datalinkGlow(VertexConsumer vc, Matrix4f m, Vec3 cam, float t, float partial, Minecraft mc) {
        // 0.1.10.2: the plain data link feeds the maps and the holo table; the world markers stay the synergy's
        if (!QuantumClient.activeLinks.contains("fleet_datalink")) return;
        float f = fx();
        float[] col = {0.44F, 0.95F, 0.84F};
        for (int id : QuantumClient.DATALINK) {
            Entity e = mc.level.getEntity(id);
            if (e == null || id == QuantumClient.datalinkMarked) continue;
            Vec3 top = e.getPosition(partial).add(0, e.getBbHeight() + 0.45, 0);
            float s = 0.16F;
            NanoFx.ribbon(vc, m, top.add(0, s, 0), top.add(s, 0, 0), cam, 0.04F, col[0], col[1], col[2], 0.85F * f, 0.85F * f, false);
            NanoFx.ribbon(vc, m, top.add(s, 0, 0), top.add(0, -s, 0), cam, 0.04F, col[0], col[1], col[2], 0.85F * f, 0.85F * f, false);
            NanoFx.ribbon(vc, m, top.add(0, -s, 0), top.add(-s, 0, 0), cam, 0.04F, col[0], col[1], col[2], 0.85F * f, 0.85F * f, false);
            NanoFx.ribbon(vc, m, top.add(-s, 0, 0), top.add(0, s, 0), cam, 0.04F, col[0], col[1], col[2], 0.85F * f, 0.85F * f, false);
        }
        if (QuantumClient.datalinkMarked >= 0) {
            Entity e = mc.level.getEntity(QuantumClient.datalinkMarked);
            if (e != null) {
                Vec3 c = center(e, partial);
                float r = e.getBbWidth() * 0.8F + 0.4F;
                FxDraw.ring(vc, m, c, new Vec3(Math.sin(t * 0.1), 1, Math.cos(t * 0.1)), r, 0.06F, col[0], col[1], col[2], 0.9F * f, 40);
                FxDraw.ring(vc, m, c, new Vec3(Math.cos(t * 0.13), 0.3, Math.sin(t * 0.13)), r * 1.2F, 0.05F, 1F, 1F, 1F, 0.6F * f, 40);
            }
        }
    }

    /** Ballistic Sync: the guided path of the next shot to each locked creature, a dotted arc of amber light. */
    private static void guidedPathsGlow(VertexConsumer vc, Matrix4f m, Vec3 cam, float t, float partial, Minecraft mc) {
        if (mc.player == null || !com.miophas.singularity_iteration.common.suit.quantum.QuantumLinks.active(mc.player,
                com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergy.BALLISTIC_SYNC)) return;
        List<Integer> locked = com.miophas.singularity_iteration.common.client.suit.SmartGuidanceClient.locked();
        if (locked.isEmpty()) return;
        float f = fx();
        Vec3 eye = mc.player.getEyePosition(partial);
        Vec3 look = mc.player.getViewVector(partial);
        Vec3 start = eye.add(look.scale(1.2)).add(0, -0.25, 0);
        for (int id : locked) {
            Entity e = mc.level.getEntity(id);
            if (e == null) continue;
            Vec3 end = center(e, partial);
            double d = start.distanceTo(end);
            Vec3 c1 = start.add(look.scale(d * 0.45)).add(0, d * 0.08, 0);
            Vec3 prev = start;
            for (int i = 1; i <= 24; i++) {
                float u = i / 24F;
                Vec3 q = start.scale((1 - u) * (1 - u)).add(c1.scale(2 * u * (1 - u))).add(end.scale(u * u));
                float flow = (float) ((u * 6 - t * 0.12) % 1.0);
                if (flow < 0) flow += 1;
                if (flow < 0.55F) NanoFx.ribbon(vc, m, prev, q, cam, 0.05F, GOLD[0], GOLD[1], GOLD[2], 0.6F * f * u, 0.6F * f * u, false);
                prev = q;
            }
        }
    }
}

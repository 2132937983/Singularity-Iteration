// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit.nano;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.suit.nano.NanoBursts;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.util.TriState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * World effects of the nano suit (0.1.8.5).
 *
 * <ul>
 *   <li><b>Kinetic Dash</b>: hologram-grid afterimages of the body along the path, a heat-haze
 *       streak that bends the scene behind the dash, a cyan speed smear and a radial pressure
 *       ring at the start.</li>
 *   <li><b>EMP</b>: a core flash, an expanding shock wave (a horizontal band, a ground ring and
 *       three meridian rings) with a haze front, jagged discharge arcs to every creature it hit
 *       and to the ground, crackle on stunned creatures, sparks where projectiles were erased.</li>
 *   <li><b>Optical camouflage</b>: deploy = the hologram grid sweeps over the body and the body
 *       flickers out; active = the body refracts the scene (shader) with a faint grid shimmer;
 *       release = the grid flickers back. Without the shader a faint grid ghost remains.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class NanoFx {
    private NanoFx() {}

    static final float[] CYAN = {0.36F, 0.92F, 1.0F}, EMP_BLUE = {0.48F, 0.7F, 1.0F}, WHITE = {0.92F, 0.98F, 1.0F};
    private static final Random RNG = new Random();
    /** Camouflage transition length (deploy and release), ticks. */
    static final int CAMO_TRANSITION = 14;

    // ------------------------------------------------------------------ state

    private record Ghost(int entity, Vec3 pos, int born, int tier) { }
    private static final class Dash {
        final int entity, born, tier;
        final List<Vec3> path = new ArrayList<>();
        final Vec3 dir;
        Dash(int entity, int born, Vec3 start, Vec3 dir, int tier) { this.entity = entity; this.born = born; this.dir = dir; this.tier = tier; path.add(start); }
    }
    private record Emp(Vec3 c, float radius, int born, int[] targets, float[] points, boolean self, int tier) { }
    /** 0.1.8.6: kinetic armor mesh run of a player; hits flash the plating. */
    private static final class Mesh {
        int on, off = -1, max, tier, lastHit = -100;
    }
    private record MeshHit(int entity, Vec3 dir, int born) { }
    private record Ping(int entity, int born, int color) { }
    private record Cloud(Vec3 eye, Vec3 dir, float range, boolean sphere, int born) { }
    private record Rebound(Vec3 at, int born, int tier) { }
    static final float[] AMBER = {1.0F, 0.66F, 0.16F}, VIOLET = {0.72F, 0.48F, 1.0F};
    private static final class Camo {
        int on, off = -1, refresh;
        boolean hidden;
    }
    private record Queued(Player player, float partial, Matrix4f pose, Matrix3f normal, int light, float grid, float refract, boolean hideBody,
                          float mesh) { }

    private static final List<Ghost> GHOSTS = new ArrayList<>();
    private static final Int2ObjectOpenHashMap<Dash> DASHES = new Int2ObjectOpenHashMap<>();
    private static final List<Dash> TRAILS = new ArrayList<>();
    private static final List<Emp> EMPS = new ArrayList<>();
    private static final Int2IntOpenHashMap STUNNED = new Int2IntOpenHashMap();
    private static final Int2ObjectOpenHashMap<Camo> CAMO = new Int2ObjectOpenHashMap<>();
    private static final Int2IntOpenHashMap VENTS = new Int2IntOpenHashMap();
    private static final List<Queued> QUEUE = new ArrayList<>();
    private static final Int2ObjectOpenHashMap<Mesh> MESH = new Int2ObjectOpenHashMap<>();
    private static final List<MeshHit> MESH_HITS = new ArrayList<>();
    /** Recon glow of the local player: entity id -> last tick of the glow. */
    private static final Int2IntOpenHashMap RECON = new Int2IntOpenHashMap();
    private static final List<Ping> PINGS = new ArrayList<>();
    private static final List<Cloud> CLOUDS = new ArrayList<>();
    private static final List<Rebound> REBOUNDS = new ArrayList<>();
    /** True while this class draws an entity itself (guards the render events). */
    private static boolean drawing;
    private static int now;

    static void clear() {
        GHOSTS.clear();
        DASHES.clear();
        TRAILS.clear();
        EMPS.clear();
        STUNNED.clear();
        CAMO.clear();
        VENTS.clear();
        QUEUE.clear();
        MESH.clear();
        MESH_HITS.clear();
        RECON.clear();
        PINGS.clear();
        CLOUDS.clear();
        REBOUNDS.clear();
    }

    static float fx() {
        return NanoClient.NanoFxIntensity.get();
    }

    // ------------------------------------------------------------------ packet entry points

    static void startDash(NanoPackets.Fx fx, boolean self) {
        Vec3 start = new Vec3(fx.x(), fx.y(), fx.z());
        Dash d = new Dash(fx.entityId(), now, start, new Vec3(fx.a(), 0, fx.b()), fx.targets().length > 0 ? fx.targets()[0] : 1);
        DASHES.put(fx.entityId(), d);
        TRAILS.add(d);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        // pressure puff at the start
        for (int i = 0; i < 14 * fx(); i++) {
            double a = RNG.nextDouble() * Math.PI * 2;
            mc.level.addParticle(ParticleTypes.CLOUD, start.x, start.y + 0.1, start.z, Math.cos(a) * 0.18, 0.02, Math.sin(a) * 0.18);
        }
    }

    static void startEmp(NanoPackets.Fx fx, boolean self) {
        EMPS.add(new Emp(new Vec3(fx.x(), fx.y(), fx.z()), fx.a(), now, fx.targets(), fx.points(), self, Math.max(1, (int) fx.b())));
        for (int id : fx.targets()) STUNNED.put(id, now + 60);
        if (self) NanoHud.NanoFxEmpClock.last = NanoClient.ticks;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float[] p = fx.points();
        for (int i = 0; i + 2 < p.length; i += 3) {
            for (int k = 0; k < 10; k++) mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, p[i], p[i + 1], p[i + 2],
                RNG.nextGaussian() * 0.25, RNG.nextGaussian() * 0.25, RNG.nextGaussian() * 0.25);
            mc.level.addParticle(ParticleTypes.FLASH, p[i], p[i + 1], p[i + 2], 0, 0, 0);
        }
        for (int k = 0; k < 40 * fx(); k++) {
            double a = RNG.nextDouble() * Math.PI * 2;
            mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, fx.x(), fx.y(), fx.z(), Math.cos(a) * 0.9, (RNG.nextDouble() - 0.3) * 0.3, Math.sin(a) * 0.9);
        }
    }

    /** 0.1.9.0: arc discharge of the advanced propulsion core: an amber EMP-like wave with heavier arcs. */
    static void startDischarge(NanoPackets.Fx fx, boolean self) {
        EMPS.add(new Emp(new Vec3(fx.x(), fx.y(), fx.z()), fx.a(), now, fx.targets(), new float[0], self, 3));
        if (self) NanoHud.NanoFxEmpClock.last = NanoClient.ticks;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (int k = 0; k < 60 * fx(); k++) {
            double a = RNG.nextDouble() * Math.PI * 2;
            mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, fx.x(), fx.y(), fx.z(), Math.cos(a) * 1.2, (RNG.nextDouble() - 0.2) * 0.4, Math.sin(a) * 1.2);
        }
        for (int k = 0; k < 24 * fx(); k++) {
            double a = RNG.nextDouble() * Math.PI * 2;
            mc.level.addParticle(ParticleTypes.CLOUD, fx.x(), fx.y() - 0.8, fx.z(), Math.cos(a) * 0.5, 0.02, Math.sin(a) * 0.5);
        }
    }

    static void camoOn(int entity, boolean hidden, boolean fresh) {
        Camo c = CAMO.get(entity);
        if (c == null || c.off >= 0) {
            c = new Camo();
            c.on = fresh ? now : now - CAMO_TRANSITION;
            CAMO.put(entity, c);
        }
        c.hidden = hidden;
        c.refresh = now;
    }

    static void camoOff(int entity, int reason) {
        Camo c = CAMO.get(entity);
        if (c == null || c.off >= 0) return;
        c.off = now;
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null ? null : mc.level.getEntity(entity);
        if (e != null && reason == 1) {     // broken: a burst of static
            for (int k = 0; k < 16; k++) mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, e.getRandomX(0.6), e.getRandomY(), e.getRandomZ(0.6),
                RNG.nextGaussian() * 0.1, RNG.nextGaussian() * 0.1, RNG.nextGaussian() * 0.1);
        }
    }

    static void overheat(int entity) {
        VENTS.put(entity, now + 30);
    }

    // ---- 0.1.8.6

    static void meshOn(int entity, int ticks, int tier) {
        Mesh m = new Mesh();
        m.on = now;
        m.max = Math.max(1, ticks);
        m.tier = tier;
        MESH.put(entity, m);
    }

    static void meshOff(int entity) {
        Mesh m = MESH.get(entity);
        if (m != null && m.off < 0) m.off = now;
    }

    static void meshHit(int entity, Vec3 dir) {
        Mesh m = MESH.get(entity);
        if (m != null) m.lastHit = now;
        MESH_HITS.add(new MeshHit(entity, dir, now));
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null ? null : mc.level.getEntity(entity);
        if (e == null) return;
        Vec3 at = e.position().add(0, e.getBbHeight() * 0.55, 0).add(dir.scale(0.45));
        for (int k = 0; k < 10 * fx(); k++) {
            mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, dir.x * 0.3 + RNG.nextGaussian() * 0.12,
                dir.y * 0.3 + RNG.nextGaussian() * 0.12, dir.z * 0.3 + RNG.nextGaussian() * 0.12);
        }
    }

    /** The local player's recon result: these creatures glow for {@code ticks}. */
    static void recon(int[] targets, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        int until = now + ticks;
        for (int i = 0; i < targets.length; i++) {
            RECON.put(targets[i], until);
            Entity e = mc.level == null ? null : mc.level.getEntity(targets[i]);
            PINGS.add(new Ping(targets[i], now + i / 2, reconColor(e)));
        }
    }

    static void reconCloud(NanoPackets.Fx fx) {
        float range = fx.targets().length > 0 ? fx.targets()[0] : 24;
        boolean sphere = fx.targets().length > 1 && fx.targets()[1] == 1;
        CLOUDS.add(new Cloud(new Vec3(fx.x(), fx.y(), fx.z()), new Vec3(fx.a(), fx.b(), fx.c()).normalize(), range, sphere, now));
    }

    static void rebound(NanoPackets.Fx fx) {
        Vec3 at = new Vec3(fx.x(), fx.y() + 0.05, fx.z());
        REBOUNDS.add(new Rebound(at, now, Math.max(1, (int) fx.a())));
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (int k = 0; k < 18 * fx(); k++) {
            double a = RNG.nextDouble() * Math.PI * 2;
            mc.level.addParticle(GEL, at.x + Math.cos(a) * 0.3, at.y + 0.1, at.z + Math.sin(a) * 0.3, Math.cos(a) * 0.25, 0.15 + RNG.nextDouble() * 0.2,
                Math.sin(a) * 0.25);
        }
    }

    private static final net.minecraft.core.particles.DustParticleOptions GEL =
        new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.31F, 0.76F, 0.97F), 0.9F);
    private static final net.minecraft.core.particles.DustParticleOptions SWARM =
        new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.4F, 1.0F, 0.85F), 0.45F);

    /** Recon glow (local player only): the glow mixins ask this through {@code FcsState}. */
    public static boolean reconGlow(Entity entity) {
        return !RECON.isEmpty() && RECON.getOrDefault(entity.getId(), -1) >= now;
    }

    /** Outline colour of a recon contact: hostile red, player yellow, others teal. */
    public static int reconColor(@org.jetbrains.annotations.Nullable Entity entity) {
        if (entity instanceof net.minecraft.world.entity.monster.Enemy) return 0xFF5A4C;
        if (entity instanceof Player) return 0xFFD23F;
        return 0x64FFDA;
    }

    /** Recon contacts still glowing (HUD). */
    static int reconCount() {
        int n = 0;
        for (var e : RECON.int2IntEntrySet()) if (e.getIntValue() >= now) n++;
        return n;
    }

    /** Camouflage state of a player for the HUD (ticks since on, or -1). */
    static boolean camoActive(int entity) {
        Camo c = CAMO.get(entity);
        return c != null && c.off < 0;
    }

    // ------------------------------------------------------------------ tick

    static void tick(Minecraft mc, int ticks) {
        now = ticks;
        // dash path capture and afterimages
        for (Iterator<Dash> it = DASHES.values().iterator(); it.hasNext(); ) {
            Dash d = it.next();
            Entity e = mc.level.getEntity(d.entity);
            int age = now - d.born;
            if (e == null || age > NanoBursts.DASH_TICKS + 1) { it.remove(); continue; }
            d.path.add(e.position());
            if (age <= NanoBursts.DASH_TICKS) GHOSTS.add(new Ghost(d.entity, e.position(), now, d.tier));
            for (int k = 0; k < 3 * fx(); k++) {
                mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, e.getRandomX(0.5), e.getRandomY(), e.getRandomZ(0.5),
                    -d.dir.x * 0.2, 0.02, -d.dir.z * 0.2);
            }
            mc.level.addParticle(ParticleTypes.END_ROD, e.getX(), e.getY() + 0.9 + RNG.nextGaussian() * 0.25, e.getZ(), -d.dir.x * 0.05, 0, -d.dir.z * 0.05);
        }
        GHOSTS.removeIf(g -> now - g.born > 9);
        TRAILS.removeIf(d -> now - d.born > 16);
        EMPS.removeIf(e -> now - e.born > 22);
        STUNNED.int2IntEntrySet().removeIf(e -> e.getIntValue() < now);
        VENTS.int2IntEntrySet().removeIf(e -> e.getIntValue() < now);
        RECON.int2IntEntrySet().removeIf(e -> e.getIntValue() < now);
        PINGS.removeIf(p -> now - p.born > 24);
        CLOUDS.removeIf(c -> now - c.born > 26);
        REBOUNDS.removeIf(r -> now - r.born > 12);
        MESH_HITS.removeIf(h -> now - h.born > 10);
        MESH.int2ObjectEntrySet().removeIf(e -> {
            Mesh m = e.getValue();
            return m.off >= 0 ? now - m.off > 10 : now - m.on > m.max + 40;
        });
        // recon swarm: nano dust spreading from the eye over the cone (or the sphere)
        for (Cloud c : CLOUDS) {
            int age = now - c.born;
            if (age > 18) continue;
            for (int k = 0; k < 14 * fx(); k++) {
                Vec3 d = c.sphere ? new Vec3(RNG.nextGaussian(), RNG.nextGaussian() * 0.5, RNG.nextGaussian()).normalize()
                    : c.dir.add(RNG.nextGaussian() * 0.35, RNG.nextGaussian() * 0.25, RNG.nextGaussian() * 0.35).normalize();
                double dist = c.range * (age / 18.0) * (0.6 + 0.4 * RNG.nextDouble());
                Vec3 at = c.eye.add(d.scale(dist));
                mc.level.addParticle(SWARM, at.x, at.y, at.z, d.x * 0.05, d.y * 0.05, d.z * 0.05);
            }
        }
        // mesh: amber sparks crawl over the plating while it runs
        if (now % 2 == 0) {
            for (var e : MESH.int2ObjectEntrySet()) {
                if (e.getValue().off >= 0) continue;
                Entity t = mc.level.getEntity(e.getIntKey());
                if (t != null && RNG.nextInt(3) == 0) mc.level.addParticle(ParticleTypes.WAX_ON, t.getRandomX(0.6), t.getRandomY(), t.getRandomZ(0.6), 0, 0, 0);
            }
        }
        // camouflage: drop stale entries (no refresh for 2 s) and finished releases
        CAMO.int2ObjectEntrySet().removeIf(e -> {
            Camo c = e.getValue();
            if (c.off >= 0) return now - c.off > CAMO_TRANSITION;
            return now - c.refresh > 45;
        });
        // crackle on stunned creatures
        if (now % 3 == 0) {
            for (var e : STUNNED.int2IntEntrySet()) {
                Entity t = mc.level.getEntity(e.getIntKey());
                if (t != null) mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, t.getRandomX(0.7), t.getRandomY(), t.getRandomZ(0.7), 0, 0.05, 0);
            }
        }
        // overheat: vent steam from the back
        for (var e : VENTS.int2IntEntrySet()) {
            Entity t = mc.level.getEntity(e.getIntKey());
            if (t == null) continue;
            Vec3 back = Vec3.directionFromRotation(0, t.getYRot()).scale(-0.35);
            for (int k = 0; k < 2; k++) mc.level.addParticle(ParticleTypes.CLOUD, t.getX() + back.x, t.getY() + 1.3, t.getZ() + back.z,
                back.x * 0.3 + RNG.nextGaussian() * 0.02, 0.06, back.z * 0.3 + RNG.nextGaussian() * 0.02);
        }
        // HF sync: sparks at the blade of every player with an active saber and the myofiber
        if (now % 4 == 0) {
            for (Player p : mc.level.players()) {
                if (p.distanceToSqr(mc.player) > 48 * 48 || p.isInvisible()) continue;
                if (!com.miophas.singularity_iteration.common.suit.nano.NanoSuit.hfSync(p)) continue;
                Vec3 hand = p.getEyePosition().add(p.getViewVector(1.0F).scale(0.8)).add(0, -0.45, 0);
                if (p == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
                mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, RNG.nextGaussian() * 0.04, 0.02, RNG.nextGaussian() * 0.04);
            }
        }
    }

    // ------------------------------------------------------------------ camouflage: player rendering

    /** Camouflage phase of a player: [0] vanilla body visibility 0..1, [1] grid intensity, [2] refraction strength. */
    private static float[] camoPhase(Player player, float partial) {
        Camo c = CAMO.get(player.getId());
        if (c == null) return null;
        Minecraft mc = Minecraft.getInstance();
        boolean hide = c.hidden || player == mc.player;
        float t = now + partial;
        float body, grid, refract;
        if (c.off < 0) {
            float k = Mth.clamp((t - c.on) / CAMO_TRANSITION, 0, 1);
            body = 1 - smooth(0.3F, 1.0F, k);
            grid = k < 1 ? 0.25F + 0.75F * (float) Math.sin(k * Math.PI) : 0.07F + 0.05F * (float) Math.sin(t * 0.37F);
            refract = smooth(0.4F, 1.0F, k);
        } else {
            float k = Mth.clamp((t - c.off) / CAMO_TRANSITION, 0, 1);
            body = smooth(0.0F, 0.75F, k);
            grid = (1 - k) * (0.4F + 0.6F * (float) Math.sin((1 - k) * Math.PI));
            refract = 1 - smooth(0.0F, 0.6F, k);
        }
        if (!hide) {           // camouflage switched off against players: a light shimmer only
            body = 1;
            refract = 0;
            grid *= 0.6F;
        }
        // occasional glitch spike of the grid while active
        if (c.off < 0 && hash(player.getId(), (int) (t * 2)) % 23 == 0) grid += 0.35F;
        return new float[]{body, grid * fx(), refract};
    }

    /** Plating brightness of the kinetic armor mesh: deploy flash, a faint pulse, hit flashes, a fade at the end. */
    private static float meshIntensity(int entity, float partial) {
        Mesh m = MESH.get(entity);
        if (m == null) return 0;
        float t = now + partial;
        float v;
        if (m.off >= 0) {
            v = Mth.clamp(1 - (t - m.off) / 10F, 0, 1) * 0.5F;
        } else {
            float age = t - m.on;
            v = age < 8 ? 1.0F - age / 8F * 0.7F : 0.2F + 0.07F * (float) Math.sin(t * 0.5F);
        }
        float sinceHit = t - m.lastHit;
        if (sinceHit < 6) v += 0.6F * (1 - sinceHit / 6F);
        return v * fx() * (m.tier >= 2 ? 1.15F : 1F);
    }

    private static float smooth(float e0, float e1, float x) {
        float t = Mth.clamp((x - e0) / (e1 - e0), 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static int hash(int a, int b) {
        int h = a * 0x9E3779B1 + b * 0x85EBCA77;
        h ^= h >>> 15;
        return h & 0x7FFFFFFF;
    }

    /** True between the solid-block stage and the end of the level pass: only world renders of a player get the camouflage. */
    private static boolean levelPass;

    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (drawing || !levelPass) return;            // not the inventory preview or other GUI renders
        Player player = event.getEntity();
        float partial = event.getPartialTick();
        float[] phase = camoPhase(player, partial);
        float mesh = meshIntensity(player.getId(), partial);
        if (phase == null && mesh <= 0) return;
        if (phase == null) {
            PoseStack pose = event.getPoseStack();
            QUEUE.add(new Queued(player, partial, new Matrix4f(pose.last().pose()), new Matrix3f(pose.last().normal()), event.getPackedLight(),
                0, 0, false, mesh));
            return;
        }
        // the body flickers out: a stable on/off pattern per half tick, weighted by visibility
        boolean bodyVisible = phase[0] >= 0.999F || phase[0] > 0.001F && (hash(player.getId() * 31, (int) ((now + partial) * 2)) % 100) < phase[0] * 100;
        PoseStack pose = event.getPoseStack();
        QUEUE.add(new Queued(player, partial, new Matrix4f(pose.last().pose()), new Matrix3f(pose.last().normal()), event.getPackedLight(),
            phase[1], bodyVisible ? 0 : phase[2], !bodyVisible, mesh));
        if (!bodyVisible) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onNameTag(RenderNameTagEvent event) {
        if (drawing) { event.setCanRender(TriState.FALSE); return; }
        if (event.getEntity() instanceof Player p) {
            Camo c = CAMO.get(p.getId());
            if (c != null && c.off < 0 && c.hidden) event.setCanRender(TriState.FALSE);
        }
    }

    // ------------------------------------------------------------------ world passes

    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            levelPass = false;
            QUEUE.clear();
            return;
        }
        if (mc.level == null || mc.player == null) return;
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            levelPass = true;
            QUEUE.clear();
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            refractionPass(event, mc);
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            glowPass(event, mc);
        }
    }

    /** Scene copy, then refracting bodies and haze; then the grid overlays of the queued players. */
    private static void refractionPass(RenderLevelStageEvent event, Minecraft mc) {
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        boolean haze = !TRAILS.isEmpty() || !EMPS.isEmpty() || !VENTS.isEmpty();
        boolean refractNeeded = haze;
        for (Queued q : QUEUE) if (q.refract > 0.01F) refractNeeded = true;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        NanoRender.setScroll((now + partial) * 0.012F, (now + partial) * 0.02F);
        boolean refraction = refractNeeded && NanoRender.copyScene();
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        try {
            if (refraction) {
                for (Queued q : QUEUE) {
                    if (q.refract <= 0.01F) continue;
                    drawPlayer(mc, q.player, q.partial, stack(q), q.light, new RemapBuffers(buffers, NanoRender.REFRACT,
                        CYAN[0], CYAN[1], CYAN[2], q.refract));
                }
                buffers.endBatch(NanoRender.REFRACT);
                PoseStack pose = event.getPoseStack();
                pose.pushPose();
                pose.translate(-cam.x, -cam.y, -cam.z);
                VertexConsumer vc = buffers.getBuffer(NanoRender.HAZE);
                Matrix4f m = pose.last().pose();
                for (Dash d : TRAILS) dashHaze(vc, m, d, cam, partial);
                for (Emp e : EMPS) empHaze(vc, m, e, partial);
                for (var v : VENTS.int2IntEntrySet()) ventHaze(vc, m, mc.level.getEntity(v.getIntKey()), v.getIntValue(), cam, partial);
                buffers.endBatch(NanoRender.HAZE);
                pose.popPose();
            }
            for (Queued q : QUEUE) {
                // without refraction a hidden body keeps a faint grid ghost
                float grid = q.grid + (!refraction && q.hideBody ? 0.12F : 0);
                if (grid <= 0.01F) continue;
                drawPlayer(mc, q.player, q.partial, stack(q), q.light, new RemapBuffers(buffers, NanoRender.HOLOGRID,
                    CYAN[0] * grid, CYAN[1] * grid, CYAN[2] * grid, 1));
            }
            buffers.endBatch(NanoRender.HOLOGRID);
            // 0.1.8.6: kinetic armor mesh plating (amber hexagons)
            for (Queued q : QUEUE) {
                if (q.mesh <= 0.01F || q.hideBody) continue;
                drawPlayer(mc, q.player, q.partial, stack(q), q.light, new RemapBuffers(buffers, NanoRender.HEXMESH,
                    AMBER[0] * q.mesh, AMBER[1] * q.mesh, AMBER[2] * q.mesh, 1));
            }
            buffers.endBatch(NanoRender.HEXMESH);
        } finally {
            QUEUE.clear();
        }
    }

    private static PoseStack stack(Queued q) {
        PoseStack ps = new PoseStack();
        ps.last().pose().set(q.pose);
        ps.last().normal().set(q.normal);
        return ps;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void drawPlayer(Minecraft mc, Player player, float partial, PoseStack pose, int light, MultiBufferSource buffers) {
        EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(player);
        drawing = true;
        try {
            renderer.render(player, Mth.lerp(partial, player.yRotO, player.getYRot()), partial, pose, buffers, light);
        } catch (RuntimeException e) {
            Singularity_Iteration.LOGGER.debug("nano effect render of {} failed", player.getName().getString(), e);
        } finally {
            drawing = false;
        }
    }

    /** Afterimages, speed smears, EMP waves and arcs: additive glow after the particles. */
    private static void glowPass(RenderLevelStageEvent event, Minecraft mc) {
        if (GHOSTS.isEmpty() && TRAILS.isEmpty() && EMPS.isEmpty() && STUNNED.isEmpty() && MESH_HITS.isEmpty() && PINGS.isEmpty()
            && REBOUNDS.isEmpty()) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Vec3 cam = event.getCamera().getPosition();
        // afterimages: the body drawn in the hologram grid at the stored positions, fading out
        for (Ghost g : GHOSTS) {
            Entity e = mc.level.getEntity(g.entity);
            if (!(e instanceof AbstractClientPlayer player)) continue;
            if (g.pos.distanceToSqr(cam) < 1.6 * 1.6) continue;          // not inside the camera
            float age = now - g.born + partial;
            float a = Mth.clamp(1 - age / 9F, 0, 1) * 0.9F * fx();
            if (a <= 0.02F) continue;
            // Mk.II: every second afterimage is violet
            float[] col = g.tier >= 2 && (g.born & 1) == 1 ? VIOLET : CYAN;
            PoseStack ps = new PoseStack();
            ps.mulPose(event.getPoseStack().last().pose());
            ps.translate(g.pos.x - cam.x, g.pos.y - cam.y, g.pos.z - cam.z);
            drawPlayer(mc, player, partial, ps, 0xF000F0, new RemapBuffers(buffers, NanoRender.HOLOGRID,
                col[0] * a, col[1] * a, col[2] * a, 1));
        }
        buffers.endBatch(NanoRender.HOLOGRID);

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        VertexConsumer glow = buffers.getBuffer(SiRenderTypes.ENERGY_BEAM);
        for (Dash d : TRAILS) dashGlow(glow, m, d, cam, partial);
        for (Emp e : EMPS) empGlow(glow, m, e, cam, partial, mc);
        stunCrackle(glow, m, cam, partial, mc);
        for (MeshHit h : MESH_HITS) meshRipple(glow, m, h, cam, partial, mc);
        for (Ping p : PINGS) ping(glow, m, p, partial, mc);
        for (Rebound r : REBOUNDS) {
            float age = now - r.born + partial;
            float a = Mth.clamp(1 - age / 12F, 0, 1) * fx();
            float[] c = r.tier >= 2 ? VIOLET : CYAN;
            ringFlat(glow, m, r.at, 0.3F + age * 0.25F, 0.12F, c[0], c[1], c[2], 0.8F * a, 40, false);
            ringFlat(glow, m, r.at.add(0, 0.02, 0), 0.15F + age * 0.14F, 0.06F, 1F, 1F, 1F, 0.5F * a, 32, false);
        }
        buffers.endBatch(SiRenderTypes.ENERGY_BEAM);
        pose.popPose();
    }

    // ------------------------------------------------------------------ dash geometry

    private static void dashHaze(VertexConsumer vc, Matrix4f m, Dash d, Vec3 cam, float partial) {
        float age = now - d.born + partial;
        float s = Mth.clamp(1 - age / 16F, 0, 1) * fx();
        if (s <= 0.01F || d.path.size() < 2) return;
        for (int i = 1; i < d.path.size(); i++) {
            Vec3 a = d.path.get(i - 1).add(0, 0.95, 0), b = d.path.get(i).add(0, 0.95, 0);
            float sa = s * (0.4F + 0.6F * (i - 1) / d.path.size()), sb = s * (0.4F + 0.6F * i / d.path.size());
            ribbon(vc, m, a, b, cam, 0.85F, 0.6F, 0.85F, 1.0F, sa, sb, true);
        }
        // pressure ring at the start: a flat expanding torus of haze
        Vec3 o = d.path.get(0).add(0, 0.08, 0);
        float r = 0.4F + age * 0.22F;
        ringFlat(vc, m, o, r, 0.35F, 0.7F, 0.9F, 1.0F, s * 0.9F, 32, true);
    }

    private static void dashGlow(VertexConsumer vc, Matrix4f m, Dash d, Vec3 cam, float partial) {
        float age = now - d.born + partial;
        float s = Mth.clamp(1 - age / 12F, 0, 1) * fx();
        if (s <= 0.01F || d.path.size() < 2) return;
        for (int i = 1; i < d.path.size(); i++) {
            Vec3 a = d.path.get(i - 1), b = d.path.get(i);
            float k0 = (i - 1F) / d.path.size(), k1 = (float) i / d.path.size();
            for (float h : new float[]{0.35F, 0.95F, 1.45F}) {
                ribbon(vc, m, a.add(0, h, 0), b.add(0, h, 0), cam, 0.06F, CYAN[0], CYAN[1], CYAN[2], 0.5F * s * k0, 0.5F * s * k1, false);
            }
        }
        Vec3 o = d.path.get(0).add(0, 0.06, 0);
        ringFlat(vc, m, o, 0.4F + age * 0.22F, 0.08F, CYAN[0], CYAN[1], CYAN[2], 0.6F * s, 32, false);
    }

    // ------------------------------------------------------------------ EMP geometry

    private static void empHaze(VertexConsumer vc, Matrix4f m, Emp e, float partial) {
        float age = now - e.born + partial;
        float k = Mth.clamp(age / 8F, 0, 1);
        float s = (age < 8 ? 1 : Mth.clamp(1 - (age - 8) / 8F, 0, 1)) * fx();
        if (s <= 0.01F) return;
        float r = e.radius * easeOut(k);
        ringBand(vc, m, e.c, r, 0.9F, 0.5F, 0.7F, 1.0F, s, 48, true);
    }

    private static void empGlow(VertexConsumer vc, Matrix4f m, Emp e, Vec3 cam, float partial, Minecraft mc) {
        float age = now - e.born + partial;
        float f = fx();
        float k = Mth.clamp(age / 8F, 0, 1);
        float fade = age < 8 ? 1 : Mth.clamp(1 - (age - 8) / 12F, 0, 1);
        float r = Math.max(0.2F, e.radius * easeOut(k));
        // core flash
        if (age < 5) {
            float a = (1 - age / 5F) * f;
            ball(vc, m, e.c, 0.4F + age * 0.35F, WHITE, a, cam);
        }
        // shock wave: band at centre height, ground ring, three meridians turning
        float[] wave = e.tier >= 3 ? AMBER : e.tier >= 2 ? VIOLET : EMP_BLUE;  // Mk.II violet; 0.1.9.0 arc discharge amber
        ringBand(vc, m, e.c, r, 0.35F, wave[0], wave[1], wave[2], 0.75F * fade * f, 56, false);
        ringFlat(vc, m, new Vec3(e.c.x, e.c.y - 0.9, e.c.z), r, 0.18F, wave[0], wave[1], wave[2], 0.6F * fade * f, 56, false);
        for (int i = 0; i < 3; i++) {
            float yaw = (float) (i * Math.PI / 3 + age * 0.05F);
            meridian(vc, m, e.c, r, yaw, 0.05F, wave, 0.35F * fade * f, cam);
        }
        // discharge arcs to every creature hit and to the ground around (re-rolled every 2 ticks)
        if (age < 14) {
            float a = (1 - age / 14F) * f;
            Random arcRng = new Random(e.born * 31L + (now - e.born) / 2);
            for (int id : e.targets) {
                Entity t = mc.level.getEntity(id);
                if (t == null) continue;
                Vec3 to = t.getPosition(partial).add(0, t.getBbHeight() * 0.6, 0);
                for (int n = 0; n < (e.tier >= 3 ? 3 : 2); n++) arc(vc, m, e.c, to, cam, arcRng, e.tier >= 3 ? 0.07F : 0.05F, a, wave);
            }
            for (int n = 0; n < 6; n++) {
                double ang = arcRng.nextDouble() * Math.PI * 2;
                double d = r * (0.6 + 0.4 * arcRng.nextDouble());
                Vec3 to = e.c.add(Math.cos(ang) * d, -0.9 + arcRng.nextDouble() * 0.4, Math.sin(ang) * d);
                arc(vc, m, e.c, to, cam, arcRng, 0.035F, a * 0.8F, wave);
            }
        }
    }

    private static void stunCrackle(VertexConsumer vc, Matrix4f m, Vec3 cam, float partial, Minecraft mc) {
        if (STUNNED.isEmpty()) return;
        Random r = new Random(now / 2 * 7919L);
        for (var en : STUNNED.int2IntEntrySet()) {
            Entity t = mc.level.getEntity(en.getIntKey());
            if (t == null || r.nextInt(3) != 0) continue;
            AABB box = t.getBoundingBox().move(t.getPosition(partial).subtract(t.position()));
            Vec3 a = new Vec3(Mth.lerp(r.nextDouble(), box.minX, box.maxX), Mth.lerp(r.nextDouble(), box.minY, box.maxY), Mth.lerp(r.nextDouble(), box.minZ, box.maxZ));
            Vec3 b = new Vec3(Mth.lerp(r.nextDouble(), box.minX, box.maxX), Mth.lerp(r.nextDouble(), box.minY, box.maxY), Mth.lerp(r.nextDouble(), box.minZ, box.maxZ));
            arc(vc, m, a, b, cam, r, 0.025F, 0.8F * fx(), EMP_BLUE);
        }
    }

    private static float easeOut(float k) {
        return 1 - (1 - k) * (1 - k) * (1 - k);
    }

    // ------------------------------------------------------------------ primitives (world space, translated by -camera)

    /** Camera-facing ribbon from a to b; {@code haze}: the alpha is the distortion strength. */
    private static void ribbon(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 cam, float width, float r, float g, float bl,
                               float aa, float ab, boolean haze) {
        Vec3 dir = b.subtract(a);
        if (dir.lengthSqr() < 1.0E-6) return;
        Vec3 side = dir.cross(a.subtract(cam));
        if (side.lengthSqr() < 1.0E-8) return;
        side = side.normalize().scale(width * 0.5);
        if (haze) {
            // soft edges: the strength falls to zero at the ribbon border (two strips)
            quad(vc, m, a.subtract(side), a, b, b.subtract(side), r, g, bl, 0, aa, ab, 0);
            quad(vc, m, a, a.add(side), b.add(side), b, r, g, bl, aa, 0, 0, ab);
        } else {
            quad(vc, m, a.subtract(side), a.add(side), b.add(side), b.subtract(side), r, g, bl, aa, aa, ab, ab);
        }
    }

    /** Flat ring (torus band) in the horizontal plane. */
    private static void ringFlat(VertexConsumer vc, Matrix4f m, Vec3 c, float radius, float width, float r, float g, float b, float a,
                                 int segments, boolean haze) {
        float r0 = Math.max(0, radius - width / 2), r1 = radius + width / 2;
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2 / segments, a1 = (i + 1) * Math.PI * 2 / segments;
            Vec3 i0 = c.add(Math.cos(a0) * r0, 0, Math.sin(a0) * r0), i1 = c.add(Math.cos(a1) * r0, 0, Math.sin(a1) * r0);
            Vec3 o0 = c.add(Math.cos(a0) * r1, 0, Math.sin(a0) * r1), o1 = c.add(Math.cos(a1) * r1, 0, Math.sin(a1) * r1);
            Vec3 m0 = c.add(Math.cos(a0) * radius, 0, Math.sin(a0) * radius), m1 = c.add(Math.cos(a1) * radius, 0, Math.sin(a1) * radius);
            if (haze) {
                quad(vc, m, i0, m0, m1, i1, r, g, b, 0, a, a, 0);
                quad(vc, m, m0, o0, o1, m1, r, g, b, a, 0, 0, a);
            } else {
                quad(vc, m, i0, o0, o1, i1, r, g, b, a * 0.4F, a, a, a * 0.4F);
            }
        }
    }

    /** Vertical band (cylinder section) of the given radius and height around c. */
    private static void ringBand(VertexConsumer vc, Matrix4f m, Vec3 c, float radius, float height, float r, float g, float b, float a,
                                 int segments, boolean haze) {
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2 / segments, a1 = (i + 1) * Math.PI * 2 / segments;
            Vec3 p0 = c.add(Math.cos(a0) * radius, 0, Math.sin(a0) * radius), p1 = c.add(Math.cos(a1) * radius, 0, Math.sin(a1) * radius);
            Vec3 up = new Vec3(0, height / 2, 0);
            if (haze) {
                quad(vc, m, p0.subtract(up), p0, p1, p1.subtract(up), r, g, b, 0, a, a, 0);
                quad(vc, m, p0, p0.add(up), p1.add(up), p1, r, g, b, a, 0, 0, a);
            } else {
                quad(vc, m, p0.subtract(up), p0, p1, p1.subtract(up), r, g, b, 0, a, a, 0);
                quad(vc, m, p0, p0.add(up), p1.add(up), p1, r, g, b, a, 0, 0, a);
            }
        }
    }

    /** A vertical great circle through c, turned by yaw, drawn as a thin camera-facing polyline. */
    private static void meridian(VertexConsumer vc, Matrix4f m, Vec3 c, float radius, float yaw, float width, float[] col, float a, Vec3 cam) {
        int n = 40;
        Vec3 prev = null;
        for (int i = 0; i <= n; i++) {
            double t = i * Math.PI * 2 / n;
            Vec3 p = c.add(Math.cos(yaw) * Math.cos(t) * radius, Math.sin(t) * radius * 0.85, Math.sin(yaw) * Math.cos(t) * radius);
            if (prev != null) ribbon(vc, m, prev, p, cam, width, col[0], col[1], col[2], a, a, false);
            prev = p;
        }
    }

    /** Jagged lightning between two points (camera-facing segments, a bright core). */
    private static void arc(VertexConsumer vc, Matrix4f m, Vec3 from, Vec3 to, Vec3 cam, Random rng, float width, float a, float[] halo) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.05) return;
        int steps = Math.max(3, (int) (len * 2.5));
        Vec3 prev = from;
        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            Vec3 p = from.add(d.scale(t));
            if (i < steps) {
                double j = 0.22 * Math.sin(t * Math.PI) * Math.min(1.5, len * 0.3);
                p = p.add((rng.nextDouble() - 0.5) * 2 * j, (rng.nextDouble() - 0.5) * 2 * j, (rng.nextDouble() - 0.5) * 2 * j);
            }
            ribbon(vc, m, prev, p, cam, width * 3, halo[0], halo[1], halo[2], a * 0.35F, a * 0.35F, false);
            ribbon(vc, m, prev, p, cam, width, WHITE[0], WHITE[1], WHITE[2], a, a, false);
            prev = p;
        }
    }

    /** Camera-facing glowing disc (two crossed quads would show edges; a fan of quads looks round). */
    private static void ball(VertexConsumer vc, Matrix4f m, Vec3 c, float radius, float[] col, float a, Vec3 cam) {
        Vec3 view = c.subtract(cam).normalize();
        Vec3 u = view.cross(new Vec3(0, 1, 0));
        if (u.lengthSqr() < 1.0E-6) u = new Vec3(1, 0, 0);
        u = u.normalize();
        Vec3 v = u.cross(view).normalize();
        int n = 16;
        for (int i = 0; i < n; i++) {
            double a0 = i * Math.PI * 2 / n, a1 = (i + 1) * Math.PI * 2 / n;
            Vec3 p0 = c.add(u.scale(Math.cos(a0) * radius)).add(v.scale(Math.sin(a0) * radius));
            Vec3 p1 = c.add(u.scale(Math.cos(a1) * radius)).add(v.scale(Math.sin(a1) * radius));
            quad(vc, m, c, c, p1, p0, col[0], col[1], col[2], a, a, 0, 0);
        }
    }

    /** Overheat: a column of heat haze rising around the body. */
    private static void ventHaze(VertexConsumer vc, Matrix4f m, Entity e, int until, Vec3 cam, float partial) {
        if (e == null) return;
        float left = (until - now - partial) / 30F;
        float s = Mth.clamp(left, 0, 1) * fx();
        if (s <= 0.01F) return;
        Vec3 base = e.getPosition(partial);
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + (now + partial) * 0.05;
            Vec3 p0 = base.add(Math.cos(a) * 0.45, 0.2, Math.sin(a) * 0.45);
            Vec3 p1 = p0.add(0, 2.2, 0);
            ribbon(vc, m, p0, p1, cam, 0.5F, 1.0F, 0.6F, 0.4F, 0.8F * s, 0.1F * s, true);
        }
    }

    /** Kinetic armor mesh hit: a hexagon rippling out from the hit point, facing the hit. */
    private static void meshRipple(VertexConsumer vc, Matrix4f m, MeshHit h, Vec3 cam, float partial, Minecraft mc) {
        Entity e = mc.level.getEntity(h.entity);
        if (e == null) return;
        float age = now - h.born + partial;
        float a = Mth.clamp(1 - age / 10F, 0, 1) * fx();
        if (a <= 0.01F) return;
        Vec3 n = h.dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : h.dir.normalize();
        Vec3 c = e.getPosition(partial).add(0, e.getBbHeight() * 0.55, 0).add(n.scale(0.5));
        Vec3 t = Math.abs(n.y) < 0.9 ? n.cross(new Vec3(0, 1, 0)).normalize() : n.cross(new Vec3(1, 0, 0)).normalize();
        Vec3 b = n.cross(t).normalize();
        for (int ring = 0; ring < 2; ring++) {
            float r = 0.15F + age * (ring == 0 ? 0.09F : 0.05F);
            Vec3 prev = null;
            for (int i = 0; i <= 6; i++) {
                double ang = i * Math.PI / 3;
                Vec3 p = c.add(t.scale(Math.cos(ang) * r)).add(b.scale(Math.sin(ang) * r));
                if (prev != null) ribbon(vc, m, prev, p, cam, 0.04F, AMBER[0], AMBER[1], AMBER[2], a * (ring == 0 ? 1F : 0.6F), a * (ring == 0 ? 1F : 0.6F), false);
                prev = p;
            }
        }
    }

    /** Recon contact ping: a ring rising and widening around the creature (local player only). */
    private static void ping(VertexConsumer vc, Matrix4f m, Ping p, float partial, Minecraft mc) {
        Entity e = mc.level.getEntity(p.entity);
        if (e == null) return;
        float age = now - p.born + partial;
        if (age < 0) return;
        float a = Mth.clamp(1 - age / 24F, 0, 1) * fx();
        float r = (p.color >> 16 & 255) / 255F, g = (p.color >> 8 & 255) / 255F, b = (p.color & 255) / 255F;
        Vec3 c = e.getPosition(partial).add(0, Math.min(1.2, age * 0.08), 0);
        ringFlat(vc, m, c, e.getBbWidth() * 0.8F + age * 0.04F, 0.06F, r, g, b, 0.8F * a, 28, false);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float r, float g, float b,
                             float a0, float a1, float a2, float a3) {
        vc.addVertex(m, (float) p0.x, (float) p0.y, (float) p0.z).setColor(r, g, b, a0);
        vc.addVertex(m, (float) p1.x, (float) p1.y, (float) p1.z).setColor(r, g, b, a1);
        vc.addVertex(m, (float) p2.x, (float) p2.y, (float) p2.z).setColor(r, g, b, a2);
        vc.addVertex(m, (float) p3.x, (float) p3.y, (float) p3.z).setColor(r, g, b, a3);
    }
}

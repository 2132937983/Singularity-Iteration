// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.block.reactor.mio_icif_Entity_IC_TNT_Primed;
import com.miophas.singularity_iteration.common.entity.dynamite.mio_icif_dynamite_entity;
import com.miophas.singularity_iteration.common.entity.dynamite.mio_icif_sticky_dynamite_entity;
import com.miophas.singularity_iteration.common.entity.mio_icif_Entity_Nuke_Primed;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_rocket_entity;
import com.miophas.singularity_iteration.common.suit.BlastWarningData;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;
import org.jetbrains.annotations.Nullable;

/**
 * What the blast warning unit counts as an explosive (0.1.10.5). Up to 0.1.10.4 only vanilla
 * primed TNT, TNT minecarts and swelling creepers counted, so the SI explosives (industrial TNT,
 * the nuke, dynamite) and the explosives of other mods gave no warning.
 *
 * <p>The rules for an entity, first match of the class wins:
 * <ol>
 *   <li>SI explosives, from their synced fuse: industrial TNT (radius 8, fuse 4 s), the nuke
 *       (radius 40, fuse 30 s), dynamite (3, 2.5 s), sticky dynamite (2, 2.5 s; a remote
 *       charge waits for its detonator, so it has no timer).</li>
 *   <li>Vanilla and every subclass: primed TNT (the TNT of a mod that extends it, with its own
 *       fuse), TNT minecarts, swelling creepers (also mod creepers that extend the creeper).</li>
 *   <li>Explosive projectiles that close in on the wearer: SI rockets (radius 5), ghast fireballs
 *       and wither skulls (2), dragon fireballs (3, acid), and any other projectile whose name has
 *       an explosive word (3). The time is the time to the closest approach; a projectile that
 *       moves away or passes farther than its radius gives no warning.</li>
 *   <li>Other entities (not creatures, items or display entities) whose registry name has an
 *       explosive word ({@link #WORDS}), or that are in the tag {@code mio_icif:blast_warning}.</li>
 * </ol>
 * The data map {@code mio_icif:blast_warning} ({@link BlastWarningData}) sets the radius, the full
 * fuse and the label of any entity type and makes any entity an explosive. The timer of an
 * explosive without a known fuse comes from a public {@code getFuse()} (or {@code getFuseTime},
 * {@code getFuseTicks}, {@code getFuseTimer}) in ticks; else from the data map fuse minus the age
 * of the entity on this client; else it has no timer (the HUD shows "--").
 *
 * <p>Client thread only. The class facts of an entity type are worked out once and kept until
 * the tags or data maps change.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class BlastDetector {
    private BlastDetector() {}

    /** Half size of the wide query box: a big charge (the nuke) warns from 2.5 times its radius. */
    static final double FAR_RANGE = 100;
    /** Explosive words of a registry name (a word of the path, or a part of it for the long ones). */
    static final String[] WORDS = {"tnt", "nuke", "nuclear", "bomb", "dynamite", "grenade", "explosive", "missile", "warhead",
        "landmine", "c4", "primed", "rocket"};
    /** These words count only as a whole word of the path ("c4", "primed", "rocket"). */
    private static final int WHOLE_WORD_FROM = 10;
    private static final String[] FUSE_METHODS = {"getFuse", "getFuseTime", "getFuseTicks", "getFuseTimer"};
    /** Look-ahead of the projectile check, in ticks. */
    private static final int PROJECTILE_TICKS = 60;

    enum Kind { NONE, SI_TNT, SI_NUKE, SI_DYNAMITE, SI_STICKY, TNT, TNT_CART, CREEPER, PROJECTILE, CHARGE }

    /** What is known about an entity type: how to read it, its radius, full fuse (seconds) and label. */
    record Probe(Kind kind, float radius, float fuse, String label, @Nullable Method fuseMethod) {
        static final Probe NONE = new Probe(Kind.NONE, 0, 0, "", null);
    }

    private static final Map<EntityType<?>, Probe> PROBES = new ConcurrentHashMap<>();
    /** The first fuse seen per entity id: the full fuse of a charge of unknown length. */
    private static final Int2IntOpenHashMap FIRST_FUSE = new Int2IntOpenHashMap();
    /** Big charges of the last wide query. */
    private static List<Entity> far = List.of();
    private static int farAge;
    @Nullable private static ClientLevel farLevel;

    /** Forgets the class facts (tags, data maps or the level changed). */
    static void invalidate() {
        PROBES.clear();
        FIRST_FUSE.clear();
        far = List.of();
        farLevel = null;
    }

    @SubscribeEvent
    static void onTags(TagsUpdatedEvent event) { invalidate(); }

    @SubscribeEvent
    static void onDataMaps(DataMapsUpdatedEvent event) { invalidate(); }

    /** The explosives near the player: timed ones soonest first, then the ones without a timer, nearest first. */
    static List<SensorTracker.Blast> scan(ClientLevel level, LocalPlayer player) {
        List<SensorTracker.Blast> out = new ArrayList<>();
        IntOpenHashSet seen = new IntOpenHashSet();
        Vec3 at = player.position();
        List<Entity> found = level.getEntities(player, player.getBoundingBox().inflate(SensorTracker.BLAST_RANGE), e -> probe(e).kind() != Kind.NONE);
        // big charges warn from farther away: a wide query every 10 ticks, the found ones read every tick
        if (level != farLevel || --farAge <= 0) {
            farLevel = level;
            farAge = 10;
            far = level.getEntities(player, player.getBoundingBox().inflate(FAR_RANGE),
                e -> probe(e).kind() != Kind.NONE && probe(e).radius() * 2.5 > SensorTracker.BLAST_RANGE);
        }
        for (Entity e : far) if (!e.isRemoved() && !found.contains(e)) found.add(e);
        for (Entity e : found) {
            Probe p = probe(e);
            double reach = Math.max(SensorTracker.BLAST_RANGE, p.radius() * 2.5);
            if (Math.abs(e.getX() - at.x) > reach || Math.abs(e.getY() - at.y) > reach || Math.abs(e.getZ() - at.z) > reach) continue;
            SensorTracker.Blast b;
            try {
                b = read(e, p, player);
            } catch (RuntimeException ex) {
                b = null;   // a broken entity of a mod is no reason to lose the other warnings
            }
            if (b != null) {
                out.add(b);
                seen.add(e.getId());
            }
        }
        FIRST_FUSE.keySet().retainAll(seen);
        out.sort((a, b) -> {
            if (a.timed() != b.timed()) return a.timed() ? -1 : 1;
            if (a.timed()) return Float.compare(a.seconds(), b.seconds());
            return Double.compare(a.entity().distanceToSqr(player), b.entity().distanceToSqr(player));
        });
        return out;
    }

    /** The class facts of the type of {@code e} (worked out on first sight). */
    static Probe probe(Entity e) {
        Probe p = PROBES.get(e.getType());
        if (p == null) {
            try {
                p = compute(e);
            } catch (RuntimeException ex) {
                p = Probe.NONE;
            }
            PROBES.put(e.getType(), p);
        }
        return p;
    }

    private static Probe compute(Entity e) {
        EntityType<?> type = e.getType();
        ResourceLocation id = EntityType.getKey(type);
        String path = id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
        Probe base = classProbe(e, path);
        BlastWarningData.Entry data = BlastWarningData.entry(type);
        if (data != null) {
            Kind kind = base.kind() != Kind.NONE ? base.kind() : e instanceof Projectile ? Kind.PROJECTILE : Kind.CHARGE;
            String label = data.label().map(BlastDetector::clip).orElse(base.kind() != Kind.NONE ? base.label() : label(path));
            return new Probe(kind, data.radius(), data.fuse() > 0 ? data.fuse() : base.fuse(), label,
                base.fuseMethod() != null ? base.fuseMethod() : fuseMethod(e.getClass()));
        }
        if (base.kind() == Kind.NONE && type.is(BlastWarningData.TAG)) {
            return new Probe(e instanceof Projectile ? Kind.PROJECTILE : Kind.CHARGE, BlastWarningData.TAG_RADIUS, 0, label(path), fuseMethod(e.getClass()));
        }
        return base;
    }

    private static Probe classProbe(Entity e, String path) {
        if (e instanceof mio_icif_Entity_IC_TNT_Primed) return new Probe(Kind.SI_TNT, 8F, 4F, "ITNT", null);
        if (e instanceof mio_icif_Entity_Nuke_Primed) return new Probe(Kind.SI_NUKE, 40F, 30F, "NUKE", null);
        if (e instanceof mio_icif_sticky_dynamite_entity) return new Probe(Kind.SI_STICKY, 2F, 2.5F, "STICKY", null);
        if (e instanceof mio_icif_dynamite_entity) return new Probe(Kind.SI_DYNAMITE, 3F, 2.5F, "DYNAMITE", null);
        if (e instanceof PrimedTnt) return new Probe(Kind.TNT, 4F, 4F, e.getClass() == PrimedTnt.class ? "TNT" : label(path), null);
        if (e instanceof MinecartTNT) return new Probe(Kind.TNT_CART, 4F, 4F, "TNT-CART", null);
        if (e instanceof Creeper) return new Probe(Kind.CREEPER, 3F, 1.5F, "CREEPER", null);
        if (e instanceof mio_icif_rocket_entity) return new Probe(Kind.PROJECTILE, 5F, 0, "ROCKET", null);
        if (e instanceof LargeFireball) return new Probe(Kind.PROJECTILE, 2F, 0, "FIREBALL", null);
        if (e instanceof WitherSkull) return new Probe(Kind.PROJECTILE, 2F, 0, "WITHER", null);
        if (e instanceof DragonFireball) return new Probe(Kind.PROJECTILE, 3F, 0, "ACID", null);
        if (e instanceof FireworkRocketEntity || e instanceof LivingEntity || e instanceof ItemEntity || e instanceof FallingBlockEntity
            || e instanceof ExperienceOrb || e instanceof Display) return Probe.NONE;
        if (!explosiveName(path)) return Probe.NONE;
        if (e instanceof Projectile) return new Probe(Kind.PROJECTILE, 3F, 0, label(path), null);
        boolean big = path.contains("nuke") || path.contains("nuclear") || path.contains("warhead");
        return new Probe(Kind.CHARGE, big ? 24F : 4F, 0, label(path), fuseMethod(e.getClass()));
    }

    /** One warning for {@code e}, or null when it is no danger now (a TNT cart that is not lit, a calm creeper). */
    @Nullable
    private static SensorTracker.Blast read(Entity e, Probe p, LocalPlayer player) {
        return switch (p.kind()) {
            case SI_TNT -> timed(e, p, ((mio_icif_Entity_IC_TNT_Primed) e).getFuse());
            case SI_NUKE -> timed(e, p, ((mio_icif_Entity_Nuke_Primed) e).getFuse());
            case SI_DYNAMITE -> timed(e, p, ((mio_icif_dynamite_entity) e).getFuse());
            case SI_STICKY -> ((mio_icif_sticky_dynamite_entity) e).isRemote()
                ? new SensorTracker.Blast(e, -1F, p.radius(), p.label(), 0F)
                : timed(e, p, ((mio_icif_sticky_dynamite_entity) e).getFuse());
            case TNT -> timed(e, p, ((PrimedTnt) e).getFuse());
            case TNT_CART -> e instanceof MinecartTNT cart && cart.isPrimed() ? timed(e, p, cart.getFuse()) : null;
            case CREEPER -> {
                Creeper creeper = (Creeper) e;
                float swell = creeper.getSwelling(1.0F);
                if (swell <= 0.02F) yield null;
                float left = (1F - swell) * 30F / 20F;
                boolean charged = creeper.isPowered();
                yield new SensorTracker.Blast(e, left, charged ? p.radius() * 2 : p.radius(), charged ? p.label() + "+" : p.label(), p.fuse());
            }
            case PROJECTILE -> approach(e, p, player);
            case CHARGE -> charge(e, p);
            case NONE -> null;
        };
    }

    private static SensorTracker.Blast timed(Entity e, Probe p, int fuseTicks) {
        int first = FIRST_FUSE.containsKey(e.getId()) ? FIRST_FUSE.get(e.getId()) : fuseTicks;
        FIRST_FUSE.put(e.getId(), Math.max(first, fuseTicks));
        float full = Math.max(p.fuse(), Math.max(first, fuseTicks) / 20F);
        return new SensorTracker.Blast(e, Math.max(0, fuseTicks) / 20F, p.radius(), p.label(), full);
    }

    /** A projectile: warns while it closes in and passes within its radius of the wearer. */
    @Nullable
    private static SensorTracker.Blast approach(Entity e, Probe p, LocalPlayer player) {
        Vec3 v = e.getDeltaMovement();
        double speed2 = v.lengthSqr();
        if (speed2 < 0.0025) return null;
        Vec3 rel = player.getBoundingBox().getCenter().subtract(e.position());
        double t = rel.dot(v) / speed2;                     // ticks to the closest approach
        if (t <= 0 || t > PROJECTILE_TICKS) return null;
        double miss = rel.subtract(v.scale(t)).length();
        if (miss > p.radius() + 1.5) return null;
        return new SensorTracker.Blast(e, (float) (t / 20.0), p.radius(), p.label(), 2F);
    }

    /** A charge of another mod: its fuse method, else the data map fuse minus its age, else no timer. */
    private static SensorTracker.Blast charge(Entity e, Probe p) {
        Method m = p.fuseMethod();
        if (m != null) {
            try {
                Object v = m.invoke(e);
                if (v instanceof Number n && n.intValue() >= 0) return timed(e, p, n.intValue());
            } catch (ReflectiveOperationException | RuntimeException ex) {
                PROBES.put(e.getType(), new Probe(p.kind(), p.radius(), p.fuse(), p.label(), null));
            }
        }
        if (p.fuse() > 0) return new SensorTracker.Blast(e, Math.max(0F, p.fuse() - e.tickCount / 20F), p.radius(), p.label(), p.fuse());
        return new SensorTracker.Blast(e, -1F, p.radius(), p.label(), 0F);
    }

    /** A public fuse getter in ticks ({@link #FUSE_METHODS}), or null. */
    @Nullable
    static Method fuseMethod(Class<?> c) {
        for (String name : FUSE_METHODS) {
            try {
                Method m = c.getMethod(name);
                Class<?> r = m.getReturnType();
                if (r != int.class && r != short.class && r != long.class && r != Integer.class) continue;
                try {
                    m.setAccessible(true);   // a public method of a class that is not public
                } catch (RuntimeException ignored) {
                    // invoke() fails then, and the probe drops the method
                }
                return m;
            } catch (NoSuchMethodException | SecurityException ignored) {
                // try the next name
            }
        }
        return null;
    }

    /** True when a registry path has an explosive word. */
    static boolean explosiveName(String path) {
        String[] parts = path.split("[_./-]");
        for (int i = 0; i < WORDS.length; i++) {
            String w = WORDS[i];
            if (i < WHOLE_WORD_FROM && path.contains(w)) return true;
            for (String part : parts) if (part.equals(w)) return true;
        }
        return false;
    }

    /** HUD label from a registry path: "nuke_primed" gives "NUKE", "sticky_bomb" gives "STICKY-BO". */
    static String label(String path) {
        StringBuilder b = new StringBuilder();
        for (String part : path.split("[_./-]")) {
            if (part.isEmpty() || part.equals("entity") || part.equals("primed") || part.equals("projectile") || part.equals("item")) continue;
            if (!b.isEmpty()) b.append('-');
            b.append(part);
        }
        return clip(b.isEmpty() ? path : b.toString());
    }

    private static String clip(String s) {
        String u = s.toUpperCase(Locale.ROOT);
        return u.length() > BlastWarningData.LABEL_LENGTH ? u.substring(0, BlastWarningData.LABEL_LENGTH) : u;
    }
}

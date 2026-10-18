// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Crash guard for Wingmen and the entities they make (0.1.10.5).
 *
 * <p>A Wingman is a mob that wields player weapons, and weapon mods do not always expect that: in
 * 0.1.10.3 a slash effect of SlashBlade: Resharped read a player-only attribute from its Wingman
 * owner and threw in every tick, and the "Ticking entity" crash came back at every load of the
 * world. {@code LevelEntityTickGuardMixin} wraps the tick of every entity in
 * {@code Level#guardEntityTick} and calls {@link #handle} when one throws:
 * <ul>
 *   <li>a Wingman: on the server its suit goes home and it leaves ({@link WingmanEntity#fault});</li>
 *   <li>an entity that a Wingman made (its owner chain leads to a Wingman, or it carries
 *       {@link #OWNED_TAG}): it is removed; an entity of SlashBlade also turns the SlashBlade
 *       bridge off ({@link SlashBladeCombat}), so the Wingman makes no more of them;</li>
 *   <li>any other entity: not handled; the vanilla crash report runs as before.</li>
 * </ul>
 * The first error of a kind (entity type and exception class) is logged with its stack trace;
 * repeats are counted and logged once a minute.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class WingmanTickGuard {
    private WingmanTickGuard() {}

    /** Entity tag of an entity that a Wingman made (set when it enters a level; saved with it). */
    public static final String OWNED_TAG = "mio_icif.wingman_owned";
    private static final int OWNER_DEPTH = 4;
    private static final long REPEAT_MS = 60_000;
    /** Per kind: time of the last log line, errors since. */
    private static final Map<String, long[]> SEEN = new ConcurrentHashMap<>();

    /**
     * Called by the mixin when the tick of {@code ticked} threw. True when the error was handled
     * (the entity is dealt with and the level goes on), false to let the vanilla handling run.
     */
    public static boolean handle(Object ticked, Throwable error) {
        if (!(ticked instanceof Entity entity) || error instanceof OutOfMemoryError) return false;
        boolean wingman = entity instanceof WingmanEntity;
        if (!wingman && !owned(entity)) return false;
        report(entity, error, wingman);
        try {
            if (entity instanceof WingmanEntity w) {
                w.fault(error);
            } else {
                if (isSlashBlade(entity)) SlashBladeCombat.failExternal(error);
                entity.discard();
            }
        } catch (Throwable inner) {
            Singularity_Iteration.LOGGER.error("Wingman tick guard: removing {} failed", entity, inner);
            try {
                if (!entity.isRemoved()) entity.discard();
            } catch (Throwable ignored) {
                // the entity stays; the next error of its tick comes here again
            }
        }
        return true;
    }

    /** True for a Wingman or an entity that a Wingman made. Never throws. */
    public static boolean related(Entity entity) {
        return entity instanceof WingmanEntity || owned(entity);
    }

    /** True when {@code entity} carries {@link #OWNED_TAG} or its owner chain leads to a Wingman. Never throws. */
    static boolean owned(Entity entity) {
        try {
            if (entity.getTags().contains(OWNED_TAG)) return true;
            Entity e = entity;
            for (int i = 0; i < OWNER_DEPTH; i++) {
                Entity next = e instanceof TraceableEntity t ? t.getOwner() : e instanceof OwnableEntity o ? o.getOwner() : null;
                if (next == null || next == e) return false;
                if (next instanceof WingmanEntity) return true;
                e = next;
            }
        } catch (Throwable ignored) {
            // an owner lookup of a broken entity: not ours to judge
        }
        return false;
    }

    private static boolean isSlashBlade(Entity entity) {
        ResourceLocation id = EntityType.getKey(entity.getType());
        return id != null && id.getNamespace().startsWith("slashblade");
    }

    private static void report(Entity entity, Throwable error, boolean wingman) {
        String key = EntityType.getKey(entity.getType()) + "|" + error.getClass().getName();
        long now = System.currentTimeMillis();
        long[] seen = SEEN.computeIfAbsent(key, k -> new long[] {0, 0});
        synchronized (seen) {
            if (seen[0] == 0) {
                seen[0] = now;
                Singularity_Iteration.LOGGER.error("Wingman tick guard: the tick of {} {} threw; {} (the level goes on)",
                    entity.level().isClientSide ? "client" : "server", entity,
                    wingman ? "the suit goes home" : "the entity is removed", error);
                return;
            }
            seen[1]++;
            if (now - seen[0] >= REPEAT_MS) {
                Singularity_Iteration.LOGGER.warn("Wingman tick guard: {} more errors of {} in the last minute", seen[1], key);
                seen[0] = now;
                seen[1] = 0;
            }
        }
    }

    /**
     * Marks the entities a Wingman makes when they enter a level (their owner is known then), so
     * the guard knows them even when the owner is gone.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.isCanceled() || event.getLevel().isClientSide() || !WingmanService.anyTracked()) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof WingmanEntity) && owned(entity)) entity.addTag(OWNED_TAG);
    }
}

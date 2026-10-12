// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Registry of addon quantum suit units (0.1.8.0, public API).
 *
 * <h2>Use</h2>
 * <ol>
 *   <li>Make an item that implements {@link ISuitModuleItem}.</li>
 *   <li>In the mod constructor (or a setup event), call {@link #register} with the spec and the
 *       behavior.</li>
 *   <li>Add the translation keys {@link SuitModuleSpec#nameKey()} and {@code nameKey + ".desc"}.</li>
 * </ol>
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>The id namespace must not be empty; ids without a namespace belong to the built-in units.</li>
 *   <li>A second registration of the same id replaces the first one (last wins).</li>
 *   <li>Registration is open until the server starts; register on both sides (the client uses
 *       the spec for names, colours and the panel).</li>
 *   <li>A piece keeps the id of a unit whose mod was removed; the unit then does nothing and
 *       does not count against the slot capacity.</li>
 * </ul>
 */
public final class SuitModuleRegistry {
    private SuitModuleRegistry() {}

    /** A registered unit. */
    public record Entry(SuitModuleSpec spec, SuitModuleBehavior behavior) { }

    /** Implementation hook of Singularity Iteration (not for addons). */
    public interface Query {
        boolean active(Player player, SuitModuleSpec spec);
    }

    private static final Map<String, Entry> ENTRIES = Collections.synchronizedMap(new LinkedHashMap<>());
    private static volatile Query query = (player, spec) -> false;

    public static void register(SuitModuleSpec spec, SuitModuleBehavior behavior) {
        if (spec.id().getNamespace().isEmpty() || "minecraft".equals(spec.id().getNamespace()))
            throw new IllegalArgumentException("a unit id needs the addon namespace: " + spec.id());
        ENTRIES.put(spec.key(), new Entry(spec, behavior == null ? SuitModuleBehavior.NONE : behavior));
    }

    @Nullable
    public static Entry get(String key) { return ENTRIES.get(key); }

    @Nullable
    public static Entry get(ResourceLocation id) { return ENTRIES.get(id.toString()); }

    /** All registered units, in registration order (a copy). */
    public static Collection<Entry> all() {
        synchronized (ENTRIES) { return java.util.List.copyOf(ENTRIES.values()); }
    }

    public static boolean isEmpty() { return ENTRIES.isEmpty(); }

    /** True when the unit is active on {@code player}: installed, switched on, powered, visor present when needed. */
    public static boolean isActive(Player player, ResourceLocation id) {
        Entry e = get(id);
        return e != null && query.active(player, e.spec());
    }

    /** Called once by Singularity Iteration. */
    public static void setQuery(Query implementation) { query = implementation; }
}

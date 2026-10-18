// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Sortie ledger of the Wingmen (0.1.10.5), saved with the world
 * ({@code data/mio_icif_wingman_sorties.dat} of the overworld).
 *
 * <p>Mob capture tools (soul vials, the mob imprisonment tool, capture balls) save an entity with
 * its equipment and then remove it. The removal of a Wingman sends its suit home, so the saved
 * copy carried a second set of the same items, and releasing it duplicated them. Every deployment
 * now gets a sortie token: the token opens at the deployment and closes when the suit goes home
 * ({@link WingmanEntity#secure()}). A Wingman that enters a level with a closed token, or with the
 * token of a Wingman that is in a level already, is a copy: it is turned away with its items (the
 * real items are in the Armory, or on the first Wingman). See {@link WingmanEntity#admit()}.
 *
 * <p>A token that the ledger does not know (a Wingman saved before 0.1.10.5, or a ledger file that
 * was lost) is adopted as open: an unknown token never costs a player the items of a real Wingman.
 * The ledger keeps the newest {@link #MAX_CLOSED} closed tokens.
 */
public final class WingmanSorties extends SavedData {
    static final String NAME = "mio_icif_wingman_sorties";
    /** Closed tokens kept (16 bytes each; many more sorties than a world sees between capture and release). */
    static final int MAX_CLOSED = 4096;

    /** What the ledger says about the token of a Wingman that enters a level. */
    public enum Status { OPEN, CLOSED, UNKNOWN }

    /** Open sorties: token to the entity id of the Wingman that flies it. */
    private final Map<UUID, UUID> open = new LinkedHashMap<>();
    private final Set<UUID> closed = new LinkedHashSet<>();

    @Nullable
    public static WingmanSorties get(@Nullable MinecraftServer server) {
        if (server == null) return null;
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(WingmanSorties::new, WingmanSorties::load, null), NAME);
    }

    /** Opens a new sortie for the Wingman {@code wingman} and returns its token. */
    UUID open(UUID wingman) {
        UUID token = UUID.randomUUID();
        open.put(token, wingman);
        setDirty();
        return token;
    }

    /** Adopts {@code token} of a Wingman that the ledger does not know (saved by an older version). */
    void adopt(UUID token, UUID wingman) {
        open.put(token, wingman);
        setDirty();
    }

    /** The suit of the sortie went home: a later copy of this Wingman is turned away. */
    void close(@Nullable UUID token) {
        if (token == null) return;
        boolean changed = open.remove(token) != null;
        if (closed.add(token)) {
            changed = true;
            for (Iterator<UUID> it = closed.iterator(); closed.size() > MAX_CLOSED && it.hasNext(); ) {
                it.next();
                it.remove();
            }
        }
        if (changed) setDirty();
    }

    public Status status(UUID token) {
        if (open.containsKey(token)) return Status.OPEN;
        return closed.contains(token) ? Status.CLOSED : Status.UNKNOWN;
    }

    /** The entity id that flies the open sortie {@code token}, or null. */
    @Nullable
    UUID wingman(UUID token) {
        return open.get(token);
    }

    /** The sortie is flown by a different entity now (a capture tool released it with a new id). */
    void rebind(UUID token, UUID wingman) {
        UUID old = open.get(token);
        if (old != null && !old.equals(wingman)) {
            open.put(token, wingman);
            setDirty();
        }
    }

    /** Number of open sorties (for the tests and the log). */
    public int openCount() { return open.size(); }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, UUID> e : open.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("T", e.getKey());
            t.putUUID("W", e.getValue());
            list.add(t);
        }
        tag.put("Open", list);
        long[] bits = new long[closed.size() * 2];
        int i = 0;
        for (UUID token : closed) {
            bits[i++] = token.getMostSignificantBits();
            bits[i++] = token.getLeastSignificantBits();
        }
        tag.put("Closed", new LongArrayTag(bits));
        return tag;
    }

    static WingmanSorties load(CompoundTag tag, HolderLookup.Provider registries) {
        WingmanSorties ledger = new WingmanSorties();
        for (Tag raw : tag.getList("Open", Tag.TAG_COMPOUND)) {
            if (raw instanceof CompoundTag t && t.hasUUID("T") && t.hasUUID("W")) ledger.open.put(t.getUUID("T"), t.getUUID("W"));
        }
        long[] bits = tag.getLongArray("Closed");
        for (int i = 0; i + 1 < bits.length; i += 2) ledger.closed.add(new UUID(bits[i], bits[i + 1]));
        return ledger;
    }
}

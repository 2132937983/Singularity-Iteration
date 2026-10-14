// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Roster of the Wingmen that sit in unloaded chunks (0.1.10.2), saved with the world
 * ({@code data/mio_icif_wingman_roster.dat} of the overworld).
 *
 * <p>A Wingman whose chunk unloads stops; the loaded-entity registry of {@link WingmanService}
 * then forgets it. The roster keeps where it is, so the command wheel can show it ("unloaded")
 * and the quantum recall can reach it: the recall loads its chunk for a moment with a ticket and
 * sends the suit home when the entity appears. An entry is removed when the Wingman loads again
 * or leaves the world for good (recall, return, removal).
 */
public final class WingmanRoster extends SavedData {
    static final String NAME = "mio_icif_wingman_roster";
    /** Entries kept per owner (more than a squad: a stale entry costs nothing but must not grow without end). */
    static final int MAX_PER_OWNER = 8;

    /** One unloaded Wingman: its id, its owner, the dimension, the last position and the Armory suit set. */
    public record Entry(UUID wingman, UUID owner, ResourceKey<Level> dimension, Vec3 pos, int set) { }

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    @Nullable
    public static WingmanRoster get(@Nullable MinecraftServer server) {
        if (server == null) return null;
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(WingmanRoster::new, WingmanRoster::load, null), NAME);
    }

    void put(Entry entry) {
        entries.remove(entry.wingman());
        entries.put(entry.wingman(), entry);
        // keep the newest MAX_PER_OWNER entries of the owner
        int count = 0;
        for (Entry e : entries.values()) if (e.owner().equals(entry.owner())) count++;
        for (Iterator<Entry> it = entries.values().iterator(); it.hasNext() && count > MAX_PER_OWNER; ) {
            if (it.next().owner().equals(entry.owner())) { it.remove(); count--; }
        }
        setDirty();
    }

    void remove(UUID wingman) {
        if (entries.remove(wingman) != null) setDirty();
    }

    /** The unloaded Wingmen of {@code owner}, oldest first (a copy). */
    public List<Entry> of(UUID owner) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries.values()) if (e.owner().equals(owner)) out.add(e);
        return out;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry e : entries.values()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("W", e.wingman());
            t.putUUID("O", e.owner());
            t.putString("D", e.dimension().location().toString());
            t.putDouble("X", e.pos().x);
            t.putDouble("Y", e.pos().y);
            t.putDouble("Z", e.pos().z);
            t.putInt("S", e.set());
            list.add(t);
        }
        tag.put("Wingmen", list);
        return tag;
    }

    static WingmanRoster load(CompoundTag tag, HolderLookup.Provider registries) {
        WingmanRoster roster = new WingmanRoster();
        for (Tag raw : tag.getList("Wingmen", Tag.TAG_COMPOUND)) {
            if (!(raw instanceof CompoundTag t) || !t.hasUUID("W") || !t.hasUUID("O")) continue;
            ResourceLocation dim = ResourceLocation.tryParse(t.getString("D"));
            if (dim == null) continue;
            Entry e = new Entry(t.getUUID("W"), t.getUUID("O"), ResourceKey.create(Registries.DIMENSION, dim),
                new Vec3(t.getDouble("X"), t.getDouble("Y"), t.getDouble("Z")), t.getInt("S"));
            roster.entries.put(e.wingman(), e);
        }
        return roster;
    }
}

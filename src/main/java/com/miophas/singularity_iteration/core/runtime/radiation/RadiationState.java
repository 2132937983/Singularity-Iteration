// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.radiation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** DataStorage owns the lifetime: no process-global map can leak into another save. */
public final class RadiationState extends SavedData {
    public record Zone(BlockPos center, int radius, float power, long created) { }
    public static final long DURATION = 24L * 60 * 60 * 1000;
    public final Map<UUID, Zone> zones = new LinkedHashMap<>();
    public final Map<Long, Long> blocks = new LinkedHashMap<>();
    public static RadiationState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(RadiationState::new, RadiationState::load), "scex_radiation");
    }
    public static RadiationState load(CompoundTag tag, HolderLookup.Provider registries) {
        var state = new RadiationState();
        for (Tag value : tag.getList("zones", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) value;
            if (!row.hasUUID("id") || !Float.isFinite(row.getFloat("power"))) continue;
            state.zones.put(row.getUUID("id"), new Zone(BlockPos.of(row.getLong("pos")),
                Math.clamp(row.getInt("radius"), 0, 2000), Math.max(0, row.getFloat("power")), row.getLong("created")));
        }
        for (Tag value : tag.getList("blocks", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) value;
            state.blocks.put(row.getLong("pos"), row.getLong("created"));
        }
        return state;
    }
    public static boolean expired(long created, long now) { return now >= created && now - created >= DURATION; }
    public void clean(long now) {
        // Once per level, never once per dimension over every world's records.
        boolean changed = zones.values().removeIf(zone -> expired(zone.created(), now));
        changed |= blocks.values().removeIf(created -> expired(created, now));
        if (changed) setDirty();
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var zoneList = new ListTag();
        zones.forEach((id, zone) -> {
            var row = new CompoundTag(); row.putUUID("id", id); row.putLong("pos", zone.center().asLong());
            row.putInt("radius", zone.radius()); row.putFloat("power", zone.power()); row.putLong("created", zone.created()); zoneList.add(row);
        });
        tag.put("zones", zoneList);
        var blockList = new ListTag();
        blocks.forEach((pos, created) -> { var row = new CompoundTag(); row.putLong("pos", pos); row.putLong("created", created); blockList.add(row); });
        tag.put("blocks", blockList);
        return tag;
    }
}

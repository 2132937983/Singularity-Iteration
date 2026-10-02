package com.miophas.singularity_iteration.common.blockentity.producer.tower;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Combat statistics of one tower: totals, per-target-type breakdown and a short log
 * of recent targets. Updated once per hit (cheap map increments); {@link #version()}
 * lets an open GUI resend only when something changed.
 */
@SuppressWarnings("null")
public final class LaserTowerStats {
    public static final int MAX_TYPES = 24;
    public static final int RECENT = 8;
    public static final String OTHER = "other";
    public static final String PLAYER = "player";

    private long energyUsed, volleys, hits, kills;
    private double damage;
    private final Object2LongOpenHashMap<String> hitsByType = new Object2LongOpenHashMap<>();
    private final Object2LongOpenHashMap<String> killsByType = new Object2LongOpenHashMap<>();
    private final List<Recent> recent = new ArrayList<>(RECENT);
    private int version;

    /** One recent engagement: target type, its display name, damage dealt and whether it died. */
    public record Recent(String type, String name, float damage, boolean killed, long gameTime) { }

    public long energyUsed() { return energyUsed; }
    public long volleys() { return volleys; }
    public long hits() { return hits; }
    public long kills() { return kills; }
    public double damage() { return damage; }
    public Object2LongMap<String> hitsByType() { return hitsByType; }
    public Object2LongMap<String> killsByType() { return killsByType; }
    public List<Recent> recent() { return recent; }
    public int version() { return version; }

    public static String typeKey(LivingEntity entity) {
        return entity instanceof Player ? PLAYER : BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    public void recordVolley() { volleys++; version++; }

    public void recordHit(LivingEntity target, float dealt, boolean killed, long cost, long gameTime) {
        String key = typeKey(target);
        energyUsed += cost;
        hits++;
        damage += dealt;
        bump(hitsByType, key);
        if (killed) { kills++; bump(killsByType, key); }
        if (recent.size() == RECENT) recent.removeFirst();
        recent.add(new Recent(key, target.getName().getString(), dealt, killed, gameTime));
        version++;
    }

    private static void bump(Object2LongOpenHashMap<String> map, String key) {
        if (!map.containsKey(key) && map.size() >= MAX_TYPES) key = OTHER;
        map.addTo(key, 1L);
    }

    public void reset() {
        energyUsed = volleys = hits = kills = 0;
        damage = 0;
        hitsByType.clear(); killsByType.clear(); recent.clear();
        version++;
    }

    // ------------------------------------------------------------------ persistence / sync
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Energy", energyUsed);
        tag.putLong("Volleys", volleys);
        tag.putLong("Hits", hits);
        tag.putLong("Kills", kills);
        tag.putDouble("Damage", damage);
        tag.put("HitsByType", saveMap(hitsByType));
        tag.put("KillsByType", saveMap(killsByType));
        ListTag list = new ListTag();
        for (Recent r : recent) {
            CompoundTag e = new CompoundTag();
            e.putString("Type", r.type()); e.putString("Name", r.name());
            e.putFloat("Damage", r.damage()); e.putBoolean("Killed", r.killed()); e.putLong("Time", r.gameTime());
            list.add(e);
        }
        tag.put("Recent", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        energyUsed = tag.getLong("Energy");
        volleys = tag.getLong("Volleys");
        hits = tag.getLong("Hits");
        kills = tag.getLong("Kills");
        damage = tag.getDouble("Damage");
        loadMap(hitsByType, tag.getCompound("HitsByType"));
        loadMap(killsByType, tag.getCompound("KillsByType"));
        recent.clear();
        ListTag list = tag.getList("Recent", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - RECENT); i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            recent.add(new Recent(e.getString("Type"), e.getString("Name"), e.getFloat("Damage"), e.getBoolean("Killed"), e.getLong("Time")));
        }
        version++;
    }

    private static CompoundTag saveMap(Object2LongOpenHashMap<String> map) {
        CompoundTag tag = new CompoundTag();
        for (Object2LongMap.Entry<String> e : map.object2LongEntrySet()) tag.putLong(e.getKey(), e.getLongValue());
        return tag;
    }

    private static void loadMap(Object2LongOpenHashMap<String> map, CompoundTag tag) {
        map.clear();
        for (String key : tag.getAllKeys()) {
            if (map.size() >= MAX_TYPES) break;
            map.put(key, tag.getLong(key));
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarLong(energyUsed); buf.writeVarLong(volleys); buf.writeVarLong(hits); buf.writeVarLong(kills);
        buf.writeDouble(damage);
        writeMap(buf, hitsByType); writeMap(buf, killsByType);
        buf.writeVarInt(recent.size());
        for (Recent r : recent) {
            buf.writeUtf(r.type(), 128); buf.writeUtf(r.name(), 64);
            buf.writeFloat(r.damage()); buf.writeBoolean(r.killed()); buf.writeVarLong(r.gameTime());
        }
    }

    public static LaserTowerStats read(FriendlyByteBuf buf) {
        LaserTowerStats s = new LaserTowerStats();
        s.energyUsed = buf.readVarLong(); s.volleys = buf.readVarLong(); s.hits = buf.readVarLong(); s.kills = buf.readVarLong();
        s.damage = buf.readDouble();
        readMap(buf, s.hitsByType); readMap(buf, s.killsByType);
        int n = Math.min(buf.readVarInt(), RECENT);
        for (int i = 0; i < n; i++) {
            s.recent.add(new Recent(buf.readUtf(128), buf.readUtf(64), buf.readFloat(), buf.readBoolean(), buf.readVarLong()));
        }
        return s;
    }

    private static void writeMap(FriendlyByteBuf buf, Object2LongOpenHashMap<String> map) {
        buf.writeVarInt(map.size());
        for (Object2LongMap.Entry<String> e : map.object2LongEntrySet()) { buf.writeUtf(e.getKey(), 128); buf.writeVarLong(e.getLongValue()); }
    }

    private static void readMap(FriendlyByteBuf buf, Object2LongOpenHashMap<String> map) {
        int n = Math.min(buf.readVarInt(), MAX_TYPES + 1);
        for (int i = 0; i < n; i++) map.put(buf.readUtf(128), buf.readVarLong());
    }
}

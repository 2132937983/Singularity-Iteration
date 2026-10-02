package com.miophas.singularity_iteration.common.blockentity.producer.tower;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Tower target list. Entries are entity type ids ({@code minecraft:creeper}), entity
 * type tags ({@code #minecraft:raiders}) or player names ({@code Steve}).
 *
 * <ul>
 *   <li><b>Blacklist</b> (default): every hostile mob ({@link Enemy}) except the listed ones.</li>
 *   <li><b>Whitelist</b>: only the listed targets, which may include passive mobs and players.
 *       Players are never attacked unless named here, and never in creative/spectator.</li>
 * </ul>
 * Matching uses sets rebuilt only when the list changes, so the per-candidate cost is O(1).
 */
@SuppressWarnings("null")
public final class TargetFilter {
    public enum Mode { BLACKLIST, WHITELIST }
    public static final int MAX_ENTRIES = 32;
    public static final int MAX_LENGTH = 96;
    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private Mode mode = Mode.BLACKLIST;
    private final List<String> entries = new ArrayList<>();
    private final Set<EntityType<?>> types = new HashSet<>();
    private final Set<String> players = new HashSet<>();
    private final List<TagKey<EntityType<?>>> tags = new ArrayList<>();

    public Mode mode() { return mode; }
    public List<String> entries() { return Collections.unmodifiableList(entries); }

    public void setMode(Mode mode) { this.mode = mode; }
    public void toggleMode() { mode = mode == Mode.BLACKLIST ? Mode.WHITELIST : Mode.BLACKLIST; }

    /** Normalised entry or {@code null} if it is not a valid id, tag or player name. */
    public static String normalise(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty() || s.length() > MAX_LENGTH) return null;
        if (s.startsWith("#")) {
            ResourceLocation id = ResourceLocation.tryParse(s.substring(1).toLowerCase(Locale.ROOT));
            return id == null ? null : "#" + id;
        }
        if (s.indexOf(':') >= 0) {
            ResourceLocation id = ResourceLocation.tryParse(s.toLowerCase(Locale.ROOT));
            return id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id) ? id.toString() : null;
        }
        if (PLAYER_NAME.matcher(s).matches()) {
            // A bare word that is a vanilla entity name ("creeper") means the entity type.
            ResourceLocation vanilla = ResourceLocation.withDefaultNamespace(s.toLowerCase(Locale.ROOT));
            return BuiltInRegistries.ENTITY_TYPE.containsKey(vanilla) ? vanilla.toString() : s;
        }
        return null;
    }

    /** Adds a validated entry; false if invalid, duplicate or the list is full. */
    public boolean add(String raw) {
        String entry = normalise(raw);
        if (entry == null || entries.size() >= MAX_ENTRIES) return false;
        for (String e : entries) if (e.equalsIgnoreCase(entry)) return false;
        entries.add(entry);
        rebuild();
        return true;
    }

    public boolean remove(int index) {
        if (index < 0 || index >= entries.size()) return false;
        entries.remove(index);
        rebuild();
        return true;
    }

    private void rebuild() {
        types.clear(); players.clear(); tags.clear();
        for (String e : entries) {
            if (e.startsWith("#")) {
                ResourceLocation id = ResourceLocation.tryParse(e.substring(1));
                if (id != null) tags.add(TagKey.create(Registries.ENTITY_TYPE, id));
            } else if (e.indexOf(':') >= 0) {
                ResourceLocation id = ResourceLocation.tryParse(e);
                if (id != null) BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(types::add);
            } else {
                players.add(e.toLowerCase(Locale.ROOT));
            }
        }
    }

    public boolean matches(LivingEntity entity) {
        if (entity instanceof Player player) return players.contains(player.getGameProfile().getName().toLowerCase(Locale.ROOT));
        EntityType<?> type = entity.getType();
        if (types.contains(type)) return true;
        for (TagKey<EntityType<?>> tag : tags) if (type.is(tag)) return true;
        return false;
    }

    /** Whether the tower may shoot this (already alive and in range) entity. */
    public boolean allows(LivingEntity entity) {
        if (entity instanceof Player player) {
            return mode == Mode.WHITELIST && !player.isCreative() && !player.isSpectator() && matches(player);
        }
        return mode == Mode.BLACKLIST ? entity instanceof Enemy && !matches(entity) : matches(entity);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Mode", mode.name());
        ListTag list = new ListTag();
        for (String e : entries) list.add(StringTag.valueOf(e));
        tag.put("Entries", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        mode = "WHITELIST".equals(tag.getString("Mode")) ? Mode.WHITELIST : Mode.BLACKLIST;
        entries.clear();
        ListTag list = tag.getList("Entries", Tag.TAG_STRING);
        for (int i = 0; i < list.size() && entries.size() < MAX_ENTRIES; i++) {
            String e = normalise(list.getString(i));
            if (e != null && !entries.contains(e)) entries.add(e);
        }
        rebuild();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(mode == Mode.WHITELIST);
        buf.writeVarInt(entries.size());
        for (String e : entries) buf.writeUtf(e, MAX_LENGTH);
    }

    public static TargetFilter read(FriendlyByteBuf buf) {
        TargetFilter f = new TargetFilter();
        f.mode = buf.readBoolean() ? Mode.WHITELIST : Mode.BLACKLIST;
        int n = Math.min(buf.readVarInt(), MAX_ENTRIES);
        for (int i = 0; i < n; i++) f.entries.add(buf.readUtf(MAX_LENGTH));
        return f;
    }
}

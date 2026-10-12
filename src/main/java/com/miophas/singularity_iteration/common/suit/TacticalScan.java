// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 3D tactical structure scanner, server side (0.1.8.0).
 *
 * <h2>Specification</h2>
 * <ul>
 *   <li>Request: the client sends {@link Request} (rate limit {@code PacketRateLimit.TACTICAL_SCAN}).
 *       The server answers only when the holomap unit is active.</li>
 *   <li>Area: the loaded chunks within {@link #CHUNK_RADIUS} chunks of the player. The scan never
 *       loads a chunk.</li>
 *   <li>Structures: every structure start that a scanned chunk starts or references (at most
 *       {@link #MAX_STRUCTURES}), with its bounding box and the boxes of its pieces (at most
 *       {@link #MAX_PIECES}).</li>
 *   <li>Loot: each container in a structure that still has a loot table (not opened yet), and the
 *       items of that loot table with the chance of each item per pool roll (weight / total
 *       weight of its pool; nested tables, groups and item tags are followed). The item lists
 *       are cached per loot table and the cache is cleared on a data pack reload.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class TacticalScan {
    private TacticalScan() {}

    public static final int CHUNK_RADIUS = 8, MAX_STRUCTURES = 32, MAX_PIECES = 64, MAX_CHESTS = 48, MAX_TABLES = 24, MAX_ITEMS = 40;

    public record Box(int x0, int y0, int z0, int x1, int y1, int z1) {
        static Box of(BoundingBox b) { return new Box(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()); }
        void write(RegistryFriendlyByteBuf buf) { buf.writeVarInt(x0); buf.writeVarInt(y0); buf.writeVarInt(z0); buf.writeVarInt(x1); buf.writeVarInt(y1); buf.writeVarInt(z1); }
        static Box read(RegistryFriendlyByteBuf buf) { return new Box(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()); }
    }
    public record Chest(BlockPos pos, String table) { }
    public record Site(String id, Box box, List<Box> pieces, List<Chest> chests) { }
    public record LootItem(String item, int percent) { }

    // ------------------------------------------------------------------ scan
    static void answer(ServerPlayer player) {
        if (!SuitModules.isActive(player, SuitModuleType.HOLOMAP)) return;
        ServerLevel level = player.serverLevel();
        ChunkPos center = player.chunkPosition();
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Map<String, Site> sites = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS && sites.size() < MAX_STRUCTURES; dx++) {
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS && sites.size() < MAX_STRUCTURES; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(center.x + dx, center.z + dz);
                if (chunk == null) continue;
                List<StructureStart> starts = new ArrayList<>(chunk.getAllStarts().values());
                for (Map.Entry<Structure, LongSet> ref : chunk.getAllReferences().entrySet()) {
                    for (long packed : ref.getValue()) {
                        ChunkPos at = new ChunkPos(packed);
                        String key = registry.getKey(ref.getKey()) + "@" + packed;
                        if (!seen.add(key)) continue;
                        ChunkAccess startChunk = level.getChunk(at.x, at.z, ChunkStatus.STRUCTURE_STARTS, false);
                        if (startChunk == null) continue;
                        StructureStart start = startChunk.getStartForStructure(ref.getKey());
                        if (start != null) starts.add(start);
                    }
                }
                for (StructureStart start : starts) {
                    if (start == null || !start.isValid() || sites.size() >= MAX_STRUCTURES) continue;
                    ResourceLocation id = registry.getKey(start.getStructure());
                    String key = id + "@" + start.getChunkPos().toLong();
                    if (sites.containsKey(key)) continue;
                    BoundingBox box = start.getBoundingBox();
                    List<Box> pieces = new ArrayList<>();
                    for (var piece : start.getPieces()) {
                        if (pieces.size() >= MAX_PIECES) break;
                        pieces.add(Box.of(piece.getBoundingBox()));
                    }
                    sites.put(key, new Site(id == null ? "?" : id.toString(), Box.of(box), pieces, chests(level, box)));
                }
            }
        }
        Map<String, List<LootItem>> tables = new LinkedHashMap<>();
        for (Site s : sites.values()) {
            for (Chest c : s.chests()) {
                if (tables.size() >= MAX_TABLES) break;
                tables.computeIfAbsent(c.table(), t -> loot(level, t));
            }
        }
        try {
            if (player.connection != null && player.connection.hasChannel(Result.TYPE)) {
                PacketDistributor.sendToPlayer(player, new Result(new ArrayList<>(sites.values()), tables));
            }
        } catch (RuntimeException ignored) {
            // fake connections have no client
        }
    }

    /** Unopened containers with a loot table inside {@code box} (loaded chunks only). */
    private static List<Chest> chests(ServerLevel level, BoundingBox box) {
        List<Chest> out = new ArrayList<>();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (var be : chunk.getBlockEntities().values()) {
                    if (out.size() >= MAX_CHESTS) return out;
                    if (be instanceof RandomizableContainer rc && rc.getLootTable() != null && box.isInside(be.getBlockPos())) {
                        out.add(new Chest(be.getBlockPos(), rc.getLootTable().location().toString()));
                    }
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ loot tables
    private static final Map<String, List<LootItem>> CACHE = new HashMap<>();

    static List<LootItem> loot(ServerLevel level, String table) {
        return CACHE.computeIfAbsent(table, t -> {
            Map<String, Double> share = new LinkedHashMap<>();
            collectTable(level, ResourceLocation.tryParse(t), 1.0, share, new HashSet<>(), 0);
            List<LootItem> out = new ArrayList<>();
            share.entrySet().stream().sorted((a, b) -> Double.compare(b.getValue(), a.getValue())).limit(MAX_ITEMS)
                .forEach(e -> out.add(new LootItem(e.getKey(), (int) Math.max(1, Math.round(e.getValue() * 100)))));
            return out;
        });
    }

    private static void collectTable(ServerLevel level, ResourceLocation id, double scale, Map<String, Double> share, Set<ResourceLocation> visited, int depth) {
        if (id == null || depth > 4 || !visited.add(id)) return;
        LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
        if (table == LootTable.EMPTY) return;
        var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
        JsonElement json = LootTable.DIRECT_CODEC.encodeStart(ops, table).result().orElse(null);
        if (json != null && json.isJsonObject()) collectPools(level, json.getAsJsonObject(), scale, share, visited, depth);
    }

    private static void collectPools(ServerLevel level, JsonObject table, double scale, Map<String, Double> share, Set<ResourceLocation> visited, int depth) {
        if (!table.has("pools")) return;
        for (JsonElement poolEl : table.getAsJsonArray("pools")) {
            if (!poolEl.isJsonObject() || !poolEl.getAsJsonObject().has("entries")) continue;
            JsonArray entries = poolEl.getAsJsonObject().getAsJsonArray("entries");
            double total = 0;
            for (JsonElement e : entries) total += weight(e);
            if (total <= 0) continue;
            for (JsonElement e : entries) collectEntry(level, e, scale * weight(e) / total, share, visited, depth);
        }
    }

    private static double weight(JsonElement e) {
        return e.isJsonObject() && e.getAsJsonObject().has("weight") ? Math.max(0, e.getAsJsonObject().get("weight").getAsDouble()) : 1;
    }

    private static void collectEntry(ServerLevel level, JsonElement el, double chance, Map<String, Double> share, Set<ResourceLocation> visited, int depth) {
        if (!el.isJsonObject()) return;
        JsonObject e = el.getAsJsonObject();
        String type = e.has("type") ? e.get("type").getAsString() : "";
        switch (type.replace("minecraft:", "")) {
            case "item" -> { if (e.has("name")) share.merge(e.get("name").getAsString(), chance, Math::max); }
            case "tag" -> {
                if (!e.has("name")) return;
                var tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(e.get("name").getAsString()));
                int n = 0;
                for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                    if (n++ >= 8) break;
                    share.merge(BuiltInRegistries.ITEM.getKey(holder.value()).toString(), chance, Math::max);
                }
            }
            case "loot_table" -> {
                JsonElement value = e.get("value");
                if (value == null) return;
                if (value.isJsonPrimitive()) collectTable(level, ResourceLocation.tryParse(value.getAsString()), chance, share, visited, depth + 1);
                else if (value.isJsonObject()) collectPools(level, value.getAsJsonObject(), chance, share, visited, depth + 1);
            }
            case "alternatives", "group", "sequence" -> {
                if (!e.has("children")) return;
                for (JsonElement c : e.getAsJsonArray("children")) collectEntry(level, c, chance, share, visited, depth);
            }
            default -> { }
        }
    }

    @SubscribeEvent
    static void onTags(TagsUpdatedEvent event) { CACHE.clear(); }

    @SubscribeEvent
    static void onStopped(ServerStoppedEvent event) { CACHE.clear(); }

    // ------------------------------------------------------------------ packets
    /** Client to server: scan request (no data). */
    public record Request() implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "tactical_scan_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.unit(new Request());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * Server to client: the scan result. Limits on both sides: {@link #MAX_STRUCTURES} sites,
     * {@link #MAX_PIECES} pieces and {@link #MAX_CHESTS} containers per site, {@link #MAX_TABLES}
     * loot tables with {@link #MAX_ITEMS} items each.
     */
    public record Result(List<Site> sites, Map<String, List<LootItem>> tables) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "tactical_scan_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> CODEC = StreamCodec.of((buf, p) -> {
            int ns = Math.min(MAX_STRUCTURES, p.sites.size());
            buf.writeVarInt(ns);
            for (int i = 0; i < ns; i++) {
                Site s = p.sites.get(i);
                buf.writeUtf(s.id(), 256);
                s.box().write(buf);
                int np = Math.min(MAX_PIECES, s.pieces().size());
                buf.writeVarInt(np);
                for (int j = 0; j < np; j++) s.pieces().get(j).write(buf);
                int nc = Math.min(MAX_CHESTS, s.chests().size());
                buf.writeVarInt(nc);
                for (int j = 0; j < nc; j++) { buf.writeBlockPos(s.chests().get(j).pos()); buf.writeUtf(s.chests().get(j).table(), 256); }
            }
            List<Map.Entry<String, List<LootItem>>> tables = new ArrayList<>(p.tables.entrySet());
            int nt = Math.min(MAX_TABLES, tables.size());
            buf.writeVarInt(nt);
            for (int i = 0; i < nt; i++) {
                buf.writeUtf(tables.get(i).getKey(), 256);
                List<LootItem> items = tables.get(i).getValue();
                int ni = Math.min(MAX_ITEMS, items.size());
                buf.writeVarInt(ni);
                for (int j = 0; j < ni; j++) { buf.writeUtf(items.get(j).item(), 256); buf.writeVarInt(items.get(j).percent()); }
            }
        }, buf -> {
            int ns = count(buf, MAX_STRUCTURES);
            List<Site> sites = new ArrayList<>(ns);
            for (int i = 0; i < ns; i++) {
                String id = buf.readUtf(256);
                Box box = Box.read(buf);
                int np = count(buf, MAX_PIECES);
                List<Box> pieces = new ArrayList<>(np);
                for (int j = 0; j < np; j++) pieces.add(Box.read(buf));
                int nc = count(buf, MAX_CHESTS);
                List<Chest> chests = new ArrayList<>(nc);
                for (int j = 0; j < nc; j++) chests.add(new Chest(buf.readBlockPos(), buf.readUtf(256)));
                sites.add(new Site(id, box, pieces, chests));
            }
            int nt = count(buf, MAX_TABLES);
            Map<String, List<LootItem>> tables = new LinkedHashMap<>();
            for (int i = 0; i < nt; i++) {
                String t = buf.readUtf(256);
                int ni = count(buf, MAX_ITEMS);
                List<LootItem> items = new ArrayList<>(ni);
                for (int j = 0; j < ni; j++) items.add(new LootItem(buf.readUtf(256), buf.readVarInt()));
                tables.put(t, items);
            }
            return new Result(sites, tables);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        private static int count(RegistryFriendlyByteBuf buf, int max) {
            int n = buf.readVarInt();
            if (n < 0 || n > max) throw new io.netty.handler.codec.DecoderException("tactical scan: count " + n + " > " + max);
            return n;
        }
    }

    /** Client side: the last result and the hook of the map screen (installed by the client setup). */
    public static volatile Result LAST;
    public static volatile Runnable CLIENT_HOOK;
}

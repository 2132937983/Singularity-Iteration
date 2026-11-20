// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/**
 * 0.1.11.107 (processing overhaul M0): material properties of items (0.1.11.108: also on clients, by {@link Sync}), from data packs
 * ({@code data/<namespace>/si_materials/*.json}). The machines read what a process needs from here instead of a fixed
 * time per recipe. For now one property:
 * <ul>
 *   <li><b>work_index</b>: the Bond work index (kWh/t), how hard a material is to grind. The energy of a comminution
 *       step is proportional to it (Bond's law, W = 10 Wi (1/√P80 − 1/√F80)).</li>
 * </ul>
 * File format: {@code {"entries": [{"item": "minecraft:glass", "work_index": 3.1}, {"tag": "c:ores/iron", "work_index": 12.7}]}}.
 * An item entry wins over a tag entry; among tag entries the file loaded last wins (packs override the mod's file).
 * Unknown keys are warned about and ignored.
 */
public final class SiMaterials extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "si_materials";
    /** Work index used when no entry matches (hematite, the iron ore of the old fixed 600 EU macerator step). */
    public static final double DEFAULT_WORK_INDEX = 12.7;
    private static final Gson GSON = new GsonBuilder().create();

    private record TagEntry(TagKey<Item> tag, double workIndex) {}

    /**
     * 0.1.11.108: one entry as read from a file, also what the server sends to clients (JEI and the screens show the
     * material's energy). {@code tag} true: {@code id} is an item tag; false: an item.
     */
    public record RawEntry(boolean tag, String id, double workIndex) {}

    private static volatile List<RawEntry> raw = List.of();

    private static volatile Map<Item, Double> itemWorkIndex = Map.of();
    private static volatile List<TagEntry> tagWorkIndex = List.of();
    /** Item → resolved work index (NaN: no entry). Cleared on reload and when tags change. */
    private static final Map<Item, Double> CACHE = new ConcurrentHashMap<>();
    private static volatile int generation;

    public SiMaterials() { super(GSON, DIRECTORY); }

    /** Number of completed reloads (diagnostics and tests). */
    public static int generation() { return generation; }

    /** The Bond work index (kWh/t) of an item, if a data pack gives one. */
    public static OptionalDouble workIndex(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return OptionalDouble.empty();
        double v = CACHE.computeIfAbsent(stack.getItem(), item -> resolve(new ItemStack(item)));
        return Double.isNaN(v) ? OptionalDouble.empty() : OptionalDouble.of(v);
    }

    /** The work index of an item, or {@link #DEFAULT_WORK_INDEX}. */
    public static double workIndexOrDefault(ItemStack stack) {
        return workIndex(stack).orElse(DEFAULT_WORK_INDEX);
    }

    private static double resolve(ItemStack stack) {
        Double direct = itemWorkIndex.get(stack.getItem());
        if (direct != null) return direct;
        List<TagEntry> tags = tagWorkIndex;
        for (int i = tags.size() - 1; i >= 0; i--) if (stack.is(tags.get(i).tag())) return tags.get(i).workIndex();
        return Double.NaN;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        List<RawEntry> entries = new ArrayList<>();
        // a stable order: by file id, so a pack's file named after the mod's replaces nothing by accident
        var ids = new ArrayList<>(files.keySet());
        ids.sort(Comparator.comparing(ResourceLocation::toString));
        for (ResourceLocation id : ids) {
            JsonElement root = files.get(id);
            if (!root.isJsonObject() || !root.getAsJsonObject().has("entries") || !root.getAsJsonObject().get("entries").isJsonArray()) {
                Singularity_Iteration.LOGGER.warn("[Materials] {}: needs an \"entries\" array; ignored", id);
                continue;
            }
            for (JsonElement e : root.getAsJsonObject().getAsJsonArray("entries")) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                for (String key : o.keySet())
                    if (!key.equals("item") && !key.equals("tag") && !key.equals("work_index") && !key.equals("_comment"))
                        Singularity_Iteration.LOGGER.warn("[Materials] {}: unknown key {}; ignored", id, key);
                if (!o.has("work_index")) continue;
                double wi;
                try { wi = o.get("work_index").getAsDouble(); } catch (RuntimeException ex) { wi = Double.NaN; }
                if (!(wi > 0) || !Double.isFinite(wi)) {
                    Singularity_Iteration.LOGGER.warn("[Materials] {}: work_index must be a positive number; entry ignored", id);
                    continue;
                }
                try {
                    if (o.has("item")) entries.add(new RawEntry(false, ResourceLocation.parse(o.get("item").getAsString()).toString(), wi));
                    else if (o.has("tag")) entries.add(new RawEntry(true, ResourceLocation.parse(o.get("tag").getAsString()).toString(), wi));
                } catch (RuntimeException ex) {
                    Singularity_Iteration.LOGGER.warn("[Materials] {}: bad entry {}: {}", id, o, ex.getMessage());
                }
            }
        }
        install(entries);
    }

    /** The entries in force (what the server sends to clients). */
    public static List<RawEntry> entries() { return raw; }

    /** Puts a list of entries in force: the server after a reload, a remote client from the server's sync. */
    public static void install(List<RawEntry> entries) {
        Map<Item, Double> items = new java.util.HashMap<>();
        List<TagEntry> tags = new ArrayList<>();
        for (RawEntry e : entries) {
            if (!(e.workIndex() > 0) || !Double.isFinite(e.workIndex())) continue;
            ResourceLocation rl = ResourceLocation.tryParse(e.id());
            if (rl == null) continue;
            if (e.tag()) tags.add(new TagEntry(TagKey.create(Registries.ITEM, rl), e.workIndex()));
            else BuiltInRegistries.ITEM.getOptional(rl).ifPresent(item -> items.put(item, e.workIndex()));
        }
        raw = List.copyOf(entries);
        itemWorkIndex = Map.copyOf(items);
        tagWorkIndex = List.copyOf(tags);
        CACHE.clear();
        generation++;
    }

    /** 0.1.11.108: the material table for a client (sent on login and after /reload). */
    public record Sync(List<RawEntry> entries) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "si_materials"));
        private static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, RawEntry> ENTRY = net.minecraft.network.codec.StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.BOOL, RawEntry::tag,
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, RawEntry::id,
            net.minecraft.network.codec.ByteBufCodecs.DOUBLE, RawEntry::workIndex,
            RawEntry::new);
        public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, Sync> CODEC = net.minecraft.network.codec.StreamCodec.composite(
            ENTRY.apply(net.minecraft.network.codec.ByteBufCodecs.list(65536)), Sync::entries, Sync::new);

        @Override
        public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() { return TYPE; }

        public static void handle(Sync packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {
            // the integrated server already holds the same table
            context.enqueueWork(() -> { if (!context.connection().isMemoryConnection()) install(packet.entries); });
        }

        /** Only to connections that negotiated the channel (vanilla clients, fake and test players are skipped). */
        public static void send(net.minecraft.server.level.ServerPlayer player) {
            if (player.connection == null || !player.connection.hasChannel(TYPE)) return;
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new Sync(raw));
        }
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void onReloadListeners(AddReloadListenerEvent event) { event.addListener(new SiMaterials()); }

        @SubscribeEvent
        public static void onTagsUpdated(TagsUpdatedEvent event) { CACHE.clear(); }

        @SubscribeEvent
        public static void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
            event.getRelevantPlayers().forEach(Sync::send);
        }
    }
}

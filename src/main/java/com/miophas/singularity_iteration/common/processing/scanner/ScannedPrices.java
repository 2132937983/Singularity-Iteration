// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing.scanner;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.36: the prices the Scanner worked out, saved with the world (item id, buckets, recipe or estimate). A scanned price
 * stays fixed, so crystals and patterns keep their price; a datapack price added later wins (the quote book only answers a
 * Scanner price for an item the published generation neither prices nor denies). Loaded into {@link UuQuoteBook} at start.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class ScannedPrices extends SavedData {
    public record Entry(double buckets, ScannerPricing.Source source) { }

    private final Map<ResourceLocation, Entry> entries = new LinkedHashMap<>();

    public static ScannedPrices get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(ScannedPrices::new, ScannedPrices::load), "si_scanned_uu_prices");
    }

    public void put(ItemStack item, ScannerPricing.Price price) {
        entries.put(BuiltInRegistries.ITEM.getKey(item.getItem()), new Entry(price.buckets(), price.source()));
        setDirty();
    }

    public @Nullable Entry get(ItemStack item) {
        return entries.get(BuiltInRegistries.ITEM.getKey(item.getItem()));
    }

    public boolean remove(ItemStack item) {
        boolean removed = entries.remove(BuiltInRegistries.ITEM.getKey(item.getItem())) != null;
        if (removed) setDirty();
        return removed;
    }

    public int size() { return entries.size(); }

    private static ScannedPrices load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new ScannedPrices();
        for (Tag value : tag.getList("prices", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) value;
            var id = ResourceLocation.tryParse(row.getString("item"));
            double buckets = row.getDouble("buckets");
            if (id == null || !(buckets > 0) || !Double.isFinite(buckets)) continue;
            var source = "recipe".equals(row.getString("source")) ? ScannerPricing.Source.RECIPE : ScannerPricing.Source.ESTIMATE;
            data.entries.put(id, new Entry(buckets, source));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        entries.forEach((id, entry) -> {
            var row = new CompoundTag();
            row.putString("item", id.toString());
            row.putDouble("buckets", entry.buckets());
            row.putString("source", entry.source() == ScannerPricing.Source.RECIPE ? "recipe" : "estimate");
            list.add(row);
        });
        tag.put("prices", list);
        return tag;
    }

    /** After the published prices (UuPricingLifecycle, normal priority): the saved Scanner prices join the quote book. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void started(ServerStartedEvent event) {
        var server = event.getServer();
        int loaded = 0;
        for (var row : get(server).entries.entrySet()) {
            var item = BuiltInRegistries.ITEM.getOptional(row.getKey());
            if (item.isPresent() && UuQuoteBook.registerScanned(server, item.get().getDefaultInstance(), row.getValue().buckets())) loaded++;
        }
        if (loaded > 0) Singularity_Iteration.LOGGER.info("[UU Scanner] {} scanned prices loaded", loaded);
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        UuQuoteBook.removeScanned(event.getServer());
        ScannerPricing.forget(event.getServer());
    }
}

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
 * <p>0.1.11.218: the saved prices carry a pricing version. Saves from before the 0.1.11.218 pricing fix (EESU and TESU
 * Energy Cell at 0.33 mB, priced as natural blocks) are priced again once, at the first start with published prices.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class ScannedPrices extends SavedData {
    public record Entry(double buckets, ScannerPricing.Source source) { }

    /** 0.1.11.218: the pricing the saved entries were made with; a save without one is version 1. */
    public static final int VERSION = 2;

    private final Map<ResourceLocation, Entry> entries = new LinkedHashMap<>();
    private int version = VERSION;

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

    public int version() { return version; }

    /**
     * 0.1.11.218: prices every saved entry again with the current Scanner pricing and marks the data current. An entry the
     * Scanner can no longer price keeps its old price. Returns how many entries changed. Server thread only.
     */
    public int reprice(MinecraftServer server) {
        int changed = 0;
        for (var row : entries.entrySet()) {
            var item = BuiltInRegistries.ITEM.getOptional(row.getKey());
            if (item.isEmpty()) continue;
            var price = ScannerPricing.price(server, item.get().getDefaultInstance());
            if (price == null) continue;
            Entry old = row.getValue();
            if (Math.abs(price.buckets() - old.buckets()) > old.buckets() * 1e-9 || price.source() != old.source()) {
                row.setValue(new Entry(price.buckets(), price.source()));
                changed++;
            }
        }
        version = VERSION;
        setDirty();
        return changed;
    }

    private static ScannedPrices load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new ScannedPrices();
        data.version = tag.contains("version", Tag.TAG_INT) ? tag.getInt("version") : 1;
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
        tag.putInt("version", version);
        return tag;
    }

    /** After the published prices (UuPricingLifecycle, normal priority): the saved Scanner prices join the quote book. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void started(ServerStartedEvent event) {
        var server = event.getServer();
        var data = get(server);
        // 0.1.11.218: a save from before the pricing fix is priced again once (only with a published generation to price from)
        if (data.version < VERSION && UuQuoteBook.generation(server) > 0) {
            int changed = data.reprice(server);
            Singularity_Iteration.LOGGER.info("[UU Scanner] {} of {} saved prices changed with the 0.1.11.218 pricing", changed, data.size());
        }
        int loaded = 0;
        for (var row : data.entries.entrySet()) {
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

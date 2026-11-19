// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 0.1.11.76: the reactor layout written on a reactor config card, and the layout guide a reactor keeps after an export.
 * One entry per occupied reactor slot (0..53, row-major, 9 columns): the slot and the registry id of its item. Only the
 * item id is kept (no durability, no heat, no fuel left), so a fresh rod and a used rod are the same part.
 *
 * <p>Ids stay ids: an item of a removed mod is kept as written and is simply never found (it shows no guide).
 */
public record ReactorLayout(List<Entry> entries) {
    public static final int SLOTS = 54, COLUMNS = 9;

    public record Entry(int slot, ResourceLocation item) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, SLOTS - 1).fieldOf("slot").forGetter(Entry::slot),
            ResourceLocation.CODEC.fieldOf("item").forGetter(Entry::item)
        ).apply(i, Entry::new));
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Entry::slot,
            ResourceLocation.STREAM_CODEC, Entry::item,
            Entry::new);
    }

    public static final Codec<ReactorLayout> CODEC = Entry.CODEC.listOf()
        .comapFlatMap(ReactorLayout::checked, ReactorLayout::entries);
    public static final StreamCodec<ByteBuf, ReactorLayout> STREAM_CODEC =
        Entry.STREAM_CODEC.apply(ByteBufCodecs.list(SLOTS)).map(ReactorLayout::of, ReactorLayout::entries);

    public ReactorLayout {
        entries = List.copyOf(entries);
    }

    private static DataResult<ReactorLayout> checked(List<Entry> entries) {
        boolean[] seen = new boolean[SLOTS];
        for (Entry e : entries) {
            if (e.slot() < 0 || e.slot() >= SLOTS || seen[e.slot()]) return DataResult.error(() -> "Bad reactor layout slot " + e.slot());
            seen[e.slot()] = true;
        }
        return DataResult.success(of(entries));
    }

    /** A layout from loose entries: slots out of range and repeated slots are dropped, the rest sorted by slot. */
    public static ReactorLayout of(List<Entry> entries) {
        boolean[] seen = new boolean[SLOTS];
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) {
            if (e == null || e.item() == null || e.slot() < 0 || e.slot() >= SLOTS || seen[e.slot()]) continue;
            seen[e.slot()] = true;
            out.add(e);
        }
        out.sort(Comparator.comparingInt(Entry::slot));
        return new ReactorLayout(out);
    }

    /** The layout of the first {@link #SLOTS} slots of a reactor inventory (empty slots are left out). */
    public static ReactorLayout capture(IItemHandler reactor) {
        List<Entry> out = new ArrayList<>();
        for (int slot = 0; slot < Math.min(SLOTS, reactor.getSlots()); slot++) {
            ItemStack stack = reactor.getStackInSlot(slot);
            if (!stack.isEmpty()) out.add(new Entry(slot, BuiltInRegistries.ITEM.getKey(stack.getItem())));
        }
        return new ReactorLayout(out);
    }

    public boolean isEmpty() { return entries.isEmpty(); }

    public int size() { return entries.size(); }

    /** The item id of one slot, or null when the layout leaves it empty. */
    public ResourceLocation at(int slot) {
        for (Entry e : entries) if (e.slot() == slot) return e.item();
        return null;
    }

    /** The ids by slot: index = slot, null = empty. */
    public ResourceLocation[] toArray() {
        ResourceLocation[] out = new ResourceLocation[SLOTS];
        for (Entry e : entries) out[e.slot()] = e.item();
        return out;
    }

    /** Columns a reactor needs for this layout: 3 (no chamber) up to 9 (six chambers). */
    public int columnsNeeded() {
        int max = -1;
        for (Entry e : entries) max = Math.max(max, e.slot() % COLUMNS);
        return Math.max(3, max + 1);
    }

    /** Parts by item, in the order they first appear (for the tooltip). Unknown ids map to air. */
    public Map<Item, Integer> counts() {
        Map<Item, Integer> out = new LinkedHashMap<>();
        for (Entry e : entries) out.merge(item(e.item()), 1, Integer::sum);
        return out;
    }

    /** The registered item of an id, or air when the id is unknown (a removed mod). */
    public static Item item(ResourceLocation id) {
        return id == null ? Items.AIR : BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
    }
}

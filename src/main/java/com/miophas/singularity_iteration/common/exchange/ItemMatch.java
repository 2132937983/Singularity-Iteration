// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.36: what a bounty or commission accepts for its requested item: the item itself and, like the old ore dictionary,
 * any item sharing one of its material tags ({@code c:ingots/copper}, {@code c:dusts/iron}, ...), so the copper ingot of
 * another mod counts for ours. Only the specific tags of the {@link #FAMILIES} count, never a broad parent such as
 * {@code c:ingots}. A table requirement may name its own {@code "tag"} instead. Only plain stacks count (an enchanted,
 * named or damaged item is never delivered as a plain one). Records, the treasury and mailboxes keep the requested item.
 */
public record ItemMatch(Item item, List<TagKey<Item>> tags) {
    /** The tag families of the material unification, as {@code c:<family>/<material>}. */
    public static final List<String> FAMILIES = List.of(
        "ingots", "nuggets", "dusts", "gems", "plates", "gears", "rods", "raw_materials", "storage_blocks", "ores");

    /**
     * The requested item {@code itemId} and the tag {@code tag} ("" = the item's own material tags); null when the item is
     * unknown.
     */
    public static @Nullable ItemMatch of(String itemId, String tag) {
        Item item = ExchangeService.item(itemId);
        if (item == null) return null;
        if (tag != null && !tag.isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(tag.startsWith("#") ? tag.substring(1) : tag);
            return new ItemMatch(item, id == null ? List.of() : List.of(TagKey.create(Registries.ITEM, id)));
        }
        return new ItemMatch(item, materialTags(item));
    }

    /** The material tags of {@code item}: {@code c:<family>/<material>} of the {@link #FAMILIES}. */
    public static List<TagKey<Item>> materialTags(Item item) {
        List<TagKey<Item>> out = new ArrayList<>();
        BuiltInRegistries.ITEM.wrapAsHolder(item).tags().forEach(t -> {
            if (materialTag(t.location())) out.add(t);
        });
        return out;
    }

    /** True for {@code c:<family>/<material>} with a family of {@link #FAMILIES}. */
    public static boolean materialTag(ResourceLocation id) {
        if (!id.getNamespace().equals("c")) return false;
        int slash = id.getPath().indexOf('/');
        return slash > 0 && slash < id.getPath().length() - 1 && FAMILIES.contains(id.getPath().substring(0, slash));
    }

    /** A plain stack of the requested item or of an item in one of the tags. */
    public boolean test(ItemStack stack) {
        if (stack.isEmpty() || !InventoryPlan.plain(stack, stack.getItem())) return false;
        if (stack.is(item)) return true;
        for (TagKey<Item> t : tags) if (stack.is(t)) return true;
        return false;
    }

    /** True when {@code stack} is the requested item itself (taken before the other items of the tags). */
    public boolean exact(ItemStack stack) {
        return stack.is(item);
    }
}

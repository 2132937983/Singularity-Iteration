// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing.scanner;

import com.miophas.singularity_iteration.common.exchange.ItemMatch;
import com.miophas.singularity_iteration.core.runtime.uu.IndependentUuValueIndex;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.36: the Scanner's estimate of an item that no recipe prices, in buckets of UU-matter. Set on the high side on
 * purpose: an estimate too low would let the Replicator copy an item for less than it is worth.
 * <ol>
 *   <li>the same material: the lowest published price of an item sharing a {@code c:} material tag ({@link ItemMatch#FAMILIES});</li>
 *   <li>a block: the price of the tool tier it needs (none / stone / iron / diamond / netherite) times its hardness / 3
 *       (clamped), an ore times {@code ore_factor};</li>
 *   <li>another item: {@code item_base} times its rarity, its stack size and, for a tool or armor, its durability.</li>
 * </ol>
 */
public final class ScannerEstimates {
    private ScannerEstimates() {}

    public static @Nullable Double estimate(Item item, ScannerPolicy policy, Map<IndependentUuValueIndex.Key, Double> published) {
        if (policy.excluded(item)) return null;
        Double peer = materialPeer(item, published);
        if (peer != null) return peer;
        if (item instanceof BlockItem block) return blockEstimate(block.getBlock().defaultBlockState(), block.getBlock().defaultDestroyTime(), policy);
        return itemEstimate(item, policy);
    }

    /**
     * The seed of an item that recipes make but that no recipe could price (a cycle with no outside source, as amethyst shard
     * and block): the higher of its estimate and the item formula, since a raw-block estimate would make a crafted block too cheap.
     */
    public static @Nullable Double conservative(Item item, ScannerPolicy policy, Map<IndependentUuValueIndex.Key, Double> published) {
        Double estimate = estimate(item, policy, published);
        if (estimate == null) return null;
        return Math.max(estimate, itemEstimate(item, policy));
    }

    /** {@code item_base} times rarity, stack size, durability and fire resistance. */
    static double itemEstimate(Item item, ScannerPolicy policy) {
        ItemStack stack = item.getDefaultInstance();
        double value = policy.itemBase() * policy.rarity()[Math.min(policy.rarity().length - 1, stack.getRarity().ordinal())];
        int max = stack.getMaxStackSize();
        value *= max >= 64 ? policy.stack64() : max >= 16 ? policy.stack16() : policy.stack1();
        Integer durability = stack.get(DataComponents.MAX_DAMAGE);
        if (durability != null && durability > 0) value *= 1 + durability / 500.0;
        if (stack.has(DataComponents.FIRE_RESISTANT)) value *= 2;
        return value;
    }

    /** The lowest published price of another item in one of this item's material tags, or null. */
    static @Nullable Double materialPeer(Item item, Map<IndependentUuValueIndex.Key, Double> published) {
        Double best = null;
        for (var tag : ItemMatch.materialTags(item)) {
            for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                if (holder.value() == item) continue;
                Double price = published.get(IndependentUuValueIndex.keyOf(holder.value().getDefaultInstance()));
                if (price != null && price > 0 && (best == null || price < best)) best = price;
            }
        }
        return best;
    }

    /** A block by the tool tier it needs and its hardness; null for an unbreakable block. */
    static @Nullable Double blockEstimate(BlockState state, float hardness, ScannerPolicy policy) {
        if (hardness < 0) return null;
        int tier = state.is(Tags.Blocks.NEEDS_NETHERITE_TOOL) ? 4 : state.is(BlockTags.NEEDS_DIAMOND_TOOL) ? 3
            : state.is(BlockTags.NEEDS_IRON_TOOL) ? 2 : state.is(BlockTags.NEEDS_STONE_TOOL) ? 1 : 0;
        double value = policy.blockTiers()[tier] * Math.clamp(hardness / 3.0, policy.hardnessMin(), policy.hardnessMax());
        if (state.is(Tags.Blocks.ORES)) value *= policy.oreFactor();
        return value;
    }
}

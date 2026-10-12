// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.block;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;

/**
 * Drop of a machine that a player breaks without a wrench (0.1.8.0).
 *
 * <h2>Why this class exists</h2>
 * <p>Before 0.1.8.0, every block with the tag {@code mio_icif:machine} dropped
 * a basic machine hull (8 iron) when it was not wrenched. A BatBox has no hull
 * in its recipe (wood, cable, batteries). Breaking it with a pickaxe thus
 * created iron from nothing.
 *
 * <h2>Rule</h2>
 * <p>The drop follows the crafting recipes of the machine:
 * <ul>
 *   <li>Each recipe whose result is the machine item gets a hull level: 2 when
 *       an ingredient accepts the advanced machine hull, 1 when an ingredient
 *       accepts the basic machine hull, else 0.</li>
 *   <li>The drop uses the lowest level of all recipes. Thus a cheap recipe can
 *       never give a more valuable hull back.</li>
 *   <li>Level 2: the advanced hull. Level 1: the basic hull. Level 0, or no
 *       recipe: the machine item itself (as with a wrench, without its data).</li>
 * </ul>
 *
 * <p>The result is cached per item. The cache is cleared when the server
 * replaces its recipe manager (a data pack reload).
 */
public final class MachineSalvage {
    private MachineSalvage() { }

    private static final Map<Item, Item> CACHE = new ConcurrentHashMap<>();
    private static volatile RecipeManager cachedManager;

    /** Returns the drop of {@code block} broken without a wrench. */
    public static ItemStack salvage(ServerLevel level, Block block) {
        RecipeManager manager = level.getRecipeManager();
        if (manager != cachedManager) {
            CACHE.clear();
            cachedManager = manager;
        }
        Item machine = block.asItem();
        return new ItemStack(CACHE.computeIfAbsent(machine, item -> compute(level, manager, item)));
    }

    private static Item compute(ServerLevel level, RecipeManager manager, Item machine) {
        Item basic = mio_icif_blocks.MACHINE_HULL_BASIC.get().asItem();
        Item advanced = mio_icif_blocks.MACHINE_HULL_ADVANCED.get().asItem();
        ItemStack basicStack = new ItemStack(basic), advancedStack = new ItemStack(advanced);
        int lowest = Integer.MAX_VALUE;
        for (var holder : manager.getRecipes()) {
            var recipe = holder.value();
            ItemStack result;
            try {
                result = recipe.getResultItem(level.registryAccess());
            } catch (RuntimeException ignored) {
                continue; // a dynamic recipe without a fixed result
            }
            if (result.isEmpty() || !result.is(machine)) continue;
            int hull = 0;
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) continue;
                if (ingredient.test(advancedStack)) { hull = 2; break; }
                if (ingredient.test(basicStack)) hull = 1;
            }
            lowest = Math.min(lowest, hull);
            if (lowest == 0) break;
        }
        if (lowest == 2) return advanced;
        if (lowest == 1) return basic;
        return machine;
    }
}

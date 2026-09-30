package com.miophas.singularity_iteration.common.client.integration.jei;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_batch_crafter;
import com.miophas.singularity_iteration.common.menu.producer.BatchCrafterMenu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Counterpart of ic2 TransferHandlerBatchCrafter: the JEI "+" shortcut writes the recipe layout
 * into the batch crafter hologram grid. The grid is a template, so nothing is consumed from the
 * player inventory - only {@code setCraftingGridStack} runs, exactly like the IC2 impl.
 */
@SuppressWarnings("null")
public class BatchCrafterTransferHandler implements IRecipeTransferHandler<BatchCrafterMenu, RecipeHolder<CraftingRecipe>> {

    private static final int GRID = BatchCrafterMenu.CRAFTING_GRID_SIZE;

    @Override
    public Class<? extends BatchCrafterMenu> getContainerClass() {
        return BatchCrafterMenu.class;
    }

    @Override
    public Optional<MenuType<BatchCrafterMenu>> getMenuType() {
        return Optional.of(mio_icif_menus.BATCH_CRAFTER_MENU_TYPE.get());
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public IRecipeTransferError transferRecipe(BatchCrafterMenu menu, RecipeHolder<CraftingRecipe> recipe,
            IRecipeSlotsView slotsView, Player player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer) return null;
        mio_icif_batch_crafter crafter = menu.getBlockEntity();
        if (crafter == null) return null;

        ItemStack[] grid = buildGrid(recipe, slotsView, player, player.level());
        for (int i = 0; i < GRID; i++) {
            crafter.setCraftingGridStack(i, grid[i]);
        }
        return null;
    }

    private static ItemStack[] buildGrid(RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView slotsView,
            Player player, Level level) {
        ItemStack[] sequential = new ItemStack[GRID];
        Arrays.fill(sequential, ItemStack.EMPTY);
        int index = 0;
        for (IRecipeSlotView view : slotsView.getSlotViews(RecipeIngredientRole.INPUT)) {
            if (index >= GRID) break;
            Optional<ItemStack> displayed = view.getDisplayedItemStack();
            if (displayed.isPresent() && !displayed.get().isEmpty()) {
                sequential[index] = displayed.get().copyWithCount(1);
            }
            index++;
        }
        if (mio_icif_batch_crafter.gridMatches(level, recipe, sequential)) return sequential;

        // A shaped layout can sit at any offset inside the 3x3 grid; pick one that actually crafts.
        List<ItemStack[]> candidates = shapedCandidates(recipe, player);
        for (ItemStack[] candidate : candidates) {
            if (mio_icif_batch_crafter.gridMatches(level, recipe, candidate)) return candidate;
        }
        if (!candidates.isEmpty()) return candidates.get(0);
        return sequential;
    }

    private static List<ItemStack[]> shapedCandidates(RecipeHolder<CraftingRecipe> recipe, Player player) {
        if (!(recipe.value() instanceof ShapedRecipe shaped)) return List.of();
        int width = shaped.getWidth();
        int height = shaped.getHeight();
        if (width <= 0 || width > 3 || height <= 0 || height > 3) return List.of();

        java.util.List<ItemStack> cells = new ArrayList<>();
        for (Ingredient ingredient : shaped.getIngredients()) {
            cells.add(representative(ingredient, player));
        }

        List<ItemStack[]> layouts = new ArrayList<>();
        for (int offsetY = 0; offsetY + height <= 3; offsetY++) {
            for (int offsetX = 0; offsetX + width <= 3; offsetX++) {
                ItemStack[] grid = new ItemStack[GRID];
                Arrays.fill(grid, ItemStack.EMPTY);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int cellIndex = x + y * width;
                        if (cellIndex >= cells.size()) continue;
                        ItemStack cell = cells.get(cellIndex);
                        grid[(x + offsetX) + (y + offsetY) * 3] = cell == null ? ItemStack.EMPTY : cell;
                    }
                }
                layouts.add(grid);
            }
        }
        return layouts;
    }

    /** Prefer an item the player actually carries, like the vanilla recipe book does. */
    private static ItemStack representative(Ingredient ingredient, Player player) {
        ItemStack[] choices = ingredient.getItems();
        if (choices.length == 0) return ItemStack.EMPTY;
        for (ItemStack choice : choices) {
            for (ItemStack owned : player.getInventory().items) {
                if (!owned.isEmpty() && ItemStack.isSameItem(owned, choice)) return owned.copyWithCount(1);
            }
        }
        return choices[0].copyWithCount(1);
    }
}

package com.miophas.singularity_iteration.common.client.integration.jei;

import com.miophas.singularity_iteration.common.menu.producer.IndustrialWorkbenchMenu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JEI "+" for the Industrial Workbench: the 3 x 3 grid (menu slots 1-9) takes its items from the
 * buffer (10-27) and the player inventory with the hotbar (34-69). 0.1.10.14: the range 10-45 used
 * before took the hammer and cutter slots (28-33) and missed most of the player inventory; a full
 * hammer or cutter output slot (no placing) also made JEI hide "+".
 */
@SuppressWarnings("null")
public class IndustrialWorkbenchTransferInfo implements IRecipeTransferInfo<IndustrialWorkbenchMenu, RecipeHolder<CraftingRecipe>> {

    @Override
    public Class<? extends IndustrialWorkbenchMenu> getContainerClass() {
        return IndustrialWorkbenchMenu.class;
    }

    @Override
    public Optional<MenuType<IndustrialWorkbenchMenu>> getMenuType() {
        return Optional.of(mio_icif_menus.INDUSTRIAL_WORKBENCH_MENU_TYPE.get());
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public boolean canHandle(IndustrialWorkbenchMenu menu, RecipeHolder<CraftingRecipe> recipe) {
        return true;
    }

    @Override
    public List<Slot> getRecipeSlots(IndustrialWorkbenchMenu menu, RecipeHolder<CraftingRecipe> recipe) {
        return slots(menu, IndustrialWorkbenchMenu.MENU_SLOT_GRID_START, IndustrialWorkbenchMenu.MENU_SLOT_GRID_END);
    }

    @Override
    public List<Slot> getInventorySlots(IndustrialWorkbenchMenu menu, RecipeHolder<CraftingRecipe> recipe) {
        List<Slot> slots = slots(menu, IndustrialWorkbenchMenu.MENU_SLOT_BUFFER_START, IndustrialWorkbenchMenu.MENU_SLOT_BUFFER_END);
        slots.addAll(slots(menu, IndustrialWorkbenchMenu.PLAYER_INVENTORY_START, IndustrialWorkbenchMenu.TOTAL_SLOTS - 1));
        return slots;
    }

    /** Menu slots {@code first} to {@code last}, both included. */
    private static List<Slot> slots(IndustrialWorkbenchMenu menu, int first, int last) {
        List<Slot> slots = new ArrayList<>();
        for (int i = first; i <= last && i < menu.slots.size(); i++) slots.add(menu.getSlot(i));
        return slots;
    }
}

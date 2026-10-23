// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.integration.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * JEI runtime access (0.1.8.0). Only {@code RecipeLookup} calls this class, and only when JEI is
 * loaded, so the JEI classes never load without JEI.
 */
@OnlyIn(Dist.CLIENT)
public final class JeiBridge {
    private JeiBridge() {}

    static volatile IJeiRuntime runtime;

    /** Opens the JEI recipes of {@code stack} (as output, else as input). False without a runtime. */
    public static boolean show(ItemStack stack, boolean uses) {
        IJeiRuntime rt = runtime;
        if (rt == null || stack.isEmpty()) return false;
        var focus = rt.getJeiHelpers().getFocusFactory().createFocus(
            uses ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack);
        rt.getRecipesGui().show(focus);
        return true;
    }

    /** The JEI recipe categories (with recipes) of {@code namespace}; empty without a runtime. Dev capture tooling. */
    public static java.util.List<net.minecraft.resources.ResourceLocation> categories(String namespace) {
        IJeiRuntime rt = runtime;
        if (rt == null) return java.util.List.of();
        return rt.getRecipeManager().createRecipeCategoryLookup().get()
            .map(c -> c.getRecipeType().getUid())
            .filter(uid -> uid.getNamespace().equals(namespace))
            .sorted(java.util.Comparator.comparing(net.minecraft.resources.ResourceLocation::getPath))
            .toList();
    }

    /**
     * Dev capture tooling (0.1.10.14): JEI's crafting "+" for {@code recipeId} in the open menu, as the
     * button would see it: "ok", or the error type with the items of the slots JEI finds missing.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static String transferCheck(net.minecraft.resources.ResourceLocation recipeId) {
        IJeiRuntime rt = runtime;
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (rt == null || mc.player == null || mc.level == null) return "no JEI runtime";
        var holder = mc.level.getRecipeManager().byKey(recipeId).orElse(null);
        if (holder == null || !(holder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe)) return "no crafting recipe";
        var menu = mc.player.containerMenu;
        var category = rt.getRecipeManager().getRecipeCategory(mezz.jei.api.constants.RecipeTypes.CRAFTING);
        var handler = rt.getRecipeTransferManager().getRecipeTransferHandler(menu, category);
        if (handler.isEmpty()) return "no transfer handler for " + menu.getClass().getSimpleName();
        var layout = rt.getRecipeManager().createRecipeLayoutDrawable((mezz.jei.api.recipe.category.IRecipeCategory) category, holder,
            rt.getJeiHelpers().getFocusFactory().getEmptyFocusGroup());
        if (layout.isEmpty()) return "no recipe layout";
        var error = ((mezz.jei.api.recipe.transfer.IRecipeTransferHandler) handler.get())
            .transferRecipe(menu, holder, ((mezz.jei.api.gui.IRecipeLayoutDrawable<?>) layout.get()).getRecipeSlotsView(), mc.player, false, false);
        if (error == null) return "ok";
        StringBuilder missing = new StringBuilder();
        try {
            var field = error.getClass().getDeclaredField("slots");
            field.setAccessible(true);
            for (var slot : (java.util.Collection<mezz.jei.api.gui.ingredient.IRecipeSlotView>) field.get(error)) {
                missing.append(' ').append(slot.getDisplayedItemStack().map(s -> s.getHoverName().getString()).orElse("?"));
            }
        } catch (ReflectiveOperationException ignored) {
            missing.append(" (slots not readable)");
        }
        return error.getType() + " missing " + error.getMissingCountHint() + ":" + missing;
    }

    /** Opens the JEI recipes of one category. False without a runtime or category. Dev capture tooling. */
    public static boolean showCategory(net.minecraft.resources.ResourceLocation uid) {
        IJeiRuntime rt = runtime;
        if (rt == null) return false;
        var type = rt.getJeiHelpers().getRecipeType(uid);
        if (type.isEmpty()) return false;
        rt.getRecipesGui().showTypes(java.util.List.of(type.get()));
        return true;
    }
}

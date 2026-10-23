package com.miophas.singularity_iteration.common.client.integration.jei;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 0.1.10.14: charged batteries, crystals, energy tools and armor and charged storage blocks match
 * the crafting recipes that take them, in JEI 19.21 as in later versions.
 *
 * <p>JEI 19.21 matches the player's items against a recipe slot with {@link UidContext#Ingredient},
 * the context its item list also uses to keep the empty and the full creative-tab variant apart
 * (JEI 19.27 and later match with {@link UidContext#Recipe}, where these items have no subtype). The
 * exact charge as that subtype never equalled the empty item a recipe shows, so a charged one was
 * always "missing" (red) and "+" refused. The subtype is now only {@link #CHARGED} or none, and the
 * {@link Extension} shows each such input of a crafting recipe both empty and charged.
 */
@SuppressWarnings("null")
public final class ChargedInputs {
    private ChargedInputs() {}

    /** Subtype of a charged energy item in the Ingredient context. */
    public static final String CHARGED = "charged";

    private static final Set<Item> ITEMS = new HashSet<>();
    /** The charged variant JEI lists for each item (the full creative-tab stack), filled on first use. */
    private static final Map<Item, ItemStack> VARIANTS = new HashMap<>();
    @Nullable
    private static IIngredientManager ingredients;
    private static boolean listed;

    /** Registers {@code interpreter} for {@code item} and remembers the item as an energy input. */
    static void register(ISubtypeRegistration registration, ItemLike item, IIngredientSubtypeInterpreter<ItemStack> interpreter) {
        registration.registerSubtypeInterpreter(item.asItem(), interpreter);
        ITEMS.add(item.asItem());
    }

    /** An item stack's charge, or a block item's {@code energy} block-entity data. */
    public static boolean isCharged(ItemStack stack) {
        if (stack.getItem() instanceof IBatteryItem battery) return battery.getEnergy(stack) > 0;
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data != null && data.copyTag().getLong("energy") > 0;
    }

    /** {@link #CHARGED} or none: the Ingredient context only, recipes match on the item alone. */
    static String subtype(ItemStack stack, UidContext context) {
        if (context == UidContext.Recipe) return IIngredientSubtypeInterpreter.NONE;
        return isCharged(stack) ? CHARGED : IIngredientSubtypeInterpreter.NONE;
    }

    static void registerExtensions(IVanillaCategoryExtensionRegistration registration) {
        ingredients = registration.getJeiHelpers().getIngredientManager();
        listed = false;
        VARIANTS.clear();
        var crafting = registration.getCraftingCategory();
        // Another mod may already extend these exact classes; JEI then keeps its extension.
        try { crafting.addExtension(ShapedRecipe.class, new Extension<>()); } catch (IllegalArgumentException ignored) {}
        try { crafting.addExtension(ShapelessRecipe.class, new Extension<>()); } catch (IllegalArgumentException ignored) {}
    }

    /** The stack a recipe slot shows, the uncharged one when JEI happens to show the added charged variant. */
    public static ItemStack recipeStack(IRecipeSlotView view, ItemStack displayed) {
        if (!ITEMS.contains(displayed.getItem()) || !isCharged(displayed)) return displayed;
        for (ItemStack stack : view.getItemStacks().toList()) if (!isCharged(stack)) return stack;
        return displayed;
    }

    @Nullable
    private static ItemStack chargedVariant(ItemStack stack) {
        if (!ITEMS.contains(stack.getItem()) || isCharged(stack)) return null;
        if (!listed && ingredients != null) {
            listed = true;
            for (ItemStack listedStack : ingredients.getAllItemStacks()) {
                if (ITEMS.contains(listedStack.getItem()) && isCharged(listedStack)) VARIANTS.putIfAbsent(listedStack.getItem(), listedStack);
            }
        }
        ItemStack variant = VARIANTS.get(stack.getItem());
        if (variant != null) return variant.copyWithCount(stack.getCount());
        if (!(stack.getItem() instanceof IBatteryItem)) return null;
        ItemStack full = stack.copy();
        var items = MioIcifAPI.instance().getItemAPI();
        items.setBatteryEnergy(full, items.getBatteryCapacity(full));
        return isCharged(full) ? full : null;
    }

    /** JEI's own crafting layout, with a charged variant beside every empty energy input. */
    static final class Extension<R extends CraftingRecipe> implements ICraftingCategoryExtension<R> {
        @Override
        public boolean isHandled(RecipeHolder<R> holder) {
            if (holder.value().isSpecial()) return false;
            for (Ingredient ingredient : holder.value().getIngredients()) {
                for (ItemStack stack : ingredient.getItems()) if (ITEMS.contains(stack.getItem())) return true;
            }
            return false;
        }

        @Override
        public void setRecipe(RecipeHolder<R> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
            R recipe = holder.value();
            List<List<ItemStack>> inputs = new ArrayList<>();
            for (Ingredient ingredient : recipe.getIngredients()) {
                ItemStack[] items = ingredient.getItems();
                List<ItemStack> shown = new ArrayList<>(Arrays.asList(items));
                for (ItemStack stack : items) {
                    ItemStack charged = chargedVariant(stack);
                    if (charged != null) shown.add(charged);
                }
                inputs.add(shown);
            }
            var level = Minecraft.getInstance().level;
            grid.createAndSetOutputs(builder, List.of(level == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess())));
            grid.createAndSetInputs(builder, inputs, getWidth(holder), getHeight(holder));
        }

        @Override
        public int getWidth(RecipeHolder<R> holder) {
            return holder.value() instanceof ShapedRecipe shaped ? shaped.getWidth() : 0;
        }

        @Override
        public int getHeight(RecipeHolder<R> holder) {
            return holder.value() instanceof ShapedRecipe shaped ? shaped.getHeight() : 0;
        }
    }
}

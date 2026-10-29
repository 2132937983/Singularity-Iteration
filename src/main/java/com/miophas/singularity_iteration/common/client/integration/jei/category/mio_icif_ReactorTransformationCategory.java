// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.reactor.ReactorTransformation;
import com.miophas.singularity_iteration.core.api.reactor.ReactorTransformationRegistry;
import java.util.Locale;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.37: "Nuclear Fuel Breeding & Decay": the component changes of {@link ReactorTransformationRegistry} (SI's own, from the
 * reactor's tables, and those of addons). Entries whose items are missing are left out by
 * {@link ReactorTransformationRegistry#available()}.
 * <pre>
 *   [lithium cell] ==> [tritium rod]
 *        Bred at 10,000 progress
 *       Per pulse: hull/3000 + 1
 * </pre>
 */
@SuppressWarnings("null")
public class mio_icif_ReactorTransformationCategory implements IRecipeCategory<ReactorTransformation> {
    public static final RecipeType<ReactorTransformation> TYPE =
        new RecipeType<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "reactor_transformation"), ReactorTransformation.class);

    private static final int WIDTH = 176, ROW_Y = 6, LINES_Y = 30, INPUT_X = 48, ARROW_X = 72, OUTPUT_X = 112;
    private static final String KEY = "jei.mio_icif.reactor_transformation.";

    private final IDrawable icon;
    private final int height;

    /** Tall enough for the entry with the most lines (the process line and the notes). */
    public mio_icif_ReactorTransformationCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(BuiltInRegistries.ITEM.get(
            ResourceLocation.fromNamespaceAndPath("mio_icif", "normal/item_tritium_cell"))));
        int lines = 1;
        for (ReactorTransformation change : ReactorTransformationRegistry.available()) lines = Math.max(lines, 1 + change.notes().size());
        this.height = LINES_Y + lines * SiJei.LINE_HEIGHT + 1;
    }

    @Override public RecipeType<ReactorTransformation> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.mio_icif.reactor_transformation"); }
    @Override public @Nullable IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return height; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ReactorTransformation change, IFocusGroup focuses) {
        SiJei.input(builder, INPUT_X, ROW_Y).addItemStack(new ItemStack(BuiltInRegistries.ITEM.get(change.input())));
        SiJei.output(builder, OUTPUT_X, ROW_Y).addItemStack(new ItemStack(BuiltInRegistries.ITEM.get(change.output())));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ReactorTransformation change, IFocusGroup focuses) {
        SiJei.arrow(builder, ARROW_X, ROW_Y, 0);
        String process = change.process().name().toLowerCase(Locale.ROOT);
        int y = LINES_Y;
        SiJei.line(builder, change.amount() > 0
            ? Component.translatable(KEY + process, String.format(Locale.ROOT, "%,d", change.amount()))
            : Component.translatable(KEY + process + ".short"), WIDTH, y);
        for (Component note : change.notes()) {
            y += SiJei.LINE_HEIGHT;
            SiJei.line(builder, note, WIDTH, y);
        }
    }
}

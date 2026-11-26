// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.integration.RecipeViews;
import com.miophas.singularity_iteration.common.reactor.ReactorLayoutAnalysis;
import com.miophas.singularity_iteration.core.api.reactor.ReactorLayout;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.19: "Reactor Layouts", like the reactor plans of IC2 Classic in JEI: one page per registered
 * {@link ReactorLayout} ({@code core.api.reactor.ReactorLayoutRegistry}, so addon layouts appear too). The
 * components are input slots (looking up the uses of a component finds the layouts with it). Under the grid:
 * EU/t, the heat balance, the risk, the explosion with the local config, and the fuel life. The numbers come
 * from {@link ReactorLayoutAnalysis} (the reactor model for a full fuel load), computed when a page is first shown.
 * <pre>
 *        Title
 *   [ ][ ][ ][ ][ ][ ]
 *   ... 6 rows x 3..9 columns
 *   Output / heat / risk / explosion / fuel (scroll box)
 * </pre>
 */
@SuppressWarnings("null")
public class mio_icif_ReactorLayoutCategory implements IRecipeCategory<ReactorLayoutAnalysis> {
    public static final RecipeType<ReactorLayoutAnalysis> TYPE =
        new RecipeType<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "reactor_layout"), ReactorLayoutAnalysis.class);

    private static final int WIDTH = 168, HEIGHT = 190, GRID_Y = 11, LINES_Y = GRID_Y + 6 * 18 + 3;
    private static final int TITLE_COLOR = 0xFF404040;

    private final IDrawable icon;

    public mio_icif_ReactorLayoutCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(BuiltInRegistries.ITEM.get(
            ResourceLocation.fromNamespaceAndPath("mio_icif", "reactor/item_reactor_uranium_quad"))));
    }

    @Override public RecipeType<ReactorLayoutAnalysis> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.mio_icif.category.reactor_layouts"); }
    @Override public @Nullable IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public ResourceLocation getRegistryName(ReactorLayoutAnalysis layout) { return layout.layout().id(); }

    private static int gridX(int columns) {
        return (WIDTH - columns * 18) / 2;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ReactorLayoutAnalysis layout, IFocusGroup focuses) {
        int columns = layout.columns(), x0 = gridX(columns);
        for (int row = 0; row < ReactorLayout.MAX_ROWS; row++) {
            for (int column = 0; column < columns; column++) {
                ResourceLocation item = layout.item(row * 9 + column);
                var slot = builder.addSlot(RecipeIngredientRole.INPUT, x0 + column * 18 + 1, GRID_Y + row * 18 + 1).setStandardSlotBackground();
                if (item != null) slot.addItemStack(new ItemStack(BuiltInRegistries.ITEM.get(item)));
            }
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ReactorLayoutAnalysis layout, IFocusGroup focuses) {
        ReactorLayout plan = layout.layout();
        builder.addText(Component.translatable(plan.effectiveTitleKey()), 0, 0, WIDTH, 10)
            .alignHorizontalCenter().setColor(TITLE_COLOR).setShadow(false);
        builder.addScrollBoxWidget(WIDTH, HEIGHT - LINES_Y, 0, LINES_Y).setContents(lines(layout));
    }

    /** The text under the grid (0.1.11.209: built by RecipeViews, shared with the EMI plugin). */
    static List<FormattedText> lines(ReactorLayoutAnalysis layout) {
        return new ArrayList<>(RecipeViews.reactorLayoutLines(layout));
    }

    /** "45 s", "4 min 40 s", "5 h 33 min". */
    static String duration(int seconds) {
        return RecipeViews.duration(seconds);
    }
}

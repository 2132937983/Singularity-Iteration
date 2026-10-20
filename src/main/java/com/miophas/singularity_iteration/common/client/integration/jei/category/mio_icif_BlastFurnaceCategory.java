package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.recipe.blast_furnace.mio_icif_BlastFurnaceRecipe;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Blast Furnace: the ore and the compressed air cells for the air of the whole run give the result
 * and the slag.
 * <pre>
 *   [ore] + [air cells] ==> [ out ][slag]
 *        30 s · Air 1 mB/t
 * </pre>
 */
@SuppressWarnings("null")
public class mio_icif_BlastFurnaceCategory extends AbstractJeiRecipeCategory<mio_icif_BlastFurnaceRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "blast_furnace");
    public static final RecipeType<RecipeHolder<mio_icif_BlastFurnaceRecipe>> BLAST_FURNACE_TYPE = RecipeType.createRecipeHolderType(UID);

    private static final int WIDTH = 150, HEIGHT = 45;
    private static final int ORE_X = 9, PLUS_X = 30, AIR_X = 48, ARROW_X = 71, OUT_X = 105, SLAG_X = 127, ROW_Y = 9, LINES_Y = 34;

    private final IDrawable icon;

    public mio_icif_BlastFurnaceCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemLike(mio_icif_blocks.BLAST_FURNACE.get());
    }

    @Override public RecipeType<RecipeHolder<mio_icif_BlastFurnaceRecipe>> getRecipeType() { return BLAST_FURNACE_TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.mio_icif.blast_furnace"); }
    @Override public @Nullable IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }

    @Override
    protected void setRecipeFor(IRecipeLayoutBuilder builder, mio_icif_BlastFurnaceRecipe recipe, IFocusGroup focuses) {
        SiJei.input(builder, ORE_X, ROW_Y).addItemStacks(SiJei.withCount(recipe.getIngredient(), recipe.getIngredientCount()));
        long air = (long) recipe.getAirCostPerTick() * recipe.getDuration();
        if (air > 0) {
            // the furnace fills its air tank from compressed air cells; the fluid itself is nearly invisible
            ItemStack cell = new ItemStack(mio_icif_cells.CELL_AIR.get());
            int perCell = Math.max(1, mio_icif_cells.getCellCapacity(cell));
            SiJei.input(builder, AIR_X, ROW_Y).addItemStack(cell.copyWithCount((int) Math.min(64, (air + perCell - 1) / perCell)));
            SiJei.contents(builder, RecipeIngredientRole.INPUT, List.of(cell));
        }
        SiJei.mainOutput(builder, OUT_X, ROW_Y).addItemStack(recipe.getResult());
        if (!recipe.getSecondaryResult().isEmpty()) SiJei.output(builder, SLAG_X, ROW_Y).addItemStack(recipe.getSecondaryResult());
    }

    @Override
    protected void createExtrasFor(IRecipeExtrasBuilder builder, mio_icif_BlastFurnaceRecipe recipe, IFocusGroup focuses) {
        if (recipe.getAirCostPerTick() > 0) builder.addRecipePlusSign().setPosition(PLUS_X, ROW_Y + 2);
        SiJei.arrow(builder, ARROW_X, ROW_Y, recipe.getDuration());
        Component line = recipe.getAirCostPerTick() > 0
            ? Component.translatable("jei.mio_icif.blast_furnace.air", SiJei.seconds(recipe.getDuration()), SiJei.number(recipe.getAirCostPerTick()))
            : Component.literal(SiJei.seconds(recipe.getDuration()));
        SiJei.line(builder, line, WIDTH, LINES_Y);
    }
}

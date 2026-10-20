package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.blockentity.kuentity.mio_icif_lathe;
import com.miophas.singularity_iteration.common.item.lathe.LatheShapes;
import com.miophas.singularity_iteration.common.item.lathe.LatheTurningBlanksItem;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.core.api.item.ILatheItem;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Lathe: the named parts of {@link LatheShapes}: an uncut blank and a lathe tool harder than it give
 * the part; the cuts and the KU they take.
 * <pre>
 *   [blank] + [tool] ==> [ part ]
 *         Profile 5-4-3-2-1
 *        10 cuts · 10,000 KU
 *      Part for Diamond Drill
 * </pre>
 */
@SuppressWarnings("null")
public class mio_icif_LatheCategory implements IRecipeCategory<LatheShapes.Shape> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "lathe");
    public static final RecipeType<LatheShapes.Shape> LATHE_TYPE = new RecipeType<>(UID, LatheShapes.Shape.class);

    private static final int WIDTH = 150, HEIGHT = 65;
    private static final int BLANK_X = 17, PLUS_X = 38, TOOL_X = 56, ARROW_X = 79, OUT_X = 113, ROW_Y = 9, LINES_Y = 34;

    private final IDrawable icon;

    public mio_icif_LatheCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemLike(mio_icif_blocks.LATHE.get());
    }

    @Override public RecipeType<LatheShapes.Shape> getRecipeType() { return LATHE_TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.mio_icif.lathe"); }
    @Override public @Nullable IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }

    private static ItemStack blank(LatheTurningBlanksItem.LatheMaterial material) {
        return new ItemStack(material == LatheTurningBlanksItem.LatheMaterial.WOOD
            ? mio_icif_normal.WOODEN_TURNING_BLANKS.get() : mio_icif_normal.IRON_TURNING_BLANKS.get());
    }

    /** Every lathe tool that can cut the blank (harder than it). */
    private static List<ItemStack> tools(ItemStack blank) {
        int hardness = ((ILatheItem) blank.getItem()).getHardness(blank);
        List<ItemStack> tools = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ILatheItem.ILatheTool tool)) continue;
            ItemStack stack = new ItemStack(item);
            if (tool.getHardness(stack) > hardness) tools.add(stack);
        }
        return tools;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, LatheShapes.Shape shape, IFocusGroup focuses) {
        ItemStack blank = blank(shape.material());
        SiJei.input(builder, BLANK_X, ROW_Y).addItemStack(blank);
        SiJei.input(builder, TOOL_X, ROW_Y).addItemStacks(tools(blank));
        SiJei.mainOutput(builder, OUT_X, ROW_Y).addItemStack(LatheTurningBlanksItem.getStackForState(blank, shape.state()));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, LatheShapes.Shape shape, IFocusGroup focuses) {
        int cuts = 0;
        for (int s : shape.state()) cuts += shape.material().width - s;
        builder.addRecipePlusSign().setPosition(PLUS_X, ROW_Y + 2);
        SiJei.arrow(builder, ARROW_X, ROW_Y, 0);
        SiJei.line(builder, Component.translatable("jei.mio_icif.lathe.profile", shape.profile()), WIDTH, LINES_Y);
        SiJei.line(builder, Component.translatable("jei.mio_icif.lathe.cost", cuts,
            SiJei.number((long) cuts * mio_icif_lathe.KU_COST_PER_OPERATION)), WIDTH, LINES_Y + SiJei.LINE_HEIGHT);
        SiJei.line(builder, Component.translatable("jei.mio_icif.lathe.part", shape.usedInItem().getDescription()),
            WIDTH, LINES_Y + 2 * SiJei.LINE_HEIGHT);
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(LatheShapes.Shape shape) {
        return ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
            "lathe/" + Integer.toHexString(LatheTurningBlanksItem.getNumericState(shape.state())));
    }
}

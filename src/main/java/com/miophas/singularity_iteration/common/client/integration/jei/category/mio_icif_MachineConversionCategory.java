package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.blockentity.MachineConversion;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One machine's own conversions ({@link MachineConversion}, from the machine's static
 * {@code conversions()}): what it makes in code rather than from a JSON recipe. One category per
 * machine, named after it. Without inputs the machine itself stands on the left.
 * <pre>
 *   [water] ==> [steam]          [rig] ==> [oil]
 *      Below 220 bar            One block every 2.25 s
 *    100–200 HU per mB         1,000 EU (500 EU if dry)
 * </pre>
 */
@SuppressWarnings("null")
public class mio_icif_MachineConversionCategory implements IRecipeCategory<MachineConversion> {

    private static final int WIDTH = 160, ROW_Y = 6, LINES_Y = 30, PITCH = 18, TO_ARROW = 24, TO_OUTPUT = 30;

    private final RecipeType<MachineConversion> type;
    private final ItemLike machine;
    private final IDrawable icon;
    private final int height;

    /** {@code lines}: the most notes any of the machine's conversions has. */
    public mio_icif_MachineConversionCategory(IGuiHelper helper, ItemLike machine, int lines) {
        this.type = typeFor(machine);
        this.machine = machine;
        this.icon = helper.createDrawableItemLike(machine);
        this.height = LINES_Y + Math.max(1, lines) * SiJei.LINE_HEIGHT + 1;
    }

    /** {@code mio_icif:conversion/<machine item path>}. */
    public static RecipeType<MachineConversion> typeFor(ItemLike machine) {
        String path = BuiltInRegistries.ITEM.getKey(machine.asItem()).getPath();
        return new RecipeType<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "conversion/" + path), MachineConversion.class);
    }

    @Override public RecipeType<MachineConversion> getRecipeType() { return type; }
    @Override public Component getTitle() { return new ItemStack(machine).getHoverName(); }
    @Override public @Nullable IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return height; }

    private static int inputs(MachineConversion conversion) {
        return Math.max(1, conversion.fluidInputs().size() + conversion.itemInputs().size());
    }

    private static int firstX(MachineConversion conversion) {
        int outputs = Math.max(1, conversion.fluidOutputs().size() + conversion.itemOutputs().size());
        int span = (inputs(conversion) - 1) * PITCH + TO_ARROW + TO_OUTPUT + (outputs - 1) * PITCH + 16;
        return (WIDTH - span) / 2;
    }

    private static int arrowX(MachineConversion conversion) {
        return firstX(conversion) + (inputs(conversion) - 1) * PITCH + TO_ARROW;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MachineConversion conversion, IFocusGroup focuses) {
        int x = firstX(conversion);
        if (conversion.fluidInputs().isEmpty() && conversion.itemInputs().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, x, ROW_Y).addItemLike(machine);
        }
        for (List<FluidStack> fluids : conversion.fluidInputs()) {
            List<Fluid> kinds = fluids.stream().map(FluidStack::getFluid).toList();
            SiJei.fluids(builder, RecipeIngredientRole.INPUT, x, ROW_Y, kinds, fluids.get(0).getAmount());
            x += PITCH;
        }
        for (List<ItemStack> items : conversion.itemInputs()) {
            SiJei.input(builder, x, ROW_Y).addItemStacks(items);
            x += PITCH;
        }
        x = arrowX(conversion) + TO_OUTPUT;
        for (FluidStack fluid : conversion.fluidOutputs()) {
            SiJei.fluid(builder, RecipeIngredientRole.OUTPUT, x, ROW_Y, fluid);
            x += PITCH;
        }
        for (ItemStack item : conversion.itemOutputs()) {
            SiJei.output(builder, x, ROW_Y).addItemStack(item);
            x += PITCH;
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MachineConversion conversion, IFocusGroup focuses) {
        SiJei.arrow(builder, arrowX(conversion), ROW_Y, conversion.ticks());
        int y = LINES_Y;
        for (Component note : conversion.notes()) {
            SiJei.line(builder, note, WIDTH, y);
            y += SiJei.LINE_HEIGHT;
        }
    }
}

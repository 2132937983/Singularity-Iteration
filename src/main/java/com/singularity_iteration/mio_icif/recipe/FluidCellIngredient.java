package com.singularity_iteration.mio_icif.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_dynamic_cell;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.stream.Stream;

public class FluidCellIngredient implements ICustomIngredient {

    public static final MapCodec<FluidCellIngredient> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(i -> i.fluid)
        ).apply(instance, FluidCellIngredient::new)
    );

    public static final IngredientType<FluidCellIngredient> TYPE = new IngredientType<>(CODEC);

    private final Fluid fluid;

    public FluidCellIngredient(Fluid fluid) {
        this.fluid = fluid;
    }

    @Override
    public boolean test(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (this.fluid == Fluids.EMPTY) {
            return mio_icif_cells.isEmptyCell(stack);
        }
        return mio_icif_cells.isCellContainingFluid(stack, fluid);
    }

    @Override
    public Stream<ItemStack> getItems() {
        if (this.fluid == Fluids.EMPTY) {
            return Stream.of(new ItemStack(mio_icif_cells.CELL_EMPTY.get()));
        }
        ItemStack staticCell = mio_icif_cells.getFilledCellForFluidStack(fluid);
        ItemStack dynamicCell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
        ((mio_icif_dynamic_cell) mio_icif_cells.CELL_EMPTY.get()).writeFluidToNBT(dynamicCell, new FluidStack(fluid, 1000));
        if (!staticCell.isEmpty()) {
            return Stream.of(staticCell, dynamicCell);
        }
        return Stream.of(dynamicCell);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public Ingredient toVanilla() {
        return new Ingredient(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FluidCellIngredient that)) return false;
        return fluid == that.fluid;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(fluid);
    }
}
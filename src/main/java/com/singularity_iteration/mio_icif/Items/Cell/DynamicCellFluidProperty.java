package com.singularity_iteration.mio_icif.Items.Cell;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings({"null", "deprecation"})
public class DynamicCellFluidProperty implements ItemPropertyFunction {

    @Override
    public float call(ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
        if (stack.getItem() instanceof mio_icif_dynamic_cell dynamicCell) {
            var fluid = dynamicCell.getFluid(stack);
            if (fluid.isEmpty()) return 0.0F;
            return (float) mio_icif_cells.getCellTypeForFluid(fluid.getFluid());
        }
        return 0.0F;
    }
}
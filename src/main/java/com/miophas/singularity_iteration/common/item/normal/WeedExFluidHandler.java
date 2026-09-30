// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.normal;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/** Original Weed-EX can: 64 doses of 50 mB. Legacy SI item damage remains valid. */
public final class WeedExFluidHandler implements IFluidHandlerItem {
    private final ItemStack stack;
    public WeedExFluidHandler(ItemStack stack) { this.stack = stack; }
    @Override public ItemStack getContainer() { return stack; }
    @Override public int getTanks() { return 1; }
    @Override public int getTankCapacity(int tank) { return 3200; }
    @Override public FluidStack getFluidInTank(int tank) {
        return stack.isEmpty() ? FluidStack.EMPTY : new FluidStack(mio_icif_fluids.WEED_EX.get(), Math.max(0, 64 - stack.getDamageValue()) * 50);
    }
    @Override public boolean isFluidValid(int tank, FluidStack fluid) { return false; }
    @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
    @Override public FluidStack drain(FluidStack request, FluidAction action) {
        return request.getFluid().isSame(mio_icif_fluids.WEED_EX.get()) ? drain(request.getAmount(), action) : FluidStack.EMPTY;
    }
    @Override public FluidStack drain(int amount, FluidAction action) {
        if (amount < 50 || stack.isEmpty() || stack.getDamageValue() >= 64) return FluidStack.EMPTY;
        if (action.execute()) {
            int damage = stack.getDamageValue() + 1;
            if (damage >= 64) stack.shrink(1); else stack.setDamageValue(damage);
        }
        return new FluidStack(mio_icif_fluids.WEED_EX.get(), 50);
    }
}

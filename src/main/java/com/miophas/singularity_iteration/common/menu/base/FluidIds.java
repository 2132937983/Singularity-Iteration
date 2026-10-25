// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.menu.base;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 0.1.10.19: a fluid as one menu data value (its registry id, -1 for none), for screens that draw a
 * tank which can hold more than one fluid. Fluid ids are synced to the client with the registries.
 */
public final class FluidIds {
    private FluidIds() { }

    /** The registry id of the fluid of {@code stack}, -1 when the stack is empty. */
    public static int idOf(FluidStack stack) {
        return stack == null || stack.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(stack.getFluid());
    }

    /** The fluid with registry id {@code id} and {@code amount} mB; empty for -1, an unknown id or no amount. */
    public static FluidStack stackOf(int id, int amount) {
        if (id < 0 || amount <= 0) return FluidStack.EMPTY;
        var fluid = BuiltInRegistries.FLUID.byId(id);
        return fluid == null || fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }
}

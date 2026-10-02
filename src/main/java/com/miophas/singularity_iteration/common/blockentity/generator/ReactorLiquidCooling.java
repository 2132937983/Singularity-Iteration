// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.core.runtime.reactor.FluidReactorCycle;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Liquid cooling for the standard (generator-mode) nuclear reactor.
 *
 * <p>Coolant piped (or bucketed) into the reactor is pumped through the core every cycle: it first
 * drains the heat stored in vents, condensators, cells and exchangers (so they never wear out or
 * burn up while coolant flows), then cools the hull. Each fluid carries a fixed amount of heat per
 * millibucket (its heat capacity); flow is capped at {@link #MAX_FLOW_PER_CYCLE} mB per 20-tick
 * cycle, so a better coolant cools proportionally faster. SI coolant and water leave as their hot
 * forms through the output tank (extract it with pipes); foreign coolants (IC2, GregTech,
 * Extreme Reactors, Mekanism, ...) are recognised by name and consumed.
 */
public final class ReactorLiquidCooling implements IFluidHandler {
    public static final int CAPACITY = 16_000;
    public static final int MAX_FLOW_PER_CYCLE = 2_000;

    /** Heat capacity of one coolant: HU absorbed per mB, and the fluid it leaves as (or null: consumed). */
    public record Coolant(int huPerMB, @Nullable Fluid hot) { }

    private final FluidTank cold = new FluidTank(CAPACITY, stack -> coolant(stack.getFluid()) != null);
    private final FluidTank hot = new FluidTank(CAPACITY);   // filled only internally (fill() routes to the cold tank)
    private final Runnable changed;
    private int lastAbsorbed;

    public ReactorLiquidCooling(Runnable changed) {
        this.changed = changed;
    }

    /**
     * Coolant table. Exact ids first, then name patterns so other mods' coolants and cryogenic
     * fluids work without hard dependencies. Hot / molten / gaseous fluids are never coolants.
     */
    @Nullable
    public static Coolant coolant(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return null;
        if (fluid == mio_icif_fluids.COOLANT.get()) return new Coolant(FluidReactorCycle.COOLANT_HU_PER_MB, mio_icif_fluids.HOTCOOLANT.get());
        if (fluid == Fluids.WATER) return new Coolant(FluidReactorCycle.WATER_HU_PER_MB, mio_icif_fluids.HOTWATER.get());
        if (fluid == mio_icif_fluids.DISTILLEDWATER.get()) return new Coolant(2, mio_icif_fluids.HOTWATER.get());
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        String p = id.getPath();
        if (p.startsWith("flowing_") || p.contains("hot") || p.contains("superheated") || p.contains("steam")
            || p.contains("molten") || p.contains("lava") || p.contains("plasma") || p.contains("heated")) return null;
        if (p.contains("cryotheum") || p.contains("redfrigium") || p.contains("cryomisi")) return new Coolant(60, null);
        if (p.contains("helium")) return new Coolant(80, null);
        if (p.contains("nitrogen")) return new Coolant(40, null);
        if (p.contains("liquid_oxygen") || p.contains("liquid_air")) return new Coolant(35, null);
        if (p.contains("sodium_potassium") || p.equals("nak") || p.contains("tangerium")) return new Coolant(45, null);
        if (p.contains("refrigerant") || p.contains("freon")) return new Coolant(30, null);
        if (p.contains("coolant")) return new Coolant(FluidReactorCycle.COOLANT_HU_PER_MB, null);
        if (p.contains("distilled") && p.contains("water")) return new Coolant(2, null);
        if (p.equals("water") || p.endsWith("_water")) return new Coolant(FluidReactorCycle.WATER_HU_PER_MB, null);
        return null;
    }

    @Nullable
    public Coolant active() {
        return cold.isEmpty() ? null : coolant(cold.getFluid().getFluid());
    }

    /** Heat this cycle's flow can still take (HU), limited by stored coolant, flow cap and hot-tank room. */
    public long budget() {
        Coolant c = active();
        if (c == null) return 0;
        int mb = Math.min(cold.getFluidAmount(), MAX_FLOW_PER_CYCLE);
        if (c.hot() != null) {
            boolean compatible = hot.isEmpty() || hot.getFluid().getFluid() == c.hot();
            mb = compatible ? Math.min(mb, CAPACITY - hot.getFluidAmount()) : 0;
        }
        return (long) mb * c.huPerMB();
    }

    /** Takes coolant for {@code heat} HU (rounded up to whole mB) and fills the hot tank. */
    public void commit(long heat) {
        lastAbsorbed = (int) Math.min(Integer.MAX_VALUE, Math.max(0, heat));
        Coolant c = active();
        if (c == null || heat <= 0) return;
        int mb = (int) Math.min(cold.getFluidAmount(), (heat + c.huPerMB() - 1) / c.huPerMB());
        cold.drain(mb, FluidAction.EXECUTE);
        if (c.hot() != null) hot.fill(new FluidStack(c.hot(), mb), FluidAction.EXECUTE);
        changed.run();
    }

    public int lastAbsorbed() { return lastAbsorbed; }
    public FluidStack coldFluid() { return cold.getFluid(); }
    public FluidStack hotFluid() { return hot.getFluid(); }

    // ---- IFluidHandler: tank 0 = coolant in (fill), tank 1 = hot coolant out (drain)
    @Override public int getTanks() { return 2; }
    @Override public FluidStack getFluidInTank(int tank) { return tank == 0 ? cold.getFluid() : hot.getFluid(); }
    @Override public int getTankCapacity(int tank) { return CAPACITY; }
    @Override public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0 && coolant(stack.getFluid()) != null; }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        int filled = cold.fill(resource, action);
        if (filled > 0 && action.execute()) changed.run();
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        FluidStack out = hot.drain(resource, action);
        if (!out.isEmpty() && action.execute()) changed.run();
        return out;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack out = hot.drain(maxDrain, action);
        if (!out.isEmpty() && action.execute()) changed.run();
        return out;
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.put("cold", cold.writeToNBT(provider, new CompoundTag()));
        tag.put("hot", hot.writeToNBT(provider, new CompoundTag()));
        tag.putInt("last", lastAbsorbed);
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider provider) {
        cold.readFromNBT(provider, tag.getCompound("cold"));
        hot.readFromNBT(provider, tag.getCompound("hot"));
        lastAbsorbed = tag.getInt("last");
    }
}

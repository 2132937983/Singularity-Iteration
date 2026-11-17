// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.menu.generator.SteamTurbineMenu;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.36: the steam turbine, the EU end of the steam reactor (IC2 Classic's steam turbine: at most 128 mB of steam a tick,
 * 64 EU/t, about 2 mB of steam per EU). It is the semifluid generator with steam for fuel: the same tank, cell slots, battery
 * slot, screen and grid behaviour (it turns steam into EU only while its buffer has room), at MV.
 */
public class mio_icif_steam_turbine extends mio_icif_Semifluid_generator {
    public static final int STEAM_CAPACITY = 16_000;
    /** mB of steam per EU. */
    public static final int STEAM_PER_EU = 2;
    public static final long MAX_OUTPUT = 64L;
    public static final long ENERGY_CAPACITY = 32_000L;
    /** 0.1.11.16: fuel is planned in units of 10 mB (EU per unit from the enthalpy drop). */
    public static final int MB_PER_UNIT = 10;

    // ---- 0.1.11.16: the enthalpy model (SteamPhysics) -----------------------------------------------------------------
    /** The exhaust goes to a working condenser beside it this tick (back pressure 0.1 bar) instead of the air (1 bar). */
    private boolean condensing;
    /** mB of exhaust the condenser took / mB vented on the last burning tick (for the screen). */
    private int lastCondensed, lastVented;

    public boolean isCondensing() { return condensing; }
    public int getLastCondensed() { return lastCondensed; }
    public int getLastVented() { return lastVented; }

    /** The steam state of a fluid (null for anything else). */
    public static com.miophas.singularity_iteration.common.energy.SteamPhysics.State stateOf(@Nullable Fluid fluid) {
        return steam(fluid) ? com.miophas.singularity_iteration.common.energy.SteamPhysics.STEAM
            : superheated(fluid) ? com.miophas.singularity_iteration.common.energy.SteamPhysics.SUPERHEATED : null;
    }

    public double backPressure() {
        return condensing ? com.miophas.singularity_iteration.common.energy.SteamPhysics.VACUUM : com.miophas.singularity_iteration.common.energy.SteamPhysics.ATMOSPHERE;
    }

    /** EU per mB of the steam in the tank now, as paid (the fuel value per 10 mB, rounded). */
    public double euPerMb() {
        if (fuelTank.isEmpty()) return 0;
        var value = fuelValue(fuelTank.getFluid().getFluid());
        return value == null ? 0 : value.energyPerMb() / (double) MB_PER_UNIT;
    }

    @Override
    protected void beforeBurn() {
        burnCondenser = condenser();
        condensing = burnCondenser != null;
        lastCondensed = lastVented = 0;
    }

    /** 0.1.11.22: the condenser found this tick (afterBurn reuses it) and the side it was last on. */
    @Nullable private IFluidHandler burnCondenser;
    @Nullable private net.minecraft.core.Direction condenserSide;

    /** A condenser beside it that takes steam now, or null. */
    @Nullable
    private IFluidHandler condenser() {
        if (level == null) return null;
        // 0.1.11.22: try the side the condenser was on first; elsewhere only the block is looked at, and the block
        // entity and its tank only on a condenser (it looked up six block entities twice a tick)
        if (condenserSide != null) {
            IFluidHandler h = condenserOn(condenserSide);
            if (h != null) return h;
            condenserSide = null;
        }
        for (net.minecraft.core.Direction side : net.minecraft.core.Direction.values()) {
            IFluidHandler h = condenserOn(side);
            if (h != null) {
                condenserSide = side;
                return h;
            }
        }
        return null;
    }

    @Nullable
    private IFluidHandler condenserOn(net.minecraft.core.Direction side) {
        var at = worldPosition.relative(side);
        if (!level.isLoaded(at) || !level.getBlockState(at).is(com.miophas.singularity_iteration.common.registry.mio_icif_blocks.CONDENSER.get())
                || !(level.getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_condenser)) return null;
        IFluidHandler handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, at, side.getOpposite());
        if (handler != null && handler.fill(new net.neoforged.neoforge.fluids.FluidStack(mio_icif_fluids.STEAM.get(), 1), IFluidHandler.FluidAction.SIMULATE) > 0) return handler;
        return null;
    }

    /** The exhaust (the same mass of low-pressure steam) goes into the condenser; what it cannot take is vented. */
    @Override
    protected void afterBurn(int millibuckets) {
        IFluidHandler handler = condensing ? burnCondenser : null;
        burnCondenser = null;
        int taken = handler == null ? 0 : handler.fill(new net.neoforged.neoforge.fluids.FluidStack(mio_icif_fluids.STEAM.get(), millibuckets),
            IFluidHandler.FluidAction.EXECUTE);
        lastCondensed = taken;
        lastVented = millibuckets - taken;
    }

    public mio_icif_steam_turbine(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.STEAM_TURBINE_ENTITY_TYPE.get(), STEAM_CAPACITY, MAX_OUTPUT, ENERGY_CAPACITY, 128L, CableTier.MV);
    }

    public static boolean steam(@Nullable Fluid fluid) {
        return fluid != null && fluid.isSame(mio_icif_fluids.STEAM.get());
    }

    public static boolean superheated(@Nullable Fluid fluid) {
        return fluid != null && fluid.isSame(mio_icif_fluids.SUPERHEATEDSTEAM.get());
    }

    @Override protected boolean acceptsFuel(@Nullable Fluid fluid) { return steam(fluid) || superheated(fluid); }
    /** 0.1.11.16: EU per 10 mB from the enthalpy drop to the back pressure (5 for steam into the air, as before). */
    @Override protected @Nullable SemifluidFuels.GeneratorValue fuelValue(Fluid fluid) {
        var state = stateOf(fluid);
        if (state == null) return null;
        double perMb = com.miophas.singularity_iteration.common.energy.SteamPhysics.euPerMb(state, backPressure(),
            com.miophas.singularity_iteration.common.energy.SteamPhysics.TURBINE_EFFICIENCY);
        return new SemifluidFuels.GeneratorValue(Math.max(1, Math.round(perMb * MB_PER_UNIT)), MAX_OUTPUT);
    }
    @Override protected int millibucketsPerUnit() { return MB_PER_UNIT; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mio_icif.generator.block_steam_turbine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SteamTurbineMenu(containerId, playerInventory, this);
    }

    public static final int DATA_COUNT = 15;

    /**
     * 0.1.11.16: menu data of the mimic screen: 0 steam mB, 1 capacity, 2 fluid id (-1 none), 3 output EU/t x100,
     * 4 EU per mB x1000, 5 condensing, 6 mB/t to the condenser, 7 mB/t vented, 8 exhaust quality x1000, 9 energy,
     * 10 capacity, 11 inlet °C, 12 inlet bar x10, 13 enthalpy drop kJ/kg, 14 back pressure bar x100.
     */
    public net.minecraft.world.inventory.ContainerData getTurbineData() {
        return new net.minecraft.world.inventory.ContainerData() {
            @Override public int getCount() { return DATA_COUNT; }
            @Override public void set(int i, int v) { }
            @Override public int get(int i) {
                var state = fuelTank.isEmpty() ? null : stateOf(fuelTank.getFluid().getFluid());
                double eta = com.miophas.singularity_iteration.common.energy.SteamPhysics.TURBINE_EFFICIENCY;
                return switch (i) {
                    case 0 -> fuelTank.getFluidAmount();
                    case 1 -> fuelTank.getCapacity();
                    case 2 -> fuelTank.isEmpty() ? -1 : net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(fuelTank.getFluid().getFluid());
                    case 3 -> (int) Math.round((lastCondensed + lastVented) * euPerMb() * outputScale() * 100);
                    case 4 -> (int) Math.round(euPerMb() * 1000);
                    case 5 -> condensing ? 1 : 0;
                    case 6 -> lastCondensed;
                    case 7 -> lastVented;
                    case 8 -> state == null ? 1000 : (int) Math.round(com.miophas.singularity_iteration.common.energy.SteamPhysics.exhaustQuality(state, backPressure(), eta) * 1000);
                    case 9 -> (int) Math.min(Integer.MAX_VALUE, energyStorage.getAmount());
                    case 10 -> (int) Math.min(Integer.MAX_VALUE, energyStorage.getCapacity());
                    case 11 -> state == null ? 0 : (int) Math.round(state.temperature());
                    case 12 -> state == null ? 0 : (int) Math.round(state.bar() * 10);
                    case 13 -> state == null ? 0 : (int) Math.round(com.miophas.singularity_iteration.common.energy.SteamPhysics.work(state, backPressure(), eta));
                    case 14 -> (int) Math.round(backPressure() * 100);
                    default -> 0;
                };
            }
        };
    }
}

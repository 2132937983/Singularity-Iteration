package com.miophas.singularity_iteration.core.runtime.fluid;

import com.miophas.singularity_iteration.core.api.fluid.IFluidHandlerAPI;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of the fluid handler API.
 */
public class FluidHandlerAPIImpl implements IFluidHandlerAPI {

    @Override
    public boolean hasFluidHandler(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        return world.getCapability(Capabilities.FluidHandler.BLOCK, pos, null) != null;
    }

    @Override
    public IFluidTankInfo getFluidTankInfo(Level world, BlockPos pos, int tank) {
        if (world.isClientSide) return null;

        IFluidHandler handler = world.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (handler == null || tank < 0 || tank >= handler.getTanks()) {
            return null;
        }

        return new FluidTankInfoImpl(tank, handler.getFluidInTank(tank), handler.getTankCapacity(tank));
    }

    @Override
    public Collection<IFluidTankInfo> getAllFluidTanks(Level world, BlockPos pos) {
        if (world.isClientSide) return Collections.emptyList();

        IFluidHandler handler = world.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (handler == null) return Collections.emptyList();

        List<IFluidTankInfo> tanks = new ArrayList<>();
        for (int i = 0; i < handler.getTanks(); i++) {
            tanks.add(new FluidTankInfoImpl(i, handler.getFluidInTank(i), handler.getTankCapacity(i)));
        }
        return tanks;
    }

    @Override
    public int getTankCount(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;

        IFluidHandler handler = world.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (handler == null) return 0;
        return handler.getTanks();
    }

    @Override
    public int fillFluid(Level world, BlockPos pos, int tank, FluidStack fluid, boolean simulate) {
        if (world.isClientSide) return 0;

        IFluidHandler handler = world.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (handler == null) return 0;
        if (tank < 0 || tank >= handler.getTanks()) return 0;

        // Check if the given tank can accept this fluid
        if (!handler.isFluidValid(tank, fluid)) {
            return 0;
        }

        // Note: the NeoForge IFluidHandler.fill() has no tank index parameter.
        // It sends the fluid to the first tank that accepts it.
        // This method cannot force the fill into a specific tank index.
        // For exact control of the target tank, use the NeoForge IFluidHandler capability directly
        // and write your own fill logic.
        return handler.fill(fluid, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public FluidStack drainFluid(Level world, BlockPos pos, int tank, int maxDrain, boolean simulate) {
        if (world.isClientSide) return FluidStack.EMPTY;

        IFluidHandler handler = world.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (handler == null) return FluidStack.EMPTY;

        FluidStack fluidInTank = handler.getFluidInTank(tank);
        if (fluidInTank.isEmpty()) return FluidStack.EMPTY;

        FluidStack toDrain = fluidInTank.copy();
        toDrain.setAmount(Math.min(maxDrain, fluidInTank.getAmount()));

        return handler.drain(toDrain, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
    }

    /**
     * Implementation of the fluid tank information.
     */
    private static class FluidTankInfoImpl implements IFluidTankInfo {
        private final int tankIndex;
        private final FluidStack fluid;
        private final int capacity;

        public FluidTankInfoImpl(int tankIndex, FluidStack fluid, int capacity) {
            this.tankIndex = tankIndex;
            this.fluid = fluid.copy();
            this.capacity = capacity;
        }

        @Override
        public int getTankIndex() {
            return tankIndex;
        }

        @Override
        public FluidStack getFluid() {
            return fluid;
        }

        @Override
        public int getAmount() {
            return fluid.getAmount();
        }

        @Override
        public int getCapacity() {
            return capacity;
        }
    }
}
package com.singularity_iteration.mio_icif.api.fluid;

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
 * 流体处理 API 实现
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

        // 检查指定 tank 是否可以接受此流体
        if (!handler.isFluidValid(tank, fluid)) {
            return 0;
        }

        // 注意：NeoForge 的 IFluidHandler.fill() 不接受 tank 索引参数
        // 它会自动将流体路由到第一个可接受的 tank
        // 此方法无法强制填充到特定 tank 索引
        // 如果需要精确控制填充目标槽位，请直接使用 NeoForge 的 IFluidHandler capability
        // 并实现自定义的填充逻辑
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
     * 流体槽信息实现
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
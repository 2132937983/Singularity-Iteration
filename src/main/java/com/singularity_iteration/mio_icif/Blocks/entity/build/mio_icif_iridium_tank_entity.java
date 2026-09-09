package com.singularity_iteration.mio_icif.Blocks.entity.build;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
public class mio_icif_iridium_tank_entity extends BlockEntity {

    public static final int TANK_CAPACITY = 1024000;

    protected final FluidTank fluidTank;

    public mio_icif_iridium_tank_entity(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.IRIDIUM_TANK.get(), pos, state);
        this.fluidTank = new FluidTank(TANK_CAPACITY) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_iridium_tank_entity entity) {
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    public List<mio_icif_iridium_tank_entity> getConnectedTanks() {
        List<mio_icif_iridium_tank_entity> tanks = new ArrayList<>();
        Level level = getLevel();
        if (level == null) {
            tanks.add(this);
            return tanks;
        }
        BlockPos checkPos = worldPosition;
        while (true) {
            BlockEntity be = level.getBlockEntity(checkPos.below());
            if (be instanceof mio_icif_iridium_tank_entity) {
                checkPos = checkPos.below();
            } else break;
        }
        while (true) {
            BlockEntity be = level.getBlockEntity(checkPos);
            if (be instanceof mio_icif_iridium_tank_entity tank) {
                tanks.add(tank);
                checkPos = checkPos.above();
            } else break;
        }
        return tanks;
    }

    public int getConnectedTankCount() {
        return getConnectedTanks().size();
    }

    public int getTotalCapacity() {
        return TANK_CAPACITY * getConnectedTankCount();
    }

    public int getTotalFluidAmount() {
        int total = 0;
        for (mio_icif_iridium_tank_entity tank : getConnectedTanks()) {
            total += tank.fluidTank.getFluidAmount();
        }
        return total;
    }

    @Nullable
    public FluidStack getCommonFluid() {
        FluidStack found = null;
        for (mio_icif_iridium_tank_entity tank : getConnectedTanks()) {
            FluidStack fs = tank.fluidTank.getFluid();
            if (fs.isEmpty()) continue;
            if (found == null) {
                found = fs;
            } else if (fs.getFluid() != found.getFluid()) {
                return null;
            }
        }
        return found;
    }

    public void onBlockPlaced(Level level) {
        if (level.isClientSide()) return;
        balanceTankFluids();
    }

    public void balanceTankFluids() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        List<mio_icif_iridium_tank_entity> tanks = getConnectedTanks();
        FluidStack commonFluid = null;
        for (mio_icif_iridium_tank_entity tank : tanks) {
            FluidStack held = tank.fluidTank.getFluid();
            if (held.isEmpty()) continue;
            if (commonFluid == null) {
                commonFluid = held.copy();
            } else if (held.getFluid() != commonFluid.getFluid()) {
                return;
            }
        }
        if (commonFluid == null) return;
        for (int i = 0; i < tanks.size() - 1; i++) {
            mio_icif_iridium_tank_entity current = tanks.get(i);
            mio_icif_iridium_tank_entity next = tanks.get(i + 1);
            int currentAmount = current.fluidTank.getFluidAmount();
            int nextAmount = next.fluidTank.getFluidAmount();
            if (nextAmount > 0 && currentAmount < TANK_CAPACITY) {
                int toMove = Math.min(nextAmount, TANK_CAPACITY - currentAmount);
                if (toMove > 0) {
                    current.fluidTank.fill(next.fluidTank.drain(toMove, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    public IFluidHandler getMultiTankHandler() {
        return new MultiTankHandler(this);
    }

    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return getMultiTankHandler();
    }

    private static class MultiTankHandler implements IFluidHandler {
        private final mio_icif_iridium_tank_entity root;
        private List<mio_icif_iridium_tank_entity> tanks;

        MultiTankHandler(mio_icif_iridium_tank_entity root) {
            this.root = root;
        }

        private List<mio_icif_iridium_tank_entity> resolveTanks() {
            if (tanks == null) {
                tanks = root.getConnectedTanks();
            }
            return tanks;
        }

        @Override
        public int getTanks() { return 1; }

        @Override
        public FluidStack getFluidInTank(int tank) {
            FluidStack common = root.getCommonFluid();
            if (common == null) return FluidStack.EMPTY;
            return common.copyWithAmount(root.getTotalFluidAmount());
        }

        @Override
        public int getTankCapacity(int tank) { return root.getTotalCapacity(); }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) { return true; }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty()) return 0;
            List<mio_icif_iridium_tank_entity> tanks = resolveTanks();
            for (mio_icif_iridium_tank_entity t : tanks) {
                FluidStack current = t.fluidTank.getFluid();
                if (!current.isEmpty() && current.getFluid() != resource.getFluid()) {
                    return 0;
                }
            }
            FluidStack toFill = resource.copy();
            int totalFilled = 0;
            for (mio_icif_iridium_tank_entity t : tanks) {
                int filled = t.fluidTank.fill(toFill, action);
                if (filled > 0) {
                    toFill.shrink(filled);
                    totalFilled += filled;
                    if (toFill.isEmpty()) break;
                }
            }
            return totalFilled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty()) return FluidStack.EMPTY;
            List<mio_icif_iridium_tank_entity> tanks = resolveTanks();
            List<mio_icif_iridium_tank_entity> reversed = new ArrayList<>(tanks);
            java.util.Collections.reverse(reversed);
            FluidStack totalDrained = null;
            int remaining = resource.getAmount();
            for (mio_icif_iridium_tank_entity t : reversed) {
                if (remaining <= 0) break;
                FluidStack toDrain = resource.copyWithAmount(remaining);
                FluidStack drained = t.fluidTank.drain(toDrain, action);
                if (!drained.isEmpty()) {
                    if (totalDrained == null) {
                        totalDrained = drained.copy();
                        totalDrained.setAmount(0);
                    }
                    totalDrained.grow(drained.getAmount());
                    remaining -= drained.getAmount();
                }
            }
            return totalDrained != null ? totalDrained : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) return FluidStack.EMPTY;
            List<mio_icif_iridium_tank_entity> tanks = resolveTanks();
            List<mio_icif_iridium_tank_entity> reversed = new ArrayList<>(tanks);
            java.util.Collections.reverse(reversed);
            FluidStack totalDrained = null;
            int remaining = maxDrain;
            for (mio_icif_iridium_tank_entity t : reversed) {
                if (remaining <= 0) break;
                FluidStack drained = t.fluidTank.drain(remaining, action);
                if (!drained.isEmpty()) {
                    if (totalDrained == null) {
                        totalDrained = drained.copy();
                        totalDrained.setAmount(0);
                    }
                    totalDrained.grow(drained.getAmount());
                    remaining -= drained.getAmount();
                }
            }
            return totalDrained != null ? totalDrained : FluidStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag fluidTag = new CompoundTag();
        fluidTank.writeToNBT(registries, fluidTag);
        tag.put("FluidTank", fluidTag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("FluidTank")) {
            CompoundTag fluidTag = tag.getCompound("FluidTank");
            fluidTank.readFromNBT(registries, fluidTag);
        }
    }
}
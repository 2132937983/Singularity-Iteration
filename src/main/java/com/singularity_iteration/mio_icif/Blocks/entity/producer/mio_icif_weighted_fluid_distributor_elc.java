package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_weighted_fluid_distributor_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Menu.Producer.WeightedFluidDistributorElcMenu;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
public class mio_icif_weighted_fluid_distributor_elc extends mio_icif_producer {

    /**
 * 加权流体分配机方块实体
 * 参考IC2: 使用 FLUID_INPUT/FLUID_OUTPUT 槽类型(原版继承父类InvSlotConsumableLiquidByTank/InvSlotOutput)
 */
    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .fluidInput(1)
        .fluidOutput(1)
        .build();

    public static final long DEFAULT_CAPACITY = 0L;
    public static final long DEFAULT_MAX_RECEIVE = 0L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_COOK_TIME = 1;
    public static final long DEFAULT_ENERGY_PER_TICK = 0L;
    // 参考IC2: 高级流体分配机继承自流体分配机的1000mB液槽 (原版TileEntityWeightedFluidDistributor继承父类1000mB)
    public static final int FLUID_TANK_CAPACITY = 1000;

    private final FluidTank fluidTank = new FluidTank(FLUID_TANK_CAPACITY);
    private List<Direction> priority = new ArrayList<>();

    public mio_icif_weighted_fluid_distributor_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.WEIGHTED_FLUID_DISTRIBUTOR_ELC.get());
    }

    public mio_icif_weighted_fluid_distributor_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_COOK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.LV);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        fluidTank.writeToNBT(registries, tag);
        if (!priority.isEmpty()) {
            tag.putIntArray("priority", priority.stream().mapToInt(Direction::get3DDataValue).toArray());
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluidTank.readFromNBT(registries, tag);
        priority.clear();
        if (tag.contains("priority")) {
            int[] indexes = tag.getIntArray("priority");
            for (int index : indexes) {
                Direction d = Direction.from3DDataValue(index);
                if (d != null && d != getFacing()) {
                    priority.add(d);
                }
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        fluidTank.writeToNBT(registries, tag);
        if (!priority.isEmpty()) {
            tag.putIntArray("priority", priority.stream().mapToInt(Direction::get3DDataValue).toArray());
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        fluidTank.readFromNBT(registries, tag);
        priority.clear();
        if (tag.contains("priority")) {
            int[] indexes = tag.getIntArray("priority");
            for (int index : indexes) {
                Direction d = Direction.from3DDataValue(index);
                if (d != null) priority.add(d);
            }
        }
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING)) {
            return state.getValue(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public IFluidHandler getFluidHandlerCapability(@org.jetbrains.annotations.Nullable Direction side) {
        if (side == null) return fluidTank;
        Direction facing = getFacing();
        if (side == facing) {
            return new SidedFluidHandler(fluidTank, true, false);
        }
        if (priority.contains(side)) {
            return new SidedFluidHandler(fluidTank, false, true);
        }
        return null;
    }

    public List<Direction> getPriority() {
        return priority;
    }

    public void setPriority(List<Direction> priorities) {
        this.priority.clear();
        Direction facing = getFacing();
        for (Direction d : priorities) {
            if (d != facing) {
                this.priority.add(d);
            }
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState oldState = getBlockState();
            level.sendBlockUpdated(worldPosition, oldState, oldState, 3);
        }
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return new int[]{0, 1};
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        // FLUID_INPUT(0): 仅接受流体容器 FLUID_OUTPUT(1): 允许外部插入
        if (slot == 0) return stack.getCapability(Capabilities.FluidHandler.ITEM) != null;
        return false;
    }

    @Override
    protected boolean canWork() {
        if (energyStorage.getAmount() < energyPerTick) return false;
        return !priority.isEmpty() || hasFluidContainerToProcess();
    }

    private boolean hasFluidContainerToProcess() {
        net.neoforged.neoforge.items.IItemHandler handler = getItemHandler();
        ItemStack inputStack = handler.getStackInSlot(WeightedFluidDistributorElcMenu.FLUID_INPUT_SLOT);
        if (inputStack.isEmpty()) return false;
        return inputStack.getCapability(Capabilities.FluidHandler.ITEM) != null;
    }

    /**
     * 对应IC2: 仅罐→物品方法（processFromTank, OpType.Fill）
     * 罐为空或容器已满时不处理
     */
    private void processFluidContainer() {
        net.neoforged.neoforge.items.IItemHandler handler = getItemHandler();
        ItemStack inputStack = handler.getStackInSlot(WeightedFluidDistributorElcMenu.FLUID_INPUT_SLOT);
        ItemStack outputStack = handler.getStackInSlot(WeightedFluidDistributorElcMenu.FLUID_OUTPUT_SLOT);

        if (inputStack.isEmpty()) return;

        IFluidHandlerItem fluidHandler = inputStack.getCapability(Capabilities.FluidHandler.ITEM);
        if (fluidHandler == null) return;

        // 仅当罐有流体 且 容器为空气时，从罐装进容器
        if (fluidTank.getFluid().isEmpty()) return;
        if (!fluidHandler.getFluidInTank(0).isEmpty()) return;

        FluidStack toFill = fluidTank.getFluid().copy();
        int maxFill = fluidHandler.fill(toFill, IFluidHandler.FluidAction.SIMULATE);
        if (maxFill <= 0) return;
        if (!outputStack.isEmpty()) return;

        FluidStack drained = fluidTank.drain(maxFill, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return;

        fluidHandler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (handler instanceof ItemStackHandler h) {
            h.setStackInSlot(WeightedFluidDistributorElcMenu.FLUID_INPUT_SLOT, ItemStack.EMPTY);
            h.setStackInSlot(WeightedFluidDistributorElcMenu.FLUID_OUTPUT_SLOT, fluidHandler.getContainer());
        }
    }

    @Override
    protected void doWork() {
        processFluidContainer();

        if (priority.isEmpty() || fluidTank.getFluidAmount() <= 0) {
            return;
        }

        boolean didWork = false;
        for (Direction dir : priority) {
            IFluidHandler target = getAdjacentFluidHandler(worldPosition.relative(dir), dir.getOpposite());
            if (target == null) continue;

            FluidStack toDrain = fluidTank.getFluid().copy();
            int filled = target.fill(toDrain, IFluidHandler.FluidAction.SIMULATE);
            if (filled > 0) {
                FluidStack drained = fluidTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                if (!drained.isEmpty()) {
                    target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                    didWork = true;
                }
                if (fluidTank.getFluidAmount() <= 0) break;
            }
        }

        if (getBlockState().getValue(mio_icif_block_weighted_fluid_distributor_elc.LIT) != didWork) {
            if (level != null && !level.isClientSide) {
                BlockState oldState = getBlockState();
                BlockState newState = oldState.setValue(mio_icif_block_weighted_fluid_distributor_elc.LIT, didWork);
                level.setBlockAndUpdate(worldPosition, newState);
                level.sendBlockUpdated(worldPosition, oldState, newState, 3);
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_weighted_fluid_distributor_elc blockEntity) {
        mio_icif_producer.tick(level, pos, state, blockEntity);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.weighted_fluid_distributor_elc");
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
        return new WeightedFluidDistributorElcMenu(containerId, playerInventory, this);
    }

    private static class SidedFluidHandler implements IFluidHandler {
        private final FluidTank tank;
        private final boolean canFill;
        private final boolean canDrain;

        SidedFluidHandler(FluidTank tank, boolean canFill, boolean canDrain) {
            this.tank = tank;
            this.canFill = canFill;
            this.canDrain = canDrain;
        }

        @Override
        public int getTanks() { return tank.getTanks(); }

        @Override
        public FluidStack getFluidInTank(int tank) { return this.tank.getFluidInTank(tank); }

        @Override
        public int getTankCapacity(int tank) { return this.tank.getTankCapacity(tank); }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) { return this.tank.isFluidValid(tank, stack); }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!canFill) return 0;
            return tank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!canDrain) return FluidStack.EMPTY;
            return tank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!canDrain) return FluidStack.EMPTY;
            return tank.drain(maxDrain, action);
        }
    }
}
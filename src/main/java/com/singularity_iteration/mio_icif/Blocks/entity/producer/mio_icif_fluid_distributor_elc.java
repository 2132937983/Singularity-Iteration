package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_fluid_distributor_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Menu.Producer.FluidDistributorElcMenu;
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

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("null")
public class mio_icif_fluid_distributor_elc extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .fluidInput(1)
        .fluidOutput(1)
        .build();

    public static final int FLUID_INPUT_SLOT = 0;
    public static final int FLUID_OUTPUT_SLOT = 1;

    public static final long DEFAULT_CAPACITY = 0L;
    public static final long DEFAULT_MAX_RECEIVE = 0L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_COOK_TIME = 10;
    public static final long DEFAULT_ENERGY_PER_TICK = 0L;
    public static final int FLUID_TANK_CAPACITY = 1000;

    private boolean active = false;
    private final FluidTank fluidTank = new FluidTank(FLUID_TANK_CAPACITY);

    public boolean isActive() {
        return active;
    }

    public void toggleMode() {
        this.active = !this.active;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState oldState = getBlockState();
            BlockState newState = oldState.setValue(mio_icif_block_fluid_distributor_elc.LIT, active);
            level.setBlockAndUpdate(worldPosition, newState);
            level.sendBlockUpdated(worldPosition, oldState, newState, 3);
        }
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    private boolean isInputSide(@org.jetbrains.annotations.Nullable Direction side) {
        if (side == null) return true;
        Direction facing = getFacing();
        if (active) {
            return side != facing;
        } else {
            return side == facing;
        }
    }

    @Override
    public net.neoforged.neoforge.fluids.capability.IFluidHandler getFluidHandlerCapability(@org.jetbrains.annotations.Nullable Direction side) {
        if (isInputSide(side)) {
            return new SidedFluidHandler(fluidTank, true, false);
        }
        return null;
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING)) {
            return state.getValue(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        }
        return Direction.NORTH;
    }

    public mio_icif_fluid_distributor_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.FLUID_DISTRIBUTOR_ELC.get());
    }

    public mio_icif_fluid_distributor_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
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
        tag.putBoolean("active", active);
        fluidTank.writeToNBT(registries, tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        active = tag.getBoolean("active");
        fluidTank.readFromNBT(registries, tag);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("active", active);
        fluidTank.writeToNBT(registries, tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("active")) {
            active = tag.getBoolean("active");
        }
        fluidTank.readFromNBT(registries, tag);
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return new int[]{FLUID_INPUT_SLOT, FLUID_OUTPUT_SLOT};
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == FLUID_INPUT_SLOT) return stack.getCapability(Capabilities.FluidHandler.ITEM) != null;
        return false;
    }

    @Override
    protected boolean canWork() {
        return true;
    }

    @Override
    protected void doWork() {
        int oldFluidAmount = fluidTank.getFluidAmount();

        processInputSlot();

        if (fluidTank.getFluidAmount() > 0) {
            moveFluid();
        }

        // 坌步浝体状思到客户端（原版水的纹睆坯以坳时渲染，但自定义浝体需覝手动坌步）
        if (fluidTank.getFluidAmount() != oldFluidAmount) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    }

    /**
     * 对应IC2: inputSlot OpType.Fill + processFromTank — 仅从罐装流体进容器（罐→物品）
     * 罐由管道注入, 容器是装流体的工具
     */
    private void processInputSlot() {
        ItemStack inputStack = itemHandler.getStackInSlot(FLUID_INPUT_SLOT);
        if (inputStack.isEmpty()) return;

        IFluidHandlerItem itemFluidHandler = inputStack.getCapability(Capabilities.FluidHandler.ITEM);
        if (itemFluidHandler == null) return;

        if (fluidTank.getFluidAmount() <= 0) return;

        ItemStack outputStack = itemHandler.getStackInSlot(FLUID_OUTPUT_SLOT);

        FluidStack tankFluid = fluidTank.getFluid();
        FluidStack simulatedDrain = fluidTank.drain(tankFluid.getAmount(), IFluidHandler.FluidAction.SIMULATE);
        if (simulatedDrain.isEmpty()) return;

        int fillAmount = itemFluidHandler.fill(simulatedDrain, IFluidHandler.FluidAction.SIMULATE);
        if (fillAmount <= 0) return;

        FluidStack drained = fluidTank.drain(fillAmount, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return;

        itemFluidHandler.fill(drained, IFluidHandler.FluidAction.EXECUTE);

        ItemStack containerResult = itemFluidHandler.getContainer();
        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(FLUID_INPUT_SLOT, ItemStack.EMPTY);
            itemHandler.setStackInSlot(FLUID_OUTPUT_SLOT, containerResult);
        } else if (ItemStack.isSameItemSameComponents(outputStack, containerResult) && outputStack.getCount() < outputStack.getMaxStackSize()) {
            itemHandler.setStackInSlot(FLUID_INPUT_SLOT, ItemStack.EMPTY);
            outputStack.grow(1);
            itemHandler.setStackInSlot(FLUID_OUTPUT_SLOT, outputStack);
        }
    }

    private void moveFluid() {
        if (active) {
            Direction facing = getFacing();
            IFluidHandler target = getAdjacentFluidHandler(worldPosition.relative(facing), facing.getOpposite());
            if (target != null) {
                FluidStack fluid = fluidTank.getFluid();
                int amount = target.fill(fluid, IFluidHandler.FluidAction.SIMULATE);
                if (amount > 0) {
                    FluidStack drained = fluidTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
                    if (!drained.isEmpty()) {
                        target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                    }
                }
            }
        } else {
            Map<Direction, IFluidHandler> acceptingNeighbors = new EnumMap<>(Direction.class);
            int acceptedVolume = 0;
            Direction facing = getFacing();

            for (Direction dir : Direction.values()) {
                if (dir == facing) continue;
                IFluidHandler target = getAdjacentFluidHandler(worldPosition.relative(dir), dir.getOpposite());
                if (target != null) {
                    FluidStack fluid = fluidTank.getFluid();
                    int amount = target.fill(fluid, IFluidHandler.FluidAction.SIMULATE);
                    if (amount > 0) {
                        acceptingNeighbors.put(dir, target);
                        acceptedVolume += amount;
                    }
                }
            }

            while (!acceptingNeighbors.isEmpty()) {
                int amount = Math.min(acceptedVolume, fluidTank.getFluidAmount());
                if (amount <= 0) break;
                amount /= acceptingNeighbors.size();

                if (amount > 0) {
                    Iterator<Map.Entry<Direction, IFluidHandler>> it = acceptingNeighbors.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<Direction, IFluidHandler> entry = it.next();
                        IFluidHandler target = entry.getValue();
                        FluidStack fluid = fluidTank.getFluid();
                        if (fluid.isEmpty()) break;
                        FluidStack copy = fluid.copy();
                        copy.setAmount(Math.min(amount, fluid.getAmount()));
                        int cAmount = target.fill(copy, IFluidHandler.FluidAction.EXECUTE);
                        fluidTank.drain(cAmount, IFluidHandler.FluidAction.EXECUTE);
                        acceptedVolume -= cAmount;
                        if (cAmount < copy.getAmount()) it.remove();
                    }
                    continue;
                }

                for (Map.Entry<Direction, IFluidHandler> entry : acceptingNeighbors.entrySet()) {
                    IFluidHandler target = entry.getValue();
                    FluidStack fluid = fluidTank.getFluid();
                    if (fluid.isEmpty()) break;
                    FluidStack copy = fluid.copy();
                    copy.setAmount(Math.min(acceptedVolume, fluid.getAmount()));
                    if (copy.getAmount() <= 0) break;
                    int cAmount = target.fill(copy, IFluidHandler.FluidAction.EXECUTE);
                    fluidTank.drain(cAmount, IFluidHandler.FluidAction.EXECUTE);
                    acceptedVolume -= cAmount;
                }
                break;
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_fluid_distributor_elc blockEntity) {
        mio_icif_producer.tick(level, pos, state, blockEntity);

        boolean shouldBeLit = blockEntity.active;
        if (state.getValue(mio_icif_block_fluid_distributor_elc.LIT) != shouldBeLit) {
            level.setBlock(pos, state.setValue(mio_icif_block_fluid_distributor_elc.LIT, shouldBeLit), 3);
        }
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.fluid_distributor_elc");
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
        return new FluidDistributorElcMenu(containerId, playerInventory, this);
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
package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_fluid_regulator_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Menu.Producer.FluidRegulatorElcMenu;
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
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

@SuppressWarnings("null")
public class mio_icif_fluid_regulator_elc extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .battery()
        .fluidInput(1)
        .fluidOutput(1)
        .build();

    public static final int BATTERY_SLOT = 0;
    public static final int FLUID_INPUT_SLOT = 1;
    public static final int FLUID_OUTPUT_SLOT = 2;

    public static final long DEFAULT_CAPACITY = 10000L;
    public static final long DEFAULT_MAX_RECEIVE = 512L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_COOK_TIME = 1;
    public static final long DEFAULT_ENERGY_PER_TICK = 0L;
    public static final int FLUID_TANK_CAPACITY = 10000;
    public static final int MAX_FLOW_RATE = 1000;
    public static final int ENERGY_PER_OPERATION = 10;
    public static final int TICK_RATE = 20;

    private int outputmb = 0;
    private int mode = 0;
    private int updateTicker = 0;
    private boolean newActive = false;

    private final FluidTank fluidTank = new FluidTank(FLUID_TANK_CAPACITY);

    public int getOutputmb() {
        return outputmb;
    }

    public int getMode() {
        return mode;
    }

    public String getModeGui() {
        return mode == 0 ? "ic2.generic.text.sec" : "ic2.generic.text.tick";
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    public void onNetworkEvent(int event) {
        if (event == 1001 || event == 1002) {
            if (event == 1001 && mode == 0) mode = 1;
            if (event == 1002 && mode == 1) mode = 0;
            setChanged();
            return;
        }
        outputmb += event;
        if (outputmb > MAX_FLOW_RATE) outputmb = MAX_FLOW_RATE;
        if (outputmb < 0) outputmb = 0;
        setChanged();
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING)) {
            return state.getValue(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public net.neoforged.neoforge.fluids.capability.IFluidHandler getFluidHandlerCapability(@org.jetbrains.annotations.Nullable Direction side) {
        if (side == null) return fluidTank;
        Direction facing = getFacing();
        if (side == facing) return null;
        return new SidedFluidHandler(fluidTank, true, false);
    }

    public mio_icif_fluid_regulator_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.FLUID_REGULATOR_ELC.get());
    }

    public mio_icif_fluid_regulator_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_COOK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.EV);
        this.updateTicker = (int) (Math.random() * TICK_RATE);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("outputmb", outputmb);
        tag.putInt("mode", mode);
        fluidTank.writeToNBT(registries, tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        outputmb = tag.contains("outputmb") ? tag.getInt("outputmb") : 0;
        mode = tag.contains("mode") ? tag.getInt("mode") : 0;
        fluidTank.readFromNBT(registries, tag);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("outputmb", outputmb);
        tag.putInt("mode", mode);
        fluidTank.writeToNBT(registries, tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("outputmb")) outputmb = tag.getInt("outputmb");
        if (tag.contains("mode")) mode = tag.getInt("mode");
        fluidTank.readFromNBT(registries, tag);
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return new int[]{BATTERY_SLOT};
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) return isBattery(stack);
        if (slot == FLUID_INPUT_SLOT) return stack.getCapability(Capabilities.FluidHandler.ITEM) != null;
        return false;
    }

    @Override
    protected boolean canWork() {
        return true;
    }

    @Override
    protected void doWork() {
        processInputSlot();

        if (updateTicker++ % TICK_RATE != 0 && mode == 0) return;

        newActive = work();
        if (getBlockState().getValue(mio_icif_block_fluid_regulator_elc.LIT) != newActive) {
            if (level != null && !level.isClientSide) {
                BlockState oldState = getBlockState();
                BlockState newState = oldState.setValue(mio_icif_block_fluid_regulator_elc.LIT, newActive);
                level.setBlockAndUpdate(worldPosition, newState);
                level.sendBlockUpdated(worldPosition, oldState, newState, 3);
            }
        }
    }

    private boolean work() {
        if (outputmb == 0) return false;
        if (energyStorage.getAmount() < ENERGY_PER_OPERATION) return false;
        if (fluidTank.getFluidAmount() <= 0) return false;

        Direction facing = getFacing();
        IFluidHandler target = getAdjacentFluidHandler(worldPosition.relative(facing), facing.getOpposite());
        if (target == null) return false;

        FluidStack toDrain = fluidTank.drain(outputmb, IFluidHandler.FluidAction.SIMULATE);
        if (toDrain.isEmpty()) return false;

        int amount = target.fill(toDrain, IFluidHandler.FluidAction.SIMULATE);
        if (amount <= 0) return false;

        FluidStack drained = fluidTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        if (!drained.isEmpty()) {
            target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            apiUseEnergy(ENERGY_PER_OPERATION, false);
            return true;
        }
        return false;
    }

    private void processInputSlot() {
        ItemStack inputStack = itemHandler.getStackInSlot(FLUID_INPUT_SLOT);
        if (inputStack.isEmpty()) return;

        IFluidHandler itemFluidHandler = inputStack.getCapability(Capabilities.FluidHandler.ITEM);
        if (itemFluidHandler == null) return;

        ItemStack outputStack = itemHandler.getStackInSlot(FLUID_OUTPUT_SLOT);

        FluidStack itemFluid = itemFluidHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (itemFluid.isEmpty()) return;

        int fillAmount = fluidTank.fill(itemFluid, IFluidHandler.FluidAction.SIMULATE);
        if (fillAmount <= 0) return;

        FluidStack drained = itemFluidHandler.drain(fillAmount, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return;

        fluidTank.fill(drained, IFluidHandler.FluidAction.EXECUTE);

        ItemStack containerResult = inputStack.copy();
        if (inputStack.getCount() == 1) {
            if (outputStack.isEmpty()) {
                itemHandler.setStackInSlot(FLUID_INPUT_SLOT, ItemStack.EMPTY);
                itemHandler.setStackInSlot(FLUID_OUTPUT_SLOT, containerResult);
            } else if (ItemStack.isSameItemSameComponents(outputStack, containerResult) && outputStack.getCount() < outputStack.getMaxStackSize()) {
                itemHandler.setStackInSlot(FLUID_INPUT_SLOT, ItemStack.EMPTY);
                outputStack.grow(1);
                itemHandler.setStackInSlot(FLUID_OUTPUT_SLOT, outputStack);
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_fluid_regulator_elc blockEntity) {
        mio_icif_producer.tick(level, pos, state, blockEntity);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.fluid_regulator_elc");
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
        return new FluidRegulatorElcMenu(containerId, playerInventory, this);
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
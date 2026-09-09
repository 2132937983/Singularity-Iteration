package com.singularity_iteration.mio_icif.Blocks.entity.HUEntity;

import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.api.tool.IWrenchable;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotType;
import com.singularity_iteration.mio_icif.Items.Upgrade.MachineUpgradeStats;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("null")
public class mio_icif_HeatU_Block extends BlockEntity implements MenuProvider, IMioIcifCapabilities.IHeatStorage, IWrenchable {
    
    protected final IMioIcifCapabilities.IHeatStorage heatStorage;
    protected final int baseHeatCapacity;

    protected SlotLayout slotLayout;
    protected MachineItemHandler itemHandler;

protected MachineUpgradeStats upgradeStats = MachineUpgradeStats.empty();

    public mio_icif_HeatU_Block(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, 10000, 100, 100, 20, 1000, 0.01f);
    }
    
    public mio_icif_HeatU_Block(BlockEntityType<?> type, BlockPos pos, BlockState state,
                               int capacity, int maxReceive, int maxExtract,
                               int baseTemp, int maxTemp, float lossFactor) {
        super(type, pos, state);
        this.heatStorage = MioIcifAPI.instance().getCapabilities().createHeatStorage(
            capacity, maxReceive, maxExtract, baseTemp, maxTemp, lossFactor);
        this.baseHeatCapacity = capacity;
    }

    public mio_icif_HeatU_Block(BlockEntityType<?> type, BlockPos pos, BlockState state,
                               SlotLayout layout,
                               int capacity, int maxReceive, int maxExtract,
                               int baseTemp, int maxTemp, float lossFactor) {
        super(type, pos, state);
        this.heatStorage = MioIcifAPI.instance().getCapabilities().createHeatStorage(
            capacity, maxReceive, maxExtract, baseTemp, maxTemp, lossFactor);
        this.baseHeatCapacity = capacity;
        this.slotLayout = layout;
        this.itemHandler = createItemHandler(layout);
    }
    
    public IMioIcifCapabilities.IHeatStorage getHeatStorage() {
        return heatStorage;
    }

    @Override
    public long receiveHeat(long toReceive, boolean simulate) {
        return heatStorage.receiveHeat(toReceive, simulate);
    }

    @Override
    public long extractHeat(long toExtract, boolean simulate) {
        return heatStorage.extractHeat(toExtract, simulate);
    }

    @Override
    public long getHeatStored() {
        return heatStorage.getHeatStored();
    }

    @Override
    public long getMaxHeatStored() {
        return heatStorage.getMaxHeatStored();
    }

    @Override
    public boolean canExtractHeat() {
        return heatStorage.canExtractHeat();
    }

    @Override
    public boolean canReceiveHeat() {
        return heatStorage.canReceiveHeat();
    }

    @Override
    public int getTemperature() {
        return heatStorage.getTemperature();
    }

    @Override
    public boolean isOverheated() {
        return heatStorage.isOverheated();
    }

    @Override
    public long getHeatLossPerTick() {
        return heatStorage.getHeatLossPerTick();
    }

    @Override
    public long getMaxReceive() {
        return heatStorage.getMaxReceive();
    }

    @Override
    public long getMaxExtract() {
        return heatStorage.getMaxExtract();
    }

    @Override
    public void setHeat(long heat) {
        heatStorage.setHeat(heat);
    }

    @Override
    public void setCapacity(long capacity) {
        heatStorage.setCapacity(capacity);
    }

    @Override
    public long applyHeatLoss() {
        return heatStorage.applyHeatLoss();
    }

    @Override
    public long consumeHeatInternal(long amount, boolean simulate) {
        return heatStorage.consumeHeatInternal(amount, simulate);
    }

    @Override
    public long generateHeatInternal(long amount, boolean simulate) {
        return heatStorage.generateHeatInternal(amount, simulate);
    }

    @Nullable
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    protected MachineItemHandler createItemHandler(SlotLayout layout) {
        MachineItemHandler handler = new MachineItemHandler(layout) {
            @Override
            protected void onContentsChanged(int slot) {
                mio_icif_HeatU_Block.this.setChanged();
            }
        };
        return handler;
    }

    public boolean isUpgradeSlot(int slot) {
        return slotLayout != null && slotLayout.isType(slot, SlotType.UPGRADE);
    }

    protected void recalculateUpgradeStats() {
        if (slotLayout == null || itemHandler == null) {
            this.upgradeStats = MachineUpgradeStats.empty();
            return;
        }
        int upgradeStart = slotLayout.getStart(SlotType.UPGRADE);
        int upgradeCount = slotLayout.getCount(SlotType.UPGRADE);
        this.upgradeStats = MachineUpgradeStats.fromInventory(itemHandler, upgradeStart, upgradeCount);
        int upgradedCapacity = baseHeatCapacity + (int) (upgradeStats.energyStorageCount * MachineUpgradeStats.ENERGY_STORAGE_BONUS);
        if (heatStorage.getMaxHeatStored() != upgradedCapacity) {
            heatStorage.setCapacity(upgradedCapacity);
        }
    }

    public MachineUpgradeStats getUpgradeStats() {
        return upgradeStats;
    }

    protected int getProcessingSpeedMultiplier() {
        return Math.max(1, (int) Math.ceil(1.0 / upgradeStats.getProcessTimeMultiplier()));
    }

    protected double getProcessingCostMultiplier() {
        return upgradeStats.getEnergyUsageMultiplier();
    }

    public int getUpgradeSlotStart() {
        return slotLayout != null ? slotLayout.getStart(SlotType.UPGRADE) : -1;
    }

    public int getUpgradeSlotCount() {
        return slotLayout != null ? slotLayout.getCount(SlotType.UPGRADE) : 0;
    }

    protected boolean canWorkRedstone() {
        if (level == null) {
            return true;
        }
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (upgradeStats.redstoneInverted) {
            return powered;
        }
        return !powered;
    }

    protected void handleAutomationUpgrades() {
        if (level == null || level.isClientSide) {
            return;
        }
        recalculateUpgradeStats();
        if (upgradeStats.ejectorCount > 0) {
            ejectItems(upgradeStats.ejectorCount);
        }
        if (upgradeStats.pullingCount > 0) {
            pullItems(upgradeStats.pullingCount);
        }
        IFluidHandler own = getFluidHandlerCapability(null);
        if (own != null) {
            if (upgradeStats.fluidEjectorCount > 0) {
                ejectFluids(own, upgradeStats.fluidEjectorCount);
            }
            if (upgradeStats.fluidPullingCount > 0) {
                pullFluids(own, upgradeStats.fluidPullingCount);
            }
        }
    }

    protected int[] getOutputSlots() {
        return slotLayout != null ? slotLayout.getSlotsOfType(SlotType.OUTPUT) : new int[0];
    }

    protected int[] getInputSlots() {
        return slotLayout != null ? slotLayout.getSlotsOfType(SlotType.INPUT) : new int[0];
    }

    protected void ejectItems(int upgradeCount) {
        int maxPerTick = Math.max(1, upgradeCount);
        List<Direction> configuredDirections = upgradeStats.getEjectorDirections();
        Iterable<Direction> targetDirections = !configuredDirections.isEmpty()
            ? configuredDirections : Arrays.asList(Direction.values());

        for (int outputSlot : getOutputSlots()) {
            ItemStack stack = itemHandler.getStackInSlot(outputSlot);
            if (stack.isEmpty()) {
                continue;
            }

            for (Direction direction : targetDirections) {
                IItemHandler target = getAdjacentItemHandler(worldPosition.relative(direction), direction.getOpposite());
                if (target == null) {
                    continue;
                }

                int moveCount = Math.min(stack.getCount(), maxPerTick);
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, stack.copyWithCount(moveCount), false);
                int moved = moveCount - remainder.getCount();
                if (moved <= 0) {
                    continue;
                }

                itemHandler.extractItem(outputSlot, moved, false);
                stack = itemHandler.getStackInSlot(outputSlot);
                if (stack.isEmpty()) {
                    break;
                }
            }
        }
    }

    protected void pullItems(int upgradeCount) {
        int maxPerTick = Math.max(1, upgradeCount);
        List<Direction> configuredDirections = upgradeStats.getPullingDirections();
        Iterable<Direction> sourceDirections = !configuredDirections.isEmpty()
            ? configuredDirections : Arrays.asList(Direction.values());

        for (int inputSlot : getInputSlots()) {
            ItemStack current = itemHandler.getStackInSlot(inputSlot);
            if (current.getCount() >= itemHandler.getSlotLimit(inputSlot)) {
                continue;
            }

            for (Direction direction : sourceDirections) {
                IItemHandler source = getAdjacentItemHandler(worldPosition.relative(direction), direction.getOpposite());
                if (source == null) {
                    continue;
                }

                boolean pulled = false;
                for (int sourceSlot = 0; sourceSlot < source.getSlots(); sourceSlot++) {
                    ItemStack sourceStack = source.getStackInSlot(sourceSlot);
                    if (sourceStack.isEmpty() || (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, sourceStack))) {
                        continue;
                    }

                    ItemStack simulatedExtract = source.extractItem(sourceSlot, Math.min(sourceStack.getCount(), maxPerTick), true);
                    if (simulatedExtract.isEmpty()) {
                        continue;
                    }

                    ItemStack simulatedRemainder = itemHandler.insertItem(inputSlot, simulatedExtract, true);
                    int accepted = simulatedExtract.getCount() - simulatedRemainder.getCount();
                    if (accepted <= 0) {
                        continue;
                    }

                    ItemStack extracted = source.extractItem(sourceSlot, accepted, false);
                    if (!extracted.isEmpty()) {
                        itemHandler.insertItem(inputSlot, extracted, false);
                        pulled = true;
                        break;
                    }
                }

                if (pulled) {
                    break;
                }
            }
        }
    }

    @Nullable
    protected IItemHandler getAdjacentItemHandler(BlockPos pos, @Nullable Direction side) {
        if (level == null || level.getBlockEntity(pos) == null) {
            return null;
        }
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
    }

    @Nullable
    protected IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return null;
    }

    @Nullable
    protected IFluidHandler getAdjacentFluidHandler(BlockPos pos, @Nullable Direction side) {
        if (level == null) {
            return null;
        }
        BlockEntity target = level.getBlockEntity(pos);
        if (target == null) {
            return null;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
    }

    protected void ejectFluids(IFluidHandler own, int upgradeCount) {
        int maxPerTick = Math.max(1, upgradeCount * 1000);
        List<Direction> configuredDirections = upgradeStats.getFluidEjectorDirections();
        Iterable<Direction> targetDirections = !configuredDirections.isEmpty()
            ? configuredDirections : Arrays.asList(Direction.values());
        for (int tank = own.getTanks() - 1; tank >= 0; tank--) {
            FluidStack fluid = own.getFluidInTank(tank);
            if (fluid.isEmpty()) {
                continue;
            }
            FluidStack toMove = fluid.copyWithAmount(Math.min(fluid.getAmount(), maxPerTick));
            if (toMove.isEmpty()) {
                continue;
            }
            for (Direction dir : targetDirections) {
                IFluidHandler target = getAdjacentFluidHandler(worldPosition.relative(dir), dir.getOpposite());
                if (target == null) {
                    continue;
                }
                int filled = target.fill(toMove, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    own.drain(fluid.copyWithAmount(filled), IFluidHandler.FluidAction.EXECUTE);
                    toMove.setAmount(toMove.getAmount() - filled);
                    if (toMove.isEmpty()) {
                        break;
                    }
                }
            }
        }
    }

    protected void pullFluids(IFluidHandler own, int upgradeCount) {
        int maxPerTick = Math.max(1, upgradeCount * 1000);
        List<Direction> configuredDirections = upgradeStats.getFluidPullingDirections();
        Iterable<Direction> sourceDirections = !configuredDirections.isEmpty()
            ? configuredDirections : Arrays.asList(Direction.values());
        for (int tank = 0; tank < own.getTanks(); tank++) {
            int capacity = own.getTankCapacity(tank);
            FluidStack current = own.getFluidInTank(tank);
            if (current.getAmount() >= capacity) {
                continue;
            }
            boolean inputTank = tank == 0 || current.isEmpty();
            if (!inputTank) {
                continue;
            }
            for (Direction dir : sourceDirections) {
                IFluidHandler source = getAdjacentFluidHandler(worldPosition.relative(dir), dir.getOpposite());
                if (source == null) {
                    continue;
                }
                int space = capacity - current.getAmount();
                int maxDrain = Math.min(space, maxPerTick);
                FluidStack available = source.drain(maxDrain, IFluidHandler.FluidAction.SIMULATE);
                if (available.isEmpty()) {
                    continue;
                }
                if (!current.isEmpty() && !FluidStack.isSameFluid(available, current)) {
                    continue;
                }
                if (!own.isFluidValid(tank, available)) {
                    continue;
                }
                int filled = own.fill(available, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    source.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    break;
                }
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_HeatU_Block blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        blockEntity.recalculateUpgradeStats();

        long loss = blockEntity.heatStorage.applyHeatLoss();
        if (loss > 0) {
            blockEntity.setChanged();
        }

        blockEntity.distributeHeat();

        blockEntity.handleAutomationUpgrades();
    }
    
    protected void distributeHeat() {
        if (heatStorage.getHeatStored() <= 0) {
            return;
        }
        
        int myTemp = heatStorage.getTemperature();
        
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = worldPosition.relative(direction);
            
            IMioIcifCapabilities.IHeatStorage adjacentHeat = level.getCapability(
                IMioIcifCapabilities.HEAT_STORAGE_BLOCK, adjacentPos, direction.getOpposite());
            
            if (adjacentHeat == null) {
                adjacentHeat = MioIcifAPI.instance().getCapabilities().adaptHeatStorage(
                    level.getBlockEntity(adjacentPos));
            }
            
            if (adjacentHeat != null && adjacentHeat.canReceiveHeat()) {
                int adjacentTemp = adjacentHeat.getTemperature();
                
                if (myTemp > adjacentTemp) {
                    int tempDiff = myTemp - adjacentTemp;
                    
                    long maxTransfer = Math.min(heatStorage.getMaxExtract(), 
                                               adjacentHeat.getMaxHeatStored() - adjacentHeat.getHeatStored());
                    long heatToTransfer = Math.min(maxTransfer, tempDiff / 10);
                    
                    if (heatToTransfer > 0) {
                        long extracted = heatStorage.extractHeat(heatToTransfer, false);
                        if (extracted > 0) {
                            long received = adjacentHeat.receiveHeat(extracted, false);
                            if (received < extracted) {
                                heatStorage.receiveHeat(extracted - received, false);
                            }
                            setChanged();
                        }
                    }
                }
            }
        }
    }
    
    @Nullable
    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        return heatStorage;
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("heat", heatStorage.getHeatStored());
    }
    
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("heat", net.minecraft.nbt.Tag.TAG_INT)) {
            heatStorage.setHeat(tag.getInt("heat"));
        } else if (tag.contains("heat", net.minecraft.nbt.Tag.TAG_LONG)) {
            heatStorage.setHeat(tag.getLong("heat"));
        }
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.heat_block");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null;
    }
}
package com.miophas.singularity_iteration.core.prefab.blockentity;

import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.tool.IWrenchable;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotType;
import com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats;
import com.miophas.singularity_iteration.core.api.machine.IMachineUpgradeStats;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
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

import java.util.List;

@SuppressWarnings("null")
public class AbstractHeatBlockEntity extends BlockEntity implements MenuProvider, IMioIcifCapabilities.IHeatStorage, IWrenchable {
    private static final int[] EMPTY_SLOTS = new int[0];
    private static final List<Direction> ALL_DIRECTIONS = List.of(Direction.values());

    private final com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent fluidAutomation =
        new com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent(
            new com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent.Host() {
                @Override public Level level() { return level; }
                @Override public BlockPos worldPosition() { return worldPosition; }
                @Override public IFluidHandler ownFluidHandler() { return getFluidHandlerCapability(null); }
                @Override public IFluidHandler adjacentFluidHandler(BlockPos pos, Direction side) {
                    return getAdjacentFluidHandler(pos, side);
                }
                @Override public com.miophas.singularity_iteration.core.api.machine.IMachineUpgradeStats upgradeStats() {
                    recalculateUpgradeStats();
                    return upgradeStats;
                }
                @Override public void markUnsaved() { ContainerToTank.markUnsaved(AbstractHeatBlockEntity.this); }
                @Override public void markNeighborUnsaved(BlockPos pos) {
                    com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent
                        .markNeighborUnsaved(level, pos);
                }
            });


    protected final IMioIcifCapabilities.IHeatStorage heatStorage;
    protected final int baseHeatCapacity;

    protected SlotLayout slotLayout;
    protected MachineItemHandler itemHandler;

    protected IMachineUpgradeStats upgradeStats = MachineUpgradeStats.empty();

    // Upgrade statistics are immutable snapshots. Rebuild them only after an upgrade-slot
    // mutation instead of scanning the inventory twice on every server tick.
    private boolean upgradeStatsDirty = true;
    private SlotLayout cachedSlotLayout;
    private int[] cachedInputSlots = EMPTY_SLOTS;
    private int[] cachedOutputSlots = EMPTY_SLOTS;

    public AbstractHeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, 10000, 100, 100, 20, 1000, 0.01f);
    }

    public AbstractHeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                               int capacity, int maxReceive, int maxExtract,
                               int baseTemp, int maxTemp, float lossFactor) {
        super(type, pos, state);
        this.heatStorage = com.miophas.singularity_iteration.core.runtime.energy.ThermalOutput.enabled()
            ? new com.miophas.singularity_iteration.core.runtime.energy.PlatformHeatStorage(capacity, maxReceive, maxExtract, baseTemp, maxTemp, lossFactor)
            : MioIcifAPI.instance().getCapabilities().createHeatStorage(
            capacity, maxReceive, maxExtract, baseTemp, maxTemp, lossFactor);
        this.baseHeatCapacity = capacity;
    }

    public AbstractHeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                               SlotLayout layout,
                               int capacity, int maxReceive, int maxExtract,
                               int baseTemp, int maxTemp, float lossFactor) {
        super(type, pos, state);
        this.heatStorage = com.miophas.singularity_iteration.core.runtime.energy.ThermalOutput.enabled()
            ? new com.miophas.singularity_iteration.core.runtime.energy.PlatformHeatStorage(capacity, maxReceive, maxExtract, baseTemp, maxTemp, lossFactor)
            : MioIcifAPI.instance().getCapabilities().createHeatStorage(
            capacity, maxReceive, maxExtract, baseTemp, maxTemp, lossFactor);
        this.baseHeatCapacity = capacity;
        this.slotLayout = layout;
        this.itemHandler = createItemHandler(layout);
    }

    /** Allow an independently replaced machine to choose its reviewed owned storage explicitly. */
    protected AbstractHeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                  SlotLayout layout, IMioIcifCapabilities.IHeatStorage ownedStorage) {
        super(type, pos, state);
        this.heatStorage = java.util.Objects.requireNonNull(ownedStorage);
        this.baseHeatCapacity = Math.toIntExact(ownedStorage.getMaxHeatStored());
        this.slotLayout = layout;
        this.itemHandler = createItemHandler(layout);
    }

    public IMioIcifCapabilities.IHeatStorage getHeatStorage() {
        return this;
    }

    @Override
    public long receiveHeat(long toReceive, boolean simulate) {
        long received = heatStorage.receiveHeat(toReceive, simulate);
        if (!simulate && received > 0) setChanged();
        return received;
    }

    @Override
    public long extractHeat(long toExtract, boolean simulate) {
        long extracted = heatStorage.extractHeat(toExtract, simulate);
        if (!simulate && extracted > 0) setChanged();
        return extracted;
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
        long before = heatStorage.getHeatStored();
        heatStorage.setHeat(heat);
        if (heatStorage.getHeatStored() != before) setChanged();
    }

    @Override
    public void setCapacity(long capacity) {
        long before = heatStorage.getMaxHeatStored();
        heatStorage.setCapacity(capacity);
        if (heatStorage.getMaxHeatStored() != before) setChanged();
    }

    @Override
    public long applyHeatLoss() {
        long lost = heatStorage.applyHeatLoss();
        if (lost > 0) setChanged();
        return lost;
    }

    @Override
    public long consumeHeatInternal(long amount, boolean simulate) {
        long consumed = heatStorage.consumeHeatInternal(amount, simulate);
        if (!simulate && consumed > 0) setChanged();
        return consumed;
    }

    @Override
    public long generateHeatInternal(long amount, boolean simulate) {
        long generated = heatStorage.generateHeatInternal(amount, simulate);
        if (!simulate && generated > 0) setChanged();
        return generated;
    }

    @Nullable
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    protected MachineItemHandler createItemHandler(SlotLayout layout) {
        MachineItemHandler handler = new MachineItemHandler(layout) {
            @Override
            protected void onContentsChanged(int slot) {
                if (layout != null && layout.isType(slot, SlotType.UPGRADE)) {
                    AbstractHeatBlockEntity.this.markUpgradeStatsDirty();
                }
                AbstractHeatBlockEntity.this.setChanged();
            }
        };
        return handler;
    }

    /** Mark the cached upgrade snapshot stale after an inventory or layout change. */
    protected final void markUpgradeStatsDirty() {
        upgradeStatsDirty = true;
    }

    /**
     * Slot layouts are immutable, but several legacy subclasses assign their layout after
     * calling the no-layout constructor. Keep the cache lazy and keyed by layout identity so
     * those subclasses remain correct without allocating arrays on every automation pass.
     */
    private void refreshSlotCaches() {
        if (cachedSlotLayout == slotLayout) {
            return;
        }
        cachedSlotLayout = slotLayout;
        if (slotLayout == null) {
            cachedInputSlots = EMPTY_SLOTS;
            cachedOutputSlots = EMPTY_SLOTS;
        } else {
            cachedInputSlots = slotLayout.getSlotsOfType(SlotType.INPUT);
            cachedOutputSlots = slotLayout.getSlotsOfType(SlotType.OUTPUT);
        }
        upgradeStatsDirty = true;
    }

    public boolean isUpgradeSlot(int slot) {
        return slotLayout != null && slotLayout.isType(slot, SlotType.UPGRADE);
    }

    protected void recalculateUpgradeStats() {
        refreshSlotCaches();
        if (!upgradeStatsDirty) {
            return;
        }
        if (slotLayout == null || itemHandler == null) {
            this.upgradeStats = MachineUpgradeStats.empty();
            upgradeStatsDirty = false;
            return;
        }
        int upgradeStart = slotLayout.getStart(SlotType.UPGRADE);
        int upgradeCount = slotLayout.getCount(SlotType.UPGRADE);
        this.upgradeStats = MachineUpgradeStats.fromInventory(itemHandler, upgradeStart, upgradeCount);
        applyHeatCapacityUpgrades();
        upgradeStatsDirty = false;
    }

    protected void applyHeatCapacityUpgrades() {
        long upgradedCapacity = upgradeStats.getHeatCapacity(baseHeatCapacity);
        if (heatStorage.getMaxHeatStored() != upgradedCapacity) {
            heatStorage.setCapacity(upgradedCapacity);
            ContainerToTank.markUnsaved(this);
        }
    }

    public IMachineUpgradeStats getUpgradeStats() {
        recalculateUpgradeStats();
        return upgradeStats;
    }

    protected int getProcessingSpeedMultiplier() {
        recalculateUpgradeStats();
        return Math.max(1, (int) Math.ceil(1.0 / upgradeStats.getProcessTimeMultiplier()));
    }

    protected double getProcessingCostMultiplier() {
        recalculateUpgradeStats();
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
        if (upgradeStats.isRedstoneInverted()) {
            return powered;
        }
        return !powered;
    }

    protected void handleAutomationUpgrades() {
        if (level == null || level.isClientSide) {
            return;
        }
        recalculateUpgradeStats();
        int ejectorCount = upgradeStats.getEjectorCount();
        if (ejectorCount > 0) {
            ejectItems(ejectorCount);
        }
        int pullingCount = upgradeStats.getPullingCount();
        if (pullingCount > 0) {
            pullItems(pullingCount);
        }
        IFluidHandler own = getFluidHandlerCapability(null);
        if (own != null) {
            int fluidEjectorCount = upgradeStats.getFluidEjectorCount();
            if (fluidEjectorCount > 0) {
                ejectFluids(own, fluidEjectorCount);
            }
            int fluidPullingCount = upgradeStats.getFluidPullingCount();
            if (fluidPullingCount > 0) {
                pullFluids(own, fluidPullingCount);
            }
        }
    }

    protected int[] getOutputSlots() {
        refreshSlotCaches();
        return cachedOutputSlots;
    }

    protected int[] getInputSlots() {
        refreshSlotCaches();
        return cachedInputSlots;
    }

    protected void ejectItems(int upgradeCount) {
        int maxPerTick = Math.max(1, upgradeCount);
        List<Direction> configuredDirections = upgradeStats.getEjectorDirections();
        Iterable<Direction> targetDirections = !configuredDirections.isEmpty()
            ? configuredDirections : ALL_DIRECTIONS;

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
            ? configuredDirections : ALL_DIRECTIONS;

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
        if (level == null || !level.hasChunkAt(pos) || level.getBlockEntity(pos) == null) {
            return null;
        }
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
    }

    @Nullable
    protected IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return null;
    }

    /** 对外暴露本机流体处理器，供附属注册流体能力；无流体支持时为 null。 */
    @Nullable
    public final IFluidHandler fluidHandler(@Nullable Direction side) {
        return getFluidHandlerCapability(side);
    }

    @Nullable
    protected IFluidHandler getAdjacentFluidHandler(BlockPos pos, @Nullable Direction side) {
        if (level == null || !level.hasChunkAt(pos)) {
            return null;
        }
        BlockEntity target = level.getBlockEntity(pos);
        if (target == null) {
            return null;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
    }

    protected void ejectFluids(IFluidHandler own, int upgradeCount) {
        fluidAutomation.ejectFluids(own, upgradeCount);
    }

    protected void pullFluids(IFluidHandler own, int upgradeCount) {
        fluidAutomation.pullFluids(own, upgradeCount);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AbstractHeatBlockEntity blockEntity) {
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

    /**
     * 传热模型已按原版 IC2 调整：热源机器各自只向“正面”输出热量
     * （见各热机自己的 outputHeat / pushFront / ThermalOutput.front），
     * 原来的“按温度差向 6 面广播”已雪藏 —— 原版 IC2 没有温度差传导，传输量与温差无关。
     *
     * 保留空实现以免影响既有调用点。
     */
    protected void distributeHeat() {
        // intentionally empty (temperature-gradient conduction shelved)
    }

    @Nullable
    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        return this;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        fluidAutomation.save(tag, registries);
        tag.putLong("heat", heatStorage.getHeatStored());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluidAutomation.load(tag, registries);
        if (tag.contains("heat", net.minecraft.nbt.Tag.TAG_INT)) {
            heatStorage.setHeat(tag.getInt("heat"));
        } else if (tag.contains("heat", net.minecraft.nbt.Tag.TAG_LONG)) {
            heatStorage.setHeat(tag.getLong("heat"));
        }
        markUpgradeStatsDirty();
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

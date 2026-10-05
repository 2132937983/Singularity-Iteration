// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Quantum modification station. Slot 1 holds a quantum suit piece, slot 2 an upgrade unit.
 * The station installs the unit in {@link SuitModules#INSTALL_TICKS} ticks at
 * {@link SuitModules#INSTALL_EU_PER_TICK} EU/t. Removal is instant and free (GUI button).
 */
@SuppressWarnings("null")
public class QuantumModStationBlockEntity extends AbstractProcessingMachineBlockEntity {
    public static final int BATTERY_SLOT = 0, SUIT_SLOT = 1, MODULE_SLOT = 2, SLOT_COUNT = 3;
    public static final long CAPACITY = 40_000L;
    public static final long MAX_RECEIVE = 512L;
    public static final int DATA_COUNT = 6;

    /** What the station is doing; shown in the GUI. */
    public enum Status { IDLE, WORKING, NO_POWER, NOT_A_SUIT_PIECE, WRONG_SLOT, ALREADY_INSTALLED, FULL, NO_UNIT }

    private static final SlotLayout LAYOUT = SlotLayout.builder().battery().extra(2).build();

    private int installProgress;
    private Status status = Status.IDLE;
    private boolean clientWorking;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            long energy = getEnergyStorage().getAmount();
            return switch (index) {
                case 0 -> (int) (energy & 0xFFFF);
                case 1 -> (int) ((energy >>> 16) & 0xFFFF);
                case 2 -> (int) (energy >>> 32);
                case 3 -> installProgress;
                case 4 -> status.ordinal();
                case 5 -> (int) Math.min(Integer.MAX_VALUE, getEnergyStorage().getCapacity());
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public QuantumModStationBlockEntity(BlockPos pos, BlockState state) {
        this(SuitRegistry.STATION_ENTITY.get(), pos, state);
    }

    public QuantumModStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(pos, state, type, CAPACITY, MAX_RECEIVE, 0L, SuitModules.INSTALL_TICKS, LAYOUT, SuitModules.INSTALL_EU_PER_TICK, CableTier.HV);
    }

    @Override
    protected MachineItemHandler createItemHandler(SlotLayout layout) {
        MachineItemHandler handler = new MachineItemHandler(layout) {
            @Override protected void onContentsChanged(int slot) {
                onInventoryChanged(slot);
                if (slot == SUIT_SLOT) sync();
            }
            @Override public int getSlotLimit(int slot) { return slot == SUIT_SLOT ? 1 : super.getSlotLimit(slot); }
        };
        handler.setValidator(this);
        return handler;
    }

    public ContainerData getContainerData() { return data; }
    public int installProgress() { return installProgress; }
    public Status status() { return status; }
    /** Client: true while the station is installing (drives the scan beam). */
    public boolean isClientWorking() { return clientWorking; }
    public ItemStack suit() { return itemHandler.getStackInSlot(SUIT_SLOT); }
    public ItemStack unit() { return itemHandler.getStackInSlot(MODULE_SLOT); }

    @Nullable
    private static SuitModuleType unitType(ItemStack stack) {
        return stack.getItem() instanceof SuitModuleItem item ? item.type() : null;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case BATTERY_SLOT -> isBattery(stack);
            case SUIT_SLOT -> SuitModules.isSuitPiece(stack);
            case MODULE_SLOT -> stack.getItem() instanceof SuitModuleItem;
            default -> false;
        };
    }

    private static final int[] AUTOMATION = {BATTERY_SLOT, SUIT_SLOT, MODULE_SLOT};

    @Override protected int[] getSlotsForDirection(Direction side) { return AUTOMATION; }
    @Override protected int getBatterySlot() { return BATTERY_SLOT; }
    /** Pipes may take the finished piece out; units stay unless the player takes them. */
    @Override protected boolean canExtractItem(int slot, @Nullable Direction side) { return slot == SUIT_SLOT; }
    @Override protected boolean canWork() { return false; }
    @Override protected void doWork() { }

    /** What the station would do with the current slots. */
    public Status evaluate() {
        ItemStack unit = unit();
        SuitModuleType type = unitType(unit);
        if (suit().isEmpty() || type == null) return Status.NO_UNIT;
        return switch (SuitModules.check(suit(), type)) {
            case OK -> Status.WORKING;
            case NOT_A_SUIT_PIECE -> Status.NOT_A_SUIT_PIECE;
            case WRONG_SLOT -> Status.WRONG_SLOT;
            case ALREADY_INSTALLED -> Status.ALREADY_INSTALLED;
            case FULL -> Status.FULL;
        };
    }

    @Override
    protected void tickProduction() {
        Status next = evaluate();
        boolean working = false;
        if (next == Status.WORKING) {
            if (getEnergyStorage().getAmount() >= SuitModules.INSTALL_EU_PER_TICK) {
                apiUseEnergy(SuitModules.INSTALL_EU_PER_TICK, false);
                working = true;
                if (++installProgress >= SuitModules.INSTALL_TICKS) finishInstall();
            } else {
                next = Status.NO_POWER;
            }
        } else {
            installProgress = 0;
        }
        if (next == Status.NO_UNIT) next = Status.IDLE;
        status = next;
        if (isWorking != working) {
            isWorking = working;
            sync();
        }
    }

    private void finishInstall() {
        installProgress = 0;
        ItemStack suit = suit().copy();
        SuitModuleType type = unitType(unit());
        if (type == null || !SuitModules.install(suit, type)) return;
        ItemStack rest = unit().copy();
        rest.shrink(1);
        ((MachineItemHandler) itemHandler).setStackInSlot(MODULE_SLOT, rest);
        ((MachineItemHandler) itemHandler).setStackInSlot(SUIT_SLOT, suit);
        if (level != null) level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.6F, 1.6F);
    }

    /**
     * GUI button: takes the n-th installed unit out of the piece. The unit goes into the unit
     * slot when it fits there, otherwise to the player.
     */
    public boolean removeUnit(int index, Player player) {
        ItemStack suit = suit();
        List<SuitModuleType> types = SuitModules.installed(suit).types();
        if (index < 0 || index >= types.size()) return false;
        SuitModuleType type = types.get(index);
        ItemStack copy = suit.copy();
        if (!SuitModules.remove(copy, type)) return false;
        ItemStack unit = new ItemStack(SuitRegistry.unitItem(type));
        // ArmorFeatureState keeps the switch; a re-installed unit comes back in the same state
        ((MachineItemHandler) itemHandler).setStackInSlot(SUIT_SLOT, copy);
        ItemStack slot = unit();
        if (slot.isEmpty()) {
            ((MachineItemHandler) itemHandler).setStackInSlot(MODULE_SLOT, unit);
        } else if (ItemStack.isSameItemSameComponents(slot, unit) && slot.getCount() < slot.getMaxStackSize()) {
            slot = slot.copy();
            slot.grow(1);
            ((MachineItemHandler) itemHandler).setStackInSlot(MODULE_SLOT, slot);
        } else if (!player.getInventory().add(unit)) {
            player.drop(unit, false);
        }
        installProgress = 0;
        if (level != null) level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.5F, 1.8F);
        return true;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mio_icif.producer.block_quantum_modification_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new QuantumModStationMenu(containerId, playerInventory, this, data);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, QuantumModStationBlockEntity station) {
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, station);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("InstallProgress", installProgress);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        installProgress = Math.max(0, Math.min(SuitModules.INSTALL_TICKS, tag.getInt("InstallProgress")));
        if (tag.contains("StationDisplay")) {
            // client copy for the renderer
            ItemStack shown = ItemStack.parseOptional(registries, tag.getCompound("StationDisplay"));
            ((MachineItemHandler) itemHandler).setStackInSlot(SUIT_SLOT, shown);
            clientWorking = tag.getBoolean("StationWorking");
        }
    }

    /** The renderer shows the piece on the pad and the scan beam while working. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("StationDisplay", suit().saveOptional(registries));
        tag.putBoolean("StationWorking", isWorking);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

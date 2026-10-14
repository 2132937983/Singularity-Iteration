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
 * Universal Suit Modification Station (the quantum modification station, generalised in 0.1.8.5).
 * Slot 1 holds a quantum or nano suit piece, slot 2 an upgrade unit or (nano pieces) a lattice
 * densifier. The station installs the unit in {@link SuitModules#INSTALL_TICKS} ticks at
 * {@link SuitModules#INSTALL_EU_PER_TICK} EU/t. Removal is instant and free (GUI button).
 * The block id stays {@code producer/block_quantum_modification_station} so that worlds keep it.
 */
@SuppressWarnings("null")
public class QuantumModStationBlockEntity extends AbstractProcessingMachineBlockEntity {
    public static final int BATTERY_SLOT = 0, SUIT_SLOT = 1, MODULE_SLOT = 2, SLOT_COUNT = 3;
    public static final long CAPACITY = 40_000L;
    public static final long MAX_RECEIVE = 512L;
    public static final int DATA_COUNT = 6;

    /** What the station is doing; shown in the GUI. */
    public enum Status { IDLE, WORKING, NO_POWER, NOT_A_SUIT_PIECE, WRONG_SLOT, ALREADY_INSTALLED, FULL, NO_UNIT,
        /** 0.1.8.5: the unit does not fit this suit class / not enough lattice points / densifier limit reached. */
        WRONG_CLASS, NO_LATTICE, LATTICE_MAX,
        /** 0.1.8.6: Mk.I to Mk.II upgrade in place; one Mk.II item at a time (the Mk.I item comes back into the slot). */
        UPGRADING, UPGRADE_ONE_AT_A_TIME,
        /** 0.1.9.0: a frame core goes into the empty core socket / replaces the installed core (which comes back). */
        CORE_INSTALL, CORE_SWAP,
        /** 0.1.9.0: the core that a swap replaced waits in the unit slot until the player takes it out. */
        CORE_RETURNED,
        /** 0.1.10.0: the quantum piece already has its expansion bay. */
        BAY_MAX }

    private static final SlotLayout LAYOUT = SlotLayout.builder().battery().extra(2).build();

    private int installProgress;
    private Status status = Status.IDLE;
    /** 0.1.9.0: the item in the unit slot was handed back by a core swap; it is not installed again until it leaves the slot. */
    private boolean heldReturn;
    private boolean placing;
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
                if (slot == MODULE_SLOT && !placing) heldReturn = false;
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

    /** 0.1.8.0: built-in unit items and registered addon unit items (public API). */
    @Nullable
    private static UnitRef unitType(ItemStack stack) {
        return UnitRef.ofItem(stack);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case BATTERY_SLOT -> isBattery(stack);
            case SUIT_SLOT -> SuitModules.isSuitPiece(stack) || com.miophas.singularity_iteration.common.suit.core.LegacyArmor.isLegacy(stack);
            case MODULE_SLOT -> UnitRef.ofItem(stack) != null || isDensifier(stack) || isCore(stack) || isBayExpander(stack);
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

    /** 0.1.8.5: the nano lattice densifier goes into the unit slot too. */
    public static boolean isDensifier(ItemStack stack) {
        return !stack.isEmpty() && stack.is(SuitRegistry.LATTICE_DENSIFIER.get());
    }

    /** 0.1.10.0: the quantum bay expander goes into the unit slot too. */
    public static boolean isBayExpander(ItemStack stack) {
        return !stack.isEmpty() && stack.is(SuitRegistry.BAY_EXPANDER.get());
    }

    /** 0.1.9.0: frame core items go into the unit slot. */
    public static boolean isCore(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof com.miophas.singularity_iteration.common.suit.core.FrameCoreItem;
    }

    /** What the station would do with the current slots. */
    public Status evaluate() {
        ItemStack unit = unit();
        if (heldReturn && isCore(unit)) return Status.CORE_RETURNED;
        if (!suit().isEmpty() && isCore(unit)) {
            var core = ((com.miophas.singularity_iteration.common.suit.core.FrameCoreItem) unit.getItem()).core();
            return switch (com.miophas.singularity_iteration.common.suit.core.FrameCores.check(suit(), core)) {
                case OK -> Status.CORE_INSTALL;
                case SWAP -> unit.getCount() == 1 ? Status.CORE_SWAP : Status.UPGRADE_ONE_AT_A_TIME;
                case NOT_A_SUIT_PIECE -> Status.NOT_A_SUIT_PIECE;
                case WRONG_CLASS -> Status.WRONG_CLASS;
                case WRONG_SLOT -> Status.WRONG_SLOT;
                case ALREADY_INSTALLED -> Status.ALREADY_INSTALLED;
            };
        }
        if (!suit().isEmpty() && isDensifier(unit)) {
            if (!SuitModules.isNanoPiece(suit())) return SuitModules.isSuitPiece(suit()) ? Status.WRONG_CLASS : Status.NOT_A_SUIT_PIECE;
            return SuitModules.canDensify(suit()) ? Status.WORKING : Status.LATTICE_MAX;
        }
        if (!suit().isEmpty() && isBayExpander(unit)) {
            if (!SuitModules.isQuantumPiece(suit())) return SuitModules.isSuitPiece(suit()) ? Status.WRONG_CLASS : Status.NOT_A_SUIT_PIECE;
            return SuitModules.canExpand(suit()) ? Status.WORKING : Status.BAY_MAX;
        }
        UnitRef type = unitType(unit);
        if (suit().isEmpty() || type == null) return Status.NO_UNIT;
        int tier = SuitModuleItem.tierOf(unit);
        if (SuitModules.isUpgrade(suit(), type, tier) && SuitModules.check(suit(), type, tier) == SuitModules.InstallCheck.OK) {
            return unit.getCount() == 1 ? Status.UPGRADING : Status.UPGRADE_ONE_AT_A_TIME;
        }
        return switch (SuitModules.check(suit(), type, tier)) {
            case OK -> Status.WORKING;
            case NOT_A_SUIT_PIECE -> Status.NOT_A_SUIT_PIECE;
            case WRONG_SLOT -> Status.WRONG_SLOT;
            case ALREADY_INSTALLED -> Status.ALREADY_INSTALLED;
            case FULL -> Status.FULL;
            case WRONG_CLASS -> Status.WRONG_CLASS;
            case NO_LATTICE -> Status.NO_LATTICE;
        };
    }

    @Override
    protected void tickProduction() {
        // 0.1.9.0: a legacy shell turns into its suit piece with the frame core at once
        if (com.miophas.singularity_iteration.common.suit.core.LegacyArmor.isLegacy(suit())) convertLegacy();
        Status next = evaluate();
        boolean working = false;
        if (next == Status.WORKING || next == Status.UPGRADING || next == Status.CORE_INSTALL || next == Status.CORE_SWAP) {
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
        if (isCore(unit())) {
            var core = ((com.miophas.singularity_iteration.common.suit.core.FrameCoreItem) unit().getItem()).core();
            var check = com.miophas.singularity_iteration.common.suit.core.FrameCores.check(suit, core);
            if (check != com.miophas.singularity_iteration.common.suit.core.FrameCores.Check.OK
                    && check != com.miophas.singularity_iteration.common.suit.core.FrameCores.Check.SWAP) return;
            var old = com.miophas.singularity_iteration.common.suit.core.FrameCores.install(suit, core);
            ItemStack rest = unit().copy();
            rest.shrink(1);
            // a replaced core comes back into the (now empty) unit slot and waits there (no swap back)
            placing = true;
            ((MachineItemHandler) itemHandler).setStackInSlot(MODULE_SLOT, old != null ? new ItemStack(SuitRegistry.coreItem(old)) : rest);
            placing = false;
            heldReturn = old != null;
            ((MachineItemHandler) itemHandler).setStackInSlot(SUIT_SLOT, suit);
            if (level != null) level.playSound(null, worldPosition, com.miophas.singularity_iteration.common.registry.mio_icif_sounds.CORE_INSTALL.get(),
                SoundSource.BLOCKS, 0.8F, 1.0F);
            return;
        }
        if (isDensifier(unit())) {
            if (!SuitModules.densify(suit)) return;
        } else if (isBayExpander(unit())) {
            if (!SuitModules.expand(suit)) return;               // 0.1.10.0: +1 unit slot
        } else {
            UnitRef type = unitType(unit());
            int tier = SuitModuleItem.tierOf(unit());
            if (type == null) return;
            boolean upgrade = SuitModules.isUpgrade(suit, type, tier);
            if (!SuitModules.install(suit, type, tier)) return;
            if (upgrade) {
                // 0.1.8.6: the replaced Mk.I unit comes back into the (now empty) unit slot
                ((MachineItemHandler) itemHandler).setStackInSlot(MODULE_SLOT, type.unitStack(1));
                ((MachineItemHandler) itemHandler).setStackInSlot(SUIT_SLOT, suit);
                if (level != null) level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.7F, 1.9F);
                return;
            }
        }
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
        List<UnitRef> types = SuitModules.installed(suit).units();
        if (index < 0 || index >= types.size()) return false;
        UnitRef type = types.get(index);
        int tier = Math.max(1, SuitModules.tier(suit, type.key()));
        ItemStack copy = suit.copy();
        if (!SuitModules.remove(copy, type)) return false;
        ItemStack unit = type.unitStack(tier);
        if (unit.isEmpty()) return false;                    // addon item missing: keep the unit installed
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

    /** 0.1.9.0 GUI button: takes the frame core out of the piece and gives it to the player. */
    public boolean removeCore(Player player) {
        ItemStack copy = suit().copy();
        var old = com.miophas.singularity_iteration.common.suit.core.FrameCores.remove(copy);
        if (old == null) return false;
        ((MachineItemHandler) itemHandler).setStackInSlot(SUIT_SLOT, copy);
        // to the player, not into the unit slot (the station would install it again at once)
        ItemStack item = new ItemStack(SuitRegistry.coreItem(old));
        if (!player.getInventory().add(item)) player.drop(item, false);
        installProgress = 0;
        if (level != null) level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.5F, 1.4F);
        return true;
    }

    /** Converts a legacy shell in the piece slot; a result that is not a suit piece (solar skin) pops out above. */
    private void convertLegacy() {
        var result = com.miophas.singularity_iteration.common.suit.core.LegacyArmor.convert(suit());
        if (result == null) return;
        if (SuitModules.isSuitPiece(result.stack())) {
            ((MachineItemHandler) itemHandler).setStackInSlot(SUIT_SLOT, result.stack());
        } else {
            ((MachineItemHandler) itemHandler).setStackInSlot(SUIT_SLOT, ItemStack.EMPTY);
            pop(result.stack());
        }
        for (ItemStack extra : result.extras()) pop(extra);
    }

    private void pop(ItemStack stack) {
        if (level == null || stack.isEmpty()) return;
        net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, stack);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mio_icif.producer.block_quantum_modification_station");   // "Universal Suit Modification Station"
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
        if (heldReturn) tag.putBoolean("HeldReturn", true);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        installProgress = Math.max(0, Math.min(SuitModules.INSTALL_TICKS, tag.getInt("InstallProgress")));
        heldReturn = tag.getBoolean("HeldReturn");
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

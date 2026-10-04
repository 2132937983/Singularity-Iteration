package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockStructure;
import com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyCore;

import com.miophas.singularity_iteration.common.menu.storage.GESUCoreMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_gesu_core_entity extends MultiblockEnergyCore implements net.minecraft.world.Container,
        com.miophas.singularity_iteration.core.api.machine.IStorageMember {

    @Override public net.minecraft.world.level.block.entity.BlockEntity storageOwner() { return this; }

    /** Charge slot (items filled from the core) and discharge slot (batteries / redstone emptied into it). */
    public static final int CHARGE_SLOT = 0, DISCHARGE_SLOT = 1;
    private final net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> items =
        net.minecraft.core.NonNullList.withSize(2, net.minecraft.world.item.ItemStack.EMPTY);

    private static final long DEFAULT_CAPACITY = 2147483647L;
    private static final ICableTier MAX_TIER =
        MioIcifAPI.instance().getEnergyNetAPI().getCableTier("max");

    @Nullable
    private IMultiblockStructure multiblockStructure;

    private int inputModuleCount = 0;
    private int outputIvModuleCount = 0;
    private int outputLuvModuleCount = 0;
    private int tickCounter = 0;
    private boolean needsRevalidation = false;

    public mio_icif_gesu_core_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.GESU_CORE.get(), DEFAULT_CAPACITY, 0, 0, MAX_TIER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_gesu_core_entity blockEntity) {
        if (level.isClientSide()) return;

        if (blockEntity.needsRevalidation) {
            blockEntity.needsRevalidation = false;
            blockEntity.revalidateAfterLoad(level, pos);
        }

        AbstractEnergyBlockEntity.tick(level, pos, state, blockEntity);
        blockEntity.tickItems();

        blockEntity.tickCounter++;
        if (blockEntity.tickCounter >= 20) {
            blockEntity.tickCounter = 0;
            if (blockEntity.isStructureComplete()) {
                com.miophas.singularity_iteration.common.multiblock.mio_icif_multiblock_manager<?> manager =
                    com.miophas.singularity_iteration.common.multiblock.mio_icif_multiblock_manager.getStructureByController(level, pos);
                if (manager != null) {
                    manager.validateStructure(level);
                } else {
                    blockEntity.onMultiblockBroken();
                }
            }
        }
    }

    /** Per-tick item rate of the slots (an LuV line: the core is the end of the ladder). */
    public static long itemRate() {
        return MioIcifAPI.instance().getEnergyNetAPI().getCableTier("luv").getPowerRating();
    }

    /** Charges the charge-slot item from the core and drains the discharge-slot item into it. */
    public void tickItems() {
        var storage = getEnergyStorageInternal();
        boolean changed = false;
        var charge = items.get(CHARGE_SLOT);
        if (!charge.isEmpty() && storage.getAmount() > 0) {
            long moved = com.miophas.singularity_iteration.core.runtime.energy.DirectItemCharging.charge(charge, Math.min(itemRate(), storage.getAmount()));
            if (moved > 0) { storage.setEnergy(storage.getAmount() - moved); changed = true; }
        }
        var discharge = items.get(DISCHARGE_SLOT);
        long room = storage.getCapacity() - storage.getAmount();
        if (!discharge.isEmpty() && room > 0) {
            long moved = com.miophas.singularity_iteration.core.runtime.energy.DirectItemCharging.discharge(discharge, Math.min(itemRate(), room));
            if (moved > 0) { storage.setEnergy(storage.getAmount() + moved); changed = true; }
        }
        if (changed) setChanged();
    }

    // ------------------------------------------------------------------ Container (2 slots)
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(net.minecraft.world.item.ItemStack::isEmpty); }
    @Override public net.minecraft.world.item.ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : net.minecraft.world.item.ItemStack.EMPTY;
    }
    @Override public net.minecraft.world.item.ItemStack removeItem(int slot, int amount) {
        var out = net.minecraft.world.ContainerHelper.removeItem(items, slot, amount);
        if (!out.isEmpty()) setChanged();
        return out;
    }
    @Override public net.minecraft.world.item.ItemStack removeItemNoUpdate(int slot) {
        return net.minecraft.world.ContainerHelper.takeItem(items, slot);
    }
    @Override public void setItem(int slot, net.minecraft.world.item.ItemStack stack) {
        if (slot < 0 || slot >= items.size()) return;
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }
    @Override public boolean canPlaceItem(int slot, net.minecraft.world.item.ItemStack stack) {
        return slot == CHARGE_SLOT ? com.miophas.singularity_iteration.core.runtime.energy.DirectItemCharging.canCharge(stack)
            : slot == DISCHARGE_SLOT && com.miophas.singularity_iteration.core.runtime.energy.DirectItemCharging.canDischarge(stack);
    }
    @Override public boolean stillValid(Player player) { return net.minecraft.world.Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); setChanged(); }

    private void revalidateAfterLoad(Level level, BlockPos pos) {
        com.miophas.singularity_iteration.common.multiblock.mio_icif_multiblock_manager<?> existing =
            com.miophas.singularity_iteration.common.multiblock.mio_icif_multiblock_manager.getStructureByController(level, pos);
        if (existing != null && existing.isValid()) {
            return;
        }

        com.miophas.singularity_iteration.common.multiblock.mio_icif_gesu_validator validator =
            new com.miophas.singularity_iteration.common.multiblock.mio_icif_gesu_validator();
        com.miophas.singularity_iteration.common.multiblock.mio_icif_multiblock_manager<com.miophas.singularity_iteration.common.multiblock.mio_icif_gesu_validator> manager =
            new com.miophas.singularity_iteration.common.multiblock.mio_icif_multiblock_manager<>(pos, validator);
        if (manager.tryForm(level)) {
            setChanged();
        } else {
            onMultiblockBroken();
        }
    }

    @Override
    public void onMultiblockFormed(IMultiblockStructure structure) {
        this.multiblockStructure = structure;
        this.setStructureComplete(true);

        java.util.Map<String, Object> data = structure.getStructureData();

        Object inputObj = data.get("inputModuleCount");
        Object outputIvObj = data.get("outputIvModuleCount");
        Object outputLuvObj = data.get("outputLuvModuleCount");

        this.inputModuleCount = (inputObj instanceof Integer) ? (Integer) inputObj : 0;
        this.outputIvModuleCount = (outputIvObj instanceof Integer) ? (Integer) outputIvObj : 0;
        this.outputLuvModuleCount = (outputLuvObj instanceof Integer) ? (Integer) outputLuvObj : 0;

        updateEnergyRates();
        setChanged();
    }

    @Override
    public void onMultiblockBroken() {
        this.multiblockStructure = null;
        this.setStructureComplete(false);
        this.inputModuleCount = 0;
        this.outputIvModuleCount = 0;
        this.outputLuvModuleCount = 0;

        updateEnergyRates();
        setChanged();
    }

    private void updateEnergyRates() {
        long maxReceive = inputModuleCount * MioIcifAPI.instance().getEnergyNetAPI().getCableTier("max").getPowerRating();
        long maxExtract = outputIvModuleCount * MioIcifAPI.instance().getEnergyNetAPI().getCableTier("iv").getPowerRating()
                + outputLuvModuleCount * MioIcifAPI.instance().getEnergyNetAPI().getCableTier("luv").getPowerRating();

        setMaxReceive(maxReceive);
        setMaxExtract(maxExtract);
    }

    public int getInputModuleCount() {
        return inputModuleCount;
    }

    public int getOutputIvModuleCount() {
        return outputIvModuleCount;
    }

    public int getOutputLuvModuleCount() {
        return outputLuvModuleCount;
    }

    public long getCurrentMaxReceive() {
        return getMaxReceive();
    }

    public long getCurrentMaxExtract() {
        return getMaxExtract();
    }

    @Override
    public double getDemandedEnergy() {
        if (isPowerSource()) return 0.0D;
        long maxRecv = getMaxReceive();
        if (maxRecv <= 0) return 0.0D;
        long spaceAvailable = getEnergyCapacity() - getStoredEnergy();
        if (spaceAvailable <= 0) return 0.0D;
        return Math.min(spaceAvailable, maxRecv);
    }

    @Override
    public double injectEnergy(Direction direction, double amount, double voltage) {
        if (isPowerSource()) return amount;
        long maxRecv = getMaxReceive();
        if (maxRecv <= 0) return amount;
        long toAdd = Math.min((long) amount, maxRecv);
        long spaceAvailable = getEnergyCapacity() - getStoredEnergy();
        long accepted = Math.min(toAdd, spaceAvailable);
        getEnergyStorageInternal().setEnergy(getStoredEnergy() + accepted);
        return amount - accepted;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.gesu_core");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GESUCoreMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("inputModuleCount", inputModuleCount);
        tag.putInt("outputIvModuleCount", outputIvModuleCount);
        tag.putInt("outputLuvModuleCount", outputLuvModuleCount);
        net.minecraft.world.ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inputModuleCount")) {
            inputModuleCount = tag.getInt("inputModuleCount");
        }
        if (tag.contains("outputIvModuleCount")) {
            outputIvModuleCount = tag.getInt("outputIvModuleCount");
        }
        if (tag.contains("outputLuvModuleCount")) {
            outputLuvModuleCount = tag.getInt("outputLuvModuleCount");
        }
        items.clear();
        net.minecraft.world.ContainerHelper.loadAllItems(tag, items, registries);
        if (isStructureComplete()) {
            updateEnergyRates();
            needsRevalidation = true;
        }
    }
}
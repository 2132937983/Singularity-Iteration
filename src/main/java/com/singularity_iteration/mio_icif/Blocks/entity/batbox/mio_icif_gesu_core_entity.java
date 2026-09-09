package com.singularity_iteration.mio_icif.Blocks.entity.batbox;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.IMultiblockStructure;
import com.singularity_iteration.mio_icif.api.machine.MultiblockEnergyCore;

import com.singularity_iteration.mio_icif.Menu.Storage.GESUCoreMenu;

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
public class mio_icif_gesu_core_entity extends MultiblockEnergyCore {

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

        mio_icif_Energy_Block.tick(level, pos, state, blockEntity);

        blockEntity.tickCounter++;
        if (blockEntity.tickCounter >= 20) {
            blockEntity.tickCounter = 0;
            if (blockEntity.isStructureComplete()) {
                com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager<?> manager =
                    com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager.getStructureByController(level, pos);
                if (manager != null) {
                    manager.validateStructure(level);
                } else {
                    blockEntity.onMultiblockBroken();
                }
            }
        }
    }

    private void revalidateAfterLoad(Level level, BlockPos pos) {
        com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager<?> existing =
            com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager.getStructureByController(level, pos);
        if (existing != null && existing.isValid()) {
            return;
        }

        com.singularity_iteration.mio_icif.multiblock.mio_icif_gesu_validator validator =
            new com.singularity_iteration.mio_icif.multiblock.mio_icif_gesu_validator();
        com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager<com.singularity_iteration.mio_icif.multiblock.mio_icif_gesu_validator> manager =
            new com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager<>(pos, validator);
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
        if (isStructureComplete()) {
            updateEnergyRates();
            needsRevalidation = true;
        }
    }
}
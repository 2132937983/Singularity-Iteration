package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Menu.Producer.LargeFabricatorCoreMenu;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.IMultiblockStructure;
import com.singularity_iteration.mio_icif.api.machine.MultiblockEnergyCore;
import com.singularity_iteration.mio_icif.multiblock.mio_icif_large_fabricator_validator;
import com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_large_fabricator_core_entity extends MultiblockEnergyCore {

    private static final long DEFAULT_CAPACITY = 0L;
    private static final ICableTier MAX_TIER =
        MioIcifAPI.instance().getEnergyNetAPI().getCableTier("max");

    public static final long ENERGY_PER_MB = 800000L;
    private static final int STRUCTURE_CHECK_INTERVAL = 15;

    public static final int UUMATTER_TANK_CAPACITY = 128000;

    @Nullable
    private IMultiblockStructure multiblockStructure;

    private int inputModuleCount = 0;
    private int tankModuleCount = 0;
    private int scrapModuleCount = 0;
    private int tickCounter = 0;
    private boolean needsRevalidation = false;

    protected final FluidTank uuMatterTank;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) (getStoredEnergy() >> 32);
                case 1 -> (int) getStoredEnergy();
                case 2 -> (int) (getEnergyCapacity() >> 32);
                case 3 -> (int) getEnergyCapacity();
                case 4 -> getTotalUuMatterAmount();
                case 5 -> getTotalUuMatterCapacity();
                case 6 -> (int) getCachedScrapValue();
                case 7 -> inputModuleCount;
                case 8 -> tankModuleCount;
                case 9 -> scrapModuleCount;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 10;
        }
    };

    public mio_icif_large_fabricator_core_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.LARGE_FABRICATOR_CORE.get(), DEFAULT_CAPACITY, 0, 0, MAX_TIER);
        this.uuMatterTank = new FluidTank(UUMATTER_TANK_CAPACITY, fluidStack ->
            fluidStack.getFluid() == mio_icif_fluids.UUMATTER.get());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_large_fabricator_core_entity blockEntity) {
        if (level.isClientSide()) return;

        if (blockEntity.needsRevalidation) {
            blockEntity.needsRevalidation = false;
            blockEntity.revalidateAfterLoad(level, pos);
        }

        mio_icif_Energy_Block.tick(level, pos, state, blockEntity);

        blockEntity.tickCounter++;
        if (blockEntity.tickCounter >= STRUCTURE_CHECK_INTERVAL) {
            blockEntity.tickCounter = 0;
            if (blockEntity.isStructureComplete()) {
                mio_icif_multiblock_manager<?> manager =
                    mio_icif_multiblock_manager.getStructureByController(level, pos);
                if (manager != null) {
                    manager.validateStructure(level);
                } else {
                    blockEntity.onMultiblockBroken();
                }
            }
        }

        if (blockEntity.isStructureComplete()) {
            blockEntity.processFabrication(level, pos);
        }
    }

    private void processFabrication(Level level, BlockPos pos) {
        distributeUuMatterToTanks();

        mio_icif_large_fabricator_scrap_entity scrapModule = findScrapModule();

        for (Direction direction : Direction.values()) {
            BlockPos partPos = pos.relative(direction);
            BlockEntity partBe = level.getBlockEntity(partPos);
            if (partBe instanceof mio_icif_large_fabricator_input_iv_entity inputEntity) {
                if (inputEntity.getStoredEnergy() >= ENERGY_PER_MB) {
                    if (canProduceUuMatter()) {
                        float discount = 0.0f;
                        if (scrapModule != null) {
                            discount = scrapModule.consumeScrapValue((float) ENERGY_PER_MB);
                        }
                        inputEntity.consumeEnergy((long)(ENERGY_PER_MB - discount), false);
                        produceUuMatter();
                        break;
                    }
                }
            }
        }
    }

    private boolean canProduceUuMatter() {
        if (uuMatterTank.getFluidAmount() < uuMatterTank.getCapacity()) {
            return true;
        }
        if (level == null) return false;
        for (Direction direction : Direction.values()) {
            BlockPos partPos = getBlockPos().relative(direction);
            BlockEntity partBe = level.getBlockEntity(partPos);
            if (partBe instanceof mio_icif_large_fabricator_tank_entity tankEntity) {
                if (tankEntity.getUuMatterTank().getFluidAmount() < tankEntity.getUuMatterTank().getCapacity()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void produceUuMatter() {
        if (level == null) return;
        for (Direction direction : Direction.values()) {
            BlockPos partPos = getBlockPos().relative(direction);
            BlockEntity partBe = level.getBlockEntity(partPos);
            if (partBe instanceof mio_icif_large_fabricator_tank_entity tankEntity) {
                if (tankEntity.getUuMatterTank().getFluidAmount() < tankEntity.getUuMatterTank().getCapacity()) {
                    tankEntity.getUuMatterTank().fill(
                        new FluidStack(mio_icif_fluids.UUMATTER.get(), 1),
                        IFluidHandler.FluidAction.EXECUTE);
                    tankEntity.setChanged();
                    setChanged();
                    return;
                }
            }
        }
        uuMatterTank.fill(new FluidStack(mio_icif_fluids.UUMATTER.get(), 1), IFluidHandler.FluidAction.EXECUTE);
        setChanged();
    }

    @Nullable
    private mio_icif_large_fabricator_scrap_entity findScrapModule() {
        if (level == null) return null;
        for (Direction direction : Direction.values()) {
            BlockPos partPos = getBlockPos().relative(direction);
            BlockEntity partBe = level.getBlockEntity(partPos);
            if (partBe instanceof mio_icif_large_fabricator_scrap_entity scrapEntity) {
                return scrapEntity;
            }
        }
        return null;
    }

    private float getCachedScrapValue() {
        mio_icif_large_fabricator_scrap_entity scrapModule = findScrapModule();
        return scrapModule != null ? scrapModule.getScrapValue() : 0.0f;
    }

    private void distributeUuMatterToTanks() {
        if (level == null) return;
        if (uuMatterTank.getFluidAmount() <= 0) return;
        for (Direction direction : Direction.values()) {
            BlockPos partPos = getBlockPos().relative(direction);
            BlockEntity partBe = level.getBlockEntity(partPos);
            if (partBe instanceof mio_icif_large_fabricator_tank_entity tankEntity) {
                if (uuMatterTank.getFluidAmount() <= 0) break;
                if (tankEntity.getUuMatterTank().getFluidAmount() < tankEntity.getUuMatterTank().getCapacity()) {
                    int toDrain = Math.min(uuMatterTank.getFluidAmount(),
                        tankEntity.getUuMatterTank().getCapacity() - tankEntity.getUuMatterTank().getFluidAmount());
                    FluidStack drained = uuMatterTank.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
                    if (!drained.isEmpty()) {
                        tankEntity.getUuMatterTank().fill(drained, IFluidHandler.FluidAction.EXECUTE);
                        tankEntity.setChanged();
                    }
                }
            }
        }
    }

    private void revalidateAfterLoad(Level level, BlockPos pos) {
        mio_icif_multiblock_manager<?> existing =
            mio_icif_multiblock_manager.getStructureByController(level, pos);
        if (existing != null && existing.isValid()) {
            return;
        }
        mio_icif_large_fabricator_validator validator =
            new mio_icif_large_fabricator_validator();
        mio_icif_multiblock_manager<mio_icif_large_fabricator_validator> manager =
            new mio_icif_multiblock_manager<>(pos, validator);
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
        Object tankObj = data.get("tankModuleCount");
        Object scrapObj = data.get("scrapModuleCount");

        this.inputModuleCount = (inputObj instanceof Integer) ? (Integer) inputObj : 0;
        this.tankModuleCount = (tankObj instanceof Integer) ? (Integer) tankObj : 0;
        this.scrapModuleCount = (scrapObj instanceof Integer) ? (Integer) scrapObj : 0;

        setChanged();
    }

    @Override
    public void onMultiblockBroken() {
        this.multiblockStructure = null;
        this.setStructureComplete(false);
        this.inputModuleCount = 0;
        this.tankModuleCount = 0;
        this.scrapModuleCount = 0;
        setChanged();
    }

    public int getInputModuleCount() { return inputModuleCount; }
    public int getTankModuleCount() { return tankModuleCount; }
    public int getScrapModuleCount() { return scrapModuleCount; }
    public float getScrapValue() { return getCachedScrapValue(); }
    public FluidTank getUuMatterTank() { return uuMatterTank; }
    public ContainerData getContainerData() { return dataAccess; }

    public int getTotalUuMatterAmount() {
        int total = uuMatterTank.getFluidAmount();
        if (level != null) {
            for (Direction direction : Direction.values()) {
                BlockPos partPos = getBlockPos().relative(direction);
                BlockEntity partBe = level.getBlockEntity(partPos);
                if (partBe instanceof mio_icif_large_fabricator_tank_entity tankEntity) {
                    total += tankEntity.getUuMatterTank().getFluidAmount();
                }
            }
        }
        return total;
    }

    public int getTotalUuMatterCapacity() {
        int total = uuMatterTank.getCapacity();
        if (level != null) {
            for (Direction direction : Direction.values()) {
                BlockPos partPos = getBlockPos().relative(direction);
                BlockEntity partBe = level.getBlockEntity(partPos);
                if (partBe instanceof mio_icif_large_fabricator_tank_entity tankEntity) {
                    total += tankEntity.getUuMatterTank().getCapacity();
                }
            }
        }
        return total;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.large_fabricator_core");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LargeFabricatorCoreMenu(containerId, playerInventory, this);
    }

    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return uuMatterTank;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("inputModuleCount", inputModuleCount);
        tag.putInt("tankModuleCount", tankModuleCount);
        tag.putInt("scrapModuleCount", scrapModuleCount);
        CompoundTag fluidTag = new CompoundTag();
        uuMatterTank.writeToNBT(registries, fluidTag);
        tag.put("UuMatterTank", fluidTag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inputModuleCount")) {
            inputModuleCount = tag.getInt("inputModuleCount");
        }
        if (tag.contains("tankModuleCount")) {
            tankModuleCount = tag.getInt("tankModuleCount");
        }
        if (tag.contains("scrapModuleCount")) {
            scrapModuleCount = tag.getInt("scrapModuleCount");
        }
        if (tag.contains("UuMatterTank")) {
            CompoundTag fluidTag = tag.getCompound("UuMatterTank");
            uuMatterTank.readFromNBT(registries, fluidTag);
        }
        if (isStructureComplete()) {
            needsRevalidation = true;
        }
    }
}
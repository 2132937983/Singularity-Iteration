package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.blockentity.MachineConversion;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.common.menu.producer.LargeFabricatorCoreMenu;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockStructure;
import com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyCore;
import com.miophas.singularity_iteration.common.multiblock.mio_icif_large_fabricator_validator;
import com.miophas.singularity_iteration.common.multiblock.mio_icif_multiblock_manager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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

        AbstractEnergyBlockEntity.tick(level, pos, state, blockEntity);

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

    /**
     * For recipe viewers: {@link #ENERGY_PER_MB} EU per mB of UU-matter; the scrap module's value pays
     * that EU one for one.
     */
    public static List<MachineConversion> conversions() {
        var core = mio_icif_blocks.LARGE_FABRICATOR_CORE.get();
        var uu = mio_icif_fluids.UUMATTER.get();
        List<MachineConversion> all = new ArrayList<>();
        all.add(MachineConversion.of(core, mio_icif_blocks.LARGE_FABRICATOR_INPUT_IV.get(), mio_icif_blocks.LARGE_FABRICATOR_TANK.get())
            .fluidOut(uu, 1).note("conversion.mio_icif.fabricator.energy", ENERGY_PER_MB).build());
        record Scrap(ItemStack stack, int value) {}
        for (Scrap scrap : List.of(
                new Scrap(new ItemStack(mio_icif_normal.SCRAP.get()), mio_icif_large_fabricator_scrap_entity.SCRAP_BONUS),
                new Scrap(new ItemStack(mio_icif_normal.SCRAPBOX.get()), mio_icif_large_fabricator_scrap_entity.SCRAPBOX_BONUS),
                new Scrap(new ItemStack(mio_icif_resources.THORIUM_SCRAP.get()), mio_icif_large_fabricator_scrap_entity.THORIUM_SCRAP_BONUS))) {
            all.add(MachineConversion.of(core, mio_icif_blocks.LARGE_FABRICATOR_SCRAP.get())
                .itemIn(scrap.stack()).fluidOut(uu, 1)
                .note("conversion.mio_icif.fabricator.energy", ENERGY_PER_MB)
                .note("conversion.mio_icif.fabricator.saves", scrap.value()).build());
        }
        return all;
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

    /** EN: Game time of the last produced millibucket. ZH: 最后一次产出 1 mB 的游戏时间。JA: 最後に 1 mB 生産したゲーム時刻。 */
    private long lastProducedTick = Long.MIN_VALUE / 2;

    /**
     * <p><b>EN:</b> 0.1.8.1: RUNNING while the core produced UU-matter in the last second. The status
     * tracker syncs this state to clients (lamp and loop sound).
     * <p><b>ZH:</b> 0.1.8.1：核心在最近 1 秒内产出过 UU 物质时为 RUNNING。状态追踪器把此状态同步到客户端（指示灯与循环音效）。
     * <p><b>JA:</b> 0.1.8.1：直近 1 秒以内に UU 物質を生産していれば RUNNING。状態トラッカーがクライアントへ同期する（ランプとループ音）。
     */
    @Override
    public com.miophas.singularity_iteration.core.api.machine.MachineStatus machineStatus() {
        if (level != null && level.getGameTime() - lastProducedTick <= 20)
            return com.miophas.singularity_iteration.core.api.machine.MachineStatus.RUNNING;
        return super.machineStatus();
    }

    private void produceUuMatter() {
        if (level == null) return;
        lastProducedTick = level.getGameTime();
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
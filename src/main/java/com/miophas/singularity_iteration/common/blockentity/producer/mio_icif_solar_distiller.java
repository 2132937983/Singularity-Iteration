package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotType;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.machine.ISlotLayout;
import com.miophas.singularity_iteration.core.runtime.processing.OwnedFluidConversion;
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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * 澶槼鑳借捀棣忔満鏂瑰潡瀹炰綋
 * 完全依靠能工作，不消耗任何EU，也没有任何电压等级
 *
 * 宸ヤ綔蹇呰鏉′欢:
 * - 在主世界
 * - 白天时间?:20 ~ 17:45（游戏时间）
 * - 天气晴朗（无下雨/下雪?
 * - 姝ｄ笂鏂规棤鏂瑰潡閬尅
 *
 * 姣?0 tick锛?绉掞級娑堣€?1mB 姘达紝浜у嚭 1mB 钂搁姘?
 */
@SuppressWarnings("null")
public class mio_icif_solar_distiller extends BlockEntity implements MenuProvider, com.miophas.singularity_iteration.core.api.machine.IProducerBlock {
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
                    return upgradeStats;
                }
                @Override public void markUnsaved() { ContainerToTank.markUnsaved(mio_icif_solar_distiller.this); }
                @Override public void markNeighborUnsaved(BlockPos pos) {
                    com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent
                        .markNeighborUnsaved(level, pos);
                }
            });


    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(2)
        .output(2)
        .upgrade(2)
        .build();

    // 槽位定义（按 builder 调用顺序：EXTRA=0-1, OUTPUT=2-3, UPGRADE=4-5?
    public static final int TOTAL_SLOTS = 6;
    public static final int WATER_INPUT_SLOT = 0;      // 水输入槽 (EXTRA)
    public static final int DISTILLED_INPUT_SLOT = 1;  // 钂搁姘磋緭鍏ユЫ (EXTRA)
    public static final int WATER_OUTPUT_SLOT = 2;     // 水输出槽 (OUTPUT)
    public static final int DISTILLED_OUTPUT_SLOT = 3; // 钂搁姘磋緭鍑烘Ы (OUTPUT)
    public static final int UPGRADE_SLOT_1 = 4;        // 升级? (UPGRADE)
    public static final int UPGRADE_SLOT_2 = 5;        // 升级? (UPGRADE)

    // 流体容量
    public static final int WATER_TANK_CAPACITY = 10000;          // 水槽 10 ?
    public static final int DISTILLED_TANK_CAPACITY = 10000;      // 钂搁姘存Ы 10 妗?
    // 工作参数
    public static final int PRODUCTION_INTERVAL = 80; // ?0 tick 产出 1mB
    public static final int FLUID_PER_CYCLE = 1;      // 姣忔寰幆 1mB

    // 发电时间段（与太阳能发电机一致）
    public static final int GENERATION_START_TIME = 333;
    public static final int GENERATION_END_TIME = 11750;

    // 流体存储
    protected final FluidTank waterTank;
    protected final FluidTank distilledTank;

    // 槽位布局和物品存?
    protected final SlotLayout slotLayout;
    protected MachineItemHandler itemHandler;

    // 进度
    private int progress = 0;
    // 鏄惁姝ｅ湪宸ヤ綔
    protected boolean isWorking = false;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> isWorking ? 1 : 0;
                case 1 -> progress;
                case 2 -> waterTank.getFluidAmount();
                case 3 -> distilledTank.getFluidAmount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() { return 4; }
    };

    // 鍗囩骇缁勪欢妲戒綅閰嶇疆
    @SuppressWarnings("unused")
    private int upgradeSlotStart = -1;
    @SuppressWarnings("unused")
    private int upgradeSlotCount = 0;
    private MachineUpgradeStats upgradeStats = MachineUpgradeStats.empty();

    public mio_icif_solar_distiller(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.SOLAR_DISTILLER_ENTITY_TYPE.get());
    }

    public mio_icif_solar_distiller(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type, pos, state);

        this.slotLayout = LAYOUT;

        this.waterTank = new FluidTank(WATER_TANK_CAPACITY, fluidStack -> {
            if (fluidStack.isEmpty()) return true;
            return fluidStack.getFluid() == Fluids.WATER;
        }) {
            @Override protected void onContentsChanged() { ContainerToTank.markUnsaved(mio_icif_solar_distiller.this); }
        };

        this.distilledTank = new FluidTank(DISTILLED_TANK_CAPACITY, fluidStack -> {
            if (fluidStack.isEmpty()) return true;
            return fluidStack.getFluid() == mio_icif_fluids.DISTILLEDWATER.get();
        }) {
            @Override protected void onContentsChanged() { ContainerToTank.markUnsaved(mio_icif_solar_distiller.this); }
        };

        this.itemHandler = new MachineItemHandler(LAYOUT) {
            @Override
            protected void onContentsChanged(int slot) { setChanged(); }
        };
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_solar_distiller.this.isItemValidForSlot(slot, stack));

        setUpgradeSlots(UPGRADE_SLOT_1, 2);
    }

    private void setUpgradeSlots(int start, int count) {
        this.upgradeSlotStart = start;
        this.upgradeSlotCount = count;
    }

    private boolean isUpgradeSlot(int slot) {
        return slotLayout.isType(slot, SlotType.UPGRADE);
    }

    public MachineUpgradeStats getUpgradeStats() {
        return upgradeStats;
    }

    private void recalculateUpgradeStats() {
        int start = slotLayout.getStart(SlotType.UPGRADE);
        int count = slotLayout.getCount(SlotType.UPGRADE);
        this.upgradeStats = MachineUpgradeStats.fromInventory(itemHandler, start, count);
    }

    /**
     * 姣弔ick 鏇存柊閫昏緫
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_solar_distiller blockEntity) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)
                || !server.getServer().isSameThread() || blockEntity.isRemoved()) return;

        blockEntity.recalculateUpgradeStats();

        blockEntity.handleWaterBucketSlot();
        blockEntity.handleDistilledBucketSlot();

        boolean wasWorking = blockEntity.isWorking;
        blockEntity.isWorking = blockEntity.canWork(level, pos);

        if (blockEntity.isWorking) {
            blockEntity.progress = Math.min(PRODUCTION_INTERVAL, blockEntity.progress + 1);
            if (blockEntity.progress >= PRODUCTION_INTERVAL) {
                blockEntity.produceDistilledWater();
            }
        } else {
            blockEntity.progress = 0;
        }

        if (wasWorking != blockEntity.isWorking) {
            blockEntity.updateBlockState(blockEntity.isWorking);
        }

        blockEntity.handleFluidUpgrades();

        blockEntity.setChanged();
    }

    /**
     * 妫€鏌ユ槸鍚﹀彲浠ュ伐浣?
     */
    private boolean canWork(Level level, BlockPos pos) {
        if (prepareDistillation().isEmpty()) return false;

        // 妫€鏌ョ淮搴?
        if (level.dimension() != Level.OVERWORLD) return false;

        // 妫€鏌ュぉ姘?
        if (level.isRaining() || level.isThundering()) return false;

        // 妫€鏌ユ椂闂?
        long timeOfDay = level.getDayTime() % 24000;
        if (timeOfDay < GENERATION_START_TIME || timeOfDay > GENERATION_END_TIME) return false;

        // 妫€鏌ュぉ绌烘槸鍚﹁閬尅
        return level.canSeeSky(pos.above());
    }

    /**
     * 浜у嚭钂搁姘?
     */
    private java.util.Optional<OwnedFluidConversion.Prepared> prepareDistillation() {
        return OwnedFluidConversion.prepare(waterTank, new FluidStack(Fluids.WATER, FLUID_PER_CYCLE),
            distilledTank, new FluidStack(mio_icif_fluids.DISTILLEDWATER.get(), FLUID_PER_CYCLE), itemHandler, -1, ItemStack.EMPTY);
    }

    private void produceDistilledWater() {
        var operation = prepareDistillation();
        if (operation.isEmpty()) return;
        int before = progress;
        progress = 0;
        if (!operation.get().commit()) progress = before;
    }

    /**
     * 澶勭悊姘存《杈撳叆锛氬皢姘翠粠姘村崟鍏冧腑鐨勬按杞叆姘存Ы锛岀┖瀹瑰櫒绉诲埌姘磋緭鍑烘Ы
     */
    private void handleWaterBucketSlot() {
        var input = itemHandler.getStackInSlot(WATER_INPUT_SLOT);
        if (input.isEmpty()) return;
        boolean cell = mio_icif_cells.isCellContainingFluid(input, Fluids.WATER);
        if (!cell && !input.is(Items.WATER_BUCKET)) return;
        var single = input.copyWithCount(1);
        var content = cell ? mio_icif_cells.getCellFluid(single) : new FluidStack(Fluids.WATER, 1000);
        var empty = cell ? mio_icif_cells.getEmptyCellForStack(single) : new ItemStack(Items.BUCKET);
        if (ContainerToTank.transfer(itemHandler, WATER_INPUT_SLOT, WATER_OUTPUT_SLOT, waterTank, content, empty)) setChanged();
    }

    /**
     * 澶勭悊钂搁姘存《杈撳叆锛氱敤绌哄崟鍏?绌烘《浠庤捀棣忔按妲藉彇姘达紝绉诲埌钂搁姘磋緭鍑烘Ы
     */
    private void handleDistilledBucketSlot() {
        var input = itemHandler.getStackInSlot(DISTILLED_INPUT_SLOT);
        if (input.isEmpty()) return;
        boolean cell = mio_icif_cells.isEmptyCell(input);
        if (!cell && !input.is(Items.BUCKET)) return;
        var filled = cell ? mio_icif_cells.getFilledCellForFluidStack(mio_icif_fluids.DISTILLEDWATER.get())
            : new ItemStack(mio_icif_fluids.DISTILLEDWATER_BUCKET.get());
        if (filled.isEmpty()) return;
        var content = cell ? mio_icif_cells.getCellFluid(filled.copyWithCount(1)) : new FluidStack(mio_icif_fluids.DISTILLEDWATER.get(), 1000);
        if (ContainerToTank.drainToContainer(itemHandler, DISTILLED_INPUT_SLOT, DISTILLED_OUTPUT_SLOT, distilledTank, content, filled)) setChanged();
    }





    private void updateBlockState(boolean working) {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (state.hasProperty(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_solar_distiller.LIT)) {
            level.setBlock(worldPosition, state.setValue(
                com.miophas.singularity_iteration.common.block.producer.mio_icif_block_solar_distiller.LIT, working), 3);
        }
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (isUpgradeSlot(slot)) {
            return MioIcifAPI.instance().getItemAPI().isUpgrade(stack);
        }
        return switch (slot) {
            case WATER_INPUT_SLOT -> stack.is(Items.WATER_BUCKET) || mio_icif_cells.isCellContainingFluid(stack, Fluids.WATER);
            case DISTILLED_INPUT_SLOT -> stack.is(Items.BUCKET) || mio_icif_cells.isEmptyCell(stack);
            case WATER_OUTPUT_SLOT -> false; // 浠呰緭鍑猴紝涓嶆帴鍙楁墜鍔ㄦ斁鍏?
            case DISTILLED_OUTPUT_SLOT -> false; // 浠呰緭鍑猴紝涓嶆帴鍙楁墜鍔ㄦ斁鍏?
            default -> false;
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.solar_distiller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.producer.SolarDistillerMenu(id, playerInventory, this);
    }

    public ContainerData getContainerData() { return containerData; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        fluidAutomation.save(tag, registries);
        tag.put("waterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.put("distilledTank", distilledTank.writeToNBT(registries, new CompoundTag()));
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putBoolean("isWorking", isWorking);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluidAutomation.load(tag, registries);
        if (tag.contains("waterTank")) waterTank.readFromNBT(registries, tag.getCompound("waterTank"));
        if (tag.contains("distilledTank")) distilledTank.readFromNBT(registries, tag.getCompound("distilledTank"));
        if (tag.contains("inventory")) itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = Math.max(0, Math.min(PRODUCTION_INTERVAL, tag.getInt("progress")));
        isWorking = tag.getBoolean("isWorking");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag(); saveAdditional(tag, registries); return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) { loadAdditional(tag, registries); }

    // ==================== Capability ====================

    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return itemHandler;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup.inputOutput(waterTank, distilledTank);
    }

    /**
     * 澶勭悊娴佷綋鑷姩鍖栧崌绾?
     */
    private void handleFluidUpgrades() {
        fluidAutomation.runAutomation();
    }

    /**
     * 鑾峰彇鐩搁偦浣嶇疆鐨勬祦浣撳鐞嗗櫒
     */
    @Nullable
    private IFluidHandler getAdjacentFluidHandler(BlockPos pos, @Nullable Direction side) {
        if (level == null || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return null;
        }
        BlockEntity target = level.getBlockEntity(pos);
        if (target == null) {
            return null;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
    }

    /**
     * 娴佷綋寮瑰嚭鍗囩骇
     */
    private void ejectFluids(IFluidHandler own, int upgradeCount) {
        fluidAutomation.ejectFluids(own, upgradeCount);
    }

    /**
     * 娴佷綋鎶藉叆鍗囩骇
     */
    private void pullFluids(IFluidHandler own, int upgradeCount) {
        fluidAutomation.pullFluids(own, upgradeCount);
    }

    public FluidTank getWaterTank() { return waterTank; }
    public FluidTank getDistilledTank() { return distilledTank; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return PRODUCTION_INTERVAL; }
    public boolean isWorking() { return isWorking; }

    // ==================== IProducerBlock API ====================

    @Override
    public void forceStartWork() {
        this.isWorking = true;
        this.progress = Math.min(progress + 1, PRODUCTION_INTERVAL);
    }

    @Override
    public void forceStopWork() {
        this.isWorking = false;
        this.progress = 0;
    }

    @Override
    public int getUpgradeSlotStart() {
        return UPGRADE_SLOT_1;
    }

    @Override
    public int getUpgradeSlotCount() {
        return 2;
    }

    @Override
    public java.util.List<ItemStack> getUpgrades() {
        java.util.List<ItemStack> upgrades = new java.util.ArrayList<>();
        for (int i = UPGRADE_SLOT_1; i <= UPGRADE_SLOT_2; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                upgrades.add(stack);
            }
        }
        return upgrades;
    }

    @Override
    public int getBaseMaxProgress() {
        return PRODUCTION_INTERVAL;
    }

    @Override
    public long getEnergyPerTick() {
        return 0;
    }

    @Override
    public ISlotLayout getSlotLayout() {
        return slotLayout;
    }

    @Override
    public int getContainerSize() {
        return itemHandler.getSlots();
    }

    @Override
    public com.miophas.singularity_iteration.core.api.energy.IEnergyStorageAccess getEnergyStorage() {
        return null;
    }

    @Override
    public com.miophas.singularity_iteration.core.api.machine.IWorkCompleteCallback getWorkCompleteCallback() {
        return null;
    }

    @Override
    public long getEffectiveEnergyPerTick() {
        return 0;
    }

    @Override
    public long getTotalProcessed() {
        return 0;
    }

    /**
     * 缁勫悎娴佷綋澶勭悊鍣?
     */
}

package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotType;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Items.Upgrade.MachineUpgradeStats;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.machine.ISlotLayout;
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
import org.jetbrains.annotations.Nullable;

/**
 * 太阳能蒸馏机方块实体
 * 完全依靠太阳能工作，不消耗任何EU，也没有任何电压等级
 *
 * 工作必要条件:
 * - 在主世界
 * - 白天时间：6:20 ~ 17:45（游戏时间）
 * - 天气晴朗（无下雨/下雪）
 * - 正上方无方块遮挡
 *
 * 每80 tick（4秒）消耗 1mB 水，产出 1mB 蒸馏水
 */
@SuppressWarnings("null")
public class mio_icif_solar_distiller extends BlockEntity implements MenuProvider, com.singularity_iteration.mio_icif.api.machine.IProducerBlock {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(2)
        .output(2)
        .upgrade(2)
        .build();

    // 槽位定义（按 builder 调用顺序：EXTRA=0-1, OUTPUT=2-3, UPGRADE=4-5）
    public static final int TOTAL_SLOTS = 6;
    public static final int WATER_INPUT_SLOT = 0;      // 水输入槽 (EXTRA)
    public static final int DISTILLED_INPUT_SLOT = 1;  // 蒸馏水输入槽 (EXTRA)
    public static final int WATER_OUTPUT_SLOT = 2;     // 水输出槽 (OUTPUT)
    public static final int DISTILLED_OUTPUT_SLOT = 3; // 蒸馏水输出槽 (OUTPUT)
    public static final int UPGRADE_SLOT_1 = 4;        // 升级槽1 (UPGRADE)
    public static final int UPGRADE_SLOT_2 = 5;        // 升级槽2 (UPGRADE)

    // 流体容量
    public static final int WATER_TANK_CAPACITY = 10000;          // 水槽 10 桶
    public static final int DISTILLED_TANK_CAPACITY = 10000;      // 蒸馏水槽 10 桶
    // 工作参数
    public static final int PRODUCTION_INTERVAL = 80; // 每80 tick 产出 1mB
    public static final int FLUID_PER_CYCLE = 1;      // 每次循环 1mB

    // 发电时间段（与太阳能发电机一致）
    public static final int GENERATION_START_TIME = 333;
    public static final int GENERATION_END_TIME = 11750;

    // 流体存储
    protected final FluidTank waterTank;
    protected final FluidTank distilledTank;

    // 槽位布局和物品存储
    protected final SlotLayout slotLayout;
    protected MachineItemHandler itemHandler;

    // 进度
    private int progress = 0;
    // 是否正在工作
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

    // 升级组件槽位配置
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
        });

        this.distilledTank = new FluidTank(DISTILLED_TANK_CAPACITY, fluidStack -> {
            if (fluidStack.isEmpty()) return true;
            return fluidStack.getFluid() == mio_icif_fluids.DISTILLEDWATER.get();
        });

        this.itemHandler = new MachineItemHandler(LAYOUT) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
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
     * 每tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_solar_distiller blockEntity) {
        if (level.isClientSide()) return;

        blockEntity.recalculateUpgradeStats();

        blockEntity.handleWaterBucketSlot();
        blockEntity.handleDistilledBucketSlot();

        boolean wasWorking = blockEntity.isWorking;
        blockEntity.isWorking = blockEntity.canWork(level, pos);

        if (blockEntity.isWorking) {
            blockEntity.progress++;
            if (blockEntity.progress >= PRODUCTION_INTERVAL) {
                blockEntity.progress = 0;
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
     * 检查是否可以工作
     */
    private boolean canWork(Level level, BlockPos pos) {
        if (waterTank.getFluidAmount() < FLUID_PER_CYCLE) return false;
        if (distilledTank.getFluidAmount() + FLUID_PER_CYCLE > distilledTank.getCapacity()) return false;

        // 检查维度
        if (level.dimension() != Level.OVERWORLD) return false;

        // 检查天气
        if (level.isRaining() || level.isThundering()) return false;

        // 检查时间
        long timeOfDay = level.getDayTime() % 24000;
        if (timeOfDay < GENERATION_START_TIME || timeOfDay > GENERATION_END_TIME) return false;

        // 检查天空是否被遮挡
        return level.canSeeSky(pos.above());
    }

    /**
     * 产出蒸馏水
     */
    private void produceDistilledWater() {
        FluidStack drained = waterTank.drain(FLUID_PER_CYCLE, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() >= FLUID_PER_CYCLE) {
            distilledTank.fill(new FluidStack(mio_icif_fluids.DISTILLEDWATER.get(), FLUID_PER_CYCLE), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /**
     * 处理水桶输入：将水从水单元中的水转入水槽，空容器移到水输出槽
     */
    private void handleWaterBucketSlot() {
        ItemStack input = itemHandler.getStackInSlot(WATER_INPUT_SLOT);
        if (input.isEmpty()) return;

        int fillAmount = 1000;
        if (input.is(Items.WATER_BUCKET)) {
            if (waterTank.getFluidAmount() + fillAmount > waterTank.getCapacity()) return;
            if (!canInsertOrMerge(WATER_OUTPUT_SLOT, new ItemStack(Items.BUCKET))) return;

            waterTank.fill(new FluidStack(Fluids.WATER, fillAmount), IFluidHandler.FluidAction.EXECUTE);
            input.shrink(1);
            insertOrMerge(WATER_OUTPUT_SLOT, new ItemStack(Items.BUCKET));
            setChanged();
        } else if (mio_icif_cells.isCellContainingFluid(input, Fluids.WATER)) {
            if (waterTank.getFluidAmount() + fillAmount > waterTank.getCapacity()) return;
            ItemStack emptyCell = mio_icif_cells.getEmptyCellForStack(input);
            if (emptyCell.isEmpty()) emptyCell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
            if (!canInsertOrMerge(WATER_OUTPUT_SLOT, emptyCell)) return;

            waterTank.fill(new FluidStack(Fluids.WATER, fillAmount), IFluidHandler.FluidAction.EXECUTE);
            input.shrink(1);
            insertOrMerge(WATER_OUTPUT_SLOT, emptyCell);
            setChanged();
        }
    }

    /**
     * 处理蒸馏水桶输入：用空单元/空桶从蒸馏水槽取水，移到蒸馏水输出槽
     */
    private void handleDistilledBucketSlot() {
        ItemStack input = itemHandler.getStackInSlot(DISTILLED_INPUT_SLOT);
        if (input.isEmpty()) return;

        int drainAmount = 1000;
        if (input.is(Items.BUCKET)) {
            if (distilledTank.getFluidAmount() < drainAmount) return;
            if (!canInsertOrMerge(DISTILLED_OUTPUT_SLOT, new ItemStack(mio_icif_fluids.DISTILLEDWATER_BUCKET.get()))) return;

            distilledTank.drain(drainAmount, IFluidHandler.FluidAction.EXECUTE);
            input.shrink(1);
            insertOrMerge(DISTILLED_OUTPUT_SLOT, new ItemStack(mio_icif_fluids.DISTILLEDWATER_BUCKET.get()));
            setChanged();
        } else if (mio_icif_cells.isEmptyCell(input)) {
            if (distilledTank.getFluidAmount() < drainAmount) return;
            ItemStack filledCell = mio_icif_cells.getFilledCellForFluidStack(mio_icif_fluids.DISTILLEDWATER.get());
            if (!canInsertOrMerge(DISTILLED_OUTPUT_SLOT, filledCell)) return;

            distilledTank.drain(drainAmount, IFluidHandler.FluidAction.EXECUTE);
            input.shrink(1);
            insertOrMerge(DISTILLED_OUTPUT_SLOT, filledCell);
            setChanged();
        }
    }

    private boolean canInsertOrMerge(int slot, ItemStack stack) {
        ItemStack current = itemHandler.getStackInSlot(slot);
        if (current.isEmpty()) return true;
        if (ItemStack.isSameItemSameComponents(current, stack)) {
            return current.getCount() + stack.getCount() <= itemHandler.getSlotLimit(slot);
        }
        return false;
    }

    private void insertOrMerge(int slot, ItemStack stack) {
        ItemStack current = itemHandler.getStackInSlot(slot);
        if (current.isEmpty()) {
            itemHandler.setStackInSlot(slot, stack);
        } else if (ItemStack.isSameItemSameComponents(current, stack)) {
            current.grow(stack.getCount());
        }
    }

    private void updateBlockState(boolean working) {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (state.hasProperty(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_solar_distiller.LIT)) {
            level.setBlock(worldPosition, state.setValue(
                com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_solar_distiller.LIT, working), 3);
        }
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (isUpgradeSlot(slot)) {
            return MioIcifAPI.instance().getItemAPI().isUpgrade(stack);
        }
        return switch (slot) {
            case WATER_INPUT_SLOT -> stack.is(Items.WATER_BUCKET) || mio_icif_cells.isCellContainingFluid(stack, Fluids.WATER);
            case DISTILLED_INPUT_SLOT -> stack.is(Items.BUCKET) || mio_icif_cells.isEmptyCell(stack);
            case WATER_OUTPUT_SLOT -> false; // 仅输出，不接受手动放入
            case DISTILLED_OUTPUT_SLOT -> false; // 仅输出，不接受手动放入
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
        return new com.singularity_iteration.mio_icif.Menu.Producer.SolarDistillerMenu(id, playerInventory, this);
    }

    public ContainerData getContainerData() { return containerData; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("waterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.put("distilledTank", distilledTank.writeToNBT(registries, new CompoundTag()));
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putBoolean("isWorking", isWorking);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("waterTank")) waterTank.readFromNBT(registries, tag.getCompound("waterTank"));
        if (tag.contains("distilledTank")) distilledTank.readFromNBT(registries, tag.getCompound("distilledTank"));
        if (tag.contains("inventory")) itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getInt("progress");
        isWorking = tag.getBoolean("isWorking");
    }

    // ==================== Capability ====================

    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return itemHandler;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return new CombinedFluidHandler(waterTank, distilledTank);
    }

    /**
     * 处理流体自动化升级
     */
    private void handleFluidUpgrades() {
        if (level == null || level.isClientSide()) {
            return;
        }
        IFluidHandler own = getFluidHandlerCapability(null);
        if (own == null) {
            return;
        }
        if (upgradeStats.fluidEjectorCount > 0) {
            ejectFluids(own, upgradeStats.fluidEjectorCount);
        }
        if (upgradeStats.fluidPullingCount > 0) {
            pullFluids(own, upgradeStats.fluidPullingCount);
        }
    }

    /**
     * 获取相邻位置的流体处理器
     */
    @Nullable
    private IFluidHandler getAdjacentFluidHandler(BlockPos pos, @Nullable Direction side) {
        if (level == null) {
            return null;
        }
        BlockEntity target = level.getBlockEntity(pos);
        if (target == null) {
            return null;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
    }

    /**
     * 流体弹出升级
     */
    private void ejectFluids(IFluidHandler own, int upgradeCount) {
        int maxPerTick = Math.max(1, upgradeCount * 1000);
        for (int tank = own.getTanks() - 1; tank >= 0; tank--) {
            FluidStack fluid = own.getFluidInTank(tank);
            if (fluid.isEmpty()) {
                continue;
            }
            FluidStack toMove = fluid.copyWithAmount(Math.min(fluid.getAmount(), maxPerTick));
            if (toMove.isEmpty()) {
                continue;
            }
            for (Direction dir : Direction.values()) {
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

    /**
     * 流体抽入升级
     */
    private void pullFluids(IFluidHandler own, int upgradeCount) {
        int maxPerTick = Math.max(1, upgradeCount * 1000);
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
            for (Direction dir : Direction.values()) {
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
    public com.singularity_iteration.mio_icif.api.energy.IEnergyStorageAccess getEnergyStorage() {
        return null;
    }

    @Override
    public com.singularity_iteration.mio_icif.api.machine.IWorkCompleteCallback getWorkCompleteCallback() {
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
     * 组合流体处理器
     */
    private static class CombinedFluidHandler implements IFluidHandler {
        private final FluidTank waterTank;
        private final FluidTank distilledTank;

        public CombinedFluidHandler(FluidTank waterTank, FluidTank distilledTank) {
            this.waterTank = waterTank;
            this.distilledTank = distilledTank;
        }

        @Override
        public int getTanks() { return 2; }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? waterTank.getFluid() : distilledTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? waterTank.getCapacity() : distilledTank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 ? waterTank.isFluidValid(stack) : distilledTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (waterTank.isFluidValid(resource)) {
                return waterTank.fill(resource, action);
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return distilledTank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return distilledTank.drain(maxDrain, action);
        }
    }
}
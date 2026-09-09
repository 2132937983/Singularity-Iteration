package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_memory;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * 复制机方块实体类
 * 用于根据记忆水晶中的信息复制物品
 *
 * 槽位定义：
 * - 0: UU物质槽位（输入UU物质，或给液体槽补充UU物质）
 * - 1: 空气单元槽位（输出空气单元）
 * - 2: 记忆水晶槽
 * - 3: 输出槽
 * - 4: 电池槽
 * - 5-8: 四个空闲槽位（升级件槽位）
 *
 * 液体槽位：
 * - UU物质液体槽位（存储UU物质，用于复制）
 *
 * 按钮操作：
 * - 停止按钮：停止当前的复制任务
 * - 单次按钮：执行一次复制（复制一个物品）
 * - 循环按钮：持续复制直到输出槽满或资源不足
 *
 * 工作机制：
 * - 复制过程中不消耗记忆水晶
 * - 根据记忆水晶中的信息输出对应物品
 * - 消耗对应的UU物质与电力
 */
@SuppressWarnings("null")
public class mio_icif_replicator_elc extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .output(1)
        .extra(2)
        .battery()
        .upgrade(4)
        .build();

    // 槽位总数：9个槽位
    public static final int SLOT_COUNT = 9;
    // UU物质槽索引（输入UU物质，或给液体槽补充）
    public static final int UU_CELL_SLOT = 0;
    // 空气单元槽索引（输出空气单元）
    public static final int EMPTY_CELL_SLOT = 1;
    // 记忆水晶槽索引
    public static final int MEMORY_SLOT = 2;
    // 输出槽索引
    public static final int OUTPUT_SLOT = 3;
    // 电池槽索引
    public static final int BATTERY_SLOT = 4;
    // 升级件槽起始索引（4个空闲槽位）
    public static final int UPGRADE_SLOT_START = 5;

    // 默认配置
    public static final long DEFAULT_CAPACITY = 2000000L;  // 2M EU），对应 IC2 原版
    public static final long DEFAULT_MAX_RECEIVE = 8192L;  // EV级输入
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 100; // 工作周期
    public static final long DEFAULT_ENERGY_PER_TICK = 512L; // 每tick耗电（512EU），对应 IC2 原版

    // UU物质液体槽容量（mB）
    public static final int UUMATTER_CAPACITY = 64000; // 64桶

    // 工作模式枚举
    @SuppressWarnings("null")
public enum WorkMode {
        STOPPED,      // 停止
        SINGLE,       // 单次复制
        LOOP          // 循环复制
    }

    // UU物质液体槽
    protected final FluidTank uuMatterTank;
    // 当前工作模式
    protected WorkMode workMode = WorkMode.STOPPED;
    // 是否正在复制中
    protected boolean isReplicating = false;
    // 当前复制进度
    protected int replicateProgress = 0;
    // 当前复制所需的最大进度
    protected int replicateMaxProgress = 100;
    // 当前复制所需UU消耗
    protected long currentUuCost = 0;
    // 当前复制所需EU消耗
    protected long currentEuCost = 0;
    // 当前复制目标物品
    protected ItemStack currentOutputItem = ItemStack.EMPTY;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> replicateProgress;
                case 1 -> replicateMaxProgress;
                case 2 -> isReplicating ? 1 : 0;
                case 3 -> (int) energyStorage.getAmount();
                case 4 -> (int) energyStorage.getCapacity();
                case 5 -> uuMatterTank.getFluidAmount();
                case 6 -> uuMatterTank.getCapacity();
                case 7 -> workMode.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 8;
        }
    };

    /**
     * 用于 BlockEntityType.Builder 的构造函数
     */
    public mio_icif_replicator_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.REPLICATOR_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_replicator_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.EV);

        // 初始化UU物质液体槽（只允许UU物质）
        this.uuMatterTank = new FluidTank(UUMATTER_CAPACITY, fluidStack ->
            fluidStack.getFluid() == mio_icif_fluids.UUMATTER.get());
    }



    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.replicator_elc");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case UU_CELL_SLOT -> isUuMatterCell(stack);
            case EMPTY_CELL_SLOT -> false; // 输出槽位不允许自动插入
            case MEMORY_SLOT -> stack.getItem() instanceof mio_icif_memory;
            case OUTPUT_SLOT -> false; // 输出槽位不允许自动插入
            case BATTERY_SLOT -> isBattery(stack);
            default -> {
                if (slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + 4) {
                    yield getItemAPI().isUpgrade(stack);
                }
                yield false;
            }
        };
    }

    /**
     * 判断物品是否为UU物质单元
     */
    private boolean isUuMatterCell(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        // 判断物品是否为UU物质单元
        return mio_icif_cells.isCellContainingFluid(stack, mio_icif_fluids.UUMATTER.get());
    }

    /**
     * 判断物品是否为空气单元
     */
    @SuppressWarnings("unused")
    private boolean isEmptyCell(ItemStack stack) {
        return mio_icif_cells.isEmptyCell(stack);
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return switch (side) {
            case UP -> new int[]{UU_CELL_SLOT}; // 上方：UU物质
            case DOWN -> new int[]{OUTPUT_SLOT}; // 下方：输出
            case NORTH, SOUTH, EAST, WEST -> new int[]{UU_CELL_SLOT, MEMORY_SLOT, BATTERY_SLOT};
        };
    }

    @Override
    protected int[] getInputSlots() {
        return new int[]{UU_CELL_SLOT};
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

    @Override
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return uuMatterTank;
    }

    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        // 输出槽位和空气单元槽位不允许插入
        if (slot == OUTPUT_SLOT || slot == EMPTY_CELL_SLOT) {
            return false;
        }

        // 电池槽：只接受电池类物品
        if (slot == BATTERY_SLOT) {
            return isBattery(stack);
        }

        // UU物质槽位：只接受UU物质单元
        if (slot == UU_CELL_SLOT) {
            return isUuMatterCell(stack);
        }

        // 记忆水晶槽位：只接受记忆水晶
        if (slot == MEMORY_SLOT) {
            return stack.getItem() instanceof mio_icif_memory;
        }

        if (slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + 4) {
            return false; // 升级槽不允许自动插入（与IC2原版一致）
        }

        return false;
    }

    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        // 只有输出槽位和空气单元槽可以提取
        return slot == OUTPUT_SLOT || slot == EMPTY_CELL_SLOT;
    }

    /**
     * 从UU物质槽位提取UU物质到液体槽
     */
    private void extractUuFromCell() {
        ItemStack uuCellStack = itemHandler.getStackInSlot(UU_CELL_SLOT);
        ItemStack emptyCellStack = itemHandler.getStackInSlot(EMPTY_CELL_SLOT);

        if (!isUuMatterCell(uuCellStack)) {
            return;
        }

        // 检查UU液体槽是否有足够空间（至少1000mB）
        if (uuMatterTank.getSpace() < 1000) {
            return;
        }

        // 检查空气单元槽是否有空位
        ItemStack emptyCell = emptyCellStack.isEmpty() ? mio_icif_cells.getEmptyCellForStack(uuCellStack) : emptyCellStack;
        if (emptyCellStack.isEmpty()) {
            // 可以放入空单元
        } else if (ItemStack.isSameItemSameComponents(emptyCellStack, emptyCell)) {
            if (emptyCellStack.getCount() >= emptyCellStack.getMaxStackSize()) {
                return; // 已满
            }
        } else {
            return; // 槽位被其他物品占用
        }

        // 消耗UU物质并填充到液体槽
        uuCellStack.shrink(1);
        if (uuCellStack.isEmpty()) {
            itemHandler.setStackInSlot(UU_CELL_SLOT, ItemStack.EMPTY);
        }

        // 填充UU液体
        FluidStack uuMatter = new FluidStack(mio_icif_fluids.UUMATTER.get(), 1000);
        uuMatterTank.fill(uuMatter, IFluidHandler.FluidAction.EXECUTE);

        // 输出空气单元
        if (emptyCellStack.isEmpty()) {
            itemHandler.setStackInSlot(EMPTY_CELL_SLOT, emptyCell);
        } else {
            emptyCellStack.grow(1);
        }

        setChanged();
    }

    /**
     * 获取记忆水晶中的扫描数据
     */
    @Nullable
    private ScanResult getMemoryData() {
        ItemStack memoryStack = itemHandler.getStackInSlot(MEMORY_SLOT);
        if (!(memoryStack.getItem() instanceof mio_icif_memory memory)) {
            return null;
        }

        if (!memory.hasData(memoryStack)) {
            return null;
        }

        // 从记忆水晶读取数据
        ItemStack storedItem = memory.getStoredItemStack(memoryStack);
        if (storedItem.isEmpty()) {
            // 尝试使用物品名称重建
            String itemName = memory.getStoredItemName(memoryStack);
            if (itemName.isEmpty()) {
                return null;
            }
            // 暂不支持名称重建，返回null
            return null;
        }

        double uuCostBuckets = memory.getUuMatterCost(memoryStack);
        long euCost = memory.getEnergyCost(memoryStack);

        return new ScanResult(storedItem, uuCostBuckets, euCost);
    }

    private record ScanResult(ItemStack itemStack, double uuMatterCostBuckets, long energyCost) {
        long getUuMatterCostMB() {
            return (long) (uuMatterCostBuckets * 1000.0);
        }
    }

    @Override
    protected boolean canWork() {
        // 首先尝试从UU物质单元提取UU物质
        extractUuFromCell();

        // 如果处于停止模式且非正常复制中断（有残留进度值需要完成）
        if (workMode == WorkMode.STOPPED && !isReplicating) {
            // 检查是否有残留进度值（部分消耗但未完成）
            if (replicateProgress > 0 && uuConsumed > 0 && uuConsumed < currentUuCost) {
                // 保留残留进度，检查是否可以继续
                return canResumeReplicating();
            }
            return false;
        }

        // 如果正在复制中，继续工作
        if (isReplicating) {
            return canContinueReplicating();
        }

        // 检查是否可以开始新的复制
        return canStartReplicating();
    }

    /**
     * 判断是否可以恢复残留的复制
     */
    private boolean canResumeReplicating() {
        // 检查UU物质是否足够（至少需要1mB）
        if (uuMatterTank.getFluidAmount() < 1) {
            return false;
        }

        // 检查输出槽是否还可以放入
        if (!currentOutputItem.isEmpty()) {
            ItemStack outputStack = itemHandler.getStackInSlot(OUTPUT_SLOT);
            if (!outputStack.isEmpty() && !ItemStack.isSameItemSameComponents(outputStack, currentOutputItem)) {
                return false;
            }
            if (!outputStack.isEmpty() && outputStack.getCount() >= outputStack.getMaxStackSize()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 判断是否可以开始新的复制
     */
    private boolean canStartReplicating() {
        // 检查记忆水晶
        ScanResult result = getMemoryData();
        if (result == null) {
            return false;
        }

        // 检查UU物质
        if (uuMatterTank.getFluidAmount() < result.getUuMatterCostMB()) {
            return false;
        }

        // 检查能量
        if (energyStorage.getAmount() < result.energyCost) {
            return false;
        }

        // 检查输出槽
        if (!canOutputItem(result.itemStack)) {
            return false;
        }

        return true;
    }

    /**
     * 判断是否可以继续复制中
     */
    private boolean canContinueReplicating() {
        // 检查能量
        if (!hasEnoughEnergy()) {
            return false;
        }

        return true;
    }

    /**
     * 判断是否可以输出物品
     */
    private boolean canOutputItem(ItemStack targetItem) {
        ItemStack outputStack = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (targetItem.isEmpty()) {
            return false;
        }

        if (outputStack.isEmpty()) {
            return true;
        }

        // 检查物品是否可以堆叠
        if (!ItemStack.isSameItemSameComponents(outputStack, targetItem)) {
            return false;
        }

        // 检查是否还有空间
        return outputStack.getCount() < outputStack.getMaxStackSize();
    }

    /**
     * 根据扫描结果重建物品
     */
    private ItemStack createItemFromScanResult(ScanResult result) {
        if (result == null || result.itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return result.itemStack.copy();
    }

    @Override
    protected void doWork() {
        isWorking = true;

        if (!isReplicating) {
            // 检查是否有残留进度值需要完成
            if (replicateProgress > 0 && uuConsumed > 0 && uuConsumed < currentUuCost) {
                // 恢复残留的复制
                resumeReplicating();
            } else {
                // 开始新的复制
                startReplicating();
            }
        } else {
            // 继续复制
            continueReplicating();
        }
    }

    // 每tick基础消耗的EU - 对应原版IC2
    private static final double EU_PER_TICK_BASE = 512.0D;   // 512 EU/tick
    // 当前复制过程中已消耗的UU物质
    private int uuConsumed = 0;
    // 额外累积的UU物质（小数部分累积）
    private double extraUuStored = 0.0D;

    /**
     * 获取每tick的UU物质消耗（根据记忆水晶记录的UU消耗计算）
     */
    private double getUuPerTick() {
        // 根据记忆水晶记录的UU消耗，平均分配到每个tick
        // UU物质消耗受频率升级影响（越快 = 每tick时消耗更多UU）
        if (replicateMaxProgress <= 0) return 0;
        double uuPerTick = (double) currentUuCost / replicateMaxProgress / 1000.0D; // 转换为UU物质
        return uuPerTick / getProcessTimeMultiplier(); // 频率升级越快，每tick消耗更多UU
    }

    /**
     * 获取每tick的EU消耗（受升级件等级影响）
     * 使用父类的upgradeStats 计算
     */
    private double getEuPerTick() {
        // 基础消耗 * 能量使用倍率（受频率升级影响，越快越耗电）
        return EU_PER_TICK_BASE * upgradeStats.getEnergyUsageMultiplier();
    }

    /**
     * 获取处理时间倍率（受升级件等级影响）
     * 使用父类的upgradeStats 计算
     */
    private double getProcessTimeMultiplier() {
        // 从升级槽计算倍率，默认1.0
        // 频率升级越快，倍率越小（处理时间越短）
        return upgradeStats.getProcessTimeMultiplier();
    }

    /**
     * 开始新的复制 - 对应原版IC2进度计算
     */
    private void startReplicating() {
        ScanResult result = getMemoryData();
        if (result == null) {
            stopReplicating();
            return;
        }

        // 设置当前复制的消耗
        currentUuCost = result.getUuMatterCostMB();
        currentEuCost = result.energyCost;
        // 对应原版IC2：最大进度 = 总能量成本 / 每tick能量消耗
        replicateMaxProgress = (int) Math.max(1, currentEuCost / getEuPerTick());
        replicateProgress = 0;
        uuConsumed = 0;
        extraUuStored = 0.0D;
        isReplicating = true;

        // 设置当前输出物品
        currentOutputItem = createItemFromScanResult(result);
        if (currentOutputItem.isEmpty()) {
            // 无法重建物品，停止复制
            stopReplicating();
            return;
        }

        setChanged();
    }

    /**
     * 恢复残留的复制
     */
    private void resumeReplicating() {
        // 恢复复制状态，继续之前的进度
        isReplicating = true;
        setChanged();
    }

    /**
     * 继续复制中 - 对应原版IC2
     */
    private void continueReplicating() {
        // 计算本tick需要消耗的UU物质（受升级件等级影响）
        double uuNeeded = getUuPerTick();
        // int uuToDrain = (int) Math.ceil(uuNeeded * 1000.0D); // 转换为mB
        
        // 先从额外累积中消耗
        if (extraUuStored >= uuNeeded) {
            extraUuStored -= uuNeeded;
            uuNeeded = 0;
        } else {
            uuNeeded -= extraUuStored;
            extraUuStored = 0;
        }
        
        // 消耗需要的UU，从液体槽中提取
        if (uuNeeded > 0) {
            int drainAmount = (int) Math.ceil(uuNeeded * 1000.0D);
            FluidStack drained = uuMatterTank.drain(drainAmount, IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() > 0) {
                double uuDrained = drained.getAmount() / 1000.0D;
                if (uuDrained >= uuNeeded) {
                    // 多余的UU存入extraUuStored
                    extraUuStored = uuDrained - uuNeeded;
                    uuConsumed += (int) (uuNeeded * 1000);
                } else {
                    // UU不足，暂停复制
                    pauseReplicating();
                    return;
                }
            } else {
                // UU物质不足，暂停复制并保留进度
                pauseReplicating();
                return;
            }
        }

        // 消耗能量，对应原版IC2
        double euNeeded = getEuPerTick();
        long extracted = apiUseEnergy((long) euNeeded, false);
        if (extracted > 0) {
            replicateProgress += 1; // 每tick进度+1（对应IC2公式）
        }

        // 检查是否完成（进度达到最大进度）
        if (replicateProgress >= replicateMaxProgress) {
            finishReplicating();
        }
    }

    /**
     * 完成复制
     */
    private void finishReplicating() {
        // 输出物品
        ItemStack outputStack = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, currentOutputItem.copy());
        } else {
            outputStack.grow(1);
        }

        // 重置复制状态
        isReplicating = false;
        replicateProgress = 0;

        // 检查工作模式是否需要停止
        if (workMode == WorkMode.SINGLE) {
            // 单次模式完成后自动停止
            workMode = WorkMode.STOPPED;
        } else if (workMode == WorkMode.LOOP) {
            // 循环模式完成后检查是否可以继续
            // 下面的tick会检查canWork()
        }

        setChanged();
    }

    /**
     * 停止复制（完全重置）
     */
    private void stopReplicating() {
        isReplicating = false;
        replicateProgress = 0;
        uuConsumed = 0;
        extraUuStored = 0.0D;
        currentUuCost = 0;
        currentEuCost = 0;
        currentOutputItem = ItemStack.EMPTY;
        isWorking = false;
    }

    /**
     * 暂停复制并保留进度
     */
    private void pauseReplicating() {
        isReplicating = false;
        isWorking = false;
        // 保留 replicateProgress 和 uuConsumed，以便恢复
    }

    @Override
    protected void stopWork() {
        isWorking = false;
    }

    // ==================== 按钮操作方法 ====================

    /**
     * 停止/暂停复制按钮
     * 保留当前进度，下次可以继续
     */
    public void stopGeneration() {
        workMode = WorkMode.STOPPED;
        if (isReplicating) {
            // 正在复制中，暂停并保留进度
            pauseReplicating();
        }
        setChanged();
    }

    /**
     * 单次复制按钮
     */
    public void generateOnce() {
        workMode = WorkMode.SINGLE;
        setChanged();
    }

    /**
     * 循环复制按钮
     */
    public void loopGeneration() {
        workMode = WorkMode.LOOP;
        setChanged();
    }

    /**
     * 设置工作模式（用于GUI按钮）
     * @param mode 0=停止, 1=单次, 2=循环
     */
    public void setWorkMode(int mode) {
        switch (mode) {
            case 0:
                workMode = WorkMode.STOPPED;
                break;
            case 1:
                workMode = WorkMode.SINGLE;
                break;
            case 2:
                workMode = WorkMode.LOOP;
                break;
        }
        setChanged();
    }

    // ==================== Getters ====================

    public WorkMode getWorkMode() {
        return workMode;
    }

    public boolean isReplicating() {
        return isReplicating;
    }

    public int getReplicateProgress() {
        return replicateProgress;
    }

    public int getReplicateMaxProgress() {
        return replicateMaxProgress;
    }

    public FluidTank getUuMatterTank() {
        return uuMatterTank;
    }

    public int getUuMatterAmount() {
        return uuMatterTank.getFluidAmount();
    }

    public int getUuMatterCapacity() {
        return UUMATTER_CAPACITY;
    }

    public ContainerData getContainerData() {
        return dataAccess;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.ReplicatorElcMenu(
            containerId, playerInventory, this);
    }

    public long getCurrentUuCost() {
        return currentUuCost;
    }

    public long getCurrentEuCost() {
        return currentEuCost;
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("WorkMode", workMode.ordinal());
        tag.putBoolean("IsReplicating", isReplicating);
        tag.putInt("ReplicateProgress", replicateProgress);
        tag.putInt("ReplicateMaxProgress", replicateMaxProgress);
        tag.putInt("UuConsumed", uuConsumed);
        tag.putDouble("ExtraUuStored", extraUuStored);
        tag.putLong("CurrentUuCost", currentUuCost);
        tag.putLong("CurrentEuCost", currentEuCost);

        // 保存UU物质液体槽
        CompoundTag fluidTag = new CompoundTag();
        uuMatterTank.writeToNBT(registries, fluidTag);
        tag.put("UuMatterTank", fluidTag);

        // 保存当前输出物品
        if (!currentOutputItem.isEmpty()) {
            tag.put("CurrentOutputItem", currentOutputItem.saveOptional(registries));
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("WorkMode")) {
            int modeOrdinal = tag.getInt("WorkMode");
            if (modeOrdinal >= 0 && modeOrdinal < WorkMode.values().length) {
                workMode = WorkMode.values()[modeOrdinal];
            }
        }
        if (tag.contains("IsReplicating")) {
            isReplicating = tag.getBoolean("IsReplicating");
        }
        if (tag.contains("ReplicateProgress")) {
            replicateProgress = tag.getInt("ReplicateProgress");
        }
        if (tag.contains("ReplicateMaxProgress")) {
            replicateMaxProgress = tag.getInt("ReplicateMaxProgress");
        }
        if (tag.contains("UuConsumed")) {
            uuConsumed = tag.getInt("UuConsumed");
        }
        if (tag.contains("ExtraUuStored")) {
            extraUuStored = tag.getDouble("ExtraUuStored");
        }
        if (tag.contains("CurrentUuCost")) {
            currentUuCost = tag.getLong("CurrentUuCost");
        }
        if (tag.contains("CurrentEuCost")) {
            currentEuCost = tag.getLong("CurrentEuCost");
        }

        // 加载UU物质液体槽
        if (tag.contains("UuMatterTank")) {
            CompoundTag fluidTag = tag.getCompound("UuMatterTank");
            uuMatterTank.readFromNBT(registries, fluidTag);
        }

        // 加载当前输出物品
        if (tag.contains("CurrentOutputItem")) {
            currentOutputItem = ItemStack.parseOptional(registries, tag.getCompound("CurrentOutputItem"));
        }
    }

    /**
     * 每tick更新方法
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_replicator_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        blockEntity.recalculateUpgradeStats();

        com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block.tick(level, pos, state, blockEntity);

        if (!blockEntity.canWorkRedstone()) {
            blockEntity.stopWork();
        } else {
            if (blockEntity.canWork()) {
                blockEntity.doWork();
            } else {
                blockEntity.stopWork();
            }
        }

        blockEntity.handleBatterySlot();

        boolean isLit = state.getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_replicator_elc.LIT);
        if (blockEntity.isWorking() != isLit) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_replicator_elc.LIT, blockEntity.isWorking()), 3);
        }

        blockEntity.handleAutomationUpgrades();
    }
}
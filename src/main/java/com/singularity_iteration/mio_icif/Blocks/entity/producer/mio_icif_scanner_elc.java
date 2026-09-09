package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Singularity_Iteration_Config;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.uu.UuIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * 模式扫描机方块实体类
 * 用于扫描物品的蓝图数据，该数据存储模式所需的UU流体和电力消耗
 *
 * 槽位布局说明:
 * - 0: 扫描物品槽位：放置被扫描物品的位置
 * - 1: 电池槽位：提供能量
 * - 2: 记忆水晶槽位：用于存储扫描结果数据
 *
 * 工作机机制:
 * - 每个被扫描物品需要 900000 EU 能量才能完成扫描
 * - 消耗功率 256 EU/t，扫描时间 165 秒（3515 ticks）
 * - 最大能量存储 512000 EU
 * - 记忆水晶槽位必须放置空的记忆水晶
 * - 点击保存按钮后数据写入记忆水晶
 *
 * UU价值计算:
 * - 基于IC2 1.12.2的UuIndex/UuGraph算法
 * - 通过合成图推导物品配方计算所需UU价值
 * - 无法合成推导的物品判定为无UU价值
 */
@SuppressWarnings("null")
public class mio_icif_scanner_elc extends mio_icif_producer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .scanner()
        .battery()
        .memory()
        .build();

    // 槽位总数: 3个（扫描槽位 + 电池槽 + 记忆水晶槽位）
    public static final int SLOT_COUNT = 3;
    // 扫描槽索引：放置被扫描物品的位置
    public static final int SCANNER_SLOT = 0;
    // 电池槽索引
    public static final int BATTERY_SLOT = 1;
    // 记忆水晶槽索引
    public static final int MEMORY_SLOT = 2;

    // 默认配置
    public static final long DEFAULT_CAPACITY = 512000L;  // 512k EU（对比IC2原版）
    public static final long DEFAULT_MAX_RECEIVE = 512L;  // HV级输入
    public static final long DEFAULT_MAX_EXTRACT = 0L; // 不输出电力
    public static final int DEFAULT_SCAN_TIME = 3515; // ~165秒（3515 ticks）
    public static final long DEFAULT_ENERGY_PER_TICK = 256L; // 每tick消耗256 EU
    public static final long TOTAL_ENERGY_COST = 900000L; // 每次扫描需要的总能量

    // 扫描结果缓存
    private ScanResult scanResult = null;
    // 扫描是否已完成
    private boolean scanComplete = false;
    // 当前被扫描物品，用于检测物品变化
    private ItemStack currentStack = ItemStack.EMPTY;
    // 状态机状态
    private State state = State.IDLE;

    /**
     * 扫描机状态枚举
     * 注册时通过ordinal索引在Screen中必须使用switch case保持一致!
     * 0=IDLE, 1=SCANNING, 2=NO_ENERGY, 3=NO_STORAGE, 4=COMPLETED, 5=FAILED
     */
    public enum State {
        IDLE,           // 0: 空闲
        SCANNING,       // 1: 正在扫描
        NO_ENERGY,      // 2: 能量不足
        NO_STORAGE,     // 3: 无记忆水晶
        COMPLETED,      // 4: 扫描完成
        FAILED,         // 5: 扫描失败（无法推导UU价值）
        TRANSFER_ERROR, // 6: 保存失败
        ALREADY_RECORDED // 7: 该物品已被记录
    }

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> isWorking ? 1 : 0;
                case 3 -> (int) energyStorage.getAmount();
                case 4 -> (int) energyStorage.getCapacity();
                case 5 -> scanComplete ? 1 : 0;
                case 6 -> getStateOrdinal();
                case 7 -> (int) (Double.doubleToRawLongBits(getUUMatterCost()) >> 32);
                case 8 -> (int) Double.doubleToRawLongBits(getUUMatterCost());
                case 9 -> (int) getEnergyCost();
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

    /**
     * 扫描结果数据类
     */
    public static class ScanResult {
        public final ItemStack item;
        public final double uuMatterCostBuckets;
        public final long energyCost;

        public ScanResult(ItemStack item, double uuMatterCostBuckets, long energyCost) {
            this.item = item.copy();
            this.uuMatterCostBuckets = uuMatterCostBuckets;
            this.energyCost = energyCost;
        }

        public long getUuMatterCostMB() {
            return (long) (uuMatterCostBuckets * 1000.0);
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.put("item", item.saveOptional(net.minecraft.core.RegistryAccess.EMPTY));
            tag.putDouble("uu_matter_cost_buckets", uuMatterCostBuckets);
            tag.putLong("energy_cost", energyCost);
            return tag;
        }

        public static ScanResult deserializeNBT(CompoundTag tag) {
            ItemStack item = ItemStack.parseOptional(net.minecraft.core.RegistryAccess.EMPTY, tag.getCompound("item"));
            double uuMatterCostBuckets = tag.getDouble("uu_matter_cost_buckets");
            long energyCost = tag.getLong("energy_cost");
            return new ScanResult(item, uuMatterCostBuckets, energyCost);
        }
    }

    /**
     * 构造 BlockEntityType.Builder 注册用参数构造函数
     */
    public mio_icif_scanner_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.SCANNER_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_scanner_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_SCAN_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.HV);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SCANNER_SLOT) {
            return !isBattery(stack) && !(stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Resource.mio_icif_memory);
        } else if (slot == BATTERY_SLOT) {
            return isBattery(stack);
        } else if (slot == MEMORY_SLOT) {
            return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Resource.mio_icif_memory;
        }
        return false;
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return new int[]{SCANNER_SLOT, BATTERY_SLOT, MEMORY_SLOT};
    }

    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return slot == BATTERY_SLOT;
    }

    @Override
    protected boolean hasValidRecipe() {
        ItemStack input = itemHandler.getStackInSlot(SCANNER_SLOT);
        return !input.isEmpty();
    }

    /**
     * 检查机器是否可以工作
     * 对比IC2原版扫描机:
     */
    @Override
    protected boolean canWork() {
        ItemStack input = itemHandler.getStackInSlot(SCANNER_SLOT);
        if (input.isEmpty()) {
            return false;
        }

        // 检查记忆水晶槽位
        ItemStack memoryStack = itemHandler.getStackInSlot(MEMORY_SLOT);
        if (memoryStack.isEmpty()) {
            state = State.NO_STORAGE;
            return false;
        }
        if (!(memoryStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Resource.mio_icif_memory memoryItem)) {
            state = State.NO_STORAGE;
            return false;
        }
        if (memoryItem.hasData(memoryStack)) {
            state = State.NO_STORAGE;
            return false;
        }

        // 检查扫描是否已完成
        if (scanComplete) {
            return false;
        }

        // 检查能量
        if (!hasEnoughEnergy()) {
            state = State.NO_ENERGY;
            return false;
        }

        return true;
    }

    @Override
    protected void doWork() {
        isWorking = true;
        state = State.SCANNING;

        if (progress >= maxProgress) {
            finishScan();
        }
    }

    /**
     * 完成扫描
     * 对比IC2原版 TileEntityScanner finishScan
     */
    private void finishScan() {
        ItemStack input = itemHandler.getStackInSlot(SCANNER_SLOT);
        if (input.isEmpty()) {
            stopWork();
            return;
        }

        // 使用UU Graph计算被扫描物品的UU价值
        double uuValue = calculateUuValue(input);

        if (uuValue == Double.POSITIVE_INFINITY) {
            // 扫描失败：无法推导出UU价值
            state = State.FAILED;
            scanComplete = true;
            // 消耗所有能量
            apiUseEnergy(TOTAL_ENERGY_COST, false);
            // 重置进度
            progress = 0;
            isWorking = false;
            setChanged();
            return;
        }

        // 计算UU流体成本（以桶为单位，转换为mB，1桶=1000mB）
        double uuMatterCostBuckets = uuValue * Singularity_Iteration_Config.SCANNER_UU_MULTIPLIER.get();
        // 计算电力成本
        long energyCost = calculateEnergyCost(uuValue);

        // 保存扫描结果
        scanResult = new ScanResult(input, uuMatterCostBuckets, energyCost);
        scanComplete = true;
        state = State.COMPLETED;

        // 消耗能量
        apiUseEnergy(TOTAL_ENERGY_COST, false);

        // 移除扫描物品
        input.shrink(1);
        if (input.isEmpty()) {
            itemHandler.setStackInSlot(SCANNER_SLOT, ItemStack.EMPTY);
        }

        // 重置进度
        progress = 0;
        isWorking = false;
        setChanged();
    }

    /**
     * 计算被扫描物品的UU价值（以桶为单位）
     * 基于UU Graph系统
     * IC2原版中 uu_scan_values.ini 中的数值是以µB（微桶）为单位的，需要转换
     * 1 桶 = 1,000,000 µB
     */
    private double calculateUuValue(ItemStack stack) {
        if (stack.isEmpty()) return Double.POSITIVE_INFINITY;

        // 检查是否有自定义配置覆盖
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        java.util.Map<String, Singularity_Iteration_Config.CostConfig> customCosts = Singularity_Iteration_Config.getCustomCostConfigs();
        if (customCosts.containsKey(itemId)) {
            return customCosts.get(itemId).getUuCost();
        }

        // 使用UU Index查询，返回值以桶为单位
        if (UuIndex.INSTANCE.isInitialized()) {
            return UuIndex.INSTANCE.getInBuckets(stack.copyWithCount(1));
        }

        // UU系统未初始化或无法推导返回正无穷大，扫描会失败
        return Double.POSITIVE_INFINITY;
    }

    /**
     * 计算电力消耗成本
     * 对比IC2原版：原版中TileEntityScanner的patternEu 始终为512000，还没有被设置过
     * 本机器通过自身的消耗功率（256 EU/tick）来模拟，本方法暂时返回固定值
     */
    private long calculateEnergyCost(double uuValue) {
        // 对比IC2原版：patternEu 始终为512000
        return 0L;
    }

    /**
     * 将扫描结果写入记忆水晶
     * @return 是否成功保存
     */
    public boolean storeResult() {
        LOGGER.info("[Scanner] storeResult() called, scanComplete: {}, scanResult: {}", scanComplete, scanResult != null);

        if (!scanComplete || scanResult == null) {
            LOGGER.info("[Scanner] Store failed: scan not complete or no result");
            return false;
        }

        ItemStack memoryStack = itemHandler.getStackInSlot(MEMORY_SLOT);
        if (memoryStack.isEmpty()) {
            LOGGER.info("[Scanner] Store failed: memory slot empty");
            state = State.TRANSFER_ERROR;
            return false;
        }

        if (!(memoryStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Resource.mio_icif_memory memoryItem)) {
            LOGGER.info("[Scanner] Store failed: item is not memory crystal");
            state = State.TRANSFER_ERROR;
            return false;
        }

        if (memoryItem.hasData(memoryStack)) {
            LOGGER.info("[Scanner] Store failed: memory already has data");
            state = State.TRANSFER_ERROR;
            return false;
        }

        LOGGER.info("[Scanner] Storing scan result to memory crystal...");
        memoryItem.storeData(memoryStack,
            scanResult.item,
            scanResult.uuMatterCostBuckets,
            scanResult.energyCost);

        LOGGER.info("[Scanner] Store successful, clearing scan result");
        clearScanResult();
        return true;
    }

    /**
     * 丢弃扫描结果
     */
    public void discardResult() {
        clearScanResult();
    }

    /**
     * 清除扫描结果
     */
    public void clearScanResult() {
        scanResult = null;
        scanComplete = false;
        progress = 0;
        currentStack = ItemStack.EMPTY;
        state = State.IDLE;
        setChanged();
    }

    /**
     * 重置扫描状态
     */
    public void reset() {
        progress = 0;
        currentStack = ItemStack.EMPTY;
        scanResult = null;
        scanComplete = false;
        state = State.IDLE;
        setChanged();
    }

    @Override
    protected boolean shouldResetProgress() {
        if (scanComplete) {
            return false;
        }
        ItemStack input = itemHandler.getStackInSlot(SCANNER_SLOT);
        return input.isEmpty();
    }

    // ==================== Getters ====================

    public boolean isScanComplete() {
        return scanComplete;
    }

    @Nullable
    public ScanResult getScanResult() {
        return scanResult;
    }

    public ItemStack getScannedItem() {
        return scanResult != null ? scanResult.item : ItemStack.EMPTY;
    }

    public double getUUMatterCost() {
        return scanResult != null ? scanResult.uuMatterCostBuckets : 0;
    }

    public long getEnergyCost() {
        return scanResult != null ? scanResult.energyCost : 0;
    }

    public State getScanState() {
        return state;
    }

    public int getPercentageDone() {
        return 100 * progress / maxProgress;
    }

    public boolean isDone() {
        return progress >= maxProgress;
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("scan_complete", scanComplete);
        tag.putInt("state", state.ordinal());
        if (!currentStack.isEmpty()) {
            tag.put("current_stack", currentStack.saveOptional(registries));
        }
        if (scanResult != null) {
            tag.put("scan_result", scanResult.serializeNBT());
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        scanComplete = tag.getBoolean("scan_complete");
        int stateIdx = tag.getInt("state");
        state = (stateIdx >= 0 && stateIdx < State.values().length) ? State.values()[stateIdx] : State.IDLE;
        if (tag.contains("current_stack")) {
            currentStack = ItemStack.parseOptional(registries, tag.getCompound("current_stack"));
        }
        if (tag.contains("scan_result")) {
            scanResult = ScanResult.deserializeNBT(tag.getCompound("scan_result"));
        }
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.scanner_elc");
    }

    public ContainerData getContainerData() {
        return dataAccess;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.ScannerElcMenu(
            containerId, playerInventory, this);
    }

    /**
     * 获取机器状态ordinal
     * 0=Idle, 1=Scanning, 2=Completed, 3=Failed, 4=No Storage, 5=No Energy, 6=Transfer Error, 7=Already Recorded
     */
    public int getStateOrdinal() {
        return state.ordinal();
    }

    // ==================== Tick ====================

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_scanner_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 调用父类tick方法处理工作逻辑
        mio_icif_producer.tick(level, pos, state, blockEntity);
    }
}
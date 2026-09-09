package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Items.Upgrade.MachineUpgradeStats;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_od_scanner;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_ov_scanner;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * 高级采矿机方块实体类
 * 继承自普通采矿机，但具有以下特点 * - 不需要采矿管道和钻头
 * - 内置超大存储7格）
 * - 支持升级插槽（超频、能量存储、牵引光束）
 * - 支持精确采集模式
 * - 自动处理液体（不需要泵 * - 更大的扫描范围（9x9 * - HV电压等级
 */
public class mio_icif_advanced_miner_elc extends mio_icif_producer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .battery()  // 电池
       .extra(1)   // 扫描仪槽
        .upgrade(4) // 升级
   .build();

    // 槽位定义 - 匹配原版IC2高级采矿机GUI布局
    public static final int SLOT_BATTERY = 0;        // 电池�?(8,80)
    public static final int SLOT_SCANNER = 1;        // 扫描仪槽 (8,26)
    public static final int SLOT_UPGRADE_START = 2;  // 升级槽起�?152,26) - 4�?垂直排列)
    public static final int SLOT_UPGRADE_COUNT = 4;
    public static final int SLOT_UPGRADE_END = SLOT_UPGRADE_START + SLOT_UPGRADE_COUNT; // 6
    public static final int TOTAL_SLOTS = 6;         // itemHandler实际槽位数（电池、扫描仪、升级）
    // 过滤槽定义（幽灵槽，不占用itemHandler�?
    public static final int FILTER_COUNT = 15;       // 15(3 - 用于黑白名单
    public static final int FILTER_ROWS = 3;
    public static final int FILTER_COLS = 5;
    public static final int FILTER_START_X = 36;
    public static final int FILTER_START_Y = 44;

    // 默认配置 - HV等级（对齐原版IC2�?
    public static final long DEFAULT_CAPACITY = 4000000L;  // 4M EU
    public static final long DEFAULT_MAX_RECEIVE = 512L;   // HV 512 EU/t
    public static final long DEFAULT_MAX_EXTRACT = 512L;   // 最大提取速率
    public static final int DEFAULT_WORK_TIME = 20;        // 20 ticks基础工作速度
    public static final long DEFAULT_ENERGY_PER_TICK = 0L; // 动态计
    // 基础耗电配置（对齐原版IC2�?
    public static final int BASE_ENERGY_COST = 512;       // 原版IC2: 512 EU/次挖�?
    public static final int OVERCLOCK_ENERGY_MULTIPLIER = 2; // 每个超频升级耗电
    // 扫描器耗电配置（对齐原版IC2�?
    public static final int SCANNER_ENERGY_COST = 64;     // 原版IC2: 64 EU/次扫
    // 扫描范围x9�?
    public static final int SCAN_RADIUS = 4; // 9x9范围，向四个方向延伸4
    // 升级类型
    public enum UpgradeType {
        NONE,
        OVERCLOCKER,      // 超频升级：加速但增加耗电
        ENERGY_STORAGE,   // 能量存储升级：增加能量容
        TRACTOR_BEAM      // 牵引光束升级：增加采集范围
        }

    // 机器状态
private int currentDepth = 0;           // 当前挖掘深度
    private BlockPos tipPos = null;         // 采矿尖端位置
    @SuppressWarnings("unused")
    private boolean isPaused = false;       // 是否暂停（遇到液体或满仓）
    private List<BlockPos> oresInCurrentLayer = new ArrayList<>(); // 当前层的矿物位置
    private int currentOreIndex = 0;        // 当前正在挖掘的矿物索引
    private boolean silkTouchMode = false;  // 精确采集模式
    private boolean autoEjectMode = false;  // 自动弹出模式（将物品输出到相邻容器）
    private boolean whitelistMode = false;  // 白名单模式（true=白名单，false=黑名单）

    // 过滤槽（幽灵槽，用于黑白名单过滤配置）
    private final ItemStack[] filterStacks = new ItemStack[FILTER_COUNT];

    // 升级缓存
    private int overclockerCount = 0;
    private int energyStorageCount = 0;
    private int tractorBeamCount = 0;
    private int effectiveScanRadius = SCAN_RADIUS;
    private int maxBlockScanCount = 5;  // 每周期最多扫�?挖掘的方块数（对齐IC2: 5*(overclockerCount+1)
    private int workTicker = 0;          // 工作周期计时器（0-20�?
    /**
     * 用于 BlockEntityType.Builder 的构造函
*/
    public mio_icif_advanced_miner_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.ADVANCED_MINER_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_advanced_miner_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.HV); // HV电压等级

        // 初始化过滤槽
        for (int i = 0; i < FILTER_COUNT; i++) {
            filterStacks[i] = ItemStack.EMPTY;
        }
    }

    /**
     * 获取过滤槽物
*/
    public ItemStack getFilterStack(int index) {
        if (index >= 0 && index < FILTER_COUNT) {
            return filterStacks[index];
        }
        return ItemStack.EMPTY;
    }

    /**
     * 设置过滤槽物
*/
    public void setFilterStack(int index, ItemStack stack) {
        if (index >= 0 && index < FILTER_COUNT) {
            filterStacks[index] = stack.copy();
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_SCANNER -> isScanner(stack);
            default -> {
                // 升级
           if (slot >= SLOT_UPGRADE_START && slot < SLOT_UPGRADE_END) {
                    yield isUpgrade(stack);
                }
                yield false;
            }
        };
    }

    /**
     * 检查物品是否是扫描
*/
    private boolean isScanner(ItemStack stack) {
        return stack.getItem() instanceof mio_icif_od_scanner || stack.getItem() instanceof mio_icif_ov_scanner;
    }

    /**
     * 获取扫描器类
*/
    private ScannerType getScannerType() {
        ItemStack scannerStack = itemHandler.getStackInSlot(SLOT_SCANNER);
        if (scannerStack.isEmpty()) {
            return ScannerType.NONE;
        }
        // 根据物品名称判断扫描器类
   String itemName = scannerStack.getItem().toString().toLowerCase();
        if (itemName.contains("ov") || itemName.contains("ov_scanner")) {
            return ScannerType.OV;
        }
        // 默认OD扫描
   return ScannerType.OD;
    }

    /**
     * 扫描器类
*/
    private enum ScannerType {
        NONE,   // 无扫描器
        OD,     // OD扫描- 7x7范围
        OV      // OV扫描- 13x13范围
    }

    /**
     * 根据扫描器类型获取扫描半
*/
        private int getScanRadiusByScanner() {
        return switch (getScannerType()) {
            case OV -> 32;  // OV扫描
            case OD -> 16;  // OD扫描
            default -> 0;  // 无扫描器: 只挖正下
       };
    }

    /**
     * 检查物品是否是升级
     */
    private boolean isUpgrade(ItemStack stack) {
        return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Upgrade.mio_icif_upgrade;
    }

    /**
     * 获取指定方向可访问的槽位
     * 高级采矿机：没有可外部访问的槽位（过滤槽是幽灵槽，不占用itemHandler
*/
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // 没有可外部访问的槽位
        return new int[0];
    }

    /**
     * 检查指定槽位是否可以从指定方向提取物品
     */
    @Override
    protected int getBatterySlot() {
        // 高级采矿机有电池
       return SLOT_BATTERY;
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        // 没有可提取的槽位（过滤槽是幽灵槽
   return false;
    }

    @Override
    protected boolean canWork() {
        // 检查基本条
   if (level == null || level.isClientSide) {
            return false;
        }

        // 检查是否有扫描
   if (getScannerType() == ScannerType.NONE) {
            return false;
        }

        // 检查存储槽是否已满
        if (isStorageFull()) {
            return false;
        }

        // 能量检查延迟到 consumeEnergyForMining() 实际执行挖掘时才
   // 这样即使电网注入能量有延迟，采矿机也能正常启
        return true;
    }

    /**
     * 检查过滤槽是否已满（高级采矿机没有存储槽，挖掘的方块直接掉落或输出到相邻容器）
     * 这个方法现在始终返回 false，因为过滤槽满了不影响工
*/
    private boolean isStorageFull() {
        return false;
    }

        /**
     * 扫描并更新升级状态（使用 MachineUpgradeStats 统一读取逻辑
    */
        private void scanUpgrades() {
        MachineUpgradeStats stats = MachineUpgradeStats.fromInventory(itemHandler, SLOT_UPGRADE_START, SLOT_UPGRADE_COUNT);

        int newOverclockerCount = stats.overclockerCount;
        int newEnergyStorageCount = stats.energyStorageCount;

        overclockerCount = newOverclockerCount;
        energyStorageCount = newEnergyStorageCount;
        // 超频升级不影响扫描范围，只影响每周期挖掘方块
       tractorBeamCount = 0;

        // 扫描范围只由扫描器类型决
       effectiveScanRadius = getScanRadiusByScanner();

        // 对齐IC2 1.12.2: 每周期扫描方块数 = 5 * (overclockerCount + 1)
        maxBlockScanCount = 5 * (overclockerCount + 1);

        long newCapacity = DEFAULT_CAPACITY + (energyStorageCount * 100000L);
        if (energyStorage.getCapacity() != newCapacity) {
            energyStorage.setCapacity(newCapacity);
        }
    }

    @Override
    protected void doWork() {
        if (level == null || level.isClientSide) return;

        // �?00 ticks扫描一次升
       if (level.getGameTime() % 100 == 0) {
            scanUpgrades();
        }

        ServerLevel serverLevel = (ServerLevel) level;

        // 初始化采矿位
       if (tipPos == null) {
            scanUpgrades();
            tipPos = worldPosition.below();
            currentDepth = 0;
            currentOreIndex = 0;
            generateLayerBlocks();
        }

        // 如果当前层列表为空（比如从旧存档加载），重新生成
        if (oresInCurrentLayer.isEmpty()) {
            generateLayerBlocks();
        }

        // 如果当前层挖掘完成，进入下一
       if (currentOreIndex >= oresInCurrentLayer.size()) {
            tipPos = tipPos.below();
            currentDepth++;
            currentOreIndex = 0;
            generateLayerBlocks();

            if (tipPos.getY() < level.getMinBuildHeight()) {
                stopWork();
                return;
            }

            if (oresInCurrentLayer.isEmpty()) {
                return;
            }
        }

        // 对齐IC2 1.12.2: �?0 ticks为一个工作周期，每周期扫描并挖掘最多maxBlockScanCount个方
       workTicker++;
        if (workTicker < DEFAULT_WORK_TIME) {
            isWorking = true;
            return;
        }
        workTicker = 0;

        // 批量扫描并挖掘（对齐IC2: 每周期最多扫描maxBlockScanCount个方块）
        int scanned = 0;
        int blocksMined = 0;
        while (scanned < maxBlockScanCount && currentOreIndex < oresInCurrentLayer.size()) {
            BlockPos targetPos = oresInCurrentLayer.get(currentOreIndex);
            BlockState targetState = level.getBlockState(targetPos);
            scanned++;

            // 跳过不可挖掘的方块（空气、基岩、黑名单等）
            if (!canMineBlock(targetState, targetPos)) {
                currentOreIndex++;
                continue;
            }

            // 消耗扫描器能量（原版IC2: 64 EU/次扫描）
            ItemStack scannerStack = itemHandler.getStackInSlot(SLOT_SCANNER);
            if (!scannerStack.isEmpty() && getItemAPI().isElectricTool(scannerStack)) {
                if (getItemAPI().getElectricToolStored(scannerStack) < SCANNER_ENERGY_COST) {
                    LOGGER.info("[AdvancedMiner] 扫描器能量不足，停止工作");
                    stopWork();
                    return;
                }
                getItemAPI().dischargeElectricTool(scannerStack, SCANNER_ENERGY_COST, false);
            }

            // 消耗机器能量（原版IC2: 512 EU/次挖掘）
            long energyBefore = energyStorage.getAmount();
            if (energyBefore < BASE_ENERGY_COST) {
                stopWork();
                return;
            }
            long actuallyExtracted = energyStorage.extract(BASE_ENERGY_COST, false);
            if (actuallyExtracted <= 0) {
                stopWork();
                return;
            }

            // 挖掘方块
            if (mineBlock(serverLevel, targetPos)) {
                blocksMined++;
            }
            currentOreIndex++;
        }

        isWorking = blocksMined > 0 || scanned > 0;
    }
    /**
     * tip 移动到新位置
     */
    @SuppressWarnings("unused")
    private boolean moveTipTo(BlockPos newTipPos) {
        if (level == null) return false;

        BlockState stateAt = level.getBlockState(newTipPos);
        if (!(stateAt.isAir() || stateAt.canBeReplaced())) {
            return false;
        }

        // 高级采矿机不放置实体管道，只更新tip位置
        tipPos = newTipPos;
        setChanged();
        return true;
    }

    /**
     * 扫描当前层的矿物
     */
    /**
     * 生成当前层需要挖掘的方块列表
     * 根据扫描器类型和升级确定范围，包含所有非空气、非基岩的方
*/
    private void generateLayerBlocks() {
        oresInCurrentLayer.clear();

        if (level == null || tipPos == null) {
            return;
        }

        // 使用 effectiveScanRadius（已包含扫描器类型和牵引光束升级
   int radius = effectiveScanRadius;
        if (radius == 0) {
            // 无扫描器，只挖正下方
            oresInCurrentLayer.add(tipPos);
            return;
        }

        // 扫描以采矿机为中心（XZ），tip 所在的这一层（Y）正方形区域
        BlockPos center = new BlockPos(worldPosition.getX(), tipPos.getY(), worldPosition.getZ());

        // 从中心向外螺旋扫描，确保先挖中心再挖边缘
        for (int r = 0; r <= radius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    // 只处理当前环的方块（避免重复
               if (Math.abs(x) != r && Math.abs(z) != r) {
                        continue;
                    }

                    BlockPos checkPos = center.offset(x, 0, z);
                    BlockState state = level.getBlockState(checkPos);

                    // 高级采矿机挖掘几乎所有方块（除了空气和基岩），同时检查黑白名
               if (canMineBlock(state, checkPos)) {
                        oresInCurrentLayer.add(checkPos);
                    }
                }
            }
        }
    }

    /**
     * 检查方块是否是矿物
     */
    @SuppressWarnings("unused")
    private boolean isOre(BlockState state) {
        // 原版矿石标签
        if (state.is(BlockTags.COAL_ORES) ||
            state.is(BlockTags.IRON_ORES) ||
            state.is(BlockTags.COPPER_ORES) ||
            state.is(BlockTags.GOLD_ORES) ||
            state.is(BlockTags.REDSTONE_ORES) ||
            state.is(BlockTags.LAPIS_ORES) ||
            state.is(BlockTags.DIAMOND_ORES) ||
            state.is(BlockTags.EMERALD_ORES)) {
            return true;
        }

        // NeoForge 通用矿石标签
        if (state.is(Tags.Blocks.ORES)) {
            return true;
        }

        // 本模组矿
   if (state.is(mio_icif_blocks.BLOCK_ORE_TIN.get()) ||
            state.is(mio_icif_blocks.BLOCK_ORE_URAN.get()) ||
            state.is(mio_icif_blocks.BLOCK_ORE_LEAD.get())) {
            return true;
        }

        return false;
    }

    /**
     * 检查是否可以挖掘方
* 高级采矿机可以挖掘几乎所有方块（除了基岩和液体）
     * 同时考虑黑白名单过滤
     */
    private boolean canMineBlock(BlockState state, BlockPos pos) {
        // 不能挖掘空气、基
   if (state.isAir() || state.is(Blocks.BEDROCK)) {
            return false;
        }

        // 检查方块硬度，-1.0F 表示不可破坏（如基岩
   if (state.getDestroySpeed(level, pos) < 0) {
            return false;
        }

        // 检查黑白名
   return checkFilter(state);
    }

    /**
     * 检查方块是否通过黑白名单过滤
     * @return true 表示可以挖掘
     */
    private boolean checkFilter(BlockState state) {
        // 获取方块的掉落物（用于匹配过滤器
   ItemStack blockItem = new ItemStack(state.getBlock().asItem());
        if (blockItem.isEmpty()) {
            // 如果没有对应的物品形式，默认允许挖掘
            return true;
        }

        // 检查过滤槽（使用幽灵槽的filterStacks
   boolean hasFilters = false;
        boolean matchesFilter = false;

        for (int i = 0; i < FILTER_COUNT; i++) {
            ItemStack filterStack = filterStacks[i];
            if (!filterStack.isEmpty()) {
                hasFilters = true;
                // 检查物品是否匹配（包括物品类型和标签）
                if (ItemStack.isSameItem(blockItem, filterStack)) {
                    matchesFilter = true;
                    break;
                }
            }
        }

        // 如果没有设置过滤器，允许挖掘所有方
   if (!hasFilters) {
            return true;
        }

        // 黑名单模式（默认）：匹配的方块不挖掘
        // 白名单模式：只有匹配的方块才挖掘
        return whitelistMode == matchesFilter;
    }

    /**
     * 挖掘方块并收集掉落物
     */
    private boolean mineBlock(ServerLevel serverLevel, BlockPos pos) {
        BlockState state = serverLevel.getBlockState(pos);

        if (!canMineBlock(state, pos)) {
            return false;
        }

        // 获取掉落
   List<ItemStack> drops;

        if (silkTouchMode) {
            // 精确采集模式：直接获取方块物品本
       // 不使用附魔，直接获取方块的物品形
       ItemStack blockItem = new ItemStack(state.getBlock().asItem());
            drops = new ArrayList<>();
            if (!blockItem.isEmpty()) {
                drops.add(blockItem);
            }
        } else {
            // 普通模式：正常掉落
            LootParams.Builder builder = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY);

            drops = state.getDrops(builder);
        }

        // 将掉落物存入存储
   for (ItemStack drop : drops) {
            if (!insertItemToStorage(drop)) {
                // 存储槽已
           return false;
            }
        }

        // 移除方块
        serverLevel.removeBlock(pos, false);

        return true;
    }

    /**
     * 将物品插入相邻容器或掉落在地
* 高级采矿机没有内部存储槽，挖掘的方块需要输出到相邻容器或掉
*/
    private boolean insertItemToStorage(ItemStack stack) {
        if (stack.isEmpty()) return true;

        // 尝试输出到相邻容
   if (level != null && !level.isClientSide) {
            for (Direction direction : Direction.values()) {
                IItemHandler neighborHandler = getAdjacentItemHandler(worldPosition.relative(direction), direction.getOpposite());
                if (neighborHandler != null) {
                    for (int i = 0; i < neighborHandler.getSlots(); i++) {
                        stack = neighborHandler.insertItem(i, stack, false);
                        if (stack.isEmpty()) return true;
                    }
                }
            }
        }

        // 如果无法输出到容器，掉落在地
   if (!stack.isEmpty() && level != null && !level.isClientSide) {
            BlockPos dropPos = worldPosition.above();
            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                level, dropPos.getX() + 0.5, dropPos.getY() + 0.5, dropPos.getZ() + 0.5, stack.copy());
            level.addFreshEntity(itemEntity);
            return true;
        }

        return false;
    }

    /**
     * 处理液体（高级采矿机自动处理，不需要泵
*/
    private void handleFluid(FluidState fluidState, BlockPos fluidPos) {
        // 高级采矿机当前直接清除扫描范围内的源液体
   if (level != null) {
            level.setBlock(fluidPos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /**
     * 检查并抽取当前层的液体
     */
    @SuppressWarnings("unused")
    private boolean checkAndExtractLayerFluid() {
        if (level == null || tipPos == null) return false;

        BlockPos center = new BlockPos(worldPosition.getX(), tipPos.getY(), worldPosition.getZ());
        boolean foundFluid = false;

        for (int x = -effectiveScanRadius; x <= effectiveScanRadius; x++) {
            for (int z = -effectiveScanRadius; z <= effectiveScanRadius; z++) {
                BlockPos checkPos = center.offset(x, 0, z);
                FluidState fluidState = level.getFluidState(checkPos);

                if (!fluidState.isEmpty() && fluidState.isSource()) {
                    // 发现液体，处理它
                    handleFluid(fluidState, checkPos);
                    foundFluid = true;
                }
            }
        }

        return foundFluid;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("currentDepth", currentDepth);
        tag.putInt("currentOreIndex", currentOreIndex);
        tag.putBoolean("silkTouchMode", silkTouchMode);
        tag.putBoolean("autoEjectMode", autoEjectMode);
        tag.putBoolean("whitelistMode", whitelistMode);
        tag.putInt("workTicker", workTicker);
        if (tipPos != null) {
            tag.putLong("tipPos", tipPos.asLong());
        }
        // 保存当前层的挖掘列表
        if (!oresInCurrentLayer.isEmpty()) {
            long[] orePositions = new long[oresInCurrentLayer.size()];
            for (int i = 0; i < oresInCurrentLayer.size(); i++) {
                orePositions[i] = oresInCurrentLayer.get(i).asLong();
            }
            tag.putLongArray("oresInCurrentLayer", orePositions);
        }
        saveFilterStacks(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        currentDepth = tag.getInt("currentDepth");
        currentOreIndex = tag.getInt("currentOreIndex");
        silkTouchMode = tag.getBoolean("silkTouchMode");
        autoEjectMode = tag.getBoolean("autoEjectMode");
        whitelistMode = tag.getBoolean("whitelistMode");
        workTicker = tag.getInt("workTicker");
        if (tag.contains("tipPos")) {
            tipPos = BlockPos.of(tag.getLong("tipPos"));
        }
        oresInCurrentLayer.clear();
        if (tag.contains("oresInCurrentLayer")) {
            long[] orePositions = tag.getLongArray("oresInCurrentLayer");
            for (long pos : orePositions) {
                oresInCurrentLayer.add(BlockPos.of(pos));
            }
        }
        loadFilterStacks(tag, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("silkTouchMode", silkTouchMode);
        tag.putBoolean("whitelistMode", whitelistMode);
        saveFilterStacks(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("silkTouchMode")) {
            silkTouchMode = tag.getBoolean("silkTouchMode");
        }
        if (tag.contains("whitelistMode")) {
            whitelistMode = tag.getBoolean("whitelistMode");
        }
        loadFilterStacks(tag, registries);
    }

    private void saveFilterStacks(CompoundTag tag, HolderLookup.Provider registries) {
        net.minecraft.nbt.ListTag filterList = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < FILTER_COUNT; i++) {
            if (!filterStacks[i].isEmpty()) {
                net.minecraft.nbt.CompoundTag filterTag = new net.minecraft.nbt.CompoundTag();
                filterTag.putInt("Slot", i);
                filterStacks[i].save(registries, filterTag);
                filterList.add(filterTag);
            }
        }
        tag.put("FilterStacks", filterList);
    }

    private void loadFilterStacks(CompoundTag tag, HolderLookup.Provider registries) {
        for (int i = 0; i < FILTER_COUNT; i++) {
            filterStacks[i] = ItemStack.EMPTY;
        }
        if (tag.contains("FilterStacks", net.minecraft.nbt.Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag filterList = tag.getList("FilterStacks", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < filterList.size(); i++) {
                net.minecraft.nbt.CompoundTag filterTag = filterList.getCompound(i);
                int slot = filterTag.getInt("Slot");
                if (slot >= 0 && slot < FILTER_COUNT) {
                    filterStacks[slot] = ItemStack.parse(registries, filterTag).orElse(ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.advanced_miner_elc");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.AdvancedMinerElcMenu(containerId, playerInventory, this);
    }

    /**
     * 每tick更新逻辑
     */
        public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_advanced_miner_elc blockEntity) {
        mio_icif_producer.tick(level, pos, state, blockEntity);

        if (!level.isClientSide()) {
            blockEntity.chargeTool();
            
            boolean isLit = state.getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_advanced_miner_elc.LIT);
            if (blockEntity.isWorking() != isLit) {
                level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_advanced_miner_elc.LIT, blockEntity.isWorking()), 3);
            }
        }
    }

    private void chargeTool() {
        ItemStack scannerStack = itemHandler.getStackInSlot(SLOT_SCANNER);
        if (!scannerStack.isEmpty() && getItemAPI().isElectricTool(scannerStack)) {
            long availableEnergy = energyStorage.getAmount();
            if (availableEnergy > 0) {
                long scannerMaxEnergy = getItemAPI().getElectricToolMaxEnergy(scannerStack);
                long currentScannerEnergy = getItemAPI().getElectricToolStored(scannerStack);
                long canAdd = Math.min(scannerMaxEnergy - currentScannerEnergy, availableEnergy);
                if (canAdd > 0) {
                    long added = getItemAPI().chargeElectricTool(scannerStack, canAdd, false);
                    if (added > 0) {
                        apiUseEnergy(added, false);
                    }
                }
            }
        }
    }

    /**
     * 处理GUI按钮点击事件（对齐原版IC2
* id=0: 重置采矿位置
     * id=1: 切换黑名白名单模
* id=2: 切换精准采集模式
     */
    public void handleButtonClick(int id) {
        if (level == null || level.isClientSide) return;
        
        switch (id) {
            case 0 -> {
                tipPos = null;
                currentDepth = 0;
                currentOreIndex = 0;
                oresInCurrentLayer.clear();
                setChanged();
            }
            case 1 -> {
                whitelistMode = !whitelistMode;
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
            case 2 -> {
                silkTouchMode = !silkTouchMode;
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    // Getters
    public boolean isSilkTouchMode() { return silkTouchMode; }
    public void setSilkTouchMode(boolean mode) { this.silkTouchMode = mode; setChanged(); }
    public boolean isAutoEjectMode() { return autoEjectMode; }
    public void setAutoEjectMode(boolean mode) { this.autoEjectMode = mode; setChanged(); }
    public boolean isWhitelistMode() { return whitelistMode; }
    public void setWhitelistMode(boolean mode) { this.whitelistMode = mode; setChanged(); }
    public int getOverclockerCount() { return overclockerCount; }
    public int getEnergyStorageCount() { return energyStorageCount; }
    public int getTractorBeamCount() { return tractorBeamCount; }
    public int getEffectiveScanRadius() { return effectiveScanRadius; }
    public int getCurrentDepth() { return currentDepth; }
}
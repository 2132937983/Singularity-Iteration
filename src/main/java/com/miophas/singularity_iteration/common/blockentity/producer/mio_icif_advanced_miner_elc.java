package com.miophas.singularity_iteration.common.blockentity.producer;

import com.mojang.authlib.GameProfile;
import com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_od_scanner;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_ov_scanner;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.runtime.processing.PendingDrops;
import com.miophas.singularity_iteration.core.runtime.processing.MiningLoot;
import com.miophas.singularity_iteration.core.runtime.processing.MiningPayment;
import com.miophas.singularity_iteration.core.runtime.processing.MiningLayer;
import com.miophas.singularity_iteration.core.runtime.processing.MachineActionOwner;
import com.miophas.singularity_iteration.core.runtime.energy.ToolEnergy;
import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
public class mio_icif_advanced_miner_elc extends AbstractProcessingMachineBlockEntity implements MiningRangeProvider {
    private static final String ACTION_OWNER_KEY = "scex_machine_action_owner";
    private static final GameProfile LEGACY_ACTOR = new GameProfile(
        UUID.nameUUIDFromBytes("mio_icif:automated_miner".getBytes(StandardCharsets.UTF_8)), "[SI Miner]");
    private final MiningPayment scexMiningPayment = new MiningPayment(() -> ContainerToTank.markUnsaved(this));
    private boolean scexScanConfigurationDirty = true;
    private final int[] scexOutputCursors = new int[6];
    private final boolean[] scexOutputScanned = new boolean[6];
    private ItemStack scexOutputHead = ItemStack.EMPTY;
    private final PendingDrops scexPendingDrops = new PendingDrops(() -> ContainerToTank.markUnsaved(this));
    private MachineActionOwner actionOwner = MachineActionOwner.legacy(LEGACY_ACTOR);

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
    // 已同步到客户端的工作半径，用于在扫描器变化时触发一次方块实体同步
    private int scexSyncedRangeRadius = -1;
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
     * 覆写物品栏创建逻辑，使升级槽内容变化时立即标记扫描配置失效。
     *
     * <p>基类 {@code onInventoryChanged} 是 final 的，无法感知本机自己的
     * {@code scexScanConfigurationDirty}；若不在这里标记，超频/储能升级需要等到
     * {@code onTick} 中每 100 tick 一次的周期性重扫才会生效（最长 5 秒延迟），
     * 表现为"装了超频但每周期挖掘数量没涨"。
     */
    @Override
    protected com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler createItemHandler(SlotLayout layout) {
        com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler handler =
            new com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler(layout) {
                @Override
                protected void onContentsChanged(int slot) {
                    onInventoryChanged(slot);
                    if (layout.isType(slot, com.miophas.singularity_iteration.core.prefab.inventory.SlotType.UPGRADE)) {
                        scexScanConfigurationDirty = true;
                    }
                }
            };
        handler.setValidator(this);
        return handler;
    }

    /**
     * 获取过滤槽物
*/
    public ItemStack getFilterStack(int index) {
        if (index >= 0 && index < FILTER_COUNT) {
            return filterStacks[index].copy();
        }
        return ItemStack.EMPTY;
    }

    /**
     * 设置过滤槽物
*/
    public void setFilterStack(int index, ItemStack stack) {
        if (index >= 0 && index < FILTER_COUNT) {
            filterStacks[index] = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
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
        var scanner = itemHandler.getStackInSlot(SLOT_SCANNER);
        if (scanner.getItem() instanceof mio_icif_ov_scanner) return ScannerType.OV;
        if (scanner.getItem() instanceof mio_icif_od_scanner) return ScannerType.OD;
        return ScannerType.NONE;
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

    /** 客户端渲染工作范围边界时使用的水平半径，与实际开挖范围保持一致。 */
    @Override
    public int getMiningRangeRadius() {
        return Math.clamp(getScanRadiusByScanner(), 0, MiningLayer.MAX_RADIUS);
    }

    /**
     * 检查物品是否是升级
     */
    private boolean isUpgrade(ItemStack stack) {
        return stack.getItem() instanceof com.miophas.singularity_iteration.common.item.upgrade.mio_icif_upgrade;
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
        if (!actionOwner.canAct() || scexMiningPayment.isBusy() || scexPendingDrops.isBusy() || !scexPendingDrops.isEmpty()) return false;
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
        maxBlockScanCount = MiningLayer.cycleBudget(overclockerCount);

        long newCapacity = DEFAULT_CAPACITY + (Math.max(0, energyStorageCount) * 100000L);
        if (energyStorage.getCapacity() != newCapacity) {
            energyStorage.setCapacity(newCapacity);
        }
    }

    @Override
    protected void onTick() {
        if (!actionOwner.canAct()) return;
        if (level instanceof ServerLevel && (scexScanConfigurationDirty || level.getGameTime() % 100 == 0)) {
            scanUpgrades(); scexScanConfigurationDirty = false;
        }
        flushPendingLoot();
        syncRangeRadiusToClient();
    }

    /**
     * 扫描器决定的工作半径发生变化时，向客户端同步一次方块实体数据，
     * 以便客户端工作范围渲染能即时更新。
     */
    private void syncRangeRadiusToClient() {
        int radius = getMiningRangeRadius();
        if (radius == scexSyncedRangeRadius) return;
        scexSyncedRangeRadius = radius;
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /**
     * 默认实现返回 null，会导致 {@link #syncRangeRadiusToClient()} 触发的方块更新
     * 无法携带方块实体数据发往客户端（表现为只有重新进入存档才刷新）。
     */
    @Nullable
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void doWork() {
        if (!(level instanceof ServerLevel serverLevel) || !serverLevel.getServer().isSameThread()) return;
        if (scexMiningPayment.isBusy() || scexPendingDrops.isBusy() || !scexPendingDrops.isEmpty()) { stopWork(); return; }
        if (tipPos == null || tipPos.getY() >= worldPosition.getY() || tipPos.getX() != worldPosition.getX()
                || tipPos.getZ() != worldPosition.getZ()) {
            scanUpgrades();
            tipPos = worldPosition.below(); currentDepth = 0; currentOreIndex = 0;
            generateLayerBlocks();
        }
        if (tipPos.getY() < level.getMinBuildHeight() || tipPos.getY() >= level.getMaxBuildHeight()) { stopWork(); return; }
        if (oresInCurrentLayer.isEmpty()) generateLayerBlocks();
        if (currentOreIndex >= oresInCurrentLayer.size()) {
            if (tipPos.getY() <= level.getMinBuildHeight()) { stopWork(); return; }
            tipPos = tipPos.below(); currentDepth = Math.max(0, worldPosition.getY() - tipPos.getY() - 1); currentOreIndex = 0;
            generateLayerBlocks();
        }
        workTicker++;
        ContainerToTank.markUnsaved(this);
        if (workTicker < DEFAULT_WORK_TIME) { isWorking = true; return; }
        workTicker = 0;
        int scanned = 0;
        int iterations = 0;
        int maxIterations = maxBlockScanCount * 20;
        while (scanned < maxBlockScanCount && currentOreIndex < oresInCurrentLayer.size() && scexPendingDrops.isEmpty() && iterations < maxIterations) {
            BlockPos targetPos = oresInCurrentLayer.get(currentOreIndex);
            iterations++;
            if (!serverLevel.getWorldBorder().isWithinBounds(targetPos)) { currentOreIndex++; continue; }
            if (!serverLevel.getChunkSource().hasChunk(targetPos.getX() >> 4, targetPos.getZ() >> 4)) { stopWork(); return; }
            BlockState targetState = serverLevel.getBlockState(targetPos);
            if (!canMineBlock(targetState, targetPos)) { currentOreIndex++; continue; }
            scanned++;
            if (!mineBlock(serverLevel, targetPos)) { stopWork(); return; }
            currentOreIndex++;
        }
        isWorking = scanned > 0 || iterations > 0;
    }

    @Override
    protected void updateProgress() {
        // This machine schedules work with its persisted workTicker, not the generic recipe counter.
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
        if (tipPos == null) return;
        var center = new BlockPos(worldPosition.getX(), tipPos.getY(), worldPosition.getZ());
        oresInCurrentLayer.addAll(MiningLayer.positions(center, Math.clamp(effectiveScanRadius, 0, MiningLayer.MAX_RADIUS)));
        ContainerToTank.markUnsaved(this);
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
   if (state.isAir() || state.is(Blocks.BEDROCK) || state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
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
        if (!serverLevel.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                || !canMineBlock(serverLevel.getBlockState(pos), pos)) return false;
        ItemStack tool = ItemStack.EMPTY;
        if (silkTouchMode) {
            tool = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
            var silk = serverLevel.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .get(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
            if (silk.isPresent()) tool.enchant(silk.get(), 1);
        }
        final ItemStack lootTool = tool;
        boolean removed = scexMiningPayment.attempt(energyStorage, itemHandler, SLOT_SCANNER, getItemAPI(),
            BASE_ENERGY_COST, SCANNER_ENERGY_COST,
            payment -> MiningLoot.capture(serverLevel, pos, lootTool, actionOwner, scexPendingDrops, drops -> true, payment));
        if (removed) flushPendingLoot();
        return removed;
    }

    /**
     * 将物品插入相邻容器或掉落在地
* 高级采矿机没有内部存储槽，挖掘的方块需要输出到相邻容器或掉
*/
    private void resetLootOutputScan() {
        java.util.Arrays.fill(scexOutputCursors, 0);
        java.util.Arrays.fill(scexOutputScanned, false);
        scexOutputHead = scexPendingDrops.first();
    }

    private void flushPendingLoot() {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread() || scexPendingDrops.isEmpty()) return;
        if (!ItemStack.matches(scexOutputHead, scexPendingDrops.first())) resetLootOutputScan();
        boolean allScanned = true;
        for (Direction direction : Direction.values()) {
            int face = direction.ordinal();
            BlockPos neighborPos = worldPosition.relative(direction);
            var target = getAdjacentItemHandler(neighborPos, direction.getOpposite());
            if (target == null) { scexOutputScanned[face] = true; continue; }
            int size = target.getSlots();
            int start = Math.min(Math.max(0, scexOutputCursors[face]), size);
            int end = (int) Math.min(size, (long) start + 64);
            int moved = scexPendingDrops.deliverRange(target, start, end - start);
            if (moved > 0) {
                var neighbor = level.getBlockEntity(neighborPos);
                if (neighbor != null) ContainerToTank.markUnsaved(neighbor);
                resetLootOutputScan();
                return;
            }
            scexOutputCursors[face] = end;
            if (end >= size) scexOutputScanned[face] = true;
            allScanned &= scexOutputScanned[face];
        }
        if (allScanned) {
            // Scan every slot, across ticks for large handlers, before using the existing ground-output fallback.
            scexPendingDrops.spawn(server, worldPosition.above());
            resetLootOutputScan();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(ACTION_OWNER_KEY, actionOwner.save());
        tag.put("scex_pending_mining_drops", scexPendingDrops.save(registries));
        tag.put("scex_mining_payment", scexMiningPayment.save());
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
        actionOwner = MachineActionOwner.load(tag, ACTION_OWNER_KEY, LEGACY_ACTOR);
        scexPendingDrops.load(registries, tag.getList("scex_pending_mining_drops", net.minecraft.nbt.Tag.TAG_COMPOUND));
        scexMiningPayment.load(tag.getCompound("scex_mining_payment"));
        scexScanConfigurationDirty = true;
        currentDepth = Math.max(0, tag.getInt("currentDepth"));
        currentOreIndex = Math.max(0, tag.getInt("currentOreIndex"));
        silkTouchMode = tag.getBoolean("silkTouchMode");
        autoEjectMode = tag.getBoolean("autoEjectMode");
        whitelistMode = tag.getBoolean("whitelistMode");
        workTicker = Math.clamp(tag.getInt("workTicker"), 0, DEFAULT_WORK_TIME - 1);
        tipPos = null;
        if (tag.contains("tipPos")) {
            var savedTip = BlockPos.of(tag.getLong("tipPos"));
            // 高级采矿机只会向下开采：开采层必须严格位于机器下方。
            // 结构方块 /clone、蓝图搬运模组复制方块实体 NBT，或旧存档可能写入更高的 Y，
            // 若不校验，机器会回头开挖自己上方（含风电等玩家建筑）的整层方块。
            if (savedTip.getY() < worldPosition.getY()) tipPos = savedTip;
        }
        oresInCurrentLayer.clear();
        if (tag.contains("oresInCurrentLayer")) {
            long[] orePositions = tag.getLongArray("oresInCurrentLayer");
            if (tipPos != null && tipPos.getX() == worldPosition.getX() && tipPos.getZ() == worldPosition.getZ()
                    && orePositions.length <= MiningLayer.MAX_POSITIONS) {
                var unique = new java.util.HashSet<BlockPos>();
                for (long packed : orePositions) {
                    var position = BlockPos.of(packed);
                    if (!MiningLayer.contains(tipPos, position) || !unique.add(position)) { oresInCurrentLayer.clear(); break; }
                    oresInCurrentLayer.add(position);
                }
            }
        }
        currentOreIndex = Math.min(currentOreIndex, oresInCurrentLayer.size());
        if (tipPos != null && (tipPos.getX() != worldPosition.getX() || tipPos.getZ() != worldPosition.getZ())) tipPos = null;
        resetLootOutputScan();
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
        return new com.miophas.singularity_iteration.common.menu.producer.AdvancedMinerElcMenu(containerId, playerInventory, this);
    }

    /**
     * 每tick更新逻辑
     */
        public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_advanced_miner_elc blockEntity) {
        if (!blockEntity.actionOwner.canAct()) { blockEntity.stopWork(); return; }
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, blockEntity);

        if (!level.isClientSide()) {
            blockEntity.chargeTool();

            boolean isLit = state.getValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_advanced_miner_elc.LIT);
            if (blockEntity.isWorking() != isLit) {
                level.setBlock(pos, state.setValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_advanced_miner_elc.LIT, blockEntity.isWorking()), 3);
            }
        }
    }

    private void chargeTool() {
        if (scexMiningPayment.isBusy() || scexPendingDrops.isBusy()) return;
        ToolEnergy.charge(itemHandler, SLOT_SCANNER, energyStorage, getItemAPI());
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
    public MachineActionOwner getActionOwner() { return actionOwner; }
    public void setActionOwnerFromPlacer(@Nullable LivingEntity placer) {
        actionOwner = MachineActionOwner.fromPlacer(placer);
        ContainerToTank.markUnsaved(this);
    }
}
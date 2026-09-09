package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Singularity_Iteration_Config;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Items.EnvTemplate.mio_icif_EvT_default;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * 地形转换机方块实体类
 * 大规模地形机器，以使用地形工具来使用生态改变方块状态
 * 没有GUI，使用权杖右键地形转换模板物品来切换/蹲下右键取出
 * 作用方式类似：以机器为中心使用56x256的圆形区域
 * 消耗大量电力来工作，模板通过GUI调整区域但机器实际使用服务器线程完成工作
 * 使用服务器线程来计算缩放散列数和区域形态来检查卡顿
 */
@SuppressWarnings("null")
public class mio_icif_terra_elc extends mio_icif_producer {

    private static final Logger LOGGER = LoggerFactory.getLogger(mio_icif_terra_elc.class);

    // 槽位和索引
    public static final int TEMPLATE_SLOT = 0;  // 模板槽
    public static final int BATTERY_SLOT = 1;   // 电池槽
    // 默认配置
    public static final long DEFAULT_CAPACITY = 100000L;  // 100k EU （对比IC2原版）
    public static final long DEFAULT_MAX_RECEIVE = 512L;  // HV级最大输入512 EU/t
    public static final long DEFAULT_MAX_EXTRACT = 0L;    // 不输出电力
    public static final int DEFAULT_WORK_TIME = 1;        // 工作时间极短，每tick都尝试执行一次
    public static final long DEFAULT_ENERGY_PER_TICK = 100L; // 每tick消耗100 EU
    public static final int DEFAULT_WORK_INTERVAL = 1;    // 每tick工作一次
    // 从配置文件读取参数
    public static int getRange() {
        return Singularity_Iteration_Config.TERRA_RANGE.get();
    }

    public static int getBlocksPerTick() {
        return Singularity_Iteration_Config.TERRA_BLOCKS_PER_TICK.get();
    }

    // 地形转换算法的延迟实现类似IC2实现方式
    private int lastX = 0;             // 上次转换偏移量X轴，螺旋的继续
    private int lastZ = 0;             // 上次转换偏移量Z轴，螺旋的继续
    private int failedAttempts = 0;    // 连续失败次数
    private int processedBlocks = 0;       // 已处理的方块总数
    private int convertedBlocks;       // 本次转换的方块数
    private transient boolean hasLoggedFirstTick; // 防止重复记录第一次tick日志，仅用于调试
    // 待处理的方块更改队列
    private final List<BlockChange> pendingChanges = new ArrayList<>();
    private int tickCounter = 0;
    private static final int BATCH_SIZE = 10;  // 每tick最多应用10个方块更改
    private static final int SKIP_TICKS = 2;   // 每2tick应用一次待处理更改
    // 方块更改记录类
    private static class BlockChange {
        final BlockPos pos;
        final BlockState newState;
        
        BlockChange(BlockPos pos, BlockState newState) {
            this.pos = pos;
            this.newState = newState;
        }
    }

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .battery()
        .build();

    public mio_icif_terra_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.TERRA_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_terra_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.HV);

        // 初始化状态变量
        this.lastX = 0;
        this.lastZ = 0;
        this.failedAttempts = 0;
        this.processedBlocks = 0;
        this.convertedBlocks = 0;
        this.tickCounter = 0;
    }

    /**
     * 检查槽位是否存放适合特定槽位
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == TEMPLATE_SLOT) {
            // 模板槽位只接受地形转换模板物品
            return stack.getItem() instanceof mio_icif_EvT_default;
        } else if (slot == BATTERY_SLOT) {
            // 电池槽：接受电池类物品
            return isBattery(stack);
        }
        return false;
    }

    /**
     * 获取特定方向可访问的槽位
     */
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // 任何方向只能访问电池槽，模板槽只能通过GUI交互来交换物品
        return new int[]{BATTERY_SLOT};
    }

    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    /**
     * 检查指定槽位是否可以特定方向提取
     */
    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        // 只有电池槽可以提取
        return slot == BATTERY_SLOT;
    }

    /**
     * 获取模板槽中物品
     */
    public ItemStack getTemplate() {
        return itemHandler.getStackInSlot(TEMPLATE_SLOT);
    }

    /**
     * 设置模板槽中物品
     */
    public void setTemplate(ItemStack stack) {
        // 检查模板类型是否改变
        mio_icif_EvT_default.TemplateType oldType = getTemplateType();
        itemHandler.setStackInSlot(TEMPLATE_SLOT, stack);
        mio_icif_EvT_default.TemplateType newType = getTemplateType();
        
        // 如果模板类型改变，重置工作状态
        if (oldType != newType) {
            resetWorkState();
        }
        
        setChanged();
    }

    /**
     * 检查是否有模板
     */
    public boolean hasTemplate() {
        return !getTemplate().isEmpty();
    }

    /**
     * 获取模板类型
     */
    public mio_icif_EvT_default.TemplateType getTemplateType() {
        ItemStack template = getTemplate();
        if (template.getItem() instanceof mio_icif_EvT_default evt) {
            return evt.getTemplateType();
        }
        return mio_icif_EvT_default.TemplateType.EMPTY;
    }

    /**
     * 是否是空白模板
     */
    public boolean isBlankTemplate() {
        return getTemplateType() == mio_icif_EvT_default.TemplateType.EMPTY;
    }

    /**
     * 重置工作状态
     */
    protected void resetWorkState() {
        progress = 0;
        isWorking = false;
        lastX = 0;
        lastZ = 0;
        failedAttempts = 0;
        processedBlocks = 0;
        convertedBlocks = 0;
        pendingChanges.clear();
        tickCounter = 0;
        setChanged();
    }

    /**
     * 检查机器是否可以工作
     */
    @Override
    protected boolean canWork() {
        // 检查是否有模板
        if (!hasTemplate()) {
            return false;
        }

        // 检查是否空白模板，空白模板不工作
        if (isBlankTemplate()) {
            return false;
        }

        // 检查方块是否被玩家经常使用，玩家在附近运行会导致位置偏移
        if (level instanceof ServerLevel serverLevel) {
            if (!serverLevel.isLoaded(worldPosition)) {
                return false;
            }
            // 检查是否有玩家在附近128格以内
            boolean hasNearbyPlayer = false;
            for (var player : serverLevel.players()) {
                if (player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 128 * 128) {
                    hasNearbyPlayer = true;
                    break;
                }
            }
            if (!hasNearbyPlayer) {
                return false;
            }
        }

        // 检查是否有足够能量来完成地形类任务
        return super.hasEnoughEnergy();
    }

    /**
     * 执行机器工作
     */
    @Override
    protected void doWork() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // 每次执行前再次检查附近玩家，防止玩家离开后继续工作
        boolean hasNearbyPlayer = false;
        for (var player : serverLevel.players()) {
            if (player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 128 * 128) {
                hasNearbyPlayer = true;
                break;
            }
        }
        if (!hasNearbyPlayer) {
            stopWork();
            return;
        }

        // 检查能量
        if (!consumeEnergy()) {
            stopWork();
            return;
        }

        isWorking = true;
        tickCounter++;

        // 执行螺旋地形转换，从机器位置开始螺旋扩散
        performSpiralTransformation(serverLevel);
        
        // 使用定时器来更新待处理的更改
        if (tickCounter >= SKIP_TICKS) {
            applyPendingChanges(serverLevel);
            tickCounter = 0;
        }
    }
    
    /**
     * 应用待处理的方块更改
     */
    private void applyPendingChanges(ServerLevel serverLevel) {
        if (pendingChanges.isEmpty()) {
            return;
        }
        
        // 每tick最多应用BATCH_SIZE个方块更改
        int toApply = Math.min(BATCH_SIZE, pendingChanges.size());
        
        for (int i = 0; i < toApply; i++) {
            BlockChange change = pendingChanges.remove(0);
            
            // 检查位置是否仍然加载
            if (!serverLevel.isLoaded(change.pos)) {
                continue;
            }
            
            // 使用flags=2来应用更改，不触发方块更新
            serverLevel.setBlock(change.pos, change.newState, 2);
        }
        
        if (!pendingChanges.isEmpty()) {
            setChanged();
        }
    }

    /**
     * 执行地形转换算法（螺旋模式）
     * 使用机器位置开始，逐次从lastPos附近搜索下一个螺旋转换位置
     */
    protected void performSpiralTransformation(ServerLevel serverLevel) {
        int centerX = worldPosition.getX();
        int centerY = worldPosition.getY();
        int centerZ = worldPosition.getZ();
        int range = getRange();
        int radiusSq = range * range;
        int blocksPerTick = getBlocksPerTick();

        @SuppressWarnings("unused")
        int processed = 0;

        // 使用level的随机数生成器来做有序螺旋的确定性随机
        net.minecraft.util.RandomSource random = serverLevel.getRandom();

        // 每tick处理多个位置
        for (int i = 0; i < blocksPerTick; i++) {
            int xOffset, zOffset;
            
            // 获取下一个待处理位置，基于上次位置的附近搜索
            if (lastX != 0 || lastZ != 0) {
                // 从lastPos附近随机偏移来生成螺旋效果
                int searchRange = Math.max(1, range / 10);
                xOffset = lastX - random.nextInt(searchRange + 1) + random.nextInt(searchRange + 1);
                zOffset = lastZ - random.nextInt(searchRange + 1) + random.nextInt(searchRange + 1);
            } else {
                // 第一次或者失败次数过多时在全范围内搜索
                if (failedAttempts > 4) {
                    failedAttempts = 4;
                }
                int searchRange = range * (failedAttempts + 1) / 5;
                xOffset = -random.nextInt(searchRange + 1) + random.nextInt(searchRange + 1);
                zOffset = -random.nextInt(searchRange + 1) + random.nextInt(searchRange + 1);
            }
            
            // 检查是否在范围内
            int distSqXZ = xOffset * xOffset + zOffset * zOffset;
            if (distSqXZ > radiusSq) {
                // 超出范围增加失败次数
                failedAttempts++;
                lastX = 0;
                lastZ = 0;
                continue;
            }
            
            // 处理这一列
            int blocksConverted = processSpiralColumn(serverLevel, centerX + xOffset, centerY, centerZ + zOffset, distSqXZ, radiusSq);
            processed += blocksConverted;
            processedBlocks++;
            
            if (blocksConverted > 0) {
                // 转换成功，更新lastPos
                convertedBlocks += blocksConverted;
                lastX = xOffset;
                lastZ = zOffset;
                failedAttempts = 0;
            } else {
                // 转换失败，增加失败次数
                failedAttempts++;
                // 如果连续失败太多，重置位置
                if (failedAttempts > 10) {
                    lastX = 0;
                    lastZ = 0;
                    failedAttempts = 0;
                }
            }
        }
        
        setChanged();
    }

    /**
     * 处理机器中心列中的每个方块的地形转换
     * @return 本次转换的方块数
     */
    protected int processSpiralColumn(ServerLevel serverLevel, int x, int yCenter, int z, int distSqXZ, int radiusSq) {
        // 计算Y轴范围，根据圆形形态来确定
        int distY = radiusSq - distSqXZ;
        if (distY <= 0) return 0;

        int yRange = (int) Math.sqrt(distY);

        int converted = 0;

        // 从中心Y位置开始搜索，优先处理与机器相同的Y高度附近的方块
        // 这样可以利用最佳优化来避免处理与机器高度相差太远的地方
        BlockPos centerPos = new BlockPos(x, yCenter, z);
        if (checkAndQueueTransform(serverLevel, centerPos)) {
            converted++;
        }

        // 向上下两侧扩展
        for (int yOffset = 1; yOffset <= yRange; yOffset++) {
            // 上方
            BlockPos posUp = new BlockPos(x, yCenter + yOffset, z);
            if (checkAndQueueTransform(serverLevel, posUp)) {
                converted++;
            }

            // 下方
            BlockPos posDown = new BlockPos(x, yCenter - yOffset, z);
            if (checkAndQueueTransform(serverLevel, posDown)) {
                converted++;
            }
        }

        return converted;
    }
    
    /**
     * 检查方块是否需要转换并加入队列
     * @return 是否加入了转换队列
     */
    protected boolean checkAndQueueTransform(ServerLevel serverLevel, BlockPos pos) {
        // 检查位置是否加载
        if (!serverLevel.isLoaded(pos)) {
            return false;
        }

        BlockState currentState = serverLevel.getBlockState(pos);
        Block currentBlock = currentState.getBlock();

        // 跳过空气方块
        if (currentState.isAir()) {
            return false;
        }

        // 根据模板类型获取目标状态
        BlockState newState = getTargetState(currentState, currentBlock, pos, serverLevel);
        
        if (newState != null) {
            // 加入待处理队列，不是立即更改
            pendingChanges.add(new BlockChange(pos, newState));
            return true;
        }
        
        return false;
    }
    
    /**
     * 根据模板类型获取目标方块状态
     * @return 目标状态，如果不需要转换则返回null
     */
    @Nullable
    protected BlockState getTargetState(BlockState currentState, Block block, BlockPos pos, ServerLevel serverLevel) {
        mio_icif_EvT_default.TemplateType templateType = getTemplateType();

        return switch (templateType) {
            case DESERTIFICATION -> getDesertTargetState(currentState, block);
            case CULTIVATION -> getCultivationTargetState(currentState, block);
            case IRRIGATION -> getIrrigationTargetState(currentState, block);
            case CHILLING -> getChillingTargetState(currentState, block);
            case FLATIFICATION -> getFlatTargetState(currentState, block, pos, serverLevel);
            case MUSHROOM -> getMushroomTargetState(currentState, block);
            default -> null;
        };
    }

    /**
     * 转换指定位置的方块
     * @return 是否成功加入转换队列
     */
    protected boolean transformBlock(ServerLevel serverLevel, BlockPos pos) {
        return checkAndQueueTransform(serverLevel, pos);
    }

    /**
     * 沙漠化模板：获取目标方块状态
     */
    @Nullable
    protected BlockState getDesertTargetState(BlockState state, Block block) {
        // 使用标签匹配dirt类方块
        if (state.is(BlockTags.DIRT)) {
            return Blocks.SAND.defaultBlockState();
        }

        // 使用标签匹配stone类方块
        if (state.is(BlockTags.STONE_ORE_REPLACEABLES) || block == Blocks.COBBLESTONE) {
            return Blocks.SANDSTONE.defaultBlockState();
        }

        // 移除植物
        if (isPlant(block)) {
            return Blocks.AIR.defaultBlockState();
        }

        return null;
    }

    /**
     * 耕地模板：获取目标方块状态
     */
    @Nullable
    protected BlockState getCultivationTargetState(BlockState state, Block block) {
        // 使用标签匹配sand类方块
        if (state.is(BlockTags.SAND)) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }

        // 使用标签匹配dirt类方块且不是草地
        if (state.is(BlockTags.DIRT) && block != Blocks.GRASS_BLOCK) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }

        return null;
    }

    /**
     * 灌溉模板：获取目标方块状态
     */
    @Nullable
    protected BlockState getIrrigationTargetState(BlockState state, Block block) {
        // 使用标签匹配sand类方块
        if (state.is(BlockTags.SAND)) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }

        return null;
    }

    /**
     * 降温模板：获取目标方块状态
     */
    @Nullable
    protected BlockState getChillingTargetState(BlockState state, Block block) {
        // 检查是否有流体状态，如果有则转为冰
        if (!state.getFluidState().isEmpty()) {
            return Blocks.ICE.defaultBlockState();
        }

        // 使用标签匹配dirt类方块，放置雪
        if (state.is(BlockTags.DIRT)) {
            // 返回原状态，雪会在上方生成
            return state;
        }

        return null;
    }

    /**
     * 平地模板：获取目标方块状态
     * 根据机器高度切平圆形区域内的地形
     * - 高于机器：移除上方方块为空气
     * - 低于机器：填充为泥土
     * - 已平整：与机器相同高度则跳过，不做任何更改
     */
    @Nullable
    protected BlockState getFlatTargetState(BlockState state, Block block, BlockPos pos, ServerLevel serverLevel) {
        int machineY = worldPosition.getY();
        int currentY = pos.getY();

        // 情况1：方块位置高于机器位置，移除上方方块为空气
        if (currentY > machineY) {
            // 检查上方是否有覆盖方块为保护玩家建筑
            if (!hasCoveringAbove(serverLevel, pos)) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        // 情况2：方块位置低于机器位置，填充为泥土
        if (currentY < machineY) {
            // 如果已经是可通行表面如泥土类则不做更改
            if (state.is(BlockTags.DIRT)) {
                return null;
            }
            // 其他方块如石头等填充为泥土
            return Blocks.DIRT.defaultBlockState();
        }

        // 情况3：方块位置与机器相同高度，已平整，跳过
        return null;
    }

    /**
     * 蘑菇模板：获取目标方块状态
     */
    @Nullable
    protected BlockState getMushroomTargetState(BlockState state, Block block) {
        // 使用标签匹配dirt类方块则转换为菌丝
        if (state.is(BlockTags.DIRT)) {
            return Blocks.MYCELIUM.defaultBlockState();
        }

        return null;
    }

    /**
     * 沙漠化模板：直接修改方块的方法
     */
    protected boolean transformToDesert(ServerLevel serverLevel, BlockPos pos, BlockState state, Block block) {
        BlockState target = getDesertTargetState(state, block);
        if (target != null) {
            serverLevel.setBlock(pos, target, 2);
            return true;
        }
        return false;
    }

    /**
     * 耕地模板：将sand方块转换为草地
     */
    protected boolean transformToCultivation(ServerLevel serverLevel, BlockPos pos, BlockState state, Block block) {
        BlockState target = getCultivationTargetState(state, block);
        if (target != null) {
            serverLevel.setBlock(pos, target, 2);
            return true;
        }
        return false;
    }

    /**
     * 灌溉模板：将沙方块转换为草地含水
     */
    protected boolean transformToIrrigation(ServerLevel serverLevel, BlockPos pos, BlockState state, Block block) {
        BlockState target = getIrrigationTargetState(state, block);
        if (target != null) {
            serverLevel.setBlock(pos, target, 2);
            return true;
        }
        return false;
    }

    /**
     * 降温模板：放置雪和水
     */
    protected boolean transformToChilling(ServerLevel serverLevel, BlockPos pos, BlockState state, Block block) {
        // 检查是否有流体状态，有则转为冰
        if (!state.getFluidState().isEmpty()) {
            serverLevel.setBlock(pos, Blocks.ICE.defaultBlockState(), 2);
            return true;
        }

        // 使用标签匹配dirt类方块，在上方放置雪
        if (state.is(BlockTags.DIRT)) {
            BlockPos above = pos.above();
            if (serverLevel.getBlockState(above).isAir()) {
                serverLevel.setBlock(above, Blocks.SNOW.defaultBlockState(), 2);
                return true;
            }
        }

        return false;
    }

    /**
     * 平地模板：平整地形
     */
    protected boolean transformToFlat(ServerLevel serverLevel, BlockPos pos, BlockState state, Block block) {
        int machineY = worldPosition.getY();

        // 如果方块位置高于机器位置，移除上方方块为空气
        if (pos.getY() > machineY) {
            // 检查上方是否有覆盖方块为保护玩家建筑
            if (!hasCoveringAbove(serverLevel, pos)) {
                serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                return true;
            }
        }

        // 如果方块位置低于机器位置，填充为泥土
        if (pos.getY() < machineY) {
            serverLevel.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
            return true;
        }

        return false;
    }

    /**
     * 蘑菇模板：转换为菌丝
     */
    protected boolean transformToMushroom(ServerLevel serverLevel, BlockPos pos, BlockState state, Block block) {
        // 使用标签匹配dirt类方块则转换为菌丝
        if (state.is(BlockTags.DIRT)) {
            serverLevel.setBlock(pos, Blocks.MYCELIUM.defaultBlockState(), 2);
            return true;
        }

        return false;
    }

    /**
     * 检查是否是植物方块
     */
    protected boolean isPlant(Block block) {
        return block == Blocks.SHORT_GRASS ||
               block == Blocks.TALL_GRASS ||
               block == Blocks.FERN ||
               block == Blocks.LARGE_FERN ||
               block == Blocks.DEAD_BUSH ||
               block == Blocks.DANDELION ||
               block == Blocks.POPPY ||
               block == Blocks.BLUE_ORCHID ||
               block == Blocks.ALLIUM ||
               block == Blocks.AZURE_BLUET ||
               block == Blocks.RED_TULIP ||
               block == Blocks.ORANGE_TULIP ||
               block == Blocks.WHITE_TULIP ||
               block == Blocks.PINK_TULIP ||
               block == Blocks.OXEYE_DAISY ||
               block == Blocks.CORNFLOWER ||
               block == Blocks.LILY_OF_THE_VALLEY ||
               block == Blocks.SUNFLOWER ||
               block == Blocks.LILAC ||
               block == Blocks.ROSE_BUSH ||
               block == Blocks.PEONY;
    }

    /**
     * 检查方块上方是否有覆盖方块用于保护建筑
     */
    protected boolean hasCoveringAbove(ServerLevel serverLevel, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = serverLevel.getBlockState(above);

        // 如果上方是空气方块则认为是没被覆盖的
        return !aboveState.isAir();
    }

    /**
     * 每tick执行工作核心逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_terra_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 防止重复记录第一次tick日志，仅用于调试加载问题
        if (!blockEntity.hasLoggedFirstTick) {
            blockEntity.hasLoggedFirstTick = true;
            LOGGER.info("[TerraELC] First tick at {} - Current lastPos: ({},{}), failed: {}", 
                pos, blockEntity.lastX, blockEntity.lastZ, blockEntity.failedAttempts);
        }

        // 调用父类tick方法处理升级和充放电等逻辑
        mio_icif_producer.tick(level, pos, state, blockEntity);

        // 同步方块状态
        
        boolean shouldBeLit = blockEntity.isWorking && blockEntity.hasTemplate();
        if (state.getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_terra_elc.LIT) != shouldBeLit) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_terra_elc.LIT, shouldBeLit), 3);
        }
    }

    /**
     * 插入模板
     * @param stack 模板物品
     * @return 是否成功插入
     */
    public boolean insertTemplate(ItemStack stack) {
        if (hasTemplate()) {
            return false;
        }
        if (!isItemValidForSlot(TEMPLATE_SLOT, stack)) {
            return false;
        }
        itemHandler.setStackInSlot(TEMPLATE_SLOT, stack.copyWithCount(1));
        setChanged();
        return true;
    }

    /**
     * 取出模板
     * @return 模板物品，没有模板则返回空
     */
    public ItemStack extractTemplate() {
        ItemStack template = getTemplate();
        if (template.isEmpty()) {
            return ItemStack.EMPTY;
        }
        itemHandler.setStackInSlot(TEMPLATE_SLOT, ItemStack.EMPTY);
        
        // 重置工作状态
        resetWorkState();
        
        setChanged();
        return template;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("last_x", lastX);
        tag.putInt("last_z", lastZ);
        tag.putInt("failed_attempts", failedAttempts);
        tag.putInt("processed_blocks", processedBlocks);
        tag.putInt("converted_blocks", convertedBlocks);
        tag.putInt("tick_counter", tickCounter);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        // 记录加载前的状态
        int oldLastX = lastX;
        int oldLastZ = lastZ;
        
        if (tag.contains("last_x")) {
            lastX = tag.getInt("last_x");
        }
        if (tag.contains("last_z")) {
            lastZ = tag.getInt("last_z");
        }
        if (tag.contains("failed_attempts")) {
            failedAttempts = tag.getInt("failed_attempts");
        }
        if (tag.contains("processed_blocks")) {
            processedBlocks = tag.getInt("processed_blocks");
        }
        if (tag.contains("converted_blocks")) {
            convertedBlocks = tag.getInt("converted_blocks");
        }
        if (tag.contains("tick_counter")) {
            tickCounter = tag.getInt("tick_counter");
        }
        // 注意：pendingChanges不会保存在NBT中，加载后会重新开始
        // 这是为了避免加载时的卡顿问题
        LOGGER.info("[TerraELC] Loaded from NBT - lastPos: ({},{}), failed: {} (was: {}, {})", 
            lastX, lastZ, failedAttempts, oldLastX, oldLastZ);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.terra_elc");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        // 地形转换机没有GUI
        return null;
    }

    /**
     * 方块被移除时掉落物品
     */
    public void dropContents(Level level, BlockPos pos) {
        ItemStack template = getTemplate();
        if (!template.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), template);
        }
        ItemStack battery = itemHandler.getStackInSlot(BATTERY_SLOT);
        if (!battery.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), battery);
        }
    }
}
package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_chamber;
import com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactor;
import com.singularity_iteration.mio_icif.Menu.Generator.FluidReactorMenu;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.IEUEnergyStorage;
import com.singularity_iteration.mio_icif.energy.grid.EnergyNetGlobal;
import com.singularity_iteration.mio_icif.energy.grid.IMetaDelegate;
import com.singularity_iteration.mio_icif.energy.grid.IEnergyTile;
import com.singularity_iteration.mio_icif.api.reactor.IReactorAPI.ReactorMode;
import com.singularity_iteration.mio_icif.api.reactor.IReactorController;
import com.singularity_iteration.mio_icif.energy.heat.HeatStorage;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.event.mio_icif_DamageTypes;
import com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager;
import com.singularity_iteration.mio_icif.util.RadiationProtectionUtil;
import com.singularity_iteration.mio_icif.multiblock.mio_icif_fluid_reactor_validator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import java.util.List;

/**
 * 核反应堆发电机方块实体类
 * 
 * 特点： * - 自身不直接发电，依靠内部放置的燃料棒进行发电
 * - 拥有18个槽位用于放置燃料棒、散热片等物品（6行 x 3列）
 * - 拥有热量存储系统，最大热量存储为10000HU
 * - 每个槽位只能接受一个物品 */
public class mio_icif_nuclear_reactor_generator extends mio_icif_Energy_Generator implements IMetaDelegate, IReactorController {

     // 槽位数量说明：54个槽位（6行 x 9列），但只有部分可用
    public static final int SLOT_COUNT = 54;
    public static final int BASE_COLUMNS = 3; // 基础可用列数
    public static final int MAX_COLUMNS = 9;  // 最大可用列数
public static final int ROWS = 6;         // 行数
    
    // 热量存储配置
    // IC2 hull heat is independent from the number of attached chambers. Chambers only add slots.
    public static final int HEAT_CAPACITY_BASE = 10000;
    public static final int HEAT_CAPACITY_PER_CHAMBER = 0;
    public static final int HEAT_MAX_RECEIVE = 0; // 核反应堆不从外部接收热量
    public static final int HEAT_MAX_EXTRACT = 1000; // 最大热量输出速率
    public static final int HEAT_BASE_TEMP = 20; // 基础温度 20°C
    public static final int HEAT_MAX_TEMP = 5000; // 最高温度 5000°C
    public static final float HEAT_LOSS_FACTOR = 0.0f; // 核反应堆没有热损失，热量会累积（积温效应）

    // 能量配置 - EV 级
public static final long ENERGY_GENERATION_RATE = 0L; // 核反应堆自身不直接发电，由燃料棒发电
    public static final long ENERGY_CAPACITY = 1000000L; // 1,000,000 EU 能量缓冲
    public static final long MAX_RECEIVE = 0L; // 发电机不接受外部能量输入
    public static final long MAX_EXTRACT = 8192L; // EV 级最大输出速率 8192 EU/tick
    
    // 热量存储
    protected final HeatStorage heatStorage;
    
    // IMetaDelegate 子方块列表（包含自身和连接的核反应仓）
private final List<IEnergyTile> subTiles = new java.util.ArrayList<>();
    
    // 当前总热量产生速率（由燃料棒产生）
    private int currentHeatGeneration = 0;
    
    // 当前总能量产生速率（由燃料棒产生）
    private int currentEnergyGeneration = 0;

    // IC2 evaluates reactor components once per second, then offers the resulting EU/t for the cycle.
    private int reactorCycleTicks = 19;
    
    // 流体模式下散热片散发的总热量（用于转换冷却液）
    private int ventDissipatedHeatForFluid = 0;
    
    // 反应堆是否运行
private boolean isRunning = false;

    // 反应堆当前温度
private int currentTemperature = HEAT_BASE_TEMP;

    // 当前可用的列数（基础 3 列，每连接一个核反应仓 +1 列，最多 9 列）
    private int availableColumns = BASE_COLUMNS;

    // 当前实际可用的槽位数量
private int currentSlotCount = BASE_COLUMNS * ROWS; // 基础 18 个槽位（6 行 x 3 列）

    // ==================== 流体反应堆模式 ====================
    // 当前反应堆模式
private mio_icif_reactor_mode reactorMode = mio_icif_reactor_mode.GENERATOR;

    // 流体反应堆处理器（仅在流体模式下使用）
private final mio_icif_fluid_reactor_handler fluidHandler;

    // 是否为有效的流体反应堆结构
private boolean isValidFluidReactorStructure = false;

    // 数据同步容器（用于流体反应堆 GUI 显示 5 个条目）
    private final ContainerData containerData;

    // 数据同步容器（用于发电模式 GUI 显示 6 个条目）
    private final ContainerData generatorContainerData;

    // ==================== 多方块结构管理器 ====================
    // 流体反应堆多方块结构管理器
private mio_icif_multiblock_manager<mio_icif_fluid_reactor_validator> fluidReactorMultiblock;

    /**
     * 构造函数（用于 BlockEntityType.Builder）
 */
    public mio_icif_nuclear_reactor_generator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    /**
     * 构造函数
 */
    public mio_icif_nuclear_reactor_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state,
              type != null ? type : mio_icif_block_entities.NUCLEAR_REACTOR_GENERATOR_ENTITY_TYPE.get(),
              SlotLayout.builder().reactor(SLOT_COUNT).build(), ENERGY_GENERATION_RATE,
              ENERGY_CAPACITY, MAX_RECEIVE, MAX_EXTRACT, CableTier.EV);

        // 初始化热量存储（使用基础热量上限，后续会根据核反应仓数量动态调整）
        this.heatStorage = new HeatStorage(HEAT_CAPACITY_BASE, HEAT_MAX_RECEIVE, HEAT_MAX_EXTRACT,
                                           HEAT_BASE_TEMP, HEAT_MAX_TEMP, HEAT_LOSS_FACTOR);

        // 初始�?IMetaDelegate 子方块列表（至少包含自身�
    this.subTiles.add(this);

        // 初始化流体反应堆处理�
    this.fluidHandler = new mio_icif_fluid_reactor_handler();

        // 初始化数据同步容器（用于流体反应�?GUI 显示�?个条目）
        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> (int) getCurrentHeat();
                    case 1 -> (int) getMaxHeat();
                    case 2 -> (int) getCurrentTemperature();
                    case 3 -> getInputFluidAmount();
                    case 4 -> getOutputFluidAmount();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 5;
            }
        };

        // 初始化数据同步容器（用于发电模式 GUI 显示 6 个条目）
        // 0=energy, 1=maxEnergy, 2=heat, 3=maxHeat, 4=outputPower, 5=availableColumns
        this.generatorContainerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> (int) getEnergyStorage().getAmount();
                    case 1 -> (int) getEnergyStorage().getCapacity();
                    case 2 -> (int) Math.min(getHeatStorage().getHeatStored(), Integer.MAX_VALUE);
                    case 3 -> (int) Math.min(getHeatStorage().getMaxHeatStored(), Integer.MAX_VALUE);
                    case 4 -> getCurrentOutput();
                    case 5 -> getAvailableColumns();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 6;
            }
        };
    }

    /**
     * 每 tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_nuclear_reactor_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 检查是否达到热量上限，如果达到则触发爆炸
    if (blockEntity.checkAndTriggerExplosion()) {
            return; // 爆炸后不再执行后续逻辑
        }

        // 更新可用的核反应仓数量
    blockEntity.updateReactorChambers(level, pos);

        // First process the reactor cycle and continuously buffer its EU/t output.
        blockEntity.processReactorLogic();

        // 如果是流体模式，处理流体加热逻辑
        if (blockEntity.reactorMode == mio_icif_reactor_mode.FLUID && blockEntity.isValidFluidReactorStructure) {
            blockEntity.processFluidHeating();
        }

        // IC2 checks for meltdown after all components and fluid cooling have settled this cycle.
        if (blockEntity.checkAndTriggerExplosion()) {
            return;
        }

        // 调用父类 tick 方法（处理能量分配等）
    // 注意：父类的 tick 会调用 distributeEnergy() 来输出能量
    // 流体模式下不发电，跳过能量分配
    if (blockEntity.reactorMode == mio_icif_reactor_mode.GENERATOR) {
            mio_icif_Energy_Generator.tick(level, pos, state, blockEntity);
        }

        // 应用热损失
    blockEntity.applyHeatLoss();

        // 向相邻方块传导热量
    blockEntity.distributeHeat();

        // 更新温度
        blockEntity.updateTemperature();

        // 处理堆温效果（着火、蒸发、伤害、岩浆等）
    blockEntity.processHeatEffects();

        // 同步运行状态到方块状态（用于模型切换）
    boolean currentActive = state.getValue(com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Nuclear_Reactor_Generator.ACTIVE);
        if (currentActive != blockEntity.isRunning) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Nuclear_Reactor_Generator.ACTIVE, blockEntity.isRunning), 3);
        }

        // 标记方块实体已更新
    blockEntity.setChanged();
    }
    
    /**
     * 处理流体加热逻辑
     * 根据 IC2 百科：流体反应堆通过散热片将燃料棒发出的热量转移至冷却液
 * 只有散热片实际散发的热量才用来转换冷却液，而不是堆温
 * 1 HU 热量转换 1 mB 冷却液为 1 mB 热冷却液
     */
    private void processFluidHeating() {
        if (fluidHandler != null && ventDissipatedHeatForFluid > 0) {
            int emittedHeat = ventDissipatedHeatForFluid;
            int emittedHu = emittedHeat * 40;
            int convertedHu = fluidHandler.tick(emittedHu);
            ventDissipatedHeatForFluid = 0;

            int unconvertedHeat = (emittedHu - convertedHu) / 40;
            addReactorHeat(unconvertedHeat);
        }
    }
    
    /**
     * 设置多方块结构管理器
     * 由事件驱动系统调用
 */
    public void setFluidReactorMultiblock(mio_icif_multiblock_manager<mio_icif_fluid_reactor_validator> multiblock) {
        this.fluidReactorMultiblock = multiblock;
        boolean wasValid = this.isValidFluidReactorStructure;
        this.isValidFluidReactorStructure = (multiblock != null && multiblock.isValid());
        
        // 如果结构变为有效，自动切换到流体模式
        if (this.isValidFluidReactorStructure && !wasValid) {
            this.reactorMode = mio_icif_reactor_mode.FLUID;
            setChanged();
        }
    }

    /**
     * 当结构被破坏时调用
 */
    public void onMultiblockBroken() {
        this.fluidReactorMultiblock = null;
        this.isValidFluidReactorStructure = false;
        if (reactorMode == mio_icif_reactor_mode.FLUID) {
            reactorMode = mio_icif_reactor_mode.GENERATOR;
        }
    }

    /**
     * 获取多方块结构管理器
     */
    public mio_icif_multiblock_manager<mio_icif_fluid_reactor_validator> getFluidReactorMultiblock() {
        return fluidReactorMultiblock;
    }

    /**
     * 检查热量是否达到上限，如果达到则触发爆炸
 * @return true=发生了爆炸
 */
    private boolean checkAndTriggerExplosion() {
        if (heatStorage.getHeatStored() >= heatStorage.getMaxHeatStored()) {
            // 热量达到上限，触发爆炸
        triggerNuclearExplosion();
            return true;
        }
        return false;
    }

    /**
     * 触发核爆炸
 * 爆炸威力根据燃料棒数量动态决定，燃料棒越多爆炸范围越大
 * 公式：半径 = 8 + 燃料棒数量 × 4（每增加1个燃料棒增加4格半径）
     * 示例：0根=12, 5根=28, 10根=48, 20根=88...
     */
    private void triggerNuclearExplosion() {
        if (level == null || level.isClientSide()) return;

        // 检查是否启用核爆炸
        if (!com.singularity_iteration.mio_icif.Singularity_Iteration_Config.ENABLE_NUCLEAR_EXPLOSION.get()) {
            // 禁用核爆炸：仅产生普通机器爆炸效�
        level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 1.0F, Level.ExplosionInteraction.BLOCK);
            level.setBlock(worldPosition, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            return;
        }

        // 计算燃料棒数量
    int fuelRodCount = countFuelRods();

        // 计算隔板降低的爆炸范围百分比
        int explosionReduction = calculateExplosionReductionFromPlating();

        // 计算爆炸半径（基于燃料棒数量动态增长，无上限）
        // 基础半径8格，每个燃料棒增加 4 格半径
    int baseRadius = 8;
        int radiusPerRod = 4;
        int radius = baseRadius + (fuelRodCount * radiusPerRod);
        radius = Math.max(baseRadius, radius); // 至少基础半径

        // 应用隔板的爆炸范围降低效果
    // 如果降低100%，则完全不爆炸
    if (explosionReduction >= 100) {
            // 隔板完全阻止了爆炸，只清除反应堆方块
            level.removeBlock(worldPosition, false);
            return;
        }

        // 根据降低百分比减少爆炸半径
    radius = (int)(radius * (100 - explosionReduction) / 100.0);
        radius = Math.max(1, radius); // 至少保留1格半径
        // Y轴半径（椭球形，高度比半径低）
    int radiusY = (int)(radius * 0.7); // Y轴为半径的 70%



        // 爆炸中心
        double centerX = worldPosition.getX() + 0.5;
        double centerY = worldPosition.getY() + 0.5;
        double centerZ = worldPosition.getZ() + 0.5;

        // 保存爆炸参数，用于延迟执行
    final int finalRadius = radius;
        final int finalRadiusY = radiusY;
        final double finalCenterX = centerX;
        final double finalCenterY = centerY;
        final double finalCenterZ = centerZ;

        // 立即移除反应堆方块（防止继续产生热量）
    level.setBlock(worldPosition, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);

        // 发送蘑菇云动画包到客户端（客户端渲染，不受服务端卡顿影响）
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.singularity_iteration.mio_icif.network.mio_icif_Network.sendNuclearExplosionAnimation(
                serverLevel, centerX, centerY, centerZ, radius);
        }

        // 延迟40 tick（2秒）执行实际爆炸，此时蘑菇云动画已经在客户端播放
        // 这样可以确保玩家看到完整的动画效果
    if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            scheduleDelayedExplosion(serverLevel, finalCenterX, finalCenterY, finalCenterZ, 
                finalRadius, finalRadiusY, 40);
        }
    }

    /**
     * 调度延迟爆炸
     * 在指定 tick 后执行实际爆炸处理
 */
    private void scheduleDelayedExplosion(net.minecraft.server.level.ServerLevel serverLevel,
                                           double centerX, double centerY, double centerZ,
                                           int radius, int radiusY, int delayTicks) {
        // 获取当前游戏时间
        final long targetTime = serverLevel.getGameTime() + delayTicks;
        
        // 注册一个 tick 监听器来检查时间
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
                if (serverLevel.getGameTime() >= targetTime) {
                    // 时间到了，执行爆炸
                executeActualExplosion(serverLevel, centerX, centerY, centerZ, radius, radiusY);
                    // 注销监听器
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(this);
                }
            }
        });
    }

    /**
     * 执行实际爆炸处理
     * 在蘑菇云动画播放后执行
 * 使用 Bresenham 3D + Octree 优化算法
     */
    private void executeActualExplosion(net.minecraft.server.level.ServerLevel serverLevel,
                                         double centerX, double centerY, double centerZ,
                                         int radius, int radiusY) {
        Level level = serverLevel;

        // 核爆后第一时间清除辐射范围内的物品实体，避免大量掉落物造成卡顿
        int itemClearRadius = (int)(radius * 1.5);
        int itemClearRadiusY = (int)(radiusY * 1.5);
        var itemEntities = level.getEntitiesOfClass(
            net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(
                centerX - itemClearRadius, centerY - itemClearRadiusY, centerZ - itemClearRadius,
                centerX + itemClearRadius, centerY + itemClearRadiusY, centerZ + itemClearRadius
            )
        );
        // 直接移除所有物品实体，不产生任何掉落物
        for (var itemEntity : itemEntities) {
            itemEntity.discard();
        }

        // 获取爆炸范围内的所有实体
    var entities = level.getEntities(null,
            new net.minecraft.world.phys.AABB(centerX - radius, centerY - radiusY, centerZ - radius,
                                               centerX + radius, centerY + radiusY, centerZ + radius));

        // 创建爆炸中心点
    Vec3 center = new Vec3(centerX, centerY, centerZ);

        // 计算爆炸威力（基于燃料棒数量）
    float explosionPower = radius * 3.0f;

        // 使用 Wave Propagation（波动传播）算法 - 产生不规则的撕裂状爆炸坑
        com.singularity_iteration.mio_icif.Blocks.entity.reactor.ReactorExplosionTaskWave task =
            new com.singularity_iteration.mio_icif.Blocks.entity.reactor.ReactorExplosionTaskWave(
                serverLevel, center, explosionPower, radius, radiusY, entities);

        // 启动爆炸任务调度器
    com.singularity_iteration.mio_icif.Blocks.entity.reactor.ReactorExplosionSchedulerWave.startExplosion(
            serverLevel, new BlockPos((int) centerX, (int) centerY, (int) centerZ), task);

        // 静默爆炸 - 不播放音效以避免音量池警告
    // 核反应堆爆炸是静默的，与核弹不同
    }

    /**
     * 清除指定位置的方块及其上方的所有植物、铁轨等可能产生掉落物的方块
     * 从顶部开始向下清除，避免产生掉落物
 * @param level 世界
     * @param pos 起始位置
     */
    @SuppressWarnings("unused")
    private void clearBlockAndPlantsAbove(Level level, BlockPos pos) {
        // 先向上查找并清除所有植物、铁轨等依附性方块
    BlockPos abovePos = pos.above();
        int maxHeight = level.getMaxBuildHeight();
        int checkedHeight = 0;

        // 向上扫描最多32格，清除所有依附性方块
    while (checkedHeight < 32 && abovePos.getY() < maxHeight) {
            BlockState aboveState = level.getBlockState(abovePos);
            Block aboveBlock = aboveState.getBlock();

            // 检查是否是植物、铁轨、红石线等需要依附的方块
            if (isAttachableBlock(aboveBlock)) {
                // 先移除方块实体（防止方块实体残留警告�
            level.removeBlockEntity(abovePos);
                // 直接设为空气，不产生掉落�?(flags=10 包含 SUPPRESS_DROPS)
                level.setBlock(abovePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 10);
                abovePos = abovePos.above();
                checkedHeight++;
            } else {
                // 遇到非依附性方块，停止向上扫描
                break;
            }
        }

        // 最后清除原始位置的方块
        level.removeBlockEntity(pos);
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 10);
    }

    /**
     * 检查方块是否是依附性方块（植物、铁轨、红石等）
 * @param block 要检查的方块
     * @return 如果是依附性方块返回 true
     */
    private boolean isAttachableBlock(Block block) {
        // 草、花、种子等植物
        if (block instanceof net.minecraft.world.level.block.BushBlock) return true;
        // 铁轨
        if (block instanceof net.minecraft.world.level.block.BaseRailBlock) return true;
        return false;
    }

    /**
     * 优化的核爆炸处理（单线程，避免死锁）
     * 使用分层处理和稀疏采样
 */
    @SuppressWarnings("unused")
    private void processExplosionOptimized(Level level, double centerX, double centerY, double centerZ,
            int radius, int radiusY, int dirtRadius, int dirtRadiusY,
            int stoneRadius, int stoneRadiusY, int deepslateRadius, int deepslateRadiusY,
            int oreRadius, int oreRadiusY, java.util.List<BlockPos> radiationBlockPositions) {

        // 预计算平方值
    double radiusSq = radius * radius;
        double radiusYSq = radiusY * radiusY;
        double dirtRadiusSq = dirtRadius * dirtRadius;
        double dirtRadiusYSq = dirtRadiusY * dirtRadiusY;
        double stoneRadiusSq = stoneRadius * stoneRadius;
        double stoneRadiusYSq = stoneRadiusY * stoneRadiusY;
        double deepslateRadiusSq = deepslateRadius * deepslateRadius;
        double deepslateRadiusYSq = deepslateRadiusY * deepslateRadiusY;
        double oreRadiusSq = oreRadius * oreRadius;
        double oreRadiusYSq = oreRadiusY * oreRadiusY;

        // ========== 使用轴对称优化 + 单次遍历：只处理第一象限(x>=0, z>=0)，然后镜像到其他三个象限 ==========
        // 只遍历一次最大的范围（dirtRadius），同时处理所有逻辑

        processExplosionQuadrantSinglePass(level, centerX, centerY, centerZ,
            radius, radiusY, radiusSq, radiusYSq,
            oreRadius, oreRadiusY, oreRadiusSq, oreRadiusYSq,
            deepslateRadius, deepslateRadiusY, deepslateRadiusSq, deepslateRadiusYSq,
            stoneRadius, stoneRadiusY, stoneRadiusSq, stoneRadiusYSq,
            dirtRadius, dirtRadiusY, dirtRadiusSq, dirtRadiusYSq,
            radiationBlockPositions);
    }

    /**
     * 处理爆炸象限 - 只处理第一象限(x>=0, z>=0)，然后镜像到其他三个象限
     * 利用椭圆关于Y轴的对称性，减少75%的计算量
     */
    @SuppressWarnings("unused")
    private void processExplosionQuadrant(Level level, double centerX, double centerY, double centerZ,
            int radius, int radiusY, double radiusSq, double radiusYSq,
            java.util.function.BiConsumer<Level, BlockPos> blockProcessor) {

        // 只遍历第一象限 (x >= 0, z >= 0)
        for (int x = 0; x <= radius; x++) {
            double xSq = (double)(x * x);
            if (xSq > radiusSq) continue;

            for (int y = -radiusY; y <= radiusY; y++) {
                double ySq = (double)(y * y);
                if (xSq / radiusSq + ySq / radiusYSq > 1.0) continue;

                for (int z = 0; z <= radius; z++) {
                    double zSq = (double)(z * z);
                    double distSq = xSq / radiusSq + ySq / radiusYSq + zSq / radiusSq;

                    if (distSq <= 1.0) {
                        // 处理四个象限的镜像位置（以核反应堆为中心）
                    // 第一象限 (X正, Z正): 东南方向
                        blockProcessor.accept(level, BlockPos.containing(centerX + x, centerY + y, centerZ + z));
                        // 第二象限 (X负, Z正): 西南方向 - x>0时镜像
                    if (x > 0) {
                            blockProcessor.accept(level, BlockPos.containing(centerX - x, centerY + y, centerZ + z));
                        }
                        // 第三象限 (X负, Z负): 西北方向 - x>0且z>0时镜像
                    if (x > 0 && z > 0) {
                            blockProcessor.accept(level, BlockPos.containing(centerX - x, centerY + y, centerZ - z));
                        }
                        // 第四象限 (X正, Z负): 东北方向 - z>0时镜像
                    if (z > 0) {
                            blockProcessor.accept(level, BlockPos.containing(centerX + x, centerY + y, centerZ - z));
                        }
                    }
                }
            }
        }
    }

    /**
     * 单次遍历处理所有爆炸逻辑（轴对称优化）
 * 一次遍历同时处理爆炸清除、矿石、深板岩、石头、泥土
 */
    private void processExplosionQuadrantSinglePass(Level level, double centerX, double centerY, double centerZ,
            int radius, int radiusY, double radiusSq, double radiusYSq,
            int oreRadius, int oreRadiusY, double oreRadiusSq, double oreRadiusYSq,
            int deepslateRadius, int deepslateRadiusY, double deepslateRadiusSq, double deepslateRadiusYSq,
            int stoneRadius, int stoneRadiusY, double stoneRadiusSq, double stoneRadiusYSq,
            int dirtRadius, int dirtRadiusY, double dirtRadiusSq, double dirtRadiusYSq,
            java.util.List<BlockPos> radiationBlockPositions) {

        // 只遍历第一象限 (x >= 0, z >= 0)
        for (int x = 0; x <= dirtRadius; x++) {
            double xSq = (double)(x * x);
            if (xSq > dirtRadiusSq) continue;

            for (int y = -dirtRadiusY; y <= dirtRadiusY; y++) {
                double ySq = (double)(y * y);
                if (xSq / dirtRadiusSq + ySq / dirtRadiusYSq > 1.0) continue;

                for (int z = 0; z <= dirtRadius; z++) {
                    double zSq = (double)(z * z);
                    double distSqDirt = xSq / dirtRadiusSq + ySq / dirtRadiusYSq + zSq / dirtRadiusSq;

                    if (distSqDirt > 1.0) continue;

                    // 四个象限的坐标
                BlockPos centerPos = BlockPos.containing(centerX, centerY, centerZ);
                    BlockPos pos1 = BlockPos.containing(centerX + x, centerY + y, centerZ + z);
                    BlockPos pos2 = x > 0 ? BlockPos.containing(centerX - x, centerY + y, centerZ + z) : pos1;
                    BlockPos pos3 = (x > 0 && z > 0) ? BlockPos.containing(centerX - x, centerY + y, centerZ - z) : pos1;
                    BlockPos pos4 = z > 0 ? BlockPos.containing(centerX + x, centerY + y, centerZ - z) : pos1;

                    // 处理四个位置
                    processSingleBlock(level, pos1, x, y, z, xSq, ySq, zSq,
                        radiusSq, radiusYSq, oreRadiusSq, oreRadiusYSq,
                        deepslateRadiusSq, deepslateRadiusYSq, stoneRadiusSq, stoneRadiusYSq,
                        dirtRadiusSq, dirtRadiusYSq, dirtRadius, radius, oreRadius, deepslateRadius, stoneRadius,
                        radiationBlockPositions, centerPos);

                    if (x > 0) {
                        processSingleBlock(level, pos2, -x, y, z, xSq, ySq, zSq,
                            radiusSq, radiusYSq, oreRadiusSq, oreRadiusYSq,
                            deepslateRadiusSq, deepslateRadiusYSq, stoneRadiusSq, stoneRadiusYSq,
                            dirtRadiusSq, dirtRadiusYSq, dirtRadius, radius, oreRadius, deepslateRadius, stoneRadius,
                            radiationBlockPositions, centerPos);
                    }

                    if (x > 0 && z > 0) {
                        processSingleBlock(level, pos3, -x, y, -z, xSq, ySq, zSq,
                            radiusSq, radiusYSq, oreRadiusSq, oreRadiusYSq,
                            deepslateRadiusSq, deepslateRadiusYSq, stoneRadiusSq, stoneRadiusYSq,
                            dirtRadiusSq, dirtRadiusYSq, dirtRadius, radius, oreRadius, deepslateRadius, stoneRadius,
                            radiationBlockPositions, centerPos);
                    }

                    if (z > 0) {
                        processSingleBlock(level, pos4, x, y, -z, xSq, ySq, zSq,
                            radiusSq, radiusYSq, oreRadiusSq, oreRadiusYSq,
                            deepslateRadiusSq, deepslateRadiusYSq, stoneRadiusSq, stoneRadiusYSq,
                            dirtRadiusSq, dirtRadiusYSq, dirtRadius, radius, oreRadius, deepslateRadius, stoneRadius,
                            radiationBlockPositions, centerPos);
                    }
                }
            }
        }
    }

    /**
     * 处理单个方块的所有逻辑
     */
    private void processSingleBlock(Level level, BlockPos pos, int x, int y, int z,
            double xSq, double ySq, double zSq,
            double radiusSq, double radiusYSq, double oreRadiusSq, double oreRadiusYSq,
            double deepslateRadiusSq, double deepslateRadiusYSq, double stoneRadiusSq, double stoneRadiusYSq,
            double dirtRadiusSq, double dirtRadiusYSq, int dirtRadius,
            int radius, int oreRadius, int deepslateRadius, int stoneRadius,
            java.util.List<BlockPos> radiationBlockPositions, BlockPos centerPos) {

        // 检查爆炸波是否被阻挡（从爆炸中心到目标方块的路径上是否有阻挡方块）
        if (isExplosionBlocked(level, centerPos, pos)) {
            return; // 爆炸被阻挡，不处理这个方块
    }

        BlockState state = level.getBlockState(pos);

        // 1. 检查是否在爆炸核心范围内 - 清除非白名单方块
        if (xSq <= radiusSq && ySq <= radiusYSq && zSq <= radiusSq) {
            double distSq = xSq / radiusSq + ySq / radiusYSq + zSq / radiusSq;
            if (distSq <= 1.0) {
                // 白名单检查
            if (!state.is(net.minecraft.world.level.block.Blocks.STONE)
                    && !state.is(net.minecraft.world.level.block.Blocks.DEEPSLATE)
                    && !state.is(net.minecraft.world.level.block.Blocks.GRANITE)
                    && !state.is(net.minecraft.world.level.block.Blocks.DIORITE)
                    && !state.is(net.minecraft.world.level.block.Blocks.ANDESITE)
                    && !state.is(net.minecraft.world.level.block.Blocks.TUFF)
                    && !state.is(net.minecraft.world.level.block.Blocks.CALCITE)
                    && !state.is(net.minecraft.world.level.block.Blocks.DRIPSTONE_BLOCK)
                    && !state.is(net.minecraft.world.level.block.Blocks.BEDROCK)
                    && !state.is(net.minecraft.world.level.block.Blocks.OBSIDIAN)
                    && !state.is(net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN)
                    && !state.is(net.minecraft.world.level.block.Blocks.NETHERITE_BLOCK)
                    && !state.is(net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS)
                    && !state.is(net.minecraft.world.level.block.Blocks.REINFORCED_DEEPSLATE)
                    && !state.is(net.neoforged.neoforge.common.Tags.Blocks.ORES)
                    ) {
                    // 先移除方块实体（防止方块实体残留警告）
                level.removeBlockEntity(pos);
                    // flags = 2 | 8 = 10 即是SUPPRESS_DROPS，防止产生掉落物
                    level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 10);
                    return;
                }
            }
        }

        // 2. 矿石和沙砾清除
    if (xSq <= oreRadiusSq && ySq <= oreRadiusYSq && zSq <= oreRadiusSq) {
            double distSq = xSq / oreRadiusSq + ySq / oreRadiusYSq + zSq / oreRadiusSq;
            if (distSq <= 1.0) {
                if (state.is(net.neoforged.neoforge.common.Tags.Blocks.ORES)
                    || state.is(net.minecraft.world.level.block.Blocks.GRAVEL)) {
                    level.removeBlockEntity(pos);
                    level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 10);
                    return;
                }
            }
        }

        // 3. 辐射深板岩
    if (xSq <= deepslateRadiusSq && ySq <= deepslateRadiusYSq && zSq <= deepslateRadiusSq) {
            double distSq = xSq / deepslateRadiusSq + ySq / deepslateRadiusYSq + zSq / deepslateRadiusSq;
            if (distSq <= 1.0) {
                if (state.is(net.minecraft.world.level.block.Blocks.DEEPSLATE)) {
                    level.setBlock(pos, mio_icif_blocks.BLOCK_RADIATING_DEEPSLATE.get().defaultBlockState(), 3);
                    radiationBlockPositions.add(pos.immutable());
                    return;
                }
            }
        }

        // 4. 辐射石头
        if (xSq <= stoneRadiusSq && ySq <= stoneRadiusYSq && zSq <= stoneRadiusSq) {
            double distSq = xSq / stoneRadiusSq + ySq / stoneRadiusYSq + zSq / stoneRadiusSq;
            if (distSq <= 1.0) {
                if (state.is(net.minecraft.world.level.block.Blocks.STONE)) {
                    level.setBlock(pos, mio_icif_blocks.BLOCK_RADIATING_STONE.get().defaultBlockState(), 3);
                    radiationBlockPositions.add(pos.immutable());
                    return;
                }
            }
        }

        // 5. 辐射泥土
        if (xSq <= dirtRadiusSq && ySq <= dirtRadiusYSq && zSq <= dirtRadiusSq) {
            double distSq = xSq / dirtRadiusSq + ySq / dirtRadiusYSq + zSq / dirtRadiusSq;
            if (distSq <= 1.0) {
                if (state.is(net.minecraft.world.level.block.Blocks.DIRT)
                        || state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                        || state.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT)
                        || state.is(net.minecraft.world.level.block.Blocks.SAND)) {
                    level.setBlock(pos, mio_icif_blocks.BLOCK_RADIATING_DIRT.get().defaultBlockState(), 3);
                    radiationBlockPositions.add(pos.immutable());
                }
            }
        }

        // 6. 辐射范围内清除树叶方块（在辐射泥土范围内）
    if (xSq <= dirtRadiusSq && ySq <= dirtRadiusYSq && zSq <= dirtRadiusSq) {
            double distSq = xSq / dirtRadiusSq + ySq / dirtRadiusYSq + zSq / dirtRadiusSq;
            if (distSq <= 1.0) {
                if (state.is(net.minecraft.tags.BlockTags.LEAVES)) {
                    // 先移除方块实体（防止方块实体残留警告）
                level.removeBlockEntity(pos);
                    // flags = 2 | 8 = 10 即是SUPPRESS_DROPS，防止产生掉落物
                    level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 10);
                    return;
                }
            }
        }
        
        // 7. 辐射范围内清除雪（在辐射泥土范围内）
        if (xSq <= dirtRadiusSq && ySq <= dirtRadiusYSq && zSq <= dirtRadiusSq) {
            double distSq = xSq / dirtRadiusSq + ySq / dirtRadiusYSq + zSq / dirtRadiusSq;
            if (distSq <= 1.0) {
                // 清除各种雪方块
            if (state.is(net.minecraft.world.level.block.Blocks.SNOW)
                        || state.is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK)
                        || state.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)) {
                    // 直接移除，不产生掉落物
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    return;
                }
            }
        }
    }

    /**
     * 计算反应堆中燃料棒的数量
     * 双联燃料棒算2根，四联燃料棒算4根
 * @return 燃料棒数量
 */
    private int countFuelRods() {
        int count = 0;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_nuclear_reactor fuelRod) {
                // 根据燃料棒类型计算数量：双联=2，四联=4，单联=1
                count += fuelRod.getRodType().getEfficiencyMultiplier();
            }
        }
        return count;
    }

    /**
     * 检查爆炸波是否被阻挡
 * 从爆炸中心到目标方块的路径上检查是否有阻挡方块（如基岩、黑曜石等）
     * @param level 世界
     * @param centerPos 爆炸中心位置
     * @param targetPos 目标方块位置
     * @return true=爆炸被阻挡，false=爆炸可以到达
     */
    private boolean isExplosionBlocked(Level level, BlockPos centerPos, BlockPos targetPos) {
        // 如果目标就是中心，不阻挡
        if (centerPos.equals(targetPos)) {
            return false;
        }

        // 使用Bresenham算法或简单的线性插值检查路径上的方块
    int x0 = centerPos.getX();
        int y0 = centerPos.getY();
        int z0 = centerPos.getZ();
        int x1 = targetPos.getX();
        int y1 = targetPos.getY();
        int z1 = targetPos.getZ();

        // 计算距离
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int dz = Math.abs(z1 - z0);

        int maxDist = Math.max(dx, Math.max(dy, dz));

        // 如果距离为0，不阻挡
        if (maxDist == 0) {
            return false;
        }

        // 沿路径检查每个方块
    for (int i = 1; i < maxDist; i++) {
            double t = (double) i / maxDist;
            int checkX = (int) Math.round(x0 + (x1 - x0) * t);
            int checkY = (int) Math.round(y0 + (y1 - y0) * t);
            int checkZ = (int) Math.round(z0 + (z1 - z0) * t);

            BlockPos checkPos = new BlockPos(checkX, checkY, checkZ);

            // 如果检查的位置就是目标位置，停止检查
        if (checkPos.equals(targetPos)) {
                break;
            }

            BlockState state = level.getBlockState(checkPos);

            // 检查是否是阻挡爆炸的方块
        if (isExplosionResistantBlock(state)) {
                return true; // 爆炸被阻挡
        }
        }

        return false; // 爆炸可以到达目标
    }

    /**
     * 检查方块是否能抵抗核爆炸（阻挡爆炸波）
     * @param state 方块状态
 * @return true=能阻挡爆炸
 */
    private boolean isExplosionResistantBlock(BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.BEDROCK)
            || state.is(net.minecraft.world.level.block.Blocks.OBSIDIAN)
            || state.is(net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN)
            || state.is(net.minecraft.world.level.block.Blocks.NETHERITE_BLOCK)
            || state.is(net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS)
            || state.is(net.minecraft.world.level.block.Blocks.REINFORCED_DEEPSLATE)
            || state.is(net.minecraft.world.level.block.Blocks.BARRIER);
    }

    /**
     * 检查是否有红石信号
     * 在流体模式下，检查红石接口方块的红石信号
     * 在发电模式下，检查自身位置的红石信号
     */
    private boolean hasRedstoneSignal() {
        if (level == null) return false;

        // 流体模式下，检查红石接口方块
    if (reactorMode == mio_icif_reactor_mode.FLUID && isValidFluidReactorStructure) {
            return hasRedstoneSignalFromPort();
        }

        // 发电模式下，检查自身位置
    return level.hasNeighborSignal(worldPosition);
    }

    /**
     * 检查红石接口方块是否有红石信号
     * 在流体反应堆多方块结构中查找红石接口方块
     */
    private boolean hasRedstoneSignalFromPort() {
        if (level == null || fluidReactorMultiblock == null) return false;

        // 获取多方块结构中的所有红石接口方块
    java.util.Set<BlockPos> redstonePorts = fluidReactorMultiblock.getRedstonePorts();

        // 检查任意一个红石接口方块是否有红石信号
        for (BlockPos portPos : redstonePorts) {
            if (level.hasNeighborSignal(portPos)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 处理反应堆核心逻辑
     * 参考IC2两遍扫描机制
 * - 第一遍（heatRun=true）：计算热量产生和分配，不发电
 * - 第二遍（heatRun=false）：计算发电，处理燃料棒耐久消耗
 * 只有在接受红石信号时才会发电
     */
    private void processReactorLogic() {
        boolean hasRedstone = hasRedstoneSignal();

        if (!hasRedstone) {
            clearReactorEnergyBuffer();
            currentEnergyGeneration = 0;
        }

        // The storage only bridges this tick to the project's EnergyNet. IC2 reactor output
        // is not a persistent battery, so unaccepted EU must not survive into another tick.
        if (hasRedstone) {
            clearReactorEnergyBuffer();
        }

        if (++reactorCycleTicks >= 20) {
            reactorCycleTicks = 0;
            currentHeatGeneration = 0;
            if (!hasRedstone) {
                currentEnergyGeneration = 0;
            } else {
                currentEnergyGeneration = 0;
            }
            ventDissipatedHeatForFluid = 0;

            boolean hasFuel = false;
            boolean isFluidMode = reactorMode == mio_icif_reactor_mode.FLUID && isValidFluidReactorStructure;
            // IC2 processes all components twice: heat first, then neutron pulses and EU output.
            // Cooling components always process regardless of redstone signal.
            // Fuel rods check redstone internally (processFuelRod respects hasRedstone).
            for (int pass = 0; pass < 2; pass++) {
                boolean heatRun = pass == 0;
                for (int i = 0; i < SLOT_COUNT; i++) {
                    if (i % MAX_COLUMNS >= availableColumns) continue;
                    ItemStack stack = itemHandler.getStackInSlot(i);
                    if (stack.isEmpty()) continue;

                    if (stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_nuclear_reactor fuelRod) {
                        if (hasRedstone) {
                            hasFuel |= processFuelRod(i, stack, fuelRod, heatRun, isFluidMode);
                        }
                    }
                    if (heatRun && stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_vent heatVent) {
                        processHeatVent(i, stack, heatVent, isFluidMode);
                    }
                    if (stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_exchanger heatExchanger && heatExchanger.isMelted(stack)) {
                        itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                        continue;
                    }
                    if (stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_switch heatSwitch) {
                        if (heatSwitch.isMelted(stack)) {
                            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                            continue;
                        }
                        if (heatRun) processHeatSwitch(i, heatSwitch);
                    }
                    if (heatRun && stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_condensator condensator) {
                        processCondensator(i, condensator);
                    }
                }
            }

            isRunning = hasRedstone && hasFuel;
        }

        // The reactor cycle computes EU/t; buffer that amount every game tick until the next cycle.
        boolean isFluidMode = reactorMode == mio_icif_reactor_mode.FLUID && isValidFluidReactorStructure;
        if (isRunning && !isFluidMode && currentEnergyGeneration > 0) {
            apiGenerateEnergy(currentEnergyGeneration, false);
        }
    }

    private void clearReactorEnergyBuffer() {
        long storedEnergy = getEnergyStorage().getAmount();
        if (storedEnergy > 0) {
            getEnergyStorageInternal().extract(storedEnergy, false);
        }
    }
    
    /**
     * 处理单个燃料棒
 * 参考IC2 processChamber 机制
 * - 热量遍（heatRun=true）：计算总脉冲数，计算热量，分配给散热片/反应堆
 * - 发电遍（heatRun=false）：自脉冲发电，检查周围元件接收脉冲，消耗耐久
     * @param slotIndex 槽位索引
     * @param stack 燃料棒物品堆
     * @param fuelRod 燃料棒实例
 * @param heatRun 是否为热量计算遍
     * @param isFluidMode 是否为流体模式
 * @param reactorHeatRatio 反应堆热量比率
 */
    private boolean processFuelRod(int slotIndex, ItemStack stack,
                                  com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_nuclear_reactor fuelRod,
                                  boolean heatRun, boolean isFluidMode) {
        if (fuelRod.isDepleted(stack)) return false;
        
        int basePulses = fuelRod.getBaseSelfPulses();
        int numberOfCells = fuelRod.getNumberOfCells();
        
        // 检查是否是MOX燃料棒
    boolean isMox = stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_mox_reactor;
        com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_mox_reactor moxRod = isMox ? 
            (com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_mox_reactor) stack.getItem() : null;
        
        for (int iteration = 0; iteration < numberOfCells; iteration++) {
            int pulses = basePulses;
            int receivedPulses = 0;
            
            if (!heatRun) {
                // ========== 发电遍 ==========
                // 1. 自脉冲：燃料棒自身发射脉冲
            java.util.concurrent.atomic.AtomicInteger energyOutput = new java.util.concurrent.atomic.AtomicInteger(0);
                for (int p = 0; p < pulses; p++) {
                    fuelRod.acceptNeutronPulse(stack, energyOutput, false);
                }
                
                // 2. 检查周围元件接收额外脉冲
            receivedPulses = countReceivedNeutronPulses(slotIndex, fuelRod);
                
                // 3. 计算总发电量
                int actualEnergy = energyOutput.get() + 5 * receivedPulses;
                
                // MOX特殊处理：根据堆温调整发电量
                if (isMox && moxRod != null) {
                    actualEnergy = moxRod.getMoxEnergyOutput(stack, receivedPulses, getReactorHeatRatio());
                }
                
                // 累加发电量（流体模式下不发电）
            if (!isFluidMode) {
                    currentEnergyGeneration += actualEnergy;
                }
                
            } else {
                // ========== 热量遍 ==========
                // 1. 检查周围元件接收脉冲（用于计算总脉冲数）
            receivedPulses = countReceivedNeutronPulses(slotIndex, fuelRod);

                // In IC2, normal reflector durability is consumed during the heat pass.
                consumeReflectorDurability(slotIndex);
                
                // 2. 计算总脉冲数
                int totalPulses = pulses + receivedPulses;
                
                // 3. 计算发热�
            int heat = fuelRod.calculateHeatOutput(totalPulses);
                
                // MOX特殊处理：流体模式下堆温>50%时热量翻�
            if (isMox && moxRod != null && isFluidMode && getReactorHeatRatio() > 0.5f) {
                    heat *= 2;
                }
                
                // 4. Every adjacent heat-storage component may accept fuel heat.
                if (heat > 0) {
                    int distributedHeat = distributeHeatToAdjacentVents(slotIndex, heat);
                    int remainingHeat = heat - distributedHeat;
                    addReactorHeat(remainingHeat);
                }
            }
        }
        if (!heatRun) {
            boolean depleted = fuelRod.damageItem(stack, 1);
            if (depleted) {
                net.minecraft.world.item.Item depletedItem = fuelRod.getDepletedItem();
                itemHandler.setStackInSlot(slotIndex, depletedItem == null ? ItemStack.EMPTY : new ItemStack(depletedItem));
            }
        }
        return true;
    }

    private float getReactorHeatRatio() {
        return heatStorage.getMaxHeatStored() == 0 ? 0.0f :
            (float) heatStorage.getHeatStored() / heatStorage.getMaxHeatStored();
    }

    /** Adds fuel heat immediately so later components in the heat pass observe the IC2 hull state. */
    private void addReactorHeat(int heat) {
        if (heat <= 0) return;
        currentHeatGeneration += heat;
        heatStorage.setHeat(Math.min(heatStorage.getHeatStored() + heat, heatStorage.getMaxHeatStored()));
    }
    
    /**
     * 处理散热片
 * 参考IC2两遍扫描机制
 * - 热量遍（heatRun=true）：散热片从反应堆吸热并自身散热
     * - 散发的热量累加到 reactorHeatDissipated（用于流体模式计算）
     */
    private void processHeatVent(int i, ItemStack stack, 
                                  com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_vent heatVent,
                                  boolean isFluidMode) {
        // 如果散热片熔毁，从槽位移除
    if (heatVent.isMelted(stack)) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            return;
        }

        // 创建累加器用于记录散发的热量
        java.util.concurrent.atomic.AtomicInteger dissipatedHeat = new java.util.concurrent.atomic.AtomicInteger(0);
        long reactorHeat = heatStorage.getHeatStored();

        // 对于反应堆散热片，执行散热操作（从自身散热和从反应堆吸热）
    if (stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactor_heat_vent reactorVent) {
            int absorbedHeat = reactorVent.cool(stack, reactorHeat, dissipatedHeat);
            // 从反应堆热量中减去被吸收的热量
        if (absorbedHeat > 0) {
                heatStorage.extractHeat(absorbedHeat, false);
            }
        }
        // 对于基础散热片，执行自身散热（每秒散发6点热量）
        else if (stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_basic_heat_vent basicVent) {
            // 执行自身散热，不从反应堆吸热
            basicVent.cool(stack, 0, dissipatedHeat);
        }
        // 对于高级散热片，执行自身散热（每秒散发12点热量）
        else if (stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_advanced_heat_vent advancedVent) {
            // 执行自身散热，不从反应堆吸热
            advancedVent.cool(stack, 0, dissipatedHeat);
        }
        // 对于超频散热片，执行散热操作（从自身散热和从反应堆吸热）
        else if (stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_overclocked_heat_vent overclockedVent) {
            int absorbedHeat = overclockedVent.cool(stack, reactorHeat, dissipatedHeat);
            // 从反应堆热量中减去被吸收的热量
        if (absorbedHeat > 0) {
                heatStorage.extractHeat(absorbedHeat, false);
            }
        }
        // 对于元件散热片，从相邻散热片吸热并直接散发
    else if (stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_component_heat_vent componentVent) {
            processComponentHeatVent(i, componentVent, dissipatedHeat);
        }

        // 流体模式下，累加散热片实际散发的热量
        if (isFluidMode) {
            ventDissipatedHeatForFluid += dissipatedHeat.get();
        }
    }
    
    /**
     * 计算指定槽位收到的中子脉冲数
     * 参考IC2 checkPulseable机制
 * - 相邻燃料棒：每个单元发射1个脉冲
 * - 中子反射器：反射脉冲，调用acceptNeutronPulse
     * @param slotIndex 槽位索引
     * @param fuelRod 燃料棒实例（用于反射器回调）
     * @return 收到的中子脉冲总数
     */
    private int countReceivedNeutronPulses(int slotIndex, 
                                            com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_nuclear_reactor fuelRod) {
        int pulseCount = 0;
        int row = slotIndex / MAX_COLUMNS;
        int col = slotIndex % MAX_COLUMNS;

        // 检查上下左右四个方�
    int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            // 检查边�
        if (newRow >= 0 && newRow < ROWS && newCol >= 0 && newCol < availableColumns) {
                int neighborIndex = newRow * MAX_COLUMNS + newCol;
                ItemStack neighborStack = itemHandler.getStackInSlot(neighborIndex);
                if (!neighborStack.isEmpty()) {
                    if (neighborStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_nuclear_reactor) {
                        // Each successful adjacent component callback represents one pulse in IC2.
                        pulseCount++;
                    } else if (neighborStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_neutron_reflector) {
                        // 相邻中子反射板会反射脉冲回燃料棒
                        // 反射器接受脉冲并反射，计�?个脉�
                    pulseCount++;
                    }
                }
            }
        }

        return pulseCount;
    }
    
    /**
     * 计算指定槽位周围的散热片数量（包括基础散热片、高级散热片、反应堆散热片和冷却单元�
 * @param slotIndex 槽位索引
     * @return 相邻散热片数�
 */
    
    /** 
    private int countAdjacentBasicHeatVents(int slotIndex) {
        int ventCount = 0;
        int row = slotIndex / MAX_COLUMNS;
        int col = slotIndex % MAX_COLUMNS;

        // 检查上下左右四个方�
    int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            // 检查边�
        if (newRow >= 0 && newRow < ROWS && newCol >= 0 && newCol < availableColumns) {
                int neighborIndex = newRow * MAX_COLUMNS + newCol;
                ItemStack neighborStack = itemHandler.getStackInSlot(neighborIndex);
                if (!neighborStack.isEmpty() &&
                    (neighborStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_basic_heat_vent ||
                      neighborStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_advanced_heat_vent ||
                      neighborStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactor_heat_vent ||
                      neighborStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_overclocked_heat_vent ||
                      neighborStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_exchanger)) {
                    ventCount++;
                }
                // 检查相邻槽位是否是冷凝模块（包括已满的，这样热量分配时会正确处理）
                else if (!neighborStack.isEmpty() &&
                    neighborStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_condensator) {
                    ventCount++;
                }
            }
        }

        return ventCount;
    }
    */

    /**
     * 消耗相邻中子反射板的耐久
     * 单燃料棒每次消�?耐久，双联消�?，四联消�?
     * @param slotIndex 燃料棒槽位索�
 * @param rodType 燃料棒类�
 */
    private void consumeReflectorDurability(int slotIndex) {
        int row = slotIndex / MAX_COLUMNS;
        int col = slotIndex % MAX_COLUMNS;

        // 检查上下左右四个方�
    int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            // 检查边�
        if (newRow >= 0 && newRow < ROWS && newCol >= 0 && newCol < availableColumns) {
                int neighborIndex = newRow * MAX_COLUMNS + newCol;
                ItemStack neighborStack = itemHandler.getStackInSlot(neighborIndex);
                if (!neighborStack.isEmpty() &&
                    neighborStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_neutron_reflector reflector) {
                    // 消耗反射板的耐久
                    boolean depleted = reflector.consumePulses(neighborStack, 1);
                    if (depleted) {
                        // 反射板耗尽，从槽位移除
                        itemHandler.setStackInSlot(neighborIndex, ItemStack.EMPTY);
                    }
                }
            }
        }
    }

    /**
     * 将热量分配给周围的散热片（包括基础散热片、高级散热片、反应堆散热片和冷却单元），并处理散热片的散�
 * 使用循环分配机制，模仿原版IC2：热量会尝试分配给所有相邻元件，直到没有可用空间或热量为0
     * @param slotIndex 燃料棒槽位索�
 * @param totalHeat 燃料棒产生的总热�
 * @return 实际分配的总热�
 */
    private int distributeHeatToAdjacentVents(int slotIndex, int totalHeat) {
        int totalDistributed = 0;
        int remainingHeat = totalHeat;
        int row = slotIndex / MAX_COLUMNS;
        int col = slotIndex % MAX_COLUMNS;

        // 收集所有相邻的散热元件信息
        java.util.List<HeatAcceptorInfo> acceptors = new java.util.ArrayList<>();
        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            // 检查边�
        if (newRow >= 0 && newRow < ROWS && newCol >= 0 && newCol < availableColumns) {
                int neighborIndex = newRow * MAX_COLUMNS + newCol;
                ItemStack neighborStack = itemHandler.getStackInSlot(neighborIndex);

                if (!neighborStack.isEmpty()) {
                    HeatAcceptorInfo info = createHeatAcceptorInfo(neighborIndex, neighborStack);
                    if (info != null) {
                        acceptors.add(info);
                    }
                }
            }
        }

        if (acceptors.isEmpty()) {
            return 0;
        }

        // 循环分配热量，直到没有热量或没有可用空间
        int maxIterations = 100; // 防止无限循环
        int iteration = 0;

        while (remainingHeat > 0 && iteration < maxIterations) {
            iteration++;

            // 过滤出还能接受热量的元件
            java.util.List<HeatAcceptorInfo> availableAcceptors = new java.util.ArrayList<>();
            for (HeatAcceptorInfo info : acceptors) {
                ItemStack stack = itemHandler.getStackInSlot(info.index);
                if (!stack.isEmpty()) {
                    int currentHeat = getCurrentHeat(stack, info.type);
                    int maxHeat = getMaxHeat(stack, info.type);
                    if (currentHeat < maxHeat) {
                        availableAcceptors.add(info);
                    }
                }
            }

            if (availableAcceptors.isEmpty()) {
                break;
            }
            
            // 平均分配热量给可用元�
        int heatPerAcceptor = remainingHeat / availableAcceptors.size();
            if (heatPerAcceptor == 0) {
                heatPerAcceptor = 1; // 至少分配1�
        }
            
            int heatDistributedThisRound = 0;
            
            for (HeatAcceptorInfo info : availableAcceptors) {
                if (remainingHeat <= 0) break;
                
                ItemStack stack = itemHandler.getStackInSlot(info.index);
                int currentHeat = getCurrentHeat(stack, info.type);
                int maxHeat = getMaxHeat(stack, info.type);
                int availableSpace = maxHeat - currentHeat;
                
                int heatToAdd = Math.min(heatPerAcceptor, Math.min(remainingHeat, availableSpace));
                
                if (heatToAdd > 0) {
                    int actuallyAdded = addHeatToAcceptor(stack, info.index, info.type, heatToAdd);
                    remainingHeat -= actuallyAdded;
                    heatDistributedThisRound += actuallyAdded;
                    totalDistributed += actuallyAdded;
                }
            }
            
            if (heatDistributedThisRound == 0) {
                // 本轮没有分配任何热量，退出循�
            break;
            }
        }

        return totalDistributed;
    }
    
    /**
     * 热量接受者信息类
     */
    private static class HeatAcceptorInfo {
        final int index;
        final VentType type;
        
        HeatAcceptorInfo(int index, VentType type) {
            this.index = index;
            this.type = type;
        }
    }
    
    /**
     * 创建热量接受者信�
 */
    private HeatAcceptorInfo createHeatAcceptorInfo(int index, ItemStack stack) {
        if (stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_basic_heat_vent) {
            return new HeatAcceptorInfo(index, VentType.BASIC);
        } else if (stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_advanced_heat_vent) {
            return new HeatAcceptorInfo(index, VentType.ADVANCED);
        } else if (stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactor_heat_vent) {
            return new HeatAcceptorInfo(index, VentType.REACTOR);
        } else if (stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_overclocked_heat_vent) {
            return new HeatAcceptorInfo(index, VentType.OVERCLOCKED);
        } else if (stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_exchanger) {
            return new HeatAcceptorInfo(index, VentType.HEAT_EXCHANGER);
        } else if (stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_switch) {
            return new HeatAcceptorInfo(index, VentType.HEAT_SWITCH);
        } else if (stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_condensator) {
            return new HeatAcceptorInfo(index, VentType.CONDENSATOR);
        }
        return null;
    }
    
    /**
     * 获取当前热量
     */
    private int getCurrentHeat(ItemStack stack, VentType type) {
        switch (type) {
            case BASIC:
                return ((com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_basic_heat_vent) stack.getItem()).getStoredHeat(stack);
            case ADVANCED:
                return ((com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_advanced_heat_vent) stack.getItem()).getStoredHeat(stack);
            case REACTOR:
                return ((com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactor_heat_vent) stack.getItem()).getStoredHeat(stack);
            case OVERCLOCKED:
                return ((com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_overclocked_heat_vent) stack.getItem()).getStoredHeat(stack);
            case HEAT_EXCHANGER:
                return ((com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_exchanger) stack.getItem()).getStoredHeat(stack);
            case HEAT_SWITCH:
                return ((com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_switch) stack.getItem()).getStoredHeat(stack);
            case CONDENSATOR:
                return ((com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_condensator) stack.getItem()).getStoredHeat(stack);
            default:
                return 0;
        }
    }
    
    /**
     * 获取最大热�
 */
    private int getMaxHeat(ItemStack stack, VentType type) {
        switch (type) {
            case BASIC:
                return com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_basic_heat_vent.MAX_HEAT;
            case ADVANCED:
                return com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_advanced_heat_vent.MAX_HEAT;
            case REACTOR:
                return com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactor_heat_vent.MAX_HEAT;
            case OVERCLOCKED:
                return com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_overclocked_heat_vent.MAX_HEAT;
            case HEAT_EXCHANGER:
                return ((com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_exchanger) stack.getItem()).maxHeatStorage;
            case HEAT_SWITCH:
                return ((com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_switch) stack.getItem()).getMaxHeatStorage();
            case CONDENSATOR:
                return ((com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_condensator) stack.getItem()).getMaxHeatStorage();
            default:
                return 0;
        }
    }
    
    /**
     * 添加热量到接受�
 */
    private int addHeatToAcceptor(ItemStack stack, int index, VentType type, int heat) {
        switch (type) {
            case BASIC:
                com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_basic_heat_vent basicVent =
                    (com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_basic_heat_vent) stack.getItem();
                int basicCurrent = basicVent.getStoredHeat(stack);
                int basicMax = com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_basic_heat_vent.MAX_HEAT;
                int basicAdded = Math.min(heat, basicMax - basicCurrent);
                if (basicAdded > 0) {
                    basicVent.setStoredHeat(stack, basicCurrent + basicAdded);
                }
                return basicAdded;
                
            case ADVANCED:
                com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_advanced_heat_vent advancedVent =
                    (com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_advanced_heat_vent) stack.getItem();
                int advancedCurrent = advancedVent.getStoredHeat(stack);
                int advancedMax = com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_advanced_heat_vent.MAX_HEAT;
                int advancedAdded = Math.min(heat, advancedMax - advancedCurrent);
                if (advancedAdded > 0) {
                    advancedVent.setStoredHeat(stack, advancedCurrent + advancedAdded);
                }
                return advancedAdded;
                
            case REACTOR:
                com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactor_heat_vent reactorVent =
                    (com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactor_heat_vent) stack.getItem();
                int reactorCurrent = reactorVent.getStoredHeat(stack);
                int reactorMax = com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactor_heat_vent.MAX_HEAT;
                int reactorAdded = Math.min(heat, reactorMax - reactorCurrent);
                if (reactorAdded > 0) {
                    reactorVent.setStoredHeat(stack, reactorCurrent + reactorAdded);
                    reactorVent.markReceivedFromFuelRod(stack);
                }
                return reactorAdded;

            case OVERCLOCKED:
                com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_overclocked_heat_vent overclockedVent =
                    (com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_overclocked_heat_vent) stack.getItem();
                int overclockedCurrent = overclockedVent.getStoredHeat(stack);
                int overclockedAdded = Math.min(heat,
                    com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_overclocked_heat_vent.MAX_HEAT - overclockedCurrent);
                if (overclockedAdded > 0) {
                    overclockedVent.setStoredHeat(stack, overclockedCurrent + overclockedAdded);
                }
                return overclockedAdded;
                
            case HEAT_EXCHANGER:
                com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_exchanger heatExchanger =
                    (com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_exchanger) stack.getItem();
                int exchangerCurrent = heatExchanger.getStoredHeat(stack);
                int exchangerMax = heatExchanger.maxHeatStorage;
                int exchangerAdded = Math.min(heat, exchangerMax - exchangerCurrent);
                if (exchangerAdded > 0) {
                    heatExchanger.setStoredHeat(stack, exchangerCurrent + exchangerAdded);
                }
                return exchangerAdded;

            case HEAT_SWITCH:
                com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_switch heatSwitch =
                    (com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_switch) stack.getItem();
                return heatSwitch.addHeat(stack, heat);
                
            case CONDENSATOR:
                com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_condensator condensator =
                    (com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_condensator) stack.getItem();
                int condensatorCurrent = condensator.getStoredHeat(stack);
                int condensatorMax = condensator.getMaxHeatStorage();
                int condensatorAdded = Math.min(heat, condensatorMax - condensatorCurrent);
                if (condensatorAdded > 0) {
                    condensator.addHeat(stack, condensatorAdded);
                }
                return condensatorAdded;
                
            default:
                return 0;
        }
    }

    /**
     * 处理元件散热�?- 从相邻散热片吸热并直接散�
 * @param slotIndex 元件散热片槽位索�
 * @param componentVent 元件散热片实�
 */
    private void processComponentHeatVent(int slotIndex,
            com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_component_heat_vent componentVent,
            java.util.concurrent.atomic.AtomicInteger dissipatedHeat) {
        int row = slotIndex / MAX_COLUMNS;
        int col = slotIndex % MAX_COLUMNS;

        int heatAbsorbPerTick = componentVent.getComponentCooling();

        // 检查上下左右四个方�
    int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            // 检查边�
        if (newRow >= 0 && newRow < ROWS && newCol >= 0 && newCol < availableColumns) {
                int neighborIndex = newRow * MAX_COLUMNS + newCol;
                ItemStack neighborStack = itemHandler.getStackInSlot(neighborIndex);

                if (neighborStack.isEmpty()) {
                    continue;
                }

                // 从基础散热片吸�
            if (neighborStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_basic_heat_vent basicVent) {
                    int currentHeat = basicVent.getStoredHeat(neighborStack);
                    int heatToRemove = Math.min(currentHeat, heatAbsorbPerTick);
                    if (heatToRemove > 0) {
                        basicVent.setStoredHeat(neighborStack, currentHeat - heatToRemove);
                        dissipatedHeat.addAndGet(heatToRemove);
                    }
                }
                // 从高级散热片吸热
                else if (neighborStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_advanced_heat_vent advancedVent) {
                    int currentHeat = advancedVent.getStoredHeat(neighborStack);
                    int heatToRemove = Math.min(currentHeat, heatAbsorbPerTick);
                    if (heatToRemove > 0) {
                        advancedVent.setStoredHeat(neighborStack, currentHeat - heatToRemove);
                        dissipatedHeat.addAndGet(heatToRemove);
                    }
                }
                // 从反应堆散热片吸�
            else if (neighborStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactor_heat_vent reactorVent) {
                    int currentHeat = reactorVent.getStoredHeat(neighborStack);
                    int heatToRemove = Math.min(currentHeat, heatAbsorbPerTick);
                    if (heatToRemove > 0) {
                        reactorVent.setStoredHeat(neighborStack, currentHeat - heatToRemove);
                        dissipatedHeat.addAndGet(heatToRemove);
                    }
                }
                // 从超频散热片吸热
                else if (neighborStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_overclocked_heat_vent overclockedVent) {
                    int currentHeat = overclockedVent.getStoredHeat(neighborStack);
                    int heatToRemove = Math.min(currentHeat, heatAbsorbPerTick);
                    if (heatToRemove > 0) {
                        overclockedVent.setStoredHeat(neighborStack, currentHeat - heatToRemove);
                        dissipatedHeat.addAndGet(heatToRemove);
                    }
                }
                // 从冷却单元吸�
            else if (neighborStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_exchanger heatExchanger) {
                    int currentHeat = heatExchanger.getStoredHeat(neighborStack);
                    int heatToRemove = Math.min(currentHeat, heatAbsorbPerTick);
                    if (heatToRemove > 0) {
                        heatExchanger.setStoredHeat(neighborStack, currentHeat - heatToRemove);
                        dissipatedHeat.addAndGet(heatToRemove);
                    }
                }
                else if (neighborStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_switch heatSwitch) {
                    int currentHeat = heatSwitch.getStoredHeat(neighborStack);
                    int heatToRemove = Math.min(currentHeat, heatAbsorbPerTick);
                    if (heatToRemove > 0) {
                        heatSwitch.setStoredHeat(neighborStack, currentHeat - heatToRemove);
                        dissipatedHeat.addAndGet(heatToRemove);
                    }
                }
            }
        }
    }

    /**
     * 散热片类型枚�
 */
    private enum VentType {
        BASIC,          // 基础散热�
    ADVANCED,       // 高级散热�
    REACTOR,        // 反应堆散热片
        OVERCLOCKED,    // 超频散热�
    HEAT_EXCHANGER, // 冷却单元（热交换器）
        HEAT_SWITCH,    // 热交换器
        CONDENSATOR     // 冷凝模块（红�?青金石）
    }
    

    
    /**
     * 应用热损�
 */
    protected void applyHeatLoss() {
        heatStorage.applyHeatLoss();
    }
    
    /**
     * 向相邻方块传导热�
 */
    protected void distributeHeat() {
        // IC2 reactor hull heat is only handled by internal components or fluid cooling.
        // Auto-exporting it into arbitrary HU storages changes the safety of component layouts.
    }
    
    /**
     * 更新当前温度
     */
    private void updateTemperature() {
        currentTemperature = heatStorage.getTemperature();
    }

    /**
     * 处理堆温效果
     * 根据热量百分比产生不同的危险效果
     */
    private void processHeatEffects() {
        if (level == null || level.isClientSide()) return;

        // 检查方块是否仍然存在且是核反应堆方�
    BlockState currentState = level.getBlockState(worldPosition);
        if (!(currentState.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Nuclear_Reactor_Generator)) {
            return; // 方块已被破坏，不生成粒子
        }

        long currentHeat = heatStorage.getHeatStored();
        long maxHeat = heatStorage.getMaxHeatStored();
        double heatPercentage = (double) currentHeat / maxHeat;

        // IC2 applies escalating hull-heat effects at 40%, 50%, 70%, and 85%.
        if (heatPercentage >= 0.40) {
            processFireEffect(5);
            spawnSmokeParticles(5);
        }

        if (heatPercentage >= 0.50) {
            processWaterEvaporation(5);
        }

        if (heatPercentage >= 0.70) {
            processEntityDamage(7);
            spawnFlameParticles(7);
        }

        if (heatPercentage >= 0.85) {
            processLavaTransformation(5);
        }
    }

    /**
     * 处理着火效�
 * @param range 范围（以反应堆为中心的立方体边长�
 */
    private void processFireEffect(int range) {
        if (level == null || level.isClientSide()) return;

        int radius = range / 2;
        BlockPos center = worldPosition;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!level.isLoaded(pos)) continue;

                    BlockState state = level.getBlockState(pos);

                    // 检查方块是否可�
                if (state.isFlammable(level, pos, null)) {
                        // 检查上方是否有空气
                        BlockPos above = pos.above();
                        if (level.getBlockState(above).isAir()) {
                            // 设置�
                        level.setBlock(above, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理水蒸发效�
 * @param range 范围
     */
    private void processWaterEvaporation(int range) {
        if (level == null || level.isClientSide()) return;

        int radius = range / 2;
        BlockPos center = worldPosition;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!level.isLoaded(pos)) continue;

                    BlockState state = level.getBlockState(pos);

                    // 检查是否是�
                if (state.getBlock() == net.minecraft.world.level.block.Blocks.WATER) {
                        // 蒸发水，变成空气
                        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);

                        // 产生蒸汽粒子效果
                        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            serverLevel.sendParticles(
                                net.minecraft.core.particles.ParticleTypes.CLOUD,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                5, 0.2, 0.2, 0.2, 0.01
                            );
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理生物伤害效果
     * @param range 范围
     */
    private void processEntityDamage(int range) {
        if (level == null || level.isClientSide()) return;

        double radius = range / 2.0;
        BlockPos center = worldPosition;

        // 获取范围内的所有实�
    var entities = level.getEntities(null,
            new net.minecraft.world.phys.AABB(
                center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                center.getX() + radius + 1, center.getY() + radius + 1, center.getZ() + radius + 1
            )
        );

        for (var entity : entities) {
            // 检查是否是生物
            if (entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
                // 应用辐射效果（与燃料棒的辐射效果一致）
                applyRadiationEffect(livingEntity);
            }
        }
    }

    /**
     * 对生物应用辐射效�
 * 与燃料棒的辐射效果一�
 */
    private void applyRadiationEffect(net.minecraft.world.entity.LivingEntity entity) {
        // 应用辐射效果
        entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            com.singularity_iteration.mio_icif.effect.mio_icif_effects.RADIATION,
            100, // 5�
        1,   // II�
        false, true
        ));

        // 计算实际受到的辐射伤害（考虑防化服防护）
        float actualDamage = RadiationProtectionUtil.calculateRadiationDamage(entity, 2.0F);

        // 如果实际伤害大于 0，才造成伤害
        if (actualDamage > 0.0F) {
            entity.hurt(new net.minecraft.world.damagesource.DamageSource(
                level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(mio_icif_DamageTypes.RADIATION), null, null), actualDamage);
        }
    }

    /**
     * 处理岩浆转换效果
     * @param range 范围
     */
    private void processLavaTransformation(int range) {
        if (level == null || level.isClientSide()) return;

        int radius = range / 2;
        BlockPos center = worldPosition;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!level.isLoaded(pos)) continue;

                    // 跳过反应堆本身的位置
                    if (pos.equals(center)) continue;

                    BlockState state = level.getBlockState(pos);

                    // 将某些方块变成岩�
                // 只转换石头、泥土、沙子等普通方块，不转换机器方�
                if (canTransformToLava(state)) {
                        level.setBlock(pos, net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /**
     * 检查方块是否可以变成岩�
 */
    private boolean canTransformToLava(BlockState state) {
        Block block = state.getBlock();
        // 可以变成岩浆的方块列�
    return block == net.minecraft.world.level.block.Blocks.STONE ||
               block == net.minecraft.world.level.block.Blocks.COBBLESTONE ||
               block == net.minecraft.world.level.block.Blocks.DIRT ||
               block == net.minecraft.world.level.block.Blocks.GRASS_BLOCK ||
               block == net.minecraft.world.level.block.Blocks.SAND ||
               block == net.minecraft.world.level.block.Blocks.GRAVEL ||
               block == net.minecraft.world.level.block.Blocks.NETHERRACK ||
               block == net.minecraft.world.level.block.Blocks.DEEPSLATE ||
               block == net.minecraft.world.level.block.Blocks.COBBLED_DEEPSLATE ||
               block == net.minecraft.world.level.block.Blocks.TUFF ||
               block == net.minecraft.world.level.block.Blocks.ANDESITE ||
               block == net.minecraft.world.level.block.Blocks.DIORITE ||
               block == net.minecraft.world.level.block.Blocks.GRANITE;
    }

    /**
     * 产生烟灰粒子
     * @param range 范围
     */
    private void spawnSmokeParticles(int range) {
        if (level == null || level.isClientSide()) return;

        BlockPos center = worldPosition;

        // 在范围内随机产生烟灰粒子
        if (level.getGameTime() % 10 == 0) { // �?0tick产生一�
        for (int i = 0; i < 5; i++) {
                double x = center.getX() + 0.5 + (level.random.nextDouble() - 0.5) * range;
                double y = center.getY() + 0.5 + (level.random.nextDouble() - 0.5) * range;
                double z = center.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * range;

                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        x, y, z,
                        1, 0.1, 0.1, 0.1, 0.01
                    );
                }
            }
        }
    }

    /**
     * 产生火焰粒子
     * @param range 范围
     */
    private void spawnFlameParticles(int range) {
        if (level == null || level.isClientSide()) return;

        BlockPos center = worldPosition;

        // 在范围内随机产生火焰粒子
        if (level.getGameTime() % 5 == 0) { // �?tick产生一�
        for (int i = 0; i < 8; i++) {
                double x = center.getX() + 0.5 + (level.random.nextDouble() - 0.5) * range;
                double y = center.getY() + 0.5 + (level.random.nextDouble() - 0.5) * range;
                double z = center.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * range;

                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.FLAME,
                        x, y, z,
                        1, 0.1, 0.1, 0.1, 0.02
                    );
                }
            }
        }
    }

    /**
     * 覆盖父类的能量分配方�
 * 核反应堆根据 isRunning 状态而不�?burnTime 来输出能�
 */
    @Override
    protected void distributeEnergy() {
        // 核反应堆只要有能量就会输出，不依�?burnTime
        if (getEnergyStorage().getAmount() <= 0) {
            return;
        }
        
        // 向六个方向输出能�
    for (Direction direction : Direction.values()) {
            outputEnergyToDirection(direction);
        }
    }
    
    /**
     * 向指定方向输出能�
 */
    private void outputEnergyToDirection(Direction direction) {
        BlockPos adjacentPos = worldPosition.relative(direction);

        IEUEnergyStorage adjacentStorage = 
            level.getCapability(com.singularity_iteration.mio_icif.energy.EnergyUnit.EUApi.SIDED, adjacentPos, direction.getOpposite());

        if (adjacentStorage != null) {
            long energyToTransfer = Math.min(getEnergyStorage().getAmount(), getEnergyStorage().getMaxExtract());
            if (energyToTransfer > 0) {
                long received = adjacentStorage.receive(energyToTransfer, false);
                if (received > 0) {
                    getEnergyStorageInternal().extract(received, false);
                }
            }
            return;
        }

        long energyToTransfer = Math.min(getEnergyStorage().getAmount(), getEnergyStorage().getMaxExtract());
        long pushed = com.singularity_iteration.mio_icif.integration.mi.EnergyBridge.pushToStorage(level, adjacentPos, direction, energyToTransfer);
        if (pushed > 0) {
            getEnergyStorageInternal().extract(pushed, false);
        }
    }
    
    // ==================== IC2 能量源接口覆盖（对齐原版核反应堆行为�?====================

    @Override
    public double getOfferedEnergy() {
        return isPowerSource && isRunning ? getEnergyStorage().getAmount() : 0.0D;
    }

    public int getPacketCount() {
        return 1;
    }

    @Override
    public int getSourceTier() {
        if (!isPowerSource) return -1;
        return Math.max(EnergyNetGlobal.getTierFromPower(currentEnergyGeneration), 2);
    }

    /**
     * 核反应堆通过电网系统分配能量，而不是直接向相邻方块输出�
 * 这样核反应仓也能作为电网连接点，将电力推送到电网�
 */
    @Override
    protected boolean shouldDirectlyDistributeEnergy() {
        return false;
    }

    // ==================== PowerGrid.IPowerSource 接口实现 ====================
    
    /**
     * 获取功率输出（动态返回当前发电量�
 */
    @Override
    public long getPowerOutput() {
        return isRunning ? currentEnergyGeneration : 0;
    }
    
    /**
     * 提取能量给用电器
     */
    public long extractPowerForConsumer(long amount, boolean simulate) {
        if (!isRunning) return 0;
        return getEnergyStorageInternal().extract(amount, simulate);
    }
    
    // ==================== Getter 方法 ====================
    
    /**
     * 获取热量存储
     */
    public HeatStorage getHeatStorage() {
        return heatStorage;
    }
    
    /**
     * 获取当前热量
     */
    public long getCurrentHeat() {
        return heatStorage.getHeatStored();
    }
    
    /**
     * 获取最大热量容量
     */
    public long getMaxHeat() {
        return heatStorage.getMaxHeatStored();
    }
    
    /**
     * 获取当前温度
     */
    @Override
    public double getCurrentTemperature() {
        return currentTemperature;
    }
    
    /**
     * 获取当前热量产生速率
     */
    public int getCurrentHeatGeneration() {
        return currentHeatGeneration;
    }
    
    /**
     * 获取当前能量产生速率
     */
    public long getCurrentEnergyGeneration() {
        return currentEnergyGeneration;
    }
    
    // 上次更新核反应仓的时间戳
    private long lastChamberUpdateTick = 0;
    
    // ==================== IMetaDelegate 接口实现 ====================

    @Override
    public List<IEnergyTile> getSubTiles() {
        return java.util.Collections.unmodifiableList(subTiles);
    }
    
    /**
     * 更新可用的核反应仓数�
 * 检测周�?个面是否有核反应仓，每个核反应仓增加一列可用槽�
 * 槽位总数固定�?4个，但只�?availableColumns 列是可用�
 */
    private void updateReactorChambers(Level level, BlockPos pos) {
        long currentTick = level.getGameTime();
        
        // 限制更新频率，每 5 tick 最多更新一次，避免死锁
        if (currentTick - lastChamberUpdateTick < 5) {
            return;
        }
        lastChamberUpdateTick = currentTick;
        
        // 安全检查：确保区块已加�
    if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return;
        }
        
        int chamberCount = 0;
        List<IEnergyTile> newSubTiles = new java.util.ArrayList<>();
        newSubTiles.add(this);
        
        // 检查六个面（上下左右前后）
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            
            // 安全检查：确保邻居区块已加�
        if (!level.hasChunk(neighborPos.getX() >> 4, neighborPos.getZ() >> 4)) {
                continue;
            }
            
            BlockState neighborState = level.getBlockState(neighborPos);
            
            // 检查是否是核反应仓方块
            if (neighborState.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_Reactor_Chamber) {
                chamberCount++;
                BlockEntity be = level.getBlockEntity(neighborPos);
                if (be instanceof mio_icif_reactor_chamber chamber) {
                    newSubTiles.add(chamber);
                }
            }
        }
        
        // 计算可用列数：基础 3 �?+ 核反应仓数量，最�?9 列，最�?3 �
    int newColumns = Math.max(BASE_COLUMNS, Math.min(BASE_COLUMNS + chamberCount, MAX_COLUMNS));
        
        // 检�?subTiles 是否发生变化，如果变化则刷新电网注册
        boolean subTilesChanged = !newSubTiles.equals(subTiles);
        if (subTilesChanged) {
            subTiles.clear();
            subTiles.addAll(newSubTiles);
            refreshRegistration();
        }
        
        // 计算基础热量上限和各种组件提供的额外热量上限
        int baseHeatCapacity = HEAT_CAPACITY_BASE + (chamberCount * HEAT_CAPACITY_PER_CHAMBER);
        int extraHeatFromPlating = calculateExtraHeatCapacityFromPlating();
        int newHeatCapacity = baseHeatCapacity + extraHeatFromPlating;

        // 如果可用列数发生变化
        if (newColumns != availableColumns) {
            int oldColumns = availableColumns;
            availableColumns = newColumns;
            currentSlotCount = availableColumns * ROWS;

            // 更新热量上限
            heatStorage.setCapacity(newHeatCapacity);

            // 如果可用列数减少，将被关闭槽位中的物品弹�
        if (newColumns < oldColumns) {
                ejectItemsFromClosedSlots(level, pos, newColumns, oldColumns);
            }

            // 通知客户端更�
        setChanged();
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
        } else if (heatStorage.getMaxHeatStored() != newHeatCapacity) {
            // 即使列数没有变化，如果冷却单元或隔板导致热量上限变化，也要更�
        heatStorage.setCapacity(newHeatCapacity);
            setChanged();
        }
    }

    /**
     * 重新计算热量容量
     * 基于当前物品栏中的冷却单元和隔板计算额外的热量上�
 * 用于世界加载时恢复正确的热量容量
     */
    private void recalculateHeatCapacity() {
        if (itemHandler == null) {
            return;
        }

        // 计算核反应仓数量
        int chamberCount = 0;
        BlockPos pos = this.worldPosition;
        Level level = this.level;
        if (level != null) {
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = pos.relative(direction);
                if (!level.hasChunk(neighborPos.getX() >> 4, neighborPos.getZ() >> 4)) {
                    continue;
                }
                BlockState neighborState = level.getBlockState(neighborPos);
                if (neighborState.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_Reactor_Chamber) {
                    chamberCount++;
                }
            }
        }

        int baseHeatCapacity = HEAT_CAPACITY_BASE + (chamberCount * HEAT_CAPACITY_PER_CHAMBER);
        int extraHeatFromPlating = calculateExtraHeatCapacityFromPlating();
        int newHeatCapacity = baseHeatCapacity + extraHeatFromPlating;

        // 更新热量容量
        heatStorage.setCapacity(newHeatCapacity);
    }

    /**
     * 将被关闭槽位中的物品弹出到世界中
     * @param level 世界
     * @param pos 反应堆位�
 * @param newColumns 新的可用列数
     * @param oldColumns 旧的可用列数
     */
    private void ejectItemsFromClosedSlots(Level level, BlockPos pos, int newColumns, int oldColumns) {
        // 遍历被关闭的�
    for (int col = newColumns; col < oldColumns; col++) {
            // 遍历该列的所有行
            for (int row = 0; row < ROWS; row++) {
                int slotIndex = row * MAX_COLUMNS + col;
                ItemStack stack = itemHandler.getStackInSlot(slotIndex);
                
                if (!stack.isEmpty()) {
                    // 弹出物品到世界中
                    Block.popResource(level, pos, stack);
                    // 清空槽位
                    itemHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
                }
            }
        }
    }
    
    /**
     * 处理热交换器的热量交�
 * 参考IC2 ItemReactorHeatSwitch.processChamber 机制�
 * 使用百分比平衡算法，根据热量差异决定传递方向和数量
     * @param slotIndex 热交换器槽位索引
     * @param heatSwitch 热交换器实例
     */
    private void processHeatSwitch(int slotIndex, com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_switch heatSwitch) {
        ItemStack stack = itemHandler.getStackInSlot(slotIndex);
        if (stack.isEmpty()) return;

        int row = slotIndex / MAX_COLUMNS;
        int col = slotIndex % MAX_COLUMNS;
        int myHeat = 0;

        // 收集相邻可存储热量的元件
        java.util.ArrayList<HeatAcceptor> heatAcceptors = new java.util.ArrayList<>();
        int switchSide = heatSwitch.getHeatTransferToAdjacent();
        int switchReactor = heatSwitch.getHeatTransferToReactor();

        if (switchSide > 0) {
            checkHeatAcceptor(row - 1, col, heatAcceptors);
            checkHeatAcceptor(row + 1, col, heatAcceptors);
            checkHeatAcceptor(row, col - 1, heatAcceptors);
            checkHeatAcceptor(row, col + 1, heatAcceptors);
        }

        // 1. 向相邻元件传递热量（使用百分比平衡算法）
        if (switchSide > 0) {
            for (HeatAcceptor acceptor : heatAcceptors) {
                double myMed = heatSwitch.getHeatPercentage(stack);
                double heatableMed = acceptor.getHeatPercentage();

                // 计算传递热量（参考IC2公式�
            int add = (int) (acceptor.getMaxHeat() / 100.0 * (heatableMed + myMed / 2.0));
                if (add > switchSide) add = switchSide;

                if (heatableMed + myMed / 2.0 < 1.0) add = switchSide / 2;
                if (heatableMed + myMed / 2.0 < 0.75) add = switchSide / 4;
                if (heatableMed + myMed / 2.0 < 0.5) add = switchSide / 8;
                if (heatableMed + myMed / 2.0 < 0.25) add = 1;

                // 决定传递方�
            if (Math.round(heatableMed * 10.0) / 10.0 > Math.round(myMed * 10.0) / 10.0) {
                    add = -2 * add; // 从相邻元件吸�
            } else if (Math.round(heatableMed * 10.0) / 10.0 == Math.round(myMed * 10.0) / 10.0) {
                    add = 0; // 热量平衡，不传�
            }

                // A positive value transfers heat from this switch; a negative one draws heat back.
                if (add > 0) {
                    myHeat -= acceptor.receiveHeat(add);
                } else if (add < 0) {
                    myHeat += acceptor.extractHeat(-add);
                }
            }
        }

        // 2. 向核反应堆传递热量（使用百分比平衡算法）
        if (switchReactor > 0) {
            double myMed = heatSwitch.getHeatPercentage(stack);
            double reactorMed = (double) heatStorage.getHeatStored() * 100.0 / heatStorage.getMaxHeatStored();

            int add = (int) Math.round(heatStorage.getMaxHeatStored() / 100.0 * (reactorMed + myMed / 2.0));
            if (add > switchReactor) add = switchReactor;

            if (reactorMed + myMed / 2.0 < 1.0) add = switchReactor / 2;
            if (reactorMed + myMed / 2.0 < 0.75) add = switchReactor / 4;
            if (reactorMed + myMed / 2.0 < 0.5) add = switchReactor / 8;
            if (reactorMed + myMed / 2.0 < 0.25) add = 1;

            // 决定传递方�
        if (Math.round(reactorMed * 10.0) / 10.0 > Math.round(myMed * 10.0) / 10.0) {
                add = -2 * add; // 从反应堆吸热
            } else if (Math.round(reactorMed * 10.0) / 10.0 == Math.round(myMed * 10.0) / 10.0) {
                add = 0; // 热量平衡，不传�
        }

            if (add > 0) {
                long accepted = Math.min(add, heatStorage.getMaxHeatStored() - heatStorage.getHeatStored());
                heatStorage.setHeat(heatStorage.getHeatStored() + accepted);
                myHeat -= accepted;
            } else if (add < 0) {
                long extracted = Math.min(-add, heatStorage.getHeatStored());
                heatStorage.setHeat(heatStorage.getHeatStored() - extracted);
                myHeat += extracted;
            }
        }

        // 3. 应用热量变化到热交换器自�
    if (myHeat != 0) {
            int currentHeat = heatSwitch.getStoredHeat(stack);
            int newHeat = currentHeat + myHeat;
            if (newHeat >= 0 && newHeat <= heatSwitch.getMaxHeatStorage()) {
                heatSwitch.setStoredHeat(stack, newHeat);
            } else if (newHeat < 0) {
                heatSwitch.setStoredHeat(stack, 0);
            } else {
                // 超过上限，熔�
            heatSwitch.setStoredHeat(stack, heatSwitch.getMaxHeatStorage());
                itemHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
            }
        }
    }

    /**
     * 热量接受者封装类（用于热交换器）
     */
    private class HeatAcceptor {
        ItemStack stack;
        com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactor component;

        HeatAcceptor(ItemStack stack, int row, int col, com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactor component) {
            this.stack = stack;
            this.component = component;
        }

        double getHeatPercentage() {
            if (component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_vent heatVent) {
                return (double) heatVent.getStoredHeat(stack) * 100.0 / heatVent.getMaxHeatStorage();
            } else if (component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_exchanger heatExchanger) {
                return (double) heatExchanger.getStoredHeat(stack) * 100.0 / heatExchanger.maxHeatStorage;
            } else if (component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_switch heatSwitch) {
                return (double) heatSwitch.getStoredHeat(stack) * 100.0 / heatSwitch.getMaxHeatStorage();
            } else if (component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_condensator condensator) {
                return (double) condensator.getStoredHeat(stack) * 100.0 / condensator.getMaxHeatStorage();
            }
            return 0;
        }

        int getMaxHeat() {
            if (component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_vent heatVent) {
                return heatVent.getMaxHeatStorage();
            } else if (component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_exchanger heatExchanger) {
                return heatExchanger.maxHeatStorage;
            } else if (component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_switch heatSwitch) {
                return heatSwitch.getMaxHeatStorage();
            } else if (component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_condensator condensator) {
                return condensator.getMaxHeatStorage();
            }
            return 0;
        }

        int receiveHeat(int amount) {
            if (component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_vent heatVent) {
                return heatVent.addHeat(stack, amount);
            } else if (component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_exchanger heatExchanger) {
                return heatExchanger.addHeat(stack, amount);
            } else if (component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_switch heatSwitch) {
                return heatSwitch.addHeat(stack, amount);
            } else if (component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_condensator condensator) {
                int before = condensator.getStoredHeat(stack);
                condensator.addHeat(stack, amount);
                return condensator.getStoredHeat(stack) - before;
            }
            return 0;
        }

        int extractHeat(int amount) {
            if (component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_vent heatVent) {
                return heatVent.removeHeat(stack, amount);
            } else if (component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_exchanger heatExchanger) {
                return heatExchanger.removeHeat(stack, amount);
            } else if (component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_switch heatSwitch) {
                return heatSwitch.removeHeat(stack, amount);
            }
            // Condensators only absorb heat, exactly as in IC2.
            return 0;
        }
    }

    /**
     * 检查相邻元件是否可以存储热�
 */
    private void checkHeatAcceptor(int row, int col, java.util.ArrayList<HeatAcceptor> heatAcceptors) {
        if (row < 0 || row >= ROWS || col < 0 || col >= availableColumns) return;

        int neighborIndex = row * MAX_COLUMNS + col;
        ItemStack neighborStack = itemHandler.getStackInSlot(neighborIndex);
        if (neighborStack.isEmpty()) return;

        if (neighborStack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactor component) {
            // 检查是否可以存储热�
        if (component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_vent ||
                component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_exchanger ||
                component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_switch ||
                component instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_condensator) {
                heatAcceptors.add(new HeatAcceptor(neighborStack, row, col, component));
            }
        }
    }

    /**
     * 处理冷凝模块的吸热功�
 * 参考IC2机制：冷凝模块通过 alterHeat 被动吸收热量，不是主动从周围吸热
     * 热量由热交换器或其他元件传递过�
 * @param slotIndex 冷凝模块槽位索引
     * @param condensator 冷凝模块实例
     */
    private void processCondensator(int slotIndex, com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_condensator condensator) {
        // 冷凝模块在IC2中是被动元件，通过 alterHeat 吸收热量
        // 不需要主动处理，热量由热交换器传�
    // 检查是否已满，如果满了则无法再吸收热量
        ItemStack stack = itemHandler.getStackInSlot(slotIndex);
        if (stack.isEmpty() || condensator.isFull(stack)) return;
        
        // 冷凝模块不需要主动吸热，热交换器会在 processHeatSwitch 中处理热量传�
    // 这里只需要检查冷凝模块是否熔�
    if (condensator.getStoredHeat(stack) >= condensator.getMaxHeatStorage()) {
            itemHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
        }
    }

    /**
     * 计算冷却单元提供的额外热量上�
 * 每个冷却单元提供10000点额外热量上�
 * @return 额外热量上限
     */

    /** 
    private int calculateExtraHeatCapacityFromExchangers() {
        int extraCapacity = 0;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() &&
                stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_heat_exchanger heatExchanger) {
                extraCapacity += heatExchanger.getExtraHeatCapacity(stack);
            }
        }
        return extraCapacity;
    }
    */
   
    /**
     * 计算反应堆隔板提供的额外热量上限
     * @return 额外热量上限
     */
    private int calculateExtraHeatCapacityFromPlating() {
        int extraCapacity = 0;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() &&
                stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactor_plating plating) {
                extraCapacity += plating.getHeatCapacityBonus(stack);
            }
        }
        return extraCapacity;
    }

    /**
     * 计算反应堆隔板降低的爆炸范围百分�
 * @return 爆炸范围降低百分比（0-100�
 */
    private int calculateExplosionReductionFromPlating() {
        int totalReduction = 0;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() &&
                stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactor_plating plating) {
                totalReduction += plating.getExplosionReductionPercent(stack);
            }
        }
        // 爆炸范围降低最�?00%（完全不会爆炸）
        return Math.min(totalReduction, 100);
    }

    /**
     * 获取当前可用的列�
 */
    public int getAvailableColumns() {
        return availableColumns;
    }
    
    /**
     * 获取当前可用的槽位数�
 */
    public int getCurrentSlotCount() {
        return currentSlotCount;
    }

    /**
     * 获取当前输出功率（用于GUI显示�
 */
    public int getCurrentOutput() {
        return currentEnergyGeneration;
    }
    
    /**
     * 检查反应堆是否正在运行
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * 获取反应堆物品栏
     */
    public NonNullList<ItemStack> getReactorItems() {
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            items.set(i, itemHandler.getStackInSlot(i));
        }
        return items;
    }
    
    // ==================== 物品栏相关方�?====================
    
    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }
    
    @Override
    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        return itemHandler.getStackInSlot(slot);
    }
    
    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        int toRemove = Math.min(amount, stack.getCount());
        ItemStack result = stack.split(toRemove);
        
        if (stack.isEmpty()) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }
        
        setChanged();
        return result;
    }
    
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = itemHandler.getStackInSlot(slot);
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }
    
    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return;
        }
        // 核反应堆每个槽位只能接受一个物�
    if (!stack.isEmpty() && stack.getCount() > 1) {
            stack.setCount(1);
        }
        itemHandler.setStackInSlot(slot, stack);
        setChanged();
    }
    
    @Override
    public int getMaxStackSize() {
        // 核反应堆每个槽位只能接受一个物�
    return 1;
    }
    
    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }
    
    @Override
    public void clearContent() {
        for (int i = 0; i < itemHandler.getSlots(); i++) { itemHandler.setStackInSlot(i, ItemStack.EMPTY); }
        setChanged();
    }
    
    // ==================== 燃料相关方法（供子类重写�?===================
    
    /**
     * 获取燃料的燃烧时�
 * 核反应堆不使用传统的燃烧时间机制，而是由燃料棒自身管理
     */
    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        // 核反应堆不直接使用此方法，燃料棒的发电逻辑�?processReactorLogic 中处�
    return 0;
    }
    
    // ==================== 数据保存与加�?====================
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // 保存反应堆物品栏（使�?ReactorItems 标签避免与基类的 Items 冲突�
    net.minecraft.nbt.ListTag listTag = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                listTag.add(stack.save(registries, itemTag));
            }
        }
        tag.put("ReactorItems", listTag);

        tag.putLong("HeatStored", heatStorage.getHeatStored());
        tag.putLong("MaxHeatStored", heatStorage.getMaxHeatStored());
        tag.putInt("Temperature", currentTemperature);

        // 保存运行状态
        tag.putBoolean("IsRunning", isRunning);
        tag.putInt("CurrentHeatGeneration", currentHeatGeneration);
        tag.putInt("CurrentEnergyGeneration", currentEnergyGeneration);
        tag.putInt("ReactorCycleTicks", reactorCycleTicks);

        // 保存反应堆模�
    tag.putString("ReactorMode", reactorMode.getName());

        // 保存流体处理器数�
    if (fluidHandler != null) {
            CompoundTag fluidTag = new CompoundTag();
            fluidHandler.saveToNBT(fluidTag, registries);
            tag.put("FluidHandler", fluidTag);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // 加载反应堆物品栏（使�?ReactorItems 标签�
    for (int i = 0; i < itemHandler.getSlots(); i++) { itemHandler.setStackInSlot(i, ItemStack.EMPTY); }
        if (tag.contains("ReactorItems", net.minecraft.nbt.Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag listTag = tag.getList("ReactorItems", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag itemTag = listTag.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot >= 0 && slot < itemHandler.getSlots()) {
                    itemHandler.setStackInSlot(slot, ItemStack.parse(registries, itemTag).orElse(ItemStack.EMPTY));
                }
            }
        }

        // 加载热量存储数据
        long heatStored = tag.contains("HeatStored", net.minecraft.nbt.Tag.TAG_LONG) ? tag.getLong("HeatStored") : tag.getInt("HeatStored");
        long savedMaxHeat = tag.contains("MaxHeatStored", net.minecraft.nbt.Tag.TAG_LONG) ? tag.getLong("MaxHeatStored") : tag.getInt("MaxHeatStored");

        if (heatStored > 0) {
            // 先根据加载的物品重新计算热量容量（冷却单元会增加热量上限�
        recalculateHeatCapacity();

            // 如果保存的最大热量大于当前计算的最大热量，使用保存的�
        // 这避免了在区块未完全加载时核反应仓数量计算错误的问题
            long currentMaxHeat = heatStorage.getMaxHeatStored();
            if (savedMaxHeat > currentMaxHeat) {
                heatStorage.setCapacity(savedMaxHeat);
            }

            // 直接设置热量（而不是receiveHeat，因为容量已经重新计算）
            // 确保热量不超过最大容�
        long heatToSet = Math.min(heatStored, heatStorage.getMaxHeatStored());
            heatStorage.setHeat(heatToSet);
        }
        currentTemperature = tag.getInt("Temperature");

        // 加载运行状�
    isRunning = tag.getBoolean("IsRunning");
        currentHeatGeneration = tag.getInt("CurrentHeatGeneration");
        currentEnergyGeneration = tag.getInt("CurrentEnergyGeneration");
        reactorCycleTicks = tag.contains("ReactorCycleTicks") ? tag.getInt("ReactorCycleTicks") : 19;

        // 加载反应堆模�
    if (tag.contains("ReactorMode")) {
            reactorMode = mio_icif_reactor_mode.fromString(tag.getString("ReactorMode"));
        }

        // 加载流体处理器数�
    if (fluidHandler != null && tag.contains("FluidHandler")) {
            fluidHandler.loadFromNBT(tag.getCompound("FluidHandler"), registries);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        // 保存反应堆物品栏（使�?ReactorItems 标签�
    net.minecraft.nbt.ListTag listTag = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                listTag.add(stack.save(registries, itemTag));
            }
        }
        tag.put("ReactorItems", listTag);
        tag.putLong("HeatStored", heatStorage.getHeatStored());
        tag.putInt("Temperature", currentTemperature);
        tag.putBoolean("IsRunning", isRunning);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        // 加载反应堆物品栏（使�?ReactorItems 标签�
    if (tag.contains("ReactorItems", net.minecraft.nbt.Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag listTag = tag.getList("ReactorItems", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag itemTag = listTag.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot >= 0 && slot < itemHandler.getSlots()) {
                    itemHandler.setStackInSlot(slot, ItemStack.parse(registries, itemTag).orElse(ItemStack.EMPTY));
                }
            }
        }
        currentTemperature = tag.getInt("Temperature");
        isRunning = tag.getBoolean("IsRunning");
    }
    
    // ==================== MenuProvider 接口实现 ====================
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.nuclear_reactor_generator");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // 根据当前模式返回不同�?GUI
        if (reactorMode == mio_icif_reactor_mode.FLUID && isValidFluidReactorStructure) {
            return new FluidReactorMenu(containerId, playerInventory, this);
        }
        return new com.singularity_iteration.mio_icif.Menu.Generator.NuclearReactorGeneratorMenu(containerId, playerInventory, this, this.getItemHandler(), this.getGeneratorContainerData());
    }
    
    /**
     * 获取核反应堆的物品处理器
     * 使用统一槽位系统�?itemHandler
     */
    @Override
    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return new NuclearReactorItemHandler();
    }
    
    // ==================== 流体反应堆模式相关方�?====================
    
    /**
     * 获取数据同步容器（用于流体反应堆 GUI�
 */
    public ContainerData getContainerData() {
        return containerData;
    }

    /**
     * 获取数据同步容器（用于发电模�?GUI�
 */
    public ContainerData getGeneratorContainerData() {
        return generatorContainerData;
    }
    
    /**
     * 获取当前反应堆模�
 */
    public mio_icif_reactor_mode getReactorMode() {
        return reactorMode;
    }

    @Override
    public ReactorMode getApiReactorMode() {
        return reactorMode == mio_icif_reactor_mode.FLUID ? ReactorMode.FLUID : ReactorMode.GENERATOR;
    }
    
    /**
     * 设置反应堆模�
 */
    public void setReactorMode(mio_icif_reactor_mode mode) {
        // 只有在结构有效时才能切换到流体模�
    if (mode == mio_icif_reactor_mode.FLUID && !isValidFluidReactorStructure) {
            return;
        }
        this.reactorMode = mode;
        setChanged();
    }
    
    /**
     * 检查是否为有效的流体反应堆结构
     */
    public boolean isValidFluidReactorStructure() {
        return isValidFluidReactorStructure;
    }
    
    /**
     * 获取流体处理�
 */
    public mio_icif_fluid_reactor_handler getFluidHandler() {
        return fluidHandler;
    }

    /**
     * 获取流体处理能力（用�?Capability 系统�
 */
    public net.neoforged.neoforge.fluids.capability.IFluidHandler getFluidHandlerCapability(@Nullable net.minecraft.core.Direction side) {
        // 只有在流体反应堆模式下才提供流体处理能力
        if (this.reactorMode == mio_icif_reactor_mode.FLUID && this.fluidHandler != null) {
            return this.fluidHandler;
        }
        return null;
    }
    
    /**
     * 获取输入液体�
 */
    public int getInputFluidAmount() {
        return fluidHandler != null ? fluidHandler.getInputFluidAmount() : 0;
    }
    
    /**
     * 获取输出液体�
 */
    public int getOutputFluidAmount() {
        return fluidHandler != null ? fluidHandler.getOutputFluidAmount() : 0;
    }
    
    /**
     * 获取反应堆物品处理器（供 GUI 使用�
 */
    public IItemHandler getReactorItemHandler() {
        return new NuclearReactorItemHandler();
    }
    
    /**
     * 核反应堆的物品处理器
     * 用于 capability 系统，限制物品的插入和提取行�
 */
    protected class NuclearReactorItemHandler implements IItemHandlerModifiable {
        /**
         * 检查槽位是否在可用范围�
     * 根据可用列数计算哪些槽位是可用的
         */
        private boolean isSlotAvailable(int slot) {
            if (slot < 0 || slot >= SLOT_COUNT) {
                return false;
            }
            // 计算槽位所在的�
        int column = slot % MAX_COLUMNS;
            // 检查列是否在可用范围内
            return column < availableColumns;
        }
        
        @Override
        public int getSlots() {
            return SLOT_COUNT; // 返回总槽位数�?4个）
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= SLOT_COUNT) {
                return ItemStack.EMPTY;
            }
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            // 检查槽位是否可用（在可用列范围内）
            if (!isSlotAvailable(slot)) {
                return stack; // 槽位不可用，返回原物�
        }
            
            ItemStack existing = itemHandler.getStackInSlot(slot);
            
            // 核反应堆每个槽位只能接受一个物�
        if (!existing.isEmpty()) {
                return stack;
            }
            
            if (stack.getCount() > 1) {
                // 只能放入一个物�
            if (!simulate) {
                    itemHandler.setStackInSlot(slot, stack.copyWithCount(1));
                    setChanged();
                }
                return stack.copyWithCount(stack.getCount() - 1);
            } else {
                if (!simulate) {
                    itemHandler.setStackInSlot(slot, stack.copy());
                    setChanged();
                }
                return ItemStack.EMPTY;
            }
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= SLOT_COUNT) {
                return ItemStack.EMPTY;
            }
            
            ItemStack existing = itemHandler.getStackInSlot(slot);
            if (existing.isEmpty()) {
                return ItemStack.EMPTY;
            }
            
            int toExtract = Math.min(amount, existing.getCount());
            ItemStack result = existing.copyWithCount(toExtract);
            
            if (!simulate) {
                if (existing.getCount() <= toExtract) {
                    itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
                } else {
                    itemHandler.setStackInSlot(slot, existing.copyWithCount(existing.getCount() - toExtract));
                }
                setChanged();
            }
            
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1; // 核反应堆每个槽位只能接受一个物�
    }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // 检查槽位是否可�
        if (!isSlotAvailable(slot)) {
                return false;
            }
            // 只接受核反应物品（燃料棒、散热片等）或实现 IBaseReactorComponent 的附属模组物品
            return stack.getItem() instanceof mio_icif_reactor
                    || stack.getItem() instanceof com.singularity_iteration.mio_icif.api.reactor.IBaseReactorComponent;
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (slot >= 0 && slot < SLOT_COUNT) {
                // 核反应堆每个槽位只能接受一个物�
            if (!stack.isEmpty() && stack.getCount() > 1) {
                    stack = stack.copyWithCount(1);
                }
                itemHandler.setStackInSlot(slot, stack);
                setChanged();
            }
        }
    }

}
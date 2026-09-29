package com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractKineticBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.machine.IKineticGeneratorBlock;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_condenser;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.jetbrains.annotations.Nullable;
import com.miophas.singularity_iteration.core.api.machine.ISlotLayout;

/**
 * 蒸汽动能发生机方块实体：消耗蒸汽产出 KU 动能。
 *
 * <p>本类已按原版 IC2 {@code ic2.core.block.kineticgenerator.tileentity.TileEntitySteamKineticGenerator}
 * （1.12.2）逐条对齐：
 * <ul>
 *   <li>普通蒸汽 1 mB = 2 KU，过热蒸汽 1 mB = 4 KU；</li>
 *   <li>每 tick <b>抽干整个蒸汽槽</b>；普通蒸汽按 1/10 记入冷凝进度、其余排给相邻冷凝器，
 *       过热蒸汽全部排给相邻冷凝器（或另一台蒸汽动能机）；</li>
 *   <li>蒸馏水槽非空即按水量线性节流（不是"满了才停"）；冷凝进度每满 100 → 1 mB 蒸馏水，
 *       装不下则"涡轮被水堵住"；</li>
 *   <li>蒸汽排不完 → {@code ventingSteam}，每 tick 1/10 概率威力 1 的热爆炸；</li>
 *   <li>涡轮槽放的是"蒸汽涡轮"转子，每 20 tick 消耗 1（过热）或 2（普通）点耐久。</li>
 * </ul>
 *
 * <p>本模组原有的附加机制（蒸汽/蒸馏水单元槽、自动外送蒸馏水与蒸汽、以及基类的摩擦损耗与
 * 按转速全向传输）全部雪藏，见 {@link #AUX_AUTOMATION_ENABLED} 与 {@link #KINETIC_CAPACITY} 一带注释。
 */
@SuppressWarnings("null")
public class mio_icif_steam_kinetic_generator extends AbstractKineticBlockEntity implements com.miophas.singularity_iteration.core.api.machine.IProducerBlock, IKineticGeneratorBlock {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(2)
        .turbine()
        .upgrade(2)
        .build();

    // 槽位定义
    public static final int STEAM_CELL_SLOT = 0;     // 【雪藏】蒸汽单元输入
    public static final int WATER_CELL_SLOT = 1;     // 【雪藏】蒸馏水单元输出槽
    public static final int TURBINE_SLOT = 2;        // 涡轮转子
    public static final int UPGRADE_SLOT_START = 3;  // 升级槽起始
    /** 原版 IC2 只有 1 个升级槽（{@code InvSlotUpgrade(this, "upgrade", 1)}）。 */
    public static final int UPGRADE_SLOT_COUNT = 1;
    public static final int TOTAL_SLOTS = 5;

    /**
     * 雪藏开关：以下都不是原版 IC2 的机制 ——
     * 蒸汽/蒸馏水单元槽的自动处理、自动把蒸馏水与蒸汽推给邻居、以及基类的摩擦损耗/
     * 按转速全向传输。置 true 可恢复本模组的附加自动化（会破坏"涡轮被水堵住"等原版行为）。
     */
    public static final boolean AUX_AUTOMATION_ENABLED = false;

    // ==================== 流体容量（对齐 IC2） ====================
    /** 蒸汽槽：原版 IC2 21000 mB，只进不出。 */
    public static final int STEAM_TANK_CAPACITY = 21000;
    /** 蒸馏水槽：原版 IC2 1000 mB，可进可出（原版还接受普通水）。 */
    public static final int WATER_TANK_CAPACITY = 1000;

    // ==================== 工作参数（对齐 IC2） ====================
    /** 普通蒸汽每 mB 产出的 KU（IC2: amount * 2）。 */
    public static final int KU_PER_MB_STEAM = 2;
    /** 过热蒸汽每 mB 产出的 KU（IC2: amount * 2 * 2）。 */
    public static final int KU_PER_MB_SUPERHEATED = 4;
    /** 原版 IC2：`turbineSlot.damage(hotSteam ? 1 : 2)`。 */
    public static final int TURBINE_DURABILITY_COST_NORMAL = 2;
    public static final int TURBINE_DURABILITY_COST_SUPERHEATED = 1;
    /**
     * 转子寿命（秒）。对应 IC2 配置 {@code balance/SteamKineticGenerator/rotorlivetime}（默认 86400 秒）。
     * 损耗为 1 点 / 20 tick，因此"耐久上限 = 秒数"正好等于 24 小时连续运行（普通蒸汽翻倍损耗 → 12 小时）。
     */
    public static final int ROTOR_LIVETIME_SECONDS = 86400;
    /** 冷凝进度阈值与除数（IC2: condensation = amount / 10，满 100 → 1 mB 蒸馏水）。 */
    public static final int CONDENSATION_THRESHOLD = 100;
    public static final int CONDENSATION_DIVISOR = 10;
    /** IC2 {@code getTickRate()}：转子耐久与冷凝只有每 20 tick 结算一次。 */
    public static final int UPDATE_TICK_INTERVAL = 20;

    // ==================== 雪藏：KU 存储的附加设置 ====================
    // 原版 IC2 的蒸汽涡轮是"速率型"机器：只有 kUoutput（本 tick 可用速率），
    // 没有 KU 缓冲、没有摩擦损耗、没有转速门槛、也没有抽取上限。
    // 下面这些 KineticStorage 参数全部中性化，并且不再调用基类的 AbstractKineticBlockEntity.tick
    // （摩擦 + 按转速全向传输）；本 tick 用不掉的 KU 直接丢弃（discardUnusedKinetic）。
    /** 雪藏：不做缓冲容量上限。 */
    public static final int KINETIC_CAPACITY = Integer.MAX_VALUE;
    public static final int KINETIC_MAX_RECEIVE = 0;
    /** 雪藏：抽取上限即当 tick 产出速率，不额外限流。 */
    public static final int KINETIC_MAX_EXTRACT = Integer.MAX_VALUE;
    /** 雪藏：无转速门槛。 */
    public static final int MAX_RPM = Integer.MAX_VALUE;
    /** 雪藏：无摩擦损耗。 */
    public static final float FRICTION_FACTOR = 0.0f;

    // 流体存储
    protected final FluidTank steamTank;
    protected final FluidTank waterTank;
    private final IFluidHandler combinedFluidHandler;

    // 物品
    protected final MachineItemHandler itemHandler;

    // 状态
    private boolean isWorking = false;
    private boolean isTurbineFilledWithWater = false;
    /** IC2 `throttled`：蒸馏水槽非空时为 true，KU 输出按水量线性衰减。 */
    private boolean throttled = false;
    /** IC2 `ventingSteam`：蒸汽排不出去时为 true。 */
    private boolean ventingSteam = false;
    private int lastKineticOutput = 0; // 上一 tick 实际产出
    private int condensationProgress = 0; // 冷凝进度（原版）
    private int updateTicker = 0; // 20 tick 计数

    /**
     * 流体弹出 / 抽取升级的通用自动化组件。
     * <p>
     * 基类 {@link AbstractKineticBlockEntity} 没有这套设施，这里按
     * {@code AbstractHeatBlockEntity} / {@code AbstractProcessingMachineBlockEntity} 的同一模式补上。
     * 对齐 IC2：{@code TileEntitySteamKineticGenerator#getUpgradableProperties()} 声明了
     * {@code ItemConsuming / FluidConsuming / FluidProducing}，所以升级槽接受流体弹出/抽取升级；
     * 而原版的蒸馏水槽是 {@code addTank}（可进可出），本类对应 {@code Role.IO}，因此：
     * <ul>
     *   <li>弹出升级 → 只会把【蒸馏水槽】的水推给相邻机器（蒸汽机的水槽、另一台汽轮机的
     *       蒸馏水槽都接受蒸馏水），这正是"涡轮被水堵住"的解药；</li>
     *   <li>抽取升级 → 把相邻的蒸汽抽进【蒸汽槽】（INPUT_ONLY，只进不出）。</li>
     * </ul>
     */
    private final com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent fluidAutomation =
        new com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent(
            new com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent.Host() {
                @Override public Level level() { return level; }
                @Override public BlockPos worldPosition() { return worldPosition; }
                @Override public IFluidHandler ownFluidHandler() { return combinedFluidHandler; }
                @Override public IFluidHandler adjacentFluidHandler(BlockPos pos, Direction side) {
                    return getAdjacentFluidHandler(pos, side);
                }
                @Override public com.miophas.singularity_iteration.core.api.machine.IMachineUpgradeStats upgradeStats() {
                    recalculateUpgradeStats();
                    return upgradeStats;
                }
                @Override public void markUnsaved() { setChanged(); }
                @Override public void markNeighborUnsaved(BlockPos pos) {
                    com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent
                        .markNeighborUnsaved(level, pos);
                }
            });

    protected com.miophas.singularity_iteration.core.api.machine.IMachineUpgradeStats upgradeStats =
        com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats.empty();
    private boolean upgradeStatsDirty = true;


    // 数据同步访问
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> isHotSteam() ? 1 : 0;
                case 1 -> hasTurbine() ? 1 : 0;
                case 2 -> isTurbineFilledWithWater ? 1 : 0;
                case 3 -> isWorking ? 1 : 0;
                case 4 -> lastKineticOutput;
                case 5 -> gaugeLiquidScaled(26, 0);
                case 6 -> waterTank.getFluidAmount();
                case 7 -> throttled ? 1 : 0;
                case 8 -> ventingSteam ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 客户端只读，不需要实现
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public mio_icif_steam_kinetic_generator(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.STEAM_KINETIC_GENERATOR_ENTITY_TYPE.get());
    }

    public mio_icif_steam_kinetic_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type, pos, state, KINETIC_CAPACITY, KINETIC_MAX_RECEIVE, KINETIC_MAX_EXTRACT, MAX_RPM, FRICTION_FACTOR);

        this.steamTank = new FluidTank(STEAM_TANK_CAPACITY, fluidStack -> {
            if (fluidStack.isEmpty()) return true;
            return fluidStack.getFluid() == mio_icif_fluids.STEAM.get()
                || fluidStack.getFluid() == mio_icif_fluids.SUPERHEATEDSTEAM.get();
        });

        // 原版 IC2：addTank("distilledWaterTank", 1000, fluidPredicate(distilled_water, WATER))
        // —— 蒸馏水槽同时接受蒸馏水和普通水。
        this.waterTank = new FluidTank(WATER_TANK_CAPACITY, fluidStack -> {
            if (fluidStack.isEmpty()) return true;
            return fluidStack.getFluid() == mio_icif_fluids.DISTILLEDWATER.get()
                || fluidStack.getFluid() == net.minecraft.world.level.material.Fluids.WATER;
        });
        this.combinedFluidHandler = new com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup(java.util.List.of(
            new com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup.Tank(steamTank,
                com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup.Role.INPUT_ONLY),
            new com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup.Tank(waterTank,
                com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup.Role.IO)));

        this.itemHandler = new MachineItemHandler(LAYOUT) {
            @Override
            protected void onContentsChanged(int slot) {
                // 升级槽变化时重算统计快照（与其它机器的做法一致）
                if (LAYOUT.isType(slot, com.miophas.singularity_iteration.core.prefab.inventory.SlotType.UPGRADE)) {
                    upgradeStatsDirty = true;
                }
                setChanged();
            }
        };
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_steam_kinetic_generator.this.isItemValidForSlot(slot, stack));
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            // 雪藏：IC2 没有蒸汽单元输入槽 / 蒸馏水单元输出槽（只靠管道与容器交互），
            // 槽位保留定义以兼容旧存档，但不再接受任何物品。
            case STEAM_CELL_SLOT, WATER_CELL_SLOT -> false;
            // 对齐 IC2：涡轮槽放的是"蒸汽涡轮"转子（CraftingItemType.steam_turbine），
            // 扇叶（steam_turbine_blade）只是合成转子的材料。
            case TURBINE_SLOT -> stack.is(mio_icif_normal.STEAM_TURBIN.get());
            // 升级槽：只接受升级组件（IC2 的 InvSlotUpgrade 同样只收升级）
            default -> slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + UPGRADE_SLOT_COUNT
                && stack.getItem() instanceof com.miophas.singularity_iteration.common.item.upgrade.mio_icif_upgrade;
        };
    }

    /** 升级统计快照（带脏标记缓存，与其它机器同构）。 */
    protected void recalculateUpgradeStats() {
        if (!upgradeStatsDirty) return;
        upgradeStatsDirty = false;
        if (itemHandler == null) {
            upgradeStats = com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats.empty();
            return;
        }
        int start = LAYOUT.getStart(com.miophas.singularity_iteration.core.prefab.inventory.SlotType.UPGRADE);
        int count = LAYOUT.getCount(com.miophas.singularity_iteration.core.prefab.inventory.SlotType.UPGRADE);
        upgradeStats = com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats
            .fromInventory(itemHandler, start, count);
    }

    @Nullable
    private IFluidHandler getAdjacentFluidHandler(BlockPos pos, @Nullable Direction side) {
        if (level == null || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return null;
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
    }

    /**
     * 升级槽驱动的流体自动化（等价 IC2 的 {@code upgradeSlot.tick()}）：
     * 弹出升级把【蒸馏水槽】的水排给相邻机器，抽取升级把相邻的蒸汽抽进【蒸汽槽】。
     * <p>
     * 注意与雪藏的 {@code outputWaterToNeighbors()} 的区别：那条是"没有升级也自动外送"，
     * 会让原版"涡轮被水堵住"机制失效；这里只在装了升级时才动，属原版机制。
     */
    protected void handleAutomationUpgrades() {
        if (level == null || level.isClientSide) return;
        recalculateUpgradeStats();
        int ejectors = upgradeStats.getFluidEjectorCount();
        if (ejectors > 0) fluidAutomation.ejectFluids(combinedFluidHandler, ejectors);
        int pullers = upgradeStats.getFluidPullingCount();
        if (pullers > 0) fluidAutomation.pullFluids(combinedFluidHandler, pullers);
    }

    /**
     * 每 tick 更新逻辑，对齐 IC2 {@code updateEntityServer}（1.12.2 L303-328）。
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_steam_kinetic_generator blockEntity) {
        if (level.isClientSide()) return;

        boolean wasWorking = blockEntity.isWorking;

        // 雪藏：以下三项都不是原版 IC2 行为，统一由 AUX_AUTOMATION_ENABLED 开关控制 ——
        //   handleSteamCellSlot   原版没有蒸汽单元槽；
        //   outputWaterToNeighbors 原版不会自动送走蒸馏水（否则"涡轮被水堵住"机制失效）；
        //   outputSteamToNeighbors 原版只在产汽流程里把蒸汽排给相邻冷凝器；
        //   AbstractKineticBlockEntity.tick 摩擦损耗 + 按转速向 6 面传输。
        if (AUX_AUTOMATION_ENABLED) {
            blockEntity.handleSteamCellSlot();
            blockEntity.generateKinetic();
            blockEntity.outputWaterToNeighbors();
            blockEntity.outputSteamToNeighbors();
            AbstractKineticBlockEntity.tick(level, pos, state, blockEntity);
        } else {
            blockEntity.generateKinetic();
        }

        // 原版 IC2：只在正面提供 kUoutput
        blockEntity.outputKineticToFront();

        // 雪藏 KU 缓冲：本 tick 没被取走的输出直接丢失（蒸汽已经消耗掉了）
        blockEntity.discardUnusedKinetic();

        // 升级槽：流体弹出 / 抽取（等价 IC2 的 upgradeSlot.tick()）。
        // 放在这里让"弹出蒸馏水"和本 tick 的产水同一 tick 结算，不会被水堵住再解堵。
        blockEntity.handleAutomationUpgrades();

        if (wasWorking != blockEntity.isWorking) {
            blockEntity.updateBlockState(blockEntity.isWorking);
        }

        blockEntity.setChanged();
    }

    /**
     * 对应 IC2 {@code updateEntityServer} 的主分支（L306-324）：蒸汽 + 转子 + 未被水堵住才工作，
     * 每 20 tick 按涡轮槽物品自身的耐久扣一次转子。
     */
    private void generateKinetic() {
        lastKineticOutput = 0;

        // IC2 L306-307：蒸馏水槽只要有 1 mB 空位就解除"涡轮被水堵住"
        if (waterTank.getCapacity() - waterTank.getFluidAmount() >= 1 && isTurbineFilledWithWater) {
            isTurbineFilledWithWater = false;
        }

        if (steamTank.getFluidAmount() > 0 && !isTurbineFilledWithWater && hasTurbine()) {
            isWorking = true;
            boolean hotSteam = isHotSteam();
            boolean turbineDoneWork = turbineDoWork(hotSteam);
            // IC2 L315-319：if (updateTicker++ >= getTickRate()) { if (turbineDoneWork) turbineSlot.damage(hotSteam ? 1 : 2, false); updateTicker = 0; }
            if (updateTicker++ >= UPDATE_TICK_INTERVAL) {
                if (turbineDoneWork) {
                    damageTurbine(hotSteam ? TURBINE_DURABILITY_COST_SUPERHEATED : TURBINE_DURABILITY_COST_NORMAL);
                }
                updateTicker = 0;
            }
        } else {
            isWorking = false;
            lastKineticOutput = 0;
        }
    }

    /**
     * 对齐 IC2 {@code handleSteam}（L330-343）：每 tick 抽干蒸汽槽，普通蒸汽按 1/10 记入冷凝进度，
     * 其余蒸汽排给相邻冷凝器；过热蒸汽全部排给相邻冷凝器/蒸汽动能机（不产生冷凝水）。
     *
     * @return 本 tick 的原始 KU 产出（未节流）
     */
    private int handleSteam(boolean hotSteam) {
        int amount = steamTank.getFluidAmount();
        if (amount <= 0) return 0;
        steamTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);

        int kuWorkBuffer = amount * (hotSteam ? KU_PER_MB_SUPERHEATED : KU_PER_MB_STEAM);
        if (hotSteam) {
            outputSteam(amount, true);
        } else {
            int condensation = amount / CONDENSATION_DIVISOR;
            condensationProgress += condensation;
            outputSteam(amount - condensation, false);
        }
        return kuWorkBuffer;
    }

    /**
     * 对齐 IC2 {@code turbineDoWork}（L345-365）：按蒸馏水槽水量线性节流 KU 输出，
     * 并把冷凝进度结算成蒸馏水。
     *
     * @return 本 tick 是否真的产出了 KU
     */
    private boolean turbineDoWork(boolean hotSteam) {
        int rawOutput = handleSteam(hotSteam);

        // IC2 L348-355：蒸馏水槽非空 → throttled，节流系数 = 1 - 水量/容量
        int waterAmount = waterTank.getFluidAmount();
        throttled = waterAmount != 0;
        float throttle = waterAmount == 0 ? 1.0F : 1.0F - (float) waterAmount / waterTank.getCapacity();

        // IC2 还乘了一个 outputModifier（配置 balance/energy/kineticgenerator/steam，默认 1.0）
        int kuOutput = (int) (rawOutput * throttle);
        lastKineticOutput = kuOutput;
        if (kuOutput > 0) {
            kineticStorage.generateKineticInternal(kuOutput, false);
        }

        // IC2 L357-363：冷凝进度满 100 → 1 mB 蒸馏水；装不下则标记"涡轮被水堵住"
        if (condensationProgress >= CONDENSATION_THRESHOLD) {
            FluidStack one = new FluidStack(mio_icif_fluids.DISTILLEDWATER.get(), 1);
            if (waterTank.fill(one, IFluidHandler.FluidAction.SIMULATE) == 1) {
                condensationProgress -= CONDENSATION_THRESHOLD;
                waterTank.fill(one, IFluidHandler.FluidAction.EXECUTE);
            } else {
                isTurbineFilledWithWater = true;
            }
        }
        return kuOutput > 0;
    }

    /**
     * 对齐 IC2 {@code outputSteam}（L367-387）：只排给相邻的冷凝器（过热蒸汽时也排给另一台
     * 蒸汽动能机），并且<b>无论输入是蒸汽还是过热蒸汽，一律以普通蒸汽形式排出</b>（原版行为）。
     * 排不完的蒸汽触发 {@code ventingSteam}，并有 1/10 概率产生威力 1 的热爆炸。
     */
    private void outputSteam(int amount, boolean hotSteam) {
        int remaining = amount;
        for (Direction dir : Direction.values()) {
            if (remaining <= 0) break;
            if (level == null) break;
            BlockPos neighborPos = worldPosition.relative(dir);
            if (!level.hasChunkAt(neighborPos)) continue;

            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            boolean accepted = neighbor instanceof mio_icif_condenser
                || (hotSteam && neighbor instanceof mio_icif_steam_kinetic_generator);
            if (!accepted) continue;

            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
            if (handler == null) continue;

            int filled = handler.fill(new FluidStack(mio_icif_fluids.STEAM.get(), remaining), IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) remaining -= filled;
        }

        if (remaining > 0) {
            ventingSteam = true;
            if (level != null && level.random.nextInt(10) == 0) {
                level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    1.0F, Level.ExplosionInteraction.NONE);
            }
        } else {
            ventingSteam = false;
        }
    }

    /**
     * 对齐 IC2 {@code turbineSlot.damage(...)}：按物品自身的耐久上限消耗转子，
     * 耐久耗尽即移除（原版用 {@code InvSlotConsumable}，行为一致）。
     */
    private void damageTurbine(int amount) {
        ItemStack turbine = itemHandler.getStackInSlot(TURBINE_SLOT);
        if (turbine.isEmpty()) return;

        int maxDamage = turbine.getMaxDamage();
        int newDamage = turbine.getDamageValue() + amount;
        if (maxDamage > 0 && newDamage >= maxDamage) {
            itemHandler.extractItem(TURBINE_SLOT, 1, false);
        } else {
            turbine.setDamageValue(newDamage);
            itemHandler.setStackInSlot(TURBINE_SLOT, turbine);
        }
    }

    /**
     * 雪藏 KU 缓冲：原版 IC2 的蒸汽涡轮没有储能，本 tick 没被取走的 kUoutput 直接丢失
     * （蒸汽已经消耗掉了，不会留到下一 tick）。
     */
    private void discardUnusedKinetic() {
        if (kineticStorage.getKineticStored() > 0) {
            kineticStorage.setKinetic(0);
        }
    }

    /**
     * KU 动能输出到正前方的方块（正面输出接口，对齐 IC2 只在正面提供 kUoutput）。
     */
    private void outputKineticToFront() {
        if (level == null || level.isClientSide()) return;
        if (kineticStorage.getKineticStored() <= 0) return;

        Direction facing = getBlockState().getValue(
            net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos frontPos = worldPosition.relative(facing);

        IMioIcifCapabilities.IKineticStorage frontKinetic = level.getCapability(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK,
            frontPos, facing.getOpposite());
        if (frontKinetic == null) {
            BlockEntity be = level.getBlockEntity(frontPos);
            frontKinetic = MioIcifAPI.instance().getCapabilities().adaptKineticStorage(be);
        }

        if (frontKinetic != null && frontKinetic.canReceiveKinetic()) {
            long toTransfer = Math.min(kineticStorage.getKineticStored(),
                                      Math.min(kineticStorage.getMaxExtract(),
                                               frontKinetic.getMaxKineticStored() - frontKinetic.getKineticStored()));
            if (toTransfer > 0) {
                long extracted = kineticStorage.extractKinetic(toTransfer, false);
                if (extracted > 0) {
                    frontKinetic.receiveKinetic(extracted, false);
                }
            }
        }
    }

    /** 雪藏：原版 IC2 没有蒸汽单元槽，蒸汽只能从管道/容器进入蒸汽槽。 */
    private void handleSteamCellSlot() {
        ItemStack cellStack = itemHandler.getStackInSlot(STEAM_CELL_SLOT);
        if (cellStack.isEmpty()) return;

        boolean isSteamCell = mio_icif_cells.isCellContainingFluid(cellStack, mio_icif_fluids.STEAM.get());
        boolean isSuperheatedCell = mio_icif_cells.isCellContainingFluid(cellStack, mio_icif_fluids.SUPERHEATEDSTEAM.get());
        if (!isSteamCell && !isSuperheatedCell) return;

        Fluid fluidToFill = isSuperheatedCell ? mio_icif_fluids.SUPERHEATEDSTEAM.get() : mio_icif_fluids.STEAM.get();
        if (steamTank.getFluidAmount() + FluidType.BUCKET_VOLUME > steamTank.getCapacity()) return;

        ItemStack waterStack = itemHandler.getStackInSlot(WATER_CELL_SLOT);
        if (!waterStack.isEmpty() && (!mio_icif_cells.isCellContainingFluid(waterStack, mio_icif_fluids.DISTILLEDWATER.get()) || waterStack.getCount() >= waterStack.getMaxStackSize())) {
            if (!mio_icif_cells.isEmptyCell(waterStack) || waterStack.getCount() >= waterStack.getMaxStackSize()) {
                return;
            }
        }

        int filled = steamTank.fill(new FluidStack(fluidToFill, FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
        if (filled >= FluidType.BUCKET_VOLUME) {
            itemHandler.extractItem(STEAM_CELL_SLOT, 1, false);
            if (waterStack.isEmpty()) {
                itemHandler.setStackInSlot(WATER_CELL_SLOT, mio_icif_cells.getEmptyCellForStack(cellStack));
            } else {
                waterStack.grow(1);
            }
            setChanged();
        }
    }

    /** 雪藏：原版 IC2 不会自动把蒸馏水送走，否则"蒸馏水槽满 → 涡轮被水堵住"机制失效。 */
    private void outputWaterToNeighbors() {
        if (level == null || waterTank.getFluidAmount() <= 0) return;

        FluidStack available = waterTank.getFluid();
        if (available.isEmpty()) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            IFluidHandler neighborHandler = level.getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                neighborPos, dir.getOpposite());

            if (neighborHandler != null) {
                FluidStack toDrain = new FluidStack(available.getFluid(), Math.min(available.getAmount(), 1000));
                int filled = neighborHandler.fill(toDrain, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    waterTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    available = waterTank.getFluid();
                    if (available.isEmpty()) return;
                }
            }
        }
    }

    /** 雪藏：原版 IC2 只在产汽流程里把蒸汽排给相邻冷凝器/蒸汽动能机（见 outputSteam）。 */
    private void outputSteamToNeighbors() {
        if (level == null || steamTank.getFluidAmount() <= 0) return;

        FluidStack available = steamTank.getFluid();
        if (available.isEmpty() || available.getFluid() != mio_icif_fluids.SUPERHEATEDSTEAM.get()) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            IFluidHandler neighborHandler = level.getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                neighborPos, dir.getOpposite());

            if (neighborHandler != null) {
                FluidStack toDrain = new FluidStack(available.getFluid(), Math.min(available.getAmount(), 1000));
                int filled = neighborHandler.fill(toDrain, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    steamTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    available = steamTank.getFluid();
                    if (available.isEmpty()) return;
                }
            }
        }
    }

    private void updateBlockState(boolean working) {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (state.hasProperty(com.miophas.singularity_iteration.common.block.kugenerator.mio_icif_block_steam_kinetic_generator.LIT)) {
            level.setBlock(worldPosition, state.setValue(
                com.miophas.singularity_iteration.common.block.kugenerator.mio_icif_block_steam_kinetic_generator.LIT, working), 3);
        }
    }

    // ==================== IProducerBlock API ====================

    @Override
    public void forceStartWork() {
        this.isWorking = true;
    }

    @Override
    public void forceStopWork() {
        this.isWorking = false;
    }

    @Override
    public com.miophas.singularity_iteration.core.api.machine.IMachineUpgradeStats getUpgradeStats() {
        int start = UPGRADE_SLOT_START;
        int count = UPGRADE_SLOT_COUNT;
        return com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats.fromInventory(itemHandler, start, count);
    }

    @Override
    public int getUpgradeSlotStart() {
        return UPGRADE_SLOT_START;
    }

    @Override
    public int getUpgradeSlotCount() {
        return UPGRADE_SLOT_COUNT;
    }

    @Override
    public java.util.List<ItemStack> getUpgrades() {
        java.util.List<ItemStack> upgrades = new java.util.ArrayList<>();
        for (int i = UPGRADE_SLOT_START; i < UPGRADE_SLOT_START + UPGRADE_SLOT_COUNT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                upgrades.add(stack);
            }
        }
        return upgrades;
    }

    @Override
    public int getProgress() {
        return 0;
    }

    @Override
    public int getMaxProgress() {
        return 1;
    }

    @Override
    public int getBaseMaxProgress() {
        return 1;
    }

    @Override
    public long getEnergyPerTick() {
        return 0;
    }

    @Override
    public ISlotLayout getSlotLayout() {
        return LAYOUT;
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
    public boolean isWorking() {
        return isWorking;
    }

    @Override
    public int getKineticOutput() {
        return isWorking ? lastKineticOutput : 0;
    }

    @Override
    public boolean isGenerating() {
        return isWorking;
    }

    @Override
    public int getBurnTime() {
        return 0;
    }

    @Override
    public int getBurnDuration() {
        return 0;
    }

    @Override
    public int getKineticGenerationRate() {
        return lastKineticOutput;
    }

    @Override
    public int getRotorRPM() {
        return kineticStorage != null ? kineticStorage.getRPM() : 0;
    }

    @Override
    public long getEffectiveEnergyPerTick() {
        return 0;
    }

    @Override
    public long getTotalProcessed() {
        return 0;
    }

    // ==================== Capability ====================

    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return itemHandler;
    }

    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return combinedFluidHandler;
    }

    @Nullable
    public net.neoforged.neoforge.energy.IEnergyStorage getEnergyStorageCapability(@Nullable Direction side) {
        return null;
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("steamTank", steamTank.writeToNBT(registries, new CompoundTag()));
        tag.put("waterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Items", itemHandler.serializeNBT(registries));
        tag.putBoolean("isWorking", isWorking);
        tag.putBoolean("isTurbineFilledWithWater", isTurbineFilledWithWater);
        tag.putInt("condensationProgress", condensationProgress);
        tag.putInt("updateTicker", updateTicker);
        // 弹出/抽取升级的传输缓冲是"先扣后交付"的耐久账，必须存档，否则重启会重放或丢账
        fluidAutomation.save(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("steamTank")) steamTank.readFromNBT(registries, tag.getCompound("steamTank"));
        if (tag.contains("waterTank")) waterTank.readFromNBT(registries, tag.getCompound("waterTank"));
        if (tag.contains("Items")) itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        fluidAutomation.load(tag, registries);
        upgradeStatsDirty = true;
        isWorking = tag.getBoolean("isWorking");
        isTurbineFilledWithWater = tag.getBoolean("isTurbineFilledWithWater");
        condensationProgress = tag.getInt("condensationProgress");
        updateTicker = tag.getInt("updateTicker");
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.steam_kinetic_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.generator.SteamKineticGeneratorMenu(containerId, playerInventory, this);
    }

    /**
     * 获取数据同步访问
     */
    public ContainerData getContainerData() {
        return dataAccess;
    }

    // ==================== Getters ====================

    public FluidTank getSteamTank() { return steamTank; }
    public FluidTank getWaterTank() { return waterTank; }
    /** 合并后的流体端口（蒸汽槽只进 / 蒸馏水槽双向），供右击容器交互使用。 */
    public IFluidHandler getCombinedFluidHandler() { return combinedFluidHandler; }
    public ItemStackHandler getItemHandler() { return itemHandler; }

    /**
     * 是否已安装涡轮转子（原版 IC2：涡轮槽放的是 steam_turbine）。
     */
    public boolean hasTurbine() {
        ItemStack stack = itemHandler.getStackInSlot(TURBINE_SLOT);
        return !stack.isEmpty() && stack.is(mio_icif_normal.STEAM_TURBIN.get());
    }

    /**
     * 涡轮是否被水填满（原版 IC2 风格）
     */
    public boolean isTurbineFilledWithWater() {
        return isTurbineFilledWithWater;
    }

    /**
     * 是否处于节流状态（原版 IC2 {@code throttled}：蒸馏水槽非空）
     */
    public boolean isThrottled() {
        return throttled;
    }

    /**
     * 是否有蒸汽排不出去（原版 IC2 {@code ventingSteam}）
     */
    public boolean isVentingSteam() {
        return ventingSteam;
    }

    /**
     * 当前输入的是否为过热蒸汽（原版 IC2 风格）
     */
    public boolean isHotSteam() {
        FluidStack fluid = steamTank.getFluid();
        return !fluid.isEmpty() && fluid.getFluid() == mio_icif_fluids.SUPERHEATEDSTEAM.get();
    }

    /**
     * 获取上一 tick 实际产出的 KU（供 GUI 显示输出，原版叫 getKUoutput）
     */
    public int getLastKineticOutput() { return lastKineticOutput; }

    /**
     * 获取蒸馏水槽（原版 getDistilledWaterTank）
     */
    public FluidTank getTank() {
        return waterTank;
    }

    /**
     * 液体条缩放（原版 IC2 风格）
     */
    public int gaugeLiquidScaled(int i, int tank) {
        switch (tank) {
            case 0:
                if (waterTank.getFluidAmount() <= 0) return 0;
                return waterTank.getFluidAmount() * i / waterTank.getCapacity();
        }
        return 0;
    }

}

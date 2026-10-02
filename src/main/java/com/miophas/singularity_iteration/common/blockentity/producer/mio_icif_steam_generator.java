package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractHeatBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 蒸汽机方块实
 * 用水 + HU 热量产生蒸汽或过热蒸
 *
 * 工作原理
 * - 从除正面外的其他面接
HU 热量
 * - 消耗水/蒸馏水产生蒸
 * - 1 mB 
= 100 mB 蒸汽，消
100 HU
 * - 使用普通水会钙化，蒸馏水不
 */
@SuppressWarnings("null")
public class mio_icif_steam_generator extends AbstractHeatBlockEntity {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(2)
        .upgrade(2)
        .build();

    /**
     * 雪藏开关：原版 IC2 的蒸汽机<b>没有任何物品槽</b>
     * （{@code ContainerSteamGenerator} 只有 {@code super(te)}，{@code TileEntitySteamGenerator}
     * 也没有声明任何 InvSlot），水桶槽与自动化升级都不是原版机制。
     * <p>槽位定义与 NBT 字段保留以兼容旧存档；置 true 可恢复本模组的辅助槽处理。
     */
    public static final boolean AUX_SLOTS_ENABLED = false;

    // 槽位定义
    public static final int WATER_BUCKET_SLOT = 0;   // 水桶输入
public static final int EMPTY_BUCKET_SLOT = 1;   // 空桶输出
public static final int UPGRADE_SLOT_START = 2;  // 升级槽起
public static final int UPGRADE_SLOT_COUNT = 2;  // 升级槽数
public static final int TOTAL_SLOTS = 4;

    // 流体容量
    public static final int WATER_TANK_CAPACITY = 10000;  // 水槽 10 
// 内部蒸汽罐已按原版 IC2 雪藏：IC2 产汽后直接推送给相邻容器，不缓存。
    // 容量置 0 让存储失效，同时保留 NBT 字段以兼容旧存档（残留蒸汽会被推送出去）。
    public static final int STEAM_TANK_CAPACITY = 0; // 蒸汽

    // 工作参数（原
public static final float HEAT_PER_HU = 5.0E-4F;   // 
    public static final float COOLING_PER_TICK = 0.01F; // 不工作时
    public static final float MAX_SYSTEM_HEAT = 500.0F; // 系统热量上限（超过爆炸）
    public static final int MAX_HU_PER_TICK = 1200;     // 
    public static final int STEAM_EXPANSION = 100;      // 1mB 

    public static final int MAX_CALCIFICATION = 100000; // 钙化上限（原版）
    public static final float EPSILON = 1.0E-4F;

    // 热量配置（对齐原版 IC2：没有 HU 容器，每 tick 直接向邻居索取至多 1200 HU）
    public static final int HEAT_CAPACITY = MAX_HU_PER_TICK;
    public static final int MAX_HEAT_RECEIVE = MAX_HU_PER_TICK;
    public static final int MAX_HEAT_EXTRACT = 0;
    public static final int MAX_TEMP = 500;
    public static final float HEAT_LOSS_FACTOR = 0.0f;

    // 流体存储
    protected final FluidTank waterTank;
    protected final FluidTank steamTank;
    private final IFluidHandler combinedFluidHandler;

    // 状态
private int calcification = 0;
private boolean isWorking = false;
// 过热爆炸只能触发一次（每个 tick 内 heatup 可能被调用多次）
private boolean overheated = false;

    // 原版 IC2 核心参数
    private float systemHeat = 20.0F; // 系统热量/温度（从环境温度开始）
    private int pressure = 0;         // 压力阀 0-300
    private int inputMB = 0;          // 水流量设

    // 输出类型记录（供 GUI 显示
@SuppressWarnings("null")
public enum OutputType { NONE, WATER, DISTILLEDWATER, STEAM, SUPERHEATEDSTEAM }
    private OutputType outputFluid = OutputType.NONE;
    private int outputMB = 0;

    // 上一 tick 的输入输出速率，供 GUI 显示（IC2 风格
private int lastWaterInput = 0;   // mB/t
    private int lastSteamOutput = 0;  // mB/t
    private long lastHeatInput = 0;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> waterTank.getFluidAmount();
                case 1 -> waterTank.getCapacity();
                case 2 -> calcification;
                case 3 -> MAX_CALCIFICATION;
                case 4 -> (int) Math.min(lastHeatInput, Integer.MAX_VALUE);
                case 5 -> Math.round(systemHeat * 10.0F);
                case 6 -> pressure;
                case 7 -> inputMB;
                case 8 -> outputMB;
                case 9 -> gaugeHeatScaled(76);
                case 10 -> gaugeCalcificationScaled(58);
                case 11 -> gaugeLiquidScaled(47, 0);
                // 水槽实际流体（原版 IC2 的 TankGauge 会显示罐内真实流体，而不是固定用水材质）
                case 12 -> waterTank.isEmpty() ? 0
                    : net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(waterTank.getFluid().getFluid());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() { return 13; }
    };

    public mio_icif_steam_generator(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.STEAM_GENERATOR_ENTITY_TYPE.get());
    }

    public mio_icif_steam_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type, pos, state, HEAT_CAPACITY, MAX_HEAT_RECEIVE, MAX_HEAT_EXTRACT,
              20, MAX_TEMP, HEAT_LOSS_FACTOR);

        this.waterTank = new FluidTank(WATER_TANK_CAPACITY, fluidStack -> {
            if (fluidStack.isEmpty()) return true;
            return fluidStack.getFluid() == Fluids.WATER || fluidStack.getFluid() == mio_icif_fluids.DISTILLEDWATER.get();
        });

        this.steamTank = new FluidTank(STEAM_TANK_CAPACITY, fluidStack -> {
            if (fluidStack.isEmpty()) return true;
            return fluidStack.getFluid() == mio_icif_fluids.STEAM.get()
                || fluidStack.getFluid() == mio_icif_fluids.SUPERHEATEDSTEAM.get();
        });
        this.combinedFluidHandler = com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup
            .inputOutput(waterTank, steamTank);

        // 初始化槽位布局和物品处理器
        this.slotLayout = LAYOUT;
        this.itemHandler = createItemHandler(LAYOUT);
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_steam_generator.this.isItemValidForSlot(slot, stack));
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        // 雪藏：原版 IC2 蒸汽机没有任何物品槽，关闭后不再接受任何物品
        // （槽位定义与 NBT 保留以兼容旧存档与既有管道）。
        if (!AUX_SLOTS_ENABLED) return false;

        if (isUpgradeSlot(slot)) {
            return stack.getItem() instanceof com.miophas.singularity_iteration.common.item.upgrade.mio_icif_upgrade;
        }
        return switch (slot) {
            case WATER_BUCKET_SLOT -> isWaterContainer(stack);
            case EMPTY_BUCKET_SLOT -> false;
            default -> false;
        };
    }

    /**
     * 水槽输入容器的判定（对齐 IC2：容器不分类别，只看储罐能否接收）。
     * 接受水桶、蒸馏水桶，以及装有水/蒸馏水的流体单元（静态或动态、允许部分填充）。
     */
    private static boolean isWaterContainer(ItemStack stack) {
        if (stack.is(Items.WATER_BUCKET) || stack.is(mio_icif_fluids.DISTILLEDWATER_BUCKET.get())) {
            return true;
        }
        FluidStack cellFluid = mio_icif_cells.getCellFluid(stack);
        return !cellFluid.isEmpty()
            && (cellFluid.getFluid() == Fluids.WATER || cellFluid.getFluid() == mio_icif_fluids.DISTILLEDWATER.get());
    }

    /**
     * 
tick 更新逻辑（原
IC2 风格
 */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_steam_generator blockEntity) {
        if (level.isClientSide()) return;

        // 原版 IC2（L67）：systemHeat 每 tick 的下限 = 生物群系温度
        blockEntity.systemHeat = Math.max(blockEntity.systemHeat, blockEntity.biomeTemperature());

        boolean calcified = blockEntity.isCalcified();

        // 原版 IC2：钙化时 work() 不会被调用，因此也不会向邻居索取热量（本 tick 不取热）
        if (!calcified) {
            blockEntity.receiveHeatFromSides();
        }

        // 雪藏：原版 IC2 蒸汽机没有物品槽、也没有内部蒸汽槽（容量已雪藏为 0），
        // 水桶槽处理与相邻蒸汽推送均不再执行；槽位与 NBT 保留以兼容旧存档。
        if (AUX_SLOTS_ENABLED) {
            blockEntity.handleWaterBucketSlot();
        }
        // blockEntity.outputStoredSteamToNeighbors(); // 雪藏：内部蒸汽槽容量为 0，恒为空

        boolean wasWorking = blockEntity.isWorking;

        if (calcified) {
            // 原版 IC2：钙化时只停机，冷却由下面统一的 !isWorking 分支处理（避免双重冷却）
            blockEntity.isWorking = false;
        } else {
            blockEntity.isWorking = blockEntity.work();
            // 原版 IC2 没有 HU 缓冲：本 tick 没用完的热量统一注入系统热量，不跨 tick 留存
            long leftoverHeat = blockEntity.heatStorage.getHeatStored();
            if (leftoverHeat > 0) {
                blockEntity.heatStorage.consumeHeatInternal(leftoverHeat, false);
                blockEntity.heatup((int) leftoverHeat);
            }
        }

        if (!blockEntity.isWorking) {
            blockEntity.cooldown(COOLING_PER_TICK);
        }

        if (wasWorking != blockEntity.isWorking) {
            blockEntity.updateBlockState(blockEntity.isWorking);
        }

        // 雪藏：IC2 蒸汽机没有升级槽，自动化升级（弹出/抽取/流体搬移）不再执行
        if (AUX_SLOTS_ENABLED) {
            blockEntity.handleAutomationUpgrades();
        }
        blockEntity.setChanged();
    }

    /** IC2 requestHeat: inspect all six neighbours; each heat source restricts its output face. */
    private void receiveHeatFromSides() {
        if (level == null || level.isClientSide()) return;

        lastHeatInput = 0;
        long heatNeeded = HEAT_CAPACITY - heatStorage.getHeatStored();
        if (heatNeeded <= 0) return;

        // 原版 IC2（TileEntitySteamGenerator#requestHeat）：遍历全部 6 面找热源，不排除正面
        for (Direction direction : Direction.values()) {
            if (heatNeeded <= 0) return;

            BlockPos neighborPos = worldPosition.relative(direction);
            IMioIcifCapabilities.IHeatStorage adjacentHeat = level.getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, neighborPos, direction.getOpposite());

            if (adjacentHeat != null && adjacentHeat.canExtractHeat()) {
                long heatToExtract = Math.min(heatNeeded, adjacentHeat.getHeatStored());
                long extracted = adjacentHeat.extractHeat(heatToExtract, false);
                if (extracted > 0) {
                    heatStorage.receiveHeat(extracted, false);
                    lastHeatInput += extracted;
                    heatNeeded -= extracted;
                }
            }
        }
    }

    /**
     * 处理水槽输入容器的物品槽（水桶 / 蒸馏水桶 / 水与蒸馏水单元，
     * 对齐 IC2：容器种类不限，只按储罐实际可接收量全有或全无地灌入）。
     */
    private void handleWaterBucketSlot() {
        ItemStack inputStack = itemHandler.getStackInSlot(WATER_BUCKET_SLOT);
        if (inputStack.isEmpty()) return;

        Fluid fluidToFill;
        int amount;
        ItemStack emptyContainer;

        if (inputStack.is(Items.WATER_BUCKET)) {
            fluidToFill = Fluids.WATER;
            amount = FluidType.BUCKET_VOLUME;
            emptyContainer = new ItemStack(Items.BUCKET);
        } else if (inputStack.is(mio_icif_fluids.DISTILLEDWATER_BUCKET.get())) {
            fluidToFill = mio_icif_fluids.DISTILLEDWATER.get();
            amount = FluidType.BUCKET_VOLUME;
            emptyContainer = new ItemStack(Items.BUCKET);
        } else {
            FluidStack cellFluid = mio_icif_cells.getCellFluid(inputStack);
            if (cellFluid.isEmpty()) return;
            fluidToFill = cellFluid.getFluid();
            amount = Math.min(cellFluid.getAmount(), FluidType.BUCKET_VOLUME);
            emptyContainer = mio_icif_cells.getEmptyCellForStack(inputStack);
        }
        if (amount <= 0 || emptyContainer.isEmpty()) return;
        if (waterTank.getFluidAmount() + amount > waterTank.getCapacity()) return;

        ItemStack emptyStack = itemHandler.getStackInSlot(EMPTY_BUCKET_SLOT);
        if (!emptyStack.isEmpty()
                && (!emptyStack.is(emptyContainer.getItem()) || emptyStack.getCount() >= emptyStack.getMaxStackSize())) {
            return;
        }

        FluidStack toFill = new FluidStack(fluidToFill, amount);
        if (waterTank.fill(toFill, IFluidHandler.FluidAction.SIMULATE) < amount) return;
        waterTank.fill(toFill, IFluidHandler.FluidAction.EXECUTE);

        itemHandler.extractItem(WATER_BUCKET_SLOT, 1, false);
        if (emptyStack.isEmpty()) {
            itemHandler.setStackInSlot(EMPTY_BUCKET_SLOT, emptyContainer.copyWithCount(1));
        } else {
            emptyStack.grow(1);
        }
        setChanged();
    }

    /** IC2 steam cycle: account for each received HU once, including fractional steam costs. */
    private boolean work() {
        lastWaterInput = 0;
        lastSteamOutput = 0;
        outputMB = 0;
        outputFluid = OutputType.NONE;

        // 原版 IC2（TileEntitySteamGenerator#work L83-85）：requestHeat(1200) 取不到热量
        // → 直接停机，既不产汽也不积热。热量缓冲不会跨 tick 留存（见 tick 末尾的统一注入）。
        if (heatStorage.getHeatStored() <= 0) {
            lastHeatInput = 0;
            return false;
        }

        // 没有水或流量
    if (waterTank.getFluidAmount() <= 0 || inputMB <= 0) {
            // 原版 IC2（L91-94）：只把本 tick 抽到的 HU 全部注入 systemHeat，仍算“工作”
            long stored = heatStorage.getHeatStored();
            if (stored > 0) {
                heatStorage.consumeHeatInternal(stored, false);
                lastHeatInput = stored;
                heatup((int) stored);
            } else {
                lastHeatInput = 0;
            }
            return true;
        }

        Fluid inputFluid = waterTank.getFluid().getFluid();
        boolean hasDistilledWater = inputFluid == mio_icif_fluids.DISTILLEDWATER.get();
        int maxAmount = Math.min(inputMB, waterTank.getFluidAmount());

        // 压力=0 且温
        if (pressure == 0 && systemHeat < 99.9999F) {
            lastWaterInput = maxAmount;
            outputMB = maxAmount;
            outputFluid = hasDistilledWater ? OutputType.DISTILLEDWATER : OutputType.WATER;
            int transferred = outputFluidToNeighbors(inputFluid, maxAmount);
            if (transferred > 0) {
                waterTank.drain(transferred, IFluidHandler.FluidAction.EXECUTE);
            }
            // 仍需要加
        // 原版 IC2（L108-121）：热量不足以烧开时，把本 tick 抽到的 HU 全部注入系统热量
            // （不再限流到 500 HU/tick，也不再在 90℃ 提前停止加热）
            long heatingHu = heatStorage.getHeatStored();
            if (heatingHu > 0) {
                heatStorage.consumeHeatInternal(heatingHu, false);
                lastHeatInput = heatingHu;
                heatup((int) heatingHu);
            } else {
                lastHeatInput = 0;
            }
            return true;
        }

        float hUneeded = 100.0F + pressure / 220.0F * 100.0F;
        float targetTemp = 100.0F + pressure / 220.0F * 100.0F * 2.74F;
        float reqHeat = targetTemp - systemHeat;

        // === 先计算实际需要的 HU ===
        int huForHeating = 0;
        if (reqHeat > EPSILON) {
            huForHeating = (int) Math.ceil(reqHeat / HEAT_PER_HU);
        }

        // 原版 IC2（L83-85）：每 tick 无条件索取至多 1200 HU（有多少要多少，不做"按需"限流），
        // Debit the whole received batch exactly once. Remaining HU below is a local
        // balance, not heat left in storage for tick() to inject a second time.
        long heatInput = heatStorage.consumeHeatInternal(Math.min(heatStorage.getHeatStored(), MAX_HU_PER_TICK), false);

        if (heatInput <= 0) {
            lastHeatInput = 0;
            return false;
        }

        lastHeatInput = heatInput;

        float remainingHu = heatInput;

        if (huForHeating > 0 && remainingHu > 0) {
            if (heatInput <= huForHeating) {
                heatup(heatInput);
                return true;
            }
            heatup(huForHeating);
            remainingHu -= huForHeating;
            reqHeat = targetTemp - systemHeat;
        }

        // === 用剩
        // 原版 IC2（L136-138）：系统热可预支 availableSystemHu（可能为负，从而减少产汽量）
        float availableSystemHu = Math.min(-reqHeat / HEAT_PER_HU, (float) (MAX_HU_PER_TICK - heatInput));
        int activeAmount = Math.min(maxAmount, (int) ((remainingHu + availableSystemHu) / hUneeded));
        if (activeAmount < 0) activeAmount = 0;
        if (activeAmount > 0) {
            // IC2 keeps the fractional HU cost (221 bar is not an integer HU/mB).
            remainingHu -= activeAmount * hUneeded;
            // 原版 IC2（L146-148）：抵扣后为负 → 不足部分由系统热支付，随即冷却
            if (remainingHu < 0) {
                cooldown(-remainingHu * HEAT_PER_HU);
                reqHeat = targetTemp - systemHeat;
            }
        }

        // === 如果温度高于目标，用冷却水生产蒸汽来降温 ===
        int coolingAmount = 0;
        if (reqHeat <= -0.1001F) {
            coolingAmount = Math.min(maxAmount, (int) (-reqHeat / 0.1F));
            coolingAmount = Math.min(coolingAmount, 20);
            // 原版 IC2（L157-158）：降温消耗的水同时直接冷却系统热
            cooldown(coolingAmount * 0.1F);
        }

        // 原版 IC2（L164-165）：没用完的 HU 注入系统热
        if (remainingHu > 0) {
            heatup(remainingHu);
        }

        int totalAmount = Math.max(activeAmount, coolingAmount);
        // 原版 IC2（L169）：totalAmount <= 0 直接 return，return 前不再降温
        // （该降温已由上面的 coolingAmount 分支按 L157-158 做过）。
        if (totalAmount <= 0) return true;

        // 钙化（普通水
    if (!hasDistilledWater) {
            calcification += totalAmount;
        }

        waterTank.drain(totalAmount, IFluidHandler.FluidAction.EXECUTE);
        lastWaterInput = totalAmount;

        // 温度高于目标时，这部分降温消耗的水不计入蒸汽产量
        // 原版 IC2（L181）：直接 return，不再额外降温。
        if (activeAmount <= 0) return true;

        outputMB = activeAmount * STEAM_EXPANSION;

        Fluid outputFluidType;
        if (systemHeat >= 373.9999F) {
            outputFluidType = mio_icif_fluids.SUPERHEATEDSTEAM.get();
            outputFluid = OutputType.SUPERHEATEDSTEAM;
        } else {
            outputFluidType = mio_icif_fluids.STEAM.get();
            outputFluid = OutputType.STEAM;
        }

        // 1. 先填内部蒸汽
    FluidStack steamStack = new FluidStack(outputFluidType, outputMB);
        int filledInternal = steamTank.fill(steamStack, IFluidHandler.FluidAction.EXECUTE);
        int remainingOutput = outputMB - filledInternal;

        // 2. 内部满了才往外推
        int transferredToNeighbors = 0;
        if (remainingOutput > 0) {
            transferredToNeighbors = outputSteamToNeighbors(outputFluidType, remainingOutput);
            remainingOutput -= transferredToNeighbors;
        }

        // 3. 内外都满了才处理溢出
        if (remainingOutput > 0) {
            if (level.random.nextInt(10) == 0) {
                // 原版 IC2（L199-201）：余汽溢出时低强度热爆炸
                level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 1.0F, Level.ExplosionInteraction.NONE);
            } else if (remainingOutput >= STEAM_EXPANSION) {
                waterTank.fill(new FluidStack(inputFluid, remainingOutput / STEAM_EXPANSION), IFluidHandler.FluidAction.EXECUTE);
            }
        }

        lastSteamOutput = filledInternal + transferredToNeighbors;
        return true;
    }

    private void heatup(float huInput) {
        if (huInput < 0) return;
        systemHeat += huInput * HEAT_PER_HU;
        // 对齐原版 IC2（TileEntitySteamGenerator L210-220）：
        //   world.setBlockToAir(pos) + ExplosionIC2(power=10, dropRate=0.01F, Type.Heat)
        // 用 vanilla 的等价热爆实现：ExplosionInteraction.NONE = 不破坏方块、仅伤害实体，
        // 本机则显式 removeBlock 摧毁。overheated 旗标避免同一 tick 内多次 heatup 重复引爆。
        if (systemHeat > MAX_SYSTEM_HEAT && level != null && !overheated) {
            overheated = true;
            level.removeBlock(worldPosition, false);
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    10.0F, Level.ExplosionInteraction.NONE);
            systemHeat = MAX_SYSTEM_HEAT;
        }
    }

    private void cooldown(float cool) {
        if (cool < 0) return;
        // 对齐原版 IC2 BiomeUtil.getBiomeTemperature：温度下限 = 生物群系温度
        systemHeat = Math.max(systemHeat - cool, biomeTemperature());
    }

    /** 对齐原版 IC2 BiomeUtil.getBiomeTemperature：炎热生物群系 45、寒冷 0、其余 25。 */
    private int biomeTemperature() {
        if (level == null) return 25;
        float base = level.getBiome(worldPosition).value().getBaseTemperature();
        if (base >= 1.0F) return 45;
        if (base <= 0.15F) return 0;
        return 25;
    }

    private boolean isCalcified() {
        return calcification >= MAX_CALCIFICATION;
    }

    /**
     * 将指定流体输出到相邻容器（尝试所
6 个方向，原版 IC2 风格），返回实际传输
 */
    private int outputFluidToNeighbors(Fluid fluid, int amount) {
        if (level == null || amount <= 0) return 0;
        int totalTransferred = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            IFluidHandler neighborHandler = level.getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                neighborPos, dir.getOpposite());
            if (neighborHandler != null) {
                // 原版 IC2（LiquidUtil.distribute）：把剩余量一次性全部分发给该邻居，
                // 不做每面 1000 mB/tick 的人为限流（接收量只由邻居自己的罐容量决定）
                FluidStack toDrain = new FluidStack(fluid, amount - totalTransferred);
                int filled = neighborHandler.fill(toDrain, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    totalTransferred += filled;
                    if (totalTransferred >= amount) return totalTransferred;
                }
            }
        }
        return totalTransferred;
    }

    /**
     * 将蒸汽输出到相邻流体容器，返回实际传输量
     */
    private int outputSteamToNeighbors(Fluid fluid, int amount) {
        return outputFluidToNeighbors(fluid, amount);
    }

    /**
     * 
tick 将内部蒸汽槽的蒸汽推送给相邻容器
     */
    private void outputStoredSteamToNeighbors() {
        if (level == null || level.isClientSide()) return;
        if (steamTank.getFluidAmount() <= 0) return;

        FluidStack available = steamTank.getFluid();
        if (available.isEmpty()) return;

        int toPush = Math.min(available.getAmount(), 1000); // 
        if (toPush <= 0) return;

        int transferred = outputSteamToNeighbors(available.getFluid(), toPush);
        if (transferred > 0) {
            steamTank.drain(transferred, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void updateBlockState(boolean working) {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (state.hasProperty(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_steam_generator.LIT)) {
            level.setBlock(worldPosition, state.setValue(
                com.miophas.singularity_iteration.common.block.producer.mio_icif_block_steam_generator.LIT, working), 3);
        }
    }

    // ==================== Capability ====================

    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return itemHandler;
    }

    @Override
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return combinedFluidHandler;
    }

    @Override
    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        if (side == null) return heatStorage;
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        if (side == facing) return null;
        return heatStorage;
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("waterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.put("steamTank", steamTank.writeToNBT(registries, new CompoundTag()));
        tag.put("items", itemHandler.serializeNBT(registries));
        tag.putInt("calcification", calcification);
        tag.putBoolean("isWorking", isWorking);
        tag.putFloat("systemHeat", systemHeat);
        tag.putInt("pressure", pressure);
        tag.putInt("inputMB", inputMB);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("waterTank")) waterTank.readFromNBT(registries, tag.getCompound("waterTank"));
        if (tag.contains("steamTank")) steamTank.readFromNBT(registries, tag.getCompound("steamTank"));
        if (tag.contains("items")) itemHandler.deserializeNBT(registries, tag.getCompound("items"));
        calcification = tag.getInt("calcification");
        isWorking = tag.getBoolean("isWorking");
        systemHeat = tag.getFloat("systemHeat");
        pressure = tag.getInt("pressure");
        inputMB = tag.getInt("inputMB");
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.steam_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.producer.SteamGeneratorMenu(containerId, playerInventory, this);
    }

    // ==================== Getters & Setters ====================

    public FluidTank getWaterTank() { return waterTank; }
    public FluidTank getSteamTank() { return steamTank; }
    /** 合并后的流体端口（水槽只进 / 蒸汽槽只出），供右击容器交互使用。 */
    public IFluidHandler getCombinedFluidHandler() { return combinedFluidHandler; }
    public ItemStackHandler getItemHandler() { return itemHandler; }
    public ContainerData getContainerData() { return containerData; }
    public int getCalcification() { return calcification; }
    public int getMaxCalcification() { return MAX_CALCIFICATION; }
    public boolean isWorking() { return isWorking; }
    public int getPressure() { return pressure; }
    public int getInputMB() { return inputMB; }
    public int getOutputMB() { return outputMB; }

    // 上一 tick 输入输出速率（供 GUI 显示
public int getLastWaterInput() { return lastWaterInput; }
    public int getLastSteamOutput() { return lastSteamOutput; }
    public long getLastHeatInput() { return lastHeatInput; }

    /**
     * 系统热量温度（供 GUI 显示，IC2 风格，保
1 位小数）
     */
    public float getSystemHeat() { return Math.round(systemHeat * 10.0F) / 10.0F; }

    /**
     * 钙化百分比（
GUI 显示，保
2 位小数）
     */
    public float getCalcificationPercent() {
        return Math.round(calcification * 100.0F / MAX_CALCIFICATION * 100.0F) / 100.0F;
    }

    /**
     * 热量条缩放（原版 IC2 风格
 */
    public int gaugeHeatScaled(int i) {
        return (int)(i * systemHeat / MAX_SYSTEM_HEAT);
    }

    /**
     * 钙化条缩放（原版 IC2 风格
 */
    public int gaugeCalcificationScaled(int i) {
        return i * calcification / MAX_CALCIFICATION;
    }

    /**
     * 液体条缩放（原版 IC2 风格
 */
    public int gaugeLiquidScaled(int i, int tank) {
        if (tank == 0) {
            if (waterTank.getFluidAmount() <= 0) return 0;
            return waterTank.getFluidAmount() * i / waterTank.getCapacity();
        }
        return 0;
    }

    /**
     * 处理 GUI 按钮事件（原
IC2 压力/流量调节
 */
    public void onButtonEvent(int eventId) {
        if (eventId >= 2000) {
            // 左侧：压力调
        int delta = eventId - 2000;
            pressure = Math.min(300, Math.max(0, pressure + delta));
        } else if (eventId <= -2000) {
            // 左侧：压力减
        int delta = -(eventId + 2000);
            pressure = Math.min(300, Math.max(0, pressure - delta));
        } else if (eventId > 0) {
            // 右侧：流量增
        inputMB = Math.min(1000, inputMB + eventId);
        } else if (eventId < 0) {
            // 右侧：流量减
        inputMB = Math.max(0, inputMB + eventId);
        }
        setChanged();
    }

    /**
     * 当前输出流体名称的翻译键（原
IC2 风格，根
outputtyp
 */
    public String getOutputFluidTranslationKey() {
        switch (outputFluid) {
            case WATER: return "gui.mio_icif.steam_generator.output.water";
            case DISTILLEDWATER: return "gui.mio_icif.steam_generator.output.destiwater";
            case STEAM: return "gui.mio_icif.steam_generator.output.steam";
            case SUPERHEATEDSTEAM: return "gui.mio_icif.steam_generator.output.hotsteam";
            default: return "";
        }
    }

    /**
     * 组合流体处理
 */
}

package com.miophas.singularity_iteration.common.blockentity.huentity.hugenerator;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.machine.IHeatGeneratorBlock;
import com.miophas.singularity_iteration.core.api.machine.IBurnControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 流体热源
 * 用热交换器把岩浆/热冷却剂转换为冷却剂/绳状熔岩，并输出 HU。
 *
 * 规则：
 * - 两个流体槽：输入槽（岩浆/热冷却剂）与输出槽（冷却剂/绳状熔岩）
 * - 10 个热交换器槽位，每个提供 10 HU/t，上限 100 HU/t
 * - 1000 mB 热冷却剂 → 20k HU + 冷却剂
 * - 1000 mB 岩浆 → 20k HU + 绳状熔岩
 * - 热量只从正面被抽取（对齐 IC2 的 facingMatchesDirection）；无人抽取时兜底推给正面邻居
 * - 热缓冲上限 = 一 tick 产热（与原版 IC2 一致，无过热爆炸）
 */
@SuppressWarnings("null")
public class mio_icif_heat_source_fluid extends com.miophas.singularity_iteration.core.prefab.blockentity.AbstractHeatBlockEntity implements WorldlyContainer, IHeatGeneratorBlock, IBurnControl {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)            // 0 热流体容器输入（IC2: hotfluidinputSlot, TOP）
        .output(1)           // 1 空容器输出（IC2: hotoutputSlot）
        .input(1)            // 2 冷流体容器输入（IC2: coolfluidinputSlot, BOTTOM）
        .output(1)           // 3 满容器输出（IC2: cooloutputSlot）
        .heatConductor(10)   // 4-13 热交换器
        .upgrade(3)          // 14-16 升级槽（对齐原版 IC2 的 3 个升级槽）
        .build();

    // 槽位索引按 builder 调用顺序：INPUT=0, OUTPUT=1, INPUT=2, OUTPUT=3, HEAT_CONDUCTOR=4-13, UPGRADE=14-16
    public static final int INPUT_FLUID_BUCKET_SLOT = 0;    // 热流体容器输入槽（INPUT）
    public static final int INPUT_EMPTY_BUCKET_SLOT = 1;    // 空容器输出槽（OUTPUT）
    public static final int OUTPUT_FLUID_BUCKET_SLOT = 2;   // 冷流体容器输入槽（INPUT）
    public static final int OUTPUT_FULL_BUCKET_SLOT = 3;    // 满容器输出槽（OUTPUT）
    public static final int HEAT_CONDUCTOR_START = 4;       // 热交换器槽起始（EXTRA）
    public static final int HEAT_CONDUCTOR_COUNT = 10;      // 热交换器槽数量
    // 槽位 14-16：对齐原版 IC2 的 3 个升级槽（IC2: upgradeSlot = new InvSlotUpgrade(this, "upgrade", 3)）
    public static final int UPGRADE_SLOT_START = 14;
    public static final int UPGRADE_SLOT_COUNT = 3;
    /** @deprecated 旧命名，等价于 {@link #UPGRADE_SLOT_START} */
    @Deprecated
    public static final int EXTRA_SLOT_START = UPGRADE_SLOT_START;
    /** @deprecated 旧命名，等价于 {@link #UPGRADE_SLOT_COUNT} */
    @Deprecated
    public static final int EXTRA_SLOT_COUNT = UPGRADE_SLOT_COUNT;
    public static final int TOTAL_SLOTS = 17;               // 槽位总数

    // 热量设置（对齐原版 IC2 液体热交换机）
    public static final int HEAT_PER_CONDUCTOR = 10;        // 每个热交换器提供 10 HU/t
    public static final int MAX_HEAT_OUTPUT = 100;          // 最大输出 100 HU/t（10 个热传导器）
    /**
     * 存储上限（纯粹是天花板，防止旧存档里的值无限增长）。
     * IC2 真正用的是"每 tick 补满到 {@code getMaxHeatEmittedPerTick()} = 热传导器数 × 10"，
     * 见 {@link #processHeatExchange()}。
     */
    public static final int HEAT_CAPACITY = MAX_HEAT_OUTPUT;
    public static final int HU_PER_MB = 20;                 // 20 HU/mB（原版）
    public static final int HU_PER_BUCKET = 20000;          // 每桶产出 20k HU
    public static final int FLUID_PER_OPERATION = 1000;     // 容器一次处理 1000 mB

    // 流体设置
    public static final int FLUID_CAPACITY = 2000;          // 每个槽 2000 mB 容量

    // 物品存储
    // Uses the inherited inventory; no shadow copy.

    // 输入流体槽 - 存放岩浆或热冷却剂
    protected final FluidTank inputTank;

    // 输出流体槽 - 存放冷却剂或绳状熔岩
    protected final FluidTank outputTank;
    private final IFluidHandler combinedFluidHandler;

    // 热量存储直接使用基类的 heatStorage

    // 当前安装的热交换器数量
    private int conductorCount = 0;

    // 当前热量输出
    private int currentHeatOutput = 0;

    /**
     * IC2 {@code TileEntityHeatSourceInventory#transmitHeat}：最近一次**实际被抽走**的 HU。
     * 只在非模拟抽取时更新，没有被抽取时保持上一次的值 —— 原版 GUI 显示的正是它
     * （{@code getOutput() = transmitHeat + " / " + getMaxHeatEmittedPerTick()}），
     * 而不是实时缓冲，所以读数稳定；Buffer 本身被抽干再补满会有 0↔上限的方波，属正常现象。
     */
    private int transmitHeat = 0;

    // 是否正在工作
    private boolean isWorking = false;

    /**
     * 本 tick 产热前缓冲里残留的热量（上一 tick 生成、到本 tick 开始时仍没被取走的部分）。
     * 只用来决定是否需要"兜底推送"：>0 说明没有任何抽取方在拿，才推给正面邻居。
     */
    private long carriedHeat = 0;

    /**
     * 构造函数
     */
    public mio_icif_heat_source_fluid(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.HEAT_SOURCE_FLUID.get(), pos, state, HEAT_CAPACITY, 0, MAX_HEAT_OUTPUT, 20, 1000, 0);

        // 初始化槽位布局与物品处理器
        this.slotLayout = LAYOUT;
        this.itemHandler = createItemHandler(LAYOUT);
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_heat_source_fluid.this.isItemValidForSlot(slot, stack));

        // 输入流体槽 - 只接受岩浆或热冷却剂
        this.inputTank = new FluidTank(FLUID_CAPACITY, fluidStack ->
            fluidStack.getFluid() == Fluids.LAVA ||
            fluidStack.getFluid() == mio_icif_fluids.HOTCOOLANT.get()) {
                @Override protected void onContentsChanged() { com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank.markUnsaved(mio_icif_heat_source_fluid.this); }
            };

        // 输出流体槽 —— IC2 用 addTankExtract 创建，**没有任何流体谓词**（只出不进由罐角色保证，
        // 外部灌入被 FluidTankGroup 的 OUTPUT_ONLY 结构性挡住）。这里同样不加过滤，
        // 若罐里出现非产物流体，processHeatExchange 会按 IC2 fillHeatBuffer 的
        // "输出罐流体种类不符 → 不工作" 处理。
        this.outputTank = new FluidTank(FLUID_CAPACITY) {
                @Override protected void onContentsChanged() { com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank.markUnsaved(mio_icif_heat_source_fluid.this); }
            };
        this.combinedFluidHandler = com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup
            .inputOutput(inputTank, outputTank);

        // 初始化热交换器数量
        updateConductorCount();
    }

    /**
     * 统计已安装的热交换器数量并计算热量输出
     */
    private void updateConductorCount() {
        int count = 0;
        for (int i = 0; i < HEAT_CONDUCTOR_COUNT; i++) {
            if (!itemHandler.getStackInSlot(HEAT_CONDUCTOR_START + i).isEmpty()) {
                count++;
            }
        }
        this.conductorCount = count;
        this.currentHeatOutput = Math.min(count * HEAT_PER_CONDUCTOR, MAX_HEAT_OUTPUT);
    }

    /**
     * 每 tick 的处理逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_heat_source_fluid blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 热交换器数量可能变化，每 tick 重新统计
        blockEntity.updateConductorCount();

        // 处理输入液体容器
        blockEntity.handleInputFluidSlot();

        // 处理输出液体容器
        blockEntity.handleOutputFluidSlot();

        // 记录产热前的残留量：>0 表示上一 tick 产出的热没人取走（详见 outputHeat）
        blockEntity.carriedHeat = blockEntity.heatStorage.getHeatStored();

        // 每 tick 只把热缓冲补满到"一 tick 的产热量"（差多少补多少），对齐 IC2
        // TileEntityHeatSourceInventory#updateEntityServer；主体热量由用热方从【正面】
        // 抽取（drawHeat / facingMatchesDirection）。
        blockEntity.processHeatExchange();

        // 兜底推送：只覆盖"对面不会来抽"的那一半场景（见 outputHeat 注释）
        blockEntity.outputHeat();

        if (blockEntity.isRemoved()) return;

        // 更新工作状态：IC2 TileEntityLiquidHeatExchanger#updateEntityServer 里是
        //     this.newActive = (this.HeatBuffer > 0);
        boolean wasWorking = blockEntity.isWorking;
        blockEntity.isWorking = blockEntity.heatStorage.getHeatStored() > 0;

        // 工作状态变化时才更新方块状态
        if (wasWorking != blockEntity.isWorking) {
            BlockState newState = state.setValue(
                com.miophas.singularity_iteration.common.block.hugenerator.mio_icif_block_heat_source_fluid.ACTIVE,
                blockEntity.isWorking);
            level.setBlock(pos, newState, 3);
        }

        // 升级槽生效（对齐原版 IC2 LHE 的 upgradeSlot.tick()）
        blockEntity.handleAutomationUpgrades();

        blockEntity.setChanged();
    }

    /**
     * 正面相邻方块的机器是否可以接收热量
     */
    @SuppressWarnings("unused")
    private boolean hasHeatRequest() {
        Direction facing = getBlockState().getValue(
            net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos adjacentPos = worldPosition.relative(facing);

        IMioIcifCapabilities.IHeatStorage adjacentHeat = level.getCapability(
                IMioIcifCapabilities.HEAT_STORAGE_BLOCK, adjacentPos, facing.getOpposite());
        if (adjacentHeat == null) {
            adjacentHeat = MioIcifAPI.instance().getCapabilities().adaptHeatStorage(
                level.getBlockEntity(adjacentPos));
        }

        return adjacentHeat != null && adjacentHeat.canReceiveHeat();
    }

    /**
     * 热交换处理（对齐原版 IC2 液体热交换机）
     *
     * 每 tick 只处理与当前热缓冲剩余空间相当的量：
     *   mbToProcess = bufferspace / 20 HU·mB⁻¹
     * 这样当缓冲只剩 20 空间时一次只换热 1 mB，不会像批量处理那样把多余的热量丢掉。
     */
    private void processHeatExchange() {
        // IC2 TileEntityHeatSourceInventory#updateEntityServer：
        //     int amount = getMaxHeatEmittedPerTick() - this.HeatBuffer;
        //     if (amount > 0) addtoHeatBuffer(fillHeatBuffer(amount));
        // 缓冲上限 = 本 tick 的产热能力（热传导器数 × 10，封顶 100），差多少补多少
        // —— 所以只装 1 个热传导器时缓冲只到 10，产热与耗流体都被限制在 10 HU/t。
        int bufferspace = currentHeatOutput - (int) heatStorage.getHeatStored();
        if (bufferspace <= 0) return;

        // IC2 fillHeatBuffer 开头：输出罐没空间 / 输入罐没料 → 直接不产热
        int amountHotCoolant = inputTank.getFluidAmount();
        int outputTankFreeCap = outputTank.getCapacity() - outputTank.getFluidAmount();
        if (outputTankFreeCap == 0 || amountHotCoolant == 0) return;

        Fluid inputFluid = inputTank.getFluid().getFluid();
        Fluid outputFluidType = inputFluid == Fluids.LAVA ? mio_icif_fluids.PAHOEHOELAVA.get()
            : inputFluid == mio_icif_fluids.HOTCOOLANT.get() ? mio_icif_fluids.COOLANT.get() : null;
        if (outputFluidType == null) return;
        if (!outputTank.isEmpty() && outputTank.getFluid().getFluid() != outputFluidType) return;

        // IC2 fillHeatBuffer 的三个分支（输入罐 / 输出罐空间 / 缓冲空间三者取瓶颈）：
        //     if (OutputTankFreeCap >= AmountHotCoolant)     drain(min(mbtofillheatbuffer, AmountHotCoolant))
        //     else if (mbtofillheatbuffer <= OutputTankFreeCap) drain(mbtofillheatbuffer)
        //     else                                            drain(OutputTankFreeCap * 20)
        // 第三支原版写的是硬编码的 `OutputTankFreeCap * 20`：它一次抽走输出罐空间的 20 倍流体，
        // 产生的 HU 会顶穿 HeatBuffer 上限（addtoHeatBuffer 不做夹取），属原版笔误。
        // 这里取该分支的**意图**（输出罐是瓶颈时按输出空间抽），不复制这个越界行为。
        int mbToFillHeatBuffer = bufferspace / HU_PER_MB;
        int mbToProcess = Math.min(mbToFillHeatBuffer, Math.min(amountHotCoolant, outputTankFreeCap));
        if (mbToProcess <= 0) return;

        if (com.miophas.singularity_iteration.core.runtime.energy.ThermalOutput.enabled()) {
            var result = new FluidStack(outputFluidType, mbToProcess);
            if (com.miophas.singularity_iteration.core.runtime.energy.OwnedHeatExchange.exchange(
                    inputTank, outputTank, heatStorage, mbToProcess, result, (long) mbToProcess * HU_PER_MB)) setChanged();
            return;
        }

        FluidStack drained = inputTank.drain(mbToProcess, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return;
        heatStorage.generateHeatInternal((long) drained.getAmount() * HU_PER_MB, false);
        outputTank.fill(new FluidStack(outputFluidType, drained.getAmount()), IFluidHandler.FluidAction.EXECUTE);
        setChanged();
    }

    /**
     * 兜底推送（原版 IC2 没有，但 SI 的布局需要它补齐另一半场景）。
     * <p>
     * IC2 的用热方分两类：
     * <ul>
     *   <li><b>全向抽取</b>：蒸汽机 {@code requestHeat(1200)} 遍历 6 面；</li>
     *   <li><b>跳过自己正面抽取</b>：动能斯特林 {@code drawHu} 的 {@code if (dir != getFacing())}、
     *       再加压机同理 —— 它们的正面是产出口，热量只能从其余 5 面抽。</li>
     * </ul>
     * 而热源又只从【自己的正面】交热（{@code facingMatchesDirection}）。于是"热源正面 →
     * 用热方正面"这种摆法在 IC2 里本来就抽不到，只能靠布局绕开。这里补一条兜底：
     * 只有当上一 tick 产出的热到本 tick 开始时**仍留在缓冲里**（{@code carriedHeat > 0}，
     * 说明没有任何抽取方来拿）才把热推给正面邻居。
     * <p>
     * 因为门槛是"没人抽"，对全向抽取的用热方（蒸汽机等）完全不触发、零干扰；
     * 只把热交给"对面不会来抽、但自己能收"的机器。
     */
    private void outputHeat() {
        if (carriedHeat <= 0) return;
        if (com.miophas.singularity_iteration.core.runtime.energy.ThermalOutput.enabled()) {
            long moved = com.miophas.singularity_iteration.core.runtime.energy.ThermalOutput.move(
                    heatStorage,
                    com.miophas.singularity_iteration.core.runtime.energy.ThermalOutput.front(this),
                    currentHeatOutput);
            if (moved > 0) {
                // 推出去的也是"实际输出"，一并记账，GUI/Jade 的读数才不会漏掉这一半
                recordDrawnHeat((int) moved);
                setChanged();
            }
            return;
        }
        if (heatStorage.getHeatStored() <= 0 || currentHeatOutput <= 0) return;

        Direction facing = getBlockState().getValue(
            net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos adjacentPos = worldPosition.relative(facing);

        IMioIcifCapabilities.IHeatStorage adjacentHeat = level.getCapability(
                IMioIcifCapabilities.HEAT_STORAGE_BLOCK, adjacentPos, facing.getOpposite());
        if (adjacentHeat == null) {
            adjacentHeat = MioIcifAPI.instance().getCapabilities().adaptHeatStorage(
                level.getBlockEntity(adjacentPos));
        }
        if (adjacentHeat == null || !adjacentHeat.canReceiveHeat()) return;

        long heatToOutput = Math.min(currentHeatOutput,
                Math.min(heatStorage.getHeatStored(),
                adjacentHeat.getMaxHeatStored() - adjacentHeat.getHeatStored()));
        if (heatToOutput <= 0) return;

        adjacentHeat.receiveHeat(heatToOutput, false);
        heatStorage.consumeHeatInternal(heatToOutput, false);
        recordDrawnHeat((int) heatToOutput);
        setChanged();
    }

    /**
     * 过热检查已移除 —— 原版 IC2 的液体热交换机没有过热爆炸。
     * 热缓冲现在等于一 tick 产热，本身也不会长期堆积。
     */
    private void checkOverheat() {
        // intentionally empty
    }

    /**
     * 处理输入液体容器
     */
    private void handleInputFluidSlot() {
        if (com.miophas.singularity_iteration.core.runtime.energy.ThermalOutput.enabled()) {
            var input = itemHandler.getStackInSlot(INPUT_FLUID_BUCKET_SLOT);
            if (input.isEmpty()) return;
            boolean cell = mio_icif_cells.isFluidCell(input);
            var content = cell ? mio_icif_cells.getCellFluid(input.copyWithCount(1))
                : input.is(Items.LAVA_BUCKET) ? new FluidStack(Fluids.LAVA, FLUID_PER_OPERATION) : FluidStack.EMPTY;
            var empty = cell ? mio_icif_cells.getEmptyCellForStack(input.copyWithCount(1)) : new ItemStack(Items.BUCKET);
            if (com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank.transfer(itemHandler, INPUT_FLUID_BUCKET_SLOT, INPUT_EMPTY_BUCKET_SLOT, inputTank, content, empty)) setChanged();
            return;
        }
        ItemStack bucketStack = itemHandler.getStackInSlot(INPUT_FLUID_BUCKET_SLOT);
        if (bucketStack.isEmpty()) {
            return;
        }

        // 输入槽只接受岩浆桶/岩浆单元/热冷却剂单元
        boolean isLavaBucket = bucketStack.is(Items.LAVA_BUCKET);
        boolean isLavaCell = mio_icif_cells.isCellContainingFluid(bucketStack, Fluids.LAVA);
        boolean isHotCoolantCell = mio_icif_cells.isCellContainingFluid(bucketStack, mio_icif_fluids.HOTCOOLANT.get());

        if (!isLavaBucket && !isLavaCell && !isHotCoolantCell) {
            return;
        }

        // 检查输入槽是否还有足够空间容纳一整桶（1000 mB）
        int remainingSpace = inputTank.getCapacity() - inputTank.getFluidAmount();
        if (remainingSpace < FLUID_PER_OPERATION) {
            return;
        }

        // 检查输入槽当前的流体类型
        FluidStack currentFluid = inputTank.getFluid();
        if (!currentFluid.isEmpty()) {
            boolean currentIsLava = currentFluid.getFluid() == Fluids.LAVA;
            boolean currentIsHotCoolant = currentFluid.getFluid() == mio_icif_fluids.HOTCOOLANT.get();
            
            // 当前是岩浆时，不再添加热冷却剂
            if (currentIsLava && isHotCoolantCell) {
                return;
            }
            // 当前是热冷却剂时，不再添加岩浆
            if (currentIsHotCoolant && (isLavaBucket || isLavaCell)) {
                return;
            }
        }
        
        // 检查输出槽的流体类型，避免输出槽混装
        FluidStack outputFluid = outputTank.getFluid();
        if (!outputFluid.isEmpty()) {
            boolean outputIsPahoehoeLava = outputFluid.getFluid() == mio_icif_fluids.PAHOEHOELAVA.get();
            boolean outputIsCoolant = outputFluid.getFluid() == mio_icif_fluids.COOLANT.get();
            
            // 输出槽是绳状熔岩时，不再添加热冷却剂
            if (outputIsPahoehoeLava && isHotCoolantCell) {
                return;
            }
            // 输出槽是冷却剂时，不再添加岩浆
            if (outputIsCoolant && (isLavaBucket || isLavaCell)) {
                return;
            }
        }

        // 判断输入容器类型
        boolean isCell = isLavaCell || isHotCoolantCell;
        ItemStack emptyContainer = isCell ? mio_icif_cells.getEmptyCellForStack(bucketStack) : new ItemStack(Items.BUCKET);
        if (isCell && emptyContainer.isEmpty()) emptyContainer = new ItemStack(mio_icif_cells.CELL_EMPTY.get());

        // 检查空容器输出槽
        ItemStack emptyStack = itemHandler.getStackInSlot(INPUT_EMPTY_BUCKET_SLOT);
        if (!emptyStack.isEmpty()) {
            if (!ItemStack.isSameItem(emptyStack, emptyContainer) ||
                emptyStack.getCount() >= emptyStack.getMaxStackSize()) {
                return;
            }
        }

        // 填充输入槽
        if (isLavaBucket || isLavaCell) {
            inputTank.fill(new FluidStack(Fluids.LAVA, FLUID_PER_OPERATION), IFluidHandler.FluidAction.EXECUTE);
        } else {
            inputTank.fill(new FluidStack(mio_icif_fluids.HOTCOOLANT.get(), FLUID_PER_OPERATION), IFluidHandler.FluidAction.EXECUTE);
        }

        // 消耗输入容器
        bucketStack.shrink(1);

        // 把空容器放入空容器输出槽
        if (emptyStack.isEmpty()) {
            itemHandler.setStackInSlot(INPUT_EMPTY_BUCKET_SLOT, emptyContainer);
        } else {
            emptyStack.grow(1);
        }

        setChanged();
    }

    /**
     * 处理输出液体容器
     */
    private void handleOutputFluidSlot() {
        if (com.miophas.singularity_iteration.core.runtime.energy.ThermalOutput.enabled()) {
            var empty = itemHandler.getStackInSlot(OUTPUT_FLUID_BUCKET_SLOT);
            if (!mio_icif_cells.isEmptyCell(empty) || outputTank.isEmpty() || !outputTank.isFluidValid(outputTank.getFluid())) return;
            var full = mio_icif_cells.getFilledCellForFluidStack(outputTank.getFluid().getFluid());
            var content = mio_icif_cells.getCellFluid(full);
            if (com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank.drainToContainer(itemHandler, OUTPUT_FLUID_BUCKET_SLOT, OUTPUT_FULL_BUCKET_SLOT, outputTank, content, full)) setChanged();
            return;
        }
        // 检查输出空容器槽是否有空单元可以填充
        ItemStack emptyStack = itemHandler.getStackInSlot(OUTPUT_FLUID_BUCKET_SLOT);
        if (emptyStack.isEmpty()) {
            return;
        }

        // 检查输出流体槽是否有足够流体
        if (outputTank.getFluidAmount() < FLUID_PER_OPERATION) {
            return;
        }

        // 读取输出流体类型
        FluidStack outputFluid = outputTank.getFluid();
        if (outputFluid.isEmpty()) {
            return;
        }

        boolean isPahoehoeLava = outputFluid.getFluid() == mio_icif_fluids.PAHOEHOELAVA.get();
        boolean isCoolant = outputFluid.getFluid() == mio_icif_fluids.COOLANT.get();

        // 判断输出槽里的空容器类型是否匹配
        boolean isEmptyCell = mio_icif_cells.isEmptyCell(emptyStack);

        ItemStack fullContainer = null;

        if (isPahoehoeLava && isEmptyCell) {
            fullContainer = mio_icif_cells.getFilledCellForFluidStack(mio_icif_fluids.PAHOEHOELAVA.get());
        } else if (isCoolant && isEmptyCell) {
            fullContainer = mio_icif_cells.getFilledCellForFluidStack(mio_icif_fluids.COOLANT.get());
        }

        if (fullContainer == null) {
            return;
        }

        // 检查满容器输出槽
        ItemStack fullStack = itemHandler.getStackInSlot(OUTPUT_FULL_BUCKET_SLOT);
        if (!fullStack.isEmpty()) {
            if (!ItemStack.isSameItem(fullStack, fullContainer) ||
                fullStack.getCount() >= fullStack.getMaxStackSize()) {
                return;
            }
        }

        // 从输出槽抽出流体
        FluidStack drained = outputTank.drain(FLUID_PER_OPERATION, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() < FLUID_PER_OPERATION) {
            return;
        }

        // 消耗空容器
        emptyStack.shrink(1);

        // 把装满的容器放入满容器输出槽
        if (fullStack.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_FULL_BUCKET_SLOT, fullContainer);
        } else {
            fullStack.grow(1);
        }

        setChanged();
    }

    // ==================== 槽位访问 ====================

    /**
     * 判断物品能否放入指定槽位
     */
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == INPUT_FLUID_BUCKET_SLOT) {
            // 输入槽：接受岩浆桶或岩浆/热冷却剂单元
            return stack.is(Items.LAVA_BUCKET) || mio_icif_cells.isCellContainingAnyFluid(stack, Fluids.LAVA, mio_icif_fluids.HOTCOOLANT.get());
        } else if (slot == INPUT_EMPTY_BUCKET_SLOT) {
            // 空容器输出槽：不允许手动放入
            return false;
        } else if (slot == OUTPUT_FLUID_BUCKET_SLOT) {
            // 输出空容器槽：只接受空单元
            return mio_icif_cells.isEmptyCell(stack);
        } else if (slot == OUTPUT_FULL_BUCKET_SLOT) {
            // 满容器输出槽：不允许手动放入
            return false;
        } else if (slot >= HEAT_CONDUCTOR_START && slot < HEAT_CONDUCTOR_START + HEAT_CONDUCTOR_COUNT) {
            // 热交换器槽：只接受热交换器
            return stack.is(mio_icif_resources.HEATCONDUCTOR.get());
        } else if (slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + UPGRADE_SLOT_COUNT) {
            // 升级槽：只接受升级组件（与原版 IC2 的 InvSlotUpgrade 一致）
            return stack.getItem() instanceof com.miophas.singularity_iteration.common.item.upgrade.mio_icif_upgrade;
        }
        return false;
    }

    // ==================== NBT 序列化 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", itemHandler.serializeNBT(registries));
        tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("ConductorCount", conductorCount);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        if (tag.contains("InputTank")) {
            inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
        }
        if (tag.contains("OutputTank")) {
            outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        }
        if (tag.contains("HeatStored")) {
            heatStorage.setHeat(tag.getInt("HeatStored"));
        }
        updateConductorCount();
        currentHeatOutput = Math.min(conductorCount * HEAT_PER_CONDUCTOR, MAX_HEAT_OUTPUT);
    }

    // ==================== 对外查询 ====================

    public long getHeatStored() {
        return heatStorage.getHeatStored();
    }

    /**
     * 容量 = IC2 的热缓冲上限 {@code getMaxHeatEmittedPerTick()}（热传导器数 × 10，封顶 100）。
     * 下限取当前存量：拆掉热传导器时缓冲里可能仍留着旧值，不能让能力快照把它夹掉。
     */
    public long getHeatCapacity() {
        return Math.max(currentHeatOutput, heatStorage.getHeatStored());
    }

    public int getConductorCount() {
        return conductorCount;
    }

    public int getCurrentHeatOutput() {
        return currentHeatOutput;
    }

    /** 最近一次实际被抽走的 HU（IC2 同名字段）；供 GUI / Jade 显示。 */
    public int getTransmitHeat() {
        return transmitHeat;
    }

    /**
     * 记账：外部抽取路径（热能能力的 {@code extractHeat}）成功扣热后调用，
     * 等价于 IC2 {@code drawHeat} 里对 {@code transmitHeat} 的赋值。
     */
    public void recordDrawnHeat(int amount) {
        if (amount > 0) this.transmitHeat = amount;
    }

    /**
     * IC2 {@code TileEntityHeatSourceInventory#drawHeat} 只在 {@code facingMatchesDirection(side)} 时交出热量，
     * 也就是"只有正面能被抽取"。这里用能力面复刻同一限制：非正面返回 null，用热方（蒸汽机等）自然只能从正面接。
     * {@code side == null} 视为内部/显示查询，照常返回。
     */
    @Nullable
    @Override
    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        if (side == null) return this;
        Direction facing = getBlockState().getValue(
            net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        return side == facing ? this : null;
    }

    @Override
    public long extractHeat(long requested, boolean simulate) {
        long extracted = super.extractHeat(Math.min(requested, currentHeatOutput), simulate);
        if (!simulate && extracted > 0) recordDrawnHeat((int) extracted);
        return extracted;
    }

    @Override public long getMaxExtract() { return currentHeatOutput; }
    @Override public boolean canExtractHeat() { return currentHeatOutput > 0 && super.canExtractHeat(); }

    public void consumeHeat(int amount) {
        heatStorage.consumeHeatInternal(amount, false);
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getInputFluidAmount() {
        return inputTank.getFluidAmount();
    }

    public int getInputFluidCapacity() {
        return inputTank.getCapacity();
    }

    public int getOutputFluidAmount() {
        return outputTank.getFluidAmount();
    }

    public int getOutputFluidCapacity() {
        return outputTank.getCapacity();
    }

    public int getInputFluidTypeId() {
        FluidStack fluid = inputTank.getFluid();
        if (fluid.isEmpty()) return -1;
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(fluid.getFluid());
    }

    public int getOutputFluidTypeId() {
        FluidStack fluid = outputTank.getFluid();
        if (fluid.isEmpty()) return -1;
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(fluid.getFluid());
    }

    public boolean isWorking() {
        return isWorking;
    }

    @Nullable
    @Override
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    /** 热缓冲容量固定为「一 tick 产热」（对齐原版 IC2），容量升级不得改动它。 */
    @Override
    protected void applyHeatCapacityUpgrades() {
        // fixed one-tick capacity, no-op
    }

    /**
     * 对齐原版 IC2 的「容器操作面」限制：
     *  - 热流体容器输入槽（0）只能从顶面插入（IC2: hotfluidinputSlot，InvSide.TOP）
     *  - 冷流体容器输入槽（2）只能从底面插入（IC2: coolfluidinputSlot，InvSide.BOTTOM）
     *  - 空容器输出槽（1）/ 满容器输出槽（3）可从任意面抽出（IC2: InvSlotOutput）
     *  - 热交换器槽（4-13）与升级槽（14-16）可从任意面插入（IC2: Access.I）
     */
    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return new SidedItemHandler(side);
    }

    private final class SidedItemHandler implements IItemHandler {
        private final Direction side;

        SidedItemHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return canInsert(slot) && itemHandler.isItemValid(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return canInsert(slot) ? itemHandler.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return canExtract(slot) ? itemHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        private boolean canInsert(int slot) {
            if (slot == INPUT_FLUID_BUCKET_SLOT) return side == null || side == Direction.UP;
            if (slot == OUTPUT_FLUID_BUCKET_SLOT) return side == null || side == Direction.DOWN;
            if (slot == INPUT_EMPTY_BUCKET_SLOT || slot == OUTPUT_FULL_BUCKET_SLOT) return false;
            if (slot >= HEAT_CONDUCTOR_START && slot < HEAT_CONDUCTOR_START + HEAT_CONDUCTOR_COUNT) return true;
            return slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + UPGRADE_SLOT_COUNT;
        }

        private boolean canExtract(int slot) {
            return slot == INPUT_EMPTY_BUCKET_SLOT || slot == OUTPUT_FULL_BUCKET_SLOT;
        }
    }

    @Nullable
    @Override
    protected IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        // 这个端口同时被 AbstractHeatBlockEntity#handleAutomationUpgrades 当作"本机流体"使用：
        //   FluidTankGroup.inputOutput(inputTank, outputTank) ⇒ 抽入进输入罐、弹出从输出罐出。
        // 原先只返回 inputTank，等于让"流体弹出升级"去推【输入罐】（热冷却剂/岩浆），
        // 输出罐里的冷却剂/绳状熔岩永远排不出去，机器就卡在"输出满→停产"。
        // 与蒸汽机（mio_icif_steam_generator）返回 combinedFluidHandler 的做法保持一致。
        return combinedFluidHandler;
    }

    public IFluidHandler getInputTankCapability(@Nullable Direction side) {
        return inputTank;
    }

    public IFluidHandler getOutputTankCapability(@Nullable Direction side) {
        return outputTank;
    }

    // 直接暴露流体槽，供 Jade 等显示
    public FluidTank getInputTank() {
        return inputTank;
    }

    public FluidTank getOutputTank() {
        return outputTank;
    }

    // 组合流体处理器（供 Jade 等显示两个槽位）
    public IFluidHandler getCombinedFluidHandler() {
        return combinedFluidHandler;
    }


    // ==================== WorldlyContainer 接口实现 ====================

    /**
     * 对齐 IC2 的侧向访问规则（{@code InvSlot.InvSide}）：
     * <ul>
     *   <li>热流体容器槽（热冷却剂/岩浆桶进）= {@code InvSide.TOP}；</li>
     *   <li>冷流体容器槽（空容器进、装冷却剂出）= {@code InvSide.BOTTOM}；</li>
     *   <li>热交换器槽、两个输出槽、升级槽 = {@code InvSide.ANY}。</li>
     * </ul>
     */
    private static boolean isSlotAccessibleFrom(int slot, @Nullable Direction side) {
        if (side == null) return true;
        if (slot == INPUT_FLUID_BUCKET_SLOT) return side == Direction.UP;
        if (slot == OUTPUT_FLUID_BUCKET_SLOT) return side == Direction.DOWN;
        return true;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] buffer = new int[TOTAL_SLOTS];
        int count = 0;
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (isSlotAccessibleFrom(i, side)) buffer[count++] = i;
        }
        return java.util.Arrays.copyOf(buffer, count);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return isSlotAccessibleFrom(slot, side) && isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // IC2：两个输出槽是 Access.O（可取），其余槽位只能进不能出
        return isSlotAccessibleFrom(slot, side)
            && (slot == INPUT_EMPTY_BUCKET_SLOT || slot == OUTPUT_FULL_BUCKET_SLOT);
    }

    // ==================== Container 接口实现 ====================

    @Override
    public int getContainerSize() {
        // 返回实际槽位数，与 NBT 载入后的槽位数保持一致
        return itemHandler != null ? itemHandler.getSlots() : TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        int slots = getContainerSize();
        for (int i = 0; i < slots; i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return itemHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (amount <= 0 || slot < 0 || slot >= itemHandler.getSlots()) return ItemStack.EMPTY;
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        itemHandler.setStackInSlot(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return player != null && Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        int slots = getContainerSize();
        for (int i = 0; i < slots; i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    // ==================== MenuProvider ?��?��实现 ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.heat_source_fluid");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.huentity.HeatSourceFluidMenu(
                containerId, playerInventory, this);
    }

    // ==================== IHeatGeneratorBlock ?��?��实现 ====================

    @Override
    public int getHeatOutput() {
        return currentHeatOutput;
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
    public int getHeatGenerationRate() {
        return currentHeatOutput;
    }

    // ==================== IBurnControl ?��?��实现 ====================

    @Override
    public int getDefaultBurnTime() {
        return 0;
    }

    @Override
    public void setBurnTime(int ticks) {
        // Not applicable for heat source fluid
    }
}

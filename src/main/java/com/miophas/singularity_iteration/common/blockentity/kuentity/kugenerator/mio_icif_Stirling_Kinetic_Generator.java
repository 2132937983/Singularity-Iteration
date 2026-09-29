package com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.machine.IBurnControl;
import com.miophas.singularity_iteration.core.api.machine.IKineticGeneratorBlock;
import com.miophas.singularity_iteration.core.api.util.BoundedUnits;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractKineticBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 斯特林动能发生机（对齐原版 IC2 {@code TileEntityStirlingKineticGenerator}）。
 *
 * <p>热力学模型与 IC2 完全一致（IC2 常量 PARTS_KU=3 / PARTS_LIQUID=1 / PARTS_TOTAL=4）：
 * <ul>
 *   <li>从除正面外的五个面吸取 HU，缓冲上限 {@code maxHeatbuffer = 1000}</li>
 *   <li>每 tick 最多使用 {@code heatbuffer / 4} 份，每份消耗 4 HU，产出 12 KU，并累计 1 点液热</li>
 *   <li>液热按 {@code huPerMB = 1} 把 1 mB 水转化为 1 mB 热水（4 HU + 1mB 水 = 1mB 热水 + 12 KU）</li>
 *   <li>KU 缓冲上限 {@code maxkUBuffer = 2000}，缓冲满则停机，不再消耗热与水</li>
 * </ul>
 *
 * <p>动能输出对齐 IC2：仅正面可被抽取（拉取式），带宽 = KU 缓冲上限。
 *
 * <p>槽位对齐 IC2 的 4 流体容器 + 3 升级（共 7 个）：
 * <ul>
 *   <li>0：冷却液输入（水容器 → 输入槽）</li>
 *   <li>1：冷却液输出（空容器）</li>
 *   <li>2-4：升级槽</li>
 *   <li>5：热液输入（空容器 → 输出槽）</li>
 *   <li>6：热液输出（装满热水的容器）</li>
 * </ul>
 */
@SuppressWarnings("null")
public class mio_icif_Stirling_Kinetic_Generator extends AbstractKineticBlockEntity implements IKineticGeneratorBlock, IBurnControl {

    // ==================== IC2 对齐参数 ====================

    /** 流体槽容量（IC2: inputTank / outputTank = 2000）。 */
    public static final int FLUID_CAPACITY = 2000;

    /** 热量缓冲上限（IC2: maxHeatbuffer = 1000）。 */
    public static final int MAX_HEATBUFFER = 1000;

    /** KU 缓冲上限 / 输出带宽（IC2: maxkUBuffer = 2000）。 */
    public static final int MAX_KU_BUFFER = 2000;

    /** IC2: PARTS_KU = 3。 */
    public static final int PARTS_KU = 3;
    /** IC2: PARTS_LIQUID = 1。 */
    public static final int PARTS_LIQUID = 1;
    /** IC2: PARTS_TOTAL = 4。 */
    public static final int PARTS_TOTAL = 4;

    /** 水 → 热水的液热消耗（IC2: huPerMB = 1）。 */
    public static final int HU_PER_MB = 1;

    // 兼容旧常量（数值与 IC2 推导一致）
    public static final int HU_PER_OPERATION = PARTS_TOTAL;          // 4 HU
    public static final int MB_PER_OPERATION = 1;                    // 1 mB
    public static final int KU_PER_OPERATION = PARTS_KU * PARTS_TOTAL; // 12 KU

    // ==================== 槽位（对齐 IC2） ====================

    public static final int COOL_FLUID_INPUT_SLOT = 0;
    public static final int COOL_OUTPUT_SLOT = 1;
    public static final int HOT_FLUID_INPUT_SLOT = 2;
    public static final int HOT_OUTPUT_SLOT = 3;
    public static final int UPGRADE_SLOT_START = 4;
    public static final int UPGRADE_SLOT_COUNT = 3;
    public static final int TOTAL_SLOTS = 7;

    // 对齐 IC2：0 冷却液输入、1 冷却液输出、2 热液输入、3 热液输出、4-6 升级
    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .fluidInput(1)   // 0
        .output(1)       // 1
        .fluidInput(1)   // 2
        .output(1)       // 3
        .upgrade(3)      // 4-6
        .build();

    private static final List<Direction> ALL_DIRECTIONS = List.of(Direction.values());

    // ==================== 状态 ====================

    /** 水（冷却液）槽。 */
    protected final FluidTank waterTank;
    /** 热水槽。 */
    protected final FluidTank hotWaterTank;
    private final IFluidHandler combinedFluidHandler;

    protected final MachineItemHandler itemHandler;

    /** 当前缓冲的热量（HU）。 */
    private int heatBuffer = 0;
    /** 当前缓冲的动能（KU）。 */
    private long kuBuffer = 0;
    /** 已累计、尚未转化为热水的液热（HU·mB 等价）。 */
    private int liquidHeatStored = 0;

    private MachineUpgradeStats upgradeStats = MachineUpgradeStats.empty();
    private boolean active = false;

    // 兼容 GUI/菜单
    public int progress = 0;
    public int maxProgress = 1;

    public mio_icif_Stirling_Kinetic_Generator(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.STIRLING_KINETIC_GENERATOR_ENTITY_TYPE.get());
    }

    public mio_icif_Stirling_Kinetic_Generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type, pos, state, MAX_KU_BUFFER, 0, MAX_KU_BUFFER, 10000, 0.0f);

        // 水槽：对齐 IC2，仅接受普通水（liquidHeatupManager 的单向液热管理器只注册了 water → ic2hotwater）
        this.waterTank = new FluidTank(FLUID_CAPACITY, fluidStack ->
            fluidStack.getFluid() == Fluids.WATER);

        // 热水槽：只接受热水
        this.hotWaterTank = new FluidTank(FLUID_CAPACITY, fluidStack ->
            fluidStack.getFluid() == mio_icif_fluids.HOTWATER.get());
        this.combinedFluidHandler = com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup
            .inputOutput(waterTank, hotWaterTank);

        this.itemHandler = new MachineItemHandler(LAYOUT) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public int getSlotLimit(int slot) {
                // 对齐 IC2：可消耗液体容器槽（冷却液输入 / 热液输入）堆叠上限为 1
                if (slot == COOL_FLUID_INPUT_SLOT || slot == HOT_FLUID_INPUT_SLOT) {
                    return 1;
                }
                return super.getSlotLimit(slot);
            }
        };
        this.itemHandler.setValidator((slot, stack, slotType) ->
            mio_icif_Stirling_Kinetic_Generator.this.isItemValidForSlot(slot, stack));
    }

    // ==================== 槽位校验 ====================

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case COOL_FLUID_INPUT_SLOT -> isValidWaterContainer(stack);
            case HOT_FLUID_INPUT_SLOT -> isValidEmptyContainer(stack);
            default -> false; // OUTPUT / UPGRADE 由 MachineItemHandler 处理
        };
    }

    private boolean isValidEmptyContainer(ItemStack stack) {
        return stack.is(Items.BUCKET) || mio_icif_cells.isEmptyCell(stack);
    }

    private boolean isValidWaterContainer(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET)
            || mio_icif_cells.isCellContainingFluid(stack, Fluids.WATER);
    }

    // ==================== 流体容器处理（对齐 IC2 输入/输出槽语义） ====================

    /**
     * 冷却液输入槽：把容器里的水抽进水槽，空容器进入冷却液输出槽。
     * 对应 IC2 {@code coolfluidinputSlot}(Drain) + {@code cooloutputSlot}。
     */
    private void handleCoolFluidInput() {
        ItemStack input = itemHandler.getStackInSlot(COOL_FLUID_INPUT_SLOT);
        if (input.isEmpty() || !isValidWaterContainer(input)) {
            return;
        }
        if (waterTank.getFluidAmount() + FluidType.BUCKET_VOLUME > waterTank.getCapacity()) {
            return;
        }

        ItemStack emptyContainer;
        if (input.is(Items.WATER_BUCKET)) {
            emptyContainer = new ItemStack(Items.BUCKET);
        } else {
            emptyContainer = mio_icif_cells.getEmptyCellForStack(input);
            if (emptyContainer.isEmpty()) {
                emptyContainer = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
            }
        }
        if (!canAcceptInto(COOL_OUTPUT_SLOT, emptyContainer)) {
            return;
        }

        int filled = waterTank.fill(new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME),
            IFluidHandler.FluidAction.EXECUTE);
        if (filled >= FluidType.BUCKET_VOLUME) {
            itemHandler.extractItem(COOL_FLUID_INPUT_SLOT, 1, false);
            addInto(COOL_OUTPUT_SLOT, emptyContainer);
            setChanged();
        }
    }

    /**
     * 热液输入槽：把热水灌进空容器，装满的容器进入热液输出槽。
     * 对应 IC2 {@code hotfluidinputSlot}(Fill, outputTank) + {@code hotoutputSlot}。
     */
    private void handleHotFluidInput() {
        ItemStack input = itemHandler.getStackInSlot(HOT_FLUID_INPUT_SLOT);
        if (input.isEmpty() || !isValidEmptyContainer(input)) {
            return;
        }
        int containerCapacity = input.is(Items.BUCKET)
            ? FluidType.BUCKET_VOLUME
            : mio_icif_cells.getCellCapacity(input);
        if (containerCapacity <= 0) {
            return;
        }
        // 对齐 IC2 processFromTank：按容器实际容量与罐内存量灌装，允许单元部分填充。
        int amount = Math.min(hotWaterTank.getFluidAmount(), containerCapacity);
        if (amount <= 0) {
            return;
        }
        ItemStack filledContainer;
        if (input.is(Items.BUCKET)) {
            if (amount < FluidType.BUCKET_VOLUME) {
                return; // 原版桶不支持部分填充
            }
            filledContainer = new ItemStack(mio_icif_fluids.HOTWATER_BUCKET.get());
        } else {
            filledContainer = mio_icif_cells.createPartiallyFilledCell(mio_icif_fluids.HOTWATER.get(), amount);
        }
        if (filledContainer.isEmpty() || !canAcceptInto(HOT_OUTPUT_SLOT, filledContainer)) {
            return;
        }

        hotWaterTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        itemHandler.extractItem(HOT_FLUID_INPUT_SLOT, 1, false);
        addInto(HOT_OUTPUT_SLOT, filledContainer);
        setChanged();
    }

    private boolean canAcceptInto(int slot, ItemStack stack) {
        ItemStack existing = itemHandler.getStackInSlot(slot);
        if (existing.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItem(existing, stack)
            && existing.getCount() < itemHandler.getSlotLimit(slot);
    }

    private void addInto(int slot, ItemStack stack) {
        ItemStack existing = itemHandler.getStackInSlot(slot);
        if (existing.isEmpty()) {
            itemHandler.setStackInSlot(slot, stack.copy());
        } else {
            existing.grow(1);
        }
    }

    // ==================== 吸热（对齐 IC2 drawHu） ====================

    /**
     * 从除正面外的各个面吸取热量，直到缓冲上限。
     */
    private void receiveHeatFromSides() {
        if (level == null || level.isClientSide()) {
            return;
        }
        int heatNeeded = MAX_HEATBUFFER - heatBuffer;
        if (heatNeeded <= 0) {
            return;
        }

        Direction facing = getFacing();
        for (Direction direction : Direction.values()) {
            if (direction == facing) {
                continue; // 跳过正面
            }
            heatNeeded = MAX_HEATBUFFER - heatBuffer;
            if (heatNeeded <= 0) {
                break;
            }

            BlockPos neighborPos = worldPosition.relative(direction);
            IMioIcifCapabilities.IHeatStorage heatStorage = level.getCapability(
                IMioIcifCapabilities.HEAT_STORAGE_BLOCK, neighborPos, direction.getOpposite());
            if (heatStorage == null) {
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                heatStorage = MioIcifAPI.instance().getCapabilities().adaptHeatStorage(neighborBe);
            }
            if (heatStorage == null || !heatStorage.canExtractHeat()) {
                continue;
            }

            long heatToExtract = Math.min(heatNeeded, heatStorage.getHeatStored());
            long extracted = heatStorage.extractHeat(heatToExtract, false);
            if (extracted > 0) {
                heatBuffer = Math.min(MAX_HEATBUFFER, heatBuffer + (int) Math.min(extracted, (long) heatNeeded));
                setChanged();
            }
        }
    }

    // ==================== 换热与发电（对齐 IC2 updateEntityServer 核心算法） ====================

    /**
     * IC2 核心换算：
     * <pre>
     * int use = heatbuffer / PARTS_TOTAL;
     * use = min(use, (min(outputRoom, inputAmount) * huPerMB - liquidHeatStored));
     * use = min(use, (maxkUBuffer - kUBuffer) / PARTS_KU);
     * kUBuffer      += use * PARTS_KU * PARTS_TOTAL;
     * liquidHeatStored += use * PARTS_LIQUID;
     * heatbuffer    -= use * PARTS_TOTAL;
     * </pre>
     */
    private boolean convertHeat() {
        this.active = false;

        if (kuBuffer >= MAX_KU_BUFFER) {
            return false; // 缓冲满则停机，避免浪费热与水
        }
        if (inputTank().getFluidAmount() <= 0) {
            return false;
        }
        if (hotWaterTank.getFluidAmount() >= hotWaterTank.getCapacity()) {
            return false;
        }

        int use = heatBuffer / PARTS_TOTAL;

        int liquidRoom = Math.min(hotWaterTank.getCapacity() - hotWaterTank.getFluidAmount(),
                                  inputTank().getFluidAmount()) * HU_PER_MB - liquidHeatStored;
        if (liquidRoom < use) {
            use = Math.max(0, liquidRoom);
        }
        int kuRoom = (int) ((MAX_KU_BUFFER - kuBuffer) / PARTS_KU);
        if (kuRoom < use) {
            use = Math.max(0, kuRoom);
        }

        if (use > 0) {
            kuBuffer += (long) use * PARTS_KU * PARTS_TOTAL;
            liquidHeatStored += use * PARTS_LIQUID;
            heatBuffer -= use * PARTS_TOTAL;
            this.active = true;
        }

        if (liquidHeatStored >= HU_PER_MB) {
            int mb = liquidHeatStored / HU_PER_MB;
            mb = inputTank().drain(mb, IFluidHandler.FluidAction.SIMULATE).getAmount();
            mb = hotWaterTank.fill(new FluidStack(mio_icif_fluids.HOTWATER.get(), mb),
                IFluidHandler.FluidAction.SIMULATE);
            if (mb > 0) {
                liquidHeatStored -= mb * HU_PER_MB;
                inputTank().drain(mb, IFluidHandler.FluidAction.EXECUTE);
                hotWaterTank.fill(new FluidStack(mio_icif_fluids.HOTWATER.get(), mb),
                    IFluidHandler.FluidAction.EXECUTE);
            }
        }
        return this.active;
    }

    private FluidTank inputTank() {
        return waterTank;
    }

    // ==================== 升级 ====================

    private void recalculateUpgradeStats() {
        this.upgradeStats = MachineUpgradeStats.fromInventory(itemHandler, UPGRADE_SLOT_START, UPGRADE_SLOT_COUNT);
    }

    private void handleAutomation() {
        if (upgradeStats.getEjectorCount() > 0) {
            ejectItems(COOL_OUTPUT_SLOT);
            ejectItems(HOT_OUTPUT_SLOT);
        }
        if (upgradeStats.getPullingCount() > 0) {
            pullItems(COOL_FLUID_INPUT_SLOT);
            pullItems(HOT_FLUID_INPUT_SLOT);
        }
        if (upgradeStats.getFluidEjectorCount() > 0) {
            ejectFluids(upgradeStats.getFluidEjectorCount() * 1000);
        }
        if (upgradeStats.getFluidPullingCount() > 0) {
            pullFluids(upgradeStats.getFluidPullingCount() * 1000);
        }
    }

    /** 物品弹出升级：把输出槽的物品推入相邻容器。 */
    private void ejectItems(int slot) {
        if (level == null) {
            return;
        }
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return;
        }
        List<Direction> configured = upgradeStats.getEjectorDirections();
        Iterable<Direction> targets = configured.isEmpty() ? ALL_DIRECTIONS : configured;
        for (Direction dir : targets) {
            BlockPos pos = worldPosition.relative(dir);
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir.getOpposite());
            if (target == null) {
                continue;
            }
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(target, stack.copy(), false);
            int moved = stack.getCount() - remaining.getCount();
            if (moved > 0) {
                itemHandler.extractItem(slot, moved, false);
                setChanged();
                return;
            }
        }
    }

    /** 物品抽取升级：从相邻容器把物品拉进输入槽。 */
    private void pullItems(int slot) {
        if (level == null) {
            return;
        }
        ItemStack current = itemHandler.getStackInSlot(slot);
        if (!current.isEmpty() && current.getCount() >= itemHandler.getSlotLimit(slot)) {
            return;
        }
        List<Direction> configured = upgradeStats.getPullingDirections();
        Iterable<Direction> targets = configured.isEmpty() ? ALL_DIRECTIONS : configured;
        for (Direction dir : targets) {
            BlockPos pos = worldPosition.relative(dir);
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            IItemHandler source = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir.getOpposite());
            if (source == null) {
                continue;
            }
            for (int slotIndex = 0; slotIndex < source.getSlots(); slotIndex++) {
                ItemStack candidate = source.extractItem(slotIndex, Integer.MAX_VALUE, true);
                if (candidate.isEmpty()) {
                    continue;
                }
                ItemStack remainder = itemHandler.insertItem(slot, candidate, true);
                int movable = candidate.getCount() - remainder.getCount();
                if (movable <= 0) {
                    continue;
                }
                ItemStack extracted = source.extractItem(slotIndex, movable, false);
                if (!extracted.isEmpty()) {
                    itemHandler.insertItem(slot, extracted, false);
                    setChanged();
                    return;
                }
            }
        }
    }

    /** 流体弹出升级：把热水推入相邻流体容器。 */
    private void ejectFluids(int budgetMb) {
        if (level == null || hotWaterTank.getFluidAmount() <= 0) {
            return;
        }
        List<Direction> configured = upgradeStats.getFluidEjectorDirections();
        Iterable<Direction> targets = configured.isEmpty() ? ALL_DIRECTIONS : configured;
        for (Direction dir : targets) {
            if (budgetMb <= 0) {
                return;
            }
            IFluidHandler adjacent = adjacentFluidHandler(dir);
            if (adjacent == null) {
                continue;
            }
            int amount = Math.min(budgetMb, hotWaterTank.getFluidAmount());
            FluidStack offer = new FluidStack(mio_icif_fluids.HOTWATER.get(), amount);
            int accepted = adjacent.fill(offer, IFluidHandler.FluidAction.EXECUTE);
            if (accepted > 0) {
                hotWaterTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                budgetMb -= accepted;
                setChanged();
            }
        }
    }

    /** 流体抽取升级：把相邻流体容器里的可接受流体拉进水槽。 */
    private void pullFluids(int budgetMb) {
        if (level == null) {
            return;
        }
        int room = waterTank.getCapacity() - waterTank.getFluidAmount();
        if (room <= 0) {
            return;
        }
        List<Direction> configured = upgradeStats.getFluidPullingDirections();
        Iterable<Direction> targets = configured.isEmpty() ? ALL_DIRECTIONS : configured;
        for (Direction dir : targets) {
            if (room <= 0 || budgetMb <= 0) {
                return;
            }
            IFluidHandler adjacent = adjacentFluidHandler(dir);
            if (adjacent == null) {
                continue;
            }
            FluidStack simulated = adjacent.drain(Math.min(room, budgetMb), IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty()) {
                continue;
            }
            int accepted = waterTank.fill(simulated, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) {
                continue;
            }
            FluidStack drained = adjacent.drain(new FluidStack(simulated.getFluid(), accepted),
                IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty()) {
                waterTank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                room -= drained.getAmount();
                budgetMb -= drained.getAmount();
                setChanged();
            }
        }
    }

    @Nullable
    private IFluidHandler adjacentFluidHandler(Direction dir) {
        if (level == null) {
            return null;
        }
        BlockPos pos = worldPosition.relative(dir);
        if (!level.hasChunkAt(pos)) {
            return null;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, dir.getOpposite());
    }

    // ==================== tick ====================

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_Stirling_Kinetic_Generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        blockEntity.recalculateUpgradeStats();

        // 流体容器输入/输出
        blockEntity.handleCoolFluidInput();
        blockEntity.handleHotFluidInput();

        // 吸热 → 换算
        blockEntity.receiveHeatFromSides();
        blockEntity.convertHeat();

        // 升级自动化
        blockEntity.handleAutomation();

        blockEntity.updateBlockState(blockEntity.isWorking(), state, level, pos);
        blockEntity.setChanged();
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    public void updateBlockState(boolean working, BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        Boolean currentActive = state.getValue(
            com.miophas.singularity_iteration.common.block.kugenerator.mio_icif_Block_Stirling_Kinetic_Generator.ACTIVE);
        if (currentActive != working) {
            level.setBlock(pos, state.setValue(
                com.miophas.singularity_iteration.common.block.kugenerator.mio_icif_Block_Stirling_Kinetic_Generator.ACTIVE,
                working), 3);
        }
    }

    // ==================== NBT ====================

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heatBuffer = Math.max(0, Math.min(MAX_HEATBUFFER, tag.getInt("HeatBuffer")));
        kuBuffer = Math.max(0, Math.min(MAX_KU_BUFFER, tag.getLong("KUBuffer")));
        liquidHeatStored = Math.max(0, tag.getInt("LiquidHeatStored"));
        progress = tag.getInt("Progress");
        waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        hotWaterTank.readFromNBT(registries, tag.getCompound("HotWaterTank"));
        itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("HeatBuffer", heatBuffer);
        tag.putLong("KUBuffer", kuBuffer);
        tag.putInt("LiquidHeatStored", liquidHeatStored);
        tag.putInt("Progress", progress);
        tag.put("WaterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.put("HotWaterTank", hotWaterTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Items", itemHandler.serializeNBT(registries));
    }

    // ==================== Getter ====================

    public int getWaterAmount() {
        return waterTank.getFluidAmount();
    }

    public int getHotWaterAmount() {
        return hotWaterTank.getFluidAmount();
    }

    public int getMaxFluidAmount() {
        return FLUID_CAPACITY;
    }

    public int getBufferedHeat() {
        return heatBuffer;
    }

    public long getKUBuffer() {
        return kuBuffer;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    /** IC2 视为“正在工作”的条件：本次 tick 实际发生了热→动能的换算。 */
    public boolean isWorking() {
        return active;
    }

    public FluidTank getWaterTank() {
        return waterTank;
    }

    public FluidTank getHotWaterTank() {
        return hotWaterTank;
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public IFluidHandler getCombinedFluidHandler() {
        return combinedFluidHandler;
    }

    public MachineUpgradeStats getUpgradeStats() {
        return upgradeStats;
    }

    // ==================== 动能存储（对齐 IC2：仅正面可抽出、带宽=缓冲上限） ====================

    @Nullable
    @Override
    public IMioIcifCapabilities.IKineticStorage getKineticStorageCapability(@Nullable Direction side) {
        if (side == null) {
            return this;
        }
        // 只在正面暴露可拉取的动能视图；其余面暴露内部存储（容量 0、不可提取、不可接收），
        // 与 IC2 中只有正面能输出 KU 的行为一致，也避免被 API 适配器绕过限制。
        return side == getFacing() ? this : kineticStorage;
    }

    @Override
    public long receiveKinetic(long toReceive, boolean simulate) {
        return 0; // IC2 动能源不接收动能
    }

    @Override
    public long extractKinetic(long toExtract, boolean simulate) {
        long extracted = Math.min(Math.max(0, toExtract), Math.min(kuBuffer, MAX_KU_BUFFER));
        if (!simulate && extracted > 0) {
            kuBuffer -= extracted;
            setChanged();
        }
        return extracted;
    }

    @Override
    public long getKineticStored() {
        return kuBuffer;
    }

    @Override
    public long getMaxKineticStored() {
        return MAX_KU_BUFFER;
    }

    @Override
    public boolean canExtractKinetic() {
        return kuBuffer > 0;
    }

    @Override
    public boolean canReceiveKinetic() {
        return false;
    }

    @Override
    public long getMaxReceive() {
        return 0;
    }

    @Override
    public long getMaxExtract() {
        return MAX_KU_BUFFER;
    }

    @Override
    public int getRPM() {
        return BoundedUnits.gauge(kuBuffer, MAX_KU_BUFFER, 0, 10000);
    }

    @Override
    public boolean isOverspeed() {
        return false; // IC2 动能系统没有转速/超速概念
    }

    @Override
    public long getKineticLossPerTick() {
        return 0; // IC2 动能源没有摩擦损耗
    }

    @Override
    public void setKinetic(long kinetic) {
        this.kuBuffer = BoundedUnits.clamp(kinetic, MAX_KU_BUFFER);
        setChanged();
    }

    @Override
    public void applyFrictionLoss() {
        // no-op
    }

    @Override
    public long generateKineticInternal(long amount, boolean simulate) {
        long generated = Math.min(Math.max(0, amount), MAX_KU_BUFFER - kuBuffer);
        if (!simulate && generated > 0) {
            kuBuffer += generated;
            setChanged();
        }
        return generated;
    }

    // ==================== 热能能力（对外只接收） ====================

    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        return new IMioIcifCapabilities.IHeatStorage() {
            @Override
            public long receiveHeat(long maxReceive, boolean simulate) {
                long heatNeeded = MAX_HEATBUFFER - heatBuffer;
                long heatToReceive = Math.min(Math.max(0L, maxReceive), Math.max(0L, heatNeeded));
                if (!simulate && heatToReceive > 0) {
                    heatBuffer = Math.min(MAX_HEATBUFFER,
                        heatBuffer + (int) Math.min(heatToReceive, (long) heatNeeded));
                    setChanged();
                }
                return heatToReceive;
            }

            @Override
            public long extractHeat(long maxExtract, boolean simulate) {
                return 0;
            }

            @Override
            public long getHeatStored() {
                return heatBuffer;
            }

            @Override
            public long getMaxHeatStored() {
                return MAX_HEATBUFFER;
            }

            @Override
            public boolean canReceiveHeat() {
                return heatBuffer < MAX_HEATBUFFER;
            }

            @Override
            public boolean canExtractHeat() {
                return false;
            }

            @Override
            public int getTemperature() {
                return 20;
            }

            @Override
            public boolean isOverheated() {
                return false;
            }

            @Override
            public long getHeatLossPerTick() {
                return 0;
            }

            @Override
            public long getMaxReceive() {
                return MAX_HEATBUFFER - heatBuffer;
            }

            @Override
            public long getMaxExtract() {
                return 0;
            }
        };
    }

    // ==================== Menu / IKineticGeneratorBlock / IBurnControl ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.stirling_kinetic_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.generator.StirlingKineticGeneratorMenu(
            containerId, playerInventory, this);
    }

    @Override
    public int getKineticOutput() {
        return isWorking() ? KU_PER_OPERATION : 0;
    }

    @Override
    public boolean isGenerating() {
        return isWorking();
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
        return KU_PER_OPERATION;
    }

    @Override
    public int getRotorRPM() {
        return getRPM();
    }

    @Override
    public int getDefaultBurnTime() {
        return 0;
    }

    @Override
    public void setBurnTime(int ticks) {
        // 斯特林动能机不燃烧燃料
    }
}

package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractHeatBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import com.miophas.singularity_iteration.core.runtime.processing.OwnedFluidConversion;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * ??��?�机?��??��?��??
 * 使用 HU ??��?��?��?��?��?�质为沼气�??并产?��??��??
 *
 * 工�?��?��??�?
 * - 从正?��?���?HU ??��?��?????�?100 HU/t�?
 * - �???��?��?�质量?0mB/次�?��?��?��?��??4000 HU/次�?�产?��沼�?��??00mB/次�??
 * - 每累计算??�?500mB ??��?�质额�?�产�?1 个�?��??
 * - ??��?��?��?��??工�?��?�度越快
 */
@SuppressWarnings("null")
public class mio_icif_fermenter_elc extends AbstractHeatBlockEntity {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(2)
        .output(2)
        .upgrade(2)
        .extra(1)
        .build();

    // 对齐 IC2 1.12.2 TileEntityFermenter#getUpgradableProperties：
    // 仅接受 ItemConsuming / ItemProducing / FluidConsuming / FluidProducing 四类升级，
    // 超频、储能、变压器等升级被拒绝。
    private static final java.util.Set<com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty> FERMENTER_UPGRADES =
        java.util.EnumSet.of(
            com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty.ITEM_CONSUMING,
            com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty.ITEM_PRODUCING,
            com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty.FLUID_CONSUMING,
            com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty.FLUID_PRODUCING);

    // 槽位?��?��?��????? builder �??��顺�?��?�EXTRA=0-1, OUTPUT=2-3, UPGRADE=4-5, EXTRA=6�?
    public static final int BIOMASS_CELL_SLOT = 0;         // ??��?�质??��??输�?�槽 (EXTRA)
    public static final int EMPTY_CELL_SLOT = 1;           // ??��?�质??��??�???��?��??空气?��??输出�?(EXTRA)
    public static final int BIOGAS_CELL_SLOT = 2;          // 沼�?��?��??输出�?(OUTPUT)
    public static final int FERTILIZER_SLOT = 3;           // ??��?��?�出�?(OUTPUT)
    public static final int UPGRADE_SLOT_START = 4;        // ???级槽起�??(UPGRADE)
    public static final int UPGRADE_SLOT_COUNT = 2;        // ???级槽?���?（对齐 IC2：2 个）
    public static final int EMPTY_CELL_INPUT_SLOT = 6;     // 沼�?��?��??专用???空气?��??输�?��??(EXTRA)
    public static final int TOTAL_SLOTS = 7;               // ??�槽位数

    // ??��?��?�置
    // 对齐 IC2 1.12.2：heatBuffer 上限即 hU_per_run = 4000，且原版发酵机没有热损耗。
    public static final int HEAT_CAPACITY = 4000;     // 热能容器：4000 HU
    public static final int MAX_HEAT_RECEIVE = 100;   // ???大接受?100 HU/t
    public static final int MAX_HEAT_EXTRACT = 0;     // 不�?�出??��??
    public static final int MAX_TEMP = 1000;          // ???高温�?
    public static final float HEAT_LOSS_FACTOR = 0f;  // 原版无热损耗

    // �?体�?�置
    public static final int BIOMASS_TANK_CAPACITY = 10000; // ??��?�质量? 10000 mB
    public static final int BIOGAS_TANK_CAPACITY = 2000;   // 沼�?��??2000 mB
    public static final int BIOMASS_PER_OPERATION = 20;    // 每次�?�??20 mB ??��?��??
    public static final int BIOGAS_PER_OPERATION = 400;    // 每次产出 400 mB 沼�??
    public static final int HEAT_PER_OPERATION = 4000;     // 每次�?�??4000 HU
    public static final int BIOMASS_PER_FERTILIZER = 500;  // �?500 mB ??��?�质产�??1 ??��??

    // 进度??�置�??���? 100 HU/t ?���?40 ticks 完成?��??次�?��?��??
    public static final int BASE_PROGRESS_REQUIRED = HEAT_PER_OPERATION; // ??��?�度???�?= 4000 HU

    // �?体�?��??
    protected final FluidTank biomassTank;
    protected final FluidTank biogasTank;
    private final IFluidHandler combinedFluidHandler;

    // 工�?��?�度�?使用??��?�累计算??
    private long workCredit;
    // 最近一次实际收到热量的游戏刻（用于 LIT 对齐 IC2 的“存在热源即点亮”）
    private long lastHeatTick = Long.MIN_VALUE;

    // 累计算?????????��?�质量??��于�?��?�产?���?
    private long biomassProcessed;

    // ?��?���??��工�??
    private boolean isWorking;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getProgress();
                case 1 -> BIOMASS_PER_FERTILIZER;
                case 2 -> isWorking ? 1 : 0;
                case 3 -> (int) heatStorage.getHeatStored();
                case 4 -> (int) heatStorage.getMaxHeatStored();
                case 5 -> biomassTank.getFluidAmount();
                case 6 -> biomassTank.getCapacity();
                case 7 -> biogasTank.getFluidAmount();
                case 8 -> biogasTank.getCapacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() { return 9; }
    };

    /**
     * ?????�函�?
     */
    public mio_icif_fermenter_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.FERMENTER_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_fermenter_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type, pos, state, HEAT_CAPACITY, MAX_HEAT_RECEIVE, MAX_HEAT_EXTRACT,
              20, MAX_TEMP, HEAT_LOSS_FACTOR);

        this.workCredit = 0;
        this.biomassProcessed = 0;
        this.isWorking = false;

        // ??��?��?�槽位置??�???��?��??�?????��
        this.slotLayout = LAYOUT;
        this.itemHandler = createItemHandler(LAYOUT);
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_fermenter_elc.this.isItemValidForSlot(slot, stack));

        // ??��?��?��?��?�质量?体�??
        this.biomassTank = new FluidTank(BIOMASS_TANK_CAPACITY, fluidStack ->
            FermenterFuels.accepts(fluidStack.getFluid())) {
                @Override protected void onContentsChanged() { ContainerToTank.markUnsaved(mio_icif_fermenter_elc.this); }
            };

        // ??��?��?�沼气�??体槽
        this.biogasTank = new FluidTank(BIOGAS_TANK_CAPACITY, fluidStack ->
            FermenterFuels.isOutputFluid(fluidStack.getFluid())) {
                @Override protected void onContentsChanged() { ContainerToTank.markUnsaved(mio_icif_fermenter_elc.this); }
            };
        this.combinedFluidHandler = com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup
            .inputOutput(biomassTank, biogasTank);
    }

    /**
     * �??��??��???��?��???????��??��??定槽位?
     */
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (isUpgradeSlot(slot)) {
            return stack.getItem() instanceof com.miophas.singularity_iteration.common.item.upgrade.mio_icif_upgrade upgrade
                && upgrade.isSuitableFor(stack, FERMENTER_UPGRADES);
        }
        return switch (slot) {
            case BIOMASS_CELL_SLOT -> isInputContainer(stack);
            case EMPTY_CELL_SLOT -> mio_icif_cells.isEmptyCell(stack);
            case EMPTY_CELL_INPUT_SLOT -> mio_icif_cells.isEmptyCell(stack);
            case BIOGAS_CELL_SLOT -> false; // 输出�?
            case FERTILIZER_SLOT -> false;  // 输出�?
            default -> false;
        };
    }

    /** 是否为装满任一已注册输入流体（如生物质）的单元。 */
    private static boolean isInputContainer(ItemStack stack) {
        FluidStack content = mio_icif_cells.getCellFluid(stack);
        return !content.isEmpty() && FermenterFuels.accepts(content.getFluid())
            && content.getAmount() >= mio_icif_cells.getCellCapacity(stack);
    }

    /**
     * 对齐原版 IC2 {@code TileEntityFermenter.work()}：只从“正面”那一格热源抽取 HU，
     * 且每 tick 请求上限固定为 100 HU（不随相邻热源数量放大）。
     */
    private void receiveHeatFromSides() {
        if (level == null || level.isClientSide()) return;
        long needed = heatStorage.getMaxHeatStored() - heatStorage.getHeatStored();
        if (needed <= 0) return;

        long perTick = Math.max(0, heatStorage.getMaxReceive());
        if (perTick <= 0) return;

        Direction front = getFrontSide();
        BlockPos neighborPos = worldPosition.relative(front);
        if (!level.hasChunkAt(neighborPos)) return;

        IMioIcifCapabilities.IHeatStorage adjacentHeat = level.getCapability(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK, neighborPos, front.getOpposite());
        if (adjacentHeat == null || !adjacentHeat.canExtractHeat()) return;

        long toExtract = Math.min(needed, Math.min(adjacentHeat.getHeatStored(), perTick));
        if (toExtract <= 0) return;

        long extracted = adjacentHeat.extractHeat(toExtract, false);
        if (extracted <= 0) return;

        long received = heatStorage.receiveHeat(extracted, false);
        if (received < extracted) {
            heatStorage.generateHeatInternal(extracted - received, false);
        }
        if (level != null) lastHeatTick = level.getGameTime();
        setChanged();
    }

    /**
     * ?��??�正?��?��??��????��?�接?��?���?
     */
    private Direction getFrontSide() {
        BlockState state = getBlockState();
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    /**
     * ??��?��?��?��?��?��?��?��?�只??�正?��?��以接?��??��??
     */
    @Override
    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        if (side == null || side == getFrontSide()) {
            return this;
        }
        return null;
    }

    /**
     * �?tick ?��?��??��??
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_fermenter_elc blockEntity) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)
                || !server.getServer().isSameThread() || blockEntity.isRemoved()) {
            return;
        }

        blockEntity.recalculateUpgradeStats();

        // 应用??��??�?
        blockEntity.applyHeatLoss();

        // 主动从相邻热源抽取 HU（对齐原版 IC2：热源不主动“推”，用热方按面抽取）
        blockEntity.receiveHeatFromSides();

        // ???�?????��?��??��??级�??�?体弹簧??��??��?��????��???????��??�?填充??
        // ??��?�沼气被优�??�?进�?��?????弹簧?��?�相??��?��??
        blockEntity.handleAutomationUpgrades();

        // �??????��??槽位?��??体槽???交换??
        blockEntity.handleBiomassCellSlot();
        blockEntity.handleBiogasCellSlot();

        // 对齐 IC2 work()：先结算肥料（累计生物质达到 500 时投放，输出槽满也清零进度）
        blockEntity.emitFertilizerIfDue();

        // ??��????��?�工�?
        if (blockEntity.canWorkRedstone() && blockEntity.canWork()) {
            blockEntity.doWork();
        }
        blockEntity.isWorking = blockEntity.computeActive();

        // ?��?��?��??�状态??
        boolean isLit = state.getValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_fermenter_elc.LIT);
        if (blockEntity.isWorking != isLit) {
            level.setBlock(pos, state.setValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_fermenter_elc.LIT, blockEntity.isWorking), 3);
        }

        blockEntity.setChanged();
    }

    /**
     * �??????��?�质??��??槽位?��????��??中�????��?�质转移??��??体�??
     */
    private void handleBiomassCellSlot() {
        var cell = itemHandler.getStackInSlot(BIOMASS_CELL_SLOT);
        if (cell.isEmpty() || !isInputContainer(cell)) return;
        var single = cell.copyWithCount(1);
        if (ContainerToTank.transfer(itemHandler, BIOMASS_CELL_SLOT, EMPTY_CELL_SLOT, biomassTank,
                mio_icif_cells.getCellFluid(single), mio_icif_cells.getEmptyCellForStack(single))) setChanged();
    }

    /**
     * �????沼�?��?�出：用空气?��??从沼气槽位?填沼�?
     * 对齐 IC2：按输出罐当前流体充装（不硬编码沼气）。
     */
    private void handleBiogasCellSlot() {
        FluidStack output = biogasTank.getFluid();
        if (output.isEmpty()) return;
        int inputSlot = EMPTY_CELL_INPUT_SLOT;
        if (!mio_icif_cells.isEmptyCell(itemHandler.getStackInSlot(inputSlot))) inputSlot = EMPTY_CELL_SLOT;
        if (!mio_icif_cells.isEmptyCell(itemHandler.getStackInSlot(inputSlot))) return;
        var filled = mio_icif_cells.getFilledCellForFluidStack(output.getFluid());
        if (filled.isEmpty()) return;
        var content = mio_icif_cells.getCellFluid(filled.copyWithCount(1));
        if (content.getFluid() != output.getFluid()) return;
        if (ContainerToTank.drainToContainer(itemHandler, inputSlot, BIOGAS_CELL_SLOT, biogasTank, content, filled)) setChanged();
    }

    /**
     * 对齐 IC2 {@code TileEntityFermenter.work()} 开头的肥料结算：
     * 累计生物质达到 500 mB 时向肥料槽投放 1 个肥料，并<b>无条件清零进度</b>
     * （输出槽满时该肥料会被丢弃——与 IC2 的 {@code fertiliserSlot.add()} 行为一致）。
     */
    private void emitFertilizerIfDue() {
        if (biomassProcessed < BIOMASS_PER_FERTILIZER) return;
        ItemStack fertilizer = new ItemStack(mio_icif_resources.FERTILIZER.get());
        ItemStack existing = itemHandler.getStackInSlot(FERTILIZER_SLOT);
        int limit = Math.min(itemHandler.getSlotLimit(FERTILIZER_SLOT), fertilizer.getMaxStackSize());
        if (existing.isEmpty()) {
            itemHandler.setStackInSlot(FERTILIZER_SLOT, fertilizer);
        } else if (ItemStack.isSameItemSameComponents(existing, fertilizer) && existing.getCount() < limit) {
            existing.grow(1);
        }
        biomassProcessed = 0;
    }

    /**
     * 对齐 IC2：发酵机没有红石控制（{@code getUpgradableProperties} 不含 RedstoneSensitive），
     * 因此红石信号不影响其工作。
     */
    @Override
    protected boolean canWorkRedstone() {
        return true;
    }

    /**
     * 对齐 IC2 的活化状态：发酵配方就绪且存在热量（缓冲/在途/最近收到热）时点亮。
     */
    private boolean computeActive() {
        if (currentProperty() == null || prepareOperation().isEmpty()) return false;
        return heatStorage.getHeatStored() > 0 || workCredit > 0 || lastHeatTick >= currentTick() - 1;
    }

    private long currentTick() {
        return level == null ? 0 : level.getGameTime();
    }

    /** 记录热量到达时刻，用于 LIT 判定（含热源“推入”与本机“拉取”两条路径）。 */
    @Override
    public long receiveHeat(long toReceive, boolean simulate) {
        long received = super.receiveHeat(toReceive, simulate);
        if (!simulate && received > 0 && level != null) lastHeatTick = level.getGameTime();
        return received;
    }

    /**
     * �??��?��?��?��以工�?
     */
    protected boolean canWork() {
        FermenterFuels.Property property = currentProperty();
        return property != null && (workCredit >= property.heat() || heatStorage.getHeatStored() > 0)
            && prepareOperation().isPresent();
    }

    /** 当前输入流体对应的发酵配方；无有效输入返回 null。 */
    @Nullable
    private FermenterFuels.Property currentProperty() {
        FluidStack input = biomassTank.getFluid();
        return input.isEmpty() ? null : FermenterFuels.get(input.getFluid());
    }

    /** 对齐 IC2 work() 的“能否发酵”条件：配方在、输入量够、输出罐有空间。 */
    private java.util.Optional<OwnedFluidConversion.Prepared> prepareOperation() {
        FermenterFuels.Property property = currentProperty();
        if (property == null) return java.util.Optional.empty();
        FluidStack input = biomassTank.getFluid();
        return OwnedFluidConversion.prepare(biomassTank, new FluidStack(input.getFluid(), property.inputAmount()),
            biogasTank, new FluidStack(property.output(), property.outputAmount()), itemHandler, -1, ItemStack.EMPTY);
    }

    /**
     * ??��????��?�工�?
     */
    protected void doWork() {
        var operation = prepareOperation();
        if (operation.isEmpty()) return;
        FermenterFuels.Property property = currentProperty();
        if (property == null) return;
        int heatRequired = property.heat();
        if (workCredit < heatRequired) {
            long remaining = heatRequired - workCredit;
            long requested = Math.min(remaining, Math.min(MAX_HEAT_RECEIVE, heatStorage.getHeatStored()));
            long consumed = heatStorage.consumeHeatInternal(requested, false);
            if (consumed <= 0) return;
            workCredit += consumed;
        }
        if (workCredit >= heatRequired) {
            long creditBefore = workCredit, processedBefore = biomassProcessed;
            workCredit -= heatRequired;
            biomassProcessed += property.inputAmount();
            if (!operation.get().commit()) {
                workCredit = creditBefore; biomassProcessed = processedBefore;
                setChanged();
                return;
            }
        }
        setChanged();
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[]{EMPTY_CELL_SLOT, BIOGAS_CELL_SLOT, FERTILIZER_SLOT};
    }

    @Override
    protected int[] getInputSlots() {
        return new int[]{BIOMASS_CELL_SLOT, EMPTY_CELL_INPUT_SLOT};
    }

    // ==================== Getter ?���? ====================

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public IFluidHandler getBiomassTank() {
        return biomassTank;
    }

    public IFluidHandler getBiogasTank() {
        return biogasTank;
    }

    public int getBiomassAmount() {
        return biomassTank.getFluidAmount();
    }

    public int getBiomassCapacity() {
        return biomassTank.getCapacity();
    }

    public int getBiogasAmount() {
        return biogasTank.getFluidAmount();
    }

    public int getBiogasCapacity() {
        return biogasTank.getCapacity();
    }

    /**
     * 对齐 IC2 {@code GuiFermenter}：GUI 进度条显示“肥料进度”（累计生物质 / 500），
     * 而不是单次运行的 BTU 进度；单次运行的热量进度由热量条（heatStorage）表示。
     */
    public int getProgress() {
        return (int) Math.min(BIOMASS_PER_FERTILIZER, biomassProcessed);
    }

    public int getMaxProgress() {
        return BIOMASS_PER_FERTILIZER;
    }

    public long getHeatStored() {
        return heatStorage.getHeatStored();
    }

    public int getHeatCapacity() {
        return (int) heatStorage.getMaxHeatStored();
    }

    public boolean isWorking() {
        return isWorking;
    }

    // ==================== Capability ???�? ====================

    public net.neoforged.neoforge.items.IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        // 对齐 IC2 的 InvSide：生物质容器输入仅顶面、沼气空容器输入仅底面，输出任意面可抽，升级槽外部不可访问。
        return new SidedItemHandler(side);
    }

    /** IC2 侧向访问语义的物品处理器视图。 */
    private final class SidedItemHandler implements net.neoforged.neoforge.items.IItemHandlerModifiable {
        private final @Nullable Direction side;

        private SidedItemHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override public int getSlots() { return itemHandler.getSlots(); }

        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }

        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }

        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return canExternallyInsert(slot) && insertSideMatches(slot) && itemHandler.isItemValid(slot, stack);
        }

        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!canExternallyInsert(slot) || !insertSideMatches(slot)) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!canExternallyExtract(slot)) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override public void setStackInSlot(int slot, ItemStack stack) {
            itemHandler.setStackInSlot(slot, stack);
        }

        private boolean insertSideMatches(int slot) {
            if (slot == BIOMASS_CELL_SLOT) return side == Direction.UP;
            if (slot == EMPTY_CELL_INPUT_SLOT) return side == Direction.DOWN;
            return false;
        }
    }

    private static boolean canExternallyInsert(int slot) {
        return slot == BIOMASS_CELL_SLOT || slot == EMPTY_CELL_INPUT_SLOT;
    }

    private static boolean canExternallyExtract(int slot) {
        return slot == EMPTY_CELL_SLOT || slot == BIOGAS_CELL_SLOT || slot == FERTILIZER_SLOT;
    }

    @Override
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return combinedFluidHandler;
    }

    // ==================== NBT 序�?��??====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.put("biomassTank", biomassTank.writeToNBT(registries, new CompoundTag()));
        tag.put("biogasTank", biogasTank.writeToNBT(registries, new CompoundTag()));
        // 旧键 "progress" 历史上存的是 BTU 工作进度（workCredit），保持写入 workCredit 以兼容旧存档。
        tag.putInt("progress", (int) Math.min(Integer.MAX_VALUE, workCredit));
        tag.putLong("scex_work_credit", workCredit);
        tag.putLong("biomassProcessed", biomassProcessed);
        tag.putBoolean("isWorking", isWorking);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("biomassTank")) {
            biomassTank.readFromNBT(registries, tag.getCompound("biomassTank"));
        }
        if (tag.contains("biogasTank")) {
            biogasTank.readFromNBT(registries, tag.getCompound("biogasTank"));
        }
        workCredit = Math.max(0, tag.contains("scex_work_credit", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)
            ? tag.getLong("scex_work_credit") : tag.getLong("progress"));
        biomassProcessed = Math.max(0, tag.getLong("biomassProcessed"));
        isWorking = level != null && level.isClientSide() && tag.getBoolean("isWorking");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.fermenter_elc");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.producer.FermenterElcMenu(containerId, playerInventory, this);
    }

    public ContainerData getContainerData() { return containerData; }
}

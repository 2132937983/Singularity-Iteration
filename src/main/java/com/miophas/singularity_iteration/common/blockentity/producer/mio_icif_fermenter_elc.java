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
 * Electric fermenter block entity.
 * It uses HU (heat units) to convert biomass into biogas and produces fertilizer as a by-product.
 *
 * Operation:
 * - The machine pulls HU only from the heat source on its front side, at a maximum of 100 HU/t.
 * - One run consumes 20 mB of biomass and 4000 HU, and produces 400 mB of biogas.
 * - Each 500 mB of processed biomass produces 1 fertilizer.
 * - A higher heat input makes the machine work faster.
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

    // Slot indices follow the SlotLayout builder order: EXTRA=0-1, OUTPUT=2-3, UPGRADE=4-5, EXTRA=6.
    public static final int BIOMASS_CELL_SLOT = 0;         // Input slot for filled biomass cells (EXTRA)
    public static final int EMPTY_CELL_SLOT = 1;           // Output slot for empty cells after the biomass drains (EXTRA)
    public static final int BIOGAS_CELL_SLOT = 2;          // Output slot for filled biogas cells (OUTPUT)
    public static final int FERTILIZER_SLOT = 3;           // Fertilizer output slot (OUTPUT)
    public static final int UPGRADE_SLOT_START = 4;        // First upgrade slot (UPGRADE)
    public static final int UPGRADE_SLOT_COUNT = 2;        // Number of upgrade slots (2, same as IC2)
    public static final int EMPTY_CELL_INPUT_SLOT = 6;     // Input slot for empty cells that receive biogas (EXTRA)
    public static final int TOTAL_SLOTS = 7;               // Total number of slots

    // Heat storage settings
    // 对齐 IC2 1.12.2：heatBuffer 上限即 hU_per_run = 4000，且原版发酵机没有热损耗。
    public static final int HEAT_CAPACITY = 4000;     // 热能容器：4000 HU
    public static final int MAX_HEAT_RECEIVE = 100;   // Maximum heat input: 100 HU/t
    public static final int MAX_HEAT_EXTRACT = 0;     // The machine does not output heat
    public static final int MAX_TEMP = 1000;          // Maximum temperature
    public static final float HEAT_LOSS_FACTOR = 0f;  // 原版无热损耗

    // Fluid settings
    public static final int BIOMASS_TANK_CAPACITY = 10000; // Biomass tank capacity: 10000 mB
    public static final int BIOGAS_TANK_CAPACITY = 2000;   // Biogas tank capacity: 2000 mB
    public static final int BIOMASS_PER_OPERATION = 20;    // Biomass consumed per run: 20 mB
    public static final int BIOGAS_PER_OPERATION = 400;    // Biogas produced per run: 400 mB
    public static final int HEAT_PER_OPERATION = 4000;     // Heat consumed per run: 4000 HU
    public static final int BIOMASS_PER_FERTILIZER = 500;  // Each 500 mB of processed biomass produces 1 fertilizer

    // Progress settings. At 100 HU/t, one run takes 40 ticks.
    public static final int BASE_PROGRESS_REQUIRED = HEAT_PER_OPERATION; // Heat required for one run = 4000 HU

    // Fluid tanks
    protected final FluidTank biomassTank;
    protected final FluidTank biogasTank;
    private final IFluidHandler combinedFluidHandler;

    // Work progress, stored as accumulated HU
    private long workCredit;
    // 最近一次实际收到热量的游戏刻（用于 LIT 对齐 IC2 的“存在热源即点亮”）
    private long lastHeatTick = Long.MIN_VALUE;

    // Total processed biomass in mB. The machine uses this value to produce fertilizer.
    private long biomassProcessed;

    // True while the machine is working
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
     * Creates the fermenter with the registered block entity type.
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

        // Set up the slot layout and the item handler
        this.slotLayout = LAYOUT;
        this.itemHandler = createItemHandler(LAYOUT);
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_fermenter_elc.this.isItemValidForSlot(slot, stack));

        // Create the biomass input tank
        this.biomassTank = new FluidTank(BIOMASS_TANK_CAPACITY, fluidStack ->
            FermenterFuels.accepts(fluidStack.getFluid())) {
                @Override protected void onContentsChanged() { ContainerToTank.markUnsaved(mio_icif_fermenter_elc.this); }
            };

        // Create the biogas output tank
        this.biogasTank = new FluidTank(BIOGAS_TANK_CAPACITY, fluidStack ->
            FermenterFuels.isOutputFluid(fluidStack.getFluid())) {
                @Override protected void onContentsChanged() { ContainerToTank.markUnsaved(mio_icif_fermenter_elc.this); }
            };
        this.combinedFluidHandler = com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup
            .inputOutput(biomassTank, biogasTank);
    }

    /**
     * Checks if the item stack is valid for the given slot.
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
            case BIOGAS_CELL_SLOT -> false; // Output slot
            case FERTILIZER_SLOT -> false;  // Output slot
            default -> false;
        };
    }

    /** 0.1.10.6：也接受生物质桶、半满单元与其他模组的容器（经流体能力倒入）。 */
    private static boolean isInputContainer(ItemStack stack) {
        return isFullInputCell(stack)
            || com.miophas.singularity_iteration.common.util.FluidContainerSlots.holds(stack, FermenterFuels::accepts);
    }

    /** 是否为装满任一已注册输入流体（如生物质）的单元。 */
    private static boolean isFullInputCell(ItemStack stack) {
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
        if (!level.isLoaded(neighborPos)) return;

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
     * Gets the front side of the block. Only this side accepts heat.
     */
    private Direction getFrontSide() {
        BlockState state = getBlockState();
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    /**
     * Returns the heat capability. Only the front side (or a null side) can access it.
     */
    @Override
    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        if (side == null || side == getFrontSide()) {
            return this;
        }
        return null;
    }

    /**
     * Runs the server-side logic once per tick.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_fermenter_elc blockEntity) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)
                || !server.getServer().isSameThread() || blockEntity.isRemoved()) {
            return;
        }

        blockEntity.recalculateUpgradeStats();

        // Apply heat loss
        blockEntity.applyHeatLoss();

        // 主动从相邻热源抽取 HU（对齐原版 IC2：热源不主动“推”，用热方按面抽取）
        blockEntity.receiveHeatFromSides();

        // Handle the automation upgrades (item and fluid ejector/pulling upgrades) first.
        // This lets the upgrades push out biogas before the cell slots fill from the tank.
        blockEntity.handleAutomationUpgrades();

        // Exchange fluid between the cell slots and the tanks
        blockEntity.handleBiomassCellSlot();
        blockEntity.handleBiogasCellSlot();

        // 对齐 IC2 work()：先结算肥料（累计生物质达到 500 时投放，输出槽满也清零进度）
        blockEntity.emitFertilizerIfDue();

        // Do one work step if the machine can work
        if (blockEntity.canWorkRedstone() && blockEntity.canWork()) {
            blockEntity.doWork();
        }
        blockEntity.isWorking = blockEntity.computeActive();

        // Update the LIT block state
        boolean isLit = state.getValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_fermenter_elc.LIT);
        if (blockEntity.isWorking != isLit) {
            level.setBlock(pos, state.setValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_fermenter_elc.LIT, blockEntity.isWorking), 3);
        }

        blockEntity.setChanged();
    }

    /**
     * Drains a filled biomass cell from the input slot into the biomass tank.
     */
    private void handleBiomassCellSlot() {
        var cell = itemHandler.getStackInSlot(BIOMASS_CELL_SLOT);
        if (cell.isEmpty() || !isInputContainer(cell)) return;
        if (!isFullInputCell(cell)) {
            if (com.miophas.singularity_iteration.common.util.FluidContainerSlots.emptyInto(itemHandler,
                    BIOMASS_CELL_SLOT, EMPTY_CELL_SLOT, biomassTank, FermenterFuels::accepts)) setChanged();
            return;
        }
        var single = cell.copyWithCount(1);
        if (ContainerToTank.transfer(itemHandler, BIOMASS_CELL_SLOT, EMPTY_CELL_SLOT, biomassTank,
                mio_icif_cells.getCellFluid(single), mio_icif_cells.getEmptyCellForStack(single))) setChanged();
    }

    /**
     * Fills biogas into cells. Empty cells from the cell slot receive fluid from the biogas tank.
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
     * Checks if the machine can work.
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
     * Does one work step.
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

    // ==================== Getters ====================

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

    // ==================== Capabilities ====================

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

    // ==================== NBT serialization ====================

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

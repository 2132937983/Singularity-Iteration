package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractHeatBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.recipe.fermenter.FermenterHeatIntake;
import com.miophas.singularity_iteration.common.recipe.fermenter.FermenterRecipe;
import com.miophas.singularity_iteration.common.recipe.fermenter.FermenterRecipes;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
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
 * It uses HU (heat units) to ferment fluids by the {@code mio_icif:fermenter} recipes (JSON, see
 * {@link FermenterRecipe}); the default recipe turns biomass into biogas with fertilizer as a by-product.
 *
 * Operation:
 * - The machine takes HU only from the heat source on its front side, at a maximum of 100 HU/t.
 * - 0.1.10.24 (IC2 1.12.2 work()): it takes heat only in a tick in which a run can start (a recipe for the input fluid,
 *   one run of input, room for one run of output); the 100 HU/t cover heat pushed in and heat pulled together
 *   ({@link FermenterHeatIntake}). Before, the 4000 HU store filled whenever it had room, also with no biomass.
 * - One run consumes the recipe's input and heat (biomass: 20 mB and 4000 HU) and produces its output (400 mB of biogas).
 * - Each by-product interval of fermented input (biomass: 500 mB) produces the by-product (1 fertilizer).
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

    // Fluid settings (the amounts per run, the heat and the by-product come from the recipe)
    public static final int BIOMASS_TANK_CAPACITY = 10000; // Input tank capacity: 10000 mB
    public static final int BIOGAS_TANK_CAPACITY = 2000;   // Output tank capacity: 2000 mB

    // Fluid tanks
    protected final FluidTank biomassTank;
    protected final FluidTank biogasTank;
    private final IFluidHandler combinedFluidHandler;

    // Work progress, stored as accumulated HU
    private long workCredit;
    // 最近一次收到热量或把热量投入本次运行的游戏刻（用于 LIT）
    private long lastHeatTick = Long.MIN_VALUE;
    // 0.1.10.24: heat allowance per tick (IC2 draws only while a run can start, 100 HU per tick)
    private final FermenterHeatIntake heatIntake = new FermenterHeatIntake();
    // 0.1.10.24: "a run can start", kept for one game tick; a tank change clears it
    private long canRunTick = Long.MIN_VALUE;
    private boolean canRunMemo;

    // Input fermented since the last by-product, in mB (IC2: biomass toward the next fertilizer)
    private long biomassProcessed;
    // The recipe of the last run: its by-product and interval apply to biomassProcessed
    @Nullable
    private ResourceLocation byproductRecipeId;
    // The recipe of the input tank's fluid, revalidated against the recipe manager
    @Nullable
    private RecipeHolder<FermenterRecipe> recipeMemo;

    // True while the machine is working
    private boolean isWorking;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getProgress();
                case 1 -> getMaxProgress();
                case 2 -> isWorking ? 1 : 0;
                case 3 -> (int) getRunHeat();
                case 4 -> getRunHeatRequired();
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

        // Create the input tank: fluids that a fermenter recipe takes
        this.biomassTank = new FluidTank(BIOMASS_TANK_CAPACITY, fluidStack ->
            acceptsFluid(fluidStack.getFluid())) {
                @Override protected void onContentsChanged() {
                    canRunTick = Long.MIN_VALUE;
                    ContainerToTank.markUnsaved(mio_icif_fermenter_elc.this);
                }
            };

        // Create the output tank: fluids that a fermenter recipe gives
        this.biogasTank = new FluidTank(BIOGAS_TANK_CAPACITY, fluidStack ->
            FermenterRecipes.isOutput(level, fluidStack.getFluid())) {
                @Override protected void onContentsChanged() {
                    canRunTick = Long.MIN_VALUE;
                    ContainerToTank.markUnsaved(mio_icif_fermenter_elc.this);
                }
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

    /** Whether a fermenter recipe takes {@code fluid}. */
    private boolean acceptsFluid(net.minecraft.world.level.material.Fluid fluid) {
        return FermenterRecipes.accepts(level, fluid);
    }

    /** 0.1.10.6：也接受生物质桶、半满单元与其他模组的容器（经流体能力倒入）。 */
    private boolean isInputContainer(ItemStack stack) {
        return isFullInputCell(stack)
            || com.miophas.singularity_iteration.common.util.FluidContainerSlots.holds(stack, this::acceptsFluid);
    }

    /** 是否为装满任一配方输入流体（如生物质）的单元。 */
    private boolean isFullInputCell(ItemStack stack) {
        FluidStack content = mio_icif_cells.getCellFluid(stack);
        return !content.isEmpty() && acceptsFluid(content.getFluid())
            && content.getAmount() >= mio_icif_cells.getCellCapacity(stack);
    }

    /**
     * 对齐原版 IC2 {@code TileEntityFermenter.work()}：只从“正面”那一格热源抽取 HU，
     * 且每 tick 请求上限固定为 100 HU（不随相邻热源数量放大）。
     * 0.1.10.24: only in a tick in which a run can start, and the 100 HU/t include heat a generator pushed in.
     */
    private void receiveHeatFromSides() {
        if (level == null || level.isClientSide()) return;
        long room = Math.min(heatIntake.room(currentTick(), canStartRun()),
            heatStorage.getMaxHeatStored() - heatStorage.getHeatStored());
        if (room <= 0) return;

        Direction front = getFrontSide();
        BlockPos neighborPos = worldPosition.relative(front);
        if (!level.isLoaded(neighborPos)) return;

        IMioIcifCapabilities.IHeatStorage adjacentHeat = level.getCapability(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK, neighborPos, front.getOpposite());
        if (adjacentHeat == null || !adjacentHeat.canExtractHeat()) return;

        long toExtract = Math.min(room, adjacentHeat.getHeatStored());
        if (toExtract <= 0) return;

        long extracted = adjacentHeat.extractHeat(toExtract, false);
        if (extracted <= 0) return;

        long received = receiveHeat(extracted, false);   // counts toward the allowance, records the tick
        if (received < extracted) {
            heatStorage.generateHeatInternal(extracted - received, false);
        }
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

        // Handle the automation upgrades (item and fluid ejector/pulling upgrades) first.
        // This lets the upgrades push out biogas before the cell slots fill from the tank.
        blockEntity.handleAutomationUpgrades();

        // Exchange fluid between the cell slots and the tanks
        blockEntity.handleBiomassCellSlot();
        blockEntity.handleBiogasCellSlot();

        // 对齐 IC2 work()：先结算肥料（累计生物质达到 500 时投放，输出槽满也清零进度）
        blockEntity.emitFertilizerIfDue();

        // 主动从相邻热源抽取 HU（对齐原版 IC2：热源不主动“推”，用热方按面抽取）
        // 0.1.10.24: after the cell slots and the fertilizer, like IC2 (processIntoTank / processFromTank, then work())
        blockEntity.receiveHeatFromSides();

        // Do one work step if the machine can work
        boolean runnable = blockEntity.canStartRun();
        if (blockEntity.canWorkRedstone() && blockEntity.canWork()) {
            blockEntity.doWork();
        }
        blockEntity.isWorking = blockEntity.computeActive(runnable);

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
                    BIOMASS_CELL_SLOT, EMPTY_CELL_SLOT, biomassTank, this::acceptsFluid)) setChanged();
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
     * 累计输入达到配方的副产物间隔（生物质 500 mB）时向肥料槽投放副产物，并<b>无条件清零进度</b>
     * （输出槽满时该副产物会被丢弃——与 IC2 的 {@code fertiliserSlot.add()} 行为一致）。
     */
    private void emitFertilizerIfDue() {
        FermenterRecipe recipe = byproductRecipe();
        if (recipe == null || biomassProcessed < recipe.getByproductInterval()) return;
        ItemStack byproduct = recipe.getByproduct();
        if (!byproduct.isEmpty()) {
            ItemStack existing = itemHandler.getStackInSlot(FERTILIZER_SLOT);
            int limit = Math.min(itemHandler.getSlotLimit(FERTILIZER_SLOT), byproduct.getMaxStackSize());
            if (existing.isEmpty()) {
                itemHandler.setStackInSlot(FERTILIZER_SLOT, byproduct);
            } else if (ItemStack.isSameItemSameComponents(existing, byproduct) && existing.getCount() + byproduct.getCount() <= limit) {
                existing.grow(byproduct.getCount());
            }
        }
        biomassProcessed = 0;
    }

    /** The recipe whose by-product the fermented input counts toward: the last run's, else the current one. */
    @Nullable
    private FermenterRecipe byproductRecipe() {
        var last = FermenterRecipes.byId(level, byproductRecipeId);
        return last != null ? last.value() : currentProperty();
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
     * 活化状态：本 tick 开始时配方可运行，且最近一 tick 有热量到达或投入运行。
     * 0.1.10.24: heat left in the store or in the run credit no longer keeps the lamp on without a heat source. IC2 lights
     * the machine whenever a heat source block is in front and a run can start, also when the source gives nothing (an
     * unfuelled generator); that case is not copied.
     */
    private boolean computeActive(boolean runnable) {
        return runnable && lastHeatTick >= currentTick() - 1;
    }

    private long currentTick() {
        return level == null ? 0 : level.getGameTime();
    }

    /**
     * 0.1.10.24 (IC2 1.12.2 work()): a run can start: a recipe for the input fluid, one run of input in the tank and room
     * for one run of output. Kept for the game tick; a tank change clears it.
     */
    private boolean canStartRun() {
        long now = currentTick();
        if (canRunTick != now) {
            canRunMemo = currentProperty() != null && prepareOperation().isPresent();
            canRunTick = now;
        }
        return canRunMemo;
    }

    /**
     * 0.1.10.24 (IC2 1.12.2 work()): heat comes in only in a tick in which a run can start, at most 100 HU per tick over
     * both paths (a generator pushing, {@link #receiveHeatFromSides} pulling). 记录热量到达时刻，用于 LIT 判定。
     */
    @Override
    public long receiveHeat(long toReceive, boolean simulate) {
        long now = currentTick();
        long allowed = Math.min(toReceive, heatIntake.room(now, canStartRun()));
        if (allowed <= 0) return 0;
        long received = super.receiveHeat(allowed, simulate);
        if (!simulate && received > 0) {
            heatIntake.took(now, received);
            lastHeatTick = now;
        }
        return received;
    }

    /** 0.1.10.24: generators see no room while no run can start or the tick's 100 HU are in (ThermalOutput.room). */
    @Override
    public boolean canReceiveHeat() {
        return super.canReceiveHeat() && heatIntake.room(currentTick(), canStartRun()) > 0;
    }

    /**
     * Checks if the machine can work.
     */
    protected boolean canWork() {
        FermenterRecipe recipe = currentProperty();
        return recipe != null && (workCredit >= recipe.getHeat() || heatStorage.getHeatStored() > 0)
            && prepareOperation().isPresent();
    }

    /** 当前输入流体对应的发酵配方（mio_icif:fermenter）；无有效输入返回 null。 */
    @Nullable
    private FermenterRecipe currentProperty() {
        RecipeHolder<FermenterRecipe> holder = currentRecipeHolder();
        return holder == null ? null : holder.value();
    }

    @Nullable
    private RecipeHolder<FermenterRecipe> currentRecipeHolder() {
        FluidStack input = biomassTank.getFluid();
        if (input.isEmpty() || level == null) return null;
        RecipeHolder<FermenterRecipe> memo = recipeMemo;
        if (memo != null && memo.value().accepts(input.getFluid())
                && level.getRecipeManager().byKey(memo.id()).orElse(null) == memo) {
            return memo;
        }
        recipeMemo = FermenterRecipes.find(level, input.getFluid());
        return recipeMemo;
    }

    /** 对齐 IC2 work() 的“能否发酵”条件：配方在、输入量够、输出罐有空间。 */
    private java.util.Optional<OwnedFluidConversion.Prepared> prepareOperation() {
        FermenterRecipe recipe = currentProperty();
        if (recipe == null) return java.util.Optional.empty();
        FluidStack input = biomassTank.getFluid();
        return OwnedFluidConversion.prepare(biomassTank, new FluidStack(input.getFluid(), recipe.getInput().amount()),
            biogasTank, recipe.getOutput(), itemHandler, -1, ItemStack.EMPTY);
    }

    /**
     * Does one work step.
     */
    protected void doWork() {
        var operation = prepareOperation();
        if (operation.isEmpty()) return;
        RecipeHolder<FermenterRecipe> holder = currentRecipeHolder();
        if (holder == null) return;
        FermenterRecipe property = holder.value();
        int heatRequired = property.getHeat();
        if (workCredit < heatRequired) {
            long remaining = heatRequired - workCredit;
            long requested = Math.min(remaining, Math.min(MAX_HEAT_RECEIVE, heatStorage.getHeatStored()));
            long consumed = heatStorage.consumeHeatInternal(requested, false);
            if (consumed <= 0) return;
            workCredit += consumed;
            lastHeatTick = currentTick();   // heat went into the run this tick (LIT)
        }
        if (workCredit >= heatRequired) {
            long creditBefore = workCredit, processedBefore = biomassProcessed;
            ResourceLocation recipeBefore = byproductRecipeId;
            workCredit -= heatRequired;
            biomassProcessed += property.getInput().amount();
            byproductRecipeId = holder.id();
            if (!operation.get().commit()) {
                workCredit = creditBefore; biomassProcessed = processedBefore; byproductRecipeId = recipeBefore;
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
        return (int) Math.min(getMaxProgress(), biomassProcessed);
    }

    /** The by-product interval of the last or current recipe (biomass: 500 mB). */
    public int getMaxProgress() {
        FermenterRecipe recipe = byproductRecipe();
        return recipe != null ? recipe.getByproductInterval() : FermenterRecipe.DEFAULT_BYPRODUCT_INTERVAL;
    }

    public long getHeatStored() {
        return heatStorage.getHeatStored();
    }

    /**
     * 0.1.10.24 (IC2 {@code GuiFermenter} "heat" gauge = heatBuffer / heat of the recipe): heat toward the current run,
     * at most one run. Before 0.1.10.24 the heat bar showed the 4000 HU store, which filled while the machine stood idle.
     */
    public long getRunHeat() {
        return Math.min(getRunHeatRequired(), Math.max(0, heatStorage.getHeatStored()) + workCredit);
    }

    /** The heat of one run: the input fluid's recipe, else the last run's, else 4000 HU (IC2 hU_per_run). */
    public int getRunHeatRequired() {
        FermenterRecipe recipe = currentProperty();
        if (recipe == null) recipe = byproductRecipe();
        return recipe != null ? recipe.getHeat() : HEAT_CAPACITY;
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
        if (byproductRecipeId != null) tag.putString("byproductRecipe", byproductRecipeId.toString());
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
        byproductRecipeId = tag.contains("byproductRecipe") ? ResourceLocation.tryParse(tag.getString("byproductRecipe")) : null;
        isWorking = level != null && level.isClientSide() && tag.getBoolean("isWorking");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // 0.1.10.17 (E-2): no client data. This used to send the whole save (inventory, tanks); the screen
        // gets those from its menu data, and nothing else reads them on the client.
        return new CompoundTag();
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

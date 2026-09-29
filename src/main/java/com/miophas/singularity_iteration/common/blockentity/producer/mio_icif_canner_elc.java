package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.recipe.IRecipeProcessingInfo;
import com.miophas.singularity_iteration.common.recipe.canner.CannerRecipeCache;
import com.miophas.singularity_iteration.common.recipe.canner.canning.CanningRecipe;
import com.miophas.singularity_iteration.common.recipe.canner.canning.CanningRecipeInput;
import com.miophas.singularity_iteration.common.recipe.canner.canning.DynamicCanningRecipe;
import com.miophas.singularity_iteration.common.item.build.CFSprayerItem;
import com.miophas.singularity_iteration.common.recipe.canner.mix.MixRecipe;
import com.miophas.singularity_iteration.common.recipe.canner.mix.MixRecipes;
import com.miophas.singularity_iteration.common.recipe.mio_icif_ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import com.miophas.singularity_iteration.common.block.producer.mio_icif_block_canner_elc;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 流体/固体装罐机方块实体类
 * 用于将流体装入容器或从容器中取出流体
 * 支持：电池输入、物品输入、物品输出、材料槽、流体输入槽、流体输出槽
 * 槽位结构
输入 + 1输出 + 1材料 + 1电池 + 4插件 + 2流体相关 = 10
 * 流体槽：输入流体
+ 输出流体
 * 
 * 四种工作模式
 * 1. CANNING - 装罐：将流体装入空单
 * 2. EMPTY_TO_TANK - 将单元内流体灌入水槽（输入流体槽
 * 3. FILL_FROM_TANK - 将水槽中流体灌满单元
 * 4. MIX - 混合流体与固体（使用水槽或单元）
 */
@SuppressWarnings("null")
public class mio_icif_canner_elc extends AbstractProcessingMachineBlockEntity implements com.miophas.singularity_iteration.core.api.fluid.ISeparateFluidPorts {

    // 工作模式枚举
    public enum Mode {
        CANNING(0, "canning"),           // 装罐
        EMPTY_TO_TANK(1, "empty_to_tank"), // 单元->水槽
        FILL_FROM_TANK(2, "fill_from_tank"), // 水槽->单元
        MIX(3, "mix");                    // 混合

        private final int id;
        private final String name;

        Mode(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public static Mode fromId(int id) {
            for (Mode mode : values()) {
                if (mode.id == id) {
                    return mode;
                }
            }
            return CANNING;
        }

        public Mode next() {
            return fromId((this.id + 1) % 4);
        }
    }

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .output(1)
        .extra(1)
        .battery()
        .upgrade(4)
        .fluidInput(1)
        .fluidOutput(1)
        .build();

    // 槽位数量
    public static final int SLOT_COUNT = 10;
    // 输入槽索引（待装罐物品）
    public static final int INPUT_SLOT = 0;
    // 输出槽索引（装罐完成物品
public static final int OUTPUT_SLOT = 1;
    // 材料槽索引（如空单元等辅助材料）
    public static final int MATERIAL_SLOT = 2;
    // 电池槽索
public static final int BATTERY_SLOT = 3;
    // 插件槽起始索引（4个插件槽
public static final int UPGRADE_SLOT_START = 4;

    // 默认配置（对
public static final long DEFAULT_CAPACITY = 800L;    // 4 EU/t × 200 ticks = 800 EU
    public static final long DEFAULT_MAX_RECEIVE = 32L;  // LV级最大输
public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 200; // 10秒（200 ticks
public static final long DEFAULT_ENERGY_PER_TICK = 4L; // 每tick消

    // 流体配置（mb = 毫桶
public static final int FLUID_CAPACITY = 8000; // 8000 mb = 8

    // CF喷枪补充配置：每次补充消耗的建筑泡沫流体量（mb
public static final int FOAM_REFILL_FLUID_AMOUNT = 1000; // 1000mb = 1
// CF喷枪补充配置：每次补充的泡沫点数
    public static final int FOAM_REFILL_AMOUNT = 64;

    // 流体存储 - 输入流体
public final FluidTank inputFluidTank;
    // 流体存储 - 输出流体
    protected final FluidTank outputFluidTank;
    // Capability lookups are frequent; this wrapper is a stable view over the
    // final tanks and can be reused safely.
    private final IFluidHandler combinedFluidHandler;
    private final CannerRecipeCache recipeCache = new CannerRecipeCache();
    private static final int[] INPUT_SLOTS = {INPUT_SLOT, MATERIAL_SLOT};
    private static final int[] OUTPUT_SLOTS = {OUTPUT_SLOT};
    private static final int[] TOP_SLOTS = {INPUT_SLOT};
    private static final int[] SIDE_SLOTS = {INPUT_SLOT, OUTPUT_SLOT, MATERIAL_SLOT, BATTERY_SLOT};

    // 当前工作模式
    private Mode currentMode = Mode.CANNING;

    // ==================== 配方驱动的处理节奏 ====================
    // 对齐 IC2：一次操作的“处理时间(=operationLength)”与“每 tick 能耗”由当前匹配到的
    // 配方决定（IRecipeProcessingInfo.getProcessingTime() / getEnergyPerTick()）。
    // EMPTY_TO_TANK / FILL_FROM_TANK 由通用流体能力驱动、没有数据包配方，使用机器默认值。
    /** 当前生效配方的处理时间（tick）。 */
    private int activeProcessingTime = DEFAULT_WORK_TIME;
    /** 当前生效配方的每 tick 能耗（EU）。 */
    private long activeEnergyPerTick = DEFAULT_ENERGY_PER_TICK;
    /** 上一 tick 生效的配方；变化时重置进度，避免用旧进度跑新配方。 */
    @Nullable
    private Object activeRecipe;

    /**
     * 用于 BlockEntityType.Builder 的构造函
 */
    public mio_icif_canner_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.CANNER_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_canner_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.LV);

        // 初始化输入流体存储，接受任何流体
        this.inputFluidTank = createFluidTank();
        
        // 初始化输出流体存储，接受任何流体
        this.outputFluidTank = createFluidTank();
        this.combinedFluidHandler = com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup
            .inputOutput(inputFluidTank, outputFluidTank);
    }

    public mio_icif_canner_elc(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                long capacity, long maxReceive, long maxExtract,
                                int workTime, long energyPerTick) {
        super(pos, state, type, capacity, maxReceive, maxExtract, workTime, LAYOUT, energyPerTick, CableTier.LV);

        // 初始化输入流体存
    this.inputFluidTank = createFluidTank();
        
        // 初始化输出流体存
    this.outputFluidTank = createFluidTank();
        this.combinedFluidHandler = com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup
            .inputOutput(inputFluidTank, outputFluidTank);
    }

    private FluidTank createFluidTank() {
        return new FluidTank(FLUID_CAPACITY, fluidStack -> true) {
            @Override
            protected void onContentsChanged() {
                // Defer visible state changes until the tick has committed all tank/item mutations.
                setChanged();
                com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank.markUnsaved(mio_icif_canner_elc.this);
            }
        };
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.canner_elc");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> isValidInput(stack);
            case OUTPUT_SLOT -> false; // 输出槽不允许手动放入
            case MATERIAL_SLOT -> isValidMaterial(stack);
            case BATTERY_SLOT -> isBattery(stack);
            default -> {
                if (slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + 4) {
                    yield getItemAPI().isUpgrade(stack);
                }
                yield false;
            }
        };
    }

    /**
     * 检查物品是否是有效的输入物
 * 根据当前模式返回不同的验证结
 * @param stack 物品
 * @return 是否有效
     */
    private boolean isValidInput(ItemStack stack) {
        return acceptsCannerInput(level, currentMode, stack);
    }

    public static boolean acceptsCannerInput(@Nullable Level level, Mode mode, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return switch (mode) {
            case CANNING -> hasMatchingCanningCan(level, stack);
            case EMPTY_TO_TANK -> {
                yield (mio_icif_cells.isFluidCell(stack) && !mio_icif_cells.isEmptyCell(stack))
                    || stack.getItem() instanceof net.minecraft.world.item.BucketItem;
            }
            case FILL_FROM_TANK -> {
                yield mio_icif_cells.isEmptyCell(stack)
                    || stack.is(net.minecraft.world.item.Items.BUCKET);
            }
            // 容器槽：只放还能继续装液的流体容器（空单元 / 部分填充单元 / 空桶）
            case MIX -> isMixContainer(stack);
        };
    }

    /**
     * 混合模式容器槽的准入判定。
     *
     * <p>对齐 IC2 {@code InvSlotConsumableLiquid(OpType.Both)} 的可灌一侧：
     * 已满的容器不能再接收产出流体，因此拒绝。
     */
    public static boolean isMixContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(net.minecraft.world.item.Items.BUCKET)) return true;
        return stack.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IFluidCellItem cell
            && !cell.isFull(stack);
    }

    private static boolean hasMatchingCanningCan(@Nullable Level level, ItemStack can) {
        if (level == null) return true;
        var rm = level.getRecipeManager();
        for (var holder : rm.getAllRecipesFor(mio_icif_ModRecipes.CANNING_TYPE.get())) {
            if (holder.value().getCanIngredient().test(can)) return true;
        }
        for (var holder : rm.getAllRecipesFor(mio_icif_ModRecipes.DYNAMIC_CANNING_TYPE.get())) {
            if (holder.value().getCanIngredient().test(can)) return true;
        }
        return false;
    }

    private boolean isFilledBucket(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BucketItem;
    }

    /**
     * 检查物品是否是有效的材
 * 根据当前模式返回不同的验证结
 * @param stack 物品
 * @return 是否有效
     */
    private boolean isValidMaterial(ItemStack stack) {
        if (currentMode == Mode.MIX) {
            return !stack.isEmpty()
                && (stack.getItem() instanceof CFSprayerItem || hasAnyMixRecipeForMaterial(level, stack));
        }
        return acceptsCannerMaterial(level, currentMode, inputFluidTank.getFluid(), itemHandler.getStackInSlot(INPUT_SLOT), stack);
    }

    /** Uses the caller's fluid snapshot rather than reading a different inventory's state. */
    public static boolean acceptsCannerMaterial(@Nullable Level level, Mode mode,
            FluidStack inputFluid, ItemStack inputCan, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return switch (mode) {
            case CANNING -> hasMatchingCanningFood(level, inputCan, stack);
            case EMPTY_TO_TANK -> {
                yield false;
            }
            case FILL_FROM_TANK -> {
                yield false;
            }
            case MIX -> {
                // 对齐 IC2：添加剂准入只看是否属于某条混合配方，不要求储罐此刻已有液体
                yield stack.getItem() instanceof CFSprayerItem
                    || hasAnyMixRecipeForMaterial(level, stack);
            }
        };
    }

    /** 该物品是否可作为任意一条混合配方的添加剂（不检查储罐状态）。 */
    private static boolean hasAnyMixRecipeForMaterial(@Nullable Level level, ItemStack material) {
        if (level == null) return true;
        if (material.isEmpty()) return false;
        for (RecipeHolder<MixRecipe> holder : level.getRecipeManager().getAllRecipesFor(MixRecipes.MIX_TYPE.get())) {
            if (ingredientMatches(holder.value().getMaterialIngredient(), material)) return true;
        }
        return false;
    }

    private static boolean hasMatchingCanningFood(@Nullable Level level, ItemStack inputCan, ItemStack food) {
        if (level == null) return true;
        var rm = level.getRecipeManager();
        for (var holder : rm.getAllRecipesFor(mio_icif_ModRecipes.CANNING_TYPE.get())) {
            var recipe = holder.value();
            if (recipe.getFoodIngredient().test(food)) {
                if (inputCan.isEmpty() || recipe.getCanIngredient().test(inputCan)) return true;
            }
        }
        for (var holder : rm.getAllRecipesFor(mio_icif_ModRecipes.DYNAMIC_CANNING_TYPE.get())) {
            var recipe = holder.value();
            if (recipe.getFoodIngredient().test(food)) {
                if (inputCan.isEmpty() || recipe.getCanIngredient().test(inputCan)) return true;
            }
        }
        return false;
    }

    private static boolean ingredientMatches(Ingredient ingredient, ItemStack stack) {
        if (ingredient.test(stack)) return true;
        for (ItemStack matching : ingredient.getItems()) {
            if (stack.getItem() == matching.getItem()) return true;
        }
        return false;
    }

    /**
     * 覆盖默认的漏斗槽位方
 * 定义各方向可访问的槽
 */
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // 根据方向返回不同的槽
    return switch (side) {
            case UP -> TOP_SLOTS; // 上方：输入槽
            case DOWN -> OUTPUT_SLOTS; // 下方：输出槽
            case NORTH, SOUTH, EAST, WEST -> SIDE_SLOTS;
        };
    }

    @Override
    protected int[] getInputSlots() {
        return INPUT_SLOTS;
    }

    @Override
    protected int[] getOutputSlots() {
        return OUTPUT_SLOTS;
    }

    /**
     * 覆盖默认的插入检查方
 */
    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        // 输出槽不能插
    if (slot == OUTPUT_SLOT) {
            return false;
        }

        // 电池槽：只接受电池类物品
        if (slot == BATTERY_SLOT) {
            return isBattery(stack);
        }

        // 输入槽：接受有效输入物品
        if (slot == INPUT_SLOT) {
            return isValidInput(stack);
        }

        // 材料槽：接受有效材料
        if (slot == MATERIAL_SLOT) {
            return isValidMaterial(stack);
        }

        if (slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + 4) {
            return getItemAPI().isUpgrade(stack);
        }

        return false;
    }

    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    /**
     * 覆盖默认的提取检查方
 */
    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        // 只有输出槽可以提
    return slot == OUTPUT_SLOT;
    }

    /**
     * 检查当前输入是否有有效的配
     * 用于判断当输入物品变更时是否应该重置进度
     */
    @Override
    protected boolean hasValidRecipe() {
        return switch (currentMode) {
            case CANNING -> findCanningRecipe().isPresent();
            case MIX -> findMixRecipe().isPresent();
            // EMPTY_TO_TANK / FILL_FROM_TANK 由通用流体能力驱动，没有数据包配方
            case EMPTY_TO_TANK, FILL_FROM_TANK -> true;
        };
    }

    /**
     * 检查机器是否可以工
 * @return 是否可以工作
     */
    @Override
    protected boolean canWork() {
        // 先按当前模式解析配方并把其处理时间/能耗应用到本 tick，再做能量与可行性判断
        refreshRecipeTiming();

        // 检查是否有足够能量
        if (!hasEnoughEnergy()) {
            return false;
        }

        // 单元水槽模式特殊处理：直接从
        if (currentMode == Mode.EMPTY_TO_TANK) {
            return canWorkEmptyToTank();
        }

        // 水槽单元模式特殊处理：从输入液体槽填充空单元/
    if (currentMode == Mode.FILL_FROM_TANK) {
            return canWorkFillFromTank();
        }

        // 混合模式特殊处理
        if (currentMode == Mode.MIX) {
            return canWorkMix();
        }

        // 检查输入槽（混合模式不需要输入槽有物品）
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }

        // 检查是否有匹配的配方
        Optional<?> recipeOptional = findCanningRecipe();
        if (recipeOptional.isEmpty()) {
            return false;
        }

        // 检查输出槽是否有空
    ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (!output.isEmpty()) {
            // 获取配方预期输出
            ItemStack expectedOutput = getExpectedOutput(recipeOptional.get());
            if (expectedOutput.isEmpty()) {
                return false;
            }

            // 检查是否可以堆叠：物品类型相同且未达到最大堆叠数
            if (!ItemStack.isSameItemSameComponents(output, expectedOutput)) {
                return false; // 物品类型不同，无法堆
        }

            // 检查是否有足够空间容纳新产出的物品
            int outputCount = getOutputCount(recipeOptional.get());
            if (output.getCount() + outputCount > output.getMaxStackSize()) {
                return false; // 超过最大堆叠数
            }
        }

        return true;
    }

    /**
     * 解析当前模式匹配的数据包配方，并把它的处理时间/能耗应用到本 tick。
     *
     * <p>EMPTY_TO_TANK / FILL_FROM_TANK 由通用流体能力驱动，没有配方，使用机器默认值。
     */
    private void refreshRecipeTiming() {
        Object recipe = null;
        if (level != null && !level.isClientSide()) {
            recipe = switch (currentMode) {
                case CANNING -> findCanningRecipe().orElse(null);
                case MIX -> findMixRecipe().orElse(null);
                case EMPTY_TO_TANK, FILL_FROM_TANK -> null;
            };
        }
        applyRecipeTiming(recipe);
    }

    /**
     * 应用配方的处理时间与每 tick 能耗。
     *
     * <p>配方变化时重置进度：否则会把旧配方上累积的进度带进新配方。
     * 未实现 {@link IRecipeProcessingInfo} 或没有配方时回退到机器默认值。
     */
    private void applyRecipeTiming(@Nullable Object recipe) {
        int processingTime = DEFAULT_WORK_TIME;
        long energyPerTick = DEFAULT_ENERGY_PER_TICK;
        if (recipe instanceof RecipeHolder<?> holder
                && holder.value() instanceof IRecipeProcessingInfo info) {
            processingTime = Math.max(1, info.getProcessingTime());
            energyPerTick = Math.max(0L, info.getEnergyPerTick());
        }

        if (recipe != activeRecipe) {
            activeRecipe = recipe;
            progress = 0;
        }

        activeProcessingTime = processingTime;
        activeEnergyPerTick = energyPerTick;
        if (maxProgress != processingTime) {
            maxProgress = processingTime;
            if (progress > processingTime) {
                progress = processingTime;
            }
        }
    }

    @Override
    protected void updateProcessingParameters() {
        // 处理时间由当前配方决定（见 applyRecipeTiming），不能被基类重置回 baseMaxProgress；
        // 超频升级通过 getProgressPerTick() 的进度加速体现。
        int target = Math.max(1, activeProcessingTime);
        if (maxProgress != target) {
            maxProgress = target;
            if (progress > target) {
                progress = target;
            }
        }
    }

    @Override
    public long getEffectiveEnergyPerTick() {
        return upgradeStats.getEnergyPerTick(Math.max(0L, activeEnergyPerTick));
    }

    /**
     * 检查单元水槽模式是否可以工
 * 从输入槽的桶/单元中提取液体到输出液体
 */
    private boolean canWorkEmptyToTank() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);

        // 获取输入物品的流体内
        FluidStack fluid = getInputFluidContent(input);
        if (fluid.isEmpty()) {
            return false; // 输入物品没有流体
        }
        // A bucket is a whole-container operation. A third-party bucket that
        // can only simulate a partial drain must stay in the input slot.
        if (input.getItem() instanceof net.minecraft.world.item.BucketItem && fluid.getAmount() < 1000) {
            return false;
        }

        // 检查输出流体槽是否有足够空
    int filled = outputFluidTank.fill(fluid, IFluidHandler.FluidAction.SIMULATE);
        if (filled < fluid.getAmount()) {
            return false; // 输出流体槽空间不
    }

        // 检查输出槽是否有空间放置空容器
        ItemStack emptyContainer = getEmptyContainerOutput(input);
        if (emptyContainer.isEmpty()) {
            return false; // 无法获取对应的空容器
        }

        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (!currentOutput.isEmpty()) {
            // 检查是否可以堆
        if (!ItemStack.isSameItemSameComponents(currentOutput, emptyContainer)) {
                return false; // 物品类型不同，无法堆
        }
            if (currentOutput.getCount() + 1 > currentOutput.getMaxStackSize()) {
                return false; // 超过最大堆叠数
            }
        }

        return true;
    }

    /**
     * 检查水槽单元模式是否可以工作
     * 从输入液体槽获取液体，填充到空单元/桶
     */
    private boolean canWorkFillFromTank() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);

        // 检查输入槽是否为空单元或空桶
        boolean isEmptyCell = input.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IFluidCellItem cellItem
            && cellItem.getFluid(input).isEmpty();
        boolean isPartiallyFilledCell = input.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IFluidCellItem cellItem
            && !cellItem.getFluid(input).isEmpty()
            && !cellItem.isFull(input);
        boolean isEmptyBucket = input.is(net.minecraft.world.item.Items.BUCKET);

        if (!isEmptyCell && !isPartiallyFilledCell && !isEmptyBucket) {
            return false;
        }

        // 检查输入液体槽是否有液体
        if (inputFluidTank.isEmpty()) {
            return false;
        }

        FluidStack fluid = inputFluidTank.getFluid();

        // 对于单元：检查是否可以接受该流体
        if (input.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IFluidCellItem cellItem) {
            if (!cellItem.canHoldFluid(fluid.getFluid())) {
                return false;
            }
            FluidStack currentFluid = cellItem.getFluid(input);
            if (!currentFluid.isEmpty() && currentFluid.getFluid() != fluid.getFluid()) {
                return false;
            }
            if (cellItem.isFull(input)) {
                return false;
            }
        }

        // 对于桶：仍然需要 1000mB
        if (isEmptyBucket && fluid.getAmount() < 1000) {
            return false;
        }

        // 获取预期的输出容器
        ItemStack filledContainer = getFilledContainerOutput(input, fluid);
        if (filledContainer.isEmpty() && !isEmptyCell && !isPartiallyFilledCell) {
            return false;
        }

        // 检查输出槽是否有空间
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (!currentOutput.isEmpty()) {
            ItemStack expectedOutput;
            if (isEmptyCell || isPartiallyFilledCell) {
                expectedOutput = input.copy();
                expectedOutput.setCount(1);
                if (input.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IFluidCellItem cellItem) {
                    FluidStack currentCellFluid = cellItem.getFluid(input);
                    int remainingCapacity = cellItem.getCapacity() - currentCellFluid.getAmount();
                    int fillAmount = Math.min(remainingCapacity, fluid.getAmount());
                    if (fillAmount > 0) {
                        cellItem.fill(expectedOutput, new FluidStack(fluid.getFluid(), fillAmount), false);
                    }
                }
            } else {
                expectedOutput = filledContainer;
            }
            if (!ItemStack.isSameItemSameComponents(currentOutput, expectedOutput)) {
                return false;
            }
            if (currentOutput.getCount() + 1 > currentOutput.getMaxStackSize()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取水槽单元模式的预期满容器输出
     * 根据输入的空容器和流体类型返回对应的满容器
     * 使用 IFluidCellItem API 支持任何实现该接口的容器
     */
    private ItemStack getFilledContainerOutput(ItemStack emptyContainer, FluidStack fluid) {
        if (emptyContainer.isEmpty() || fluid.isEmpty()) {
            return ItemStack.EMPTY;
        }

        net.minecraft.world.level.material.Fluid fluidType = fluid.getFluid();

        // 如果是空桶
        if (emptyContainer.is(net.minecraft.world.item.Items.BUCKET)) {
            Item bucketItem = fluidType.getBucket();
            if (bucketItem != null && bucketItem != net.minecraft.world.item.Items.BUCKET) {
                return new ItemStack(bucketItem);
            }
            return ItemStack.EMPTY;
        }

        // 使用 IFluidCellItem API 获取填充后的容器
        // 这支持任何实现 IFluidCellItem 接口的容器，包括附属模组的单元
        if (emptyContainer.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IFluidCellItem cellItem) {
            return cellItem.getFilledContainer(emptyContainer, fluidType);
        }

        return ItemStack.EMPTY;
    }

    /**
     * 检查混合模式是否可以工
 * 特殊功能：CF喷枪补充 - 材料槽放CF喷枪 + 输入液体槽有建筑泡沫流体 
补充喷枪泡沫
     * 普通功能：输入液体槽有液体 + 材料槽有材料 -> 输出液体槽产生混合液
 * 如果输入槽有空单元，则输出装满的单元
     */
    private boolean canWorkMix() {
        ItemStack material = itemHandler.getStackInSlot(MATERIAL_SLOT);
        ItemStack inputSlotItem = itemHandler.getStackInSlot(INPUT_SLOT);

        // 检查材料槽
        if (material.isEmpty()) {
            return false;
        }

        // 检查输入液体槽是否有液
    if (inputFluidTank.isEmpty()) {
            return false;
        }

        // ===== CF喷枪补充特殊处理 =====
        if (material.getItem() instanceof CFSprayerItem sprayer) {
            // 材料槽是CF喷枪，检查输入液体槽是否为建筑泡沫流
        FluidStack inputFluid = inputFluidTank.getFluid();
            if (inputFluid.getFluid() != com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids.CONSTRUCTIONFOAM.get()) {
                return false; // 不是建筑泡沫流体
            }
            // 检查喷枪是否已
        if (sprayer.isFull(material)) {
                return false; // 喷枪已满，无需补充
            }
            if (sprayer.getFoamAmount(material) > CFSprayerItem.MAX_FOAM - FOAM_REFILL_AMOUNT) {
                return false; // 一次补充必须完整容纳，避免扣液后静默截断
            }
            // 检查流体是否足够（每次补充消
            if (inputFluid.getAmount() < FOAM_REFILL_FLUID_AMOUNT) {
                return false;
            }
            return true;
        }

        // ===== 普通混合配方 =====
        // 对齐 IC2 EnrichLiquid：先尽力灌满“容器槽”里的容器，余量再进输出液体槽；
        // 容器槽允许留空（此时产出全部进输出液体槽）。
        Optional<RecipeHolder<MixRecipe>> recipeHolder = findMixRecipe();
        if (recipeHolder.isEmpty()) {
            return false;
        }

        MixRecipe recipe = recipeHolder.get().value();
        if (material.getCount() < recipe.getMaterialCount()) {
            return false;
        }

        return simulateMix(recipe, inputSlotItem) != null;
    }

    /** 一次混合的模拟结果：要落进输出槽的容器、要进输出罐的余量、要消耗的输入流体。 */
    private record MixOutcome(ItemStack filledContainer, FluidStack tankFluid, FluidStack drainedInput) {
    }

    /**
     * 模拟一次混合产出，完全无副作用（容器操作在副本上进行）。
     *
     * <p>对齐 IC2 EnrichLiquid 的 {@code getOutput()}：先尝试把产出流体灌进容器槽的容器，
     * 装不下的余量必须能全部进输出液体槽；两者都成立才返回结果，否则返回 {@code null}。
     * 容器槽为空或容器已满时，产出全部进输出液体槽。
     */
    @Nullable
    private MixOutcome simulateMix(MixRecipe recipe, ItemStack container) {
        FluidStack required = recipe.getInputFluid();
        FluidStack result = recipe.getResultFluid();
        if (required.isEmpty() || result.isEmpty()) {
            return null;
        }
        if (!canDrainFluidExactly(inputFluidTank, required)) {
            return null;
        }

        FluidStack remaining = result.copy();
        ItemStack filledContainer = ItemStack.EMPTY;

        if (!container.isEmpty()) {
            ItemStack single = container.copyWithCount(1);
            IFluidHandlerItem handler = single.getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
            if (handler != null) {
                int accepted = handler.fill(remaining.copy(), IFluidHandler.FluidAction.SIMULATE);
                if (accepted > 0) {
                    handler.fill(remaining.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
                    remaining.shrink(accepted);
                    ItemStack produced = handler.getContainer();
                    // 与原容器完全一致（例如已满/无法容纳）时不产出，避免凭空多给一个物品
                    if (!produced.isEmpty()
                            && !ItemStack.isSameItemSameComponents(produced, single)) {
                        if (!canAcceptItemOutput(produced, 1)) {
                            return null;
                        }
                        filledContainer = produced;
                    }
                }
            }
        }

        if (!remaining.isEmpty() && !canFillFluidExactly(outputFluidTank, remaining)) {
            return null;
        }

        return new MixOutcome(filledContainer, remaining, required);
    }

    /**
     * 获取配方预期输出物品（用于检查堆叠）
     */
    private ItemStack getExpectedOutput(Object recipe) {
        if (recipe instanceof RecipeHolder<?> holder) {
            Object recipeValue = holder.value();

            if (recipeValue instanceof DynamicCanningRecipe dynamicRecipe) {
                ItemStack inputCan = itemHandler.getStackInSlot(INPUT_SLOT);
                ItemStack material = itemHandler.getStackInSlot(MATERIAL_SLOT);
                CanningRecipeInput recipeInput = recipeCache.canningInput(inputCan, material);
                return dynamicRecipe.assemble(recipeInput, level.registryAccess());
            } else if (recipeValue instanceof CanningRecipe canningRecipe) {
                return canningRecipe.assemble(null, level.registryAccess());
            } else if (recipeValue instanceof MixRecipe mixRecipe) {
                return mixRecipe.assemble(null, level.registryAccess());
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 获取单元水槽模式的预期空容器输出
     * 根据输入槽的物品返回对应的空容器（空桶或空单元）
     * 使用 IFluidCellItem API 支持任何实现该接口的容器
     */
    private ItemStack getEmptyContainerOutput(ItemStack input) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (input.getItem() instanceof net.minecraft.world.item.BucketItem) {
            return new ItemStack(net.minecraft.world.item.Items.BUCKET);
        }

        if (input.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IFluidCellItem cellItem) {
            return cellItem.getEmptyContainer(input);
        }

        return ItemStack.EMPTY;
    }

    /**
     * 获取输入物品的流体内容
     * 使用 IFluidCellItem API 支持任何实现该接口的容器
     */
    private FluidStack getInputFluidContent(ItemStack input) {
        if (input.isEmpty()) {
            return FluidStack.EMPTY;
        }

        if (input.getItem() instanceof net.minecraft.world.item.BucketItem) {
            IFluidHandlerItem fluidHandler = input.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
            if (fluidHandler != null) {
                FluidStack drained = fluidHandler.drain(1000, IFluidHandler.FluidAction.SIMULATE);
                if (!drained.isEmpty()) {
                    return drained;
                }
            }
        }

        if (input.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IFluidCellItem cellItem) {
            FluidStack content = cellItem.getFluid(input);
            if (!content.isEmpty()) {
                return content;
            }
        }

        return FluidStack.EMPTY;
    }

    /**
     * 获取配方的真实产出数量。
     * 统一以 {@code assemble().getCount()} 为准，避免预检数量与实际产出不一致。
     */
    private int getOutputCount(Object recipe) {
        ItemStack expected = getExpectedOutput(recipe);
        return expected.isEmpty() ? 1 : expected.getCount();
    }

    /**
     * 查找装罐配方（优先查找动态配方，如果没有则查找普通配方）
     */
    private Optional<?> findCanningRecipe() {
        ItemStack inputCan = itemHandler.getStackInSlot(INPUT_SLOT);
        ItemStack material = itemHandler.getStackInSlot(MATERIAL_SLOT);

        if (inputCan.isEmpty() || material.isEmpty()) {
            return Optional.empty();
        }

        CanningRecipeInput recipeInput = recipeCache.canningInput(inputCan, material);

        // 首先尝试查找动态配方
        Optional<RecipeHolder<DynamicCanningRecipe>> dynamicRecipe = recipeCache.find(level.getRecipeManager(),
            mio_icif_ModRecipes.DYNAMIC_CANNING_TYPE.get(), recipeInput, level);

        if (dynamicRecipe.isPresent()) {
            return dynamicRecipe;
        }

        // 如果没有动态配方，查找普通配方
        return recipeCache.find(level.getRecipeManager(), mio_icif_ModRecipes.CANNING_TYPE.get(), recipeInput, level);
    }

    /**
     * 查找混合配方
     */
    private Optional<RecipeHolder<MixRecipe>> findMixRecipe() {
        ItemStack material = itemHandler.getStackInSlot(MATERIAL_SLOT);

        if (material.isEmpty()) {
            return Optional.empty();
        }

        FluidStack inputFluid = inputFluidTank.getFluid();
        if (inputFluid.isEmpty()) {
            return Optional.empty();
        }

        for (RecipeHolder<MixRecipe> holder : recipeCache.recipes(level.getRecipeManager(), MixRecipes.MIX_TYPE.get())) {
            if (holder.value() instanceof MixRecipe mixRecipe) {
                if (!ingredientMatches(mixRecipe.getMaterialIngredient(), material)) {
                    continue;
                }
                if (material.getCount() < mixRecipe.getMaterialCount()) {
                    continue;
                }
                FluidStack requiredFluid = mixRecipe.getInputFluid();
                if (inputFluid.getFluid() == requiredFluid.getFluid()
                        && inputFluid.getAmount() >= requiredFluid.getAmount()) {
                    return Optional.of(holder);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 执行生产工作
     */
    @Override
    protected void doWork() {
        // 消耗能
    if (!consumeEnergy()) {
            stopWork();
            return;
        }

        isWorking = true;

        // 检查是否完
    if (progress >= maxProgress) {
            finishCanning();
        }
    }

    /**
     * 完成装罐操作
     */
    private void finishCanning() {
        // 混合模式的容器槽允许为空，其余模式必须有输入物品
        if (currentMode != Mode.MIX) {
            ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
            if (input.isEmpty()) {
                progress = 0;
                stopWork();
                return;
            }
        }

        // 根据当前模式执行不同的配方处理
        boolean success = switch (currentMode) {
            case CANNING -> processCanningRecipe();
            case EMPTY_TO_TANK -> processEmptyToTankRecipe();
            case FILL_FROM_TANK -> processFillFromTankRecipe();
            case MIX -> processMixRecipe();
        };

        if (!success) {
            // 必须重置进度，否则会在 maxProgress 处反复扣电却永远不产出
            progress = 0;
            stopWork();
            return;
        }

        // 重置进度
        finishWork();

        // 检查是否还可以继续工作
        if (canWork()) {
            isWorking = true;
        }
    }

    /**
     * 处理装罐配方
     */
    private boolean processCanningRecipe() {
        Optional<?> recipeHolder = findCanningRecipe();
        if (recipeHolder.isEmpty()) {
            return false;
        }

        ItemStack inputCan = itemHandler.getStackInSlot(INPUT_SLOT);
        ItemStack material = itemHandler.getStackInSlot(MATERIAL_SLOT);

        Object recipe = recipeHolder.get();
        if (recipe instanceof RecipeHolder<?> holder) {
            Object recipeValue = holder.value();

            if (recipeValue instanceof DynamicCanningRecipe dynamicRecipe) {
                // 处理动态配
                CanningRecipeInput recipeInput = recipeCache.canningInput(inputCan, material);
                int requiredCans = dynamicRecipe.getRequiredCanCount(recipeInput);
                int outputCount = dynamicRecipe.getOutputCount(recipeInput);

                // 检查是否有足够的锡
            if (requiredCans <= 0 || outputCount <= 0 || inputCan.getCount() < requiredCans
                    || material.getCount() < 1
                    ) {
                    return false;
                }

                ItemStack result = dynamicRecipe.assemble(recipeInput, level.registryAccess());
                if (result.isEmpty()) {
                    return false;
                }
                result.setCount(Math.min(outputCount, 64)); // 保持动态配方的产出数量语义
                if (result.getCount() <= 0
                    || !canAcceptItemOutput(result, result.getCount())) {
                    return false;
                }

                // 所有输出检查完成后才提交输入物和产出。
                inputCan.shrink(requiredCans);
                material.shrink(1);
                insertItemOutput(result, result.getCount());

                return true;
            } else if (recipeValue instanceof CanningRecipe canningRecipe) {
                // 处理普通配
                // 输出结果（支持堆叠）
                ItemStack result = canningRecipe.assemble(null, level.registryAccess());
                int outputCount = result.getCount();
                if (inputCan.getCount() < 1 || material.getCount() < 1
                        || result.isEmpty() || outputCount <= 0
                        || !canAcceptItemOutput(result, outputCount)) {
                    return false;
                }

                inputCan.shrink(1);
                material.shrink(1);
                insertItemOutput(result, outputCount);

                return true;
            }
        }

        return false;
    }

    /**
     * 处理单元灌入水槽配方
     * 从输入槽的桶/单元中提取液体到输出液体槽，输出槽得到空容器
     */
    private boolean processEmptyToTankRecipe() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);

        // 获取输入物品的流体内
    FluidStack fluid = getInputFluidContent(input);
        if (fluid.isEmpty()) {
            return false; // 输入物品没有流体
        }
        if (input.getItem() instanceof net.minecraft.world.item.BucketItem && fluid.getAmount() < 1000) {
            return false;
        }

        // 检查输出流体槽是否有足够空
    int filled = outputFluidTank.fill(fluid, IFluidHandler.FluidAction.SIMULATE);
        if (filled < fluid.getAmount()) {
            return false; // 输出流体槽空间不
    }

        // 获取对应的空容器
        ItemStack emptyContainer = getEmptyContainerOutput(input);
        if (emptyContainer.isEmpty()) {
            return false; // 无法获取对应的空容器
        }
        // A single input container always yields one empty container. Do not
        // propagate an accidental stack count from a custom cell implementation.
        emptyContainer = emptyContainer.copy();
        emptyContainer.setCount(1);

        // The output slot may have changed while the machine was working. Do
        // every output check before consuming either the input item or fluid.
        if (!canAcceptItemOutput(emptyContainer, 1)) {
            return false;
        }

        // Commit the fluid first only after both output destinations have been
        // validated. A failed execute is rolled back to avoid partial transfer.
        if (!fillFluidExactly(outputFluidTank, fluid)) {
            return false;
        }

        input.shrink(1);
        insertItemOutput(emptyContainer, 1);

        return true;
    }

    /**
     * 处理水槽灌满单元配方
     * 从输入液体槽获取液体，填充到空单

 */
    private boolean processFillFromTankRecipe() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);

        if (inputFluidTank.isEmpty()) {
            return false;
        }

        FluidStack tankFluid = inputFluidTank.getFluid();

        // 处理流体单元（支持部分填充）
        if (input.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IFluidCellItem cellItem) {
            FluidStack currentCellFluid = cellItem.getFluid(input);

            // 验证流体兼容性
            if (!currentCellFluid.isEmpty() && currentCellFluid.getFluid() != tankFluid.getFluid()) {
                return false;
            }
            if (!cellItem.canHoldFluid(tankFluid.getFluid())) {
                return false;
            }
            if (cellItem.isFull(input)) {
                return false;
            }

            // 计算可填充量
            int remainingCapacity = cellItem.getCapacity() - currentCellFluid.getAmount();
            int fillAmount = Math.min(remainingCapacity, tankFluid.getAmount());

            if (fillAmount <= 0) {
                return false;
            }

            ItemStack outputItem = input.copy();
            outputItem.setCount(1);
            FluidStack requested = new FluidStack(tankFluid.getFluid(), fillAmount);
            if (cellItem.fill(outputItem, requested, true) < fillAmount) {
                return false;
            }
            if (cellItem.fill(outputItem, requested, false) < fillAmount) {
                return false;
            }

            // Validate the detached result before touching the machine input.
            if (!canAcceptItemOutput(outputItem, 1)
                    || !canDrainFluidExactly(inputFluidTank, requested)) {
                return false;
            }

            if (!drainFluidExactly(inputFluidTank, requested)) {
                return false;
            }
            input.shrink(1);
            insertItemOutput(outputItem, 1);

            return true;
        }

        // 处理桶（固定 1000mB）
        if (input.is(net.minecraft.world.item.Items.BUCKET)) {
            if (tankFluid.getAmount() < 1000) {
                return false;
            }

            ItemStack filledContainer = getFilledContainerOutput(input, tankFluid);
            if (filledContainer.isEmpty()) {
                return false;
            }

            if (!canAcceptItemOutput(filledContainer, 1)) {
                return false;
            }

            FluidStack requested = new FluidStack(tankFluid.getFluid(), 1000);
            if (!canDrainFluidExactly(inputFluidTank, requested)
                    || !drainFluidExactly(inputFluidTank, requested)) {
                return false;
            }
            input.shrink(1);
            insertItemOutput(filledContainer, 1);

            return true;
        }

        return false;
    }

    /**
     * 处理混合配方
     * 特殊功能：CF喷枪补充 - 消耗输入液体槽的建筑泡沫流体来给材料槽的CF喷枪补充泡沫
     * 普通功能：输入液体槽的液体 + 材料槽的材料 -> 输出液体槽产生混合液
 * 如果输入槽有空单元，则输出装满的单元
     */
    private boolean processMixRecipe() {
        ItemStack material = itemHandler.getStackInSlot(MATERIAL_SLOT);

        // ===== CF喷枪补充特殊处理 =====
        if (material.getItem() instanceof CFSprayerItem sprayer) {
            // 检查输入液体槽是否为建筑泡沫流
        FluidStack inputFluid = inputFluidTank.getFluid();
            if (inputFluid.getFluid() != com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids.CONSTRUCTIONFOAM.get()) {
                return false;
            }
            if (inputFluid.getAmount() < FOAM_REFILL_FLUID_AMOUNT) {
                return false;
            }
            if (sprayer.isFull(material)) {
                return false;
            }
            if (sprayer.getFoamAmount(material) > CFSprayerItem.MAX_FOAM - FOAM_REFILL_AMOUNT) {
                return false;
            }

            FluidStack inputBefore = inputFluid.copy();
            int foamBefore = sprayer.getFoamAmount(material);
            FluidStack requested = new FluidStack(inputFluid.getFluid(), FOAM_REFILL_FLUID_AMOUNT);
            if (!canDrainFluidExactly(inputFluidTank, requested)
                    || !drainFluidExactly(inputFluidTank, requested)) {
                return false;
            }

            // 补充CF喷枪泡沫；若实现拒绝完整数量，恢复流体快照。
            int added;
            try {
                added = sprayer.addFoam(material, FOAM_REFILL_AMOUNT);
            } catch (RuntimeException failure) {
                sprayer.setFoamAmount(material, foamBefore);
                inputFluidTank.setFluid(inputBefore);
                return false;
            }
            if (added != FOAM_REFILL_AMOUNT) {
                sprayer.setFoamAmount(material, foamBefore);
                inputFluidTank.setFluid(inputBefore);
                return false;
            }

            return true;
        }

        // ===== 普通混合配方 =====
        Optional<RecipeHolder<MixRecipe>> recipeHolder = findMixRecipe();
        if (recipeHolder.isEmpty()) {
            return false;
        }

        MixRecipe recipe = recipeHolder.get().value();
        ItemStack inputSlotItem = itemHandler.getStackInSlot(INPUT_SLOT);

        // 完成时重新校验可变的库存状态：查找配方后自动化可能已改动槽位。
        if (material.getCount() < recipe.getMaterialCount()) {
            return false;
        }

        // 对齐 IC2 EnrichLiquid：先灌容器，余量进输出罐。容器槽为空是合法情况。
        MixOutcome outcome = simulateMix(recipe, inputSlotItem);
        if (outcome == null) {
            return false;
        }

        // 所有输出侧检查完成后才提交输入：任一步失败则整体回滚
        FluidStack inputBefore = inputFluidTank.getFluid().copy();
        FluidStack outputBefore = outputFluidTank.getFluid().copy();
        if (!drainFluidExactly(inputFluidTank, outcome.drainedInput())) {
            return false;
        }
        if (!outcome.tankFluid().isEmpty() && !fillFluidExactly(outputFluidTank, outcome.tankFluid())) {
            inputFluidTank.setFluid(inputBefore);
            outputFluidTank.setFluid(outputBefore);
            return false;
        }

        material.shrink(recipe.getMaterialCount());
        if (!outcome.filledContainer().isEmpty() && !inputSlotItem.isEmpty()) {
            inputSlotItem.shrink(1);
            insertItemOutput(outcome.filledContainer(), 1);
        }

        return true;
    }

    /**
     * Checks the current output slot without mutating it. This check is made
     * at completion time because automation may have changed the slot while a
     * recipe was in progress.
     */
    private boolean canAcceptItemOutput(ItemStack candidate, int count) {
        if (candidate.isEmpty() || count <= 0) {
            return false;
        }
        ItemStack current = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (current.isEmpty()) {
            return count <= candidate.getMaxStackSize();
        }
        return ItemStack.isSameItemSameComponents(current, candidate)
                && count <= current.getMaxStackSize() - current.getCount();
    }

    /** Must only be called after {@link #canAcceptItemOutput(ItemStack, int)}. */
    private void insertItemOutput(ItemStack candidate, int count) {
        ItemStack current = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (current.isEmpty()) {
            ItemStack placed = candidate.copy();
            placed.setCount(count);
            itemHandler.setStackInSlot(OUTPUT_SLOT, placed);
        } else {
            current.grow(count);
        }
    }

    private boolean canDrainFluidExactly(FluidTank tank, FluidStack requested) {
        if (requested.isEmpty()) {
            return false;
        }
        FluidStack simulated = tank.drain(requested, IFluidHandler.FluidAction.SIMULATE);
        return sameFluidAmount(simulated, requested);
    }

    private boolean drainFluidExactly(FluidTank tank, FluidStack requested) {
        FluidStack before = tank.getFluid().copy();
        FluidStack drained = tank.drain(requested, IFluidHandler.FluidAction.EXECUTE);
        if (sameFluidAmount(drained, requested)) {
            return true;
        }
        tank.setFluid(before);
        return false;
    }

    private boolean canFillFluidExactly(FluidTank tank, FluidStack requested) {
        return !requested.isEmpty()
                && tank.fill(requested, IFluidHandler.FluidAction.SIMULATE) >= requested.getAmount();
    }

    private boolean fillFluidExactly(FluidTank tank, FluidStack requested) {
        if (requested.isEmpty()
                || tank.fill(requested, IFluidHandler.FluidAction.SIMULATE) < requested.getAmount()) {
            return false;
        }
        FluidStack before = tank.getFluid().copy();
        int filled = tank.fill(requested, IFluidHandler.FluidAction.EXECUTE);
        if (filled == requested.getAmount()) {
            return true;
        }
        tank.setFluid(before);
        return false;
    }

    private static boolean sameFluidAmount(FluidStack actual, FluidStack expected) {
        return !actual.isEmpty()
                && actual.getFluid() == expected.getFluid()
                && actual.getAmount() == expected.getAmount();
    }

    /**
     * 获取输入流体存储
     * @return 输入流体
 */
    public FluidTank getInputFluidTank() {
        return inputFluidTank;
    }

    /**
     * 获取输出流体存储
     * @return 输出流体
 */
    public FluidTank getOutputFluidTank() {
        return outputFluidTank;
    }

    /**
     * 获取输入流体数量
     * @return 流体数量（mb
 */
    public int getInputFluidAmount() {
        return inputFluidTank.getFluidAmount();
    }

    /**
     * 获取输出流体数量
     * @return 流体数量（mb
 */
    public int getOutputFluidAmount() {
        return outputFluidTank.getFluidAmount();
    }

    /**
     * 获取输入流体容量
     * @return 流体容量（mb
 */
    public int getInputFluidCapacity() {
        return inputFluidTank.getCapacity();
    }

    /**
     * 获取输出流体容量
     * @return 流体容量（mb
 */
    public int getOutputFluidCapacity() {
        return outputFluidTank.getCapacity();
    }

    /**
     * 获取输入流体
 * @return 流体
 */
    public FluidStack getInputFluid() {
        return inputFluidTank.getFluid();
    }

    /**
     * 获取输出流体
 * @return 流体
 */
    public FluidStack getOutputFluid() {
        return outputFluidTank.getFluid();
    }

    /**
     * 获取输入流体类型名称（用于GUI显示
 */
    public String getInputFluidTypeName() {
        FluidStack fluid = inputFluidTank.getFluid();
        if (fluid.isEmpty()) return "empty";
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid.getFluid()).getPath();
    }

    /**
     * 获取输出流体类型名称（用于GUI显示
 */
    public String getOutputFluidTypeName() {
        FluidStack fluid = outputFluidTank.getFluid();
        if (fluid.isEmpty()) return "empty";
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid.getFluid()).getPath();
    }

    /**
     * 获取输入流体注册表ID（用于客户端同步
 */
    public int getInputFluidTypeId() {
        FluidStack fluid = inputFluidTank.getFluid();
        if (fluid.isEmpty()) return -1;
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(fluid.getFluid());
    }

    /**
     * 获取输出流体注册表ID（用于客户端同步
 */
    public int getOutputFluidTypeId() {
        FluidStack fluid = outputFluidTank.getFluid();
        if (fluid.isEmpty()) return -1;
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(fluid.getFluid());
    }

    /**
     * 
tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_canner_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 先调用父类的 tick 逻辑（消耗能量进行工作，包含电池槽放电）
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, blockEntity);

        if (blockEntity.isRemoved()) return;
        // Reconcile once after processing. Also repairs old saves and setFluid/load/swap/clear,
        // which do not call FluidTank.onContentsChanged. Never overwrite a fresh state with tick input.
        BlockState current = blockEntity.getBlockState();
        boolean hasFluid = !blockEntity.inputFluidTank.isEmpty() || !blockEntity.outputFluidTank.isEmpty();
        if (current.getValue(mio_icif_block_canner_elc.LIT) != blockEntity.isWorking()
                || current.getValue(mio_icif_block_canner_elc.HAS_FLUID) != hasFluid) {
            level.setBlock(pos, current.setValue(mio_icif_block_canner_elc.LIT, blockEntity.isWorking())
                .setValue(mio_icif_block_canner_elc.HAS_FLUID, hasFluid), Block.UPDATE_CLIENTS);
        }
    }

    // ==================== NBT 数据保存 ====================

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        
        // 保存输入流体槽数
    tag.put("InputFluidTank", inputFluidTank.writeToNBT(registries, new CompoundTag()));
        
        // 保存输出流体槽数
    tag.put("OutputFluidTank", outputFluidTank.writeToNBT(registries, new CompoundTag()));
        
        // 保存工作模式
        tag.putInt("Mode", currentMode.getId());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        // 加载输入流体槽数
    if (tag.contains("InputFluidTank")) {
            inputFluidTank.readFromNBT(registries, tag.getCompound("InputFluidTank"));
        }
        
        // 加载输出流体槽数
    if (tag.contains("OutputFluidTank")) {
            outputFluidTank.readFromNBT(registries, tag.getCompound("OutputFluidTank"));
        }
        
        // 加载工作模式
        if (tag.contains("Mode")) {
            this.currentMode = Mode.fromId(tag.getInt("Mode"));
        }
    }

    // ==================== 模式相关方法 ====================

    /**
     * 获取当前工作模式
     * @return 当前模式
     */
    public Mode getMode() {
        return currentMode;
    }

    /**
     * 获取当前模式ID
     * @return 模式ID (0-3)
     */
    public int getModeId() {
        return currentMode.getId();
    }

    /**
     * 设置工作模式
     * @param mode 新模
 */
    public void setMode(Mode mode) {
        if (this.currentMode != mode) {
            this.currentMode = mode;
            setChanged();
        }
    }

    /**
     * 设置工作模式（通过ID
 * @param modeId 模式ID
     */
    public void setMode(int modeId) {
        setMode(Mode.fromId(modeId));
    }

    /**
     * 切换到下一个模
 */
    public void nextMode() {
        setMode(currentMode.next());
    }

    /**
     * 交换输入槽和输出槽的液体
     */
    public void swapFluids() {
        if (level == null || level.isClientSide()) {
            return;
        }
        // 对齐 IC2 switchTanks：工作进行中禁止交换，避免进行中的操作数据错位
        if (isWorking || progress != 0) {
            return;
        }

        // 获取当前液体
        FluidStack inputFluid = inputFluidTank.getFluid().copy();
        FluidStack outputFluid = outputFluidTank.getFluid().copy();

        // 清空两个
    inputFluidTank.setFluid(FluidStack.EMPTY);
        outputFluidTank.setFluid(FluidStack.EMPTY);

        // 交换液体
        if (!outputFluid.isEmpty()) {
            inputFluidTank.setFluid(outputFluid);
        }
        if (!inputFluid.isEmpty()) {
            outputFluidTank.setFluid(inputFluid);
        }

        setChanged();
    }

    // ==================== 流体处理相关方法 ====================

    /**
     * 获取组合流体处理器（用于Jade等显示双槽）
     * @return 包含输入槽和输出槽的组合流体处理
 */
    public net.neoforged.neoforge.fluids.capability.IFluidHandler getCombinedFluidHandler() {
        return combinedFluidHandler;
    }

    @Override
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return getCombinedFluidHandler();
    }

}
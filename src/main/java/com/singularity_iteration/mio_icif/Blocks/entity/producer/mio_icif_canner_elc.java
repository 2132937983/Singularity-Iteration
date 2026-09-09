package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.recipe.canner.canning.CanningRecipe;
import com.singularity_iteration.mio_icif.recipe.canner.canning.CanningRecipeInput;
import com.singularity_iteration.mio_icif.recipe.canner.canning.DynamicCanningRecipe;
import com.singularity_iteration.mio_icif.recipe.canner.empty_to_tank.EmptyToTankRecipe;
import com.singularity_iteration.mio_icif.recipe.canner.empty_to_tank.EmptyToTankRecipeInput;
import com.singularity_iteration.mio_icif.recipe.canner.fill_from_tank.FillFromTankRecipe;
import com.singularity_iteration.mio_icif.recipe.canner.fill_from_tank.FillFromTankRecipeInput;
import com.singularity_iteration.mio_icif.Items.Build.CFSprayerItem;
import com.singularity_iteration.mio_icif.recipe.canner.mix.MixRecipe;
import com.singularity_iteration.mio_icif.recipe.mio_icif_ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
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
public class mio_icif_canner_elc extends mio_icif_producer {

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

    // 当前工作模式
    private Mode currentMode = Mode.CANNING;

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
        this.inputFluidTank = new FluidTank(FLUID_CAPACITY, fluidStack -> true);
        
        // 初始化输出流体存储，接受任何流体
        this.outputFluidTank = new FluidTank(FLUID_CAPACITY, fluidStack -> true);
    }

    public mio_icif_canner_elc(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                long capacity, long maxReceive, long maxExtract,
                                int workTime, long energyPerTick) {
        super(pos, state, type, capacity, maxReceive, maxExtract, workTime, LAYOUT, energyPerTick, CableTier.LV);

        // 初始化输入流体存
    this.inputFluidTank = new FluidTank(FLUID_CAPACITY, fluidStack -> true);
        
        // 初始化输出流体存
    this.outputFluidTank = new FluidTank(FLUID_CAPACITY, fluidStack -> true);
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
        if (stack.isEmpty()) {
            return false;
        }

        return switch (currentMode) {
            case CANNING -> {
                yield stack.is(com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal.TIN_EMPTY_CAN.get())
                    || stack.is(com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal.FUEL_ROD.get());
            }
            case EMPTY_TO_TANK -> {
                yield (mio_icif_cells.isFluidCell(stack) && !mio_icif_cells.isEmptyCell(stack))
                    || isFilledBucket(stack);
            }
            case FILL_FROM_TANK -> {
                yield mio_icif_cells.isEmptyCell(stack)
                    || stack.is(net.minecraft.world.item.Items.BUCKET);
            }
            case MIX -> {
                yield true;
            }
        };
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
        if (stack.isEmpty()) {
            return false;
        }

        return switch (currentMode) {
            case CANNING -> {
                yield stack.getItem().getFoodProperties(stack, null) != null
                    || stack.is(com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources.URAN_238.get())
                    || stack.is(com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources.MOX.get());
            }
            case EMPTY_TO_TANK -> {
                yield false;
            }
            case FILL_FROM_TANK -> {
                yield false;
            }
            case MIX -> {
                yield stack.getItem() instanceof CFSprayerItem
                    || hasMatchingMixRecipe(stack);
            }
        };
    }

    private boolean hasMatchingMixRecipe(ItemStack material) {
        if (level == null) {
            return true;
        }
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            if (holder.value() instanceof MixRecipe mixRecipe) {
                if (!ingredientMatches(mixRecipe.getMaterialIngredient(), material)) {
                    continue;
                }
                if (inputFluidTank.isEmpty()) {
                    return true;
                }
                FluidStack tankFluid = inputFluidTank.getFluid();
                if (tankFluid.getFluid() == mixRecipe.getInputFluid().getFluid()
                        && tankFluid.getAmount() >= mixRecipe.getInputFluid().getAmount()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean ingredientMatches(Ingredient ingredient, ItemStack stack) {
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
            case UP -> new int[]{INPUT_SLOT}; // 上方：输入槽
            case DOWN -> new int[]{OUTPUT_SLOT}; // 下方：输出槽
            case NORTH, SOUTH, EAST, WEST -> new int[]{INPUT_SLOT, OUTPUT_SLOT, MATERIAL_SLOT, BATTERY_SLOT};
        };
    }

    @Override
    protected int[] getInputSlots() {
        return new int[]{INPUT_SLOT, MATERIAL_SLOT};
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[]{OUTPUT_SLOT};
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
        return findRecipe().isPresent();
    }

    /**
     * 检查机器是否可以工
 * @return 是否可以工作
     */
    @Override
    protected boolean canWork() {
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

        // 检查是否有匹配的配
    Optional<?> recipeOptional = findRecipe();
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
        boolean isEmptyCell = input.getItem() instanceof com.singularity_iteration.mio_icif.api.item.IFluidCellItem cellItem
            && cellItem.getFluid(input).isEmpty();
        boolean isPartiallyFilledCell = input.getItem() instanceof com.singularity_iteration.mio_icif.api.item.IFluidCellItem cellItem
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
        if (input.getItem() instanceof com.singularity_iteration.mio_icif.api.item.IFluidCellItem cellItem) {
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
                if (input.getItem() instanceof com.singularity_iteration.mio_icif.api.item.IFluidCellItem cellItem) {
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
        if (emptyContainer.getItem() instanceof com.singularity_iteration.mio_icif.api.item.IFluidCellItem cellItem) {
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
            if (inputFluid.getFluid() != com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids.CONSTRUCTIONFOAM.get()) {
                return false; // 不是建筑泡沫流体
            }
            // 检查喷枪是否已
        if (sprayer.isFull(material)) {
                return false; // 喷枪已满，无需补充
            }
            // 检查流体是否足够（每次补充消
            if (inputFluid.getAmount() < FOAM_REFILL_FLUID_AMOUNT) {
                return false;
            }
            return true;
        }

        // ===== 普通混合配方处
        // 查找匹配的配
    Optional<RecipeHolder<MixRecipe>> recipeHolder = findMixRecipe();
        if (recipeHolder.isEmpty()) {
            return false;
        }

        MixRecipe recipe = recipeHolder.get().value();

        // 检查材料数
    if (material.getCount() < recipe.getMaterialCount()) {
            return false;
        }

        // 检查输入流体是否足
    FluidStack inputFluid = inputFluidTank.getFluid();
        if (inputFluid.getAmount() < recipe.getInputFluid().getAmount()) {
            return false;
        }

        boolean hasEmptyCell = mio_icif_cells.isEmptyCell(inputSlotItem);

        if (hasEmptyCell) {
            FluidStack resultFluid = recipe.getResultFluid();
            if (resultFluid.isEmpty()) {
                return false;
            }

            ItemStack filledCell = recipe.getDynamicResultCell();
            ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
            if (!currentOutput.isEmpty()) {
                if (!ItemStack.isSameItemSameComponents(currentOutput, filledCell)) {
                    return false;
                }
                if (currentOutput.getCount() + 1 > currentOutput.getMaxStackSize()) {
                    return false;
                }
            }

            if (inputSlotItem.getCount() < 1) {
                return false;
            }
        } else {
            // 输出到输出液体槽
            FluidStack resultFluid = recipe.getResultFluid();

            // 检查输出液体槽是否有足够空
        int filled = outputFluidTank.fill(resultFluid, IFluidHandler.FluidAction.SIMULATE);
            if (filled < resultFluid.getAmount()) {
                return false;
            }
        }

        return true;
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
                CanningRecipeInput recipeInput = new CanningRecipeInput(inputCan, material);
                return dynamicRecipe.assemble(recipeInput, level.registryAccess());
            } else if (recipeValue instanceof CanningRecipe canningRecipe) {
                return canningRecipe.assemble(null, level.registryAccess());
            } else if (recipeValue instanceof EmptyToTankRecipe emptyToTankRecipe) {
                return emptyToTankRecipe.getEmptyCellResult();
            } else if (recipeValue instanceof FillFromTankRecipe fillFromTankRecipe) {
                return fillFromTankRecipe.getFilledCellResult();
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

        if (input.getItem() instanceof com.singularity_iteration.mio_icif.api.item.IFluidCellItem cellItem) {
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

        if (input.getItem() instanceof com.singularity_iteration.mio_icif.api.item.IFluidCellItem cellItem) {
            FluidStack content = cellItem.getFluid(input);
            if (!content.isEmpty()) {
                return content;
            }
        }

        return FluidStack.EMPTY;
    }

    /**
     * 获取配方输出数量
     */
    private int getOutputCount(Object recipe) {
        if (recipe instanceof RecipeHolder<?> holder) {
            Object recipeValue = holder.value();

            if (recipeValue instanceof DynamicCanningRecipe dynamicRecipe) {
                ItemStack inputCan = itemHandler.getStackInSlot(INPUT_SLOT);
                ItemStack material = itemHandler.getStackInSlot(MATERIAL_SLOT);
                CanningRecipeInput recipeInput = new CanningRecipeInput(inputCan, material);
                return dynamicRecipe.getOutputCount(recipeInput);
            }
        }
        return 1; // 默认输出1
}

    /**
     * 根据当前模式查找匹配的配
 * @return 匹配的配方（如果有）
     */
    private Optional<?> findRecipe() {
        if (level == null) return Optional.empty();

        return switch (currentMode) {
            case CANNING -> findCanningRecipe();
            case EMPTY_TO_TANK -> findEmptyToTankRecipe();
            case FILL_FROM_TANK -> findFillFromTankRecipe();
            case MIX -> findMixRecipe();
        };
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

        CanningRecipeInput recipeInput = new CanningRecipeInput(inputCan, material);

        // 首先尝试查找动态配
    Optional<RecipeHolder<DynamicCanningRecipe>> dynamicRecipe = level.getRecipeManager()
            .getRecipeFor(mio_icif_ModRecipes.DYNAMIC_CANNING_TYPE.get(), recipeInput, level);

        if (dynamicRecipe.isPresent()) {
            return dynamicRecipe;
        }

        // 如果没有动态配方，查找普通配
    return level.getRecipeManager()
            .getRecipeFor(mio_icif_ModRecipes.CANNING_TYPE.get(), recipeInput, level);
    }

    /**
     * 查找单元灌入水槽配方
     */
    private Optional<RecipeHolder<EmptyToTankRecipe>> findEmptyToTankRecipe() {
        ItemStack filledCell = itemHandler.getStackInSlot(INPUT_SLOT);

        if (filledCell.isEmpty()) {
            return Optional.empty();
        }

        EmptyToTankRecipeInput recipeInput = new EmptyToTankRecipeInput(filledCell);
        return level.getRecipeManager()
            .getRecipeFor(mio_icif_ModRecipes.EMPTY_TO_TANK_TYPE.get(), recipeInput, level);
    }

    /**
     * 查找水槽灌满单元配方
     */
    private Optional<RecipeHolder<FillFromTankRecipe>> findFillFromTankRecipe() {
        ItemStack emptyCell = itemHandler.getStackInSlot(INPUT_SLOT);

        if (emptyCell.isEmpty()) {
            return Optional.empty();
        }

        FillFromTankRecipeInput recipeInput = new FillFromTankRecipeInput(emptyCell);
        return level.getRecipeManager()
            .getRecipeFor(mio_icif_ModRecipes.FILL_FROM_TANK_TYPE.get(), recipeInput, level);
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

        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
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
                    @SuppressWarnings("unchecked")
                    Optional<RecipeHolder<MixRecipe>> result = Optional.of((RecipeHolder<MixRecipe>) holder);
                    return result;
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
        // 混合模式不需要输入槽有物
    if (currentMode != Mode.MIX) {
            ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
            if (input.isEmpty()) {
                stopWork();
                return;
            }
        }

        // 根据当前模式执行不同的配方处
    boolean success = switch (currentMode) {
            case CANNING -> processCanningRecipe();
            case EMPTY_TO_TANK -> processEmptyToTankRecipe();
            case FILL_FROM_TANK -> processFillFromTankRecipe();
            case MIX -> processMixRecipe();
        };

        if (!success) {
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
            CanningRecipeInput recipeInput = new CanningRecipeInput(inputCan, material);
                int requiredCans = dynamicRecipe.getRequiredCanCount(recipeInput);
                int outputCount = dynamicRecipe.getOutputCount(recipeInput);

                // 检查是否有足够的锡
            if (inputCan.getCount() < requiredCans) {
                    return false;
                }

                // 消耗输入物
            inputCan.shrink(requiredCans);
                material.shrink(1);

                // 输出结果（支持堆叠）
                ItemStack result = dynamicRecipe.assemble(recipeInput, level.registryAccess());
                result.setCount(Math.min(outputCount, 64)); // 最

                ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
                if (currentOutput.isEmpty()) {
                    // 输出槽为空，直接设置
                    itemHandler.setStackInSlot(OUTPUT_SLOT, result);
                } else {
                    // 输出槽已有物品，增加数量
                    currentOutput.grow(result.getCount());
                }

                return true;
            } else if (recipeValue instanceof CanningRecipe canningRecipe) {
                // 处理普通配
            // 消耗输入物
            inputCan.shrink(1);
                material.shrink(1);

                // 输出结果（支持堆叠）
                ItemStack result = canningRecipe.assemble(null, level.registryAccess());

                ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
                if (currentOutput.isEmpty()) {
                    // 输出槽为空，直接设置
                    itemHandler.setStackInSlot(OUTPUT_SLOT, result);
                } else if (ItemStack.isSameItemSameComponents(currentOutput, result)) {
                    // 输出槽已有相同物品，增加数量
                    currentOutput.grow(result.getCount());
                } else {
                    // 物品不同，无法堆叠（这种情况不应该发生，因为canWork已经检查过
                return false;
                }

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

        // 消耗输入物
    input.shrink(1);

        // 添加流体到输出流体槽
        outputFluidTank.fill(fluid, IFluidHandler.FluidAction.EXECUTE);

        // 输出空容器（支持堆叠
    ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (currentOutput.isEmpty()) {
            // 输出槽为空，直接设置
            itemHandler.setStackInSlot(OUTPUT_SLOT, emptyContainer.copy());
        } else if (ItemStack.isSameItemSameComponents(currentOutput, emptyContainer)) {
            // 输出槽已有相同物品，增加数量
            currentOutput.grow(1);
        } else {
            // 物品不同，无法堆叠（这种情况不应该发生，因为canWork已经检查过
        return false;
        }

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
        if (input.getItem() instanceof com.singularity_iteration.mio_icif.api.item.IFluidCellItem cellItem) {
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
            cellItem.fill(outputItem, new FluidStack(tankFluid.getFluid(), fillAmount), false);

            inputFluidTank.drain(fillAmount, IFluidHandler.FluidAction.EXECUTE);

            ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
            if (currentOutput.isEmpty()) {
                itemHandler.setStackInSlot(OUTPUT_SLOT, outputItem);
                input.shrink(1);
            } else if (ItemStack.isSameItemSameComponents(currentOutput, outputItem)) {
                currentOutput.grow(1);
                input.shrink(1);
            } else {
                return false;
            }

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

            input.shrink(1);
            inputFluidTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);

            ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
            if (currentOutput.isEmpty()) {
                itemHandler.setStackInSlot(OUTPUT_SLOT, filledContainer.copy());
            } else if (ItemStack.isSameItemSameComponents(currentOutput, filledContainer)) {
                currentOutput.grow(1);
            } else {
                return false;
            }

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
            if (inputFluid.getFluid() != com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids.CONSTRUCTIONFOAM.get()) {
                return false;
            }
            if (inputFluid.getAmount() < FOAM_REFILL_FLUID_AMOUNT) {
                return false;
            }
            if (sprayer.isFull(material)) {
                return false;
            }

            // 消耗建筑泡沫流
        inputFluidTank.drain(FOAM_REFILL_FLUID_AMOUNT, IFluidHandler.FluidAction.EXECUTE);

            // 补充CF喷枪泡沫
            sprayer.addFoam(material, FOAM_REFILL_AMOUNT);

            return true;
        }

        // ===== 普通混合配方处
        Optional<RecipeHolder<MixRecipe>> recipeHolder = findMixRecipe();
        if (recipeHolder.isEmpty()) {
            return false;
        }

        MixRecipe recipe = recipeHolder.get().value();
        ItemStack inputSlotItem = itemHandler.getStackInSlot(INPUT_SLOT);

        // 消耗输入液体槽的流
    FluidStack requiredFluid = recipe.getInputFluid();
        inputFluidTank.drain(requiredFluid, IFluidHandler.FluidAction.EXECUTE);

        // 消耗材
    material.shrink(recipe.getMaterialCount());

        // 检查是否有空单元在输入槽
        boolean hasEmptyCell = inputSlotItem.getItem() instanceof com.singularity_iteration.mio_icif.api.item.IFluidCellItem cellItem
            && cellItem.getFluid(inputSlotItem).isEmpty();

        if (hasEmptyCell) {
            inputSlotItem.shrink(1);

            ItemStack filledCell = recipe.getDynamicResultCell();
            ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
            if (currentOutput.isEmpty()) {
                itemHandler.setStackInSlot(OUTPUT_SLOT, filledCell);
            } else if (ItemStack.isSameItemSameComponents(currentOutput, filledCell)) {
                currentOutput.grow(1);
            } else {
                return false;
            }
        } else {
            // 输出到输出液体槽
            FluidStack resultFluid = recipe.getResultFluid();
            outputFluidTank.fill(resultFluid, IFluidHandler.FluidAction.EXECUTE);
        }

        return true;
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
        mio_icif_producer.tick(level, pos, state, blockEntity);

        // 更新方块状态（运行/停止
    boolean isLit = state.getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_canner_elc.LIT);
        if (blockEntity.isWorking() != isLit) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_canner_elc.LIT, blockEntity.isWorking()), 3);
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

    /**
     * 清空输入流体
     */
    public void clearInputTank() {
        if (level == null || level.isClientSide()) {
            return;
        }
        inputFluidTank.setFluid(FluidStack.EMPTY);
        setChanged();
    }

    /**
     * 清空输出流体
     */
    public void clearOutputTank() {
        if (level == null || level.isClientSide()) {
            return;
        }
        outputFluidTank.setFluid(FluidStack.EMPTY);
        setChanged();
    }

    // ==================== 流体处理相关方法 ====================

    /**
     * 获取组合流体处理器（用于Jade等显示双槽）
     * @return 包含输入槽和输出槽的组合流体处理
 */
    public net.neoforged.neoforge.fluids.capability.IFluidHandler getCombinedFluidHandler() {
        return new CombinedFluidHandler(inputFluidTank, outputFluidTank);
    }

    @Override
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return getCombinedFluidHandler();
    }

    /**
     * 组合流体处理
- 将输入槽和输出槽合并为一个有两个槽的处理
 */
    private static class CombinedFluidHandler implements net.neoforged.neoforge.fluids.capability.IFluidHandler {
        private final FluidTank inputTank;
        private final FluidTank outputTank;

        public CombinedFluidHandler(FluidTank inputTank, FluidTank outputTank) {
            this.inputTank = inputTank;
            this.outputTank = outputTank;
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank) {
            if (tank == 0) {
                return inputTank.getFluid();
            } else if (tank == 1) {
                return outputTank.getFluid();
            }
            return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            if (tank == 0) {
                return inputTank.getCapacity();
            } else if (tank == 1) {
                return outputTank.getCapacity();
            }
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, net.neoforged.neoforge.fluids.FluidStack stack) {
            if (tank == 0) {
                return inputTank.isFluidValid(stack);
            }
            return false; // 输出槽不接受输入
        }

        @Override
        public int fill(net.neoforged.neoforge.fluids.FluidStack resource, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action) {
            // 只允许填充到输入
        return inputTank.fill(resource, action);
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action) {
            // 优先从输出槽抽取
            net.neoforged.neoforge.fluids.FluidStack result = outputTank.drain(resource, action);
            if (result.isEmpty()) {
                result = inputTank.drain(resource, action);
            }
            return result;
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action) {
            // 优先从输出槽抽取
            net.neoforged.neoforge.fluids.FluidStack result = outputTank.drain(maxDrain, action);
            if (result.isEmpty()) {
                result = inputTank.drain(maxDrain, action);
            }
            return result;
        }
    }
}
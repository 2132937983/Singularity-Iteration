package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.common.recipe.washer.mio_icif_WasherRecipe;
import com.miophas.singularity_iteration.common.recipe.washer.mio_icif_WasherRecipeInput;
import com.miophas.singularity_iteration.common.recipe.washer.mio_icif_WasherRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 洗矿机方块实体类
 * 使用清洁剂和水洗矿物品，产出多种矿物
 * 配方逻辑：电池 + 清洁剂 + 输入 + 水（200mb每次操作）
 * 槽位布局说明：输入 + 3输出 + 1电池 + 1水桶输入 + 1空桶输出 + 4升级 = 11槽位
 */
@SuppressWarnings("null")
public class mio_icif_washer_elc extends AbstractProcessingMachineBlockEntity {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .output(3)
        .battery()
        .extra(2)
        .upgrade(4)
        .build();

    // 槽位总数：11个槽位
    public static final int SLOT_COUNT = 11;
    // 输入槽索引：放置待洗矿物品
    public static final int INPUT_SLOT = 0;
    // 输出槽起始索引：3个输出槽位
    public static final int OUTPUT_SLOT_1 = 1;
    public static final int OUTPUT_SLOT_2 = 2;
    public static final int OUTPUT_SLOT_3 = 3;
    // 电池槽索引
    public static final int BATTERY_SLOT = 4;
    // 水桶输入槽索引
    public static final int WATER_BUCKET_SLOT = 5;
    // 空桶输出槽索引
    public static final int EMPTY_BUCKET_SLOT = 6;
    // 升级件槽起始索引：4个升级件槽位
    public static final int UPGRADE_SLOT_START = 7;

    // 默认配置（对比IC2原版）
    public static final long DEFAULT_CAPACITY = 8000L;   // 16 EU/t x 500 ticks = 8000 EU
    public static final long DEFAULT_MAX_RECEIVE = 128L; // MV级输入
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 400;
    public static final long DEFAULT_ENERGY_PER_TICK = 16L; // 每tick消耗16 EU）对比IC2原版

    // 流体配置（mb = 毫桶）
    public static final int FLUID_CAPACITY = 8000; // 8000 mb = 8桶水
    public static final int WATER_PER_OPERATION = 200; // 每次洗矿消耗200mb水

    // 流体存储
    protected final FluidTank fluidTank;

    /**
     * 构造 BlockEntityType.Builder 注册用参数构造函数
     */
    public mio_icif_washer_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.WASHER_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_washer_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.MV);

        // 创建流体槽只接受水
        this.fluidTank = new FluidTank(FLUID_CAPACITY, fluidStack ->
            fluidStack.getFluid() == Fluids.WATER) {
                @Override protected void onContentsChanged() { com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank.markUnsaved(mio_icif_washer_elc.this); }
            };
    }

    public mio_icif_washer_elc(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                long capacity, long maxReceive, long maxExtract,
                                int workTime, long energyPerTick) {
        super(pos, state, type, capacity, maxReceive, maxExtract, workTime, LAYOUT, energyPerTick, CableTier.MV);

        // 创建流体槽只接受水
        this.fluidTank = new FluidTank(FLUID_CAPACITY, fluidStack ->
            fluidStack.getFluid() == Fluids.WATER) {
                @Override protected void onContentsChanged() { com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank.markUnsaved(mio_icif_washer_elc.this); }
            };
    }



    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.washer_elc");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.producer.WasherElcMenu(containerId, playerInventory, this);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) {
            // 输入槽：只接受可以洗矿的物品
            return isWashable(stack);
        } else if (slot >= OUTPUT_SLOT_1 && slot <= OUTPUT_SLOT_3) {
            // 输出槽位不允许自动化插入
            return false;
        } else if (slot == BATTERY_SLOT) {
            // 电池槽：接受电池类物品
            return isBattery(stack);
        } else if (slot == WATER_BUCKET_SLOT) {
            // 水桶输入槽：只接受水
            return isWaterBucket(stack);
        } else if (slot == EMPTY_BUCKET_SLOT) {
            // 空桶输出槽位不允许自动化插入
            return false;
        } else if (slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + 4) {
            return getItemAPI().isUpgrade(stack);
        }
        return false;
    }

    /**
     * 检查物品是否是水桶或水单元
     */
    private boolean isWaterBucket(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET) || mio_icif_cells.isCellContainingFluid(stack, Fluids.WATER);
    }

    /**
     * 检查物品是否是空桶或空单元
     */
    @SuppressWarnings("unused")
    private boolean isEmptyBucket(ItemStack stack) {
        return stack.is(Items.BUCKET) || mio_icif_cells.isEmptyCell(stack);
    }

    /**
     * 检查物品是否可以洗矿（是否有匹配配方）
     * 通过尝试获取配方来判断
     */
    private boolean isWashable(ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return false;
        }
        // 构建配方输入
        mio_icif_WasherRecipeInput recipeInput = new mio_icif_WasherRecipeInput(stack);
        // 尝试匹配配方
        Optional<RecipeHolder<mio_icif_WasherRecipe>> recipe = level.getRecipeManager()
            .getRecipeFor(mio_icif_WasherRecipes.WASHER_TYPE.get(), recipeInput, level);
        return recipe.isPresent();
    }

    /**
     * 获取匹配的洗矿配方
     */
    private Optional<mio_icif_WasherRecipe> getRecipe(ItemStack input) {
        if (level == null || input.isEmpty()) {
            return Optional.empty();
        }
        mio_icif_WasherRecipeInput recipeInput = new mio_icif_WasherRecipeInput(input);
        return level.getRecipeManager()
            .getRecipeFor(mio_icif_WasherRecipes.WASHER_TYPE.get(), recipeInput, level)
            .map(RecipeHolder::value);
    }

    /**
     * 获取方块朝向
     */
    @SuppressWarnings("unused")
    private Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(com.miophas.singularity_iteration.common.block.mio_icif_entity_block.FACING)) {
            return state.getValue(com.miophas.singularity_iteration.common.block.mio_icif_entity_block.FACING);
        }
        return Direction.NORTH;
    }

    /**
     * 默认返回可访问的槽位方法
     * 任何方向都可以访问所有公共槽位
     */
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // 任何方向都可以访问所有功能槽位
        return new int[]{INPUT_SLOT, BATTERY_SLOT, OUTPUT_SLOT_1, OUTPUT_SLOT_2, OUTPUT_SLOT_3, WATER_BUCKET_SLOT, EMPTY_BUCKET_SLOT};
    }

    @Override
    protected int[] getInputSlots() {
        return new int[]{INPUT_SLOT};
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[]{OUTPUT_SLOT_1, OUTPUT_SLOT_2, OUTPUT_SLOT_3};
    }

    /**
     * 默认返回是否可以插入槽位方法
     * 任何方向都可以访问3个输出槽位和空桶槽
     */
    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        // 输出槽位不允许插入
        if (slot >= OUTPUT_SLOT_1 && slot <= OUTPUT_SLOT_3) {
            return false;
        }

        // 空桶输出槽位不允许插入
        if (slot == EMPTY_BUCKET_SLOT) {
            return false;
        }

        // 电池槽位（槽位4）只允许电池类物品
        if (slot == BATTERY_SLOT) {
            return isBattery(stack);
        }

        // 水桶输入槽位（槽位5）只接受水
        if (slot == WATER_BUCKET_SLOT) {
            return isWaterBucket(stack);
        }

        // 输入槽位（槽位0）接受可洗矿物品
        if (slot == INPUT_SLOT) {
            return isWashable(stack);
        }

        // 其他槽位为升级件槽位，只接受升级件不允许放入其他
        return false;
    }

    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    /**
     * 默认返回是否可以提取槽位方法
     * 任何方向都可以提取3个输出槽位和空桶槽
     */
    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        // 3个输出槽位和空桶槽可以被任何方向提取
        return (slot >= OUTPUT_SLOT_1 && slot <= OUTPUT_SLOT_3) || slot == EMPTY_BUCKET_SLOT;
    }

    /**
     * 检查是否有足够水来工作
     */
    protected boolean hasEnoughWater() {
        if (fluidTank.isEmpty() || fluidTank.getFluid().getFluid() != Fluids.WATER) {
            return false;
        }
        return fluidTank.getFluidAmount() >= WATER_PER_OPERATION;
    }

    /**
     * 消耗水
     */
    protected boolean consumeWater() {
        if (!hasEnoughWater()) {
            return false;
        }
        FluidStack drained = fluidTank.drain(WATER_PER_OPERATION, IFluidHandler.FluidAction.EXECUTE);
        return drained.getAmount() >= WATER_PER_OPERATION;
    }

    /**
     * 处理水桶输入槽
     * 将水桶/水单元中的水转移到流体槽，空容器移到空桶输出槽
     */
    private void handleWaterBucketSlot() {
        var input = itemHandler.getStackInSlot(WATER_BUCKET_SLOT);
        if (input.isEmpty() || !isWaterBucket(input)) return;
        boolean cell = mio_icif_cells.isFluidCell(input);
        var content = cell ? mio_icif_cells.getCellFluid(input.copyWithCount(1)) : new FluidStack(Fluids.WATER, 1000);
        var empty = cell ? mio_icif_cells.getEmptyCellForStack(input.copyWithCount(1)) : new ItemStack(Items.BUCKET);
        if (com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank.transfer(itemHandler, WATER_BUCKET_SLOT, EMPTY_BUCKET_SLOT, fluidTank, content, empty)) setChanged();
    }

    /**
     * 判断是否应该重置进度
     * 洗矿机特殊情况：只有输入槽为空或物品不匹配时才重置，缺水时保持进度
     */
    @Override
    protected boolean shouldResetProgress() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        // 输入槽为空，重置进度
        if (input.isEmpty()) {
            return true;
        }
        // 物品不匹配则重置进度
        return !isWashable(input);
    }

    @Override
    protected boolean canWork() {
        var input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || !hasEnoughEnergy() || !hasEnoughWater()) return false;
        return com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots.prepare(itemHandler, INPUT_SLOT, 1,
            new int[]{OUTPUT_SLOT_1, OUTPUT_SLOT_2, OUTPUT_SLOT_3}, getWashingResults(input)).isPresent();
    }

    /**
     * 获取洗矿产出的所有结果
     * @param input 输入物品
     * @return 产出物品列表
     */
    private List<ItemStack> getWashingResults(ItemStack input) {
        Optional<mio_icif_WasherRecipe> recipe = getRecipe(input);
        if (recipe.isPresent()) {
            return recipe.get().getAllResults();
        }
        // 没有匹配配方，返回空列表
        return java.util.Collections.emptyList();
    }

    @Override
    protected void doWork() {
        // 检查能量
        if (!consumeEnergy()) {
            stopWork();
            return;
        }

        isWorking = true;

        // 进度完成执行洗矿
        if (progress >= maxProgress) {
            finishWashing();
        }
    }

    /**
     * 完成一次洗矿，产出多个结果
     */
    private void finishWashing() {
        var input = itemHandler.getStackInSlot(INPUT_SLOT);
        var plan = com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots.prepare(itemHandler, INPUT_SLOT, 1,
            new int[]{OUTPUT_SLOT_1, OUTPUT_SLOT_2, OUTPUT_SLOT_3}, getWashingResults(input));
        if (plan.isEmpty()) { stopWork(); return; }
        int completedProgress = progress;
        progress = 0; // An inventory observer must never save completed work against the next input.
        if (!plan.get().commitWithFluid(fluidTank, new FluidStack(Fluids.WATER, WATER_PER_OPERATION))) { progress = completedProgress; stopWork(); return; }
        finishWork();
        if (canWork()) isWorking = true;
    }

    /**
     * 尝试将结果放入指定输出槽
     * @param slot 输出槽索引
     * @param result 结果物品
     * @return 是否成功放入
     */
    private boolean tryPlaceResult(int slot, ItemStack result) {
        if (result.isEmpty()) {
            return true; // 空气不需要放入
        }
        
        ItemStack currentOutput = itemHandler.getStackInSlot(slot);
        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(slot, result.copy());
            return true;
        } else if (ItemStack.isSameItem(currentOutput, result) && 
                   ItemStack.isSameItemSameComponents(currentOutput, result)) {
            int newCount = currentOutput.getCount() + result.getCount();
            if (newCount <= currentOutput.getMaxStackSize()) {
                currentOutput.grow(result.getCount());
                return true;
            }
        }
        return false;
    }

    /**
     * 每tick执行工作核心逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_washer_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 调用父类tick方法处理工作逻辑和电池槽升级槽
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, blockEntity);

        // 处理水桶输入槽
        blockEntity.handleWaterBucketSlot();

        // 同步方块状态亮灭
        boolean isLit = state.getValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_washer_elc.LIT);
        if (blockEntity.isWorking() != isLit) {
            level.setBlock(pos, state.setValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_washer_elc.LIT, blockEntity.isWorking()), 3);
        }
    }

    /**
     * 获取流体处理器
     */
    public IFluidHandler getFluidHandler() {
        return fluidTank;
    }

    /**
     * 获取流体处理器能力用于特定方向
     */
    @Override
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return fluidTank;
    }

    /**
     * 获取存储流体总量单位mb
     */
    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    /**
     * 获取最大流体容量单位mb
     */
    public int getFluidCapacity() {
        return fluidTank.getCapacity();
    }

    /**
     * 获取流体类型
     */
    public FluidStack getFluid() {
        return fluidTank.getFluid();
    }

    /**
     * 获取流体填充百分比用于GUI显示
     */
    public int getFluidProgress() {
        if (fluidTank.getCapacity() <= 0) {
            return 0;
        }
        return (fluidTank.getFluidAmount() * 100) / fluidTank.getCapacity();
    }

    /**
     * 获取输入槽物品
     */
    public ItemStack getInputItem() {
        return itemHandler.getStackInSlot(INPUT_SLOT);
    }

    /**
     * 获取指定输出槽位物品
     */
    public ItemStack getOutputItem(int index) {
        if (index >= 0 && index < 3) {
            return itemHandler.getStackInSlot(OUTPUT_SLOT_1 + index);
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // 保存流体数据
        tag.put("fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 读取流体数据
        if (tag.contains("fluid")) {
            fluidTank.readFromNBT(registries, tag.getCompound("fluid"));
        }
    }
}

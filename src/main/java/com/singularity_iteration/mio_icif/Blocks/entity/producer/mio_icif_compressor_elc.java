package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 电力压缩机方块实体类
 * 使用电力进行压缩加工，将物品压缩成其他形
 * 拥有4个物品槽：输入槽、电池槽、输出槽、额外槽
 */
@SuppressWarnings("null")
public class mio_icif_compressor_elc extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .battery()
        .output(1)
        .extra(1)
        .upgrade(4)
        .build();

    // 槽位数量：输入槽 + 电池

public static final int SLOT_COUNT = 8;
    // 输入槽索
public static final int INPUT_SLOT = 0;
    // 电池槽索
public static final int BATTERY_SLOT = 1;
    // 输出槽索
public static final int OUTPUT_SLOT = 2;
    // 额外槽索
public static final int EXTRA_SLOT = 3;
    // 升级槽起始索引（4个升级槽
public static final int UPGRADE_SLOT_START = 4;

    // 默认配置（对
public static final long DEFAULT_CAPACITY = 600L;    // 2 EU/t × 300 ticks = 600 EU
    public static final long DEFAULT_MAX_RECEIVE = 32L;  // LV级最大输
public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 300; // 15秒（300 ticks
public static final long DEFAULT_ENERGY_PER_TICK = 2L; // 每tick消

    /**
     * 用于 BlockEntityType.Builder 的构造函
 * 自动获取已注册的方块实体类型
     */
    public mio_icif_compressor_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.COMPRESSOR_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_compressor_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.LV);
    }

    public mio_icif_compressor_elc(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                    long capacity, long maxReceive, long maxExtract,
                                    int workTime, long energyPerTick) {
        super(pos, state, type, capacity, maxReceive, maxExtract, workTime, LAYOUT, energyPerTick, CableTier.LV);
    }



    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT ->
                    isCompressible(stack);
            case BATTERY_SLOT ->
                    isBattery(stack);
            case OUTPUT_SLOT ->
                    false;
            default -> getItemAPI().isUpgrade(stack);
        };
    }

    /**
     * 获取指定方向可访问的槽位
     * 压缩机槽位结构：0=输入, 1=电池, 2=输出, 3=
 4-7=升级
 * @param side 方向
     * @return 可访问的槽位数组
     */
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return new int[]{INPUT_SLOT, BATTERY_SLOT, OUTPUT_SLOT};
    }

    @Override
    protected int[] getInputSlots() {
        return new int[]{INPUT_SLOT};
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    /**
     * 检查指定槽位是否可以从指定方向提取物品
     * @param slot 槽位
     * @param side 方向
     * @return 是否可以提取
     */
    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return slot == OUTPUT_SLOT;
    }

    /**
     * 检查物品是否可以压
 * @param stack 物品
 * @return 是否可以压缩
     */
    private boolean isCompressible(ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return false;
        }

        ItemStack checkStack = stack.copyWithCount(64);
        com.singularity_iteration.mio_icif.recipe.compressor.mio_icif_CompressorRecipeInput recipeInput =
                new com.singularity_iteration.mio_icif.recipe.compressor.mio_icif_CompressorRecipeInput(checkStack);

        var recipe = level.getRecipeManager()
            .getRecipeFor(com.singularity_iteration.mio_icif.recipe.mio_icif_ModRecipes.COMPRESSOR_TYPE.get(), recipeInput, level);

        return recipe.isPresent();
    }

    /**
     * 检查是否有有效的配
 * 用于判断进度是否应该重置
     */
    @Override
    protected boolean hasValidRecipe() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        return isCompressible(input);
    }

    @Override
    protected boolean canWork() {
        // 检查是否有输入物品
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }

        // 检查是否有足够能量
        if (!hasEnoughEnergy()) {
            return false;
        }

        // 获取当前配方
        var recipe = getCurrentRecipe();
        if (recipe == null) {
            return false;
        }

        // 检查输入物品数量是否足
    if (input.getCount() < recipe.getIngredientCount()) {
            return false;
        }

        // 检查输出槽是否可以容纳结果
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
        ItemStack result = recipe.getResult();

        if (result.isEmpty()) {
            return false;
        }

        if (currentOutput.isEmpty()) {
            return true;
        }

        if (!ItemStack.isSameItemSameComponents(currentOutput, result)) {
            return false;
        }

        int newCount = currentOutput.getCount() + result.getCount();
        return newCount <= currentOutput.getMaxStackSize();
    }

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
            finishCompressing();
        }
    }

    /**
     * 完成压缩
     */
    private void finishCompressing() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            stopWork();
            return;
        }

        // 获取当前配方
        var recipe = getCurrentRecipe();
        if (recipe == null) {
            stopWork();
            return;
        }

        // 获取压缩结果
        ItemStack result = recipe.getResult();
        if (result.isEmpty()) {
            stopWork();
            return;
        }

        // 添加结果到输出槽
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, result.copy());
        } else {
            currentOutput.grow(result.getCount());
        }

        // 消耗输入物品（根据配方

        input.shrink(recipe.getIngredientCount());

        // 重置进度
        finishWork();

        // 检查是否还可以继续工作
        if (canWork()) {
            isWorking = true;
        }
    }

    /**
     * 获取压缩后的结果物品
     * 使用配方系统查找对应的压缩结
 * @param input 输入物品
     * @return 压缩后的物品
     */
    @SuppressWarnings("unused")
    private ItemStack getCompressedResult(ItemStack input) {
        if (level == null || input.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // 创建配方输入
        com.singularity_iteration.mio_icif.recipe.compressor.mio_icif_CompressorRecipeInput recipeInput =
            new com.singularity_iteration.mio_icif.recipe.compressor.mio_icif_CompressorRecipeInput(input);

        // 查找匹配的配
    var recipe = level.getRecipeManager()
            .getRecipeFor(com.singularity_iteration.mio_icif.recipe.mio_icif_ModRecipes.COMPRESSOR_TYPE.get(), recipeInput, level);

        return recipe.map(r -> r.value().getResult()).orElse(ItemStack.EMPTY);
    }

    /**
     * 获取当前匹配的配
 * @return 当前配方（如果有
 */
    @Nullable
    public com.singularity_iteration.mio_icif.recipe.compressor.mio_icif_CompressorRecipe getCurrentRecipe() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (level == null || input.isEmpty()) {
            return null;
        }

        com.singularity_iteration.mio_icif.recipe.compressor.mio_icif_CompressorRecipeInput recipeInput =
            new com.singularity_iteration.mio_icif.recipe.compressor.mio_icif_CompressorRecipeInput(input);

        var recipe = level.getRecipeManager()
            .getRecipeFor(com.singularity_iteration.mio_icif.recipe.mio_icif_ModRecipes.COMPRESSOR_TYPE.get(), recipeInput, level);

        return recipe.map(r -> r.value()).orElse(null);
    }

    /**
     * 从当前配方获取处理时
 * @return 处理时间（ticks
 */
    public int getRecipeProcessingTime() {
        var recipe = getCurrentRecipe();
        return recipe != null ? recipe.getProcessingTime() : DEFAULT_WORK_TIME;
    }

    /**
     * 从当前配方获取每tick能量消耗
 * @return 每tick能量消耗
 */
    public long getRecipeEnergyPerTick() {
        var recipe = getCurrentRecipe();
        return recipe != null ? recipe.getEnergyPerTick() : DEFAULT_ENERGY_PER_TICK;
    }

    /**
     * 获取输入槽物
 * @return 输入槽物
 */
    public ItemStack getInputItem() {
        return itemHandler.getStackInSlot(INPUT_SLOT);
    }

    /**
     * 获取输出槽物
 * @return 输出槽物
 */
    public ItemStack getOutputItem() {
        return itemHandler.getStackInSlot(OUTPUT_SLOT);
    }

    /**
     * 获取电池槽物
 * @return 电池槽物
 */
    public ItemStack getBatteryItem() {
        return itemHandler.getStackInSlot(BATTERY_SLOT);
    }

    /**
     * 
tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_compressor_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 先调用父类的 tick 逻辑（消耗能量进行工作，包含电池槽放电）
        mio_icif_producer.tick(level, pos, state, blockEntity);

        // 更新方块状态（运行/停止
    boolean isLit = state.getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_compressor_elc.LIT);
        if (blockEntity.isWorking() != isLit) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_compressor_elc.LIT, blockEntity.isWorking()), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.compressor_elc");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.CompressorElcMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // 可以在这里保存额外的数据
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 可以在这里加载额外的数据
    }
}
package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.recipe.mio_icif_ModRecipes;
import com.singularity_iteration.mio_icif.recipe.mio_icif_PowderRecipe;
import com.singularity_iteration.mio_icif.recipe.mio_icif_SingleItemRecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 打粉机方块实体类
 * 使用电力将物品粉碎成粉末
 */
@SuppressWarnings("null")
public class mio_icif_powder_elc extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .battery()
        .output(1)
        .upgrade(4)
        .build();

    // 槽位总数：1输入槽 + 电池槽 + 输出槽 + 4个升级件槽
    public static final int SLOT_COUNT = 7;
    // 输入槽索引
    public static final int INPUT_SLOT = 0;
    // 电池槽索引
    public static final int BATTERY_SLOT = 1;
    // 输出槽索引
    public static final int OUTPUT_SLOT = 2;
    // 升级件槽起始索引
    public static final int UPGRADE_SLOT_START = 3;

    // 默认配置
    // 默认配置（对应 IC2 原版）
    public static final long DEFAULT_CAPACITY = 600L;    // 2 EU/t × 300 ticks = 600 EU），对应 IC2 原版
    public static final long DEFAULT_MAX_RECEIVE = 32L;  // LV级最大输入
    public static final long DEFAULT_MAX_EXTRACT = 0L; // 打粉机不输出能量
    public static final int DEFAULT_WORK_TIME = 300; // 15秒（300 ticks）
    public static final long DEFAULT_ENERGY_PER_TICK = 2L; // 每tick耗电（2 EU），对应 IC2 原版

    // 当前配方
    @SuppressWarnings("unused")
    private Optional<RecipeHolder<mio_icif_PowderRecipe>> currentRecipe = Optional.empty();

    /**
     * 用于 BlockEntityType.Builder 的构造函数
     * 传递已注册的方块实体类型给父类
     */
    public mio_icif_powder_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.POWDER_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_powder_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.LV);
    }

    public mio_icif_powder_elc(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                 long capacity, long maxReceive, long maxExtract,
                                 int workTime, long energyPerTick) {
        super(pos, state, type, capacity, maxReceive, maxExtract, workTime, LAYOUT, energyPerTick, CableTier.LV);
    }



    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) {
            // 输入槽：只接受有对应打粉配方的物品
            return isCrushable(stack);
        } else if (slot == BATTERY_SLOT) {
            // 电池槽：接受可充电的电池物品
            return isBattery(stack);
        } else if (slot == OUTPUT_SLOT) {
            // 输出槽位不允许自动插入
            return false;
        } else if (slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + 4) {
            return getItemAPI().isUpgrade(stack);
        }
        return false;
    }

    /**
     * 指定方向可访问的槽位
     * 打粉机槽位定义：0=输入, 1=电池, 2=输出, 3-6=升级件
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
     * 判断指定槽位是否可以从指定方向提取物品
     * @param slot 槽位
     * @param side 方向
     * @return 是否可以提取
     */
    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        // 输出槽可以从任何方向提取
        return slot == OUTPUT_SLOT;
    }

    /**
     * 判断物品是否可以被粉碎
     * @param stack 物品栈
     * @return 是否可以粉碎
     */
    private boolean isCrushable(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        mio_icif_SingleItemRecipeInput input = new mio_icif_SingleItemRecipeInput(stack);
        return level.getRecipeManager().getRecipeFor(mio_icif_ModRecipes.POWDER_TYPE.get(), input, level).isPresent();
    }

    /**
     * 判断当前输入是否有有效的打粉配方
     * 用于判断进度是否应该重置
     */
    @Override
    protected boolean hasValidRecipe() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        return isCrushable(input);
    }

    /**
     * 查找当前输入对应的打粉配方
     * @return 配方持有者（可能为空）
     */
    private Optional<RecipeHolder<mio_icif_PowderRecipe>> findRecipe() {
        if (level == null) {
            return Optional.empty();
        }
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        mio_icif_SingleItemRecipeInput recipeInput = new mio_icif_SingleItemRecipeInput(input);
        return level.getRecipeManager().getRecipeFor(mio_icif_ModRecipes.POWDER_TYPE.get(), recipeInput, level);
    }

    @Override
    protected boolean canWork() {
        // 检查输入槽是否有物品
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }

        // 检查是否有足够的能量
        if (!hasEnoughEnergy()) {
            return false;
        }

        // 查找配方
        Optional<RecipeHolder<mio_icif_PowderRecipe>> recipe = findRecipe();
        if (recipe.isEmpty()) {
            return false;
        }

        // 检查输出槽是否可以容纳结果
        ItemStack result = recipe.get().value().getResultItem(level.registryAccess());
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (currentOutput.isEmpty()) {
            return true;
        }

        if (!ItemStack.isSameItem(currentOutput, result)) {
            return false;
        }

        int newCount = currentOutput.getCount() + result.getCount();
        return newCount <= currentOutput.getMaxStackSize();
    }

    @Override
    protected void doWork() {
        // �???��?��??
        if (!consumeEnergy()) {
            stopWork();
            return;
        }

        isWorking = true;

        // ?��??��?��?��?�方
        Optional<RecipeHolder<mio_icif_PowderRecipe>> recipe = findRecipe();
        if (recipe.isEmpty()) {
            stopWork();
            return;
        }

        currentRecipe = recipe;

        // 检查是否完成
        if (progress >= maxProgress) {
            finishCrushing(recipe.get());
        }
    }

    /**
     * 完成粉碎
     * @param recipe 打粉配方
     */
    private void finishCrushing(RecipeHolder<mio_icif_PowderRecipe> recipe) {
        ItemStack result = recipe.value().getResultItem(level.registryAccess());
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);

        // 添�?��?��?��?��?�出�?
        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, result.copy());
        } else {
            currentOutput.grow(result.getCount());
        }

        // 消耗输入物品
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        input.shrink(1);

        // 重置进度
        finishWork();

        // 检查是否还可以继续工作
        if (canWork()) {
            isWorking = true;
        }
    }

    /**
     * 获取当前配方的结果物品（用于客户端显示）
     * @return 结果物品栈
     */
    public ItemStack getResultItem() {
        if (level == null) {
            return ItemStack.EMPTY;
        }
        Optional<RecipeHolder<mio_icif_PowderRecipe>> recipe = findRecipe();
        return recipe.map(r -> r.value().getResultItem(level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    /**
     * 获取输入槽物品
     * @return 输入槽物品栈
     */
    public ItemStack getInputItem() {
        return itemHandler.getStackInSlot(INPUT_SLOT);
    }

    /**
     * 获取输出槽位物品
     * @return 输出槽位物品栈
     */
    public ItemStack getOutputItem() {
        return itemHandler.getStackInSlot(OUTPUT_SLOT);
    }

    /**
     * 每 tick 更新方法
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_powder_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 调用父类的 tick 方法（处理能量、工作状态、电池槽等）
        mio_icif_producer.tick(level, pos, state, blockEntity);

        // 更新方块状态（运行/停止）
        boolean isLit = state.getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_powder_elc.LIT);
        if (blockEntity.isWorking() != isLit) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_powder_elc.LIT, blockEntity.isWorking()), 3);
        }
    }

    // MenuProvider ?��?��实现

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.powder_elc");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.PowderElcMenu(containerId, playerInventory, this);
    }
}
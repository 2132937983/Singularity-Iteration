package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.recipe.extractor.mio_icif_ExtractorRecipe;
import com.singularity_iteration.mio_icif.recipe.extractor.mio_icif_ExtractorRecipeInput;
import com.singularity_iteration.mio_icif.recipe.mio_icif_ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
 * 提取机方块实体类
 * 使用电力从原材料中提取更多资
 * 槽位结构
输入 + 1输出 + 1电池 + 4物品
= 7
 */
@SuppressWarnings("null")
public class mio_icif_extrator_elc extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .output(1)
        .battery()
        .upgrade(4)
        .build();

    // 槽位数量
public static final int SLOT_COUNT = 7;
    // 输入槽索
public static final int INPUT_SLOT = 0;
    // 输出槽索
public static final int OUTPUT_SLOT = 1;
    // 电池槽索
public static final int BATTERY_SLOT = 2;
    // 升级槽起始索引（4个升级槽
public static final int UPGRADE_SLOT_START = 3;

    // 默认配置（对
public static final long DEFAULT_CAPACITY = 600L;   // 2 EU/t × 300 ticks = 600 EU
    public static final long DEFAULT_MAX_RECEIVE = 32L;  // LV级最大输
public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 300; // 15秒（300 ticks
public static final long DEFAULT_ENERGY_PER_TICK = 2L; // 每tick消

    // 当前正在处理的配
private mio_icif_ExtractorRecipe currentRecipe = null;

    /**
     * 用于 BlockEntityType.Builder 的构造函
 */
    public mio_icif_extrator_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.EXTRACTOR_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_extrator_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.LV);
    }

    public mio_icif_extrator_elc(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                  long capacity, long maxReceive, long maxExtract,
                                  int workTime, long energyPerTick) {
        super(pos, state, type, capacity, maxReceive, maxExtract, workTime, LAYOUT, energyPerTick, CableTier.LV);
    }



    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) {
            return isExtractable(stack);
        } else if (slot == OUTPUT_SLOT) {
            return false;
        } else if (slot == BATTERY_SLOT) {
            return isBattery(stack);
        } else if (slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + 4) {
            return getItemAPI().isUpgrade(stack);
        }
        return false;
    }

    /**
     * 检查物品是否可以被提取机加
     */
    private boolean isExtractable(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (getItemAPI().isUpgrade(stack)) return false;
        if (level == null) return true;

        mio_icif_ExtractorRecipeInput recipeInput = new mio_icif_ExtractorRecipeInput(stack);
        return level.getRecipeManager()
            .getRecipeFor(mio_icif_ModRecipes.EXTRACTOR_TYPE.get(), recipeInput, level).isPresent();
    }

    /**
     * 获取指定方向可访问的槽位
     * 提取机槽位结构：0=输入, 1=输出, 2=电池, 3-6=升级
 * @param side 方向
     * @return 可访问的槽位数组
     */
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return new int[]{INPUT_SLOT, OUTPUT_SLOT, BATTERY_SLOT};
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
     * 获取当前输入物品的提取配
 * @return 提取配方（如果存在）
     */
    private mio_icif_ExtractorRecipe findRecipe() {
        if (level == null) {
            return null;
        }
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return null;
        }
        
        mio_icif_ExtractorRecipeInput recipeInput = new mio_icif_ExtractorRecipeInput(input);
        Optional<RecipeHolder<mio_icif_ExtractorRecipe>> recipe = level.getRecipeManager()
            .getRecipeFor(mio_icif_ModRecipes.EXTRACTOR_TYPE.get(), recipeInput, level);
        
        return recipe.map(RecipeHolder::value).orElse(null);
    }

    @Override
    protected boolean canWork() {
        // 检查是否有输入物品
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            currentRecipe = null;
            return false;
        }

        // 查找配方
        mio_icif_ExtractorRecipe recipe = findRecipe();
        if (recipe == null) {
            currentRecipe = null;
            return false;
        }

        // 检查是否有足够能量
        if (!hasEnoughEnergy()) {
            return false;
        }

        // 检查输出槽是否可以容纳结果
        ItemStack result = recipe.getResultItem(level.registryAccess());
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (currentOutput.isEmpty()) {
            currentRecipe = recipe;
            return true;
        }

        if (!ItemStack.isSameItem(currentOutput, result) ||
            !ItemStack.isSameItemSameComponents(currentOutput, result)) {
            currentRecipe = null;
            return false;
        }

        int newCount = currentOutput.getCount() + result.getCount();
        if (newCount > currentOutput.getMaxStackSize()) {
            return false;
        }

        currentRecipe = recipe;
        return true;
    }

    /**
     * 检查当前输入物品是否有有效的提取配
     * 用于判断当输入物品变更时是否应该重置进度
     */
    @Override
    protected boolean hasValidRecipe() {
        return findRecipe() != null;
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
            finishExtraction();
        }
    }

    /**
     * 完成提取
     */
    private void finishExtraction() {
        if (currentRecipe == null || level == null) {
            stopWork();
            return;
        }

        ItemStack result = currentRecipe.getResultItem(level.registryAccess()).copy();
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);

        // 添加结果到输出槽
        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, result);
        } else {
            currentOutput.grow(result.getCount());
        }

        // 消耗输入物
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
     * @return 结果物品
     */
    public ItemStack getResultItem() {
        if (currentRecipe != null && level != null) {
            return currentRecipe.getResultItem(level.registryAccess()).copy();
        }
        mio_icif_ExtractorRecipe recipe = findRecipe();
        return recipe != null && level != null ? recipe.getResultItem(level.registryAccess()).copy() : ItemStack.EMPTY;
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
     * 获取指定物品槽的物品
     * @param index 物品槽索引（0-3
 * @return 物品槽中的物
 */
    public ItemStack getUpgradeSlot(int index) {
        if (index >= 0 && index < 4) {
            return itemHandler.getStackInSlot(UPGRADE_SLOT_START + index);
        }
        return ItemStack.EMPTY;
    }

    /**
     * 
tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_extrator_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 先调用父类的 tick 逻辑（消耗能量进行工作，包含电池槽放电）
        mio_icif_producer.tick(level, pos, state, blockEntity);

        // 更新方块状态（运行/停止
    boolean isLit = state.getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_extrator_elc.LIT);
        if (blockEntity.isWorking() != isLit) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_extrator_elc.LIT, blockEntity.isWorking()), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // 保存当前配方信息（如果需要）
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 加载当前配方信息（如果需要）
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.extrator_elc");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.ExtractorElcMenu(containerId, playerInventory, this);
    }
}
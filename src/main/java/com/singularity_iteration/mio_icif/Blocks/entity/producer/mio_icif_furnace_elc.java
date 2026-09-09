package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 电方块实体
 * 使用电力进行冶炼，不需要燃
 */
@SuppressWarnings("null")
public class mio_icif_furnace_elc extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .battery()
        .output(1)
        .upgrade(4)
        .build();

    // 槽位数量：输入槽 + 电池
    public static final int SLOT_COUNT = 7;
    // 输入槽索
public static final int INPUT_SLOT = 0;
    // 电池槽索
public static final int BATTERY_SLOT = 1;
    // 输出槽索
public static final int OUTPUT_SLOT = 2;
    // 插件槽起始索
public static final int UPGRADE_SLOT_START = 3;

    // 默认配置（对
public static final long DEFAULT_CAPACITY = 300L;    // 3 EU/t × 100 ticks = 300 EU（对齐IC2原版
public static final long DEFAULT_MAX_RECEIVE = 32L;  // LV级最大输
public static final long DEFAULT_MAX_EXTRACT = 0L;   // 电炉不输出能
public static final int DEFAULT_COOK_TIME = 100;     // 5秒（100 ticks，对齐IC2原版电炉
public static final long DEFAULT_ENERGY_PER_TICK = 3L; // 每tick消


    // 当前配方
    @SuppressWarnings("unused")
    private Optional<RecipeHolder<SmeltingRecipe>> currentRecipe = Optional.empty();

    // 记录已使用的配方及其次数（用于经验计算）
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();

    /**
     * 用于 BlockEntityType.Builder 的构造函
 * 自动获取已注册的方块实体类型
     */
    public mio_icif_furnace_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.FURNACE_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_furnace_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_COOK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.LV);
    }

    public mio_icif_furnace_elc(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                 long capacity, long maxReceive, long maxExtract,
                                 int cookTime, long energyPerTick) {
        super(pos, state, type, capacity, maxReceive, maxExtract, cookTime, LAYOUT, energyPerTick, CableTier.LV);
    }



    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) {
            // 输入槽：检查是否是有效的冶炼原
        return isSmeltable(stack);
        } else if (slot == BATTERY_SLOT) {
            // 电池槽：接受能量存储物品（电池）
            return isBattery(stack);
        } else if (slot == OUTPUT_SLOT) {
            // 输出槽：不允许手动放入物
        return false;
        } else if (slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + 4) {
            return getItemAPI().isUpgrade(stack);
        }
        return false;
    }

    /**
     * 获取指定方向可访问的槽位
 * 电槽位结构：0=输入, 1=电池, 2=输出, 3-6=插件
     * @param side 方向
     * @return 可访问的槽位数组
     */
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // 所有面都可以访问所有槽位（升级槽除外，与IC2原版一致）
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
        // 输出槽可以从任何方向提取
        return slot == OUTPUT_SLOT;
    }

    /**
     * 检查物品是否可以冶
 * @param stack 物品
 * @return 是否可以冶炼
     */
    private boolean isSmeltable(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, stack);
        SingleRecipeInput input = new SingleRecipeInput(stack);
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level).isPresent();
    }

    /**
     * 查找当前输入物品的冶炼配
 *
     * @return 配方（如果存在）
     */
    private Optional<RecipeHolder<SmeltingRecipe>> findRecipe() {
        if (level == null) {
            return Optional.empty();
        }
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, level);
    }

    /**
     * 检查当前输入物品是否有有效的冶炼配
     * 用于判断当输入物品变更时是否应该重置进度
     */
    @Override
    protected boolean hasValidRecipe() {
        return findRecipe().isPresent();
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

        // 查找配方
        Optional<RecipeHolder<SmeltingRecipe>> recipe = findRecipe();
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
        // 消耗能
    if (!consumeEnergy()) {
            stopWork();
            return;
        }

        isWorking = true;

        // 获取当前配方
        Optional<RecipeHolder<SmeltingRecipe>> recipe = findRecipe();
        if (recipe.isEmpty()) {
            stopWork();
            return;
        }

        currentRecipe = recipe;

        // 检查是否完
    if (progress >= maxProgress) {
            finishSmelting(recipe.get());
        }
    }

    /**
     * 完成冶炼
     * @param recipe 冶炼配方
     */
    private void finishSmelting(RecipeHolder<SmeltingRecipe> recipe) {
        ItemStack result = recipe.value().getResultItem(level.registryAccess());
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);

        // 添加结果到输出槽
        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, result.copy());
        } else {
            currentOutput.grow(result.getCount());
        }

        // 消耗输入物
    ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        input.shrink(1);

        // 记录配方使用（用于经验计算）
        ResourceLocation recipeId = recipe.id();
        recipesUsed.addTo(recipeId, 1);

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
        if (level == null) {
            return ItemStack.EMPTY;
        }
        Optional<RecipeHolder<SmeltingRecipe>> recipe = findRecipe();
        return recipe.map(r -> r.value().getResultItem(level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    /**
      * 获取输入槽物品
 * @return 输入槽物品
 */
    public ItemStack getInputItem() {
        return itemHandler.getStackInSlot(INPUT_SLOT);
    }

    /**
      * 获取输出槽物品
 * @return 输出槽物品
 */
    public ItemStack getOutputItem() {
        return itemHandler.getStackInSlot(OUTPUT_SLOT);
    }

    /**
     * 
tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_furnace_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 先调用父类的 tick 逻辑（消耗能量进行工作，包含电池槽放电）
        mio_icif_producer.tick(level, pos, state, blockEntity);

        // 更新方块状态（运行/停止
    boolean isLit = state.getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_furnace_elc.LIT);
        if (blockEntity.isWorking() != isLit) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_furnace_elc.LIT, blockEntity.isWorking()), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // 保存已使用的配方记录
        CompoundTag recipesTag = new CompoundTag();
        recipesUsed.forEach((recipeId, count) -> recipesTag.putInt(recipeId.toString(), count));
        tag.put("RecipesUsed", recipesTag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 加载已使用的配方记录
        recipesUsed.clear();
        if (tag.contains("RecipesUsed", CompoundTag.TAG_COMPOUND)) {
            CompoundTag recipesTag = tag.getCompound("RecipesUsed");
            for (String key : recipesTag.getAllKeys()) {
                recipesUsed.put(ResourceLocation.parse(key), recipesTag.getInt(key));
            }
        }
    }

    // ==================== 经验给予功能 ====================

    /**
     * 给予玩家使用配方获得的经
     * 在玩家取出物品时调用
     * @param player 玩家
     */
    public void awardUsedRecipesAndPopExperience(ServerPlayer player) {
        List<RecipeHolder<?>> list = this.getRecipesToAwardAndPopExperience(player.serverLevel(), player.position());
        player.awardRecipes(list);

        for (RecipeHolder<?> recipeHolder : list) {
            if (recipeHolder != null) {
                // 构建物品列表用于触发配方合成事件
                List<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    items.add(itemHandler.getStackInSlot(i));
                }
                player.triggerRecipeCrafted(recipeHolder, items);
            }
        }

        this.recipesUsed.clear();
    }

    /**
     * 获取需要给予经验的配方列表，并生成经验
     * @param level 服务器世
     * @param popVec 经验球生成位
     * @return 配方列表
     */
    public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel level, Vec3 popVec) {
        List<RecipeHolder<?>> list = new ArrayList<>();

        for (Object2IntMap.Entry<ResourceLocation> entry : recipesUsed.object2IntEntrySet()) {
            level.getRecipeManager().byKey(entry.getKey()).ifPresent(recipeHolder -> {
                list.add(recipeHolder);
                if (recipeHolder.value() instanceof SmeltingRecipe smeltingRecipe) {
                    createExperience(level, popVec, entry.getIntValue(), smeltingRecipe.getExperience());
                }
            });
        }

        return list;
    }

    /**
     * 创建经验
     * @param level 服务器世
     * @param popVec 经验球生成位
     * @param count 配方使用次数
     * @param experience 每次使用获得的经验
     */
    private static void createExperience(ServerLevel level, Vec3 popVec, int count, float experience) {
        int amount = Mth.floor((float) count * experience);
        float frac = Mth.frac((float) count * experience);
        if (frac != 0.0F && Math.random() < (double) frac) {
            amount++;
        }

        while (amount > 0) {
            int orbValue = ExperienceOrb.getExperienceValue(amount);
            amount -= orbValue;
            level.addFreshEntity(new ExperienceOrb(level, popVec.x, popVec.y, popVec.z, orbValue));
        }
    }

    /**
     * 获取存储的经验值（用于显示或其他用途）
     * @return 经验
     */
    @SuppressWarnings("unchecked")
    public float getStoredExperience() {
        if (level == null) return 0.0f;

        float totalExperience = 0.0f;
        for (Object2IntMap.Entry<ResourceLocation> entry : recipesUsed.object2IntEntrySet()) {
            Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getRecipeManager().byKey(entry.getKey())
                .map(r -> (RecipeHolder<SmeltingRecipe>) (RecipeHolder<?>) r);
            if (recipe.isPresent()) {
                totalExperience += entry.getIntValue() * recipe.get().value().getExperience();
            }
        }
        return totalExperience;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.furnace_elc");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.FurnaceElcMenu(id, playerInventory, this);
    }
}
package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.block.producer.mio_icif_block_metal_former;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.common.recipe.metal_former.cutting.mio_icif_CuttingRecipe;
import com.miophas.singularity_iteration.common.recipe.metal_former.cutting.mio_icif_CuttingRecipeInput;
import com.miophas.singularity_iteration.common.recipe.metal_former.cutting.mio_icif_CuttingRecipes;
import com.miophas.singularity_iteration.common.recipe.metal_former.extruding.mio_icif_ExtrudingRecipe;
import com.miophas.singularity_iteration.common.recipe.metal_former.extruding.mio_icif_ExtrudingRecipeInput;
import com.miophas.singularity_iteration.common.recipe.metal_former.extruding.mio_icif_ExtrudingRecipes;
import com.miophas.singularity_iteration.common.recipe.metal_former.rolling.mio_icif_RollingRecipe;
import com.miophas.singularity_iteration.common.recipe.metal_former.rolling.mio_icif_RollingRecipeInput;
import com.miophas.singularity_iteration.common.recipe.metal_former.rolling.mio_icif_RollingRecipes;
import com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 金属成型机方块实体类
 * 支持三种加工模式：辊压/切割/挤压
 * 包含7个槽位：输入槽、电池槽、输出槽、4个升级件槽
 */
@SuppressWarnings("null")
public class mio_icif_metal_former extends AbstractProcessingMachineBlockEntity {
    // Per-call-site recipe memo: canWork() runs every tick, getRecipeFor is a linear scan.
    private final com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<com.miophas.singularity_iteration.common.recipe.metal_former.rolling.mio_icif_RollingRecipeInput, com.miophas.singularity_iteration.common.recipe.metal_former.rolling.mio_icif_RollingRecipe> recipeCache1 = new com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<>(mio_icif_RollingRecipes.ROLLING_TYPE);
    private final com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<com.miophas.singularity_iteration.common.recipe.metal_former.cutting.mio_icif_CuttingRecipeInput, com.miophas.singularity_iteration.common.recipe.metal_former.cutting.mio_icif_CuttingRecipe> recipeCache2 = new com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<>(mio_icif_CuttingRecipes.CUTTING_TYPE);
    private final com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<com.miophas.singularity_iteration.common.recipe.metal_former.extruding.mio_icif_ExtrudingRecipeInput, com.miophas.singularity_iteration.common.recipe.metal_former.extruding.mio_icif_ExtrudingRecipe> recipeCache3 = new com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<>(mio_icif_ExtrudingRecipes.EXTRUDING_TYPE);
    private final com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<com.miophas.singularity_iteration.common.recipe.metal_former.rolling.mio_icif_RollingRecipeInput, com.miophas.singularity_iteration.common.recipe.metal_former.rolling.mio_icif_RollingRecipe> recipeCache4 = new com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<>(mio_icif_RollingRecipes.ROLLING_TYPE);
    private final com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<com.miophas.singularity_iteration.common.recipe.metal_former.cutting.mio_icif_CuttingRecipeInput, com.miophas.singularity_iteration.common.recipe.metal_former.cutting.mio_icif_CuttingRecipe> recipeCache5 = new com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<>(mio_icif_CuttingRecipes.CUTTING_TYPE);
    private final com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<com.miophas.singularity_iteration.common.recipe.metal_former.extruding.mio_icif_ExtrudingRecipeInput, com.miophas.singularity_iteration.common.recipe.metal_former.extruding.mio_icif_ExtrudingRecipe> recipeCache6 = new com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<>(mio_icif_ExtrudingRecipes.EXTRUDING_TYPE);

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .battery()
        .output(1)
        .upgrade(4)
        .build();

    // 槽位总数
    public static final int SLOT_COUNT = 7;
    // 输入槽索引
    public static final int INPUT_SLOT = 0;
    // 电池槽索引
    public static final int BATTERY_SLOT = 1;
    // 输出槽索引
    public static final int OUTPUT_SLOT = 2;
    // 升级件槽起始索引
    public static final int PLUGIN_SLOT_START = 3;
    // 升级件槽数量
    public static final int PLUGIN_SLOT_COUNT = 4;

    // 默认配置（对应 IC2 原版）
    public static final long DEFAULT_CAPACITY = 2000L;   // 10 EU/t × 200 ticks = 2000 EU
    public static final long DEFAULT_MAX_RECEIVE = 32L;  // LV级最大输入
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 200; // 10秒（200 ticks）
    public static final long DEFAULT_ENERGY_PER_TICK = 10L; // 每tick耗电（10 EU），对应 IC2 原版

    // 加工模式枚举
public enum MetalFormerMode {
        ROLLING,
        CUTTING,
        EXTRUDING
    }

    // 当前模式
    private MetalFormerMode currentMode = MetalFormerMode.ROLLING;

    /**
     * 用于 BlockEntityType.Builder 的构造函数
     */
    public mio_icif_metal_former(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.METAL_FORMER_ENTITY_TYPE.get());
    }

    public mio_icif_metal_former(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.LV);
    }



    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> isValidMetalFormerInput(stack);
            case BATTERY_SLOT -> isBattery(stack);
            case OUTPUT_SLOT -> false;
            default -> getItemAPI().isUpgrade(stack);
        };
    }

    /**
     * 判断物品是否可以被金属成型机接受为输入
     * 物品必须被至少一种模式（辊压/切割/挤压）的配方匹配
     */
    private boolean isValidMetalFormerInput(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (isBattery(stack)) return false;
        if (getItemAPI().isUpgrade(stack)) return false;

        if (level == null) return true;

        ItemStack checkStack = stack.copyWithCount(64);

        mio_icif_RollingRecipeInput rollingInput = new mio_icif_RollingRecipeInput(checkStack);
        boolean canRoll = recipeCache1.find(level, rollingInput).isPresent();

        mio_icif_CuttingRecipeInput cuttingInput = new mio_icif_CuttingRecipeInput(checkStack);
        boolean canCut = recipeCache2.find(level, cuttingInput).isPresent();

        mio_icif_ExtrudingRecipeInput extrudingInput = new mio_icif_ExtrudingRecipeInput(checkStack);
        boolean canExtrude = recipeCache3.find(level, extrudingInput).isPresent();

        return canRoll || canCut || canExtrude;
    }

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

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    protected boolean canWork() {
        // �??��?��?��??��?��?��?��??
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }

        // �??��?��?��??�足够�?��??
        if (getEffectiveEnergyPerTick() <= 0 || progress < Math.max(1, getRecipeProcessingTime()) && !hasEnoughEnergy()) {
            return false;
        }

        // ?��?��当�?�模式�???��??��??
        return switch (currentMode) {
            case ROLLING -> canWorkRolling(input);
            case CUTTING -> canWorkCutting(input);
            case EXTRUDING -> canWorkExtruding(input);
        };
    }

    /**
     * 判断是否应该重置进度
     * 当输入槽为空或配方不匹配时重置，断电不保留进度
     */
    @Override
    protected boolean shouldResetProgress() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        // 输入槽为空，重置进度
        if (input.isEmpty()) {
            return true;
        }

        // 判断当前模式是否有匹配的配方
        return switch (currentMode) {
            case ROLLING -> getRollingRecipe() == null;
            case CUTTING -> getCuttingRecipe() == null;
            case EXTRUDING -> getExtrudingRecipe() == null;
        };
    }

    private boolean canWorkRolling(ItemStack input) {
        var recipe = getRollingRecipe();
        return recipe != null && RecipeSlots.prepare(itemHandler, INPUT_SLOT, recipe.getIngredientCount(),
            new int[]{OUTPUT_SLOT}, java.util.List.of(recipe.getResultItem(level.registryAccess()))).isPresent();
    }

    private boolean canWorkCutting(ItemStack input) {
        var recipe = getCuttingRecipe();
        return recipe != null && RecipeSlots.prepare(itemHandler, INPUT_SLOT, recipe.getIngredientCount(),
            new int[]{OUTPUT_SLOT}, java.util.List.of(recipe.getResultItem(level.registryAccess()))).isPresent();
    }

    private boolean canWorkExtruding(ItemStack input) {
        var recipe = getExtrudingRecipe();
        return recipe != null && RecipeSlots.prepare(itemHandler, INPUT_SLOT, recipe.getIngredientCount(),
            new int[]{OUTPUT_SLOT}, java.util.List.of(recipe.getResultItem(level.registryAccess()))).isPresent();
    }



    @Override
    protected void doWork() {
        if (progress < Math.max(1, getRecipeProcessingTime()) && !consumeEnergy()) { stopWork(); return; }
        isWorking = true;
    }

    /**
     * 更新工作进度 - 覆盖父类以使用配方相关的时间（超频时减少时间/增加频率）
     */
    @Override
    protected void updateProgress() {
        if (isWorking) {
            int recipeMaxProgress = Math.max(1, getRecipeProcessingTime());
            int progressPerTick = getProgressPerTick();
            if (progress < recipeMaxProgress) {
                progress = (int) Math.min(recipeMaxProgress, (long) progress + progressPerTick);
            }
            // 检查是否完成
            if (progress >= recipeMaxProgress) {
                finishProcessing();
            }
        }
    }

    /**
     * 完成加工
     */
    private void finishProcessing() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            stopWork();
            return;
        }

        // ?��?��当�?�模式�?��?��?�工
        switch (currentMode) {
            case ROLLING -> finishRolling(input);
            case CUTTING -> finishCutting(input);
            case EXTRUDING -> finishExtruding(input);
        }
    }

    private void finishRolling(ItemStack input) {
        var recipe = getRollingRecipe();
        if (recipe == null) {
            stopWork();
            return;
        }
        processRecipeResult(recipe.getResultItem(level.registryAccess()), recipe.getIngredientCount(), input);
    }

    private void finishCutting(ItemStack input) {
        var recipe = getCuttingRecipe();
        if (recipe == null) {
            stopWork();
            return;
        }
        processRecipeResult(recipe.getResultItem(level.registryAccess()), recipe.getIngredientCount(), input);
    }

    private void finishExtruding(ItemStack input) {
        var recipe = getExtrudingRecipe();
        if (recipe == null) {
            stopWork();
            return;
        }
        processRecipeResult(recipe.getResultItem(level.registryAccess()), recipe.getIngredientCount(), input);
    }

    private void processRecipeResult(ItemStack result, int ingredientCount, ItemStack input) {
        var operation = RecipeSlots.prepare(itemHandler, INPUT_SLOT, ingredientCount, new int[]{OUTPUT_SLOT}, java.util.List.of(result));
        if (operation.isEmpty()) { stopWork(); return; }
        int completed = progress;
        progress = 0;
        if (!operation.get().commit()) { progress = completed; stopWork(); return; }
        finishWork();
        setChanged();
    }

    /**
     * 获取辊压配方匹配
     */
    @Nullable
    private mio_icif_RollingRecipe getRollingRecipe() {
        if (level == null) return null;
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return null;

        mio_icif_RollingRecipeInput recipeInput = new mio_icif_RollingRecipeInput(input);
        var recipe = recipeCache4.find(level, recipeInput);

        return recipe.map(r -> r.value()).orElse(null);
    }

    @Nullable
    private mio_icif_CuttingRecipe getCuttingRecipe() {
        if (level == null) return null;
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return null;

        mio_icif_CuttingRecipeInput recipeInput = new mio_icif_CuttingRecipeInput(input);
        var recipe = recipeCache5.find(level, recipeInput);

        return recipe.map(r -> r.value()).orElse(null);
    }

    @Nullable
    private mio_icif_ExtrudingRecipe getExtrudingRecipe() {
        if (level == null) return null;
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return null;

        mio_icif_ExtrudingRecipeInput recipeInput = new mio_icif_ExtrudingRecipeInput(input);
        var recipe = recipeCache6.find(level, recipeInput);

        return recipe.map(r -> r.value()).orElse(null);
    }

    /**
     * 获取当前配方相关的处理时间
     */
    public int getRecipeProcessingTime() {
        return switch (currentMode) {
            case ROLLING -> {
                var recipe = getRollingRecipe();
                yield recipe != null ? recipe.getProcessingTime() : DEFAULT_WORK_TIME;
            }
            case CUTTING -> {
                var recipe = getCuttingRecipe();
                yield recipe != null ? recipe.getProcessingTime() : DEFAULT_WORK_TIME;
            }
            case EXTRUDING -> {
                var recipe = getExtrudingRecipe();
                yield recipe != null ? recipe.getProcessingTime() : DEFAULT_WORK_TIME;
            }
        };
    }

    /**
     * 获取当前配方相关的每tick耗电量
     */
    public long getRecipeEnergyPerTick() {
        return switch (currentMode) {
            case ROLLING -> {
                var recipe = getRollingRecipe();
                yield recipe != null ? recipe.getEnergyPerTick() : DEFAULT_ENERGY_PER_TICK;
            }
            case CUTTING -> {
                var recipe = getCuttingRecipe();
                yield recipe != null ? recipe.getEnergyPerTick() : DEFAULT_ENERGY_PER_TICK;
            }
            case EXTRUDING -> {
                var recipe = getExtrudingRecipe();
                yield recipe != null ? recipe.getEnergyPerTick() : DEFAULT_ENERGY_PER_TICK;
            }
        };
    }

    /**
     * 设置加工模式
     */
    public void setMode(MetalFormerMode mode) {
        if (mode == null) return;
        stopWork();
        this.currentMode = mode;
        this.progress = 0;
        if (getLevel() != null && !getLevel().isClientSide) {
            getLevel().setBlockAndUpdate(getBlockPos(), getBlockState().setValue(mio_icif_block_metal_former.MODE,
                mio_icif_block_metal_former.MetalFormerMode.valueOf(mode.name())));
        }
        setChanged();
    }

    /**
     * 获取当前加工模式
     */
    public MetalFormerMode getMode() {
        return currentMode;
    }

    @Override
    public int getMaxProgress() {
        return getRecipeProcessingTime();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Mode", currentMode.ordinal());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int modeOrdinal = tag.getInt("Mode");
        if (modeOrdinal >= 0 && modeOrdinal < MetalFormerMode.values().length) {
            this.currentMode = MetalFormerMode.values()[modeOrdinal];
        }
    }

    /**
     * �?tick ?��?��??��??
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_metal_former blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 调用父类的 tick 方法（处理能量、电池槽等）
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, blockEntity);

        // 更新方块状态（运行/停止）
        boolean isLit = state.getValue(mio_icif_block_metal_former.LIT);
        if (blockEntity.isWorking() != isLit) {
            level.setBlock(pos, state.setValue(mio_icif_block_metal_former.LIT, blockEntity.isWorking()), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.metal_former");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.producer.MetalFormerMenu(id, playerInventory, this);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return getSlotsForDirection(side);
    }
}
package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipeInput;
import com.singularity_iteration.mio_icif.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipes;
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
import org.jetbrains.annotations.Nullable;

public class mio_icif_molecular_transformer extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .output(1)
        .build();

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int TOTAL_SLOTS = 2;

    // 参考ASP源代码：分子转换仪接受IV等级（最大8192 EU/t）
    // 能量容量需要足够大以支持高EU消耗（最大配方需要约20,000,000 EU）
    public static final long DEFAULT_CAPACITY = 120_000_000L;  // 最大能量容量（EU）
    public static final long DEFAULT_MAX_RECEIVE = 8192L;      // IV等级输入
    public static final long DEFAULT_MAX_EXTRACT = 0L;         // 不输出能量
    public static final int DEFAULT_MAX_PROGRESS = 100;        // 最大进度（实际使用EU消耗）
    public static final long DEFAULT_ENERGY_PER_TICK = 8192L;  // 每tick消耗8192 EU

    // 累计消耗的 EU，用于追踪配方进度值
    private long consumedEU = 0;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                // 返回 0-100 的进度百分比，用于 UI 显示
                case 0 -> currentRecipeEU != null && currentRecipeEU > 0
                    ? (int) Math.min((consumedEU * 100) / currentRecipeEU, 100)
                    : 0;
                case 1 -> 100; // maxProgress 固定为100（百分比）
                case 2 -> isWorking ? 1 : 0;
                case 3 -> (int) Math.min(energyStorage.getAmount(), Integer.MAX_VALUE);
                case 4 -> (int) Math.min(energyStorage.getCapacity(), Integer.MAX_VALUE);
                case 5 -> currentRecipeEU != null ? (int) Math.min(currentRecipeEU, Integer.MAX_VALUE) : 0;
                case 6 -> lastEnergyPerTick != null ? (int) Math.min(lastEnergyPerTick, Integer.MAX_VALUE) : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 客户端不能直接设置这些值
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    private Long currentRecipeEU = null;
    private Long lastEnergyPerTick = null;

    public mio_icif_molecular_transformer(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.MOLECULAR_TRANSFORMER_ENTITY_TYPE.get());
    }

    public mio_icif_molecular_transformer(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_MAX_PROGRESS,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.IV);
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return new int[]{INPUT_SLOT, OUTPUT_SLOT};
    }

    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == INPUT_SLOT) {
            return itemHandler.isItemValid(slot, stack);
        }
        return false;
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) {
            if (level == null || stack.isEmpty()) return false;
            var recipeManager = level.getRecipeManager();
            var input = new mio_icif_MolecularTransformerRecipeInput(stack);
            return recipeManager.getRecipeFor(mio_icif_MolecularTransformerRecipes.MOLECULAR_TRANSFORMER_TYPE.get(), input, level).isPresent();
        }
        return false;
    }

    @Override
    protected int[] getInputSlots() {
        return new int[]{INPUT_SLOT};
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

    /**
     * 判断当前输入是否有有效的分子转换配方
     * 用于判断当前物品是否更该设置进度
     */
    @Override
    protected boolean hasValidRecipe() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || level == null) return false;
        var recipeManager = level.getRecipeManager();
        var recipeInput = new mio_icif_MolecularTransformerRecipeInput(input);
        return recipeManager.getRecipeFor(mio_icif_MolecularTransformerRecipes.MOLECULAR_TRANSFORMER_TYPE.get(), recipeInput, level).isPresent();
    }

    @Override
    protected boolean canWork() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return false;

        if (level == null) return false;
        var recipeManager = level.getRecipeManager();
        var recipeInput = new mio_icif_MolecularTransformerRecipeInput(input);
        var recipeOpt = recipeManager.getRecipeFor(mio_icif_MolecularTransformerRecipes.MOLECULAR_TRANSFORMER_TYPE.get(), recipeInput, level);
        if (recipeOpt.isEmpty()) return false;

        var recipe = recipeOpt.get().value();

        long recipeEUCost = recipe.getEuCost();
        if (currentRecipeEU == null || currentRecipeEU != recipeEUCost) {
            currentRecipeEU = recipeEUCost;
            consumedEU = 0;
        }

        if (consumedEU >= currentRecipeEU) {
            return true;
        }

        if (!canAddItem(OUTPUT_SLOT, recipe.getResult())) return false;

        return energyStorage.getAmount() >= getEffectiveEnergyPerTick();
    }

    @Override
    protected void doWork() {
        if (currentRecipeEU == null) return;

        if (consumedEU >= currentRecipeEU) {
            operate();
            return;
        }

        long euNeeded = currentRecipeEU - consumedEU;

        long effectiveEnergyPerTick = getEffectiveEnergyPerTick();
        long euPerTick = Math.min(effectiveEnergyPerTick, euNeeded);

        long extracted = apiUseEnergy(euPerTick, false);
        if (extracted <= 0) {
            stopWork();
            return;
        }

        lastEnergyPerTick = extracted;
        isWorking = true;
        consumedEU += extracted;

        syncProgressToBase();

        if (consumedEU >= currentRecipeEU) {
            operate();
        }
    }

    private void syncProgressToBase() {
        if (currentRecipeEU != null && currentRecipeEU > 0) {
            this.progress = (int) Math.min((consumedEU * maxProgress) / currentRecipeEU, maxProgress);
        }
    }

    @Override
    protected void updateProgress() {
    }

    @Override
    protected void checkInputChanged() {
        int[] inputSlots = getInputSlots();
        if (inputSlots.length == 0) return;

        if (lastInputStacks == null || lastInputStacks.length != inputSlots.length) {
            lastInputStacks = new ItemStack[inputSlots.length];
            for (int i = 0; i < inputSlots.length; i++) {
                lastInputStacks[i] = itemHandler.getStackInSlot(inputSlots[i]).copy();
            }
            return;
        }

        boolean changed = false;
        for (int i = 0; i < inputSlots.length; i++) {
            ItemStack current = itemHandler.getStackInSlot(inputSlots[i]);
            ItemStack last = lastInputStacks[i];
            if (!ItemStack.isSameItemSameComponents(current, last)) {
                changed = true;
                break;
            }
        }

        if (changed) {
            progress = 0;
            isWorking = false;
            consumedEU = 0;
            currentRecipeEU = null;
            lastEnergyPerTick = null;
            for (int i = 0; i < inputSlots.length; i++) {
                lastInputStacks[i] = itemHandler.getStackInSlot(inputSlots[i]).copy();
            }
            setChanged();
        }
    }

    @Override
    protected boolean shouldResetProgress() {
        int[] inputSlots = getInputSlots();
        if (inputSlots.length == 0) return false;
        ItemStack input = itemHandler.getStackInSlot(inputSlots[0]);
        if (input.isEmpty()) {
            consumedEU = 0;
            currentRecipeEU = null;
            lastEnergyPerTick = null;
            return true;
        }
        boolean valid = hasValidRecipe();
        if (!valid) {
            consumedEU = 0;
            currentRecipeEU = null;
            lastEnergyPerTick = null;
        }
        return !valid;
    }

    private void operate() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            resetWork();
            return;
        }

        if (level == null) {
            resetWork();
            return;
        }

        var recipeManager = level.getRecipeManager();
        var recipeInput = new mio_icif_MolecularTransformerRecipeInput(input);
        var recipeOpt = recipeManager.getRecipeFor(mio_icif_MolecularTransformerRecipes.MOLECULAR_TRANSFORMER_TYPE.get(), recipeInput, level);

        if (recipeOpt.isPresent()) {
            var recipe = recipeOpt.get().value();
            // 再次验证是否可以添加到输出槽
            if (canAddItem(OUTPUT_SLOT, recipe.getResult())) {
                input.shrink(1);
                addItemToSlot(OUTPUT_SLOT, recipe.getResult().copy());
                // 成功产出物品，重置工作状态
                resetWork();
            } else {
                // 无法添加到输出槽，停止工作状态，不重置进度
                // 等待输出槽位有空位时再继续
                isWorking = false;
                stopWork();
            }
            return;
        }

        // ??�方不�?�在，�?�置工�??
        resetWork();
    }

    private void resetWork() {
        progress = 0;
        maxProgress = DEFAULT_MAX_PROGRESS;
        consumedEU = 0;
        currentRecipeEU = null;
        lastEnergyPerTick = null;
        isWorking = false;
        setChanged();
    }

    private void addItemToSlot(int slot, ItemStack stack) {
        ItemStack current = itemHandler.getStackInSlot(slot);
        if (current.isEmpty()) {
            itemHandler.setStackInSlot(slot, stack);
        } else if (ItemStack.isSameItemSameComponents(current, stack)) {
            current.grow(stack.getCount());
        }
    }

    private boolean canAddItem(int slot, ItemStack stack) {
        ItemStack current = itemHandler.getStackInSlot(slot);
        if (current.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(current, stack)) return false;
        return current.getCount() + stack.getCount() <= current.getMaxStackSize();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (currentRecipeEU != null) {
            tag.putLong("currentRecipeEU", currentRecipeEU);
        }
        tag.putLong("consumedEU", consumedEU);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("currentRecipeEU")) {
            currentRecipeEU = tag.getLong("currentRecipeEU");
        }
        if (tag.contains("consumedEU")) {
            consumedEU = tag.getLong("consumedEU");
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.molecular_transformer");
    }

    public ContainerData getContainerData() {
        return dataAccess;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.MolecularTransformerMenu(
            containerId, playerInventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_molecular_transformer blockEntity) {
        if (level.isClientSide()) return;
        mio_icif_producer.tick(level, pos, state, blockEntity);
    }
}
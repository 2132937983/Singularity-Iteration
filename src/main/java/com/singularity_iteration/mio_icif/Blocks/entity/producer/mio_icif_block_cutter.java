package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.recipe.block_cutter.mio_icif_BlockCutterRecipe;
import com.singularity_iteration.mio_icif.recipe.block_cutter.mio_icif_BlockCutterRecipeInput;
import com.singularity_iteration.mio_icif.recipe.block_cutter.mio_icif_BlockCutterRecipes;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("null")
public class mio_icif_block_cutter extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .extra(1)
        .output(1)
        .battery()
        .upgrade(4)
        .build();

    // 槽位定义（对应 IC2 block_cutter.xml 布局）
    public static final int TOTAL_SLOTS = 8;
    public static final int INPUT_SLOT = 0;        // 输入槽(26, 16)
    public static final int BLADE_SLOT = 1;        // 刀片槽(70, 34)
    public static final int OUTPUT_SLOT = 2;       // 输出槽(111, 34)
    public static final int BATTERY_SLOT = 3;      // 电池槽(26, 52)
    public static final int UPGRADE_SLOT_1 = 4;    // 升级槽1 (151, 7)
    public static final int UPGRADE_SLOT_2 = 5;    // 升级槽2 (151, 25)
    public static final int UPGRADE_SLOT_3 = 6;    // 升级槽3 (151, 43)
    public static final int UPGRADE_SLOT_4 = 7;    // 升级槽4 (151, 61)

    // 能量参数
    public static final long ENERGY_CAPACITY = 43200;
    public static final long MAX_RECEIVE = 128; // MV 级
    public static final long ENERGY_PER_TICK = 48;
    public static final int MAX_PROGRESS = 900;

    public mio_icif_block_cutter(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.BLOCK_CUTTER_ENTITY_TYPE.get());
    }

    public mio_icif_block_cutter(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, ENERGY_CAPACITY, MAX_RECEIVE, 0, MAX_PROGRESS, LAYOUT, ENERGY_PER_TICK, CableTier.MV);
    }



    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> isCuttable(stack);
        case BLADE_SLOT -> isValidBlade(stack);
            case OUTPUT_SLOT -> false;
            case BATTERY_SLOT -> isBattery(stack);
            case UPGRADE_SLOT_1, UPGRADE_SLOT_2, UPGRADE_SLOT_3, UPGRADE_SLOT_4 -> stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Upgrade.mio_icif_upgrade;
            default -> true;
        };
    }

    private boolean isValidBlade(ItemStack stack) {
        return stack.is(mio_icif_resources.IRON_CUT_BLADE.get())
            || stack.is(mio_icif_resources.DIAMOND_CUT_BLADE.get());
    }

    /**
     * 判断物品是否可以被切割
     */
    private boolean isCuttable(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (level == null) return true;

        ItemStack checkStack = stack.copyWithCount(64);
        mio_icif_BlockCutterRecipeInput recipeInput = new mio_icif_BlockCutterRecipeInput(checkStack);
        return level.getRecipeManager()
            .getRecipeFor(mio_icif_BlockCutterRecipes.BLOCK_CUTTER_TYPE.get(), recipeInput, level).isPresent();
    }

    /**
     * 获取锯片的硬度上限
     */
    private int getBladeHardness(ItemStack stack) {
        if (stack.is(mio_icif_resources.IRON_CUT_BLADE.get())) return 2;
        if (stack.is(mio_icif_resources.DIAMOND_CUT_BLADE.get())) return 8;
        return 0;
    }

    /**
     * 从配方管理器中查找当前输入对应的配方
     */
    @Nullable
    private mio_icif_BlockCutterRecipe getCurrentRecipe() {
        if (this.level == null) return null;
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return null;

        RecipeManager recipeManager = this.level.getRecipeManager();
        mio_icif_BlockCutterRecipeInput recipeInput = new mio_icif_BlockCutterRecipeInput(input);
        Optional<RecipeHolder<mio_icif_BlockCutterRecipe>> recipe = recipeManager
            .getRecipeFor(mio_icif_BlockCutterRecipes.BLOCK_CUTTER_TYPE.get(), recipeInput, this.level);
        return recipe.map(RecipeHolder::value).orElse(null);
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return new int[]{INPUT_SLOT, BLADE_SLOT, OUTPUT_SLOT, BATTERY_SLOT};
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
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        if (!isItemValidForSlot(slot, stack)) return false;
        if (slot == BATTERY_SLOT) return isBattery(stack);
        if (slot == OUTPUT_SLOT) return false;
        if (isUpgradeSlot(slot)) return false; // 升级槽不允许自动插入（与IC2原版一致）
        return true;
    }

    @Override
    protected void doWork() {
        if (!consumeEnergy()) {
            stopWork();
            return;
        }

        isWorking = true;

        if (progress >= maxProgress) {
            finishCutting();
        }
    }

    /**
     * 判断当前输入是否有有效的配方
     * 用于判断当前物品是否更该设置进度
     */
    @Override
    protected boolean hasValidRecipe() {
        return getCurrentRecipe() != null;
    }

    @Override
    protected boolean canWork() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        ItemStack blade = itemHandler.getStackInSlot(BLADE_SLOT);
        if (input.isEmpty() || blade.isEmpty()) return false;

        mio_icif_BlockCutterRecipe recipe = getCurrentRecipe();
        if (recipe == null) return false;
        if (!isValidBlade(blade)) return false;
        if (getBladeHardness(blade) < recipe.getHardness()) return false;
        if (input.getCount() < recipe.getIngredientCount()) return false;

        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);
        ItemStack result = recipe.getResult();
        if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, result) || output.getCount() + result.getCount() > output.getMaxStackSize())) {
            return false;
        }

        return energyStorage.getAmount() >= ENERGY_PER_TICK;
    }

    /**
     * 完成一次切割
     */
    private void finishCutting() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return;

        mio_icif_BlockCutterRecipe recipe = getCurrentRecipe();
        if (recipe == null) return;

        input.shrink(recipe.getIngredientCount());

        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);
        ItemStack result = recipe.getResult();
        if (output.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, result.copy());
        } else {
            output.grow(result.getCount());
        }

        finishWork();

        if (canWork()) {
            isWorking = true;
        }

        setChanged();
    }

    /**
     * �?tick ?��?��??��??
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_block_cutter blockEntity) {
        if (level.isClientSide()) return;
        // 调用父类 tick
        mio_icif_producer.tick(level, pos, state, blockEntity);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.block_cutter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.BlockCutterMenu(id, playerInventory, this);
    }
}
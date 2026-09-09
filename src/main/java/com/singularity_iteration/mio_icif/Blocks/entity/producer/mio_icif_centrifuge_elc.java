package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.recipe.centrifuge.mio_icif_CentrifugeRecipe;
import com.singularity_iteration.mio_icif.recipe.centrifuge.mio_icif_CentrifugeRecipeInput;
import com.singularity_iteration.mio_icif.recipe.centrifuge.mio_icif_CentrifugeRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * ??��?�离�??��?��??��?��?�类
 * 使用?��??�产??��?��?�来??�工??��??
 * 槽位?��?��??�?输�?? + 3输出 + 1?���? + 4??�件 = 9�?
 * 
 * ?��??��??
 * - 5000 HU??��?��?��??
 * - 1EU产�??1HU
 * - 达�??5000HU??��??始�?��??
 * - 40EU/t??��?��??500tick??�工?��?��
 * - MV?��??��?�级
 */
@SuppressWarnings("null")
public class mio_icif_centrifuge_elc extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .output(3)
        .battery()
        .upgrade(4)
        .build();

    // 槽位?�数??��??个槽位?
    public static final int SLOT_COUNT = 9;
    // 输�?�槽位?�???
    public static final int INPUT_SLOT = 0;
    // 输出槽起始索引�??3个�?�出槽位??
    public static final int OUTPUT_SLOT_1 = 1;
    public static final int OUTPUT_SLOT_2 = 2;
    public static final int OUTPUT_SLOT_3 = 3;
    // ?��池槽位?�???
    public static final int BATTERY_SLOT = 4;
    // ??�件槽起始索引�??4个�?�件槽位??
    public static final int UPGRADE_SLOT_START = 5;

    // 默�????�置
    // 默�????�置�?对�??IC2 ??��??�?
    public static final long DEFAULT_CAPACITY = 24000L;  // 48 EU/t ?? 500 ticks = 24000 EU
    public static final long DEFAULT_MAX_RECEIVE = 128L; // MV级�?��??
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 500; // 25�?= 500tick
    public static final long DEFAULT_ENERGY_PER_TICK = 48L; // 48EU/t�?对�?�IC2??��??�?

    // ??��?�系统）?�置
    public static final int MAX_HEAT = 5000; // ???大�?��??000HU
    public static final int HEAT_PER_EU = 1; // 1EU产�??1HU
    public static final int HEAT_GENERATION_RATE = 4; // 每tick产�?��?��?��?��??
    public static final int HEAT_COOLING_RATE = 2; // 每tick?��??��?�却??��??
    public static final int MIN_HEAT_FOR_WORK = 5000; // �?始工作�????????小�?��??

    // ??��?��?��??
    protected int heatStorage;

    /**
     * ?���? BlockEntityType.Builder ????????�函???
     */
    public mio_icif_centrifuge_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.CENTRIFUGE_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_centrifuge_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.MV);

        this.heatStorage = 0;
    }



    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.centrifuge_elc");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) {
            // 输�?�槽：�???��?��?��?��?��??�工?????��??
            return isProcessable(stack);
        } else if (slot >= OUTPUT_SLOT_1 && slot <= OUTPUT_SLOT_3) {
            // 输出槽位?��?��??许�?�动?��??��?��??
            return false;
        } else if (slot == BATTERY_SLOT) {
            // ?��池槽：接??��?��?��?��?��?��??�??��池�??
            return isBattery(stack);
        } else if (slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + 4) {
            // ??�件槽位?�只?��??��??级�?��??
            return getItemAPI().isUpgrade(stack);
        }
        return false;
    }

    /**
     * �??��??��???��?��?��??�工
     * ??��????�方系�??�????
     */
    private boolean isProcessable(ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return false;
        }
        // ??�建??�方输�??
        mio_icif_CentrifugeRecipeInput recipeInput = new mio_icif_CentrifugeRecipeInput(stack);
        // �??��?��?��??�匹??��????��??
        Optional<RecipeHolder<mio_icif_CentrifugeRecipe>> recipe = level.getRecipeManager()
            .getRecipeFor(mio_icif_CentrifugeRecipes.CENTRIFUGE_TYPE.get(), recipeInput, level);
        return recipe.isPresent();
    }

    /**
     * ?��??�匹??��????��?�离�??��??�方
     */
    private Optional<mio_icif_CentrifugeRecipe> getRecipe(ItemStack input) {
        if (level == null || input.isEmpty()) {
            return Optional.empty();
        }
        mio_icif_CentrifugeRecipeInput recipeInput = new mio_icif_CentrifugeRecipeInput(input);
        return level.getRecipeManager()
            .getRecipeFor(mio_icif_CentrifugeRecipes.CENTRIFUGE_TYPE.get(), recipeInput, level)
            .map(RecipeHolder::value);
    }



    /**
     * �???��?��?????漏�?�槽位方法?
     * ?????�面??�可以�???��?????�槽位?
     */
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // ?????�面??�可以�???��?????�槽位?
        return new int[]{INPUT_SLOT, BATTERY_SLOT, OUTPUT_SLOT_1, OUTPUT_SLOT_2, OUTPUT_SLOT_3};
    }

    @Override
    protected int[] getInputSlots() {
        return new int[]{INPUT_SLOT};
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[]{OUTPUT_SLOT_1, OUTPUT_SLOT_2, OUTPUT_SLOT_3};
    }

    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    /**
     * �???��?��???????��?��???��?���?
     */
    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        // 输出槽位?��?��?��??
        if (slot >= OUTPUT_SLOT_1 && slot <= OUTPUT_SLOT_3) {
            return false;
        }

        // ?��池槽：只?��??�电池类??��??
        if (slot == BATTERY_SLOT) {
            return isBattery(stack);
        }

        // 输�?�槽：接??�可??�工?????��??
        if (slot == INPUT_SLOT) {
            return isProcessable(stack);
        }

        // ??��?�槽位置????�件槽位?��?��?��???��不�??许放???
        return false;
    }

    /**
     * �???��?��???????��?��???��?���?
     * ?????�方??��?�可以�?��??3个�?�出�?
     */
    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        // 3个�?�出槽可以�?�任何方??��?��??
        return slot >= OUTPUT_SLOT_1 && slot <= OUTPUT_SLOT_3;
    }

    /**
     * �??��?��?��应�????�置进�??
     * ?��?��输�?�槽为空?��??�置
     */
    @Override
    protected boolean shouldResetProgress() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        // 输�?�槽为空，�?�置进度
        if (input.isEmpty()) {
            return true;
        }
        // ??�方不匹??��?��?�置进度
        return !isProcessable(input);
    }

    @Override
    protected boolean canWork() {
        // �??��?��?��??��?��?��?��??
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }

        // �??��?��?��?��?��??�工??��??
        if (!isProcessable(input)) {
            return false;
        }

        // �??��??��?�是?��达�?�工作�??�?
        if (heatStorage < MIN_HEAT_FOR_WORK) {
            return false;
        }

        // �??��?��?��??�足够�?��??
        if (!hasEnoughEnergy()) {
            return false;
        }

        // �??��?��?��??�足够�??输出槽空?��容纳?????��?��??
        List<ItemStack> results = getProcessingResults(input);
        if (results.isEmpty()) {
            return false;
        }
        
        // �??��每个输出?��?��??�足够空?��
        for (int i = 0; i < results.size() && i < 3; i++) {
            ItemStack result = results.get(i);
            int outputSlot = OUTPUT_SLOT_1 + i;
            ItemStack currentOutput = itemHandler.getStackInSlot(outputSlot);
            
            if (result.isEmpty()) {
                continue; // 空气?��??不�??�?空气??
            }
            
            if (currentOutput.isEmpty()) {
                continue; // 空槽?��以�?��??
            }
            
            if (ItemStack.isSameItem(currentOutput, result) && 
                ItemStack.isSameItemSameComponents(currentOutput, result)) {
                int newCount = currentOutput.getCount() + result.getCount();
                if (newCount > currentOutput.getMaxStackSize()) {
                    return false; // 空间不足
                }
            } else {
                return false; // 槽位?�已被�?��?��?��????�用
            }
        }
        return true;
    }

    /**
     * ?��??��?�工结果?��???????��?�出�?
     * @param input 输�?��?��??
     * @return ?????��?�出??��???????�表
     */
    private List<ItemStack> getProcessingResults(ItemStack input) {
        Optional<mio_icif_CentrifugeRecipe> recipe = getRecipe(input);
        if (recipe.isPresent()) {
            return recipe.get().getAllResults();
        }
        // �???�没??��?�方，�?��?�空??�表
        return java.util.Collections.emptyList();
    }

    @Override
    protected void doWork() {
        // �???��?��??
        if (!consumeEnergy()) {
            stopWork();
            return;
        }

        isWorking = true;

        // �??��?��?��完成??
        if (progress >= maxProgress) {
            finishProcessing();
        }
    }

    /**
     * 完成?��?�工�?多�?�出????���?
     */
    private void finishProcessing() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            stopWork();
            return;
        }

        // ?��??��????��?�出结果??
        List<ItemStack> results = getProcessingResults(input);
        if (results.isEmpty()) {
            stopWork();
            return;
        }

        // 尝�?��???????��?��?�放??��?�应�???输出�?
        boolean allPlaced = true;
        for (int i = 0; i < results.size() && i < 3; i++) {
            ItemStack result = results.get(i);
            int outputSlot = OUTPUT_SLOT_1 + i;
            
            if (!tryPlaceResult(outputSlot, result)) {
                allPlaced = false;
                break;
            }
        }

        if (!allPlaced) {
            stopWork();
            return;
        }

        // �???��?��?��?��??
        input.shrink(1);

        // ??�置进度
        finishWork();

        // �??��?��?��还可以继续?工�??
        if (canWork()) {
            isWorking = true;
        }
    }

    /**
     * 尝�?��??结果?�放??��??定�?�出�?
     * @param slot 输出槽索引?
     * @param result �??��??��????��??
     * @return ?��?��??��???��???
     */
    private boolean tryPlaceResult(int slot, ItemStack result) {
        if (result.isEmpty()) {
            return true; // 空气?��??�?为�?��??
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
     * �?tick ?��?��??��??
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_centrifuge_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // ?��?��??��?�系�?
        blockEntity.updateHeat();

        // �??��??�类???tick ??��?��??�???��?��?��?��??工�?��??????��?��池槽?��?���?
        mio_icif_producer.tick(level, pos, state, blockEntity);

        // ?��?��?��??�状???�?运�??/??��??�?
        boolean isLit = state.getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_centrifuge_elc.LIT);
        if (blockEntity.isWorking() != isLit) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_centrifuge_elc.LIT, blockEntity.isWorking()), 3);
        }
    }

    /**
     * ?��?��??��?�系�?
     * - ??�电且�?��?��?�在?��，�?��?��?��??
     * - ?��??��?��?��?��??
     */
    private void updateHeat() {
        if (!hasEnoughEnergy()) {
            if (heatStorage > 0) {
                heatStorage = Math.max(0, heatStorage - HEAT_COOLING_RATE);
                setChanged();
            }
            return;
        }

        int maxHeatToGenerate = Math.min(HEAT_GENERATION_RATE, MAX_HEAT - heatStorage);
        if (maxHeatToGenerate > 0) {
            long energyAvailable = Math.min(energyStorage.getAmount(), maxHeatToGenerate);
            int heatGenerated = (int) energyAvailable;

            if (heatGenerated > 0) {
                apiUseEnergy(heatGenerated, false);
                heatStorage += heatGenerated;
                setChanged();
            }
        }
    }



    /**
     * ?��??��?��?��?��?��??
     */
    public int getHeatStorage() {
        return heatStorage;
    }

    /**
     * ?��??��??大�?��?��??
     */
    public int getMaxHeat() {
        return MAX_HEAT;
    }

    /**
     * ?��??��?��?�百???比�???��于GUI?��示�??
     */
    public int getHeatProgress() {
        return (heatStorage * 100) / MAX_HEAT;
    }

    /**
     * ?��??��?��?�槽??��??
     */
    public ItemStack getInputItem() {
        return itemHandler.getStackInSlot(INPUT_SLOT);
    }

    /**
     * ?��??��??定�?�出槽位?��??
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
        // 保�?��?��?�数?��
        tag.putInt("heat_storage", heatStorage);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 读�?��?��?�数?��
        if (tag.contains("heat_storage")) {
            heatStorage = tag.getInt("heat_storage");
        }
    }
}
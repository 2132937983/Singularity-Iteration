package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.heat.HeatStorage;
import com.singularity_iteration.mio_icif.energy.heat.IHeatStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 感应炉方块实体类
 * 使用感应生产热量来同时处理两个输入（可以同时冶炼两个物品）
 * 
 * 特点：
 * - 持续耗电产生热量（不输出能量，热量仅用于内部加工）
 * - 温度范围 0-100%，影响加工速度
 * - 两个输入槽 + 两个输出槽 + 一个电池槽 + 两个升级槽
 * - 停电就停止加热，温度自然冷却
 * - 两个输入槽共用一个温度值（同时加工）
 * 
 * 参数：
 * - 起始1%需要约1000EU，达到100%需要约95EU每刻维持
 * - 最低加热时间16EU/t，每次加工约0.609375s（2.1875ticks）
 * - 最低维持耗电约1EU/t
 * - 最大持续耗电约128EU/t（MV）
 */
@SuppressWarnings("null")
public class mio_icif_induction_elc extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(2)
        .battery()
        .output(2)
        .upgrade(2)
        .build();

    // 槽位定义
    public static final int INPUT_SLOT_1 = 0;      // 第一个输入槽
    public static final int INPUT_SLOT_2 = 1;      // 第二个输入槽
    public static final int BATTERY_SLOT = 2;      // 电池槽
    public static final int OUTPUT_SLOT_1 = 3;     // 第一个输出槽
    public static final int OUTPUT_SLOT_2 = 4;     // 第二个输出槽
    public static final int UPGRADE_SLOT_1 = 5;    // 升级槽
    public static final int UPGRADE_SLOT_2 = 6;    // 升级槽
    public static final int TOTAL_SLOTS = 7;       // 总槽位数

    public static final long DEFAULT_CAPACITY = 10000L;
    public static final long DEFAULT_MAX_RECEIVE = 128L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final long DEFAULT_ENERGY_PER_TICK = 15L;
    
    public static final int MAX_HEAT = 10000;
    public static final int PROGRESS_TARGET = 4000;
    public static final int HEAT_PROGRESS_DIVISOR = 30;
    public static final int HEAT_UP_COST = 1;
    public static final int HEAT_COOL_RATE = 4;
    public static final int WORK_ENERGY_COST = 15;
    
    public static final int HEAT_CAPACITY = 10000;
    public static final int HEAT_MAX_RECEIVE = 0;
    public static final int HEAT_MAX_EXTRACT = 0;
    public static final int HEAT_BASE_TEMP = 20;
    public static final int HEAT_MAX_TEMP = 1000;
    public static final float HEAT_LOSS_FACTOR = 0.005f;
    public static final int HEAT_UP_RATE = 1;
    
    // ??��?��?��??
    protected final HeatStorage heatStorage;
    
    private Optional<RecipeHolder<SmeltingRecipe>> currentRecipe1 = Optional.empty();
    private Optional<RecipeHolder<SmeltingRecipe>> currentRecipe2 = Optional.empty();
    
    private int progress = 0;

    /**
     * ?���? BlockEntityType.Builder ????????�函�?
     */
    public mio_icif_induction_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.INDUCTION_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_induction_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            PROGRESS_TARGET,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.MV);
        
        this.heatStorage = new HeatStorage(
            HEAT_CAPACITY,
            HEAT_MAX_RECEIVE,
            HEAT_MAX_EXTRACT,
            HEAT_BASE_TEMP,
            HEAT_MAX_TEMP,
            HEAT_LOSS_FACTOR
        );
    }



    /**
     * ?��??��?��?��?��??
     */
    public HeatStorage getHeatStorage() {
        return heatStorage;
    }

    /**
     * ?��??��?��?��?��?��?��?��???���?capability 系�??�?
     */
    @Nullable
    public IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        return heatStorage;
    }

    /**
     * 判断物品是否可以放入指定槽位
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT_1, INPUT_SLOT_2 -> isSmeltable(stack);
            case BATTERY_SLOT -> isBattery(stack);
            case OUTPUT_SLOT_1, OUTPUT_SLOT_2 -> false;
            case UPGRADE_SLOT_1, UPGRADE_SLOT_2 -> isUpgrade(stack);
            default -> false;
        };
    }

    /**
     * 判断物品是否是升级件
     */
    private boolean isUpgrade(ItemStack stack) {
        return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Upgrade.mio_icif_upgrade;
    }

    /**
     * 判断物品是否可以冶炼
     */
    private boolean isSmeltable(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        SingleRecipeInput input = new SingleRecipeInput(stack);
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level).isPresent();
    }

    /**
     * ?��?��??��?��?�方
     */
    private Optional<RecipeHolder<SmeltingRecipe>> findRecipe(int inputSlot) {
        if (level == null) {
            return Optional.empty();
        }
        ItemStack input = itemHandler.getStackInSlot(inputSlot);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, level);
    }

    /**
     * 指定方向可访问的槽位
     */
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return new int[]{INPUT_SLOT_1, INPUT_SLOT_2, BATTERY_SLOT, OUTPUT_SLOT_1, OUTPUT_SLOT_2};
    }

    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    /**
     * 判断指定槽位是否可以从指定方向提取物品
     */
    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return slot == OUTPUT_SLOT_1 || slot == OUTPUT_SLOT_2;
    }

    /**
     * 判断当前输入是否有有效的冶炼配方
     * 用于判断当前物品是否更该设置进度
     */
    @Override
    protected boolean hasValidRecipe() {
        return canProcessSlot(INPUT_SLOT_1, OUTPUT_SLOT_1) || canProcessSlot(INPUT_SLOT_2, OUTPUT_SLOT_2);
    }

    /**
     * �??��?��?��?��?��?��以工�?
     */
    @Override
    protected boolean canWork() {
        return canProcessSlot(INPUT_SLOT_1, OUTPUT_SLOT_1) || canProcessSlot(INPUT_SLOT_2, OUTPUT_SLOT_2);
    }

    /**
     * 判断指定槽位是否可以加工
     */
    private boolean canProcessSlot(int inputSlot, int outputSlot) {
        ItemStack input = itemHandler.getStackInSlot(inputSlot);
        if (input.isEmpty()) {
            return false;
        }

        Optional<RecipeHolder<SmeltingRecipe>> recipe = findRecipe(inputSlot);
        if (recipe.isEmpty()) {
            return false;
        }

        ItemStack result = recipe.get().value().getResultItem(level.registryAccess());
        ItemStack currentOutput = itemHandler.getStackInSlot(outputSlot);

        if (currentOutput.isEmpty()) {
            return true;
        }

        if (!ItemStack.isSameItem(currentOutput, result)) {
            return false;
        }

        int newCount = currentOutput.getCount() + result.getCount();
        return newCount <= currentOutput.getMaxStackSize();
    }

    /**
     * 执行生产工作 - 两个槽位共用进度值（同时加工）
     */
    @Override
    protected void doWork() {
        currentRecipe1 = findRecipe(INPUT_SLOT_1);
        currentRecipe2 = findRecipe(INPUT_SLOT_2);
        
        boolean canWork1 = canProcessSlot(INPUT_SLOT_1, OUTPUT_SLOT_1);
        boolean canWork2 = canProcessSlot(INPUT_SLOT_2, OUTPUT_SLOT_2);
        boolean canOperate = canWork1 || canWork2;
        
        if (progress >= PROGRESS_TARGET) {
            finishSmeltingBoth();
            progress = 0;
            isWorking = false;
            return;
        }
        
        boolean hasRedstone = level != null && level.hasNeighborSignal(worldPosition);
        
        if ((canOperate || hasRedstone) && energyStorage.getAmount() >= HEAT_UP_COST) {
            apiUseEnergy(HEAT_UP_COST, false);
            if (heatStorage.getHeatStored() < MAX_HEAT) {
                heatStorage.generateHeatInternal(HEAT_UP_RATE, false);
            }
            isWorking = true;
        } else {
            long currentHeat = heatStorage.getHeatStored();
            if (currentHeat > 0) {
                heatStorage.setHeat(currentHeat - Math.min(currentHeat, HEAT_COOL_RATE));
            }
        }
        
        if (!isWorking || progress == 0) {
            if (canOperate) {
                if (energyStorage.getAmount() >= WORK_ENERGY_COST) {
                    isWorking = true;
                }
            } else {
                progress = 0;
            }
        } else if (!canOperate || energyStorage.getAmount() < WORK_ENERGY_COST) {
            if (!canOperate) {
                progress = 0;
            }
            isWorking = false;
        }
        
        if (isWorking && canOperate) {
            int heatValue = (int) heatStorage.getHeatStored();
            progress += heatValue / HEAT_PROGRESS_DIVISOR;
            apiUseEnergy(WORK_ENERGY_COST, false);
        }
    }

    /**
     * 完成冶炼 - 同一时间处理两个槽位
     */
    private void finishSmeltingBoth() {
        if (canProcessSlot(INPUT_SLOT_1, OUTPUT_SLOT_1) && currentRecipe1.isPresent()) {
            finishSmelting(INPUT_SLOT_1, OUTPUT_SLOT_1, currentRecipe1.get());
        }
        
        if (canProcessSlot(INPUT_SLOT_2, OUTPUT_SLOT_2) && currentRecipe2.isPresent()) {
            finishSmelting(INPUT_SLOT_2, OUTPUT_SLOT_2, currentRecipe2.get());
        }
    }

    /**
     * 完成单个槽位的冶炼
     */
    private void finishSmelting(int inputSlot, int outputSlot, RecipeHolder<SmeltingRecipe> recipe) {
        ItemStack result = recipe.value().getResultItem(level.registryAccess());
        ItemStack currentOutput = itemHandler.getStackInSlot(outputSlot);

        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(outputSlot, result.copy());
        } else {
            currentOutput.grow(result.getCount());
        }

        ItemStack input = itemHandler.getStackInSlot(inputSlot);
        input.shrink(1);
    }

    public int getHeatPercent() {
        return (int) ((heatStorage.getHeatStored() * 100) / heatStorage.getMaxHeatStored());
    }

    public int gaugeProgressScaled(int i) {
        return i * progress / PROGRESS_TARGET;
    }

    /**
     * �?tick ?��?��??��??
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_induction_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        mio_icif_producer.tick(level, pos, state, blockEntity);

        boolean isLit = state.getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_induction_elc.LIT);
        if (blockEntity.isWorking() != isLit) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_induction_elc.LIT, blockEntity.isWorking()), 3);
        }
    }

    @Override
    protected boolean canWorkRedstone() {
        if (level == null) {
            return true;
        }
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (upgradeStats.isRedstoneInverted()) {
            return powered;
        }
        return !powered;
    }

    @Override
    protected void stopWork() {
        isWorking = false;
    }

    @Override
    protected void finishWork() {
    }

    @Override
    protected void updateProgress() {
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("heat", heatStorage.getHeatStored());
        tag.putInt("progress", progress);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("heat")) {
            heatStorage.setHeat(tag.getInt("heat"));
        }
        if (tag.contains("heat_percent")) {
            int oldHeatPercent = tag.getInt("heat_percent");
            heatStorage.setHeat(oldHeatPercent * HEAT_CAPACITY / 100);
        }
        if (tag.contains("progress")) {
            progress = tag.getInt("progress");
        }
        if (tag.contains("progress1")) {
            progress = tag.getInt("progress1");
        }
    }

    // ==================== Getter ?���? ====================

    public int getProgress1() {
        return progress;
    }

    public int getProgress2() {
        return progress;
    }

    public int getMaxProgress1() {
        return PROGRESS_TARGET;
    }

    public int getMaxProgress2() {
        return PROGRESS_TARGET;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return PROGRESS_TARGET;
    }

    public long getCurrentEnergyCost() {
        return WORK_ENERGY_COST;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.induction_elc");
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.InductionElcMenu(
            containerId, playerInventory, this);
    }
}
package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots;
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

@SuppressWarnings("null")
public class mio_icif_recycler_elc extends AbstractProcessingMachineBlockEntity {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .battery()
        .output(1)
        .upgrade(4)
        .build();

    public static final int SLOT_COUNT = 7;
    public static final int INPUT_SLOT = 0;
    public static final int BATTERY_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int UPGRADE_SLOT_1 = 3;
    public static final int UPGRADE_SLOT_2 = 4;
    public static final int UPGRADE_SLOT_3 = 5;
    public static final int UPGRADE_SLOT_4 = 6;

// 默认配置（对应 IC2 原版）
    public static final long DEFAULT_CAPACITY = 45L;     // 1 EU/t × 45 ticks = 45 EU
    public static final long DEFAULT_MAX_RECEIVE = 32L;  // LV级最大输入
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 45; // 2.25秒（45 ticks）
    public static final long DEFAULT_ENERGY_PER_TICK = 1L; // 每tick耗电（1 EU），对应 IC2 原版

    private static final double RECYCLE_CHANCE = 0.125;
    private int scexRecycleOutcome = -1;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> (int) energyStorage.getAmount();
                case 3 -> (int) energyStorage.getCapacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() { return 4; }
    };

    public mio_icif_recycler_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.RECYCLER_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_recycler_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.LV);
    }

    public mio_icif_recycler_elc(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                  long capacity, long maxReceive, long maxExtract,
                                  int workTime, long energyPerTick) {
        super(pos, state, type, capacity, maxReceive, maxExtract, workTime, LAYOUT, energyPerTick, CableTier.LV);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> true;
            case BATTERY_SLOT -> isBattery(stack);
            case OUTPUT_SLOT -> false;
            case UPGRADE_SLOT_1, UPGRADE_SLOT_2, UPGRADE_SLOT_3, UPGRADE_SLOT_4 -> stack.getItem() instanceof com.miophas.singularity_iteration.common.item.upgrade.mio_icif_upgrade;
            default -> false;
        };
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

    /**
     * 判断当前输入是否可以被回收：任何非空物品都可以被回收
     * 用于判断当前物品是否更该设置进度
     */
    @Override
    protected boolean hasValidRecipe() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        return !input.isEmpty();
    }

    @Override
    protected boolean canWork() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }

        if (getEffectiveEnergyPerTick() <= 0 || progress < maxProgress && !hasEnoughEnergy()) {
            return false;
        }

        if (!hasOutputSpace()) {
            return false;
        }

        return true;
    }

    private boolean hasOutputSpace() {
        return RecipeSlots.prepare(itemHandler, INPUT_SLOT, 1, new int[]{OUTPUT_SLOT},
            java.util.List.of(new ItemStack(mio_icif_normal.SCRAP.get()))).isPresent();
    }

    @Override
    protected void doWork() {
        if (!canWork()) { stopWork(); return; }
        if (progress < maxProgress) {
            if (!consumeEnergy()) { stopWork(); return; }
            progress = (int) Math.min(maxProgress, (long) progress + getProgressPerTick());
        }
        isWorking = true;
        if (progress >= maxProgress) finishRecycling();
    }

    @Override
    protected void updateProgress() { /* doWork owns the paid increment. */ }

    private void finishRecycling() {
        if (level == null || !hasOutputSpace()) { stopWork(); return; }
        if (scexRecycleOutcome < 0) scexRecycleOutcome = level.random.nextDouble() < RECYCLE_CHANCE ? 1 : 0;
        var operation = scexRecycleOutcome == 1 ? RecipeSlots.prepare(itemHandler, INPUT_SLOT, 1,
            new int[]{OUTPUT_SLOT}, java.util.List.of(new ItemStack(mio_icif_normal.SCRAP.get())))
            : RecipeSlots.consumeOnly(itemHandler, INPUT_SLOT, 1);
        if (operation.isEmpty()) { stopWork(); return; }
        int completed = progress, outcome = scexRecycleOutcome;
        progress = 0; scexRecycleOutcome = -1;
        if (!operation.get().commit()) { progress = completed; scexRecycleOutcome = outcome; stopWork(); return; }
        finishWork();
        setChanged();
    }



    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.recycler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.producer.RecyclerElcMenu(containerId, playerInventory, this);
    }

    public ContainerData getContainerData() { return containerData; }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", itemHandler.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("scex_recycle_outcome", scexRecycleOutcome);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (!tag.contains("inventory") && tag.contains("Items")) itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        if (!tag.contains("progress") && tag.contains("Progress")) progress = tag.getInt("Progress");
        progress = Math.max(0, Math.min(maxProgress, progress));
        int outcome = tag.contains("scex_recycle_outcome") ? tag.getInt("scex_recycle_outcome") : -1;
        scexRecycleOutcome = outcome >= 0 && outcome <= 1 ? outcome : -1;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_recycler_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        AbstractProcessingMachineBlockEntity.tick(level, pos, state, blockEntity);
        blockEntity.setChanged();

        // 更新方块状态（运行/停止）
        boolean isLit = state.getValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_recycler_elc.LIT);
        if (blockEntity.isWorking() != isLit) {
            level.setBlock(pos, state.setValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_recycler_elc.LIT, blockEntity.isWorking()), 3);
        }
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public boolean isWorking() {
        return isWorking;
    }
}
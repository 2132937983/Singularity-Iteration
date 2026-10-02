package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** IC2 Experimental agriculture machine; cursor and resource tanks survive reloads. */
@SuppressWarnings("null")
public class mio_icif_harvest_elc extends AbstractProcessingMachineBlockEntity implements com.miophas.singularity_iteration.common.area.WorkAreaProvider {
    private int scanIndex;

    // 对齐 1.7.10 TileEntityCropHavester：15 格收获存储 + 1 格作物分析仪 + 1 格升级，
    // 外加本项目有意保留的电池槽 1 格。
    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .output(15)
        .extra(1)
        .battery()
        .upgrade(1)
        .build();
    public static final int SLOT_STORAGE_START = 0;
    public static final int SLOT_STORAGE_COUNT = 15;
    public static final int SLOT_CROPNALYZER = 15;
    public static final int SLOT_BATTERY = 16;
    public static final int SLOT_UPGRADE = 17;
    public static final int TOTAL_SLOTS = 18;
    public static final long DEFAULT_CAPACITY = 10000L;
    public static final long DEFAULT_MAX_RECEIVE = 32L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 10;
    public static final long DEFAULT_ENERGY_PER_TICK = 1L;
    // 对齐 IC2 2.8：每次扫描 1 EU，每个返回的物品堆 20 EU；需 21 EU 才能工作。
    public static final long ENERGY_PER_HARVEST = 20L;
    // SI 扩展：装有作物分析仪时每个物品堆额外消耗（原版 2.8 收割机无分析仪槽）。
    public static final long ENERGY_PER_ANALYZER = 100L;
    // 1（扫描）+ 20（一个物品堆）
    public static final long MIN_START_ENERGY = 21L;
    public static final int SCAN_RADIUS = com.miophas.singularity_iteration.core.runtime.crop.CropScanCursor.RADIUS;
    public static final int SCAN_VOLUME = com.miophas.singularity_iteration.core.runtime.crop.CropScanCursor.VOLUME;

    public mio_icif_harvest_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.HARVEST_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_harvest_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
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
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.harvest");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> (int) energyStorage.getAmount();
                    case 1 -> (int) energyStorage.getCapacity();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 2;
            }
        };

        return new com.miophas.singularity_iteration.common.menu.producer.HarvestElcMenu(
            containerId, playerInventory, this, data);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= SLOT_STORAGE_START && slot < SLOT_STORAGE_START + SLOT_STORAGE_COUNT) {
            // 对齐原版 contentSlot (InvSlot.Access.IO)：存储槽接受任意物品
            return true;
        }
        if (slot == SLOT_CROPNALYZER) {
            return isCropAnalyzer(stack);
        }
        if (slot == SLOT_BATTERY) {
            return isBattery(stack);
        }
        if (slot >= SLOT_UPGRADE && slot < TOTAL_SLOTS) {
            return isSuitableUpgrade(stack);
        }
        return false;
    }

    private boolean isCropAnalyzer(ItemStack stack) {
        return stack.getItem() instanceof com.miophas.singularity_iteration.common.item.tools.CropAnalyzerItem;
    }

    private boolean hasCropAnalyzer() {
        return isCropAnalyzer(itemHandler.getStackInSlot(SLOT_CROPNALYZER));
    }

    /** 对齐原版 InvSlotUpgrade.accepts：升级插件必须声明适用于本机属性。 */
    private boolean isSuitableUpgrade(ItemStack stack) {
        return stack.getItem() instanceof com.miophas.singularity_iteration.common.item.upgrade.mio_icif_upgrade upgrade
            && upgrade.isSuitableFor(stack, getUpgradableProperties());
    }

    /** 对齐 1.7.10 TileEntityCropHavester：仅 ItemProducing。 */
    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.ITEM_PRODUCING);
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        int[] slots = new int[SLOT_STORAGE_COUNT + 1];
        for (int i = 0; i < SLOT_STORAGE_COUNT; i++) slots[i] = SLOT_STORAGE_START + i;
        slots[SLOT_STORAGE_COUNT] = SLOT_CROPNALYZER;
        return slots;
    }

    @Override
    protected int getBatterySlot() {
        return SLOT_BATTERY;
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return slot >= SLOT_STORAGE_START && slot < SLOT_STORAGE_START + SLOT_STORAGE_COUNT;
    }

    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot >= SLOT_STORAGE_START && slot < SLOT_STORAGE_START + SLOT_STORAGE_COUNT) {
            // 对齐原版 contentSlot (InvSlot.Access.IO)：自动化可插入存储槽
            return true;
        }
        if (slot == SLOT_CROPNALYZER) {
            // 对齐原版 InvSlotConsumableId(InvSide.TOP)：分析仪仅可从顶面自动化输入
            return side == Direction.UP && isCropAnalyzer(stack);
        }
        return super.canInsertItem(slot, stack, side);
    }

    @Override
    protected boolean canWork() {
        return energyStorage.getAmount() >= DEFAULT_ENERGY_PER_TICK;
    }

    @Override
    protected void doWork() {
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_harvest_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        AbstractProcessingMachineBlockEntity.tick(level, pos, state, blockEntity);
        // 每 tick 扫描一次，门槛见 MIN_START_ENERGY
        if (blockEntity.energyStorage.getAmount() < MIN_START_ENERGY) {
            blockEntity.updateWorkingState(level, pos, false);
            return;
        }
        blockEntity.apiUseEnergy(DEFAULT_ENERGY_PER_TICK, false);
        boolean harvested = blockEntity.scanAndHarvest(level, pos);

        blockEntity.updateWorkingState(level, pos, harvested);
    }

    private boolean scanAndHarvest(Level level, BlockPos center) {
        scanIndex = com.miophas.singularity_iteration.core.runtime.crop.CropScanCursor.next(scanIndex);
        setChanged();
        // 水平 ±4（9 格）、垂直 -1..1（3 层），x→z→y 推进，与 IC2 2.8 一致（见 CropScanCursor）
        BlockPos target = com.miophas.singularity_iteration.core.runtime.crop.CropScanCursor.target(center, scanIndex);
        return level.hasChunkAt(target) && tryHarvestCropStick(level, target);
    }

    private boolean tryHarvestCropStick(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof com.miophas.singularity_iteration.core.api.crop.IPlanter cropEntity)) {
            return false;
        }
        if (cropEntity.getPlant() == null) {
            return false;
        }
        // 对齐 1.7.10 harvest_automated：canBeHarvested 为前置条件（杂草天然不满足）
        if (!cropEntity.getPlant().canBeHarvested(cropEntity)) {
            return false;
        }
        // IC2 2.8：在最佳收获尺寸收割，否则等到最大尺寸再收割。
        // SI 扩展：装有作物分析仪时只在最佳收获尺寸收割（并额外计费）。
        boolean optimal = hasCropAnalyzer();
        var plant = cropEntity.getPlant();
        int stage = cropEntity.getGrowthStage();
        boolean atOptimal = stage == plant.getOptimalHarvestStage(cropEntity);
        if (!atOptimal && (optimal || stage != plant.getMaxGrowthStage())) {
            return false;
        }
        if (energyStorage.getAmount() < ENERGY_PER_HARVEST) {
            return false;
        }
        if (isStorageFull()) {
            return false;
        }
        List<ItemStack> drops = cropEntity.doHarvest();
        if (drops == null || drops.isEmpty()) {
            return false;
        }
        for (ItemStack drop : drops) {
            apiUseEnergy(ENERGY_PER_HARVEST, false);
            if (optimal) {
                apiUseEnergy(ENERGY_PER_ANALYZER, false);
            }
            boolean added = addItemToStorage(drop);
            if (!added) {
                dropItem(level, drop);
            }
        }
        return true;
    }

    private boolean isStorageFull() {
        for (int i = SLOT_STORAGE_START; i < SLOT_STORAGE_START + SLOT_STORAGE_COUNT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    private boolean addItemToStorage(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        for (int i = SLOT_STORAGE_START; i < SLOT_STORAGE_START + SLOT_STORAGE_COUNT; i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, stack.getCount());
                    ItemStack newStack = existing.copy();
                    newStack.grow(toAdd);
                    itemHandler.setStackInSlot(i, newStack);
                    stack.shrink(toAdd);
                    if (stack.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        for (int i = SLOT_STORAGE_START; i < SLOT_STORAGE_START + SLOT_STORAGE_COUNT; i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            if (existing.isEmpty()) {
                itemHandler.setStackInSlot(i, stack.copy());
                return true;
            }
        }

        return false;
    }

    private void dropItem(Level level, ItemStack stack) {
        if (stack.isEmpty()) return;
        net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
            level, getBlockPos().getX() + 0.5, getBlockPos().getY() + 1.0, getBlockPos().getZ() + 0.5, stack);
        level.addFreshEntity(itemEntity);
    }

    private void updateWorkingState(Level level, BlockPos pos, boolean working) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof com.miophas.singularity_iteration.common.block.producer.mio_icif_block_harvest) {
            boolean currentWorking = state.getValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_harvest.WORKING);
            if (currentWorking != working) {
                level.setBlock(pos, state.setValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_harvest.WORKING, working), 3);
            }
        }
    }

    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CropScanIndex", scanIndex);
    }
    @Override public void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        scanIndex = com.miophas.singularity_iteration.core.runtime.crop.CropScanCursor.restore(tag.getInt("CropScanIndex"));
    }

    @Override
    public java.util.List<com.miophas.singularity_iteration.common.area.WorkArea> workAreas() {
        int r = com.miophas.singularity_iteration.core.runtime.crop.CropScanCursor.RADIUS;
        return java.util.List.of(com.miophas.singularity_iteration.common.area.WorkArea.box(worldPosition, r, 1, 1, r, com.miophas.singularity_iteration.common.area.WorkArea.FARM));
    }
}

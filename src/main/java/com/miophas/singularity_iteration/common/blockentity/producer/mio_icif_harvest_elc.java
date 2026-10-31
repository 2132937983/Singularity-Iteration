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

/**
 * <p><b>EN:</b> Crop Harvester (IC2 Experimental). It scans one position of the 9 x 3 x 9 area
 * each tick ({@link com.miophas.singularity_iteration.core.runtime.crop.CropScanCursor}) and
 * harvests a ripe crop. The crop stays planted and regrows from its after-harvest stage.
 * <ul>
 *   <li>0.1.8.1: no item ever goes to the world. Drops that do not fit go to a saved pending
 *       list. While the list is not empty, the machine does not scan and uses no energy. It
 *       moves the pending items into storage as space becomes free.</li>
 *   <li>0.1.8.1: the WORKING state stays on for {@link #WORKING_HOLD_TICKS} after the last
 *       harvest. The block state, the lamp and the loop sound thus do not flicker each tick.</li>
 * </ul>
 * <p><b>ZH:</b> 作物收割机（IC2 Experimental）。每 tick 扫描 9 × 3 × 9 区域中的一个位置并收割成熟作物。
 * 作物保留在作物架上，从收获后阶段重新生长。
 * <ul>
 *   <li>0.1.8.1：物品绝不掉落到世界中。放不下的掉落物进入已保存的待存列表。列表不为空时机器不扫描、不耗电，
 *       有空位时再把待存物品移入存储。</li>
 *   <li>0.1.8.1：最后一次收割后 WORKING 状态保持 {@link #WORKING_HOLD_TICKS} tick，方块状态、指示灯与循环音效不再逐 tick 闪烁。</li>
 * </ul>
 * <p><b>JA:</b> 作物収穫機（IC2 Experimental）。毎 tick、9 × 3 × 9 範囲の 1 か所を走査し、熟した作物を収穫する。
 * 作物は作物棒に残り、収穫後の段階から再成長する。
 * <ul>
 *   <li>0.1.8.1：アイテムを地面に落とさない。入らないドロップは保存される保留リストに入る。リストが空でない間は
 *       走査せず電力も使わない。空きができしだい保留アイテムを格納する。</li>
 *   <li>0.1.8.1：最後の収穫から {@link #WORKING_HOLD_TICKS} tick の間 WORKING 状態を保つ。ブロックステート、
 *       ランプ、ループ音が毎 tick 点滅しない。</li>
 * </ul>
 */
@SuppressWarnings("null")
public class mio_icif_harvest_elc extends AbstractProcessingMachineBlockEntity implements com.miophas.singularity_iteration.common.area.WorkAreaProvider {
    private int scanIndex;
    /** EN: Ticks the WORKING state stays on after a harvest. ZH: 收割后 WORKING 状态保持的 tick 数。JA: 収穫後に WORKING 状態を保つ tick 数。 */
    public static final int WORKING_HOLD_TICKS = 40;
    /** EN: Drops that did not fit into storage (saved). ZH: 未能放入存储的掉落物（会保存）。JA: 格納できなかったドロップ（保存される）。 */
    private final List<ItemStack> pendingDrops = new java.util.ArrayList<>();
    private int workingHold;

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
                    case 2 -> pendingDrops.size();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 3;
            }
        };

        return new com.miophas.singularity_iteration.common.menu.producer.HarvestElcMenu(
            containerId, playerInventory, this, data);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= SLOT_STORAGE_START && slot < SLOT_STORAGE_START + SLOT_STORAGE_COUNT) {
            // 存储槽接受任意物品（收获时由机器自己放入）。0.1.10.42：存储槽在 SlotLayout 中是 OUTPUT，物品处理器对外只允许取出，
            // 自动化不能插入（不同于 IC2 contentSlot 的 Access.IO）
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
            // 0.1.10.42：此分支不会生效。存储槽是 OUTPUT，MachineItemHandler 先拒绝插入
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
        // EN: Storage full: move pending drops first; do not scan until all of them are stored.
        // ZH: 存储已满：先移入待存掉落物；全部存入前不扫描。
        // JA: 格納庫が満杯：先に保留ドロップを移す。すべて格納するまで走査しない。
        if (!blockEntity.pendingDrops.isEmpty() && !blockEntity.flushPending()) {
            blockEntity.updateWorkingState(level, pos, false);
            return;
        }
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
        return level.isLoaded(target) && tryHarvestCropStick(level, target);
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
            if (drop == null || drop.isEmpty()) continue;
            apiUseEnergy(ENERGY_PER_HARVEST, false);
            if (optimal) {
                apiUseEnergy(ENERGY_PER_ANALYZER, false);
            }
            ItemStack rest = drop.copy();
            if (!addItemToStorage(rest) && !rest.isEmpty()) {
                // EN: 0.1.8.1: keep the rest in the machine; never spill it into the world.
                // ZH: 0.1.8.1：剩余部分留在机器中，绝不撒到世界里。
                // JA: 0.1.8.1：残りは機械内に保持し、地面にはこぼさない。
                pendingDrops.add(rest);
            }
        }
        setChanged();
        return true;
    }

    /**
     * <p><b>EN:</b> Moves pending drops into storage. Returns true when the list is empty after the move.
     * <p><b>ZH:</b> 把待存掉落物移入存储。移完后列表为空时返回 true。
     * <p><b>JA:</b> 保留ドロップを格納庫へ移す。移動後にリストが空なら true を返す。
     */
    private boolean flushPending() {
        boolean changed = false;
        for (var it = pendingDrops.iterator(); it.hasNext(); ) {
            ItemStack stack = it.next();
            int before = stack.getCount();
            if (addItemToStorage(stack) || stack.isEmpty()) {
                it.remove();
                changed = true;
            } else if (stack.getCount() != before) {
                changed = true;
            }
        }
        if (changed) setChanged();
        return pendingDrops.isEmpty();
    }

    /**
     * <p><b>EN:</b> Drops the pending items at the machine. The block calls this when it is removed.
     * <p><b>ZH:</b> 在机器处掉落待存物品。方块被移除时调用。
     * <p><b>JA:</b> 保留アイテムを機械の位置に落とす。ブロックが壊されたときに呼ばれる。
     */
    public void dropPending(Level level, BlockPos pos) {
        for (ItemStack stack : pendingDrops) {
            if (!stack.isEmpty()) net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        pendingDrops.clear();
    }

    /** EN: Number of pending stacks (for the GUI and tests). ZH: 待存物品堆数（供界面与测试）。JA: 保留スタック数（GUI とテスト用）。 */
    public int pendingCount() {
        return pendingDrops.size();
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

    private void updateWorkingState(Level level, BlockPos pos, boolean working) {
        // EN: Hold the state for WORKING_HOLD_TICKS after the last harvest (no flicker).
        // ZH: 最后一次收割后保持状态 WORKING_HOLD_TICKS tick（不闪烁）。
        // JA: 最後の収穫から WORKING_HOLD_TICKS tick 状態を保つ（点滅しない）。
        if (working) workingHold = WORKING_HOLD_TICKS;
        else if (workingHold > 0) workingHold--;
        boolean shown = workingHold > 0;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof com.miophas.singularity_iteration.common.block.producer.mio_icif_block_harvest) {
            boolean currentWorking = state.getValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_harvest.WORKING);
            if (currentWorking != shown) {
                level.setBlock(pos, state.setValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_harvest.WORKING, shown), 3);
            }
        }
    }

    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CropScanIndex", scanIndex);
        if (!pendingDrops.isEmpty()) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (ItemStack stack : pendingDrops) if (!stack.isEmpty()) list.add(stack.save(registries));
            tag.put("PendingDrops", list);
        }
    }
    @Override public void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        scanIndex = com.miophas.singularity_iteration.core.runtime.crop.CropScanCursor.restore(tag.getInt("CropScanIndex"));
        pendingDrops.clear();
        net.minecraft.nbt.ListTag list = tag.getList("PendingDrops", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack.parse(registries, list.getCompound(i)).filter(s -> !s.isEmpty()).ifPresent(pendingDrops::add);
        }
    }

    @Override
    public java.util.List<com.miophas.singularity_iteration.common.area.WorkArea> workAreas() {
        int r = com.miophas.singularity_iteration.core.runtime.crop.CropScanCursor.RADIUS;
        return java.util.List.of(com.miophas.singularity_iteration.common.area.WorkArea.box(worldPosition, r, 1, 1, r, com.miophas.singularity_iteration.common.area.WorkArea.FARM));
    }
}

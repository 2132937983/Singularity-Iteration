package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.Menu.Producer.SorterElcMenu;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_sorter_elc extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .battery()
        .upgrade(3)
        .input(11)
        .build();

    public static final int BATTERY_SLOT = 0;
    public static final int UPGRADE_SLOT_START = 1;
    public static final int UPGRADE_SLOT_COUNT = 3;
    public static final int BUFFER_SLOT_START = 4;
    public static final int BUFFER_SLOT_COUNT = 11;
    public static final int FILTER_SLOT_START = 15;
    public static final int FILTER_SLOT_COUNT = 42;

    public static final int FILTER_SLOTS_PER_DIRECTION = 7;
    public static final Direction[] FILTER_DIRECTIONS = {
        Direction.DOWN, Direction.UP, Direction.NORTH,
        Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    public static final long DEFAULT_CAPACITY = 15000L;
    public static final long DEFAULT_MAX_RECEIVE = 128L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_COOK_TIME = 20;
    public static final long DEFAULT_ENERGY_PER_TICK = 0L;
    public static final long ENERGY_PER_ITEM = 20L;

    private final ItemStack[] filterStacks = new ItemStack[FILTER_SLOT_COUNT];
    private Direction defaultOutputDirection = Direction.DOWN;

    public mio_icif_sorter_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.SORTER_ELC.get());
    }

    public mio_icif_sorter_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_COOK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.MV);
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            filterStacks[i] = ItemStack.EMPTY;
        }
    }

    public Direction getDefaultOutputDirection() {
        return defaultOutputDirection;
    }

    public void setDefaultOutputDirection(Direction dir) {
        this.defaultOutputDirection = dir;
        setChanged();
    }

    public ItemStack getFilterStack(int index) {
        if (index < 0 || index >= FILTER_SLOT_COUNT) return ItemStack.EMPTY;
        return filterStacks[index];
    }

    public void setFilterStack(int index, ItemStack stack) {
        if (index < 0 || index >= FILTER_SLOT_COUNT) return;
        filterStacks[index] = stack;
        setChanged();
    }

    private int getDirectionIndex(Direction dir) {
        for (int i = 0; i < FILTER_DIRECTIONS.length; i++) {
            if (FILTER_DIRECTIONS[i] == dir) return i;
        }
        return -1;
    }

    private boolean hasFilterForDirection(int dirIndex) {
        for (int j = 0; j < FILTER_SLOTS_PER_DIRECTION; j++) {
            if (!filterStacks[dirIndex * FILTER_SLOTS_PER_DIRECTION + j].isEmpty()) return true;
        }
        return false;
    }

    private boolean matchesFilter(int dirIndex, ItemStack stack) {
        for (int j = 0; j < FILTER_SLOTS_PER_DIRECTION; j++) {
            ItemStack filter = filterStacks[dirIndex * FILTER_SLOTS_PER_DIRECTION + j];
            if (!filter.isEmpty() && ic2ItemEquality(stack, filter)) return true;
        }
        return false;
    }

    /**
     * IC2 风格的物品匹配（对应原版 StackUtil.checkItemEquality）：
     * - 相同 Item
     * - 可损坏物品（含DataComponents.DAMAGE）：耐久必须完全相等
     * - 普通物品：忽略耐久
     * - 组件忽略 repair_cost（RepairCost）差异
     */
    private static boolean ic2ItemEquality(ItemStack a, ItemStack b) {
        if (!ItemStack.isSameItem(a, b)) return false;
        boolean aDamageable = a.isDamageableItem();
        boolean bDamageable = b.isDamageableItem();
        if (aDamageable || bDamageable) {
            int aDmg = a.getDamageValue();
            int bDmg = b.getDamageValue();
            if (aDmg != bDmg) return false;
        }
        // 忽略 RepairCost 差异，其余组件需一致
        var aPatch = a.copy();
        var bPatch = b.copy();
        aPatch.remove(net.minecraft.core.component.DataComponents.REPAIR_COST);
        bPatch.remove(net.minecraft.core.component.DataComponents.REPAIR_COST);
        return ItemStack.isSameItemSameComponents(aPatch, bPatch);
    }

    private boolean isItemInAnyFilter(ItemStack stack) {
        for (int dir = 0; dir < 6; dir++) {
            if (matchesFilter(dir, stack)) return true;
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag filterTag = new CompoundTag();
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            if (!filterStacks[i].isEmpty()) {
                filterTag.put("slot" + i, filterStacks[i].save(registries));
            }
        }
        tag.put("filterStacks", filterTag);
        tag.putByte("defaultOutputDir", (byte) defaultOutputDirection.get3DDataValue());
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompoundTag filterTag = tag.getCompound("filterStacks");
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            if (filterTag.contains("slot" + i)) {
                filterStacks[i] = ItemStack.parseOptional(registries, filterTag.getCompound("slot" + i));
            } else {
                filterStacks[i] = ItemStack.EMPTY;
            }
        }
        if (tag.contains("defaultOutputDir")) {
            int dirVal = tag.getByte("defaultOutputDir");
            if (dirVal >= 0 && dirVal <= 5) {
                defaultOutputDirection = Direction.from3DDataValue(dirVal);
            }
        }
    }

    @Override
    protected int[] getSlotsForDirection(@Nullable Direction side) {
        int[] bufferSlots = new int[BUFFER_SLOT_COUNT];
        for (int i = 0; i < BUFFER_SLOT_COUNT; i++) bufferSlots[i] = BUFFER_SLOT_START + i;
        return bufferSlots;
    }

    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot < BUFFER_SLOT_START || slot >= BUFFER_SLOT_START + BUFFER_SLOT_COUNT) {
            return super.canInsertItem(slot, stack, side);
        }
        if (side == null) return true;
 // 允许任何方向插入到缓冲区，确保物品能被该分拣机处理
        // 修复：两个分拣机串联时，不应因为物品不匹配特定方向过滤器就拒绝插入
        return true;
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        if (slot < BUFFER_SLOT_START || slot >= BUFFER_SLOT_START + BUFFER_SLOT_COUNT) {
            return super.canExtractItem(slot, side);
        }
        if (side == null) return true;

        if (energyStorage.getAmount() < energyPerTick) return false;

        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) return false;

        if (side == defaultOutputDirection) {
            int dirIdx = getDirectionIndex(side);
            if (dirIdx >= 0 && matchesFilter(dirIdx, stack)) return false;
            for (int dir = 0; dir < 6; dir++) {
                if (FILTER_DIRECTIONS[dir] == defaultOutputDirection) continue;
                if (matchesFilter(dir, stack)) return false;
            }
            return true;
        } else {
            int dirIdx = getDirectionIndex(side);
            if (dirIdx < 0 || !hasFilterForDirection(dirIdx)) return false;
            return matchesFilter(dirIdx, stack);
        }
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) return isSorterBattery(stack);
        if (slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + UPGRADE_SLOT_COUNT)
            return isTransformerUpgrade(stack);
        if (slot >= BUFFER_SLOT_START && slot < BUFFER_SLOT_START + BUFFER_SLOT_COUNT) return true;
        return false;
    }

    @SuppressWarnings("deprecation")
    private boolean isSorterBattery(ItemStack stack) {
        if (stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat) return true;
        if (hasTransformerUpgrade()) {
            return isBattery(stack);
        }
        return false;
    }

    public boolean isSorterBatteryForGUI(ItemStack stack) {
        return isSorterBattery(stack);
    }

    private boolean isTransformerUpgrade(ItemStack stack) {
        return MioIcifAPI.instance().getUpgradeAPI().getUpgradeType(stack) == com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.UpgradeType.TRANSFORMER;
    }

    private boolean hasTransformerUpgrade() {
        for (int i = UPGRADE_SLOT_START; i < UPGRADE_SLOT_START + UPGRADE_SLOT_COUNT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (isTransformerUpgrade(stack)) return true;
        }
        return false;
    }

    @Override
    protected boolean canWork() {
        if (energyStorage.getAmount() < ENERGY_PER_ITEM) return false;
        for (int i = BUFFER_SLOT_START; i < BUFFER_SLOT_START + BUFFER_SLOT_COUNT; i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) return true;
        }
        return false;
    }

    @Override
    protected void doWork() {
        if (energyStorage.getAmount() < ENERGY_PER_ITEM) return;

        // 对应IC2: 遍历所有buffer槽位, 每个非空槽位独立尝试路由到邻居
        for (int i = BUFFER_SLOT_START; i < BUFFER_SLOT_START + BUFFER_SLOT_COUNT; i++) {
            // 对应IC2: 每个槽位开始前检查能量门槛
            if (energyStorage.getAmount() < ENERGY_PER_ITEM) return;

            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            tryOutputItem(stack, i);
        }
    }

    /**
     * 对应IC2 StackUtil.getAdjacentInventories 排序: 按邻居库存容量（总槽数×每槽上限）之和降序
     * 返回 [dirIndex, capacity] 列表
     */
    private List<int[]> sortedDirectionsWithCapacity() {
        List<int[]> list = new ArrayList<>();
        for (int dir = 0; dir < 6; dir++) {
            Direction direction = FILTER_DIRECTIONS[dir];
            IItemHandler h = getAdjacentItemHandler(worldPosition.relative(direction), direction.getOpposite());
            int cap = 0;
            if (h != null) {
                for (int s = 0; s < h.getSlots(); s++) {
                    cap += h.getSlotLimit(s);
                }
            }
            list.add(new int[]{dir, cap});
        }
        list.sort((a, b) -> Integer.compare(b[1], a[1]));
        return list;
    }

    private boolean tryOutputItem(ItemStack stack, int slotIndex) {
        int stackSize = stack.getCount();

        // 对应IC2: 按邻居容量降序遍历方向（IC2 StackUtil.getAdjacentInventories 排序: 个人方块优先, 然后按容量降序）
        List<int[]> ordered = sortedDirectionsWithCapacity();

        for (int[] entry : ordered) {
            int dir = entry[0];
            Direction direction = FILTER_DIRECTIONS[dir];
            if (direction == defaultOutputDirection) continue;
            if (!hasFilterForDirection(dir)) continue;

            IItemHandler target = getAdjacentItemHandler(worldPosition.relative(direction), direction.getOpposite());
            if (target == null) continue;

            ItemStack[] filterStacksForDir = getFilterStacksForDirection(dir);
            for (ItemStack filter : filterStacksForDir) {
                if (filter.isEmpty()) continue;
                if (!ic2ItemEquality(stack, filter)) continue;

                int filterSize = filter.getCount();
                if (stackSize < filterSize) continue;
                if (energyStorage.getAmount() < ENERGY_PER_ITEM * filterSize) continue;

                // 对应IC2: 匹配的过滤项已确定, simulate整批
                ItemStack transferStack = stack.copyWithCount(filterSize);
                ItemStack remaining = ItemHandlerHelper.insertItemStacked(target, transferStack, true);
                // 整批全塞入-> 执行 + 返回; 否则该邻居不再尝试其它过滤(break)
                if (remaining.isEmpty()) {
                    ItemHandlerHelper.insertItemStacked(target, transferStack, false);
                    itemHandler.extractItem(slotIndex, filterSize, false);
                    apiUseEnergy(ENERGY_PER_ITEM * filterSize, false);
                    return true;
                }
                // 匹配但塞不下: 跳过该邻居的其他过滤(对应IC2的break)
                break;
            }
        }

        // 默认路由: 仅当该物品未被任何过滤匹配时才走默认路由 (对应IC2的inFilter 检查)
        boolean inFilter = isItemInAnyFilter(stack);

        if (!inFilter) {
            IItemHandler target = getAdjacentItemHandler(worldPosition.relative(defaultOutputDirection), defaultOutputDirection.getOpposite());
            if (target != null) {
                // 对齝IC2: 默认路由丝simulate, 直接execute?? 按实际塞入針扣凝
                ItemStack toMove = stack.copyWithCount(1);
                ItemStack remaining = ItemHandlerHelper.insertItemStacked(target, toMove, false);
                int amount = 1 - remaining.getCount();
                if (amount > 0) {
                    itemHandler.extractItem(slotIndex, amount, false);
                    apiUseEnergy(ENERGY_PER_ITEM * amount, false);
                    return true;
                }
            }
        }

        return false;
    }

    private ItemStack[] getFilterStacksForDirection(int dirIndex) {
        ItemStack[] result = new ItemStack[FILTER_SLOTS_PER_DIRECTION];
        for (int j = 0; j < FILTER_SLOTS_PER_DIRECTION; j++) {
            result[j] = filterStacks[dirIndex * FILTER_SLOTS_PER_DIRECTION + j];
        }
        return result;
    }

    public boolean hasAdjacentInventory(Direction dir) {
        if (level == null) return false;
        IItemHandler handler = getAdjacentItemHandler(worldPosition.relative(dir), dir.getOpposite());
        return handler != null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_sorter_elc blockEntity) {
        mio_icif_producer.tick(level, pos, state, blockEntity);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.sorter_elc");
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
        return new SorterElcMenu(containerId, playerInventory, this);
    }
}
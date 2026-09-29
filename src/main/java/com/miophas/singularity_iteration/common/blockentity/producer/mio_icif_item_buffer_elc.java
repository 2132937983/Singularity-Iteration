package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.core.api.item.IItemBuffer;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.upgrade.IUpgradeAPI;

import com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.menu.producer.ItemBufferMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_item_buffer_elc extends BlockEntity implements WorldlyContainer, MenuProvider, IItemBuffer {

    private static final List<Direction> ALL_DIRECTIONS = List.of(Direction.values());

    public static final int SLOT_COUNT = 50;
    public static final int LEFT_CONTENT_SLOTS = 24;
    public static final int RIGHT_CONTENT_SLOTS = 24;
    public static final int UPGRADE_SLOT_1 = 48;
    public static final int UPGRADE_SLOT_2 = 49;

    // Upgrade statistics include the immutable directional projections used by the
    // transfer loops. Rebuild them only when one of the two upgrade slots changes;
    // rebuilding both fromInventory snapshots every tick otherwise allocates on the
    // hot path even when the buffer contents are the only thing changing.
    private MachineUpgradeStats leftUpgradeStats = MachineUpgradeStats.empty();
    private MachineUpgradeStats rightUpgradeStats = MachineUpgradeStats.empty();
    private boolean upgradeStatsDirty = true;

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // 升级槽(48,49): 仅接受 ItemProducing 适用升级 (对齐IC2 getUpgradableProperties={ItemProducing})
            if (slot == UPGRADE_SLOT_1 || slot == UPGRADE_SLOT_2) {
                return isValidBufferUpgrade(stack);
            }
            return true;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == UPGRADE_SLOT_1 || slot == UPGRADE_SLOT_2) {
                upgradeStatsDirty = true;
            }
            setChanged();
        }
    };

    /**
     * 物品缓冲机可用的升级类型 (对应IC2 UpgradableProperty.ItemProducing):
     * Overclocker(超频)、Ejector(弹出)、Pulling(抽取)。
     * 不接受: Transformer、Energy_storage、Redstone_inverter、Fluid*。
     */
    private static boolean isValidBufferUpgrade(ItemStack stack) {
        if (stack.isEmpty()) return true;
        IUpgradeAPI.UpgradeType t = MioIcifAPI.instance().getUpgradeAPI().getUpgradeType(stack);
        return t == IUpgradeAPI.UpgradeType.OVERCLOCKER
            || t == IUpgradeAPI.UpgradeType.EJECTOR
            || t == IUpgradeAPI.UpgradeType.IMPORT;
    }

    private static final int[] LEFT_SLOTS = new int[LEFT_CONTENT_SLOTS];
    private static final int[] RIGHT_SLOTS = new int[RIGHT_CONTENT_SLOTS];
    private static final int[] ALL_CONTENT_SLOTS = new int[LEFT_CONTENT_SLOTS + RIGHT_CONTENT_SLOTS];
    static {
        for (int i = 0; i < LEFT_CONTENT_SLOTS; i++) {
            LEFT_SLOTS[i] = i;
        }
        for (int i = 0; i < RIGHT_CONTENT_SLOTS; i++) {
            RIGHT_SLOTS[i] = LEFT_CONTENT_SLOTS + i;
        }
        for (int i = 0; i < LEFT_CONTENT_SLOTS + RIGHT_CONTENT_SLOTS; i++) {
            ALL_CONTENT_SLOTS[i] = i;
        }
    }

    public mio_icif_item_buffer_elc(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.ITEM_BUFFER_ELC.get(), pos, state);
    }

    private void refreshUpgradeStats() {
        if (!upgradeStatsDirty) return;
        leftUpgradeStats = MachineUpgradeStats.fromInventory(itemHandler, UPGRADE_SLOT_1, 1);
        rightUpgradeStats = MachineUpgradeStats.fromInventory(itemHandler, UPGRADE_SLOT_2, 1);
        upgradeStatsDirty = false;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_item_buffer_elc blockEntity) {
        if (level.isClientSide()) return;

        blockEntity.refreshUpgradeStats();

        // 升级槽1(左/绿色面) → 仅影响左侧内容槽(0~23)
        // 升级槽2(右/蓝色面) → 仅影响右侧内容槽(24~47)
        MachineUpgradeStats leftStats = blockEntity.leftUpgradeStats;
        MachineUpgradeStats rightStats = blockEntity.rightUpgradeStats;

        if (leftStats.ejectorCount > 0) {
            blockEntity.bufferEjectItems(leftStats, 0, LEFT_CONTENT_SLOTS);
        }
        if (rightStats.ejectorCount > 0) {
            blockEntity.bufferEjectItems(rightStats, LEFT_CONTENT_SLOTS, RIGHT_CONTENT_SLOTS);
        }
        if (leftStats.pullingCount > 0) {
            blockEntity.bufferPullItems(leftStats, 0, LEFT_CONTENT_SLOTS);
        }
        if (rightStats.pullingCount > 0) {
            blockEntity.bufferPullItems(rightStats, LEFT_CONTENT_SLOTS, RIGHT_CONTENT_SLOTS);
        }
    }

    private void bufferEjectItems(MachineUpgradeStats stats, int slotStart, int slotCount) {
        int maxPerTick = Math.max(1, stats.ejectorCount);
        List<Direction> dirs = stats.getEjectorDirections();
        Iterable<Direction> targetDirs = !dirs.isEmpty() ? dirs : ALL_DIRECTIONS;
        for (int i = 0; i < slotCount; i++) {
            int slot = slotStart + i;
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            for (Direction dir : targetDirs) {
                IItemHandler target = getAdjacentItemHandlerForBuffer(worldPosition.relative(dir), dir.getOpposite());
                if (target == null) continue;
                int moveCount = Math.min(stack.getCount(), maxPerTick);
                ItemStack toMove = stack.copyWithCount(moveCount);
                ItemStack remaining = ItemHandlerHelper.insertItemStacked(target, toMove, false);
                int moved = moveCount - remaining.getCount();
                if (moved > 0) {
                    itemHandler.extractItem(slot, moved, false);
                    stack = itemHandler.getStackInSlot(slot);
                    if (stack.isEmpty()) break;
                }
            }
        }
    }

    private void bufferPullItems(MachineUpgradeStats stats, int slotStart, int slotCount) {
        int maxPerTick = Math.max(1, stats.pullingCount);
        List<Direction> dirs = stats.getPullingDirections();
        Iterable<Direction> targetDirs = !dirs.isEmpty() ? dirs : ALL_DIRECTIONS;
        for (int i = 0; i < slotCount; i++) {
            int slot = slotStart + i;
            ItemStack current = itemHandler.getStackInSlot(slot);
            if (!current.isEmpty() && current.getCount() >= current.getMaxStackSize()) continue;
            for (Direction dir : targetDirs) {
                IItemHandler source = getAdjacentItemHandlerForBuffer(worldPosition.relative(dir), dir.getOpposite());
                if (source == null) continue;
                boolean pulled = false;
                for (int j = 0; j < source.getSlots(); j++) {
                    ItemStack sourceStack = source.getStackInSlot(j);
                    if (sourceStack.isEmpty()) continue;
                    if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, sourceStack)) continue;
                    int moveCount = Math.min(sourceStack.getCount(), maxPerTick);
                    ItemStack simulatedExtract = source.extractItem(j, moveCount, true);
                    if (simulatedExtract.isEmpty()) continue;
                    ItemStack remainder = itemHandler.insertItem(slot, simulatedExtract, true);
                    int canMove = simulatedExtract.getCount() - remainder.getCount();
                    if (canMove <= 0) continue;
                    ItemStack extracted = source.extractItem(j, canMove, false);
                    if (!extracted.isEmpty()) {
                        itemHandler.insertItem(slot, extracted, false);
                        pulled = true;
                        break;
                    }
                }
                if (pulled) break;
            }
        }
    }

    @Nullable
    private IItemHandler getAdjacentItemHandlerForBuffer(BlockPos pos, @Nullable Direction side) {
        if (level == null) return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return isValidSlot(slot) ? itemHandler.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!isValidSlot(slot) || amount <= 0) return ItemStack.EMPTY;
        ItemStack result = itemHandler.extractItem(slot, amount, false);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) return ItemStack.EMPTY;
        ItemStack stack = itemHandler.getStackInSlot(slot).copy();
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        if (!stack.isEmpty()) setChanged();
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) return;
        itemHandler.setStackInSlot(slot, stack == null ? ItemStack.EMPTY : stack);
        setChanged();
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < itemHandler.getSlots();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null) return false;
        if (level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == null) {
            return ALL_CONTENT_SLOTS;
        }
        if (isSide(side)) {
            // 水平面(SIDE): 仅右槽(24~47)可见，对应原版IC2 rightcontent(InvSide.SIDE)
            return RIGHT_SLOTS;
        }
        // UP/DOWN(NOTSIDE): 仅左槽(0~23)可见，对应原版IC2 leftcontent(InvSide.NOTSIDE)
        return LEFT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if (slot < 0 || slot >= LEFT_CONTENT_SLOTS + RIGHT_CONTENT_SLOTS) return false;
        if (direction == null) return true;
        if (isSide(direction)) {
            // 水平面(SIDE): 仅右槽(24~47)可插入
            return slot >= LEFT_CONTENT_SLOTS && slot < LEFT_CONTENT_SLOTS + RIGHT_CONTENT_SLOTS;
        }
        // UP/DOWN(NOTSIDE): 仅左槽(0~23)可插入
        return slot < LEFT_CONTENT_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        if (slot < 0 || slot >= LEFT_CONTENT_SLOTS + RIGHT_CONTENT_SLOTS) return false;
        if (direction == null) return true;
        if (isSide(direction)) {
            // 水平面(SIDE): 仅右槽(24~47)可抽取
            return slot >= LEFT_CONTENT_SLOTS && slot < LEFT_CONTENT_SLOTS + RIGHT_CONTENT_SLOTS;
        }
        // UP/DOWN(NOTSIDE): 仅左槽(0~23)可抽取
        return slot < LEFT_CONTENT_SLOTS;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", itemHandler.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("items"));
            // Some ItemStackHandler implementations do not notify listeners while
            // loading. Ensure a post-load tick cannot reuse a pre-load snapshot.
            upgradeStatsDirty = true;
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.item_buffer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ItemBufferMenu(containerId, playerInventory, itemHandler, this);
    }

    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        if (side == null) return itemHandler;
        return new BufferSidedItemHandler(side);
    }

    private static boolean isSide(Direction dir) {
        return dir == Direction.NORTH || dir == Direction.SOUTH || dir == Direction.EAST || dir == Direction.WEST;
    }

    private class BufferSidedItemHandler implements IItemHandler {
        private final Direction side;
        private final int[] accessibleSlots;

        BufferSidedItemHandler(Direction side) {
            this.side = (side != null) ? side.getOpposite() : null;
            if (this.side != null && isSide(this.side)) {
                // 水平面(SIDE): 仅右槽(24~47)可见，对应原版IC2 rightcontent(InvSide.SIDE)
                accessibleSlots = RIGHT_SLOTS;
            } else if (this.side != null) {
                // UP/DOWN(NOTSIDE): 仅左槽(0~23)可见，对应原版IC2 leftcontent(InvSide.NOTSIDE)
                accessibleSlots = LEFT_SLOTS;
            } else {
                // null: 内部访问，左右两槽都可见
                accessibleSlots = ALL_CONTENT_SLOTS;
            }
        }

        @Override
        public int getSlots() {
            return accessibleSlots.length;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= accessibleSlots.length) return ItemStack.EMPTY;
            return itemHandler.getStackInSlot(accessibleSlots[slot]);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot < 0 || slot >= accessibleSlots.length) return stack;
            return itemHandler.insertItem(accessibleSlots[slot], stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= accessibleSlots.length) return ItemStack.EMPTY;
            return itemHandler.extractItem(accessibleSlots[slot], amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot < 0 || slot >= accessibleSlots.length) return 0;
            return itemHandler.getSlotLimit(accessibleSlots[slot]);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot < 0 || slot >= accessibleSlots.length) return false;
            return itemHandler.isItemValid(accessibleSlots[slot], stack);
        }
    }
}

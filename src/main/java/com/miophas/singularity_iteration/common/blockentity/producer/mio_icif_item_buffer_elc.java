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
    /** What a side sees: the right half first (its own), then the left half (taken from once the right half is empty). */
    private static final int[] SIDE_ORDER = new int[LEFT_CONTENT_SLOTS + RIGHT_CONTENT_SLOTS];
    static {
        for (int i = 0; i < RIGHT_CONTENT_SLOTS; i++) SIDE_ORDER[i] = LEFT_CONTENT_SLOTS + i;
        for (int i = 0; i < LEFT_CONTENT_SLOTS; i++) SIDE_ORDER[RIGHT_CONTENT_SLOTS + i] = i;
    }
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

        // 0.1.11.4: both upgrade slots act on all 48 content slots, as in IC2. Slot 1 used to act on the left 24 slots
        // and slot 2 on the right 24 only, and nothing moves items between the halves: with a pulling upgrade in one
        // slot and an ejector in the other, what was pulled in never came out. Pipe access per face is unchanged.
        int contentSlots = LEFT_CONTENT_SLOTS + RIGHT_CONTENT_SLOTS;
        MachineUpgradeStats first = blockEntity.leftUpgradeStats, second = blockEntity.rightUpgradeStats;
        if (first.ejectorCount > 0) blockEntity.bufferEjectItems(first, 0, contentSlots);
        if (second.ejectorCount > 0) blockEntity.bufferEjectItems(second, 0, contentSlots);
        if (first.pullingCount > 0) blockEntity.bufferPullItems(first, 0, contentSlots);
        if (second.pullingCount > 0) blockEntity.bufferPullItems(second, 0, contentSlots);
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

    /**
     * 0.1.11.164: the comparator signal of the 48 content slots (the upgrade slots do not count), as a vanilla container
     * gives it: 0 when empty, 1 with anything inside, 15 when every slot holds a full stack. Contents changing call
     * {@link #setChanged}, which updates the comparators next to the block.
     */
    public int getComparatorSignal() {
        float fill = 0;
        boolean any = false;
        for (int slot : ALL_CONTENT_SLOTS) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            fill += stack.getCount() / (float) Math.min(itemHandler.getSlotLimit(slot), stack.getMaxStackSize());
            any = true;
        }
        return net.minecraft.util.Mth.floor(fill / ALL_CONTENT_SLOTS.length * 14.0F) + (any ? 1 : 0);
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

    // 0.1.11.163: the faces follow IC2 (the right half prefers the four sides, the left half prefers no face):
    //  - top and bottom reach all 48 content slots, in and out;
    //  - a side puts items into the right half only, and takes from the right half, or from the left half once the right half
    //    is empty.
    // Before, the top and bottom reached the left half only and the sides the right half only, so the halves never met:
    // items piped in from a side never came out of the bottom (a hopper under the buffer), nor items from the top out of a side.

    private static boolean isContent(int slot) { return slot >= 0 && slot < LEFT_CONTENT_SLOTS + RIGHT_CONTENT_SLOTS; }
    private static boolean isRight(int slot) { return slot >= LEFT_CONTENT_SLOTS && slot < LEFT_CONTENT_SLOTS + RIGHT_CONTENT_SLOTS; }

    private boolean rightHalfEmpty() {
        for (int slot : RIGHT_SLOTS) if (!itemHandler.getStackInSlot(slot).isEmpty()) return false;
        return true;
    }

    /** Whether {@code face} (null: inside access) may put items into {@code slot}. */
    private static boolean mayInsertThrough(int slot, @Nullable Direction face) {
        if (!isContent(slot)) return false;
        return face == null || !isSide(face) || isRight(slot);
    }

    /** Whether {@code face} (null: inside access) may take items out of {@code slot}. */
    private boolean mayExtractThrough(int slot, @Nullable Direction face) {
        if (!isContent(slot)) return false;
        return face == null || !isSide(face) || isRight(slot) || rightHalfEmpty();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side != null && isSide(side) ? SIDE_ORDER : ALL_CONTENT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return mayInsertThrough(slot, direction);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return mayExtractThrough(slot, direction);
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

    /**
     * What one face sees: all 48 content slots (a side lists the right half first), with the IC2 rules of
     * {@link #mayInsertThrough} and {@link #mayExtractThrough}. The upgrade slots are never reachable from outside.
     */
    private class BufferSidedItemHandler implements IItemHandler {
        private final Direction face;
        private final int[] slots;

        BufferSidedItemHandler(Direction face) {
            this.face = face;
            this.slots = isSide(face) ? SIDE_ORDER : ALL_CONTENT_SLOTS;
        }

        private int slot(int index) { return index >= 0 && index < slots.length ? slots[index] : -1; }

        @Override
        public int getSlots() {
            return slots.length;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int index) {
            int slot = slot(index);
            return slot < 0 ? ItemStack.EMPTY : itemHandler.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int index, @NotNull ItemStack stack, boolean simulate) {
            int slot = slot(index);
            if (!mayInsertThrough(slot, face)) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int index, int amount, boolean simulate) {
            int slot = slot(index);
            if (!mayExtractThrough(slot, face)) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int index) {
            int slot = slot(index);
            return slot < 0 ? 0 : itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int index, @NotNull ItemStack stack) {
            int slot = slot(index);
            return mayInsertThrough(slot, face) && itemHandler.isItemValid(slot, stack);
        }
    }
}

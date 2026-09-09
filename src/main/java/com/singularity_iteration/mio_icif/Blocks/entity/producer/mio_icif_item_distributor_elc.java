package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.api.item.IItemDistributor;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Menu.Producer.ItemDistributorElcMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
public class mio_icif_item_distributor_elc extends BlockEntity implements net.minecraft.world.MenuProvider, IItemDistributor {

    public static final int BUFFER_SLOT_COUNT = 9;
    public static final int PRIORITY_DATA_SIZE = 6;

    private final ItemStackHandler itemHandler = new ItemStackHandler(BUFFER_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final net.minecraft.world.inventory.ContainerData priorityData = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            if (index < priority.size()) {
                return priority.get(index).get3DDataValue();
            }
            return -1;
        }

        @Override
        public void set(int index, int value) {
            if (value >= 0 && value <= 5) {
                while (priority.size() <= index) {
                    priority.add(Direction.NORTH);
                }
                priority.set(index, Direction.from3DDataValue(value));
            } else if (index < priority.size()) {
                priority.remove(index);
            }
        }

        @Override
        public int getCount() {
            return PRIORITY_DATA_SIZE;
        }
    };

    private List<Direction> priority = new ArrayList<>(5);

    public mio_icif_item_distributor_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.ITEM_DISTRIBUTOR_ELC.get());
    }

    public mio_icif_item_distributor_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type, pos, state);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return new DistributorSidedItemHandler(side);
    }

    public net.minecraft.world.inventory.ContainerData getPriorityData() {
        return priorityData;
    }

    public List<Direction> getPriority() {
        return priority;
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING)) {
            return state.getValue(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        }
        return Direction.NORTH;
    }

    public void setPriority(List<Direction> priorities) {
        this.priority.clear();
        this.priority.addAll(priorities);
        Direction facing = getFacing();
        this.priority.removeIf(d -> d == facing);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        if (!priority.isEmpty()) {
            tag.putIntArray("priority", priority.stream().mapToInt(Direction::get3DDataValue).toArray());
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        priority.clear();
        if (tag.contains("priority")) {
            int[] indexes = tag.getIntArray("priority");
            Direction facing = getFacing();
            for (int index : indexes) {
                Direction d = Direction.from3DDataValue(index);
                if (d != null && d != facing) {
                    priority.add(d);
                }
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        if (!priority.isEmpty()) {
            tag.putIntArray("priority", priority.stream().mapToInt(Direction::get3DDataValue).toArray());
        }
        return tag;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    protected IItemHandler getAdjacentItemHandler(BlockPos pos, @Nullable Direction side) {
        if (level == null) return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_item_distributor_elc blockEntity) {
        if (level.isClientSide()) return;

        if (blockEntity.priority.isEmpty()) return;

        boolean hasItems = false;
        for (int i = 0; i < BUFFER_SLOT_COUNT; i++) {
            if (!blockEntity.itemHandler.getStackInSlot(i).isEmpty()) {
                hasItems = true;
                break;
            }
        }
        if (!hasItems) return;

        boolean hasChangedOuter = false;

        for (Direction dir : blockEntity.priority) {
            IItemHandler target = blockEntity.getAdjacentItemHandler(pos.relative(dir), dir.getOpposite());
            if (target == null) continue;

            boolean hasChanged = false;
            boolean allEmpty = true;

            for (int i = 0; i < BUFFER_SLOT_COUNT; i++) {
                ItemStack stack = blockEntity.itemHandler.getStackInSlot(i);
                if (stack.isEmpty()) continue;

                // 对齐IC2: simulate整个堆, 若能塞入任意数量(amount>0)则执行实际移动的数量
                ItemStack toMove = stack.copyWithCount(stack.getCount());
                ItemStack remaining = ItemHandlerHelper.insertItemStacked(target, toMove, true);
                int amount = stack.getCount() - remaining.getCount();
                if (amount > 0) {
                    ItemHandlerHelper.insertItemStacked(target, stack.copyWithCount(amount), false);
                    blockEntity.itemHandler.extractItem(i, amount, false);
                    hasChanged = true;
                }
                if (!blockEntity.itemHandler.getStackInSlot(i).isEmpty()) {
                    allEmpty = false;
                }
            }

            hasChangedOuter |= hasChanged;

            if (hasChanged && allEmpty) break;
        }

        if (hasChangedOuter) blockEntity.setChanged();
    }
    

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.item_distributor_elc");
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
        return new ItemDistributorElcMenu(containerId, playerInventory, this);
    }

    private class DistributorSidedItemHandler implements IItemHandlerModifiable {
        @Nullable
        private final Direction side;

        DistributorSidedItemHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            itemHandler.setStackInSlot(slot, stack);
        }

        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return itemHandler.isItemValid(slot, stack);
        }
    }
}


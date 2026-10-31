package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The menu of a fluid port with one upgrade slot (Fluid Ejector / Fluid Pulling Upgrade): the fluid reactor's fluid port, and since
 * 0.1.10.39 the Steam Tunnel's fluid port.
 */
@SuppressWarnings("null")
public class ReactorFluidPortMenu extends mio_icif_base_menu {

    /** A block entity whose menu this is. */
    public interface Host {
        MachineItemHandler getItemHandler();

        boolean hasPlugin();
    }

    private final ContainerLevelAccess access;
    private final @Nullable BlockEntity blockEntity;

    public ReactorFluidPortMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    /** {@code blockEntity}: a {@link Host}; null on the client. */
    public ReactorFluidPortMenu(int containerId, Inventory playerInventory, @Nullable BlockEntity blockEntity) {
        super(mio_icif_menus.REACTOR_FLUID_PORT_MENU_TYPE.get(), containerId, 1, 1);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;

        if (blockEntity instanceof Host host) {
            this.addSlot(new SlotItemHandler(host.getItemHandler(), 0, 80, 43));
        } else {
            this.addSlot(new Slot(new SimpleContainer(1), 0, 80, 43));
        }

        addPlayerInventory(playerInventory, 84, 142);
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, blockEntity != null ? blockEntity.getBlockState().getBlock() : mio_icif_blocks.REACTOR_FLUID_PORT.get());
    }

    @Nullable
    public BlockEntity getBlockEntity() {
        return blockEntity;
    }

    public boolean hasPlugin() {
        return blockEntity instanceof Host host && host.hasPlugin();
    }

    private static class SlotItemHandler extends Slot {
        private final net.neoforged.neoforge.items.IItemHandler itemHandler;
        private final int index;

        public SlotItemHandler(net.neoforged.neoforge.items.IItemHandler itemHandler, int index, int x, int y) {
            super(new SimpleContainer(0), index, x, y);
            this.itemHandler = itemHandler;
            this.index = index;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.itemHandler.isItemValid(this.index, stack);
        }

        @Override
        public ItemStack getItem() {
            return this.itemHandler.getStackInSlot(this.index);
        }

        @Override
        public void set(ItemStack stack) {
            this.itemHandler.insertItem(this.index, stack, false);
            this.setChanged();
        }

        @Override
        public void setByPlayer(ItemStack stack) {
            this.itemHandler.extractItem(this.index, this.getItem().getCount(), false);
            this.itemHandler.insertItem(this.index, stack, false);
            this.setChanged();
        }

        @Override
        public ItemStack remove(int amount) {
            return this.itemHandler.extractItem(this.index, amount, false);
        }

        @Override
        public boolean hasItem() {
            return !this.getItem().isEmpty();
        }

        @Override
        public int getMaxStackSize() {
            return this.itemHandler.getSlotLimit(this.index);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.min(this.getMaxStackSize(), stack.getMaxStackSize());
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean allowModification(Player player) {
            return true;
        }

        @Override
        public boolean isSameInventory(Slot other) {
            return other instanceof SlotItemHandler && ((SlotItemHandler) other).itemHandler == this.itemHandler;
        }
    }
}


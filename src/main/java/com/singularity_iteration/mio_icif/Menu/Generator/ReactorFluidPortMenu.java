package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_base_menu;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_fluid_port;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class ReactorFluidPortMenu extends mio_icif_base_menu {

    private final ContainerLevelAccess access;
    private final mio_icif_reactor_fluid_port blockEntity;

    public ReactorFluidPortMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public ReactorFluidPortMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_reactor_fluid_port blockEntity) {
        super(mio_icif_menus.REACTOR_FLUID_PORT_MENU_TYPE.get(), containerId, 1, 1);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;

        if (blockEntity != null) {
            this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, 80, 43) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return true;
                }
            });
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
        return stillValid(this.access, player, mio_icif_blocks.REACTOR_FLUID_PORT.get());
    }

    @Nullable
    public mio_icif_reactor_fluid_port getBlockEntity() {
        return blockEntity;
    }

    public boolean hasPlugin() {
        return blockEntity != null && blockEntity.hasPlugin();
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


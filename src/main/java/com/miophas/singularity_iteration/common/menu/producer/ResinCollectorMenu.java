package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.blockentity.producer.ResinCollectorBlockEntity;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class ResinCollectorMenu extends mio_icif_base_menu {
    private final @Nullable ResinCollectorBlockEntity collector;
    @Override protected boolean isBattery(ItemStack stack) { return false; }
    @Override protected boolean isUpgrade(ItemStack stack) { return false; }
    public ResinCollectorMenu(int id, Inventory inventory) { this(id, inventory, null); }
    public ResinCollectorMenu(int id, Inventory inventory, @Nullable ResinCollectorBlockEntity collector) {
        super(mio_icif_menus.RESIN_COLLECTOR_MENU.get(), id, 1, 1);
        this.collector = collector;
        var handler = collector == null ? ResinCollectorBlockEntity.createInventory(() -> {}) : collector.getItemHandler();
        addSlot(new SlotItemHandler(handler, 0, 80, 35));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 67 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 125));
    }
    @Override public boolean stillValid(Player player) {
        if (collector == null) return player.level().isClientSide;
        return !collector.isRemoved() && collector.getLevel() == player.level()
            && player.level().getBlockEntity(collector.getBlockPos()) == collector
            && player.canInteractWithBlock(collector.getBlockPos(), 4.0);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), before = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return before;
    }
}

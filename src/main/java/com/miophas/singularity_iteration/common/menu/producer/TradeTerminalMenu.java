// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.blockentity.producer.TradeTerminalBlockEntity;
import com.miophas.singularity_iteration.common.exchange.ExchangeService;
import com.miophas.singularity_iteration.common.exchange.SettlementStorages;
import com.miophas.singularity_iteration.common.exchange.TerminalRules;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.27 (X2): the menu of the Trade Terminal. Slots 0-26 the sell buffer (put in: TRADE, take out: CONFIG), 27-35 the
 * output buffer (take out: CONFIG), 36 the settlement slot of this viewer (TRADE; returned when the menu closes), 37-72 the
 * player inventory. The viewer's access level is menu data 0; the server checks it again on every click and action.
 */
@SuppressWarnings("null")
public class TradeTerminalMenu extends AbstractContainerMenu {
    public static final int SELL_FIRST = 0, OUTPUT_FIRST = TerminalRules.SELL_SLOTS, SETTLE = OUTPUT_FIRST + TerminalRules.OUTPUT_SLOTS,
        INV_FIRST = SETTLE + 1, INV_END = INV_FIRST + 36;
    /** Slot positions in the panel (the screen draws the frames there). */
    public static final int SELL_X = 8, SELL_Y = 34, OUTPUT_Y = 104, SETTLE_X = 192, SETTLE_Y = 34, INV_Y = 140, HOTBAR_Y = 198;

    @Nullable private final TradeTerminalBlockEntity terminal;
    private final ContainerData data;
    private final SimpleContainer settlement = new SimpleContainer(1);
    private final Player viewer;
    /** Client: the panel shows the buffer page (all slots drawn and clickable). */
    public boolean bufferView = true;
    /** Client: the panel shows the settlement page (the settlement slot and the inventory drawn and clickable). */
    public boolean settleView;

    public TradeTerminalMenu(int id, Inventory inventory) {
        this(id, inventory, null, new SimpleContainer(TerminalRules.SELL_SLOTS), new SimpleContainer(TerminalRules.OUTPUT_SLOTS),
            new SimpleContainerData(TradeTerminalBlockEntity.DATA_COUNT));
    }

    public TradeTerminalMenu(int id, Inventory inventory, TradeTerminalBlockEntity terminal) {
        this(id, inventory, terminal, terminal.sellBuffer(), terminal.outputBuffer(), terminal.viewerData(inventory.player));
    }

    private TradeTerminalMenu(int id, Inventory inventory, @Nullable TradeTerminalBlockEntity terminal, Container sell, Container output, ContainerData data) {
        super(mio_icif_menus.TRADE_TERMINAL_MENU_TYPE.get(), id);
        this.terminal = terminal;
        this.data = data;
        this.viewer = inventory.player;
        for (int i = 0; i < TerminalRules.SELL_SLOTS; i++) addSlot(new SellSlot(sell, i, SELL_X + (i % 9) * 18, SELL_Y + (i / 9) * 18));
        for (int i = 0; i < TerminalRules.OUTPUT_SLOTS; i++) addSlot(new OutputSlot(output, i, SELL_X + i * 18, OUTPUT_Y));
        addSlot(new SettleSlot(settlement, SETTLE_X, SETTLE_Y));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new PageSlot(inventory, 9 + row * 9 + col, SELL_X + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new PageSlot(inventory, col, SELL_X + col * 18, HOTBAR_Y));
        addDataSlots(data);
    }

    /** The viewer's access level ({@link TerminalRules}). */
    public int level() { return data.get(TradeTerminalBlockEntity.DATA_LEVEL); }

    @Nullable
    public TradeTerminalBlockEntity terminal() { return terminal; }

    /** The settlement container of this viewer. */
    public SimpleContainer settlementSlot() { return settlement; }

    private boolean shown() { return !viewer.level().isClientSide || bufferView || settleView; }

    /** The inventory and the settlement slot: on the buffer and the settlement page. */
    private class PageSlot extends Slot {
        PageSlot(Container c, int index, int x, int y) { super(c, index, x, y); }

        @Override
        public boolean isActive() { return shown(); }
    }

    /** The buffers: on the buffer page only. */
    private class BufferSlot extends PageSlot {
        BufferSlot(Container c, int index, int x, int y) { super(c, index, x, y); }

        @Override
        public boolean isActive() { return !viewer.level().isClientSide || bufferView; }
    }

    private final class SellSlot extends BufferSlot {
        SellSlot(Container c, int index, int x, int y) { super(c, index, x, y); }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return TerminalRules.may(level(), TerminalRules.Action.LOAD_BUFFER) && ExchangeService.sellableAs(stack) != null;
        }

        @Override
        public boolean mayPickup(Player player) { return TerminalRules.may(level(), TerminalRules.Action.TAKE_BUFFER); }
    }

    private final class OutputSlot extends BufferSlot {
        OutputSlot(Container c, int index, int x, int y) { super(c, index, x, y); }

        @Override
        public boolean mayPlace(ItemStack stack) { return false; }

        @Override
        public boolean mayPickup(Player player) { return TerminalRules.may(level(), TerminalRules.Action.TAKE_BUFFER); }
    }

    private final class SettleSlot extends PageSlot {
        SettleSlot(Container c, int x, int y) { super(c, 0, x, y); }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return TerminalRules.may(level(), TerminalRules.Action.SETTLE) && SettlementStorages.accepts(stack);
        }

        @Override
        public int getMaxStackSize() { return 1; }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= INV_END) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < INV_FIRST) {
            if (!moveItemStackTo(stack, INV_FIRST, INV_END, true)) return ItemStack.EMPTY;
        } else {
            Slot settle = slots.get(SETTLE);
            boolean moved = false;
            if (!settle.hasItem() && settle.mayPlace(stack)) moved = moveItemStackTo(stack, SETTLE, SETTLE + 1, false);
            // into the sell buffer only for a viewer who may take items out again (a shift-click of a trade-level player
            // would give the items to the owner by mistake; they put items in by clicking a slot)
            if (!moved && TerminalRules.may(level(), TerminalRules.Action.TAKE_BUFFER) && slots.get(SELL_FIRST).mayPlace(stack))
                moved = moveItemStackTo(stack, SELL_FIRST, OUTPUT_FIRST, false);
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (terminal == null) return player.level().isClientSide;
        if (!(player instanceof ServerPlayer) || !(terminal.getLevel() instanceof ServerLevel world) || player.level() != world
                || !player.isAlive() || player.isSpectator() || terminal.isRemoved()) return false;
        var pos = terminal.getBlockPos();
        if (world.getBlockEntity(pos) != terminal) return false;
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return false;
        // a viewer whose access was taken away loses the panel
        return TerminalRules.may(terminal.levelOf(player), TerminalRules.Action.OPEN);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer) {
            ItemStack held = settlement.removeItemNoUpdate(0);
            if (!held.isEmpty() && !player.getInventory().add(held) && !held.isEmpty()) player.drop(held, false);
        }
    }
}

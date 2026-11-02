// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.blockentity.producer.TradeBoxBlockEntity;
import com.miophas.singularity_iteration.common.exchange.ExchangeService;
import com.miophas.singularity_iteration.common.exchange.TradeBoxRules;
import com.miophas.singularity_iteration.common.exchange.TradeBoxes;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.42: the menu of a Trade Box. Slots 0..26 (basic) or 0..53 (advanced) the box, then the 9 return slots of the
 * advanced box (take out only), then the player inventory. Each tier has its own menu type (the open buffer carries the
 * position only, as for every menu of the mod); the filter is not a slot: the panel sets it with an action and the server
 * reads the item on the cursor.
 */
@SuppressWarnings("null")
public class TradeBoxMenu extends AbstractContainerMenu {
    /** Slot positions in the panel (the screen draws the frames there). */
    public static final int SELL_X = 8, SELL_Y = 18, INV_Y = 140, HOTBAR_Y = 198, RETURN_X = 260, RETURN_Y = 96;

    @Nullable private final TradeBoxBlockEntity box;
    private final int tier;
    public final int sellSlots, returnFirst, invFirst, invEnd;

    /** Client: a basic box (the open buffer carries the position). */
    public static TradeBoxMenu basic(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        buf.readBlockPos();
        return new TradeBoxMenu(id, inventory, null, TradeBoxRules.BASIC);
    }

    /** Client: an advanced box. */
    public static TradeBoxMenu advanced(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        buf.readBlockPos();
        return new TradeBoxMenu(id, inventory, null, TradeBoxRules.ADVANCED);
    }

    public TradeBoxMenu(int id, Inventory inventory, TradeBoxBlockEntity box) {
        this(id, inventory, box, box.tier());
    }

    private TradeBoxMenu(int id, Inventory inventory, @Nullable TradeBoxBlockEntity box, int tier) {
        super(tier == TradeBoxRules.ADVANCED ? mio_icif_menus.TRADE_BOX_ADVANCED_MENU_TYPE.get() : mio_icif_menus.TRADE_BOX_MENU_TYPE.get(), id);
        this.box = box;
        this.tier = tier;
        this.sellSlots = TradeBoxRules.slots(tier);
        Container sell = box != null ? box.sellContainer() : new SimpleContainer(sellSlots);
        Container back = box != null ? box.returnContainer() : new SimpleContainer(TradeBoxRules.RETURN_SLOTS);
        for (int i = 0; i < sellSlots; i++) addSlot(new SellSlot(sell, i, SELL_X + (i % 9) * 18, SELL_Y + (i / 9) * 18));
        this.returnFirst = slots.size();
        if (tier == TradeBoxRules.ADVANCED)
            for (int i = 0; i < TradeBoxRules.RETURN_SLOTS; i++) addSlot(new ReturnSlot(back, i, RETURN_X + (i % 3) * 18, RETURN_Y + (i / 3) * 18));
        this.invFirst = slots.size();
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, SELL_X + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, SELL_X + col * 18, HOTBAR_Y));
        this.invEnd = slots.size();
    }

    public int tier() { return tier; }

    public boolean advanced() { return tier == TradeBoxRules.ADVANCED; }

    @Nullable
    public TradeBoxBlockEntity box() { return box; }

    private final class SellSlot extends Slot {
        SellSlot(Container c, int index, int x, int y) { super(c, index, x, y); }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // the server checks the filter too (the client knows only that it is a commodity)
            return box != null ? TradeBoxes.accepts(tier, box.filter(), stack) : ExchangeService.sellableAs(stack) != null;
        }
    }

    private static final class ReturnSlot extends Slot {
        ReturnSlot(Container c, int index, int x, int y) { super(c, index, x, y); }

        @Override
        public boolean mayPlace(ItemStack stack) { return false; }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= invEnd) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < invFirst) {
            if (!moveItemStackTo(stack, invFirst, invEnd, true)) return ItemStack.EMPTY;
        } else {
            if (!slots.get(0).mayPlace(stack) || !moveItemStackTo(stack, 0, sellSlots, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (box == null) return player.level().isClientSide;
        if (!(player instanceof ServerPlayer) || !(box.getLevel() instanceof ServerLevel world) || player.level() != world
                || !player.isAlive() || player.isSpectator() || box.isRemoved()) return false;
        var pos = box.getBlockPos();
        if (world.getBlockEntity(pos) != box) return false;
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return false;
        return box.mayOpen(player);
    }
}

package com.miophas.singularity_iteration.common.menu.tool;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import com.miophas.singularity_iteration.common.exchange.DataPlan;
import com.miophas.singularity_iteration.common.exchange.ExchangeEvents;
import com.miophas.singularity_iteration.common.exchange.ExchangeHost;
import com.miophas.singularity_iteration.common.exchange.ExchangeService;
import com.miophas.singularity_iteration.common.item.tools.PortableFuturesTraderItem;
import com.miophas.singularity_iteration.common.menu.producer.FutureElcMenu;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

/**
 * 0.1.10.38: the futures panel of a Portable Futures Trader held in one hand. It is a {@link FutureElcMenu} (the same panel,
 * sell box and settlement slot) whose host is the item: it stays open while that hand holds a trader, pays the energy fee of
 * each action from the trader's charge and needs the player's data plan ({@link DataPlan}). The trader's own slot cannot be
 * moved while the panel is open.
 */
public class PortableFuturesMenu extends FutureElcMenu implements ExchangeHost {
    private final InteractionHand hand;
    private final Player player;
    private boolean trading;

    /** Client: the hand comes in the opening buffer. */
    public PortableFuturesMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readEnum(InteractionHand.class));
    }

    public PortableFuturesMenu(int containerId, Inventory inventory, InteractionHand hand) {
        super(mio_icif_tool_menus.PORTABLE_FUTURES_MENU.get(), containerId, inventory);
        this.hand = hand;
        this.player = inventory.player;
    }

    /** The trader in the menu's hand (EMPTY when it is gone). */
    private ItemStack trader() {
        ItemStack stack = player.getItemInHand(hand);
        return stack.getItem() instanceof PortableFuturesTraderItem ? stack : ItemStack.EMPTY;
    }

    private static int fee() { return Singularity_Iteration_Config.FUTURE_ENERGY_PER_TRADE.get(); }

    @Override
    public ExchangeHost host() { return this; }

    @Override
    public boolean stillValid(Player p) {
        return p == player && p.isAlive() && !p.isSpectator() && !trader().isEmpty();
    }

    @Override
    public boolean serves(ServerPlayer p) {
        return p == player && p.containerMenu == this && stillValid(p);
    }

    @Override
    public boolean hasEnoughEnergy() {
        ItemStack stack = trader();
        return !stack.isEmpty() && ((PortableFuturesTraderItem) stack.getItem()).hasEnoughEnergy(stack, fee());
    }

    @Override
    public boolean exchangeAction(ServerPlayer p, Supplier<ExchangeService.Result> action) {
        if (trading || !serves(p)) return false;
        trading = true;
        try {
            if (!DataPlan.ensure(p)) return refuse(p, "message.mio_icif.portable_trader.plan_unpaid", DataPlan.price(), DataPlan.days());
            int fee = fee();
            if (fee > 0 && !hasEnoughEnergy()) return refuse(p, "message.mio_icif.future.no_energy");
            var result = action.get();
            p.displayClientMessage(Component.translatable(result.key(), result.args()), true);
            ItemStack stack = trader();
            if (result.ok() && fee > 0 && !stack.isEmpty()) ((PortableFuturesTraderItem) stack.getItem()).consumeEnergy(stack, fee);
            if (result.ok()) p.inventoryMenu.broadcastChanges();
            return result.ok();
        } finally {
            trading = false;
            ExchangeEvents.flush(p.server);
        }
    }

    private static boolean refuse(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable(key, args), true);
        return false;
    }

    /** The trader's slot stays where it is: no pick-up, shift-click or number-key swap of it. */
    @Override
    public void clicked(int slotId, int button, ClickType type, Player p) {
        if (hand == InteractionHand.MAIN_HAND) {
            int selected = p.getInventory().selected;
            if (slotId == INVENTORY_FIRST + 27 + selected || type == ClickType.SWAP && button == selected) return;
        } else if (type == ClickType.SWAP && button == Inventory.SLOT_OFFHAND) {
            return;
        }
        super.clicked(slotId, button, type, p);
    }
}

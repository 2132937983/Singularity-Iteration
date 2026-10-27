// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import com.miophas.singularity_iteration.common.exchange.ExchangeEvents;
import com.miophas.singularity_iteration.common.exchange.ExchangeService;
import com.miophas.singularity_iteration.common.exchange.TerminalOrders;
import com.miophas.singularity_iteration.common.exchange.TerminalRules;
import com.miophas.singularity_iteration.common.exchange.TerminalRules.Order;
import com.miophas.singularity_iteration.common.exchange.TerminalRules.Status;
import com.miophas.singularity_iteration.common.future.MarketDayDriver;
import com.miophas.singularity_iteration.common.menu.producer.TradeTerminalMenu;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.runtime.processing.MachineActionOwner;
import com.mojang.authlib.GameProfile;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.27 (X2): the Trade Terminal (proposal B1-B4). A block of its placer (the owner, {@link MachineActionOwner}) that
 * trades for the owner while it is loaded:
 * <ul>
 *   <li>a <b>sell buffer</b> of 27 slots that pipes and hoppers fill with commodities (and items of a commodity's tag);
 *       pipes cannot take anything out of it;</li>
 *   <li>an <b>output buffer</b> of 9 slots for purchases when the output mode is on (pipes take them out); else the purchases
 *       go to the owner's mailbox;</li>
 *   <li>up to 8 <b>standing orders</b> ({@link TerminalOrders}): sell what is above a kept amount (into the daily auction or
 *       at once), buy up to a stock at a price limit;</li>
 *   <li>an <b>access list</b> ({@link TerminalRules#level}): view, trade, configure; members of the owner's team; operators.</li>
 * </ul>
 * Every trade is the owner's (UUID) under the rules of a manual trade; while the owner is offline only the daily auction
 * runs. The terminal advances the market day like a futures machine ({@link MarketDayDriver}). When it is broken the buffers go
 * to the owner's mailbox (plain stacks; others drop).
 */
@SuppressWarnings("null")
public class TradeTerminalBlockEntity extends AbstractEnergyBlockEntity {
    public static final int DATA_LEVEL = 0, DATA_COUNT = 1;
    public static final int MAX_LOG = 32;
    private static final String OWNER_KEY = "ActionOwner";
    /** The terminal has no legacy state: a missing owner is an invalid one (the terminal does nothing until placed by a player). */
    private static final GameProfile NO_LEGACY = new GameProfile(new UUID(0L, 0L), "[SI trade terminal]");

    private MachineActionOwner owner = MachineActionOwner.invalid();
    private final LinkedHashMap<UUID, Integer> access = new LinkedHashMap<>();
    private final Map<UUID, String> accessNames = new HashMap<>();
    private int teamLevel = TerminalRules.NONE;
    private boolean outputMode;
    private final List<Order> orders = new ArrayList<>();
    private final List<Status> statuses = new ArrayList<>();
    private final ArrayDeque<LogLine> log = new ArrayDeque<>();

    /** One line of the trade log ({@code kind}: {@link TerminalOrders#LOG_SOLD}, {@code LOG_AUCTION}, {@code LOG_BOUGHT}). */
    public record LogLine(long day, int kind, String item, long quantity, long coins) { }

    private final SimpleContainer sellBuffer = new Buffer(TerminalRules.SELL_SLOTS);
    private final SimpleContainer outputBuffer = new Buffer(TerminalRules.OUTPUT_SLOTS);
    private final IItemHandler handler = new Handler();

    private final class Buffer extends SimpleContainer {
        Buffer(int size) { super(size); }

        @Override
        public void setChanged() {
            super.setChanged();
            TradeTerminalBlockEntity.this.setChanged();
        }
    }

    public TradeTerminalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(pos, state, type, Singularity_Iteration_Config.FUTURE_ENERGY_CAPACITY.get(), Singularity_Iteration_Config.FUTURE_MAX_RECEIVE.get(), 100, CableTier.LV);
    }

    public TradeTerminalBlockEntity(BlockPos pos, BlockState state) {
        this(mio_icif_block_entities.TRADE_TERMINAL_ENTITY_TYPE.get(), pos, state);
    }

    /** Takes FE through the guarded input ledger, like the futures machine. */
    @Override
    public net.neoforged.neoforge.energy.IEnergyStorage scexFeCapability(@Nullable Direction side) {
        return scexFeBridge().port(side);
    }

    // ------------------------------------------------------------------ tick

    public static void tick(Level level, BlockPos pos, BlockState state, TradeTerminalBlockEntity be) {
        if (level.isClientSide) return;
        long capacity = Singularity_Iteration_Config.FUTURE_ENERGY_CAPACITY.get();
        long receive = Singularity_Iteration_Config.FUTURE_MAX_RECEIVE.get();
        if (be.energyStorage.getCapacity() != capacity || be.energyStorage.getMaxReceive() != receive) {
            be.energyStorage.setCapacity(capacity);
            be.energyStorage.setMaxReceive(receive);
            be.refreshRegistration();
            be.setChanged();
        }
        AbstractEnergyBlockEntity.tick(level, pos, state, be);
        if (!(level instanceof ServerLevel server)) return;
        // the market day (0.1.10.27: a terminal alone keeps the daily auction going)
        MarketDayDriver.check(server);
        if (TerminalRules.runsOn(server.getGameTime(), pos.asLong())) be.runOrders(server);
    }

    /** Runs the standing orders once (every {@link TerminalRules#RUN_TICKS} ticks, or at once from the panel). */
    public void runOrders(ServerLevel server) {
        if (!owner.canAct() || orders.isEmpty()) return;
        List<Status> result = TerminalOrders.run(server.getServer(), host(), List.copyOf(orders));
        statuses.clear();
        statuses.addAll(result);
        setChanged();
    }

    private TerminalOrders.Host host() {
        return new TerminalOrders.Host() {
            @Override public UUID ownerId() { return owner.uuid(); }
            @Override public String ownerName() { return owner.name(); }
            @Override public Container sellBuffer() { return sellBuffer; }
            @Override public Container outputBuffer() { return outputBuffer; }
            @Override public boolean outputMode() { return outputMode; }
            @Override public TerminalOrders.Fee reserveFee() { return TradeTerminalBlockEntity.this.reserveFee(); }
            @Override public void log(long day, int kind, String item, long quantity, long coins) { addLog(new LogLine(day, kind, item, quantity, coins)); }
            @Override public void overflow(ItemStack stack) { TradeTerminalBlockEntity.this.overflow(stack); }
        };
    }

    /** The energy fee of one trade, or null when the terminal lacks it (no fee when the config sets 0). */
    TerminalOrders.Fee reserveFee() {
        int fee = Singularity_Iteration_Config.FUTURE_ENERGY_PER_TRADE.get();
        if (fee <= 0) return new TerminalOrders.Fee() {
            @Override public void commit() { }
            @Override public void cancel() { }
        };
        var payment = energyStorage.scexReserveInternal(fee);
        if (payment == null) return null;
        return new TerminalOrders.Fee() {
            @Override public void commit() { if (payment.isPending()) payment.commit(); }
            @Override public void cancel() { if (payment.isPending()) payment.cancel(); }
        };
    }

    public boolean hasEnoughEnergy() {
        return energyStorage.getAmount() >= Singularity_Iteration_Config.FUTURE_ENERGY_PER_TRADE.get();
    }

    public long energy() { return energyStorage.getAmount(); }

    public long maxEnergy() { return energyStorage.getCapacity(); }

    /**
     * Runs a settlement action of a player at this terminal for the energy fee of a trade (as the futures machine's
     * {@code exchangeAction}); the result goes to the action bar.
     */
    public boolean exchangeAction(ServerPlayer player, java.util.function.Supplier<ExchangeService.Result> action) {
        TerminalOrders.Fee fee = reserveFee();
        if (fee == null) {
            player.displayClientMessage(Component.translatable("message.mio_icif.future.no_energy"), true);
            return false;
        }
        boolean ok = false;
        try {
            var result = action.get();
            ok = result.ok();
            player.displayClientMessage(Component.translatable(result.key(), result.args()), true);
            if (ok) { setChanged(); player.inventoryMenu.broadcastChanges(); }
            return ok;
        } finally {
            if (ok) fee.commit(); else fee.cancel();
            ExchangeEvents.flush(player.server);
        }
    }

    // ------------------------------------------------------------------ owner and access

    public void setOwnerFromPlacer(@Nullable LivingEntity placer) {
        owner = MachineActionOwner.fromPlacer(placer);
        setChanged();
    }

    public MachineActionOwner owner() { return owner; }

    /** The level of {@code player} at this terminal ({@link TerminalRules#level}). */
    public int levelOf(Player player) {
        if (!(player instanceof ServerPlayer sp) || !owner.canAct()) return player != null && player.hasPermissions(2) ? TerminalRules.CONFIG : TerminalRules.NONE;
        boolean sameTeam = false;
        if (teamLevel > TerminalRules.NONE && sp.getTeam() != null) {
            String ownerTeam = ExchangeService.team(sp.server, owner.name());
            sameTeam = !ownerTeam.isEmpty() && ownerTeam.equals(sp.getTeam().getName());
        }
        int level = TerminalRules.level(sp.getUUID(), owner.uuid(), access, sameTeam, teamLevel, sp.hasPermissions(2));
        // 0.1.10.28 (X4): a valid delegate ID card of the owner carried by the player
        if (level < TerminalRules.CONFIG) level = Math.max(level, com.miophas.singularity_iteration.common.exchange.IdCards.delegatedLevel(sp, owner.uuid()));
        return level;
    }

    public Map<UUID, Integer> access() { return Collections.unmodifiableMap(access); }

    public String accessName(UUID id) { return accessNames.getOrDefault(id, id.toString().substring(0, 8)); }

    /** Sets the level of a player (NONE removes); false when the list is full or the player is the owner. */
    public boolean setAccess(UUID id, String name, int level) {
        if (id == null || id.equals(owner.uuid())) return false;
        if (level <= TerminalRules.NONE) {
            access.remove(id);
            accessNames.remove(id);
        } else {
            if (!access.containsKey(id) && access.size() >= TerminalRules.MAX_ACCESS) return false;
            access.put(id, Math.min(TerminalRules.CONFIG, level));
            accessNames.put(id, name);
        }
        setChanged();
        return true;
    }

    public int teamLevel() { return teamLevel; }

    public void setTeamLevel(int level) {
        teamLevel = Math.max(TerminalRules.NONE, Math.min(TerminalRules.CONFIG, level));
        setChanged();
    }

    // ------------------------------------------------------------------ orders, output mode, log

    public List<Order> orders() { return Collections.unmodifiableList(orders); }

    public Status status(int index) { return index < statuses.size() ? statuses.get(index) : null; }

    /** Replaces order {@code index}, or adds one ({@code index} -1); false when there are {@link TerminalRules#MAX_ORDERS}. */
    public boolean setOrder(int index, Order order) {
        if (index >= 0 && index < orders.size()) orders.set(index, order);
        else if (orders.size() < TerminalRules.MAX_ORDERS) orders.add(order);
        else return false;
        statuses.clear();
        setChanged();
        return true;
    }

    public void removeOrder(int index) {
        if (index < 0 || index >= orders.size()) return;
        orders.remove(index);
        statuses.clear();
        setChanged();
    }

    public boolean outputMode() { return outputMode; }

    public void setOutputMode(boolean on) {
        outputMode = on;
        setChanged();
    }

    public List<LogLine> log() { return List.copyOf(log); }

    void addLog(LogLine line) {
        log.addLast(line);
        while (log.size() > MAX_LOG) log.removeFirst();
    }

    // ------------------------------------------------------------------ buffers

    public SimpleContainer sellBuffer() { return sellBuffer; }

    public SimpleContainer outputBuffer() { return outputBuffer; }

    public IItemHandler getItemHandlerCapability(@Nullable Direction side) { return handler; }

    /** Pipes and hoppers: insert commodities into the sell buffer (slots 0-26), extract purchases from the output buffer (27-35). */
    private final class Handler implements IItemHandler {
        @Override
        public int getSlots() { return TerminalRules.SELL_SLOTS + TerminalRules.OUTPUT_SLOTS; }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;
            return slot < TerminalRules.SELL_SLOTS ? sellBuffer.getItem(slot) : outputBuffer.getItem(slot - TerminalRules.SELL_SLOTS);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || slot < 0 || slot >= TerminalRules.SELL_SLOTS || ExchangeService.sellableAs(stack) == null) return stack;
            ItemStack cur = sellBuffer.getItem(slot);
            if (!cur.isEmpty() && !ItemStack.isSameItemSameComponents(cur, stack)) return stack;
            int limit = Math.min(sellBuffer.getMaxStackSize(), stack.getMaxStackSize());
            int room = limit - cur.getCount();
            if (room <= 0) return stack;
            int n = Math.min(room, stack.getCount());
            if (!simulate) {
                if (cur.isEmpty()) sellBuffer.setItem(slot, stack.copyWithCount(n)); else cur.grow(n);
                sellBuffer.setChanged();
            }
            return n == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - n);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || slot < TerminalRules.SELL_SLOTS || slot >= getSlots()) return ItemStack.EMPTY;
            int i = slot - TerminalRules.SELL_SLOTS;
            ItemStack cur = outputBuffer.getItem(i);
            if (cur.isEmpty()) return ItemStack.EMPTY;
            int n = Math.min(amount, Math.min(cur.getCount(), cur.getMaxStackSize()));
            ItemStack out = cur.copyWithCount(n);
            if (!simulate) {
                cur.shrink(n);
                if (cur.isEmpty()) outputBuffer.setItem(i, ItemStack.EMPTY);
                outputBuffer.setChanged();
            }
            return out;
        }

        @Override
        public int getSlotLimit(int slot) { return 64; }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= 0 && slot < TerminalRules.SELL_SLOTS && ExchangeService.sellableAs(stack) != null;
        }
    }

    /** A stack that fits nowhere: the owner's mailbox when plain, else dropped at the terminal. */
    void overflow(ItemStack stack) {
        if (stack.isEmpty()) return;
        if (level instanceof ServerLevel server && owner.canAct() && ExchangeService.mailStack(server.getServer(), owner.uuid(), stack)) return;
        if (level != null) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, stack);
    }

    /** The block is broken: the buffers go to the owner's mailbox (plain stacks), the rest drops. */
    public void emptyBuffers() {
        for (SimpleContainer c : List.of(sellBuffer, outputBuffer)) {
            for (int i = 0; i < c.getContainerSize(); i++) {
                ItemStack s = c.removeItemNoUpdate(i);
                if (!s.isEmpty()) overflow(s);
            }
        }
        setChanged();
    }

    // ------------------------------------------------------------------ menu

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TradeTerminalMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.trade_terminal");
    }

    /** The menu data of one viewer: their access level. */
    public net.minecraft.world.inventory.ContainerData viewerData(Player viewer) {
        return new net.minecraft.world.inventory.ContainerData() {
            @Override public int get(int index) { return index == DATA_LEVEL ? levelOf(viewer) : 0; }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return DATA_COUNT; }
        };
    }

    /** The owner's name for the panel. */
    public String ownerName() { return owner.canAct() ? owner.name() : ""; }

    /** True when the owner is online. */
    public boolean ownerOnline(MinecraftServer server) { return owner.canAct() && server.getPlayerList().getPlayer(owner.uuid()) != null; }

    // ------------------------------------------------------------------ save

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put(OWNER_KEY, owner.save());
        ListTag acc = new ListTag();
        access.forEach((id, lv) -> {
            CompoundTag a = new CompoundTag();
            a.putUUID("id", id);
            a.putString("name", accessNames.getOrDefault(id, ""));
            a.putInt("level", lv);
            acc.add(a);
        });
        tag.put("Access", acc);
        tag.putInt("TeamLevel", teamLevel);
        tag.putBoolean("OutputMode", outputMode);
        ListTag ord = new ListTag();
        for (Order o : orders) {
            CompoundTag t = new CompoundTag();
            t.putString("side", o.side().name());
            t.putString("item", o.item());
            t.putLong("amount", o.amount());
            t.putString("mode", o.mode().name());
            t.putLong("limit", o.limit());
            t.putBoolean("enabled", o.enabled());
            ord.add(t);
        }
        tag.put("Orders", ord);
        ListTag lg = new ListTag();
        for (LogLine l : log) {
            CompoundTag t = new CompoundTag();
            t.putLong("day", l.day());
            t.putInt("kind", l.kind());
            t.putString("item", l.item());
            t.putLong("qty", l.quantity());
            t.putLong("coins", l.coins());
            lg.add(t);
        }
        tag.put("Log", lg);
        tag.put("Sell", ContainerHelper.saveAllItems(new CompoundTag(), sellBuffer.getItems(), provider));
        tag.put("Output", ContainerHelper.saveAllItems(new CompoundTag(), outputBuffer.getItems(), provider));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        owner = tag.contains(OWNER_KEY) ? MachineActionOwner.load(tag, OWNER_KEY, NO_LEGACY) : MachineActionOwner.invalid();
        if (owner.kind() == MachineActionOwner.Kind.LEGACY) owner = MachineActionOwner.invalid();
        access.clear();
        accessNames.clear();
        for (Tag t : tag.getList("Access", Tag.TAG_COMPOUND)) {
            CompoundTag a = (CompoundTag) t;
            if (!a.hasUUID("id") || access.size() >= TerminalRules.MAX_ACCESS) continue;
            int lv = Math.max(TerminalRules.NONE, Math.min(TerminalRules.CONFIG, a.getInt("level")));
            if (lv <= TerminalRules.NONE) continue;
            access.put(a.getUUID("id"), lv);
            accessNames.put(a.getUUID("id"), a.getString("name"));
        }
        teamLevel = Math.max(TerminalRules.NONE, Math.min(TerminalRules.CONFIG, tag.getInt("TeamLevel")));
        outputMode = tag.getBoolean("OutputMode");
        orders.clear();
        statuses.clear();
        for (Tag t : tag.getList("Orders", Tag.TAG_COMPOUND)) {
            if (orders.size() >= TerminalRules.MAX_ORDERS) break;
            CompoundTag o = (CompoundTag) t;
            try {
                orders.add(new Order(TerminalRules.Side.valueOf(o.getString("side")), o.getString("item"), o.getLong("amount"),
                    TerminalRules.Mode.valueOf(o.getString("mode")), o.getLong("limit"), o.getBoolean("enabled")));
            } catch (IllegalArgumentException ignored) {
                // an order of an unknown form is dropped
            }
        }
        log.clear();
        for (Tag t : tag.getList("Log", Tag.TAG_COMPOUND)) {
            CompoundTag l = (CompoundTag) t;
            addLog(new LogLine(l.getLong("day"), l.getInt("kind"), l.getString("item"), l.getLong("qty"), l.getLong("coins")));
        }
        sellBuffer.clearContent();
        outputBuffer.clearContent();
        ContainerHelper.loadAllItems(tag.getCompound("Sell"), sellBuffer.getItems(), provider);
        ContainerHelper.loadAllItems(tag.getCompound("Output"), outputBuffer.getItems(), provider);
    }
}

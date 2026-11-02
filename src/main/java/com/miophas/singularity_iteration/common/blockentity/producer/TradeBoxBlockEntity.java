// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.block.producer.mio_icif_block_trade_box;
import com.miophas.singularity_iteration.common.exchange.ExchangeService;
import com.miophas.singularity_iteration.common.exchange.TerminalOrders;
import com.miophas.singularity_iteration.common.exchange.TerminalRules;
import com.miophas.singularity_iteration.common.exchange.TerminalRules.Status;
import com.miophas.singularity_iteration.common.exchange.TradeBoxRules;
import com.miophas.singularity_iteration.common.exchange.TradeBoxes;
import com.miophas.singularity_iteration.common.future.MarketDayDriver;
import com.miophas.singularity_iteration.common.menu.producer.TradeBoxMenu;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.runtime.processing.MachineActionOwner;
import com.mojang.authlib.GameProfile;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.42: the Basic and the Advanced Trade Box ({@link TradeBoxRules}, {@link TradeBoxes}). A box of its placer (the owner)
 * that sells the commodities hoppers and pipes put into it for the owner while it is loaded, every
 * {@link TerminalRules#RUN_TICKS} ticks; it advances the market day like a Trade Terminal. Only the owner and operators open
 * it. When it is broken its contents go to the owner's mailbox (plain stacks; others drop). The upgrade kit turns a placed
 * basic box into an advanced one with its contents ({@link #upgrading}).
 */
@SuppressWarnings("null")
public class TradeBoxBlockEntity extends BlockEntity implements MenuProvider {
    private static final String OWNER_KEY = "ActionOwner";
    private static final GameProfile NO_LEGACY = new GameProfile(new UUID(0L, 0L), "[SI trade box]");

    private final int tier;
    private MachineActionOwner owner = MachineActionOwner.invalid();
    private final SimpleContainer sell;
    private final SimpleContainer returns = new Buffer(TradeBoxRules.RETURN_SLOTS);
    private final String[] filter = new String[TradeBoxRules.FILTER_SLOTS];
    private TradeBoxRules.Settings settings = TradeBoxRules.Settings.DEFAULT;
    @Nullable private Status lastStatus;
    private long counterDay = Long.MIN_VALUE;
    private final long[] counters = new long[TradeBoxRules.COUNTERS];
    private final IItemHandler handler = new Handler();
    /** Set while the upgrade kit replaces the block: the contents move to the advanced box instead of the mailbox. */
    public boolean upgrading;

    private final class Buffer extends SimpleContainer {
        Buffer(int size) { super(size); }

        @Override
        public void setChanged() {
            super.setChanged();
            TradeBoxBlockEntity.this.setChanged();
        }
    }

    public TradeBoxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.tier = state.getBlock() instanceof mio_icif_block_trade_box b && b.advanced() ? TradeBoxRules.ADVANCED : TradeBoxRules.BASIC;
        this.sell = new Buffer(TradeBoxRules.slots(tier));
        Arrays.fill(filter, "");
    }

    public TradeBoxBlockEntity(BlockPos pos, BlockState state) {
        this(mio_icif_block_entities.TRADE_BOX_ENTITY_TYPE.get(), pos, state);
    }

    // ------------------------------------------------------------------ tick

    public static void tick(Level level, BlockPos pos, BlockState state, TradeBoxBlockEntity be) {
        if (!(level instanceof ServerLevel server)) return;
        if (!TerminalRules.runsOn(server.getGameTime(), pos.asLong())) return;
        // the market day (a box alone keeps the daily auction going, like a Trade Terminal)
        MarketDayDriver.check(server);
        be.run(server);
    }

    /** Runs the box once (every {@link TerminalRules#RUN_TICKS} ticks). */
    public void run(ServerLevel server) {
        if (!owner.canAct()) return;
        boolean empty = sell.isEmpty();
        Status s = TradeBoxes.run(server.getServer(), host());
        if (empty && s == lastStatus) return;
        lastStatus = s;
        setChanged();
    }

    private TradeBoxes.Box host() {
        return new TradeBoxes.Box() {
            @Override public UUID ownerId() { return owner.uuid(); }
            @Override public String ownerName() { return owner.name(); }
            @Override public Container sellBuffer() { return sell; }
            @Override public Container outputBuffer() { return returns; }
            @Override public boolean outputMode() { return tier == TradeBoxRules.ADVANCED; }
            @Override public TerminalOrders.Fee reserveFee() { return TradeBoxes.FREE; }
            @Override public void log(long day, int kind, String item, long quantity, long coins) {
                int c = TradeBoxRules.counter(kind);
                if (c < 0) return;
                count(c, quantity);
                if (c == TradeBoxRules.SOLD) count(TradeBoxRules.COINS, coins);
            }
            @Override public void overflow(ItemStack stack) { TradeBoxBlockEntity.this.overflow(stack); }
            @Override public int tier() { return tier; }
            @Override public TradeBoxRules.Settings settings() { return settings; }
            @Override public List<String> filter() { return TradeBoxBlockEntity.this.filter(); }
            @Override public void count(int counter, long amount) {
                if (!(level instanceof ServerLevel sl)) return;
                counters(ExchangeService.today(sl.getServer()));
                counters[counter] += amount;
            }
        };
    }

    // ------------------------------------------------------------------ owner, settings, status

    public int tier() { return tier; }

    public boolean advanced() { return tier == TradeBoxRules.ADVANCED; }

    public void setOwnerFromPlacer(@Nullable LivingEntity placer) {
        owner = MachineActionOwner.fromPlacer(placer);
        setChanged();
    }

    public MachineActionOwner owner() { return owner; }

    public String ownerName() { return owner.canAct() ? owner.name() : ""; }

    public boolean ownerOnline(MinecraftServer server) { return owner.canAct() && server.getPlayerList().getPlayer(owner.uuid()) != null; }

    /** The owner and operators open the box (a box without a valid owner: operators only). */
    public boolean mayOpen(Player player) {
        if (player == null) return false;
        if (player.hasPermissions(2)) return true;
        return owner.canAct() && player.getUUID().equals(owner.uuid());
    }

    public TradeBoxRules.Settings settings() { return settings; }

    public void setSettings(TradeBoxRules.Settings s) {
        settings = TradeBoxRules.of(tier, s);
        setChanged();
    }

    public List<String> filter() { return List.of(filter); }

    public void setFilter(int index, String itemId) {
        if (!advanced() || index < 0 || index >= filter.length) return;
        filter[index] = itemId == null ? "" : itemId;
        setChanged();
    }

    public void clearFilter() {
        Arrays.fill(filter, "");
        setChanged();
    }

    @Nullable
    public Status lastStatus() { return lastStatus; }

    /** The counters of market day {@code day} (cleared on a new day). */
    public long[] counters(long day) {
        counterDay = TradeBoxRules.roll(counters, counterDay, day);
        return counters.clone();
    }

    // ------------------------------------------------------------------ contents

    public SimpleContainer sellContainer() { return sell; }

    public SimpleContainer returnContainer() { return returns; }

    public IItemHandler getItemHandlerCapability(@Nullable Direction side) { return handler; }

    /** Pipes and hoppers insert commodities (the filter's, advanced) into the box and take items out of the return slots. */
    private final class Handler implements IItemHandler {
        @Override
        public int getSlots() { return sell.getContainerSize() + (advanced() ? TradeBoxRules.RETURN_SLOTS : 0); }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;
            return slot < sell.getContainerSize() ? sell.getItem(slot) : returns.getItem(slot - sell.getContainerSize());
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || slot < 0 || slot >= sell.getContainerSize() || !TradeBoxes.accepts(tier, filter(), stack)) return stack;
            ItemStack cur = sell.getItem(slot);
            if (!cur.isEmpty() && !ItemStack.isSameItemSameComponents(cur, stack)) return stack;
            int limit = Math.min(sell.getMaxStackSize(), stack.getMaxStackSize());
            int room = limit - cur.getCount();
            if (room <= 0) return stack;
            int n = Math.min(room, stack.getCount());
            if (!simulate) {
                if (cur.isEmpty()) sell.setItem(slot, stack.copyWithCount(n)); else cur.grow(n);
                sell.setChanged();
            }
            return n == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - n);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || !advanced() || slot < sell.getContainerSize() || slot >= getSlots()) return ItemStack.EMPTY;
            int i = slot - sell.getContainerSize();
            ItemStack cur = returns.getItem(i);
            if (cur.isEmpty()) return ItemStack.EMPTY;
            int n = Math.min(amount, Math.min(cur.getCount(), cur.getMaxStackSize()));
            ItemStack out = cur.copyWithCount(n);
            if (!simulate) {
                cur.shrink(n);
                if (cur.isEmpty()) returns.setItem(i, ItemStack.EMPTY);
                returns.setChanged();
            }
            return out;
        }

        @Override
        public int getSlotLimit(int slot) { return 64; }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= 0 && slot < sell.getContainerSize() && TradeBoxes.accepts(tier, filter(), stack);
        }
    }

    /** A stack that fits nowhere: the owner's mailbox when plain, else dropped at the box. */
    void overflow(ItemStack stack) {
        if (stack.isEmpty()) return;
        if (level instanceof ServerLevel server && owner.canAct() && ExchangeService.mailStack(server.getServer(), owner.uuid(), stack)) return;
        if (level != null) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, stack);
    }

    /** The block is broken: the contents go to the owner's mailbox (plain stacks), the rest drops; not while it is upgraded. */
    public void emptyContents() {
        if (upgrading) return;
        for (SimpleContainer c : List.of(sell, returns)) {
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
        return new TradeBoxMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(advanced() ? "container.mio_icif.trade_box_advanced" : "container.mio_icif.trade_box");
    }

    /** Opens the panel for {@code player} (the menu type of the tier; the buffer carries the position). */
    public void open(ServerPlayer player) {
        player.openMenu(this, buf -> buf.writeBlockPos(worldPosition));
    }

    // ------------------------------------------------------------------ save

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put(OWNER_KEY, owner.save());
        CompoundTag s = new CompoundTag();
        s.putInt("Mode", settings.mode());
        s.putInt("Percent", settings.percent());
        s.putLong("Coins", settings.coins());
        s.putBoolean("InCoins", settings.inCoins());
        s.putBoolean("AutoDeliver", settings.autoDeliver());
        s.putBoolean("OfflineAuction", settings.offlineAuction());
        tag.put("Settings", s);
        ListTag f = new ListTag();
        for (String id : filter) f.add(StringTag.valueOf(id));
        tag.put("Filter", f);
        if (lastStatus != null) tag.putString("Status", lastStatus.name());
        if (counterDay != Long.MIN_VALUE) {
            tag.putLong("CounterDay", counterDay);
            tag.putLongArray("Counters", counters);
        }
        tag.put("Sell", ContainerHelper.saveAllItems(new CompoundTag(), sell.getItems(), provider));
        tag.put("Returns", ContainerHelper.saveAllItems(new CompoundTag(), returns.getItems(), provider));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        owner = tag.contains(OWNER_KEY) ? MachineActionOwner.load(tag, OWNER_KEY, NO_LEGACY) : MachineActionOwner.invalid();
        if (owner.kind() == MachineActionOwner.Kind.LEGACY) owner = MachineActionOwner.invalid();
        CompoundTag s = tag.getCompound("Settings");
        settings = s.isEmpty() ? TradeBoxRules.of(tier, TradeBoxRules.Settings.DEFAULT)
            : TradeBoxRules.of(tier, new TradeBoxRules.Settings(s.getInt("Mode"), s.getInt("Percent"), s.getLong("Coins"), s.getBoolean("InCoins"),
                s.getBoolean("AutoDeliver"), s.getBoolean("OfflineAuction")));
        Arrays.fill(filter, "");
        ListTag f = tag.getList("Filter", Tag.TAG_STRING);
        for (int i = 0; i < f.size() && i < filter.length; i++) filter[i] = f.getString(i).length() <= 128 ? f.getString(i) : "";
        lastStatus = null;
        if (tag.contains("Status")) {
            try { lastStatus = Status.valueOf(tag.getString("Status")); } catch (IllegalArgumentException ignored) { }
        }
        Arrays.fill(counters, 0);
        counterDay = tag.contains("CounterDay") ? tag.getLong("CounterDay") : Long.MIN_VALUE;
        long[] saved = tag.getLongArray("Counters");
        for (int i = 0; i < saved.length && i < counters.length; i++) counters[i] = Math.max(0, saved[i]);
        sell.clearContent();
        returns.clearContent();
        // a basic box's 27 slots load into the first slots of an upgraded box; slots past the size are dropped by the helper
        ContainerHelper.loadAllItems(tag.getCompound("Sell"), sell.getItems(), provider);
        ContainerHelper.loadAllItems(tag.getCompound("Returns"), returns.getItems(), provider);
    }

    /** The contents and settings as a tag for the upgraded box (the save of this box). */
    public CompoundTag upgradeTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
}

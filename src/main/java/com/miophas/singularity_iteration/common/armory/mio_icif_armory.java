package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * Armory: powered suit storage that flies its suits to its owner.
 *
 * <p>Six suits of six pieces (helmet, chestplate, leggings, boots, main hand, off hand).
 * The first player to open a <b>powered</b> Armory becomes its owner; afterwards only the
 * owner can open it or summon from it (operators can always open it).
 *
 * <p>A summon pays its EU up front, then launches one {@link ArmoryPieceEntity} per stored
 * piece. Each piece owns its item while in flight; on docking it is equipped and the piece
 * the player was wearing goes back into the same suit position (it flies home as a visual
 * echo). Nothing is ever destroyed: if anything goes wrong the item lands back in the
 * Armory, in the player's inventory or on the ground, in that order.
 */
@SuppressWarnings("null")
public class mio_icif_armory extends AbstractProcessingMachineBlockEntity {

    public static final int SETS = 6;
    public static final int PIECES = 6;
    public static final int BATTERY_SLOT = 0;
    public static final int FIRST_SUIT_SLOT = 1;
    public static final int SLOT_COUNT = FIRST_SUIT_SLOT + SETS * PIECES;

    public static final long CAPACITY = 1_000_000L;
    public static final long MAX_RECEIVE = 512L;
    /** Stored EU needed before an Armory accepts an owner (it must be powered). */
    public static final long BIND_ENERGY = 1_000L;
    public static final long COST_PER_PIECE = 400L;
    public static final long COST_PER_PIECE_PER_BLOCK = 8L;
    public static final long COST_PER_PIECE_CROSS_DIMENSION = 6_000L;
    public static final int LOG_SIZE = 16;
    public static final int NAME_LENGTH = 16;

    /** Botania mana tank (a Mana Pool holds 1,000,000). */
    public static final int MANA_CAPACITY = 500_000;

    /**
     * What pipes, hoppers and AE2 storage/import/export buses may do with the stored suits.
     * The battery slot is always reachable.
     */
    public enum Automation {
        /** Suits are sealed: nothing goes in or out. */
        LOCKED,
        /** Gear may be deposited into free, matching suit slots; nothing is taken out. */
        DEPOSIT,
        /** Full access: storage buses see and can take stored gear. */
        OPEN;
        public Automation next() { return values()[(ordinal() + 1) % values().length]; }
    }

    private static final SlotLayout LAYOUT = SlotLayout.builder().battery().extra(SETS * PIECES).build();

    @Nullable private UUID owner;
    private String ownerName = "";
    private final String[] setNames = new String[SETS];
    private final Deque<ArmoryLogEntry> log = new ArrayDeque<>();
    private long busyUntil;
    private Automation automation = Automation.LOCKED;
    private int mana;
    private final ArmoryMaintenance maintenance = new ArmoryMaintenance(this);

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            long energy = getEnergyStorage().getAmount();
            return switch (index) {
                case 0 -> (int) (energy & 0xFFFF);
                case 1 -> (int) ((energy >>> 16) & 0xFFFF);
                case 2 -> (int) (energy >>> 32);
                case 3 -> owner != null ? 1 : 0;
                case 4 -> isBusy() ? 1 : 0;
                case 5 -> automation.ordinal();
                case 6 -> mana;
                case 7 -> maintenance.state();
                case 8 -> maintenance.etaTicks();
                case 9 -> maintenance.pendingPieces();
                case 10 -> (int) Math.min(Integer.MAX_VALUE, maintenance.euNeeded());
                case 11 -> maintenance.manaNeeded();
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return ArmoryMenu.DATA_COUNT; }
    };

    public mio_icif_armory(BlockPos pos, BlockState state) {
        this(ArmoryRegistry.ARMORY_ENTITY.get(), pos, state);
    }

    public mio_icif_armory(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(pos, state, type, CAPACITY, MAX_RECEIVE, 0L, 100, LAYOUT, 0L, CableTier.HV);
        for (int i = 0; i < SETS; i++) setNames[i] = defaultName(i);
    }

    public static String defaultName(int set) {
        return "Mark " + "I II III IV V VI".split(" ")[set];
    }

    public static int slotOf(int set, ArmoryPiece piece) {
        return FIRST_SUIT_SLOT + set * PIECES + piece.column();
    }

    // ------------------------------------------------------------------ ownership
    @Nullable public UUID owner() { return owner; }
    public String ownerName() { return ownerName; }
    public boolean isOwner(Player player) { return owner != null && owner.equals(player.getUUID()); }

    public boolean mayAccess(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    /** Binds an unowned, powered Armory to the player. Returns the message to show, or null on silent success. */
    @Nullable
    public Component tryBind(Player player) {
        if (owner != null) return isOwner(player) ? null : Component.translatable("message.mio_icif.armory.owned", ownerName);
        if (getEnergyStorage().getAmount() < BIND_ENERGY) return Component.translatable("message.mio_icif.armory.unpowered", BIND_ENERGY);
        owner = player.getUUID();
        ownerName = player.getGameProfile().getName();
        setChanged();
        sync();
        return Component.translatable("message.mio_icif.armory.bound", ownerName);
    }

    public void unbind(Player player) {
        if (!isOwner(player) && !player.hasPermissions(2)) return;
        owner = null;
        ownerName = "";
        setChanged();
        sync();
    }

    // ------------------------------------------------------------------ suits
    public String setName(int set) { return setNames[Math.floorMod(set, SETS)]; }

    public void rename(Player player, int set, String name) {
        if (!isOwner(player) || set < 0 || set >= SETS) return;
        String clean = name == null ? "" : name.strip();
        if (clean.length() > NAME_LENGTH) clean = clean.substring(0, NAME_LENGTH);
        setNames[set] = clean.isEmpty() ? defaultName(set) : clean;
        setChanged();
        sync();
    }

    public ItemStack piece(int set, ArmoryPiece piece) {
        return getItemHandler().getStackInSlot(slotOf(set, piece));
    }

    public int pieceCount(int set) {
        int n = 0;
        for (ArmoryPiece p : ArmoryPiece.VALUES) if (!piece(set, p).isEmpty()) n++;
        return n;
    }

    public boolean isBusy() {
        return level != null && level.getGameTime() < busyUntil;
    }

    public List<ArmoryLogEntry> log() { return List.copyOf(log); }

    // ------------------------------------------------------------------ automation access
    public Automation automation() { return automation; }

    public void cycleAutomation(Player player) {
        if (!isOwner(player) && !player.hasPermissions(2)) return;
        automation = automation.next();
        setChanged();
        invalidateCapabilities();
    }

    // ------------------------------------------------------------------ energy, mana, maintenance
    public long storedEnergy() { return getEnergyStorage().getAmount(); }

    void spendEnergy(long eu) {
        if (eu > 0) getEnergyStorageInternal().consumeEnergyInternal(eu, false);
    }

    public int mana() { return mana; }

    /** Botania ManaReceiver entry point (bursts add, a negative value takes mana away). */
    public void receiveMana(int amount) {
        int next = (int) Math.max(0, Math.min(MANA_CAPACITY, (long) mana + amount));
        if (next != mana) {
            mana = next;
            setChanged();
        }
    }

    void spendMana(int amount) {
        mana = Math.max(0, mana - Math.max(0, amount));
    }

    void markStorageChanged() { setChanged(); }

    public ArmoryMaintenance maintenance() { return maintenance; }

    /** EU a summon of {@code pieces} pieces costs over the given straight distance. */
    public static long cost(int pieces, double straightDistance, boolean sameDimension) {
        if (pieces <= 0) return 0;
        if (!sameDimension) return pieces * COST_PER_PIECE_CROSS_DIMENSION;
        return pieces * (COST_PER_PIECE + Math.round(COST_PER_PIECE_PER_BLOCK * Math.max(0, straightDistance)));
    }

    public Vec3 launchTop() {
        return Vec3.atCenterOf(worldPosition).add(0, 0.7, 0);
    }

    /** Outcome of a summon request. */
    public record SummonResult(boolean ok, Component message, long cost) {
        static SummonResult fail(String key, Object... args) { return new SummonResult(false, Component.translatable(key, args), 0); }
    }

    /** Fly suit {@code set} to the player. Server side only. */
    public SummonResult summon(ServerPlayer player, int set) {
        if (!(level instanceof ServerLevel world)) return SummonResult.fail("message.mio_icif.armory.failed");
        if (set < 0 || set >= SETS) return SummonResult.fail("message.mio_icif.armory.failed");
        if (!isOwner(player)) return SummonResult.fail("message.mio_icif.armory.not_owner");
        if (isBusy()) return SummonResult.fail("message.mio_icif.armory.busy");
        if (!player.isAlive() || player.isSpectator()) return SummonResult.fail("message.mio_icif.armory.failed");

        List<ArmoryPiece> flying = new ArrayList<>();
        for (ArmoryPiece p : ArmoryFlight.SEQUENCE) {
            ItemStack stored = piece(set, p);
            if (stored.isEmpty()) continue;
            ItemStack worn = player.getItemBySlot(p.slot);
            if (!worn.isEmpty() && !player.isCreative()
                    && EnchantmentHelper.has(worn, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) continue;
            flying.add(p);
        }
        if (flying.isEmpty()) return SummonResult.fail("message.mio_icif.armory.empty", setName(set));

        boolean sameDimension = player.level() == world;
        double straight = sameDimension ? player.position().distanceTo(Vec3.atCenterOf(worldPosition)) : -1;
        long cost = cost(flying.size(), straight, sameDimension);
        var payment = getEnergyStorageInternal().scexReserveInternal(cost);
        if (payment == null) return SummonResult.fail("message.mio_icif.armory.no_energy", cost, getEnergyStorage().getAmount());
        payment.commit();

        ServerLevel playerLevel = (ServerLevel) player.level();
        Vec3 feet = player.position();
        long latestArrival = 0;
        double flight = 0;
        int order = 0;
        for (ArmoryPiece p : flying) {
            int slot = slotOf(set, p);
            ItemStack item = getItemHandler().extractItem(slot, 64, false);
            if (item.isEmpty()) continue;
            Vec3 dock = ArmoryFlight.dockPoint(feet, player.getBbHeight(), player.yBodyRot, p);
            Vec3 hold = ArmoryFlight.holdPoint(dock, player.yBodyRot);
            Vec3 start = ArmoryFlight.launchPoint(dock, launchTop(), sameDimension, p);
            double length = ArmoryFlight.approachLength(start, hold, ArmoryFlight.forward(player.yBodyRot), p);
            int duration = ArmoryFlight.ticksFor(length);
            // warning + hatch first, then one piece every STAGGER ticks
            int delay = ArmoryFlight.SEQUENCE_LEAD + order++ * ArmoryFlight.STAGGER;
            flight = Math.max(flight, length);
            ArmoryPieceEntity entity = ArmoryPieceEntity.deliver(playerLevel, player, item, p, start,
                GlobalPos.of(world.dimension(), worldPosition), set, delay, duration);
            playerLevel.addFreshEntity(entity);
            latestArrival = Math.max(latestArrival, delay + ArmoryFlight.arrivalTicks(duration));
        }
        // every lock of the old suit releases at once: the pieces blow off and fly home to this Armory
        ArmoryPieceEntity.burstPurge(player, flying, GlobalPos.of(world.dimension(), worldPosition), (p, stack) -> store(set, p, stack));
        busyUntil = world.getGameTime() + latestArrival + 20;
        // Sequence start: warning buzzer and the launch hatch opening (each launch roars on its own).
        world.playSound(null, worldPosition, com.miophas.singularity_iteration.common.registry.mio_icif_sounds.ARMORY_ALARM.get(),
            SoundSource.BLOCKS, 1.0F, 1.0F);
        world.playSound(null, worldPosition, com.miophas.singularity_iteration.common.registry.mio_icif_sounds.ARMORY_HATCH.get(),
            SoundSource.BLOCKS, 1.0F, 1.0F);
        playerLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.8F);

        log.addFirst(new ArmoryLogEntry(System.currentTimeMillis(), world.getGameTime(), ownerOrName(player),
            setName(set), cost, sameDimension ? straight : -1, flight, flying.size(),
            player.level().dimension().location().toString()));
        while (log.size() > LOG_SIZE) log.removeLast();
        setChanged();
        sync();
        return new SummonResult(true, Component.translatable("message.mio_icif.armory.summoned", setName(set), cost), cost);
    }

    private static String ownerOrName(Player player) {
        return player.getGameProfile().getName();
    }

    /**
     * Returns an item to suit {@code set}: preferred position first, then any free
     * position of that suit, then any free suit slot. Returns what did not fit.
     */
    public ItemStack store(int set, ArmoryPiece preferred, ItemStack stack) {
        if (stack.isEmpty() || !ArmoryRules.isSummonable(stack)) return stack;
        var handler = getItemHandler();
        int slot = slotOf(set, preferred);
        if (handler.getStackInSlot(slot).isEmpty() && ArmoryRules.fits(stack, preferred)) {
            ItemStack rest = handler.insertItem(slot, stack, false);
            if (rest.isEmpty()) { setChanged(); return ItemStack.EMPTY; }
            stack = rest;
        }
        for (int s = 0; s < SETS; s++) {
            for (ArmoryPiece p : ArmoryPiece.VALUES) {
                int i = slotOf(s, p);
                if (handler.getStackInSlot(i).isEmpty() && ArmoryRules.fits(stack, p)) {
                    stack = handler.insertItem(i, stack, false);
                    if (stack.isEmpty()) { setChanged(); return ItemStack.EMPTY; }
                }
            }
        }
        return stack;
    }

    private void sync() {
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // ------------------------------------------------------------------ machine plumbing
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) return isBattery(stack);
        if (slot < FIRST_SUIT_SLOT || slot >= SLOT_COUNT) return false;
        ArmoryPiece piece = ArmoryPiece.COLUMNS[(slot - FIRST_SUIT_SLOT) % PIECES];
        return ArmoryRules.fits(stack, piece);
    }

    private static final int[] BATTERY_ONLY = {BATTERY_SLOT};
    private static final int[] ALL_SLOTS = java.util.stream.IntStream.range(0, SLOT_COUNT).toArray();

    /** The battery is always reachable; the suits only as far as {@link Automation} allows. */
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return automation == Automation.LOCKED ? BATTERY_ONLY : ALL_SLOTS;
    }

    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == BATTERY_SLOT) return super.canInsertItem(slot, stack, side);
        if (automation == Automation.LOCKED || slot < FIRST_SUIT_SLOT || slot >= SLOT_COUNT) return false;
        return getItemHandler().getStackInSlot(slot).isEmpty() && isItemValidForSlot(slot, stack);
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return automation == Automation.OPEN && slot >= FIRST_SUIT_SLOT && slot < SLOT_COUNT;
    }

    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    @Override
    protected boolean canWork() {
        return false;
    }

    @Override
    protected void doWork() { }

    @Override
    protected void tickProduction() {
        if (level instanceof ServerLevel server) {
            ArmoryMaintenance.Report done = maintenance.tick(server.getGameTime());
            if (done != null) notifyOwner(server, done);
        }
        boolean lit = owner != null && getEnergyStorage().getAmount() >= BIND_ENERGY;
        BlockState state = getBlockState();
        if (level != null && state.hasProperty(ArmoryBlock.LIT) && state.getValue(ArmoryBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(ArmoryBlock.LIT, lit), 3);
        }
    }

    /** Pushes the "suits repaired and charged" notice to the owner, wherever they are. */
    private void notifyOwner(ServerLevel server, ArmoryMaintenance.Report report) {
        if (owner == null) return;
        ServerPlayer player = server.getServer().getPlayerList().getPlayer(owner);
        if (player == null) return;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new ArmoryToastPacket(
            GlobalPos.of(server.dimension(), worldPosition), report.repairedPoints(), report.chargedEu(), report.manaUsed()));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mio_icif.producer.block_armory");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ArmoryMenu(containerId, playerInventory, this, data);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_armory armory) {
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, armory);
    }

    // ------------------------------------------------------------------ persistence
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeShared(tag);
        ListTag entries = new ListTag();
        for (ArmoryLogEntry e : log) entries.add(e.save());
        tag.put("ArmoryLog", entries);
        tag.putLong("ArmoryBusyUntil", busyUntil);
        tag.putString("ArmoryAutomation", automation.name());
        tag.putInt("ArmoryMana", mana);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readShared(tag);
        log.clear();
        ListTag entries = tag.getList("ArmoryLog", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size() && i < LOG_SIZE; i++) log.addLast(ArmoryLogEntry.load(entries.getCompound(i)));
        busyUntil = tag.getLong("ArmoryBusyUntil");
        try {
            automation = tag.contains("ArmoryAutomation") ? Automation.valueOf(tag.getString("ArmoryAutomation")) : Automation.LOCKED;
        } catch (IllegalArgumentException unknown) {
            automation = Automation.LOCKED;
        }
        mana = Math.max(0, Math.min(MANA_CAPACITY, tag.getInt("ArmoryMana")));
    }

    private void writeShared(CompoundTag tag) {
        if (owner != null) tag.putUUID("ArmoryOwner", owner);
        tag.putString("ArmoryOwnerName", ownerName);
        ListTag names = new ListTag();
        for (String n : setNames) names.add(net.minecraft.nbt.StringTag.valueOf(n));
        tag.put("ArmorySetNames", names);
    }

    private void readShared(CompoundTag tag) {
        owner = tag.hasUUID("ArmoryOwner") ? tag.getUUID("ArmoryOwner") : null;
        ownerName = tag.getString("ArmoryOwnerName");
        ListTag names = tag.getList("ArmorySetNames", Tag.TAG_STRING);
        for (int i = 0; i < SETS; i++) {
            String n = i < names.size() ? names.getString(i) : "";
            setNames[i] = n.isEmpty() ? defaultName(i) : n;
        }
    }

    /** Owner and suit names go to clients (GUI title / renderer); inventories do not. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeShared(tag);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    /** Equipment slot helper used by the menu tooltip. */
    public static EquipmentSlot slotForColumn(int column) {
        return ArmoryPiece.COLUMNS[column].slot;
    }
}

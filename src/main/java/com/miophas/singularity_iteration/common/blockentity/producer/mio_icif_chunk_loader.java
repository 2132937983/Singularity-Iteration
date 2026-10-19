package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.ISlotValidator;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotType;
import com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.machine.IMachineUpgradeStats;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Set;
import java.util.UUID;
import com.miophas.singularity_iteration.common.energy.ChunkLoaderPolicy;
import com.miophas.singularity_iteration.common.energy.OwnedChunkTickets;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.neoforge.common.world.chunk.TicketSet;
import org.jetbrains.annotations.Nullable;

/**
 * Chunk loader: keeps up to 25 selected chunks of the 9 x 9 around it loaded for EU per chunk and tick.
 *
 * <p>0.1.10.11 (server administration):
 * <ul>
 *   <li>Owner (the placing player), stored with the loader. Loaders placed before 0.1.10.11 have no
 *       owner; player limits and the offline rule skip them, the admin command lists them.</li>
 *   <li>Rules of {@link ChunkLoaderPolicy} (config {@code ChunkLoaders}): master switch, chunks per
 *       loader, chunks and loaders per player, offline owner timeout, EU per chunk, random ticks,
 *       owner-only editing. Over a chunk budget the loader keeps its own chunk first, then the nearest
 *       selected chunks ({@link #STATUS_LIMITED}).</li>
 *   <li>Admin lock ({@code /mio_icif chunkloaders disable}), saved with the loader.</li>
 *   <li>A loader that stops for a rule is parked and woken again when the reason is gone (see the policy).</li>
 *   <li>Cost: the steady state pays the EU and allocates nothing; tickets change only when the
 *       granted set changes, the rules are re-checked every second (at once after a change),
 *       the upgrades only after an inventory change, and no per-tick setChanged (6 neighbour updates)
 *       is left.</li>
 * </ul>
 */
@SuppressWarnings("null")
public class mio_icif_chunk_loader extends AbstractEnergyBlockEntity implements com.miophas.singularity_iteration.common.area.WorkAreaProvider, ISlotValidator {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .battery()
        .upgrade(4)
        .build();

    public static final int BATTERY_SLOT = 0;
    public static final int UPGRADE_SLOT_START = 1;
    public static final int UPGRADE_SLOT_COUNT = 4;

    public static final long DEFAULT_CAPACITY = 2500L;
    public static final long DEFAULT_MAX_RECEIVE = 128L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final double DEFAULT_EU_PER_CHUNK = 1.0;
    public static final int CHUNK_RADIUS = 4;
    public static final int MAX_CHUNKS = 25;

    // 0.1.10.11: state shown in the screen and by /mio_icif chunkloaders
    public static final int STATUS_STARTING = 0;
    public static final int STATUS_ACTIVE = 1;
    /** Active, but a chunk budget left part of the selection unloaded. */
    public static final int STATUS_LIMITED = 2;
    public static final int STATUS_NO_ENERGY = 3;
    public static final int STATUS_EMPTY = 4;
    public static final int STATUS_CONFIG_OFF = 5;
    public static final int STATUS_ADMIN_OFF = 6;
    public static final int STATUS_OWNER_OFFLINE = 7;
    public static final int STATUS_CHUNK_LIMIT = 8;
    public static final int STATUS_LOADER_LIMIT = 9;
    public static final String[] STATUS_NAMES = {"starting", "active", "limited", "no_energy", "empty", "config_off",
        "admin_off", "owner_offline", "chunk_limit", "loader_limit"};
    /** Rules are re-checked this often (ticks) in the steady state. */
    private static final int POLICY_INTERVAL = 20;

    protected final MachineItemHandler itemHandler;
    protected IMachineUpgradeStats upgradeStats = MachineUpgradeStats.empty();
    protected final long baseCapacity;

    private final LongOpenHashSet loadedChunks = new LongOpenHashSet();
    private final LongOpenHashSet forcedChunks = new LongOpenHashSet();
    /** Chunks the rules grant now (subset of the selection); rebuilt by {@link #evaluate}. */
    private final LongOpenHashSet granted = new LongOpenHashSet();
    private final int[] chunkBits = new int[3];
    private boolean active = false;
    private boolean ticketUpdate;
    private boolean destroyed;
    private long paidTick = Long.MIN_VALUE;

    @Nullable private UUID owner;
    private String ownerName = "";
    /** Wall-clock time the owner was last seen online (ms); survives restarts. */
    private long ownerSeen;
    private boolean adminDisabled;
    private int status = STATUS_STARTING;
    private int verdict = STATUS_STARTING;
    /** Random-tick mode of the tickets in {@link #forcedChunks}. */
    private boolean forcedTicking = true;
    private boolean rulesDirty = true;
    private boolean upgradesDirty = true;
    private boolean parked;
    private int policyGeneration = Integer.MIN_VALUE;
    private int policyCountdown;
    private int reportedStatus = -1, reportedGranted = -1, reportedSelected = -1;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) energyStorage.getAmount();
                case 1 -> (int) energyStorage.getCapacity();
                case 2 -> active ? 1 : 0;
                case 3 -> loadedChunks.size();
                case 4 -> maxChunks();
                case 5, 6, 7 -> chunkBits[index - 5];
                case 8 -> status;
                case 9 -> active ? forcedChunks.size() : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() { return DATA_COUNT; }
    };
    public static final int DATA_COUNT = 10;

    /** Recomputes the selection bits of the screen map (after a selection change only). */
    private void refreshChunkBits() {
        ChunkPos self = getSelfChunkPos();
        java.util.Arrays.fill(chunkBits, 0);
        int cells = (CHUNK_RADIUS * 2 + 1) * (CHUNK_RADIUS * 2 + 1);
        for (int idx = 0; idx < cells; idx++) {
            int dx = idx / 9 - CHUNK_RADIUS;
            int dz = idx % 9 - CHUNK_RADIUS;
            if (loadedChunks.contains(ChunkPos.asLong(self.x + dx, self.z + dz))) chunkBits[idx / 32] |= 1 << (idx % 32);
        }
    }

    public mio_icif_chunk_loader(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.CHUNK_LOADER.get());
    }

    public mio_icif_chunk_loader(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, DEFAULT_CAPACITY, DEFAULT_MAX_RECEIVE, DEFAULT_MAX_EXTRACT, CableTier.MV);
        this.baseCapacity = DEFAULT_CAPACITY;
        this.itemHandler = new MachineItemHandler(LAYOUT) {
            @Override protected void onContentsChanged(int slot) { upgradesDirty = true; setChanged(); }
        };
        this.itemHandler.setValidator(this);
        this.setAsConsumer();
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public boolean isValidForSlot(int slot, ItemStack stack, SlotType type) {
        return true;
    }

    public Set<Long> getLoadedChunks() {
        return Set.copyOf(loadedChunks);
    }

    public int getLoadedChunkCount() {
        return loadedChunks.size();
    }

    /** Chunks that hold a ticket of this loader now. */
    public int getForcedChunkCount() {
        return active ? forcedChunks.size() : 0;
    }

    public double getEuPerChunk() {
        return ChunkLoaderPolicy.euPerChunk();
    }

    public boolean isActive() {
        return active;
    }

    public int status() { return status; }
    @Nullable public UUID owner() { return owner; }
    public String ownerName() { return ownerName; }
    public boolean adminDisabled() { return adminDisabled; }

    /** Selection limit of this loader: the config value, at most {@link #MAX_CHUNKS}. */
    public static int maxChunks() {
        return Math.min(MAX_CHUNKS, ChunkLoaderPolicy.maxChunksPerLoader());
    }

    public static long chunkKey(ChunkPos pos) {
        return ChunkPos.asLong(pos.x, pos.z);
    }

    public static ChunkPos fromChunkKey(long key) {
        return new ChunkPos(key);
    }

    public ChunkPos getSelfChunkPos() {
        return new ChunkPos(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
    }

    private long selfKey() {
        return ChunkPos.asLong(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
    }

    public boolean isChunkInRange(ChunkPos chunk) {
        ChunkPos self = getSelfChunkPos();
        return Math.abs((long) chunk.x - self.x) <= CHUNK_RADIUS
            && Math.abs((long) chunk.z - self.z) <= CHUNK_RADIUS;
    }

    public boolean isChunkInRange(int xOff, int zOff) {
        return Math.abs((long) xOff) <= CHUNK_RADIUS && Math.abs((long) zOff) <= CHUNK_RADIUS;
    }

    // ------------------------------------------------------------------ owner and admin

    /** Sets the owner (placement); also counts as seen online now. */
    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getGameProfile().getName();
        ownerSeen = System.currentTimeMillis();
        rulesDirty = true;
        setChanged();
    }

    /** True when {@code player} may change the selection (config ownerOnlyEdit; operators always). */
    public boolean mayEdit(Player player) {
        return !ChunkLoaderPolicy.ownerOnlyEdit() || owner == null || owner.equals(player.getUUID()) || player.hasPermissions(2);
    }

    /** Admin lock (command); the loader stops or resumes on its next tick. */
    public void setAdminDisabled(boolean disabled) {
        if (adminDisabled == disabled) return;
        adminDisabled = disabled;
        rulesDirty = true;
        setChanged();
    }

    private GlobalPos globalPos() {
        return GlobalPos.of(level.dimension(), worldPosition);
    }

    // ------------------------------------------------------------------ selection

    private boolean live() {
        if (destroyed || isRemoved() || !(level instanceof ServerLevel server) || !server.getServer().isSameThread()) return false;
        var chunk = server.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        return chunk != null && chunk.getBlockEntity(worldPosition, net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK) == this
            && chunk.getBlockState(worldPosition).getBlock() == getBlockState().getBlock();
    }

    public boolean addChunkToLoaded(ChunkPos chunk) {
        if (!live() || ticketUpdate || !isChunkInRange(chunk) || loadedChunks.size() >= maxChunks()) return false;
        // Selection is free; the next natural tick pays before installing tickets.
        if (!loadedChunks.add(chunkKey(chunk))) return false;
        selectionChanged(); return true;
    }

    public boolean removeChunkFromLoaded(ChunkPos chunk) {
        if (!live() || ticketUpdate || chunk.equals(getSelfChunkPos())) return false;
        long key = chunkKey(chunk);
        if (!loadedChunks.remove(key)) return false;
        if (forcedChunks.contains(key)) removeTicket((ServerLevel) level, key);
        selectionChanged(); return true;
    }

    private void selectionChanged() {
        refreshChunkBits();
        rulesDirty = true;
        setChanged();
    }

    public void toggleChunk(ChunkPos chunk) {
        if (loadedChunks.contains(chunkKey(chunk))) removeChunkFromLoaded(chunk);
        else addChunkToLoaded(chunk);
    }

    // ------------------------------------------------------------------ tickets

    private void removeTicket(ServerLevel server, long key) {
        ChunkPos chunk = fromChunkKey(key);
        OwnedChunkTickets.CONTROLLER.forceChunk(server, worldPosition, chunk.x, chunk.z, false, forcedTicking);
        forcedChunks.remove(key);
    }

    private void releaseTickets(ServerLevel server, LongSet additional) {
        var all = new LongOpenHashSet(forcedChunks); all.addAll(additional);
        for (long key : all) removeTicket(server, key);
        active = false;
    }

    private static long cost(int count) {
        double amount = Math.ceil(count * ChunkLoaderPolicy.euPerChunk());
        return count == 0 ? 0 : !Double.isFinite(amount) || amount < 0 || amount >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) amount;
    }

    /**
     * Applies the rules: fills {@link #granted} (own chunk first, then the nearest selected chunks,
     * up to the loader and owner budgets) and returns the state they allow.
     */
    private int evaluate(ServerLevel server) {
        granted.clear();
        if (!ChunkLoaderPolicy.enabled()) return STATUS_CONFIG_OFF;
        if (adminDisabled) return STATUS_ADMIN_OFF;
        if (loadedChunks.isEmpty() || !loadedChunks.contains(selfKey())) return STATUS_EMPTY;
        int budget = Math.min(loadedChunks.size(), maxChunks());
        if (owner != null) {
            ServerPlayer online = server.getServer().getPlayerList().getPlayer(owner);
            long now = System.currentTimeMillis();
            if (online != null) {
                ownerSeen = now;
                ownerName = online.getGameProfile().getName();
            } else {
                if (ownerSeen <= 0) ownerSeen = now;   // loaders from before 0.1.10.11: start the clock now
                int minutes = ChunkLoaderPolicy.offlineMinutes();
                if (minutes >= 0 && now - ownerSeen >= minutes * 60_000L) return STATUS_OWNER_OFFLINE;
            }
            int[] usage = ChunkLoaderPolicy.usage(owner, globalPos());
            int maxLoaders = ChunkLoaderPolicy.maxLoadersPerPlayer();
            if (maxLoaders > 0 && usage[0] >= maxLoaders) return STATUS_LOADER_LIMIT;
            int maxChunks = ChunkLoaderPolicy.maxChunksPerPlayer();
            if (maxChunks > 0) budget = Math.min(budget, maxChunks - usage[1]);
            if (budget <= 0) return STATUS_CHUNK_LIMIT;
        }
        if (budget >= loadedChunks.size()) {
            granted.addAll(loadedChunks);
            return STATUS_ACTIVE;
        }
        // own chunk first, then by distance (ties by key): the same chunks every time
        long self = selfKey();
        int sx = ChunkPos.getX(self), sz = ChunkPos.getZ(self);
        LongArrayList order = new LongArrayList(loadedChunks);
        order.sort((a, b) -> {
            int da = sq(ChunkPos.getX(a) - sx) + sq(ChunkPos.getZ(a) - sz), db = sq(ChunkPos.getX(b) - sx) + sq(ChunkPos.getZ(b) - sz);
            return da != db ? Integer.compare(da, db) : Long.compare(a, b);
        });
        for (int i = 0; i < budget; i++) granted.add(order.getLong(i));
        return STATUS_LIMITED;
    }

    private static int sq(int v) { return v * v; }

    private void updateTickets(ServerLevel server) {
        if (!live() || ticketUpdate || paidTick == server.getGameTime()) return;
        paidTick = server.getGameTime();
        int generation = ChunkLoaderPolicy.generation();
        if (rulesDirty || generation != policyGeneration || --policyCountdown <= 0) {
            rulesDirty = false;
            policyGeneration = generation;
            policyCountdown = POLICY_INTERVAL;
            verdict = evaluate(server);
        }
        if (granted.isEmpty()) { stop(server, verdict); return; }
        if (!active || forcedTicking != ChunkLoaderPolicy.randomTicks() || !granted.equals(forcedChunks)) {
            syncTickets(server);
            return;
        }
        // steady state: pay for the granted chunks, nothing else
        long amount = cost(forcedChunks.size());
        if (amount > 0 && (energyStorage.consumeEnergyInternal(amount, true) != amount
                || energyStorage.consumeEnergyInternal(amount, false) != amount)) {
            stop(server, STATUS_NO_ENERGY);
            return;
        }
        setStatus(verdict);
    }

    /** Pays one tick for {@link #granted}, then moves the tickets to exactly that set. */
    private void syncTickets(ServerLevel server) {
        boolean ticking = ChunkLoaderPolicy.randomTicks();
        long amount = cost(granted.size());
        var payment = amount == 0 ? null : energyStorage.scexReserveInternal(amount);
        if (amount != 0 && payment == null) { stop(server, STATUS_NO_ENERGY); return; }
        LongOpenHashSet selected = new LongOpenHashSet(granted);
        ticketUpdate = true;
        boolean complete = false;
        try {
            // tickets of the other random-tick mode, or no longer granted
            for (long key : new LongOpenHashSet(forcedChunks))
                if (forcedTicking != ticking || !selected.contains(key)) removeTicket(server, key);
            forcedTicking = ticking;
            for (long key : selected) {
                if (!live()) return;
                if (forcedChunks.add(key)) {
                    ChunkPos chunk = fromChunkKey(key);
                    OwnedChunkTickets.CONTROLLER.forceChunk(server, worldPosition, chunk.x, chunk.z, true, ticking);
                }
                if (!live()) return;
            }
            if (!selected.equals(granted)) return;
            if (payment != null) payment.commit();
            complete = true; active = true;
        } finally {
            if (!complete) {
                // forceChunk can add its ticket after a loading callback removes this owner.
                try { releaseTickets(server, selected); } finally { if (payment != null) payment.cancel(); }
            }
            ticketUpdate = false;
        }
        if (live()) {
            setLit(active);
            if (active) {
                if (parked) { parked = false; ChunkLoaderPolicy.unpark(server, worldPosition); }
                setStatus(verdict);
            } else setStatus(STATUS_NO_ENERGY);
            setChanged();
        }
    }

    /** Releases every ticket; parks the loader when a rule (not the energy) stopped it. */
    private void stop(ServerLevel server, int reason) {
        stop(server, reason, true);
    }

    private void stop(ServerLevel server, int reason, boolean touchBlock) {
        // nothing held and the same reason as last tick: nothing to do (no allocation, no lookups)
        if (!active && forcedChunks.isEmpty() && status == reason) return;
        int before = active ? forcedChunks.size() : 0;
        if (!forcedChunks.isEmpty() || active) releaseTickets(server, LongSet.of());
        if (touchBlock) setLit(false);
        setStatus(reason);
        ChunkLoaderPolicy.Reason park = switch (reason) {
            case STATUS_CONFIG_OFF -> ChunkLoaderPolicy.Reason.CONFIG;
            case STATUS_ADMIN_OFF -> ChunkLoaderPolicy.Reason.ADMIN;
            case STATUS_OWNER_OFFLINE -> ChunkLoaderPolicy.Reason.OFFLINE;
            case STATUS_CHUNK_LIMIT, STATUS_LOADER_LIMIT -> ChunkLoaderPolicy.Reason.LIMIT;
            default -> null;
        };
        if (park != null) {
            parked = true;
            ChunkLoaderPolicy.park(server, worldPosition, owner, park);
            setChanged();
        }
        if (before > 0) ChunkLoaderPolicy.budgetFreed(server.getServer(), owner);
    }

    private void setStatus(int value) {
        status = value;
        int grantedNow = active ? forcedChunks.size() : 0;
        if (value == reportedStatus && grantedNow == reportedGranted && loadedChunks.size() == reportedSelected) return;
        reportedStatus = value; reportedGranted = grantedNow; reportedSelected = loadedChunks.size();
        if (level instanceof ServerLevel) ChunkLoaderPolicy.report(new ChunkLoaderPolicy.Entry(globalPos(), owner, ownerName, grantedNow, loadedChunks.size(), value));
    }

    /**
     * Takes over the ticket that {@link ChunkLoaderPolicy#wake} placed on this loader's chunk; the
     * rules run on the next tick. False when the loader cannot take it (the caller removes it).
     */
    public boolean adoptWakeTicket(boolean ticking) {
        if (!live() || ticketUpdate) return false;
        if (!forcedChunks.isEmpty() && forcedTicking != ticking) return false;
        forcedTicking = ticking;
        forcedChunks.add(selfKey());
        parked = false;
        if (owner != null && ((ServerLevel) level).getServer().getPlayerList().getPlayer(owner) != null) ownerSeen = System.currentTimeMillis();
        rulesDirty = true;
        return true;
    }

    /** NeoForge startup validation runs before saved tickets are reinstated. */
    public void scexValidateSavedTickets(TicketHelper helper, TicketSet saved) {
        boolean ticking = ChunkLoaderPolicy.randomTicks();
        LongSet mine = ticking ? saved.ticking() : saved.nonTicking();
        LongSet other = ticking ? saved.nonTicking() : saved.ticking();
        if (!live() || ticketUpdate || !mine.contains(selfKey())) {
            // a mode change of the config: the loader installs its tickets again on its first tick
            helper.removeAllTickets(worldPosition); return;
        }
        ServerLevel server = (ServerLevel) level;
        verdict = evaluate(server);
        var retained = new LongOpenHashSet();
        for (long key : mine) {
            if (granted.contains(key) && isChunkInRange(fromChunkKey(key))) retained.add(key);
            else helper.removeTicket(worldPosition, key, ticking);
        }
        for (long key : other) helper.removeTicket(worldPosition, key, !ticking);
        long amount = cost(retained.size());
        // No external callback occurs between this quote and whole-EU debit.
        if (!retained.contains(selfKey()) || energyStorage.consumeEnergyInternal(amount, true) != amount) {
            helper.removeAllTickets(worldPosition); active = false;
            stop(server, granted.isEmpty() ? verdict : STATUS_NO_ENERGY, false);
            return;
        }
        if (energyStorage.consumeEnergyInternal(amount, false) != amount) throw new IllegalStateException("Chunk startup payment changed");
        forcedChunks.addAll(retained); forcedTicking = ticking; active = true; paidTick = level.getGameTime();
        policyGeneration = ChunkLoaderPolicy.generation(); policyCountdown = POLICY_INTERVAL;
        setStatus(verdict);
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_chunk_loader blockEntity) {
        if (!(level instanceof ServerLevel server) || !blockEntity.live()) return;
        if (blockEntity.upgradesDirty) {
            blockEntity.upgradesDirty = false;
            blockEntity.recalculateUpgradeStats();
        }
        blockEntity.handleBatterySlot();
        blockEntity.updateTickets(server);
    }

    private void recalculateUpgradeStats() {
        int upgradeStart = LAYOUT.getStart(SlotType.UPGRADE);
        int upgradeCount = LAYOUT.getCount(SlotType.UPGRADE);
        this.upgradeStats = MachineUpgradeStats.fromInventory(itemHandler, upgradeStart, upgradeCount);
        long newCapacity = baseCapacity + upgradeStats.getEnergyCapacityBonus();
        if (newCapacity != energyStorage.getMaxEnergyStored()) {
            energyStorage.setCapacity(newCapacity);
        }
        long effectiveMaxReceive = getEffectiveMaxReceive();
        if (effectiveMaxReceive != energyStorage.getMaxReceive()) {
            energyStorage.setMaxReceive(effectiveMaxReceive);
        }
    }

    private void handleBatterySlot() {
        scexFeBridge().discharge(itemHandler, BATTERY_SLOT);
    }

    @Override
    public net.neoforged.neoforge.energy.IEnergyStorage scexFeCapability(net.minecraft.core.Direction side) {
        return scexFeBridge().port(side);
    }

    public void setLit(boolean lit) {
        if (getLevel() != null && !getLevel().isClientSide) {
            BlockState state = getBlockState();
            for (var property : state.getProperties()) {
                if (property instanceof net.minecraft.world.level.block.state.properties.BooleanProperty boolProp
                    && "lit".equals(boolProp.getName())) {
                    if (state.getValue(boolProp) != lit) {
                        getLevel().setBlock(getBlockPos(), state.setValue(boolProp, lit), 3);
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Saved active is display history, never permission for an unpaid ticket.
        active = false;
        refreshChunkBits();
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel server && server.getServer().isSameThread()
                && !ticketUpdate && !OwnedChunkTickets.stopping(server.getServer())) releaseTickets(server, LongSet.of());
        if (level instanceof ServerLevel) ChunkLoaderPolicy.forget(globalPos());
        super.setRemoved();
    }

    public void destroy() {
        if (level instanceof ServerLevel server && !server.getServer().isSameThread())
            throw new IllegalStateException("Chunk loader destruction requires server thread");
        destroyed = true;
        boolean held = active;
        active = false;
        // During synchronous chunk loading, defer removal until forceChunk has
        // installed its region ticket. Early tracker removal would strand it.
        if (!ticketUpdate && level instanceof ServerLevel server && server.getServer().isSameThread()) releaseTickets(server, LongSet.of());
        if (level instanceof ServerLevel server) {
            ChunkLoaderPolicy.unpark(server, worldPosition);
            ChunkLoaderPolicy.forget(globalPos());
            if (held) ChunkLoaderPolicy.budgetFreed(server.getServer(), owner);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag chunksTag = new CompoundTag();
        int i = 0;
        for (long key : loadedChunks) {
            chunksTag.put("c" + i, LongTag.valueOf(key));
            i++;
        }
        chunksTag.putInt("count", i);
        tag.put("loadedChunks", chunksTag);
        tag.putBoolean("active", active);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
            tag.putLong("OwnerSeen", ownerSeen);
        }
        if (adminDisabled) tag.putBoolean("AdminDisabled", true);
        if (parked) tag.putBoolean("Parked", true);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        // Restore upgrade capacity before legacy-mode energy loading can clamp it.
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        recalculateUpgradeStats();
        super.loadAdditional(tag, registries);
        loadedChunks.clear();
        CompoundTag chunksTag = tag.getCompound("loadedChunks");
        int count = Math.max(0, Math.min(MAX_CHUNKS, chunksTag.getInt("count")));
        for (int i = 0; i < count; i++) {
            if (chunksTag.contains("c" + i, Tag.TAG_LONG)) {
                long key = chunksTag.getLong("c" + i);
                if (isChunkInRange(fromChunkKey(key))) loadedChunks.add(key);
            }
        }
        active = false;
        paidTick = Long.MIN_VALUE;
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        ownerSeen = tag.getLong("OwnerSeen");
        adminDisabled = tag.getBoolean("AdminDisabled");
        parked = tag.getBoolean("Parked");
        rulesDirty = true;
        // the loader always keeps its own chunk (tickets elsewhere would outlive an unloaded loader)
        if (!loadedChunks.isEmpty() && !loadedChunks.contains(selfKey()) && loadedChunks.size() < MAX_CHUNKS) loadedChunks.add(selfKey());
        refreshChunkBits();

        recalculateUpgradeStats();
        upgradesDirty = false;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.producer.ChunkLoaderMenu(containerId, playerInventory, this, this.containerData);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.chunk_loader");
    }

    public ContainerData getContainerData() { return containerData; }

    @Override
    public long getEnergy() {
        return energyStorage.getAmount();
    }

    @Override
    public boolean useEnergy(long amount) {
        if (!live() || amount < 0 || energyStorage.consumeEnergyInternal(amount, true) != amount) return false;
        return energyStorage.consumeEnergyInternal(amount, false) == amount;
    }

    @Override
    public java.util.Set<com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty> getUpgradableProperties() {
        return java.util.EnumSet.of(
            com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty.ENERGY_STORAGE,
            com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty.ITEM_CONSUMING,
            com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty.ITEM_PRODUCING,
            com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty.TRANSFORMER
        );
    }

    @Override
    public ICableTier getEffectiveCableTier() {
        ICableTier baseTier = (ICableTier) energyStorage.getCableTier();
        return upgradeStats.getEffectiveCableTier(baseTier);
    }

    @Override
    public long getEffectiveCapacity() {
        return baseCapacity + upgradeStats.getEnergyCapacityBonus();
    }

    @Override
    public long getEffectiveMaxReceive() {
        long base = DEFAULT_MAX_RECEIVE;
        return Math.max(base, getEffectiveCableTier().getPowerRating());
    }

    @Override
    public double getDemandedEnergy() {
        if (energyStorage.scexNetworkControlled() || isPowerSource) return 0.0D;
        long spaceAvailable = getEffectiveCapacity() - energyStorage.getAmount();
        if (spaceAvailable <= 0) return 0.0D;
        return spaceAvailable;
    }

    @Override
    public java.util.List<com.miophas.singularity_iteration.common.area.WorkArea> workAreas() {
        java.util.List<com.miophas.singularity_iteration.common.area.WorkArea> out = new java.util.ArrayList<>();
        int y = worldPosition.getY();
        for (long key : loadedChunks) {
            var chunk = new net.minecraft.world.level.ChunkPos(key);
            out.add(new com.miophas.singularity_iteration.common.area.WorkArea(new net.minecraft.world.phys.AABB(chunk.getMinBlockX(), y - 8, chunk.getMinBlockZ(),
                chunk.getMaxBlockX() + 1, y + 9, chunk.getMaxBlockZ() + 1), com.miophas.singularity_iteration.common.area.WorkArea.CHUNK, com.miophas.singularity_iteration.common.area.WorkArea.PRIMARY));
        }
        var self = getSelfChunkPos();
        out.add(new com.miophas.singularity_iteration.common.area.WorkArea(new net.minecraft.world.phys.AABB((self.x - CHUNK_RADIUS) * 16, y - 8, (self.z - CHUNK_RADIUS) * 16,
            (self.x + CHUNK_RADIUS + 1) * 16, y + 9, (self.z + CHUNK_RADIUS + 1) * 16), com.miophas.singularity_iteration.common.area.WorkArea.CHUNK, com.miophas.singularity_iteration.common.area.WorkArea.ENVELOPE));
        return out;
    }
}

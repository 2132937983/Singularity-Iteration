package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.ISlotValidator;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotType;
import com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.machine.IMachineUpgradeStats;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import com.miophas.singularity_iteration.common.energy.OwnedChunkTickets;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.neoforge.common.world.chunk.TicketSet;

@SuppressWarnings("null")
public class mio_icif_chunk_loader extends AbstractEnergyBlockEntity implements ISlotValidator {

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

    protected final MachineItemHandler itemHandler;
    protected IMachineUpgradeStats upgradeStats = MachineUpgradeStats.empty();
    protected final long baseCapacity;

    private final LongOpenHashSet loadedChunks = new LongOpenHashSet();
    private final LongOpenHashSet forcedChunks = new LongOpenHashSet();
    private boolean active = false;
    private boolean ticketUpdate;
    private boolean destroyed;
    private long paidTick = Long.MIN_VALUE;
    private double euPerChunk = DEFAULT_EU_PER_CHUNK;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) energyStorage.getAmount();
                case 1 -> (int) energyStorage.getCapacity();
                case 2 -> active ? 1 : 0;
                case 3 -> loadedChunks.size();
                case 4 -> MAX_CHUNKS;
                case 5 -> computeChunkBits(0);
                case 6 -> computeChunkBits(1);
                case 7 -> computeChunkBits(2);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() { return 8; }
    };

    private int computeChunkBits(int wordIndex) {
        ChunkPos self = getSelfChunkPos();
        int bits = 0;
        int start = wordIndex * 32;
        int end = Math.min(start + 32, (CHUNK_RADIUS * 2 + 1) * (CHUNK_RADIUS * 2 + 1));
        for (int idx = start; idx < end; idx++) {
            int dx = idx / 9 - CHUNK_RADIUS;
            int dz = idx % 9 - CHUNK_RADIUS;
            ChunkPos chunk = new ChunkPos(self.x + dx, self.z + dz);
            if (loadedChunks.contains(chunkKey(chunk))) {
                bits |= (1 << (idx - start));
            }
        }
        return bits;
    }

    public mio_icif_chunk_loader(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.CHUNK_LOADER.get());
    }

    public mio_icif_chunk_loader(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, DEFAULT_CAPACITY, DEFAULT_MAX_RECEIVE, DEFAULT_MAX_EXTRACT, CableTier.MV);
        this.baseCapacity = DEFAULT_CAPACITY;
        this.itemHandler = new MachineItemHandler(LAYOUT) {
            @Override protected void onContentsChanged(int slot) { setChanged(); }
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

    public double getEuPerChunk() {
        return euPerChunk;
    }

    public boolean isActive() {
        return active;
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

    public boolean isChunkInRange(ChunkPos chunk) {
        ChunkPos self = getSelfChunkPos();
        return Math.abs((long) chunk.x - self.x) <= CHUNK_RADIUS
            && Math.abs((long) chunk.z - self.z) <= CHUNK_RADIUS;
    }

    public boolean isChunkInRange(int xOff, int zOff) {
        return Math.abs((long) xOff) <= CHUNK_RADIUS && Math.abs((long) zOff) <= CHUNK_RADIUS;
    }


    private boolean live() {
        if (destroyed || isRemoved() || !(level instanceof ServerLevel server) || !server.getServer().isSameThread()) return false;
        var chunk = server.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        return chunk != null && chunk.getBlockEntity(worldPosition, net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK) == this
            && chunk.getBlockState(worldPosition).getBlock() == getBlockState().getBlock();
    }

    public boolean addChunkToLoaded(ChunkPos chunk) {
        if (!live() || ticketUpdate || !isChunkInRange(chunk) || loadedChunks.size() >= MAX_CHUNKS) return false;
        // Selection is free; the next natural tick pays before installing tickets.
        if (!loadedChunks.add(chunkKey(chunk))) return false;
        setChanged(); return true;
    }

    public boolean removeChunkFromLoaded(ChunkPos chunk) {
        if (!live() || ticketUpdate || chunk.equals(getSelfChunkPos())) return false;
        long key = chunkKey(chunk);
        if (!loadedChunks.remove(key)) return false;
        removeTicket((ServerLevel) level, key);
        setChanged(); return true;
    }

    public void toggleChunk(ChunkPos chunk) {
        if (loadedChunks.contains(chunkKey(chunk))) removeChunkFromLoaded(chunk);
        else addChunkToLoaded(chunk);
    }

    private void removeTicket(ServerLevel server, long key) {
        ChunkPos chunk = fromChunkKey(key);
        OwnedChunkTickets.CONTROLLER.forceChunk(server, worldPosition, chunk.x, chunk.z, false, true);
        forcedChunks.remove(key);
    }

    private void releaseTickets(ServerLevel server, Set<Long> additional) {
        var all = new java.util.HashSet<Long>(forcedChunks); all.addAll(additional);
        for (long key : all) removeTicket(server, key);
        active = false;
    }

    private long cost(int count) {
        double amount = Math.ceil(count * euPerChunk);
        return count == 0 ? 0 : !Double.isFinite(amount) || amount <= 0 || amount >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) amount;
    }

    private void updateTickets(ServerLevel server) {
        if (!live() || ticketUpdate || paidTick == server.getGameTime()) return;
        paidTick = server.getGameTime();
        Set<Long> selected = Set.copyOf(loadedChunks);
        long amount = cost(selected.size());
        if (amount == 0) {
            // An empty selection has no reservation to commit/cancel.  Clear
            // any stale tickets and leave the loader inactive for this tick.
            releaseTickets(server, Set.of());
            setLit(false);
            setChanged();
            return;
        }
        var payment = energyStorage.scexReserveInternal(amount);
        if (payment == null) { releaseTickets(server, Set.of()); setLit(false); setChanged(); return; }
        ticketUpdate = true;
        boolean complete = false;
        try {
            for (long key : Set.copyOf(forcedChunks)) if (!selected.contains(key)) removeTicket(server, key);
            for (long key : selected) {
                if (!live()) return;
                if (forcedChunks.add(key)) {
                    ChunkPos chunk = fromChunkKey(key);
                    OwnedChunkTickets.CONTROLLER.forceChunk(server, worldPosition, chunk.x, chunk.z, true, true);
                }
                if (!live()) return;
            }
            if (!selected.equals(Set.copyOf(loadedChunks))) return;
            payment.commit(); complete = true; active = true;
        } finally {
            if (!complete) {
                // forceChunk can add its ticket after a loading callback removes this owner.
                try { releaseTickets(server, selected); } finally { payment.cancel(); }
            }
            ticketUpdate = false;
        }
        if (live()) { setLit(active); setChanged(); }
    }

    /** NeoForge startup validation runs before saved tickets are reinstated. */
    public void scexValidateSavedTickets(TicketHelper helper, TicketSet saved) {
        if (!live() || ticketUpdate || !saved.ticking().contains(chunkKey(getSelfChunkPos()))) {
            helper.removeAllTickets(worldPosition); return;
        }
        var retained = new LongOpenHashSet();
        for (long key : saved.ticking()) {
            if (loadedChunks.contains(key) && isChunkInRange(fromChunkKey(key))) retained.add(key);
            else helper.removeTicket(worldPosition, key, true);
        }
        for (long key : saved.nonTicking()) helper.removeTicket(worldPosition, key, false);
        long amount = cost(retained.size());
        // No external callback occurs between this quote and whole-EU debit.
        if (!retained.contains(chunkKey(getSelfChunkPos())) || amount == 0 || energyStorage.consumeEnergyInternal(amount, true) != amount) {
            helper.removeAllTickets(worldPosition); active = false; return;
        }
        if (energyStorage.consumeEnergyInternal(amount, false) != amount) throw new IllegalStateException("Chunk startup payment changed");
        forcedChunks.addAll(retained); active = true; paidTick = level.getGameTime(); setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_chunk_loader blockEntity) {
        if (!(level instanceof ServerLevel server) || !blockEntity.live()) return;
        blockEntity.recalculateUpgradeStats();
        AbstractEnergyBlockEntity.tick(level, pos, state, blockEntity);
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
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel server && server.getServer().isSameThread()
                && !ticketUpdate && !OwnedChunkTickets.stopping(server.getServer())) releaseTickets(server, Set.of());
        super.setRemoved();
    }

    public void destroy() {
        if (level instanceof ServerLevel server && !server.getServer().isSameThread())
            throw new IllegalStateException("Chunk loader destruction requires server thread");
        destroyed = true;
        active = false;
        // During synchronous chunk loading, defer removal until forceChunk has
        // installed its region ticket. Early tracker removal would strand it.
        if (!ticketUpdate && level instanceof ServerLevel server && server.getServer().isSameThread()) releaseTickets(server, Set.of());
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

        recalculateUpgradeStats();
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
}

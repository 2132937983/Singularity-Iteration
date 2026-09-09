package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.ISlotValidator;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotType;
import com.singularity_iteration.mio_icif.Items.Upgrade.MachineUpgradeStats;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.IMachineUpgradeStats;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
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

@SuppressWarnings("null")
public class mio_icif_chunk_loader extends mio_icif_Energy_Block implements ISlotValidator {

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
    private boolean active = false;
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
        this.itemHandler = new MachineItemHandler(LAYOUT);
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
        return loadedChunks;
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
        return Math.abs(chunk.x - self.x) <= CHUNK_RADIUS
            && Math.abs(chunk.z - self.z) <= CHUNK_RADIUS;
    }

    public boolean isChunkInRange(int xOff, int zOff) {
        return Math.abs(xOff) <= CHUNK_RADIUS && Math.abs(zOff) <= CHUNK_RADIUS;
    }

    public boolean addChunkToLoaded(ChunkPos chunk) {
        if (level == null || level.isClientSide) return false;
        if (!isChunkInRange(chunk)) return false;
        if (loadedChunks.size() >= MAX_CHUNKS) return false;

        long key = chunkKey(chunk);
        if (loadedChunks.add(key)) {
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.setChunkForced(chunk.x, chunk.z, true);
            }
            setChanged();
            return true;
        }
        return false;
    }

    public boolean removeChunkFromLoaded(ChunkPos chunk) {
        if (level == null || level.isClientSide) return false;

        ChunkPos self = getSelfChunkPos();
        if (chunk.x == self.x && chunk.z == self.z) return false;

        long key = chunkKey(chunk);
        if (loadedChunks.remove(key)) {
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.setChunkForced(chunk.x, chunk.z, false);
            }
            setChanged();
            return true;
        }
        return false;
    }

    public void toggleChunk(ChunkPos chunk) {
        long key = chunkKey(chunk);
        if (loadedChunks.contains(key)) {
            removeChunkFromLoaded(chunk);
        } else {
            addChunkToLoaded(chunk);
        }
    }

    private void forceLoadAllChunks() {
        if (level == null || !(level instanceof ServerLevel serverLevel)) return;
        for (long key : loadedChunks) {
            ChunkPos pos = fromChunkKey(key);
            serverLevel.setChunkForced(pos.x, pos.z, true);
        }
    }

    private void unforceNonSelfChunks() {
        if (level == null || !(level instanceof ServerLevel serverLevel)) return;
        ChunkPos self = getSelfChunkPos();
        for (long key : loadedChunks) {
            ChunkPos pos = fromChunkKey(key);
            if (pos.x != self.x || pos.z != self.z) {
                serverLevel.setChunkForced(pos.x, pos.z, false);
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_chunk_loader blockEntity) {
        if (level.isClientSide()) return;

        blockEntity.recalculateUpgradeStats();
        mio_icif_Energy_Block.tick(level, pos, state, blockEntity);
        blockEntity.handleBatterySlot();

        long energyCost = (long) Math.ceil(blockEntity.loadedChunks.size() * blockEntity.euPerChunk);
        boolean canAfford = blockEntity.energyStorage.getAmount() >= energyCost;
        if (canAfford && energyCost > 0) {
            canAfford = blockEntity.energyStorage.extract(energyCost, true) >= energyCost;
            if (canAfford) {
                blockEntity.energyStorage.extract(energyCost, false);
            }
        } else if (energyCost == 0) {
            canAfford = true;
        }

        if (canAfford != blockEntity.active) {
            if (canAfford) {
                blockEntity.active = true;
                blockEntity.forceLoadAllChunks();
            } else {
                blockEntity.active = false;
                blockEntity.unforceNonSelfChunks();
            }
            blockEntity.setLit(blockEntity.active);
            blockEntity.setChanged();
        }
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
        if (level == null || level.isClientSide) return;
        ItemStack batteryStack = itemHandler.getStackInSlot(BATTERY_SLOT);
        if (batteryStack.isEmpty()) return;

        if (energyStorage.getAmount() >= getEffectiveCapacity()) return;

        long energyNeeded = getEffectiveCapacity() - energyStorage.getAmount();
        long maxTransfer = Math.min(energyNeeded, getEffectiveMaxReceive());

        if (batteryStack.getItem() == net.minecraft.world.item.Items.REDSTONE) {
            long energyToAdd = Math.min(mio_icif_producer.REDSTONE_ENERGY_VALUE, maxTransfer);
            if (energyToAdd > 0) {
                batteryStack.shrink(1);
                energyStorage.receive(energyToAdd, false);
                setChanged();
            }
            return;
        }

        if (isBattery(batteryStack)) {
            long stored = getBatteryStored(batteryStack);
            if (stored > 0) {
                long chargeRate = getBatteryChargeRate(batteryStack);
                long toExtract = Math.min(stored, Math.min(chargeRate, maxTransfer));
                long extracted = dischargeBattery(batteryStack, toExtract, false);
                if (extracted > 0) {
                    energyStorage.receive(extracted, false);
                    setChanged();
                }
            }
        }
    }

    private boolean isBattery(ItemStack stack) {
        return com.singularity_iteration.mio_icif.api.MioIcifAPI.instance().getItemAPI().isBattery(stack)
            || stack.getItem() == net.minecraft.world.item.Items.REDSTONE;
    }

    private long getBatteryStored(ItemStack stack) {
        return com.singularity_iteration.mio_icif.api.MioIcifAPI.instance().getItemAPI().getBatteryStored(stack);
    }

    private long dischargeBattery(ItemStack stack, long amount, boolean simulate) {
        return com.singularity_iteration.mio_icif.api.MioIcifAPI.instance().getItemAPI().dischargeBattery(stack, amount, simulate);
    }

    private long getBatteryChargeRate(ItemStack stack) {
        return com.singularity_iteration.mio_icif.api.MioIcifAPI.instance().getItemAPI().getChargeRate(stack);
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
        if (level != null && !level.isClientSide && !loadedChunks.isEmpty()) {
            forceLoadAllChunks();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

    public void destroy() {
        if (level != null && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            for (long key : loadedChunks) {
                ChunkPos pos = fromChunkKey(key);
                serverLevel.setChunkForced(pos.x, pos.z, false);
            }
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
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadedChunks.clear();
        CompoundTag chunksTag = tag.getCompound("loadedChunks");
        int count = chunksTag.getInt("count");
        for (int i = 0; i < count; i++) {
            if (chunksTag.contains("c" + i, Tag.TAG_LONG)) {
                loadedChunks.add(chunksTag.getLong("c" + i));
            }
        }
        active = tag.getBoolean("active");

        recalculateUpgradeStats();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.ChunkLoaderMenu(containerId, playerInventory, this, this.containerData);
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
        return energyStorage.extract(amount, false) >= amount;
    }

    @Override
    public java.util.Set<com.singularity_iteration.mio_icif.api.upgrade.tile.UpgradableProperty> getUpgradableProperties() {
        return java.util.EnumSet.of(
            com.singularity_iteration.mio_icif.api.upgrade.tile.UpgradableProperty.ENERGY_STORAGE,
            com.singularity_iteration.mio_icif.api.upgrade.tile.UpgradableProperty.ITEM_CONSUMING,
            com.singularity_iteration.mio_icif.api.upgrade.tile.UpgradableProperty.ITEM_PRODUCING,
            com.singularity_iteration.mio_icif.api.upgrade.tile.UpgradableProperty.TRANSFORMER
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
        long base = energyStorage.getMaxReceive();
        return Math.max(base, getEffectiveCableTier().getPowerRating());
    }

    @Override
    public double getDemandedEnergy() {
        if (isPowerSource) return 0.0D;
        long spaceAvailable = getEffectiveCapacity() - energyStorage.getAmount();
        if (spaceAvailable <= 0) return 0.0D;
        return spaceAvailable;
    }
}
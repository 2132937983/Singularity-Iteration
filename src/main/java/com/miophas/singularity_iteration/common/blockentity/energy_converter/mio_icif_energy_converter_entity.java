package com.miophas.singularity_iteration.common.blockentity.energy_converter;

import com.miophas.singularity_iteration.common.block.energy_converter.mio_icif_block_energy_converter;
import com.miophas.singularity_iteration.core.api.energy.EnergyPacketPolicy;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.energy.IEnergyPacketSource;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.machine.IEnergyConverter;
import com.miophas.singularity_iteration.core.api.machine.IMachineAPI;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.runtime.PerfCounters;
import com.miophas.singularity_iteration.core.runtime.energy.FeLedger;
import com.miophas.singularity_iteration.core.runtime.energy.GridExchangePort;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import com.miophas.singularity_iteration.core.runtime.energy.grid.FECompatTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.1: the grid-tie interface (block id {@code energy_converter}, the Energy Converter until 0.1.11.0). It is the
 * one connection between the SI grid and the cables of other mods: its front faces the other network, its other five
 * faces join the SI grid. SI cables take no FE themselves.
 *
 * <ul>
 *   <li>Export: the SI side is a receiver of the storage class (it takes the surplus after the loads); the front hands
 *       what the buffer holds to the block in front.</li>
 *   <li>Import: the front takes FE from the block in front; the SI side is a generator.</li>
 *   <li>Bidirectional: the interface keeps its buffer between two edges around half charge (dead band 10, 25 or 50 % of
 *       the buffer). Above the upper edge the front hands the excess out, below the lower edge it takes FE in. On the SI
 *       side it takes only the surplus of generators (nothing from storage blocks) and supplies only loads (it charges no
 *       storage), so energy cannot circle between the two networks through SI storage.</li>
 *   <li>A limit (32, 128, 512 or 2,048 EU/t) bounds both sides: the SI input and the FE taken in share one input budget
 *       per tick, the SI output and the FE handed out one output budget. The SI side sends one packet of the limit per
 *       tick.</li>
 * </ul>
 * Sneak + right-click cycles the mode (export, import, bidirectional 10 / 25 / 50 %); right-click cycles the limit.
 * Lossless: 4 FE = 1 EU.
 */
@SuppressWarnings("null")
public class mio_icif_energy_converter_entity extends AbstractEnergyBlockEntity
        implements IEnergyConverter, GridExchangePort, IEnergyPacketSource {

    /** FE per EU (the one ratio of the mod). */
    public static final int EU_TO_FE_RATE = com.miophas.singularity_iteration.core.api.energy.storage.ILongEnergyStorage.FE_PER_EU;
    /** The buffer in EU (until 0.1.11.0: 100,000 EU and a separate buffer of 400,000 FE). */
    public static final long CAPACITY = 200_000;
    /** The limits in EU/t, in cycle order. */
    public static final long[] LIMITS = {32, 128, 512, 2048};
    /** The dead bands of the bidirectional mode in percent of the buffer, in cycle order. */
    public static final int[] BANDS = {10, 25, 50};

    /** The direction of the energy across the interface. */
    public enum Mode { EXPORT, IMPORT, BIDIRECTIONAL }

    private Mode mode = Mode.EXPORT;
    private int band = 25;
    private long limit = 2048;
    /** The kept adapter of the block in front, and the face it was made for. */
    private FECompatTile frontAdapter;
    private Direction frontAdapterFace;
    private IEnergyStorage frontPort;

    public mio_icif_energy_converter_entity(BlockPos pos, BlockState state) {
        this(pos, state, com.miophas.singularity_iteration.common.registry.mio_icif_block_entities.ENERGY_CONVERTER_ENTITY_TYPE.get());
    }

    public mio_icif_energy_converter_entity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, CAPACITY, 2048, 2048, CableTier.EV);
        apply();
    }

    // ------------------------------------------------------------------ settings

    public Mode mode() { return mode; }
    public int band() { return band; }
    public long limit() { return limit; }

    /** The mode flag of the block state: false for export, true for import and bidirectional. */
    public boolean getMode() { return mode != Mode.EXPORT; }

    /** Sets the mode and the dead band (10, 25 or 50; other values become 25). */
    public void setMode(Mode next, int nextBand) {
        mode = next == null ? Mode.EXPORT : next;
        band = validBand(nextBand);
        apply();
        changedSettings();
    }

    /** The next mode in the cycle: export, import, bidirectional 10 / 25 / 50 %, export. */
    public void cycleMode() {
        switch (mode) {
            case EXPORT -> setMode(Mode.IMPORT, band);
            case IMPORT -> setMode(Mode.BIDIRECTIONAL, BANDS[0]);
            case BIDIRECTIONAL -> {
                int index = java.util.Arrays.binarySearch(BANDS, band);
                if (index >= 0 && index + 1 < BANDS.length) setMode(Mode.BIDIRECTIONAL, BANDS[index + 1]);
                else setMode(Mode.EXPORT, band);
            }
        }
    }

    /** Sets the limit in EU/t (one of {@link #LIMITS}; other values become 2,048). */
    public void setLimit(long next) {
        limit = validLimit(next);
        apply();
        changedSettings();
    }

    /** The next limit in the cycle. */
    public void cycleLimit() {
        for (int i = 0; i < LIMITS.length; i++) if (LIMITS[i] == limit) { setLimit(LIMITS[(i + 1) % LIMITS.length]); return; }
        setLimit(LIMITS[0]);
    }

    private static int validBand(int value) {
        for (int candidate : BANDS) if (candidate == value) return value;
        return 25;
    }

    private static long validLimit(long value) {
        for (long candidate : LIMITS) if (candidate == value) return value;
        return 2048;
    }

    /** The budgets and the generator flag follow the settings. */
    private void apply() {
        energyStorage.setMaxReceive(limit);
        energyStorage.setMaxExtract(limit);
        if (mode == Mode.IMPORT) setAsPowerSource(limit); else setAsConsumer();
    }

    private void changedSettings() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            if (state.hasProperty(mio_icif_block_energy_converter.MODE) && state.getValue(mio_icif_block_energy_converter.MODE) != getMode()) {
                level.setBlock(worldPosition, state.setValue(mio_icif_block_energy_converter.MODE, getMode()), 3);
            } else {
                level.sendBlockUpdated(worldPosition, state, state, 3);
            }
            // new SI faces, budgets or packet size: the grid reads the endpoint again
            refreshRegistration();
        }
    }

    /** The lower edge of the bidirectional band in EU. */
    public long lowerEdge() { return CAPACITY * (100 - band) / 200; }

    /** The upper edge of the bidirectional band in EU. */
    public long upperEdge() { return CAPACITY * (100 + band) / 200; }

    public Direction getFrontFacing() {
        return getBlockState().getValue(mio_icif_block_energy_converter.FACING);
    }

    // ------------------------------------------------------------------ the SI side

    @Override
    public Direction exchangeFace() { return getFrontFacing(); }

    private int siFaces() { return 63 & ~(1 << getFrontFacing().ordinal()); }

    @Override
    public int exchangeInputs() { return mode == Mode.IMPORT ? 0 : siFaces(); }

    @Override
    public int exchangeOutputs() { return mode == Mode.EXPORT ? 0 : siFaces(); }

    @Override
    public boolean exchangeLoadsOnly() { return mode == Mode.BIDIRECTIONAL; }

    @Override
    public boolean exchangeSurplusOnly() { return mode == Mode.BIDIRECTIONAL; }

    /** One packet of the limit per tick, partial packets allowed. */
    @Override
    public EnergyPacketPolicy getEnergyPacketPolicy() { return new EnergyPacketPolicy(limit, 1, true); }

    /** The front is not an SI face. */
    @Override
    public boolean canConnect(@Nullable Direction side) {
        return side == null || side != getFrontFacing();
    }

    /** The EU capability of a face: the SI faces only (the front is FE only). */
    @SuppressWarnings("deprecation")
    public @Nullable com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage siPort(@Nullable Direction side) {
        return side != null && side == getFrontFacing() ? null : getEnergyStorageCapability(side);
    }

    // ------------------------------------------------------------------ the front

    /**
     * Moves FE across the front once per tick: export hands out what the buffer holds, import takes in what the buffer
     * has room for, bidirectional moves the buffer back to its band. Nothing toward an SI node (the grid reaches it).
     */
    public void tick() {
        if (!(level instanceof ServerLevel server) || isRemoved()) return;
        Direction front = getFrontFacing();
        BlockPos at = worldPosition.relative(front);
        if (server.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4) == null
                || !server.shouldTickBlocksAt(ChunkPos.asLong(at))) return;
        if (IndependentSiEnergy.controls(server.getBlockState(at))) return;
        if (frontAdapter == null || frontAdapterFace != front) {
            frontAdapter = new FECompatTile(server, at, front).retain();
            frontAdapterFace = front;
        }
        var ledger = scexFeBridge().ledger();
        int moved = 0;
        switch (mode) {
            case EXPORT -> moved = ledger.push(frontAdapter, this::live);
            case IMPORT -> moved = ledger.pull(frontAdapter, 0, this::live);
            case BIDIRECTIONAL -> {
                long stored = energyStorage.getAmount();
                if (stored > upperEdge()) moved = ledger.push(capped(frontAdapter, toFe(stored - upperEdge())), this::live);
                else if (stored < lowerEdge()) moved = ledger.pull(capped(frontAdapter, toFe(lowerEdge() - stored)), 0, this::live);
            }
        }
        if (moved > 0) PerfCounters.add(PerfCounters.Kind.FE_GRID_TIE, moved);
    }

    private boolean live() { return !isRemoved() && level != null; }

    private static long toFe(long eu) { return eu > Long.MAX_VALUE / EU_TO_FE_RATE ? Long.MAX_VALUE : eu * EU_TO_FE_RATE; }

    /** {@code target} limited to {@code fe} FE per call. */
    private static IEnergyStorage capped(IEnergyStorage target, long fe) {
        int cap = (int) Math.min(Integer.MAX_VALUE, Math.max(0, fe));
        return new IEnergyStorage() {
            @Override public int receiveEnergy(int amount, boolean simulate) { return target.receiveEnergy(Math.min(amount, cap), simulate); }
            @Override public int extractEnergy(int amount, boolean simulate) { return target.extractEnergy(Math.min(amount, cap), simulate); }
            @Override public int getEnergyStored() { return target.getEnergyStored(); }
            @Override public int getMaxEnergyStored() { return target.getMaxEnergyStored(); }
            @Override public boolean canReceive() { return cap > 0 && target.canReceive(); }
            @Override public boolean canExtract() { return cap > 0 && target.canExtract(); }
        };
    }

    /**
     * The FE capability of the front (the cables of other mods push into it or pull from it), or {@code null} on the
     * other faces. Import takes FE, export gives it, bidirectional only toward its band. Same budgets as the transfer of
     * {@link #tick()}.
     */
    public @Nullable IEnergyStorage frontFe(@Nullable Direction side) {
        if (side == null || side != getFrontFacing()) return null;
        if (frontPort == null) frontPort = new IEnergyStorage() {
            @Override public int receiveEnergy(int amount, boolean simulate) {
                if (mode == Mode.EXPORT || amount <= 0) return 0;
                long room = mode == Mode.IMPORT ? amount : toFe(Math.max(0, lowerEdge() - energyStorage.getAmount()));
                int taken = scexFeBridge().ledger().receive((int) Math.min(amount, room), simulate);
                if (!simulate && taken > 0) PerfCounters.add(PerfCounters.Kind.FE_GRID_TIE, taken);
                return taken;
            }
            @Override public int extractEnergy(int amount, boolean simulate) {
                if (mode == Mode.IMPORT || amount <= 0) return 0;
                long held = mode == Mode.EXPORT ? amount : toFe(Math.max(0, energyStorage.getAmount() - upperEdge()));
                int given = scexFeBridge().ledger().extract((int) Math.min(amount, held), simulate);
                if (!simulate && given > 0) PerfCounters.add(PerfCounters.Kind.FE_GRID_TIE, given);
                return given;
            }
            @Override public int getEnergyStored() { return FeLedger.fe(energyStorage.scexExactAmount()); }
            @Override public int getMaxEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, toFe(CAPACITY)); }
            @Override public boolean canReceive() { return mode != Mode.EXPORT; }
            @Override public boolean canExtract() { return mode != Mode.IMPORT; }
        };
        return frontPort;
    }

    // ------------------------------------------------------------------ saving

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("GridTieMode", mode.name());
        tag.putInt("GridTieBand", band);
        tag.putLong("GridTieLimit", limit);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("GridTieMode")) {
            readSettings(tag);
        } else {
            // 0.1.11.0 and older: the Energy Converter (Mode true = FE to EU, an import). Its FE buffer joins the EU buffer
            // in whole EU (the new buffer holds both old ones: 100,000 EU + 400,000 FE = 200,000 EU).
            mode = tag.getBoolean("Mode") ? Mode.IMPORT : Mode.EXPORT;
            int fe = tag.getInt("FEStored");
            if (fe >= EU_TO_FE_RATE) apiSetEnergy(Math.min(CAPACITY, apiGetStoredEnergy() + fe / EU_TO_FE_RATE));
        }
        apply();
    }

    private void readSettings(CompoundTag tag) {
        try { mode = Mode.valueOf(tag.getString("GridTieMode")); } catch (IllegalArgumentException ignored) { mode = Mode.EXPORT; }
        band = validBand(tag.getInt("GridTieBand"));
        limit = validLimit(tag.getLong("GridTieLimit"));
    }

    /** The client tag of the parent and the mode flag (the client shows the mode through the block state only). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Mode", getMode());
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ------------------------------------------------------------------ IEnergyConverter

    @Override
    public EnergyType getInputType() { return mode == Mode.IMPORT ? EnergyType.FE : EnergyType.EU; }

    @Override
    public EnergyType getOutputType() { return mode == Mode.IMPORT ? EnergyType.EU : EnergyType.FE; }

    @Override
    public double getConversionRatio() { return mode == Mode.IMPORT ? 1.0 / EU_TO_FE_RATE : EU_TO_FE_RATE; }

    @Override
    public ICableTier getInputCableTier() { return CableTier.EV; }

    @Override
    public ICableTier getOutputCableTier() { return CableTier.EV; }

    /** The buffer in EU (all modes). */
    @Override
    public long getBufferAmount() { return energyStorage.getAmount(); }

    @Override
    public long getBufferCapacity() { return CAPACITY; }

    @Override
    public IMachineAPI.MachineType getMachineType() { return IMachineAPI.MachineType.ENERGY_CONVERTER; }
}

// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.grid;

import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyEmitter;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergySink;
import com.miophas.singularity_iteration.core.api.energy.grid.ILocatableTile;

import com.miophas.singularity_iteration.core.api.energy.storage.EUApi;
import com.miophas.singularity_iteration.core.api.energy.storage.ILongEnergyStorage;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.EnergyCompatibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Live sided capability adapter. It never retains a capability beyond a transaction.
 *
 * <p>This class lets the EU grid deliver energy to a foreign block that has no SI energy node. The
 * grid sees an {@link IEnergySink} at {@link #getPos()}. Each call looks up the capabilities of the
 * target block again. The adapter keeps no reference to a capability between calls.
 *
 * <h2>Units</h2>
 * <p>1 EU is 4 FE. The {@link IEnergySink} methods use EU as {@code double}. The {@link IEnergyStorage}
 * methods use FE as {@code int}.
 *
 * <h2>Capability layers</h2>
 * <p>The target can expose several capabilities on the same face. The receive path tries them in
 * this order: an AE2 network sink, {@link EUApi#SIDED}, {@link ILongEnergyStorage#BLOCK},
 * {@link Capabilities.EnergyStorage#BLOCK}, and last {@link EnergyCompatibility#findEuStorage}. If the
 * AE2 sink exists, only that sink is used. Otherwise a layer that is absent, cannot receive, or
 * accepts 0 FE passes the request to the next layer. The extract and stored-energy paths use only
 * the first layer that is present.
 *
 * <h2>Chunk and thread rules</h2>
 * <p>Each public call checks {@link #loaded()} first and returns 0 or false if the target is not
 * available. {@link #loaded()} never loads a chunk. Foreign code can unload the target during a call,
 * so the check runs again after each foreign call.
 *
 * <h2>Debug output</h2>
 * <p>The JVM property {@code -Dsingularity_iteration.wireConnectDebug=true} enables a layer report in
 * the log. The wires use the same property.
 */
public final class FECompatTile implements IEnergySink, ILocatableTile, IEnergyStorage {
    private static final int FE_PER_EU = com.miophas.singularity_iteration.core.api.energy.storage.ILongEnergyStorage.FE_PER_EU;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(FECompatTile.class);
    /** Shares the JVM property with the wires: -Dsingularity_iteration.wireConnectDebug=true */
    private static final boolean DEBUG =
        Boolean.parseBoolean(System.getProperty("singularity_iteration.wireConnectDebug", "false"));
    /** Debug keys already logged. The set is cleared when it passes 512 entries, to bound memory. */
    private static final java.util.Set<String> DEBUG_ONCE =
        java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    private final Level world;
    private final BlockPos pos;
    /** Face of the target block that receives energy. It points from the target back to the wire. */
    private final Direction input;
    /** True while the energy engine holds this adapter as a registered tile. */
    private boolean registered;
    /**
     * 0.1.10.39: FE that the target pushed into an SI cable (universal cables) and the grid has not taken yet. The FE import pass
     * takes it first, like FE extracted from the target (voltage 0, after the native machines). Not saved: at most this much is
     * lost when the world stops.
     */
    public static final int INBOX_FE = 32_768;
    private int inbox;

    /** direction is the existing SI caller's direction from its wire to the receiver. */
    public FECompatTile(Level world, BlockPos pos, Direction direction) {
        this.world = java.util.Objects.requireNonNull(world);
        this.pos = pos.immutable();
        this.input = java.util.Objects.requireNonNull(direction).getOpposite();
    }

    /**
     * Debug aid for the fault "the wire connects, but the machine does not charge". It logs which
     * capability layer of this contact point is present. It logs each key (phase, position, side,
     * simulate) once. It has no effect when {@link #DEBUG} is false. It never throws.
     */
    private void debugLayers(String phase, int amount, boolean simulate) {
        if (!DEBUG) return;
        String key = phase + "|" + pos + "|" + input + "|" + simulate;
        if (DEBUG_ONCE.size() > 512) {
            DEBUG_ONCE.clear();
        }
        if (!DEBUG_ONCE.add(key)) return;
        try {
            var eu = world.getCapability(EUApi.SIDED, pos, input);
            var wide = world.getCapability(ILongEnergyStorage.BLOCK, pos, input);
            var fe = world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, input);
            var compat = EnergyCompatibility.get().findEuStorage(world, pos, input);
            var be = world.getBlockEntity(pos);
            LOG.warn("[FECompat-DEBUG] {} {} (side={}, request={} FE, simulate={})"
                    + " | EUApi={} ILong={} FE={}(canReceive={}) compat={}(canReceive={}) | BE={}",
                phase, pos, input, amount, simulate,
                eu != null, wide != null,
                fe != null, fe != null && fe.canReceive(),
                compat != null, compat != null && compat.canReceive(),
                be == null ? "null" : be.getClass().getName());
        } catch (RuntimeException error) {
            LOG.warn("[FECompat-DEBUG] {} {} layer diagnosis failed: {}", phase, pos, error.toString());
        }
    }
    /**
     * Returns true if the caller is on the server thread, the target chunk is loaded now, and the
     * chunk ticks block entities. This method never loads a chunk. A client level always gives false.
     */
    public boolean loaded() {
        if (!(world instanceof ServerLevel level) || !level.getServer().isSameThread()) return false;
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null
            && level.shouldTickBlocksAt(net.minecraft.world.level.ChunkPos.asLong(pos));
    }
    // Call only after the public entry has checked thread/chunk availability.
    // Returns the first present storage layer, or null.
    private IEnergyStorage capabilityWhenLoaded() {
        var eu = cap(EUApi.SIDED);
        if (eu != null) return eu;
        var wide = cap(ILongEnergyStorage.BLOCK);
        if (wide != null) return wide;
        var fe = cap(Capabilities.EnergyStorage.BLOCK);
        if (fe != null) return fe;
        return EnergyCompatibility.get().findEuStorage(world, pos, input);
    }

    /**
     * 0.1.10.39: capability lookups through NeoForge's {@code BlockCapabilityCache} (invalidated by the game when the block or
     * its capabilities change). Each public call made up to four uncached lookups before.
     */
    private final java.util.Map<net.neoforged.neoforge.capabilities.BlockCapability<?, ?>,
        net.neoforged.neoforge.capabilities.BlockCapabilityCache<?, Direction>> caches = new java.util.IdentityHashMap<>(4);

    @SuppressWarnings("unchecked")
    private <T> T cap(net.neoforged.neoforge.capabilities.BlockCapability<T, Direction> capability) {
        if (!(world instanceof net.minecraft.server.level.ServerLevel server)) return world.getCapability(capability, pos, input);
        var cache = (net.neoforged.neoforge.capabilities.BlockCapabilityCache<T, Direction>) caches.computeIfAbsent(capability,
            c -> net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(capability, server, pos, input));
        return cache.getCapability();
    }
    // Call only after the public entry has checked thread/chunk availability.
    // Returns the AE2 network sink at the position, or null.
    private IEnergySink ae2WhenLoaded() {
        return EnergyCompatibility.get().findNetworkSink(world, pos);
    }
    /** Returns true if the target is loaded and has a storage layer or an AE2 sink. */
    public boolean isValid() {
        if (!loaded()) return false;
        if (inbox > 0) return true;   // 0.1.10.39: a target that only pushes keeps its contact while FE waits
        boolean valid = capabilityWhenLoaded() != null || ae2WhenLoaded() != null;
        return valid && loaded();
    }
    /** 0.1.10.39: the target pushes {@code fe} into the cable; returns the FE accepted (the inbox holds at most INBOX_FE). */
    public int offerInbox(int fe, boolean simulate) {
        int accepted = Math.max(0, Math.min(fe, INBOX_FE - inbox));
        if (!simulate) inbox += accepted;
        return accepted;
    }
    public int inbox() { return inbox; }
    public boolean isRegistered() { return registered; }
    public void setRegistered(boolean value) { registered = value; }
    /** Returns the face of the target that receives energy. */
    public Direction inputSide() { return input; }
    @Override public Level getWorld() { return world; }
    @Override public BlockPos getPos() { return pos; }
    /** Accepts energy only on the input face, and only while some layer can receive. */
    @Override public boolean acceptsEnergyFrom(IEnergyEmitter source, Direction side) { return side == input && canReceive(); }
    /** Returns the demand in EU: a simulated FE receive of the maximum amount, divided by 4. */
    @Override public double getDemandedEnergy() { return receiveEnergy(Integer.MAX_VALUE, true) / (double) FE_PER_EU; }
    /** Returns 14, the highest sink tier. The adapter accepts any packet size. */
    @Override public int getSinkTier() { return 14; }
    /**
     * Delivers {@code amount} EU on {@code side}. Returns the EU that was not accepted.
     * The offer is rounded down to whole FE. The fraction below 0.25 EU is returned as not accepted.
     * A wrong side, a non-finite amount, or an amount of 0 or less returns the full amount.
     */
    @Override public double injectEnergy(Direction side, double amount, double voltage) {
        if (side != input || !Double.isFinite(amount) || amount <= 0) return amount;
        int offer = (int) Math.min(Integer.MAX_VALUE, Math.floor(amount * FE_PER_EU));
        return amount - receiveEnergy(offer, false) / (double) FE_PER_EU;
    }
    /**
     * Receives up to {@code amount} FE. Returns the FE accepted, in the range {@code 0..amount}.
     *
     * <p>If an AE2 sink exists, the offer is limited to its demand and the call goes only to that sink.
     * Otherwise the layers are tried in order until one accepts more than 0 FE.
     *
     * @throws IllegalStateException if a foreign layer reports an amount out of range
     */
    @Override public int receiveEnergy(int amount, boolean simulate) {
        if (amount <= 0 || !loaded()) return 0;
        var sink = ae2WhenLoaded();
        if (sink != null) {
            int offered = (int)Math.min(amount, Math.floor(Math.max(0, sink.getDemandedEnergy()) * FE_PER_EU));
            // The demand query is foreign code. Check again that the target is still loaded.
            if (!loaded()) return 0;
            if (simulate || offered == 0) return offered;
            double rejected = sink.injectEnergy(input, offered / (double) FE_PER_EU, offered / (double) FE_PER_EU);
            if (!Double.isFinite(rejected) || rejected < 0 || rejected > offered / (double) FE_PER_EU)
                throw new IllegalStateException("Invalid AE2 energy receipt at " + pos);
            // FE has integer granularity. Round the debit upwards, never create EU by truncation.
            return Math.min(offered, (int)Math.ceil((offered / (double) FE_PER_EU - rejected) * FE_PER_EU));
        }

        // Try each layer in order and fall back to the next one. Some mods (for example Modern
        // Industrialization) expose their own capability and an FE adapter at the same time. The FE
        // adapter can refuse energy on a face (canReceive=false, or an offer of 0). An adapter that
        // used only the first non-null capability showed the fault "the wire connects and the
        // machine faces the wire, but it never charges". When a layer accepts no energy, the next
        // layer must be tried. Each layer is looked up only when it is needed, so the common path
        // has no extra cost.
        int accepted = receiveThrough(world.getCapability(EUApi.SIDED, pos, input), amount, simulate);
        if (accepted >= 0) return accepted;
        accepted = receiveThrough(world.getCapability(ILongEnergyStorage.BLOCK, pos, input), amount, simulate);
        if (accepted >= 0) return accepted;
        accepted = receiveThrough(world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, input), amount, simulate);
        if (accepted >= 0) return accepted;
        accepted = receiveThrough(EnergyCompatibility.get().findEuStorage(world, pos, input), amount, simulate);
        if (accepted >= 0) return accepted;

        // No layer accepted energy.
        debugLayers("no energy layer accepted", amount, simulate);
        return 0;
    }

    /**
     * Offers {@code amount} FE to one layer.
     *
     * @return the amount that this layer accepted, in the range {@code 1..amount}. Returns -1 if the
     *         layer is absent, cannot receive, or accepts 0 FE. The caller must then try the next layer.
     * @throws IllegalStateException if the layer reports an amount out of range
     */
    private int receiveThrough(IEnergyStorage target, int amount, boolean simulate) {
        // Providers and policy callbacks are foreign code. They can unload the target.
        // Thus loaded() runs before and after canReceive().
        if (target == null || !loaded() || !target.canReceive() || !loaded()) return -1;
        int accepted = target.receiveEnergy(amount, simulate);
        if (accepted < 0 || accepted > amount) throw new IllegalStateException("Invalid external FE receipt at " + pos);
        // This layer accepted nothing (also a simulated offer of 0). Try the next layer.
        return accepted > 0 ? accepted : -1;
    }

    /**
     * Returns true if any layer can receive. Thus a layer with higher priority that refuses energy now
     * does not hide a lower layer that accepts energy.
     */
    private boolean anyLayerCanReceive() {
        if (capabilityCanReceive(cap(EUApi.SIDED))) return true;
        if (capabilityCanReceive(cap(ILongEnergyStorage.BLOCK))) return true;
        if (capabilityCanReceive(cap(Capabilities.EnergyStorage.BLOCK))) return true;
        return capabilityCanReceive(EnergyCompatibility.get().findEuStorage(world, pos, input));
    }

    /** Returns true if the layer is present and can receive, and the target is still loaded after the query. */
    private boolean capabilityCanReceive(IEnergyStorage target) {
        return target != null && loaded() && target.canReceive() && loaded();
    }
    /**
     * Extracts up to {@code amount} FE from the first present layer. Returns the FE extracted, in the
     * range {@code 0..amount}.
     *
     * @throws IllegalStateException if the layer reports an amount out of range
     */
    @Override public int extractEnergy(int amount, boolean simulate) {
        if (amount <= 0 || !loaded()) return 0;
        int fromInbox = Math.min(amount, inbox);   // 0.1.10.39: what the target pushed into the cable comes first
        if (!simulate) inbox -= fromInbox;
        if (fromInbox == amount) return fromInbox;
        var target = capabilityWhenLoaded();
        if (target == null || !loaded() || !target.canExtract() || !loaded()) return fromInbox;
        int extracted = target.extractEnergy(amount - fromInbox, simulate);
        if (extracted < 0 || extracted > amount - fromInbox) throw new IllegalStateException("Invalid external FE extraction at " + pos);
        return fromInbox + extracted;
    }
    /** Returns the FE stored in the first present layer, or 0. */
    @Override public int getEnergyStored() {
        if (!loaded()) return 0;
        var target = capabilityWhenLoaded();
        return target == null || !loaded() ? 0 : target.getEnergyStored();
    }
    /** Returns the FE capacity of the first present layer, or 0. */
    @Override public int getMaxEnergyStored() {
        if (!loaded()) return 0;
        var target = capabilityWhenLoaded();
        return target == null || !loaded() ? 0 : target.getMaxEnergyStored();
    }
    /** Returns true if the AE2 sink has demand, or, without an AE2 sink, if any layer can receive. */
    @Override public boolean canReceive() {
        if (!loaded()) return false;
        var sink = ae2WhenLoaded();
        if (sink != null) return sink.getDemandedEnergy() > 0 && loaded();
        if (anyLayerCanReceive()) return true;
        debugLayers("canReceive=false", 0, true);
        return false;
    }
    /** Returns true if the first present layer can extract. */
    @Override public boolean canExtract() {
        if (!loaded()) return false;
        if (inbox > 0) return true;
        var target = capabilityWhenLoaded();
        return target != null && loaded() && target.canExtract();
    }
}

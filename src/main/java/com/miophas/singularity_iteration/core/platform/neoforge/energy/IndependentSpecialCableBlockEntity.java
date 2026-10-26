// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.platform.neoforge.energy;

import com.miophas.singularity_iteration.core.runtime.energy.GridAlarms;
import com.miophas.singularity_iteration.core.runtime.energy.engine.ProtectionRelay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Independent special-wire state, based solely on retained public gameplay observations.
 *
 * <p>0.1.10.22 (G5): a special cable can also be a protective cable (a breaker or a fuse) with a
 * {@link ProtectionRelay}. It conducts while the relay is closed and no redstone signal holds it open. The engine
 * feeds it the energy of each delivery through it ({@link #carry}), opens it at once for an overvoltage
 * ({@link #tripForVoltage}) and evaluates it at the end of each tick ({@link #evaluateProtection}); a change of
 * conduction is published at the next tick start like a splitter cable.
 */
public final class IndependentSpecialCableBlockEntity extends BlockEntity {
    private final boolean detector;
    private final ProtectionRelay relay;
    private boolean active;
    private long samplingOrigin, lastTransfer = Long.MIN_VALUE, lastSample = Long.MIN_VALUE;
    private int restoredAge;

    public IndependentSpecialCableBlockEntity(BlockEntityType<?> type, BlockPos at, BlockState state, boolean detector) {
        super(type, at, state);
        this.detector = detector;
        this.relay = null;
        active = !detector;
    }

    /** 0.1.10.22 (G5): a protective cable (breaker or fuse). */
    public IndependentSpecialCableBlockEntity(BlockEntityType<?> type, BlockPos at, BlockState state, ProtectionRelay.Kind protection) {
        super(type, at, state);
        this.detector = false;
        this.relay = new ProtectionRelay(protection);
        active = true;
    }

    private ServerLevel serverLevel() {
        if (!(level instanceof ServerLevel world) || !world.getServer().isSameThread())
            throw new IllegalStateException("Special cable requires its server thread");
        return world;
    }

    public boolean detector() { return detector; }
    /** 0.1.10.22: true for a breaker or fuse cable. */
    public boolean protective() { return relay != null; }
    /** 0.1.10.22: the relay of a protective cable, or null. Read-only use outside the server thread. */
    public ProtectionRelay relay() { return relay; }
    public boolean conducts() { return detector || active; }
    public boolean conductsNow() {
        return detector || closedBy(serverLevel());
    }

    /** Splitter and breaker: no redstone signal (and a closed relay); fuse: not blown (it ignores redstone). */
    private boolean closedBy(ServerLevel world) {
        if (relay == null) return !world.hasNeighborSignal(worldPosition);
        if (!relay.conducts()) return false;
        return relay.kind() == ProtectionRelay.Kind.FUSE || !world.hasNeighborSignal(worldPosition);
    }
    public int signal() { return detector && active ? 15 : 0; }

    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel) refreshInput();
    }

    public boolean refreshInput() {
        var world = serverLevel();
        return !detector && setActive(closedBy(world));
    }

    public void delivered() { lastTransfer = serverLevel().getGameTime(); }

    /** 0.1.10.22 (G5): energy of one delivery through this protective cable in this tick. */
    public void carry(double energy, double packet) {
        if (relay != null) relay.carry(serverLevel().getGameTime(), energy, packet);
    }

    /** 0.1.10.22 (G5): true when a packet of {@code packet} EU opens this protective cable at once. */
    public boolean voltageExceeded(double packet) {
        return relay != null && relay.voltageExceeded(packet);
    }

    /** 0.1.10.22 (G5): opens the cable for an overvoltage (after the engine committed the delivery). */
    public void tripForVoltage() {
        if (relay == null) return;
        var world = serverLevel();
        report(world, relay.tripNow(world.getGameTime(), ProtectionRelay.Cause.OVERVOLTAGE));
    }

    /** 0.1.10.22 (G5): the end of a tick: overload, reclosing, cooling. */
    public void evaluateProtection() {
        if (relay == null) return;
        var world = serverLevel();
        report(world, relay.evaluate(world.getGameTime()));
    }

    /** 0.1.10.22 (G5): settings from the player (server side, checked by the caller). */
    public void configure(long ratedFlow, long ratedVoltage, int delay, boolean autoReclose) {
        if (relay == null) return;
        relay.configure(ratedFlow, ratedVoltage, delay, autoReclose);
        setChanged();
        sync();
    }

    /** 0.1.10.22 (G5): a player closes the open breaker or restores the blown fuse. True when it changed. */
    public boolean resetProtection() {
        if (relay == null) return false;
        var world = serverLevel();
        var event = relay.reset(world.getGameTime());
        if (event == ProtectionRelay.Event.NONE) return false;
        report(world, event);
        return true;
    }

    private void report(ServerLevel world, ProtectionRelay.Event event) {
        if (event == ProtectionRelay.Event.NONE) return;
        GridAlarms.Type type = switch (event) {
            case TRIP -> GridAlarms.Type.BREAKER_TRIP;
            case RECLOSE -> GridAlarms.Type.BREAKER_RECLOSE;
            case LOCKOUT -> GridAlarms.Type.BREAKER_LOCKOUT;
            case BLOWN -> GridAlarms.Type.FUSE_BLOWN;
            default -> GridAlarms.Type.BREAKER_RESET;
        };
        double value = event == ProtectionRelay.Event.RESET || event == ProtectionRelay.Event.RECLOSE ? 0
            : relay.lastCause() == ProtectionRelay.Cause.OVERVOLTAGE ? relay.lastVoltage() : relay.lastFlow();
        GridAlarms.record(world, type, worldPosition, relay.lastCause().ordinal(), value);
        boolean opened = event == ProtectionRelay.Event.TRIP || event == ProtectionRelay.Event.LOCKOUT || event == ProtectionRelay.Event.BLOWN;
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.5, z = worldPosition.getZ() + 0.5;
        if (opened) {
            world.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, x, y, z, 12, 0.2, 0.2, 0.2, 0.15);
            world.playSound(null, worldPosition, event == ProtectionRelay.Event.BLOWN ? net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH
                : net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.4F);
        } else {
            world.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_OPEN,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.6F);
        }
        setChanged();
        sync();
    }

    private void sync() {
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    public void sampleDetector() {
        var world = serverLevel();
        long now = world.getGameTime();
        if (!detector || now == lastSample || now <= samplingOrigin || (now - samplingOrigin) % 32 != 0) return;
        lastSample = now;
        // IC2 polls the latest completed flow tick every 32 ticks; it does not
        // latch any pulse occurring anywhere inside the whole polling interval.
        // A START catch-up may already have delivered during the current tick.
        setActive(lastTransfer != Long.MIN_VALUE && lastTransfer >= now - 1);
    }

    private boolean setActive(boolean value) {
        if (active == value) { syncBlockState(); return false; }
        active = value;
        setChanged();
        if (!syncBlockState()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        if (detector) level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        return true;
    }

    private boolean syncBlockState() {
        var state = getBlockState();
        var property = state.getBlock().getStateDefinition().getProperty("active");
        if (property instanceof net.minecraft.world.level.block.state.properties.BooleanProperty flag && state.getValue(flag) != active)
            return level.setBlock(worldPosition, state.setValue(flag, active), 2);
        return false;
    }

    @Override public void setLevel(Level newLevel) {
        if (level != newLevel) {
            samplingOrigin = newLevel.getGameTime() - restoredAge;
            lastTransfer = Long.MIN_VALUE;
            lastSample = Long.MIN_VALUE;
        }
        super.setLevel(newLevel);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("active")) active = tag.getBoolean("active");
        restoredAge = Math.floorMod(tag.getInt("scex_poll_age"), 32);
        if (level != null) samplingOrigin = level.getGameTime() - restoredAge;
        lastTransfer = Long.MIN_VALUE;
        lastSample = Long.MIN_VALUE;
        if (relay != null && tag.contains("protection")) relay.load(tag.getLongArray("protection"));
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("active", active);
        tag.putInt("scex_poll_age", level == null ? restoredAge : (int)Math.floorMod(level.getGameTime() - samplingOrigin, 32L));
        // The new adapter owns these ordinary public saved fields. Other old-world migration is separate.
        if (detector) tag.putInt("RedstoneLevel", signal());
        if (relay != null) tag.putLongArray("protection", relay.save());
    }

    /**
     * 0.1.10.17 (E-2): the client gets the switch state only; the poll age and the detector level are server data.
     * 0.1.10.22: a protective cable also sends its settings and state (for its settings screen).
     */
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("active", active);
        if (relay != null) tag.putLongArray("protection", relay.save());
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}

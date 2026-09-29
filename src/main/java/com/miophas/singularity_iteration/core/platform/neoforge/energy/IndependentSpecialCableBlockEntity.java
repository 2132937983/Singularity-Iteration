// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.platform.neoforge.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Independent special-wire state, based solely on retained public gameplay observations. */
public final class IndependentSpecialCableBlockEntity extends BlockEntity {
    private final boolean detector;
    private boolean active;
    private long samplingOrigin, lastTransfer = Long.MIN_VALUE, lastSample = Long.MIN_VALUE;
    private int restoredAge;

    public IndependentSpecialCableBlockEntity(BlockEntityType<?> type, BlockPos at, BlockState state, boolean detector) {
        super(type, at, state);
        this.detector = detector;
        active = !detector;
    }

    private ServerLevel serverLevel() {
        if (!(level instanceof ServerLevel world) || !world.getServer().isSameThread())
            throw new IllegalStateException("Special cable requires its server thread");
        return world;
    }

    public boolean detector() { return detector; }
    public boolean conducts() { return detector || active; }
    public boolean conductsNow() { return detector || !serverLevel().hasNeighborSignal(worldPosition); }
    public int signal() { return detector && active ? 15 : 0; }

    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel) refreshInput();
    }

    public boolean refreshInput() {
        var world = serverLevel();
        return !detector && setActive(!world.hasNeighborSignal(worldPosition));
    }

    public void delivered() { lastTransfer = serverLevel().getGameTime(); }

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
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("active", active);
        tag.putInt("scex_poll_age", level == null ? restoredAge : (int)Math.floorMod(level.getGameTime() - samplingOrigin, 32L));
        // The new adapter owns these ordinary public saved fields. Other old-world migration is separate.
        if (detector) tag.putInt("RedstoneLevel", signal());
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}

package com.miophas.singularity_iteration.core.runtime.heat;

import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.heat.IHeatAPI;
import com.miophas.singularity_iteration.core.api.heat.IHeatStorageAccess;
import com.miophas.singularity_iteration.core.api.machine.IHeatGeneratorBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Heat API implementation.
 *
 * <p>A position holds a heat block in either of two ways:
 * <ul>
 *   <li>its block entity implements the core API heat interfaces ({@code core.api.heat.IHeatStorage},
 *       {@code IHeatSource}, {@code IHeatConductor}), read through {@link HeatBridge};</li>
 *   <li>it exposes {@link IMioIcifCapabilities#HEAT_STORAGE_BLOCK} on some face. All of SI's own heat blocks (heat
 *       generators, Stirling generators, boilers, blast furnaces, reactors...) work only this way.</li>
 * </ul>
 *
 * <p>0.1.11.87: the capability way is new (as for the kinetic API in 0.1.11.86). Before, only the core API interfaces
 * counted, so {@code hasHeatTile}, {@code getHeatStorage}, {@code getHeatConnections}, {@code insertHeat} and
 * {@code extractHeat} saw none of SI's heat blocks.
 */
public class HeatAPIImpl implements IHeatAPI {

    /** The faces tried for the capability: unsided first, then the six faces. */
    private static final Direction[] FACES = {null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private static boolean isBridged(BlockEntity be) {
        return HeatBridge.isStorage(be) || HeatBridge.isSource(be) || HeatBridge.isConductor(be);
    }

    private static boolean hasCapability(Level world, BlockPos pos) {
        for (Direction face : FACES) {
            if (world.getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, pos, face) != null) return true;
        }
        return false;
    }

    @Override
    public Optional<IHeatStorageAccess> getHeatStorage(Level world, BlockPos pos) {
        if (world.isClientSide) return Optional.empty();
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return Optional.empty();
        if (HeatBridge.isStorage(be)) return Optional.of(new HeatStorageAccessImpl(be));
        return hasCapability(world, pos) ? Optional.of(new CapabilityAccess(world, pos, be)) : Optional.empty();
    }

    @Override
    public boolean hasHeatTile(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return false;
        return isBridged(be) || hasCapability(world, pos);
    }

    @Override
    public Collection<Direction> getHeatConnections(Level world, BlockPos pos) {
        if (world.isClientSide) return Collections.emptyList();
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return Collections.emptyList();
        List<Direction> connections = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            BlockPos at = pos.relative(dir);
            BlockEntity neighbor = world.getBlockEntity(at);
            if (neighbor == null) continue;
            // a core API block has no faces; a capability block connects through its face turned to pos
            if (isBridged(neighbor) || world.getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, at, dir.getOpposite()) != null) {
                connections.add(dir);
            }
        }
        return Collections.unmodifiableList(connections);
    }

    private static void setStored(IHeatStorageAccess access, long amount) {
        long clamped = Math.max(0, Math.min(amount, access.getMaxHeatStored()));
        long current = access.getHeatStored();
        if (clamped > current) access.insertHeat(clamped - current, false);
        else if (clamped < current) access.extractHeat(current - clamped, false);
    }

    private static class HeatStorageAccessImpl implements IHeatStorageAccess {
        private final BlockEntity be;

        HeatStorageAccessImpl(BlockEntity be) { this.be = be; }

        @Override public Level getWorld() { return be.getLevel(); }
        @Override public BlockPos getPos() { return be.getBlockPos(); }
        @Override public long getHeatStored() { return HeatBridge.getHeatStored(be); }
        @Override public long getMaxHeatStored() { return HeatBridge.getMaxHeatStored(be); }
        @Override public int getTemperature() { return HeatBridge.getTemperature(be); }
        @Override public boolean isHeatSource() { return HeatBridge.isSource(be); }
        @Override public boolean isHeatConductor() { return HeatBridge.isConductor(be); }
        @Override public boolean canExtractHeat() { return HeatBridge.canExtractHeat(be); }
        @Override public boolean canReceiveHeat() { return HeatBridge.canReceiveHeat(be); }
        @Override public boolean isOverheated() { return HeatBridge.isOverheated(be); }
        @Override public long getHeatLossPerTick() { return HeatBridge.getHeatLossPerTick(be); }
        @Override public long insertHeat(long amount, boolean simulate) { return HeatBridge.insertHeat(be, amount, simulate); }
        @Override public long extractHeat(long amount, boolean simulate) { return HeatBridge.extractHeat(be, amount, simulate); }
        @Override public void setHeatStored(long amount) { setStored(this, amount); }
    }

    /**
     * A block that exposes {@link IMioIcifCapabilities#HEAT_STORAGE_BLOCK}. Its storage can differ by face (a machine
     * may take HU on one face only, a heat generator give it on one face only). So every call looks the faces up
     * again (the block can turn or change), and a transfer goes through one face: the first, in {@link #FACES} order, that
     * takes (or gives) any of it. The amounts are read from the first face that has a storage.
     */
    private static final class CapabilityAccess implements IHeatStorageAccess {
        private final Level world;
        private final BlockPos pos;
        private final BlockEntity be;

        CapabilityAccess(Level world, BlockPos pos, BlockEntity be) {
            this.world = world;
            this.pos = pos;
            this.be = be;
        }

        private List<IMioIcifCapabilities.IHeatStorage> storages() {
            List<IMioIcifCapabilities.IHeatStorage> found = new ArrayList<>(FACES.length);
            for (Direction face : FACES) {
                IMioIcifCapabilities.IHeatStorage storage = world.getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, pos, face);
                if (storage != null) found.add(storage);
            }
            return found;
        }

        private IMioIcifCapabilities.IHeatStorage first() {
            for (Direction face : FACES) {
                IMioIcifCapabilities.IHeatStorage storage = world.getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, pos, face);
                if (storage != null) return storage;
            }
            return null;
        }

        @Override public Level getWorld() { return world; }
        @Override public BlockPos getPos() { return pos; }

        @Override public long getHeatStored() { var s = first(); return s == null ? 0 : s.getHeatStored(); }
        @Override public long getMaxHeatStored() { var s = first(); return s == null ? 0 : s.getMaxHeatStored(); }
        @Override public int getTemperature() { var s = first(); return s == null ? 0 : s.getTemperature(); }
        @Override public boolean isOverheated() { var s = first(); return s != null && s.isOverheated(); }
        @Override public long getHeatLossPerTick() { var s = first(); return s == null ? 0 : s.getHeatLossPerTick(); }

        @Override public boolean isHeatSource() { return be instanceof IHeatGeneratorBlock || HeatBridge.isSource(be); }
        @Override public boolean isHeatConductor() { return HeatBridge.isConductor(be); }

        @Override public boolean canExtractHeat() {
            for (var s : storages()) if (s.canExtractHeat()) return true;
            return false;
        }

        @Override public boolean canReceiveHeat() {
            for (var s : storages()) if (s.canReceiveHeat()) return true;
            return false;
        }

        @Override
        public long insertHeat(long amount, boolean simulate) {
            if (amount <= 0) return 0;
            for (var s : storages()) {
                if (!s.canReceiveHeat()) continue;
                long n = s.receiveHeat(amount, true);
                if (n > 0) return simulate ? n : s.receiveHeat(n, false);
            }
            return 0;
        }

        @Override
        public long extractHeat(long amount, boolean simulate) {
            if (amount <= 0) return 0;
            for (var s : storages()) {
                if (!s.canExtractHeat()) continue;
                long n = s.extractHeat(amount, true);
                if (n > 0) return simulate ? n : s.extractHeat(n, false);
            }
            return 0;
        }

        @Override public void setHeatStored(long amount) { setStored(this, amount); }
    }
}

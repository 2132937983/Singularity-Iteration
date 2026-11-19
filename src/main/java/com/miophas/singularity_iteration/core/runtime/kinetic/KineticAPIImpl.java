package com.miophas.singularity_iteration.core.runtime.kinetic;

import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.kinetic.IKineticAPI;
import com.miophas.singularity_iteration.core.api.kinetic.IKineticStorageAccess;
import com.miophas.singularity_iteration.core.api.machine.IKineticGeneratorBlock;

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
 * Implementation of the kinetic API.
 *
 * <p>A position holds a kinetic block in either of two ways:
 * <ul>
 *   <li>its block entity implements the core API kinetic interfaces ({@code core.api.kinetic.IKineticStorage},
 *       {@code IKineticSource}, {@code IKineticConductor}), read through {@link KineticBridge};</li>
 *   <li>it exposes {@link IMioIcifCapabilities#KINETIC_STORAGE_BLOCK} on some face. All of SI's own kinetic blocks
 *       (the kinetic sources and the kinetic generators) work only this way.</li>
 * </ul>
 *
 * <p>0.1.11.86: the capability way is new. Before, only the core API interfaces counted, so {@code hasKineticTile},
 * {@code getKineticStorage}, {@code getKineticConnections}, {@code insertKinetic} and {@code extractKinetic} saw none of
 * SI's kinetic blocks.
 */
public class KineticAPIImpl implements IKineticAPI {

    /** The faces tried for the capability: unsided first, then the six faces. */
    private static final Direction[] FACES = {null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private static boolean isBridged(BlockEntity be) {
        return KineticBridge.isStorage(be) || KineticBridge.isSource(be) || KineticBridge.isConductor(be);
    }

    private static boolean hasCapability(Level world, BlockPos pos) {
        for (Direction face : FACES) {
            if (world.getCapability(IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, pos, face) != null) return true;
        }
        return false;
    }

    @Override
    public Optional<IKineticStorageAccess> getKineticStorage(Level world, BlockPos pos) {
        if (world.isClientSide) return Optional.empty();
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return Optional.empty();
        if (KineticBridge.isStorage(be)) return Optional.of(new KineticStorageAccessImpl(be));
        return hasCapability(world, pos) ? Optional.of(new CapabilityAccess(world, pos, be)) : Optional.empty();
    }

    @Override
    public boolean hasKineticTile(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return false;
        return isBridged(be) || hasCapability(world, pos);
    }

    @Override
    public Collection<Direction> getKineticConnections(Level world, BlockPos pos) {
        if (world.isClientSide) return Collections.emptyList();
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return Collections.emptyList();
        List<Direction> connections = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            BlockPos at = pos.relative(dir);
            BlockEntity neighbor = world.getBlockEntity(at);
            if (neighbor == null) continue;
            // a core API block has no faces; a capability block connects through its face turned to pos
            if (isBridged(neighbor) || world.getCapability(IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, at, dir.getOpposite()) != null) {
                connections.add(dir);
            }
        }
        return Collections.unmodifiableList(connections);
    }

    private static void setStored(IKineticStorageAccess access, long amount) {
        long clamped = Math.max(0, Math.min(amount, access.getMaxKineticStored()));
        long current = access.getKineticStored();
        if (clamped > current) access.insertKinetic(clamped - current, false);
        else if (clamped < current) access.extractKinetic(current - clamped, false);
    }

    private static class KineticStorageAccessImpl implements IKineticStorageAccess {
        private final BlockEntity be;

        KineticStorageAccessImpl(BlockEntity be) { this.be = be; }

        @Override public Level getWorld() { return be.getLevel(); }
        @Override public BlockPos getPos() { return be.getBlockPos(); }
        @Override public long getKineticStored() { return KineticBridge.getKineticStored(be); }
        @Override public long getMaxKineticStored() { return KineticBridge.getMaxKineticStored(be); }
        @Override public int getRPM() { return KineticBridge.getRPM(be); }
        @Override public boolean isKineticSource() { return KineticBridge.isSource(be); }
        @Override public boolean isKineticConductor() { return KineticBridge.isConductor(be); }
        @Override public boolean canExtractKinetic() { return KineticBridge.canExtractKinetic(be); }
        @Override public boolean canReceiveKinetic() { return KineticBridge.canReceiveKinetic(be); }
        @Override public boolean isOverspeed() { return KineticBridge.isOverspeed(be); }
        @Override public long getKineticLossPerTick() { return KineticBridge.getKineticLossPerTick(be); }
        @Override public long getMaxReceive() { return KineticBridge.getMaxReceive(be); }
        @Override public long getMaxExtract() { return KineticBridge.getMaxExtract(be); }
        @Override public long insertKinetic(long amount, boolean simulate) { return KineticBridge.insertKinetic(be, amount, simulate); }
        @Override public long extractKinetic(long amount, boolean simulate) { return KineticBridge.extractKinetic(be, amount, simulate); }
        @Override public void setKineticStored(long amount) { setStored(this, amount); }
    }

    /**
     * A block that exposes {@link IMioIcifCapabilities#KINETIC_STORAGE_BLOCK}. Its storage can differ by face: a kinetic
     * generator takes KU only at its front, a wind kinetic generator gives KU only at its back. So every call looks the
     * faces up again (the block can turn or change), and a transfer goes through one face: the first, in {@link #FACES}
     * order, that takes (or gives) any of it. The amounts are read from the first face that has a storage.
     */
    private static final class CapabilityAccess implements IKineticStorageAccess {
        private final Level world;
        private final BlockPos pos;
        private final BlockEntity be;

        CapabilityAccess(Level world, BlockPos pos, BlockEntity be) {
            this.world = world;
            this.pos = pos;
            this.be = be;
        }

        private List<IMioIcifCapabilities.IKineticStorage> storages() {
            List<IMioIcifCapabilities.IKineticStorage> found = new ArrayList<>(FACES.length);
            for (Direction face : FACES) {
                IMioIcifCapabilities.IKineticStorage storage = world.getCapability(IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, pos, face);
                if (storage != null) found.add(storage);
            }
            return found;
        }

        private IMioIcifCapabilities.IKineticStorage first() {
            for (Direction face : FACES) {
                IMioIcifCapabilities.IKineticStorage storage = world.getCapability(IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, pos, face);
                if (storage != null) return storage;
            }
            return null;
        }

        @Override public Level getWorld() { return world; }
        @Override public BlockPos getPos() { return pos; }

        @Override public long getKineticStored() { var s = first(); return s == null ? 0 : s.getKineticStored(); }
        @Override public long getMaxKineticStored() { var s = first(); return s == null ? 0 : s.getMaxKineticStored(); }
        @Override public int getRPM() { var s = first(); return s == null ? 0 : s.getRPM(); }
        @Override public boolean isOverspeed() { var s = first(); return s != null && s.isOverspeed(); }
        @Override public long getKineticLossPerTick() { var s = first(); return s == null ? 0 : s.getKineticLossPerTick(); }

        @Override public boolean isKineticSource() { return be instanceof IKineticGeneratorBlock || KineticBridge.isSource(be); }
        @Override public boolean isKineticConductor() { return KineticBridge.isConductor(be); }

        @Override public boolean canExtractKinetic() {
            for (var s : storages()) if (s.canExtractKinetic()) return true;
            return false;
        }

        @Override public boolean canReceiveKinetic() {
            for (var s : storages()) if (s.canReceiveKinetic()) return true;
            return false;
        }

        @Override public long getMaxReceive() {
            long max = 0;
            for (var s : storages()) if (s.canReceiveKinetic()) max = Math.max(max, s.getMaxReceive());
            return max;
        }

        @Override public long getMaxExtract() {
            long max = 0;
            for (var s : storages()) if (s.canExtractKinetic()) max = Math.max(max, s.getMaxExtract());
            return max;
        }

        @Override
        public long insertKinetic(long amount, boolean simulate) {
            if (amount <= 0) return 0;
            for (var s : storages()) {
                if (!s.canReceiveKinetic()) continue;
                long n = s.receiveKinetic(amount, true);
                if (n > 0) return simulate ? n : s.receiveKinetic(n, false);
            }
            return 0;
        }

        @Override
        public long extractKinetic(long amount, boolean simulate) {
            if (amount <= 0) return 0;
            for (var s : storages()) {
                if (!s.canExtractKinetic()) continue;
                long n = s.extractKinetic(amount, true);
                if (n > 0) return simulate ? n : s.extractKinetic(n, false);
            }
            return 0;
        }

        @Override public void setKineticStored(long amount) { setStored(this, amount); }
    }
}

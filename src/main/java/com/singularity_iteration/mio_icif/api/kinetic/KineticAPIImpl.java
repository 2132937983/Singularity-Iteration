package com.singularity_iteration.mio_icif.api.kinetic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * 动能 API 实现
 *
 * <p>所有内部类型访问通过 {@link KineticBridge} 完成，
 * 此文件零内部包引用。
 */
public class KineticAPIImpl implements IKineticAPI {

    @Override
    public Optional<IKineticStorageAccess> getKineticStorage(Level world, BlockPos pos) {
        if (world.isClientSide) return Optional.empty();
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null || !KineticBridge.isStorage(be)) return Optional.empty();
        return Optional.of(new KineticStorageAccessImpl(be));
    }

    @Override
    public boolean hasKineticTile(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return false;
        return KineticBridge.isStorage(be) || KineticBridge.isSource(be) || KineticBridge.isConductor(be);
    }

    @Override
    public Collection<Direction> getKineticConnections(Level world, BlockPos pos) {
        if (world.isClientSide) return Collections.emptyList();
        Collection<Direction> connections = new ArrayList<>();
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return connections;
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = world.getBlockEntity(pos.relative(dir));
            if (neighbor != null && (KineticBridge.isStorage(neighbor) || KineticBridge.isConductor(neighbor) || KineticBridge.isSource(neighbor))) {
                connections.add(dir);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(connections));
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
        @Override public void setKineticStored(long amount) {
            long clamped = Math.max(0, Math.min(amount, getMaxKineticStored()));
            long current = getKineticStored();
            if (clamped > current) insertKinetic(clamped - current, false);
            else if (clamped < current) extractKinetic(current - clamped, false);
        }
    }
}
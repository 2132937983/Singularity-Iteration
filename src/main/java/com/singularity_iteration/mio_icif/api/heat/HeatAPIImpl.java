package com.singularity_iteration.mio_icif.api.heat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * 热能 API 实现
 *
 * <p>所有内部类型访问通过 {@link HeatBridge} 完成，
 * 此文件零内部包引用。
 */
public class HeatAPIImpl implements IHeatAPI {

    @Override
    public Optional<IHeatStorageAccess> getHeatStorage(Level world, BlockPos pos) {
        if (world.isClientSide) return Optional.empty();
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null || !HeatBridge.isStorage(be)) return Optional.empty();
        return Optional.of(new HeatStorageAccessImpl(be));
    }

    @Override
    public boolean hasHeatTile(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return false;
        return HeatBridge.isStorage(be) || HeatBridge.isSource(be) || HeatBridge.isConductor(be);
    }

    @Override
    public Collection<Direction> getHeatConnections(Level world, BlockPos pos) {
        if (world.isClientSide) return Collections.emptyList();
        Collection<Direction> connections = new ArrayList<>();
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return connections;
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = world.getBlockEntity(pos.relative(dir));
            if (neighbor != null && (HeatBridge.isStorage(neighbor) || HeatBridge.isConductor(neighbor) || HeatBridge.isSource(neighbor))) {
                connections.add(dir);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(connections));
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
        @Override public void setHeatStored(long amount) {
            long clamped = Math.max(0, Math.min(amount, getMaxHeatStored()));
            long current = getHeatStored();
            if (clamped > current) insertHeat(clamped - current, false);
            else if (clamped < current) extractHeat(current - clamped, false);
        }
    }
}
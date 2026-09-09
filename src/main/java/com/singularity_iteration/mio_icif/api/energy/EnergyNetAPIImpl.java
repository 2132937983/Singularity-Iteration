package com.singularity_iteration.mio_icif.api.energy;

import com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 能源网络 API 实现
 *
 * <p>通过 {@link GridBridge} 访问内部电网，不再直接引用任何 {@code energy.grid.*} 或
 * {@code energy.EnergyUnit.*} 内部类。
 */
public class EnergyNetAPIImpl implements IEnergyNetAPI {

    @Override
    public NodeStats getNodeStats(Level world, BlockPos pos) {
        if (world.isClientSide) return null;
        var tile = GridBridge.findTile(world, pos);
        if (tile == null) return null;
        return GridBridge.getNodeStats(tile);
    }

    @Override
    public boolean hasEnergyTile(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        return GridBridge.hasTile(world, pos);
    }

    @Override
    public IEnergyTileAccess getEnergyTile(Level world, BlockPos pos) {
        if (world.isClientSide) return null;
        var tile = GridBridge.findTile(world, pos);
        if (tile == null) return null;
        return new EnergyTileAccessImpl(tile);
    }

    @Override
    public IEnergyTileAccess getSubTile(Level world, BlockPos pos) {
        if (world.isClientSide) return null;
        var tile = GridBridge.findSubTile(world, pos);
        if (tile == null) return null;
        return new EnergyTileAccessImpl(tile);
    }

    @Override
    public double getPowerFromTier(int tier) {
        return GridBridge.getPowerFromTier(tier);
    }

    @Override
    public int getTierFromPower(double power) {
        return GridBridge.getTierFromPower(power);
    }

    @Override
    public int cableTierToSourceTier(ICableTier tier) {
        return GridBridge.cableTierToSourceTier(tier);
    }

    @Override
    public Collection<Direction> getConnections(Level world, BlockPos pos) {
        if (world.isClientSide) return Collections.emptyList();

        List<Direction> connections = new ArrayList<>(6);

        for (Direction dir : Direction.values()) {
            if (GridBridge.localEmitsTo(world, pos, dir) && GridBridge.localAcceptsFrom(world, pos.relative(dir), dir.getOpposite())) {
                connections.add(dir);
            } else if (GridBridge.localEmitsTo(world, pos.relative(dir), dir.getOpposite()) && GridBridge.localAcceptsFrom(world, pos, dir)) {
                connections.add(dir);
            }
        }

        return Collections.unmodifiableList(connections);
    }

    // ========== 能源方块类型查询 API ==========

    @Override
    public boolean isEnergySource(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        var tile = GridBridge.findTile(world, pos);
        return GridBridge.isSource(tile);
    }

    @Override
    public boolean isEnergySink(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        var tile = GridBridge.findTile(world, pos);
        return GridBridge.isSink(tile);
    }

    @Override
    public boolean isEnergyConductor(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        var tile = GridBridge.findTile(world, pos);
        return GridBridge.isConductor(tile);
    }

    @Override
    public int getSourceTier(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        var tile = GridBridge.findTile(world, pos);
        return GridBridge.getSourceTier(tile);
    }

    @Override
    public int getSinkTier(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        var tile = GridBridge.findTile(world, pos);
        return GridBridge.getSinkTier(tile);
    }

    @Override
    public long getOfferedEnergy(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        var tile = GridBridge.findTile(world, pos);
        return GridBridge.getOfferedEnergy(tile);
    }

    @Override
    public long getDemandedEnergy(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        var tile = GridBridge.findTile(world, pos);
        return GridBridge.getDemandedEnergy(tile);
    }

    @Override
    public boolean acceptsEnergyFrom(Level world, BlockPos pos, Direction direction) {
        if (world.isClientSide) return false;
        return GridBridge.localAcceptsFrom(world, pos, direction);
    }

    @Override
    public boolean emitsEnergyTo(Level world, BlockPos pos, Direction direction) {
        if (world.isClientSide) return false;
        return GridBridge.localEmitsTo(world, pos, direction);
    }

    // ========== 电网统计 API ==========

    @Override
    public Collection<BlockPos> getAllEnergyTiles(Level world) {
        if (world.isClientSide) return Collections.emptyList();
        return GridBridge.getAllRegisteredPositions(world);
    }

    @Override
    public Collection<BlockPos> getConnectedNodes(Level world, BlockPos pos) {
        if (world.isClientSide) return Collections.emptyList();
        return GridBridge.getConnectedNodes(world, pos);
    }

    @Override
    public long getGridTotalEnergy(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        Collection<BlockPos> nodes = getConnectedNodes(world, pos);
        long total = 0;
        for (BlockPos nodePos : nodes) {
            IEnergyTileAccess access = getEnergyTile(world, nodePos);
            if (access != null) {
                total += access.getStoredEnergy();
            }
        }
        return total;
    }

    @Override
    public long getGridTotalCapacity(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        Collection<BlockPos> nodes = getConnectedNodes(world, pos);
        long total = 0;
        for (BlockPos nodePos : nodes) {
            IEnergyTileAccess access = getEnergyTile(world, nodePos);
            if (access != null) {
                total += access.getMaxEnergy();
            }
        }
        return total;
    }

    // ========== 公共 API 能源方块注册（适配器到内部电网） ==========

    @Override
    public void addTile(Level world, EnergyTileEvent.IEnergyTileMarker tile) {
        if (world.isClientSide) return;
        Level w = world;
        BlockPos pos = null;
        if (tile instanceof BlockEntity be) {
            w = be.getLevel();
            pos = be.getBlockPos();
        }
        if (pos == null) return;
        GridBridge.addTileToGrid(tile, w, pos);
    }

    @Override
    public void removeTile(Level world, EnergyTileEvent.IEnergyTileMarker tile) {
        GridBridge.removeTileFromGrid(tile);
    }

    @Override
    public Level getWorld(EnergyTileEvent.IEnergyTileMarker tile) {
        return GridBridge.getWorldForMarker(tile);
    }

    @Override
    public BlockPos getPos(EnergyTileEvent.IEnergyTileMarker tile) {
        return GridBridge.getPosForMarker(tile);
    }

    @Override
    public ICableTier getCableTier(String name) {
        return GridBridge.getCableTierByName(name);
    }

    @Override
    public java.util.List<ICableTier> getAllCableTiers() {
        return GridBridge.getAllCableTiers();
    }

    @Override
    public ICableTier getDefaultCableTier() {
        return GridBridge.getCableTierByName("lv");
    }
}
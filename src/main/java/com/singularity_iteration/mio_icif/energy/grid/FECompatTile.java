package com.singularity_iteration.mio_icif.energy.grid;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * FE兼容代理tile：将FE机器的能量存储包装为IEnergySink，使其能直接参与EU电网。
 * 能量转换比例：1 EU = 4 FE
 */
@SuppressWarnings("null")
public class FECompatTile implements IEnergySink, ILocatableTile {

    private static final int FE_PER_EU = 4;
    private static final int CACHE_REFRESH_INTERVAL = 20;

    private final Level world;
    private final BlockPos pos;
    private final Direction side;
    private final int sinkTier;

    private IEnergyStorage cachedStorage;
    private long lastCacheTick = -1;
    private boolean registered = false;

    public FECompatTile(Level world, BlockPos pos, Direction side) {
        this.world = world;
        this.pos = pos;
        this.side = side;
        this.sinkTier = calculateSinkTier(world, pos, side);
    }

    private static int calculateSinkTier(Level world, BlockPos pos, Direction side) {
        try {
            IEnergyStorage storage = world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side.getOpposite());
            if (storage == null) return 1;
            int maxReceive = storage.getMaxEnergyStored();
            if (maxReceive >= 8192 * FE_PER_EU) return 4;
            if (maxReceive >= 2048 * FE_PER_EU) return 3;
            if (maxReceive >= 512 * FE_PER_EU) return 2;
            return 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private IEnergyStorage getStorage() {
        long currentTick = world.getGameTime();
        if (cachedStorage == null || currentTick - lastCacheTick >= CACHE_REFRESH_INTERVAL) {
            try {
                cachedStorage = world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side.getOpposite());
            } catch (Exception e) {
                Singularity_Iteration.LOGGER.error("[FECompatTile] Error getting FE capability at {}: {}", pos, e.getMessage());
                cachedStorage = null;
            }
            lastCacheTick = currentTick;
        }
        return cachedStorage;
    }

    public boolean isValid() {
        IEnergyStorage storage = getStorage();
        return storage != null;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public boolean isRegistered() {
        return registered;
    }

    // ==================== ILocatableTile ====================

    @Override
    public Level getWorld() {
        return world;
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    // ==================== IEnergySink ====================

    @Override
    public double getDemandedEnergy() {
        IEnergyStorage storage = getStorage();
        if (storage == null || !storage.canReceive()) return 0.0D;
        int space = storage.getMaxEnergyStored() - storage.getEnergyStored();
        if (space <= 0) return 0.0D;
        return space / (double) FE_PER_EU;
    }

    @Override
    public int getSinkTier() {
        // FE machines don't have voltage restrictions - return max tier to prevent explosion
        // This ensures FE machines can accept energy from any voltage level (LV to SC)
        return Integer.MAX_VALUE;
    }

    @Override
    public double injectEnergy(Direction direction, double amount, double voltage) {
        IEnergyStorage storage = getStorage();
        if (storage == null || !storage.canReceive()) return amount;

        // FE machines don't have voltage restrictions - accept any voltage level
        // This prevents FE machines from exploding when connected to higher voltage grids

        long euToInject = (long) amount;
        long feToInject = euToInject * FE_PER_EU;
        if (feToInject <= 0) return amount;

        try {
            int accepted = storage.receiveEnergy((int) Math.min(feToInject, Integer.MAX_VALUE), false);
            long euAccepted = accepted / FE_PER_EU;
            return amount - euAccepted;
        } catch (Exception e) {
            Singularity_Iteration.LOGGER.error("[FECompatTile] Error injecting energy at {}: {}", pos, e.getMessage());
            return amount;
        }
    }

    // ==================== IEnergyAcceptor ====================

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) {
        IEnergyStorage storage = getStorage();
        return storage != null && storage.canReceive();
    }
}
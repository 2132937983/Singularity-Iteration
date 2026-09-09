package com.singularity_iteration.mio_icif.integration.ae2;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.energy.grid.IEnergyEmitter;
import com.singularity_iteration.mio_icif.energy.grid.IEnergySink;
import com.singularity_iteration.mio_icif.energy.grid.ILocatableTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

@SuppressWarnings("null")
public class Ae2EnergySink implements IEnergySink, ILocatableTile {

    private static final int FE_PER_EU = 4;
    private static final int CACHE_REFRESH_INTERVAL = 20;

    private final Level world;
    private final BlockPos pos;
    private boolean registered = false;

    private IEnergyStorage cachedFeStorage;
    private long lastCacheTick = -1;

    public Ae2EnergySink(Level world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
    }

    public boolean isValid() {
        if (world.getBlockEntity(pos) == null) return false;
        return AE2Compat.isAe2NetworkBlock(world, pos);
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public boolean isRegistered() {
        return registered;
    }

    @Nullable
    private IEnergyStorage getFeStorage() {
        long currentTick = world.getGameTime();
        if (cachedFeStorage == null || currentTick - lastCacheTick >= CACHE_REFRESH_INTERVAL) {
            cachedFeStorage = AE2Compat.getFeStorage(world, pos);
            lastCacheTick = currentTick;
        }
        return cachedFeStorage;
    }

    @Override
    public Level getWorld() {
        return world;
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) {
        return true;
    }

    @Override
    public double getDemandedEnergy() {
        if (AE2Compat.isGridApiAvailable()) {
            Object gridNode = AE2Compat.getGridNode(world, pos);
            if (gridNode != null) {
                Object energyService = AE2Compat.getEnergyService(gridNode);
                if (energyService != null) {
                    double aeDemand = AE2Compat.getAeEnergyDemand(energyService);
                    if (aeDemand > 0.0D) {
                        return aeDemand / AE2Compat.EU_TO_AE_RATIO;
                    }
                }
            }
        }

        IEnergyStorage storage = getFeStorage();
        if (storage != null && storage.canReceive()) {
            int feFree = storage.getMaxEnergyStored() - storage.getEnergyStored();
            if (feFree > 0) {
                return feFree / (double) FE_PER_EU;
            }
        }

        return 0.0D;
    }

    @Override
    public int getSinkTier() {
        return 4;
    }

    @Override
    public double injectEnergy(Direction direction, double amount, double voltage) {
        if (AE2Compat.isGridApiAvailable()) {
            Object gridNode = AE2Compat.getGridNode(world, pos);
            if (gridNode != null) {
                Object energyService = AE2Compat.getEnergyService(gridNode);
                if (energyService != null) {
                    double aeAmount = amount * AE2Compat.EU_TO_AE_RATIO;
                    double leftoverAe = AE2Compat.injectAePower(energyService, aeAmount);
                    double acceptedAe = aeAmount - leftoverAe;
                    if (acceptedAe > 0.0D) {
                        return amount - acceptedAe / AE2Compat.EU_TO_AE_RATIO;
                    }
                }
            }
        }

        IEnergyStorage storage = getFeStorage();
        if (storage != null && storage.canReceive()) {
            int feToSend = (int) Math.min(amount * FE_PER_EU, Integer.MAX_VALUE);
            if (feToSend > 0) {
                try {
                    int feAccepted = storage.receiveEnergy(feToSend, false);
                    if (feAccepted > 0) {
                        double euAccepted = Math.min(feAccepted / (double) FE_PER_EU, amount);
                        return amount - euAccepted;
                    }
                } catch (Exception e) {
                    Singularity_Iteration.LOGGER.error("[Ae2EnergySink] Error injecting FE energy at {}: {}", pos, e.getMessage());
                }
            }
        }

        return amount;
    }
}
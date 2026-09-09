package com.singularity_iteration.mio_icif.energy.grid;

import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Global energy net manager (singleton).
 * Provides the public API for tile registration and lookup.
 * Adapted to use the IC2 1.20 grid system implementation.
 */
@SuppressWarnings("null")
public class EnergyNetGlobal {

    private static IEnergyCalculator calculator;
    private static boolean initialized = false;

    public static void init() {
        if (initialized) throw new IllegalStateException("already initialized");
        initialized = true;
        calculator = new com.singularity_iteration.mio_icif.energy.leg.EnergyCalculator();
    }

    public static IEnergyTile getTile(Level world, BlockPos pos) {
        return getLocal(world).getIoTile(pos);
    }

    public static IEnergyTile getSubTile(Level world, BlockPos pos) {
        return getLocal(world).getSubTile(pos);
    }

    public static void addTile(IEnergyTile tile, Level world, BlockPos pos) {
        getLocal(world).addTile(tile, pos);
    }

    public static void removeTile(IEnergyTile tile) {
        Level world = getWorld(tile);
        if (world == null) return;
        BlockPos pos = getPos(tile);
        getLocal(world).removeTile(tile, pos);
    }

    public static void removeTile(IEnergyTile tile, Level world, BlockPos pos) {
        getLocal(world).removeTile(tile, pos);
    }

    public static Level getWorld(IEnergyTile tile) {
        if (tile instanceof BlockEntity) {
            return ((BlockEntity) tile).getLevel();
        }
        if (tile instanceof ILocatableTile) {
            return ((ILocatableTile) tile).getWorld();
        }
        throw new UnsupportedOperationException("unlocatable tile type: " + tile.getClass().getName());
    }

    public static BlockPos getPos(IEnergyTile tile) {
        if (tile instanceof BlockEntity) {
            return ((BlockEntity) tile).getBlockPos();
        }
        if (tile instanceof ILocatableTile) {
            return ((ILocatableTile) tile).getPos();
        }
        throw new UnsupportedOperationException("unlocatable tile type: " + tile.getClass().getName());
    }

    public static NodeStats getNodeStats(IEnergyTile tile) {
        return getLocal(getWorld(tile)).getNodeStats(tile);
    }

    public static boolean sourceHasReachableSinks(IEnergyTile tile) {
        return getLocal(getWorld(tile)).sourceHasReachableSinks(tile);
    }

    static IEnergyCalculator getCalculator() { return calculator; }

    public static EnergyNetLocal getLocal(Level world) {
        if (world.isClientSide) throw new IllegalStateException("not applicable clientside");
        return WorldData.get(world).energyNet;
    }

    public static double getPowerFromTier(int tier) {
        if (tier < 14) return (8 << tier * 2);
        if (tier < 30) return 8.0D * Math.pow(4.0D, tier);
        return 9.223372036854776E18D;
    }

    public static int getTierFromPower(double power) {
        if (power <= 0.0D) return 0;
        int tier = (int) Math.ceil(Math.log(power / 8.0D) / Math.log(4.0D));
        return Math.max(0, tier);
    }

    /**
     * Convert our CableTier class to IC2 tier number.
     * 注意：ASP 的电压体系与 IC2 不同
     * ASP Tier 5 (Quantum Solar) = 2048 EU/t，对应 IC2 Tier 4 (EV)
     */
    public static int cableTierToSourceTier(CableTier tier) {
        if (tier == null) return 1;
        return tier.tierIndex + 1;
    }

    /**
     * Convert IC2 tier number to packet power.
     */
    public static double tierToPower(int tier) {
        return getPowerFromTier(tier);
    }

    public static int powerToTier(double power) {
        return getTierFromPower(power);
    }
}
package com.singularity_iteration.mio_icif.integration.mi;

import com.singularity_iteration.mio_icif.energy.CustomEUEnergyStorage;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.EUApi;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.IEUEnergyStorage;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.ILongEnergyStorage;
import com.singularity_iteration.mio_icif.energy.grid.IEnergyTile;
import com.singularity_iteration.mio_icif.integration.ae2.AE2Compat;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

@SuppressWarnings({"null", "deprecation"})
public class EnergyBridge {

    private static final int BRIDGE_SCAN_INTERVAL = 20;

    private boolean hasAdjacentCompatSinks = false;
    private int ticksUntilBridgeScan = 0;

    public void tick(Level level, BlockPos wirePos, CableTier cableTier, CustomEUEnergyStorage energyStorage) {
        ticksUntilBridgeScan--;
        if (ticksUntilBridgeScan <= 0) {
            ticksUntilBridgeScan = BRIDGE_SCAN_INTERVAL;
            scanAdjacentCompatSinks(level, wirePos, cableTier, energyStorage);
        }
        pushEnergyToCompatSinks(level, wirePos, energyStorage);
    }

    public boolean hasAdjacentCompatSinks() {
        return hasAdjacentCompatSinks;
    }

    private void scanAdjacentCompatSinks(Level level, BlockPos wirePos, CableTier cableTier, CustomEUEnergyStorage energyStorage) {
        if (level == null) return;
        boolean found = false;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = wirePos.relative(dir);
            if (hasCompatEnergyStorage(level, neighborPos, dir)) {
                found = true;
                break;
            }
        }
        if (found != hasAdjacentCompatSinks) {
            hasAdjacentCompatSinks = found;
            if (hasAdjacentCompatSinks) {
                // 确保capacity足够（wire构造函数已设置，这里只是确保）
                long minCapacity = cableTier.getPowerRating() * 2;
                if (energyStorage.getCapacity() < minCapacity) {
                    energyStorage.setCapacity(minCapacity);
                }
            } else {
                // 只清空energy，不清空capacity，让FE机器仍能访问wire
                energyStorage.setEnergy(0);
            }
        }
    }

    public static boolean hasCompatEnergyStorage(Level level, BlockPos pos, Direction dir) {
        if (level == null) return false;
        try {
            if (level.getBlockEntity(pos) instanceof IEnergyTile) return false;

            if (AE2Compat.isAE2Loaded() && AE2Compat.isAe2NetworkBlock(level, pos)) return false;

            Direction queryDir = dir.getOpposite();

            if (level.getCapability(EUApi.SIDED, pos, queryDir) != null) return true;
            if (level.getCapability(EUApi.SIDED, pos, null) != null) return true;

            if (level.getCapability(ILongEnergyStorage.BLOCK, pos, queryDir) != null) return true;
            if (level.getCapability(ILongEnergyStorage.BLOCK, pos, null) != null) return true;

            if (level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, queryDir) != null) return true;
            if (level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null) != null) return true;

            if (MICompat.isMILoaded()) {
                if (MICompat.getMIStorage(level, pos, queryDir) != null) return true;
                if (MICompat.getMIStorage(level, pos, null) != null) return true;
            }

            return false;
        } catch (Exception e) {
            Singularity_Iteration.LOGGER.error("[EnergyBridge] Error checking compat energy storage at {}: {}", pos, e.getMessage());
            return false;
        }
    }

    private void pushEnergyToCompatSinks(Level level, BlockPos wirePos, CustomEUEnergyStorage energyStorage) {
        if (level == null || !hasAdjacentCompatSinks) return;
        long buffered = energyStorage.getAmount();
        if (buffered <= 0) return;

        for (Direction dir : Direction.values()) {
            if (buffered <= 0) break;
            BlockPos neighborPos = wirePos.relative(dir);

            if (level.getBlockEntity(neighborPos) instanceof IEnergyTile) continue;

            long pushed = pushToStorage(level, neighborPos, dir, buffered);
            if (pushed > 0) {
                energyStorage.consumeEnergyInternal(pushed, false);
                buffered -= pushed;
            }
        }
    }

    public static long pushToStorage(Level level, BlockPos pos, Direction dir, long maxAmount) {
        if (level == null || maxAmount <= 0) return 0;

        try {
            if (AE2Compat.isAE2Loaded() && AE2Compat.isAe2NetworkBlock(level, pos)) return 0;
            Direction queryDir = dir.getOpposite();

            IEUEnergyStorage euStorage = level.getCapability(EUApi.SIDED, pos, queryDir);
            if (euStorage == null) euStorage = level.getCapability(EUApi.SIDED, pos, null);
            if (euStorage != null && euStorage.canReceive()) {
                return euStorage.receive(maxAmount, false);
            }

            ILongEnergyStorage longStorage = level.getCapability(ILongEnergyStorage.BLOCK, pos, queryDir);
            if (longStorage == null) longStorage = level.getCapability(ILongEnergyStorage.BLOCK, pos, null);
            if (longStorage != null && longStorage.canReceive()) {
                return longStorage.receive(maxAmount, false);
            }

            IEnergyStorage feStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, queryDir);
            if (feStorage == null) feStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            if (feStorage != null && feStorage.canReceive()) {
                int feToSend = (int) Math.min(maxAmount * ILongEnergyStorage.FE_PER_EU, Integer.MAX_VALUE);
                int feSent = feStorage.receiveEnergy(feToSend, false);
                return feSent / ILongEnergyStorage.FE_PER_EU;
            }

            if (MICompat.isMILoaded()) {
                Object miStorage = MICompat.getMIStorage(level, pos, queryDir);
                if (miStorage == null) miStorage = MICompat.getMIStorage(level, pos, null);
                if (miStorage != null) {
                    IEUEnergyStorage wrapped = MICompat.wrapMIStorage(miStorage);
                    if (wrapped != null && wrapped.canReceive()) {
                        return wrapped.receive(maxAmount, false);
                    }
                }
            }

            return 0;
        } catch (Exception e) {
            Singularity_Iteration.LOGGER.error("[EnergyBridge] Error pushing energy to {}: {}", pos, e.getMessage());
            return 0;
        }
    }
}
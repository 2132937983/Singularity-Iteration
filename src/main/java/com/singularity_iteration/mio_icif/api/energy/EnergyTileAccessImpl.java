package com.singularity_iteration.mio_icif.api.energy;

import com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent;
import com.singularity_iteration.mio_icif.api.machine.IEnergyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * 能源方块访问实现
 *
 * <p>通过 {@link GridBridge} 访问内部电网和能量存储。
 * 构造器接受 API 层 {@link EnergyTileEvent.IEnergyTileMarker}，
 * 内部查询通过 {@link GridBridge} 方法完成。
 * 此文件零内部包引用。
 */
public class EnergyTileAccessImpl implements IEnergyTileAccess {

    private static final Direction[] ALL_DIRECTIONS = Direction.values();
    private static final Logger LOGGER = LoggerFactory.getLogger(EnergyTileAccessImpl.class);
    private final EnergyTileEvent.IEnergyTileMarker marker;

    public EnergyTileAccessImpl(EnergyTileEvent.IEnergyTileMarker marker) {
        this.marker = marker;
    }

    @org.jetbrains.annotations.Nullable
    private <T> T findCapability(BlockCapability<T, Direction> cap) {
        Level level = getWorld();
        if (level == null) return null;
        for (Direction dir : ALL_DIRECTIONS) {
            T instance = level.getCapability(cap, getPos(), dir);
            if (instance != null) return instance;
        }
        return null;
    }

    @org.jetbrains.annotations.Nullable
    private <T> T findCapability(BlockCapability<T, Direction> cap, Direction direction) {
        Level level = getWorld();
        if (level == null) return null;
        return level.getCapability(cap, getPos(), direction);
    }

    private BlockEntity getBlockEntity() {
        return (marker instanceof BlockEntity be) ? be : null;
    }

    @Override
    public Level getWorld() {
        var tile = GridBridge.asTile(marker);
        return tile != null ? GridBridge.getWorldForTile(tile) : null;
    }

    @Override
    public BlockPos getPos() {
        var tile = GridBridge.asTile(marker);
        if (tile != null) return GridBridge.getPosForTile(tile);
        if (marker instanceof BlockEntity be) return be.getBlockPos();
        return null;
    }

    @Override
    public long getStoredEnergy() {
        if (marker instanceof IEnergyBlock energyBlock) {
            return energyBlock.getEnergyStorage().getAmount();
        }
        var storage = findCapability(
            com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities.EU_STORAGE_BLOCK
        );
        return storage != null ? storage.getStored() : 0;
    }

    @Override
    public long getMaxEnergy() {
        if (marker instanceof IEnergyBlock energyBlock) {
            return energyBlock.getEnergyStorage().getCapacity();
        }
        var storage = findCapability(
            com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities.EU_STORAGE_BLOCK
        );
        return storage != null ? storage.getCapacity() : 0;
    }

    @Override
    public long getOfferedEnergy() {
        var tile = GridBridge.asTile(marker);
        return tile != null ? GridBridge.getOfferedEnergy(tile) : 0;
    }

    @Override
    public long getDemandedEnergy() {
        var tile = GridBridge.asTile(marker);
        return tile != null ? GridBridge.getDemandedEnergy(tile) : 0;
    }

    @Override
    public int getSourceTier() {
        var tile = GridBridge.asTile(marker);
        return tile != null ? GridBridge.getSourceTier(tile) : 0;
    }

    @Override
    public int getSinkTier() {
        var tile = GridBridge.asTile(marker);
        return tile != null ? GridBridge.getSinkTier(tile) : 0;
    }

    @Override
    public Optional<ICableTier> getCableTier() {
        if (marker instanceof IEnergyBlock energyBlock) {
            return Optional.ofNullable(energyBlock.getEffectiveCableTier());
        }
        var tile = GridBridge.asTile(marker);
        if (tile != null) {
            ICableTier tier = GridBridge.getCableTier(tile);
            if (tier != null) return Optional.of(tier);
        }
        if (marker instanceof BlockEntity) {
            var storage = findCapability(
                com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities.EU_STORAGE_BLOCK
            );
            if (storage != null) {
                return Optional.ofNullable(storage.getTier());
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean acceptsEnergyFrom(Direction direction) {
        Level world = getWorld();
        BlockPos pos = getPos();
        if (world == null || pos == null) return false;
        return GridBridge.localAcceptsFrom(world, pos, direction);
    }

    @Override
    public boolean emitsEnergyTo(Direction direction) {
        Level world = getWorld();
        BlockPos pos = getPos();
        if (world == null || pos == null) return false;
        return GridBridge.localEmitsTo(world, pos, direction);
    }

    @Override
    public boolean isSource() {
        var tile = GridBridge.asTile(marker);
        return tile != null && GridBridge.isSource(tile);
    }

    @Override
    public boolean isSink() {
        var tile = GridBridge.asTile(marker);
        return tile != null && GridBridge.isSink(tile);
    }

    @Override
    public boolean isConductor() {
        var tile = GridBridge.asTile(marker);
        return tile != null && GridBridge.isConductor(tile);
    }

    // ========== 能源写入 API 实现 ==========

    @Override
    public long chargeEnergy(long amount, boolean simulate) {
        BlockEntity be = getBlockEntity();
        if (be != null) {
            long result = GridBridge.chargeInternal(be, amount, simulate);
            if (result > 0) return result;
        }
        var storage = findCapability(
            com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities.EU_STORAGE_BLOCK
        );
        if (storage != null) {
            long spaceAvailable = storage.getCapacity() - storage.getStored();
            long toCharge = Math.min(amount, spaceAvailable);
            if (!simulate && toCharge > 0) {
                storage.receiveEnergy(toCharge, false);
            }
            return toCharge;
        }
        return 0;
    }

    @Override
    public long dischargeEnergy(long amount, boolean simulate) {
        BlockEntity be = getBlockEntity();
        if (be != null) {
            long result = GridBridge.dischargeInternal(be, amount, simulate);
            if (result > 0) return result;
        }
        var storage = findCapability(
            com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities.EU_STORAGE_BLOCK
        );
        if (storage != null) {
            long toDischarge = Math.min(amount, storage.getStored());
            if (!simulate && toDischarge > 0) {
                storage.extractEnergy(toDischarge, false);
            }
            return toDischarge;
        }
        return 0;
    }

    @Override
    public long useEnergy(long amount, boolean simulate) {
        BlockEntity be = getBlockEntity();
        if (be != null) {
            long result = GridBridge.useInternal(be, amount, simulate);
            if (result > 0) return result;
        }
        var storage = findCapability(
            com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities.EU_STORAGE_BLOCK
        );
        if (storage != null) {
            long toUse = Math.min(amount, storage.getStored());
            if (!simulate && toUse > 0) {
                storage.useEnergy(toUse, false);
            }
            return toUse;
        }
        return 0;
    }

    @Override
    public long generateEnergy(long amount, boolean simulate) {
        BlockEntity be = getBlockEntity();
        if (be != null) {
            long result = GridBridge.generateInternal(be, amount, simulate);
            if (result > 0) return result;
        }
        var storage = findCapability(
            com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities.EU_STORAGE_BLOCK
        );
        if (storage != null) {
            long toGenerate = Math.min(amount, storage.getCapacity() - storage.getStored());
            if (!simulate && toGenerate > 0) {
                storage.generateEnergy(toGenerate, false);
            }
            return toGenerate;
        }
        return 0;
    }

    @Override
    public boolean setEnergy(long amount) {
        BlockEntity be = getBlockEntity();
        if (be != null) {
            GridBridge.setInternalEnergy(be, amount);
            return true;
        }
        var storage = findCapability(
            com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities.EU_STORAGE_BLOCK
        );
        if (storage != null) {
            try {
                storage.setStored(amount);
                return true;
            } catch (UnsupportedOperationException e) {
                LOGGER.debug("IEUStorage for {} does not support setStored", marker.getClass().getSimpleName());
            }
        }
        LOGGER.warn("setEnergy not supported for tile: {}", marker.getClass().getSimpleName());
        return false;
    }

    @Override
    public boolean setCapacity(long capacity) {
        BlockEntity be = getBlockEntity();
        if (be != null) {
            GridBridge.setInternalCapacity(be, capacity);
            return true;
        }
        var storage = findCapability(
            com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities.EU_STORAGE_BLOCK
        );
        if (storage != null) {
            try {
                storage.setCapacity(capacity);
                return true;
            } catch (UnsupportedOperationException e) {
                LOGGER.debug("IEUStorage for {} does not support setCapacity", marker.getClass().getSimpleName());
            }
        }
        LOGGER.warn("setCapacity not supported for tile: {}", marker.getClass().getSimpleName());
        return false;
    }

    @Override
    public long getMaxReceive() {
        BlockEntity be = getBlockEntity();
        if (be != null) {
            long result = GridBridge.getInternalMaxReceive(be);
            if (result > 0) return result;
        }
        return 0;
    }

    @Override
    public long getMaxExtract() {
        BlockEntity be = getBlockEntity();
        if (be != null) {
            long result = GridBridge.getInternalMaxExtract(be);
            if (result > 0) return result;
        }
        return 0;
    }
}
package com.singularity_iteration.mio_icif.api.energy;

import com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent;
import com.singularity_iteration.mio_icif.energy.CustomEUEnergyStorage;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.grid.ApiTileCompat;
import com.singularity_iteration.mio_icif.energy.grid.EnergyNetGlobal;
import com.singularity_iteration.mio_icif.energy.grid.EnergyNetLocal;
import com.singularity_iteration.mio_icif.energy.grid.IEnergyAcceptor;
import com.singularity_iteration.mio_icif.energy.grid.IEnergyConductor;
import com.singularity_iteration.mio_icif.energy.grid.IEnergyEmitter;
import com.singularity_iteration.mio_icif.energy.grid.IEnergySink;
import com.singularity_iteration.mio_icif.energy.grid.IEnergySource;
import com.singularity_iteration.mio_icif.energy.grid.IEnergyTile;
import com.singularity_iteration.mio_icif.energy.grid.Tile;
import com.singularity_iteration.mio_icif.energy.grid.WorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 电网内部类型的集中适配层。
 *
 * <p>{@code EnergyNetAPIImpl} 和 {@code EnergyTileAccessImpl} 通过此类访问内部电网，
 * 而非直接引用 10+ 个 {@code energy.grid.*} 类。内部重构只需修改此类。
 *
 * <p>此类为包级私有，不对外暴露。
 */
final class GridBridge {

    private GridBridge() {}

    // ========== Marker → Tile 转换 ==========

    static IEnergyTile asTile(EnergyTileEvent.IEnergyTileMarker marker) {
        return (marker instanceof IEnergyTile ie) ? ie : null;
    }

    // ========== 查询 ==========

    static IEnergyTile findTile(Level world, BlockPos pos) {
        return EnergyNetGlobal.getTile(world, pos);
    }

    static IEnergyTile findSubTile(Level world, BlockPos pos) {
        return EnergyNetGlobal.getSubTile(world, pos);
    }

    static boolean hasTile(Level world, BlockPos pos) {
        return EnergyNetGlobal.getTile(world, pos) != null;
    }

    static EnergyNetLocal getLocal(Level world) {
        return EnergyNetGlobal.getLocal(world);
    }

    static Tile getLocalTile(Level world, BlockPos pos) {
        EnergyNetLocal local = EnergyNetGlobal.getLocal(world);
        return local != null ? local.getTile(pos) : null;
    }

    static Level getWorldForTile(IEnergyTile tile) {
        return EnergyNetGlobal.getWorld(tile);
    }

    static BlockPos getPosForTile(IEnergyTile tile) {
        return EnergyNetGlobal.getPos(tile);
    }

    // ========== 类型检查 ==========

    static boolean isSource(IEnergyTile tile) {
        return tile instanceof IEnergySource;
    }

    static boolean isSink(IEnergyTile tile) {
        return tile instanceof IEnergySink;
    }

    static boolean isConductor(IEnergyTile tile) {
        return tile instanceof IEnergyConductor;
    }

    static boolean isEmitter(IEnergyTile tile) {
        return tile instanceof IEnergyEmitter;
    }

    static boolean isAcceptor(IEnergyTile tile) {
        return tile instanceof IEnergyAcceptor;
    }

    // ========== 属性查询 ==========

    static int getSourceTier(IEnergyTile tile) {
        return tile instanceof IEnergySource s ? s.getSourceTier() : 0;
    }

    static int getSinkTier(IEnergyTile tile) {
        return tile instanceof IEnergySink s ? s.getSinkTier() : 0;
    }

    static long getOfferedEnergy(IEnergyTile tile) {
        return tile instanceof IEnergySource s ? (long) s.getOfferedEnergy() : 0;
    }

    static long getDemandedEnergy(IEnergyTile tile) {
        return tile instanceof IEnergySink s ? (long) s.getDemandedEnergy() : 0;
    }

    static com.singularity_iteration.mio_icif.api.energy.ICableTier getCableTier(IEnergyTile tile) {
        if (tile instanceof IEnergyConductor c) {
            return c.getCableTier();
        }
        return null;
    }

    // ========== 连接查询 ==========

    static boolean emitsEnergyTo(IEnergyTile emitter, IEnergyTile target, Direction dir) {
        if (emitter instanceof IEnergyEmitter e && target instanceof IEnergyAcceptor a) {
            return e.emitsEnergyTo(a, dir);
        }
        return false;
    }

    static boolean acceptsEnergyFrom(IEnergyTile acceptor, IEnergyTile source, Direction dir) {
        if (acceptor instanceof IEnergyAcceptor a && source instanceof IEnergyEmitter e) {
            return a.acceptsEnergyFrom(e, dir);
        }
        return false;
    }

    // ========== 电网统计 ==========

    static com.singularity_iteration.mio_icif.api.energy.NodeStats getNodeStats(IEnergyTile tile) {
        com.singularity_iteration.mio_icif.energy.grid.NodeStats internal = EnergyNetGlobal.getNodeStats(tile);
        if (internal == null) return null;
        return new com.singularity_iteration.mio_icif.api.energy.NodeStats(
            (long) internal.getEnergyIn(), (long) internal.getEnergyOut(), internal.getVoltage());
    }

    static Collection<BlockPos> getAllRegisteredPositions(Level world) {
        EnergyNetLocal local = EnergyNetGlobal.getLocal(world);
        return local != null ? local.getAllRegisteredPositions() : Collections.emptyList();
    }

    static List<BlockPos> getConnectedNodes(Level world, BlockPos pos) {
        EnergyNetLocal local = EnergyNetGlobal.getLocal(world);
        if (local == null) return Collections.emptyList();

        Tile tile = local.getTile(pos);
        if (tile == null) return Collections.emptyList();

        IEnergyTile mainTile = tile.getMainTile();
        List<BlockPos> connected = new ArrayList<>();

        for (BlockPos nodePos : local.getAllRegisteredPositions()) {
            Tile nodeTile = local.getTile(nodePos);
            if (nodeTile != null && nodeTile.getMainTile() == mainTile) {
                connected.add(nodePos);
            }
        }
        return Collections.unmodifiableList(connected);
    }

    // ========== Tier 转换 ==========

    static double getPowerFromTier(int tier) {
        return EnergyNetGlobal.getPowerFromTier(tier);
    }

    static int getTierFromPower(double power) {
        return EnergyNetGlobal.getTierFromPower(power);
    }

    // ========== Tile 注册/注销 ==========

    static void addTileToGrid(EnergyTileEvent.IEnergyTileMarker marker, Level world, BlockPos pos) {
        ApiTileCompat wrapper = ApiTileCompat.create(marker, world, pos);
        ApiTileCompat.putMarker(marker, wrapper);
        EnergyNetGlobal.addTile(wrapper, world, pos);
    }

    static void removeTileFromGrid(EnergyTileEvent.IEnergyTileMarker marker) {
        ApiTileCompat wrapper = ApiTileCompat.removeMarker(marker);
        if (wrapper != null && WorldData.has(wrapper.getWorld())) {
            EnergyNetGlobal.removeTile(wrapper);
        }
    }

    static Level getWorldForMarker(EnergyTileEvent.IEnergyTileMarker marker) {
        ApiTileCompat wrapper = ApiTileCompat.getMarker(marker);
        return wrapper != null ? wrapper.getWorld() : null;
    }

    static BlockPos getPosForMarker(EnergyTileEvent.IEnergyTileMarker marker) {
        ApiTileCompat wrapper = ApiTileCompat.getMarker(marker);
        return wrapper != null ? wrapper.getPos() : null;
    }

    // ========== CableTier 名称查询 ==========

    static ICableTier getCableTierByName(String name) {
        if (name == null) return CableTier.LV;
        try {
            return CableTier.getTier(name.toLowerCase());
        } catch (java.util.NoSuchElementException e) {
            return CableTier.LV;
        }
    }

    @SuppressWarnings("unchecked")
    static java.util.List<ICableTier> getAllCableTiers() {
        return (java.util.List<ICableTier>) (java.util.List<?>) CableTier.allTiers();
    }

    // ========== 内部存储访问 ==========

    /**
     * 获取可写的内部能量存储。
     * 仅对 mio_icif_Energy_Block 及其子类有效，其他方块返回 null。
     */
    static CustomEUEnergyStorage getInternalStorage(BlockEntity be) {
        if (be instanceof com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block energyBlock) {
            return energyBlock.getEnergyStorageInternal();
        }
        return null;
    }

    static long chargeInternal(BlockEntity be, long amount, boolean simulate) {
        CustomEUEnergyStorage storage = getInternalStorage(be);
        if (storage == null) return 0;
        long spaceAvailable = storage.getCapacity() - storage.getAmount();
        long toCharge = Math.min(amount, spaceAvailable);
        if (!simulate && toCharge > 0) {
            storage.receive(toCharge, false);
        }
        return toCharge;
    }

    static long dischargeInternal(BlockEntity be, long amount, boolean simulate) {
        CustomEUEnergyStorage storage = getInternalStorage(be);
        if (storage == null) return 0;
        long toDischarge = Math.min(amount, storage.getAmount());
        if (!simulate && toDischarge > 0) {
            storage.extract(toDischarge, false);
        }
        return toDischarge;
    }

    static long useInternal(BlockEntity be, long amount, boolean simulate) {
        CustomEUEnergyStorage storage = getInternalStorage(be);
        if (storage == null) return 0;
        long toUse = Math.min(amount, storage.getAmount());
        if (!simulate && toUse > 0) {
            storage.consumeEnergyInternal(toUse, false);
        }
        return toUse;
    }

    static long generateInternal(BlockEntity be, long amount, boolean simulate) {
        CustomEUEnergyStorage storage = getInternalStorage(be);
        if (storage == null) return 0;
        long toGenerate = Math.min(amount, storage.getCapacity() - storage.getAmount());
        if (!simulate && toGenerate > 0) {
            storage.generateEnergyInternal(toGenerate, false);
        }
        return toGenerate;
    }

    static void setInternalEnergy(BlockEntity be, long amount) {
        CustomEUEnergyStorage storage = getInternalStorage(be);
        if (storage != null) {
            long clamped = Math.max(0, Math.min(amount, storage.getCapacity()));
            storage.setEnergy(clamped);
        }
    }

    static void setInternalCapacity(BlockEntity be, long capacity) {
        CustomEUEnergyStorage storage = getInternalStorage(be);
        if (storage != null) {
            storage.setCapacity(capacity);
        }
    }

    static long getInternalMaxReceive(BlockEntity be) {
        CustomEUEnergyStorage storage = getInternalStorage(be);
        return storage != null ? storage.getMaxReceive() : 0;
    }

    static long getInternalMaxExtract(BlockEntity be) {
        CustomEUEnergyStorage storage = getInternalStorage(be);
        return storage != null ? storage.getMaxExtract() : 0;
    }

    // ========== CableTier ==========

    static int cableTierToSourceTier(ICableTier tier) {
        if (tier == null) return 1;
        if (tier instanceof CableTier cableTier) {
            return EnergyNetGlobal.cableTierToSourceTier(cableTier);
        }
        return tier.getTier() + 1;
    }

    /**
     * 将 ICableTier 转换为内部 CableTier 实例。
     * 如果传入的不是 CableTier，按名称查找对应实例；找不到则降级到 LV。
     */
    static CableTier toInternalCableTier(ICableTier tier) {
        if (tier instanceof CableTier ct) return ct;
        if (tier != null && tier.getName() != null) {
            return CableTier.getTier(tier.getName());
        }
        return CableTier.LV;
    }

    static boolean worldDataHas(Level world) {
        return WorldData.has(world);
    }

    // ========== 连接查询（本地网格） ==========

    static boolean localEmitsTo(Level world, BlockPos from, Direction dir) {
        EnergyNetLocal local = EnergyNetGlobal.getLocal(world);
        if (local == null) return false;
        Tile tile = local.getTile(from);
        if (tile == null) return false;
        Tile neighbor = local.getTile(from.relative(dir));
        if (neighbor == null) return false;
        return emitsEnergyTo(tile.getMainTile(), neighbor.getMainTile(), dir);
    }

    static boolean localAcceptsFrom(Level world, BlockPos from, Direction dir) {
        EnergyNetLocal local = EnergyNetGlobal.getLocal(world);
        if (local == null) return false;
        Tile tile = local.getTile(from);
        if (tile == null) return false;
        Tile neighbor = local.getTile(from.relative(dir));
        if (neighbor == null) return false;
        return acceptsEnergyFrom(tile.getMainTile(), neighbor.getMainTile(), dir);
    }
}
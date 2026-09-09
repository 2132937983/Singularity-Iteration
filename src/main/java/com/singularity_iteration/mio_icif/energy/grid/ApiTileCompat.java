package com.singularity_iteration.mio_icif.energy.grid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent.IEnergyTileMarker;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 适配器：将附属模组通过公共 API 提供的 {@code api.energy.tile.*} 能源标记
 * 包装为电网内部使用的 {@code grid.*} 接口，使其能参与 EU 电网。
 * <p>
 * 能量值类型在 api 接口中为 {@code long}，在 grid 接口中为 {@code double}，此处做转换。
 * <b>精度权衡：</b>{@code double} 仅有 53 位有效尾数位，当 EU 值超过 2^53（约 9.0×10^15 EU）时
 * 会出现精度损失（最低位舍入）。在当前游戏数值范围内（通常 &lt;10^9 EU）不会产生实际影响，
 * 附属模组若处理超大能量值应注意此限制。
 * <p>
 * 可选接口（IMultiEnergySource / IColoredEnergyTile / IMetaDelegate）在此统一暴露给电网逻辑检测。
 */
@SuppressWarnings("null")
public class ApiTileCompat
        implements ILocatableTile, IEnergyAcceptor, IEnergyEmitter {

    private static final Map<IEnergyTileMarker, ApiTileCompat> BY_MARKER = new ConcurrentHashMap<>();

    public static void putMarker(IEnergyTileMarker marker, ApiTileCompat compat) {
        BY_MARKER.put(marker, compat);
    }

    public static ApiTileCompat removeMarker(IEnergyTileMarker marker) {
        return BY_MARKER.remove(marker);
    }

    public static ApiTileCompat getMarker(IEnergyTileMarker marker) {
        return BY_MARKER.get(marker);
    }

    protected final IEnergyTileMarker marker;
    protected final Level world;
    protected final BlockPos pos;

    protected ApiTileCompat(IEnergyTileMarker marker, Level world, BlockPos pos) {
        this.marker = marker;
        this.world = world;
        this.pos = pos;
    }

    public static ApiTileCompat create(IEnergyTileMarker marker, Level world, BlockPos pos) {
        boolean isApiConductor = marker instanceof com.singularity_iteration.mio_icif.api.energy.tile.IEnergyConductor;
        boolean isGridConductor = marker instanceof IEnergyConductor;
        boolean isApiSource = marker instanceof com.singularity_iteration.mio_icif.api.energy.tile.IEnergySource;
        boolean isApiSink = marker instanceof com.singularity_iteration.mio_icif.api.energy.tile.IEnergySink;

        System.out.println("[ApiTileCompat] create: class=" + marker.getClass().getSimpleName()
            + " pos=" + pos
            + " apiConductor=" + isApiConductor
            + " gridConductor=" + isGridConductor
            + " apiSource=" + isApiSource
            + " apiSink=" + isApiSink);

        if (isApiConductor || isGridConductor)
            return new Conductor(marker, world, pos);
        if (isApiSource && isApiSink)
            return new SourceSink(marker, world, pos);
        if (isApiSource)
            return new Source(marker, world, pos);
        if (isApiSink)
            return new Sink(marker, world, pos);
        return new ApiTileCompat(marker, world, pos);
    }

    // ============ ILocatableTile ============

    @Override
    public Level getWorld() {
        return world;
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    // ============ IEnergyTile ============

    @Override
    public void onConnectionChange() {
    }

    // ============ grid.IEnergyAcceptor / grid.IEnergyEmitter ============

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) {
        if (marker instanceof com.singularity_iteration.mio_icif.api.energy.tile.IEnergyAcceptor aa
                && emitter instanceof com.singularity_iteration.mio_icif.api.energy.tile.IEnergyEmitter ie)
            return aa.acceptsEnergyFrom(ie, direction);
        return true;
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) {
        if (marker instanceof com.singularity_iteration.mio_icif.api.energy.tile.IEnergyEmitter ae
                && acceptor instanceof com.singularity_iteration.mio_icif.api.energy.tile.IEnergyAcceptor aa)
            return ae.emitsEnergyTo(aa, direction);
        return true;
    }

    // ============ api.IMultiEnergySource（暴露给电网多包发送逻辑） ============

    public boolean sendMultipleEnergyPackets() {
        return marker instanceof com.singularity_iteration.mio_icif.api.energy.tile.IMultiEnergySource m && m.sendMultipleEnergyPackets();
    }

    public int getMultipleEnergyPacketAmount() {
        return marker instanceof com.singularity_iteration.mio_icif.api.energy.tile.IMultiEnergySource m
                ? m.getMultipleEnergyPacketAmount() : 0;
    }

    // ============ api.IColoredEnergyTile（暴露给电网连接染色逻辑） ============

    public DyeColor getColor(Direction side) {
        return marker instanceof com.singularity_iteration.mio_icif.api.energy.tile.IColoredEnergyTile c ? c.getColor(side) : null;
    }

    // ============ api.IMetaDelegate（暴露给电网子方块展开逻辑） ============

    public List<IEnergyTileMarker> getSubTiles() {
        if (marker instanceof com.singularity_iteration.mio_icif.api.energy.tile.IMetaDelegate m)
            return m.getSubTiles();
        return List.of(marker);
    }

    // ============ 角色特定子类 ============

    public static final class Source extends ApiTileCompat implements IEnergySource {
        Source(IEnergyTileMarker marker, Level world, BlockPos pos) {
            super(marker, world, pos);
        }

        @Override
        public double getOfferedEnergy() {
            return (double) ((com.singularity_iteration.mio_icif.api.energy.tile.IEnergySource) marker).getOfferedEnergy();
        }

        @Override
        public void drawEnergy(double amount) {
            ((com.singularity_iteration.mio_icif.api.energy.tile.IEnergySource) marker).drawEnergy((long) amount);
        }

        @Override
        public int getSourceTier() {
            return ((com.singularity_iteration.mio_icif.api.energy.tile.IEnergySource) marker).getSourceTier();
        }

        @Override
        public int getPacketCount() {
            if (marker instanceof com.singularity_iteration.mio_icif.api.energy.tile.IMultiEnergySource m && m.sendMultipleEnergyPackets()) {
                int packetAmount = m.getMultipleEnergyPacketAmount();
                if (packetAmount > 0) {
                    double offered = ((com.singularity_iteration.mio_icif.api.energy.tile.IEnergySource) marker).getOfferedEnergy();
                    return (int) Math.max(1, Math.ceil(offered / (double) packetAmount));
                }
            }
            return 1;
        }
    }

    public static final class Sink extends ApiTileCompat implements IEnergySink {
        Sink(IEnergyTileMarker marker, Level world, BlockPos pos) {
            super(marker, world, pos);
        }

        @Override
        public double getDemandedEnergy() {
            return ((com.singularity_iteration.mio_icif.api.energy.tile.IEnergySink) marker).getDemandedEnergy();
        }

        @Override
        public int getSinkTier() {
            return ((com.singularity_iteration.mio_icif.api.energy.tile.IEnergySink) marker).getSinkTier();
        }

        @Override
        public double injectEnergy(Direction from, double amount, double voltage) {
            long remaining = ((com.singularity_iteration.mio_icif.api.energy.tile.IEnergySink) marker)
                    .injectEnergy(from, (long) amount, voltage);
            return remaining;
        }
    }

    public static final class SourceSink extends ApiTileCompat implements IEnergySource, IEnergySink {
        SourceSink(IEnergyTileMarker marker, Level world, BlockPos pos) {
            super(marker, world, pos);
        }

        @Override
        public double getOfferedEnergy() {
            return (double) ((com.singularity_iteration.mio_icif.api.energy.tile.IEnergySource) marker).getOfferedEnergy();
        }

        @Override
        public void drawEnergy(double amount) {
            ((com.singularity_iteration.mio_icif.api.energy.tile.IEnergySource) marker).drawEnergy((long) amount);
        }

        @Override
        public int getSourceTier() {
            return ((com.singularity_iteration.mio_icif.api.energy.tile.IEnergySource) marker).getSourceTier();
        }

        @Override
        public int getPacketCount() {
            if (marker instanceof com.singularity_iteration.mio_icif.api.energy.tile.IMultiEnergySource m && m.sendMultipleEnergyPackets()) {
                int packetAmount = m.getMultipleEnergyPacketAmount();
                if (packetAmount > 0) {
                    double offered = ((com.singularity_iteration.mio_icif.api.energy.tile.IEnergySource) marker).getOfferedEnergy();
                    return (int) Math.max(1, Math.ceil(offered / (double) packetAmount));
                }
            }
            return 1;
        }

        @Override
        public double getDemandedEnergy() {
            return ((com.singularity_iteration.mio_icif.api.energy.tile.IEnergySink) marker).getDemandedEnergy();
        }

        @Override
        public int getSinkTier() {
            return ((com.singularity_iteration.mio_icif.api.energy.tile.IEnergySink) marker).getSinkTier();
        }

        @Override
        public double injectEnergy(Direction from, double amount, double voltage) {
            long remaining = ((com.singularity_iteration.mio_icif.api.energy.tile.IEnergySink) marker)
                    .injectEnergy(from, (long) amount, voltage);
            return remaining;
        }
    }

    public static final class Conductor extends ApiTileCompat implements IEnergyConductor {
        Conductor(IEnergyTileMarker marker, Level world, BlockPos pos) {
            super(marker, world, pos);
        }

        private com.singularity_iteration.mio_icif.api.energy.tile.IEnergyConductor apiConductor() {
            return (com.singularity_iteration.mio_icif.api.energy.tile.IEnergyConductor) marker;
        }

        private IEnergyConductor gridConductor() {
            return (IEnergyConductor) marker;
        }

        private boolean isApiConductor() {
            return marker instanceof com.singularity_iteration.mio_icif.api.energy.tile.IEnergyConductor;
        }

        @Override
        public double getConductionLoss() {
            if (isApiConductor()) return apiConductor().getConductionLoss();
            return gridConductor().getConductionLoss();
        }

        @Override
        public double getInsulationEnergyAbsorption() {
            if (isApiConductor()) return (double) apiConductor().getInsulationEnergyAbsorption();
            return gridConductor().getInsulationEnergyAbsorption();
        }

        @Override
        public double getInsulationBreakdownEnergy() {
            if (isApiConductor()) return (double) apiConductor().getInsulationBreakdownEnergy();
            return gridConductor().getInsulationBreakdownEnergy();
        }

        @Override
        public double getConductorBreakdownEnergy() {
            if (isApiConductor()) return (double) apiConductor().getConductorBreakdownEnergy();
            return gridConductor().getConductorBreakdownEnergy();
        }

        @Override
        public void removeInsulation() {
            if (isApiConductor()) apiConductor().removeInsulation();
            else gridConductor().removeInsulation();
        }

        @Override
        public void removeConductor() {
            if (isApiConductor()) apiConductor().removeConductor();
            else gridConductor().removeConductor();
        }

        @Override
        public CableTier getCableTier() {
            if (isApiConductor()) {
                ICableTier tier = apiConductor().getCableTier();
                if (tier instanceof CableTier ct) return ct;
            } else {
                return gridConductor().getCableTier();
            }
            return CableTier.LV;
        }
    }
}
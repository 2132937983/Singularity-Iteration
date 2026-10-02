package com.miophas.singularity_iteration.common.blockentity.wiring.terminal;

import com.miophas.singularity_iteration.common.blockentity.transformer.mio_icif_transformer;
import com.miophas.singularity_iteration.core.api.energy.ICableEnergyNode;
import com.miophas.singularity_iteration.core.api.energy.IColoredEnergyTile;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentSpecialCableBlockEntity;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyStorageBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractWireBlockEntity;
import com.miophas.singularity_iteration.core.prefab.component.EnergyComponentHost;
import com.miophas.singularity_iteration.core.runtime.energy.DemandEnergySource;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Walks an EU cable network from a starting block and lists what is on it - shared by the
 * Energy Management Terminal and the EU meter / voltage detector.
 *
 * <p>Conductors are every cable the engine routes through: SI wires ({@link ICableEnergyNode},
 * including superconducting cable) and the engine's special cables (detector / splitter,
 * {@link IndependentSpecialCableBlockEntity}). Transformers - legacy {@link mio_icif_transformer}
 * and the engine's {@link IndependentTransformerBlockEntity} (every tier up to LuV->ZPMV) - are
 * endpoints of a sub-network. In <b>global</b> mode the walk continues through each transformer
 * into the sub-network on its other side, so a factory split into voltage tiers is seen as one
 * system with a per-sub-network breakdown.
 */
public final class NetworkWalker {
    private NetworkWalker() {}

    /** One sub-network: the cables flooded from one start, between transformers. */
    public static final class Subnet {
        public final int index;
        public int conductors;
        /** Highest packet the sub-network's cables are rated for (EU/packet), 0 if unknown. */
        public long ratedPacket;
        Subnet(int index) { this.index = index; }
    }

    public static final class Result {
        public final List<BlockPos> devices = new ArrayList<>();
        public final List<EnergyNetworkSnapshot.Category> categories = new ArrayList<>();
        public final List<Integer> deviceSubnet = new ArrayList<>();
        public final List<Subnet> subnets = new ArrayList<>();
        public int conductors;
        public boolean truncated;
    }

    public static boolean isConductor(@Nullable BlockEntity be) {
        if (be instanceof ICableEnergyNode) return true;
        return be instanceof IndependentSpecialCableBlockEntity special && special.conducts();
    }

    public static boolean isTransformer(@Nullable BlockEntity be) {
        return be instanceof mio_icif_transformer || be instanceof IndependentTransformerBlockEntity;
    }

    /** Endpoint category of a block entity on a cable, or null when it is no EU endpoint. */
    @Nullable
    public static EnergyNetworkSnapshot.Category categorize(@Nullable BlockEntity be) {
        if (be == null || be instanceof EnergyTerminalBlockEntity) return null;
        if (isTransformer(be)) return EnergyNetworkSnapshot.Category.TRANSFORMER;
        if (be instanceof AbstractEnergyStorageBlockEntity) return EnergyNetworkSnapshot.Category.STORAGE;
        if (be instanceof DemandEnergySource) return EnergyNetworkSnapshot.Category.GENERATOR;
        if (be instanceof AbstractEnergyBlockEntity machine && !(be instanceof ICableEnergyNode)) {
            return machine.isPowerSource() ? EnergyNetworkSnapshot.Category.GENERATOR : EnergyNetworkSnapshot.Category.CONSUMER;
        }
        if (be instanceof EnergyComponentHost) return EnergyNetworkSnapshot.Category.CONSUMER;
        return null;
    }

    private static long rating(BlockEntity be) {
        try {
            if (be instanceof AbstractWireBlockEntity wire && wire.getCableTier() != null) return wire.getCableTier().getMaxTransfer();
        } catch (RuntimeException ignored) { }
        return 0;
    }

    /**
     * Walks from {@code origin} (a terminal, cable, transformer or endpoint). Cables touching the
     * origin are flooded; an origin that is itself a cable starts the flood. {@code global} also
     * crosses transformers.
     */
    public static Result walk(ServerLevel level, BlockPos origin, boolean global, int maxConductors, int maxDevices) {
        Result r = new Result();
        LongOpenHashSet visited = new LongOpenHashSet();
        LongOpenHashSet devices = new LongOpenHashSet();
        ArrayDeque<BlockPos> transformers = new ArrayDeque<>();
        LongOpenHashSet expanded = new LongOpenHashSet();
        visited.add(origin.asLong());
        BlockEntity originBe = level.isLoaded(origin) ? level.getBlockEntity(origin) : null;
        if (isConductor(originBe)) {
            flood(level, origin, r, visited, devices, transformers, maxConductors, maxDevices, true);
        } else {
            if (isTransformer(originBe) && global) { transformers.add(origin); }
            else if (categorize(originBe) != null && !(originBe instanceof EnergyTerminalBlockEntity)) {
                addDevice(r, devices, origin, categorize(originBe), -1, maxDevices);
            }
            for (Direction side : Direction.values()) {
                BlockPos next = origin.relative(side);
                if (visited.contains(next.asLong()) || !level.isLoaded(next)) continue;
                if (isConductor(level.getBlockEntity(next))) flood(level, next, r, visited, devices, transformers, maxConductors, maxDevices, false);
            }
        }
        if (global) {
            while (!transformers.isEmpty()) {
                BlockPos t = transformers.poll();
                if (!expanded.add(t.asLong())) continue;
                for (Direction side : Direction.values()) {
                    BlockPos next = t.relative(side);
                    if (visited.contains(next.asLong()) || !level.isLoaded(next)) continue;
                    BlockEntity be = level.getBlockEntity(next);
                    if (isConductor(be)) {
                        flood(level, next, r, visited, devices, transformers, maxConductors, maxDevices, false);
                    } else if (isTransformer(be)) {
                        // transformer bolted straight onto a transformer: its own sub-network of zero cables
                        Subnet s = new Subnet(r.subnets.size());
                        r.subnets.add(s);
                        addDevice(r, devices, next, EnergyNetworkSnapshot.Category.TRANSFORMER, s.index, maxDevices);
                        transformers.add(next);
                    }
                }
            }
        }
        return r;
    }

    private static void addDevice(Result r, LongOpenHashSet devices, BlockPos at, EnergyNetworkSnapshot.Category cat, int subnet, int maxDevices) {
        if (!devices.add(at.asLong())) return;
        if (r.devices.size() >= maxDevices) { r.truncated = true; return; }
        r.devices.add(at.immutable());
        r.categories.add(cat);
        r.deviceSubnet.add(subnet);
    }

    private static void flood(ServerLevel level, BlockPos start, Result r, LongOpenHashSet visited, LongOpenHashSet devices,
                              ArrayDeque<BlockPos> transformers, int maxConductors, int maxDevices, boolean startIsOrigin) {
        Subnet subnet = new Subnet(r.subnets.size());
        r.subnets.add(subnet);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start.asLong());
        queue.add(start.immutable());
        BlockEntity startBe = level.getBlockEntity(start);
        subnet.conductors++; r.conductors++;
        subnet.ratedPacket = Math.max(subnet.ratedPacket, rating(startBe));
        while (!queue.isEmpty()) {
            BlockPos at = queue.poll();
            BlockEntity from = level.getBlockEntity(at);
            for (Direction side : Direction.values()) {
                if (from instanceof ICableEnergyNode cable && cable.isDirectionBlocked(side)) continue;
                BlockPos next = at.relative(side);
                long key = next.asLong();
                if (visited.contains(key) || devices.contains(key) || !level.isLoaded(next)) continue;
                BlockEntity be = level.getBlockEntity(next);
                if (be == null) continue;
                if (isConductor(be)) {
                    if (be instanceof ICableEnergyNode cable && cable.isDirectionBlocked(side.getOpposite())) continue;
                    if ((from instanceof IColoredEnergyTile || be instanceof IColoredEnergyTile) && !IColoredEnergyTile.connects(from, be, side)) continue;
                    if (r.conductors >= maxConductors) { r.truncated = true; continue; }
                    visited.add(key);
                    subnet.conductors++; r.conductors++;
                    subnet.ratedPacket = Math.max(subnet.ratedPacket, rating(be));
                    queue.add(next.immutable());
                    continue;
                }
                EnergyNetworkSnapshot.Category cat = categorize(be);
                if (cat == null) continue;
                addDevice(r, devices, next, cat, subnet.index, maxDevices);
                if (cat == EnergyNetworkSnapshot.Category.TRANSFORMER) transformers.add(next.immutable());
            }
        }
    }

    /** IC2 voltage tier names by packet size (8 V ULV ... ). */
    public static String tierName(double packet) {
        String[] names = {"ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV", "UEV", "UIV", "UMV", "UXV", "MAX"};
        if (packet <= 0) return "-";
        int tier = com.miophas.singularity_iteration.core.runtime.energy.grid.EnergyNetGlobal.getTierFromPower(packet);
        return names[Math.min(names.length - 1, tier)];
    }
}

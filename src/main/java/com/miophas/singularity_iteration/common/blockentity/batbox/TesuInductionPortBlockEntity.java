package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.runtime.machine.ChargepadHelper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 0.1.11.206: the TESU wireless induction port. In a port slot of a formed TESU it is a wireless output port (the centre of a
 * wireless field; relays link to it, machines with the wireless upgrade are filled), and within {@link #RANGE} blocks of itself:
 * <ul>
 *   <li>players are charged: their armor, hands and inventory, as full as the items take every tick;</li>
 *   <li>machines that take energy are filled every tick <b>without</b> the wireless upgrade (not generators, energy stores,
 *       cables or nodes: {@link WirelessInduction#chargeable}), with the particle stream to each.</li>
 * </ul>
 * The machines in the cube are looked up every {@link #RESCAN} ticks (block entities of the loaded chunks it touches).
 */
@SuppressWarnings("null")
public class TesuInductionPortBlockEntity extends TesuWirelessPortBlockEntity {
    public static final int RANGE = TesuWireless.RANGE, RESCAN = 40;
    public static final long PER_ITEM = TesuOutputPortBlockEntity.MAX_RATE;

    private List<BlockPos> machines = List.of();
    private long scannedAt = Long.MIN_VALUE / 2;

    public TesuInductionPortBlockEntity(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.TESU_INDUCTION_PORT.get(), pos, state);
    }

    public static void tick(Level level, TesuInductionPortBlockEntity port) {
        if (!(level instanceof ServerLevel server)) return;
        TesuControllerBlockEntity core = port.controller();
        if (core == null || core.getStoredEnergy() <= 0) return;
        ChargepadHelper.Source source = new ChargepadHelper.Source() {
            @Override public long available() { return core.getStoredEnergy(); }
            @Override public long take(long amount) { return core.consumeEnergy(amount, false); }
            @Override public void refund(long amount) { core.generateEnergyInternal(amount, false); }
        };
        long moved = WirelessInduction.chargePlayers(server, port.getBlockPos(), RANGE, source, PER_ITEM);
        long now = level.getGameTime();
        if (now - port.scannedAt >= RESCAN || now < port.scannedAt) port.rescan(server, now);
        for (BlockPos at : port.machines) {
            if (source.available() <= 0) break;
            if (!level.isLoaded(at) || !(level.getBlockEntity(at) instanceof AbstractEnergyBlockEntity machine) || !WirelessInduction.chargeable(machine)) continue;
            long filled = WirelessInduction.fill(machine, source);
            if (filled > 0) {
                moved += filled;
                TesuWirelessBeams.fed(server, port.getBlockPos(), at);   // the particle stream to it
            }
        }
        if (moved > 0) core.setChanged();
    }

    /** The machines in the cube (block entities of the loaded chunks it touches). */
    private void rescan(ServerLevel level, long now) {
        scannedAt = now;
        List<BlockPos> found = new ArrayList<>();
        BlockPos at = getBlockPos();
        for (int cx = (at.getX() - RANGE) >> 4; cx <= (at.getX() + RANGE) >> 4; cx++)
            for (int cz = (at.getZ() - RANGE) >> 4; cz <= (at.getZ() + RANGE) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values())
                    if (TesuWireless.covers(at, be.getBlockPos()) && WirelessInduction.chargeable(be)) found.add(be.getBlockPos().immutable());
            }
        machines = List.copyOf(found);
    }

    /** The machines found in the cube at the last look (tests). */
    public int machineCount() { return machines.size(); }
}

package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.core.api.machine.IEnergyContainerBlock;
import com.miophas.singularity_iteration.core.api.machine.IStorageMember;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyCore;
import com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyPart;
import com.miophas.singularity_iteration.core.runtime.machine.ChargepadHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * 0.1.11.206: what the wireless induction ports share (the GESU's and the TESU's): charging the players in a cube round the port
 * (their armor, hands and inventory, as a charge pad does, see {@link ChargepadHelper}) and, for the TESU's, filling machines
 * that have no wireless upgrade.
 */
public final class WirelessInduction {
    private WirelessInduction() {}

    /**
     * Charges every player within {@code range} blocks of {@code at} (a cube) from {@code source}, at most {@code perItem} EU an
     * item a tick; a charged player gives off a few sparks now and then. Returns the EU moved.
     */
    public static long chargePlayers(ServerLevel level, BlockPos at, int range, ChargepadHelper.Source source, long perItem) {
        if (source.available() <= 0) return 0;
        long total = 0;
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(at).inflate(range), p -> p.isAlive() && !p.isSpectator())) {
            if (source.available() <= 0) break;
            long moved = ChargepadHelper.chargePlayer(player, source, perItem);
            if (moved <= 0) continue;
            total += moved;
            if (level.getGameTime() % 10 == 0)
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(), 3, 0.3, 0.5, 0.3, 0.02);
        }
        return total;
    }

    /**
     * Whether the TESU's induction port fills {@code be} without a wireless upgrade: a machine that takes energy. Not a generator
     * (a power source), not an energy store (a battery box, a GESU or TESU part: energy would only move between stores), not a
     * cable or a node.
     */
    public static boolean chargeable(BlockEntity be) {
        if (!(be instanceof AbstractEnergyBlockEntity machine) || machine.isPowerSource()) return false;
        if (be instanceof IEnergyContainerBlock || be instanceof IStorageMember || be instanceof MultiblockEnergyPart || be instanceof MultiblockEnergyCore) return false;
        String pkg = be.getClass().getPackageName();
        return !pkg.endsWith(".wiring") && !pkg.endsWith(".batbox") && !pkg.endsWith(".transformer");
    }

    /** Fills {@code machine}'s storage from {@code source} (whatever its voltage, as the wireless upgrade does); returns the EU moved. */
    public static long fill(AbstractEnergyBlockEntity machine, ChargepadHelper.Source source) {
        var storage = machine.getEnergyStorageInternal();
        long room = Math.min(storage.getCapacity(), machine.getEffectiveCapacity()) - storage.getAmount();
        if (room <= 0) return 0;
        long got = source.take(Math.min(room, source.available()));
        if (got <= 0) return 0;
        long stored = storage.generateEnergyInternal(got, false);
        if (stored < got) source.refund(got - stored);
        return stored;
    }
}

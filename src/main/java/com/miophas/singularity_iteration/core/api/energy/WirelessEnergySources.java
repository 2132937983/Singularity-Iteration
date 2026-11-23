package com.miophas.singularity_iteration.core.api.energy;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * 0.1.11.158: where machines with the wireless upgrade take their energy from. The content side installs the source (the TESU
 * wireless field: a wireless output port and the relays linked to it); a machine asks it for the EU it has room for and adds
 * what it gets. Nothing is installed by default: the machine gets nothing.
 */
public final class WirelessEnergySources {
    private WirelessEnergySources() {}

    /** A wireless energy source. */
    @FunctionalInterface
    public interface Source {
        /**
         * Takes up to {@code wanted} EU for a machine at {@code at} and returns what it took (0 when no field covers it). What it
         * returns is gone from the source: the caller must add all of it.
         */
        long draw(ServerLevel level, BlockPos at, long wanted);
    }

    private static volatile Source source = (level, at, wanted) -> 0L;

    /** Installs the source (the content side, once at start). */
    public static void install(Source s) { source = Objects.requireNonNull(s, "wireless energy source"); }

    /** EU a machine at {@code at} gets now, at most {@code wanted}. */
    public static long draw(ServerLevel level, BlockPos at, long wanted) {
        if (wanted <= 0) return 0L;
        return Math.max(0L, Math.min(wanted, source.draw(level, at, wanted)));
    }
}

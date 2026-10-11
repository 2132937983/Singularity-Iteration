package com.miophas.singularity_iteration.core.api.machine;

import net.minecraft.core.Direction;

import java.util.Collection;

/**
 * Pipe block interface.
 *
 * <p>It describes the behavior and state of fluid and item pipes.
 * A block that implements this interface can manage connection sides, extract/push configuration and transfer rate.
 */
public interface IPipeBlock {

    /**
     * Pipe type enumeration.
     */
    enum PipeType {
        /** Item pipe */
        ITEM,
        /** Fluid pipe */
        FLUID,
        /** Cable (energy pipe) */
        CABLE,
        /** General pipe */
        UNIVERSAL
    }

    /**
     * Gets the pipe type.
     * @return the pipe type
     */
    PipeType getPipeType();

    /**
     * Gets the set of sides that are connected now.
     * @return the connected sides
     */
    Collection<Direction> getConnections();

    /**
     * Checks if the given side is connected.
     * @param side the side
     * @return true if the side is connected
     */
    boolean isConnected(Direction side);

    /**
     * Checks if the given side is in extract mode.
     * @param side the side
     * @return true if the pipe extracts from this side
     */
    boolean isExtracting(Direction side);

    /**
     * Sets the extract mode of the given side.
     * @param side       the side
     * @param extracting true to extract
     */
    void setExtracting(Direction side, boolean extracting);

    /**
     * Gets the transfer rate.
     * @return the maximum transfer amount per tick
     */
    int getTransferRate();

    /**
     * Checks if the pipe is blocked.
     * @return true if the pipe is blocked
     */
    default boolean isBlocked() {
        return false;
    }
}

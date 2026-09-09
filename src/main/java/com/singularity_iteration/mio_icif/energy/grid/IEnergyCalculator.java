package com.singularity_iteration.mio_icif.energy.grid;

import java.io.PrintStream;

/**
 * Interface for the energy calculation engine.
 */
public interface IEnergyCalculator {
    void handleGridChange(Grid grid);
    boolean runSyncStep(EnergyNetLocal enet);
    boolean runSyncStep(Grid grid);
    void runAsyncStep(Grid grid);
    NodeStats getNodeStats(Tile tile);
    void dumpNodeInfo(Node node, String prefix, PrintStream console, PrintStream chat);
}



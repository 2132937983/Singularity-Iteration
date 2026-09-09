package com.singularity_iteration.mio_icif.energy.grid;

/**
 * Settings for the EnergyNet debugging/logging.
 */
public class EnergyNetSettings {
    public static boolean logGridUpdateIssues = false;
    public static boolean logGridUpdatesVerbose = false;
    
    public static int pathfindingThreshold = 2048;
    public static boolean roundLossDown = true;

    public static int maxTileRegistrationsPerTick = 64;
}
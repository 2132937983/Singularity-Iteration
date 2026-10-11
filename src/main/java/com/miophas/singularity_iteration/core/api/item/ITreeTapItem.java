package com.miophas.singularity_iteration.core.api.item;

/**
 * Marker interface for tree tap items.
 * <p>
 * Items that implement this interface are identified as tree tap tools.
 * Use them to extract resin from rubber wood.
 *
 * <p>Addon mods can let a custom item implement this interface. {@code IRubberTreeAPI}
 * and the rubber wood blocks then identify it and use it to harvest resin. The default values match the normal (wooden) tap:
 * each harvest uses 1 durability point and drops 1 to 3 resin. An electric version can override the methods to change this behavior.
 */
public interface ITreeTapItem {

    /**
     * Returns the durability that one harvest removes from the tool.
     *
     * <p>The default is 1. An electric tap must return 0 (it uses electric energy instead).
     *
     * @return the durability cost
     */
    default int getDurabilityCost() {
        return 1;
    }

    /**
     * Returns the minimum number of resin items from one harvest (inclusive).
     *
     * @return the minimum number
     */
    default int getResinDropMin() {
        return 1;
    }

    /**
     * Returns the maximum number of resin items from one harvest (inclusive).
     *
     * @return the maximum number
     */
    default int getResinDropMax() {
        return 3;
    }
}

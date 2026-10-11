package com.miophas.singularity_iteration.core.api.world;

/**
 * Result of one resin harvest.
 *
 * @param harvested  true if the harvest completed (only on the server side, when the conditions are met)
 * @param resinCount the number of resin items dropped by this harvest. 0 if no harvest occurred.
 */
public record RubberHarvestResult(boolean harvested, int resinCount) {

    /** Result when no harvest occurred. */
    public static final RubberHarvestResult NONE = new RubberHarvestResult(false, 0);

    /** Result of a successful harvest. */
    public static RubberHarvestResult success(int resinCount) {
        return new RubberHarvestResult(true, resinCount);
    }
}

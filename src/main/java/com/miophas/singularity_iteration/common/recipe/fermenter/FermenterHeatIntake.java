package com.miophas.singularity_iteration.common.recipe.fermenter;

/**
 * 0.1.10.24: how much heat the fermenter takes, as in IC2 1.12.2 {@code TileEntityFermenter.work()}.
 *
 * <p>IC2 draws heat from the front heat source only in a tick in which a run can start (a recipe for the input fluid, at
 * least one run of input in the tank and room for one run of output), and asks for 100 HU in that tick. The fermenter here
 * gets heat on two paths (a generator pushes it in, the machine pulls from a heat store), so both share one allowance of
 * {@link #DRAW_PER_TICK} HU per game tick. Before 0.1.10.24 the fermenter filled its 4,000 HU store whenever the store had
 * room, also with no biomass or a full biogas tank; the heat source burned fuel for it and the first runs after that went
 * faster than the source.
 *
 * <p>Pure (no game types): the offline harness runs this class.
 */
public final class FermenterHeatIntake {
    /** IC2 asks its front heat source for 100 HU per tick. */
    public static final long DRAW_PER_TICK = 100;

    private long tick = Long.MIN_VALUE;
    private long taken;

    /** HU the fermenter may still take in game tick {@code now}; 0 when no run can start. */
    public long room(long now, boolean canRun) {
        if (!canRun) return 0;
        return now == tick ? Math.max(0, DRAW_PER_TICK - taken) : DRAW_PER_TICK;
    }

    /** Records {@code amount} HU taken in game tick {@code now}. */
    public void took(long now, long amount) {
        if (amount <= 0) return;
        if (now != tick) {
            tick = now;
            taken = 0;
        }
        taken += amount;
    }
}

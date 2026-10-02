package com.miophas.singularity_iteration.common.armory;

import net.minecraft.world.item.ItemStack;

/**
 * Keeps stored suits battle-ready: repairs durability and recharges built-in batteries,
 * paying with the Armory's EU (or mana for Botania gear), and estimates how long it will
 * take until everything is done.
 *
 * <p>All pieces are serviced in parallel, each at a fixed rate, inside a per-tick EU budget.
 * A reserve is always kept so a summon never fails because maintenance drained the tank.
 * The estimate is the slowest of: the slowest piece at its service rate, the EU budget,
 * the EU still missing at the current income, and the same for mana.
 */
public final class ArmoryMaintenance {
    public static final long REPAIR_EU_PER_POINT = 40;
    public static final int REPAIR_POINTS_PER_TICK = 1;
    public static final long CHARGE_EU_PER_TICK = 256;
    public static final long EU_BUDGET_PER_TICK = 1024;
    public static final int MANA_CHARGE_PER_TICK = 1000;
    /** EU never spent on maintenance: enough for a full six-piece summon over ~60 blocks. */
    public static final long SUMMON_RESERVE = 6 * (mio_icif_armory.COST_PER_PIECE + 64 * mio_icif_armory.COST_PER_PIECE_PER_BLOCK);

    public static final int IDLE = 0, WORKING = 1, NEED_POWER = 2, NEED_MANA = 3;
    /** {@link #etaTicks} value for "cannot finish at the current income". */
    public static final int ETA_UNKNOWN = -1;

    private final mio_icif_armory armory;
    private double euIncome, manaIncome;
    private long lastEnergy = -1;
    private int lastMana = -1;
    private long spentThisTick;
    private int manaSpentThisTick;
    private boolean repairedThisTick;

    // published state
    private int state = IDLE, etaTicks, pendingPieces;
    private long euNeeded;
    private int manaNeeded;
    // stats since work started, for the completion notice
    private int repairedPoints;
    private long chargedEu;
    private int manaUsed;
    private boolean session;

    ArmoryMaintenance(mio_icif_armory armory) {
        this.armory = armory;
    }

    public int state() { return state; }
    public int etaTicks() { return etaTicks; }
    public int pendingPieces() { return pendingPieces; }
    public long euNeeded() { return euNeeded; }
    public int manaNeeded() { return manaNeeded; }
    /** Finished service sessions (each one sent a completion notice). */
    public int completedSessions() { return completedSessions; }
    private int completedSessions;

    /** Runs one tick of service. Returns a finished-session report, or null. */
    Report tick(long gameTime) {
        sampleIncome();
        spentThisTick = 0;
        manaSpentThisTick = 0;
        repairedThisTick = false;
        long budget = Math.min(EU_BUDGET_PER_TICK, Math.max(0, armory.storedEnergy() - SUMMON_RESERVE));
        for (int set = 0; set < mio_icif_armory.SETS; set++) {
            for (ArmoryPiece piece : ArmoryPiece.COLUMNS) {
                ItemStack stack = armory.piece(set, piece);
                if (stack.isEmpty()) continue;
                budget -= service(stack, budget);
            }
        }
        if (spentThisTick > 0) armory.spendEnergy(spentThisTick);
        if (manaSpentThisTick > 0) armory.spendMana(manaSpentThisTick);
        if (spentThisTick > 0 || manaSpentThisTick > 0 || repairedThisTick) armory.markStorageChanged();
        lastEnergy = armory.storedEnergy();
        lastMana = armory.mana();

        boolean worked = spentThisTick > 0 || manaSpentThisTick > 0;
        if (gameTime % 10 == 0 || (worked && pendingPieces == 0)) estimate();
        if (pendingPieces > 0) {
            if (repairedPoints > 0 || chargedEu > 0 || manaUsed > 0) session = true;
            return null;
        }
        if (!session) return null;
        Report report = new Report(repairedPoints, chargedEu, manaUsed);
        completedSessions++;
        session = false;
        repairedPoints = 0;
        chargedEu = 0;
        manaUsed = 0;
        return report;
    }

    /** Services one piece within {@code budget} EU; returns the EU it spent. */
    private long service(ItemStack stack, long budget) {
        long spent = 0;
        if (needsRepair(stack)) {
            int points = Math.min(REPAIR_POINTS_PER_TICK, stack.getDamageValue());
            if (ArmoryMana.isManaGear(stack)) {
                int cost = points * ArmoryMana.MANA_PER_DURABILITY;
                if (armory.mana() - manaSpentThisTick >= cost) {
                    stack.setDamageValue(stack.getDamageValue() - points);
                    manaSpentThisTick += cost;
                    manaUsed += cost;
                    repairedPoints += points;
                    repairedThisTick = true;
                }
            } else if (budget - spent >= points * REPAIR_EU_PER_POINT) {
                stack.setDamageValue(stack.getDamageValue() - points);
                spent += points * REPAIR_EU_PER_POINT;
                repairedPoints += points;
                repairedThisTick = true;
            }
        }
        long offer = Math.min(CHARGE_EU_PER_TICK, budget - spent);
        if (offer > 0 && ArmoryItemEnergy.room(stack) > 0) {
            long took = ArmoryItemEnergy.charge(stack, offer, false);
            spent += took;
            chargedEu += took;
        }
        int manaLeft = armory.mana() - manaSpentThisTick;
        if (manaLeft > 0 && ArmoryMana.itemRoom(stack) > 0) {
            int took = ArmoryMana.fillItem(stack, Math.min(MANA_CHARGE_PER_TICK, manaLeft));
            manaSpentThisTick += took;
            manaUsed += took;
        }
        spentThisTick += spent;
        return spent;
    }

    static boolean needsRepair(ItemStack stack) {
        return stack.isDamageableItem() && stack.isDamaged() && stack.getCount() == 1;
    }

    private void sampleIncome() {
        long energy = armory.storedEnergy();
        int mana = armory.mana();
        // Anything that went up since the end of last tick came in from outside.
        if (lastEnergy >= 0) euIncome = euIncome * 0.95 + Math.max(0, energy - lastEnergy) * 0.05;
        if (lastMana >= 0) manaIncome = manaIncome * 0.95 + Math.max(0, mana - lastMana) * 0.05;
    }

    /** Recomputes the outstanding work and the time to finish it. */
    void estimate() {
        long eu = 0;
        int mana = 0, pending = 0;
        long slowest = 0;
        for (int set = 0; set < mio_icif_armory.SETS; set++) {
            for (ArmoryPiece piece : ArmoryPiece.COLUMNS) {
                ItemStack stack = armory.piece(set, piece);
                if (stack.isEmpty()) continue;
                long ticks = 0;
                boolean work = false;
                if (needsRepair(stack)) {
                    int points = stack.getDamageValue();
                    if (ArmoryMana.isManaGear(stack)) mana += points * ArmoryMana.MANA_PER_DURABILITY;
                    else eu += points * REPAIR_EU_PER_POINT;
                    ticks = Math.max(ticks, ceil(points, REPAIR_POINTS_PER_TICK));
                    work = true;
                }
                long room = ArmoryItemEnergy.room(stack);
                if (room > 0) {
                    eu += room;
                    ticks = Math.max(ticks, ceil(room, CHARGE_EU_PER_TICK));
                    work = true;
                }
                int manaRoom = ArmoryMana.itemRoom(stack);
                if (manaRoom > 0) {
                    mana += manaRoom;
                    ticks = Math.max(ticks, ceil(manaRoom, MANA_CHARGE_PER_TICK));
                    work = true;
                }
                if (work) pending++;
                slowest = Math.max(slowest, ticks);
            }
        }
        pendingPieces = pending;
        euNeeded = eu;
        manaNeeded = mana;
        if (pending == 0) {
            state = IDLE;
            etaTicks = 0;
            return;
        }
        long eta = Math.max(slowest, ceil(eu, EU_BUDGET_PER_TICK));
        state = WORKING;
        long euAvailable = Math.max(0, armory.storedEnergy() - SUMMON_RESERVE);
        if (eu > euAvailable) {
            if (euIncome < 0.5) { state = NEED_POWER; eta = ETA_UNKNOWN; }
            else eta = Math.max(eta, (long) Math.ceil((eu - euAvailable) / euIncome));
        }
        if (eta != ETA_UNKNOWN && mana > armory.mana()) {
            if (manaIncome < 0.5) { state = NEED_MANA; eta = ETA_UNKNOWN; }
            else eta = Math.max(eta, (long) Math.ceil((mana - armory.mana()) / manaIncome));
        }
        etaTicks = eta == ETA_UNKNOWN ? ETA_UNKNOWN : (int) Math.min(Integer.MAX_VALUE, eta);
    }

    private static long ceil(long a, long b) {
        return (a + b - 1) / b;
    }

    /** What a finished maintenance session did. */
    public record Report(int repairedPoints, long chargedEu, int manaUsed) { }
}

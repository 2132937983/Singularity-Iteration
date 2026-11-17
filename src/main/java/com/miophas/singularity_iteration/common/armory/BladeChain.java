// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.53 (gaze and Iron Man phase P1, decision two A): the SlashBlade: Resharped combo chains a Wingman plays, with
 * the input timing that makes each chain run through to its finisher.
 *
 * <p>From the 2.1.9 bytecode ({@code ComboStateRegistry}): a click advances the combo only inside the state's window
 * (counted in ticks from the click that started the state; a click too early is dropped, a late one restarts the
 * chain), the ground or aerial chain is chosen by {@code onGround} at each click, and a finisher plays for its full
 * length unless a new click cuts it. Before, a Wingman clicked every 8 ticks while flying: it only ever played the aerial
 * chain, cut the finishers, and its "rapid slash" (sprint + click) was a plain click.
 *
 * <p>Each chain is a list of steps: the ticks after the last click and the combo the click must lead to; after the last
 * step the Wingman rests for the finisher's length. A click that leads elsewhere ends the chain (a short rest, then a new
 * one). Pure logic, so tests can check the plans against the windows.
 */
public final class BladeChain {
    /** The chains. */
    public enum Kind {
        /** ar_a1, ar_a2, ar_a3. */
        AERIAL_A(false),
        /** ar_a1, ar_a2, (ar_a2_end) ar_b3 (the kick up), ar_b4. */
        AERIAL_B(false),
        /** The aerial cleave: a dive onto the target, the landing slash. */
        CLEAVE(false),
        /** a1, a2, a3, a4 (a4_ex and a5ex when powered: Strength or Hunger). */
        GROUND_A(true),
        /** a1, a2, (a2_end) combo_c. */
        GROUND_C(true),
        /** a1, a2, a3, (a3_end) b1 .. b7: the flurry. */
        GROUND_B(true);

        public final boolean grounded;

        Kind(boolean grounded) { this.grounded = grounded; }
    }

    /** One click: {@code after} ticks after the last one, leading to one of {@code expect} (paths in slashblade:). */
    public record Step(int after, String... expect) {
        public boolean accepts(@Nullable String combo) {
            if (combo == null) return false;
            for (String e : expect) if (e.equals(combo)) return true;
            return false;
        }
    }

    /** Ticks of rest after a click that led elsewhere. */
    public static final int MISS_REST = 10;

    /** The steps of a chain. */
    public static List<Step> steps(Kind kind) {
        List<Step> s = new ArrayList<>();
        switch (kind) {
            case AERIAL_A -> {
                s.add(new Step(0, "aerial_rave_a1"));
                s.add(new Step(4, "aerial_rave_a2"));        // a1 window 3..14
                s.add(new Step(4, "aerial_rave_a3"));        // a2 window 3..6
            }
            case AERIAL_B -> {
                s.add(new Step(0, "aerial_rave_a1"));
                s.add(new Step(4, "aerial_rave_a2"));
                s.add(new Step(11, "aerial_rave_b3"));       // a2 ends at 7, a2_end 7..21 leads to b3
                s.add(new Step(8, "aerial_rave_b4"));        // b3 window 6..24
            }
            case CLEAVE -> s.add(new Step(0, "aerial_cleave"));
            case GROUND_A -> {
                s.add(new Step(0, "combo_a1"));
                s.add(new Step(4, "combo_a2"));              // a1 window 3..6 (a1_end to 14)
                s.add(new Step(5, "combo_a3"));              // a2 window 3..10
                s.add(new Step(8, "combo_a4", "combo_a4_ex"));   // a3 window 6..12
            }
            case GROUND_C -> {
                s.add(new Step(0, "combo_a1"));
                s.add(new Step(4, "combo_a2"));
                s.add(new Step(15, "combo_c"));              // a2 ends at 11, a2_end 11..22 leads to combo_c
            }
            case GROUND_B -> {
                s.add(new Step(0, "combo_a1"));
                s.add(new Step(4, "combo_a2"));
                s.add(new Step(5, "combo_a3"));
                s.add(new Step(17, "combo_b1"));             // a3 ends at 13, a3_end 13..21 leads to b1
                s.add(new Step(10, "combo_b2"));             // b1 window 8..13
                for (int b = 3; b <= 7; b++) s.add(new Step(5, "combo_b" + b));   // b2..b6 window 4..6
            }
        }
        return s;
    }

    /** Ticks a finisher plays after the last click of {@code kind} (the last combo reached). */
    public static int rest(Kind kind, @Nullable String last) {
        return switch (kind) {
            case AERIAL_A -> 19;
            case AERIAL_B -> 25;
            case CLEAVE -> 40;
            case GROUND_A -> "combo_a5ex".equals(last) ? 76 : "combo_a4_ex".equals(last) ? 27 : 51;
            case GROUND_C -> 40;
            case GROUND_B -> 37;
        };
    }

    private Kind kind;
    private List<Step> steps = List.of();
    private int index;
    private long lastClick = Long.MIN_VALUE / 2, restUntil = Long.MIN_VALUE / 2;
    private String last;

    /** The chain running (between its first click and the end of its rest), or null. */
    @Nullable
    public Kind running(long now) { return kind != null && (index < steps.size() || now < restUntil) ? kind : null; }

    /** True when no chain runs and no rest is left: a new chain may start. */
    public boolean free(long now) { return running(now) == null && now >= restUntil; }

    /** True while the finisher of the chain plays (after its last click). */
    public boolean resting(long now) { return kind != null && index >= steps.size() && now < restUntil; }

    /** Starts {@code kind}; its first click is due at once. */
    public void start(Kind kind, long now) {
        this.kind = kind;
        this.steps = steps(kind);
        this.index = 0;
        this.last = null;
        this.lastClick = now;
        this.restUntil = Long.MIN_VALUE / 2;
    }

    /** True when the next click of the running chain is due. */
    public boolean due(long now) {
        return kind != null && index < steps.size() && now - lastClick >= steps.get(index).after();
    }

    /** True when the click due now is the first of the chain. */
    public boolean first() { return index == 0; }

    /**
     * Records the combo a due click led to. Returns true when the chain goes on (or finished properly), false when it
     * ended because the click led elsewhere.
     */
    public boolean clicked(long now, @Nullable String combo) {
        Step step = steps.get(index);
        if (!step.accepts(combo)) {
            end(now, MISS_REST);
            return false;
        }
        last = combo;
        lastClick = now;
        index++;
        if (kind == Kind.GROUND_A && "combo_a4_ex".equals(combo)) {
            steps = new ArrayList<>(steps);
            steps.add(new Step(15, "combo_a5ex"));   // a4_ex window 14..26
        }
        if (index >= steps.size()) end(now, rest(kind, combo));
        return true;
    }

    /** Ends the chain with a rest of {@code ticks} (an abort: 0 or MISS_REST). */
    public void end(long now, int ticks) {
        index = steps.size();
        restUntil = now + ticks;
        if (ticks <= 0) kind = null;
    }

    /** The last combo reached in the chain (null before the first click). */
    @Nullable
    public String last() { return last; }

    /** Clicks done in the running chain. */
    public int clicks() { return index; }
}

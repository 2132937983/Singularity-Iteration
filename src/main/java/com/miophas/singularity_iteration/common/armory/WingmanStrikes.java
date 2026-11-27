// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.229 (Wingman motion, AN1 part 2): the Wingman's combat actions and their timing, shared by the server (when a blow
 * lands) and the client (which keyframe animation plays). The ticks come from {@code docs/art/an1/spec.json}: a strike
 * starts, the blow lands on its {@code impact} tick (the wind-up before it is what gives it weight), and the animation runs
 * on for its recovery.
 *
 * <p>The action kinds are synced in the low {@link #KIND_BITS} bits of a counter ({@code WingmanEntity.DATA_ACTION}); a new
 * value starts the animation again on every client, even for the same kind twice in a row.
 */
public final class WingmanStrikes {
    private WingmanStrikes() {}

    public static final int KIND_BITS = 5, KIND_MASK = (1 << KIND_BITS) - 1;

    /** One action: its id, keyframe animation (short name), impact and length in ticks, damage factor, finisher, knockback. */
    public enum Action {
        NONE(0, null, 0, 0, 0, false, 0),
        MELEE_1(1, "melee_1", 3, 9, 1.0, false, 0),
        MELEE_2(2, "melee_2", 3, 9, 1.0, false, 0),
        MELEE_3(3, "melee_3", 5, 13, 1.25, true, 0.6),
        HEAVY(4, "melee_heavy", 11, 20, 1.0, true, 0.8),
        KICK(5, "kick", 5, 11, 0.5, true, 1.4),
        DASH(6, "dash_stab", 4, 10, 1.0, true, 0.5),
        SLAM_FALL(7, "slam_fall", 0, 40, 0, false, 0),
        SLAM_IMPACT(8, "slam_impact", 0, 14, 1.5, true, 1.0),
        SHOVE(9, "shove", 3, 8, 0.5, true, 1.6),
        INTERCEPT(10, "intercept", 2, 6, 0, false, 0),
        FLOURISH(11, "kill_flourish", 0, 24, 0, false, 0);

        public final int id, impact, length;
        @Nullable public final String animation;
        public final double damage, knockback;
        public final boolean finisher;

        Action(int id, @Nullable String animation, int impact, int length, double damage, boolean finisher, double knockback) {
            this.id = id;
            this.animation = animation;
            this.impact = impact;
            this.length = length;
            this.damage = damage;
            this.finisher = finisher;
            this.knockback = knockback;
        }

        private static final Action[] BY_ID = new Action[KIND_MASK + 1];
        static { for (Action a : values()) BY_ID[a.id] = a; }

        public static Action byId(int id) {
            Action a = id >= 0 && id < BY_ID.length ? BY_ID[id] : null;
            return a == null ? NONE : a;
        }
    }

    /** The combo step after {@code step} (0, 1, 2 → melee 1, 2, 3); a pause longer than {@link #COMBO_GAP} ticks starts over. */
    public static final int COMBO_GAP = 30;

    public static Action combo(int step) {
        return switch (Math.floorMod(step, 3)) {
            case 0 -> Action.MELEE_1;
            case 1 -> Action.MELEE_2;
            default -> Action.MELEE_3;
        };
    }

    /** The synced value after {@code value} for a new {@code action}: the counter goes up, the kind is replaced. */
    public static int next(int value, Action action) {
        return ((value >>> KIND_BITS) + 1 << KIND_BITS) | action.id;
    }

    public static Action kindOf(int value) { return Action.byId(value & KIND_MASK); }
}

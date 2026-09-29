// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.Objects;
import java.util.random.RandomGenerator;

/** Independent recipient priority policy fitted to public R14/R15/R16 observations. */
public final class ReceiverOrder {
    private ReceiverOrder() { }

    /**
     * Preserve registration priority every fourth world tick. Otherwise choose a
     * uniform starting recipient, using the caller's generator, and traverse
     * eligible registration ranks backwards with wraparound. R15's 31/1 splits
     * distinguish this order from R14's equally distributed full permutations.
     * The caller includes full but connected receivers in the eligibility domain:
     * R16 observes that they still occupy an offset; packet accounting skips their
     * zero demand. Unreachable entries remain after considered entries, retaining
     * stable receiver IDs without occupying a random starting position.
     * This models the observed marginals and fixed phase, not an original PRNG.
     */
    public static int[] create(int[] registrationOrder, boolean[] eligible,
                               long worldTime, RandomGenerator random) {
        Objects.requireNonNull(registrationOrder, "registrationOrder");
        Objects.requireNonNull(eligible, "eligible");
        Objects.requireNonNull(random, "random");
        if (worldTime < 0 || registrationOrder.length != eligible.length) {
            throw new IllegalArgumentException("Invalid time or receiver vectors");
        }
        int size = eligible.length, count = 0;
        boolean[] seen = new boolean[size];
        for (int receiver : registrationOrder) {
            if (receiver < 0 || receiver >= size || seen[receiver]) {
                throw new IllegalArgumentException("Registration order must be a complete permutation");
            }
            seen[receiver] = true;
            if (eligible[receiver]) count++;
        }
        int[] result = new int[size];
        int active = 0, inactive = count;
        for (int receiver : registrationOrder) {
            if (eligible[receiver]) result[active++] = receiver;
            else result[inactive++] = receiver;
        }
        if (count > 1) {
            int first = worldTime % 4 == 0 ? 0 : random.nextInt(count);
            // Two disjoint reversals form a backwards circular visit without a
            // second receiver array, preserving the inactive tail unchanged.
            reverse(result, 0, first);
            reverse(result, first + 1, count - 1);
        }
        return result;
    }

    private static void reverse(int[] values, int left, int right) {
        while (left < right) {
            int saved = values[left]; values[left++] = values[right]; values[right--] = saved;
        }
    }
}

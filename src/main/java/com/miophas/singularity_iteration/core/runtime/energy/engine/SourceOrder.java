// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.Objects;
import java.util.random.RandomGenerator;

/** Independent forward source cycle within one connected conductor domain. */
public final class SourceOrder {
    private SourceOrder() { }

    /**
     * Choose a uniform starting source, then traverse caller registration IDs
     * forwards. R16 mixed-offer observations distinguish this from complete
     * random permutations and from the receiver's reverse/fixed-phase policy.
     * Only currently offering sources occupy a starting position. Different
     * conductor domains must be settled separately; a shared receiver does not
     * by itself connect their conductors. The original PRNG is not reproduced.
     */
    public static int[] create(int count, RandomGenerator random) {
        Objects.requireNonNull(random, "random");
        if (count < 0) throw new IllegalArgumentException("Negative source count");
        int[] result = new int[count];
        int first = count > 1 ? random.nextInt(count) : 0;
        for (int i = 0; i < count; i++) result[i] = (int) (((long) first + i) % count);
        return result;
    }
}

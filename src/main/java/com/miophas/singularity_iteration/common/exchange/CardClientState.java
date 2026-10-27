// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

/** 0.1.10.28 (X4): the last {@link CardView} the server sent (client thread). */
public final class CardClientState {
    private CardClientState() {}

    private static CardView view;

    public static CardView view() { return view; }

    public static void set(CardView v) { view = v; }

    public static void clear() { view = null; }
}

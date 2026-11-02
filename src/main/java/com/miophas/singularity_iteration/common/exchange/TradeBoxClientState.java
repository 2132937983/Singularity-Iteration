// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

/** 0.1.10.42: the last {@link TradeBoxView} the server sent (client thread). */
public final class TradeBoxClientState {
    private TradeBoxClientState() {}

    private static TradeBoxView view;

    public static TradeBoxView view() { return view; }

    public static void set(TradeBoxView v) { view = v; }

    public static void clear() { view = null; }
}

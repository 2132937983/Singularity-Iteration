// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

/**
 * 0.1.10.21: the last {@link ExchangeView} that the server sent to this client (the exchange panel reads it). No client
 * classes, so the payload handler can be registered in common code. Client main thread only.
 */
public final class ExchangeClientState {
    private ExchangeClientState() {}

    private static ExchangeView view;
    private static long revision;

    public static void set(ExchangeView next) { view = next; revision++; }

    public static ExchangeView view() { return view; }

    /** Changes with every new view. */
    public static long revision() { return revision; }

    public static void clear() { view = null; revision++; }
}

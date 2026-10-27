// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

/** 0.1.10.27 (X2): the last {@link TerminalView} the server sent (client thread). */
public final class TerminalClientState {
    private TerminalClientState() {}

    private static TerminalView view;

    public static TerminalView view() { return view; }

    public static void set(TerminalView v) { view = v; }

    public static void clear() { view = null; }
}

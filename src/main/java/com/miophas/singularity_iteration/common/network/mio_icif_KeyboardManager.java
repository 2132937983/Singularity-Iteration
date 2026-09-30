// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.core.prefab.flight.JetpackKeyHandler;
import net.minecraft.world.entity.player.Player;

/** Compatibility facade for the original public keyboard manager ABI. */
public class mio_icif_KeyboardManager {
    public static final int KEY_JUMP = 0;
    public static final int KEY_BOOST = 1;
    public static final int KEY_FORWARD = 2;
    public static final int KEY_SNEAK = 3;
    public static final int KEY_SAFETY = 4;

    public static void processKeyUpdate(Player player, int keyState) {
        JetpackKeyHandler.processKeyboardUpdate(player, keyState);
    }

    public static boolean isJumpKeyDown(Player player) { return JetpackKeyHandler.isJumpKeyDown(player); }
    public static boolean isBoostKeyDown(Player player) { return JetpackKeyHandler.isBoostKeyDown(player); }
    public static boolean isForwardKeyDown(Player player) { return JetpackKeyHandler.isForwardKeyDown(player); }
    public static boolean isSneakKeyDown(Player player) { return JetpackKeyHandler.isSneakKeyDown(player); }
    public static boolean isSafetyKeyDown(Player player) { return JetpackKeyHandler.isSafetyKeyDown(player); }
    public static void removePlayerReferences(Player player) { JetpackKeyHandler.removePlayer(player); }
}

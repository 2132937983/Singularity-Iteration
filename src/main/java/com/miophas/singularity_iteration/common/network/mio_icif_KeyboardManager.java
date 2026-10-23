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
    public static void removePlayerReferences(Player player) {
        JetpackKeyHandler.removePlayer(player);
        LOCK_NOTICES.remove(player.getUUID());
    }

    /** Game time of the last "hold the Safety Key" notice per player. */
    private static final java.util.Map<java.util.UUID, Long> LOCK_NOTICES = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 0.1.10.14: whether {@code player} may take a Transformer Upgrade out of a slot. It stays put
     * unless the Safety Key is held; the server then tells the player which key, at most once a second.
     */
    public static boolean mayTakeTransformerUpgrade(Player player) {
        if (isSafetyKeyDown(player)) return true;
        if (player instanceof net.minecraft.server.level.ServerPlayer server) {
            long now = server.serverLevel().getGameTime();
            Long last = LOCK_NOTICES.get(server.getUUID());
            if (last == null || now - last >= 20 || now < last) {
                LOCK_NOTICES.put(server.getUUID(), now);
                server.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.mio_icif.transformer_upgrade_locked",
                    net.minecraft.network.chat.Component.keybind("key.mio_icif.safety")).withStyle(net.minecraft.ChatFormatting.YELLOW));
            }
        }
        return false;
    }
}

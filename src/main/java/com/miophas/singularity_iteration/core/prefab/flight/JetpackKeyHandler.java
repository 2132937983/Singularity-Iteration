// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.prefab.flight;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;

/** Side-local input snapshots. The two existing packet layouts retain their own bit-four meaning. */
public class JetpackKeyHandler {
    private static final int JUMP = 1 << 0;
    private static final int BOOST = 1 << 1;
    private static final int FORWARD = 1 << 2;
    private static final int SNEAK = 1 << 3;
    private static final int MODE = 1 << 4;
    private static final int VALID_MASK = JUMP | BOOST | FORWARD | SNEAK | MODE;
    private static final Map<UUID, Snapshot> STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, Snapshot> CLIENT_STATES = new ConcurrentHashMap<>();
    private static final Snapshot EMPTY = new Snapshot(0, false);
    private record Snapshot(int bits, boolean safety) {}

    private static Map<UUID, Snapshot> states(Player player) {
        return player.level().isClientSide ? CLIENT_STATES : STATES;
    }

    public static void processKeyUpdate(Player player, int keyState) {
        if (player == null) return;
        Snapshot previous = state(player);
        update(player, keyState & VALID_MASK, previous.safety);
    }

    /** KeyboardStatePacket uses bit four for safety, unlike JetpackKeyStatePacket's mode bit. */
    public static void processKeyboardUpdate(Player player, int keyState) {
        if (player == null) return;
        Snapshot previous = state(player);
        update(player, (previous.bits & MODE) | (keyState & (VALID_MASK ^ MODE)),
                (keyState & MODE) != 0);
    }

    private static void update(Player player, int bits, boolean safety) {
        Map<UUID, Snapshot> states = states(player);
        UUID id = player.getUUID();
        if (bits == 0 && !safety) {
            states.remove(id);
        } else {
            Snapshot previous = states.get(id);
            if (previous == null || previous.bits != bits || previous.safety != safety) {
                states.put(id, new Snapshot(bits, safety));
            }
        }
    }

    private static Snapshot state(Player player) {
        return player == null ? EMPTY : states(player).getOrDefault(player.getUUID(), EMPTY);
    }

    public static boolean isJumpKeyDown(Player player) { return (state(player).bits & JUMP) != 0; }
    public static boolean isForwardKeyDown(Player player) { return (state(player).bits & FORWARD) != 0; }
    public static boolean isSneakKeyDown(Player player) { return (state(player).bits & SNEAK) != 0; }
    public static boolean isModeSwitchKeyDown(Player player) { return (state(player).bits & MODE) != 0; }
    public static boolean isAltKeyDown(Player player) { return isModeSwitchKeyDown(player); }
    public static boolean isBoostKeyDown(Player player) { return (state(player).bits & BOOST) != 0; }
    public static boolean isSafetyKeyDown(Player player) { return state(player).safety; }

    public static void removePlayer(Player player) {
        if (player != null) states(player).remove(player.getUUID());
    }

    public static void clearServerStates() { STATES.clear(); }
    public static void clearClientStates() { CLIENT_STATES.clear(); }

    public static class KeyState {
        public boolean jump;
        public boolean forward;
        public boolean sneak;
        public boolean modeSwitch;
        public boolean sprint;

        public int toInt() {
            int result = 0;
            if (jump) result |= JUMP;
            if (sprint) result |= BOOST;
            if (forward) result |= FORWARD;
            if (sneak) result |= SNEAK;
            if (modeSwitch) result |= MODE;
            return result;
        }

        public static KeyState fromInt(int bits) {
            KeyState state = new KeyState();
            state.jump = (bits & JUMP) != 0;
            state.sprint = (bits & BOOST) != 0;
            state.forward = (bits & FORWARD) != 0;
            state.sneak = (bits & SNEAK) != 0;
            state.modeSwitch = (bits & MODE) != 0;
            return state;
        }
    }
}

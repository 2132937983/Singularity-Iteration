// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Key repeat filter of the quantum command screens (0.1.11.40, Wingman audit W1, U-1 / U-2).
 *
 * <p>The game hands a key repeat to {@code Screen#keyPressed} and to {@code KeyMapping#click} exactly like a new press.
 * The command wheel ran its order on the first repeat of the held wheel key (about 0.5 s into a hold), the holo table
 * closed on the first repeat of the held map key, and with no screen open the repeats restarted the "hold N" timer. A
 * screen keeps a {@link Repeats} of the keys that are down: a press of a key that is already down is a repeat. The
 * clients' key loops wait with {@link #down} for the release of a key that closed or opened a screen.
 */
public final class HeldKeys {
    private HeldKeys() {}

    /** True while the key of {@code mapping} is physically down (a {@code KeyMapping} forgets it when a screen opens). */
    public static boolean down(@Nullable KeyMapping mapping) {
        if (mapping == null || mapping.isUnbound()) return false;
        long window = Minecraft.getInstance().getWindow().getWindow();
        InputConstants.Key key = mapping.getKey();
        return switch (key.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, key.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
            default -> false;
        };
    }

    /** The keys that are down while one screen is open. */
    public static final class Repeats {
        private final Set<Integer> down = new HashSet<>();

        /** Marks the key of {@code mapping} as down when it is held as the screen opens (the press that opened it). */
        public Repeats seed(@Nullable KeyMapping mapping) {
            if (down(mapping) && mapping.getKey().getType() == InputConstants.Type.KEYSYM) down.add(mapping.getKey().getValue());
            return this;
        }

        /** Called from {@code keyPressed}: true when the key was already down (a repeat to ignore). */
        public boolean repeat(int keyCode) {
            return !down.add(keyCode);
        }

        /** Called from {@code keyReleased}. */
        public void release(int keyCode) {
            down.remove(keyCode);
        }
    }
}

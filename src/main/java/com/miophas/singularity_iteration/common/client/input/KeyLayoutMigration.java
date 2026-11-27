// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.input;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/**
 * 0.1.11.230 (key consolidation, decision five A): moves the bindings that still sit on their old default to the new
 * default, once per client ({@code key_layout} in the client config). Minecraft saves every binding to options.txt, the
 * defaults too, so without this a player of an older version would keep the old keys; a key the player set themselves
 * (anything but the old default) is left as it is. The player is told once, in game.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class KeyLayoutMigration {
    private KeyLayoutMigration() {}

    /** The layout this version moves to. */
    public static final int LAYOUT = 1;

    /** Bindings whose default changed in layout 1, with their old default key code. */
    public static final Map<String, Integer> OLD_DEFAULTS = Map.of(
        "key.mio_icif.jetpack_mode", GLFW.GLFW_KEY_K,
        "key.mio_icif.boost", GLFW.GLFW_KEY_LEFT_CONTROL,
        "key.mio_icif.pilot", GLFW.GLFW_KEY_U,
        "key.mio_icif.piece_call", GLFW.GLFW_KEY_J,
        "key.mio_icif.tactical_map", GLFW.GLFW_KEY_N,
        "key.mio_icif.fcs_hud", GLFW.GLFW_KEY_H,
        "key.mio_icif.hud_detail", GLFW.GLFW_KEY_LEFT_ALT,
        "key.mio_icif.core_burst", GLFW.GLFW_KEY_R);

    private static boolean done, tell;

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!done && mc.options != null && FcsClientConfig.loaded()) {
            done = true;
            if (FcsClientConfig.KEY_LAYOUT.get() < LAYOUT) {
                int moved = migrate(mc.options.keyMappings);
                if (moved > 0) {
                    KeyMapping.resetMapping();
                    mc.options.save();
                    tell = true;
                }
                FcsClientConfig.KEY_LAYOUT.set(LAYOUT);
                FcsClientConfig.save();
                Singularity_Iteration.LOGGER.info("Key layout {}: {} bindings moved to their new defaults", LAYOUT, moved);
            }
        }
        if (tell && mc.player != null) {
            tell = false;
            mc.player.displayClientMessage(Component.translatable("message.mio_icif.keys_consolidated"), false);
        }
    }

    /** Moves each binding of {@link #OLD_DEFAULTS} that is still on its old default (without a modifier); returns how many. */
    public static int migrate(KeyMapping[] mappings) {
        int moved = 0;
        for (KeyMapping k : mappings) {
            Integer old = OLD_DEFAULTS.get(k.getName());
            if (old == null) continue;
            if (!k.getKey().equals(InputConstants.Type.KEYSYM.getOrCreate(old)) || k.getKeyModifier() != KeyModifier.NONE) continue;
            k.setKeyModifierAndCode(k.getDefaultKeyModifier(), k.getDefaultKey());
            moved++;
        }
        return moved;
    }
}

// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Key binding of the tactical map (0.1.8.0). The map needs the holomap unit (quantum helmet).
 * 0.1.10.2: with a holomap Mk.II, a tap opens the tactical map and a hold (0.4 s) projects the 3D
 * holo table; without the Mk.II the key opens the map at once, as before.
 */
public final class TacticalMapClient {
    private TacticalMapClient() {}

    public static KeyMapping KEY;
    /** Ticks the key has been held since a press that may become the holo table, -1 when none. */
    private static int held = -1;
    private static final int HOLD_TICKS = 8;
    /** 0.1.11.40: the key opened or closed a screen and is still held; its repeats wait for the release. */
    private static boolean waitRelease;

    /** Called when the holo table closes: a still held map key must not open the map. */
    public static void waitForRelease() {
        waitRelease = com.miophas.singularity_iteration.common.client.armory.HeldKeys.down(KEY);
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        static void keys(RegisterKeyMappingsEvent event) {
            KEY = new KeyMapping("key.mio_icif.tactical_map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.category.mio_icif");
            event.register(KEY);
        }
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        static void tick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (waitRelease && !com.miophas.singularity_iteration.common.client.armory.HeldKeys.down(KEY)) waitRelease = false;
            while (KEY != null && KEY.consumeClick()) {
                if (mc.player == null || mc.screen != null) continue;
                // 0.1.11.40 (U-2): a key repeat is no new press: it neither restarts the hold timer nor reopens what the key closed
                if (held >= 0 || waitRelease) continue;
                if (!FcsState.active(SuitModuleType.HOLOMAP)) {
                    mc.player.displayClientMessage(Component.translatable("gui.mio_icif.tactical_map.need_unit"), true);
                    waitRelease = true;
                    continue;
                }
                if (com.miophas.singularity_iteration.common.client.armory.HoloTable.available()) { held = 0; continue; }
                mc.setScreen(new TacticalMapScreen());
                waitRelease = true;
            }
            // 0.1.10.2: tap = tactical map, hold = holo table
            if (held >= 0) {
                if (mc.player == null || mc.screen != null) { held = -1; return; }
                if (com.miophas.singularity_iteration.common.client.armory.HeldKeys.down(KEY)) {
                    if (++held >= HOLD_TICKS) {
                        held = -1;
                        com.miophas.singularity_iteration.common.client.armory.HoloTable.open();
                    }
                } else {
                    held = -1;
                    mc.setScreen(new TacticalMapScreen());
                }
            }
        }
    }
}

// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.compat.slashblade;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * 0.1.11.233 (SlashBlade collab B1): entry of the optional SlashBlade: Resharped integration of the collab blades. This
 * class names no SlashBlade type; {@link SiSlashBladeServer} and {@link SiSlashBladeClient} do and load only from here,
 * only when SlashBlade is installed. Any failure (an older or newer SlashBlade that lacks a class or member) turns the
 * integration off with one warning; the blades then work as plain SlashBlade named blades.
 */
public final class SiSlashBlade {
    private SiSlashBlade() {}

    public static final String MOD_ID = "slashblade";
    private static volatile boolean active;

    /** True when the integration is on. */
    public static boolean active() { return active; }

    public static void init(IEventBus modEventBus) {
        if (!ModList.get().isLoaded(MOD_ID)) return;
        try {
            SiSlashBladeServer.register(modEventBus);
            if (FMLEnvironment.dist == Dist.CLIENT) SiSlashBladeClient.register();
            active = true;
            Singularity_Iteration.LOGGER.info("SlashBlade collab blades: integration on");
        } catch (Throwable t) {
            Singularity_Iteration.LOGGER.warn("SlashBlade collab blades: integration off ({})", t.toString());
        }
    }
}

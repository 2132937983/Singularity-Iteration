// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.compat.slashblade;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.blade.CollabBlade;
import mods.flammpfeil.slashblade.event.client.RenderOverrideEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 0.1.11.233 (SlashBlade collab B1): the client side of the collab blades on SlashBlade's render hook. SlashBlade draws
 * the {@code effect} object over the sheath while a slash art is charged, with the creeper charge stripes scrolling over
 * it; a collab blade scrolls its own {@code charge.png} instead (nano: graphene honeycomb, quantum: moire fringes,
 * singularity: spiral arms). Loaded only by {@link SiSlashBlade} on the client when SlashBlade is installed.
 */
final class SiSlashBladeClient {
    private SiSlashBladeClient() {}

    static void register() {
        NeoForge.EVENT_BUS.register(SiSlashBladeClient.class);
    }

    /** The charge glow of a collab blade. */
    static ResourceLocation charge(CollabBlade kind) {
        return ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "model/slashblade/" + kind.folder() + "/charge.png");
    }

    @SubscribeEvent
    static void onRender(RenderOverrideEvent event) {
        if (!"effect".equals(event.getTarget())) return;
        CollabBlade kind = CollabBlade.of(event.getStack());
        if (kind != null) event.setTexture(charge(kind));
    }
}

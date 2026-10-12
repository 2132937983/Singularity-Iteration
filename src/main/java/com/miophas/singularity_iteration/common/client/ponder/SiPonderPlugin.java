// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.ponder;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Create "Ponder" support (0.1.8.0). Hold the ponder key on these items to watch the scenes:
 * Armory, Nuclear Reactor Generator (and Reactor Chamber), Blueprint Builder (and Armory
 * Material Interface), Ejector Pipe. The scene schematics are in {@code assets/mio_icif/ponder/};
 * the texts are ASD-STE100 English with translations in the language files
 * ({@code mio_icif.ponder.<scene>.header / text_N}).
 *
 * <p>Only {@code Singularity_Iteration_Client} registers this plugin, and only when the Ponder
 * library (bundled with Create) is loaded.
 */
@OnlyIn(Dist.CLIENT)
public final class SiPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return Singularity_Iteration.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(id("producer/block_armory"))
            .addStoryBoard("armory", SiPonderScenes::armory);
        helper.forComponents(id("generator/block_nuclear_reactor_generator"), id("reactor/block_reactor_chamber"))
            .addStoryBoard("nuclear_reactor", SiPonderScenes::nuclearReactor);
        helper.forComponents(id("producer/block_blueprint_builder"), id("producer/block_material_interface"))
            .addStoryBoard("blueprint_builder", SiPonderScenes::blueprintBuilder);
        helper.forComponents(id("pipe/block_pipe_ejector"))
            .addStoryBoard("ejector_pipe", SiPonderScenes::ejectorPipe);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, path);
    }
}

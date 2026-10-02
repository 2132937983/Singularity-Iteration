package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.generator.UltimatePhotonResonanceSolarGeneratorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Modern (Molecular-Transformer-style) solar GUI with charge and discharge buffer slots. */
@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_ultimate_photon_resonance_solar_generator extends ModernSolarScreen<UltimatePhotonResonanceSolarGeneratorMenu> {
    public mio_icif_gui_ultimate_photon_resonance_solar_generator(UltimatePhotonResonanceSolarGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}

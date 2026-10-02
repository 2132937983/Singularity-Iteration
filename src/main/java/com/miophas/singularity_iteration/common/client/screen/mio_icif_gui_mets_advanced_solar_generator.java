package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.generator.MetsAdvancedSolarGeneratorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Modern (Molecular-Transformer-style) solar GUI with charge and discharge buffer slots. */
@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_mets_advanced_solar_generator extends ModernSolarScreen<MetsAdvancedSolarGeneratorMenu> {
    public mio_icif_gui_mets_advanced_solar_generator(MetsAdvancedSolarGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}

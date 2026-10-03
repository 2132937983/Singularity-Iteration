package com.miophas.singularity_iteration.common.client;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Appearance style switch of the Equipment Console's "Look" tab.
 *
 * <ul>
 *   <li><b>Default</b> - the classic IC2 look the mod ships in {@code assets/}: plain full-cube
 *       machines, and machine items drawn with the very same block model in the inventory.</li>
 *   <li><b>Experimental</b> - the built-in resource pack {@code resourcepacks/si_experimental}:
 *       refined machine models with status lamps and physical fronts, plus the matching flat
 *       DSP inventory icons, so blocks and items always switch together.</li>
 * </ul>
 * The choice is the pack's selection, saved with the vanilla resource-pack list.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class AppearanceStyle {
    private AppearanceStyle() {}

    /** Pack id NeoForge gives {@code resourcepacks/si_experimental} of this mod. */
    public static final String EXPERIMENTAL_PACK = "mod/mio_icif:resourcepacks/si_experimental";

    @SubscribeEvent
    static void packs(AddPackFindersEvent event) {
        event.addPackFinders(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "resourcepacks/si_experimental"),
            PackType.CLIENT_RESOURCES, Component.translatable("pack.mio_icif.experimental"), PackSource.BUILT_IN, false, Pack.Position.TOP);
    }

    public static boolean isExperimental() {
        try {
            return Minecraft.getInstance().getResourcePackRepository().getSelectedIds().contains(EXPERIMENTAL_PACK);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static boolean available() {
        return Minecraft.getInstance().getResourcePackRepository().getAvailableIds().contains(EXPERIMENTAL_PACK);
    }

    /** Selects / deselects the experimental pack and reloads resources (persisted in options.txt). */
    public static void setExperimental(boolean on) {
        Minecraft mc = Minecraft.getInstance();
        PackRepository repo = mc.getResourcePackRepository();
        if (!repo.getAvailableIds().contains(EXPERIMENTAL_PACK)) return;
        List<String> ids = new ArrayList<>(repo.getSelectedIds());
        if (on == ids.contains(EXPERIMENTAL_PACK)) return;
        if (on) ids.add(EXPERIMENTAL_PACK); else ids.remove(EXPERIMENTAL_PACK);
        repo.setSelected(ids);
        mc.options.updateResourcePacks(repo);
    }
}

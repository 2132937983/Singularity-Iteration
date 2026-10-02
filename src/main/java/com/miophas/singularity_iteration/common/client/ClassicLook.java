package com.miophas.singularity_iteration.common.client;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.util.SiVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The "classic texture" appearance switch: a built-in resource pack (resourcepacks/si_classic)
 * that puts back the plain full-cube IC2 machine models and the original inventory icons, drops
 * the status lamps. Toggled
 * from the Equipment Console's appearance tab; the choice persists with the vanilla pack list.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class ClassicLook {
    private ClassicLook() {}

    @SubscribeEvent
    static void packs(AddPackFindersEvent event) {
        event.addPackFinders(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "resourcepacks/si_classic"),
            PackType.CLIENT_RESOURCES, Component.translatable("pack.mio_icif.classic"), PackSource.BUILT_IN, false, Pack.Position.TOP);
    }

    @SubscribeEvent
    static void reload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> refresh());
    }

    /** Re-reads the switch from the selected packs. */
    public static void refresh() {
        try {
            SiVisuals.classic = Minecraft.getInstance().getResourcePackRepository().getSelectedIds().contains(SiVisuals.CLASSIC_PACK);
        } catch (RuntimeException e) {
            SiVisuals.classic = false;
        }
    }

    public static boolean isClassic() { return SiVisuals.classic; }

    public static boolean available() {
        return Minecraft.getInstance().getResourcePackRepository().getAvailableIds().contains(SiVisuals.CLASSIC_PACK);
    }

    /** Selects / deselects the classic pack and reloads resources (persisted in options.txt). */
    public static void setClassic(boolean on) {
        Minecraft mc = Minecraft.getInstance();
        PackRepository repo = mc.getResourcePackRepository();
        if (!repo.getAvailableIds().contains(SiVisuals.CLASSIC_PACK)) return;
        List<String> ids = new ArrayList<>(repo.getSelectedIds());
        boolean has = ids.contains(SiVisuals.CLASSIC_PACK);
        if (on == has) return;
        if (on) ids.add(SiVisuals.CLASSIC_PACK); else ids.remove(SiVisuals.CLASSIC_PACK);
        repo.setSelected(ids);
        SiVisuals.classic = on;
        mc.options.updateResourcePacks(repo);
    }
}

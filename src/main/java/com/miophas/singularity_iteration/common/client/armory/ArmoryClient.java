package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.ArmoryRegistry;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client registrations of the Armory system. */
public final class ArmoryClient {
    private ArmoryClient() {}

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        static void screens(RegisterMenuScreensEvent event) {
            event.register(ArmoryRegistry.ARMORY_MENU.get(), ArmoryScreen::new);
        }

        @SubscribeEvent
        static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ArmoryRegistry.PIECE_ENTITY.get(), ArmoryPieceRenderer::new);
            event.registerEntityRenderer(ArmoryRegistry.WINGMAN.get(), WingmanRenderer::new);
            event.registerBlockEntityRenderer(ArmoryRegistry.SHOWCASE_ENTITY.get(), ArmorShowcaseRenderer::new);
            event.registerBlockEntityRenderer(ArmoryRegistry.ARMORY_ENTITY.get(), ArmoryRenderer::new);     // 0.1.10.32 gantry
        }

        @SubscribeEvent
        static void particles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ArmoryRegistry.THRUST.get(), ArmoryClientEffects.ThrustParticle.Provider::new);
            event.registerSpriteSet(ArmoryRegistry.CORE.get(), ArmoryClientEffects.CoreParticle.Provider::new);
            event.registerSpriteSet(ArmoryRegistry.TRAIL.get(), ArmoryClientEffects.TrailParticle.Provider::new);
        }

        @SubscribeEvent
        static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
            com.miophas.singularity_iteration.common.armory.ArmoryPieceEntity.CLIENT_EFFECTS = new ArmoryClientEffects();
            com.miophas.singularity_iteration.common.armory.WingmanDeployFxPacket.HOOK = WingmanDeployFx::onPacket;
            // using the remote opens the Equipment Console on its Armory section
            com.miophas.singularity_iteration.common.armory.ArmoryRemoteItem.OPEN_CONSOLE = () -> net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features().tab(
                    com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features.TAB_ARMORY));
        }
    }
}

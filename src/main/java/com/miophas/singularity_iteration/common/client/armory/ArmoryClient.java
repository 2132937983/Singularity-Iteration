package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.ArmoryComponents;
import com.miophas.singularity_iteration.common.armory.ArmoryLink;
import com.miophas.singularity_iteration.common.armory.ArmoryRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Client registrations of the Armory system. */
public final class ArmoryClient {
    private ArmoryClient() {}

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        static void screens(RegisterMenuScreensEvent event) {
            event.register(ArmoryRegistry.ARMORY_MENU.get(), ArmoryScreen::new);
            event.register(ArmoryRegistry.REMOTE_MENU.get(), ArmoryRemoteScreen::new);
        }

        @SubscribeEvent
        static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ArmoryRegistry.PIECE_ENTITY.get(), ArmoryPieceRenderer::new);
            event.registerBlockEntityRenderer(ArmoryRegistry.SHOWCASE_ENTITY.get(), ArmorShowcaseRenderer::new);
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
        }
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        /** Equipment fitted with a Connector Kit shows its Armory link. */
        @SubscribeEvent
        static void tooltip(ItemTooltipEvent event) {
            ArmoryLink link = event.getItemStack().get(ArmoryComponents.LINK.get());
            if (link == null) return;
            event.getToolTip().add(Component.translatable("item.mio_icif.armory_link",
                Component.translatable("gui.mio_icif.armory.piece." + link.pieceOrDefault().name().toLowerCase(java.util.Locale.ROOT)))
                .withStyle(ChatFormatting.AQUA));
        }
    }
}

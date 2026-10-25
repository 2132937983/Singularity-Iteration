package com.miophas.singularity_iteration.common.client.lathe;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.item.lathe.LatheProfileTooltip;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Turning blanks on the client: the item is drawn by its current profile (IC2 LatheRenderer; the
 * item models use builtin/entity) and its tooltip shows the profile.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class LatheBlankClient {
    private LatheBlankClient() {}

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private LatheProfileRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new LatheProfileRenderer();
                return renderer;
            }
        }, mio_icif_normal.IRON_TURNING_BLANKS.get(), mio_icif_normal.WOODEN_TURNING_BLANKS.get());
    }

    @SubscribeEvent
    static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(LatheProfileTooltip.class, ClientLatheProfileTooltip::new);
    }
}

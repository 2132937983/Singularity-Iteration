package com.miophas.singularity_iteration.common.event;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_toolbox;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 0.1.11.170: a toolbox used up as an ingredient (the industrial workbench, the metal former) gives its contents back to the
 * player who crafted, instead of taking them with it. Making the advanced toolbox from a toolbox keeps them in the new box
 * ({@code carry_contents}), so nothing is given back then. Runs before the grid is used up.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class ToolboxContentsGuard {
    private ToolboxContentsGuard() {}

    @SubscribeEvent
    static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || event.getCrafting().getItem() instanceof mio_icif_toolbox) return;
        Container grid = event.getInventory();
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack stack = grid.getItem(i);
            if (!(stack.getItem() instanceof mio_icif_toolbox)) continue;
            for (ItemStack stored : mio_icif_toolbox.contents(stack, player.registryAccess())) {
                if (!player.getInventory().add(stored)) player.drop(stored, false);
            }
            stack.remove(DataComponents.CUSTOM_DATA);
        }
    }
}

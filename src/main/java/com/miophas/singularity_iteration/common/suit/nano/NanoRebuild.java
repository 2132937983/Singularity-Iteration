// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.nano;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.SuitRegistry;
import com.miophas.singularity_iteration.common.suit.UnitRef;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Nano to quantum rebuild (0.1.8.6). A quantum piece crafted from a nano piece keeps the shared
 * units of the nano piece (at their tier) when they fit; nano-only units, units that do not fit and
 * the lattice densifiers come back as items. Nothing is lost.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class NanoRebuild {
    private NanoRebuild() {}

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.getCrafting();
        if (event.getEntity().level().isClientSide || !SuitModules.isQuantumPiece(result)) return;
        Container grid = event.getInventory();
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack in = grid.getItem(i);
            if (!SuitModules.isNanoPiece(in)) continue;
            if (SuitModules.installed(in).ids().isEmpty() && SuitModules.latticeLevel(in) == 0) continue;
            Player player = event.getEntity();
            transfer(in, result, stack -> {
                if (!player.getInventory().add(stack)) player.drop(stack, false);
            });
            break;
        }
    }

    /**
     * Moves the units of {@code nano} onto {@code quantum} or hands them back.
     *
     * @return the items handed back (also passed to {@code giveBack})
     */
    public static List<ItemStack> transfer(ItemStack nano, ItemStack quantum, Consumer<ItemStack> giveBack) {
        List<ItemStack> back = new ArrayList<>();
        for (UnitRef unit : SuitModules.installed(nano).units()) {
            int tier = Math.max(1, SuitModules.tier(nano, unit.key()));
            if (SuitModules.install(quantum, unit, tier)) continue;
            ItemStack item = unit.unitStack(tier);
            if (!item.isEmpty()) back.add(item);
        }
        int densifiers = SuitModules.latticeLevel(nano);
        if (densifiers > 0) back.add(new ItemStack(SuitRegistry.LATTICE_DENSIFIER.get(), densifiers));
        for (ItemStack stack : back) giveBack.accept(stack.copy());
        return back;
    }
}

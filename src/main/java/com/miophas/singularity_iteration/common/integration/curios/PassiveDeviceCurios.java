// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.integration.curios;

import com.miophas.singularity_iteration.common.item.tools.PassiveDeviceRules;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_items_tools;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * 0.1.11.201 (tool overhaul T5d, decision D7): the nutrition supply (belt), the first-aid life support (charm) and the force
 * field generator (necklace) are accessories when Curios is installed. They work from their accessory slot only; in the
 * plain inventory they did their work from any slot. Loaded only with Curios.
 */
public final class PassiveDeviceCurios implements ICurioItem {
    public static void register(IEventBus bus) {
        bus.addListener(PassiveDeviceCurios::onCommonSetup);
        PassiveDeviceRules.wornCopies = PassiveDeviceCurios::worn;
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            PassiveDeviceCurios adapter = new PassiveDeviceCurios();
            CuriosApi.registerCurio(mio_icif_items_tools.ELECTRIC_NUTRITION_SUPPLY.get(), adapter);
            CuriosApi.registerCurio(mio_icif_items_tools.ELECTRIC_FIRST_AID_LIFE_SUPPORT.get(), adapter);
            CuriosApi.registerCurio(mio_icif_items_tools.ELECTRIC_FORCE_FIELD_GENERATOR.get(), adapter);
        });
    }

    /** The player's functional (not cosmetic) accessory stacks, slot by slot. */
    private static List<ItemStack> worn(Player player) {
        List<ItemStack> out = new ArrayList<>();
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> inventory.getCurios().values().forEach(handler -> {
            var stacks = handler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) out.add(stacks.getStackInSlot(i));
        }));
        return out;
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (context.cosmetic() || !(context.entity() instanceof Player player)) return;
        if (stack.getItem() instanceof PassiveDeviceRules.WornDevice device) device.tickWorn(stack, player);
    }
}

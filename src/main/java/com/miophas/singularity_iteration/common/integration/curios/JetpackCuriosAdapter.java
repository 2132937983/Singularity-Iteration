// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.integration.curios;

import com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors;
import com.miophas.singularity_iteration.core.api.item.IBackSlotItem;
import com.miophas.singularity_iteration.core.api.item.IJetpackItem;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Optional Curios bridge for the two SI jetpacks. */
public final class JetpackCuriosAdapter implements ICurioItem {
    public static void register(IEventBus bus) {
        com.miophas.singularity_iteration.core.api.item.BatteryTargetProviders.register(JetpackCuriosAdapter::appendFunctionalBatteryTargets);
        bus.addListener(JetpackCuriosAdapter::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            JetpackCuriosAdapter adapter = new JetpackCuriosAdapter();
            CuriosApi.registerCurio(mio_icif_items_armors.ARMOR_JETPACK_ELECTRIC.get(), adapter);
            CuriosApi.registerCurio(mio_icif_items_armors.ARMOR_ADVANCED_JETPACK.get(), adapter);
        });
    }

    @Override
    public boolean canEquip(SlotContext context, ItemStack stack) {
        return "back".equals(context.identifier()) && stack.getItem() instanceof IJetpackItem;
    }

    /** Reads functional stacks only; cosmetic copies must never receive mode writes. */
    public static ItemStack findModeTarget(Player player) {
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return ItemStack.EMPTY;
        var handler = inventory.getStacksHandler("back").orElse(null);
        if (handler == null) return ItemStack.EMPTY;
        // findFirstCurio may include a cosmetic mirror depending on Curios' slot
        // implementation. Read the functional handler directly so mode writes can
        // never target a cosmetic-only stack.
        var stacks = handler.getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);
            if (stack.getCount() == 1 && stack.getItem() instanceof IBackSlotItem
                    && stack.getItem() instanceof IJetpackItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** Append functional Curios stacks that can receive a battery transfer. */
    public static void appendFunctionalBatteryTargets(Player player, List<ItemStack> targets, ItemStack excluded) {
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return;
        for (var handler : inventory.getCurios().values()) {
            var stacks = handler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack.isEmpty() || stack == excluded || stack.getCount() != 1) continue;
                if (stack.getItem() instanceof IBatteryItem battery
                        && battery.getEnergy(stack) < battery.getMaxEnergy(stack)) targets.add(stack);
            }
        }
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (context.cosmetic() || !(context.entity() instanceof Player player)) return;
        if (stack.getItem() instanceof IBackSlotItem backSlotItem) {
            backSlotItem.tickInBackSlot(player, stack);
        }
    }
}

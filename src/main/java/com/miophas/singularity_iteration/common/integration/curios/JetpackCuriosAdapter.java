// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.integration.curios;

import com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors;
import com.miophas.singularity_iteration.core.api.item.IBackSlotItem;
import com.miophas.singularity_iteration.core.api.item.IJetpackItem;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.item.IElectricArmorItem;
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

    /** 背槽中第一个可管理装备特性的物品堆下标；无则 -1。 */
    private static int backFeatureIndex(Player player) {
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return -1;
        var handler = inventory.getStacksHandler("back").orElse(null);
        if (handler == null) return -1;
        var stacks = handler.getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);
            if (stack.getCount() == 1 && stack.getItem() instanceof IElectricArmorItem) return i;
        }
        return -1;
    }

    /** 读取背槽中可管理装备特性的物品堆；无则返回空。 */
    public static ItemStack backFeatureStack(Player player) {
        int index = backFeatureIndex(player);
        if (index < 0) return ItemStack.EMPTY;
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return ItemStack.EMPTY;
        var handler = inventory.getStacksHandler("back").orElse(null);
        if (handler == null) return ItemStack.EMPTY;
        return handler.getStacks().getStackInSlot(index);
    }

    /** 把修改后的物品堆写回背槽，触发 Curios 同步；成功返回 true。 */
    public static boolean writeBackFeatureStack(Player player, ItemStack stack) {
        int index = backFeatureIndex(player);
        if (index < 0 || stack == null) return false;
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return false;
        var handler = inventory.getStacksHandler("back").orElse(null);
        if (handler == null) return false;
        handler.getStacks().setStackInSlot(index, stack);
        return true;
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (context.cosmetic() || !(context.entity() instanceof Player player)) return;
        if (stack.getItem() instanceof IBackSlotItem backSlotItem) {
            backSlotItem.tickInBackSlot(player, stack);
        }
    }

    // ---- equipment console: every functional accessory with settings (tuning sliders, modes, charge)
    private static boolean manageable(ItemStack stack) {
        return stack.getCount() == 1 && (stack.getItem() instanceof com.miophas.singularity_iteration.common.item.tuning.ITunableItem
            || stack.getItem() instanceof com.miophas.singularity_iteration.core.api.tool.IToolModeProvider
            || stack.getItem() instanceof IBatteryItem);
    }

    /** Accessory slots for the console as {id, stack}; ids are {@code curio:<slotType>:<index>}. The back-slot armor is listed separately. */
    public static List<Object[]> accessorySlots(Player player) {
        List<Object[]> out = new java.util.ArrayList<>();
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return out;
        for (var entry : inventory.getCurios().entrySet()) {
            var stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack.isEmpty() || !manageable(stack)) continue;
                if ("back".equals(entry.getKey()) && stack.getItem() instanceof IElectricArmorItem) continue;
                out.add(new Object[]{"curio:" + entry.getKey() + ":" + i, stack});
            }
        }
        return out;
    }

    public static ItemStack accessoryStack(Player player, String id) {
        String[] parts = id.split(":");
        if (parts.length != 3) return ItemStack.EMPTY;
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return ItemStack.EMPTY;
        var handler = inventory.getStacksHandler(parts[1]).orElse(null);
        if (handler == null) return ItemStack.EMPTY;
        int index;
        try { index = Integer.parseInt(parts[2]); } catch (NumberFormatException e) { return ItemStack.EMPTY; }
        var stacks = handler.getStacks();
        return index >= 0 && index < stacks.getSlots() ? stacks.getStackInSlot(index) : ItemStack.EMPTY;
    }

    public static boolean writeAccessory(Player player, String id, ItemStack stack) {
        String[] parts = id.split(":");
        if (parts.length != 3) return false;
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return false;
        var handler = inventory.getStacksHandler(parts[1]).orElse(null);
        if (handler == null) return false;
        int index;
        try { index = Integer.parseInt(parts[2]); } catch (NumberFormatException e) { return false; }
        if (index < 0 || index >= handler.getStacks().getSlots()) return false;
        handler.getStacks().setStackInSlot(index, stack);
        return true;
    }
}

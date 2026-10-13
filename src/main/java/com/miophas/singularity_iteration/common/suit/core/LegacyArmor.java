// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.core;

import com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitRegistry;
import com.miophas.singularity_iteration.core.platform.neoforge.CoreDataComponents;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Retired equipment (0.1.9.0): the solar helmets, the advanced quantum chestplate and the heavy
 * quantum chestplate. Their ids stay registered as hidden "legacy shells" so that old worlds load
 * them without loss. A shell converts into the standard suit piece with the matching frame core:
 * at once when a player carries or wears it, in the Universal Suit Modification Station, on a
 * Wingman, and with a one-item crafting recipe for shells in storage.
 *
 * <table>
 *   <caption>Conversion</caption>
 *   <tr><th>Legacy item</th><th>Becomes</th></tr>
 *   <tr><td>solar helmet</td><td>a solar skin unit item</td></tr>
 *   <tr><td>advanced solar helmet</td><td>nano helmet + Solar Array I</td></tr>
 *   <tr><td>hybrid / ultimate solar helmet</td><td>quantum helmet + Solar Array II / III</td></tr>
 *   <tr><td>heavy quantum chestplate</td><td>quantum chestplate + Graviton Bulwark core</td></tr>
 *   <tr><td>advanced quantum chestplate</td><td>quantum chestplate + Advanced Propulsion core</td></tr>
 * </table>
 * The converted piece keeps the stored energy, the installed units and their tiers, the switches,
 * the palette, the enchantments and the name (the item components move over).
 */
public final class LegacyArmor {
    private LegacyArmor() {}

    /** Target of one legacy item: the new piece (or unit item) and the core to install. */
    public record Target(Supplier<? extends Item> item, @Nullable FrameCore core) { }

    /** The shell item of a legacy id. */
    public static final class Shell extends ArmorItem {
        private final Supplier<Target> target;

        public Shell(Holder<ArmorMaterial> material, Type type, Properties properties, Supplier<Target> target) {
            super(material, type, properties.stacksTo(1));
            this.target = target;
        }

        public Target target() { return target.get(); }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
            if (!level.isClientSide && entity instanceof Player player) convertInInventory(player, stack);
        }

        /**
         * The worn texture of the retired piece (a shell on an armour stand keeps its look; players
         * convert it at once): {@code textures/models/armor/<name>.png}, name from the item id.
         */
        @Override
        public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
            String path = BuiltInRegistries.ITEM.getKey(this).getPath();
            String name = path.substring(path.lastIndexOf('/') + 1).replaceFirst("^item_(armor_)?", "");
            return ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/models/armor/" + name + ".png");
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("tooltip.mio_icif.legacy_armor").withStyle(ChatFormatting.GOLD));
            Target t = target();
            Component into = t.core() == null ? Component.translatable(t.item().get().getDescriptionId())
                : Component.translatable("tooltip.mio_icif.legacy_armor.into", Component.translatable(t.item().get().getDescriptionId()),
                    Component.translatable(t.core().nameKey()));
            tooltip.add(Component.translatable("tooltip.mio_icif.legacy_armor.becomes", into).withStyle(ChatFormatting.GRAY));
        }
    }

    public static boolean isLegacy(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof Shell;
    }

    /** Result of a conversion: the new stack and the items handed back (none today; kept for later shells). */
    public record Result(ItemStack stack, List<ItemStack> extras) { }

    /** Converts a legacy stack; returns null for any other item. */
    @Nullable
    public static Result convert(ItemStack legacy) {
        if (!(legacy.getItem() instanceof Shell shell)) return null;
        Target target = shell.target();
        Item item = target.item().get();
        List<ItemStack> extras = new ArrayList<>();
        if (target.core() == null) {
            // not a suit piece (the basic solar helmet): the plain item
            return new Result(new ItemStack(item), extras);
        }
        var stored = legacy.get(CoreDataComponents.BATTERY_ENERGY.get());
        long energy = stored == null ? 0 : stored.energy();
        ItemStack converted = legacy.transmuteCopy(item, 1);
        FrameCores.install(converted, target.core());
        if (converted.getItem() instanceof AbstractElectricArmor armor) armor.setEnergy(converted, energy);
        return new Result(converted, extras);
    }

    /** Replaces a legacy stack in the player's inventory (main, armour or off hand) by its conversion. */
    static void convertInInventory(Player player, ItemStack legacy) {
        var inv = player.getInventory();
        for (NonNullList<ItemStack> list : List.of(inv.items, inv.armor, inv.offhand)) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) != legacy) continue;
                Result result = convert(legacy);
                if (result == null) return;
                list.set(i, result.stack());
                for (ItemStack extra : result.extras()) if (!inv.add(extra)) player.drop(extra, false);
                inv.setChanged();
                return;
            }
        }
    }

    // ---------------------------------------------------------------- targets (used by the registrations)

    public static Target solarSkin() {
        return new Target(() -> SuitRegistry.unitItem(SuitModuleType.SOLAR_SKIN), null);
    }

    public static Target nanoHelmet(FrameCore core) {
        return new Target(mio_icif_items_armors.ARMOR_NANO_HELMET, core);
    }

    public static Target quantumHelmet(FrameCore core) {
        return new Target(mio_icif_items_armors.ARMOR_QUANTUM_HELMET, core);
    }

    public static Target quantumChest(FrameCore core) {
        return new Target(mio_icif_items_armors.ARMOR_QUANTUM_CHESTPLATE, core);
    }
}

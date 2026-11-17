package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Which items the Armory can store and summon, and where they dock.
 *
 * <p>0.1.11.49 (escort and open Armory phase F0, decision one A): the Armory takes the items of every mod; the Connector
 * Kit is gone. Only the items in the item tag {@link #BLACKLIST} ({@code mio_icif:armory_blacklist}, empty by default, for
 * modpacks) stay out. Armour positions take the items whose body slot matches ({@link Equipable} or the item's own
 * equipment slot); the hands take anything that is not body armour.
 *
 * <p>Decision two A: an item linked by a Connector Kit before 0.1.11.49 keeps its {@link ArmoryLink} component (still
 * registered, read only, no longer shown or written); the slot it names still counts, so a linked foreign item without
 * an equipment slot stays where it was stored.
 */
public final class ArmoryRules {
    private ArmoryRules() {}

    /** Items the Armory does not take (empty by default). */
    public static final TagKey<Item> BLACKLIST =
        TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armory_blacklist"));

    /** True when the item may be stored in an Armory and flown to its owner. */
    public static boolean isSummonable(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(BLACKLIST);
    }

    /** The body slot an item naturally goes to, or null for hand-held items. */
    @Nullable
    public static EquipmentSlot naturalSlot(ItemStack stack) {
        ArmoryLink legacy = stack.get(ArmoryComponents.LINK.get());
        if (legacy != null) return legacy.pieceOrDefault().slot;
        Equipable equipable = Equipable.get(stack);
        if (equipable != null) return equipable.getEquipmentSlot();
        return stack.getEquipmentSlot();
    }

    /** True when the item may sit in the given suit position. */
    public static boolean fits(ItemStack stack, ArmoryPiece piece) {
        if (!isSummonable(stack)) return false;
        EquipmentSlot natural = naturalSlot(stack);
        if (piece.isArmor()) return natural == piece.slot;
        // hands take anything that is not body armour (weapons, tools, shields, totems...)
        return natural == null || natural.getType() == EquipmentSlot.Type.HAND || natural == piece.slot;
    }
}

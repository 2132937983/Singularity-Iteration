package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Which items the Armory can store and summon, and where they dock.
 *
 * <p>Vanilla and Singularity Iteration equipment is supported natively. Equipment from
 * other mods must first be fitted with a Connector Kit ({@link ArmoryLink}); the link also
 * names the body slot, because foreign items do not always declare one.
 */
public final class ArmoryRules {
    private ArmoryRules() {}

    public static boolean isNative(ItemStack stack) {
        String ns = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        return "minecraft".equals(ns) || Singularity_Iteration.MOD_ID.equals(ns);
    }

    public static boolean isLinked(ItemStack stack) {
        return stack.has(ArmoryComponents.LINK.get());
    }

    /** True when the item may be stored in an Armory and flown to its owner. */
    public static boolean isSummonable(ItemStack stack) {
        return !stack.isEmpty() && (isNative(stack) || isLinked(stack));
    }

    /** The body slot an item naturally goes to, or null for hand-held items. */
    @Nullable
    public static EquipmentSlot naturalSlot(ItemStack stack) {
        ArmoryLink link = stack.get(ArmoryComponents.LINK.get());
        if (link != null) return link.pieceOrDefault().slot;
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

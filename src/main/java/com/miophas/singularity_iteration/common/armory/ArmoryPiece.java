package com.miophas.singularity_iteration.common.armory;

import net.minecraft.world.entity.EquipmentSlot;

/**
 * The six pieces of an Armory suit, in the order they fly in (boots first, weapons last),
 * with where on the body each piece docks (fraction of the player's height, sideways
 * offset in blocks: + = player's right).
 */
public enum ArmoryPiece {
    FEET(EquipmentSlot.FEET, 0.10, 0.0),
    LEGS(EquipmentSlot.LEGS, 0.45, 0.0),
    CHEST(EquipmentSlot.CHEST, 0.70, 0.0),
    HEAD(EquipmentSlot.HEAD, 0.90, 0.0),
    OFFHAND(EquipmentSlot.OFFHAND, 0.50, -0.38),
    MAINHAND(EquipmentSlot.MAINHAND, 0.50, 0.38);

    public static final ArmoryPiece[] VALUES = values();
    /** GUI column order: helmet, chestplate, leggings, boots, main hand, off hand. */
    public static final ArmoryPiece[] COLUMNS = {HEAD, CHEST, LEGS, FEET, MAINHAND, OFFHAND};

    public final EquipmentSlot slot;
    public final double heightFraction;
    public final double side;

    ArmoryPiece(EquipmentSlot slot, double heightFraction, double side) {
        this.slot = slot;
        this.heightFraction = heightFraction;
        this.side = side;
    }

    public boolean isArmor() {
        return slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
    }

    public int column() {
        for (int i = 0; i < COLUMNS.length; i++) if (COLUMNS[i] == this) return i;
        return 0;
    }

    public static ArmoryPiece of(EquipmentSlot slot) {
        for (ArmoryPiece p : VALUES) if (p.slot == slot) return p;
        return MAINHAND;
    }

    public static ArmoryPiece byName(String name) {
        for (ArmoryPiece p : VALUES) if (p.name().equalsIgnoreCase(name)) return p;
        return null;
    }
}

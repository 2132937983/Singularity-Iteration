package com.miophas.singularity_iteration.core.api.energy;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

/** Optional electrical color. Null (including the default black cable) connects to every color. */
public interface IColoredEnergyTile {
    @Nullable DyeColor getEnergyColor(Direction side);

    static boolean connects(Object first, Object second, Direction side) {
        var a = first instanceof IColoredEnergyTile tile ? tile.getEnergyColor(side) : null;
        var b = second instanceof IColoredEnergyTile tile ? tile.getEnergyColor(side.getOpposite()) : null;
        return a == null || b == null || a == b;
    }
}

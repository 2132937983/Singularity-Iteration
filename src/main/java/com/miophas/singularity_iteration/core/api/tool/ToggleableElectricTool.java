package com.miophas.singularity_iteration.core.api.tool;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for electric tools that you can switch on and off.
 * The player can toggle an electric tool that implements this interface with the G key.
 * Examples: the nano saber and the plasma ray gun.
 */
public interface ToggleableElectricTool {

    /**
     * Toggles the on/off state of the tool.
     * @param stack the tool item stack
     * @param player the player
     */
    void toggleActive(ItemStack stack, Player player);
}

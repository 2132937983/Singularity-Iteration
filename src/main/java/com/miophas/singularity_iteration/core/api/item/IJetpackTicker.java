package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Tick interface for jetpacks.
 * <p>When Curios is installed, the Curios adapter uses it to run the flight logic.
 * Thus the jetpack class itself has no dependency on the Curios API.
 */
public interface IJetpackTicker {

    /**
     * Runs the jetpack flight logic for one tick.
     *
     * @param player the player who wears the jetpack
     * @param stack  the jetpack item stack
     * @param level  the world
     */
    void tickJetpack(Player player, ItemStack stack, Level level);
}

package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Interface for enhanced overlay providers.
 * <p>
 * It is the equivalent of {@code IEnhancedOverlayProvider} in IC2 1.12.2.
 * When a player holds an item that implements this interface, the item can give an enhanced overlay for a block.
 * The overlay shows more information or an interaction interface.
 */
public interface IEnhancedOverlayProvider {

    /**
     * Returns true if this item gives an enhanced overlay for the given block.
     *
     * @param world the world
     * @param pos the block position
     * @param side the face that the player looks at
     * @param player the player
     * @param stack the held item
     * @return true if the item gives an enhanced overlay
     */
    boolean providesEnhancedOverlay(Level world, BlockPos pos, Direction side, Player player, ItemStack stack);
}

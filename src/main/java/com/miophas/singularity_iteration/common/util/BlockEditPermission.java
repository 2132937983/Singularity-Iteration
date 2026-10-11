// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side permission checks for item actions that change or remove blocks (0.1.7.36).
 *
 * <p>Why: NeoForge fires {@code RightClickBlock} before vanilla applies the spectator and
 * adventure-mode rules, and a flood fill or an area tool changes blocks that the player never
 * clicked. An item that edits blocks itself must therefore ask the same questions that vanilla
 * asks for a normal block placement or break. Protection and claim mods hook those questions.
 *
 * <p>Contract: call on the logical server only (on a client level the result is always false).
 * A false result means "do nothing at this position": the caller must not change the block.
 */
public final class BlockEditPermission {
    private BlockEditPermission() {}

    /** May {@code player} change the block at {@code pos} with {@code tool}? (rotate, configure, place) */
    public static boolean canEdit(Player player, Level level, BlockPos pos, @Nullable Direction face, ItemStack tool) {
        if (!(level instanceof ServerLevel) || player.isSpectator()) return false;
        if (!level.isLoaded(pos) || !level.getWorldBorder().isWithinBounds(pos)) return false;
        if (!player.mayUseItemAt(pos, face == null ? Direction.UP : face, tool)) return false;
        if (!level.mayInteract(player, pos)) return false;
        // Adventure-mode rules need the game mode, which only a ServerPlayer carries. A player
        // entity that is not a ServerPlayer (machine or test players) passes the checks above.
        return !(player instanceof ServerPlayer sp) || !sp.blockActionRestricted(level, pos, sp.gameMode.getGameModeForPlayer());
    }

    /**
     * May {@code player} remove the block at {@code pos}? Runs {@link #canEdit} and then posts a
     * {@link BlockEvent.BreakEvent}, as a normal break does. The event can be cancelled by any mod.
     */
    public static boolean canBreak(Player player, Level level, BlockPos pos, BlockState state, @Nullable Direction face, ItemStack tool) {
        if (!canEdit(player, level, pos, face, tool)) return false;
        return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled();
    }
}

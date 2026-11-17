// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.57 (gaze and Iron Man phase P5, decisions A / A): one piece called on its own, like the gauntlet and the helmet
 * that fly ahead of the Mark 42. The piece key (default V) opens a small wheel of the six positions; the choice is sent
 * as the console action {@link ArmoryRemoteService#PIECE_CALL}. No command link is needed, only the paired remote.
 *
 * <p>The piece comes from the remote's Armory when one of its suits that is not out on a sortie has it there (the first
 * such suit): it flies as in a summon, at a summon's cost for one piece, and the piece worn there flies home. Without such
 * a suit (or without a paired remote) the nearest Wingman with that piece hands it over: it comes to the owner and the
 * two swap that one piece (the suit swap of 0.1.11.54 for one position).
 */
public final class ArmoryPieceCall {
    private ArmoryPieceCall() {}

    /** Farthest a Wingman may be to hand a piece over, blocks. */
    static final double WINGMAN_RANGE = 64;

    public static Component call(ServerPlayer player, @Nullable mio_icif_armory armory, int index) {
        if (index < 0 || index >= ArmoryPiece.VALUES.length) return Component.translatable("message.mio_icif.armory.failed");
        ArmoryPiece piece = ArmoryPiece.VALUES[index];
        Component name = Component.translatable("gui.mio_icif.armory.piece." + piece.name().toLowerCase(java.util.Locale.ROOT));
        ItemStack worn = player.getItemBySlot(piece.slot);
        if (!worn.isEmpty() && !player.isCreative() && net.minecraft.world.item.enchantment.EnchantmentHelper.has(worn,
                net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE))
            return Component.translatable("message.mio_icif.armory.piece_bound", name);
        if (armory != null && armory.isOwner(player)) {
            for (int set = 0; set < mio_icif_armory.SETS; set++) {
                if (armory.deployed(set) || armory.piece(set, piece).isEmpty()) continue;
                var result = armory.summon(player, set, piece);
                return result.ok() ? Component.translatable("message.mio_icif.armory.piece_called", name, armory.setName(set)) : result.message();
            }
        }
        WingmanEntity best = null;
        double bestD = WINGMAN_RANGE * WINGMAN_RANGE;
        for (WingmanEntity w : WingmanService.of(player)) {
            if (w.level() != player.level() || w.state() != WingmanEntity.ACTIVE || w.rescuing() || w.swapPhase() != WingmanEntity.SWAP_NONE) continue;
            if (w.getItemBySlot(piece.slot).isEmpty()) continue;
            double d = w.distanceToSqr(player);
            if (d < bestD) { bestD = d; best = w; }
        }
        if (best == null) return Component.translatable("message.mio_icif.armory.piece_none", name);
        String refused = best.suitUp(player, piece);
        return refused != null ? Component.translatable(refused, best.getName())
            : Component.translatable("message.mio_icif.wingman.piece_handover", best.getName(), name);
    }
}

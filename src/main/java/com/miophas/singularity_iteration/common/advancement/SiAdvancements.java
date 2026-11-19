// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.advancement;

import com.miophas.singularity_iteration.core.api.advancement.MioAchievements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 0.1.11.79: grants the code-driven advancements of the tree (docs/advancement_tree_plan.md) to a player. Each criterion
 * is a {@code minecraft:impossible} criterion of the advancement data; a client player or a fake player is ignored.
 * Events without a player use {@link MioAchievements#at} (players within 16 blocks).
 */
public final class SiAdvancements {
    private SiAdvancements() {}

    /** Grants {@code criterion} of {@code mio_icif:<path>} to a real server player (no-op otherwise). */
    public static void grant(Player player, String path, String criterion) {
        if (player instanceof ServerPlayer server && !(player instanceof net.neoforged.neoforge.common.util.FakePlayer)) {
            MioAchievements.grant(server, path, criterion);
        }
    }
}

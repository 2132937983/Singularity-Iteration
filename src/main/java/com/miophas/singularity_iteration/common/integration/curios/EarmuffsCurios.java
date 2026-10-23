// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.integration.curios;

import com.miophas.singularity_iteration.common.item.armor.mio_icif_soundproof_earmuffs;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

/** Optional Curios bridge for the Soundproof Earmuffs (head slot, tag {@code curios:head}). Loaded only with Curios. */
public final class EarmuffsCurios {
    private EarmuffsCurios() {}

    public static boolean isWorn(Player player) {
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return false;
        var handler = inventory.getStacksHandler("head").orElse(null);
        if (handler == null) return false;
        var stacks = handler.getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            if (stacks.getStackInSlot(i).getItem() instanceof mio_icif_soundproof_earmuffs) return true;
        }
        return false;
    }
}

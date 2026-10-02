// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Smelting experience banked inside an electric furnace. Collecting it adds the points straight to
 * the player's experience bar (no orbs are spawned); the fractional remainder stays banked.
 */
public final class StoredExperience {
    private float xp;

    public void add(float amount) {
        if (amount > 0 && Float.isFinite(amount)) xp = Math.min(1_000_000F, xp + amount);
    }

    public float get() { return xp; }

    /** Gives the whole points to {@code player}; returns how many were given. */
    public int collect(ServerPlayer player) {
        int whole = (int) Math.floor(xp);
        if (whole <= 0) return 0;
        xp -= whole;
        player.giveExperiencePoints(whole);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
            SoundSource.PLAYERS, 0.35F, 0.9F + player.getRandom().nextFloat() * 0.3F);
        return whole;
    }

    public void save(CompoundTag tag) { tag.putFloat("StoredXp", xp); }

    public boolean load(CompoundTag tag) {
        if (!tag.contains("StoredXp")) return false;
        xp = Math.max(0, tag.getFloat("StoredXp"));
        return true;
    }
}

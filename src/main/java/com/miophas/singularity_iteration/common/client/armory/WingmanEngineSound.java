// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * 0.1.11.65 (hit feel and constructivist FX phase Q4): the engine of a flying Wingman, a looping turbine drone at the suit
 * that rises with its speed (louder and higher; a boost kicks it up), fades out on the ground and stops when the Wingman is
 * gone. One loop per Wingman within {@link #RANGE} blocks; client option {@code wingman_engine_sound}. Before, a Wingman
 * flew in silence between its boost and lock-on chirps.
 */
public final class WingmanEngineSound extends AbstractTickableSoundInstance {
    /** Blocks within which a Wingman's engine plays. */
    static final double RANGE = 48;
    private static final Int2ObjectOpenHashMap<WingmanEngineSound> PLAYING = new Int2ObjectOpenHashMap<>();

    private final WingmanEntity wingman;
    private float level;
    private int boosts = Integer.MIN_VALUE, boostAge = 100;

    private WingmanEngineSound(WingmanEntity wingman) {
        super(mio_icif_sounds.ARMORY_TURBINE_LOOP.get(), SoundSource.NEUTRAL, RandomSource.create());
        this.wingman = wingman;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F;
        this.pitch = 0.8F;
        this.x = wingman.getX();
        this.y = wingman.getY() + 0.5;
        this.z = wingman.getZ();
    }

    @Override
    public void tick() {
        if (wingman.isRemoved() || !wingman.isAlive() || !FcsClientConfig.wingmanEngineSound()) {
            stop();
            return;
        }
        x = wingman.getX();
        y = wingman.getY() + 0.5;
        z = wingman.getZ();
        if (boosts != Integer.MIN_VALUE && wingman.boostCount() != boosts) boostAge = 0;
        boosts = wingman.boostCount();
        boostAge++;
        float[] want = com.miophas.singularity_iteration.common.armory.WingmanPose.engineMix((float) wingman.getDeltaMovement().length(), boostAge, wingman.landed());
        level += (want[0] - level) * 0.25F;
        volume = Math.max(0.001F, level);
        pitch += (want[1] - pitch) * 0.3F;
        if (want[0] == 0 && level < 0.01F) stop();   // on the ground: silent until it takes off again
    }

    /** Client tick: starts the engine of each flying Wingman near the player that has none. */
    public static void tick(Minecraft mc) {
        PLAYING.int2ObjectEntrySet().removeIf(e -> e.getValue().isStopped());
        if (mc.level == null || mc.player == null || !FcsClientConfig.wingmanEngineSound()) return;
        for (WingmanEntity w : mc.level.getEntitiesOfClass(WingmanEntity.class, mc.player.getBoundingBox().inflate(RANGE))) {
            if (w.landed() || PLAYING.containsKey(w.getId())) continue;
            WingmanEngineSound sound = new WingmanEngineSound(w);
            PLAYING.put(w.getId(), sound);
            mc.getSoundManager().play(sound);
        }
    }
}

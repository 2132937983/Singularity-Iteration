// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory.blade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 0.1.11.233 (SlashBlade collab B1): the resources a collab blade carries, as an item data component (persistent and
 * synced, so the HUD reads it off the held stack). {@code kind} is the {@link CollabBlade#id}; {@code heat} and
 * {@code overheated} are the nano blade's, {@code mass} and {@code unstable} the singularity blade's.
 */
public record BladeCore(String kind, long eu, float heat, boolean overheated, float mass, boolean unstable) {
    public static final Codec<BladeCore> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("kind").forGetter(BladeCore::kind),
        Codec.LONG.optionalFieldOf("eu", 0L).forGetter(BladeCore::eu),
        Codec.FLOAT.optionalFieldOf("heat", 0F).forGetter(BladeCore::heat),
        Codec.BOOL.optionalFieldOf("overheated", false).forGetter(BladeCore::overheated),
        Codec.FLOAT.optionalFieldOf("mass", 0F).forGetter(BladeCore::mass),
        Codec.BOOL.optionalFieldOf("unstable", false).forGetter(BladeCore::unstable)
    ).apply(i, BladeCore::new));

    public static final StreamCodec<ByteBuf, BladeCore> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, BladeCore::kind,
        ByteBufCodecs.VAR_LONG, BladeCore::eu,
        ByteBufCodecs.FLOAT, BladeCore::heat,
        ByteBufCodecs.BOOL, BladeCore::overheated,
        ByteBufCodecs.FLOAT, BladeCore::mass,
        ByteBufCodecs.BOOL, BladeCore::unstable,
        BladeCore::new);

    public static BladeCore fresh(CollabBlade kind) {
        return new BladeCore(kind.id, 0, 0, false, 0, false);
    }

    public BladeCore withEu(long value) { return new BladeCore(kind, value, heat, overheated, mass, unstable); }

    public BladeCore withHeat(float value, boolean over) { return new BladeCore(kind, eu, value, over, mass, unstable); }

    public BladeCore withMass(float value, boolean unstableNow) { return new BladeCore(kind, eu, heat, overheated, value, unstableNow); }
}

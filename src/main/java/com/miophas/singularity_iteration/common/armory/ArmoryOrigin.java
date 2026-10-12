// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.GlobalPos;

/**
 * Where the suit that a player wears came from (0.1.8.0, player data attachment
 * {@code mio_icif:armory_origin}). A summon from a holo projector sets it; the return action sends
 * the suit back to that projector and clears it. An empty value means "the paired Armory".
 */
public record ArmoryOrigin(Optional<GlobalPos> projector) {
    public static final ArmoryOrigin NONE = new ArmoryOrigin(Optional.empty());
    public static final Codec<ArmoryOrigin> CODEC = RecordCodecBuilder.create(i -> i.group(
        GlobalPos.CODEC.optionalFieldOf("projector").forGetter(ArmoryOrigin::projector)
    ).apply(i, ArmoryOrigin::new));

    public static ArmoryOrigin of(GlobalPos projector) { return new ArmoryOrigin(Optional.of(projector)); }
}

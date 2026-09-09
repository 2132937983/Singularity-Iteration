package com.singularity_iteration.mio_icif.Items.Normal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record BatteryEnergy(long energy, long maxEnergy) {
    public static final Codec<BatteryEnergy> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.fieldOf("energy").forGetter(BatteryEnergy::energy),
            Codec.LONG.fieldOf("maxEnergy").forGetter(BatteryEnergy::maxEnergy)
        ).apply(instance, BatteryEnergy::new)
    );

    public static final StreamCodec<ByteBuf, BatteryEnergy> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG, BatteryEnergy::energy,
        ByteBufCodecs.VAR_LONG, BatteryEnergy::maxEnergy,
        BatteryEnergy::new
    );
}
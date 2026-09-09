package com.singularity_iteration.mio_icif.Items.DataComponent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ReactorComponentData(int storedValue, int maxValue) {

    public static final Codec<ReactorComponentData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.INT.fieldOf("stored_value").forGetter(ReactorComponentData::storedValue),
            Codec.INT.fieldOf("max_value").forGetter(ReactorComponentData::maxValue)
        ).apply(instance, ReactorComponentData::new)
    );

    public static final StreamCodec<ByteBuf, ReactorComponentData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, ReactorComponentData::storedValue,
        ByteBufCodecs.INT, ReactorComponentData::maxValue,
        ReactorComponentData::new
    );

    public ReactorComponentData withStoredValue(int storedValue) {
        return new ReactorComponentData(storedValue, this.maxValue);
    }

    public static ReactorComponentData full(int maxValue) {
        return new ReactorComponentData(0, maxValue);
    }

    public boolean isDepleted() {
        return storedValue >= maxValue;
    }

    public float getDurabilityRatio() {
        if (maxValue <= 0) return 1.0F;
        return 1.0F - (float) storedValue / (float) maxValue;
    }

    public int getRemaining() {
        return Math.max(0, maxValue - storedValue);
    }
}


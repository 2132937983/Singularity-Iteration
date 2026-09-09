package com.singularity_iteration.mio_icif.Items.DataComponent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record FuelRodDurability(int remainingUses, int maxUses, int tickCounter) {

    public static final Codec<FuelRodDurability> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.INT.fieldOf("remaining_uses").forGetter(FuelRodDurability::remainingUses),
            Codec.INT.fieldOf("max_uses").forGetter(FuelRodDurability::maxUses),
            Codec.INT.fieldOf("tick_counter").forGetter(FuelRodDurability::tickCounter)
        ).apply(instance, FuelRodDurability::new)
    );

    public static final StreamCodec<ByteBuf, FuelRodDurability> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, FuelRodDurability::remainingUses,
        ByteBufCodecs.INT, FuelRodDurability::maxUses,
        ByteBufCodecs.INT, FuelRodDurability::tickCounter,
        FuelRodDurability::new
    );

    public FuelRodDurability withRemainingUses(int remainingUses) {
        return new FuelRodDurability(remainingUses, this.maxUses, this.tickCounter);
    }

    public FuelRodDurability withTickCounter(int tickCounter) {
        return new FuelRodDurability(this.remainingUses, this.maxUses, tickCounter);
    }

    public static FuelRodDurability full(int maxUses) {
        return new FuelRodDurability(maxUses, maxUses, 0);
    }

    public boolean isDepleted() {
        return remainingUses <= 0;
    }

    public float getDurabilityRatio() {
        if (maxUses <= 0) return 0.0F;
        return (float) remainingUses / (float) maxUses;
    }
}


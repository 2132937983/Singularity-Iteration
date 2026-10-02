package com.miophas.singularity_iteration.common.armory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data component written by the Connector Kit onto equipment from other mods: marks the
 * item as Armory-compatible and records which body slot it docks to.
 */
public record ArmoryLink(String piece) {
    public static final Codec<ArmoryLink> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("piece").forGetter(ArmoryLink::piece)).apply(i, ArmoryLink::new));
    public static final StreamCodec<ByteBuf, ArmoryLink> STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.map(ArmoryLink::new, ArmoryLink::piece);

    public ArmoryPiece pieceOrDefault() {
        ArmoryPiece p = ArmoryPiece.byName(piece);
        return p == null ? ArmoryPiece.MAINHAND : p;
    }
}

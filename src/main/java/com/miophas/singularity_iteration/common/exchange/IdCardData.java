// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 0.1.10.28 (X4): the data of a written ID card (item component {@code mio_icif:id_card}); a blank card has none. The fields
 * of {@link IdCardRules.Fields} plus the names shown in the tooltip and the server's signature ({@link IdCardRules#sign}).
 */
public record IdCardData(UUID issuer, String issuerName, UUID holder, String holderName, int level, long expiry, long serial,
                         boolean personal, long signature) {
    public static final Codec<IdCardData> CODEC = RecordCodecBuilder.create(i -> i.group(
        UUIDUtil.CODEC.fieldOf("issuer").forGetter(IdCardData::issuer),
        Codec.STRING.fieldOf("issuer_name").forGetter(IdCardData::issuerName),
        UUIDUtil.CODEC.fieldOf("holder").forGetter(IdCardData::holder),
        Codec.STRING.fieldOf("holder_name").forGetter(IdCardData::holderName),
        Codec.INT.fieldOf("level").forGetter(IdCardData::level),
        Codec.LONG.fieldOf("expiry").forGetter(IdCardData::expiry),
        Codec.LONG.fieldOf("serial").forGetter(IdCardData::serial),
        Codec.BOOL.fieldOf("personal").forGetter(IdCardData::personal),
        Codec.LONG.fieldOf("signature").forGetter(IdCardData::signature)
    ).apply(i, IdCardData::new));

    public static final StreamCodec<ByteBuf, IdCardData> STREAM_CODEC = StreamCodec.of((b, d) -> {
        UUIDUtil.STREAM_CODEC.encode(b, d.issuer());
        ByteBufCodecs.STRING_UTF8.encode(b, d.issuerName());
        UUIDUtil.STREAM_CODEC.encode(b, d.holder());
        ByteBufCodecs.STRING_UTF8.encode(b, d.holderName());
        ByteBufCodecs.VAR_INT.encode(b, d.level());
        ByteBufCodecs.VAR_LONG.encode(b, d.expiry());
        ByteBufCodecs.VAR_LONG.encode(b, d.serial());
        ByteBufCodecs.BOOL.encode(b, d.personal());
        ByteBufCodecs.VAR_LONG.encode(b, d.signature());
    }, b -> new IdCardData(UUIDUtil.STREAM_CODEC.decode(b), ByteBufCodecs.STRING_UTF8.decode(b), UUIDUtil.STREAM_CODEC.decode(b),
        ByteBufCodecs.STRING_UTF8.decode(b), ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_LONG.decode(b), ByteBufCodecs.VAR_LONG.decode(b),
        ByteBufCodecs.BOOL.decode(b), ByteBufCodecs.VAR_LONG.decode(b)));

    /** The signed fields. */
    public IdCardRules.Fields fields() {
        return new IdCardRules.Fields(issuer, holder, level, expiry, serial, personal);
    }
}

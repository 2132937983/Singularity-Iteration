// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.recipe.generic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * 0.1.11.8: what a Particle Polymerizer recipe does physically. Optional in the recipe JSON:
 *
 * <pre>"process": { "kind": "densify", "density_from": 7.87, "density_to": 6.0, "real": true, "radiation": false }</pre>
 *
 * <ul>
 *   <li>{@code kind}: {@code densify}, {@code transmute}, {@code phase} or {@code structure} (shown in the log).</li>
 *   <li>{@code density_from}, {@code density_to}: g/cm³ of the input and the product; the screen's implosion photographs
 *       compress the sample by the cube root of their ratio. 0 when unknown.</li>
 *   <li>{@code real}: false for invented materials (the screen marks their densities as a game setting).</li>
 *   <li>{@code radiation}: the step gives off neutrons; living things near an unshielded machine get radiation.</li>
 * </ul>
 * A recipe without the field (from packs or other mods) has kind {@code unknown}, no densities and no radiation.
 */
public record PolymerizerProcess(String kind, float densityFrom, float densityTo, boolean real, boolean radiation) {
    public static final PolymerizerProcess UNKNOWN = new PolymerizerProcess("unknown", 0, 0, true, false);

    public PolymerizerProcess {
        kind = kind == null ? "unknown" : kind.toLowerCase(Locale.ROOT);
    }

    public static final Codec<PolymerizerProcess> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.optionalFieldOf("kind", "unknown").forGetter(PolymerizerProcess::kind),
        Codec.FLOAT.optionalFieldOf("density_from", 0F).forGetter(PolymerizerProcess::densityFrom),
        Codec.FLOAT.optionalFieldOf("density_to", 0F).forGetter(PolymerizerProcess::densityTo),
        Codec.BOOL.optionalFieldOf("real", true).forGetter(PolymerizerProcess::real),
        Codec.BOOL.optionalFieldOf("radiation", false).forGetter(PolymerizerProcess::radiation)
    ).apply(i, PolymerizerProcess::new));

    public static PolymerizerProcess read(RegistryFriendlyByteBuf buf) {
        return new PolymerizerProcess(buf.readUtf(32), buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readBoolean());
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(kind, 32);
        buf.writeFloat(densityFrom);
        buf.writeFloat(densityTo);
        buf.writeBoolean(real);
        buf.writeBoolean(radiation);
    }
}

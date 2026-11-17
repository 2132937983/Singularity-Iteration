// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.recipe.molecular_transformer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * 0.1.11.6: what a Molecular Transformer recipe does physically, for the observation window of the machine's screen.
 * Only the screen reads it; it does not change the recipe. Optional in the recipe JSON:
 *
 * <pre>"process": { "kind": "nuclear", "from": "Fe", "to": "Ir" }</pre>
 *
 * <ul>
 *   <li>{@code nuclear}: transmutation; {@code from} and {@code to} are element symbols (H .. Pu). The window shows an
 *       ion trap, accelerator pulses and the atomic number stepping from {@code from} to {@code to}.</li>
 *   <li>{@code lattice}: the same atoms in a new arrangement (graphite to diamond); a rotating lattice and press
 *       cycles.</li>
 *   <li>{@code molecular}: molecules taken apart and rebuilt; nuclei unchanged.</li>
 *   <li>{@code exotic}: no known physical process (nether star, sunnarium); the window says so.</li>
 * </ul>
 * A recipe without the field (from other mods or packs) shows as {@code unknown}: a generic trap with no element line.
 *
 * @param kind one of {@code nuclear}, {@code lattice}, {@code molecular}, {@code exotic}, {@code unknown}
 * @param from element symbol before, or "" (nuclear only)
 * @param to   element symbol after, or "" (nuclear only)
 */
public record MolecularProcess(String kind, String from, String to) {
    public static final MolecularProcess UNKNOWN = new MolecularProcess("unknown", "", "");

    public MolecularProcess {
        kind = kind == null ? "unknown" : kind.toLowerCase(Locale.ROOT);
        from = from == null ? "" : from;
        to = to == null ? "" : to;
    }

    public static final Codec<MolecularProcess> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("kind").forGetter(MolecularProcess::kind),
        Codec.STRING.optionalFieldOf("from", "").forGetter(MolecularProcess::from),
        Codec.STRING.optionalFieldOf("to", "").forGetter(MolecularProcess::to)
    ).apply(i, MolecularProcess::new));

    public static MolecularProcess read(RegistryFriendlyByteBuf buf) {
        return new MolecularProcess(buf.readUtf(32), buf.readUtf(8), buf.readUtf(8));
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(kind, 32);
        buf.writeUtf(from, 8);
        buf.writeUtf(to, 8);
    }
}

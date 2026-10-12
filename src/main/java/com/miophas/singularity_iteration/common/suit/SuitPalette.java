// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Colour palette of one suit piece (0.1.8.0, data component {@code mio_icif:suit_palette}).
 *
 * <p>{@code primary} tints the plates, {@code accent} tints the light strips and the visor. Both
 * are opaque RGB values (0xRRGGBB). A piece without the component shows its original texture.
 *
 * <p>The armor material of a palette piece has two layers. The armor layer renders layer 0 with
 * the plating texture and the primary colour, then layer 1 with the accent texture and the accent
 * colour. Without a palette, layer 0 uses the original texture and layer 1 is skipped (tint 0).
 *
 * <p>The nano suit unlocks the palette first. {@link #supports} lists the pieces.
 */
public record SuitPalette(int primary, int accent) {
    public static final Codec<SuitPalette> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("primary").forGetter(SuitPalette::primary),
        Codec.INT.fieldOf("accent").forGetter(SuitPalette::accent)
    ).apply(i, SuitPalette::new));
    public static final StreamCodec<ByteBuf, SuitPalette> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, SuitPalette::primary, ByteBufCodecs.INT, SuitPalette::accent, SuitPalette::new);

    public SuitPalette {
        primary &= 0xFFFFFF;
        accent &= 0xFFFFFF;
    }

    /** Presets of the palette editor: name key suffix, primary, accent. */
    public record Preset(String id, int primary, int accent) { }
    public static final List<Preset> PRESETS = List.of(
        new Preset("stealth", 0x3A3F46, 0x37E35A),
        new Preset("crimson", 0xC8323C, 0xFFD24A),
        new Preset("arctic", 0xE8F0FF, 0x3FD4FF),
        new Preset("cobalt", 0x2F5FD0, 0xE8F4FF),
        new Preset("desert", 0xC9A86A, 0xFF8A2A),
        new Preset("jungle", 0x4E6B3A, 0xC9F04A),
        new Preset("violet", 0x6C3FB0, 0xFF5AE8),
        new Preset("graphite", 0x5B6168, 0xFF3B30));

    /** Items that accept a palette. The nano suit first. */
    private static final List<ResourceLocation> SUPPORTED = List.of(
        id("armor/item_armor_nano_helmet"), id("armor/item_armor_nano_chestplate"),
        id("armor/item_armor_nano_leggings"), id("armor/item_armor_nano_boots"));

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    /** True when the item accepts a palette. */
    public static boolean supports(ItemStack stack) {
        return !stack.isEmpty() && SUPPORTED.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** The palette of a stack, or {@code null}. */
    @Nullable
    public static SuitPalette of(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(com.miophas.singularity_iteration.common.registry.mio_icif_data_components.SUIT_PALETTE.get());
    }

    /** Sets or removes ({@code null}) the palette. Unsupported items are not changed. */
    public static void set(ItemStack stack, @Nullable SuitPalette palette) {
        if (!supports(stack)) return;
        var type = com.miophas.singularity_iteration.common.registry.mio_icif_data_components.SUIT_PALETTE.get();
        if (palette == null) stack.remove(type); else stack.set(type, palette);
    }

    /**
     * Texture of one armor layer: the original texture without a palette, else the plating
     * (layer 0) or accent (layer 1) texture. {@code base} is the original texture path without
     * {@code .png}, for example {@code textures/models/armor/nano_helmet}.
     */
    public static ResourceLocation texture(ItemStack stack, String base, boolean accentLayer) {
        String path = accentLayer ? base + "_accent" : of(stack) == null ? base : base + "_plating";
        return ResourceLocation.fromNamespaceAndPath("mio_icif", path + ".png");
    }

    /** Tint of one armor layer in ARGB, 0 to skip the layer. */
    public static int tint(ItemStack stack, int layerIndex) {
        SuitPalette palette = of(stack);
        if (palette == null) return layerIndex == 0 ? 0xFFFFFFFF : 0;
        return 0xFF000000 | (layerIndex == 0 ? palette.primary : palette.accent);
    }
}

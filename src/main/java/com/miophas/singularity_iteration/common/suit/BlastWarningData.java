// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Data of the blast warning unit (0.1.10.5): which entities of other mods are explosives.
 *
 * <ul>
 *   <li>Data map {@code mio_icif:blast_warning} on entity types
 *       ({@code data/<namespace>/data_maps/entity_type/blast_warning.json}): {@code radius} in
 *       blocks (required), {@code fuse} = full fuse in seconds (optional), {@code label} for the
 *       HUD (optional, at most {@link #LABEL_LENGTH} characters). Synced to the clients.</li>
 *   <li>Entity type tag {@code mio_icif:blast_warning}: the entity counts as an explosive with
 *       radius 4 when it has no data map entry.</li>
 * </ul>
 * The timer of such an explosive comes from a public {@code int getFuse()} (or
 * {@code getFuseTime}, {@code getFuseTicks}, {@code getFuseTimer}) in ticks when the entity has
 * one; else from {@code fuse} minus the age of the entity on the client; else the HUD shows the
 * charge without a timer. See {@code BlastDetector} for every rule.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class BlastWarningData {
    private BlastWarningData() {}

    public static final int LABEL_LENGTH = 9;
    /** Radius of a tagged explosive without a data map entry (vanilla TNT). */
    public static final float TAG_RADIUS = 4F;

    /** One data map value. */
    public record Entry(float radius, float fuse, Optional<String> label) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.floatRange(0.5F, 512F).fieldOf("radius").forGetter(Entry::radius),
            Codec.floatRange(0F, 3600F).optionalFieldOf("fuse", 0F).forGetter(Entry::fuse),
            Codec.STRING.optionalFieldOf("label").forGetter(Entry::label)
        ).apply(i, Entry::new));
    }

    public static final DataMapType<EntityType<?>, Entry> MAP = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "blast_warning"), Registries.ENTITY_TYPE, Entry.CODEC)
        .synced(Entry.CODEC, false)
        .build();

    public static final TagKey<EntityType<?>> TAG = TagKey.create(Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "blast_warning"));

    /** The data map value of {@code type}, or null. */
    @Nullable
    public static Entry entry(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).getData(MAP);
    }

    @SubscribeEvent
    static void register(RegisterDataMapTypesEvent event) {
        event.register(MAP);
    }
}

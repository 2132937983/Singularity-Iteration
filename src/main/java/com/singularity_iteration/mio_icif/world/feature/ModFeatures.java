package com.singularity_iteration.mio_icif.world.feature;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

@SuppressWarnings("null")
public class ModFeatures {

    public static ResourceKey<ConfiguredFeature<?, ?>> createOreFeatureKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, name));
    }

    public static ResourceKey<PlacedFeature> registerPlacedFeature(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, name));
    }

    public static void registerOreFeature(BootstrapContext<ConfiguredFeature<?, ?>> context, String name, 
                                        net.minecraft.world.level.block.state.BlockState state, 
                                        int size, float discardChance) {
        context.register(createOreFeatureKey(name), new ConfiguredFeature<>(
                Feature.ORE,
                new OreConfiguration(List.of(
                        OreConfiguration.target(
                                new net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest(net.minecraft.world.level.block.Blocks.STONE),
                                state
                        )
                ), size, discardChance)
        ));
    }

    public static void registerDeepOreFeature(BootstrapContext<ConfiguredFeature<?, ?>> context, String name, 
                                            net.minecraft.world.level.block.state.BlockState state, 
                                            int size, float discardChance) {
        context.register(createOreFeatureKey(name), new ConfiguredFeature<>(
                Feature.ORE,
                new OreConfiguration(List.of(
                        OreConfiguration.target(
                                new net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest(net.minecraft.world.level.block.Blocks.DEEPSLATE),
                                state
                        )
                ), size, discardChance)
        ));
    }

    public static void registerPlacedFeature(BootstrapContext<PlacedFeature> context, String name, 
                                           Holder<ConfiguredFeature<?, ?>> feature, 
                                           PlacementModifier... placementModifiers) {
        context.register(registerPlacedFeature(name), new PlacedFeature(feature, List.of(placementModifiers)));
    }

    public static PlacementModifier[] commonOrePlacement(int count, PlacementModifier height) {
        return new PlacementModifier[] {
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                CountPlacement.of(count),
                InSquarePlacement.spread(),
                height,
                BiomeFilter.biome()
        };
    }
}


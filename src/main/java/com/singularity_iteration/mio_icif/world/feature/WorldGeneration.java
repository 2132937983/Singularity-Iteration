package com.singularity_iteration.mio_icif.world.feature;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.tags.BiomeTags;

import java.util.List;

@SuppressWarnings("null")
public class WorldGeneration {

    // 锡矿石
public static final ResourceKey<ConfiguredFeature<?, ?>> TIN_ORE_KEY = 
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "tin_ore"));
    
    public static final ResourceKey<PlacedFeature> TIN_ORE_PLACED_KEY = 
        ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "tin_ore_placed"));
    
    // 铀矿石
    public static final ResourceKey<ConfiguredFeature<?, ?>> URAN_ORE_KEY = 
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "uran_ore"));
    
    public static final ResourceKey<PlacedFeature> URAN_ORE_PLACED_KEY = 
        ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "uran_ore_placed"));
    
    // 铅矿石
public static final ResourceKey<ConfiguredFeature<?, ?>> LEAD_ORE_KEY = 
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "lead_ore"));
    
    public static final ResourceKey<PlacedFeature> LEAD_ORE_PLACED_KEY = 
        ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "lead_ore_placed"));

    // 深层锡矿石
public static final ResourceKey<ConfiguredFeature<?, ?>> TIN_ORE_DEEP_KEY = 
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "tin_ore_deep"));
    
    public static final ResourceKey<PlacedFeature> TIN_ORE_DEEP_PLACED_KEY = 
        ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "tin_ore_deep_placed"));
    
    // 深层铀矿石
    public static final ResourceKey<ConfiguredFeature<?, ?>> URAN_ORE_DEEP_KEY = 
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "uran_ore_deep"));
    
    public static final ResourceKey<PlacedFeature> URAN_ORE_DEEP_PLACED_KEY = 
        ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "uran_ore_deep_placed"));
    
    // 深层铅矿石
public static final ResourceKey<ConfiguredFeature<?, ?>> LEAD_ORE_DEEP_KEY = 
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "lead_ore_deep"));
    
    public static final ResourceKey<PlacedFeature> LEAD_ORE_DEEP_PLACED_KEY = 
        ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "lead_ore_deep_placed"));

    // 铌矿石
public static final ResourceKey<ConfiguredFeature<?, ?>> NIOBIUM_ORE_KEY = 
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "niobium_ore"));
    
    public static final ResourceKey<PlacedFeature> NIOBIUM_ORE_PLACED_KEY = 
        ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "niobium_ore_placed"));
    
    public static final ResourceKey<ConfiguredFeature<?, ?>> NIOBIUM_ORE_DEEP_KEY = 
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "niobium_ore_deep"));
    
    public static final ResourceKey<PlacedFeature> NIOBIUM_ORE_DEEP_PLACED_KEY = 
        ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "niobium_ore_deep_placed"));

    // 钛矿石
public static final ResourceKey<ConfiguredFeature<?, ?>> TITANIUM_ORE_KEY = 
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "titanium_ore"));
    
    public static final ResourceKey<PlacedFeature> TITANIUM_ORE_PLACED_KEY = 
        ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "titanium_ore_placed"));
    
    public static final ResourceKey<ConfiguredFeature<?, ?>> TITANIUM_ORE_DEEP_KEY = 
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "titanium_ore_deep"));
    
    public static final ResourceKey<PlacedFeature> TITANIUM_ORE_DEEP_PLACED_KEY = 
        ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "titanium_ore_deep_placed"));

    // 橡胶�
public static final ResourceKey<ConfiguredFeature<?, ?>> RUBBER_TREE_KEY = 
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "rubber_tree"));
    
    public static final ResourceKey<PlacedFeature> RUBBER_TREE_PLACED_KEY = 
        ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "rubber_tree_placed"));

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // 注册锡矿石
    ModFeatures.registerOreFeature(context, "tin_ore", 
            mio_icif_blocks.BLOCK_ORE_TIN.get().defaultBlockState(),
            10, 0.0F);
        
        // 注册铀矿石（生成在主世界深层，类似铀矿）
        ModFeatures.registerOreFeature(context, "uran_ore", 
            mio_icif_blocks.BLOCK_ORE_URAN.get().defaultBlockState(),
            8, 0.0F);
        
        // 注册铅矿石（生成在主世界深层，类似铅矿）
        ModFeatures.registerOreFeature(context, "lead_ore", 
            mio_icif_blocks.BLOCK_ORE_LEAD.get().defaultBlockState(),
            10, 0.0F);

        // 注册深层锡矿石（替换深板岩）
        ModFeatures.registerDeepOreFeature(context, "tin_ore_deep", 
            mio_icif_blocks.BLOCK_ORE_TIN_IN_DEEP.get().defaultBlockState(),
            10, 0.0F);
        
        // 注册深层铀矿石（替换深板岩�
    ModFeatures.registerDeepOreFeature(context, "uran_ore_deep", 
            mio_icif_blocks.BLOCK_ORE_URAN_IN_DEEP.get().defaultBlockState(),
            8, 0.0F);
        
        // 注册深层铅矿石（替换深板岩）
        ModFeatures.registerDeepOreFeature(context, "lead_ore_deep", 
            mio_icif_blocks.BLOCK_ORE_LEAD_IN_DEEP.get().defaultBlockState(),
            10, 0.0F);

        // 注册铌矿石（比钻石少，size=6）
        ModFeatures.registerOreFeature(context, "niobium_ore", 
            mio_icif_blocks.BLOCK_ORE_NIOBIUM.get().defaultBlockState(),
            6, 0.0F);
        
        // 注册深层铌矿石
        ModFeatures.registerDeepOreFeature(context, "niobium_ore_deep", 
            mio_icif_blocks.BLOCK_ORE_NIOBIUM_IN_DEEP.get().defaultBlockState(),
            6, 0.0F);

        // 注册钛矿石（比金少，size=7）
        ModFeatures.registerOreFeature(context, "titanium_ore", 
            mio_icif_blocks.BLOCK_ORE_TITANIUM.get().defaultBlockState(),
            7, 0.0F);
        
        // 注册深层钛矿石
        ModFeatures.registerDeepOreFeature(context, "titanium_ore_deep", 
            mio_icif_blocks.BLOCK_ORE_TITANIUM_IN_DEEP.get().defaultBlockState(),
            7, 0.0F);

        // 注册橡胶�
    RubberTreeFeature.registerConfiguredFeature(context);
    }

    public static void bootstrapPlaced(BootstrapContext<PlacedFeature> context) {
        // 锡矿石的放置
        Holder<ConfiguredFeature<?, ?>> tinOreFeature = context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(TIN_ORE_KEY);
        
        context.register(TIN_ORE_PLACED_KEY, new PlacedFeature(
            tinOreFeature,
            List.of(
                CountPlacement.of(16), // 每次尝试生成 16 个矿石
            InSquarePlacement.spread(),
                HeightRangePlacement.uniform(
                    VerticalAnchor.absolute(5),
                    VerticalAnchor.absolute(112) // 与铜矿相同的高度范围
                ),
                BiomeFilter.biome()
            )
        ));
        
        // 铀矿石的放置（生成在深层，Y=5 �?Y=64�
    Holder<ConfiguredFeature<?, ?>> uranOreFeature = context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(URAN_ORE_KEY);
        
        context.register(URAN_ORE_PLACED_KEY, new PlacedFeature(
            uranOreFeature,
            List.of(
                CountPlacement.of(8), // 每次尝试生成 8 个矿脉（比锡矿少�
            InSquarePlacement.spread(),
                HeightRangePlacement.uniform(
                    VerticalAnchor.absolute(5),
                    VerticalAnchor.absolute(64) // 深层生成
                ),
                BiomeFilter.biome()
            )
        ));
        
        // 铅矿石的放置（生成在深层，Y=5 �?Y=80�
    Holder<ConfiguredFeature<?, ?>> leadOreFeature = context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(LEAD_ORE_KEY);
        
        context.register(LEAD_ORE_PLACED_KEY, new PlacedFeature(
            leadOreFeature,
            List.of(
                CountPlacement.of(12), // 每次尝试生成 12 个矿石
            InSquarePlacement.spread(),
                HeightRangePlacement.uniform(
                    VerticalAnchor.absolute(5),
                    VerticalAnchor.absolute(80) // 深层生成
                ),
                BiomeFilter.biome()
            )
        ));

        // 深层锡矿石的放置（Y=-64 �?Y=0，深板岩层）
        Holder<ConfiguredFeature<?, ?>> tinOreDeepFeature = context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(TIN_ORE_DEEP_KEY);
        
        context.register(TIN_ORE_DEEP_PLACED_KEY, new PlacedFeature(
            tinOreDeepFeature,
            List.of(
                CountPlacement.of(12),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(
                    VerticalAnchor.absolute(-64),
                    VerticalAnchor.absolute(0)
                ),
                BiomeFilter.biome()
            )
        ));
        
        // 深层铀矿石的放置（Y=-64 �?Y=-20，更深层�
    Holder<ConfiguredFeature<?, ?>> uranOreDeepFeature = context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(URAN_ORE_DEEP_KEY);
        
        context.register(URAN_ORE_DEEP_PLACED_KEY, new PlacedFeature(
            uranOreDeepFeature,
            List.of(
                CountPlacement.of(6),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(
                    VerticalAnchor.absolute(-64),
                    VerticalAnchor.absolute(-20)
                ),
                BiomeFilter.biome()
            )
        ));
        
        // 深层铅矿石的放置（Y=-64 �?Y=-10�
    Holder<ConfiguredFeature<?, ?>> leadOreDeepFeature = context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(LEAD_ORE_DEEP_KEY);
        
        context.register(LEAD_ORE_DEEP_PLACED_KEY, new PlacedFeature(
            leadOreDeepFeature,
            List.of(
                CountPlacement.of(10),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(
                    VerticalAnchor.absolute(-64),
                    VerticalAnchor.absolute(-10)
                ),
                BiomeFilter.biome()
            )
        ));

        // 铌矿石的放置（比钻石少，Y=-64 �?Y=16，count=4）
        Holder<ConfiguredFeature<?, ?>> niobiumOreFeature = context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(NIOBIUM_ORE_KEY);
        
        context.register(NIOBIUM_ORE_PLACED_KEY, new PlacedFeature(
            niobiumOreFeature,
            List.of(
                CountPlacement.of(4),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(
                    VerticalAnchor.absolute(-64),
                    VerticalAnchor.absolute(16)
                ),
                BiomeFilter.biome()
            )
        ));
        
        // 深层铌矿石的放置（Y=-64 �?Y=0，count=3）
        Holder<ConfiguredFeature<?, ?>> niobiumOreDeepFeature = context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(NIOBIUM_ORE_DEEP_KEY);
        
        context.register(NIOBIUM_ORE_DEEP_PLACED_KEY, new PlacedFeature(
            niobiumOreDeepFeature,
            List.of(
                CountPlacement.of(3),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(
                    VerticalAnchor.absolute(-64),
                    VerticalAnchor.absolute(0)
                ),
                BiomeFilter.biome()
            )
        ));
        
        // 钛矿石的放置（比金少，Y=-64 �?Y=32，count=5）
        Holder<ConfiguredFeature<?, ?>> titaniumOreFeature = context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(TITANIUM_ORE_KEY);
        
        context.register(TITANIUM_ORE_PLACED_KEY, new PlacedFeature(
            titaniumOreFeature,
            List.of(
                CountPlacement.of(5),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(
                    VerticalAnchor.absolute(-64),
                    VerticalAnchor.absolute(32)
                ),
                BiomeFilter.biome()
            )
        ));
        
        // 深层钛矿石的放置（Y=-64 �?Y=0，count=4）
        Holder<ConfiguredFeature<?, ?>> titaniumOreDeepFeature = context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(TITANIUM_ORE_DEEP_KEY);
        
        context.register(TITANIUM_ORE_DEEP_PLACED_KEY, new PlacedFeature(
            titaniumOreDeepFeature,
            List.of(
                CountPlacement.of(4),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(
                    VerticalAnchor.absolute(-64),
                    VerticalAnchor.absolute(0)
                ),
                BiomeFilter.biome()
            )
        ));

        // 橡胶树的放置
        RubberTreeFeature.registerPlacedFeature(context);
    }

    public static void register(RegistrySetBuilder builder) {
        builder.add(Registries.CONFIGURED_FEATURE, WorldGeneration::bootstrap);
        builder.add(Registries.PLACED_FEATURE, WorldGeneration::bootstrapPlaced);
        builder.add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, WorldGeneration::bootstrapBiomeModifiers);
    }

    public static void bootstrapBiomeModifiers(BootstrapContext<BiomeModifier> context) {
        Holder<PlacedFeature> tinOrePlaced = context.lookup(Registries.PLACED_FEATURE).getOrThrow(TIN_ORE_PLACED_KEY);
        Holder<PlacedFeature> uranOrePlaced = context.lookup(Registries.PLACED_FEATURE).getOrThrow(URAN_ORE_PLACED_KEY);
        Holder<PlacedFeature> leadOrePlaced = context.lookup(Registries.PLACED_FEATURE).getOrThrow(LEAD_ORE_PLACED_KEY);
        Holder<PlacedFeature> tinOreDeepPlaced = context.lookup(Registries.PLACED_FEATURE).getOrThrow(TIN_ORE_DEEP_PLACED_KEY);
        Holder<PlacedFeature> uranOreDeepPlaced = context.lookup(Registries.PLACED_FEATURE).getOrThrow(URAN_ORE_DEEP_PLACED_KEY);
        Holder<PlacedFeature> leadOreDeepPlaced = context.lookup(Registries.PLACED_FEATURE).getOrThrow(LEAD_ORE_DEEP_PLACED_KEY);
        Holder<PlacedFeature> niobiumOrePlaced = context.lookup(Registries.PLACED_FEATURE).getOrThrow(NIOBIUM_ORE_PLACED_KEY);
        Holder<PlacedFeature> niobiumOreDeepPlaced = context.lookup(Registries.PLACED_FEATURE).getOrThrow(NIOBIUM_ORE_DEEP_PLACED_KEY);
        Holder<PlacedFeature> titaniumOrePlaced = context.lookup(Registries.PLACED_FEATURE).getOrThrow(TITANIUM_ORE_PLACED_KEY);
        Holder<PlacedFeature> titaniumOreDeepPlaced = context.lookup(Registries.PLACED_FEATURE).getOrThrow(TITANIUM_ORE_DEEP_PLACED_KEY);
        Holder<PlacedFeature> rubberTreePlaced = context.lookup(Registries.PLACED_FEATURE).getOrThrow(RUBBER_TREE_PLACED_KEY);
        
        // 为主世界生物群系添加锡矿石
    context.register(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, 
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "tin_ore_overworld")),
            new BiomeModifiers.AddFeaturesBiomeModifier(
                context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(tinOrePlaced),
                GenerationStep.Decoration.UNDERGROUND_ORES
            )
        );
        
        // 为主世界生物群系添加铀矿石
        context.register(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, 
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "uran_ore_overworld")),
            new BiomeModifiers.AddFeaturesBiomeModifier(
                context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(uranOrePlaced),
                GenerationStep.Decoration.UNDERGROUND_ORES
            )
        );
        
        // 为主世界生物群系添加铅矿石
    context.register(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, 
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "lead_ore_overworld")),
            new BiomeModifiers.AddFeaturesBiomeModifier(
                context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(leadOrePlaced),
                GenerationStep.Decoration.UNDERGROUND_ORES
            )
        );

        // 为主世界生物群系添加深层锡矿石
    context.register(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, 
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "tin_ore_deep_overworld")),
            new BiomeModifiers.AddFeaturesBiomeModifier(
                context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(tinOreDeepPlaced),
                GenerationStep.Decoration.UNDERGROUND_ORES
            )
        );
        
        // 为主世界生物群系添加深层铀矿石
        context.register(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, 
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "uran_ore_deep_overworld")),
            new BiomeModifiers.AddFeaturesBiomeModifier(
                context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(uranOreDeepPlaced),
                GenerationStep.Decoration.UNDERGROUND_ORES
            )
        );
        
        // 为主世界生物群系添加深层铅矿石
    context.register(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, 
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "lead_ore_deep_overworld")),
            new BiomeModifiers.AddFeaturesBiomeModifier(
                context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(leadOreDeepPlaced),
                GenerationStep.Decoration.UNDERGROUND_ORES
            )
        );

        // 为主世界生物群系添加铌矿石
    context.register(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, 
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "niobium_ore_overworld")),
            new BiomeModifiers.AddFeaturesBiomeModifier(
                context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(niobiumOrePlaced),
                GenerationStep.Decoration.UNDERGROUND_ORES
            )
        );
        
        // 为主世界生物群系添加深层铌矿石
        context.register(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, 
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "niobium_ore_deep_overworld")),
            new BiomeModifiers.AddFeaturesBiomeModifier(
                context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(niobiumOreDeepPlaced),
                GenerationStep.Decoration.UNDERGROUND_ORES
            )
        );
        
        // 为主世界生物群系添加钛矿石
    context.register(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, 
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "titanium_ore_overworld")),
            new BiomeModifiers.AddFeaturesBiomeModifier(
                context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(titaniumOrePlaced),
                GenerationStep.Decoration.UNDERGROUND_ORES
            )
        );
        
        // 为主世界生物群系添加深层钛矿石
        context.register(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, 
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "titanium_ore_deep_overworld")),
            new BiomeModifiers.AddFeaturesBiomeModifier(
                context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(titaniumOreDeepPlaced),
                GenerationStep.Decoration.UNDERGROUND_ORES
            )
        );

        // 为丛林生物群系添加橡胶树（最符合橡胶树生长环境）
        context.register(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, 
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "rubber_tree_jungle")),
            new BiomeModifiers.AddFeaturesBiomeModifier(
                context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_JUNGLE),
                HolderSet.direct(rubberTreePlaced),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION
            )
        );

        // 为森林生物群系添加橡胶树（温暖湿润环境也适合
    context.register(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, 
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "rubber_tree_forest")),
            new BiomeModifiers.AddFeaturesBiomeModifier(
                context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_FOREST),
                HolderSet.direct(rubberTreePlaced),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION
            )
        );

        // 为针叶林生物群系添加橡胶树
        context.register(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, 
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "rubber_tree_taiga")),
            new BiomeModifiers.AddFeaturesBiomeModifier(
                context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_TAIGA),
                HolderSet.direct(rubberTreePlaced),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION
            )
        );
    }

    public static void bootstrapConfiguredFeature(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        bootstrap(context);
    }
}
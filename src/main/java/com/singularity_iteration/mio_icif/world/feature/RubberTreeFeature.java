package com.singularity_iteration.mio_icif.world.feature;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Blocks.Environment.BlockRubberWood;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.core.Vec3i;

import java.util.List;

@SuppressWarnings("null")
public class RubberTreeFeature {

    /**
     * 注册橡胶树的配置特征
     * 使用高大阔叶树形态，参考巴西橡胶树（Hevea brasiliensis�?
     */
    public static void registerConfiguredFeature(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // 创建橡胶树配置（高大阔叶树，参考现实中巴西橡胶树形态）
        TreeConfiguration.TreeConfigurationBuilder treeConfigurationBuilder = new TreeConfiguration.TreeConfigurationBuilder(
            BlockStateProvider.simple(mio_icif_blocks.BLOCK_RUBBER_TREE.get().defaultBlockState().setValue(BlockRubberWood.HAS_HARZ, false).setValue(BlockRubberWood.HAS_SPOT, false)),
            new StraightTrunkPlacer(7, 2, 1),
            BlockStateProvider.simple(mio_icif_blocks.BLOCK_RUBBER_LEAF.get()),
            new RubberTreeFoliagePlacer(UniformInt.of(3, 4), UniformInt.of(0, 0)),
            new TwoLayersFeatureSize(2, 0, 2)
        );

        // 忽略藤蔓
        treeConfigurationBuilder.ignoreVines();

        // 添加装饰器：在树干上放置有胶橡胶木（最终?个，最终?个）
        treeConfigurationBuilder.decorators(List.of(new RubberWoodDecorator(1)));

        context.register(WorldGeneration.RUBBER_TREE_KEY, new ConfiguredFeature<>(Feature.TREE, treeConfigurationBuilder.build()));
        Singularity_Iteration.LOGGER.info("Rubber tree configured feature registered");
    }

    /**
     * 注册橡胶树的放置特征
     */
    public static void registerPlacedFeature(BootstrapContext<PlacedFeature> context) {
        Holder<ConfiguredFeature<?, ?>> rubberTreeFeature = context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(WorldGeneration.RUBBER_TREE_KEY);

        context.register(WorldGeneration.RUBBER_TREE_PLACED_KEY, new PlacedFeature(
            rubberTreeFeature,
            List.of(
                PlacementUtils.countExtra(1, 0.3F, 1),
                InSquarePlacement.spread(), // 在区块内随机分布
                SurfaceWaterDepthFilter.forMaxDepth(0), // 不在水中生成
                PlacementUtils.HEIGHTMAP_OCEAN_FLOOR, // 基于海平面高度图
                // 检查下方一格是否是适合树生长的方块（泥土、草地、沙子等�?
                BlockPredicateFilter.forPredicate(BlockPredicate.matchesTag(new Vec3i(0, -1, 0), BlockTags.DIRT)),
                BiomeFilter.biome() // 只在合适的生物群系生成
            )
        ));
        Singularity_Iteration.LOGGER.info("Rubber tree placed feature registered");
    }
}


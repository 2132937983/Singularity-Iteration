package com.singularity_iteration.mio_icif.world.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

@SuppressWarnings("null")
public class RubberTreeFoliagePlacer extends FoliagePlacer {

    public static final MapCodec<RubberTreeFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
        instance -> foliagePlacerParts(instance).apply(instance, RubberTreeFoliagePlacer::new)
    );

    public RubberTreeFoliagePlacer(IntProvider radius, IntProvider offset) {
        super(radius, offset);
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return mio_icif_foliage_placers.RUBBER_TREE.get();
    }

    @Override
    protected void createFoliage(
        LevelSimulatedReader level,
        FoliagePlacer.FoliageSetter blockSetter,
        RandomSource random,
        TreeConfiguration config,
        int maxFreeTreeHeight,
        FoliagePlacer.FoliageAttachment attachment,
        int foliageHeight,
        int foliageRadius,
        int offset
    ) {
        BlockPos pos = attachment.pos();
        int baseRadius = foliageRadius + attachment.radiusOffset();

        int bottomRadius = Math.max(baseRadius, 1);
        int topRadius = Math.max(baseRadius - 1, 1);

        this.placeLeavesRow(level, blockSetter, random, config, pos, topRadius, 0, attachment.doubleTrunk());
        this.placeLeavesRow(level, blockSetter, random, config, pos, bottomRadius, -1, attachment.doubleTrunk());
    }

    @Override
    public int foliageHeight(RandomSource random, int height, TreeConfiguration config) {
        return 1;
    }

    @Override
    protected boolean shouldSkipLocation(RandomSource random, int localX, int localY, int localZ, int range, boolean large) {
        return localX == range && localZ == range && (random.nextInt(2) == 0 || localY == 0);
    }
}


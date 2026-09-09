package com.singularity_iteration.mio_icif.Blocks.Crop;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("null")
public class mio_icif_block_weed extends BushBlock {

    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public static final MapCodec<mio_icif_block_weed> CODEC = simpleCodec(mio_icif_block_weed::new);

    public mio_icif_block_weed(Properties properties) {
        super(properties);
    }

    public mio_icif_block_weed() {
        this(Block.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .noOcclusion()
                .instabreak()
                .sound(SoundType.GRASS)
                .randomTicks()
                .offsetType(Block.OffsetType.XZ));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(net.minecraft.world.level.block.Blocks.FARMLAND)
                || state.is(net.minecraft.world.level.block.Blocks.DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(25) == 0) {
            BlockPos targetPos = pos.offset(
                    random.nextInt(3) - 1,
                    random.nextInt(3) - 1,
                    random.nextInt(3) - 1
            );

            if (targetPos.equals(pos)) return;

            BlockState targetState = level.getBlockState(targetPos);
            BlockState belowState = level.getBlockState(targetPos.below());

            if (targetState.isAir() && belowState.is(net.minecraft.world.level.block.Blocks.FARMLAND)) {
                level.setBlock(targetPos, this.defaultBlockState(), 3);
            }
        }

        if (random.nextInt(20) == 0) {
            for (BlockPos offset : new BlockPos[]{pos.north(), pos.south(), pos.east(), pos.west()}) {
                if (level.getBlockEntity(offset) instanceof com.singularity_iteration.mio_icif.api.crop.IPlanter planter) {
                    if (planter.getWeedControl() > 0) continue;

                    if (planter.getPlant() == null) {
                        planter.setPlant(com.singularity_iteration.mio_icif.api.internal.crop.PlantRegistry.instance.getPlant("mio_icif", "weed"));
                        planter.setGrowthStage(1);
                        planter.updateState();
                        break;
                    } else if (!planter.getPlant().getTypeId().equals("weed")) {
                        int resistance = planter.getResilience();
                        int baseChance = 20;
                        int actualChance = Math.max(3, baseChance - resistance / 2);

                        if (random.nextInt(100) < actualChance) {
                            planter.setPlant(com.singularity_iteration.mio_icif.api.internal.crop.PlantRegistry.instance.getPlant("mio_icif", "weed"));
                            planter.setGrowthStage(1);
                            planter.updateState();
                            break;
                        }
                    }
                }
            }
        }
    }
}
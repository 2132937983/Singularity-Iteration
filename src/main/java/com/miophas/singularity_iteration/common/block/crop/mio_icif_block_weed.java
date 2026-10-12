package com.miophas.singularity_iteration.common.block.crop;

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

    /**
     * <p><b>EN:</b> Weed spread from a weed block. 1/25: a new weed block on free farmland next to
     * it. 1/20: one horizontal crop planter without Weed-EX becomes a weed.
     * 0.1.8.1 fixes: (1) the planter is reset first, like the crop cycle does, so the weed does
     * not keep the traits, growth points, scan level or crossing flag of the old plant;
     * (2) positions in unloaded chunks are skipped, so a random tick at a chunk edge never
     * loads a chunk; (3) nothing happens when the weed plant is not registered.
     * <p><b>ZH:</b> 杂草方块的扩散。1/25：在相邻空闲耕地上生成新杂草方块。1/20：一个没有除草剂的水平相邻作物架变为杂草。
     * 0.1.8.1 修复：(1) 先重置作物架（与作物周期相同），杂草不再继承旧植物的属性、生长点、扫描等级或杂交标志；
     * (2) 跳过未加载区块中的位置，区块边缘的随机刻不会加载区块；(3) 杂草植物未注册时不做任何事。
     * <p><b>JA:</b> 雑草ブロックからの拡散。1/25：隣の空いた耕地に新しい雑草ブロック。1/20：除草剤のない水平隣接の
     * 作物棒 1 本が雑草になる。0.1.8.1 の修正：(1) 作物サイクルと同じく先に作物棒をリセットするので、雑草は旧植物の
     * 特性値・成長ポイント・スキャン段階・交配フラグを引き継がない。(2) 未読み込みチャンクの位置は飛ばすので、
     * チャンク境界のランダムティックがチャンクを読み込まない。(3) 雑草植物が未登録なら何もしない。
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(25) == 0) {
            BlockPos targetPos = pos.offset(
                    random.nextInt(3) - 1,
                    random.nextInt(3) - 1,
                    random.nextInt(3) - 1
            );

            if (targetPos.equals(pos) || !level.isLoaded(targetPos)) return;

            BlockState targetState = level.getBlockState(targetPos);
            BlockState belowState = level.getBlockState(targetPos.below());

            if (targetState.isAir() && belowState.is(net.minecraft.world.level.block.Blocks.FARMLAND)) {
                level.setBlock(targetPos, this.defaultBlockState(), 3);
            }
        }

        if (random.nextInt(20) == 0) {
            var weed = com.miophas.singularity_iteration.core.runtime.crop.PlantRegistry.instance.getPlant("mio_icif", "weed");
            if (weed == null) return;
            for (BlockPos offset : new BlockPos[]{pos.north(), pos.south(), pos.east(), pos.west()}) {
                if (!level.isLoaded(offset)) continue;
                if (level.getBlockEntity(offset) instanceof com.miophas.singularity_iteration.core.api.crop.IPlanter planter) {
                    if (planter.getWeedControl() > 0) continue;

                    if (planter.getPlant() == null) {
                        becomeWeed(planter, weed);
                        break;
                    } else if (!planter.getPlant().getTypeId().equals("weed")) {
                        int resistance = planter.getResilience();
                        int baseChance = 20;
                        int actualChance = Math.max(3, baseChance - resistance / 2);

                        if (random.nextInt(100) < actualChance) {
                            becomeWeed(planter, weed);
                            break;
                        }
                    }
                }
            }
        }
    }

    private static void becomeWeed(com.miophas.singularity_iteration.core.api.crop.IPlanter planter,
                                   com.miophas.singularity_iteration.core.api.crop.PlantType weed) {
        planter.reset();
        planter.setPlant(weed);
        planter.setGrowthStage(1);
        planter.updateState();
    }
}
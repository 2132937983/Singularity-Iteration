package com.singularity_iteration.mio_icif.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("null")
public class mio_icif_block_electric_light extends Block {

    private static final VoxelShape LIGHT_SHAPE = Shapes.join(
            Shapes.box(0.3D, 0.3D, 0.3D, 0.7D, 0.7D, 0.7D),
            Shapes.block(),
            BooleanOp.ONLY_FIRST
    );

    public mio_icif_block_electric_light() {
        super(Block.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .noCollission()
                .instabreak()
                .noOcclusion()
                .sound(SoundType.AMETHYST)
                .lightLevel(state -> 15));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LIGHT_SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        if (random.nextInt(3) == 0) {
            level.addParticle(
                    ParticleTypes.END_ROD,
                    x + (random.nextDouble() - 0.5) * 0.6,
                    y + (random.nextDouble() - 0.5) * 0.6,
                    z + (random.nextDouble() - 0.5) * 0.6,
                    (random.nextDouble() - 0.5) * 0.03,
                    (random.nextDouble() - 0.5) * 0.03,
                    (random.nextDouble() - 0.5) * 0.03
            );
        }
    }
}

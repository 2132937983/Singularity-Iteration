package com.miophas.singularity_iteration.common.block;

import com.miophas.singularity_iteration.common.item.tools.mio_icif_electric_lighter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible light placed by the Electric Light Generator. Like vanilla's light block it is only
 * targetable (and instantly breakable) while the player holds the generator; otherwise it has no
 * shape, is replaceable by any block or fluid and is destroyed by pistons.
 */
@SuppressWarnings("null")
public class mio_icif_block_electric_light extends Block {

    private static final VoxelShape MARKER = Block.box(5, 5, 5, 11, 11, 11);

    public mio_icif_block_electric_light() {
        super(Block.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .noCollission()
                .instabreak()
                .noOcclusion()
                .replaceable()
                .noLootTable()
                .pushReaction(PushReaction.DESTROY)
                .sound(SoundType.AMETHYST)
                .lightLevel(state -> 15));
    }

    static boolean holdingGenerator(CollisionContext context) {
        return context instanceof EntityCollisionContext ec && ec.getEntity() instanceof Player p
            && (p.getMainHandItem().getItem() instanceof mio_icif_electric_lighter
                || p.getOffhandItem().getItem() instanceof mio_icif_electric_lighter);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return holdingGenerator(context) ? MARKER : Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
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
            level.addParticle(ParticleTypes.END_ROD,
                    x + (random.nextDouble() - 0.5) * 0.6, y + (random.nextDouble() - 0.5) * 0.6, z + (random.nextDouble() - 0.5) * 0.6,
                    (random.nextDouble() - 0.5) * 0.03, (random.nextDouble() - 0.5) * 0.03, (random.nextDouble() - 0.5) * 0.03);
        }
    }
}

package com.singularity_iteration.mio_icif.Blocks.reactor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_Block_IC_TNT extends Block {
    public static final MapCodec<mio_icif_Block_IC_TNT> CODEC = simpleCodec(mio_icif_Block_IC_TNT::new);

    @Override
    public MapCodec<mio_icif_Block_IC_TNT> codec() {
        return CODEC;
    }

    public mio_icif_Block_IC_TNT(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void onCaughtFire(BlockState state, Level world, BlockPos pos, @Nullable net.minecraft.core.Direction face, @Nullable LivingEntity igniter) {
        explode(world, pos, igniter);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock())) {
            if (level.hasNeighborSignal(pos)) {
                onCaughtFire(state, level, pos, null, null);
                level.removeBlock(pos, false);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.hasNeighborSignal(pos)) {
            onCaughtFire(state, level, pos, null, null);
            level.removeBlock(pos, false);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide) {
            mio_icif_Entity_IC_TNT_Primed primedTnt = new mio_icif_Entity_IC_TNT_Primed(
                level, (double) pos.getX() + 0.5, (double) pos.getY(), (double) pos.getZ() + 0.5, explosion.getIndirectSourceEntity()
            );
            int i = primedTnt.getFuse();
            primedTnt.setFuse((short) (level.random.nextInt(i / 4) + i / 8));
            level.addFreshEntity(primedTnt);
        }
    }

    private static void explode(Level level, BlockPos pos, @Nullable LivingEntity entity) {
        if (!level.isClientSide) {
            mio_icif_Entity_IC_TNT_Primed primedTnt = new mio_icif_Entity_IC_TNT_Primed(level, (double) pos.getX() + 0.5, (double) pos.getY(), (double) pos.getZ() + 0.5, entity);
            level.addFreshEntity(primedTnt);
            level.playSound(null, primedTnt.getX(), primedTnt.getY(), primedTnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(entity, GameEvent.PRIME_FUSE, pos);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        onCaughtFire(state, level, pos, hitResult.getDirection(), player);
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 11);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide) {
            BlockPos blockpos = hit.getBlockPos();
            Entity entity = projectile.getOwner();
            if (projectile.isOnFire() && projectile.mayInteract(level, blockpos)) {
                onCaughtFire(state, level, blockpos, null, entity instanceof LivingEntity ? (LivingEntity) entity : null);
                level.removeBlock(blockpos, false);
            }
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }
}



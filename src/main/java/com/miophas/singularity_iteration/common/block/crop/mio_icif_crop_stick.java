// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.block.crop;

import com.mojang.serialization.MapCodec;
import com.miophas.singularity_iteration.common.blockentity.crop.mio_icif_crop_entity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Independent crop-stick block with a seven-stage synchronized render property. */
public class mio_icif_crop_stick extends BaseEntityBlock {
    public static final MapCodec<mio_icif_crop_stick> CODEC = simpleCodec(mio_icif_crop_stick::new);
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty CROSSING = net.minecraft.world.level.block.state.properties.BooleanProperty.create("crossing");
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    public mio_icif_crop_stick(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CROSSING, false).setValue(AGE, 0));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new mio_icif_crop_entity(pos, state); }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_crop_entity crop) crop.tick(tickLevel, pos, tickState, crop);
        };
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(net.minecraft.world.level.block.Blocks.FARMLAND);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AGE, 0);
    }

    @Override public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }
    @Override public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) { return 1.0F; }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        if (!state.canSurvive(level, pos)) level.destroyBlock(pos, true);
    }

    // NeoForge uses this to discard empty chunk sections, not to test whether a seed is planted.
    @Override public boolean isEmpty(BlockState state) { return false; }
    public int getAge(BlockState state) { return state.getValue(AGE); }
    public int getMaxAge() { return 7; }
    public BlockState withAge(int age) { return defaultBlockState().setValue(AGE, Math.clamp(age, 0, getMaxAge())); }
    public boolean isMaxAge(BlockState state) { return getAge(state) >= getMaxAge(); }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        return com.miophas.singularity_iteration.common.crop.CropInteractions.use(stack, state, level, pos, player, hand, hit);
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        harvestAfterPermission(state, level, pos, player);
        super.attack(state, level, pos, player);
    }

    static void harvestAfterPermission(BlockState state, Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)
                || !server.hasChunkAt(pos) || !server.mayInteract(player, pos)
                || player.blockActionRestricted(server, pos, serverPlayer.gameMode.getGameModeForPlayer())
                || server.getBlockState(pos) != state
                || !(server.getBlockEntity(pos) instanceof mio_icif_crop_entity crop)) return;
        // attack precedes the normal break event; ask protection before changing the crop or its drops.
        var event = new BlockEvent.BreakEvent(server, pos, state, player);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled() || !server.hasChunkAt(pos) || server.getBlockState(pos) != state
                || server.getBlockEntity(pos) != crop) return;
        if (crop.getPlant() != null) crop.getPlant().onLeftClick(crop, player);
        else if (crop.isHybridBase()) {
            crop.setHybridBase(false); crop.updateState();
            Block.popResource(level, pos, new ItemStack(state.getBlock()));
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CROSSING, AGE);
    }

    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        return com.miophas.singularity_iteration.common.crop.CropInteractions.emptyHand(state, level, pos, player);
    }
    @Override protected boolean isSignalSource(BlockState state) { return true; }
    @Override protected int getSignal(BlockState state, BlockGetter world, BlockPos pos, net.minecraft.core.Direction direction) {
        return world.getBlockEntity(pos) instanceof mio_icif_crop_entity crop && crop.getPlant() != null ? crop.getPlant().getRedstoneSignal(crop) : 0;
    }
    @Override public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof mio_icif_crop_entity crop && crop.getPlant() != null ? crop.getPlant().getLightEmission(crop) : 0;
    }
    @Override protected void entityInside(BlockState state, Level world, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        if (!world.isClientSide && world.getBlockEntity(pos) instanceof mio_icif_crop_entity crop && crop.getPlant() != null
                && crop.getPlant().onCollision(crop, entity) && world.random.nextInt(100) == 0
                && world.random.nextInt(40) > crop.getResilience()) {
            crop.reset(); crop.updateState(); world.setBlockAndUpdate(pos.below(), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
        }
    }
    @Override protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !world.isClientSide && world.getBlockEntity(pos) instanceof mio_icif_crop_entity crop) {
            if (crop.isHybridBase()) Block.popResource(world, pos, new ItemStack(state.getBlock()));
            crop.pick();
        }
        super.onRemove(state, world, pos, replacement, moving);
    }
    @Override public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target,
            LevelReader world, BlockPos pos, Player player) {
        if (world.getBlockEntity(pos) instanceof mio_icif_crop_entity crop && crop.getPlant() != null)
            return crop.makeSeeds(crop.getPlant(), 1, crop.getGrowthSpeed(), crop.getYield(), crop.getResilience());
        return new ItemStack(this);
    }
}

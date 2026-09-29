package com.miophas.singularity_iteration.common.block.pipe;

import com.miophas.singularity_iteration.common.blockentity.pipe.FluidPipeBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.core.api.transport.PipeGrade;
import com.miophas.singularity_iteration.core.api.transport.PipeSize;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two material blocks, each with four sizes. The legacy water pipe retains its ID. */
public final class FluidPipeBlock extends mio_icif_block_pipe_water {
    public static final EnumProperty<PipeSize> SIZE = EnumProperty.create("size", PipeSize.class);
    private static final BooleanProperty[] FACES = {DOWN, UP, NORTH, SOUTH, WEST, EAST};
    public static final MapCodec<FluidPipeBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        propertiesCodec(), StringRepresentable.fromEnum(PipeGrade::values).fieldOf("grade")
            .forGetter(FluidPipeBlock::grade)).apply(instance, FluidPipeBlock::new));
    private final PipeGrade grade;

    public FluidPipeBlock(Properties properties, PipeGrade grade) {
        super(properties);
        this.grade = grade;
        registerDefaultState(defaultBlockState().setValue(SIZE, PipeSize.MEDIUM));
    }

    public PipeGrade grade() { return grade; }
    @Override protected MapCodec<FluidPipeBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST, SIZE);
    }

    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double min = (16 - state.getValue(SIZE).diameterPixels()) / 2.0, max = 16 - min;
        VoxelShape shape = Block.box(min, min, min, max, max, max);
        for (Direction side : Direction.values()) {
            if (!state.getValue(FACES[side.ordinal()])) continue;
            shape = Shapes.or(shape, Block.box(
                side == Direction.WEST ? 0 : min, side == Direction.DOWN ? 0 : min, side == Direction.NORTH ? 0 : min,
                side == Direction.EAST ? 16 : max, side == Direction.UP ? 16 : max, side == Direction.SOUTH ? 16 : max));
        }
        return shape;
    }

    /** Wrench hits use the actual diameter, including tiny and large centre faces. */
    public static Direction hitSide(BlockState state, BlockPos pos, Vec3 hit, Direction clickedFace) {
        double min = (16 - state.getValue(SIZE).diameterPixels()) / 32.0, max = 1 - min;
        Vec3 local = hit.subtract(pos.getX(), pos.getY(), pos.getZ());
        double epsilon = 1.0e-5;
        if (local.x < min - epsilon) return Direction.WEST;
        if (local.x > max + epsilon) return Direction.EAST;
        if (local.y < min - epsilon) return Direction.DOWN;
        if (local.y > max + epsilon) return Direction.UP;
        if (local.z < min - epsilon) return Direction.NORTH;
        if (local.z > max + epsilon) return Direction.SOUTH;
        return clickedFace;
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FluidPipeBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, mio_icif_block_entities.FLUID_PIPE_ENTITY_TYPE.get(),
            (world, pos, current, pipe) -> FluidPipeBlockEntity.tick(world, pos, current, pipe));
    }

    @Override public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(mio_icif_blocks.fluidPipeItem(grade, state.getValue(SIZE)));
    }

    @Override public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(mio_icif_blocks.fluidPipeItem(grade, state.getValue(SIZE)));
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        // IC2 spills a world source only above one bucket; the item never carries fluid.
        // Defer until the enclosing block removal completes, avoiding reentrant replacement.
        var contents = !state.is(next.getBlock()) && next.isAir()
            && level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe
                ? pipe.getFluid() : net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        super.onRemove(state, level, pos, next, moved);
        if (contents.getAmount() > 1000 && level instanceof net.minecraft.server.level.ServerLevel server) {
            BlockPos at = pos.immutable();
            server.getServer().tell(new net.minecraft.server.TickTask(server.getServer().getTickCount(), () -> {
                if (server.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4) == null
                        || !server.getBlockState(at).isAir()) return;
                var source = new net.neoforged.neoforge.fluids.capability.templates.FluidTank(1000);
                source.setFluid(contents.copyWithAmount(1000));
                net.neoforged.neoforge.fluids.FluidUtil.tryPlaceFluid(null, server,
                    net.minecraft.world.InteractionHand.MAIN_HAND, at, source, source.getFluid().copy());
            }));
        }
    }
}

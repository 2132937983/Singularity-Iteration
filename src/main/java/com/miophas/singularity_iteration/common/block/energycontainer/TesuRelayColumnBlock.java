package com.miophas.singularity_iteration.common.block.energycontainer;

import com.miophas.singularity_iteration.common.item.normal.TesuConnectorItem;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.167: the two blocks of the TESU relay pylon above the relay (the shaft and the head). They have no item and drop
 * nothing; they stand only on their relay (a middle part on the relay, the top on the middle part) and go when it goes. Breaking
 * one breaks the relay, which drops itself as the player's tool allows; a click on one is a click on the relay.
 */
@SuppressWarnings("null")
public class TesuRelayColumnBlock extends Block {
    public enum Part implements StringRepresentable {
        MIDDLE("middle"), TOP("top");

        private final String name;

        Part(String name) { this.name = name; }

        @Override public String getSerializedName() { return name; }
    }

    public static final MapCodec<TesuRelayColumnBlock> CODEC = simpleCodec(TesuRelayColumnBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final VoxelShape MIDDLE_SHAPE = Block.box(4.5, 0, 4.5, 11.5, 16, 11.5);
    private static final VoxelShape TOP_SHAPE = Shapes.or(Block.box(4.5, 0, 4.5, 11.5, 14.5, 11.5), Block.box(5.5, 14.5, 5.5, 10.5, 16, 10.5));

    public TesuRelayColumnBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, Part.MIDDLE).setValue(POWERED, false));
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, POWERED);
    }

    /** Places the column on the relay at {@code base} (both blocks above it, which the caller has found free). */
    public static void place(LevelAccessor level, BlockPos base, boolean powered) {
        BlockState column = mio_icif_blocks.TESU_RELAY_COLUMN.get().defaultBlockState().setValue(POWERED, powered);
        level.setBlock(base.above(), column.setValue(PART, Part.MIDDLE), Block.UPDATE_ALL);
        level.setBlock(base.above(2), column.setValue(PART, Part.TOP), Block.UPDATE_ALL);
    }

    /** Whether the column stands on the relay at {@code base}. */
    public static boolean standsOn(BlockGetter level, BlockPos base) {
        BlockState middle = level.getBlockState(base.above()), top = level.getBlockState(base.above(2));
        return isPart(middle, Part.MIDDLE) && isPart(top, Part.TOP);
    }

    /** Takes the column off the relay at {@code base} (nothing drops). */
    public static void clear(Level level, BlockPos base) {
        for (int i = 2; i >= 1; i--) {
            BlockPos at = base.above(i);
            if (level.getBlockState(at).is(mio_icif_blocks.TESU_RELAY_COLUMN.get()))
                level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
    }

    /** Lights or darkens the column on the relay at {@code base} with it. */
    public static void power(Level level, BlockPos base, boolean powered) {
        for (int i = 1; i <= 2; i++) {
            BlockPos at = base.above(i);
            BlockState state = level.getBlockState(at);
            if (state.is(mio_icif_blocks.TESU_RELAY_COLUMN.get()) && state.getValue(POWERED) != powered)
                level.setBlock(at, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean isPart(BlockState state, Part part) {
        return state.is(mio_icif_blocks.TESU_RELAY_COLUMN.get()) && state.getValue(PART) == part;
    }

    /** The relay of the pylon {@code pos} belongs to: {@code pos} itself for a relay, the relay below for a column part; or null. */
    @Nullable
    public static BlockPos base(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(mio_icif_blocks.TESU_RELAY.get())) return pos;
        if (!state.is(mio_icif_blocks.TESU_RELAY_COLUMN.get())) return null;
        BlockPos base = pos.below(state.getValue(PART) == Part.MIDDLE ? 1 : 2);
        return level.getBlockState(base).is(mio_icif_blocks.TESU_RELAY.get()) ? base : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PART) == Part.MIDDLE ? MIDDLE_SHAPE : TOP_SHAPE;
    }

    /** A middle part needs its relay below and the top above; the top needs the middle part below. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Part part = state.getValue(PART);
        if (direction == Direction.DOWN) {
            boolean held = part == Part.MIDDLE ? neighbor.is(mio_icif_blocks.TESU_RELAY.get()) : isPart(neighbor, Part.MIDDLE);
            if (!held) return Blocks.AIR.defaultBlockState();
        } else if (direction == Direction.UP && part == Part.MIDDLE && !isPart(neighbor, Part.TOP)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    /** Breaking a column part breaks the relay: it drops itself when the player's tool could take the relay. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos base = base(level, pos);
            if (base != null) {
                BlockState relay = level.getBlockState(base);
                level.destroyBlock(base, !player.isCreative() && player.hasCorrectToolForDrops(relay), player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(mio_icif_blocks.TESU_RELAY.get());
    }

    /** The connector's click goes to the connector (it finds the relay from here); any other click is the relay's. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (stack.getItem() instanceof TesuConnectorItem) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos base = base(level, pos);
        if (base == null) return InteractionResult.PASS;
        return level.getBlockState(base).useWithoutItem(level, player, hit.withPosition(base));
    }
}

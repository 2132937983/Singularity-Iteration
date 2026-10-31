package com.miophas.singularity_iteration.common.block.generator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 0.1.10.41: a Steam Turbine Rotor, in six tiers (bronze, iron, refined iron, titanium, iridium, netherite). Up to three go into the hollow inside of a Steam Tunnel; each adds its
 * {@link Tier#bonusEu} to every step of the tunnel (1,024 mB of steam), on top of the 512 EU it makes without rotors. Hidden in the
 * tunnel, it has no animation. It faces the player who places it, in any of the six directions (its model is the same front and
 * back, so it shows the axis); the tunnel counts it in any direction.
 */
@SuppressWarnings("null")
public class mio_icif_block_steam_turbine_rotor extends Block {
    /** The tiers and the EU each rotor adds to a step: iron is the standard one (+64 EU, +12.5%). */
    public enum Tier {
        BRONZE(32), IRON(64), ADVIRON(80), TITANIUM(96), IRIDIUM(128), NETHERITE(160);

        private final int bonusEu;

        Tier(int bonusEu) { this.bonusEu = bonusEu; }

        /** EU this rotor adds to each step of the tunnel. */
        public int bonusEu() { return bonusEu; }

        /** The tier in block ids and textures ({@code bronze} ... {@code netherite}; refined iron is {@code adviron}). */
        public String id() { return name().toLowerCase(Locale.ROOT); }
    }

    /** The EU of a step without rotors, the base the bonus is a share of. */
    public static final int BASE_STEP_EU = 512;
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape SHAPE_Z = Block.box(1, 1, 0, 15, 15, 16), SHAPE_X = Block.box(0, 1, 1, 16, 15, 15),
        SHAPE_Y = Block.box(1, 0, 1, 15, 16, 15);

    private final Tier tier;

    public mio_icif_block_steam_turbine_rotor(Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    public Tier tier() { return tier; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING).getAxis()) {
            case X -> SHAPE_X;
            case Y -> SHAPE_Y;
            case Z -> SHAPE_Z;
        };
    }

    /** Placed or removed inside a Steam Tunnel: its controller counts the rotors again at once. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean movedByPiston) {
        super.onPlace(state, level, pos, old, movedByPiston);
        if (!old.is(this)) com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelBlockEntity.partRemoved(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean replaced = !state.is(newState.getBlock());
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (replaced) com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelBlockEntity.partRemoved(level, pos);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mio_icif.steam_turbine_rotor.bonus", tier.bonusEu(), percent(tier.bonusEu())).withStyle(ChatFormatting.GRAY));
    }

    /** A bonus in EU per step as a share of the 512 EU of a step without rotors, without trailing zeros ({@code 12.5}). */
    public static String percent(long bonusEu) {
        return BigDecimal.valueOf(bonusEu * 100.0 / BASE_STEP_EU).stripTrailingZeros().toPlainString();
    }
}

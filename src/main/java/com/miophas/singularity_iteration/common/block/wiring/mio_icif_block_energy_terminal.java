package com.miophas.singularity_iteration.common.block.wiring;

import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.miophas.singularity_iteration.common.blockentity.wiring.terminal.EnergyTerminalBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/** Energy Management Terminal: console desk that monitors and switches its cable network. */
@SuppressWarnings("null")
public class mio_icif_block_energy_terminal extends mio_icif_entity_block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final MapCodec<mio_icif_block_energy_terminal> CODEC = simpleCodec(mio_icif_block_energy_terminal::new);

    private static final Map<net.minecraft.core.Direction, VoxelShape> SHAPES = new EnumMap<>(net.minecraft.core.Direction.class);
    static {
        // modelled facing north; the monitor sits toward the back (+Z)
        for (var dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            SHAPES.put(dir, Shapes.or(rotate(0, 0, 1, 16, 8.5, 16, dir), rotate(1.5, 8.5, 8, 14.5, 16, 14, dir)));
        }
    }

    private static VoxelShape rotate(double x0, double y0, double z0, double x1, double y1, double z1, net.minecraft.core.Direction facing) {
        return switch (facing) {
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    public mio_icif_block_energy_terminal(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.getOrDefault(state.getValue(FACING), SHAPES.get(net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EnergyTerminalBlockEntity terminal) {
            player.openMenu(terminal, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, neighbor, from, moving);
        if (level.getBlockEntity(pos) instanceof EnergyTerminalBlockEntity terminal) terminal.invalidateTopology();
    }

    @Override
    protected void addBlockTooltip(net.minecraft.world.item.ItemStack stack, java.util.List<net.minecraft.network.chat.Component> tooltip) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.mio_icif.energy_terminal").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyTerminalBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return createTickerHelper(type, mio_icif_block_entities.ENERGY_TERMINAL.get(),
                (lvl, pos, st, be) -> com.miophas.singularity_iteration.common.client.terminal.EnergyTerminalClient.tick(lvl, pos, st, be));
        }
        return createTickerHelper(type, mio_icif_block_entities.ENERGY_TERMINAL.get(), EnergyTerminalBlockEntity::serverTick);
    }
}

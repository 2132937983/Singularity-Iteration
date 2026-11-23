package com.miophas.singularity_iteration.common.block.energycontainer;

import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.miophas.singularity_iteration.common.blockentity.batbox.TesuControllerBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.batbox.TesuInputPortBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.batbox.TesuOutputPortBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyPart;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.157: the TESU ports, one class for the two: the input port (any voltage in) and the output port (a right click steps
 * its voltage up, a sneaking right click down, IV to MAX). Taking a port out tells its controller at once.
 */
@SuppressWarnings("null")
public class TesuPortBlock extends mio_icif_entity_block {
    public static final MapCodec<TesuPortBlock> INPUT_CODEC = simpleCodec(p -> new TesuPortBlock(p, false));
    public static final MapCodec<TesuPortBlock> OUTPUT_CODEC = simpleCodec(p -> new TesuPortBlock(p, true));

    private final boolean output;

    public TesuPortBlock(Properties properties, boolean output) {
        super(properties);
        this.output = output;
    }

    public boolean isOutput() { return output; }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return output ? OUTPUT_CODEC : INPUT_CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return output ? new TesuOutputPortBlockEntity(pos, state) : new TesuInputPortBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (l, p, s, be) -> {
            if (be instanceof TesuOutputPortBlockEntity out) TesuOutputPortBlockEntity.tick(l, p, s, out);
            else if (be instanceof TesuInputPortBlockEntity in) TesuInputPortBlockEntity.tick(l, p, s, in);
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!output) return InteractionResult.PASS;
        // 0.1.11.191: the screen sets the output in EU/t (the click stepped the voltage)
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TesuOutputPortBlockEntity port)
            player.openMenu(port, buf -> buf.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide() && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MultiblockEnergyPart part) {
            BlockPos core = part.getCorePosition();
            part.detachFromCore();
            if (core != null && level.isLoaded(core) && level.getBlockEntity(core) instanceof TesuControllerBlockEntity controller) controller.check();
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected void addBlockTooltip(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.translatable(output ? "tooltip.mio_icif.tesu.output" : "tooltip.mio_icif.tesu.input").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.tesu.port_slot").withStyle(ChatFormatting.YELLOW));
    }
}

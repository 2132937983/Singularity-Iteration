package com.miophas.singularity_iteration.common.item.transport;

import com.miophas.singularity_iteration.common.block.pipe.FluidPipeBlock;
import com.miophas.singularity_iteration.common.blockentity.pipe.FluidPipeBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.pipe.mio_icif_pipe_fluid;
import com.miophas.singularity_iteration.core.api.transport.PipeSize;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import java.util.List;
import java.util.Map;

public final class FluidPipeItem extends BlockItem {
    private final PipeSize size;
    public FluidPipeItem(FluidPipeBlock block, PipeSize size) {
        super(block, new Item.Properties());
        this.size = size;
    }

    public PipeSize size() { return size; }
    @Override public String getDescriptionId() { return getOrCreateDescriptionId(); }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(FluidPipeBlock.SIZE, size);
    }

    @Override public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (result.consumesAction() && !level.isClientSide && level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            var side = context.getClickedFace().getOpposite();
            var adjacent = pos.relative(side);
            if (level.getChunkSource().hasChunk(adjacent.getX() >> 4, adjacent.getZ() >> 4)
                    && (level.getBlockEntity(adjacent) instanceof mio_icif_pipe_fluid
                        || level.getCapability(Capabilities.FluidHandler.BLOCK, adjacent, side.getOpposite()) != null)) {
                pipe.unblockDirection(side);
                if (level.getBlockEntity(adjacent) instanceof mio_icif_pipe_fluid other) other.unblockDirection(side.getOpposite());
            }
        }
        return result;
    }

    // There is only one vanilla block -> item mapping; make its fallback deterministic.
    @Override public void registerBlocks(Map<Block, Item> map, Item item) {
        if (size == PipeSize.MEDIUM) super.registerBlocks(map, item);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        var grade = ((FluidPipeBlock) getBlock()).grade();
        lines.add(Component.translatable("tooltip.mio_icif.fluid_pipe.capacity", grade.capacity(size)));
        lines.add(Component.translatable("tooltip.mio_icif.fluid_pipe.rate", grade.transferPerTick(size)));
        lines.add(Component.translatable("tooltip.mio_icif.fluid_pipe.connection"));
    }
}

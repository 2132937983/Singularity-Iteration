package com.singularity_iteration.mio_icif.client.render;

import com.singularity_iteration.mio_icif.Blocks.Wire.mio_icif_block_wire;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_wire;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@SuppressWarnings({"null", "deprecation"})
public class WireDisguiseRenderer implements BlockEntityRenderer<mio_icif_wire> {

    private final BlockRenderDispatcher blockRenderer;

    public WireDisguiseRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(mio_icif_wire blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        Level level = blockEntity.getLevel();
        if (level == null) return;

        BlockState wireState = blockEntity.getBlockState();
        if (!(wireState.getBlock() instanceof mio_icif_block_wire)) return;

        boolean disguised = wireState.hasProperty(mio_icif_block_wire.DISGUISED)
                && wireState.getValue(mio_icif_block_wire.DISGUISED);
        boolean foamlogged = wireState.hasProperty(mio_icif_block_wire.FOAMLOGGED)
                && wireState.getValue(mio_icif_block_wire.FOAMLOGGED);

        if (!disguised) {
            return;
        }

        poseStack.pushPose();

        if (foamlogged && blockEntity.hasDisguise()) {
            Block disguisedBlock = blockEntity.getDisguisedBlock();
            if (disguisedBlock != null && disguisedBlock != Blocks.AIR) {
                BlockState disguisedState = disguisedBlock.defaultBlockState();
                blockRenderer.renderSingleBlock(
                        disguisedState,
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay
                );
            }
        }

        blockRenderer.renderSingleBlock(
                wireState,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    public static void registerAll(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.WIRE_LV_ENTITY_TYPE.get(),
            WireDisguiseRenderer::new
        );
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.WIRE_MV_ENTITY_TYPE.get(),
            WireDisguiseRenderer::new
        );
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.WIRE_HV_ENTITY_TYPE.get(),
            WireDisguiseRenderer::new
        );
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.WIRE_EV_ENTITY_TYPE.get(),
            WireDisguiseRenderer::new
        );
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.WIRE_IV_ENTITY_TYPE.get(),
            WireDisguiseRenderer::new
        );
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.WIRE_LuV_ENTITY_TYPE.get(),
            WireDisguiseRenderer::new
        );
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.WIRE_ISOLATION_LV_ENTITY_TYPE.get(),
            WireDisguiseRenderer::new
        );
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.WIRE_ISOLATION_MV_ENTITY_TYPE.get(),
            WireDisguiseRenderer::new
        );
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.WIRE_ISOLATION_HV_ENTITY_TYPE.get(),
            WireDisguiseRenderer::new
        );
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.WIRE_ISOLATION_EV_ENTITY_TYPE.get(),
            WireDisguiseRenderer::new
        );
    }
}
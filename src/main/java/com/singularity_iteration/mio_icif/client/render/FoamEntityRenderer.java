package com.singularity_iteration.mio_icif.client.render;

import com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_block_foam_entity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 建筑泡沫方块实体渲染�?
 * 只在有伪装时渲染伪装方块的外�?
 * 无伪装时由默认的MODEL渲染方式处理（RenderShape.MODEL�?
 */
@SuppressWarnings("null")
public class FoamEntityRenderer implements BlockEntityRenderer<mio_icif_block_foam_entity> {

    private final BlockRenderDispatcher blockRenderer;

    public FoamEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void render(mio_icif_block_foam_entity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (!blockEntity.hasDisguise()) {
            return;
        }

        Block disguisedBlock = blockEntity.getDisguisedBlock();
        if (disguisedBlock == null || disguisedBlock == Blocks.AIR) {
            return;
        }

        Level level = blockEntity.getLevel();
        if (level == null) return;

        BlockState disguisedState = disguisedBlock.defaultBlockState();
        int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, blockEntity.getBlockPos());

        poseStack.pushPose();
        blockRenderer.renderSingleBlock(
            disguisedState,
            poseStack,
            bufferSource,
            light,
            packedOverlay
        );
        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public boolean shouldRenderOffScreen(mio_icif_block_foam_entity blockEntity) {
        return false;
    }
}


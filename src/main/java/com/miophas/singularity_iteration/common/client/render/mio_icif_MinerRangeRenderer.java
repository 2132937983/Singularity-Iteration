package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import com.miophas.singularity_iteration.common.blockentity.producer.MiningRangeProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/**
 * 采矿机工作范围渲染器。
 *
 * <p>在基础/高级采矿机四周渲染一圈黄黑相间的垂直边界：边界为一格高、位于机器上方
 * 一格的带子，水平范围由机器内安装的扫描器决定（由
 * {@link MiningRangeProvider#getMiningRangeRadius()} 提供），从而直观标示出机器的
 * 水平工作范围。
 *
 * <p>可在配置文件中通过 {@code MinerRange.enableRangeDisplay} 关闭该渲染效果。
 */
@SuppressWarnings("null")
public class mio_icif_MinerRangeRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    // 黄黑相间的警示配色
    private static final float YELLOW_R = 1.0F;
    private static final float YELLOW_G = 0.85F;
    private static final float YELLOW_B = 0.05F;
    private static final float BLACK_R = 0.04F;
    private static final float BLACK_G = 0.04F;
    private static final float BLACK_B = 0.04F;
    // 边界整体透明度，保持与边境之墙类似的半透明观感
    private static final float ALPHA = 0.5F;

    public mio_icif_MinerRangeRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** 供方块实体渲染器注册使用的工厂方法，便于同一渲染器注册到多种采矿机。 */
    public static <T extends BlockEntity> BlockEntityRendererProvider<T> provider() {
        return mio_icif_MinerRangeRenderer::new;
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        if (!(blockEntity instanceof MiningRangeProvider provider)) return;
        if (!Singularity_Iteration_Config.MINER_RANGE_DISPLAY_ENABLED.get()) return;

        int radius = provider.getMiningRangeRadius();
        if (radius <= 0) return;

        Level level = blockEntity.getLevel();
        if (level == null) return;

        BlockPos origin = blockEntity.getBlockPos();
        // 方块坐标为局部坐标，渲染器原点即机器方块原点。
        // 边界为一格高的带子，位置在机器上方一格（局部 y = 1..2）。
        float yBottom = 1.0F;
        float yTop = 2.0F;
        // 向内收缩 INSET，避免边界平面与外侧方块面完全重合而产生面闪烁
        float inset = 0.01F;
        float x0 = -radius + inset;
        float x1 = radius + 1 - inset;
        float z0 = -radius + inset;
        float z1 = radius + 1 - inset;
        int span = 2 * radius + 1;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugQuads());
        Matrix4f matrix = poseStack.last().pose();

        // 南北两面墙壁（沿 X 轴展开），按世界坐标奇偶交替黄黑
        for (int i = 0; i < span; i++) {
            float xa = x0 + i;
            float xb = Math.min(xa + 1.0F, x1);
            boolean yellow = ((origin.getX() - radius + i) & 1) == 0;
            addVerticalQuad(consumer, matrix, xa, z0, xb, z0, yBottom, yTop, yellow);
            addVerticalQuad(consumer, matrix, xa, z1, xb, z1, yBottom, yTop, yellow);
        }
        // 东西两面墙壁（沿 Z 轴展开），按世界坐标奇偶交替黄黑
        for (int j = 0; j < span; j++) {
            float za = z0 + j;
            float zb = Math.min(za + 1.0F, z1);
            boolean yellow = ((origin.getZ() - radius + j) & 1) == 0;
            addVerticalQuad(consumer, matrix, x0, za, x0, zb, yBottom, yTop, yellow);
            addVerticalQuad(consumer, matrix, x1, za, x1, zb, yBottom, yTop, yellow);
        }
    }

    /** 绘制一段垂直于地面的四边形（墙片）。 */
    private static void addVerticalQuad(VertexConsumer consumer, Matrix4f matrix,
                                        float x1, float z1, float x2, float z2,
                                        float bottom, float top, boolean yellow) {
        float r = yellow ? YELLOW_R : BLACK_R;
        float g = yellow ? YELLOW_G : BLACK_G;
        float b = yellow ? YELLOW_B : BLACK_B;

        consumer.addVertex(matrix, x1, bottom, z1).setColor(r, g, b, ALPHA);
        consumer.addVertex(matrix, x2, bottom, z2).setColor(r, g, b, ALPHA);
        consumer.addVertex(matrix, x2, top, z2).setColor(r, g, b, ALPHA);
        consumer.addVertex(matrix, x1, top, z1).setColor(r, g, b, ALPHA);
    }

    @Override
    public int getViewDistance() {
        // 高级采矿机工作半径最大可达 32 格，需要更大的可视距离以免边界被提前裁剪
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        // 边界可能延伸出机器所在方块，忽略基于方块本体的视锥裁剪
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        if (!(blockEntity instanceof MiningRangeProvider provider)) {
            return BlockEntityRenderer.super.getRenderBoundingBox(blockEntity);
        }
        int radius = Math.max(0, provider.getMiningRangeRadius());
        BlockPos pos = blockEntity.getBlockPos();
        // 覆盖整圈边界（含机器上方一格），否则边界移出视野时会被裁剪掉
        return new AABB(
            pos.getX() - radius, pos.getY(), pos.getZ() - radius,
            pos.getX() + radius + 1, pos.getY() + 2, pos.getZ() + radius + 1
        );
    }
}

package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.blockentity.wiring.mio_icif_wireless_power_transmission_node;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

@SuppressWarnings("null")
public class WirelessTransmissionNodeRenderer implements BlockEntityRenderer<mio_icif_wireless_power_transmission_node> {

    private static final int PARTICLES_PER_TICK = 6;
    private static final float PARTICLE_SIZE = 0.06f;
    private static final float COLOR_R = 0.0f;
    private static final float COLOR_G = 0.9f;
    private static final float COLOR_B = 1.0f;
    private static final float COLOR_A = 0.7f;


    public WirelessTransmissionNodeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(mio_icif_wireless_power_transmission_node blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!blockEntity.isTransmitting()) return;

        BlockPos targetPos = blockEntity.getTargetPosition();
        if (targetPos == null) return;
        renderStream(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockPos(), targetPos, partialTick, poseStack, bufferSource);
    }

    /**
     * 0.1.11.158: the stream of particles from {@code from} to {@code to}, drawn by the block entity at {@code origin} (its pose
     * stands at that block); the TESU relays draw theirs with it (TesuRelayRenderer).
     */
    public static void renderStream(@org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level, BlockPos origin, BlockPos from,
                                    BlockPos to, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        renderStream(level, origin, net.minecraft.world.phys.Vec3.atCenterOf(from), net.minecraft.world.phys.Vec3.atCenterOf(to),
            partialTick, poseStack, bufferSource);
    }

    /** 0.1.11.167: the stream between two points (the TESU relay pylons send and take it at their emitters, near their tops). */
    public static void renderStream(@org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level, BlockPos origin,
                                    net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 to, float partialTick,
                                    PoseStack poseStack, MultiBufferSource bufferSource) {
        BlockPos sourcePos = origin;
        double sourceX = from.x;
        double sourceY = from.y;
        double sourceZ = from.z;

        double targetX = to.x;
        double targetY = to.y;
        double targetZ = to.z;

        double dx = targetX - sourceX;
        double dy = targetY - sourceY;
        double dz = targetZ - sourceZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.1) return;

        // 0.1.11.22: game time, not a counter bumped on each frame (the beam ran faster at a higher frame rate); three
        // steps a tick keeps the speed it had at 60 frames a second
        float tickCounter = level == null ? 0
            : (float) ((level.getGameTime() % 72000L) + partialTick) * 3.0f;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugQuads());

        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            float progress = ((tickCounter + i * 7) % 60f) / 60.0f;

            double px = sourceX + dx * progress - sourcePos.getX();
            double py = sourceY + dy * progress - sourcePos.getY();
            double pz = sourceZ + dz * progress - sourcePos.getZ();

            float wobbleX = (float) (Math.sin((tickCounter + i) * 0.3) * 0.05);
            float wobbleY = (float) (Math.cos((tickCounter + i) * 0.4) * 0.05);
            float wobbleZ = (float) (Math.sin((tickCounter + i) * 0.5 + 1.0) * 0.05);

            float x = (float) px + wobbleX;
            float y = (float) py + wobbleY;
            float z = (float) pz + wobbleZ;

            float alpha = COLOR_A * (1.0f - Math.abs(progress - 0.5f) * 2.0f) * 0.6f + COLOR_A * 0.4f;

            renderParticleQuad(poseStack, consumer, x, y, z, PARTICLE_SIZE, alpha);
        }
    }

    /**
     * debugQuads 的顶点格式是 POSITION_COLOR，只写位置与颜色；
     * 额外调用 setUv/setLight/setNormal 与格式不符（同样的写法可参考 mio_icif_MinerRangeRenderer）。
     */
    private static void renderParticleQuad(PoseStack poseStack, VertexConsumer consumer,
                                    float cx, float cy, float cz, float size, float alpha) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();

        int r = Mth.floor(COLOR_R * 255);
        int g = Mth.floor(COLOR_G * 255);
        int b = Mth.floor(COLOR_B * 255);
        int a = Mth.floor(alpha * 255);

        consumer.addVertex(matrix, cx - size, cy - size, cz).setColor(r, g, b, a);
        consumer.addVertex(matrix, cx + size, cy - size, cz).setColor(r, g, b, a);
        consumer.addVertex(matrix, cx + size, cy + size, cz).setColor(r, g, b, a);
        consumer.addVertex(matrix, cx - size, cy + size, cz).setColor(r, g, b, a);

        consumer.addVertex(matrix, cx - size, cy, cz - size).setColor(r, g, b, a);
        consumer.addVertex(matrix, cx + size, cy, cz - size).setColor(r, g, b, a);
        consumer.addVertex(matrix, cx + size, cy, cz + size).setColor(r, g, b, a);
        consumer.addVertex(matrix, cx - size, cy, cz + size).setColor(r, g, b, a);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public boolean shouldRenderOffScreen(mio_icif_wireless_power_transmission_node blockEntity) {
        return true;
    }
}
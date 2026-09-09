package com.singularity_iteration.mio_icif.Render;

import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Water_Kinetic_Generator;
import com.singularity_iteration.mio_icif.api.item.IKineticRotor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 水力动能发生机转子渲染器
 * 在水能机前方渲染一个旋转的长方体转子
 * 模型高度为风能机转子的三分之一
 * 纹理和尺寸从转子物品属性中读取
 *
 * 光照说明（对齐IC2 1.12.2）：
 * IC2在渲染转子前会获取转子实际位置（发电机前方一格）的光照，
 * 并通过OpenGlHelper.setLightmapTextureCoords设置光照贴图坐标。
 * 本渲染器同样在转子位置采样光照，避免转子因发电机自身位置过暗而渲染为黑色。
 */
@SuppressWarnings("null")
public class mio_icif_WaterRotorRender implements BlockEntityRenderer<mio_icif_Water_Kinetic_Generator> {

    private float accumulatedRotation = 0.0f;
    private long lastRenderTime = 0;

    private static final float PIXEL_SCALE = 1.0f / 16.0f;
    private static final int TEXTURE_TOTAL_HEIGHT = 128;
    private static final float ROTOR_WIDTH = 9 * PIXEL_SCALE;
    private static final float ROTOR_DEPTH = 1 * PIXEL_SCALE;
    private static final float ROTOR_DISTANCE = 0.6f;

    public mio_icif_WaterRotorRender(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(mio_icif_Water_Kinetic_Generator blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack rotorStack = blockEntity.getRotorStack();
        if (rotorStack.isEmpty()) return;

        if (!(rotorStack.getItem() instanceof IKineticRotor rotor)) return;

        int diameter = rotor.getDiameter(rotorStack);
        ResourceLocation texture = rotor.getRotorRenderTexture(rotorStack);
        int texturePixelHeight = getTexturePixelHeight(diameter);

        // 水力发电机转子高度为风力的三分之一
        float rotorHeight = (texturePixelHeight / 3.0f) * PIXEL_SCALE;

        Direction facing = blockEntity.getBlockState().getValue(
                com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);

        // 在转子实际位置采样光照（IC2对齐：转子在发电机前方一格）
        int rotorPackedLight = getRotorPackedLight(blockEntity.getLevel(), blockEntity.getBlockPos(), facing);

        // 计算旋转速度：根据当前发电量决定转速
        int currentOutput = blockEntity.getCurrentKineticOutput();
        float rotationSpeed = currentOutput * 15.0f / 200.0f;

        long currentTime = System.currentTimeMillis();
        if (lastRenderTime == 0) lastRenderTime = currentTime;
        float deltaTime = (currentTime - lastRenderTime) / 1000.0f;
        lastRenderTime = currentTime;

        accumulatedRotation += rotationSpeed * deltaTime * 20.0f;
        accumulatedRotation = accumulatedRotation % 360.0f;
        float rotation = accumulatedRotation;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        float yRot = switch (facing) {
            case NORTH -> (float) Math.PI;
            case SOUTH -> 0.0f;
            case EAST -> (float) Math.PI / 2;
            case WEST -> -(float) Math.PI / 2;
            default -> 0.0f;
        };
        poseStack.mulPose(new org.joml.Quaternionf().rotateY(yRot));
        poseStack.translate(0, 0, ROTOR_DISTANCE);
        poseStack.mulPose(new org.joml.Quaternionf().rotateZ((float) Math.toRadians(rotation)));

        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0, rotorHeight / 2.0f, 0);
            poseStack.translate(0, -rotorHeight / 2.0f, 0);
            poseStack.mulPose(new org.joml.Quaternionf().rotateY((float) Math.toRadians(-15)));
            poseStack.translate(0, rotorHeight / 2.0f, 0);

            renderBox(poseStack, bufferSource, rotorHeight, texturePixelHeight, texture, rotorPackedLight);

            poseStack.popPose();
            poseStack.mulPose(new org.joml.Quaternionf().rotateZ((float) Math.toRadians(90)));
        }

        poseStack.popPose();
    }

    /**
     * 在转子实际位置（发电机前方一格）采样光照
     * 对齐IC2 1.12.2的 KineticGeneratorRenderer.renderBlockRotor()：
     *   int light = world.getCombinedLight(pos.offset(facing), 0);
     */
    private static int getRotorPackedLight(Level level, BlockPos pos, Direction facing) {
        if (level == null) return 15728880; // FULL_BRIGHT fallback
        return LevelRenderer.getLightColor(level, pos.relative(facing));
    }

    /**
     * 根据直径获取纹理像素高度（用于UV映射和模型高度）
     */
    private int getTexturePixelHeight(int diameter) {
        return switch (diameter) {
            case 5 -> 37;   // 木转子
            case 7 -> 46;   // 铁/青铜转子
            case 9 -> 58;   // 钢/钛铁合金转子
            case 11 -> 91;  // 碳/超级铱合金转子
            default -> 46;
        };
    }

    private void renderBox(PoseStack poseStack, MultiBufferSource bufferSource, float height, int pixelHeight, ResourceLocation texture, int packedLight) {
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose pose = poseStack.last();

        float w = ROTOR_WIDTH / 2.0f;
        float h = height / 2.0f;
        float d = ROTOR_DEPTH / 2.0f;
        float vMax = (float) pixelHeight / TEXTURE_TOTAL_HEIGHT;

        renderQuad(builder, pose, -w, -h, d,  0, vMax,  w, -h, d,  1, vMax,  w,  h, d,  1, 0, -w,  h, d,  0, 0,  0, 0, 1, packedLight);
        renderQuad(builder, pose,  w, -h, -d,  0, vMax, -w, -h, -d,  1, vMax, -w,  h, -d,  1, 0,  w,  h, -d,  0, 0,  0, 0, -1, packedLight);
        renderQuad(builder, pose, -w, h, -d,  0, 0,  w, h, -d,  1, 0,  w, h,  d,  1, 0.1f, -w, h,  d,  0, 0.1f,  0, 1, 0, packedLight);
        renderQuad(builder, pose, -w, -h,  d,  0, vMax - 0.1f,  w, -h,  d,  1, vMax - 0.1f,  w, -h, -d,  1, vMax, -w, -h, -d,  0, vMax,  0, -1, 0, packedLight);
        renderQuad(builder, pose, -w, -h,  d,  0, vMax, -w, -h, -d,  1, vMax, -w,  h, -d,  1, 0, -w,  h,  d,  0, 0,  -1, 0, 0, packedLight);
        renderQuad(builder, pose,  w, -h, -d,  0, vMax,  w, -h,  d,  1, vMax,  w,  h,  d,  1, 0,  w,  h, -d,  0, 0,  1, 0, 0, packedLight);
    }

    private void renderQuad(VertexConsumer builder, PoseStack.Pose pose,
                            float x0, float y0, float z0, float u0, float v0,
                            float x1, float y1, float z1, float u1, float v1,
                            float x2, float y2, float z2, float u2, float v2,
                            float x3, float y3, float z3, float u3, float v3,
                            float nx, float ny, float nz, int packedLight) {
        vertex(builder, pose, x0, y0, z0, u0, v0, nx, ny, nz, packedLight);
        vertex(builder, pose, x1, y1, z1, u1, v1, nx, ny, nz, packedLight);
        vertex(builder, pose, x2, y2, z2, u2, v2, nx, ny, nz, packedLight);
        vertex(builder, pose, x3, y3, z3, u3, v3, nx, ny, nz, packedLight);
    }

    private void vertex(VertexConsumer builder, PoseStack.Pose pose,
                        float x, float y, float z, float u, float v,
                        float nx, float ny, float nz, int packedLight) {
        builder.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
    }

    @Override
    public int getViewDistance() { return 64; }

    @Override
    public boolean shouldRenderOffScreen(mio_icif_Water_Kinetic_Generator blockEntity) { return true; }
}
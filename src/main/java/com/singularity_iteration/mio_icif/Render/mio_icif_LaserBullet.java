package com.singularity_iteration.mio_icif.Render;

import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_laser_bullet;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 镭射枪子弹渲染类 - 还原IC2原版LaserBulletEntityRenderer效果
 *
 * IC2原版渲染方式�? * - 使用laser.png纹理，白色着色（纹理本身提供颜色�? * - 旋转朝向飞行方向后，绕飞行轴(X�?旋转45�? * - 渲染一个头部方块（4x4双面方块，位于子弹尾部x=-7�? * - 渲染4个交叉的长方形片（每�?6x4，绕飞行轴每90度旋转一次，形成十字星形截面�? * - 缩放0.05625倍，X轴平�?4
 */
@SuppressWarnings("null")
public class mio_icif_LaserBullet extends EntityRenderer<mio_icif_laser_bullet> {

    public static final ResourceLocation LASER_BULLET_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "mio_icif", "textures/entity/laser.png"
    );

    public mio_icif_LaserBullet(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(mio_icif_laser_bullet entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.scale(0.05625F, 0.05625F, 0.05625F);
        poseStack.translate(-4.0D, 0.0D, 0.0D);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(this.getTextureLocation(entity)));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();

        vertex(matrix4f, vertexConsumer, -7, -2, -2, 0.0F, 0.15625F, -1, 0, 0, packedLight);
        vertex(matrix4f, vertexConsumer, -7, -2, 2, 0.15625F, 0.15625F, -1, 0, 0, packedLight);
        vertex(matrix4f, vertexConsumer, -7, 2, 2, 0.15625F, 0.3125F, -1, 0, 0, packedLight);
        vertex(matrix4f, vertexConsumer, -7, 2, -2, 0.0F, 0.3125F, -1, 0, 0, packedLight);
        vertex(matrix4f, vertexConsumer, -7, 2, -2, 0.0F, 0.15625F, 1, 0, 0, packedLight);
        vertex(matrix4f, vertexConsumer, -7, 2, 2, 0.15625F, 0.15625F, 1, 0, 0, packedLight);
        vertex(matrix4f, vertexConsumer, -7, -2, 2, 0.15625F, 0.3125F, 1, 0, 0, packedLight);
        vertex(matrix4f, vertexConsumer, -7, -2, -2, 0.0F, 0.3125F, 1, 0, 0, packedLight);

        for (int u = 0; u < 4; u++) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            PoseStack.Pose currentPose = poseStack.last();
            Matrix4f currentMatrix = currentPose.pose();
            vertex(currentMatrix, vertexConsumer, -8, -2, 0, 0.0F, 0.0F, 0, 0, 1, packedLight);
            vertex(currentMatrix, vertexConsumer, 8, -2, 0, 0.5F, 0.0F, 0, 0, 1, packedLight);
            vertex(currentMatrix, vertexConsumer, 8, 2, 0, 0.5F, 0.15625F, 0, 0, 1, packedLight);
            vertex(currentMatrix, vertexConsumer, -8, 2, 0, 0.0F, 0.15625F, 0, 0, 1, packedLight);
        }

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void vertex(Matrix4f positionMatrix, VertexConsumer vertexConsumer,
                        int x, int y, int z, float u, float v, int normalX, int normalY, int normalZ, int light) {
        vertexConsumer.addVertex(positionMatrix, x, y, z)
            .setColor(255, 255, 255, 255)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(normalX, normalY, normalZ);
    }

    @Override
    public ResourceLocation getTextureLocation(mio_icif_laser_bullet entity) {
        return LASER_BULLET_TEXTURE;
    }
}


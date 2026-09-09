package com.singularity_iteration.mio_icif.Render;

import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_plasma_bullet;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * 等离子子弹渲染类 - 还原IC2原版RenderBillboardEntity效果
 *
 * IC2原版渲染方式�?
 * - Billboard广告牌渲染（始终面向玩家�?
 * - 使用beam.png纹理
 * - 加法混合(Additive Blending)实现发光效果
 * - 半径0.4的发光球�?
 */
@SuppressWarnings("null")
public class mio_icif_PlasmaBullet extends EntityRenderer<mio_icif_plasma_bullet> {

    public static final ResourceLocation BEAM_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "mio_icif", "textures/entity/beam.png"
    );

    private static final float RADIUS = 0.4F;

    public mio_icif_PlasmaBullet(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(mio_icif_plasma_bullet entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        float scale = RADIUS;
        poseStack.scale(scale, scale, scale);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(this.getTextureLocation(entity)));

        Matrix4f matrix4f = poseStack.last().pose();

        float u0 = 0.0F;
        float v0 = 0.0F;
        float u1 = 1.0F;
        float v1 = 1.0F;

        vertexConsumer.addVertex(matrix4f, -1, -1, 0).setColor(255, 255, 255, 200).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 0, 1);
        vertexConsumer.addVertex(matrix4f, 1, -1, 0).setColor(255, 255, 255, 200).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 0, 1);
        vertexConsumer.addVertex(matrix4f, 1, 1, 0).setColor(255, 255, 255, 200).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 0, 1);
        vertexConsumer.addVertex(matrix4f, -1, 1, 0).setColor(255, 255, 255, 200).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 0, 1);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(mio_icif_plasma_bullet entity) {
        return BEAM_TEXTURE;
    }
}


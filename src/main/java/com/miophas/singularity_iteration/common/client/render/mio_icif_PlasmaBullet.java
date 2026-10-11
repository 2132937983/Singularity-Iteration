// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.item.tools.mio_icif_plasma_bullet;
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
 * Renderer for the plasma bullet. It recreates the RenderBillboardEntity effect of the original IC2.
 *
 * Render method of the original IC2:
 * - Billboard rendering (the quad always faces the player)
 * - Uses the beam.png texture
 * - Additive blending makes the glow effect
 * - A glowing sphere with a radius of 0.4
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


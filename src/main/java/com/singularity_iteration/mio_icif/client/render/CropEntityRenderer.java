package com.singularity_iteration.mio_icif.client.render;

import com.singularity_iteration.mio_icif.Blocks.entity.crop.mio_icif_crop_entity;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

/**
 * 作物方块实体渲染�? * 用于渲染种植在作物架上的植物
 */
@SuppressWarnings("null")
public class CropEntityRenderer implements BlockEntityRenderer<mio_icif_crop_entity> {

    public CropEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(mio_icif_crop_entity blockEntity, float partialTick, PoseStack poseStack, 
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        PlantType plant = blockEntity.getPlant();
        if (plant == null) {
            return; // 没有种植作物，不渲染
        }

        // 获取当前生长阶段的纹�
    int stage = blockEntity.getGrowthStage();
        String texturePath = plant.getTexture(stage);
        
        if (texturePath == null || texturePath.isEmpty()) {
            return;
        }

        // 构建完整的资源位置
    // getTexture 返回格式�?"mio_icif:block/crop/wheat_1"
        ResourceLocation textureLoc = ResourceLocation.parse(texturePath);

        // 获取纹理图集中的精灵
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(textureLoc);
        
        if (sprite == null) {
            return;
        }

        // 渲染井字交叉模型（类似原版作物）
        poseStack.pushPose();
        
        // 将坐标系移动到方块中�
    poseStack.translate(0.5, 0.0, 0.5);
        
        // 渲染井字交叉（四个方向的交叉�
    renderCropCross(poseStack, bufferSource, sprite, packedLight, packedOverlay);
        
        poseStack.popPose();
    }

    /**
     * 渲染井字交叉模型（类似原版小麦、胡萝卜等作物）
     * 四个平面呈井字形交叉
     */
    private void renderCropCross(PoseStack poseStack, MultiBufferSource bufferSource,
                                  TextureAtlasSprite sprite, int packedLight, int packedOverlay) {
        VertexConsumer builder = bufferSource.getBuffer(RenderType.cutout());

        // 获取纹理坐标
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // 作物的高度（�?�?.8125，接地并稍微降低高度�
    float yBottom = 0.0f;
        float yTop = 0.8125f;
        float offset = 0.25625f; // 平面位置往内移�?格像�?(4.1/16 = 0.25625)，稍微往�?.1像素避免闪烁
        float width = 0.4f;      // 面的半宽保持不变，确保四角交换?
        // 四个平面呈井字形交叉，位置往内移动但宽度不变
        // 平面1: 南北方向 (X = -offset)
        renderQuad(poseStack, builder, -offset, yBottom, -width, -offset, yTop, width, u0, v0, u1, v1, packedLight, packedOverlay);
        // 平面2: 南北方向 (X = offset)
        renderQuad(poseStack, builder, offset, yBottom, -width, offset, yTop, width, u0, v0, u1, v1, packedLight, packedOverlay);
        // 平面3: 东西方向 (Z = -offset)
        renderQuad(poseStack, builder, -width, yBottom, -offset, width, yTop, -offset, u0, v0, u1, v1, packedLight, packedOverlay);
        // 平面4: 东西方向 (Z = offset)
        renderQuad(poseStack, builder, -width, yBottom, offset, width, yTop, offset, u0, v0, u1, v1, packedLight, packedOverlay);
    }

    /**
     * 渲染一个双面四边形（正面和背面�
 */
    private void renderQuad(PoseStack poseStack, VertexConsumer builder,
                            float x1, float y1, float z1, float x2, float y2, float z2,
                            float u0, float v0, float u1, float v1, int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();

        // 计算法向量（正面�
    float nx = z2 - z1;
        float nz = x1 - x2;
        float len = (float) Math.sqrt(nx * nx + nz * nz);
        if (len > 0) {
            nx /= len;
            nz /= len;
        }

        // 渲染正面
        // 左下
        builder.addVertex(matrix, x1, y1, z1)
               .setColor(255, 255, 255, 255)
               .setUv(u0, v1)
               .setOverlay(packedOverlay)
               .setLight(packedLight)
               .setNormal(nx, 0, nz);
        // 左上
        builder.addVertex(matrix, x1, y2, z1)
               .setColor(255, 255, 255, 255)
               .setUv(u0, v0)
               .setOverlay(packedOverlay)
               .setLight(packedLight)
               .setNormal(nx, 0, nz);
        // 右上
        builder.addVertex(matrix, x2, y2, z2)
               .setColor(255, 255, 255, 255)
               .setUv(u1, v0)
               .setOverlay(packedOverlay)
               .setLight(packedLight)
               .setNormal(nx, 0, nz);
        // 右下
        builder.addVertex(matrix, x2, y1, z2)
               .setColor(255, 255, 255, 255)
               .setUv(u1, v1)
               .setOverlay(packedOverlay)
               .setLight(packedLight)
               .setNormal(nx, 0, nz);

        // 渲染背面（法向量反向，顶点顺序反向）
        // 右下
        builder.addVertex(matrix, x2, y1, z2)
               .setColor(255, 255, 255, 255)
               .setUv(u1, v1)
               .setOverlay(packedOverlay)
               .setLight(packedLight)
               .setNormal(-nx, 0, -nz);
        // 右上
        builder.addVertex(matrix, x2, y2, z2)
               .setColor(255, 255, 255, 255)
               .setUv(u1, v0)
               .setOverlay(packedOverlay)
               .setLight(packedLight)
               .setNormal(-nx, 0, -nz);
        // 左上
        builder.addVertex(matrix, x1, y2, z1)
               .setColor(255, 255, 255, 255)
               .setUv(u0, v0)
               .setOverlay(packedOverlay)
               .setLight(packedLight)
               .setNormal(-nx, 0, -nz);
        // 左下
        builder.addVertex(matrix, x1, y1, z1)
               .setColor(255, 255, 255, 255)
               .setUv(u0, v1)
               .setOverlay(packedOverlay)
               .setLight(packedLight)
               .setNormal(-nx, 0, -nz);
    }

    @Override
    public boolean shouldRenderOffScreen(mio_icif_crop_entity blockEntity) {
        return true; // 确保即使作物在视野外也能渲染
    }

    @Override
    public int getViewDistance() {
        return 64; // 渲染距离
    }
}


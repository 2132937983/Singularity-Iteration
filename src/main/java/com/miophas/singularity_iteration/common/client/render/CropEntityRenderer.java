package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.blockentity.crop.mio_icif_crop_entity;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

/**
 * Block entity renderer for crops.
 * It renders the plant that grows on a crop stick.
 */
@SuppressWarnings("null")
public class CropEntityRenderer implements BlockEntityRenderer<mio_icif_crop_entity> {
    /** EN: Texture string to parsed id (render thread only). ZH: 纹理字符串到已解析 ID（仅渲染线程）。JA: テクスチャ文字列から解析済み ID（描画スレッドのみ）。 */
    private static final java.util.Map<String, ResourceLocation> TEXTURE_IDS = new java.util.HashMap<>();
    /** EN: Neutral fallback plant (four growth frames). ZH: 中性备用植物（四个生长帧）。JA: 中立の代替植物（成長 4 コマ）。 */
    private static final ResourceLocation[] FALLBACK = {
        ResourceLocation.fromNamespaceAndPath("mio_icif", "block/crop/fallback_1"), ResourceLocation.fromNamespaceAndPath("mio_icif", "block/crop/fallback_2"),
        ResourceLocation.fromNamespaceAndPath("mio_icif", "block/crop/fallback_3"), ResourceLocation.fromNamespaceAndPath("mio_icif", "block/crop/fallback_4")};

    /** EN: Invalid ids (for example upper case from an addon) use the fallback instead of a crash. ZH: 非法 ID（例如附属的大写字母）使用备用纹理而不是崩溃。JA: 不正な ID（例：アドオンの大文字）はクラッシュせず代替テクスチャを使う。 */
    private static ResourceLocation parseOrFallback(String path) {
        ResourceLocation id = ResourceLocation.tryParse(path);
        return id != null ? id : FALLBACK[0];
    }

    public CropEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(mio_icif_crop_entity blockEntity, float partialTick, PoseStack poseStack, 
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        PlantType plant = blockEntity.getPlant();
        if (plant == null) {
            return; // 没有种植作物，不渲染
        }

        // Get the texture for the current growth stage.
    int stage = blockEntity.getGrowthStage();
        String texturePath = plant.getTexture(stage);
        
        if (texturePath == null || texturePath.isEmpty()) {
            return;
        }

        // getTexture returns a value in the format "mio_icif:block/crop/wheat_1".
        // EN: Parse each texture id once; the renderer runs every frame for every visible crop.
        // ZH: 每个纹理 ID 只解析一次；渲染器每帧都会为每株可见作物运行。
        // JA: テクスチャ ID は 1 回だけ解析する。レンダラーは毎フレーム、見えている作物ごとに動く。
        ResourceLocation textureLoc = TEXTURE_IDS.computeIfAbsent(texturePath, CropEntityRenderer::parseOrFallback);

        // 获取纹理图集中的精灵
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite sprite = atlas.apply(textureLoc);
        // EN: 0.1.8.2: a plant without its stage texture (13 configured crops had none, addon plants can
        //     have none) showed the purple-black missing texture. Use the neutral fallback plant instead.
        // ZH: 0.1.8.2：缺少阶段纹理的植物（13 种配置作物此前没有纹理，附属植物也可能没有）会显示紫黑缺失纹理，现在改用中性的备用植物纹理。
        // JA: 0.1.8.2：段階テクスチャのない植物（設定作物 13 種には無かった。アドオンの植物にも無いことがある）は紫と黒の欠落テクスチャになっていた。
        //     代わりに中立の代替植物テクスチャを使う。
        if (sprite == null || MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
            int frame = Math.clamp(Math.round(4f * Math.max(1, stage) / Math.max(1, plant.getMaxGrowthStage())), 1, 4);
            sprite = atlas.apply(FALLBACK[frame - 1]);
        }
        if (sprite == null) {
            return;
        }

        // 渲染井字交叉模型（类似原版作物）
        poseStack.pushPose();
        
        // Move the origin to the block center.
    poseStack.translate(0.5, 0.0, 0.5);
        
        // Render the hash-shaped cross (four planes in a # pattern).
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

        // Crop height: 0 to 0.8125. The crop touches the ground and is slightly lower than a full block.
    float yBottom = 0.0f;
        float yTop = 0.8125f;
        float offset = 0.25625f; // Planes move 4.1 pixels inward (4.1/16 = 0.25625). The extra 0.1 pixel prevents z-fighting.
        float width = 0.4f;      // The half width of each plane does not change. This makes sure that the planes cross at the corners.
        // The four planes cross in a # shape. They move inward, but their width does not change.
        // Plane 1: north-south (X = -offset)
        renderQuad(poseStack, builder, -offset, yBottom, -width, -offset, yTop, width, u0, v0, u1, v1, packedLight, packedOverlay);
        // Plane 2: north-south (X = offset)
        renderQuad(poseStack, builder, offset, yBottom, -width, offset, yTop, width, u0, v0, u1, v1, packedLight, packedOverlay);
        // Plane 3: east-west (Z = -offset)
        renderQuad(poseStack, builder, -width, yBottom, -offset, width, yTop, -offset, u0, v0, u1, v1, packedLight, packedOverlay);
        // Plane 4: east-west (Z = offset)
        renderQuad(poseStack, builder, -width, yBottom, offset, width, yTop, offset, u0, v0, u1, v1, packedLight, packedOverlay);
    }

    /**
     * Renders a double-sided quad (front face and back face).
     */
    private void renderQuad(PoseStack poseStack, VertexConsumer builder,
                            float x1, float y1, float z1, float x2, float y2, float z2,
                            float u0, float v0, float u1, float v1, int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();

        // Calculate the normal vector of the front face.
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

    /**
     * <p><b>EN:</b> 0.1.8.1: false. The plant stays inside its own block, so the normal frustum
     * culling is correct. The former value true drew every crop within 64 blocks each frame,
     * also crops behind the camera.
     * <p><b>ZH:</b> 0.1.8.1：返回 false。植物不超出自身方块，普通视锥剔除即可。原先的 true 会在每帧绘制 64 格内的所有作物，包括镜头后方的作物。
     * <p><b>JA:</b> 0.1.8.1：false を返す。植物は自分のブロック内に収まるので、通常の視錐台カリングで正しい。
     * 以前の true では、カメラの後ろも含め 64 ブロック以内の全作物を毎フレーム描画していた。
     */
    @Override
    public boolean shouldRenderOffScreen(mio_icif_crop_entity blockEntity) {
        return false;
    }

    @Override
    public int getViewDistance() {
        return 64; // 渲染距离
    }
}


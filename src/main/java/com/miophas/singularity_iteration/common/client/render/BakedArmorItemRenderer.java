// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Item renderer of a baked armor piece (0.1.7.35): the inventory icon, the held item and the
 * dropped item show the same 3D Blockbench model as the worn armor, like the machine items show
 * their block model. The item model ({@code parent: builtin/entity}) holds the display transforms;
 * this renderer fits the piece into the unit item cube.
 */
public class BakedArmorItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final float[][] PIVOT = {
        {0, 0, 0}, {0, 0, 0}, {-5, 2, 0}, {5, 2, 0}, {-1.9F, 12, 0}, {1.9F, 12, 0}};
    private static final String[] PARTS = {"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"};

    private final ResourceLocation geometry;
    private final ResourceLocation texture;
    private final BakedArmorModel model;
    /** Fit of the piece into the item cube: centre (model units) and scale; recomputed after a reload. */
    private Map<String, float[][]> fittedFor;
    private float cx, cy, cz, scale;

    public BakedArmorItemRenderer(ResourceLocation geometry, ResourceLocation texture) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.geometry = geometry;
        this.texture = texture;
        this.model = new BakedArmorModel(geometry);
    }

    private void fit(Map<String, float[][]> geo) {
        if (geo == fittedFor) return;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int p = 0; p < PARTS.length; p++) {
            float[][] quads = geo.get(PARTS[p]);
            if (quads == null) continue;
            for (float[] q : quads) {
                for (int i = 0; i < 4; i++) {
                    float x = q[i * 5] + PIVOT[p][0], y = q[i * 5 + 1] + PIVOT[p][1], z = q[i * 5 + 2] + PIVOT[p][2];
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                    minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
                }
            }
        }
        if (minX > maxX) { cx = cy = cz = 0; scale = 1F / 16F; fittedFor = geo; return; }
        cx = (minX + maxX) / 2; cy = (minY + maxY) / 2; cz = (minZ + maxZ) / 2;
        float extent = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        scale = 0.92F / Math.max(1F, extent);
        fittedFor = geo;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Map<String, float[][]> geo = model.geometry();
        if (geo.isEmpty()) return;
        fit(geo);
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        // model space is y down with the front at -z: turn it 180 deg about x so the front faces the viewer
        pose.scale(scale, -scale, -scale);
        pose.translate(-cx, -cy, -cz);
        VertexConsumer vc = ItemRenderer.getArmorFoilBuffer(buffers, RenderType.armorCutoutNoCull(texture), stack.hasFoil());
        PoseStack.Pose last = pose.last();
        for (int p = 0; p < PARTS.length; p++) {
            float[][] quads = geo.get(PARTS[p]);
            if (quads == null) continue;
            float px = PIVOT[p][0], py = PIVOT[p][1], pz = PIVOT[p][2];
            for (float[] q : quads) {
                for (int i = 0; i < 4; i++) {
                    int k = i * 5;
                    vc.addVertex(last, q[k] + px, q[k + 1] + py, q[k + 2] + pz)
                        .setColor(-1)
                        .setUv(q[k + 3], q[k + 4])
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(last, q[20], q[21], q[22]);
                }
            }
        }
        pose.popPose();
    }

    public ResourceLocation geometryId() { return geometry; }
}

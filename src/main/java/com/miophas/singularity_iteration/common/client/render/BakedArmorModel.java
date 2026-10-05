// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Humanoid armor model whose geometry comes from a baked Blockbench export
 * ({@code assets/mio_icif/armor_models/*.json}, made by {@code tools/gen_nano_armor.py}).
 *
 * <p>The model keeps the usual humanoid parts, so the armor layer can pose it and switch parts on
 * and off as for vanilla armor; each part then draws its baked quads instead of cubes. The JSON
 * is read on first use and again after a resource reload.
 */
public class BakedArmorModel extends HumanoidModel<LivingEntity> {
    private static final Map<ResourceLocation, Map<String, float[][]>> CACHE = new HashMap<>();
    private static int generation;

    private final ResourceLocation source;
    private Map<String, float[][]> geometry;
    private int loadedGeneration = -1;

    public BakedArmorModel(ResourceLocation source) {
        super(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0F), 64, 32).bakeRoot());
        this.source = source;
    }

    /** Called by the client reload listener: the next frame reads the JSON again. */
    public static synchronized void invalidate() {
        CACHE.clear();
        generation++;
    }

    public ResourceLocation source() { return source; }

    /** Baked quads per part ("head", "body", "right_arm", ...), each float[23]: 4 x (x y z u v) + normal. */
    public Map<String, float[][]> geometry() {
        synchronized (BakedArmorModel.class) {
            if (geometry == null || loadedGeneration != generation) {
                geometry = CACHE.computeIfAbsent(source, BakedArmorModel::load);
                loadedGeneration = generation;
            }
            return geometry;
        }
    }

    static Map<String, float[][]> load(ResourceLocation id) {
        Map<String, float[][]> parts = new HashMap<>();
        var resource = Minecraft.getInstance().getResourceManager().getResource(id);
        if (resource.isEmpty()) {
            Singularity_Iteration.LOGGER.warn("Baked armor model {} is missing", id);
            return parts;
        }
        try (Reader reader = resource.get().openAsReader()) {
            parts.putAll(parse(JsonParser.parseReader(reader).getAsJsonObject()));
        } catch (Exception e) {
            Singularity_Iteration.LOGGER.error("Cannot read baked armor model {}", id, e);
        }
        return parts;
    }

    /** JSON -> quads. Public for the GameTests (they check the shipped files). */
    public static Map<String, float[][]> parse(JsonObject root) {
        Map<String, float[][]> parts = new HashMap<>();
        JsonObject partsJson = root.getAsJsonObject("parts");
        for (Map.Entry<String, JsonElement> entry : partsJson.entrySet()) {
            List<float[]> quads = new ArrayList<>();
            for (JsonElement q : entry.getValue().getAsJsonArray()) {
                JsonObject o = q.getAsJsonObject();
                JsonArray v = o.getAsJsonArray("v"), n = o.getAsJsonArray("n");
                if (v.size() != 20 || n.size() != 3) continue;
                float[] quad = new float[23];
                for (int i = 0; i < 20; i++) quad[i] = v.get(i).getAsFloat();
                for (int i = 0; i < 3; i++) quad[20 + i] = n.get(i).getAsFloat();
                quads.add(quad);
            }
            parts.put(entry.getKey(), quads.toArray(new float[0][]));
        }
        return parts;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
        Map<String, float[][]> geo = geometry();
        if (geo.isEmpty()) return;
        draw(pose, vc, head, geo.get("head"), light, overlay, color);
        draw(pose, vc, body, geo.get("body"), light, overlay, color);
        draw(pose, vc, rightArm, geo.get("right_arm"), light, overlay, color);
        draw(pose, vc, leftArm, geo.get("left_arm"), light, overlay, color);
        draw(pose, vc, rightLeg, geo.get("right_leg"), light, overlay, color);
        draw(pose, vc, leftLeg, geo.get("left_leg"), light, overlay, color);
    }

    private static void draw(PoseStack pose, VertexConsumer vc, ModelPart part, float[][] quads, int light, int overlay, int color) {
        if (quads == null || quads.length == 0 || !part.visible || part.skipDraw) return;
        pose.pushPose();
        part.translateAndRotate(pose);
        PoseStack.Pose last = pose.last();
        for (float[] q : quads) {
            for (int i = 0; i < 4; i++) {
                int k = i * 5;
                vc.addVertex(last, q[k] / 16F, q[k + 1] / 16F, q[k + 2] / 16F)
                    .setColor(color)
                    .setUv(q[k + 3], q[k + 4])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(last, q[20], q[21], q[22]);
            }
        }
        pose.popPose();
    }
}

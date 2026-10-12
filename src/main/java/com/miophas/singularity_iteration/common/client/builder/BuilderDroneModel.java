// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.builder;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * Builder drone (0.1.8.0): a small quad-rotor in the nano / quantum suit colours. The model is
 * built in code (no model file); the light strips use an emissive layer. Shared by the
 * Blueprint Builder renderer and the RTS Building drone visuals.
 */
@OnlyIn(Dist.CLIENT)
public final class BuilderDroneModel {
    private BuilderDroneModel() {}

    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/entity/builder_drone.png");
    static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/entity/builder_drone_glow.png");
    static final float SCALE = 0.6F;

    private static ModelPart root;
    private static ModelPart[] rotors;

    private static void build() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -1.5F, -3, 6, 3, 6), PartPose.ZERO);
        r.addOrReplaceChild("canopy", CubeListBuilder.create().texOffs(0, 9).addBox(-2, 1.5F, -2, 4, 1, 4), PartPose.ZERO);
        r.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(16, 9).addBox(-1, -0.5F, -3.4F, 2, 1, 1), PartPose.ZERO);
        r.addOrReplaceChild("arm_a", CubeListBuilder.create().texOffs(0, 14).addBox(-6, -0.5F, -0.5F, 12, 1, 1),
            PartPose.rotation(0, (float) Math.PI / 4, 0));
        r.addOrReplaceChild("arm_b", CubeListBuilder.create().texOffs(0, 14).addBox(-6, -0.5F, -0.5F, 12, 1, 1),
            PartPose.rotation(0, -(float) Math.PI / 4, 0));
        float d = 4.24F;
        float[][] at = {{d, d}, {-d, d}, {d, -d}, {-d, -d}};
        for (int i = 0; i < 4; i++) {
            r.addOrReplaceChild("rotor" + i, CubeListBuilder.create().texOffs(0, 16).addBox(-2.5F, 0, -2.5F, 5, 0.3F, 5)
                .texOffs(20, 16).addBox(-0.5F, -1, -0.5F, 1, 1, 1), PartPose.offset(at[i][0], 0.6F, at[i][1]));
        }
        root = LayerDefinition.create(mesh, 32, 32).bakeRoot();
        rotors = new ModelPart[4];
        for (int i = 0; i < 4; i++) rotors[i] = root.getChild("rotor" + i);
    }

    /**
     * Draws one drone at the pose origin (the drone centre), nose along -z, with the cargo
     * item hanging below it.
     *
     * @param time ticks (with partial tick), for the rotors and the hover bob
     */
    public static void render(PoseStack pose, MultiBufferSource buffers, int light, float time, ItemStack cargo, Level level) {
        if (root == null) build();
        for (int i = 0; i < 4; i++) rotors[i].yRot = time * (i % 2 == 0 ? 1.9F : -1.9F);
        pose.pushPose();
        pose.translate(0, Math.sin(time * 0.25F) * 0.03F, 0);
        pose.pushPose();
        pose.scale(SCALE / 16F, SCALE / 16F, SCALE / 16F);   // model pixels to blocks
        root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        root.render(pose, buffers.getBuffer(RenderType.eyes(GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        if (!cargo.isEmpty()) {
            pose.translate(0, -0.22F, 0);
            pose.scale(0.32F, 0.32F, 0.32F);
            Minecraft.getInstance().getItemRenderer().renderStatic(cargo, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, level, 0);
        }
        pose.popPose();
    }

    /** A thin cyan placement beam from {@code from} to {@code to} (both relative to the pose origin). */
    public static void beam(PoseStack pose, MultiBufferSource buffers, Vec3 from, Vec3 to, float alpha) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lines());
        Matrix4f m = pose.last().pose();
        Vec3 n = to.subtract(from).normalize();
        int a = (int) (alpha * 255);
        vc.addVertex(m, (float) from.x, (float) from.y, (float) from.z).setColor(90, 225, 255, a).setNormal(pose.last(), (float) n.x, (float) n.y, (float) n.z);
        vc.addVertex(m, (float) to.x, (float) to.y, (float) to.z).setColor(160, 245, 255, a).setNormal(pose.last(), (float) n.x, (float) n.y, (float) n.z);
    }
}

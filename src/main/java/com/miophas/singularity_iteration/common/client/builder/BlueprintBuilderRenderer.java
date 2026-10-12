// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.builder;

import com.miophas.singularity_iteration.common.builder.BlueprintBuilderBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * Blueprint Builder renderer (0.1.8.0): the builder drones on their flight legs (with cargo and
 * placement beam) and the work area as a translucent hologram box with a block grid.
 */
@OnlyIn(Dist.CLIENT)
public class BlueprintBuilderRenderer implements BlockEntityRenderer<BlueprintBuilderBlockEntity> {
    public BlueprintBuilderRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(BlueprintBuilderBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;
        BlockPos origin = be.getBlockPos();
        double time = be.getLevel().getGameTime() + partialTick;
        AABB bounds = be.bounds();
        if (bounds != null && (be.preview() || be.running())) hologram(pose, buffers, bounds.move(-origin.getX(), -origin.getY(), -origin.getZ()), (float) time);
        BuilderDroneView.render(be, pose, buffers, origin, time, partialTick);
    }

    /** Translucent faces, edge lines and a block grid on the faces (every block up to 32, else every 4). */
    private static void hologram(PoseStack pose, MultiBufferSource buffers, AABB box, float time) {
        float pulse = 0.55F + 0.15F * (float) Math.sin(time * 0.12F);
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), box, 0.35F, 0.85F, 1.0F, 0.9F * pulse);
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        Matrix4f m = pose.last().pose();
        double sx = box.getXsize(), sy = box.getYsize(), sz = box.getZsize();
        int step = Math.max(sx, Math.max(sy, sz)) > 32 ? 4 : 1;
        int a = (int) (90 * pulse);
        for (int x = step; x < sx; x += step) {
            line(lines, pose, m, box.minX + x, box.minY, box.minZ, box.minX + x, box.minY, box.maxZ, a);
            line(lines, pose, m, box.minX + x, box.maxY, box.minZ, box.minX + x, box.maxY, box.maxZ, a);
        }
        for (int z = step; z < sz; z += step) {
            line(lines, pose, m, box.minX, box.minY, box.minZ + z, box.maxX, box.minY, box.minZ + z, a);
            line(lines, pose, m, box.minX, box.maxY, box.minZ + z, box.maxX, box.maxY, box.minZ + z, a);
        }
        for (int y = step; y < sy; y += step) {
            line(lines, pose, m, box.minX, box.minY + y, box.minZ, box.maxX, box.minY + y, box.minZ, a);
            line(lines, pose, m, box.minX, box.minY + y, box.maxZ, box.maxX, box.minY + y, box.maxZ, a);
            line(lines, pose, m, box.minX, box.minY + y, box.minZ, box.minX, box.minY + y, box.maxZ, a);
            line(lines, pose, m, box.maxX, box.minY + y, box.minZ, box.maxX, box.minY + y, box.maxZ, a);
        }
        // faint faces (additive glow of the suit field effects)
        VertexConsumer glow = buffers.getBuffer(com.miophas.singularity_iteration.common.client.render.SiRenderTypes.FIELD_GLOW);
        int fa = (int) (26 * pulse);
        face(glow, m, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, fa, true);
        face(glow, m, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, fa, true);
        face(glow, m, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, fa, false);
        face(glow, m, box.minX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, fa, false);
        sideX(glow, m, box.minX, box, fa);
        sideX(glow, m, box.maxX, box, fa);
    }

    private static void line(VertexConsumer vc, PoseStack pose, Matrix4f m, double x0, double y0, double z0, double x1, double y1, double z1, int a) {
        float nx = (float) (x1 - x0), ny = (float) (y1 - y0), nz = (float) (z1 - z0);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-4F) return;
        nx /= len; ny /= len; nz /= len;
        vc.addVertex(m, (float) x0, (float) y0, (float) z0).setColor(90, 210, 255, a).setNormal(pose.last(), nx, ny, nz);
        vc.addVertex(m, (float) x1, (float) y1, (float) z1).setColor(90, 210, 255, a).setNormal(pose.last(), nx, ny, nz);
    }

    /** A horizontal (y fixed) or a z-facing (z fixed) quad, both windings. */
    private static void face(VertexConsumer vc, Matrix4f m, double x0, double y0, double z0, double x1, double y1, double z1, int a, boolean horizontal) {
        float[][] q = horizontal
            ? new float[][]{{(float) x0, (float) y0, (float) z0}, {(float) x1, (float) y0, (float) z0}, {(float) x1, (float) y0, (float) z1}, {(float) x0, (float) y0, (float) z1}}
            : new float[][]{{(float) x0, (float) y0, (float) z0}, {(float) x1, (float) y0, (float) z0}, {(float) x1, (float) y1, (float) z0}, {(float) x0, (float) y1, (float) z0}};
        quad(vc, m, q, a);
    }

    private static void sideX(VertexConsumer vc, Matrix4f m, double x, AABB b, int a) {
        quad(vc, m, new float[][]{{(float) x, (float) b.minY, (float) b.minZ}, {(float) x, (float) b.minY, (float) b.maxZ},
            {(float) x, (float) b.maxY, (float) b.maxZ}, {(float) x, (float) b.maxY, (float) b.minZ}}, a);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float[][] q, int a) {
        for (int i = 0; i < 4; i++) vc.addVertex(m, q[i][0], q[i][1], q[i][2]).setColor(70, 190, 255, a);
        for (int i = 3; i >= 0; i--) vc.addVertex(m, q[i][0], q[i][1], q[i][2]).setColor(70, 190, 255, a);
    }

    @Override public boolean shouldRenderOffScreen(BlueprintBuilderBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 128; }

    @Override
    public AABB getRenderBoundingBox(BlueprintBuilderBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(BlueprintBuilderBlockEntity.RANGE + 2);
    }

    /** Item stack for a synced cargo id (cached per id). */
    static ItemStack cargo(String id) {
        if (id == null || id.isEmpty()) return ItemStack.EMPTY;
        return CARGO.computeIfAbsent(id, k -> {
            ResourceLocation rl = ResourceLocation.tryParse(k);
            return rl == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(rl));
        });
    }

    private static final java.util.Map<String, ItemStack> CARGO = new java.util.HashMap<>();

    /** Yaw (degrees) that turns the drone nose (-z) along {@code dir}. */
    static float yaw(Vec3 dir) {
        return (float) Math.toDegrees(Math.atan2(-dir.x, -dir.z));
    }

    static void face(PoseStack pose, Vec3 dir) {
        if (dir.horizontalDistanceSqr() > 1.0E-4) pose.mulPose(Axis.YP.rotationDegrees(yaw(dir)));
    }
}

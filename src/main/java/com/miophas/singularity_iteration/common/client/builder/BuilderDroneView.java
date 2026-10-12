// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.builder;

import com.miophas.singularity_iteration.common.builder.BlueprintBuilderBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Draws the drones of one Blueprint Builder (0.1.8.0). Docked drones sit on the landing pad. */
@OnlyIn(Dist.CLIENT)
final class BuilderDroneView {
    private BuilderDroneView() {}

    static void render(BlueprintBuilderBlockEntity be, PoseStack pose, MultiBufferSource buffers, BlockPos origin, double time, float partialTick) {
        var level = be.getLevel();
        for (int i = 0; i < be.droneCount(); i++) {
            Vec3 at = be.dronePosition(i, time);
            Vec3 rel = at.subtract(origin.getX(), origin.getY(), origin.getZ());
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(at));
            pose.pushPose();
            pose.translate(rel.x, rel.y, rel.z);
            if (be.droneDocked(i)) {
                pose.scale(0.8F, 0.8F, 0.8F);                               // folded on the pad
            } else {
                BlueprintBuilderRenderer.face(pose, be.droneHeading(i));
            }
            BuilderDroneModel.render(pose, buffers, light, (float) time + i * 7, BlueprintBuilderRenderer.cargo(be.droneCargo(i)), level);
            pose.popPose();
            BlockPos work = be.droneWork(i);
            double left = be.droneArrivalIn(i, time);
            if (work != null && left < 5) {
                Vec3 target = Vec3.atCenterOf(work).subtract(origin.getX(), origin.getY(), origin.getZ());
                BuilderDroneModel.beam(pose, buffers, rel.add(0, -0.1, 0), target, (float) Math.max(0.2, 1 - left / 5));
            }
        }
    }
}

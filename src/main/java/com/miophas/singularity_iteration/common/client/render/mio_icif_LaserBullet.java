// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_laser_bullet;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Mining-laser bolt: a slim, condensed, self-lit crimson beam drawn by {@link BeamFx}. Since
 * 0.1.8.3 its tail starts at the muzzle of the mining laser (tracer), so the beam is visible from
 * the shooter's own view instead of collapsing to a dot.
 */
@SuppressWarnings("null")
public class mio_icif_LaserBullet extends EntityRenderer<mio_icif_laser_bullet> {

    public static final ResourceLocation LASER_BULLET_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "mio_icif", "textures/entity/laser.png"
    );
    private static final float MAX_LENGTH = 2.6F;
    private static final int COLOR = 0xFF3A2A;

    public mio_icif_LaserBullet(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(mio_icif_laser_bullet entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Vec3 motion = entity.getDeltaMovement();
        double speed = motion.length();
        if (speed < 1.0E-4) return;
        Vec3 dir = motion.scale(1 / speed);
        float age = entity.tickCount + partialTicks;
        // grows out of the muzzle over the first ticks instead of popping in at full length
        float length = Math.min(MAX_LENGTH, (float) speed * Math.max(0.35F, age));
        Vec3 tail = BeamFx.tail(entity, Vec3.ZERO, dir, length, age, partialTicks);
        if (tail == null) return;
        Vec3 toCam = BeamFx.toCamera(entity, this.entityRenderDispatcher.camera.getPosition(), partialTicks);
        float flicker = 0.9F + 0.1F * Mth.sin(age * 2.7F + entity.getId());
        BeamFx.bolt(buffer.getBuffer(SiRenderTypes.ENERGY_BEAM), poseStack.last().pose(), Vec3.ZERO, tail, toCam, COLOR,
            0.014F * flicker, 0.55F + 0.45F * FcsClientConfig.flashScale());
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(mio_icif_laser_bullet entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBox().inflate(MAX_LENGTH));
    }

    @Override
    public ResourceLocation getTextureLocation(mio_icif_laser_bullet entity) {
        return LASER_BULLET_TEXTURE;
    }
}

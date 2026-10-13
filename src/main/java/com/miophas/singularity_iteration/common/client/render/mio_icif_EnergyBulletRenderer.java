package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_energy_bullet;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Energy bolt of the rifles, the plasma gun and the tachyon disruptor (0.1.8.3 rewrite).
 *
 * <p>Up to 0.1.8.2 this drew the vanilla arrow quads with a laser texture, lit by the world light
 * and turned by the entity rotation: a small dark dart that was hard to see, and from the
 * shooter's eye it pointed straight away and vanished. It is now a light beam drawn by
 * {@link BeamFx}: additive and full bright, in the colour of the gun (synced from the server),
 * as wide as the shot is strong, with a tracer that starts at the muzzle.
 */
@SuppressWarnings("null")
public class mio_icif_EnergyBulletRenderer extends EntityRenderer<mio_icif_energy_bullet> {

    public static final ResourceLocation ENERGY_BULLET_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "mio_icif", "textures/entity/laser.png"
    );
    private static final float MAX_LENGTH = 3.4F;

    public mio_icif_EnergyBulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(mio_icif_energy_bullet entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Vec3 motion = entity.getDeltaMovement();
        double speed = motion.length();
        if (speed < 1.0E-4) return;
        Vec3 dir = motion.scale(1 / speed);
        float age = entity.tickCount + partialTicks;
        float power = entity.beamPower();
        float length = Math.min(MAX_LENGTH * (0.7F + 0.3F * power), (float) speed * Math.max(0.35F, age));
        Vec3 head = Vec3.ZERO;
        Vec3 tail = BeamFx.tail(entity, head, dir, length, age, partialTicks);
        if (tail == null) return;
        Vec3 toCam = BeamFx.toCamera(entity, this.entityRenderDispatcher.camera.getPosition(), partialTicks);
        float flicker = 0.9F + 0.1F * Mth.sin(age * 2.7F + entity.getId());
        float width = (0.012F + 0.016F * power) * flicker;
        float alpha = 0.55F + 0.45F * FcsClientConfig.flashScale();   // reduced flashes dims the beam a little
        BeamFx.bolt(buffer.getBuffer(SiRenderTypes.ENERGY_BEAM), poseStack.last().pose(), head, tail, toCam, entity.beamColor(), width, alpha);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(mio_icif_energy_bullet entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        // the beam reaches back to the muzzle: test a box that covers the tail too
        return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBox().inflate(MAX_LENGTH));
    }

    @Override
    public ResourceLocation getTextureLocation(mio_icif_energy_bullet entity) {
        return ENERGY_BULLET_TEXTURE;
    }
}

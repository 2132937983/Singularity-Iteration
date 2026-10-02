// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.item.tools.mio_icif_laser_bullet;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Mining-laser bolt: a slim, condensed, self-lit beam. Three additive camera-facing ribbons laid
 * along the flight path — a hair-thin white-hot core, a tight crimson glow and a faint wide halo —
 * tapering to nothing at the tail, plus a bright bloom at the head. No texture, full-bright.
 */
@SuppressWarnings("null")
public class mio_icif_LaserBullet extends EntityRenderer<mio_icif_laser_bullet> {

    public static final ResourceLocation LASER_BULLET_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "mio_icif", "textures/entity/laser.png"
    );
    private static final float MAX_LENGTH = 2.6F;

    public mio_icif_LaserBullet(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(mio_icif_laser_bullet entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Vec3 motion = entity.getDeltaMovement();
        double speed = motion.length();
        if (speed < 1.0E-4) return;
        Vec3 dir = motion.scale(1 / speed);
        // grows out of the muzzle over the first ticks instead of popping in at full length
        float length = Math.min(MAX_LENGTH, (float) speed * Math.max(0.35F, entity.tickCount + partialTicks));
        Vec3 pos = entity.getPosition(partialTicks);
        Vec3 cam = this.entityRenderDispatcher.camera.getPosition();
        Vec3 toCam = cam.subtract(pos);
        Vec3 side = dir.cross(toCam);
        if (side.lengthSqr() < 1.0E-6) side = dir.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-6) side = new Vec3(1, 0, 0);
        side = side.normalize();
        Vector3f head = new Vector3f(0, 0, 0);
        Vector3f tail = new Vector3f((float) (-dir.x * length), (float) (-dir.y * length), (float) (-dir.z * length));
        Vector3f s = new Vector3f((float) side.x, (float) side.y, (float) side.z);

        float flicker = 0.9F + 0.1F * Mth.sin((entity.tickCount + partialTicks) * 2.7F + entity.getId());
        // Seen end-on (shooter's own view) the ribbons collapse into a wedge: fade them, keep the bloom.
        double align = Math.abs(dir.dot(toCam.normalize()));
        float sideFade = Mth.clamp((float) (1 - align) * 12F, 0.12F, 1F);
        VertexConsumer vc = buffer.getBuffer(SiRenderTypes.ENERGY_BEAM);
        Matrix4f m = poseStack.last().pose();
        ribbon(vc, m, head, tail, s, 0.11F * flicker, 255, 40, 30, (int) (46 * sideFade));    // halo
        ribbon(vc, m, head, tail, s, 0.045F, 255, 70, 50, (int) (150 * sideFade));            // crimson glow
        ribbon(vc, m, head, tail, s, 0.014F, 255, 235, 225, (int) (255 * sideFade));          // white-hot core

        // head bloom: two crossed camera-facing quads
        Vec3 up = side.cross(toCam.normalize()).normalize();
        Vector3f u = new Vector3f((float) up.x, (float) up.y, (float) up.z);
        bloom(vc, m, s, u, 0.085F * flicker, 255, 45, 35, 140);
        bloom(vc, m, s, u, 0.03F, 255, 240, 230, 255);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** Ribbon from head (full alpha) to tail (transparent), half-width {@code w}. */
    private static void ribbon(VertexConsumer vc, Matrix4f m, Vector3f head, Vector3f tail, Vector3f side, float w,
                               int r, int g, int b, int a) {
        float tw = w * 0.35F;
        vc.addVertex(m, head.x + side.x * w, head.y + side.y * w, head.z + side.z * w).setColor(r, g, b, a);
        vc.addVertex(m, head.x - side.x * w, head.y - side.y * w, head.z - side.z * w).setColor(r, g, b, a);
        vc.addVertex(m, tail.x - side.x * tw, tail.y - side.y * tw, tail.z - side.z * tw).setColor(r, g, b, 0);
        vc.addVertex(m, tail.x + side.x * tw, tail.y + side.y * tw, tail.z + side.z * tw).setColor(r, g, b, 0);
    }

    /** Round soft glow: a fan of 12 triangles (as degenerate quads) fading from the centre to the rim. */
    private static void bloom(VertexConsumer vc, Matrix4f m, Vector3f s, Vector3f u, float w, int r, int g, int b, int a) {
        int n = 12;
        for (int i = 0; i < n; i++) {
            float a0 = i * Mth.TWO_PI / n, a1 = (i + 1) * Mth.TWO_PI / n;
            float c0 = Mth.cos(a0) * w, s0 = Mth.sin(a0) * w, c1 = Mth.cos(a1) * w, s1 = Mth.sin(a1) * w;
            vc.addVertex(m, 0, 0, 0).setColor(r, g, b, a);
            vc.addVertex(m, s.x * c0 + u.x * s0, s.y * c0 + u.y * s0, s.z * c0 + u.z * s0).setColor(r, g, b, 0);
            vc.addVertex(m, s.x * c1 + u.x * s1, s.y * c1 + u.y * s1, s.z * c1 + u.z * s1).setColor(r, g, b, 0);
            vc.addVertex(m, 0, 0, 0).setColor(r, g, b, a);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(mio_icif_laser_bullet entity) {
        return LASER_BULLET_TEXTURE;
    }
}

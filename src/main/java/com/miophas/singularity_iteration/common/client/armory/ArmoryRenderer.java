// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.ArmoryMaintenance;
import com.miophas.singularity_iteration.common.armory.ArmoryPieceEntity;
import com.miophas.singularity_iteration.common.armory.mio_icif_armory;
import com.miophas.singularity_iteration.common.client.render.FxBudget;
import com.miophas.singularity_iteration.common.client.render.FxLodRules;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 0.1.10.32 (A-1): the Armory as a gantry.
 * <ul>
 *   <li><b>Iris hatch</b> on top: six blades ({@link ArmoryFxRules#blade}) twist open {@link ArmoryFxRules#HATCH_LEAD} ticks
 *       before a piece launches and while a returning piece comes in; the shaft below glows and a light column stands over
 *       it while open. The opening follows the pieces near the Armory (their ages), so every viewer sees the same.</li>
 *   <li><b>Suit icons</b>: small holograms of the stored suits (one piece each, synced by the Armory) circle above it, only
 *       within {@link ArmoryFxRules#ICON_RANGE} blocks.</li>
 *   <li><b>Maintenance</b>: while it repairs and charges, a ring with running arcs lies on the floor around it and a scan
 *       ring moves up and down its sides; waiting for power (amber) or mana (violet) the floor ring blinks.</li>
 * </ul>
 * Option {@code armory_fx}.
 */
public class ArmoryRenderer implements BlockEntityRenderer<mio_icif_armory> {
    static final ResourceLocation IRIS = ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/block/armory_iris.png");
    /** Top of the side panels of the Armory model (16.5 pixels). */
    static final float TOP = 16.5F / 16F;
    private final float[] blade = new float[6];

    public ArmoryRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(mio_icif_armory armory, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (armory.getLevel() == null || !FcsClientConfig.armoryFx()) return;
        var level = armory.getLevel();
        float time = level.getGameTime() + partial;
        Vec3 top = armory.launchTop();
        float open = 0F;
        for (ArmoryPieceEntity piece : level.getEntitiesOfClass(ArmoryPieceEntity.class, new AABB(top, top).inflate(24))) {
            if (piece.isReturn()) {
                if (piece.destination(partial).distanceToSqr(top) < 0.6 * 0.6) open = Math.max(open, ArmoryFxRules.hatch(piece.age(partial), piece.duration(), true));
            } else if (piece.start().distanceToSqr(top) < 0.6 * 0.6) {
                open = Math.max(open, ArmoryFxRules.hatch(piece.age(partial), piece.duration(), false));
            }
        }
        int above = LevelRenderer.getLightColor(level, armory.getBlockPos().above());
        int tier = FxBudget.tier(Vec3.atCenterOf(armory.getBlockPos()));
        iris(pose, buffers, open, above, tier);
        maintenance(pose, buffers, armory.shownMaintenance(), time, tier);
        if (Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(Vec3.atCenterOf(armory.getBlockPos()))
                < ArmoryFxRules.ICON_RANGE * ArmoryFxRules.ICON_RANGE) icons(pose, buffers, armory, time);
    }

    /** The six blades (opaque, world light), and while open the glowing shaft and the light column (additive). */
    private void iris(PoseStack pose, MultiBufferSource buffers, float open, int light, int tier) {
        Matrix4f m = pose.last().pose();
        if (open > 0.01F) {
            VertexConsumer glow = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
            int seg = FxLodRules.segments(18, tier);
            float r0 = ArmoryFxRules.IRIS_RADIUS * 0.98F;
            for (int s = 0; s < seg; s++) {
                double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
                float x0 = 0.5F + (float) Math.cos(a0) * r0, z0 = 0.5F + (float) Math.sin(a0) * r0;
                float x1 = 0.5F + (float) Math.cos(a1) * r0, z1 = 0.5F + (float) Math.sin(a1) * r0;
                put(glow, m, 0.5F, TOP + 0.001F, 0.5F, 0.6F, 0.95F, 1F, 0.95F * open);
                put(glow, m, x0, TOP + 0.001F, z0, 0.3F, 0.75F, 1F, 0.55F * open);
                put(glow, m, x1, TOP + 0.001F, z1, 0.3F, 0.75F, 1F, 0.55F * open);
                put(glow, m, 0.5F, TOP + 0.001F, 0.5F, 0.6F, 0.95F, 1F, 0.95F * open);
                if (tier < FxLodRules.FAR) {
                    // the light column over the open hatch
                    float rr = 0.2F * open;
                    float cx0 = 0.5F + (float) Math.cos(a0) * rr, cz0 = 0.5F + (float) Math.sin(a0) * rr;
                    float cx1 = 0.5F + (float) Math.cos(a1) * rr, cz1 = 0.5F + (float) Math.sin(a1) * rr;
                    put(glow, m, cx0, TOP, cz0, 0.4F, 0.85F, 1F, 0.3F * open);
                    put(glow, m, cx1, TOP, cz1, 0.4F, 0.85F, 1F, 0.3F * open);
                    put(glow, m, cx1, TOP + 1.4F, cz1, 0.4F, 0.85F, 1F, 0F);
                    put(glow, m, cx0, TOP + 1.4F, cz0, 0.4F, 0.85F, 1F, 0F);
                }
            }
        }
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(IRIS));
        PoseStack.Pose p = pose.last();
        float y = TOP + 0.003F, r = ArmoryFxRules.IRIS_RADIUS;
        for (int i = 0; i < 6; i++) {
            ArmoryFxRules.blade(i, open, blade);
            // planar mapping: the texture lies over the hatch, the centre in the middle
            blade(vc, p, blade[0], blade[1], y, light, r);
            blade(vc, p, blade[2], blade[3], y, light, r);
            blade(vc, p, blade[4], blade[5], y, light, r);
            blade(vc, p, blade[4], blade[5], y, light, r);
        }
    }

    private static void blade(VertexConsumer vc, PoseStack.Pose p, float x, float z, float y, int light, float r) {
        vc.addVertex(p, 0.5F + x, y, 0.5F + z).setColor(0xFFFFFFFF).setUv(0.5F + x / (2 * r), 0.5F + z / (2 * r))
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
    }

    /** Floor ring and scan ring of the maintenance (additive). */
    private static void maintenance(PoseStack pose, MultiBufferSource buffers, int state, float time, int tier) {
        if (state == ArmoryMaintenance.IDLE) return;
        float r, g, b, a;
        switch (state) {
            case ArmoryMaintenance.WORKING -> { r = 0.35F; g = 0.85F; b = 1F; a = 0.6F; }
            case ArmoryMaintenance.NEED_POWER -> { r = 1F; g = 0.66F; b = 0.22F; a = 0.35F + 0.35F * (float) Math.sin(time * 0.3F); }
            default -> { r = 0.72F; g = 0.47F; b = 1F; a = 0.35F + 0.35F * (float) Math.sin(time * 0.3F); }
        }
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        Matrix4f m = pose.last().pose();
        int seg = FxLodRules.segments(40, tier);
        for (int s = 0; s < seg; s++) {
            double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
            // running arcs: three bright arcs travel round the ring while it works
            float arc = state == ArmoryMaintenance.WORKING ? 0.35F + 0.65F * (float) Math.pow(Math.max(0, Math.cos(a0 * 3 - time * 0.12F)), 6) : 1F;
            ring(vc, m, a0, a1, 0.82F, 0.9F, 0.015F, r, g, b, a * arc);
        }
        if (state == ArmoryMaintenance.WORKING) {
            // the scan ring runs up and down the sides of the Armory
            float h = 0.05F + 0.9F * (0.5F + 0.5F * (float) Math.sin(time * 0.08F));
            float e0 = -0.02F, e1 = 1.02F;
            square(vc, m, e0, e1, h, 0.03F, r, g, b, 0.55F);
        }
    }

    private static void ring(VertexConsumer vc, Matrix4f m, double a0, double a1, float r0, float r1, float y, float r, float g, float b, float a) {
        float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
        put(vc, m, 0.5F + c0 * r0, y, 0.5F + s0 * r0, r, g, b, a);
        put(vc, m, 0.5F + c1 * r0, y, 0.5F + s1 * r0, r, g, b, a);
        put(vc, m, 0.5F + c1 * r1, y, 0.5F + s1 * r1, r, g, b, 0F);
        put(vc, m, 0.5F + c0 * r1, y, 0.5F + s0 * r1, r, g, b, 0F);
    }

    /** A thin band round the four sides of the block at height {@code h}. */
    private static void square(VertexConsumer vc, Matrix4f m, float e0, float e1, float h, float w, float r, float g, float b, float a) {
        float[][] edges = {{e0, e0, e1, e0}, {e1, e0, e1, e1}, {e1, e1, e0, e1}, {e0, e1, e0, e0}};
        for (float[] e : edges) {
            put(vc, m, e[0], h - w, e[1], r, g, b, 0F);
            put(vc, m, e[2], h - w, e[3], r, g, b, 0F);
            put(vc, m, e[2], h, e[3], r, g, b, a);
            put(vc, m, e[0], h, e[1], r, g, b, a);
            put(vc, m, e[0], h, e[1], r, g, b, a);
            put(vc, m, e[2], h, e[3], r, g, b, a);
            put(vc, m, e[2], h + w, e[3], r, g, b, 0F);
            put(vc, m, e[0], h + w, e[1], r, g, b, 0F);
        }
    }

    /** The stored suits as small holograms circling above the Armory. */
    private static void icons(PoseStack pose, MultiBufferSource buffers, mio_icif_armory armory, float time) {
        List<ItemStack> icons = armory.icons();
        Minecraft mc = Minecraft.getInstance();
        float flicker = 0.9F + 0.1F * (float) Math.sin(time * 0.37F);
        for (int i = 0; i < icons.size(); i++) {
            ItemStack stack = icons.get(i);
            if (stack.isEmpty()) continue;
            double angle = ArmoryFxRules.iconAngle(i, icons.size(), time);
            pose.pushPose();
            pose.translate(0.5 + Math.cos(angle) * ArmoryFxRules.ICON_RADIUS, ArmoryFxRules.ICON_HEIGHT + 0.04 * Math.sin(time * 0.1 + i),
                0.5 + Math.sin(angle) * ArmoryFxRules.ICON_RADIUS);
            pose.mulPose(Axis.YP.rotation((float) (-angle + Math.PI / 2)));
            pose.scale(0.3F, 0.3F, 0.3F);
            var holo = new ArmorShowcaseRenderer.Holo(new Matrix4f(pose.last().pose()).invert(), flicker, time, -1F, new float[0]);
            mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose,
                new ArmorShowcaseRenderer.HoloSource(buffers, holo, false), armory.getLevel(), i);
            pose.popPose();
        }
    }

    private static void put(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r, float g, float b, float a) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, Math.max(0F, Math.min(1F, a)));
    }

    @Override
    public AABB getRenderBoundingBox(mio_icif_armory armory) {
        var p = armory.getBlockPos();
        return new AABB(p.getX() - 0.6, p.getY() - 0.1, p.getZ() - 0.6, p.getX() + 1.6, p.getY() + 2.6, p.getZ() + 1.6);
    }

    @Override
    public int getViewDistance() { return 48; }
}

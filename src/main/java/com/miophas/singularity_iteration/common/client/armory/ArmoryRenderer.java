// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.ArmoryBlock;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 1x2 Armory linear railgun silo renderer:
 * <ul>
 *   <li><b>Recessed iris hatch &amp; 3-stage railgun pulse</b>: six blades ({@link ArmoryFxRules#blade}) sit inside the
 *       recessed octagonal silo well ({@link #TOP}); while open, the silo core glows, 3-stage electromagnetic acceleration
 *       pulses travel vertically up the twin rails, and a cyan launch column shoots out of the open corridor.</li>
 *   <li><b>Suit holograms &amp; proximity wake-up</b>: stored suit holograms float centered between the twin railgun pylons
 *       over a soft projection cone; approaching within {@link ArmoryFxRules#WAKE_RANGE} blocks wakes the railgun stator
 *       nodes and holo-projector glow on a powered Armory.</li>
 *   <li><b>Maintenance</b>: floor ring with running arcs, 3-stage rail stator status glow (cyan working, amber power,
 *       violet mana), and a horizontal laser scan plane sweeping vertically between the twin rails.</li>
 * </ul>
 * Option {@code armory_fx}.
 */
public class ArmoryRenderer implements BlockEntityRenderer<mio_icif_armory> {
    static final ResourceLocation IRIS = ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/block/armory_iris.png");
    /** Recessed inside the octagonal launch silo well (14.5 pixels, tucked below the 15.5px rim). */
    static final float TOP = 14.5F / 16F;
    /** World heights (in blocks above lower base) of the 3 electromagnetic stator stages on the twin pylons. */
    private static final float[] RAIL_STAGES = {(16F + 4.95F) / 16F, (16F + 8.55F) / 16F, (16F + 12.15F) / 16F};
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
        BlockState state = armory.getBlockState();
        boolean lit = state.hasProperty(ArmoryBlock.LIT) && state.getValue(ArmoryBlock.LIT);
        Direction facing = state.hasProperty(ArmoryBlock.FACING) ? state.getValue(ArmoryBlock.FACING) : Direction.NORTH;
        Vec3 center = Vec3.atCenterOf(armory.getBlockPos());
        double camDistSqr = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(center);
        float wake = lit ? ArmoryFxRules.wake(camDistSqr) : 0F;

        int above = LevelRenderer.getLightColor(level, armory.getBlockPos().above());
        int tier = FxBudget.tier(center);
        irisAndRailPulse(pose, buffers, open, wake, facing, time, above, tier);
        maintenance(pose, buffers, armory.shownMaintenance(), facing, time, tier);
        if (camDistSqr < ArmoryFxRules.ICON_RANGE * ArmoryFxRules.ICON_RANGE) {
            icons(pose, buffers, armory, wake, time, tier);
        }
    }

    /** Recessed six-blade iris, silo core glow, 3-stage vertical railgun pulses, and open-corridor launch column. */
    private void irisAndRailPulse(PoseStack pose, MultiBufferSource buffers, float open, float wake, Direction facing, float time, int light, int tier) {
        Matrix4f m = pose.last().pose();
        if (open > 0.01F || wake > 0.01F) {
            VertexConsumer glow = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
            int seg = FxLodRules.segments(20, tier);
            if (open > 0.01F) {
                float r0 = ArmoryFxRules.IRIS_RADIUS * 0.96F;
                for (int s = 0; s < seg; s++) {
                    double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
                    float x0 = 0.5F + (float) Math.cos(a0) * r0, z0 = 0.5F + (float) Math.sin(a0) * r0;
                    float x1 = 0.5F + (float) Math.cos(a1) * r0, z1 = 0.5F + (float) Math.sin(a1) * r0;
                    put(glow, m, 0.5F, TOP + 0.001F, 0.5F, 0.65F, 0.96F, 1F, 0.95F * open);
                    put(glow, m, x0, TOP + 0.001F, z0, 0.28F, 0.78F, 1F, 0.55F * open);
                    put(glow, m, x1, TOP + 0.001F, z1, 0.28F, 0.78F, 1F, 0.55F * open);
                    put(glow, m, 0.5F, TOP + 0.001F, 0.5F, 0.65F, 0.96F, 1F, 0.95F * open);
                    if (tier < FxLodRules.FAR) {
                        // Launch beam shoots straight up through the open twin-rail corridor
                        float rr = 0.19F * open;
                        float cx0 = 0.5F + (float) Math.cos(a0) * rr, cz0 = 0.5F + (float) Math.sin(a0) * rr;
                        float cx1 = 0.5F + (float) Math.cos(a1) * rr, cz1 = 0.5F + (float) Math.sin(a1) * rr;
                        put(glow, m, cx0, TOP, cz0, 0.42F, 0.88F, 1F, 0.34F * open);
                        put(glow, m, cx1, TOP, cz1, 0.42F, 0.88F, 1F, 0.34F * open);
                        put(glow, m, cx1, TOP + 2.05F, cz1, 0.42F, 0.88F, 1F, 0F);
                        put(glow, m, cx0, TOP + 2.05F, cz0, 0.42F, 0.88F, 1F, 0F);
                    }
                }
            }
            // 3-Stage Linear Railgun Stator Pulses traveling up the twin pylons (strong on launch, soft hum on wake)
            boolean railsOnX = facing.getAxis() == Direction.Axis.Z;
            for (int st = 0; st < RAIL_STAGES.length; st++) {
                float sy = RAIL_STAGES[st];
                float wave = open > 0.01F
                    ? open * (0.45F + 0.55F * (float) Math.pow(Math.max(0, Math.sin(time * 0.42F - st * 1.15F)), 2))
                    : wake * (0.20F + 0.12F * (float) Math.sin(time * 0.14F - st * 0.9F));
                if (wave > 0.01F) {
                    railBridge(glow, m, railsOnX, sy, 0.022F + 0.018F * open, 0.36F, 0.92F, 1F, wave * 0.72F);
                }
            }
        }
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(IRIS));
        PoseStack.Pose p = pose.last();
        float y = TOP + 0.003F, r = ArmoryFxRules.IRIS_RADIUS;
        for (int i = 0; i < 6; i++) {
            ArmoryFxRules.blade(i, open, blade);
            blade(vc, p, blade[0], blade[1], y, light, r);
            blade(vc, p, blade[2], blade[3], y, light, r);
            blade(vc, p, blade[4], blade[5], y, light, r);
            blade(vc, p, blade[4], blade[5], y, light, r);
        }
    }

    /** Horizontal electromagnetic field ribbon bridging the twin vertical rails at height {@code y}. */
    private static void railBridge(VertexConsumer vc, Matrix4f m, boolean railsOnX, float y, float halfH, float r, float g, float b, float a) {
        float x0 = railsOnX ? 0.23F : 0.44F, x1 = railsOnX ? 0.77F : 0.56F;
        float z0 = railsOnX ? 0.44F : 0.23F, z1 = railsOnX ? 0.56F : 0.77F;
        // Horizontal glow band between the facing stator pads
        put(vc, m, x0, y, z0, r, g, b, a);
        put(vc, m, x1, y, z0, r, g, b, a);
        put(vc, m, x1, y, z1, r, g, b, a);
        put(vc, m, x0, y, z1, r, g, b, a);
        // Vertical glowing flare along the two facing stator faces
        if (railsOnX) {
            for (float rx : new float[]{0.235F, 0.765F}) {
                put(vc, m, rx, y - halfH, z0, r, g, b, 0F);
                put(vc, m, rx, y - halfH, z1, r, g, b, 0F);
                put(vc, m, rx, y + halfH, z1, r, g, b, a);
                put(vc, m, rx, y + halfH, z0, r, g, b, a);
            }
        } else {
            for (float rz : new float[]{0.235F, 0.765F}) {
                put(vc, m, x0, y - halfH, rz, r, g, b, 0F);
                put(vc, m, x1, y - halfH, rz, r, g, b, 0F);
                put(vc, m, x1, y + halfH, rz, r, g, b, a);
                put(vc, m, x0, y + halfH, rz, r, g, b, a);
            }
        }
    }

    private static void blade(VertexConsumer vc, PoseStack.Pose p, float x, float z, float y, int light, float r) {
        vc.addVertex(p, 0.5F + x, y, 0.5F + z).setColor(0xFFFFFFFF).setUv(0.5F + x / (2 * r), 0.5F + z / (2 * r))
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
    }

    /** Floor ring, 3-stage rail status glow, and between-rails laser scan plane during maintenance (additive). */
    private static void maintenance(PoseStack pose, MultiBufferSource buffers, int state, Direction facing, float time, int tier) {
        if (state == ArmoryMaintenance.IDLE) return;
        float r, g, b, a;
        switch (state) {
            case ArmoryMaintenance.WORKING -> { r = 0.35F; g = 0.85F; b = 1F; a = 0.62F; }
            case ArmoryMaintenance.NEED_POWER -> { r = 1F; g = 0.66F; b = 0.22F; a = 0.35F + 0.35F * (float) Math.sin(time * 0.3F); }
            default -> { r = 0.72F; g = 0.47F; b = 1F; a = 0.35F + 0.35F * (float) Math.sin(time * 0.3F); }
        }
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        Matrix4f m = pose.last().pose();
        int seg = FxLodRules.segments(36, tier);
        for (int s = 0; s < seg; s++) {
            double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
            float arc = state == ArmoryMaintenance.WORKING ? 0.35F + 0.65F * (float) Math.pow(Math.max(0, Math.cos(a0 * 3 - time * 0.12F)), 6) : 1F;
            ring(vc, m, a0, a1, 0.80F, 0.89F, 0.015F, r, g, b, a * arc);
        }
        boolean railsOnX = facing.getAxis() == Direction.Axis.Z;
        // Mirror maintenance status color on the 3 vertical rail stator stages
        for (int st = 0; st < RAIL_STAGES.length; st++) {
            float pulse = state == ArmoryMaintenance.WORKING
                ? 0.45F + 0.55F * (float) Math.sin(time * 0.16F - st * 0.8F)
                : 1F;
            railBridge(vc, m, railsOnX, RAIL_STAGES[st], 0.025F, r, g, b, a * 0.45F * Math.max(0.15F, pulse));
        }
        if (state == ArmoryMaintenance.WORKING) {
            // Horizontal laser scan plane sweeping vertically between the twin railgun pylons
            float h = 0.98F + 0.78F * (0.5F + 0.5F * (float) Math.sin(time * 0.09F));
            float x0 = railsOnX ? 0.23F : 0.28F, x1 = railsOnX ? 0.77F : 0.72F;
            float z0 = railsOnX ? 0.28F : 0.23F, z1 = railsOnX ? 0.72F : 0.77F;
            put(vc, m, x0, h, z0, r, g, b, 0.22F);
            put(vc, m, x1, h, z0, r, g, b, 0.22F);
            put(vc, m, x1, h, z1, r, g, b, 0.22F);
            put(vc, m, x0, h, z1, r, g, b, 0.22F);
            square(vc, m, x0, x1, z0, z1, h, 0.025F, r, g, b, 0.65F);
        }
    }

    private static void ring(VertexConsumer vc, Matrix4f m, double a0, double a1, float r0, float r1, float y, float r, float g, float b, float a) {
        float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
        put(vc, m, 0.5F + c0 * r0, y, 0.5F + s0 * r0, r, g, b, a);
        put(vc, m, 0.5F + c1 * r0, y, 0.5F + s1 * r0, r, g, b, a);
        put(vc, m, 0.5F + c1 * r1, y, 0.5F + s1 * r1, r, g, b, 0F);
        put(vc, m, 0.5F + c0 * r1, y, 0.5F + s0 * r1, r, g, b, 0F);
    }

    /** A thin glowing laser perimeter at height {@code h}. */
    private static void square(VertexConsumer vc, Matrix4f m, float x0, float x1, float z0, float z1, float h, float w, float r, float g, float b, float a) {
        float[][] edges = {{x0, z0, x1, z0}, {x1, z0, x1, z1}, {x1, z1, x0, z1}, {x0, z1, x0, z0}};
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

    /** Stored suits as holograms orbiting inside the twin-rail corridor over a soft projection cone. */
    private static void icons(PoseStack pose, MultiBufferSource buffers, mio_icif_armory armory, float wake, float time, int tier) {
        List<ItemStack> icons = armory.icons();
        int count = 0;
        for (ItemStack s : icons) if (!s.isEmpty()) count++;
        if (count == 0 && wake <= 0.01F) return;

        if (tier < FxLodRules.FAR) {
            VertexConsumer glow = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
            Matrix4f m = pose.last().pose();
            int seg = FxLodRules.segments(16, tier);
            float coneAlpha = (count > 0 ? 0.14F : 0.07F) + 0.08F * wake;
            float rBot = 0.12F, rTop = 0.28F;
            float yBot = TOP + 0.02F, yTop = ArmoryFxRules.ICON_HEIGHT + 0.18F;
            for (int s = 0; s < seg; s++) {
                double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
                float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                put(glow, m, 0.5F + c0 * rBot, yBot, 0.5F + s0 * rBot, 0.30F, 0.88F, 1F, coneAlpha);
                put(glow, m, 0.5F + c1 * rBot, yBot, 0.5F + s1 * rBot, 0.30F, 0.88F, 1F, coneAlpha);
                put(glow, m, 0.5F + c1 * rTop, yTop, 0.5F + s1 * rTop, 0.30F, 0.88F, 1F, 0F);
                put(glow, m, 0.5F + c0 * rTop, yTop, 0.5F + s0 * rTop, 0.30F, 0.88F, 1F, 0F);
            }
        }
        if (count == 0) return;

        Minecraft mc = Minecraft.getInstance();
        float flicker = 0.9F + 0.1F * (float) Math.sin(time * 0.37F);
        int idx = 0;
        for (int i = 0; i < icons.size(); i++) {
            ItemStack stack = icons.get(i);
            if (stack.isEmpty()) continue;
            double angle = ArmoryFxRules.iconAngle(idx++, count, time);
            float radius = count == 1 ? 0F : ArmoryFxRules.ICON_RADIUS;
            pose.pushPose();
            pose.translate(0.5 + Math.cos(angle) * radius, ArmoryFxRules.ICON_HEIGHT + 0.035 * Math.sin(time * 0.1 + i),
                0.5 + Math.sin(angle) * radius);
            pose.mulPose(Axis.YP.rotation((float) (-angle + Math.PI / 2)));
            float sc = count == 1 ? 0.36F : 0.28F;
            pose.scale(sc, sc, sc);
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
        return new AABB(p.getX() - 0.6, p.getY() - 0.1, p.getZ() - 0.6, p.getX() + 1.6, p.getY() + 3.2, p.getZ() + 1.6);
    }

    @Override
    public int getViewDistance() { return 48; }
}

// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.WingmanCommandPackets.Unit;
import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.armory.WingmanOrder;
import com.miophas.singularity_iteration.common.client.render.FxDraw;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.client.suit.FcsState;
import com.miophas.singularity_iteration.common.client.suit.OreScanner;
import com.miophas.singularity_iteration.common.client.suit.nano.QuantumClient;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 3D holo table (0.1.10.2, holomap Mk.II): a hologram of the terrain around the wearer, projected
 * into the world 1.25 blocks ahead at waist height, 1.5 blocks across.
 *
 * <h2>Model</h2>
 * The table samples the surface (the highest block, its map colour, water) of {@code cells x cells}
 * columns (client option, 49 by default) over a square of {@link #radius} blocks around the place
 * where it opened (24, 48 or 96 blocks: the mouse wheel), again every 2 s. A world point
 * {@code (x, y, z)} maps to {@code centre + R(rot) (x - originX, (y - baseY) * VERTICAL, z - originZ) * k}
 * with {@code k = 0.75 / radius}: the miniature is aligned with the world (its north is north)
 * until the wearer turns it (drag).
 *
 * <h2>Look</h2>
 * Additive light, so no depth sorting: each column is a top face and the side faces down to a lower
 * neighbour, tinted from the block colour towards cyan, with diagonal interference bands and a scan
 * line that sweeps the table. A projector frame with corner brackets and a north notch. Opening: the
 * projector beams reach out from the chest and the columns rise from the centre outwards; closing
 * folds them back. Markers: the wearer (white, with the heading), the Wingmen (cyan, with their
 * numbers as segment digits, order lines and points), hostile creatures (red stalks; the ones the
 * Wingmen see through the data link brighter, with a ring), other players (amber), the scanned ores
 * with Ore Hologram, and the hovered column or creature.
 */
public final class HoloTable {
    private HoloTable() {}

    static final float SIZE = 1.5F, VERTICAL = 1.3F;
    private static final int NONE = Integer.MIN_VALUE;
    static final int[] RADII = {24, 48, 96};

    static boolean wantOpen;
    static float deploy, prevDeploy;
    static Vec3 centre = Vec3.ZERO;
    static float rot;
    static int originX, originZ, baseY, radius = 48, cells = 49;
    static int[] heights = new int[0], colors = new int[0];
    static boolean[] water = new boolean[0];
    private static int sampleAge;
    /** Hover state from the screen: a column index or -1, a creature id or -1. */
    static int hoverCell = -1, hoverEntity = -1;

    /** True when the wearer can open the table: an active holomap Mk.II. */
    public static boolean available() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && FcsState.active(SuitModuleType.HOLOMAP) && SuitModules.tier(p, SuitModuleType.HOLOMAP) >= 2;
    }

    /** Opens the table screen (or says why not). */
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!available()) {
            mc.player.displayClientMessage(Component.translatable("message.mio_icif.command.holo_needs_mk2"), true);
            return;
        }
        mc.setScreen(new HoloTableScreen());
    }

    /** Called by the screen when it opens: anchors the table in front of the wearer and samples the terrain. */
    static void begin() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        double yaw = Math.toRadians(p.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        centre = p.getEyePosition(1.0F).add(forward.scale(1.25)).add(0, -0.62, 0);
        if (!wantOpen && deploy <= 0) rot = 0;
        wantOpen = true;
        resample();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(mio_icif_sounds.HOLO_OPEN.get(), 1.0F, 0.6F));
    }

    /** Folds the table away ({@code instant}: no animation). */
    static void close(boolean instant) {
        if (wantOpen) {
            Minecraft mc = Minecraft.getInstance();
            if (!instant && mc.player != null) mc.getSoundManager().play(SimpleSoundInstance.forUI(mio_icif_sounds.HOLO_CLOSE.get(), 1.0F, 0.5F));
        }
        wantOpen = false;
        if (instant) deploy = prevDeploy = 0;
    }

    static void tick() {
        prevDeploy = deploy;
        deploy = Mth.clamp(deploy + (wantOpen ? 0.09F : -0.14F), 0, 1);
        if (wantOpen && ++sampleAge >= 40) resample();
        if (wantOpen && !available()) {
            close(false);
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof HoloTableScreen) mc.setScreen(null);
        }
    }

    static void zoom(int direction) {
        int i = 0;
        for (int k = 0; k < RADII.length; k++) if (RADII[k] == radius) i = k;
        int next = RADII[Mth.clamp(i + direction, 0, RADII.length - 1)];
        if (next == radius) return;
        radius = next;
        resample();
    }

    static int stride() {
        return Math.max(1, Math.round(radius * 2F / (cells - 1)));
    }

    /** Samples the surface around the wearer (client chunks only; a column out of the loaded area is empty). */
    static void resample() {
        sampleAge = 0;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer p = mc.player;
        if (level == null || p == null) return;
        cells = com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.holoTableCells();
        int stride = stride();
        originX = p.getBlockX();
        originZ = p.getBlockZ();
        baseY = p.getBlockY();
        int n = cells * cells;
        if (heights.length != n) { heights = new int[n]; colors = new int[n]; water = new boolean[n]; }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int half = cells / 2;
        for (int i = 0; i < cells; i++) {
            for (int j = 0; j < cells; j++) {
                int k = i * cells + j;
                int x = originX + (i - half) * stride, z = originZ + (j - half) * stride;
                m.set(x, baseY, z);
                if (!level.hasChunkAt(m)) { heights[k] = NONE; continue; }
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                m.set(x, top, z);
                BlockState state = level.getBlockState(m);
                int c = state.getMapColor(level, m).col;
                if (c == 0) c = MapColor.STONE.col;
                water[k] = !state.getFluidState().isEmpty();
                heights[k] = top;
                colors[k] = c;
            }
        }
    }

    static double k() {
        return SIZE / 2 / radius;
    }

    /** World point to its place on the table. */
    static Vec3 toTable(double x, double y, double z) {
        double dx = x - originX - 0.5, dz = z - originZ - 0.5;
        double dy = Mth.clamp(y - baseY, -40, 40) * VERTICAL;
        double c = Math.cos(rot), s = Math.sin(rot);
        double lx = dx * c - dz * s, lz = dx * s + dz * c;
        double kk = k();
        return centre.add(lx * kk, dy * kk, lz * kk);
    }

    /** True when the world point lies on the table square. */
    static boolean onTable(double x, double z) {
        return Math.abs(x - originX - 0.5) <= radius && Math.abs(z - originZ - 0.5) <= radius;
    }

    /** World position of the top centre of column {@code index}. */
    static Vec3 columnTop(int index) {
        int half = cells / 2, stride = stride();
        int i = index / cells, j = index % cells;
        return new Vec3(originX + (i - half) * stride + 0.5, heights[index] + 1, originZ + (j - half) * stride + 0.5);
    }

    // ------------------------------------------------------------------ render

    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float d = Mth.lerp(partial, prevDeploy, deploy);
        if (d <= 0.001F || mc.level == null || mc.player == null || heights.length == 0) return;
        float t = SquadClient.ticks + partial;
        Vec3 cam = event.getCamera().getPosition();
        Vector3f left = event.getCamera().getLeftVector(), up = event.getCamera().getUpVector();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        float ease = d * d * (3 - 2 * d);
        frame(vc, m, cam, mc.player, t, ease, partial);
        terrain(vc, m, t, d);
        if (ease > 0.6F) markers(vc, m, cam, left, up, mc, t, partial, (ease - 0.6F) / 0.4F);
        buffers.endBatch(SiRenderTypes.FIELD_GLOW);
        pose.popPose();
    }

    private static void frame(VertexConsumer vc, Matrix4f m, Vec3 cam, LocalPlayer p, float t, float ease, float partial) {
        double half = SIZE / 2 * ease;
        Vec3 base = centre.add(0, -0.03, 0);
        double c = Math.cos(rot), s = Math.sin(rot);
        Vec3 ax = new Vec3(c, 0, s), az = new Vec3(-s, 0, c);
        Vec3[] corner = {base.add(ax.scale(-half)).add(az.scale(-half)), base.add(ax.scale(half)).add(az.scale(-half)),
            base.add(ax.scale(half)).add(az.scale(half)), base.add(ax.scale(-half)).add(az.scale(half))};
        for (int i = 0; i < 4; i++) {
            Vec3 a = corner[i], b = corner[(i + 1) % 4];
            FxDraw.line(vc, m, a, b, cam, 0.008F, 0.36F, 0.92F, 1F, 0.55F * ease);
            Vec3 along = b.subtract(a).normalize().scale(0.12), back = corner[(i + 3) % 4].subtract(a).normalize().scale(0.12);
            FxDraw.line(vc, m, a, a.add(along), cam, 0.018F, 0.8F, 1F, 1F, 0.9F * ease);
            FxDraw.line(vc, m, a, a.add(back), cam, 0.018F, 0.8F, 1F, 1F, 0.9F * ease);
        }
        // north notch (world -Z on the table)
        Vec3 north = base.add(az.scale(-half - 0.06));
        FxDraw.line(vc, m, north.add(ax.scale(-0.05)), north.add(az.scale(-0.08)), cam, 0.012F, 1F, 0.45F, 0.4F, 0.9F * ease);
        FxDraw.line(vc, m, north.add(ax.scale(0.05)), north.add(az.scale(-0.08)), cam, 0.012F, 1F, 0.45F, 0.4F, 0.9F * ease);
        // projector: beams from the chest to the corners, strongest while the table unfolds
        Vec3 chest = p.getPosition(partial).add(0, 1.15, 0);
        float beam = 0.08F + 0.5F * (float) Math.sin(Math.PI * Mth.clamp(ease, 0, 1));
        for (Vec3 a : corner) FxDraw.beam(vc, m, chest, a, cam, 0.01F, 0.36F, 0.92F, 1F, 0.35F * beam, 0.1F * beam);
        FxDraw.disc(vc, m, base, new Vec3(0, 1, 0), (float) half * 1.1F, 0.2F, 0.6F, 0.8F, 0.08F * ease, 32);
    }

    private static void terrain(VertexConsumer vc, Matrix4f m, float t, float deployRaw) {
        int stride = stride(), half = cells / 2;
        double hs = stride / 2.0;
        double sweep = ((t * 0.012) % 1.4 - 0.2) * 2 - 1;                      // -1.4 .. 1.4 across the table (local x)
        float fold = deployRaw * deployRaw * (3 - 2 * deployRaw);
        for (int i = 0; i < cells; i++) {
            for (int j = 0; j < cells; j++) {
                int k = i * cells + j;
                if (heights[k] == NONE) continue;
                double wx = originX + (i - half) * stride + 0.5, wz = originZ + (j - half) * stride + 0.5;
                double rn = Math.sqrt((i - half) * (i - half) + (j - half) * (j - half)) / Math.max(1, half);
                float rise = Mth.clamp((float) (fold * 1.7 - rn * 0.7), 0, 1);
                if (rise <= 0.01F) continue;
                double top = baseY + (heights[k] + 1 - baseY) * rise;
                int col = colors[k];
                float cr = ((col >> 16) & 255) / 255F, cg = ((col >> 8) & 255) / 255F, cb = (col & 255) / 255F;
                // hologram tint: towards cyan, water bluer
                float mix = water[k] ? 0.4F : 0.62F;
                float r = Mth.lerp(mix, cr, 0.3F), g = Mth.lerp(mix, cg, 0.85F), b = Mth.lerp(mix, cb, 1.0F);
                if (water[k]) { r *= 0.6F; g *= 0.85F; }
                Vec3 local = toTable(wx, 0, wz).subtract(centre);
                double lx = local.x / (SIZE / 2), lz = local.z / (SIZE / 2);
                float fringe = 0.78F + 0.22F * (float) Math.sin((lx + lz) * 26 + t * 0.25);
                float scan = (float) Math.max(0, 1 - Math.abs(lx - sweep) / 0.05);
                float a = (0.11F + 0.25F * scan) * fringe * rise;
                if (k == hoverCell) a = 0.5F;
                Vec3 c00 = toTable(wx - hs, top, wz - hs), c10 = toTable(wx + hs, top, wz - hs);
                Vec3 c11 = toTable(wx + hs, top, wz + hs), c01 = toTable(wx - hs, top, wz + hs);
                quad(vc, m, c00, c10, c11, c01, r, g, b, a);
                // sides down to a lower neighbour (or to the base at the table edge)
                side(vc, m, i + 1 < cells ? heights[k + cells] : NONE, heights[k], rise, wx + hs, wz - hs, wx + hs, wz + hs, r, g, b, a * 0.55F);
                side(vc, m, i > 0 ? heights[k - cells] : NONE, heights[k], rise, wx - hs, wz + hs, wx - hs, wz - hs, r, g, b, a * 0.55F);
                side(vc, m, j + 1 < cells ? heights[k + 1] : NONE, heights[k], rise, wx + hs, wz + hs, wx - hs, wz + hs, r, g, b, a * 0.45F);
                side(vc, m, j > 0 ? heights[k - 1] : NONE, heights[k], rise, wx - hs, wz - hs, wx + hs, wz - hs, r, g, b, a * 0.45F);
            }
        }
    }

    private static void side(VertexConsumer vc, Matrix4f m, int neighbour, int self, float rise, double x0, double z0, double x1, double z1,
                             float r, float g, float b, float a) {
        int low = neighbour == NONE ? Math.min(self, baseY - 8) : neighbour;
        if (low >= self) return;
        double topY = baseY + (self + 1 - baseY) * rise, lowY = baseY + (low + 1 - baseY) * rise;
        quad(vc, m, toTable(x0, topY, z0), toTable(x1, topY, z1), toTable(x1, lowY, z1), toTable(x0, lowY, z0), r, g, b, a);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float r, float g, float bl, float alpha) {
        FxDraw.vertex(vc, m, a, r, g, bl, alpha);
        FxDraw.vertex(vc, m, b, r, g, bl, alpha);
        FxDraw.vertex(vc, m, c, r, g, bl, alpha);
        FxDraw.vertex(vc, m, d, r, g, bl, alpha);
    }

    private static void markers(VertexConsumer vc, Matrix4f m, Vec3 cam, Vector3f leftV, Vector3f upV, Minecraft mc, float t, float partial, float a) {
        Vec3 l = new Vec3(leftV.x(), leftV.y(), leftV.z()), u = new Vec3(upV.x(), upV.y(), upV.z());
        float pin = (float) (SIZE / 2 * 0.035);
        // the wearer
        LocalPlayer p = mc.player;
        Vec3 self = toTable(p.getX(), p.getY() + 1, p.getZ());
        diamond(vc, m, self, cam, pin * 1.3F, 1F, 1F, 1F, a);
        double yaw = Math.toRadians(p.getYRot());
        Vec3 ahead = toTable(p.getX() - Math.sin(yaw) * radius * 0.12, p.getY() + 1, p.getZ() + Math.cos(yaw) * radius * 0.12);
        FxDraw.line(vc, m, self, ahead, cam, 0.006F, 1F, 1F, 1F, 0.8F * a);
        // creatures and players on the table
        AABB box = new AABB(originX - radius, baseY - 48, originZ - radius, originX + radius + 1, baseY + 48, originZ + radius + 1);
        var seen = QuantumClient.datalinkSeen();
        for (Entity e : mc.level.getEntities((Entity) null, box, e -> e instanceof LivingEntity && e != p && !(e instanceof WingmanEntity) && e.isAlive())) {
            Vec3 at = e.getPosition(partial);
            if (!onTable(at.x, at.z)) continue;
            boolean hostile = e instanceof Enemy, player = e instanceof Player, linked = seen.contains(e.getId());
            if (!hostile && !player && !linked) continue;
            Vec3 head = toTable(at.x, at.y + e.getBbHeight() + 2, at.z), foot = toTable(at.x, at.y, at.z);
            float[] c = player ? new float[]{1F, 0.8F, 0.3F} : new float[]{1F, 0.3F, 0.25F};
            float al = (linked ? 1F : 0.65F) * a;
            FxDraw.line(vc, m, foot, head, cam, 0.004F, c[0], c[1], c[2], al * 0.6F);
            diamond(vc, m, head, cam, pin * (linked ? 1.0F : 0.75F), c[0], c[1], c[2], al);
            if (linked) FxDraw.ring(vc, m, head, head.subtract(cam), pin * 1.6F, pin * 0.5F, 0.44F, 0.95F, 0.84F, 0.8F * a, 16);
            if (e.getId() == hoverEntity) FxDraw.ring(vc, m, head, head.subtract(cam), pin * 2.2F, pin * 0.6F, 1F, 1F, 1F, 0.9F, 20);
        }
        // the Wingmen, their numbers, orders and order points
        for (Unit unit : SquadClient.units()) {
            if (unit.where() != 0) continue;
            Vec3 at = SquadClient.position(unit, partial);
            if (!onTable(at.x, at.z)) continue;
            Vec3 w = toTable(at.x, at.y + 1, at.z);
            boolean sel = unit.id().equals(SquadClient.selected());
            diamond(vc, m, w, cam, pin * (sel ? 1.5F : 1.15F), 0.36F, 0.92F, 1F, a);
            digit(vc, m, w.add(u.scale(pin * 2.6)), cam, l, u, pin * 1.1F, SquadClient.index(unit) + 1, a);
            WingmanOrder o = SquadClient.order(unit);
            Vec3 point = SquadClient.orderPoint(unit);
            if (point != null && onTable(point.x, point.z)) {
                float[] c = CommandFx.orderColor(o);
                Vec3 pt = toTable(point.x, point.y + 0.2, point.z);
                FxDraw.line(vc, m, w, pt, cam, 0.004F, c[0], c[1], c[2], 0.6F * a);
                FxDraw.ring(vc, m, pt, new Vec3(0, 1, 0), pin * 1.2F, pin * 0.5F, c[0], c[1], c[2], 0.9F * a, 18);
                if (o == WingmanOrder.SUPPRESS) {
                    float rr = (float) (SquadClient.suppressRadius() * k());
                    FxDraw.ring(vc, m, pt, new Vec3(0, 1, 0), rr, pin * 0.4F, c[0], c[1], c[2], 0.6F * a, 40);
                }
            }
        }
        // scanned ores (Ore Hologram)
        if (QuantumClient.activeLinks().contains("ore_holo")) {
            for (OreScanner.Hit hit : OreScanner.hits()) {
                BlockPos bp = hit.pos();
                if (!onTable(bp.getX(), bp.getZ())) continue;
                float[] c = FxDraw.rgb(hit.color());
                diamond(vc, m, toTable(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5), cam, pin * 0.5F, c[0], c[1], c[2], 0.9F * a);
            }
        }
        // the hovered column: a bright post
        if (hoverCell >= 0 && hoverCell < heights.length && heights[hoverCell] != NONE) {
            Vec3 top = columnTop(hoverCell);
            Vec3 tt = toTable(top.x, top.y, top.z);
            FxDraw.beam(vc, m, tt, tt.add(0, 0.25, 0), cam, 0.012F, 1F, 1F, 1F, 0.9F, 0);
            FxDraw.ring(vc, m, tt, new Vec3(0, 1, 0), pin * 1.1F, pin * 0.6F, 1F, 1F, 1F, 0.9F, 16);
        }
    }

    /** A small diamond facing the camera. */
    private static void diamond(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 cam, float s, float r, float g, float b, float a) {
        Vec3 view = c.subtract(cam).normalize();
        Vec3 side = FxDraw.perp(view);
        Vec3 up = view.cross(side).normalize();
        FxDraw.vertex(vc, m, c.add(up.scale(s)), r, g, b, a);
        FxDraw.vertex(vc, m, c.add(side.scale(s * 0.7)), r, g, b, a * 0.6F);
        FxDraw.vertex(vc, m, c.subtract(up.scale(s)), r, g, b, a);
        FxDraw.vertex(vc, m, c.subtract(side.scale(s * 0.7)), r, g, b, a * 0.6F);
    }

    /** Segment masks of the digits 0..9 (bits: a, b, c, d, e, f, g). */
    private static final int[] SEGMENTS = {0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F};

    /** One seven-segment digit facing the camera, {@code h} = half height. */
    private static void digit(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 cam, Vec3 left, Vec3 up, float h, int value, float a) {
        int mask = SEGMENTS[Math.floorMod(value, 10)];
        Vec3 right = left.scale(-1);
        float w = h * 0.6F;
        Vec3 tl = c.add(right.scale(-w)).add(up.scale(h)), tr = c.add(right.scale(w)).add(up.scale(h));
        Vec3 ml = c.add(right.scale(-w)), mr = c.add(right.scale(w));
        Vec3 bl = c.add(right.scale(-w)).subtract(up.scale(h)), br = c.add(right.scale(w)).subtract(up.scale(h));
        Vec3[][] seg = {{tl, tr}, {tr, mr}, {mr, br}, {bl, br}, {ml, bl}, {tl, ml}, {ml, mr}};
        for (int i = 0; i < 7; i++) {
            if ((mask & (1 << i)) != 0) FxDraw.line(vc, m, seg[i][0], seg[i][1], cam, h * 0.22F, 0.75F, 0.97F, 1F, a);
        }
    }

    /** The table column nearest to a screen point (within 14 GUI pixels), or -1. */
    static int pickCell(double mouseX, double mouseY) {
        int best = -1;
        double bestD = 14 * 14;
        for (int k = 0; k < heights.length; k++) {
            if (heights[k] == NONE) continue;
            Vec3 top = columnTop(k);
            float[] s = com.miophas.singularity_iteration.common.client.suit.FcsProjection.project(toTable(top.x, top.y, top.z));
            if (s == null) continue;
            double d = (s[0] - mouseX) * (s[0] - mouseX) + (s[1] - mouseY) * (s[1] - mouseY);
            if (d < bestD) { bestD = d; best = k; }
        }
        return best;
    }

    /** The hostile creature marker nearest to a screen point (within 8 GUI pixels), or null. */
    @Nullable
    static LivingEntity pickEntity(double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;
        AABB box = new AABB(originX - radius, baseY - 48, originZ - radius, originX + radius + 1, baseY + 48, originZ + radius + 1);
        LivingEntity best = null;
        double bestD = 8 * 8;
        for (Entity e : mc.level.getEntities((Entity) null, box, e -> e instanceof LivingEntity && e instanceof Enemy && e.isAlive())) {
            if (!onTable(e.getX(), e.getZ())) continue;
            float[] s = com.miophas.singularity_iteration.common.client.suit.FcsProjection.project(toTable(e.getX(), e.getY() + e.getBbHeight() + 2, e.getZ()));
            if (s == null) continue;
            double d = (s[0] - mouseX) * (s[0] - mouseX) + (s[1] - mouseY) * (s[1] - mouseY);
            if (d < bestD) { bestD = d; best = (LivingEntity) e; }
        }
        return best;
    }
}

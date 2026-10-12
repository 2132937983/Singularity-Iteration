// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.client.integration.RecipeLookup;
import com.miophas.singularity_iteration.common.suit.TacticalScan;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/**
 * Tactical map (0.1.8.0): full-screen 3D view of the terrain around the wearer with the
 * structures, the unopened loot containers and the contacts of the tactical scanner.
 *
 * <p><b>Terrain.</b> A surface relief of {@link #RADIUS} blocks around the player, one cell per
 * {@link #STRIDE} blocks, read from the client chunks when the map opens, on a scan and when the
 * slice changes (never per frame). With a slice, each column shows the highest block at or below
 * the slice level, so caves and buried structures show.
 *
 * <p><b>Drawing.</b> Orthographic orbit camera (yaw, tilt, zoom, pan). The cells are drawn back to
 * front (painter's algorithm) as one batch of coloured quads: the top face, plus the side faces
 * that look at the camera where the neighbour is lower. Structure boxes, pieces, containers,
 * contacts and the player are drawn on top.
 *
 * <p><b>Controls.</b> Left drag: turn and tilt. Right drag or arrow keys: pan. Wheel: zoom.
 * Page Up / Page Down: tilt. [ and ]: slice level down / up (Backspace: slice off). R: reset view.
 * Click a structure (map or list) to see its loot; click a loot item to open its recipes in JEI
 * (right click: its uses).
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class TacticalMapScreen extends Screen {
    static final int RADIUS = 96, STRIDE = 2, N = RADIUS * 2 / STRIDE + 1;
    private static final int PANEL_W = 168, BAR_H = 16, NONE = Integer.MIN_VALUE;

    private final int[] heights = new int[N * N], color = new int[N * N];
    private int originX, originZ, baseY;
    private Integer slice;
    private float yaw = 0, tilt = 55, zoom = 3.0F, panX, panZ;
    private boolean showTerrain = true, showStructures = true, showChests = true, showContacts = true;
    private int selected = -1, panelScroll;
    private Integer[] order;
    private float orderYaw = Float.NaN, orderTilt = Float.NaN;
    private double dragX, dragY;
    private boolean dragged;
    private long lastScan;

    public TacticalMapScreen() {
        super(Component.translatable("gui.mio_icif.tactical_map.title"));
    }

    @Override
    protected void init() {
        resample();
        if (minecraft != null && minecraft.level != null && minecraft.level.getGameTime() - lastScan > 40) requestScan();
        TacticalScan.CLIENT_HOOK = () -> { if (selected >= sites().size()) selected = -1; };
    }

    @Override
    public void removed() {
        TacticalScan.CLIENT_HOOK = null;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void requestScan() {
        var connection = Minecraft.getInstance().getConnection();
        var payload = new TacticalScan.Request();
        if (connection != null && connection.hasChannel(payload.type())) PacketDistributor.sendToServer(payload);
        if (minecraft != null && minecraft.level != null) lastScan = minecraft.level.getGameTime();
    }

    private static List<TacticalScan.Site> sites() {
        TacticalScan.Result r = TacticalScan.LAST;
        return r == null ? List.of() : r.sites();
    }

    // ------------------------------------------------------------------ terrain sampling
    private void resample() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        originX = mc.player.getBlockX();
        originZ = mc.player.getBlockZ();
        baseY = mc.player.getBlockY();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                int x = originX - RADIUS + i * STRIDE, z = originZ - RADIUS + j * STRIDE;
                int k = i * N + j;
                m.set(x, baseY, z);
                if (!level.hasChunkAt(m)) { heights[k] = NONE; continue; }
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                int y = top;
                if (slice != null && top > slice) {
                    y = NONE;
                    for (int yy = slice; yy >= Math.max(level.getMinBuildHeight(), slice - 96); yy--) {
                        m.set(x, yy, z);
                        if (!level.getBlockState(m).isAir()) { y = yy; break; }
                    }
                    if (y == NONE) { heights[k] = NONE; continue; }
                }
                m.set(x, y, z);
                BlockState state = level.getBlockState(m);
                int c = state.getMapColor(level, m).col;
                if (c == 0) c = MapColor.STONE.col;
                if (!state.getFluidState().isEmpty()) c = 0x3F76E4;
                heights[k] = y;
                color[k] = c;
            }
        }
        orderYaw = Float.NaN;
    }

    // ------------------------------------------------------------------ projection
    private float cosY, sinY, cosT, sinT, cx, cy;

    private void setupCamera() {
        double a = Math.toRadians(yaw), t = Math.toRadians(tilt);
        cosY = (float) Math.cos(a); sinY = (float) Math.sin(a);
        cosT = (float) Math.cos(t); sinT = (float) Math.sin(t);
        cx = (width - PANEL_W) / 2F;
        cy = (height + BAR_H) / 2F;
    }

    /** World point to {screenX, screenY, depth}; larger depth is nearer to the viewer. */
    private float[] project(double x, double y, double z, float[] out) {
        double dx = x - (originX + panX), dy = y - baseY, dz = z - (originZ + panZ);
        double u = dx * cosY - dz * sinY, v = dx * sinY + dz * cosY;
        out[0] = (float) (cx + u * zoom);
        out[1] = (float) (cy + (v * sinT - dy * cosT) * zoom);
        out[2] = (float) (v * cosT + dy * sinT);
        return out;
    }

    // ------------------------------------------------------------------ drawing
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, 0xF0060C14);
        setupCamera();
        if (showTerrain) drawTerrain(g);
        drawGrid(g);
        if (showStructures) drawStructures(g);
        if (showContacts) drawContacts(g, partial);
        drawPlayer(g, partial);
        drawToolbar(g, mouseX, mouseY);
        drawPanel(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) { }

    private void drawTerrain(GuiGraphics g) {
        if (order == null || orderYaw != yaw || orderTilt != tilt) {
            float[] depth = new float[N * N];
            float[] p = new float[3];
            List<Integer> idx = new ArrayList<>(N * N);
            for (int i = 0; i < N; i++) for (int j = 0; j < N; j++) {
                int k = i * N + j;
                if (heights[k] == NONE) continue;
                project(originX - RADIUS + i * STRIDE, heights[k], originZ - RADIUS + j * STRIDE, p);
                depth[k] = p[2];
                idx.add(k);
            }
            idx.sort((a, b) -> Float.compare(depth[a], depth[b]));
            order = idx.toArray(new Integer[0]);
            orderYaw = yaw;
            orderTilt = tilt;
        }
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f mat = g.pose().last().pose();
        float h2 = STRIDE / 2F;
        float[] a = new float[3], b = new float[3], c = new float[3], d = new float[3];
        // side faces that look at the viewer: outward normal with a positive v component
        boolean east = sinY > 0, west = sinY < 0, south = cosY > 0, north = cosY < 0;
        for (int k : order) {
            int i = k / N, j = k % N;
            double x = originX - RADIUS + i * STRIDE, z = originZ - RADIUS + j * STRIDE;
            int y = heights[k] + 1;
            int col = color[k];
            int west1 = i > 0 ? heights[k - N] : NONE;
            float shade = west1 != NONE && west1 < heights[k] ? 1.12F : west1 != NONE && west1 > heights[k] ? 0.82F : 1.0F;
            if (slice != null && heights[k] < slice - 24) shade *= 0.7F;
            int top = argb(col, shade, 0xE0);
            project(x - h2, y, z - h2, a); project(x + h2, y, z - h2, b); project(x + h2, y, z + h2, c); project(x - h2, y, z + h2, d);
            quad(vc, mat, a, b, c, d, top);
            if (east) side(vc, mat, i < N - 1 ? heights[k + N] : NONE, heights[k], x + h2, z - h2, x + h2, z + h2, argb(col, 0.66F, 0xE0));
            if (west) side(vc, mat, i > 0 ? heights[k - N] : NONE, heights[k], x - h2, z + h2, x - h2, z - h2, argb(col, 0.66F, 0xE0));
            if (south) side(vc, mat, j < N - 1 ? heights[k + 1] : NONE, heights[k], x + h2, z + h2, x - h2, z + h2, argb(col, 0.52F, 0xE0));
            if (north) side(vc, mat, j > 0 ? heights[k - 1] : NONE, heights[k], x - h2, z - h2, x + h2, z - h2, argb(col, 0.52F, 0xE0));
        }
        g.flush();
    }

    private final float[] sa = new float[3], sb = new float[3], sc = new float[3], sd = new float[3];

    private void side(VertexConsumer vc, Matrix4f mat, int neighbour, int self, double x0, double z0, double x1, double z1, int col) {
        int low = neighbour == NONE ? self - 4 : neighbour;
        if (low >= self) return;
        project(x0, self + 1, z0, sa); project(x1, self + 1, z1, sb); project(x1, low + 1, z1, sc); project(x0, low + 1, z0, sd);
        quad(vc, mat, sa, sb, sc, sd, col);
    }

    private static void quad(VertexConsumer vc, Matrix4f mat, float[] a, float[] b, float[] c, float[] d, int argb) {
        vc.addVertex(mat, a[0], a[1], 0).setColor(argb);
        vc.addVertex(mat, d[0], d[1], 0).setColor(argb);
        vc.addVertex(mat, c[0], c[1], 0).setColor(argb);
        vc.addVertex(mat, b[0], b[1], 0).setColor(argb);
    }

    private static int argb(int rgb, float f, int alpha) {
        int r = Math.min(255, (int) ((rgb >> 16 & 255) * f)), g = Math.min(255, (int) ((rgb >> 8 & 255) * f)), b = Math.min(255, (int) ((rgb & 255) * f));
        return alpha << 24 | r << 16 | g << 8 | b;
    }

    private void drawGrid(GuiGraphics g) {
        float[] a = new float[3], b = new float[3];
        int y = slice != null ? slice : baseY;
        for (int t = -RADIUS; t <= RADIUS; t += 16) {
            project(originX + t, y, originZ - RADIUS, a); project(originX + t, y, originZ + RADIUS, b);
            FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.5F, 0x2235C8FF);
            project(originX - RADIUS, y, originZ + t, a); project(originX + RADIUS, y, originZ + t, b);
            FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.5F, 0x2235C8FF);
        }
    }

    private static int siteColor(String id) {
        float hue = (id.hashCode() & 0xFFFF) / 65535F;
        return java.awt.Color.HSBtoRGB(hue, 0.55F, 1.0F) & 0xFFFFFF;
    }

    private void drawStructures(GuiGraphics g) {
        List<TacticalScan.Site> sites = sites();
        float[] p = new float[3];
        for (int s = 0; s < sites.size(); s++) {
            TacticalScan.Site site = sites.get(s);
            int rgb = siteColor(site.id());
            boolean sel = s == selected;
            if (zoom >= 1.5F || sel) for (TacticalScan.Box piece : site.pieces()) box(g, piece, FcsDraw.argb(rgb, sel ? 0.5F : 0.22F), 0.6F);
            box(g, site.box(), FcsDraw.argb(rgb, sel ? 1.0F : 0.75F), sel ? 1.6F : 1.0F);
            TacticalScan.Box b = site.box();
            project((b.x0() + b.x1() + 1) / 2.0, b.y1() + 1, (b.z0() + b.z1() + 1) / 2.0, p);
            FcsDraw.textCentered(g, shortName(site.id()), p[0], p[1] - 9, FcsDraw.argb(rgb, 1.0F), 0.6F);
            if (showChests) {
                for (TacticalScan.Chest c : site.chests()) {
                    project(c.pos().getX() + 0.5, c.pos().getY() + 1, c.pos().getZ() + 0.5, p);
                    FcsDraw.quad(g, p[0], p[1] - 3, p[0] + 3, p[1], p[0], p[1] + 3, p[0] - 3, p[1], 0xFFFFC940);
                }
            }
        }
    }

    private void box(GuiGraphics g, TacticalScan.Box b, int argb, float w) {
        double[] xs = {b.x0(), b.x1() + 1}, ys = {b.y0(), b.y1() + 1}, zs = {b.z0(), b.z1() + 1};
        float[][] v = new float[8][];
        for (int i = 0; i < 8; i++) v[i] = project(xs[i & 1], ys[i >> 1 & 1], zs[i >> 2 & 1], new float[3]);
        int[][] edges = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) FcsDraw.line(g, v[e[0]][0], v[e[0]][1], v[e[1]][0], v[e[1]][1], w, argb);
    }

    private void drawContacts(GuiGraphics g, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        float[] p = new float[3];
        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(RADIUS), LivingEntity::isAlive)) {
            if (e == mc.player) continue;
            var at = e.getPosition(partial);
            project(at.x, at.y + e.getBbHeight() * 0.5, at.z, p);
            int col = e instanceof Player ? 0xFF4DE1FF : e instanceof Enemy ? 0xFFFF4D4D : 0xFF7CFFB2;
            g.fill((int) p[0] - 1, (int) p[1] - 1, (int) p[0] + 2, (int) p[1] + 2, col);
        }
    }

    private void drawPlayer(GuiGraphics g, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var at = mc.player.getPosition(partial);
        float[] p = project(at.x, at.y + 1, at.z, new float[3]);
        double look = Math.toRadians(mc.player.getYRot());
        float[] f = project(at.x - Math.sin(look) * 6, at.y + 1, at.z + Math.cos(look) * 6, new float[3]);
        FcsDraw.line(g, p[0], p[1], f[0], f[1], 1.5F, 0xFFFFFFFF);
        FcsDraw.disc(g, p[0], p[1], 3, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------------ toolbar and panel
    private static final String[] TOOLS = {"scan", "terrain", "structures", "chests", "contacts", "reset", "slice_down", "slice_up"};

    private int toolX(int i) { return 4 + i * 62; }

    private boolean toolOn(int i) {
        return switch (i) {
            case 1 -> showTerrain; case 2 -> showStructures; case 3 -> showChests; case 4 -> showContacts;
            default -> false;
        };
    }

    private void drawToolbar(GuiGraphics g, int mx, int my) {
        g.fill(0, 0, width, BAR_H, 0xE00A1622);
        for (int i = 0; i < TOOLS.length; i++) {
            int x = toolX(i);
            boolean hot = mx >= x && mx < x + 60 && my < BAR_H;
            int c = toolOn(i) ? 0xFF4DE1C1 : 0xFF7A8C99;
            g.fill(x, 2, x + 60, BAR_H - 2, hot ? 0x60FFFFFF : 0x30000000);
            g.renderOutline(x, 2, 60, BAR_H - 4, c);
            FcsDraw.textCentered(g, Component.translatable("gui.mio_icif.tactical_map." + TOOLS[i]).getString(), x + 30, 5, 0xFFFFFFFF, 0.6F);
        }
        String sliceText = Component.translatable("gui.mio_icif.tactical_map.slice", slice == null ? "-" : String.valueOf(slice)).getString();
        FcsDraw.text(g, sliceText, toolX(TOOLS.length) + 4, 5, 0xFFB8C7D1, 0.6F);
        String help = Component.translatable(RecipeLookup.available() ? "gui.mio_icif.tactical_map.help.jei" : "gui.mio_icif.tactical_map.help").getString();
        FcsDraw.text(g, help, 4, height - 9, 0xFF8FA3B0, 0.55F);
    }

    /** Loot of the selected site: item -> best chance over its containers. */
    private Map<String, Integer> lootOf(TacticalScan.Site site) {
        Map<String, Integer> out = new LinkedHashMap<>();
        TacticalScan.Result r = TacticalScan.LAST;
        if (r == null) return out;
        for (TacticalScan.Chest c : site.chests()) {
            List<TacticalScan.LootItem> items = r.tables().get(c.table());
            if (items == null) continue;
            for (TacticalScan.LootItem it : items) out.merge(it.item(), it.percent(), Math::max);
        }
        return out;
    }

    private record LootSlot(int x, int y, ItemStack stack, int percent) { }
    private final List<LootSlot> lootSlots = new ArrayList<>();

    private void drawPanel(GuiGraphics g, int mx, int my) {
        int x0 = width - PANEL_W, y0 = BAR_H;
        g.fill(x0, y0, width, height, 0xE00A1622);
        g.fill(x0, y0, x0 + 1, height, 0xFF2A4A5E);
        List<TacticalScan.Site> sites = sites();
        FcsDraw.text(g, Component.translatable("gui.mio_icif.tactical_map.sites", sites.size()).getString(), x0 + 6, y0 + 5, 0xFF4DE1C1, 0.7F);
        Minecraft mc = Minecraft.getInstance();
        int ly = y0 + 16;
        int rows = Math.max(1, Math.min(8, sites.size()));
        panelScroll = Math.max(0, Math.min(panelScroll, Math.max(0, sites.size() - rows)));
        for (int r = 0; r < rows && panelScroll + r < sites.size(); r++) {
            int s = panelScroll + r;
            TacticalScan.Site site = sites.get(s);
            int yy = ly + r * 11;
            boolean hot = mx >= x0 && my >= yy && my < yy + 11;
            if (s == selected) g.fill(x0 + 2, yy, width - 2, yy + 11, 0x404DE1C1);
            else if (hot) g.fill(x0 + 2, yy, width - 2, yy + 11, 0x20FFFFFF);
            TacticalScan.Box b = site.box();
            double dist = mc.player == null ? 0 : Math.sqrt(mc.player.distanceToSqr((b.x0() + b.x1()) / 2.0, mc.player.getY(), (b.z0() + b.z1()) / 2.0));
            FcsDraw.text(g, shortName(site.id()), x0 + 6, yy + 2, FcsDraw.argb(siteColor(site.id()), 1.0F), 0.65F);
            FcsDraw.textRight(g, Math.round(dist) + "m", width - 6, yy + 2, 0xFFB8C7D1, 0.65F);
        }
        lootSlots.clear();
        if (selected < 0 || selected >= sites.size()) {
            FcsDraw.text(g, Component.translatable(sites.isEmpty() ? "gui.mio_icif.tactical_map.none" : "gui.mio_icif.tactical_map.pick").getString(),
                x0 + 6, ly + rows * 11 + 8, 0xFF8FA3B0, 0.6F);
            return;
        }
        TacticalScan.Site site = sites.get(selected);
        TacticalScan.Box b = site.box();
        int dy = ly + rows * 11 + 6;
        FcsDraw.text(g, site.id(), x0 + 6, dy, FcsDraw.argb(siteColor(site.id()), 1.0F), 0.6F);
        FcsDraw.text(g, Component.translatable("gui.mio_icif.tactical_map.size", b.x1() - b.x0() + 1, b.y1() - b.y0() + 1, b.z1() - b.z0() + 1,
            site.pieces().size(), site.chests().size()).getString(), x0 + 6, dy + 9, 0xFFB8C7D1, 0.55F);
        FcsDraw.text(g, Component.translatable("gui.mio_icif.tactical_map.loot").getString(), x0 + 6, dy + 20, 0xFFFFC940, 0.6F);
        int ix = 0, iy = dy + 30;
        ItemStack hover = ItemStack.EMPTY;
        int hoverPercent = 0;
        for (Map.Entry<String, Integer> en : lootOf(site).entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(en.getKey());
            if (id == null) continue;
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
            if (stack.isEmpty()) continue;
            int sx = x0 + 6 + ix * 19, sy = iy;
            if (sy > height - 30) break;
            g.renderItem(stack, sx, sy);
            FcsDraw.text(g, en.getValue() + "%", sx, sy + 15, 0xFFB8C7D1, 0.45F);
            lootSlots.add(new LootSlot(sx, sy, stack, en.getValue()));
            if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16) { hover = stack; hoverPercent = en.getValue(); }
            if (++ix >= 8) { ix = 0; iy += 22; }
        }
        if (site.chests().isEmpty()) FcsDraw.text(g, Component.translatable("gui.mio_icif.tactical_map.no_loot").getString(), x0 + 6, iy, 0xFF8FA3B0, 0.55F);
        if (!hover.isEmpty()) {
            List<Component> tip = new ArrayList<>(getTooltipFromItem(Minecraft.getInstance(), hover));
            tip.add(Component.translatable("gui.mio_icif.tactical_map.chance", hoverPercent).withStyle(net.minecraft.ChatFormatting.GOLD));
            if (RecipeLookup.available()) tip.add(Component.translatable("gui.mio_icif.tactical_map.jei").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(font, tip, mx, my);
        }
    }

    private static String shortName(String id) {
        int c = id.indexOf(':');
        return c >= 0 ? id.substring(c + 1) : id;
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        dragX = mx; dragY = my; dragged = false;
        if (my < BAR_H) {
            for (int i = 0; i < TOOLS.length; i++) {
                if (mx >= toolX(i) && mx < toolX(i) + 60) { tool(i); return true; }
            }
            return true;
        }
        if (mx >= width - PANEL_W) {
            for (LootSlot s : lootSlots) {
                if (mx >= s.x() && mx < s.x() + 16 && my >= s.y() && my < s.y() + 16) {
                    RecipeLookup.show(s.stack(), button == 1);
                    return true;
                }
            }
            int ly = BAR_H + 16;
            int row = (int) ((my - ly) / 11);
            List<TacticalScan.Site> sites = sites();
            if (my >= ly && row >= 0 && row < 8 && panelScroll + row < sites.size()) selected = panelScroll + row;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (!dragged && button == 0 && mx < width - PANEL_W && my > BAR_H) pick(mx, my);
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double ddx, double ddy) {
        if (Math.abs(mx - dragX) + Math.abs(my - dragY) > 3) dragged = true;
        if (button == 0 && !hasShiftDown()) {
            yaw = (yaw + (float) ddx * 0.5F) % 360;
            tilt = Math.max(20, Math.min(88, tilt - (float) ddy * 0.4F));
        } else {
            pan(-ddx / zoom, -ddy / zoom / Math.max(0.2, sinT));
        }
        return true;
    }

    private void pan(double screenU, double screenV) {
        // screen axes back to world x/z (inverse of the yaw rotation)
        panX += (float) (screenU * cosY + screenV * sinY);
        panZ += (float) (-screenU * sinY + screenV * cosY);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mx >= width - PANEL_W) { panelScroll -= (int) Math.signum(sy); return true; }
        zoom = (float) Math.max(0.5, Math.min(14, zoom * (sy > 0 ? 1.15 : 1 / 1.15)));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        switch (key) {
            case GLFW.GLFW_KEY_PAGE_UP -> tilt = Math.min(88, tilt + 5);
            case GLFW.GLFW_KEY_PAGE_DOWN -> tilt = Math.max(20, tilt - 5);
            case GLFW.GLFW_KEY_LEFT_BRACKET -> tool(6);
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> tool(7);
            case GLFW.GLFW_KEY_BACKSPACE -> { slice = null; resample(); }
            case GLFW.GLFW_KEY_R -> tool(5);
            case GLFW.GLFW_KEY_LEFT -> pan(-8, 0);
            case GLFW.GLFW_KEY_RIGHT -> pan(8, 0);
            case GLFW.GLFW_KEY_UP -> pan(0, -8);
            case GLFW.GLFW_KEY_DOWN -> pan(0, 8);
            default -> { return super.keyPressed(key, scan, mods); }
        }
        return true;
    }

    private void tool(int i) {
        switch (i) {
            case 0 -> { resample(); requestScan(); }
            case 1 -> showTerrain = !showTerrain;
            case 2 -> showStructures = !showStructures;
            case 3 -> showChests = !showChests;
            case 4 -> showContacts = !showContacts;
            case 5 -> { yaw = 0; tilt = 55; zoom = 3; panX = 0; panZ = 0; }
            case 6 -> { slice = (slice == null ? baseY : slice) - 4; resample(); }
            case 7 -> { slice = (slice == null ? baseY : slice) + 4; resample(); }
            default -> { }
        }
        if (minecraft != null) minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.6F, 0.25F));
    }

    /** Selects the structure whose label or box centre is nearest to the click (within 24 px). */
    private void pick(double mx, double my) {
        List<TacticalScan.Site> sites = sites();
        float[] p = new float[3];
        double best = 24 * 24;
        int found = -1;
        for (int s = 0; s < sites.size(); s++) {
            TacticalScan.Box b = sites.get(s).box();
            project((b.x0() + b.x1() + 1) / 2.0, b.y1() + 1, (b.z0() + b.z1() + 1) / 2.0, p);
            double d = (p[0] - mx) * (p[0] - mx) + (p[1] - my) * (p[1] - my);
            if (d < best) { best = d; found = s; }
        }
        selected = found;
    }
}

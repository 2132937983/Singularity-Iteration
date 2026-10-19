// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.client.screen.SiGuiTheme;
import com.miophas.singularity_iteration.common.client.suit.nano.QuantumClient;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.UnitRef;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumBandwidth;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumLinks;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumPackets;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergy;
import com.miophas.singularity_iteration.core.api.suit.SuitSynergies;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Entanglement Matrix (0.1.10.1): the link editor of the quantum chestplate, opened from the
 * Singularity Management Panel.
 *
 * <p>Left, the matrix: every unit on the worn quantum pieces is a node on a ring (its item icon in
 * a diamond of its colour). Each synergy whose two units are worn is a line between their nodes:
 * a faint dashed line when it can be linked, a solid line when linked but not working (a unit is
 * off or unpowered), and a pulsing double helix in the synergy colour when it works. Right, the
 * list of all synergies with their state; a click links or unlinks (the server checks the hub,
 * the capacity and the units). A node can also be dragged onto another node: the thread follows
 * the mouse, the synergy of the pair is previewed, and the release links or unlinks it. The footer
 * shows the links in use, the capacity and the compute bandwidth.
 */
public final class EntanglementScreen extends Screen {
    private static final int W = 316, H = 206, GRAPH = 150, ROW = 15;
    private static final int DARK = 0xFF0A0F16, GRID = 0xFF141C26, TEXT = 0xFFDCEBF5, DIM = 0xFF6F8494;
    @Nullable private final Screen parent;
    private int left, top, scroll, ticks;
    /** Node centres of the last frame (for the mouse), and the node being dragged. */
    private final Map<String, float[]> nodeAt = new LinkedHashMap<>();
    @Nullable private String dragFrom;
    private int missAt = -100;

    public EntanglementScreen(@Nullable Screen parent) {
        super(Component.translatable("gui.mio_icif.entangle.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
    }

    @Override
    public void tick() {
        ticks++;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Unit keys of the worn quantum pieces, each once, with the item to draw. */
    private Map<String, ItemStack> nodes(LocalPlayer player) {
        Map<String, ItemStack> out = new LinkedHashMap<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack piece = player.getItemBySlot(slot);
            if (!SuitModules.isQuantumPiece(piece)) continue;
            for (UnitRef u : SuitModules.installed(piece).units()) {
                if (!out.containsKey(u.key())) out.put(u.key(), u.unitStack(Math.max(1, SuitModules.tier(piece, u.key()))));
            }
        }
        return out;
    }

    private static int color(String key) {
        QuantumSynergy s = QuantumSynergy.byId(key);
        return s != null ? s.color : 0x9EF0FF;
    }

    private static Component name(String key) {
        QuantumSynergy s = QuantumSynergy.byId(key);
        if (s != null) return Component.translatable(s.nameKey());
        var e = SuitSynergies.get(key);
        return e == null ? Component.literal(key) : Component.translatable(e.nameKey());
    }

    private static String descKey(String key) {
        QuantumSynergy s = QuantumSynergy.byId(key);
        if (s != null) return s.descKey();
        var e = SuitSynergies.get(key);
        return e == null ? key : e.nameKey() + ".desc";
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player == null) return;
        float t = ticks + partial;
        SiGuiTheme.panel(g, left, top, W, H);
        g.drawString(font, title, left + 8, top + 6, SiGuiTheme.TEXT, false);
        // dark hologram screens
        int gx = left + 7, gy = top + 18, lx = gx + GRAPH + 6, lw = W - GRAPH - 20;
        g.fill(gx, gy, gx + GRAPH, gy + GRAPH, DARK);
        g.fill(lx, gy, lx + lw, gy + GRAPH, DARK);
        for (int k = 10; k < GRAPH; k += 10) {
            g.fill(gx + k, gy, gx + k + 1, gy + GRAPH, GRID);
            g.fill(gx, gy + k, gx + GRAPH, gy + k + 1, GRID);
        }
        Map<String, ItemStack> nodes = nodes(player);
        Map<String, float[]> at = nodeAt;
        at.clear();
        float cx = gx + GRAPH / 2F, cy = gy + GRAPH / 2F, r = GRAPH * 0.36F;
        int n = Math.max(1, nodes.size()), i = 0;
        for (String key : nodes.keySet()) {
            double ang = -Math.PI / 2 + i++ * Math.PI * 2 / n;
            at.put(key, new float[]{cx + (float) Math.cos(ang) * r, cy + (float) Math.sin(ang) * r});
        }
        List<String> keys = QuantumLinks.allKeys();
        List<String> linked = QuantumLinks.links(QuantumLinks.hub(player));
        int capacity = QuantumLinks.capacity(player);
        // threads first (under the nodes)
        for (String key : keys) {
            String[] u = QuantumLinks.units(key);
            if (u == null || !at.containsKey(u[0]) || !at.containsKey(u[1])) continue;
            float[] a = at.get(u[0]), b = at.get(u[1]);
            int col = color(key);
            boolean isLinked = linked.contains(key) && linked.indexOf(key) < capacity;
            boolean works = isLinked && QuantumLinks.active(player, key);
            if (works) helix(g, a, b, col, t);
            else if (isLinked) FcsDraw.line(g, a[0], a[1], b[0], b[1], 1F, FcsDraw.argb(col, 0.55F));
            else FcsDraw.dashed(g, a[0], a[1], b[0], b[1], 3F, 0.7F, FcsDraw.argb(col, 0.28F));
        }
        // nodes
        String hoverNode = null;
        for (var e : at.entrySet()) {
            float[] p = e.getValue();
            UnitRef u = UnitRef.byKey(e.getKey());
            int col = u == null ? 0x9EF0FF : u.color();
            boolean active = u != null && SuitModules.isActive(player, u);
            float s = 11 + (active ? 1.2F * (float) Math.sin(t * 0.15F + p[0]) : 0);
            FcsDraw.quad(g, p[0], p[1] - s, p[0] + s, p[1], p[0], p[1] + s, p[0] - s, p[1], FcsDraw.argb(0x0A0F16, 0.95F));
            FcsDraw.line(g, p[0], p[1] - s, p[0] + s, p[1], 1F, FcsDraw.argb(col, active ? 0.95F : 0.4F));
            FcsDraw.line(g, p[0] + s, p[1], p[0], p[1] + s, 1F, FcsDraw.argb(col, active ? 0.95F : 0.4F));
            FcsDraw.line(g, p[0], p[1] + s, p[0] - s, p[1], 1F, FcsDraw.argb(col, active ? 0.95F : 0.4F));
            FcsDraw.line(g, p[0] - s, p[1], p[0], p[1] - s, 1F, FcsDraw.argb(col, active ? 0.95F : 0.4F));
            g.flush();
            g.renderItem(nodes.get(e.getKey()), (int) p[0] - 8, (int) p[1] - 8);
            if (Math.abs(mouseX - p[0]) < 10 && Math.abs(mouseY - p[1]) < 10) hoverNode = e.getKey();
        }
        // drag: a thread from the node to the mouse and the preview of the pair's synergy
        String dragKey = null;
        if (dragFrom != null && at.containsKey(dragFrom)) {
            float[] a = at.get(dragFrom);
            String over = nodeUnder(mouseX, mouseY);
            dragKey = over == null || over.equals(dragFrom) ? null : synergyOf(dragFrom, over);
            int col = dragKey != null ? color(dragKey) : 0x9EF0FF;
            if (dragKey != null) helix(g, a, at.get(over), col, t * 1.6F);
            else FcsDraw.dashed(g, a[0], a[1], mouseX, mouseY, 4F, 1.2F, FcsDraw.argb(col, 0.8F));
            FcsDraw.circle(g, mouseX, mouseY, 3 + (float) Math.sin(t * 0.4F), 1F, FcsDraw.argb(col, 0.9F));
        }
        if (ticks - missAt < 30) {
            Component miss = Component.translatable("gui.mio_icif.entangle.no_pair");
            g.drawCenteredString(font, miss, gx + GRAPH / 2, gy + GRAPH - 12, 0xFFFF7A6A);
        }
        if (nodes.isEmpty()) {
            Component none = Component.translatable("gui.mio_icif.entangle.no_units");
            for (var line : font.split(none, GRAPH - 12)) {
                g.drawString(font, line, gx + 6, (int) cy - 4, DIM, false);
                cy += 10;
            }
        }
        // the list
        String hoverRow = null;
        int rows = (GRAPH - 4) / ROW;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, keys.size() - rows)));
        for (int k = scroll; k < keys.size() && k < scroll + rows; k++) {
            String key = keys.get(k);
            int ry = gy + 3 + (k - scroll) * ROW;
            boolean isLinked = linked.contains(key);
            boolean overCap = isLinked && linked.indexOf(key) >= capacity;
            boolean works = isLinked && !overCap && QuantumLinks.active(player, key);
            boolean available = QuantumLinks.available(player, key);
            boolean hot = mouseX >= lx && mouseX < lx + lw && mouseY >= ry - 1 && mouseY < ry + ROW - 2;
            if (hot) { g.fill(lx + 1, ry - 1, lx + lw - 1, ry + ROW - 2, 0x30FFFFFF); hoverRow = key; }
            int col = color(key);
            g.fill(lx + 2, ry, lx + 4, ry + ROW - 3, 0xFF000000 | col);
            g.drawString(font, font.plainSubstrByWidth(name(key).getString(), lw - 60), lx + 7, ry + 2, available || isLinked ? TEXT : DIM, false);
            String state = works ? "LINKED" : overCap ? "OVER CAP" : isLinked ? "OFFLINE" : available ? "READY" : "MISSING";
            int sc = works ? 0xFF5CFF9A : overCap || isLinked ? 0xFFFFB347 : available ? 0xFF9FDFFF : DIM;
            g.drawString(font, state, lx + lw - 4 - font.width(state), ry + 2, sc, false);
            int result = QuantumClient.linkResult(key);
            if (result > QuantumLinks.UNLINKED) {
                float blink = (ticks / 4) % 2 == 0 ? 1F : 0.5F;
                g.fill(lx + 1, ry + ROW - 3, lx + lw - 1, ry + ROW - 2, FcsDraw.argb(0xFF5A4C, blink));
            }
        }
        // footer
        int fy = gy + GRAPH + 6;
        int used = 0;
        for (String key : linked) if (QuantumLinks.units(key) != null) used++;
        Component links = QuantumLinks.hub(player).isEmpty() ? Component.translatable("gui.mio_icif.entangle.no_hub")
            : Component.translatable("gui.mio_icif.entangle.links", Math.min(used, capacity), capacity);
        g.drawString(font, links, left + 8, fy, SiGuiTheme.TEXT, false);
        int load = QuantumBandwidth.load(player), budget = QuantumBandwidth.budget(player);
        Component qb = Component.translatable("gui.mio_icif.entangle.bandwidth", load, budget);
        g.drawString(font, qb, left + W - 8 - font.width(qb), fy, load > budget ? 0xFFC8423C : SiGuiTheme.TEXT, false);
        // 0.1.10.4: the compute core state between the two (why the third link and the +8 QB are there or not)
        QuantumLinks.CoreState core = QuantumLinks.computeCoreState(player);
        if (core != QuantumLinks.CoreState.NONE) {
            int cores = QuantumLinks.activeComputeCores(player);
            Component cs = Component.translatable("gui.mio_icif.entangle.core." + core.name().toLowerCase(java.util.Locale.ROOT));
            if (cores > 1) cs = cs.copy().append(" ×" + cores);   // 0.1.10.6: stacked cores
            int csx = left + 8 + font.width(links) + 8, room = left + W - 8 - font.width(qb) - 8 - csx;
            if (room > 20) g.drawString(font, font.plainSubstrByWidth(cs.getString(), room), csx, fy,
                core == QuantumLinks.CoreState.ACTIVE ? 0xFF7FD8FF : 0xFFFFB347, false);
        }
        SiGuiTheme.bar(g, left + 8, fy + 11, W - 16, 4, Math.min(1.0, load / (double) Math.max(1, budget)),
            load > budget ? 0xFFE04030 : SiGuiTheme.BAR_FROM, load > budget ? 0xFFF07060 : SiGuiTheme.BAR_TO);
        // tooltips
        if (dragKey != null) {
            boolean isLinked = linked.contains(dragKey);
            g.renderComponentTooltip(font, List.of(name(dragKey).copy().withStyle(ChatFormatting.AQUA),
                Component.translatable(descKey(dragKey)).withStyle(ChatFormatting.GRAY),
                Component.translatable(isLinked ? "gui.mio_icif.entangle.release_unlink" : "gui.mio_icif.entangle.release_link")
                    .withStyle(isLinked ? ChatFormatting.GOLD : ChatFormatting.GREEN)), mouseX, mouseY);
        } else if (dragFrom != null) {
            // no tooltip while a thread is dragged
        } else if (hoverRow != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(name(hoverRow).copy().withStyle(ChatFormatting.AQUA));
            tip.add(Component.translatable(descKey(hoverRow)).withStyle(ChatFormatting.GRAY));
            String[] u = QuantumLinks.units(hoverRow);
            if (u != null) {
                UnitRef a = UnitRef.byKey(u[0]), b = UnitRef.byKey(u[1]);
                tip.add(Component.translatable("gui.mio_icif.entangle.units",
                    a == null ? Component.literal(u[0]) : Component.translatable(a.nameKey()),
                    b == null ? Component.literal(u[1]) : Component.translatable(b.nameKey())).withStyle(ChatFormatting.DARK_AQUA));
            }
            int result = QuantumClient.linkResult(hoverRow);
            if (result > QuantumLinks.UNLINKED) tip.add(Component.translatable("gui.mio_icif.entangle.result." + result).withStyle(ChatFormatting.RED));
            tip.add(Component.translatable("gui.mio_icif.entangle.click").withStyle(ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        } else if (hoverNode != null) {
            UnitRef u = UnitRef.byKey(hoverNode);
            if (u != null) g.renderComponentTooltip(font, List.of(Component.translatable(u.nameKey())), mouseX, mouseY);
        }
    }

    /** A pulsing double helix between two nodes. */
    private static void helix(GuiGraphics g, float[] a, float[] b, int col, float t) {
        float dx = b[0] - a[0], dy = b[1] - a[1], len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;
        float nx = -dy / len, ny = dx / len;
        for (int strand = 0; strand < 2; strand++) {
            float px = a[0], py = a[1];
            for (int i = 1; i <= 24; i++) {
                float u = i / 24F;
                float amp = 3.2F * (float) Math.sin(u * Math.PI);
                float off = amp * (float) Math.sin(u * 14 - t * 0.35F + strand * Math.PI);
                float x = a[0] + dx * u + nx * off, y = a[1] + dy * u + ny * off;
                float pulse = 0.55F + 0.45F * (float) Math.sin(u * 9 - t * 0.5F);
                FcsDraw.line(g, px, py, x, y, 1.1F, FcsDraw.argb(strand == 0 ? col : 0xFFFFFF, (strand == 0 ? 0.95F : 0.5F) * pulse));
                px = x;
                py = y;
            }
        }
    }

    // ------------------------------------------------------------------ input

    @Nullable
    private String nodeUnder(double mouseX, double mouseY) {
        for (var e : nodeAt.entrySet()) {
            float[] p = e.getValue();
            if (Math.abs(mouseX - p[0]) < 11 && Math.abs(mouseY - p[1]) < 11) return e.getKey();
        }
        return null;
    }

    /** The synergy of two unit keys (either order), or null. */
    @Nullable
    private static String synergyOf(String unitA, String unitB) {
        for (String key : QuantumLinks.allKeys()) {
            String[] u = QuantumLinks.units(key);
            if (u != null && (u[0].equals(unitA) && u[1].equals(unitB) || u[0].equals(unitB) && u[1].equals(unitA))) return key;
        }
        return null;
    }

    private void send(String key) {
        if (minecraft == null) return;
        var connection = minecraft.getConnection();
        var payload = new QuantumPackets.LinkToggle(key);
        if (connection != null && connection.hasChannel(payload.type())) PacketDistributor.sendToServer(payload);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(mio_icif_sounds.UI_CLICK.get(), 1.2F, 0.4F));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return dragFrom != null || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragFrom != null) {
            String over = nodeUnder(mouseX, mouseY);
            if (over != null && !over.equals(dragFrom)) {
                String key = synergyOf(dragFrom, over);
                if (key != null) send(key);
                else {
                    missAt = ticks;
                    if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(mio_icif_sounds.NANO_DENIED.get(), 1.0F, 0.4F));
                }
            }
            dragFrom = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || minecraft == null || minecraft.player == null) return super.mouseClicked(mouseX, mouseY, button);
        String node = nodeUnder(mouseX, mouseY);
        if (node != null) {
            dragFrom = node;
            return true;
        }
        int gy = top + 18, lx = left + 7 + GRAPH + 6, lw = W - GRAPH - 20;
        List<String> keys = QuantumLinks.allKeys();
        int rows = (GRAPH - 4) / ROW;
        for (int k = scroll; k < keys.size() && k < scroll + rows; k++) {
            int ry = gy + 3 + (k - scroll) * ROW;
            if (mouseX >= lx && mouseX < lx + lw && mouseY >= ry - 1 && mouseY < ry + ROW - 2) {
                send(keys.get(k));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
        return true;
    }
}

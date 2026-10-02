package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.blockentity.wiring.terminal.EnergyNetworkSnapshot;
import com.miophas.singularity_iteration.common.blockentity.wiring.terminal.EnergyNetworkSnapshot.Category;
import com.miophas.singularity_iteration.common.blockentity.wiring.terminal.EnergyNetworkSnapshot.Device;
import com.miophas.singularity_iteration.common.client.screen.widget.SiButton;
import com.miophas.singularity_iteration.common.menu.wiring.EnergyTerminalMenu;
import com.miophas.singularity_iteration.common.network.EnergyTerminalTogglePacket;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Energy Management Terminal, styled as a holographic power-grid panel: four KPI gauges
 * (generation vs. nameplate capacity, consumption vs. full demand, storage charge, net balance
 * with time-to-full / time-to-empty), a 112 s trend chart (generation, consumption, charge
 * curve), a ranking of the biggest consumers grouped by machine type, and the filterable
 * device table with per-device read-outs and remote ON/OFF switches.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class EnergyTerminalScreen extends AbstractContainerScreen<EnergyTerminalMenu> {
    private static final int W = 320, H = 214;
    private static final int GEN = DspUi.GREEN, USE = DspUi.ORANGE, CHARGE = DspUi.CYAN;
    private static final int TILE_Y = 17, TILE_H = 38, TILE_W = 75;
    private static final int GRAPH_X = 7, GRAPH_Y = 59, GRAPH_W = 200, GRAPH_H = 72;
    private static final int RANK_X = 210, RANK_Y = 59, RANK_W = 103, RANK_H = 72, RANK_ROWS = 5;
    private static final int LIST_X = 7, LIST_Y = 150, LIST_W = 306, ROW_H = 14, ROWS = 4;
    private static final int[] CATEGORY_COLOR = {GEN, USE, CHARGE, DspUi.VIOLET};

    private int filter = -1;     // -1 all, else Category ordinal
    private int scroll;
    private final SiButton[] filterButtons = new SiButton[4];
    private SiButton scopeButton;
    private int cachedSync = -1, cachedFilter = -2;
    private final List<Device> rows = new ArrayList<>();
    private final List<Rank> ranking = new ArrayList<>();
    private final Map<String, ItemStack> icons = new HashMap<>();
    private float lastChartMax = 1;

    /** Consumption of one machine type: total EU/t, number of machines. */
    private record Rank(String blockId, float use, int count) { }

    public EnergyTerminalScreen(EnergyTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
    }

    @Override
    protected void init() {
        super.init();
        String[] keys = {"all", "generators", "consumers", "storage"};
        int[] ids = {-1, Category.GENERATOR.ordinal(), Category.CONSUMER.ordinal(), Category.STORAGE.ordinal()};
        for (int i = 0; i < 4; i++) {
            int id = ids[i];
            filterButtons[i] = addRenderableWidget(new SiButton(leftPos + 7 + i * 46, topPos + 135, 45, 11,
                Component.translatable("gui.mio_icif.energy_terminal.filter." + keys[i]), b -> setFilter(id)));
        }
        setFilter(filter);
        // scope: this sub-network / the whole system across transformers (global aggregation)
        scopeButton = addRenderableWidget(new SiButton(leftPos + W - 8 - 62, topPos + 4, 62, 11, Component.empty(), b -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menu.snapshot().global ? 0 : 1);
            click();
        }));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (scopeButton != null) {
            boolean global = menu.snapshot().global;
            scopeButton.setMessage(Component.translatable(global ? "gui.mio_icif.energy_terminal.scope.global" : "gui.mio_icif.energy_terminal.scope.local"));
            scopeButton.selected(global);
        }
    }

    private void setFilter(int f) {
        if (filter != f) click();
        filter = f;
        scroll = 0;
        int[] ids = {-1, 0, 1, 2};
        for (int i = 0; i < 4; i++) filterButtons[i].selected(ids[i] == f);
    }

    private void click() {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(mio_icif_sounds.UI_CLICK.get(), 1.0F, 0.6F));
    }

    private void refreshRows() {
        if (cachedSync == menu.syncCount() && cachedFilter == filter) return;
        cachedSync = menu.syncCount();
        cachedFilter = filter;
        rows.clear();
        for (Device d : menu.snapshot().devices) if (filter < 0 || d.category().ordinal() == filter) rows.add(d);
        rows.sort(Comparator.comparingInt((Device d) -> d.category().ordinal())
            .thenComparing(d -> -Math.max(d.input(), d.output())));
        scroll = Mth.clamp(scroll, 0, Math.max(0, rows.size() - ROWS));
        // ranking: consumption grouped by machine type, biggest first
        Map<String, float[]> byType = new HashMap<>();
        for (Device d : menu.snapshot().devices) {
            if (d.category() != Category.CONSUMER) continue;
            float[] acc = byType.computeIfAbsent(d.blockId(), k -> new float[2]);
            acc[0] += d.input();
            acc[1]++;
        }
        ranking.clear();
        byType.forEach((id, acc) -> ranking.add(new Rank(id, acc[0], (int) acc[1])));
        ranking.sort(Comparator.comparingDouble((Rank r) -> -r.use()));
    }

    private ItemStack icon(String blockId) {
        return icons.computeIfAbsent(blockId, id -> {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            Block block = rl == null ? null : BuiltInRegistries.BLOCK.get(rl);
            return block == null ? ItemStack.EMPTY : new ItemStack(block);
        });
    }

    private String name(String blockId) {
        ItemStack stack = icon(blockId);
        return stack.isEmpty() ? blockId : stack.getHoverName().getString();
    }

    // ------------------------------------------------------------------ rendering
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltips(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        refreshRows();
        int x = leftPos, y = topPos;
        SiGuiTheme.panel(g, x, y, W, H);
        EnergyNetworkSnapshot s = menu.snapshot();
        for (int i = 0; i < 4; i++) DspUi.screen(g, x + 7 + i * (TILE_W + 2), y + TILE_Y, TILE_W, TILE_H);
        DspUi.screen(g, x + GRAPH_X, y + GRAPH_Y, GRAPH_W, GRAPH_H);
        DspUi.screen(g, x + RANK_X, y + RANK_Y, RANK_W, RANK_H);
        DspUi.screen(g, x + LIST_X, y + LIST_Y - 1, LIST_W, ROWS * ROW_H + 2);

        // KPI gauges
        float demandRatio = s.demandRatio();
        int demandColor = demandRatio >= 0.999F ? GEN : demandRatio >= 0.5F ? USE : DspUi.RED;
        DspUi.arc(g, x + 7 + 15, y + TILE_Y + 21, 11, 3, s.generationRatio(), GEN);
        DspUi.arc(g, x + 7 + (TILE_W + 2) + 15, y + TILE_Y + 21, 11, 3, demandRatio, demandColor);
        DspUi.arc(g, x + 7 + 2 * (TILE_W + 2) + 15, y + TILE_Y + 21, 11, 3, s.chargeFraction(), CHARGE);
        float net = s.generation + s.storageOut - s.consumption - s.storageIn;
        float balance = s.generation - s.consumption;
        DspUi.arc(g, x + 7 + 3 * (TILE_W + 2) + 15, y + TILE_Y + 21, 11, 3,
            0.5F + 0.5F * (float) Math.tanh(balance / Math.max(1F, Math.max(s.generation, s.consumption))), balance >= 0 ? GEN : DspUi.RED);

        // trend chart
        int cx = x + GRAPH_X + 2, cy = y + GRAPH_Y + 12, cw = GRAPH_W - 4, ch = GRAPH_H - 15;
        float max = 1;
        for (int i = 0; i < EnergyNetworkSnapshot.HISTORY; i++) max = Math.max(max, Math.max(s.genHistory[i], s.useHistory[i]));
        max = niceCeil(max * 1.1F);
        lastChartMax = max;
        for (int gy = 1; gy < 4; gy++) for (int gx = cx; gx < cx + cw; gx += 3) g.fill(gx, cy + ch * gy / 4, gx + 1, cy + ch * gy / 4 + 1, 0x40FFFFFF);
        DspUi.area(g, cx, cy, cw, ch, s.useHistory, max, USE);
        DspUi.area(g, cx, cy, cw, ch, s.genHistory, max, GEN);
        DspUi.line(g, cx, cy, cw, ch, s.chargeHistory, 1F, CHARGE);
        if (isIn(mouseX, mouseY, cx, cy, cw, ch)) g.fill(mouseX, cy, mouseX + 1, cy + ch, 0x80FFFFFF);

        // ranking bars (global mode: per-sub-network supply / demand bars)
        if (s.global) {
            int shown = 0;
            for (int i = 0; i < s.subnets.size() && shown < 4; i++) {
                EnergyNetworkSnapshot.Subnet n = s.subnets.get(i);
                if (n.devices == 0 && n.conductors == 0) continue;
                int ry = y + RANK_Y + 12 + shown * 15;
                g.fill(x + RANK_X + 2, ry, x + RANK_X + 3, ry + 13, subnetColor(i));
                float need = Math.max(n.demand, n.consumption);
                DspUi.meter(g, x + RANK_X + 60, ry + 2, RANK_W - 64 - 30, 2, need <= 0 ? 1F : n.consumption / need, n.consumption + 0.01F >= need ? GEN : DspUi.RED);
                shown++;
            }
        }
        float top = ranking.isEmpty() ? 1 : Math.max(1e-3F, ranking.get(0).use());
        for (int r = 0; r < Math.min(RANK_ROWS, ranking.size()) && !s.global; r++) {
            Rank rank = ranking.get(r);
            int ry = y + RANK_Y + 12 + r * 12;
            DspUi.meter(g, x + RANK_X + 14, ry + 7, RANK_W - 18, 2, rank.use() / top, r == 0 ? DspUi.RED : USE);
        }

        // device table rows
        for (int r = 0; r < ROWS; r++) {
            int i = r + scroll;
            if (i >= rows.size()) break;
            Device d = rows.get(i);
            int ry = y + LIST_Y + r * ROW_H;
            if (isIn(mouseX, mouseY, x + LIST_X, ry, LIST_W, ROW_H)) g.fill(x + LIST_X + 1, ry, x + LIST_X + LIST_W - 1, ry + ROW_H, 0x305FD3F5);
            g.fill(x + LIST_X + 1, ry + 1, x + LIST_X + 3, ry + ROW_H - 1, CATEGORY_COLOR[d.category().ordinal()]);
            if (d.rated() > 0 && d.category() != Category.STORAGE) {
                float load = d.category() == Category.GENERATOR ? d.output() / d.rated() : d.input() / d.rated();
                DspUi.meter(g, x + LIST_X + 180, ry + 6, 40, 2, load, CATEGORY_COLOR[d.category().ordinal()]);
            }
            if (d.switchable()) drawSwitch(g, x + LIST_X + LIST_W - 26, ry + 2, !d.disabled());
        }
        if (rows.size() > ROWS) {
            int barH = ROWS * ROW_H;
            int knob = Math.max(6, barH * ROWS / rows.size());
            int ky = y + LIST_Y + (barH - knob) * scroll / Math.max(1, rows.size() - ROWS);
            g.fill(x + LIST_X + LIST_W - 3, ky, x + LIST_X + LIST_W - 1, ky + knob, DspUi.CYAN_DIM);
        }
    }

    private void drawSwitch(GuiGraphics g, int sx, int sy, boolean on) {
        g.fill(sx, sy, sx + 22, sy + 10, 0xFF56606A);
        g.fill(sx + 1, sy + 1, sx + 21, sy + 9, on ? 0xFF2F8F57 : 0xFF33404C);
        int knob = on ? sx + 12 : sx + 1;
        g.fillGradient(knob, sy + 1, knob + 9, sy + 9, 0xFFFFFFFF, 0xFFC8CCD0);
        g.fill(on ? sx + 3 : sx + 13, sy + 4, on ? sx + 8 : sx + 18, sy + 6, on ? GEN : DspUi.RED);
    }

    private static float niceCeil(float v) {
        double p = Math.pow(10, Math.floor(Math.log10(Math.max(1e-3, v))));
        double m = v / p;
        double nice = m <= 1 ? 1 : m <= 2 ? 2 : m <= 5 ? 5 : 10;
        return (float) (nice * p);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        EnergyNetworkSnapshot s = menu.snapshot();
        g.drawString(font, title, 8, 6, SiGuiTheme.TEXT, false);
        String info = Component.translatable("gui.mio_icif.energy_terminal.summary", s.devices.size(), s.conductors).getString()
            + (s.truncated ? " +" : "");
        small(g, info, W - 8 - 66 - DspUi.smallWidth(font, info), 7, SiGuiTheme.TEXT);

        float balance = s.generation - s.consumption;
        tile(g, 0, "gui.mio_icif.energy_terminal.generation", DspUi.compact(s.generation),
            "/ " + DspUi.compact(s.generationCapacity) + " EU/t", pct(s.generationRatio()) + " " + tr("gui.mio_icif.energy_terminal.util"), GEN);
        float demandRatio = s.demandRatio();
        tile(g, 1, "gui.mio_icif.energy_terminal.consumption", DspUi.compact(s.consumption),
            "/ " + DspUi.compact(s.demand) + " EU/t", pct(demandRatio) + " " + tr("gui.mio_icif.energy_terminal.satisfied"),
            demandRatio >= 0.999F ? GEN : demandRatio >= 0.5F ? USE : DspUi.RED);
        tile(g, 2, "gui.mio_icif.energy_terminal.storage", pct(s.chargeFraction()),
            "+" + DspUi.compact(s.storageIn) + " / -" + DspUi.compact(s.storageOut), DspUi.compact(s.stored) + " EU", CHARGE);
        float flow = s.storageIn - s.storageOut;
        String eta = flow > 0.01F ? tr("gui.mio_icif.energy_terminal.full_in") + " " + DspUi.duration((s.capacity - s.stored) / (flow * 20.0))
            : flow < -0.01F ? tr("gui.mio_icif.energy_terminal.empty_in") + " " + DspUi.duration(s.stored / (-flow * 20.0))
            : tr("gui.mio_icif.energy_terminal.steady");
        tile(g, 3, "gui.mio_icif.energy_terminal.net", (balance >= 0 ? "+" : "") + DspUi.compact(balance), "EU/t", eta,
            balance >= 0 ? GEN : DspUi.RED);

        // chart header and legend
        int lx = GRAPH_X + GRAPH_W - 4;
        lx = legend(g, lx, GRAPH_Y + 3, "gui.mio_icif.energy_terminal.legend.charge", CHARGE);
        lx = legend(g, lx, GRAPH_Y + 3, "gui.mio_icif.energy_terminal.legend.use", USE);
        lx = legend(g, lx, GRAPH_Y + 3, "gui.mio_icif.energy_terminal.legend.gen", GEN);
        small(g, font.plainSubstrByWidth(tr("gui.mio_icif.energy_terminal.chart"), (int) Math.max(0, (lx - GRAPH_X - 8) / 0.75F)),
            GRAPH_X + 4, GRAPH_Y + 3, DspUi.TEXT_DIM);
        small(g, DspUi.compact(lastChartMax), GRAPH_X + 4, GRAPH_Y + 12, DspUi.TEXT_DIM);
        small(g, "-112s", GRAPH_X + 4, GRAPH_Y + GRAPH_H - 8, DspUi.TEXT_DIM);
        small(g, tr("gui.mio_icif.energy_terminal.now"), GRAPH_X + GRAPH_W - 4 - DspUi.smallWidth(font, tr("gui.mio_icif.energy_terminal.now")),
            GRAPH_Y + GRAPH_H - 8, DspUi.TEXT_DIM);

        if (s.global) { renderSubnetLabels(g, s); renderDeviceTable(g); return; }
        // ranking
        small(g, tr("gui.mio_icif.energy_terminal.top_consumers"), RANK_X + 4, RANK_Y + 3, DspUi.TEXT_DIM);
        float total = Math.max(1e-3F, s.consumption);
        for (int r = 0; r < Math.min(RANK_ROWS, ranking.size()); r++) {
            Rank rank = ranking.get(r);
            int ry = RANK_Y + 12 + r * 12;
            g.pose().pushPose();
            g.pose().translate(RANK_X + 3, ry - 1, 0);
            g.pose().scale(0.6F, 0.6F, 1);
            g.renderItem(icon(rank.blockId()), 0, 0);
            g.pose().popPose();
            String label = (rank.count() > 1 ? rank.count() + "× " : "") + name(rank.blockId());
            String value = DspUi.compact(rank.use()) + " (" + Math.round(100 * rank.use() / total) + "%)";
            int vw = DspUi.smallWidth(font, value);
            small(g, font.plainSubstrByWidth(label, (int) ((RANK_W - 20 - vw) / 0.75F)), RANK_X + 14, ry, r == 0 ? DspUi.TEXT : DspUi.TEXT_DIM);
            small(g, value, RANK_X + RANK_W - 4 - vw, ry, r == 0 ? DspUi.RED : USE);
        }
        if (ranking.isEmpty()) small(g, tr("gui.mio_icif.energy_terminal.no_consumers"), RANK_X + 4, RANK_Y + 16, DspUi.TEXT_DIM);
        renderDeviceTable(g);
    }

    /** Global mode: the right panel lists every sub-network (voltage tier, generation / demand, devices). */
    private void renderSubnetLabels(GuiGraphics g, EnergyNetworkSnapshot s) {
        small(g, tr("gui.mio_icif.energy_terminal.subnet_title") + " (" + s.subnets.size() + ")", RANK_X + 4, RANK_Y + 3, DspUi.TEXT_DIM);
        int shown = 0;
        for (int i = 0; i < s.subnets.size() && shown < 4; i++) {
            EnergyNetworkSnapshot.Subnet n = s.subnets.get(i);
            if (n.devices == 0 && n.conductors == 0) continue;
            int ry = RANK_Y + 12 + shown * 15;
            long packet = Math.max(n.ratedPacket, (long) n.measuredPacket);
            String head = "#" + (i + 1) + " " + com.miophas.singularity_iteration.common.blockentity.wiring.terminal.NetworkWalker.tierName(packet)
                + (packet > 0 ? " " + DspUi.compact(packet) + "V" : "");
            small(g, head, RANK_X + 4, ry, subnetColor(i));
            String dev = n.devices + "dev";
            small(g, dev, RANK_X + RANK_W - 4 - DspUi.smallWidth(font, dev), ry, DspUi.TEXT_DIM);
            String flow = "+" + DspUi.compact(n.generation) + " / -" + DspUi.compact(n.consumption) + " EU/t";
            small(g, font.plainSubstrByWidth(flow, (int) ((RANK_W - 8) / 0.75F)), RANK_X + 4, ry + 7, n.generation >= n.consumption ? GEN : DspUi.RED);
            shown++;
        }
        if (s.subnets.size() > shown && shown == 4) small(g, "+" + (s.subnets.size() - 4), RANK_X + RANK_W - 12, RANK_Y + 3, DspUi.TEXT_DIM);
    }

    private static int subnetColor(int i) {
        int[] c = {DspUi.CYAN, DspUi.VIOLET, DspUi.ORANGE, DspUi.GREEN, 0xFFF5D76E, 0xFFEF7FB0};
        return c[Math.floorMod(i, c.length)];
    }

    private void renderDeviceTable(GuiGraphics g) {
        // device table
        String count = rows.size() + "";
        small(g, count, LIST_X + LIST_W - DspUi.smallWidth(font, count), 138, SiGuiTheme.TEXT);
        for (int r = 0; r < ROWS; r++) {
            int i = r + scroll;
            if (i >= rows.size()) break;
            Device d = rows.get(i);
            int ry = LIST_Y + r * ROW_H;
            ItemStack stack = icon(d.blockId());
            g.pose().pushPose();
            g.pose().translate(LIST_X + 5, ry + 1, 0);
            g.pose().scale(0.75F, 0.75F, 1);
            g.renderItem(stack, 0, 0);
            g.pose().popPose();
            String nm = stack.isEmpty() ? d.blockId() : stack.getHoverName().getString();
            int nameColor = d.disabled() ? 0xFF5E6E7B : DspUi.TEXT;
            g.drawString(font, font.plainSubstrByWidth(nm, 140), LIST_X + 19, ry + 3, nameColor, false);
            if (menu.snapshot().global && d.subnet() >= 0) small(g, "#" + (d.subnet() + 1), LIST_X + 162, ry + 4, subnetColor(d.subnet()));
            String value = switch (d.category()) {
                case GENERATOR -> "+" + DspUi.compact(d.output());
                case CONSUMER -> "-" + DspUi.compact(d.input());
                case STORAGE -> d.capacity() > 0 ? String.format("%.0f%%", 100.0 * d.stored() / d.capacity()) : "-";
                default -> DspUi.compact(Math.max(d.input(), d.output()));
            };
            int vColor = d.disabled() ? 0xFF5E6E7B : CATEGORY_COLOR[d.category().ordinal()];
            g.drawString(font, value, LIST_X + LIST_W - 32 - font.width(value), ry + 3, vColor, false);
        }
        if (rows.isEmpty()) {
            g.drawString(font, Component.translatable(menu.snapshot().devices.isEmpty()
                ? "gui.mio_icif.energy_terminal.no_network" : "gui.mio_icif.energy_terminal.no_match"),
                LIST_X + 6, LIST_Y + 4, DspUi.TEXT_DIM, false);
        }
    }

    private void tile(GuiGraphics g, int index, String key, String value, String sub, String foot, int color) {
        int tx = 7 + index * (TILE_W + 2) + 30, ty = TILE_Y;
        // label across the tile top (above the gauge), never past the tile edge
        small(g, font.plainSubstrByWidth(tr(key), (int) ((TILE_W - 8) / 0.75F)), tx - 26, ty + 3, DspUi.TEXT_DIM);
        g.drawString(font, font.plainSubstrByWidth(value, TILE_W - 32), tx, ty + 11, color, false);
        small(g, font.plainSubstrByWidth(sub, (int) ((TILE_W - 32) / 0.75F)), tx, ty + 21, DspUi.TEXT_DIM);
        small(g, font.plainSubstrByWidth(foot, (int) ((TILE_W - 32) / 0.75F)), tx, ty + 29, color);
    }

    private int legend(GuiGraphics g, int rightX, int y, String key, int color) {
        String text = tr(key);
        int w = DspUi.smallWidth(font, text);
        int x = rightX - w;
        small(g, text, x, y, color);
        g.fill(x - 5, y + 2, x - 2, y + 5, color);
        return x - 10;
    }

    private void small(GuiGraphics g, String text, int x, int y, int color) {
        DspUi.small(g, font, text, x, y, color);
    }

    private static String tr(String key) { return Component.translatable(key).getString(); }

    private static String pct(float f) { return String.format("%.0f%%", f * 100); }

    private static String rate(float euPerTick) { return DspUi.compact(euPerTick) + " EU/t"; }

    private void renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        EnergyNetworkSnapshot s = menu.snapshot();
        for (int i = 0; i < 4; i++) {
            if (!isIn(mouseX, mouseY, x + 7 + i * (TILE_W + 2), y + TILE_Y, TILE_W, TILE_H)) continue;
            List<Component> lines = switch (i) {
                case 0 -> List.of(Component.translatable("gui.mio_icif.energy_terminal.generation"),
                    Component.translatable("gui.mio_icif.energy_terminal.tip.generation", rate(s.generation), rate(s.generationCapacity), pct(s.generationRatio())).withStyle(ChatFormatting.GRAY));
                case 1 -> List.of(Component.translatable("gui.mio_icif.energy_terminal.consumption"),
                    Component.translatable("gui.mio_icif.energy_terminal.tip.demand", rate(s.consumption), rate(s.demand), pct(s.demandRatio())).withStyle(ChatFormatting.GRAY));
                case 2 -> List.of(Component.translatable("gui.mio_icif.energy_terminal.storage"),
                    Component.literal(String.format("%,d / %,d EU", s.stored, s.capacity)).withStyle(ChatFormatting.AQUA),
                    Component.translatable("gui.mio_icif.energy_terminal.storage_flow", DspUi.compact(s.storageIn), DspUi.compact(s.storageOut)).withStyle(ChatFormatting.GRAY));
                default -> List.of(Component.translatable("gui.mio_icif.energy_terminal.net"),
                    Component.translatable("gui.mio_icif.energy_terminal.tip.net").withStyle(ChatFormatting.GRAY));
            };
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        int cx = x + GRAPH_X + 2, cw = GRAPH_W - 4;
        if (isIn(mouseX, mouseY, cx, y + GRAPH_Y + 12, cw, GRAPH_H - 15)) {
            int i = Mth.clamp((int) ((mouseX - cx) / (cw / (float) EnergyNetworkSnapshot.HISTORY)), 0, EnergyNetworkSnapshot.HISTORY - 1);
            int ago = EnergyNetworkSnapshot.HISTORY - 1 - i;
            g.renderComponentTooltip(font, List.of(
                Component.translatable("gui.mio_icif.energy_terminal.ago", ago),
                Component.literal(rate(s.genHistory[i])).withColor(GEN),
                Component.literal(rate(s.useHistory[i])).withColor(USE),
                Component.literal(String.format("%.1f%%", s.chargeHistory[i] * 100)).withColor(CHARGE)), mouseX, mouseY);
            return;
        }
        for (int r = 0; r < Math.min(RANK_ROWS, ranking.size()); r++) {
            if (!isIn(mouseX, mouseY, x + RANK_X, y + RANK_Y + 11 + r * 12, RANK_W, 12)) continue;
            Rank rank = ranking.get(r);
            g.renderComponentTooltip(font, List.of(Component.literal(name(rank.blockId())),
                Component.translatable("gui.mio_icif.energy_terminal.tip.rank", rank.count(), rate(rank.use()),
                    pct(rank.use() / Math.max(1e-3F, s.consumption))).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            return;
        }
        for (int r = 0; r < ROWS; r++) {
            int i = r + scroll;
            if (i >= rows.size()) break;
            int ry = y + LIST_Y + r * ROW_H;
            if (!isIn(mouseX, mouseY, x + LIST_X, ry, LIST_W, ROW_H)) continue;
            Device d = rows.get(i);
            ItemStack stack = icon(d.blockId());
            List<Component> lines = new ArrayList<>();
            lines.add(stack.isEmpty() ? Component.literal(d.blockId()) : stack.getHoverName().copy());
            lines.add(Component.translatable("gui.mio_icif.energy_terminal.category." + d.category().name().toLowerCase())
                .withColor(CATEGORY_COLOR[d.category().ordinal()]));
            lines.add(Component.literal(String.format("in %s / out %s EU/t", DspUi.compact(d.input()), DspUi.compact(d.output()))).withStyle(ChatFormatting.GRAY));
            if (d.rated() > 0) lines.add(Component.translatable("gui.mio_icif.energy_terminal.tip.rated", rate(d.rated())).withStyle(ChatFormatting.GRAY));
            if (d.capacity() > 0) lines.add(Component.literal(String.format("%,d / %,d EU", d.stored(), d.capacity())).withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal(d.pos().toShortString()).withStyle(ChatFormatting.DARK_GRAY));
            if (d.switchable()) lines.add(Component.translatable(d.disabled()
                ? "gui.mio_icif.energy_terminal.switched_off" : "gui.mio_icif.energy_terminal.switch_hint")
                .withStyle(d.disabled() ? ChatFormatting.RED : ChatFormatting.DARK_AQUA));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
    }

    private static boolean isIn(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = leftPos, y = topPos;
            for (int r = 0; r < ROWS; r++) {
                int i = r + scroll;
                if (i >= rows.size()) break;
                Device d = rows.get(i);
                int ry = y + LIST_Y + r * ROW_H;
                if (d.switchable() && isIn(mouseX, mouseY, x + LIST_X + LIST_W - 26, ry + 2, 22, 10)) {
                    PacketDistributor.sendToServer(new EnergyTerminalTogglePacket(menu.containerId, d.pos()));
                    if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(mio_icif_sounds.UI_TOGGLE.get(), 1.0F, 0.7F));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isIn(mouseX, mouseY, leftPos + LIST_X, topPos + LIST_Y, LIST_W, ROWS * ROW_H)) {
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, rows.size() - ROWS));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}

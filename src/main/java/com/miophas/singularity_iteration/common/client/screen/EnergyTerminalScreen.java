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
 * <p><b>EN:</b> Energy Management Terminal, styled as a holographic power-grid panel. The top row has
 * four gauges: generation, consumption, storage charge and net balance. The middle row has a 112 s
 * trend chart and a side panel. The side panel shows the biggest consumers (local scope) or the
 * sub-networks (global scope). The bottom has the filterable device table with remote ON/OFF
 * switches. Text layout rule (0.1.8.1): each text gets a measured width in the current language.
 * Right-aligned parts are drawn first; the left part is clipped with "…".
 * <p><b>ZH:</b> 能源管理终端，全息电网面板风格：四个指标仪表（发电/铭牌容量、耗电/满需求、储能电量、净收支与充满/耗尽时间）、
 * 112 秒趋势图、按机器类型的耗电排行（本网）或子网列表（全网），以及带远程开关的可筛选设备表。
 * 文字布局规则（0.1.8.1）：每段文字按当前语言测量宽度；先绘制右对齐部分，左侧部分超出时以“…”截断。
 * <p><b>JA:</b> エネルギー管理端末。ホログラムの電力網パネル風：4 つの指標メーター（発電/定格容量、消費/全需要、蓄電量、収支と満充電/枯渇までの時間）、
 * 112 秒の推移グラフ、機械種別の消費ランキング（局所）またはサブネット一覧（全域）、遠隔入切スイッチ付きの絞り込み可能な機器表。
 * 文字配置の規則（0.1.8.1）：各文字列の幅を現在の言語で測る。右寄せ部分を先に描き、左側ははみ出したら「…」で省略する。
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
    /**
     * <p><b>EN:</b> Sub-network panel (global scope), 0.1.8.1 layout. Each row has two lines of
     * small text (7 px apart) and is {@link #SUBNET_ROW_H} px high; four rows fill the 72 px panel.
     * Line 1: "#n tier voltage" (left, clipped) and the device count (right). Line 2: the flow
     * (left, clipped) and the satisfaction meter (right, fixed {@link #SUBNET_METER_W} px). Text
     * widths are measured in the current language, so no text runs under another text or the meter.
     * <p><b>ZH:</b> 子网面板（全网范围），0.1.8.1 布局。每行两行小字（间距 7 px），行高 {@link #SUBNET_ROW_H} px，4 行填满 72 px 面板。
     * 第 1 行：“#n 电压等级 电压”（左，超长截断）与设备数（右）。第 2 行：功率流（左，超长截断）与满足度条（右，固定 {@link #SUBNET_METER_W} px）。
     * 文字宽度按当前语言测量，文字不会压到其他文字或进度条上。
     * <p><b>JA:</b> サブネットパネル（全域表示）、0.1.8.1 のレイアウト。各行は小さい文字 2 行（間隔 7 px）で高さ {@link #SUBNET_ROW_H} px、
     * 4 行で 72 px のパネルを埋める。1 行目：「#n 電圧階級 電圧」（左、はみ出しは省略）と機器数（右）。2 行目：電力の流れ（左、省略あり）と
     * 充足率メーター（右、固定 {@link #SUBNET_METER_W} px）。文字幅は現在の言語で測るので、文字が他の文字やメーターに重ならない。
     */
    private static final int SUBNET_ROWS = 4, SUBNET_ROW_H = 15, SUBNET_METER_W = 16;

    private int filter = -1;     // -1 all, else Category ordinal
    private int scroll;
    /** EN: First shown sub-network row (mouse wheel). ZH: 第一条显示的子网行（鼠标滚轮）。JA: 表示する最初のサブネット行（マウスホイール）。 */
    private int subnetScroll;
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

        // ranking bars (global mode: per-sub-network colour strip and satisfaction meter)
        if (s.global) {
            List<Integer> shown = visibleSubnets(s);
            for (int row = 0; row < shown.size(); row++) {
                int i = shown.get(row);
                EnergyNetworkSnapshot.Subnet n = s.subnets.get(i);
                int ry = y + RANK_Y + 12 + row * SUBNET_ROW_H;
                if (isIn(mouseX, mouseY, x + RANK_X, ry, RANK_W, SUBNET_ROW_H - 1)) g.fill(x + RANK_X + 1, ry - 1, x + RANK_X + RANK_W - 1, ry + 13, 0x305FD3F5);
                g.fill(x + RANK_X + 2, ry, x + RANK_X + 3, ry + 13, subnetColor(i));
                float need = Math.max(n.demand, n.consumption);
                // 0.1.8.1: the meter is on line 2, right of the clipped flow text (was on line 1, under the header text)
                DspUi.meter(g, x + RANK_X + RANK_W - 4 - SUBNET_METER_W, ry + 9, SUBNET_METER_W, 2,
                    need <= 0 ? 1F : n.consumption / need, n.consumption + 0.01F >= need ? GEN : DspUi.RED);
            }
            int total = nonEmptySubnets(s).size();
            if (total > SUBNET_ROWS) {   // scroll bar on the right edge of the panel
                int barY = y + RANK_Y + 12, barH = SUBNET_ROWS * SUBNET_ROW_H - 2;
                int knob = Math.max(6, barH * SUBNET_ROWS / total);
                int ky = barY + (barH - knob) * subnetScroll / Math.max(1, total - SUBNET_ROWS);
                g.fill(x + RANK_X + RANK_W - 2, ky, x + RANK_X + RANK_W - 1, ky + knob, DspUi.CYAN_DIM);
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
        String info = Component.translatable("gui.mio_icif.energy_terminal.summary", s.devices.size(), s.conductors).getString()
            + (s.truncated ? " +" : "");
        small(g, info, W - 8 - 66 - DspUi.smallWidth(font, info), 7, SiGuiTheme.TEXT);
        // 0.1.8.0: the title stops before the summary (no overlap with long translations)
        com.miophas.singularity_iteration.common.client.screen.mio_icif_screen.drawFitted(g, font, title, 8, 6, W - 8 - 66 - DspUi.smallWidth(font, info) - 14, SiGuiTheme.TEXT, false);

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

    /**
     * <p><b>EN:</b> Global mode: the right panel lists the sub-networks (Subnet Details). Layout rules
     * (0.1.8.1, see {@link #SUBNET_ROWS}): the right-aligned part of each line is drawn first; the
     * left part gets only the remaining width and is clipped with "…". The header shows
     * "first-last/total" when the list scrolls. A tooltip shows the full values.
     * <p><b>ZH:</b> 全网模式：右侧面板列出各子网（子网明细）。布局规则（0.1.8.1，见 {@link #SUBNET_ROWS}）：先绘制每行右对齐的部分；
     * 左侧部分只占剩余宽度，超出部分以“…”截断。列表可滚动时标题显示“起-止/总数”。悬停提示显示完整数值。
     * <p><b>JA:</b> 全域モード：右パネルにサブネット一覧（サブネット内訳）を表示する。レイアウト規則（0.1.8.1、{@link #SUBNET_ROWS} 参照）：
     * 各行の右寄せ部分を先に描き、左側は残りの幅だけを使い「…」で省略する。一覧がスクロールするとき見出しに「先頭-末尾/総数」を出す。
     * ツールチップで完全な値を表示する。
     */
    private void renderSubnetLabels(GuiGraphics g, EnergyNetworkSnapshot s) {
        List<Integer> all = nonEmptySubnets(s);
        List<Integer> shown = visibleSubnets(s);
        String count = all.size() > SUBNET_ROWS
            ? (subnetScroll + 1) + "-" + (subnetScroll + shown.size()) + "/" + all.size()
            : String.valueOf(all.size());
        int countW = DspUi.smallWidth(font, count);
        small(g, count, RANK_X + RANK_W - 4 - countW, RANK_Y + 3, DspUi.TEXT_DIM);
        smallFitted(g, tr("gui.mio_icif.energy_terminal.subnet_title"), RANK_X + 4, RANK_Y + 3, RANK_W - 8 - countW - 4, DspUi.TEXT_DIM);
        for (int row = 0; row < shown.size(); row++) {
            int i = shown.get(row);
            EnergyNetworkSnapshot.Subnet n = s.subnets.get(i);
            int ry = RANK_Y + 12 + row * SUBNET_ROW_H;
            // line 1: device count (right) first, then the header in the remaining width
            String dev = Component.translatable("gui.mio_icif.energy_terminal.subnet_devices", n.devices).getString();
            int devW = DspUi.smallWidth(font, dev);
            small(g, dev, RANK_X + RANK_W - 4 - devW, ry, DspUi.TEXT_DIM);
            smallFitted(g, subnetHeader(i, n), RANK_X + 5, ry, RANK_W - 9 - devW - 3, subnetColor(i));
            // line 2: the meter is at the right (renderBg); the flow uses the width left of it
            int flowMax = RANK_W - 9 - SUBNET_METER_W - 3;
            String flow = "+" + DspUi.compact(n.generation) + " / -" + DspUi.compact(n.consumption);
            if (DspUi.smallWidth(font, flow + " EU/t") <= flowMax) flow += " EU/t";
            smallFitted(g, flow, RANK_X + 5, ry + 7, flowMax, n.generation >= n.consumption ? GEN : DspUi.RED);
        }
    }

    private static String subnetHeader(int index, EnergyNetworkSnapshot.Subnet n) {
        long packet = Math.max(n.ratedPacket, (long) n.measuredPacket);
        return "#" + (index + 1) + " " + com.miophas.singularity_iteration.common.blockentity.wiring.terminal.NetworkWalker.tierName(packet)
            + (packet > 0 ? " " + DspUi.compact(packet) + "V" : "");
    }

    /** EN: Indexes of the sub-networks that have devices or cables. ZH: 有设备或线缆的子网索引。JA: 機器か電線がある子網の番号。 */
    private static List<Integer> nonEmptySubnets(EnergyNetworkSnapshot s) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < s.subnets.size(); i++) {
            EnergyNetworkSnapshot.Subnet n = s.subnets.get(i);
            if (n.devices != 0 || n.conductors != 0) result.add(i);
        }
        return result;
    }

    /** EN: The rows on screen now (at most SUBNET_ROWS, from subnetScroll). ZH: 当前屏幕上的行。JA: 現在画面に出す行。 */
    private List<Integer> visibleSubnets(EnergyNetworkSnapshot s) {
        List<Integer> all = nonEmptySubnets(s);
        subnetScroll = Mth.clamp(subnetScroll, 0, Math.max(0, all.size() - SUBNET_ROWS));
        return all.subList(subnetScroll, Math.min(all.size(), subnetScroll + SUBNET_ROWS));
    }

    /**
     * <p><b>EN:</b> Small (0.75 scale) text clipped to {@code maxPx} screen pixels, with "…" when clipped.
     * <p><b>ZH:</b> 小字（0.75 缩放），截断到 {@code maxPx} 屏幕像素，被截断时加“…”。
     * <p><b>JA:</b> 小さい文字（0.75 倍）を {@code maxPx} 画面ピクセルに収め、省略したときは「…」を付ける。
     */
    private void smallFitted(GuiGraphics g, String text, int x, int y, int maxPx, int color) {
        if (maxPx <= 0) return;
        if (DspUi.smallWidth(font, text) <= maxPx) { small(g, text, x, y, color); return; }
        int room = (int) (maxPx / 0.75F) - font.width("…");
        small(g, font.plainSubstrByWidth(text, Math.max(0, room)) + "…", x, y, color);
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
            com.miophas.singularity_iteration.common.client.screen.mio_icif_screen.drawFitted(g, font, Component.translatable(menu.snapshot().devices.isEmpty()
                ? "gui.mio_icif.energy_terminal.no_network" : "gui.mio_icif.energy_terminal.no_match"),
                LIST_X + 6, LIST_Y + 4, LIST_W - 12, DspUi.TEXT_DIM, false);
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
        if (s.global) {
            // 0.1.8.1: the panel shows sub-networks here; the consumer ranking tooltips were shown
            // for rows that were not drawn.
            List<Integer> shown = visibleSubnets(s);
            for (int row = 0; row < shown.size(); row++) {
                if (!isIn(mouseX, mouseY, x + RANK_X, y + RANK_Y + 12 + row * SUBNET_ROW_H - 1, RANK_W, SUBNET_ROW_H)) continue;
                int i = shown.get(row);
                EnergyNetworkSnapshot.Subnet n = s.subnets.get(i);
                long packet = Math.max(n.ratedPacket, (long) n.measuredPacket);
                float need = Math.max(n.demand, n.consumption);
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal(subnetHeader(i, n)).withColor(subnetColor(i)));
                lines.add(Component.translatable("gui.mio_icif.energy_terminal.tip.subnet.packet",
                    DspUi.compact(n.measuredPacket), DspUi.compact(n.ratedPacket)).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("gui.mio_icif.energy_terminal.tip.generation",
                    rate(n.generation), rate(n.generationCapacity), pct(n.generationCapacity <= 0 ? 0 : Math.min(1F, n.generation / n.generationCapacity))).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("gui.mio_icif.energy_terminal.tip.demand",
                    rate(n.consumption), rate(n.demand), pct(need <= 0 ? 1F : Math.min(1F, n.consumption / need))).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("gui.mio_icif.energy_terminal.storage_flow",
                    DspUi.compact(n.storageIn), DspUi.compact(n.storageOut)).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("gui.mio_icif.energy_terminal.summary", n.devices, n.conductors).withStyle(ChatFormatting.DARK_GRAY));
                if (packet <= 0) lines.add(Component.translatable("gui.mio_icif.energy_terminal.tip.subnet.idle").withStyle(ChatFormatting.DARK_GRAY));
                if (nonEmptySubnets(s).size() > SUBNET_ROWS)
                    lines.add(Component.translatable("gui.mio_icif.energy_terminal.tip.subnet.scroll").withStyle(ChatFormatting.DARK_AQUA));
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
                return;
            }
        }
        for (int r = 0; r < Math.min(RANK_ROWS, ranking.size()) && !s.global; r++) {
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
            lines.add(Component.translatable("gui.mio_icif.energy_terminal.category." + d.category().name().toLowerCase(java.util.Locale.ROOT))
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
        if (menu.snapshot().global && isIn(mouseX, mouseY, leftPos + RANK_X, topPos + RANK_Y, RANK_W, RANK_H)) {
            subnetScroll = Mth.clamp(subnetScroll - (int) Math.signum(scrollY), 0,
                Math.max(0, nonEmptySubnets(menu.snapshot()).size() - SUBNET_ROWS));
            return true;
        }
        if (isIn(mouseX, mouseY, leftPos + LIST_X, topPos + LIST_Y, LIST_W, ROWS * ROW_H)) {
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, rows.size() - ROWS));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}

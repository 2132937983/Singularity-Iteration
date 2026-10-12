package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.screen.widget.SiButton;
import com.miophas.singularity_iteration.common.crop.CropTaxonomy;
import com.miophas.singularity_iteration.common.menu.tool.CropAnalyzerMenu;
import com.miophas.singularity_iteration.common.menu.tool.CropScannerPacket;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import com.miophas.singularity_iteration.core.api.item.ICropSeedItem;
import com.miophas.singularity_iteration.core.runtime.crop.BreedingResearch;
import com.miophas.singularity_iteration.core.runtime.crop.ScannerMath;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * <p><b>EN:</b> Screen of the Advanced Crop Scanner (0.1.8.2). Code-drawn in the SI theme (no
 * background texture). Three tabs: analysis (chest grid; each slot shows its own scan line that
 * moves from top to bottom, the scan ends at the bottom), breeding research (candidate list,
 * progress bar) and gene editing (sliders and -/+ buttons for Growth / Gain / Resistance). A locked
 * tab shows its install cost and an Install button. The battery slot, the four overclocker slots
 * and the energy gauge are on all tabs.
 * <p><b>ZH:</b> 次世代作物扫描仪的界面（0.1.8.2）。以 SI 主题用代码绘制（无背景贴图）。三个标签页：分析（箱子式网格；每个槽位显示自己的扫描线，
 * 从上向下移动，到达底部时扫描完成）、交配研究（候选列表、进度条）与基因编辑（生长／产量／抗性的滑块与 -/+ 按钮）。未解锁的标签页显示安装成本与“安装”按钮。
 * 电池槽、4 个超频槽与能量表在所有标签页中显示。
 * <p><b>JA:</b> 次世代型作物スキャナーの画面（0.1.8.2）。SI テーマでコード描画する（背景テクスチャなし）。3 つのタブ：分析（チェスト型グリッド。
 * 各スロットが自分のスキャンラインを表示し、上から下へ動き、下端で走査が終わる）、交配研究（候補一覧、進捗バー）、遺伝子改変（成長・収量・耐性の
 * スライダーと -/+ ボタン）。ロック中のタブは取り付けコストと「取り付け」ボタンを表示する。電池スロット、オーバークロッカー 4 スロット、エネルギー計は全タブ共通。
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_crop_analyzer extends mio_icif_screen<CropAnalyzerMenu> {
    private static final String K = "gui.mio_icif.crop_scanner.";
    // EN: Layout (GUI-relative).
    // ZH: 布局（相对 GUI）。
    // JA: レイアウト（GUI 相対）。
    private static final int LIST_X = 28, LIST_Y = 30, LIST_W = 118, LIST_ROW = 9, LIST_ROWS = 5;
    private static final int BAR_X = 28, BAR_Y = 78, BAR_W = 116;
    private static final int STAT_X = 28, STAT_Y = 31, STAT_H = 15, TRACK_X = 92, TRACK_W = 48;
    private static final int ENERGY_X = 120, ENERGY_Y = 90, ENERGY_W = 48;

    private final SiButton[] tabs = new SiButton[3];
    private SiButton unlock, breedToggle, geneToggle;
    private final SiButton[] minus = new SiButton[3], plus = new SiButton[3];
    private int listScroll;
    private int dragStat = -1;

    public mio_icif_gui_crop_analyzer(CropAnalyzerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 216;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 123;
        String[] names = {"tab.analysis", "tab.breed", "tab.gene"};
        for (int i = 0; i < 3; i++) {
            final int page = i;
            tabs[i] = addRenderableWidget(new SiButton(leftPos + 7 + i * 54, topPos + 16, 54, 12, Component.translatable(K + names[i]), b -> click(page)));
        }
        unlock = addRenderableWidget(new SiButton(leftPos + 112, topPos + 71, 54, 13, Component.translatable(K + "install"),
            b -> click(menu.page() == CropAnalyzerMenu.PAGE_BREED ? CropAnalyzerMenu.BTN_UNLOCK_BREED : CropAnalyzerMenu.BTN_UNLOCK_GENE)));
        breedToggle = addRenderableWidget(new SiButton(leftPos + 147, topPos + 53, 24, 13, Component.literal("▶"), b -> click(CropAnalyzerMenu.BTN_BREED_TOGGLE)));
        geneToggle = addRenderableWidget(new SiButton(leftPos + 147, topPos + 74, 24, 12, Component.literal("▶"), b -> click(CropAnalyzerMenu.BTN_GENE_TOGGLE)));
        for (int s = 0; s < 3; s++) {
            final int stat = s;
            int y = topPos + STAT_Y + s * STAT_H;
            minus[s] = addRenderableWidget(new SiButton(leftPos + 143, y, 13, 12, Component.literal("-"),
                b -> click(CropAnalyzerMenu.BTN_GENE_ADJUST + stat * 4 + (hasShiftDown() ? 2 : 0))));
            plus[s] = addRenderableWidget(new SiButton(leftPos + 157, y, 13, 12, Component.literal("+"),
                b -> click(CropAnalyzerMenu.BTN_GENE_ADJUST + stat * 4 + (hasShiftDown() ? 3 : 1))));
        }
        updateWidgets();
    }

    private void click(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateWidgets();
    }

    private void updateWidgets() {
        if (tabs[0] == null) return;
        int page = menu.page();
        for (int i = 0; i < 3; i++) tabs[i].selected(page == i);
        boolean locked = !menu.pageUnlocked(page);
        unlock.visible = page != CropAnalyzerMenu.PAGE_ANALYSIS && locked;
        unlock.active = unlock.visible && (canAfford(page) || (minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild));
        boolean breed = page == CropAnalyzerMenu.PAGE_BREED && !locked, gene = page == CropAnalyzerMenu.PAGE_GENE && !locked;
        breedToggle.visible = breed;
        boolean breedRun = menu.data(CropAnalyzerMenu.D_BREED_RUN) != 0;
        breedToggle.setMessage(Component.literal(breedRun ? "■" : "▶"));
        breedToggle.active = breedRun || menu.selectedCandidate() != null;
        geneToggle.visible = gene;
        boolean geneRun = menu.data(CropAnalyzerMenu.D_GENE_RUN) != 0;
        geneToggle.setMessage(Component.literal(geneRun ? "■" : "▶"));
        int[] cur = menu.geneCurrent();
        geneToggle.active = geneRun || (cur != null && ScannerMath.geneCost(cur, menu.geneTarget()) > 0
            && menu.catalystCount() >= ScannerMath.geneCatalyst(cur, menu.geneTarget()));
        for (int s = 0; s < 3; s++) {
            minus[s].visible = plus[s].visible = gene;
            minus[s].active = plus[s].active = gene && !geneRun && cur != null;
        }
    }

    private boolean canAfford(int page) {
        if (minecraft == null || minecraft.player == null) return false;
        CropAnalyzerMenu.Requirement[] req = page == CropAnalyzerMenu.PAGE_BREED ? CropAnalyzerMenu.BREED_UNLOCK : CropAnalyzerMenu.GENE_UNLOCK;
        for (CropAnalyzerMenu.Requirement r : req) if (minecraft.player.getInventory().countItem(r.item()) < r.count()) return false;
        return true;
    }

    // ================================================================== background
    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        SiGuiTheme.panel(g, x, y, imageWidth, imageHeight);
        SiGuiTheme.groove(g, x + 6, y + 28, imageWidth - 12);
        int page = menu.page();
        if (page == CropAnalyzerMenu.PAGE_ANALYSIS) {
            for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) SiGuiTheme.slot(g, x + 7 + c * 18, y + 30 + r * 18);
        } else if (!menu.pageUnlocked(page)) {
            renderLocked(g, x, y, page, mouseX, mouseY);
        } else if (page == CropAnalyzerMenu.PAGE_BREED) {
            renderBreed(g, x, y, mouseX, mouseY);
        } else {
            renderGene(g, x, y, mouseX, mouseY);
        }
        // EN: Shared row: battery, overclockers, energy.
        // ZH: 公共行：电池、超频、能量。
        // JA: 共通の行：電池、オーバークロッカー、エネルギー。
        SiGuiTheme.groove(g, x + 6, y + 86, imageWidth - 12);
        SiGuiTheme.slot(g, x + 7, y + 88);
        for (int i = 0; i < CropAnalyzerMenu.UPGRADES; i++) SiGuiTheme.slot(g, x + 43 + i * 18, y + 88);
        if (menu.getSlot(CropAnalyzerMenu.SLOT_BATTERY).getItem().isEmpty()) batteryGlyph(g, x + 11, y + 92);
        int oc = ScannerMath.overclockers(menu.data(CropAnalyzerMenu.D_OC));
        DspUi.small(g, font, "OC", x + 28, y + 91, oc > 0 ? 0xFF3A6EA5 : 0xFF80868D);
        DspUi.small(g, font, "x" + oc, x + 28, y + 99, oc > 0 ? 0xFF3A6EA5 : 0xFF80868D);
        long energy = menu.energy(), cap = Math.max(1, menu.capacity());
        SiGuiTheme.bar(g, x + ENERGY_X, y + ENERGY_Y, ENERGY_W, 5, energy / (double) cap, 0xFFF5C04A, 0xFFE08A1E);
        String e = DspUi.compact(energy) + " EU";
        DspUi.small(g, font, e, x + ENERGY_X + ENERGY_W - DspUi.smallWidth(font, e), y + ENERGY_Y + 8, SiGuiTheme.TEXT_SCREEN);
        // EN: Player inventory frames.
        // ZH: 玩家物品栏边框。
        // JA: プレイヤーインベントリの枠。
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) SiGuiTheme.slot(g, x + 7 + c * 18, y + 133 + r * 18);
        for (int c = 0; c < 9; c++) SiGuiTheme.slot(g, x + 7 + c * 18, y + 191);
    }

    private static void batteryGlyph(GuiGraphics g, int x, int y) {
        g.fill(x + 3, y, x + 7, y + 1, 0x60585D63);
        g.fill(x + 1, y + 1, x + 9, y + 10, 0x60585D63);
        g.fill(x + 2, y + 2, x + 8, y + 9, 0x30FFFFFF);
    }

    private void renderLocked(GuiGraphics g, int x, int y, int page, int mouseX, int mouseY) {
        SiGuiTheme.well(g, x + 7, y + 31, 162, 54);
        // EN: Padlock icon.
        // ZH: 挂锁图标。
        // JA: 南京錠のアイコン。
        int lx = x + 13, ly = y + 36;
        g.fill(lx + 2, ly, lx + 8, ly + 1, 0xFF585D63);
        g.fill(lx + 1, ly + 1, lx + 2, ly + 5, 0xFF585D63);
        g.fill(lx + 8, ly + 1, lx + 9, ly + 5, 0xFF585D63);
        g.fill(lx, ly + 5, lx + 10, ly + 13, 0xFF585D63);
        g.fill(lx + 1, ly + 6, lx + 9, ly + 12, 0xFFD2902A);
        g.fill(lx + 4, ly + 8, lx + 6, ly + 11, 0xFF585D63);
        Component head = Component.translatable(K + (page == CropAnalyzerMenu.PAGE_BREED ? "locked_breed" : "locked_gene"));
        drawFitted(g, font, head, x + 27, y + 35, 138, SiGuiTheme.TEXT_SCREEN, false);
        drawFitted(g, font, Component.translatable(K + "locked_hint"), x + 27, y + 45, 138, SiGuiTheme.TEXT_SCREEN_DIM, false);
        CropAnalyzerMenu.Requirement[] req = page == CropAnalyzerMenu.PAGE_BREED ? CropAnalyzerMenu.BREED_UNLOCK : CropAnalyzerMenu.GENE_UNLOCK;
        for (int i = 0; i < req.length; i++) {
            int ix = x + 12 + i * 32, iy = y + 63;
            ItemStack icon = new ItemStack(req[i].item());
            g.renderItem(icon, ix, iy);
            int have = minecraft == null || minecraft.player == null ? 0 : minecraft.player.getInventory().countItem(req[i].item());
            String t = Math.min(have, 999) + "/" + req[i].count();
            DspUi.small(g, font, t, ix + 17, iy + 9, have >= req[i].count() ? 0xFF2E7D3E : 0xFFB0352F);
        }
    }

    private void renderBreed(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        SiGuiTheme.slot(g, x + 7, y + 32);
        SiGuiTheme.slot(g, x + 7, y + 62);
        g.drawString(font, "×", x + 13, y + 51, SiGuiTheme.TEXT, false);
        SiGuiTheme.slot(g, x + 151, y + 32);
        // EN: Candidate list.
        // ZH: 候选列表。
        // JA: 候補一覧。
        SiGuiTheme.well(g, x + LIST_X, y + LIST_Y, LIST_W, LIST_ROW * LIST_ROWS + 2);
        List<BreedingResearch.Candidate> list = menu.candidates();
        BreedingResearch.Candidate selected = menu.selectedCandidate();
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, list.size() - LIST_ROWS));
        if (list.isEmpty()) {
            drawFitted(g, font, Component.translatable(K + "need_parents"), x + LIST_X + 3, y + LIST_Y + 12, LIST_W - 6, SiGuiTheme.TEXT_SCREEN_DIM, false);
            drawFitted(g, font, Component.translatable(K + "need_parents2"), x + LIST_X + 3, y + LIST_Y + 23, LIST_W - 6, SiGuiTheme.TEXT_SCREEN_DIM, false);
        }
        int hovered = listRowAt(mouseX, mouseY);
        for (int r = 0; r < LIST_ROWS && listScroll + r < list.size(); r++) {
            BreedingResearch.Candidate c = list.get(listScroll + r);
            int ry = y + LIST_Y + 1 + r * LIST_ROW;
            boolean sel = c == selected || (selected != null && c.key().equals(selected.key()));
            if (sel) g.fill(x + LIST_X + 1, ry, x + LIST_X + LIST_W - 4, ry + LIST_ROW, SiGuiTheme.ACCENT);
            else if (hovered == listScroll + r) g.fill(x + LIST_X + 1, ry, x + LIST_X + LIST_W - 4, ry + LIST_ROW, 0x303A6EA5);
            int color = sel ? 0xFFFFFFFF : SiGuiTheme.TEXT_SCREEN;
            String cost = DspUi.compact(c.cost());
            int cw = DspUi.smallWidth(font, cost);
            g.pose().pushPose();
            g.pose().translate(x + LIST_X + 3, ry + 2, 0);
            g.pose().scale(0.75F, 0.75F, 1F);
            drawFitted(g, font, Component.translatable(c.plant().getTranslationKey()), 0, 0, (int) ((LIST_W - 12 - cw) / 0.75F), color, false);
            g.pose().popPose();
            DspUi.small(g, font, cost, x + LIST_X + LIST_W - 6 - cw, ry + 2, sel ? 0xFFDDE8F5 : 0xFF6B737D);
        }
        if (list.size() > LIST_ROWS) {
            int trackH = LIST_ROW * LIST_ROWS - 2, knobH = Math.max(6, trackH * LIST_ROWS / list.size());
            int ky = (int) ((trackH - knobH) * (listScroll / (double) (list.size() - LIST_ROWS)));
            g.fill(x + LIST_X + LIST_W - 3, y + LIST_Y + 2, x + LIST_X + LIST_W - 2, y + LIST_Y + 2 + trackH, 0x40585D63);
            g.fill(x + LIST_X + LIST_W - 3, y + LIST_Y + 2 + ky, x + LIST_X + LIST_W - 2, y + LIST_Y + 2 + ky + knobH, SiGuiTheme.ACCENT);
        }
        float p = menu.data(CropAnalyzerMenu.D_BREED_PROGRESS) / 1000F;
        SiGuiTheme.bar(g, x + BAR_X, y + BAR_Y, BAR_W, 5, p, 0xFFC08AF0, 0xFF8A5BD0);
    }

    private void renderGene(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        SiGuiTheme.slot(g, x + 7, y + 32);
        SiGuiTheme.slot(g, x + 7, y + 62);
        if (menu.getSlot(CropAnalyzerMenu.SLOT_CATALYST).getItem().isEmpty()) DspUi.small(g, font, "UU", x + 11, y + 68, 0x80585D63);
        int[] cur = menu.geneCurrent(), tgt = menu.geneTarget();
        String[] labels = {"stat.growth", "stat.gain", "stat.resistance"};
        int[] colors = {0xFF3C9A52, 0xFFC07A10, 0xFF2A6E8A};
        for (int s = 0; s < 3; s++) {
            int ry = y + STAT_Y + s * STAT_H;
            drawFitted(g, font, Component.translatable(K + labels[s]), x + STAT_X, ry + 2, 32, colors[s], false);
            String value = cur == null ? "--" : cur[s] + "→" + tgt[s];
            int vc = cur == null ? SiGuiTheme.TEXT_SCREEN_DIM : tgt[s] > cur[s] ? 0xFF2E7D3E : tgt[s] < cur[s] ? 0xFFB0352F : SiGuiTheme.TEXT_SCREEN;
            DspUi.small(g, font, value, x + 62, ry + 3, vc);
            // EN: Slider track: filled to the target, a mark at the current value.
            // ZH: 滑块轨道：填充到目标值，当前值处有标记。
            // JA: スライダーの軌道：目標値まで塗り、現在値に印。
            int tx = x + TRACK_X, ty = ry + 4;
            g.fill(tx - 1, ty - 1, tx + TRACK_W + 1, ty + 5, SiGuiTheme.SLOT_SHADOW);
            g.fill(tx, ty, tx + TRACK_W, ty + 4, SiGuiTheme.SCREEN_BG);
            if (cur != null) {
                int fill = tgt[s] * TRACK_W / 31;
                g.fillGradient(tx, ty, tx + fill, ty + 4, DspUi.brighten(colors[s]), colors[s]);
                int mark = tx + cur[s] * (TRACK_W - 1) / 31;
                g.fill(mark, ty - 2, mark + 1, ty + 6, 0xFF2A2E33);
                int knob = tx + tgt[s] * (TRACK_W - 3) / 31;
                g.fill(knob, ty - 2, knob + 3, ty + 6, 0xFFFFFFFF);
                g.renderOutline(knob, ty - 2, 3, 8, 0xFF585D63);
            }
        }
        float p = menu.data(CropAnalyzerMenu.D_GENE_PROGRESS) / 1000F;
        SiGuiTheme.bar(g, x + BAR_X, y + BAR_Y, BAR_W, 5, p, 0xFF7FE0F0, 0xFF2A9AB8);
    }

    // ================================================================== foreground
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        drawTitle(g);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, SiGuiTheme.TEXT, false);
        int page = menu.page();
        if (page == CropAnalyzerMenu.PAGE_ANALYSIS) renderScanlines(g);
        Component status = statusText(page);
        if (status != null) drawFitted(g, font, status, 8, 109, 160, statusColor(page), false);
    }

    /** EN: Scan lines over the analysis slots (drawn above the items). ZH: 分析槽位上的扫描线（绘制在物品之上）。JA: 分析スロット上のスキャンライン（アイテムの上に描く）。 */
    private void renderScanlines(GuiGraphics g) {
        long time = minecraft == null || minecraft.level == null ? 0 : minecraft.level.getGameTime();
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        for (int i = 0; i < CropAnalyzerMenu.GRID; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            int sx = 8 + (i % 9) * 18, sy = 31 + (i / 9) * 18;
            if (stack.isEmpty() || !(stack.getItem() instanceof ICropSeedItem seed)) continue;
            if (seed.cropScanLevel(stack) >= 4) {
                // EN: Analyzed: small green corner mark.
                // ZH: 已分析：绿色角标。
                // JA: 分析済み：緑の角の印。
                g.fill(sx + 13, sy, sx + 16, sy + 1, 0xFF3C9A52);
                g.fill(sx + 15, sy, sx + 16, sy + 3, 0xFF3C9A52);
                continue;
            }
            int permille = menu.data(CropAnalyzerMenu.D_PROGRESS + i);
            if (permille <= 0) {
                if (menu.data(CropAnalyzerMenu.D_SCAN_STATUS) == CropAnalyzerMenu.ST_NO_ENERGY) g.fill(sx, sy + 15, sx + 16, sy + 16, 0xA0C8423C);
                continue;
            }
            int line = Math.min(15, permille * 16 / 1000);
            g.fill(sx, sy, sx + 16, sy + line, 0x2E5FD3F5);
            int pulse = (int) (0x90 + 0x50 * Math.sin((time + i * 3) * 0.6));
            g.fill(sx, sy + line, sx + 16, sy + line + 1, (pulse << 24) | 0x7FF0FF);
            if (line > 0) g.fill(sx, sy + line - 1, sx + 16, sy + line, 0x405FD3F5);
            if (line < 15) g.fill(sx, sy + line + 1, sx + 16, sy + line + 2, 0x305FD3F5);
        }
        g.pose().popPose();
    }

    private Component statusText(int page) {
        if (page != CropAnalyzerMenu.PAGE_ANALYSIS && !menu.pageUnlocked(page)) return null;
        int oc = ScannerMath.overclockers(menu.data(CropAnalyzerMenu.D_OC));
        if (page == CropAnalyzerMenu.PAGE_ANALYSIS) {
            int scanning = 0;
            long eut = 0;
            for (int i = 0; i < CropAnalyzerMenu.GRID; i++) {
                ItemStack s = menu.getSlot(i).getItem();
                if (s.getItem() instanceof ICropSeedItem seed && seed.cropScanLevel(s) < 4) {
                    scanning++;
                    eut += ScannerMath.analysisEuPerTick(s.getCount(), oc);
                }
            }
            return switch (menu.data(CropAnalyzerMenu.D_SCAN_STATUS)) {
                case CropAnalyzerMenu.ST_RUNNING -> Component.translatable(K + "scan_running", scanning, DspUi.compact(eut),
                    DspUi.duration(ScannerMath.analysisTicks(oc) / 20.0));
                case CropAnalyzerMenu.ST_NO_ENERGY -> Component.translatable(K + "no_energy");
                default -> Component.translatable(K + "scan_idle");
            };
        }
        if (page == CropAnalyzerMenu.PAGE_BREED) {
            BreedingResearch.Candidate c = menu.selectedCandidate();
            return switch (menu.data(CropAnalyzerMenu.D_BREED_STATUS)) {
                case CropAnalyzerMenu.ST_NOT_ANALYZED -> Component.translatable(K + "need_parents");
                case CropAnalyzerMenu.ST_SELECT_TARGET -> Component.translatable(K + "select_target");
                case CropAnalyzerMenu.ST_OUTPUT_FULL -> Component.translatable(K + "output_full");
                case CropAnalyzerMenu.ST_NO_ENERGY -> Component.translatable(K + "no_energy");
                case CropAnalyzerMenu.ST_DONE -> Component.translatable(K + "breed_done");
                case CropAnalyzerMenu.ST_RUNNING -> c == null ? null : Component.translatable(K + "breed_running",
                    Component.translatable(c.plant().getTranslationKey()), eta(c.cost(), ScannerMath.BREED_POWER, oc, CropAnalyzerMenu.D_BREED_PROGRESS));
                default -> c == null ? null : Component.translatable(K + "breed_ready", DspUi.compact(ScannerMath.researchTotalEu(c.cost(), ScannerMath.BREED_POWER, oc)),
                    DspUi.duration(ScannerMath.researchTicks(c.cost(), ScannerMath.BREED_POWER, oc) / 20.0));
            };
        }
        int[] cur = menu.geneCurrent(), tgt = menu.geneTarget();
        long cost = cur == null ? 0 : ScannerMath.geneCost(cur, tgt);
        int uu = cur == null ? 0 : ScannerMath.geneCatalyst(cur, tgt);
        return switch (menu.data(CropAnalyzerMenu.D_GENE_STATUS)) {
            case CropAnalyzerMenu.ST_NOT_ANALYZED -> Component.translatable(K + "need_seed");
            case CropAnalyzerMenu.ST_NO_CHANGE -> Component.translatable(K + "no_change");
            case CropAnalyzerMenu.ST_NEED_CATALYST -> Component.translatable(K + "need_catalyst", uu);
            case CropAnalyzerMenu.ST_NO_ENERGY -> Component.translatable(K + "no_energy");
            case CropAnalyzerMenu.ST_DONE -> Component.translatable(K + "gene_done");
            case CropAnalyzerMenu.ST_RUNNING -> Component.translatable(K + "gene_running", eta(cost, ScannerMath.GENE_POWER, oc, CropAnalyzerMenu.D_GENE_PROGRESS));
            default -> cost == 0 ? Component.translatable(K + "no_change") : Component.translatable(K + "gene_ready", DspUi.compact(ScannerMath.researchTotalEu(cost, ScannerMath.GENE_POWER, oc)),
                DspUi.duration(ScannerMath.researchTicks(cost, ScannerMath.GENE_POWER, oc) / 20.0), uu);
        };
    }

    private int statusColor(int page) {
        int code = menu.data(page == CropAnalyzerMenu.PAGE_ANALYSIS ? CropAnalyzerMenu.D_SCAN_STATUS
            : page == CropAnalyzerMenu.PAGE_BREED ? CropAnalyzerMenu.D_BREED_STATUS : CropAnalyzerMenu.D_GENE_STATUS);
        return switch (code) {
            case CropAnalyzerMenu.ST_RUNNING -> 0xFF2F5A88;
            case CropAnalyzerMenu.ST_DONE -> 0xFF2E7D3E;
            case CropAnalyzerMenu.ST_NO_ENERGY, CropAnalyzerMenu.ST_NEED_CATALYST, CropAnalyzerMenu.ST_OUTPUT_FULL -> 0xFFB0352F;
            default -> 0xFF5A6067;
        };
    }

    private String eta(long cost, long power, int oc, int progressIndex) {
        int ticks = ScannerMath.researchTicks(cost, power, oc);
        double left = ticks * (1 - menu.data(progressIndex) / 1000.0) / 20.0;
        return (menu.data(progressIndex) / 10) + "% · " + DspUi.duration(left);
    }

    @Override
    protected int statusLampColor() {
        int a = menu.data(CropAnalyzerMenu.D_SCAN_STATUS), b = menu.data(CropAnalyzerMenu.D_BREED_STATUS), c = menu.data(CropAnalyzerMenu.D_GENE_STATUS);
        if (a == CropAnalyzerMenu.ST_RUNNING || b == CropAnalyzerMenu.ST_RUNNING || c == CropAnalyzerMenu.ST_RUNNING) return SiGuiTheme.GOOD;
        if (a == CropAnalyzerMenu.ST_NO_ENERGY || b == CropAnalyzerMenu.ST_NO_ENERGY || c == CropAnalyzerMenu.ST_NO_ENERGY) return SiGuiTheme.WARN;
        return SiGuiTheme.LED_OFF;
    }

    // ================================================================== input
    private int listRowAt(double mx, double my) {
        if (menu.page() != CropAnalyzerMenu.PAGE_BREED || !menu.pageUnlocked(CropAnalyzerMenu.PAGE_BREED)) return -1;
        int rx = (int) mx - leftPos - LIST_X, ry = (int) my - topPos - LIST_Y - 1;
        if (rx < 1 || rx >= LIST_W - 4 || ry < 0 || ry >= LIST_ROW * LIST_ROWS) return -1;
        int index = listScroll + ry / LIST_ROW;
        return index < menu.candidates().size() ? index : -1;
    }

    private int trackAt(double mx, double my) {
        if (menu.page() != CropAnalyzerMenu.PAGE_GENE || !menu.pageUnlocked(CropAnalyzerMenu.PAGE_GENE) || menu.geneCurrent() == null
                || menu.data(CropAnalyzerMenu.D_GENE_RUN) != 0) return -1;
        int rx = (int) mx - leftPos - TRACK_X;
        if (rx < -2 || rx > TRACK_W + 2) return -1;
        for (int s = 0; s < 3; s++) {
            int ry = (int) my - topPos - STAT_Y - s * STAT_H;
            if (ry >= 1 && ry <= 11) return s;
        }
        return -1;
    }

    private void setFromTrack(int stat, double mx) {
        int value = Mth.clamp((int) Math.round((mx - leftPos - TRACK_X) * 31.0 / (TRACK_W - 1)), 0, 31);
        if (menu.geneTarget()[stat] == value) return;
        try {
            PacketDistributor.sendToServer(new CropScannerPacket.SetGene(menu.containerId, stat, value));
        } catch (RuntimeException ignored) { }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int row = listRowAt(mouseX, mouseY);
            if (row >= 0) {
                click(CropAnalyzerMenu.BTN_SELECT + row);
                playClick();
                return true;
            }
            int stat = trackAt(mouseX, mouseY);
            if (stat >= 0) {
                dragStat = stat;
                setFromTrack(stat, mouseX);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragStat >= 0 && button == 0) {
            setFromTrack(dragStat, mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragStat >= 0 && button == 0) {
            dragStat = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int rx = (int) mouseX - leftPos - LIST_X, ry = (int) mouseY - topPos - LIST_Y;
        if (menu.page() == CropAnalyzerMenu.PAGE_BREED && rx >= 0 && rx < LIST_W && ry >= 0 && ry < LIST_ROW * LIST_ROWS + 2) {
            listScroll = Mth.clamp(listScroll - (int) Math.signum(scrollY), 0, Math.max(0, menu.candidates().size() - LIST_ROWS));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ================================================================== tooltips
    @Override
    protected void renderCustomTooltips(GuiGraphics g, int mouseX, int mouseY) {
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        int oc = ScannerMath.overclockers(menu.data(CropAnalyzerMenu.D_OC));
        List<Component> tip = new ArrayList<>();
        if (rx >= ENERGY_X - 1 && rx <= ENERGY_X + ENERGY_W + 1 && ry >= ENERGY_Y - 1 && ry <= ENERGY_Y + 14) {
            tip.add(Component.translatable(K + "tip.energy", DspUi.compact(menu.energy()), DspUi.compact(menu.capacity())));
            tip.add(Component.translatable(K + "tip.energy_charge", com.miophas.singularity_iteration.common.item.tools.CropAnalyzerItem.CHARGE_RATE)
                .withStyle(ChatFormatting.GRAY));
        } else if (rx >= 26 && rx <= 42 && ry >= 88 && ry <= 106) {
            tip.add(Component.translatable(K + "tip.oc", menu.data(CropAnalyzerMenu.D_OC), ScannerMath.MAX_OVERCLOCKERS));
            tip.add(Component.translatable(K + "tip.oc_time", String.format("%.2f", ScannerMath.timeFactor(oc))).withStyle(ChatFormatting.GRAY));
            tip.add(Component.translatable(K + "tip.oc_power", String.format("%.2f", ScannerMath.powerFactor(oc))).withStyle(ChatFormatting.GRAY));
        } else if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < CropAnalyzerMenu.SIZE) {
            String key = switch (hoveredSlot.index) {
                case CropAnalyzerMenu.SLOT_BATTERY -> "tip.slot_battery";
                case CropAnalyzerMenu.SLOT_PARENT_A, CropAnalyzerMenu.SLOT_PARENT_B -> "tip.slot_parent";
                case CropAnalyzerMenu.SLOT_BREED_OUT -> "tip.slot_output";
                case CropAnalyzerMenu.SLOT_GENE -> "tip.slot_gene";
                case CropAnalyzerMenu.SLOT_CATALYST -> "tip.slot_catalyst";
                default -> hoveredSlot.index >= CropAnalyzerMenu.SLOT_UPGRADE && hoveredSlot.index < CropAnalyzerMenu.SLOT_UPGRADE + CropAnalyzerMenu.UPGRADES
                    ? "tip.slot_upgrade" : null;
            };
            if (key != null) tip.add(Component.translatable(K + key));
            if (hoveredSlot.index == CropAnalyzerMenu.SLOT_BREED_OUT && menu.selectedCandidate() != null) previewResult(tip);
        } else {
            int row = listRowAt(mouseX, mouseY);
            if (row >= 0) candidateTip(menu.candidates().get(row), oc, tip);
            int stat = menu.page() == CropAnalyzerMenu.PAGE_GENE && menu.pageUnlocked(CropAnalyzerMenu.PAGE_GENE) ? statRowAt(rx, ry) : -1;
            if (stat >= 0) geneTip(stat, tip);
            if (menu.page() != CropAnalyzerMenu.PAGE_ANALYSIS && !menu.pageUnlocked(menu.page())) lockedTip(rx, ry, tip);
        }
        if (!tip.isEmpty()) g.renderComponentTooltip(font, tip, mouseX, mouseY);
    }

    private int statRowAt(int rx, int ry) {
        if (rx < STAT_X || rx > TRACK_X + TRACK_W) return -1;
        for (int s = 0; s < 3; s++) if (ry >= STAT_Y + s * STAT_H && ry < STAT_Y + s * STAT_H + 12) return s;
        return -1;
    }

    private void candidateTip(BreedingResearch.Candidate c, int oc, List<Component> tip) {
        tip.add(Component.translatable(c.plant().getTranslationKey()).withStyle(ChatFormatting.AQUA));
        tip.add(CropTaxonomy.scientificName(c.plant().getModId(), c.plant().getTypeId()).copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        tip.add(Component.translatable(K + "tip.tier", c.plant().getStats().getLevel()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable(K + "tip.chance", String.format("%.1f", c.share() * 100)).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable(K + "tip.cost", DspUi.compact(ScannerMath.researchTotalEu(c.cost(), ScannerMath.BREED_POWER, oc)),
            DspUi.duration(ScannerMath.researchTicks(c.cost(), ScannerMath.BREED_POWER, oc) / 20.0),
            DspUi.compact(ScannerMath.researchEuPerTick(ScannerMath.BREED_POWER, oc))).withStyle(ChatFormatting.YELLOW));
    }

    private void previewResult(List<Component> tip) {
        BreedingResearch.Candidate c = menu.selectedCandidate();
        ItemStack a = menu.getSlot(CropAnalyzerMenu.SLOT_PARENT_A).getItem(), b = menu.getSlot(CropAnalyzerMenu.SLOT_PARENT_B).getItem();
        if (c == null || !(a.getItem() instanceof ICropSeedItem sa) || !(b.getItem() instanceof ICropSeedItem sb)) return;
        tip.add(Component.translatable(K + "tip.result", Component.translatable(c.plant().getTranslationKey()),
            ScannerMath.breedTrait(sa.cropGrowth(a), sb.cropGrowth(b)), ScannerMath.breedTrait(sa.cropGain(a), sb.cropGain(b)),
            ScannerMath.breedTrait(sa.cropResistance(a), sb.cropResistance(b))).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private void geneTip(int stat, List<Component> tip) {
        int[] cur = menu.geneCurrent(), tgt = menu.geneTarget();
        if (cur == null) return;
        tip.add(Component.translatable(K + "tip.gene_step", cur[stat], tgt[stat], DspUi.compact(ScannerMath.geneStepCost(cur[stat], tgt[stat]))));
        tip.add(Component.translatable(K + "tip.gene_rule").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable(K + "tip.gene_shift").withStyle(ChatFormatting.DARK_GRAY));
    }

    private void lockedTip(int rx, int ry, List<Component> tip) {
        CropAnalyzerMenu.Requirement[] req = menu.page() == CropAnalyzerMenu.PAGE_BREED ? CropAnalyzerMenu.BREED_UNLOCK : CropAnalyzerMenu.GENE_UNLOCK;
        for (int i = 0; i < req.length; i++) {
            int ix = 12 + i * 32, iy = 63;
            if (rx >= ix && rx < ix + 16 && ry >= iy && ry < iy + 16) {
                tip.add(Component.translatable(K + "tip.require", req[i].count(), new ItemStack(req[i].item()).getHoverName()));
                if (menu.page() == CropAnalyzerMenu.PAGE_GENE) tip.add(Component.translatable(K + "tip.gene_catalyst_rule").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    protected boolean showUpgradeHint() { return false; }
}

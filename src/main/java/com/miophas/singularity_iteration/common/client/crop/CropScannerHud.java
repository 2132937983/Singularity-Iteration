// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.screen.DspUi;
import com.miophas.singularity_iteration.common.crop.CropDiagnosis;
import com.miophas.singularity_iteration.common.crop.CropTaxonomy;
import com.miophas.singularity_iteration.common.item.tools.CropAnalyzerItem;
import com.miophas.singularity_iteration.common.menu.tool.CropScannerPacket;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import com.miophas.singularity_iteration.core.runtime.crop.CropGrowthMath;
import com.miophas.singularity_iteration.core.runtime.crop.PlantRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * <p><b>EN:</b> Crop HUD of the Advanced Crop Scanner (0.1.8.2). While the player holds a charged scanner
 * in either hand and aims at a crop stick, a holographic panel beside the crosshair shows:
 * plant name and scientific name, stage and maturity, Growth / Gain / Resistance, environment
 * (humidity, soil nutrients, air quality, light), stored water / nutrients / Weed-EX, the
 * environment quality against the requirement (growth rate or penalty), and the first growth
 * condition that is not met. The data comes from the server twice a second
 * ({@link CropScannerPacket.HudRequest}); each reading costs {@link CropAnalyzerItem#HUD_EU_PER_READING} EU.
 * An empty scanner shows only "no energy".
 * <p><b>ZH:</b> 次世代作物扫描仪的作物 HUD（0.1.8.2）。玩家任一只手持有有电的扫描仪并瞄准作物架时，准星旁的全息面板显示：植物名与学名、阶段与成熟度、
 * 生长／产量／抗性、环境（湿度、土壤营养、空气质量、光照）、储水／营养／除草剂、环境品质与需求的比较（生长速度或惩罚），以及第一个未满足的生长条件。
 * 数据每秒两次来自服务器（{@link CropScannerPacket.HudRequest}），每次读数消耗 {@link CropAnalyzerItem#HUD_EU_PER_READING} EU。没电的扫描仪只显示 “无电”。
 * <p><b>JA:</b> 次世代型作物スキャナーの作物 HUD（0.1.8.2）。どちらかの手に充電済みのスキャナーを持って作物棒を狙うと、照準の横のホログラムパネルに次を表示する：
 * 植物名と学名、段階と成熟度、成長・収量・耐性、環境（湿度・土壌栄養・空気品質・明るさ）、貯水・栄養・除草剤、要求に対する環境品質（成長速度かペナルティ）、
 * 満たしていない最初の成長条件。データは毎秒 2 回サーバーから来る（{@link CropScannerPacket.HudRequest}）。読み取り 1 回に
 * {@link CropAnalyzerItem#HUD_EU_PER_READING} EU を使う。電力のないスキャナーは「電力なし」だけを表示する。
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class CropScannerHud {
    private CropScannerHud() {}

    private static int timer;
    private static final String K = "hud.mio_icif.crop_scanner.";

    private static ItemStack scanner(Minecraft mc) {
        return mc.player == null ? ItemStack.EMPTY : CropScannerPacket.heldScanner(mc.player);
    }

    private static boolean charged(ItemStack scanner) {
        return scanner.getItem() instanceof CropAnalyzerItem item && item.getEnergy(scanner) >= CropAnalyzerItem.HUD_EU_PER_READING;
    }

    private static BlockPos target(Minecraft mc) {
        if (mc.level == null || mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return null;
        BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
        return mc.level.getBlockEntity(pos) instanceof IPlanter ? pos : null;
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        ItemStack scanner = scanner(mc);
        if (scanner.isEmpty() || !charged(scanner)) return;
        BlockPos pos = target(mc);
        if (pos == null) { timer = 0; return; }
        if (timer-- > 0) return;
        timer = 10;
        try {
            PacketDistributor.sendToServer(new CropScannerPacket.HudRequest(pos));
        } catch (RuntimeException ignored) { }
    }

    @SubscribeEvent
    static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null) return;
        ItemStack scanner = scanner(mc);
        if (scanner.isEmpty()) return;
        BlockPos pos = target(mc);
        if (pos == null) return;
        List<Line> lines = new ArrayList<>();
        if (!charged(scanner)) {
            lines.add(new Line(Component.translatable(K + "no_energy"), DspUi.RED));
        } else {
            CropScannerPacket.HudReply reply = CropScannerPacket.latest();
            if (reply == null || !reply.pos().equals(pos)) lines.add(new Line(Component.translatable(K + "scanning"), DspUi.TEXT_DIM));
            else describe(reply.snapshot(), lines);
        }
        draw(event.getGuiGraphics(), mc, lines);
    }

    /** EN: One HUD line: text, colour, optional bar fraction (negative = no bar). ZH: 一行 HUD：文本、颜色、可选进度条比例（负数 = 无进度条）。JA: HUD の 1 行：文字、色、任意のバー比率（負 = バーなし）。 */
    record Line(Component text, int color, float bar) {
        Line(Component text, int color) { this(text, color, -1F); }
    }

    /**
     * <p><b>EN:</b> Lines for one reading (shared with the scanner menu tooltips and tests).
     * <p><b>ZH:</b> 一次读数的各行（扫描仪菜单提示与测试共用）。
     * <p><b>JA:</b> 1 回の読み取りの各行（スキャナーメニューのツールチップとテストで共用）。
     */
    static void describe(CropDiagnosis.Snapshot s, List<Line> out) {
        if (s.empty()) {
            out.add(new Line(Component.translatable(s.has(CropDiagnosis.F_HYBRID) ? K + "crossing" : K + "empty"), DspUi.CYAN));
            out.add(new Line(Component.translatable(K + "env", s.humidity(), s.soil(), s.air(), s.light()), DspUi.TEXT_DIM));
            out.add(stored(s));
            return;
        }
        PlantType plant = PlantRegistry.instance.getPlant(s.modId(), s.typeId());
        Component name = plant == null ? Component.literal(s.modId() + ":" + s.typeId()) : Component.translatable(plant.getTranslationKey());
        int tier = plant == null ? 0 : plant.getStats().getLevel();
        out.add(new Line(Component.empty().append(name.copy().withStyle(ChatFormatting.BOLD))
            .append(Component.literal("  ")).append(Component.translatable(K + "tier", tier)), s.has(CropDiagnosis.F_WEED) ? DspUi.RED : DspUi.CYAN));
        out.add(new Line(CropTaxonomy.scientificName(s.modId(), s.typeId()).copy().withStyle(ChatFormatting.ITALIC), DspUi.TEXT_DIM));

        float maturity = maturity(s);
        Component stage = Component.translatable(K + "stage", s.stage(), s.maxStage());
        Component state = s.has(CropDiagnosis.F_HARVESTABLE) ? Component.translatable(K + "harvestable")
            : Component.translatable(K + "maturity", Math.round(maturity * 100));
        out.add(new Line(Component.empty().append(stage).append(" · ").append(state),
            s.has(CropDiagnosis.F_HARVESTABLE) ? DspUi.GREEN : DspUi.TEXT, maturity));

        out.add(new Line(Component.translatable(K + "traits", s.growth(), s.gain(), s.resistance()), DspUi.ORANGE));
        out.add(new Line(Component.translatable(K + "env", s.humidity(), s.soil(), s.air(), s.light()), DspUi.TEXT));

        int rate = s.growthRate(), penalty = s.penalty();
        int qualityColor = penalty > CropGrowthMath.LETHAL_PENALTY ? DspUi.RED : penalty > 0 ? DspUi.ORANGE : DspUi.GREEN;
        out.add(new Line(Component.translatable(K + "quality", s.quality(), s.required(), rate), qualityColor));
        if (penalty > CropGrowthMath.LETHAL_PENALTY) out.add(new Line(Component.translatable(K + "lethal", penalty), DspUi.RED));
        out.add(stored(s));
        if (s.has(CropDiagnosis.F_TENDED)) out.add(new Line(Component.translatable(K + "tended", CropGrowthMath.TENDED_MARGIN), DspUi.VIOLET));
        if (s.has(CropDiagnosis.F_WEED)) out.add(new Line(Component.translatable(K + "weed"), DspUi.RED));
        out.add(condition(s));
    }

    private static Line stored(CropDiagnosis.Snapshot s) {
        int color = s.water() <= 0 || s.nutrients() <= 0 ? DspUi.ORANGE : DspUi.TEXT_DIM;
        return new Line(Component.translatable(K + "stored", s.water(), s.nutrients(), s.weedEx()), color);
    }

    private static Line condition(CropDiagnosis.Snapshot s) {
        if (s.has(CropDiagnosis.F_MATURE)) return new Line(Component.literal("✔ ").append(Component.translatable(K + "cond_mature")), DspUi.GREEN);
        return switch (s.blocker()) {
            case CropDiagnosis.B_NONE -> new Line(Component.literal("✔ ").append(Component.translatable(K + "cond_ok")), DspUi.GREEN);
            case CropDiagnosis.B_LIGHT_LOW -> new Line(Component.literal("✖ ").append(Component.translatable(K + "light_low", s.light(), s.blockerValue())), DspUi.RED);
            case CropDiagnosis.B_LIGHT_HIGH -> new Line(Component.literal("✖ ").append(Component.translatable(K + "light_high", s.light(), s.blockerValue())), DspUi.RED);
            case CropDiagnosis.B_ROOT -> new Line(Component.literal("✖ ").append(Component.translatable(K + "root",
                s.detail().startsWith("#") ? Component.literal(s.detail()) : Component.translatable(s.detail()))), DspUi.RED);
            case CropDiagnosis.B_WATER -> new Line(Component.literal("✖ ").append(Component.translatable(K + "water")), DspUi.RED);
            case CropDiagnosis.B_NUTRIENTS -> new Line(Component.literal("✖ ").append(Component.translatable(K + "nutrients", s.blockerValue())), DspUi.RED);
            default -> new Line(Component.literal("✖ ").append(Component.translatable(K + "other")), DspUi.RED);
        };
    }

    /** EN: Maturity 0..1: whole stages plus the growth points of the current stage. ZH: 成熟度 0..1：已完成阶段加当前阶段的生长点。JA: 成熟度 0..1：完了した段階＋現在の段階の成長ポイント。 */
    static float maturity(CropDiagnosis.Snapshot s) {
        if (s.maxStage() <= 0) return 0F;
        if (s.stage() >= s.maxStage()) return 1F;
        float part = Math.min(1F, s.progress() / (float) Math.max(1, s.growthTime()));
        return Math.min(1F, (s.stage() + part) / s.maxStage());
    }

    private static void draw(GuiGraphics g, Minecraft mc, List<Line> lines) {
        if (lines.isEmpty()) return;
        int x = mc.getWindow().getGuiScaledWidth() / 2 + 14, y = mc.getWindow().getGuiScaledHeight() / 2 + 8;
        int w = 0;
        for (Line l : lines) w = Math.max(w, mc.font.width(l.text()));
        w = Math.max(w, 120);
        int rowH = 10, h = 4;
        for (Line l : lines) h += rowH + (l.bar() >= 0 ? 3 : 0);
        // EN: Keep the panel on the screen.
        // ZH: 使面板保持在屏幕内。
        // JA: パネルを画面内に収める。
        int screenH = mc.getWindow().getGuiScaledHeight(), screenW = mc.getWindow().getGuiScaledWidth();
        if (y + h > screenH - 4) y = Math.max(4, screenH - 4 - h);
        if (x + w + 6 > screenW) x = Math.max(4, screenW - w - 6);
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        g.fill(x - 3, y - 3, x + w + 4, y + h - 1, 0xC00C1622);
        g.fill(x - 3, y - 3, x - 2, y + h - 1, DspUi.CYAN);
        g.fill(x - 3, y - 3, x + 10, y - 2, DspUi.CYAN);
        g.fill(x + w - 6, y + h - 2, x + w + 4, y + h - 1, DspUi.CYAN_DIM);
        int cy = y;
        for (Line l : lines) {
            g.drawString(mc.font, l.text(), x, cy, l.color(), true);
            cy += rowH;
            if (l.bar() >= 0) {
                DspUi.meter(g, x, cy - 1, w, 2, l.bar(), l.bar() >= 1F ? DspUi.GREEN : DspUi.CYAN);
                cy += 3;
            }
        }
        g.pose().popPose();
    }
}

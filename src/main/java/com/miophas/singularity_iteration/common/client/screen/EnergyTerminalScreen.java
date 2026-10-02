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
 * Energy Management Terminal console, in the spirit of a factory power panel:
 * KPI tiles (generation / consumption / storage), a rolling 112 s chart of
 * generation vs. consumption with the storage charge curve, and a filterable device
 * table with per-device EU/t read-outs and remote ON/OFF switches.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class EnergyTerminalScreen extends AbstractContainerScreen<EnergyTerminalMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("mio_icif:textures/gui/gui_energy_terminal.png");
    private static final int W = 240, H = 210;
    private static final int GEN = 0xFF3C9A52, USE = 0xFFD2902A, CHARGE = 0xFF3A6EA5;
    private static final int GRAPH_X = 7, GRAPH_Y = 50, GRAPH_W = 226, GRAPH_H = 64;
    private static final int LIST_X = 7, LIST_Y = 132, LIST_W = 226, ROW_H = 14, ROWS = 5;
    private static final int[] CATEGORY_COLOR = {GEN, USE, CHARGE, 0xFF7A62B0};

    private int filter = -1;     // -1 all, else Category ordinal
    private int scroll;
    private final SiButton[] filterButtons = new SiButton[4];
    private int cachedSync = -1, cachedFilter = -2;
    private final List<Device> rows = new ArrayList<>();
    private final Map<String, ItemStack> icons = new HashMap<>();

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
            filterButtons[i] = addRenderableWidget(new SiButton(leftPos + 7 + i * 42, topPos + 118, 41, 11,
                Component.translatable("gui.mio_icif.energy_terminal.filter." + keys[i]), b -> setFilter(id)));
        }
        setFilter(filter);
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
    }

    private ItemStack icon(String blockId) {
        return icons.computeIfAbsent(blockId, id -> {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            Block block = rl == null ? null : BuiltInRegistries.BLOCK.get(rl);
            return block == null ? ItemStack.EMPTY : new ItemStack(block);
        });
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
        g.blit(TEXTURE, x, y, 0, 0, W, H);
        for (int i = 0; i < 3; i++) SiGuiTheme.well(g, x + 7 + i * 76, y + 19, 74, 28);
        SiGuiTheme.well(g, x + GRAPH_X, y + GRAPH_Y, GRAPH_W, GRAPH_H);
        SiGuiTheme.well(g, x + LIST_X, y + LIST_Y - 1, LIST_W, ROWS * ROW_H + 2);
        drawChart(g, x + GRAPH_X + 1, y + GRAPH_Y + 1, GRAPH_W - 2, GRAPH_H - 2);
        // charge gauge under the storage tile value
        EnergyNetworkSnapshot s = menu.snapshot();
        SiGuiTheme.bar(g, x + 7 + 2 * 76 + 4, y + 41, 66, 3, s.chargeFraction(), SiGuiTheme.BAR_FROM, SiGuiTheme.BAR_TO);
        // row highlights and switches
        for (int r = 0; r < ROWS; r++) {
            int i = r + scroll;
            if (i >= rows.size()) break;
            Device d = rows.get(i);
            int ry = y + LIST_Y + r * ROW_H;
            if (mouseX >= x + LIST_X && mouseX < x + LIST_X + LIST_W && mouseY >= ry && mouseY < ry + ROW_H) {
                g.fill(x + LIST_X + 1, ry, x + LIST_X + LIST_W - 1, ry + ROW_H, 0x303A6EA5);
            }
            g.fill(x + LIST_X + 1, ry + 1, x + LIST_X + 3, ry + ROW_H - 1, CATEGORY_COLOR[d.category().ordinal()]);
            if (d.switchable()) drawSwitch(g, x + LIST_X + LIST_W - 26, ry + 2, !d.disabled());
        }
    }

    private void drawSwitch(GuiGraphics g, int sx, int sy, boolean on) {
        g.fill(sx, sy, sx + 22, sy + 10, SiGuiTheme.OUTLINE);
        g.fill(sx + 1, sy + 1, sx + 21, sy + 9, on ? 0xFF3C9A52 : 0xFFB0B4B9);
        int knob = on ? sx + 12 : sx + 1;
        g.fillGradient(knob, sy + 1, knob + 9, sy + 9, 0xFFFFFFFF, 0xFFC8CCD0);
        g.fill(on ? sx + 3 : sx + 13, sy + 4, on ? sx + 8 : sx + 18, sy + 6, on ? SiGuiTheme.GOOD : SiGuiTheme.BAD);
    }

    private void drawChart(GuiGraphics g, int cx, int cy, int cw, int ch) {
        EnergyNetworkSnapshot s = menu.snapshot();
        int n = EnergyNetworkSnapshot.HISTORY;
        float max = 1;
        for (int i = 0; i < n; i++) max = Math.max(max, Math.max(s.genHistory[i], s.useHistory[i]));
        max = niceCeil(max * 1.1F);
        int top = cy + 9, h = ch - 10;   // leave room for the legend line
        float step = cw / (float) n;
        int prevGen = -1, prevCharge = -1;
        for (int i = 0; i < n; i++) {
            int x0 = cx + (int) (i * step), x1 = Math.max(x0 + 1, cx + (int) ((i + 1) * step));
            int use = Math.round(h * Math.min(1, s.useHistory[i] / max));
            if (use > 0) g.fill(x0, top + h - use, x1, top + h, 0x66FFAA46);
            if (use > 0) g.fill(x0, top + h - use, x1, top + h - use + 1, USE);
            int gen = top + h - Math.round(h * Math.min(1, s.genHistory[i] / max));
            if (prevGen >= 0) g.fill(x0, Math.min(prevGen, gen), x0 + 1, Math.max(prevGen, gen) + 1, GEN);
            g.fill(x0, gen, x1, gen + 1, GEN);
            prevGen = gen;
            int charge = top + h - Math.round((h - 1) * s.chargeHistory[i]);
            if (i % 2 == 0) g.fill(x0, charge, x0 + 1, charge + 1, CHARGE);
            prevCharge = charge;
        }
        // grid line at 50 %
        for (int gx = cx; gx < cx + cw; gx += 4) g.fill(gx, top + h / 2, gx + 2, top + h / 2 + 1, 0x30000000);
        lastChartMax = max;
    }

    private float lastChartMax = 1;

    private static float niceCeil(float v) {
        double p = Math.pow(10, Math.floor(Math.log10(Math.max(1e-3, v))));
        double m = v / p;
        double nice = m <= 1 ? 1 : m <= 2 ? 2 : m <= 5 ? 5 : 10;
        return (float) (nice * p);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        EnergyNetworkSnapshot s = menu.snapshot();
        g.drawString(font, title, 8, 5, SiGuiTheme.TEXT, false);
        String info = Component.translatable("gui.mio_icif.energy_terminal.summary", s.devices.size(), s.conductors).getString()
            + (s.truncated ? " +" : "");
        small(g, info, W - 8 - (int) (font.width(info) * 0.75F), 7, SiGuiTheme.TEXT);

        tile(g, 0, "gui.mio_icif.energy_terminal.generation", rate(s.generation), GEN);
        tile(g, 1, "gui.mio_icif.energy_terminal.consumption", rate(s.consumption), USE);
        tile(g, 2, "gui.mio_icif.energy_terminal.storage", String.format("%.1f%%", s.chargeFraction() * 100), CHARGE);

        // chart legend and scale
        int ly = GRAPH_Y + 2;
        small(g, Component.translatable("gui.mio_icif.energy_terminal.chart").getString(), GRAPH_X + 3, ly, SiGuiTheme.TEXT_SCREEN_DIM);
        int lx = GRAPH_X + GRAPH_W - 4;
        lx = legend(g, lx, ly, "gui.mio_icif.energy_terminal.legend.charge", CHARGE);
        lx = legend(g, lx, ly, "gui.mio_icif.energy_terminal.legend.use", USE);
        legend(g, lx, ly, "gui.mio_icif.energy_terminal.legend.gen", GEN);
        small(g, compact(lastChartMax) + " EU/t", GRAPH_X + 3, GRAPH_Y + 10, SiGuiTheme.TEXT_SCREEN_DIM);
        float balance = s.generation - s.consumption;
        String bal = (balance >= 0 ? "+" : "") + compact(balance) + " EU/t";
        small(g, bal, GRAPH_X + GRAPH_W - 4 - (int) (font.width(bal) * 0.75F), GRAPH_Y + GRAPH_H - 8,
            balance >= 0 ? SiGuiTheme.GOOD : SiGuiTheme.BAD);

        // device table
        String count = rows.size() + "";
        small(g, count, LIST_X + LIST_W - (int) (font.width(count) * 0.75F), 121, SiGuiTheme.TEXT);
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
            String name = stack.isEmpty() ? d.blockId() : stack.getHoverName().getString();
            int nameColor = d.disabled() ? 0xFFA0A5AB : SiGuiTheme.TEXT_SCREEN;
            g.drawString(font, font.plainSubstrByWidth(name, 104), LIST_X + 19, ry + 3, nameColor, false);
            String value = switch (d.category()) {
                case GENERATOR -> "+" + compact(d.output());
                case CONSUMER -> "-" + compact(d.input());
                case STORAGE -> d.capacity() > 0 ? String.format("%.0f%%", 100.0 * d.stored() / d.capacity()) : "-";
                default -> compact(Math.max(d.input(), d.output()));
            };
            int vColor = d.disabled() ? 0xFFA0A5AB : CATEGORY_COLOR[d.category().ordinal()];
            g.drawString(font, value, LIST_X + LIST_W - 32 - font.width(value), ry + 3, vColor, false);
        }
        if (rows.isEmpty()) {
            g.drawString(font, Component.translatable(menu.snapshot().devices.isEmpty()
                ? "gui.mio_icif.energy_terminal.no_network" : "gui.mio_icif.energy_terminal.no_match"),
                LIST_X + 6, LIST_Y + 4, SiGuiTheme.TEXT_SCREEN_DIM, false);
        }
    }

    private void tile(GuiGraphics g, int index, String key, String value, int color) {
        int tx = 7 + index * 76;
        small(g, Component.translatable(key).getString(), tx + 4, 22, SiGuiTheme.TEXT_SCREEN_DIM);
        g.drawString(font, value, tx + 4, 31, color, false);
    }

    private int legend(GuiGraphics g, int rightX, int y, String key, int color) {
        String text = Component.translatable(key).getString();
        int w = (int) (font.width(text) * 0.75F);
        int x = rightX - w;
        small(g, text, x, y, color);
        return x - 8;
    }

    private void small(GuiGraphics g, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private static String rate(float euPerTick) {
        return compact(euPerTick) + " EU/t";
    }

    private static String compact(double v) {
        double a = Math.abs(v);
        if (a >= 1e9) return String.format("%.2fG", v / 1e9);
        if (a >= 1e6) return String.format("%.2fM", v / 1e6);
        if (a >= 1e4) return String.format("%.1fk", v / 1e3);
        if (a >= 100) return String.format("%.0f", v);
        return String.format("%.1f", v);
    }

    private void renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        EnergyNetworkSnapshot s = menu.snapshot();
        if (isIn(mouseX, mouseY, x + 7 + 2 * 76, y + 19, 74, 28)) {
            g.renderComponentTooltip(font, List.of(
                Component.translatable("gui.mio_icif.energy_terminal.storage"),
                Component.literal(String.format("%,d / %,d EU", s.stored, s.capacity)).withStyle(ChatFormatting.AQUA),
                Component.translatable("gui.mio_icif.energy_terminal.storage_flow", compact(s.storageIn), compact(s.storageOut))
                    .withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            return;
        }
        if (isIn(mouseX, mouseY, x + GRAPH_X, y + GRAPH_Y, GRAPH_W, GRAPH_H)) {
            int i = Mth.clamp((int) ((mouseX - x - GRAPH_X - 1) / ((GRAPH_W - 2) / (float) EnergyNetworkSnapshot.HISTORY)), 0, EnergyNetworkSnapshot.HISTORY - 1);
            int ago = EnergyNetworkSnapshot.HISTORY - 1 - i;
            g.renderComponentTooltip(font, List.of(
                Component.translatable("gui.mio_icif.energy_terminal.ago", ago),
                Component.literal(rate(s.genHistory[i])).withColor(GEN),
                Component.literal(rate(s.useHistory[i])).withColor(USE),
                Component.literal(String.format("%.1f%%", s.chargeHistory[i] * 100)).withColor(CHARGE)), mouseX, mouseY);
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
            lines.add(Component.literal(String.format("in %s / out %s EU/t", compact(d.input()), compact(d.output()))).withStyle(ChatFormatting.GRAY));
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

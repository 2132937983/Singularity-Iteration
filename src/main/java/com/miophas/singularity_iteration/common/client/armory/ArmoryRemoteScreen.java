package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.ArmoryActionPacket;
import com.miophas.singularity_iteration.common.armory.ArmoryFlight;
import com.miophas.singularity_iteration.common.armory.ArmoryLogEntry;
import com.miophas.singularity_iteration.common.armory.ArmoryPiece;
import com.miophas.singularity_iteration.common.armory.ArmoryRemoteMenu;
import com.miophas.singularity_iteration.common.armory.ArmorySnapshot;
import com.miophas.singularity_iteration.common.armory.mio_icif_armory;
import com.miophas.singularity_iteration.common.client.screen.SiGuiTheme;
import com.miophas.singularity_iteration.common.client.screen.widget.SiButton;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Armory Remote Controller console.
 * <ul>
 *   <li>Left: the six suits with their pieces and a one-tap SUMMON button each.</li>
 *   <li>Right: live panel - caller, owner, Armory energy, EU cost of the hovered suit,
 *       straight-line and flight distance to the Armory (recomputed every frame from the
 *       player's position with the same flight library the pieces use), dimension, status.</li>
 *   <li>Bottom: summon history (scroll with the wheel).</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class ArmoryRemoteScreen extends AbstractContainerScreen<ArmoryRemoteMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("mio_icif:textures/gui/gui_armory_remote.png");
    private static final int LIST_X = 7, LIST_Y = 19, ROW_H = 20;
    private static final int INFO_X = 169, INFO_Y = 19, INFO_W = 80;
    private static final int LOG_X = 7, LOG_Y = 149, LOG_ROWS = 5;
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss");

    private final SiButton[] summon = new SiButton[mio_icif_armory.SETS];
    private int hoveredSet = -1;
    private int logScroll;

    public ArmoryRemoteScreen(ArmoryRemoteMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 200;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 8;
        titleLabelY = 5;
        for (int i = 0; i < summon.length; i++) {
            int set = i;
            summon[i] = addRenderableWidget(new SiButton(leftPos + LIST_X + 124, topPos + LIST_Y + 2 + i * ROW_H + 2, 31, 13,
                Component.translatable("gui.mio_icif.armory_remote.summon"), b -> {
                    PacketDistributor.sendToServer(new ArmoryActionPacket(menu.containerId, ArmoryActionPacket.SUMMON, set, ""));
                    if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(mio_icif_sounds.UI_CLICK.get(), 1.0F, 0.6F));
                }));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ArmorySnapshot s = menu.snapshot();
        boolean ok = s != null && s.status() == ArmorySnapshot.OK && !s.busy();
        for (int i = 0; i < summon.length; i++) summon[i].active = ok && s.pieces(i) > 0;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hoveredSet = -1;
        for (int i = 0; i < mio_icif_armory.SETS; i++) {
            if (isHovering(LIST_X, LIST_Y + 1 + i * ROW_H, 158, ROW_H, mouseX, mouseY)) hoveredSet = i;
        }
        super.render(g, mouseX, mouseY, partialTick);
        renderItemTooltips(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        ArmorySnapshot s = menu.snapshot();
        if (hoveredSet >= 0) {
            int ry = y + LIST_Y + 1 + hoveredSet * ROW_H;
            g.fill(x + LIST_X + 1, ry, x + LIST_X + 157, ry + ROW_H, 0x303A6EA5);
        }
        for (int i = 1; i < mio_icif_armory.SETS; i++) {
            g.fill(x + LIST_X + 2, y + LIST_Y + 1 + i * ROW_H, x + LIST_X + 156, y + LIST_Y + 2 + i * ROW_H, 0xFFDADDE0);
        }
        if (s == null) return;
        for (int set = 0; set < Math.min(s.suits().size(), mio_icif_armory.SETS); set++) {
            List<ItemStack> row = s.suits().get(set);
            for (int c = 0; c < row.size(); c++) {
                int ix = x + LIST_X + 3 + c * 19, iy = y + LIST_Y + 1 + set * ROW_H + 8;
                ItemStack stack = row.get(c);
                g.pose().pushPose();
                g.pose().translate(ix, iy, 0);
                g.pose().scale(0.75F, 0.75F, 1);
                if (stack.isEmpty()) g.fill(1, 1, 15, 15, 0x22000000);
                else g.renderItem(stack, 0, 0);
                g.pose().popPose();
            }
        }
        // energy gauge in the info panel
        double fill = s.capacity() > 0 ? (double) s.energy() / s.capacity() : 0;
        SiGuiTheme.bar(g, x + INFO_X + 4, y + INFO_Y + 3 + 39, INFO_W - 8, 3, fill, SiGuiTheme.BAR_FROM, SiGuiTheme.BAR_TO);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, SiGuiTheme.TEXT, false);
        ArmorySnapshot s = menu.snapshot();
        if (s == null) {
            small(g, Component.translatable("gui.mio_icif.armory_remote.connecting").getString(), LIST_X + 4, LIST_Y + 4, SiGuiTheme.TEXT_SCREEN_DIM);
            return;
        }
        // status line in the title bar
        String status = switch (s.status()) {
            case ArmorySnapshot.OK -> s.busy() ? "gui.mio_icif.armory_remote.status.busy" : "gui.mio_icif.armory_remote.status.ready";
            case ArmorySnapshot.NOT_OWNER -> "gui.mio_icif.armory_remote.status.not_owner";
            case ArmorySnapshot.UNBOUND -> "gui.mio_icif.armory_remote.status.unbound";
            default -> "gui.mio_icif.armory_remote.status.missing";
        };
        int led = s.status() != ArmorySnapshot.OK ? SiGuiTheme.BAD : s.busy() ? SiGuiTheme.WARN : SiGuiTheme.GOOD;
        Component statusText = Component.translatable(status);
        int sw = (int) (font.width(statusText) * 0.75F);
        small(g, statusText.getString(), imageWidth - 10 - sw, 6, SiGuiTheme.TEXT_SCREEN);
        SiGuiTheme.led(g, imageWidth - 18 - sw, 5, led);

        // suit list
        for (int set = 0; set < s.names().size(); set++) {
            int ry = LIST_Y + 1 + set * ROW_H;
            small(g, font.plainSubstrByWidth(s.names().get(set), 135), LIST_X + 4, ry + 1, SiGuiTheme.TEXT_SCREEN);
            small(g, s.pieces(set) + "/6", LIST_X + 106, ry + 1, SiGuiTheme.TEXT_SCREEN_DIM);
        }

        // live info panel
        var player = minecraft != null ? minecraft.player : null;
        boolean sameDim = player != null && player.level().dimension() == s.armory().dimension();
        Vec3 armoryTop = Vec3.atCenterOf(s.armory().pos()).add(0, 0.7, 0);
        double straight = sameDim ? player.position().distanceTo(Vec3.atCenterOf(s.armory().pos())) : -1;
        double flight = 0;
        if (player != null) {
            Vec3 dock = ArmoryFlight.dockPoint(player, 1.0F, ArmoryPiece.CHEST);
            flight = ArmoryFlight.length(ArmoryFlight.launchPoint(dock, armoryTop, sameDim, ArmoryPiece.CHEST), dock);
        }
        int previewSet = hoveredSet >= 0 ? hoveredSet : 0;
        long cost = mio_icif_armory.cost(s.pieces(previewSet), straight, sameDim);
        int ix = INFO_X + 4, iy = INFO_Y + 3;
        info(g, ix, iy, "gui.mio_icif.armory_remote.caller", player != null ? player.getGameProfile().getName() : "-");
        info(g, ix, iy + 13, "gui.mio_icif.armory_remote.owner", s.owner().isEmpty() ? "-" : s.owner());
        info(g, ix, iy + 26, "gui.mio_icif.armory_remote.energy", String.format("%,d EU", s.energy()));
        info(g, ix, iy + 45, "gui.mio_icif.armory_remote.cost", String.format("%,d EU", cost));
        small(g, "(" + (previewSet < s.names().size() ? font.plainSubstrByWidth(s.names().get(previewSet), 90) : "") + ")", ix, iy + 57, SiGuiTheme.TEXT_SCREEN_DIM);
        info(g, ix, iy + 66, "gui.mio_icif.armory_remote.straight", straight < 0 ? "--" : String.format("%.1f m", straight));
        info(g, ix, iy + 79, "gui.mio_icif.armory_remote.flight", String.format("%.1f m", flight));
        var at = s.armory().pos();
        small(g, Component.translatable("gui.mio_icif.armory_remote.dimension").getString(), ix, iy + 91, SiGuiTheme.TEXT_SCREEN_DIM);
        small(g, font.plainSubstrByWidth(tinyDim(s.armory().dimension().location().getPath()) + "  " + at.getX() + " " + at.getY() + " " + at.getZ(),
            (int) ((INFO_W - 8) / 0.75F)), ix, iy + 97, SiGuiTheme.TEXT_SCREEN);
        String maint = switch (s.maintenanceState()) {
            case com.miophas.singularity_iteration.common.armory.ArmoryMaintenance.WORKING ->
                Component.translatable("gui.mio_icif.armory.maint.eta", ArmoryScreen.formatTicks(s.etaTicks())).getString();
            case com.miophas.singularity_iteration.common.armory.ArmoryMaintenance.NEED_POWER ->
                Component.translatable("gui.mio_icif.armory.maint.power").getString();
            case com.miophas.singularity_iteration.common.armory.ArmoryMaintenance.NEED_MANA ->
                Component.translatable("gui.mio_icif.armory.maint.mana").getString();
            default -> Component.translatable("gui.mio_icif.armory.maint.ready").getString();
        };
        int maintColor = s.maintenanceState() == com.miophas.singularity_iteration.common.armory.ArmoryMaintenance.IDLE ? SiGuiTheme.GOOD
            : s.maintenanceState() == com.miophas.singularity_iteration.common.armory.ArmoryMaintenance.WORKING ? SiGuiTheme.ACCENT : SiGuiTheme.WARN;
        small(g, Component.translatable("gui.mio_icif.armory.maint.title").getString(), ix, iy + 104, SiGuiTheme.TEXT_SCREEN_DIM);
        small(g, font.plainSubstrByWidth(maint, (int) ((INFO_W - 8) / 0.75F)), ix, iy + 110, maintColor);

        // history (the latest server reply, if any, is pinned on top)
        List<ArmoryLogEntry> log = s.log();
        Component msg = menu.lastMessage();
        int first = 0;
        if (!msg.getString().isEmpty()) {
            small(g, font.plainSubstrByWidth("\u25B6 " + msg.getString(), 318), LOG_X + 4, LOG_Y + 3, SiGuiTheme.ACCENT);
            first = 1;
        }
        int rows = LOG_ROWS - first;
        logScroll = Mth.clamp(logScroll, 0, Math.max(0, log.size() - rows));
        if (log.isEmpty()) {
            small(g, Component.translatable("gui.mio_icif.armory_remote.log.empty").getString(), LOG_X + 4, LOG_Y + 3 + first * 8, SiGuiTheme.TEXT_SCREEN_DIM);
        }
        for (int i = 0; i < rows && i + logScroll < log.size(); i++) {
            ArmoryLogEntry e = log.get(i + logScroll);
            String dist = e.straight() < 0 ? Component.translatable("gui.mio_icif.armory_remote.log.cross").getString()
                : String.format("%.1fm/%.1fm", e.straight(), e.flight());
            String line = String.format("%s  %s > %s  %d pcs  %,d EU  %s", TIME.format(new Date(e.epochMillis())),
                e.caller(), e.suit(), e.pieces(), e.cost(), dist);
            small(g, font.plainSubstrByWidth(line, 318), LOG_X + 4, LOG_Y + 3 + (i + first) * 8,
                i + logScroll == 0 ? SiGuiTheme.TEXT_SCREEN : SiGuiTheme.TEXT_SCREEN_DIM);
        }
    }

    /** Compact dimension tag for the one-line location read-out. */
    private static String tinyDim(String path) {
        return switch (path) {
            case "overworld" -> "OW";
            case "the_nether" -> "Nether";
            case "the_end" -> "End";
            default -> path.length() > 8 ? path.substring(0, 8) : path;
        };
    }

    private static String shortDim(String path) {
        return path.length() > 14 ? path.substring(0, 14) : path;
    }

    private void info(GuiGraphics g, int x, int y, String key, String value) {
        small(g, Component.translatable(key).getString(), x, y, SiGuiTheme.TEXT_SCREEN_DIM);
        small(g, value, x, y + 6, SiGuiTheme.TEXT_SCREEN);
    }

    private void small(GuiGraphics g, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private void renderItemTooltips(GuiGraphics g, int mouseX, int mouseY) {
        ArmorySnapshot s = menu.snapshot();
        if (s == null) return;
        for (int set = 0; set < s.suits().size(); set++) {
            for (int c = 0; c < mio_icif_armory.PIECES; c++) {
                if (isHovering(LIST_X + 3 + c * 19, LIST_Y + 1 + set * ROW_H + 8, 12, 12, mouseX, mouseY)) {
                    ItemStack stack = s.suits().get(set).get(c);
                    if (!stack.isEmpty()) g.renderTooltip(font, stack, mouseX, mouseY);
                    return;
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isHovering(LOG_X, LOG_Y, 242, 44, mouseX, mouseY)) {
            logScroll -= (int) Math.signum(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}

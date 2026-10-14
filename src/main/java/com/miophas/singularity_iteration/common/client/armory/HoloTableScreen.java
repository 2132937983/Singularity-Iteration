// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.WingmanCommandPackets;
import com.miophas.singularity_iteration.common.armory.WingmanCommandPackets.Unit;
import com.miophas.singularity_iteration.common.armory.WingmanCommands;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.client.suit.TacticalMapClient;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Controls of the 3D holo table (0.1.10.2). The table itself is drawn in the world by
 * {@link HoloTable}; this screen frees the mouse and gives the orders.
 *
 * <ul>
 *   <li>Left drag: turn the table. Mouse wheel: 24 / 48 / 96 blocks.</li>
 *   <li>Left click on the terrain: MOVE the selection there. Right click: area SUPPRESS there.
 *       Left click on a hostile creature: FOCUS fire on it.</li>
 *   <li>H: HOLD where they are. F: FORM UP. 0..9: select all / one Wingman (also the chips).</li>
 *   <li>Esc, the tactical map key or the command wheel key: fold the table away.</li>
 * </ul>
 */
public final class HoloTableScreen extends Screen {
    private double pressX, pressY;
    private boolean dragging;
    private final List<float[]> chips = new ArrayList<>();
    private final List<UUID> chipIds = new ArrayList<>();
    private int ackAt = -100;
    private String ack = "";

    public HoloTableScreen() {
        super(Component.translatable("gui.mio_icif.holo_table.title"));
    }

    @Override
    protected void init() {
        HoloTable.begin();
    }

    @Override
    public void removed() {
        HoloTable.close(false);
        HoloTable.hoverCell = -1;
        HoloTable.hoverEntity = -1;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) { }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        LivingEntity hostile = HoloTable.pickEntity(mouseX, mouseY);
        HoloTable.hoverEntity = hostile == null ? -1 : hostile.getId();
        HoloTable.hoverCell = hostile == null && !dragging ? HoloTable.pickCell(mouseX, mouseY) : -1;
        int w = width, h = height;
        // header and footer bars
        FcsDraw.panel(g, w / 2 - 110, 8, 220, 22, Component.translatable("gui.mio_icif.holo_table.title").getString(), 0x4DE1C1);
        FcsDraw.textCentered(g, String.format(Locale.ROOT, "R %dm  ·  %s", HoloTable.radius,
            Component.translatable("gui.mio_icif.holo_table.rotate").getString()), w / 2F, 18, FcsDraw.argb(0xBFFFF2, 0.9F), 0.5F);
        chips(g, mouseX, mouseY);
        String hint = Component.translatable("gui.mio_icif.holo_table.hint").getString();
        FcsDraw.textCentered(g, hint, w / 2F, h - 14, FcsDraw.argb(0x9FDFFF, 0.75F), 0.5F);
        // hover read-out next to the mouse
        String line = null;
        int col = 0x5CEBFF;
        if (hostile != null) {
            line = Component.translatable("gui.mio_icif.holo_table.focus", hostile.getDisplayName()).getString();
            col = 0xFF4D6D;
        } else if (HoloTable.hoverCell >= 0) {
            Vec3 p = HoloTable.columnTop(HoloTable.hoverCell);
            double d = minecraft != null && minecraft.player != null ? p.distanceTo(minecraft.player.position()) : 0;
            line = String.format(Locale.ROOT, "%s  %d %d %d  (%.0fm)", Component.translatable("gui.mio_icif.holo_table.point").getString(),
                (int) Math.floor(p.x), (int) p.y, (int) Math.floor(p.z), d);
        }
        if (line != null) {
            float tw = FcsDraw.width(line, 0.55F);
            g.fill(mouseX + 8, mouseY - 12, (int) (mouseX + 14 + tw), mouseY - 2, 0x90000000);
            FcsDraw.text(g, line, mouseX + 11, mouseY - 10, FcsDraw.argb(col, 1F), 0.55F);
        }
        if (SquadClient.ticks - ackAt < 30) FcsDraw.textCentered(g, ack, w / 2F, h - 26, FcsDraw.argb(0xFFE08A, 1F), 0.6F);
    }

    private void chips(GuiGraphics g, int mouseX, int mouseY) {
        chips.clear();
        chipIds.clear();
        List<Unit> units = SquadClient.orderable();
        float w = 34, hh = 14, gap = 4;
        int n = units.size() + 1;
        float x = width / 2F - (n * w + (n - 1) * gap) / 2, y = 34;
        UUID sel = SquadClient.selected();
        for (int i = 0; i < n; i++) {
            UUID id = i == 0 ? WingmanCommandPackets.ALL : units.get(i - 1).id();
            boolean selected = id.equals(sel);
            int col = i == 0 ? 0x5CEBFF : CommandWheelScreen.modeColor(units.get(i - 1).mode());
            g.fill((int) x, (int) y, (int) (x + w), (int) (y + hh), FcsDraw.argb(col, selected ? 0.4F : 0.12F));
            FcsDraw.line(g, x, y, x + w, y, selected ? 1.5F : 0.8F, FcsDraw.argb(col, 0.9F));
            String name = i == 0 ? Component.translatable("gui.mio_icif.command.chip_all").getString() : SquadClient.label(units.get(i - 1));
            FcsDraw.textCentered(g, (i == 0 ? "0 " : i + " ") + name, x + w / 2, y + 4, FcsDraw.argb(0xEFFFFF, 1F), 0.5F);
            chips.add(new float[]{x, y, w, hh});
            chipIds.add(id);
            x += w + gap;
        }
    }

    private void order(byte action, Vec3 point, int target) {
        if (SquadClient.orderable().isEmpty()) return;
        SquadClient.send(action, SquadClient.selected(), action == WingmanCommands.A_HOLD ? (byte) 1 : 0, point, target);
        ack = Component.translatable("gui.mio_icif.command.action." + action).getString();
        ackAt = SquadClient.ticks;
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(mio_icif_sounds.WHEEL_SELECT.get(), 1.2F, 0.5F));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < chips.size(); i++) {
            float[] c = chips.get(i);
            if (mouseX >= c[0] && mouseX < c[0] + c[2] && mouseY >= c[1] && mouseY < c[1] + c[3]) {
                SquadClient.select(chipIds.get(i));
                return true;
            }
        }
        pressX = mouseX;
        pressY = mouseY;
        dragging = false;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && (dragging || Math.abs(mouseX - pressX) + Math.abs(mouseY - pressY) > 4)) {
            dragging = true;
            HoloTable.rot += (float) (dragX * 0.012);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) { dragging = false; return true; }
        LivingEntity hostile = HoloTable.pickEntity(mouseX, mouseY);
        if (button == 0 && hostile != null) {
            order(WingmanCommands.A_FOCUS, null, hostile.getId());
            return true;
        }
        int cell = HoloTable.pickCell(mouseX, mouseY);
        if (cell >= 0) {
            Vec3 p = HoloTable.columnTop(cell);
            if (button == 0) order(WingmanCommands.A_MOVE, p, -1);
            else if (button == 1) order(WingmanCommands.A_SUPPRESS, p, -1);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        HoloTable.zoom(scrollY > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_H) { order(WingmanCommands.A_HOLD, null, -1); return true; }
        if (keyCode == GLFW.GLFW_KEY_F) { order(WingmanCommands.A_FORM_UP, null, -1); return true; }
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            int i = keyCode - GLFW.GLFW_KEY_0;
            List<Unit> units = SquadClient.orderable();
            SquadClient.select(i == 0 || i > units.size() ? WingmanCommandPackets.ALL : units.get(i - 1).id());
            return true;
        }
        if (TacticalMapClient.KEY != null && TacticalMapClient.KEY.matches(keyCode, scanCode)
                || CommandWheelClient.KEY != null && CommandWheelClient.KEY.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

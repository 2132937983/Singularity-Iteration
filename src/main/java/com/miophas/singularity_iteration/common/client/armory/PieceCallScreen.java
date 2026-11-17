// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.ArmoryConsolePacket;
import com.miophas.singularity_iteration.common.armory.ArmoryPiece;
import com.miophas.singularity_iteration.common.armory.ArmoryRemoteService;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 0.1.11.57 (gaze and Iron Man phase P5): the piece wheel. Six sectors (helmet at the top, then clockwise chestplate,
 * main hand, boots, leggings... in body order), each with the item worn there now; the mouse direction picks one, the
 * release of the piece key (or a click) calls it ({@code ArmoryRemoteService#PIECE_CALL}); Escape or a release in the
 * middle cancels. The game does not pause.
 */
public final class PieceCallScreen extends Screen {
    /** Clockwise from the top. */
    static final ArmoryPiece[] ORDER = {ArmoryPiece.HEAD, ArmoryPiece.MAINHAND, ArmoryPiece.CHEST, ArmoryPiece.FEET, ArmoryPiece.LEGS,
        ArmoryPiece.OFFHAND};
    private static final float R_IN = 26, R_OUT = 74;
    private static final int COLOR = 0x6FE3FF;
    private int hover = -1;
    private boolean done;

    public PieceCallScreen() { super(Component.translatable("gui.mio_icif.piece_call.title")); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float cx = width / 2F, cy = height / 2F;
        float dx = mouseX - cx, dy = mouseY - cy;
        hover = dx * dx + dy * dy < (R_IN - 6) * (R_IN - 6) ? -1 : sector(dx, dy);
        float step = Mth.TWO_PI / ORDER.length;
        FcsDraw.disc(g, cx, cy, R_IN - 4, FcsDraw.argb(0x01060C, 0.75F));
        for (int i = 0; i < ORDER.length; i++) {
            float a0 = i * step - step / 2 + 0.03F, a1 = (i + 1) * step - step / 2 - 0.03F;
            boolean hot = i == hover;
            // a dark base first: the labels stay readable over the sky
            FcsDraw.sector(g, cx, cy, R_IN, R_OUT + (hot ? 5 : 0), a0, a1, FcsDraw.argb(0x01060C, 0.62F));
            FcsDraw.sector(g, cx, cy, R_IN, R_OUT + (hot ? 5 : 0), a0, a1, FcsDraw.argb(COLOR, hot ? 0.40F : 0.10F));
            FcsDraw.arc(g, cx, cy, R_OUT + (hot ? 5 : 0), a0, a1, hot ? 2F : 1F, FcsDraw.argb(COLOR, hot ? 1F : 0.55F));
            float am = i * step, rm = (R_IN + R_OUT) / 2;
            float x = cx + Mth.sin(am) * rm, y = cy - Mth.cos(am) * rm;
            ItemStack worn = Minecraft.getInstance().player == null ? ItemStack.EMPTY : Minecraft.getInstance().player.getItemBySlot(ORDER[i].slot);
            if (!worn.isEmpty()) g.renderItem(worn, (int) x - 8, (int) y - 14);
            String label = Component.translatable("gui.mio_icif.armory.piece." + ORDER[i].name().toLowerCase(Locale.ROOT)).getString();
            FcsDraw.textCentered(g, label, x, y + 4, FcsDraw.argb(hot ? 0xFFFFFF : COLOR, 1F), 0.6F);
        }
        String centre = hover < 0 ? Component.translatable("gui.mio_icif.command.cancel").getString()
            : Component.translatable("gui.mio_icif.piece_call.call").getString();
        FcsDraw.textCentered(g, centre, cx, cy - 3, FcsDraw.argb(0xDFFBFF, 0.9F), 0.5F);
    }

    /** The sector index of a direction from the centre (screen y down; 0 at the top, clockwise). */
    static int sector(float dx, float dy) {
        double a = Math.atan2(dx, -dy);
        if (a < 0) a += Math.PI * 2;
        double step = Math.PI * 2 / ORDER.length;
        return (int) Math.floor((a + step / 2) / step) % ORDER.length;
    }

    private void run() {
        if (done) return;
        done = true;
        if (hover >= 0) PacketDistributor.sendToServer(new ArmoryConsolePacket(ArmoryRemoteService.PIECE_CALL, ORDER[hover].ordinal()));
        onClose();
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (PieceCallClient.KEY != null && PieceCallClient.KEY.matches(keyCode, scanCode)) {
            run();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        run();
        return true;
    }
}

package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.ArmoryActionPacket;
import com.miophas.singularity_iteration.common.armory.ArmoryMenu;
import com.miophas.singularity_iteration.common.armory.mio_icif_armory;
import com.miophas.singularity_iteration.common.client.screen.SiGuiTheme;
import com.miophas.singularity_iteration.common.client.screen.mio_icif_screen;
import com.miophas.singularity_iteration.common.client.screen.widget.SiButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Armory console: six suit rows (click a name to rename it), battery, energy gauge,
 * owner line and an unbind button. Drawn in the shared Modern-IC theme.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class ArmoryScreen extends mio_icif_screen<ArmoryMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("mio_icif:textures/gui/gui_armory.png");
    private static final int NAME_X = 7, NAME_W = 56;
    private static final int BAR_X = 30, BAR_Y = 130, BAR_W = 138, BAR_H = 4;
    private static final int MANA_Y = 135, MANA_H = 2;
    private static final int STATUS_RIGHT = 157;

    private SiButton automationButton;

    private EditBox renameBox;
    private int renaming = -1;

    public ArmoryScreen(ArmoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = ArmoryMenu.GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 8;
        titleLabelY = 5;
        inventoryLabelY = 139;
        renameBox = new EditBox(font, leftPos + NAME_X + 2, topPos, NAME_W - 4, 10, Component.empty());
        renameBox.setMaxLength(mio_icif_armory.NAME_LENGTH);
        renameBox.setTextColor(0x2A2E33);
        renameBox.setBordered(false);
        renameBox.visible = false;
        addRenderableWidget(renameBox);
        SiButton unbind = addRenderableWidget(new SiButton(leftPos + 128, topPos + 3, 41, 11,
            Component.translatable("gui.mio_icif.armory.unbind"), b -> {
                if (hasShiftDown()) send(ArmoryActionPacket.UNBIND, 0, "");
            }));
        unbind.setTooltip(Tooltip.create(Component.translatable("gui.mio_icif.armory.unbind.tip")));
        automationButton = addRenderableWidget(new SiButton(leftPos + 70, topPos + 3, 56, 11, automationLabel(), b -> {
            send(ArmoryActionPacket.AUTOMATION, 0, "");
            if (minecraft != null) minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                com.miophas.singularity_iteration.common.registry.mio_icif_sounds.UI_TOGGLE.get(), 1.0F, 0.8F));
        }));
    }

    private Component automationLabel() {
        return Component.translatable("gui.mio_icif.armory.automation." + menu.automation().name().toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (automationButton != null) {
            automationButton.setMessage(automationLabel());
            automationButton.setTooltip(Tooltip.create(Component.translatable("gui.mio_icif.armory.automation."
                + menu.automation().name().toLowerCase(java.util.Locale.ROOT) + ".tip")));
        }
    }

    private boolean showMana() {
        return net.neoforged.fml.ModList.get().isLoaded("botania") || menu.mana() > 0;
    }

    /** Short maintenance read-out: ready / time left / what it is waiting for. */
    private Component maintenanceText() {
        return switch (menu.maintenanceState()) {
            case com.miophas.singularity_iteration.common.armory.ArmoryMaintenance.WORKING ->
                Component.translatable("gui.mio_icif.armory.maint.eta", formatTicks(menu.etaTicks()));
            case com.miophas.singularity_iteration.common.armory.ArmoryMaintenance.NEED_POWER ->
                Component.translatable("gui.mio_icif.armory.maint.power");
            case com.miophas.singularity_iteration.common.armory.ArmoryMaintenance.NEED_MANA ->
                Component.translatable("gui.mio_icif.armory.maint.mana");
            default -> Component.translatable("gui.mio_icif.armory.maint.ready");
        };
    }

    private int maintenanceColor() {
        return switch (menu.maintenanceState()) {
            case com.miophas.singularity_iteration.common.armory.ArmoryMaintenance.WORKING -> SiGuiTheme.ACCENT;
            case com.miophas.singularity_iteration.common.armory.ArmoryMaintenance.NEED_POWER,
                 com.miophas.singularity_iteration.common.armory.ArmoryMaintenance.NEED_MANA -> SiGuiTheme.WARN;
            default -> SiGuiTheme.GOOD;
        };
    }

    public static String formatTicks(int ticks) {
        if (ticks < 0) return "--:--";
        int seconds = (ticks + 19) / 20;
        return seconds >= 3600 ? String.format("%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
            : String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private void send(int action, int index, String text) {
        PacketDistributor.sendToServer(new ArmoryActionPacket(menu.containerId, action, index, text));
    }

    private mio_icif_armory armory() {
        if (minecraft == null || minecraft.level == null || menu.pos() == null) return null;
        return minecraft.level.getBlockEntity(menu.pos()) instanceof mio_icif_armory a ? a : null;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        double fill = (double) menu.energy() / mio_icif_armory.CAPACITY;
        SiGuiTheme.bar(g, x + BAR_X, y + BAR_Y, BAR_W, BAR_H, fill, SiGuiTheme.BAR_FROM, SiGuiTheme.BAR_TO);
        if (showMana()) {
            double manaFill = (double) menu.mana() / mio_icif_armory.MANA_CAPACITY;
            SiGuiTheme.bar(g, x + BAR_X, y + MANA_Y, BAR_W, MANA_H, manaFill, 0xFF7FD0F5, 0xFF2E8FD8);
        }
        // amber marker on suits that still need repair or charge
        for (int r = 0; r < mio_icif_armory.SETS; r++) {
            if (suitNeedsService(r)) g.fill(x + NAME_X + NAME_W - 6, y + 18 + r * 18 + 12, x + NAME_X + NAME_W - 3, y + 18 + r * 18 + 15, SiGuiTheme.WARN);
        }
        for (int r = 0; r < mio_icif_armory.SETS; r++) {
            int ry = y + 18 + r * 18;
            if (r != renaming && mouseX >= x + NAME_X && mouseX < x + NAME_X + NAME_W && mouseY >= ry && mouseY < ry + 18) {
                g.fill(x + NAME_X + 1, ry + 1, x + NAME_X + NAME_W - 1, ry + 17, 0x303A6EA5);
            }
        }
    }

    /** The Armory shows its own bind/energy lamp beside the owner line. */
    @Override
    protected int statusLampColor() { return 0; }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, SiGuiTheme.TEXT, false);
        mio_icif_armory armory = armory();
        for (int r = 0; r < mio_icif_armory.SETS; r++) {
            if (r == renaming) continue;
            String name = armory != null ? armory.setName(r) : mio_icif_armory.defaultName(r);
            small(g, font.plainSubstrByWidth(name, (int) ((NAME_W - 6) / 0.75F)), NAME_X + 3, 18 + r * 18 + 4, SiGuiTheme.TEXT_SCREEN);
            if (armory != null) small(g, armory.pieceCount(r) + "/6", NAME_X + 3, 18 + r * 18 + 11, SiGuiTheme.TEXT_SCREEN_DIM);
        }
        String owner = armory != null && !armory.ownerName().isEmpty() ? armory.ownerName() : "-";
        int state = menu.busy() ? SiGuiTheme.WARN : menu.bound() && menu.energy() >= mio_icif_armory.BIND_ENERGY ? SiGuiTheme.GOOD : SiGuiTheme.LED_OFF;
        SiGuiTheme.led(g, 161, 138, state);
        small(g, Component.translatable("gui.mio_icif.armory.owner", owner).getString(), BAR_X, 138, SiGuiTheme.TEXT_SCREEN);
        String status = maintenanceText().getString();
        small(g, status, STATUS_RIGHT - (int) (font.width(status) * 0.75F), 138, maintenanceColor());
    }

    private boolean suitNeedsService(int set) {
        for (int col = 0; col < mio_icif_armory.PIECES; col++) {
            var stack = menu.getSlot(1 + set * mio_icif_armory.PIECES + col).getItem();
            if (stack.isEmpty()) continue;
            if (stack.isDamageableItem() && stack.isDamaged()) return true;
            if (com.miophas.singularity_iteration.common.armory.ArmoryItemEnergy.room(stack) > 0) return true;
        }
        return false;
    }

    private void small(GuiGraphics g, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    /**
     * Hover test in GUI-relative coordinates. Do not call isHovering(int...) here: the base screen
     * has an int overload with the mouse position first, and int arguments bind to it (0.1.7.35:
     * the maintenance tooltip covered most of the screen and the other tooltips never showed).
     */
    private boolean over(int x, int y, int w, int h, double mouseX, double mouseY) {
        double mx = mouseX - leftPos, my = mouseY - topPos;
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    protected void renderCustomTooltips(GuiGraphics g, int mouseX, int mouseY) {
        if (over(BAR_X, BAR_Y - 1, BAR_W, BAR_H + 2, mouseX, mouseY)) {
            g.renderTooltip(font, Component.translatable("gui.mio_icif.armory.energy",
                String.format("%,d", menu.energy()), String.format("%,d", mio_icif_armory.CAPACITY)), mouseX, mouseY);
            return;
        }
        if (showMana() && over(BAR_X, MANA_Y - 1, BAR_W, MANA_H + 2, mouseX, mouseY)) {
            g.renderTooltip(font, Component.translatable("gui.mio_icif.armory.mana",
                String.format("%,d", menu.mana()), String.format("%,d", mio_icif_armory.MANA_CAPACITY)), mouseX, mouseY);
            return;
        }
        if (over(BAR_X + 60, 137, STATUS_RIGHT - BAR_X - 60, 8, mouseX, mouseY)) {
            java.util.List<Component> lines = new java.util.ArrayList<>();
            lines.add(Component.translatable("gui.mio_icif.armory.maint.title").withStyle(ChatFormatting.WHITE));
            lines.add(Component.translatable("gui.mio_icif.armory.maint.pieces", menu.pendingPieces()).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.mio_icif.armory.maint.eu", String.format("%,d", menu.euNeeded())).withStyle(ChatFormatting.GRAY));
            if (showMana()) lines.add(Component.translatable("gui.mio_icif.armory.maint.mana_need",
                String.format("%,d", menu.manaNeeded())).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.mio_icif.armory.maint.time", formatTicks(menu.etaTicks())).withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable("gui.mio_icif.armory.maint.reserve",
                String.format("%,d", com.miophas.singularity_iteration.common.armory.ArmoryMaintenance.SUMMON_RESERVE)).withStyle(ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        for (int r = 0; r < mio_icif_armory.SETS; r++) {
            if (r != renaming && over(NAME_X, 18 + r * 18, NAME_W, 18, mouseX, mouseY)) {
                g.renderTooltip(font, Component.translatable("gui.mio_icif.armory.rename").withStyle(ChatFormatting.GRAY), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int r = 0; r < mio_icif_armory.SETS; r++) {
            if (over(NAME_X, 18 + r * 18, NAME_W, 18, mouseX, mouseY)) {
                startRename(r);
                return true;
            }
        }
        if (renaming >= 0 && !renameBox.isMouseOver(mouseX, mouseY)) commitRename();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void startRename(int row) {
        if (renaming >= 0) commitRename();
        renaming = row;
        mio_icif_armory armory = armory();
        renameBox.setValue(armory != null ? armory.setName(row) : mio_icif_armory.defaultName(row));
        renameBox.setY(topPos + 18 + row * 18 + 5);
        renameBox.visible = true;
        setFocused(renameBox);
        renameBox.setFocused(true);
    }

    private void commitRename() {
        if (renaming < 0) return;
        send(ArmoryActionPacket.RENAME, renaming, renameBox.getValue());
        renaming = -1;
        renameBox.visible = false;
        renameBox.setFocused(false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (renaming >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { commitRename(); return true; }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { renaming = -1; renameBox.visible = false; return true; }
            return renameBox.keyPressed(keyCode, scanCode, modifiers) || true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        commitRename();
        super.removed();
    }
}

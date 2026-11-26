package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_replicator_elc;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 复制机方块的 GUI 类
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_replicator_elc extends mio_icif_screen<com.miophas.singularity_iteration.common.menu.producer.ReplicatorElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_replicator_elc.png");

    // GUI 尺寸 (背景纹理 176x184)
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 184;

    // 流体槽位置 (27,30), 使用基类标准流体槽大小
    private static final int FLUID_TANK_X = 27;
    private static final int FLUID_TANK_Y = 30;

    // 闪电标志位置 (131,83) - 对齐原版IC2
    private static final int LIGHTNING_X = 131;
    private static final int LIGHTNING_Y = 83;

    // 按钮位置 (75,82), (92,82), (109,82)
    private static final int STOP_BTN_X = 75;
    private static final int STOP_BTN_Y = 82;
    private static final int SINGLE_BTN_X = 92;
    private static final int SINGLE_BTN_Y = 82;
    private static final int LOOP_BTN_X = 109;
    private static final int LOOP_BTN_Y = 82;
    private static final int BTN_WIDTH = 16;
    private static final int BTN_HEIGHT = 16;

    public mio_icif_gui_replicator_elc(com.miophas.singularity_iteration.common.menu.producer.ReplicatorElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 绘制背景
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        com.miophas.singularity_iteration.common.menu.producer.ReplicatorElcMenu menu = this.menu;
        if (menu != null) {
            // 使用基类标准方法绘制流体槽
            FluidStack fluid = new FluidStack(
                com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids.UUMATTER.get(),
                menu.getFluidAmount()
            );
            drawFluidTank(guiGraphics, x + FLUID_TANK_X, y + FLUID_TANK_Y,
                fluid, menu.getFluidAmount(), menu.getFluidCapacity());

            // 使用基类标准方法绘制闪电能量标志
            drawLightningEnergy(guiGraphics, x + LIGHTNING_X, y + LIGHTNING_Y, menu.getEnergy(), menu.getMaxEnergy());

            // 绘制三个按钮（始终显示在固定位置）
            // 停止按钮 (纹理 176,112) - 位置 (75,82)
            guiGraphics.blit(ATLAS_TEXTURE, x + STOP_BTN_X, y + STOP_BTN_Y, 0, (float) 76, (float) 243, BTN_WIDTH, BTN_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
            // 单次按钮 (纹理 176,124) - 位置 (92,82)
            guiGraphics.blit(ATLAS_TEXTURE, x + SINGLE_BTN_X, y + SINGLE_BTN_Y, 0, (float) 94, (float) 243, BTN_WIDTH, BTN_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
            // 循环按钮 (纹理 176,136) - 位置 (109,82)
            guiGraphics.blit(ATLAS_TEXTURE, x + LOOP_BTN_X, y + LOOP_BTN_Y, 0, (float) 112, (float) 243, BTN_WIDTH, BTN_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);

            // 绘制按钮高光（当鼠标悬停时）
            // 停止按钮高光
            if (mouseX >= x + STOP_BTN_X && mouseX <= x + STOP_BTN_X + BTN_WIDTH &&
                mouseY >= y + STOP_BTN_Y && mouseY <= y + STOP_BTN_Y + BTN_HEIGHT) {
                guiGraphics.fill(x + STOP_BTN_X, y + STOP_BTN_Y,
                    x + STOP_BTN_X + BTN_WIDTH, y + STOP_BTN_Y + BTN_HEIGHT,
                    0x80FFFFFF); // 半透明白色高光
            }
            // 单次按钮高光
            if (mouseX >= x + SINGLE_BTN_X && mouseX <= x + SINGLE_BTN_X + BTN_WIDTH &&
                mouseY >= y + SINGLE_BTN_Y && mouseY <= y + SINGLE_BTN_Y + BTN_HEIGHT) {
                guiGraphics.fill(x + SINGLE_BTN_X, y + SINGLE_BTN_Y,
                    x + SINGLE_BTN_X + BTN_WIDTH, y + SINGLE_BTN_Y + BTN_HEIGHT,
                    0x80FFFFFF);
            }
            // 循环按钮高光
            if (mouseX >= x + LOOP_BTN_X && mouseX <= x + LOOP_BTN_X + BTN_WIDTH &&
                mouseY >= y + LOOP_BTN_Y && mouseY <= y + LOOP_BTN_Y + BTN_HEIGHT) {
                guiGraphics.fill(x + LOOP_BTN_X, y + LOOP_BTN_Y,
                    x + LOOP_BTN_X + BTN_WIDTH, y + LOOP_BTN_Y + BTN_HEIGHT,
                    0x80FFFFFF);
            }
        }
    }

    // 0.1.11.219: the status line (STATUS_* of the block entity), with the reason and the paid UU in its tooltip
    private static final String[] STATUS_KEYS = {"stopped", "replicating", "ready", "paused", "pattern_paused",
        "no_pattern", "no_uu", "no_energy", "output_full", "held"};
    private static final int STATUS_X = 50, STATUS_Y = 39, STATUS_WIDTH = 100;

    private static String statusKey(int status) {
        return STATUS_KEYS[status >= 0 && status < STATUS_KEYS.length ? status : 0];
    }

    private Component statusLine(com.miophas.singularity_iteration.common.menu.producer.ReplicatorElcMenu menu) {
        int status = menu.getStatus();
        String key = "gui.mio_icif.replicator.status." + statusKey(status);
        boolean withPercent = status == mio_icif_replicator_elc.STATUS_REPLICATING || status == mio_icif_replicator_elc.STATUS_PAUSED
            || status == mio_icif_replicator_elc.STATUS_PATTERN_PAUSED;
        return withPercent ? Component.translatable(key, String.format(java.util.Locale.ROOT, "%.2f%%", menu.getProgress() / 100.0))
            : Component.translatable(key);
    }

    private static int statusColor(int status) {
        return switch (status) {
            case mio_icif_replicator_elc.STATUS_REPLICATING, mio_icif_replicator_elc.STATUS_READY -> 0x2A2E33;
            case mio_icif_replicator_elc.STATUS_HELD -> 0xB02020;
            default -> 0xA86A00;
        };
    }

    /** 0.1 mB units as "1,234.5". */
    private static String millibuckets(int tenths) {
        return String.format(java.util.Locale.ROOT, "%,.1f", tenths / 10.0);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        com.miophas.singularity_iteration.common.menu.producer.ReplicatorElcMenu menu = this.menu;
        if (menu == null) return;
        Component line = statusLine(menu);
        String text = line.getString();
        if (this.font.width(text) > STATUS_WIDTH) text = this.font.plainSubstrByWidth(text, STATUS_WIDTH - this.font.width("…")) + "…";
        guiGraphics.drawString(this.font, text, STATUS_X, STATUS_Y, statusColor(menu.getStatus()), false);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        com.miophas.singularity_iteration.common.menu.producer.ReplicatorElcMenu menu = this.menu;
        if (menu == null) return;

        // 鼠标悬停提示 - 闪电标志区域
        if (mouseX >= x + LIGHTNING_X && mouseX <= x + LIGHTNING_X + LIGHTNING_WIDTH &&
            mouseY >= y + LIGHTNING_Y && mouseY <= y + LIGHTNING_Y + LIGHTNING_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal(menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                mouseX, mouseY);
        }

        // 鼠标悬停提示 - 流体槽区域 (0.1.11.219: and the UU balance)
        if (mouseX >= x + FLUID_TANK_X && mouseX <= x + FLUID_TANK_X + FLUID_TANK_WIDTH &&
            mouseY >= y + FLUID_TANK_Y && mouseY <= y + FLUID_TANK_Y + FLUID_TANK_HEIGHT) {
            java.util.List<Component> tooltip = new java.util.ArrayList<>();
            tooltip.add(Component.literal(mio_icif_fluids.UUMATTER.get().getFluidType().getDescription().getString()));
            tooltip.add(Component.literal(menu.getFluidAmount() + " / " + menu.getFluidCapacity() + " mB"));
            if (menu.getBalanceTenths() > 0) tooltip.add(Component.translatable("gui.mio_icif.replicator.tip.balance", millibuckets(menu.getBalanceTenths())));
            guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }

        // 0.1.11.219: the status line - why, the paid UU, the balance
        if (mouseX >= x + STATUS_X - 2 && mouseX <= x + STATUS_X + STATUS_WIDTH && mouseY >= y + STATUS_Y - 2 && mouseY <= y + STATUS_Y + 10) {
            java.util.List<net.minecraft.util.FormattedCharSequence> lines = new java.util.ArrayList<>();
            lines.add(statusLine(menu).getVisualOrderText());
            int status = menu.getStatus();
            if (status == mio_icif_replicator_elc.STATUS_PAUSED || status == mio_icif_replicator_elc.STATUS_PATTERN_PAUSED
                    || status == mio_icif_replicator_elc.STATUS_HELD || status == mio_icif_replicator_elc.STATUS_NO_UU) {
                lines.addAll(this.font.split(Component.translatable("gui.mio_icif.replicator.detail." + statusKey(status)), 220));
            }
            if (menu.getPriceTenths() > 0) lines.add(Component.translatable("gui.mio_icif.replicator.tip.paid",
                millibuckets(menu.getPaidTenths()), millibuckets(menu.getPriceTenths())).getVisualOrderText());
            if (menu.getBalanceTenths() > 0) lines.add(Component.translatable("gui.mio_icif.replicator.tip.balance",
                millibuckets(menu.getBalanceTenths())).getVisualOrderText());
            guiGraphics.renderTooltip(this.font, lines, mouseX, mouseY);
        }

        // 鼠标悬停提示 - 停止按钮 (0.1.11.219: Stop pauses, a second Stop ends the copy)
        if (mouseX >= x + STOP_BTN_X && mouseX <= x + STOP_BTN_X + BTN_WIDTH &&
            mouseY >= y + STOP_BTN_Y && mouseY <= y + STOP_BTN_Y + BTN_HEIGHT) {
            java.util.List<net.minecraft.util.FormattedCharSequence> lines = new java.util.ArrayList<>();
            lines.add(Component.translatable("gui.mio_icif.replicator.button.stop").getVisualOrderText());
            lines.addAll(this.font.split(Component.translatable("gui.mio_icif.replicator.button.stop.tip"), 220));
            guiGraphics.renderTooltip(this.font, lines, mouseX, mouseY);
        }

        // 鼠标悬停提示 - 单次按钮
        if (mouseX >= x + SINGLE_BTN_X && mouseX <= x + SINGLE_BTN_X + BTN_WIDTH &&
            mouseY >= y + SINGLE_BTN_Y && mouseY <= y + SINGLE_BTN_Y + BTN_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.mio_icif.replicator.button.single"), mouseX, mouseY);
        }

        // 鼠标悬停提示 - 循环按钮
        if (mouseX >= x + LOOP_BTN_X && mouseX <= x + LOOP_BTN_X + BTN_WIDTH &&
            mouseY >= y + LOOP_BTN_Y && mouseY <= y + LOOP_BTN_Y + BTN_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.mio_icif.replicator.button.loop"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu != null && this.minecraft != null) {
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;

            // 检查停止按钮点击 (按钮ID: 0)
            if (mouseX >= x + STOP_BTN_X && mouseX <= x + STOP_BTN_X + BTN_WIDTH &&
                mouseY >= y + STOP_BTN_Y && mouseY <= y + STOP_BTN_Y + BTN_HEIGHT) {
                // 发送按钮点击到服务器
                this.minecraft.getConnection().send(new net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket(
                    this.menu.containerId, 0));
                // 客户端立即响应
                this.menu.clickMenuButton(this.minecraft.player, 0);
                return true;
            }

            // 检查单次按钮点击 (按钮ID: 1)
            if (mouseX >= x + SINGLE_BTN_X && mouseX <= x + SINGLE_BTN_X + BTN_WIDTH &&
                mouseY >= y + SINGLE_BTN_Y && mouseY <= y + SINGLE_BTN_Y + BTN_HEIGHT) {
                this.minecraft.getConnection().send(new net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket(
                    this.menu.containerId, 1));
                this.menu.clickMenuButton(this.minecraft.player, 1);
                return true;
            }

            // 检查循环按钮点击 (按钮ID: 2)
            if (mouseX >= x + LOOP_BTN_X && mouseX <= x + LOOP_BTN_X + BTN_WIDTH &&
                mouseY >= y + LOOP_BTN_Y && mouseY <= y + LOOP_BTN_Y + BTN_HEIGHT) {
                this.minecraft.getConnection().send(new net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket(
                    this.menu.containerId, 2));
                this.menu.clickMenuButton(this.minecraft.player, 2);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 数据通过 ContainerData 自动同步，无需手动同步
    }
}
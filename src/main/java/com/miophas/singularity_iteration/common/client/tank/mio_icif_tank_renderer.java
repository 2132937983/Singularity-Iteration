package com.miophas.singularity_iteration.common.client.tank;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractTankBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 储罐信息渲染器。
 * 在准心指向储罐（铁/青铜/精炼铁/钛/铱储罐）时，显示存储流体的名称、总容量与当前容积。
 */
@SuppressWarnings("null")
public class mio_icif_tank_renderer {

    /**
     * 渲染GUI事件处理，在准心指向储罐时显示流体信息。
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
        BlockPos pos = blockHit.getBlockPos();
        BlockEntity blockEntity = mc.level.getBlockEntity(pos);
        if (!(blockEntity instanceof AbstractTankBlockEntity<?> tank)) {
            return;
        }

        int amount = tank.getTotalFluidAmount();
        int capacity = tank.getTotalCapacity();
        FluidStack common = tank.getCommonFluid();

        // 第一行：流体名称（空罐 / 多种流体 / 具体流体名称）
        String fluidText;
        if (amount <= 0) {
            fluidText = Component.translatable("gui.mio_icif.tank.empty").getString();
        } else if (common == null) {
            fluidText = Component.translatable("gui.mio_icif.tank.mixed").getString();
        } else {
            fluidText = Component.translatable("gui.mio_icif.tank.fluid", common.getHoverName()).getString();
        }

        // 第二行：当前容积 / 总容量
        String amountText = Component.translatable("gui.mio_icif.tank.amount",
                formatFluidAmount(amount), formatFluidAmount(capacity)).getString();

        renderTankText(event.getGuiGraphics(), fluidText, amountText, mc.font);
    }

    /**
     * 将流体体积（mB）格式化为易读文本。
     * <p>不足 1000 mB 时以 {@code mB} 为单位；否则换算为桶（{@code B}），
     * 并在桶数过大时以 {@code K/M/B} 缩写（与 EU 电表显示风格一致）。
     */
    private static String formatFluidAmount(int milliBuckets) {
        if (milliBuckets < 1000) {
            return milliBuckets + " mB";
        }
        double buckets = milliBuckets / 1000.0;
        if (buckets >= 1_000_000_000D) {
            return String.format("%.2fB B", buckets / 1_000_000_000D);
        }
        if (buckets >= 1_000_000D) {
            return String.format("%.2fM B", buckets / 1_000_000D);
        }
        if (buckets >= 1_000D) {
            return String.format("%.2fK B", buckets / 1_000D);
        }
        if (buckets == Math.floor(buckets)) {
            return (long) buckets + " B";
        }
        return String.format("%.2f B", buckets);
    }

    /**
     * 将储罐信息渲染到准心右上方。
     */
    private static void renderTankText(GuiGraphics graphics, String fluidText, String amountText, Font font) {
        Minecraft mc = Minecraft.getInstance();

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        int textX = centerX + 15; // 准心右侧15像素
        int textY = centerY - 10; // 准心上方10像素

        int maxWidth = Math.max(font.width(fluidText), font.width(amountText));
        int bgHeight = font.lineHeight * 2 + 6;
        graphics.fill(textX - 2, textY - 2, textX + maxWidth + 2, textY + bgHeight, 0xAA000000);

        // 流体名称（白色）
        graphics.drawString(font, fluidText, textX, textY, 0xFFFFFF);
        // 容积信息（青色）
        graphics.drawString(font, amountText, textX, textY + font.lineHeight + 2, 0x55FFFF);
    }
}

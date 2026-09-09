package com.singularity_iteration.mio_icif.client.checker;

import com.singularity_iteration.mio_icif.Blocks.entity.checker.mio_icif_checker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 电压检测器渲染�? * 在准心指向电压检测器时渲染电压信息? */
@SuppressWarnings("null")
public class mio_icif_checker_renderer {

    /**
     * 渲染GUI事件处理
     * 在准心指向电压检测器时显示电压信息
 */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        
        // 检查是否指向方法
    if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
        BlockPos pos = blockHit.getBlockPos();

        // 检查是否指向电压检测器
        if (mc.level == null) return;
        BlockEntity blockEntity = mc.level.getBlockEntity(pos);
        if (!(blockEntity instanceof mio_icif_checker checker)) {
            return;
        }

        // 获取检测到的电压和等级
        long detectedVoltage = checker.getDetectedVoltage();
        String tierName = getTierName(checker.getDetectedTier().tierIndex);
        long inputEnergy = checker.getInputEnergy();
        
        // 获取格式化的电压显示文本
        String voltageText;
        if (detectedVoltage <= 0) {
            voltageText = Component.translatable("gui.mio_icif.checker.no_voltage").getString();
        } else {
            voltageText = Component.translatable("gui.mio_icif.checker.voltage_display", 
                detectedVoltage + " EU/t", tierName).getString();
        }
        
        // 获取格式化的输入电能文本
        String inputEnergyText;
        if (inputEnergy <= 0) {
            inputEnergyText = Component.translatable("gui.mio_icif.checker.no_input").getString();
        } else {
            inputEnergyText = Component.translatable("gui.mio_icif.checker.input_energy_display", 
                inputEnergy + " EU/t").getString();
        }
        
        // 渲染文本到准心右上方
        renderVoltageText(event.getGuiGraphics(), voltageText, inputEnergyText, mc.font);
    }

    /**
     * 获取等级名称
     */
    private static String getTierName(int tierIndex) {
        return switch (tierIndex) {
            case 0 -> "LV-低压";
            case 1 -> "MV-中压";
            case 2 -> "HV-高压";
            case 3 -> "EV-超高压";
            case 4 -> "IV-强导压";
            case 5 -> "LuV-剧差压";
            case 6 -> "ZPM-零点压";
            case 7 -> "UV-极高压";
            case 8 -> "UHV-超极限压";
            case 9 -> "UEV-超极限压";
            case 10 -> "UIV-极强导压";
            case 11 -> "UXV-极上拓压";
            case 12 -> "OpV-过载压";
            case 13 -> "MAX-终压";
            default -> "未知";
        };
    }

    /**
     * 渲染电压文本到准心右上方
     */
    private static void renderVoltageText(net.minecraft.client.gui.GuiGraphics graphics, String voltageText, String inputEnergyText, Font font) {
        Minecraft mc = Minecraft.getInstance();

        // 获取屏幕中心（准心位置）
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // 计算文本位置（准心右上方法
    int textX = centerX + 15; // 准心右侧15像素
        int textY = centerY - 10; // 准心上方10像素

        // 判断是否显示输入电能（无输入时不显示�
    boolean hasInput = !inputEnergyText.equals(Component.translatable("gui.mio_icif.checker.no_input").getString());

        // 获取文本宽度
        int voltageWidth = font.width(voltageText);
        int maxWidth = voltageWidth;
        if (hasInput) {
            int inputWidth = font.width(inputEnergyText);
            maxWidth = Math.max(voltageWidth, inputWidth);
        }

        // 绘制背景（半透明黑色�
    int bgHeight = hasInput ? font.lineHeight * 2 + 6 : font.lineHeight + 4; // 根据是否有输入调整高�
    graphics.fill(textX - 2, textY - 2, textX + maxWidth + 2, textY + bgHeight, 0xAA000000);

        // 绘制电压文本（白色）
        graphics.drawString(font, voltageText, textX, textY, 0xFFFFFF);

        // 绘制输入电能文本（绿色）- 仅当有输入时显示
        if (hasInput) {
            graphics.drawString(font, inputEnergyText, textX, textY + font.lineHeight + 2, 0x55FF55);
        }
    }
}
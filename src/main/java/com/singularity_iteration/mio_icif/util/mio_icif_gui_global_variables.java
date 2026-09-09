package com.singularity_iteration.mio_icif.util;

import net.minecraft.resources.ResourceLocation;

/**
 * GUI 通用组件常量定义
 * 供 Menu 和 Screen 共同引用，避免重复定义
 */
@SuppressWarnings("null")
public final class mio_icif_gui_global_variables {

    // === 图集纹理 ===
    public static final ResourceLocation ATLAS_TEXTURE = ResourceLocation.parse("mio_icif:textures/gui/gui_components_atlas.png");
    public static final int ATLAS_WIDTH = 256;
    public static final int ATLAS_HEIGHT = 512;

    // === 标准能量条背景 xy=(32,271) uv=(33,17) ===
    public static final int ENERGY_BAR_BG_WIDTH = 33;
    public static final int ENERGY_BAR_BG_HEIGHT = 17;
    public static final int ENERGY_BAR_BG_TEXTURE_X = 32;
    public static final int ENERGY_BAR_BG_TEXTURE_Y = 271;

    // === 标准能量条 xy=(2,271) uv=(29,17) - 从左往右填充 ===
    public static final int ENERGY_BAR_WIDTH = 29;
    public static final int ENERGY_BAR_HEIGHT = 17;
    public static final int ENERGY_BAR_TEXTURE_X = 2;
    public static final int ENERGY_BAR_TEXTURE_Y = 271;

    // === 动能发电机能量条背景 xy=(2,289) uv=(98,17) - 从左往右填充 ===
    public static final int KINETIC_ENERGY_BAR_WIDTH = 98;
    public static final int KINETIC_ENERGY_BAR_HEIGHT = 17;
    public static final int KINETIC_ENERGY_BAR_TEXTURE_X = 2;
    public static final int KINETIC_ENERGY_BAR_TEXTURE_Y = 289;

    // === 动能发电机能量条 xy=(4,307) uv=(94,17) - 从左往右填充 ===
    public static final int KINETIC_ENERGY_BAR2_WIDTH = 94;
    public static final int KINETIC_ENERGY_BAR2_HEIGHT = 17;
    public static final int KINETIC_ENERGY_BAR2_TEXTURE_X = 4;
    public static final int KINETIC_ENERGY_BAR2_TEXTURE_Y = 307;

    // 闪电标志背景 xy=(87,227) uv=(14,14) - 从左往右填充
    public static final int LIGHTNING_BG_WIDTH = 14;
    public static final int LIGHTNING_BG_HEIGHT = 14;
    public static final int LIGHTNING_BG_TEXTURE_X = 87;
    public static final int LIGHTNING_BG_TEXTURE_Y = 227;

    // === 闪电能量图标 (14x14, uv=72,227) - 从下往上填充 ===
    public static final int LIGHTNING_WIDTH = 14;
    public static final int LIGHTNING_HEIGHT = 14;
    public static final int LIGHTNING_U = 72;
    public static final int LIGHTNING_V = 227;

    // 标准工作进度条背景 xy=(189，164) uv=(23，15)
    public static final int PROGRESS_BAR_BG_WIDTH = 23;
    public static final int PROGRESS_BAR_BG_HEIGHT = 15;
    public static final int PROGRESS_BAR_BG_TEXTURE_X = 189;
    public static final int PROGRESS_BAR_BG_TEXTURE_Y = 164;

    // === 标准工作进度条,进度箭头 (23x15, uv=164,164) - 从左往右填充 ===
    public static final int ARROW_WIDTH = 23;
    public static final int ARROW_HEIGHT = 15;
    public static final int ARROW_U = 164;
    public static final int ARROW_V = 164;

    // === 横向能量条 (24x17, uv=2,192) ===
    public static final int ENERGY_BAR2_WIDTH = 24;
    public static final int ENERGY_BAR2_HEIGHT = 17;
    public static final int ENERGY_BAR2_U = 2;
    public static final int ENERGY_BAR2_V = 192;

    // === 动能条 (58x14, uv=2,211) - 从左往右填充 ===
    public static final int KINETIC_BAR_WIDTH = 58;
    public static final int KINETIC_BAR_HEIGHT = 14;
    public static final int KINETIC_BAR_U = 2;
    public static final int KINETIC_BAR_V = 211;

    // === 工作状态图标 (14x14) ===
    public static final int WORK_ICON_WIDTH = 14;
    public static final int WORK_ICON_HEIGHT = 14;
    public static final int WORK_ACTIVE_U = 88;
    public static final int WORK_ACTIVE_V = 227;
    public static final int WORK_INACTIVE_U = 104;
    public static final int WORK_INACTIVE_V = 227;

 // === 高热量进度条 (23x8, uv=35,259) - 从左往右填充 ===
    public static final int HEAT_BAR_WIDTH = 23;
    public static final int HEAT_BAR_HEIGHT = 8;
    public static final int HEAT_BAR_U = 35;
    public static final int HEAT_BAR_V = 259;

 // === 高热量允许标志 (14x14, uv=136,227) ===
    public static final int HEAT_OK_WIDTH = 14;
    public static final int HEAT_OK_HEIGHT = 14;
    public static final int HEAT_OK_U = 136;
    public static final int HEAT_OK_V = 227;

 // === 高压缩空气允许标志 (27x27, uv=201,125) ===
    public static final int AIR_OK_WIDTH = 27;
    public static final int AIR_OK_HEIGHT = 27;
    public static final int AIR_OK_U = 201;
    public static final int AIR_OK_V = 125;

 // === 高冶炼进度条 (27x27, uv=172,125) - 从下往上填充 ===
    public static final int BLAST_PROGRESS_WIDTH = 27;
    public static final int BLAST_PROGRESS_HEIGHT = 27;
    public static final int BLAST_PROGRESS_U = 172;
    public static final int BLAST_PROGRESS_V = 125;

    // 热能离心机进度条 xy=(66,125) uv=(5,30) - 从下往上填充
    public static final int HEAT_ENERGY_CENTRIFUGE_X = 5;
    public static final int HEAT_ENERGY_CENTRIFUGE_Y = 30;
    public static final int HEAT_ENERGY_CENTRIFUGE_U = 66;
    public static final int HEAT_ENERGY_CENTRIFUGE_V = 125;

    // 压缩机专属工作进度条背景 xy=(103,289) uv=(22,15) - 从左往右填充
    public static final int COMPRESSOR_WORK_PROGRESS_BG_X = 103;
    public static final int COMPRESSOR_WORK_PROGRESS_BG_Y = 289;
    public static final int COMPRESSOR_WORK_PROGRESS_BG_U = 22;
    public static final int COMPRESSOR_WORK_PROGRESS_BG_V = 15;

    // 压缩机专属工作进度条 xy=(126,289) uv=(22,15) - 从左往右填充
    public static final int COMPRESSOR_WORK_PROGRESS_X = 126;
    public static final int COMPRESSOR_WORK_PROGRESS_Y = 289;
    public static final int COMPRESSOR_WORK_PROGRESS_U = 22;
    public static final int COMPRESSOR_WORK_PROGRESS_V = 15;

    // 冷凝机专属工作进度条 xy=(144,243) uv=(84,9) - 从左往右填充
    public static final int CONDENSER_WORK_PROGRESS_X = 144;
    public static final int CONDENSER_WORK_PROGRESS_Y = 243;
    public static final int CONDENSER_WORK_PROGRESS_U = 84;
    public static final int CONDENSER_WORK_PROGRESS_V = 9;

    // 电动切割机专属工作进度条背景 xy=(208,288) uv=(48,16)
    public static final int CUTTER_WORK_PROGRESS_BG_X = 208;
    public static final int CUTTER_WORK_PROGRESS_BG_Y = 288;
    public static final int CUTTER_WORK_PROGRESS_BG_U = 48;
    public static final int CUTTER_WORK_PROGRESS_BG_V = 16;

    // 电动切割机专属工作进度条 xy=(160,288) uv=(48,16) - 从左往右填充
    public static final int CUTTER_WORK_PROGRESS_X = 160;
    public static final int CUTTER_WORK_PROGRESS_Y = 288;
    public static final int CUTTER_WORK_PROGRESS_U = 48;
    public static final int CUTTER_WORK_PROGRESS_V = 16;

    // 提取机专属工作进度条背景 xy=(103,307) uv=(22,15)
    public static final int EXTRACTOR_WORK_PROGRESS_BG_X = 103;
    public static final int EXTRACTOR_WORK_PROGRESS_BG_Y = 307;
    public static final int EXTRACTOR_WORK_PROGRESS_BG_U = 22;
    public static final int EXTRACTOR_WORK_PROGRESS_BG_V = 15;

    // 提取机专属工作进度条 xy=(126,307) uv=(22,15) - 从左往右填充
    public static final int EXTRACTOR_WORK_PROGRESS_X = 126;
    public static final int EXTRACTOR_WORK_PROGRESS_Y = 307;
    public static final int EXTRACTOR_WORK_PROGRESS_U = 22;
    public static final int EXTRACTOR_WORK_PROGRESS_V = 15;

    // 发酵机工作进度条 xy=(96,259) uv=(40,7) - 从左往右填充
    public static final int FERMENTER_PROGRESS_X = 96;
    public static final int FERMENTER_PROGRESS_Y = 259;
    public static final int FERMENTER_PROGRESS_U = 40;
    public static final int FERMENTER_PROGRESS_V = 7;

    // 发酵机热量条 xy=(156,259) uv=(40,3) - 从左往右填充
    public static final int FERMENTER_HEAT_X = 156;
    public static final int FERMENTER_HEAT_Y = 259;
    public static final int FERMENTER_HEAT_U = 40;
    public static final int FERMENTER_HEAT_V = 3;

    // 标准流体槽背景 xy=(38,2) uv=(20,55)
    public static final int FLUID_TANK_BG_TEXTURE_X = 38;
    public static final int FLUID_TANK_BG_TEXTURE_Y = 2;
    public static final int FLUID_TANK_BG_U = 20;
    public static final int FLUID_TANK_BG_V = 55;

    // 标准流体空槽 xy=(60,2) uv=(20,55)
    public static final int FLUID_TANK_X = 60;
    public static final int FLUID_TANK_Y = 2;
    public static final int FLUID_TANK_U = 20;
    public static final int FLUID_TANK_V = 55;

    // 标准流体尺寸 uv=(12,47)
    public static final int FLUID_U = 12;
    public static final int FLUID_V = 47;

    // 标准流体槽刻度 xy=(197,7) uv=(9,37)
    public static final int FLUID_TANK_SCALE_X = 197;
    public static final int FLUID_TANK_SCALE_Y = 7;
    public static final int FLUID_TANK_SCALE_U = 9;
    public static final int FLUID_TANK_SCALE_V = 37;

    // 标准火焰标志背景 xy=(198,259) uv=(14,14) - 固定显示
    public static final int FLAME_ICON_BG_X = 198;
    public static final int FLAME_ICON_BG_Y = 259;
    public static final int FLAME_ICON_BG_U = 14;
    public static final int FLAME_ICON_BG_V = 14;

    // 标准火焰标志 xy=(214,259) uv=(14,14) - 从下往上填充
    public static final int FLAME_ICON_X = 214;
    public static final int FLAME_ICON_Y = 259;
    public static final int FLAME_ICON_U = 14;
    public static final int FLAME_ICON_V = 14;

    // 金属成型机专属工作进度条背景 xy=(208,304) uv=(48,17)
    public static final int METAL_FORMER_WORK_PROGRESS_BG_X = 208;
    public static final int METAL_FORMER_WORK_PROGRESS_BG_Y = 304;
    public static final int METAL_FORMER_WORK_PROGRESS_BG_U = 48;
    public static final int METAL_FORMER_WORK_PROGRESS_BG_V = 17;

    // 金属成型机专属工作进度条 xy=(160,304) uv=(48,17) - 从左往右填充
    public static final int METAL_FORMER_WORK_PROGRESS_X = 160;
    public static final int METAL_FORMER_WORK_PROGRESS_Y = 304;
    public static final int METAL_FORMER_WORK_PROGRESS_U = 48;
    public static final int METAL_FORMER_WORK_PROGRESS_V = 17;

    // 滑块纹理 位置xy=(96,192) 长宽uv=(12,15)
    public static final int SCROLLBAR_TEXTURE_X = 96;
    public static final int SCROLLBAR_TEXTURE_Y = 192; 
    public static final int SCROLLBAR_TEXTURE_U = 12;
    public static final int SCROLLBAR_TEXTURE_V = 15;

    // 储电盒红石模式按钮图标未被点击 位置xy=(140,190) 长宽uv=(18,18)
    public static final int REDSTONE_BUTTON_TEXTURE_X = 140;
    public static final int REDSTONE_BUTTON_TEXTURE_Y = 190;
    public static final int REDSTONE_BUTTON_TEXTURE_U = 18;
    public static final int REDSTONE_BUTTON_TEXTURE_V = 18;

    // 储电盒红石模式按钮图标被点击 位置xy=(160,190) 长宽uv=(18,18)
    public static final int REDSTONE_BUTTON_CLICKED_TEXTURE_X = 160;
    public static final int REDSTONE_BUTTON_CLICKED_TEXTURE_Y = 190;
    public static final int REDSTONE_BUTTON_CLICKED_TEXTURE_U = 18;
    public static final int REDSTONE_BUTTON_CLICKED_TEXTURE_V = 18;

    private mio_icif_gui_global_variables() {
        // 防止实例化
    }
}
// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.crop;

/**
 * <p><b>EN:</b> Immutable plant attributes: tier ({@link #getLevel()}) and five IC2 attribute values
 * (chemistry, nutrition / food, colour / defensive, medicinal / colourful, danger / weed). The
 * crossing weight compares these values ({@code PlantHybridization.calculateRatio}).
 * <p><b>ZH:</b> 不可变的植物属性：等级（{@link #getLevel()}）与五项 IC2 属性值（化学、营养/食物、颜色/防御、药用/多彩、危险/杂草）。
 * 杂交权重会比较这些数值（{@code PlantHybridization.calculateRatio}）。
 * <p><b>JA:</b> 不変の植物属性：ティア（{@link #getLevel()}）と IC2 の 5 つの属性値（化学、栄養/食用、色/防御、薬効/彩り、危険/雑草）。
 * 交配の重みはこれらの値を比べる（{@code PlantHybridization.calculateRatio}）。
 */
public class PlantStats {
    private final int level;
    private final int chemistry;
    private final int nutrition;
    private final int color;
    private final int medicinal;
    private final int danger;

    public PlantStats(int level, int chemistry, int nutrition, int color, int medicinal, int danger) {
        this.level = level;
        this.chemistry = chemistry;
        this.nutrition = nutrition;
        this.color = color;
        this.medicinal = medicinal;
        this.danger = danger;
    }

    public int getLevel() { return level; }
    public int getChemistry() { return chemistry; }
    public int getNutrition() { return nutrition; }
    public int getColor() { return color; }
    public int getMedicinal() { return medicinal; }
    public int getDanger() { return danger; }

    /**
     * <p><b>EN:</b> IC2 positional names of the same values; the old getter names stay for binary compatibility.
     * <p><b>ZH:</b> 同一数值的 IC2 位置名称；旧的 getter 名称为二进制兼容而保留。
     * <p><b>JA:</b> 同じ値の IC2 での位置名。旧ゲッター名はバイナリ互換のため残す。
     */
    public int getDefensive() { return color; }
    public int getColorful() { return medicinal; }
    public int getWeed() { return danger; }

    public int stat(int index) {
        return switch (index) {
            case 0 -> chemistry;
            case 1 -> nutrition;
            case 2 -> color;
            case 3 -> medicinal;
            case 4 -> danger;
            default -> 0;
        };
    }

    @Override
    public String toString() {
        return "PlantStats{level=" + level + ", chemistry=" + chemistry + ", nutrition=" + nutrition
            + ", color=" + color + ", medicinal=" + medicinal + ", danger=" + danger + "}";
    }
}

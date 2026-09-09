package com.singularity_iteration.mio_icif.api.crop;

/**
 * 植物属性类
 * 包含植物的基本属性：等级、化学性、食物性、颜色性、医学性、杂草 * 对标IC2的CropCard.stat(n)五属性系  * stat(0)=chemistry, stat(1)=nutrition, stat(2)=color, stat(3)=medicinal, stat(4)=danger(weediness)
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

    public int getLevel() {
        return level;
    }

    public int getChemistry() {
        return chemistry;
    }

    public int getNutrition() {
        return nutrition;
    }

    public int getColor() {
        return color;
    }

    public int getMedicinal() {
        return medicinal;
    }

    public int getDanger() {
        return danger;
    }

    public int stat(int n) {
        return switch (n) {
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
        return "PlantStats{" +
                "level=" + level +
                ", chemistry=" + chemistry +
                ", nutrition=" + nutrition +
                ", color=" + color +
                ", medicinal=" + medicinal +
                ", danger=" + danger +
                '}';
    }
}


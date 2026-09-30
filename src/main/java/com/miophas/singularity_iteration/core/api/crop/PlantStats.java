// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.crop;

/** Immutable crop metadata matching the frozen public SI contract. */
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

    /** IC2 positional semantics; old getter names remain for binary compatibility. */
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

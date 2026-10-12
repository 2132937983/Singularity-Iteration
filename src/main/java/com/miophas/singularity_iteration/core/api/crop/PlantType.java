// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.crop;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * <p><b>EN:</b> A plant species of the crop-stick system. Identity is "modId:typeId" (equals and hashCode
 * use it). Two growth models exist:
 * <ul>
 *   <li>IC2 crop cycle ({@link #usesIc2CropCycle()} true, see {@code Ic2PlantType}): the planter
 *       adds growth points every 256 ticks from the environment quality; {@link #tick} is only a
 *       hook for special behavior.</li>
 *   <li>Legacy per-tick model (default): the planter calls {@link #tick} every tick and this class
 *       adds 1 + growth points.</li>
 * </ul>
 * <p><b>ZH:</b> 作物架系统中的一个植物物种。身份为 “modId:typeId”（equals 与 hashCode 使用它）。存在两种生长模型：
 * <ul>
 *   <li>IC2 作物周期（{@link #usesIc2CropCycle()} 为真，见 {@code Ic2PlantType}）：作物架每 256 tick 按环境品质增加生长点；
 *       {@link #tick} 仅作为特殊行为的挂钩。</li>
 *   <li>旧的逐 tick 模型（默认）：作物架每 tick 调用 {@link #tick}，本类增加 1 + 生长属性的生长点。</li>
 * </ul>
 * <p><b>JA:</b> 作物棒システムの植物の種。識別子は「modId:typeId」（equals と hashCode がこれを使う）。成長モデルは 2 つ：
 * <ul>
 *   <li>IC2 作物サイクル（{@link #usesIc2CropCycle()} が真、{@code Ic2PlantType} 参照）：作物棒は 256 tick ごとに環境品質から
 *       成長ポイントを加える。{@link #tick} は特殊動作のフックにすぎない。</li>
 *   <li>旧来の毎 tick モデル（既定）：作物棒が毎 tick {@link #tick} を呼び、このクラスが 1 + 成長値のポイントを加える。</li>
 * </ul>
 */
public abstract class PlantType {
    public abstract String getTypeId();
    public abstract String getModId();

    public String getTranslationKey() {
        return getModId() + ".plant." + getTypeId();
    }

    public String getFoundBy() {
        return "Unknown";
    }

    public String getFoundBy(String fallback) {
        String foundBy = getFoundBy();
        return foundBy == null || foundBy.isBlank() || "Unknown".equals(foundBy) ? fallback : foundBy;
    }

    public abstract String[] getTraits();
    public abstract PlantStats getStats();
    public abstract int getMaxGrowthStage();

    public int getHarvestStage() { return getMaxGrowthStage(); }
    public int getOptimalHarvestStage() { return getHarvestStage(); }
    public int getStageAfterHarvest() { return 1; }
    /**
     * <p><b>EN:</b> True to use the IC2 256-tick growth-point cycle. False (default) keeps the legacy per-tick {@link #tick} model for old addons.
     * <p><b>ZH:</b> 返回 true 时使用 IC2 的 256 tick 生长点周期。false（默认）为旧附属保留逐 tick 的 {@link #tick} 模型。
     * <p><b>JA:</b> true なら IC2 の 256 tick 成長ポイントサイクルを使う。false（既定）は旧アドオン向けに毎 tick の {@link #tick} モデルを保つ。
     */
    public boolean usesIc2CropCycle() { return false; }
    public int getStageAfterHarvest(IPlanter planter) { return getStageAfterHarvest(); }
    public boolean onLeftClick(IPlanter planter, Player player) { return planter.pick(); }
    public boolean onCollision(IPlanter planter, net.minecraft.world.entity.Entity entity) {
        return entity instanceof net.minecraft.world.entity.LivingEntity && entity.isSprinting();
    }
    public int getRedstoneSignal(IPlanter planter) { return 0; }
    public int getLightEmission(IPlanter planter) { return 0; }

    public int getGrowthTime(IPlanter planter) {
        int level = Math.max(0, getStats().getLevel());
        // Frozen R153 public observations for the ordinary SI plant catalog.
        // Level-specific classes (generic crops, saplings and weeds) retain
        // their own overrides.
        return switch (level) {
            case 1 -> 17_391;
            case 2 -> 36_036;
            case 3 -> 56_074;
            case 4 -> 77_669;
            case 5 -> 102_000;
            default -> Math.max(200, (level + 1) * 24_000);
        };
    }

    /**
     * <p><b>EN:</b> True when the plant can grow now. Default: below the max stage, light 9 or more,
     * stored water and nutrients above 0. IC2 plants override this.
     * <p><b>ZH:</b> 植物现在能生长时为真。默认：未达最大阶段、光照不低于 9、储水与营养大于 0。IC2 植物会覆盖此方法。
     * <p><b>JA:</b> 植物が今成長できるなら真。既定：最大段階未満、明るさ 9 以上、貯水と栄養が 0 より大きい。IC2 植物は上書きする。
     */
    public boolean canGrow(IPlanter planter) {
        return planter != null
            && planter.getGrowthStage() < getMaxGrowthStage()
            && planter.getLightLevel() >= 9
            && planter.getWater() > 0
            && planter.getNutrients() > 0;
    }

    public boolean canHybridize(IPlanter planter) {
        return planter != null && !isWeed(planter) && planter.getGrowthStage() >= getHarvestStage();
    }

    public boolean isWeed(IPlanter planter) { return false; }

    public void tick(IPlanter planter) {
        if (!canGrow(planter)) return;
        int increment = Math.max(1, 1 + planter.getGrowthSpeed());
        int progress = Math.max(0, planter.getProgress()) + increment;
        int required = Math.max(1, getGrowthTime(planter));
        if (progress >= required) {
            planter.setProgress(progress - required);
            planter.setGrowthStage(Math.min(getMaxGrowthStage(), planter.getGrowthStage() + 1));
            planter.updateState();
        } else {
            planter.setProgress(progress);
        }
    }

    public boolean isHarvestable(IPlanter planter) {
        return planter != null && planter.getGrowthStage() >= getHarvestStage() && canBeHarvested(planter);
    }

    public boolean canBeHarvested(IPlanter planter) {
        return getHarvest(planter).length > 0;
    }

    public int getOptimalHarvestStage(IPlanter planter) { return getOptimalHarvestStage(); }
    public abstract ItemStack[] getHarvest(IPlanter planter);

    public ItemStack getGain(IPlanter planter) {
        ItemStack[] gains = getGains(planter);
        return gains.length == 0 ? ItemStack.EMPTY : gains[0].copy();
    }

    public ItemStack[] getGains(IPlanter planter) {
        ItemStack[] harvest = getHarvest(planter);
        ItemStack[] copy = new ItemStack[harvest.length];
        for (int i = 0; i < harvest.length; i++) copy[i] = harvest[i].copy();
        return copy;
    }

    public ItemStack getSeedItem(IPlanter planter) {
        return ItemStack.EMPTY;
    }

    public double dropGainChance() { return 0.95D; }

    /**
     * <p><b>EN:</b> Number of harvest rolls. Each roll adds all items of {@link #getGains}. The result can be 0.
     * <p><b>ZH:</b> 收获次数。每次加入 {@link #getGains} 的全部物品。结果可以为 0。
     * <p><b>JA:</b> 収穫の回数。1 回ごとに {@link #getGains} のアイテムをすべて加える。結果は 0 になりうる。
     */
    public int calculateDropCount(IPlanter planter) {
        if (planter == null) return 1;
        int bound = Math.max(1, 3 + planter.getYield());
        return 1 + planter.getPlanterWorld().random.nextInt(bound) / 3;
    }

    public ItemStack getSpecialDrop(IPlanter planter) { return ItemStack.EMPTY; }
    public int getRootDepth(IPlanter planter) { return 1; }
    public abstract String getTexture(int stage);
    public boolean onInteract(IPlanter planter, Player player) { return false; }

    public float dropSeedChance(IPlanter planter) {
        int resistance = planter == null ? 0 : Math.max(0, planter.getResilience());
        return Mth.clamp(0.2F - resistance * 0.01F, 0.02F, 0.25F);
    }

    /**
     * <p><b>EN:</b> Environment quality before the ×5 factor of the crop cycle. A plant can weight
     * humidity, nutrients and air quality differently (for example coffee needs nutrients).
     * <p><b>ZH:</b> 作物周期 ×5 系数之前的环境品质。植物可以对湿度、营养与空气质量使用不同权重（例如咖啡更需要营养）。
     * <p><b>JA:</b> 作物サイクルの ×5 係数を掛ける前の環境品質。植物ごとに湿度・栄養・空気品質の重みを変えられる（例：コーヒーは栄養を重視）。
     */
    public int weightInfluences(IPlanter planter, int humidity, int nutrients, int airQuality) {
        int growth = planter == null ? 0 : planter.getGrowthSpeed();
        return humidity + nutrients + airQuality + growth - getStats().getLevel();
    }

    public List<String> getExtraInfo() { return Collections.emptyList(); }

    @Override
    public boolean equals(Object value) {
        return value instanceof PlantType other
            && Objects.equals(getModId(), other.getModId())
            && Objects.equals(getTypeId(), other.getTypeId());
    }

    @Override
    public int hashCode() { return Objects.hash(getModId(), getTypeId()); }
}

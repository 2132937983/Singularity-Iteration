// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.crop;

/**
 * <p><b>EN:</b> Pure crop formulas of the IC2 crop cycle. The class has no Minecraft dependency.
 * The crop block entity and the crossing code call these methods. A standalone test checks
 * them without the game. All random values come in as arguments, so each method is
 * deterministic.
 *
 * <p><b>ZH:</b> IC2 作物周期的纯公式。本类不依赖 Minecraft。作物方块实体与杂交代码调用这些方法。
 * 独立测试可在不启动游戏的情况下检查它们。所有随机值都作为参数传入，因此每个方法的结果都是确定的。
 *
 * <p><b>JA:</b> IC2 作物サイクルの純粋な計算式。このクラスは Minecraft に依存しない。
 * 作物ブロックエンティティと交配コードがこれらのメソッドを呼ぶ。単体テストはゲームを起動せずに検査できる。
 * 乱数はすべて引数で渡すので、各メソッドの結果は決定的である。
 *
 * <h2>Cycle / 周期 / サイクル</h2>
 * <p><b>EN:</b> One cycle runs every 256 ticks. The order is: environment refresh, plant tick,
 * growth points, stage advance, resource decay (water and nutrients each -1), weed spread.
 * <p><b>ZH:</b> 每 256 tick 运行一个周期。顺序：刷新环境、植物 tick、生长点、进阶、资源衰减（水与营养各 -1）、杂草扩散。
 * <p><b>JA:</b> 256 tick ごとに 1 サイクルを実行する。順序：環境更新、植物 tick、成長ポイント、段階進行、
 * 資源減少（水と栄養を各 -1）、雑草拡散。
 */
public final class CropGrowthMath {
    /** EN: Ticks between two crop cycles. ZH: 两个作物周期之间的 tick 数。JA: 作物サイクル間の tick 数。 */
    public static final int CYCLE_TICKS = 256;
    /** EN: Highest value of a trait (growth, gain, resistance). ZH: 属性（生长、产量、抗性）的最大值。JA: 特性値（成長・収量・耐性）の最大値。 */
    public static final int MAX_TRAIT = 31;
    /** EN: Penalty above this value can kill the plant. ZH: 惩罚值超过此值时植物可能死亡。JA: ペナルティがこの値を超えると植物が枯れることがある。 */
    public static final int LETHAL_PENALTY = 100;

    private CropGrowthMath() {}

    /**
     * <p><b>EN:</b> Minimum environment quality that the plant needs for full growth.
     * <p><b>ZH:</b> 植物完全生长所需的最低环境品质。
     * <p><b>JA:</b> 植物が通常どおり成長するために必要な最低環境品質。
     */
    public static int requiredQuality(int tier, int growth, int gain, int resistance) {
        return Math.max(0, (tier - 1) * 4 + growth + gain + resistance);
    }

    /** EN: Quality margin above the requirement that a Cropmatron guarantees. ZH: 作物监护机保证的、高于需求的品质余量。JA: 作物管理機が保証する、要求を上回る品質の余裕。 */
    public static final int TENDED_MARGIN = 50;

    /**
     * <p><b>EN:</b> Quality of a crop that a Cropmatron tends (0.1.8.2): at least the requirement plus
     * {@link #TENDED_MARGIN}. A tended crop thus has no penalty, no death roll and at least +50 %
     * growth points. A better natural environment keeps its higher value.
     * The reason: the requirement grows with growth + gain + resistance (10/10/10 adds 30). In a poor
     * environment the missing quality times 4 reaches the lethal 100. The crop then stops or dies,
     * although the Cropmatron fills water, nutrients and Weed-EX.
     * <p><b>ZH:</b> 作物监护机照料的作物的品质（0.1.8.2）：至少为需求加 {@link #TENDED_MARGIN}。因此被照料的作物没有惩罚、没有死亡判定，
     * 生长点至少 +50%；自然环境更好时保留更高的值。原因：需求随生长 + 产量 + 抗性增加（10/10/10 增加 30）。环境较差时，缺少的品质 × 4 会达到致命的 100，
     * 即使监护机补满了水、营养与除草剂，作物也会停止生长或死亡。
     * <p><b>JA:</b> 作物管理機が管理する作物の品質（0.1.8.2）：少なくとも要求＋{@link #TENDED_MARGIN}。管理下の作物にはペナルティも枯死判定もなく、
     * 成長ポイントは最低でも +50 %。自然環境の方が良ければ高い値をそのまま使う。理由：要求は成長＋収量＋耐性で増える（10/10/10 で +30）。
     * 環境が悪いと不足分 × 4 が致命値 100 に達し、管理機が水・栄養・除草剤を満たしていても作物は止まるか枯れる。
     */
    public static int tendedQuality(int quality, int required) {
        return Math.max(quality, required + TENDED_MARGIN);
    }

    /**
     * <p><b>EN:</b> Penalty when the quality is too low: four points for each missing quality
     * point. The result is 0 when the quality is sufficient.
     * <p><b>ZH:</b> 品质不足时的惩罚：每缺少 1 点品质记 4 点。品质足够时结果为 0。
     * <p><b>JA:</b> 品質不足時のペナルティ：不足 1 点につき 4 点。品質が十分なら 0。
     */
    public static int penalty(int required, int quality) {
        return quality >= required ? 0 : (required - quality) * 4;
    }

    /**
     * <p><b>EN:</b> True when the penalty is lethal and the resistance roll fails.
     * {@code roll32} is a random value in [0, 31].
     * <p><b>ZH:</b> 惩罚致命且抗性检定失败时为真。{@code roll32} 是 [0, 31] 内的随机值。
     * <p><b>JA:</b> ペナルティが致命的で、耐性判定に失敗したとき真。{@code roll32} は [0, 31] の乱数。
     */
    public static boolean dies(int penalty, int resistance, int roll32) {
        return penalty > LETHAL_PENALTY && roll32 > resistance;
    }

    /**
     * <p><b>EN:</b> Growth points for one cycle. {@code base} is 3 + random[0, 6] + growth.
     * A quality above the requirement adds one percent for each extra point. A quality below
     * the requirement removes the penalty in percent. The result is never negative.
     * <p><b>ZH:</b> 一个周期的生长点。{@code base} 为 3 + 随机[0, 6] + 生长。品质每超出 1 点增加 1%；
     * 品质不足时按惩罚值（百分比）减少。结果不会为负。
     * <p><b>JA:</b> 1 サイクルの成長ポイント。{@code base} は 3 + 乱数[0, 6] + 成長値。品質が要求を 1 点超えるごとに
     * 1% 増える。品質が不足するとペナルティ（%）だけ減る。結果は負にならない。
     */
    public static int growthPoints(int base, int required, int quality) {
        if (quality >= required) return base * (100 + quality - required) / 100;
        return Math.max(0, base * (100 - penalty(required, quality)) / 100);
    }

    /**
     * <p><b>EN:</b> Trait of a crossed plant. {@code sum} is the sum of the parent traits.
     * {@code roll} is a random value in [0, 2 * parents]. The result is the parent average
     * plus a mutation in [-parents, +parents], limited to [0, 31].
     * <p><b>ZH:</b> 杂交后代的属性。{@code sum} 为各亲本属性之和，{@code roll} 为 [0, 2 × 亲本数] 内的随机值。
     * 结果为亲本平均值加上 [-亲本数, +亲本数] 的变异，并限制在 [0, 31]。
     * <p><b>JA:</b> 交配した子の特性値。{@code sum} は親の特性値の合計、{@code roll} は [0, 2 × 親の数] の乱数。
     * 結果は親の平均に [-親の数, +親の数] の変異を加え、[0, 31] に制限した値。
     */
    public static int inherit(int sum, int parents, int roll) {
        if (parents <= 0) return 0;
        return Math.clamp(sum / parents + roll - parents, 0, MAX_TRAIT);
    }

    /**
     * <p><b>EN:</b> Participation value of a parent. The parent joins a crossing when this value
     * is at least a random value in [0, 15]. High resistance (28 or more) lowers the value.
     * <p><b>ZH:</b> 亲本的参与值。该值不小于 [0, 15] 内的随机值时亲本参与杂交。抗性很高（28 及以上）会降低该值。
     * <p><b>JA:</b> 親の参加値。この値が [0, 15] の乱数以上なら親は交配に参加する。耐性が高い（28 以上）と値が下がる。
     */
    public static int participationBase(int growth, int resistance) {
        return 4 + (growth >= 16 ? 1 : 0) + (growth >= 30 ? 1 : 0) + (resistance >= 28 ? 27 - resistance : 0);
    }

    /**
     * <p><b>EN:</b> Biome part of the humidity. {@code downfall} and {@code temperature} come from
     * the biome climate.
     * <p><b>ZH:</b> 湿度中的生物群系部分。{@code downfall} 与 {@code temperature} 取自生物群系气候。
     * <p><b>JA:</b> 湿度のバイオーム成分。{@code downfall} と {@code temperature} はバイオームの気候値。
     */
    public static int biomeHumidity(float downfall, float temperature) {
        int rainfall = Math.clamp((int) (25f * downfall - 12.5), -10, 10);
        return rainfall + Math.clamp((int) (Math.abs(rainfall) * (-2 * temperature * temperature + 4 * temperature - 1)), -10, 10);
    }

    /**
     * <p><b>EN:</b> Humidity: biome part, +2 for wet farmland (moisture 7), +2 when the stored
     * water is 5 or more, and +1 for each started 25 units of stored water.
     * <p><b>ZH:</b> 湿度：生物群系部分；湿润耕地（湿度 7）+2；储水不少于 5 时 +2；每 25 单位储水（向上取整）+1。
     * <p><b>JA:</b> 湿度：バイオーム成分、湿った耕地（水分 7）で +2、貯水 5 以上で +2、貯水 25 単位ごと（切り上げ）に +1。
     */
    public static int humidity(int biomePart, boolean wetFarmland, int water) {
        return biomePart + (wetFarmland ? 2 : 0) + (water >= 5 ? 2 : 0) + (water + 24) / 25;
    }

    /** EN: Most dirt blocks under the farmland that count. ZH: 耕地下方最多计入的泥土方块数。JA: 耕地の下で数える土ブロックの最大数。 */
    public static final int MAX_DIRT_BONUS = 3;

    /**
     * <p><b>EN:</b> Soil nutrients: biome bonus, +1 for each dirt block directly under the
     * farmland (up to three, without a gap), and +1 for each started 20 units of stored nutrients.
     * <p><b>ZH:</b> 土壤营养：生物群系加成；耕地正下方连续的泥土方块每个 +1（最多 3 个）；每 20 单位储存营养（向上取整）+1。
     * <p><b>JA:</b> 土壌栄養：バイオーム補正、耕地の真下に連続する土ブロック 1 個ごとに +1（最大 3 個）、
     * 貯蔵栄養 20 単位ごと（切り上げ）に +1。
     */
    public static int soilNutrients(int biomeBonus, int dirtBelow, int nutrients) {
        return biomeBonus + Math.clamp(dirtBelow, 0, MAX_DIRT_BONUS) + (nutrients + 19) / 20;
    }

    /**
     * <p><b>EN:</b> Air quality: height part (0..2 from y = 40), half of the free neighbour cells
     * of the 2 x 2 area (start value 9), and +4 when the crop sees the sky.
     * <p><b>ZH:</b> 空气质量：高度部分（自 y = 40 起 0..2）、2 × 2 区域空闲格数的一半（初值 9）、能看到天空时 +4。
     * <p><b>JA:</b> 空気品質：高さ成分（y = 40 から 0..2）、2 × 2 範囲の空きセル数の半分（初期値 9）、空が見えれば +4。
     */
    public static int airQuality(int y, int fresh, boolean seesSky) {
        return Math.clamp((int) Math.floor((y - 40) / 15.0), 0, 2) + fresh / 2 + (seesSky ? 4 : 0);
    }

    /**
     * <p><b>EN:</b> Client-visible resource bucket. The crop sends an update to clients only when
     * a bucket changes, not on each one-unit decay. The buckets are the same steps that the
     * humidity and nutrient formulas use.
     * <p><b>ZH:</b> 客户端可见的资源档位。只有档位变化时作物才向客户端发送更新，而不是每减少 1 单位就发送。
     * 档位与湿度和营养公式使用的步长相同。
     * <p><b>JA:</b> クライアントに見える資源の段階。作物は段階が変わったときだけクライアントへ更新を送り、
     * 1 単位減るたびには送らない。段階は湿度・栄養の計算式と同じ刻み。
     */
    public static int resourceBucket(int water, int nutrients, int weedControl) {
        return ((water + 24) / 25) | (((nutrients + 19) / 20) << 8) | ((weedControl > 0 ? 1 : 0) << 16);
    }
}

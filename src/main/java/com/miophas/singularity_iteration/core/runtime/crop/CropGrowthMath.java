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
 *
 * <h2>Rules / 规则 / ルール (0.1.10.38)</h2>
 * <p><b>EN:</b> SI rules by default: Resistance lowers the requirement, each missing quality point costs 2.5 % of the growth
 * points (compounding), and Growth 24+ gives a gradual weed chance that Resistance reduces. With the server option
 * {@code Crops.strictIc2Rules} the formulas are IC2 Experimental's exactly. The overloads with a {@code strict} argument take the
 * rule set explicitly (a client shows the server's numbers with them); the others use the server setting.
 * <p><b>ZH:</b> 默认采用 SI 规则：抗性降低需求，每缺 1 点品质使生长点减少 2.5%（复合计算），生长 24 以上按渐进概率扩散杂草，抗性可降低该概率。
 * 开启服务器选项 {@code Crops.strictIc2Rules} 后公式与 IC2 Experimental 完全一致。带 {@code strict} 参数的重载显式指定规则（客户端用它显示服务器的数值），
 * 其余方法使用服务器设置。
 * <p><b>JA:</b> 既定は SI ルール：耐性は要求値を下げ、品質の不足 1 点ごとに成長ポイントが 2.5 % 減り（複利）、成長 24 以上は耐性で下がる
 * 段階的な確率で雑草を広げる。サーバー設定 {@code Crops.strictIc2Rules} を有効にすると IC2 Experimental と同じ式になる。{@code strict}
 * 引数付きのオーバーロードはルールを明示する（クライアントはこれでサーバーの数値を表示する）。それ以外はサーバー設定を使う。
 */
public final class CropGrowthMath {
    /** EN: Ticks between two crop cycles. ZH: 两个作物周期之间的 tick 数。JA: 作物サイクル間の tick 数。 */
    public static final int CYCLE_TICKS = 256;
    /** EN: Highest value of a trait (growth, gain, resistance). ZH: 属性（生长、产量、抗性）的最大值。JA: 特性値（成長・収量・耐性）の最大値。 */
    public static final int MAX_TRAIT = 31;
    /** EN: Penalty above this value can kill the plant. ZH: 惩罚值超过此值时植物可能死亡。JA: ペナルティがこの値を超えると植物が枯れることがある。 */
    public static final int LETHAL_PENALTY = 100;

    private CropGrowthMath() {}

    private static volatile boolean strictIc2;

    /** EN: 0.1.10.38: sets the rule set (server option Crops.strictIc2Rules). ZH: 0.1.10.38：设置规则（服务器选项 Crops.strictIc2Rules）。JA: 0.1.10.38：ルールを設定する（サーバー設定 Crops.strictIc2Rules）。 */
    public static void setStrictIc2Rules(boolean strict) {
        strictIc2 = strict;
    }

    /** EN: True when the strict IC2 Experimental rules apply. ZH: 采用严格 IC2 Experimental 规则时为真。JA: 厳密な IC2 Experimental ルールのとき真。 */
    public static boolean strictIc2Rules() {
        return strictIc2;
    }

    /** EN: Penalty for each missing quality point (SI / IC2). ZH: 每缺 1 点品质的惩罚（SI / IC2）。JA: 品質不足 1 点あたりのペナルティ（SI / IC2）。 */
    public static final int PENALTY_PER_POINT = 2, IC2_PENALTY_PER_POINT = 4;
    /** EN: Growth factor for each missing quality point under SI rules (-2.5 %, compounding). ZH: SI 规则下每缺 1 点品质的生长系数（-2.5%，复合）。JA: SI ルールで品質不足 1 点ごとの成長係数（-2.5 %、複利）。 */
    public static final double GROWTH_PER_MISSING_POINT = 0.975;

    /**
     * <p><b>EN:</b> Minimum environment quality that the plant needs for full growth, with the server's rules.
     * <p><b>ZH:</b> 植物完全生长所需的最低环境品质（服务器规则）。
     * <p><b>JA:</b> 植物が通常どおり成長するために必要な最低環境品質（サーバーのルール）。
     */
    public static int requiredQuality(int tier, int growth, int gain, int resistance) {
        return requiredQuality(tier, growth, gain, resistance, strictIc2);
    }

    /**
     * <p><b>EN:</b> Requirement: (tier - 1) x 4 + growth + gain, minus the resistance (SI) or plus it (IC2). Under SI rules a
     * maxed 31/31/31 seed needs 62 less quality than in IC2, so it can be planted in an ordinary field.
     * <p><b>ZH:</b> 需求：（等级 - 1）× 4 ＋ 生长 ＋ 产量，减去抗性（SI）或加上抗性（IC2）。SI 规则下满属性 31/31/31 的种子比 IC2 少需 62 点品质，
     * 普通田地即可种植。
     * <p><b>JA:</b> 要求値：（ティア - 1）× 4 ＋ 成長 ＋ 収量 から耐性を引く（SI）か足す（IC2）。SI ルールでは 31/31/31 の種は IC2 より
     * 品質 62 点少なくて済み、普通の畑に植えられる。
     */
    public static int requiredQuality(int tier, int growth, int gain, int resistance, boolean strict) {
        return Math.max(0, (tier - 1) * 4 + growth + gain + (strict ? resistance : -resistance));
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
     * <p><b>EN:</b> Penalty when the quality is too low: two points (SI) or four points (IC2) for each missing quality
     * point. The result is 0 when the quality is sufficient. Above {@link #LETHAL_PENALTY} the crop can die.
     * <p><b>ZH:</b> 品质不足时的惩罚：每缺少 1 点品质记 2 点（SI）或 4 点（IC2）。品质足够时结果为 0。超过 {@link #LETHAL_PENALTY} 时作物可能死亡。
     * <p><b>JA:</b> 品質不足時のペナルティ：不足 1 点につき 2 点（SI）または 4 点（IC2）。品質が十分なら 0。{@link #LETHAL_PENALTY} を超えると枯れることがある。
     */
    public static int penalty(int required, int quality) {
        return penalty(required, quality, strictIc2);
    }

    public static int penalty(int required, int quality, boolean strict) {
        return quality >= required ? 0 : (required - quality) * (strict ? IC2_PENALTY_PER_POINT : PENALTY_PER_POINT);
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
        return growthPoints(base, required, quality, strictIc2);
    }

    /**
     * <p><b>EN:</b> 0.1.10.38: below the requirement, SI rules multiply the points by 0.975 for each missing point (IC2: minus the
     * penalty in percent, 0 at 25 missing points). Gain raises the drops by 3 % per point and the requirement by 1, Growth raises
     * the base by more than 2.5 %, so neither lowers the expected yield per hour any more.
     * <p><b>ZH:</b> 0.1.10.38：低于需求时，SI 规则每缺 1 点将生长点乘以 0.975（IC2：减去惩罚百分比，缺 25 点时为 0）。产量每点使掉落 +3%、需求 +1，
     * 生长使基础值增加超过 2.5%，因此两者都不会再降低每小时期望产量。
     * <p><b>JA:</b> 0.1.10.38：要求値を下回ると、SI ルールでは不足 1 点ごとにポイントを 0.975 倍する（IC2：ペナルティの % を引き、不足 25 点で 0）。
     * 収量は 1 点ごとにドロップ +3 %・要求値 +1、成長は基礎値を 2.5 % 以上増やすので、どちらも 1 時間あたりの期待収穫を下げなくなった。
     */
    public static int growthPoints(int base, int required, int quality, boolean strict) {
        if (quality >= required) return base * (100 + quality - required) / 100;
        if (strict) return Math.max(0, base * (100 - penalty(required, quality, true)) / 100);
        return (int) (base * Math.pow(GROWTH_PER_MISSING_POINT, required - quality));
    }

    /** EN: Growth from which a crop can spread weeds (SI) or is a weed source (IC2). ZH: 作物可能扩散杂草（SI）或成为杂草源（IC2）的生长值。JA: 作物が雑草を広げうる（SI）か雑草源になる（IC2）成長値。 */
    public static final int WEED_GROWTH = 24;

    /**
     * <p><b>EN:</b> 0.1.10.38: chance in percent per cycle that a crop of stage 2 or more spreads weeds: 4 for each growth point
     * from 24 (4 % at 24, 32 % at 31), minus the resistance. Under IC2 rules it is 0: such a crop is a weed source instead
     * ({@code Ic2PlantType.isWeed}), cannot be harvested and spreads every cycle with a high chance.
     * <p><b>ZH:</b> 0.1.10.38：阶段 2 及以上的作物每周期扩散杂草的概率（百分比）：生长自 24 起每点 4（24 时 4%，31 时 32%），减去抗性。
     * IC2 规则下为 0：此类作物直接成为杂草源（{@code Ic2PlantType.isWeed}），无法收获，且每周期高概率扩散。
     * <p><b>JA:</b> 0.1.10.38：段階 2 以上の作物が 1 サイクルに雑草を広げる確率（%）：成長 24 から 1 点ごとに 4（24 で 4 %、31 で 32 %）、
     * そこから耐性を引く。IC2 ルールでは 0：その作物は雑草源になり（{@code Ic2PlantType.isWeed}）、収穫できず、毎サイクル高い確率で広がる。
     */
    public static int weedChance(int growth, int resistance) {
        return weedChance(growth, resistance, strictIc2);
    }

    public static int weedChance(int growth, int resistance, boolean strict) {
        if (strict || growth < WEED_GROWTH) return 0;
        return Math.max(0, (growth - WEED_GROWTH + 1) * 4 - resistance);
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

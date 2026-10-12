// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.crop;

/**
 * <p><b>EN:</b> Cost and time rules of the Advanced Crop Scanner (0.1.8.2). Pure functions; no Minecraft
 * dependency, so a standalone test checks them.
 * <ul>
 *   <li>Overclocker (IC2 rule): each card multiplies the time by 0.7 and the power by 1.6. The
 *       energy of one job thus rises by 1.12 for each card. At most {@link #MAX_OVERCLOCKERS} count.</li>
 *   <li>Analysis: a full scan (level 0 to 4) of one slot takes {@link #ANALYSIS_TICKS} ticks and
 *       {@link #ANALYSIS_EU_PER_BAG} EU for each bag in the stack. All slots scan in parallel.</li>
 *   <li>Breeding research: {@code 40,000 × tier^1.6 × (1 + 3 × (1 − share))} EU at 1,024 EU/t, where
 *       share is the natural crossing chance of the target. A rare target costs up to four times more.</li>
 *   <li>Gene editing: raising a trait to value v costs {@code 1,000 × 1.25^v} EU for each step; lowering
 *       costs {@link #GENE_LOWER_EU} EU for each step. Power 2,048 EU/t. One UU-Matter for each three
 *       raised points (catalyst).</li>
 * </ul>
 * <p><b>ZH:</b> 次世代作物扫描仪（0.1.8.2）的成本与时间规则。纯函数，不依赖 Minecraft，可由独立测试检查。
 * <ul>
 *   <li>超频升级（IC2 规则）：每张使时间 ×0.7、功率 ×1.6，因此每个任务的能量每张 ×1.12。最多计入 {@link #MAX_OVERCLOCKERS} 张。</li>
 *   <li>分析：一个槽位的完整扫描（等级 0 到 4）耗时 {@link #ANALYSIS_TICKS} tick，堆中每个种子袋消耗 {@link #ANALYSIS_EU_PER_BAG} EU。所有槽位并行扫描。</li>
 *   <li>交配研究：{@code 40,000 × 等级^1.6 × (1 + 3 × (1 − 占比))} EU，功率 1,024 EU/t；占比为目标在自然杂交中的概率，稀有目标最多贵 4 倍。</li>
 *   <li>基因编辑：把属性提升到 v 的每一步耗 {@code 1,000 × 1.25^v} EU；降低每一步 {@link #GENE_LOWER_EU} EU。功率 2,048 EU/t。每提升 3 点需要 1 个 UU 物质（催化剂）。</li>
 * </ul>
 * <p><b>JA:</b> 次世代型作物スキャナー（0.1.8.2）のコストと時間の規則。純粋関数で Minecraft に依存しないので、単体テストで検査できる。
 * <ul>
 *   <li>オーバークロッカー（IC2 の規則）：1 枚ごとに時間 ×0.7、電力 ×1.6。1 件の作業のエネルギーは 1 枚ごとに ×1.12 になる。最大 {@link #MAX_OVERCLOCKERS} 枚まで数える。</li>
 *   <li>分析：1 スロットの完全スキャン（段階 0 から 4）は {@link #ANALYSIS_TICKS} tick、スタック内の種子袋 1 個につき {@link #ANALYSIS_EU_PER_BAG} EU。全スロットが並行して走査する。</li>
 *   <li>交配研究：{@code 40,000 × ティア^1.6 × (1 + 3 × (1 − 割合))} EU、電力 1,024 EU/t。割合は目標の自然交配での確率で、稀な目標は最大 4 倍のコスト。</li>
 *   <li>遺伝子改変：特性値を v へ上げる 1 段ごとに {@code 1,000 × 1.25^v} EU、下げる 1 段ごとに {@link #GENE_LOWER_EU} EU。電力 2,048 EU/t。
 *       上げた 3 点ごとに UU 物質 1 個（触媒）。</li>
 * </ul>
 */
public final class ScannerMath {
    /** EN: Most overclockers that count. ZH: 最多计入的超频升级数。JA: 数に入るオーバークロッカーの最大数。 */
    public static final int MAX_OVERCLOCKERS = 8;
    /** EN: Ticks of a full scan without overclockers. ZH: 无超频时完整扫描的 tick 数。JA: オーバークロックなしの完全スキャンの tick 数。 */
    public static final int ANALYSIS_TICKS = 40;
    /** EN: EU of a full scan for one bag. ZH: 一个种子袋完整扫描的 EU。JA: 種子袋 1 個の完全スキャンの EU。 */
    public static final long ANALYSIS_EU_PER_BAG = 500;
    /** EN: Base power of breeding research (EU/t). ZH: 交配研究的基础功率（EU/t）。JA: 交配研究の基本電力（EU/t）。 */
    public static final long BREED_POWER = 1024;
    /** EN: Base power of gene editing (EU/t). ZH: 基因编辑的基础功率（EU/t）。JA: 遺伝子改変の基本電力（EU/t）。 */
    public static final long GENE_POWER = 2048;
    /** EN: EU for each lowered trait point. ZH: 每降低 1 点属性的 EU。JA: 特性値を 1 点下げる EU。 */
    public static final long GENE_LOWER_EU = 2000;
    /** EN: Raised trait points for one UU-Matter. ZH: 每个 UU 物质对应的提升点数。JA: UU 物質 1 個あたりの上昇点数。 */
    public static final int POINTS_PER_UU = 3;

    private ScannerMath() {}

    /** EN: Overclockers that count (0..MAX). ZH: 计入的超频数（0..上限）。JA: 数に入るオーバークロッカー数（0..上限）。 */
    public static int overclockers(int cards) {
        return Math.clamp(cards, 0, MAX_OVERCLOCKERS);
    }

    /** EN: Time factor 0.7^n. ZH: 时间系数 0.7^n。JA: 時間係数 0.7^n。 */
    public static double timeFactor(int cards) {
        return Math.pow(0.7, overclockers(cards));
    }

    /** EN: Power factor 1.6^n. ZH: 功率系数 1.6^n。JA: 電力係数 1.6^n。 */
    public static double powerFactor(int cards) {
        return Math.pow(1.6, overclockers(cards));
    }

    /** EN: Ticks of a full scan with n overclockers (at least 1). ZH: n 张超频时完整扫描的 tick 数（至少 1）。JA: n 枚時の完全スキャンの tick 数（最低 1）。 */
    public static int analysisTicks(int cards) {
        return Math.max(1, (int) Math.ceil(ANALYSIS_TICKS * timeFactor(cards)));
    }

    /** EN: EU per tick to scan a stack of {@code count} bags. ZH: 扫描 {@code count} 个种子袋每 tick 的 EU。JA: {@code count} 個の種子袋を走査する 1 tick あたりの EU。 */
    public static long analysisEuPerTick(int count, int cards) {
        return Math.max(1, (long) Math.ceil(ANALYSIS_EU_PER_BAG * Math.max(1, count) / (double) ANALYSIS_TICKS * powerFactor(cards)));
    }

    /**
     * <p><b>EN:</b> EU of breeding research for a target of {@code tier} with natural share {@code share} (0..1].
     * <p><b>ZH:</b> 目标等级 {@code tier}、自然占比 {@code share}（0..1]）的交配研究 EU。
     * <p><b>JA:</b> ティア {@code tier}、自然割合 {@code share}（0..1]）の目標に対する交配研究の EU。
     */
    public static long breedingCost(int tier, double share) {
        double s = Math.clamp(share, 0.0, 1.0);
        return Math.round(40_000 * Math.pow(Math.max(1, tier), 1.6) * (1 + 3 * (1 - s)));
    }

    /** EN: Ticks of a research job of {@code cost} EU at {@code power} EU/t with n overclockers. ZH: 能量 {@code cost}、功率 {@code power} 的研究在 n 张超频下的 tick 数。JA: {@code cost} EU・{@code power} EU/t の研究の n 枚時の tick 数。 */
    public static int researchTicks(long cost, long power, int cards) {
        return Math.max(1, (int) Math.ceil(cost / (double) power * timeFactor(cards)));
    }

    /** EN: EU per tick of a research job with n overclockers. ZH: n 张超频时研究每 tick 的 EU。JA: n 枚時の研究の 1 tick あたりの EU。 */
    public static long researchEuPerTick(long power, int cards) {
        return (long) Math.ceil(power * powerFactor(cards));
    }

    /**
     * <p><b>EN:</b> Total EU of a research job with n overclockers: {@code ceil(cost × 1.6^n × 0.7^n)} (cost × 1.12^n).
     * The last tick takes only the rest ({@link #tickCharge}), so a short job does not pay for a full last tick.
     * <p><b>ZH:</b> n 张超频时研究任务的总 EU：{@code ceil(cost × 1.6^n × 0.7^n)}（cost × 1.12^n）。最后一个 tick 只扣剩余部分（{@link #tickCharge}），短任务不会为完整的最后一个 tick 付费。
     * <p><b>JA:</b> n 枚時の研究作業の総 EU：{@code ceil(cost × 1.6^n × 0.7^n)}（cost × 1.12^n）。最後の tick は残りだけを取る（{@link #tickCharge}）ので、短い作業が最後の 1 tick 分を丸ごと払うことはない。
     */
    public static long researchTotalEu(long cost, long power, int cards) {
        if (cost <= 0) return 0;
        return jobTotal((long) Math.ceil(cost * powerFactor(cards) * timeFactor(cards)), researchEuPerTick(power, cards), researchTicks(cost, power, cards));
    }

    /** EN: Total EU of analyzing a stack of {@code count} bags with n overclockers (500 × count × 1.12^n). ZH: n 张超频时分析 {@code count} 个种子袋的总 EU（500 × count × 1.12^n）。JA: n 枚時に {@code count} 個の種子袋を分析する総 EU（500 × count × 1.12^n）。 */
    public static long analysisTotalEu(int count, int cards) {
        long ideal = (long) Math.ceil(ANALYSIS_EU_PER_BAG * Math.max(1, count) * powerFactor(cards) * timeFactor(cards));
        return jobTotal(ideal, analysisEuPerTick(count, cards), analysisTicks(cards));
    }

    // EN: The total is at least (ticks - 1) full ticks and at most all full ticks.
    // ZH: 总量至少为 (ticks - 1) 个完整 tick，至多为全部完整 tick。
    // JA: 総量は少なくとも (ticks - 1) 回分の満額、多くても全 tick の満額。
    private static long jobTotal(long ideal, long perTick, int ticks) {
        return Math.clamp(ideal, perTick * (ticks - 1L) + 1, perTick * (long) ticks);
    }

    /**
     * <p><b>EN:</b> EU taken in tick {@code tick} (1-based) of a job with {@code ticks} ticks: full power, and the rest in the last tick.
     * <p><b>ZH:</b> 共 {@code ticks} 个 tick 的任务中第 {@code tick} 个 tick（从 1 开始）扣除的 EU：满功率，最后一个 tick 扣剩余部分。
     * <p><b>JA:</b> {@code ticks} tick の作業の第 {@code tick} tick（1 始まり）に取る EU：満額、最後の tick は残り。
     */
    public static long tickCharge(long total, long perTick, int tick, int ticks) {
        if (tick < ticks) return perTick;
        return Math.max(0, Math.min(perTick, total - perTick * (ticks - 1L)));
    }

    /**
     * <p><b>EN:</b> EU to change one trait from {@code from} to {@code to} (0..31).
     * <p><b>ZH:</b> 把一项属性从 {@code from} 改为 {@code to}（0..31）的 EU。
     * <p><b>JA:</b> 1 つの特性値を {@code from} から {@code to}（0..31）へ変える EU。
     */
    public static long geneStepCost(int from, int to) {
        int a = Math.clamp(from, 0, 31), b = Math.clamp(to, 0, 31);
        if (b < a) return (a - b) * GENE_LOWER_EU;
        double sum = 0;
        for (int v = a + 1; v <= b; v++) sum += 1000 * Math.pow(1.25, v);
        return Math.round(sum);
    }

    /** EN: EU to change all three traits. ZH: 修改三项属性的 EU。JA: 3 つの特性値を変える EU。 */
    public static long geneCost(int[] current, int[] target) {
        long total = 0;
        for (int i = 0; i < 3; i++) total += geneStepCost(current[i], target[i]);
        return total;
    }

    /** EN: UU-Matter catalyst for the change (raised points / 3, rounded up). ZH: 该修改所需的 UU 物质催化剂（提升点数 / 3，向上取整）。JA: 変更に要る UU 物質の触媒（上昇点数 / 3、切り上げ）。 */
    public static int geneCatalyst(int[] current, int[] target) {
        int raised = 0;
        for (int i = 0; i < 3; i++) raised += Math.max(0, Math.clamp(target[i], 0, 31) - Math.clamp(current[i], 0, 31));
        return (raised + POINTS_PER_UU - 1) / POINTS_PER_UU;
    }

    /** EN: Trait of a researched seed: the floor of the parent average (no mutation). ZH: 研究所得种子的属性：亲本平均值向下取整（无变异）。JA: 研究で得る種子の特性値：親の平均の切り捨て（変異なし）。 */
    public static int breedTrait(int a, int b) {
        return Math.clamp((a + b) / 2, 0, 31);
    }
}

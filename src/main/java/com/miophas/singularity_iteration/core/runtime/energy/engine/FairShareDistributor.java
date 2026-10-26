// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p><b>EN:</b> Deterministic fair-share distributor for the smart grid mode.
 *
 * <p>This class replaces the packet priority walk of {@link FractionalDistributor}
 * when {@link IndependentEnergyMode#fairDistribution()} is on. It does not use
 * a random number. It does not depend on the order of receivers. It gives the
 * same result for the same input.
 *
 * <h2>Why this class exists</h2>
 * <p>The packet priority walk gives one packet to the receivers in one order.
 * The first receiver takes all the energy that it can hold. The next receivers
 * get only the remainder. Each delivery also pays the full path loss in whole
 * EU. Thus receivers far from the source, or late in the order, got less
 * energy. Players saw this as 100 %, 75 %, 50 % power on machines in one line.
 * This class divides the energy without an order. Since 0.1.10.12 it uses a
 * max-min share (see below); until then the share was in proportion to the demand.
 *
 * <h2>Model</h2>
 * <ul>
 *   <li><b>Source:</b> a budget (EU that the source can give in this round), a
 *       packet size (the voltage of the source), and a class. Class 0 is a
 *       generator or a relay (a transformer). Class 1 is a storage block.</li>
 *   <li><b>Receiver:</b> a room (free capacity), a rated input for this round
 *       ({@code null} for no limit), and a class. Class 0 is a load or a relay.
 *       Class 1 is a storage block.</li>
 *   <li><b>Domain:</b> one group of routes, as in {@link DomainDistributor.Domain}.
 *       This class reads the reach and the loss of each route. It ignores the
 *       receiver priorities of the domain.</li>
 *   <li><b>Shared storage:</b> a port that is a source and a receiver at the
 *       same time. Its debit increases its own room. It never sends energy to
 *       itself.</li>
 * </ul>
 *
 * <h2>Stages</h2>
 * <p>The round has four stages, in this order:
 * <ol>
 *   <li>Priority demand: the rated demand {@code min(room, rated input)} of the
 *       class 0 receivers, and the relay demand {@code min(room, rated input,
 *       relay)} of the class 1 receivers (0.1.10.12).</li>
 *   <li>Class 0 receivers, surplus demand: the remaining room.</li>
 *   <li>Class 1 receivers, rated demand.</li>
 *   <li>Class 1 receivers, surplus demand.</li>
 * </ol>
 * <p>In each stage, the sources give energy in this order: class 0 sources,
 * then class 1 sources. In one class, the order starts at index
 * {@code rotation mod count} and wraps. Each source divides its remaining
 * budget among the receivers that it can reach by max-min share (0.1.10.12;
 * before: in proportion to the demand). When the budget is sufficient, each
 * receiver gets its full demand.
 *
 * <p><b>Result:</b> loads and relays get energy before storage. A storage
 * block gives energy only for demand that the generators cannot supply. The
 * surplus of the generators charges the storage blocks.
 *
 * <h2>0.1.10.12: max-min share and storage relays</h2>
 * <ul>
 *   <li><b>Max-min share (water filling).</b> In each stage a source gives every
 *       receiver the same amount, up to the demand of the receiver. The budget
 *       that a small demand does not use goes to the larger demands. Before, the
 *       split was in proportion to the demand: a machine with a demand of 32 EU
 *       next to a Molecular Transformer with a demand of 120,000,000 EU got almost
 *       nothing. Now a small demand is filled first. Receivers with equal demand
 *       still get equal shares.</li>
 *   <li><b>Relay demand of storage.</b> A storage receiver can declare a relay
 *       demand ({@link Receiver#relay()}): the EU that it needs to send its next
 *       tick of output to the machines behind it. Stage 1 serves this demand
 *       together with the rated demand of the loads. Before, a storage block
 *       that feeds a branch of machines got energy only after every load that
 *       the source reached directly was full. Thus the whole branch starved.</li>
 * </ul>
 *
 * <h2>0.1.10.22: merit order and load tiers</h2>
 * <ul>
 *   <li><b>Rank of a source</b> ({@link Source#rank()}): the visit order of the sources in each
 *       stage, from rank 0 up. The engine gives must-run generators and relays rank 0. In merit order,
 *       fuel generators come next and storage last; in economic dispatch, storage comes before fuel.
 *       The rotation works inside one rank. With rank = class, the order is the one before.</li>
 *   <li><b>Charging of storage</b> ({@link Source#chargesStorage()}): a source without it gives
 *       nothing in the two storage stages (economic dispatch: fuel does not charge storage).</li>
 *   <li><b>Tier of a receiver</b> ({@link Receiver#tier()}, 0 critical, 1 normal, 2 deferrable):
 *       each stage runs once per tier, the critical tier first. Thus a shortage stops the deferrable
 *       loads first and the critical loads last (lexicographic max-min). The relay demand of a storage
 *       receiver has the tier of that receiver. With only normal receivers, the stages are the four
 *       stages before.</li>
 * </ul>
 *
 * <h2>Line loss</h2>
 * <ul>
 *   <li>{@link LineLoss#LOSSLESS}: a delivery has no loss. This is the default.</li>
 *   <li>{@link LineLoss#PROPORTIONAL}: the loss is
 *       {@code credit * lossMilli / (1000 * packet)}. A higher voltage thus
 *       has a lower loss fraction. A route whose loss fraction is 1 or more
 *       cannot deliver.</li>
 * </ul>
 *
 * <h2>Arithmetic</h2>
 * <p>The planner works in fixed-point {@code long} values with
 * {@value #COARSE_BITS} fraction bits (2<sup>-24</sup> EU). It reads each
 * budget and room rounded down to this grid, so it never gives more than the
 * real budget or room. The level of the share with line loss is a
 * {@code double}. Each
 * credit is capped by the remaining budget of its source, so a rounding error
 * cannot overspend. Conservation is exact: each debit is the sum of its credit
 * and its loss in the same {@code long} grid. The method converts the result
 * to {@link EnergyAmount} without loss of precision (a left shift).
 * Budgets and rooms above 2<sup>38</sup> EU are clamped to 2<sup>38</sup> EU.
 * One round can never move more than that.
 *
 * <h2>Cost</h2>
 * <p>The method visits each linked (source, receiver) pair of the route table a
 * fixed number of times: once to select the best route, and twice per stage.
 * The max-min share sorts the demands of one pool in each stage (0.1.10.12).
 * Thus the cost is {@code O(S * R log R)}, where S is the number of sources
 * and R is the number of receivers in the window. There is no wide integer
 * arithmetic in the loops.
 * The engine limits a window to 32 sources and 128 receivers.
 *
 * <h2>Invariants</h2>
 * <ul>
 *   <li>Conservation: the sum of debits equals the sum of credits plus the
 *       loss. The method throws {@link IllegalStateException} when this is
 *       false.</li>
 *   <li>A source never gives more than its budget.</li>
 *   <li>A receiver never gets more than its room at the start of the round,
 *       plus the energy that it gives as a shared storage source.</li>
 *   <li>The inputs are not changed. No world state is read or written.</li>
 * </ul>
 *
 * <p><b>ZH:</b> 智能电网模式的确定性公平分配器。
 * <ul>
 *   <li><b>原因：</b>旧的数据包优先级遍历按固定顺序把一个包交给接收方，第一个接收方取走能容纳的全部能量，后面的只得到剩余部分，
 *       且每次输送都按整 EU 支付完整路径损耗。因此同一条线上的机器出现 100 %、75 %、50 % 的逐级衰减。本类不按顺序分配，
 *       0.1.10.12 起按最大最小均分（此前按需求比例）。不使用随机数，不依赖接收方顺序，相同输入得到相同结果。</li>
 *   <li><b>模型：</b>源 = 本轮预算、包大小（电压）、类别（0 = 发电机或变压器中继，1 = 储能）；接收方 = 剩余容量、本轮额定输入
 *       （{@code null} 为无限制）、类别（0 = 用电器或中继，1 = 储能）；域 = 一组路由（只读取可达性与损耗，忽略优先级）；
 *       共享储能 = 同时为源与接收方的端口，其放出的能量会增加自身容量，且从不向自己供电。</li>
 *   <li><b>四个阶段：</b>优先需求（类别 0 的额定需求 {@code min(容量, 额定输入)} 与类别 1 的中继需求，0.1.10.12）→ 类别 0 的剩余容量 → 类别 1 的额定需求 → 类别 1 的剩余容量。
 *       每个阶段内先由类别 0 的源供电，再由类别 1 的源供电；同类内从 {@code rotation mod 数量} 开始循环。每个源把剩余预算按
 *       最大最小均分分给可达的接收方（0.1.10.12 之前按需求比例）；预算足够时每个接收方得到全部需求。结果：用电器、中继与为机器供电的储能先于其他储能得到能量；储能只补足发电机
 *       供不上的部分；发电机的盈余为储能充电。</li>
 *   <li><b>0.1.10.12：</b>（1）最大最小均分（注水法）：每个阶段内，源给每个接收方相同的电量，以其需求为上限，小需求用不完的预算分给大需求。
 *       此前按需求比例分配，需求 32 EU 的机器与需求 1.2 亿 EU 的分子重组仪同时存在时，前者几乎分不到电。需求相同的接收方仍得到相同份额。
 *       （2）储能的中继需求：储能接收方可以声明中继需求（{@link Receiver#relay()}），即向其后方机器送出下一 tick 输出所需的电量；
 *       第 1 阶段与用电器的额定需求一起满足它。此前为一组机器供电的储能，要等源直接连接的所有用电器都充满后才能得到电，整个支路因此断电。</li>
 *   <li><b>0.1.10.22：</b>（1）源的次序（{@link Source#rank()}）：每个阶段内按次序从 0 起访问源。引擎给必须运行的发电机与中继次序 0；
 *       经济次序下燃料发电机其次、储能最后；经济调度下储能在燃料之前。轮转只在同一次序内进行。次序等于类别时与此前相同。
 *       （2）储能充电（{@link Source#chargesStorage()}）：不充电的源在两个储能阶段不供电（经济调度：燃料不为储能充电）。
 *       （3）接收方的优先级（{@link Receiver#tier()}，0 重要、1 普通、2 可延后）：每个阶段按优先级各执行一次，重要的先。
 *       因此电力不足时先停可延后的负荷，最后才停重要负荷（字典序最大最小）。储能接收方的中继需求使用该接收方的优先级。
 *       只有普通接收方时，阶段与此前的四个阶段相同。</li>
 *   <li><b>线损：</b>{@link LineLoss#LOSSLESS} 无损（默认）；{@link LineLoss#PROPORTIONAL} 损耗 = {@code credit * lossMilli / (1000 * packet)}，
 *       电压越高损耗比例越低；损耗比例不小于 1 的路由无法输送。</li>
 *   <li><b>算术：</b>以 {@value #COARSE_BITS} 位小数的定点 {@code long}（2<sup>-24</sup> EU）规划；预算与容量向下取整，绝不超发。
 *       带线损的均分水位为 {@code double}，每笔输送受源剩余预算限制，舍入误差不会超支。守恒是精确的：每笔扣款 = 输送量 + 损耗（同一网格）。
 *       单个预算或容量超过 2<sup>38</sup> EU 时被截断。</li>
 *   <li><b>开销：</b>每个已连接的（源、接收方）对访问固定次数（选路 1 次，每阶段 2 次），最大最小均分在每个阶段对一个池的需求排序（0.1.10.12），即 {@code O(S * R log R)}；循环中无宽整数运算。
 *       引擎把一个窗口限制为 32 个源与 128 个接收方。</li>
 *   <li><b>不变量：</b>扣款总和 = 输送总和 + 损耗（否则抛出 {@link IllegalStateException}）；源不会超出预算；接收方不会超过本轮开始时的容量
 *       （加上其作为共享储能放出的能量）；不修改输入，不读写世界状态。</li>
 * </ul>
 *
 * <p><b>JA:</b> スマート電力網モードの決定的な公平分配器。
 * <ul>
 *   <li><b>理由：</b>旧来のパケット優先度方式は、1 つのパケットを決まった順で受信側に渡していた。最初の受信側が入るだけ取り、後の受信側は
 *       残りしか得られず、しかも毎回の送電で経路損失を整数 EU で丸ごと払っていた。そのため一列に並んだ機械が 100 %、75 %、50 % と
 *       減衰した。このクラスは順序なしで分け、0.1.10.12 からは最大最小の均等配分を使う（以前は需要比例）。乱数を使わず、受信側の順序に依存せず、
 *       同じ入力から同じ結果を出す。</li>
 *   <li><b>モデル：</b>供給元 = 今回の予算、パケット量（電圧）、種別（0 = 発電機または変圧器の中継、1 = 蓄電）。受信側 = 空き容量、今回の定格入力
 *       （{@code null} は無制限）、種別（0 = 消費機器または中継、1 = 蓄電）。ドメイン = 経路のまとまり（到達性と損失だけを読み、優先度は無視）。
 *       共有蓄電 = 供給元と受信側を兼ねるポート。放出した分だけ自分の空きが増え、自分自身には送らない。</li>
 *   <li><b>4 段階：</b>優先需要（種別 0 の定格需要 {@code min(空き, 定格入力)} と種別 1 の中継需要、0.1.10.12）→ 種別 0 の残りの空き → 種別 1 の定格需要 → 種別 1 の残りの空き。
 *       各段階では種別 0 の供給元、次に種別 1 の供給元の順に送る。同じ種別では {@code rotation mod 数} から巡回する。各供給元は残り予算を、
 *       到達できる受信側へ最大最小の均等配分で分ける（0.1.10.12 より前は需要比例）。予算が足りれば各受信側は需要を全部得る。結果：消費機器・中継・機械へ給電する蓄電が、ほかの蓄電より先に電力を得る。
 *       蓄電は発電機で足りない分だけを出し、発電機の余剰が蓄電を充電する。</li>
 *   <li><b>0.1.10.12：</b>(1) 最大最小の均等配分（水位法）：各段階で、供給元は各受信側に同じ量を、その需要を上限として渡す。
 *       小さな需要が使い切らない予算は大きな需要に回る。以前は需要比例だったため、需要 32 EU の機械と需要 1.2 億 EU の分子再構築機が
 *       並ぶと、前者はほとんど電力を得られなかった。需要が同じ受信側は今も同じ量を得る。(2) 蓄電の中継需要：蓄電の受信側は中継需要
 *       （{@link Receiver#relay()}）を申告できる。後ろの機械へ次の tick の出力を送るのに必要な量で、第 1 段階で消費機器の定格需要と
 *       一緒に満たす。以前は、機械群に給電する蓄電は、供給元が直接つながる消費機器がすべて満タンになるまで電力を得られず、系統全体が止まった。</li>
 *   <li><b>0.1.10.22：</b>(1) 供給元の順位（{@link Source#rank()}）：各段階で順位 0 から供給元を訪れる。エンジンは必ず使う発電機と中継に順位 0 を与える。
 *       メリットオーダーでは燃料発電機が次、蓄電が最後。経済給電では蓄電が燃料より先。巡回は同じ順位の中で行う。順位が種別と同じなら以前と同じ順。
 *       (2) 蓄電の充電（{@link Source#chargesStorage()}）：充電しない供給元は 2 つの蓄電段階で何も出さない（経済給電：燃料で蓄電を充電しない）。
 *       (3) 受信側の優先度（{@link Receiver#tier()}、0 重要、1 通常、2 後回し）：各段階を優先度ごとに 1 回ずつ、重要から行う。
 *       電力不足では後回しの負荷から止まり、重要な負荷は最後まで止まらない（辞書式の最大最小）。蓄電の受信側の中継需要は、その受信側の優先度を使う。
 *       通常の受信側だけなら、段階は以前の 4 段階と同じ。</li>
 *   <li><b>線路損失：</b>{@link LineLoss#LOSSLESS} は損失なし（既定）。{@link LineLoss#PROPORTIONAL} は損失 = {@code credit * lossMilli / (1000 * packet)}。
 *       電圧が高いほど損失の割合は小さい。損失の割合が 1 以上の経路は送電できない。</li>
 *   <li><b>演算：</b>小数 {@value #COARSE_BITS} ビットの固定小数点 {@code long}（2<sup>-24</sup> EU）で計画する。予算と空きは切り捨てるので、
 *       実際より多く渡すことはない。線路損失ありの均等配分の水位は {@code double}。各送電量は供給元の残り予算で上限をかけるので、丸め誤差で使い過ぎない。
 *       保存則は厳密：各引き落とし = 送電量 + 損失（同じ格子）。1 つの予算・空きが 2<sup>38</sup> EU を超えると切り詰める。</li>
 *   <li><b>コスト：</b>接続済みの（供給元、受信側）の組を決まった回数（経路選択 1 回、各段階 2 回）だけ訪れる。
 *       最大最小の均等配分は各段階で 1 つのプールの需要を並べ替える（0.1.10.12）ので {@code O(S * R log R)}。ループ内に多倍長演算はない。エンジンは 1 つのウィンドウを供給元 32、受信側 128 に制限する。</li>
 *   <li><b>不変条件：</b>引き落とし合計 = 送電合計 + 損失（違えば {@link IllegalStateException}）。供給元は予算を超えない。受信側は今回開始時の
 *       空き（＋共有蓄電として出した分）を超えない。入力を変えず、ワールドの状態を読み書きしない。</li>
 * </ul>
 */
public final class FairShareDistributor {
    private FairShareDistributor() { }

    /** Fraction bits of the planning grid. */
    public static final int COARSE_BITS = 24;
    private static final int SHIFT = EnergyAmount.FRACTION_BITS - COARSE_BITS;
    private static final long COARSE_MASK = (1L << COARSE_BITS) - 1;
    /** Largest whole EU that the planning grid accepts for one budget or room. */
    private static final long MAX_WHOLE = 1L << 38;

    /** The line loss model. */
    public enum LineLoss { LOSSLESS, PROPORTIONAL }

    /** 0.1.10.22: the highest source rank. */
    public static final int MAX_RANK = 3;
    /** 0.1.10.22: receiver tiers: 0 critical, 1 normal (the default), 2 deferrable. */
    public static final int TIERS = 3, NORMAL_TIER = 1;

    /**
     * One source of the round.
     *
     * @param budget the EU that the source can give in this round
     * @param packet the packet size (voltage) of the source, in EU. Must be above 0.
     * @param sourceClass 0 for a generator or relay, 1 for storage
     * @param rank 0.1.10.22: the visit order in each stage (0 first, at most {@link #MAX_RANK})
     * @param chargesStorage 0.1.10.22: false when the source gives nothing in the storage stages
     */
    public record Source(EnergyAmount budget, long packet, int sourceClass, int rank, boolean chargesStorage) {
        public Source {
            Objects.requireNonNull(budget, "budget");
            if (packet <= 0 || sourceClass < 0 || sourceClass > 1 || rank < 0 || rank > MAX_RANK)
                throw new IllegalArgumentException("Invalid fair-share source");
        }

        /** The order before 0.1.10.22: rank = class, charges storage. */
        public Source(EnergyAmount budget, long packet, int sourceClass) {
            this(budget, packet, sourceClass, sourceClass, true);
        }
    }

    /**
     * One receiver of the round.
     *
     * @param room the free capacity at the start of the round
     * @param rated the rated input for this round, or {@code null} for no limit
     * @param receiverClass 0 for a load or relay, 1 for storage
     * @param relay 0.1.10.12: for a class 1 receiver, the part of its demand that
     *        stage 1 serves with the loads (a storage block that feeds machines);
     *        ignored for class 0. Zero for none.
     * @param tier 0.1.10.22: 0 critical, 1 normal, 2 deferrable
     */
    public record Receiver(EnergyAmount room, EnergyAmount rated, int receiverClass, EnergyAmount relay, int tier) {
        public Receiver {
            Objects.requireNonNull(room, "room");
            if (receiverClass < 0 || receiverClass > 1 || tier < 0 || tier >= TIERS)
                throw new IllegalArgumentException("Invalid fair-share receiver");
            if (relay == null) relay = EnergyAmount.ZERO;
        }

        /** A normal-tier receiver. */
        public Receiver(EnergyAmount room, EnergyAmount rated, int receiverClass, EnergyAmount relay) {
            this(room, rated, receiverClass, relay, NORMAL_TIER);
        }

        /** A normal-tier receiver without relay demand. */
        public Receiver(EnergyAmount room, EnergyAmount rated, int receiverClass) {
            this(room, rated, receiverClass, EnergyAmount.ZERO, NORMAL_TIER);
        }
    }

    /**
     * One positive delivery. {@code domain} and {@code entry} identify the
     * route (the same values as {@link FractionalDistributor.Delivery}).
     * {@code voltage} is the packet that the source pushes to this receiver,
     * for overvoltage effects: {@code min(packet, max(debit, min(room, budget)))},
     * where room and budget are the values at the start of the round.
     */
    public record Delivery(int domain, int entry, int source, int receiver,
                           EnergyAmount credit, EnergyAmount pathLoss, double voltage) {
        public EnergyAmount sourceDebit() { return credit.add(pathLoss); }
    }

    /** The result of one round. Indexes match the input lists. */
    public static final class Round {
        private final EnergyAmount[] debits, credits;
        private final EnergyAmount dissipated;
        private final List<Delivery> deliveries;
        private Round(EnergyAmount[] debits, EnergyAmount[] credits, EnergyAmount dissipated, List<Delivery> deliveries) {
            this.debits = debits; this.credits = credits; this.dissipated = dissipated;
            this.deliveries = List.copyOf(deliveries);
        }
        public EnergyAmount debit(int source) { return debits[source]; }
        public EnergyAmount credit(int receiver) { return credits[receiver]; }
        public EnergyAmount dissipated() { return dissipated; }
        public List<Delivery> deliveries() { return deliveries; }
        public int sourceCount() { return debits.length; }
        public int receiverCount() { return credits.length; }
    }

    /** Converts an amount to the planning grid, rounded down and clamped. */
    static long toCoarse(EnergyAmount amount) {
        if (amount.whole() >= MAX_WHOLE) return MAX_WHOLE << COARSE_BITS;
        return (amount.whole() << COARSE_BITS) | (amount.fraction() >>> SHIFT);
    }

    /** Converts a planning grid value to an amount. This conversion is exact. */
    static EnergyAmount fromCoarse(long value) {
        if (value < 0) throw new IllegalArgumentException("Negative planned energy");
        return new EnergyAmount(value >>> COARSE_BITS, (value & COARSE_MASK) << SHIFT);
    }

    /**
     * <p><b>EN:</b> Allocates one round (four stages, see the class comment). Pure function: no world access.
     * <p><b>ZH:</b> 分配一轮（四个阶段，见类注释）。纯函数：不访问世界。
     * <p><b>JA:</b> 1 ラウンド分を割り当てる（4 段階、クラスのコメント参照）。純粋関数：ワールドにアクセスしない。
     *
     * @param sources the sources, indexed as in the domains
     * @param domains the route groups. The priorities are ignored.
     * @param contacts the contact index of each receiver in the route costs
     * @param receivers the receivers
     * @param shared the ports that are a source and a receiver
     * @param loss the line loss model
     * @param rotation the start offset of the source order (for example the world time)
     * @return the round
     */
    public static Round allocate(List<Source> sources, List<DomainDistributor.Domain> domains, int[] contacts,
            List<Receiver> receivers, List<DomainDistributor.SharedStorage> shared, LineLoss loss, long rotation) {
        Objects.requireNonNull(loss, "loss");
        int sourceCount = sources.size(), receiverCount = receivers.size();
        if (contacts.length != receiverCount) throw new IllegalArgumentException("Receiver vector lengths");

        // Shared storage: source -> own receiver.
        int[] ownReceiver = new int[sourceCount];
        Arrays.fill(ownReceiver, -1);
        boolean[] bound = new boolean[receiverCount];
        for (var binding : shared) {
            if (binding.source() >= sourceCount || binding.receiver() >= receiverCount
                    || ownReceiver[binding.source()] >= 0 || bound[binding.receiver()])
                throw new IllegalArgumentException("Invalid shared storage binding");
            ownReceiver[binding.source()] = binding.receiver();
            bound[binding.receiver()] = true;
        }

        long[] packet = new long[sourceCount];
        int[] sourceClass = new int[sourceCount], rank = new int[sourceCount];
        boolean[] charges = new boolean[sourceCount];
        long[] budget = new long[sourceCount], initialBudget = new long[sourceCount];
        for (int i = 0; i < sourceCount; i++) {
            var source = sources.get(i);
            packet[i] = source.packet();
            sourceClass[i] = source.sourceClass();
            rank[i] = source.rank();
            charges[i] = source.chargesStorage();
            budget[i] = toCoarse(source.budget());
            initialBudget[i] = budget[i];
        }
        int[] receiverClass = new int[receiverCount], tier = new int[receiverCount];
        long[] room = new long[receiverCount], initialRoom = new long[receiverCount], rateLeft = new long[receiverCount];
        long[] relayLeft = new long[receiverCount];
        boolean[] tierUsed = new boolean[TIERS];
        for (int r = 0; r < receiverCount; r++) {
            var receiver = receivers.get(r);
            receiverClass[r] = receiver.receiverClass();
            tier[r] = receiver.tier();
            tierUsed[tier[r]] = true;
            room[r] = toCoarse(receiver.room());
            initialRoom[r] = room[r];
            rateLeft[r] = receiver.rated() == null ? -1 : toCoarse(receiver.rated());
            relayLeft[r] = receiver.receiverClass() == 1 ? toCoarse(receiver.relay()) : 0;
        }

        // EN: Select the best route for each (source, receiver) pair: the lowest loss. A tie keeps
        //     the first route in domain and entry order (deterministic). linkDomain < 0 = no route.
        // ZH: 为每个（源、接收方）对选择损耗最低的路由；相同时保留域与条目顺序中的第一条（确定性）。linkDomain < 0 = 无路由。
        // JA: 各（供給元、受信側）の組で損失が最小の経路を選ぶ。同点ならドメインと項目の順で最初の経路（決定的）。linkDomain < 0 = 経路なし。
        int[][] linkDomain = new int[sourceCount][receiverCount], linkEntry = new int[sourceCount][receiverCount];
        long[][] linkLoss = new long[sourceCount][receiverCount];
        for (int i = 0; i < sourceCount; i++) Arrays.fill(linkDomain[i], -1);
        for (int d = 0; d < domains.size(); d++) {
            var domain = domains.get(d);
            for (int entry = 0; entry < domain.sources.length; entry++) {
                int source = domain.sources[entry];
                if (source < 0 || source >= sourceCount) throw new IllegalArgumentException("Invalid domain source");
                var route = domain.routes.get(entry);
                long den = Math.multiplyExact(1000L, packet[source]);
                for (int r = 0; r < receiverCount; r++) {
                    if (r == ownReceiver[source] || !route.reaches(contacts[r])) continue;
                    long milli = route.lossMilliTo(contacts[r]);
                    if (milli < 0) throw new IllegalArgumentException("Negative route loss");
                    if (loss == LineLoss.PROPORTIONAL && milli >= den) continue; // The loss takes the full packet.
                    long effective = loss == LineLoss.LOSSLESS ? 0 : milli;
                    if (linkDomain[source][r] < 0 || effective < linkLoss[source][r]) {
                        linkDomain[source][r] = d; linkEntry[source][r] = entry; linkLoss[source][r] = effective;
                    }
                }
            }
        }
        // Compact the linked receivers of each source, and the loss fraction of each link.
        int[][] linked = new int[sourceCount][];
        double[][] fraction = new double[sourceCount][];
        for (int i = 0; i < sourceCount; i++) {
            int count = 0;
            for (int r = 0; r < receiverCount; r++) if (linkDomain[i][r] >= 0) count++;
            linked[i] = new int[count];
            fraction[i] = new double[count];
            double den = 1000.0 * packet[i];
            int at = 0;
            for (int r = 0; r < receiverCount; r++) if (linkDomain[i][r] >= 0) {
                linked[i][at] = r;
                fraction[i][at++] = linkLoss[i][r] / den;
            }
        }

        int[] order = sourceOrder(rank, rotation);
        int[][] pools = pools(order, sourceClass, rank, charges, linked, fraction);
        long[] debits = new long[sourceCount], credits = new long[receiverCount];
        long lossTotal = 0;
        long[][] pairCredit = new long[sourceCount][], pairLoss = new long[sourceCount][];
        long[] demand = new long[receiverCount];

        // EN: 0.1.10.22: each of the four stages runs once per used tier, the critical tier first.
        // ZH: 0.1.10.22：四个阶段各按使用中的优先级执行一次，重要的先。
        // JA: 0.1.10.22：4 つの段階をそれぞれ使われている優先度ごとに 1 回、重要から行う。
        for (int step = 0; step < 4 * TIERS; step++) {
            int stage = step / TIERS, stageTier = step % TIERS;
            if (!tierUsed[stageTier]) continue;
            for (int[] pool : pools) {
                if (stage >= 2 && !charges[pool[0]]) continue;
                // EN: A pool is one source, or more sources of one class with the same links and no loss.
                //     It acts as one source with the summed budget.
                // ZH: 池是一个源，或同类、连接相同且无损耗的多个源；它相当于预算相加后的单个源。
                // JA: プールは 1 つの供給元、または同じ種別・同じ接続・損失なしの複数の供給元。予算を合計した 1 つの供給元として動く。
                long poolBudget = 0;
                for (int member : pool) poolBudget = saturatingAdd(poolBudget, budget[member]);
                if (poolBudget <= 0) continue;
                int lead = pool[0];
                int[] targets = linked[lead];
                double[] f = fraction[lead];
                long[] want = new long[targets.length];
                boolean any = false;
                for (int c = 0; c < targets.length; c++) {
                    int r = targets[c];
                    want[c] = tier[r] != stageTier ? 0 : Math.max(0, stageDemand(stage, receiverClass[r], room[r], rateLeft[r], relayLeft[r]));
                    demand[r] = want[c];
                    any |= want[c] > 0;
                }
                if (!any) continue;
                // EN: 0.1.10.12: max-min share (water filling) instead of a share in proportion to the demand.
                // ZH: 0.1.10.12：最大最小均分（注水法），取代按需求比例分配。
                // JA: 0.1.10.12：需要比例の配分に代えて、最大最小の均等配分（水位法）。
                long[] planned = loss == LineLoss.LOSSLESS ? maxMinLossless(want, poolBudget) : maxMinWeighted(want, f, poolBudget);
                int member = 0;
                for (int c = 0; c < targets.length && poolBudget > 0; c++) {
                    int r = targets[c];
                    if (demand[r] == 0) continue;
                    long credit = Math.min(demand[r], planned[c]);
                    long pathLoss = f[c] == 0 ? 0 : (long) Math.floor(credit * f[c]);
                    // EN: Cap by the remaining budget, so a rounding error cannot overspend.
                    // ZH: 以剩余预算为上限，舍入误差不会超支。
                    // JA: 残り予算で上限をかけるので、丸め誤差で使い過ぎない。
                    if (credit + pathLoss > poolBudget) {
                        credit = f[c] == 0 ? poolBudget : (long) Math.floor(poolBudget / (1.0 + f[c]));
                        pathLoss = f[c] == 0 ? 0 : Math.min(poolBudget - credit, (long) Math.floor(credit * f[c]));
                    }
                    if (credit <= 0) continue;
                    poolBudget -= credit + pathLoss;
                    credits[r] += credit;
                    room[r] -= credit;
                    if (rateLeft[r] >= 0) rateLeft[r] = Math.max(0, rateLeft[r] - credit);
                    if (stage == 0 && receiverClass[r] == 1) relayLeft[r] = Math.max(0, relayLeft[r] - credit);
                    lossTotal += pathLoss;
                    // EN: Northwest-corner rule: pay this credit from the pool members in visit order.
                    //     A pool with loss has exactly one member.
                    // ZH: 西北角法则：按访问顺序由池成员支付这笔输送。有损耗的池只有一个成员。
                    // JA: 北西隅ルール：この送電量をプールのメンバーが訪問順に支払う。損失のあるプールのメンバーは 1 つだけ。
                    long creditLeft = credit, lossLeft = pathLoss;
                    while (creditLeft > 0 || lossLeft > 0) {
                        while (budget[pool[member]] == 0) member++;
                        int payer = pool[member];
                        long take = Math.min(creditLeft + lossLeft, budget[payer]);
                        long lossPart = Math.min(lossLeft, take), creditPart = take - lossPart;
                        budget[payer] -= take;
                        debits[payer] += take;
                        creditLeft -= creditPart;
                        lossLeft -= lossPart;
                        int own = ownReceiver[payer];
                        if (own >= 0) room[own] += take;
                        if (pairCredit[payer] == null) {
                            pairCredit[payer] = new long[receiverCount];
                            pairLoss[payer] = new long[receiverCount];
                        }
                        pairCredit[payer][r] += creditPart;
                        pairLoss[payer][r] += lossPart;
                    }
                }
                for (int payer : pool) if (budget[payer] < 0) throw new IllegalStateException("Fair-share source overspent");
            }
        }

        // One delivery per (source, receiver) pair, in source then receiver order.
        var deliveries = new ArrayList<Delivery>();
        for (int source = 0; source < sourceCount; source++) {
            if (pairCredit[source] == null) continue;
            for (int r = 0; r < receiverCount; r++) {
                long credit = pairCredit[source][r];
                if (credit <= 0) continue;
                long pathLoss = pairLoss[source][r];
                double pushed = fromCoarse(Math.min(initialRoom[r], initialBudget[source])).toDouble();
                double debit = fromCoarse(credit + pathLoss).toDouble();
                double voltage = Math.min((double) packet[source], Math.max(debit, pushed));
                deliveries.add(new Delivery(linkDomain[source][r], linkEntry[source][r], source, r,
                    fromCoarse(credit), fromCoarse(pathLoss), voltage));
            }
        }

        long debitTotal = 0, creditTotal = 0;
        var debitAmounts = new EnergyAmount[sourceCount];
        var creditAmounts = new EnergyAmount[receiverCount];
        for (int i = 0; i < sourceCount; i++) { debitTotal = Math.addExact(debitTotal, debits[i]); debitAmounts[i] = fromCoarse(debits[i]); }
        for (int r = 0; r < receiverCount; r++) { creditTotal = Math.addExact(creditTotal, credits[r]); creditAmounts[r] = fromCoarse(credits[r]); }
        if (debitTotal != Math.addExact(creditTotal, lossTotal))
            throw new IllegalStateException("Fair-share allocation lost energy");
        return new Round(debitAmounts, creditAmounts, fromCoarse(lossTotal), deliveries);
    }

    /**
     * The demand of one receiver in one stage (0.1.10.12): stage 0 is the
     * priority stage (rated demand of a load, relay demand of a storage block),
     * stage 1 the surplus of the loads, stage 2 the rated demand of the storage
     * blocks, stage 3 their surplus. {@code rateLeft < 0} means no rated limit.
     */
    static long stageDemand(int stage, int receiverClass, long room, long rateLeft, long relayLeft) {
        long rated = rateLeft >= 0 ? Math.min(room, rateLeft) : room;
        return switch (stage) {
            case 0 -> receiverClass == 0 ? rated : Math.min(rated, relayLeft);
            case 1 -> receiverClass == 0 ? room : 0;
            case 2 -> receiverClass == 1 ? rated : 0;
            default -> receiverClass == 1 ? room : 0;
        };
    }

    /**
     * Max-min share without loss (0.1.10.12), exact in the planning grid: every
     * receiver gets {@code min(want, level)}; the level is the highest one that
     * the budget pays. The rest of a level that does not divide evenly (less than
     * one grid unit per receiver) goes to the first receivers above the level, so
     * the budget is used in full when the demand is larger.
     *
     * @return the planned credit per entry of {@code want}
     */
    static long[] maxMinLossless(long[] want, long budget) {
        long total = 0;
        int count = 0;
        for (long w : want) if (w > 0) { total = saturatingAdd(total, w); count++; }
        if (total <= budget) return want.clone();
        long[] sorted = new long[count];
        int at = 0;
        for (long w : want) if (w > 0) sorted[at++] = w;
        Arrays.sort(sorted);
        long left = budget, level = 0, extra = 0;
        int remaining = count;
        for (long w : sorted) {
            // w * remaining <= left, without an overflow
            if (w <= left / remaining) { left -= w; remaining--; continue; }
            level = left / remaining;
            extra = left - level * remaining;
            break;
        }
        long[] planned = new long[want.length];
        for (int c = 0; c < want.length; c++) {
            if (want[c] <= 0) continue;
            if (want[c] <= level) { planned[c] = want[c]; continue; }
            planned[c] = level;
            if (extra > 0) { planned[c]++; extra--; }
        }
        return planned;
    }

    /**
     * Max-min share with proportional line loss (0.1.10.12): the same credit level
     * for every receiver, where a credit {@code c} to entry {@code i} costs
     * {@code c * (1 + f[i])}. The caller caps each credit by the remaining budget.
     *
     * @return the planned credit per entry of {@code want}
     */
    static long[] maxMinWeighted(long[] want, double[] f, long budget) {
        int n = want.length;
        double cost = 0;
        for (int c = 0; c < n; c++) if (want[c] > 0) cost += want[c] * (1.0 + f[c]);
        if (cost <= budget) return want.clone();
        Integer[] order = new Integer[n];
        int count = 0;
        for (int c = 0; c < n; c++) if (want[c] > 0) order[count++] = c;
        order = Arrays.copyOf(order, count);
        Arrays.sort(order, (a, b) -> Long.compare(want[a], want[b]));
        double weight = 0;
        for (int c : order) weight += 1.0 + f[c];
        double left = budget, level = Double.MAX_VALUE;
        for (int c : order) {
            if (want[c] * weight <= left) { left -= want[c] * (1.0 + f[c]); weight -= 1.0 + f[c]; continue; }
            level = left / weight;
            break;
        }
        long[] planned = new long[n];
        for (int c = 0; c < n; c++) if (want[c] > 0) planned[c] = Math.min(want[c], (long) Math.floor(level));
        return planned;
    }

    /**
     * Groups the sources into pools, in visit order.
     *
     * <p>Sources of the same class with the same linked receivers and no loss
     * on any link join one pool. A pool gives the same credits as its members
     * in sequence (the fractions add), but it makes at most one delivery per
     * receiver and member change. This keeps the number of deliveries near
     * {@code S + R} on a shared bus, instead of {@code S * R}. Each other source
     * is a pool of one.
     */
    static int[][] pools(int[] order, int[] sourceClass, int[] rank, boolean[] charges, int[][] linked, double[][] fraction) {
        var groups = new java.util.LinkedHashMap<Object, java.util.ArrayList<Integer>>();
        for (int source : order) {
            boolean lossless = true;
            for (double value : fraction[source]) if (value != 0) { lossless = false; break; }
            Object key = lossless ? new PoolKey(sourceClass[source], rank[source], charges[source], linked[source]) : Integer.valueOf(-1 - source);
            groups.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(source);
        }
        int[][] result = new int[groups.size()][];
        int at = 0;
        for (var members : groups.values()) result[at++] = members.stream().mapToInt(Integer::intValue).toArray();
        return result;
    }

    /** Pool identity: source class and linked receivers. */
    private record PoolKey(int sourceClass, int rank, boolean charges, int[] linked) {
        @Override public boolean equals(Object other) {
            return other instanceof PoolKey key && key.sourceClass == sourceClass && key.rank == rank && key.charges == charges
                && Arrays.equals(key.linked, linked);
        }
        @Override public int hashCode() { return 31 * (31 * (31 * sourceClass + rank) + (charges ? 1 : 0)) + Arrays.hashCode(linked); }
    }

    private static long saturatingAdd(long left, long right) {
        long sum = left + right;
        return sum < 0 ? Long.MAX_VALUE : sum;
    }

    /**
     * Returns the source visit order: all class 0 sources, then all class 1
     * sources. Each class starts at {@code rotation mod size} and wraps.
     */
    static int[] sourceOrder(int[] rank, long rotation) {
        int[] order = new int[rank.length];
        int at = 0;
        for (int wanted = 0; wanted <= MAX_RANK; wanted++) {
            int size = 0;
            for (int value : rank) if (value == wanted) size++;
            if (size == 0) continue;
            int[] members = new int[size];
            int m = 0;
            for (int i = 0; i < rank.length; i++) if (rank[i] == wanted) members[m++] = i;
            int start = (int) Math.floorMod(rotation, (long) size);
            for (int k = 0; k < size; k++) order[at++] = members[(start + k) % size];
        }
        return order;
    }

    /**
     * <p><b>EN:</b> 0.1.10.20 (G2), 0.1.10.22: the part of {@code rotation} that the round uses: the start of the visit
     * order in each rank ({@code rotation mod size}). Equal keys give equal visit orders.
     * <p><b>ZH:</b> 0.1.10.20（G2）、0.1.10.22：本轮实际使用的 {@code rotation} 部分，即每个次序内访问的起点（{@code rotation mod 数量}）。键相同则访问顺序相同。
     * <p><b>JA:</b> 0.1.10.20（G2）、0.1.10.22：ラウンドが使う {@code rotation} の部分。各順位の中で訪れる起点（{@code rotation mod 数}）。キーが同じなら訪れる順も同じ。
     */
    public static long rotationKey(List<Source> sources, long rotation) {
        long key = 0;
        for (int wanted = 0; wanted <= MAX_RANK; wanted++) {
            int size = 0;
            for (var source : sources) if (source.rank() == wanted) size++;
            long start = size == 0 ? 0 : Math.floorMod(rotation, (long) size);
            key = key * 4096 + start;
        }
        return key;
    }
}

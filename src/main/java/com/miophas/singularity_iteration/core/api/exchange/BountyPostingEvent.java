// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.exchange;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * <p><b>EN:</b> A system bounty is about to be posted (public API since 0.1.10.26), on {@code NeoForge.EVENT_BUS}, on the
 * server thread. The needs and rewards are already drawn; a listener may change both lists, the days and the split, or
 * cancel the posting. After the event the exchange checks the bounty again. Unknown items and empty needs cancel it.
 * Rewards that cannot be paid (stock of the treasury and the vault, weekly caps of issued items and UU-matter) are
 * dropped. A NORMAL or ADVANCED bounty without any reward pays coins (value x the tier multiplier).
 * <p><b>ZH:</b> 系统悬赏即将发布（0.1.10.26 起的公共 API），在服务器线程发布到 {@code NeoForge.EVENT_BUS}。要求与奖励已抽取；监听器可以修改两个列表、
 * 天数与分配方式，或取消发布。事件之后交易所会再次检查：未知物品或要求为空时取消；无法支付的奖励（国库与宝库的库存、发放物品与 UU 物质的每周上限）
 * 被去掉，没有任何奖励的 NORMAL 或 ADVANCED 悬赏改为支付货币（价值 × 该等级的倍率）。
 * <p><b>JA:</b> システム懸賞が掲示される直前（0.1.10.26 以降の公開 API）。サーバースレッドで {@code NeoForge.EVENT_BUS} に投稿する。要求と報酬は
 * 引いた後で、リスナーは両方のリスト、日数、分配方法を変えたり、掲示を取り消したりできる。イベントの後、取引所はもう一度確認する：未知のアイテムや
 * 空の要求なら取り消し、払えない報酬（国庫と宝物庫の在庫、発行アイテムと UU 物質の週上限）は外し、報酬が残らない NORMAL・ADVANCED の懸賞は
 * 硬貨（価値 × 段階の倍率）を払う。
 */
public final class BountyPostingEvent extends Event implements ICancellableEvent {
    private final MinecraftServer server;
    private final BountyTier tier;
    private final String table;
    private final List<BountyNeed> needs;
    private final List<BountyReward> rewards;
    private int days;
    private BountySplit split;

    public BountyPostingEvent(MinecraftServer server, BountyTier tier, String table, List<BountyNeed> needs, List<BountyReward> rewards,
                              int days, BountySplit split) {
        this.server = server;
        this.tier = tier;
        this.table = table;
        this.needs = new ArrayList<>(needs);
        this.rewards = new ArrayList<>(rewards);
        this.days = days;
        this.split = split;
    }

    /** EN: The server. ZH: 服务器。JA: サーバー。 */
    public MinecraftServer getServer() { return server; }
    /** EN: The tier. ZH: 等级。JA: 段階。 */
    public BountyTier getTier() { return tier; }
    /** EN: The datapack table id, or "" (generated NORMAL request, Java API). ZH: 数据包表 ID，或 ""（自动生成的 NORMAL 委托、Java API）。JA: データパックの表の ID、または ""（自動生成の NORMAL、Java API）。 */
    public String getTable() { return table; }
    /** EN: The needs; may be changed (1 to 8). ZH: 要求，可修改（1～8 项）。JA: 要求。変更できる（1〜8 件）。 */
    public List<BountyNeed> getNeeds() { return needs; }
    /** EN: The rewards; may be changed (at most 8). ZH: 奖励，可修改（最多 8 项）。JA: 報酬。変更できる（最大 8 件）。 */
    public List<BountyReward> getRewards() { return rewards; }
    /** EN: Days the bounty stays open. ZH: 悬赏开放的天数。JA: 懸賞が開いている日数。 */
    public int getDays() { return days; }
    /** EN: Sets the days (1 to 60). ZH: 设置天数（1～60）。JA: 日数を設定する（1〜60）。 */
    public void setDays(int days) {
        if (days < 1 || days > 60) throw new IllegalArgumentException("days 1..60");
        this.days = days;
    }
    /** EN: How the rewards are paid. ZH: 奖励的支付方式。JA: 報酬の払い方。 */
    public BountySplit getSplit() { return split; }
    /** EN: Sets how the rewards are paid. ZH: 设置奖励的支付方式。JA: 報酬の払い方を設定する。 */
    public void setSplit(BountySplit split) {
        if (split == null) throw new IllegalArgumentException("split");
        this.split = split;
    }
}

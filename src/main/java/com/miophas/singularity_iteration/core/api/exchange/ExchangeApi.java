// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.exchange;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * <p><b>EN:</b> The SI exchange for pack makers and addons (public API since 0.1.10.26), server side, on the server thread.
 * Read: the commodities and their prices, and quotes of a sale or a purchase now (with the sell depth and the stock).
 * Also the market's treasury, the bounty vault, the coin ledger and the open bounties. Write: coins into or out of a player's exchange
 * mailbox (with a reason code; the ledger counts them as created or destroyed), bounties (a datapack table or a
 * {@link BountySpec}), cancelling a bounty. The exchange keeps every rule: a quote is what the market would do now, a posted
 * bounty reserves its rewards. Events on {@code NeoForge.EVENT_BUS}: {@link MarketDayEvent}, {@link ExchangeTradeEvent},
 * {@link BountyPostingEvent}, {@link BountyCompletedEvent}. All methods return empty results before Singularity Iteration
 * installs its service ({@link #isAvailable}).
 * <p><b>ZH:</b> 面向整合包作者与附属模组的 SI 交易所（0.1.10.26 起的公共 API），仅服务器端、在服务器线程调用。读取：商品与价格、此刻卖出或买入的报价
 * （含卖出深度与库存）、市场国库、悬赏宝库、货币账本、未结束的悬赏。写入：向玩家的交易所邮箱存入或取出货币（带原因代码；账本计为发行或销毁）、
 * 发布悬赏（数据包表或 {@link BountySpec}）、取消悬赏。交易所保持全部规则：报价即市场此刻的行为，发布的悬赏会预留奖励。事件发布在
 * {@code NeoForge.EVENT_BUS}：{@link MarketDayEvent}、{@link ExchangeTradeEvent}、{@link BountyPostingEvent}、{@link BountyCompletedEvent}。
 * 奇点迭代安装服务之前（{@link #isAvailable}）所有方法返回空结果。
 * <p><b>JA:</b> パック作者とアドオン向けの SI 取引所（0.1.10.26 以降の公開 API）。サーバー側でサーバースレッドから呼ぶ。読み取り：品目と価格、
 * 今の売却・購入の見積（売りの深さと在庫を含む）、市場の国庫、懸賞の宝物庫、硬貨の台帳、開いている懸賞。書き込み：プレイヤーの取引所メールボックスへの
 * 硬貨の入金・出金（理由コード付き。台帳は発行・消滅として数える）、懸賞の掲示（データパックの表か {@link BountySpec}）、懸賞の取り消し。
 * 取引所はすべての規則を守る：見積は市場が今行うこと、掲示した懸賞は報酬を確保する。イベントは {@code NeoForge.EVENT_BUS} に出る：
 * {@link MarketDayEvent}、{@link ExchangeTradeEvent}、{@link BountyPostingEvent}、{@link BountyCompletedEvent}。奇点迭代がサービスを
 * 組み込む前（{@link #isAvailable}）は、すべてのメソッドが空の結果を返す。
 */
public final class ExchangeApi {
    private ExchangeApi() {}

    /**
     * <p><b>EN:</b> A commodity of the market: item id, category, base price, price now (coins).
     * <p><b>ZH:</b> 市场商品：物品 ID、类别、基础价格、当前价格（货币）。
     * <p><b>JA:</b> 市場の品目：アイテム ID、分類、基準価格、現在の価格（硬貨）。
     */
    public record Commodity(String item, String category, long basePrice, long price) { }

    /**
     * <p><b>EN:</b> A quote: the items the market would take or give now and the coins for all of them.
     * <p><b>ZH:</b> 报价：市场此刻会收下或给出的物品数量，以及全部的货币金额。
     * <p><b>JA:</b> 見積：市場が今受け取る（または渡す）個数と、その全体の硬貨。
     */
    public record Quote(long quantity, long coins) { }

    /**
     * <p><b>EN:</b> One market day of the ledger: coins created and destroyed by the exchange. Of the created coins, those
     * of bounties; the items and UU-matter (mB) that bounties created. The coins of the Java API per reason code:
     * {@code deposits} created, {@code withdrawals} destroyed.
     * <p><b>ZH:</b> 账本中的一个交易日：交易所发行与销毁的货币，其中悬赏发行的货币，悬赏创建的物品与 UU 物质（mB），以及 Java API 按原因代码的货币
     * （{@code deposits} 发行，{@code withdrawals} 销毁）。
     * <p><b>JA:</b> 台帳の 1 取引日：取引所が発行・消滅させた硬貨、そのうち懸賞の硬貨、懸賞が生成したアイテムと UU 物質（mB）、Java API の理由コード
     * ごとの硬貨（{@code deposits} 発行、{@code withdrawals} 消滅）。
     */
    public record LedgerDay(long day, long minted, long burned, long bountyCoins, long bountyItems, long bountyUu,
                            Map<String, Long> deposits, Map<String, Long> withdrawals) {
        public LedgerDay {
            deposits = Map.copyOf(deposits);
            withdrawals = Map.copyOf(withdrawals);
        }
    }

    /**
     * <p><b>EN:</b> What Singularity Iteration implements; addons call the static methods of {@link ExchangeApi}.
     * <p><b>ZH:</b> 由奇点迭代实现；附属请调用 {@link ExchangeApi} 的静态方法。
     * <p><b>JA:</b> 奇点迭代が実装する。アドオンは {@link ExchangeApi} の静的メソッドを呼ぶ。
     */
    @org.jetbrains.annotations.ApiStatus.Internal
    public interface Service {
        List<Commodity> commodities(MinecraftServer server);
        Quote quoteSell(MinecraftServer server, UUID player, String item, long quantity);
        Quote quoteBuy(MinecraftServer server, UUID player, String item, long quantity);
        Map<String, Long> treasury(MinecraftServer server);
        Map<String, Long> vault(MinecraftServer server);
        List<LedgerDay> ledger(MinecraftServer server, int days);
        long mailboxCoins(MinecraftServer server, UUID player);
        long deposit(MinecraftServer server, UUID player, long coins, String reason);
        boolean withdraw(MinecraftServer server, UUID player, long coins, String reason);
        List<BountyInfo> bounties(MinecraftServer server);
        long postBounty(MinecraftServer server, ResourceLocation table);
        long postBounty(MinecraftServer server, BountySpec spec);
        boolean cancelBounty(MinecraftServer server, long id);
    }

    private static volatile Service service;

    /**
     * <p><b>EN:</b> Called once by Singularity Iteration.
     * <p><b>ZH:</b> 由奇点迭代调用一次。
     * <p><b>JA:</b> 奇点迭代が 1 回だけ呼ぶ。
     */
    @org.jetbrains.annotations.ApiStatus.Internal
    public static synchronized void install(Service s) {
        if (service != null && service != s) throw new IllegalStateException("exchange service already installed");
        service = s;
    }

    /**
     * <p><b>EN:</b> True once the exchange service is installed (Singularity Iteration is loaded).
     * <p><b>ZH:</b> 交易所服务已安装（奇点迭代已加载）时为 true。
     * <p><b>JA:</b> 取引所サービスが組み込まれていれば（奇点迭代が読み込まれていれば）true。
     */
    public static boolean isAvailable() { return service != null; }

    private static final Pattern REASON = Pattern.compile("[a-z0-9_.:/-]{1,64}");

    /**
     * <p><b>EN:</b> The commodities of the market (datapack {@code si_futures} and {@code si_exchange/commodities}).
     * <p><b>ZH:</b> 市场的商品（数据包 {@code si_futures} 与 {@code si_exchange/commodities}）。
     * <p><b>JA:</b> 市場の品目（データパック {@code si_futures} と {@code si_exchange/commodities}）。
     */
    public static List<Commodity> commodities(MinecraftServer server) { Service s = service; return s == null ? List.of() : s.commodities(server); }

    /**
     * <p><b>EN:</b> What an instant sale of {@code quantity} by {@code player} to the market would pay now (sell depth, sell
     * pressure, the player's allowance). Nothing changes.
     * <p><b>ZH:</b> {@code player} 此刻立即卖给市场 {@code quantity} 个能得到多少（卖出深度、卖压、玩家额度）。不改变任何状态。
     * <p><b>JA:</b> {@code player} が今 {@code quantity} 個を市場に即時売却したらいくらになるか（売りの深さ、売り圧力、プレイヤーの上限）。何も変えない。
     */
    public static Quote quoteSell(MinecraftServer server, UUID player, String item, long quantity) {
        Service s = service;
        return s == null || quantity <= 0 ? new Quote(0, 0) : s.quoteSell(server, player, item, quantity);
    }

    /**
     * <p><b>EN:</b> What buying {@code quantity} from the market would cost now (treasury, then import at a premium; the
     * player's daily buy limit). Nothing changes.
     * <p><b>ZH:</b> 此刻从市场买入 {@code quantity} 个的花费（先国库，再加价进口；玩家每日购买上限）。不改变任何状态。
     * <p><b>JA:</b> 今、市場から {@code quantity} 個買うといくらかかるか（国庫、次に割増の輸入。プレイヤーの 1 日の購入上限）。何も変えない。
     */
    public static Quote quoteBuy(MinecraftServer server, UUID player, String item, long quantity) {
        Service s = service;
        return s == null || quantity <= 0 ? new Quote(0, 0) : s.quoteBuy(server, player, item, quantity);
    }

    /**
     * <p><b>EN:</b> The market's stock per item id (a copy).
     * <p><b>ZH:</b> 市场按物品 ID 的库存（副本）。
     * <p><b>JA:</b> 市場のアイテム ID ごとの在庫（コピー）。
     */
    public static Map<String, Long> treasury(MinecraftServer server) { Service s = service; return s == null ? Map.of() : s.treasury(server); }

    /**
     * <p><b>EN:</b> The bounty vault per item id (a copy): rare items delivered to bounties, paid out by later ones.
     * <p><b>ZH:</b> 悬赏宝库按物品 ID 的库存（副本）：交付给悬赏的稀有物品，之后的悬赏会将其作为奖励发出。
     * <p><b>JA:</b> 懸賞の宝物庫のアイテム ID ごとの在庫（コピー）：懸賞に納品された希少品で、後の懸賞が報酬として払い出す。
     */
    public static Map<String, Long> vault(MinecraftServer server) { Service s = service; return s == null ? Map.of() : s.vault(server); }

    /**
     * <p><b>EN:</b> The last {@code days} market days of the ledger (at most 60), oldest first.
     * <p><b>ZH:</b> 账本中最近 {@code days} 个交易日（最多 60），从早到晚。
     * <p><b>JA:</b> 台帳の直近 {@code days} 取引日（最大 60）、古い順。
     */
    public static List<LedgerDay> ledger(MinecraftServer server, int days) { Service s = service; return s == null ? List.of() : s.ledger(server, days); }

    /**
     * <p><b>EN:</b> Coins waiting in the player's exchange mailbox (collected at any futures machine).
     * <p><b>ZH:</b> 玩家交易所邮箱中待领取的货币（可在任意期货交易机领取）。
     * <p><b>JA:</b> プレイヤーの取引所メールボックスにある硬貨（どの先物取引機でも受け取れる）。
     */
    public static long mailboxCoins(MinecraftServer server, UUID player) { Service s = service; return s == null ? 0 : s.mailboxCoins(server, player); }

    /**
     * <p><b>EN:</b> Pays {@code coins} into the player's exchange mailbox; the ledger counts them as created under
     * {@code reason} ([a-z0-9_.:/-], 1 to 64 characters). Returns the mailbox coins after the deposit.
     * <p><b>ZH:</b> 向玩家的交易所邮箱存入 {@code coins} 货币；账本以 {@code reason}（[a-z0-9_.:/-]，1～64 个字符）记为发行。返回存入后的邮箱货币。
     * <p><b>JA:</b> プレイヤーの取引所メールボックスに {@code coins} 硬貨を入れる。台帳は {@code reason}（[a-z0-9_.:/-]、1〜64 文字）で発行として数える。
     * 入金後のメールボックスの硬貨を返す。
     */
    public static long deposit(MinecraftServer server, UUID player, long coins, String reason) {
        check(coins, reason);
        Service s = service;
        return s == null ? 0 : s.deposit(server, player, coins, reason);
    }

    /**
     * <p><b>EN:</b> Takes {@code coins} out of the player's exchange mailbox, all or nothing; the ledger counts them as
     * destroyed under {@code reason}. False when the mailbox holds fewer.
     * <p><b>ZH:</b> 从玩家的交易所邮箱取出 {@code coins} 货币，全部或不取；账本以 {@code reason} 记为销毁。邮箱不足时返回 false。
     * <p><b>JA:</b> プレイヤーの取引所メールボックスから {@code coins} 硬貨を全額取り出す（足りなければ何もしない）。台帳は {@code reason} で消滅として数える。
     * 足りないときは false。
     */
    public static boolean withdraw(MinecraftServer server, UUID player, long coins, String reason) {
        check(coins, reason);
        Service s = service;
        return s != null && s.withdraw(server, player, coins, reason);
    }

    private static void check(long coins, String reason) {
        if (coins < 1) throw new IllegalArgumentException("coins must be at least 1");
        if (reason == null || !REASON.matcher(reason).matches()) throw new IllegalArgumentException("reason must match [a-z0-9_.:/-]{1,64}");
    }

    /**
     * <p><b>EN:</b> The open system bounties.
     * <p><b>ZH:</b> 未结束的系统悬赏。
     * <p><b>JA:</b> 開いているシステム懸賞。
     */
    public static List<BountyInfo> bounties(MinecraftServer server) { Service s = service; return s == null ? List.of() : s.bounties(server); }

    /**
     * <p><b>EN:</b> Posts the datapack table {@code table} now (its weight, cooldown and conditions are not checked; the
     * rewards must be payable). Returns the bounty id, or -1 when it was not posted.
     * <p><b>ZH:</b> 立即发布数据包表 {@code table}（不检查权重、冷却与条件；奖励必须可支付）。返回悬赏 ID，未发布时为 -1。
     * <p><b>JA:</b> データパックの表 {@code table} を今すぐ掲示する（重み・クールダウン・条件は見ない。報酬は払える必要がある）。懸賞の ID、
     * 掲示しなかったときは -1 を返す。
     */
    public static long postBounty(MinecraftServer server, ResourceLocation table) { Service s = service; return s == null ? -1 : s.postBounty(server, table); }

    /**
     * <p><b>EN:</b> Posts a bounty described by {@code spec}. Returns the bounty id, or -1 when it was not posted.
     * <p><b>ZH:</b> 发布由 {@code spec} 描述的悬赏。返回悬赏 ID，未发布时为 -1。
     * <p><b>JA:</b> {@code spec} の懸賞を掲示する。懸賞の ID、掲示しなかったときは -1 を返す。
     */
    public static long postBounty(MinecraftServer server, BountySpec spec) { Service s = service; return s == null ? -1 : s.postBounty(server, spec); }

    /**
     * <p><b>EN:</b> Removes an open bounty: reserved rewards not paid go back to the treasury and the vault; nothing more is
     * paid. False when there is no such bounty.
     * <p><b>ZH:</b> 移除未结束的悬赏：未支付的预留奖励退回国库与宝库，不再支付。没有该悬赏时返回 false。
     * <p><b>JA:</b> 開いている懸賞を取り消す：払っていない確保済みの報酬は国庫と宝物庫に戻り、それ以上は払わない。その懸賞がなければ false。
     */
    public static boolean cancelBounty(MinecraftServer server, long id) { Service s = service; return s != null && s.cancelBounty(server, id); }
}

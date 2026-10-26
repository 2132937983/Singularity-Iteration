#!/usr/bin/env python3
"""0.1.10.21 language keys (en_us / zh_cn / ja_jp): the exchange of the futures machine (M2-M5): action messages, the
listing broadcast, the data panel, /mio_icif market audit, and the new config entries.
Idempotent; keeps the line endings of each file. With --check it only reports missing or different keys.
Usage: lang_1121.py <repo root> [--check]
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

M = 'message.mio_icif.exchange.'
G = 'gui.mio_icif.exchange.'
A = 'command.mio_icif.market_audit.'
CFG = 'mio_icif.configuration.'

KEYS = {
    # action results (action bar)
    M + 'unknown_market': ('§cThis market does not exist (any more).', '§c该市场不存在（或已下市）。', '§cこの市場は存在しません（上場廃止済み）。'),
    M + 'locked': ('§cThis commodity is not unlocked for you yet.', '§c你尚未解锁该货品。', '§cこの品目はまだ解放されていません。'),
    M + 'bad_quantity': ('§cInvalid quantity, amount or duration.', '§c数量、金额或期限无效。', '§c数量・金額・期間が不正です。'),
    M + 'outside_band': ('§cThe price must be between %1$s and %2$s.', '§c价格必须在 %1$s 到 %2$s 之间。', '§c価格は %1$s から %2$s の間で指定してください。'),
    M + 'too_many_orders': ('§cYou already have %s resting orders.', '§c你已有 %s 个挂单。', '§c指値注文がすでに %s 件あります。'),
    M + 'not_enough_uu': ('§cNot enough UU-matter on your exchange account!', '§c交易账户中的 UU 物质不足！', '§c取引口座の UU 物質が足りません！'),
    M + 'not_enough_coins': ('§cNot enough coins!', '§c货币不足！', '§cコインが足りません！'),
    M + 'not_enough_items': ('§cNot enough of these items (only plain items without extra data count)!',
                             '§c物品不足（只计算没有附加数据的普通物品）！', '§cアイテムが足りません（追加データのない通常のアイテムだけが対象です）！'),
    M + 'no_match': ('§eNothing matched within your limit price.', '§e在你的限价内没有可成交的对象。', '§e指値の範囲で約定する相手がいませんでした。'),
    M + 'order_rested': ('§aOrder for %s placed; it waits in the book.', '§a已挂单 %s 个，等待成交。', '§a%s 個の指値注文を出しました。板で約定を待ちます。'),
    M + 'bought': ('§aBought %1$s of %2$s.', '§a买入 %1$s / %2$s 个。', '§a%2$s 個中 %1$s 個を購入しました。'),
    M + 'sold': ('§aSold %1$s of %2$s.', '§a卖出 %1$s / %2$s 个。', '§a%2$s 個中 %1$s 個を売却しました。'),
    M + 'not_yours': ('§cThat is not yours.', '§c这不是你的。', '§cあなたのものではありません。'),
    M + 'cancelled': ('§eOrder cancelled (%s left); its escrow went to your mailbox.', '§e已撤单（剩余 %s 个），冻结的货币或物品已退回邮箱。',
                      '§e注文を取り消しました（残り %s 個）。預かり分は郵便箱に戻りました。'),
    M + 'no_order': ('§cThat order no longer exists.', '§c该挂单已不存在。', '§cその注文はもうありません。'),
    M + 'collected': ('§aCollected %s from the mailbox.', '§a从邮箱领取了 %s 个。', '§a郵便箱から %s 個を受け取りました。'),
    M + 'nothing_to_collect': ('§eNothing to collect, or no inventory space.', '§e没有可领取的物品，或背包已满。',
                               '§e受け取るものがないか、インベントリに空きがありません。'),
    M + 'no_uu': ('§cNo UU-matter in the containers of your inventory.', '§c背包中的容器里没有 UU 物质。', '§cインベントリの容器に UU 物質がありません。'),
    M + 'uu_deposited': ('§aDeposited %s mB of UU-matter.', '§a存入了 %s mB UU 物质。', '§aUU 物質 %s mB を預け入れました。'),
    M + 'no_uu_container': ('§cNo empty container for UU-matter in your inventory, or the account is empty.',
                            '§c背包中没有可装 UU 物质的空容器，或账户为空。', '§cUU 物質を入れる空の容器がないか、口座が空です。'),
    M + 'uu_withdrawn': ('§aWithdrew %s mB of UU-matter.', '§a取出了 %s mB UU 物质。', '§aUU 物質 %s mB を引き出しました。'),
    M + 'bad_item': ('§cThis item cannot be used here.', '§c此处不能使用该物品。', '§cこのアイテムはここでは使えません。'),
    M + 'too_many_commissions': ('§cYou already have %s commissions.', '§c你已有 %s 个委托。', '§c委託がすでに %s 件あります。'),
    M + 'board_full': ('§cThe commission board is full.', '§c委托板已满。', '§c委託掲示板がいっぱいです。'),
    M + 'posted': ('§aCommission for %s posted.', '§a已发布 %s 个的委托。', '§a%s 個の委託を掲示しました。'),
    M + 'no_commission': ('§cThat commission no longer exists.', '§c该委托已不存在。', '§cその委託はもうありません。'),
    M + 'own_commission': ('§cYou cannot deliver to your own commission.', '§c不能交付自己的委托。', '§c自分の委託には納品できません。'),
    M + 'delivered': ('§aDelivered %1$s; reward %2$s.', '§a已交付 %1$s 个，报酬 %2$s。', '§a%1$s 個を納品しました。報酬 %2$s。'),
    M + 'withdrawn': ('§eCommission withdrawn; the unpaid reward went to your mailbox.', '§e已撤回委托，未支付的报酬已退回邮箱。',
                      '§e委託を取り下げました。未払いの報酬は郵便箱に戻りました。'),
    M + 'listed': ('§6[Exchange] %1$s is now listed (traded in %2$s).', '§6[交易所] %1$s 已上市（以%2$s交易）。',
                   '§6[取引所] %1$s が上場しました（%2$s建て）。'),
    M + 'no_futures': ('§cFutures exist only for market commodities.', '§c只有市场货品才有期货。', '§c先物があるのは市場の品目だけです。'),
    M + 'too_many_positions': ('§cYou already have %s open futures.', '§c你已有 %s 份未平仓期货。', '§c未決済の先物がすでに %s 件あります。'),
    M + 'future_opened': ('§aOpened %1$s contracts at %2$s (collateral %3$s coins).', '§a以 %2$s 开仓 %1$s 份（保证金 %3$s 货币）。',
                          '§a%2$s で %1$s 枚を建てました（証拠金 %3$s コイン）。'),
    M + 'unknown_action': ('§cUnknown exchange action.', '§c未知的交易操作。', '§c不明な取引操作です。'),

    # data panel
    G + 'title': ('Exchange', '交易所', '取引所'),
    G + 'tab.0': ('Market', '行情', 'マーケット'),
    G + 'tab.1': ('Commissions', '委托', '委託'),
    G + 'tab.2': ('Futures', '期货', '先物'),
    G + 'tab.3': ('Orders & Mail', '挂单与邮箱', '注文と郵便箱'),
    G + 'tab.4': ('Treasury', '国库', '国庫'),
    G + 'header': ('Day %1$s · %2$s coins · %3$s mB UU', '第 %1$s 天 · %2$s 货币 · %3$s mB UU', '%1$s 日目 · %2$s コイン · %3$s mB UU'),
    G + 'loading': ('Loading market data...', '正在载入市场数据……', '市場データを読み込み中…'),
    G + 'coins': ('coins', '货币', 'コイン'),
    G + 'uu': ('mB UU', 'mB UU', 'mB UU'),
    G + 'player_listing': ('player listing', '玩家上市品', 'プレイヤー上場品'),
    G + 'system_market': ('market commodity', '市场货品', '市場品目'),
    G + 'volume': ('Volume today: %s', '今日成交量：%s', '本日の出来高: %s'),
    G + 'locked': ('locked (advancement needed)', '未解锁（需要进度）', '未解放（進捗が必要）'),
    G + 'meta': ('Craft depth %1$s · fundamental %2$s · %3$s', '合成深度 %1$s · 基本价值 %2$s · %3$s', 'クラフト深度 %1$s · 基礎価値 %2$s · %3$s'),
    G + 'chart_help': ('Daily candles with volume; blue line: 5-day mean; yellow dashes: fundamental value',
                       '日 K 线与成交量；蓝线：5 日均线；黄色虚线：基本价值', '日足ローソクと出来高。青線: 5 日平均、黄色の破線: 基礎価値'),
    G + 'quotes': ('Market quotes', '市场报价', '市場の気配'),
    G + 'ask': ('Market sells at', '市场卖价', '市場の売値'),
    G + 'supply': ('Stock + import left', '库存 + 剩余进口', '在庫 + 残り輸入枠'),
    G + 'bid': ('Market buys at', '市场收购价', '市場の買値'),
    G + 'budget': ('Buy budget left', '剩余收购额度', '残り買い取り枠'),
    G + 'daily_left': ('Your daily volume left', '你今日剩余交易量', '本日の残り取引量'),
    G + 'band': ('Price band', '价格区间', '値幅'),
    G + 'listing_only_book': ('Player listing: only player orders trade here.', '玩家上市品：只与玩家挂单成交。', 'プレイヤー上場品: プレイヤーの注文同士でのみ約定します。'),
    G + 'trades': ('Last trades', '最近成交', '直近の約定'),
    G + 'depth': ('Order book', '盘口', '板'),
    G + 'buy': ('Buy', '买入', '買い'),
    G + 'sell': ('Sell', '卖出', '売り'),
    G + 'now': ('Now', '即时', '即時'),
    G + 'rest': ('Order', '挂单', '指値'),
    G + 'price': ('Limit price', '限价', '指値'),
    G + 'quantity': ('Quantity', '数量', '数量'),
    G + 'total': ('Total', '合计', '合計'),
    G + 'now_help': ('Fills now up to the limit; the rest is dropped', '立即按限价成交，未成交部分取消', '指値まで即時に約定し、残りは取り消し'),
    G + 'rest_help': ('Unfilled quantity waits in the book for 7 days', '未成交部分挂单 7 天', '未約定分は 7 日間板に残ります'),
    G + 'submit_buy': ('Place buy', '下买单', '買い注文'),
    G + 'submit_sell': ('Place sell', '下卖单', '売り注文'),
    G + 'board': ('Commission board', '委托板', '委託掲示板'),
    G + 'market_request': ('Market request', '市场委托', '市場の依頼'),
    G + 'days_left': ('%s days left', '剩余 %s 天', '残り %s 日'),
    G + 'withdraw': ('Withdraw', '撤回', '取り下げ'),
    G + 'deliver': ('Deliver', '交付', '納品'),
    G + 'post': ('Post a commission', '发布委托', '委託を出す'),
    G + 'pick_target': ('Item wanted', '所需物品', '欲しいアイテム'),
    G + 'reward.0': ('Coins', '货币', 'コイン'),
    G + 'reward.1': ('Item', '物品', 'アイテム'),
    G + 'reward.2': ('UU', 'UU', 'UU'),
    G + 'pick_reward': ('Reward item', '报酬物品', '報酬アイテム'),
    G + 'reward_count': ('Reward items', '报酬物品数', '報酬の個数'),
    G + 'reward_amount': ('Reward amount', '报酬金额', '報酬額'),
    G + 'days': ('Days (1-7)', '天数（1-7）', '日数（1-7）'),
    G + 'fee_coin': ('Fee: 2%% of the reward (destroyed)', '手续费：报酬的 2%%（销毁）', '手数料: 報酬の 2%%（消滅）'),
    G + 'fee_flat': ('Fee: 5 coins (destroyed)', '手续费：5 货币（销毁）', '手数料: 5 コイン（消滅）'),
    G + 'post_button': ('Post', '发布', '掲示'),
    G + 'inventory_pick': ('Click an item of your inventory to pick it', '点击背包中的物品进行选择', 'インベントリのアイテムをクリックして選択'),
    G + 'candidates': ('Toward a listing', '上市进度', '上場までの進み具合'),
    G + 'candidate': ('%1$s requesters · %2$s filled · %3$s deliverers', '%1$s 名委托人 · %2$s 次完成 · %3$s 名交付者',
                      '依頼者 %1$s 人 · 達成 %2$s 件 · 納品者 %3$s 人'),
    G + 'futures': ('Futures (cash-settled)', '期货（现金结算）', '先物（差金決済）'),
    G + 'futures_pick': ('Select a market commodity on the Market tab to trade its futures.', '请在“行情”页选择一种市场货品后交易其期货。',
                         '先物を取引するには「マーケット」タブで市場品目を選んでください。'),
    G + 'contract_days': ('%s days', '%s 天', '%s 日'),
    G + 'long': ('Long %s', '做多 %s', '買い %s'),
    G + 'short': ('Short %s', '做空 %s', '売り %s'),
    G + 'collateral': ('Collateral: long %1$s / short %2$s', '保证金：做多 %1$s / 做空 %2$s', '証拠金: 買い %1$s / 売り %2$s'),
    G + 'futures_help': ('A contract pays at expiry by the price change, at most twice its collateral and at least nothing. '
                         'Its price already includes the expected drift back toward the base price and half the spread, '
                         'so futures only pay off when you foresee more than the market does.',
                         '合约到期时按价格变动结算，最多返还保证金的两倍，最少为零。合约价格已包含价格回归基准价的预期和一半价差，'
                         '因此只有当你比市场看得更准时，期货才会盈利。',
                         '契約は満期に価格の変化で決済され、受け取りは証拠金の 2 倍が上限、0 が下限です。契約価格には基準価格への'
                         '回帰の見込みとスプレッドの半分がすでに含まれるため、市場より先を読めたときだけ利益になります。'),
    G + 'positions': ('Open positions', '持仓', '建玉'),
    G + 'side_long': ('Long', '多', '買い'),
    G + 'side_short': ('Short', '空', '売り'),
    G + 'position_value': ('Worth %1$s now · %2$s days left', '当前价值 %1$s · 剩余 %2$s 天', '現在価値 %1$s · 残り %2$s 日'),
    G + 'my_orders': ('My orders', '我的挂单', '自分の注文'),
    G + 'cancel': ('Cancel', '撤单', '取消'),
    G + 'mailbox': ('Mailbox', '邮箱', '郵便箱'),
    G + 'mail_coins': ('Coins: %s', '货币：%s', 'コイン: %s'),
    G + 'collect': ('Collect', '领取', '受け取る'),
    G + 'uu_account': ('UU account: %s mB', 'UU 账户：%s mB', 'UU 口座: %s mB'),
    G + 'uu_deposit': ('Deposit UU', '存入 UU', 'UU を預ける'),
    G + 'uu_withdraw': ('Withdraw UU', '取出 UU', 'UU を引き出す'),
    G + 'ledger': ('Coins created and destroyed (14 days)', '货币的产生与销毁（14 天）', 'コインの発行と消滅（14 日）'),
    G + 'ledger_day': ('Day %1$s: created %2$s, destroyed %3$s', '第 %1$s 天：产生 %2$s，销毁 %3$s', '%1$s 日目: 発行 %2$s、消滅 %3$s'),
    G + 'ledger_total': ('Created %1$s · destroyed %2$s · net %3$s', '产生 %1$s · 销毁 %2$s · 净额 %3$s', '発行 %1$s · 消滅 %2$s · 差引 %3$s'),
    G + 'ledger_help': ('Left bar: created (market buys, rewards, futures wins); right bar: destroyed (market sales, fees)',
                        '左柱：产生（市场收购、报酬、期货盈利）；右柱：销毁（市场出售、手续费）',
                        '左の棒: 発行（市場の買い取り・報酬・先物の利益）、右の棒: 消滅（市場の販売・手数料）'),
    G + 'stock': ('Treasury stock', '国库库存', '国庫の在庫'),
    G + 'listings': ('Player listings: %s', '玩家上市品：%s', 'プレイヤー上場品: %s'),

    # /mio_icif market audit
    A + 'header': ('Exchange, day %1$s: %2$s player listings, %3$s resting orders, %4$s commissions, %5$s open futures (collateral %6$s coins)',
                   '交易所，第 %1$s 天：玩家上市品 %2$s 个，挂单 %3$s 个，委托 %4$s 个，未平仓期货 %5$s 份（保证金 %6$s 货币）',
                   '取引所 %1$s 日目: プレイヤー上場品 %2$s、指値注文 %3$s、委託 %4$s、未決済の先物 %5$s（証拠金 %6$s コイン）'),
    A + 'ledger': ('Last 7 days: %1$s coins created, %2$s destroyed, net %3$s', '最近 7 天：产生 %1$s 货币，销毁 %2$s，净额 %3$s',
                   '直近 7 日: 発行 %1$s コイン、消滅 %2$s、差引 %3$s'),
    A + 'no_pairs': ('No trades between players in the last 7 days.', '最近 7 天没有玩家之间的交易。', '直近 7 日にプレイヤー間の取引はありません。'),
    A + 'pairs': ('Largest trading pairs between players, last 7 days (%s items traded between players in total):',
                  '最近 7 天交易量最大的玩家组合（玩家之间共成交 %s 个）：', '直近 7 日の取引量が多いプレイヤーの組（プレイヤー間の合計 %s 個）:'),
    A + 'pair': (' %1$s / %2$s: %3$s items in %4$s trades, value %5$s, %6$s of the volume', ' %1$s / %2$s：%4$s 笔成交 %3$s 个，价值 %5$s，占成交量 %6$s',
                 ' %1$s / %2$s: %4$s 回で %3$s 個、金額 %5$s、出来高の %6$s'),

    # config
    CFG + 'FutureMarket.exchangeFee': ('Fee on Trades Between Players', '玩家间交易手续费', 'プレイヤー間取引の手数料'),
    CFG + 'exchangeFee': ('Fee on Trades Between Players', '玩家间交易手续费', 'プレイヤー間取引の手数料'),
    CFG + 'FutureMarket.importPerDay': ('Daily Import per Commodity', '每种货品每日进口量', '品目ごとの 1 日の輸入量'),
    CFG + 'importPerDay': ('Daily Import per Commodity', '每种货品每日进口量', '品目ごとの 1 日の輸入量'),
    CFG + 'FutureMarket.importPremium': ('Price Premium of Imports', '进口品溢价', '輸入品の割増'),
    CFG + 'importPremium': ('Price Premium of Imports', '进口品溢价', '輸入品の割増'),
    CFG + 'FutureMarket.listingRequesters': ('Requesters Needed for a Listing', '上市所需委托人数', '上場に必要な依頼者数'),
    CFG + 'listingRequesters': ('Requesters Needed for a Listing', '上市所需委托人数', '上場に必要な依頼者数'),
    CFG + 'FutureMarket.listingFulfilled': ('Delivered Commissions Needed for a Listing', '上市所需完成委托数', '上場に必要な達成委託数'),
    CFG + 'listingFulfilled': ('Delivered Commissions Needed for a Listing', '上市所需完成委托数', '上場に必要な達成委託数'),
    CFG + 'FutureMarket.listingMinPlayMinutes': ('Play Time Before Commissions Count (minutes)', '委托计入需求所需游戏时间（分钟）',
                                                 '委託が需要に数えられるまでのプレイ時間（分）'),
    CFG + 'listingMinPlayMinutes': ('Play Time Before Commissions Count (minutes)', '委托计入需求所需游戏时间（分钟）',
                                    '委託が需要に数えられるまでのプレイ時間（分）'),
    CFG + 'FutureMarket.uuDepth': ('Craft Depth Traded in UU-Matter', '以 UU 物质交易的合成深度', 'UU 物質建てになるクラフト深度'),
    CFG + 'uuDepth': ('Craft Depth Traded in UU-Matter', '以 UU 物质交易的合成深度', 'UU 物質建てになるクラフト深度'),
    CFG + 'FutureMarket.systemCommissionsPerDay': ('Market Commissions per Day', '市场每日委托数', '市場の 1 日の委託数'),
    CFG + 'systemCommissionsPerDay': ('Market Commissions per Day', '市场每日委托数', '市場の 1 日の委託数'),
}


def main():
    col = {'en_us': 0, 'zh_cn': 1, 'ja_jp': 2}
    bad = 0
    for lang, i in col.items():
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        if CHECK:
            wrong = [k for k, t in KEYS.items() if data.get(k) != t[i]]
            bad += len(wrong)
            print(lang, 'missing or different:', len(wrong), wrong[:5])
            continue
        for key, t in KEYS.items():
            data[key] = t[i]
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(KEYS))
    if CHECK and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()

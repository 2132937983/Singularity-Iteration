#!/usr/bin/env python3
"""0.1.10.25 language values (en_us / zh_cn / ja_jp): the exchange X1 - the sell page of the futures panel (sell box,
quotes, daily auction), the sell curve lines of the market page, the industrial category, the new config entries, the
messages and the operator commands simulate / arbitrage / calibrate / ledger. Idempotent; keeps the line endings of each
file. With --check it only reports missing or different keys.
Usage: lang_1125.py <repo root> [--check]
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

G = 'gui.mio_icif.exchange.'
M = 'message.mio_icif.exchange.'
C = 'command.mio_icif.market_tools.'
F = 'mio_icif.configuration.'

KEYS = {
    G + 'tab.5': ('Sell', '出售', '売却'),
    G + 'pressure': ('Sell pressure', '卖压', '売り圧力'),
    G + 'sold_today': ('You sold today', '你今天已卖', '今日あなたが売った数'),
    G + 'buy_left': ('You can buy today', '你今天还可买', '今日あと買える数'),
    G + 'sell_curve_help': (
        'The market pays less for each item of this commodity you sold today and for each item everyone sold that it has '
        'not passed on yet (half of that leaves each day). Bid: the price of your next item. Any amount can be sold.',
        '你今天每卖出一个该商品，以及所有人卖出且市场尚未转出的每一个，都会让市场的买价降低（每天转出一半）。买价：你下一个的价格。数量不限。',
        'この品目を今日あなたが売った分と、全員が売って市場がまだ流していない分だけ、市場の買値は下がります（毎日半分が流れます）。'
        '買値：あなたの次の 1 個の値段。数量の上限はありません。'),
    G + 'box.title': ('Sell box', '出售箱', '売却ボックス'),
    G + 'box.fill': ('Fill', '放入', '入れる'),
    G + 'box.fill_help': (
        'Puts every stack of your inventory that the market buys into the box (not the hotbar)',
        '把物品栏中市场收购的所有物品放入箱中（快捷栏除外）',
        'インベントリのうち市場が買う物をすべてボックスに入れます（ホットバーは除く）'),
    G + 'box.return': ('Return', '取回', '戻す'),
    G + 'box.inventory': ('Inventory', '物品栏', 'インベントリ'),
    G + 'box.quote': ('Quote', '报价', '見積もり'),
    G + 'box.qty': ('Items', '数量', '数量'),
    G + 'box.now': ('Now', '立即', '今すぐ'),
    G + 'box.auction': ('Auction', '集中清算', '一括清算'),
    G + 'box.empty': (
        'Shift-click commodities from your inventory or press Fill. Only plain stacks of commodities you have unlocked fit; '
        'what stays in the box when you close the panel comes back.',
        '从物品栏 Shift 点击商品，或按“放入”。只能放入已解锁商品的普通物品；关闭界面时箱中剩余的物品会退回。',
        'インベントリから Shift クリックするか「入れる」を押してください。解禁済みの品目の、手を加えていない物だけ入ります。'
        '画面を閉じるとボックスの中身は戻ります。'),
    G + 'box.line_help': (
        '%1$s in the box. Now: %2$s sell for %3$s coins. Auction (if the day changed now): %4$s for %5$s coins.',
        '箱中 %1$s 个。立即：卖出 %2$s 个，得 %3$s 货币。集中清算（若现在换日）：%4$s 个，得 %5$s 货币。',
        'ボックスに %1$s 個。今すぐ：%2$s 個を %3$s コインで売却。一括清算（いま日が変わった場合）：%4$s 個で %5$s コイン。'),
    G + 'box.total': ('Total', '合计', '合計'),
    G + 'box.allowance': ('Coins the market can still pay you today', '市场今天还能付给你的货币', '市場が今日まだ払える硬貨'),
    G + 'box.unlimited': ('no limit', '无限制', '制限なし'),
    G + 'box.min': ('Auction minimum (% of the bid)', '清算最低价（买价的 %）', '一括清算の最低価格（買値の %）'),
    G + 'box.sell_now': ('Sell now', '立即出售', '今すぐ売る'),
    G + 'box.sell_now_help': (
        'Each commodity sells down its bid curve now; the coins go to your inventory. What the market does not take stays in the box.',
        '每种商品沿买价曲线立即卖出，货币放入物品栏。市场不收的部分留在箱中。',
        '品目ごとに買値の曲線に沿って今すぐ売り、硬貨はインベントリに入ります。市場が受け取らない分はボックスに残ります。'),
    G + 'box.sell_auction': ('Sell at the auction', '参加集中清算', '一括清算に出す'),
    G + 'box.sell_auction_help': (
        'The items are held and sold together with everyone\'s at the change of the market day, so the order of the sellers '
        'does not matter. Coins and unsold items go to your mailbox. Below the minimum nothing of the order is sold.',
        '物品由市场保管，在交易日更替时与所有人的一起出售，先后顺序无关。货币与未售出的物品进入邮箱。低于最低价时该订单不出售。',
        '物は預かられ、取引日の変わり目に全員分と一緒に売られるので、順番は関係ありません。硬貨と売れ残りはメールボックスへ。'
        '最低価格を下回ると、その注文は売られません。'),
    G + 'auction.title': ('My auction orders', '我的清算订单', '一括清算の注文'),
    G + 'auction.none': ('No orders', '无订单', '注文なし'),
    G + 'auction.cancel': ('Cancel', '取消', '取消'),
    G + 'auction.line': (
        '%1$s x%2$s (min %3$s%%): about %4$s for %5$s',
        '%1$s x%2$s（最低 %3$s%%）：约 %4$s 个，%5$s 货币',
        '%1$s x%2$s（最低 %3$s%%）：約 %4$s 個で %5$s コイン'),
    'gui.mio_icif.future.category.industrial': ('Industrial', '工业品', '工業品'),
    M + 'box_empty': ('§cThe sell box holds nothing the market buys.', '§c出售箱中没有市场收购的物品。', '§c売却ボックスに市場が買う物がありません。'),
    M + 'box_nothing_sold': (
        '§cThe market took nothing (its bid is below 1 coin, or it can pay you no more today).',
        '§c市场没有收购（买价低于 1 货币，或今天已不能再付给你）。',
        '§c市場は何も買いませんでした（買値が 1 コイン未満か、今日はもう払えません）。'),
    M + 'box_sold': ('§aSold %1$s items for %2$s coins.', '§a卖出 %1$s 个，得 %2$s 货币。', '§a%1$s 個を %2$s コインで売却しました。'),
    M + 'auction_submitted': (
        '§a%1$s items wait for the auction at the change of the market day.',
        '§a%1$s 个物品等待交易日更替时的集中清算。',
        '§a%1$s 個を取引日の変わり目の一括清算に出しました。'),
    M + 'auction_cancelled': ('§aAuction order cancelled: %1$s items back.', '§a已取消清算订单：退回 %1$s 个。', '§a一括清算の注文を取り消しました：%1$s 個を返却。'),
    M + 'auction_full': ('§cThe auction is full.', '§c集中清算已满。', '§c一括清算がいっぱいです。'),
    M + 'auction_cleared': (
        '§6[Exchange] Auction: %1$s items sold for %2$s coins, %3$s items back (mailbox).',
        '§6[交易所] 集中清算：卖出 %1$s 个，得 %2$s 货币，退回 %3$s 个（邮箱）。',
        '§6[取引所] 一括清算：%1$s 個を %2$s コインで売却、%3$s 個を返却（メールボックス）。'),
    C + 'not_commodity': ('%1$s is not a market commodity.', '%1$s 不是市场商品。', '%1$s は市場の品目ではありません。'),
    C + 'simulate': (
        '%1$s: price %2$s, bid without pressure %3$s, depth %4$s (player) / %5$s (server) items, sell pressure %6$s, sold today %7$s',
        '%1$s：价格 %2$s，无卖压买价 %3$s，深度 %4$s（个人）/ %5$s（服务器）个，卖压 %6$s，今日已卖 %7$s',
        '%1$s：価格 %2$s、売り圧力なしの買値 %3$s、深さ %4$s（個人）/ %5$s（サーバー）個、売り圧力 %6$s、今日売った数 %7$s'),
    C + 'simulate_sale': (
        'Selling %1$s now pays %2$s coins (average %3$s, last bid %4$s); alone at the auction %5$s; one sale pays at most %6$s',
        '立即卖出 %1$s 个得 %2$s 货币（平均 %3$s，最后买价 %4$s）；单独参加集中清算 %5$s；单次最多 %6$s',
        '%1$s 個を今すぐ売ると %2$s コイン（平均 %3$s、最後の買値 %4$s）。単独で一括清算なら %5$s。1 回の売却の上限 %6$s'),
    C + 'simulate_allowance': ('%1$s may still receive %2$s coins from the market today', '%1$s 今天还能从市场获得 %2$s 货币', '%1$s が今日市場から受け取れる残り：%2$s コイン'),
    C + 'arbitrage': (
        '%1$s recipes, %2$s commodities buyable now: %3$s loops, %4$s items on recipe cycles that multiply them',
        '%1$s 个配方，%2$s 个当前可买的商品：%3$s 个套利循环，%4$s 个物品处于增殖配方循环中',
        'レシピ %1$s、今買える品目 %2$s：裁定ループ %3$s、増殖するレシピ循環上のアイテム %4$s'),
    C + 'loop': (
        '%1$s: bid %2$s, make %3$s, profit %4$s, EU per item %5$s (EU per coin %6$s), via %7$s from %8$s',
        '%1$s：买价 %2$s，制作 %3$s，利润 %4$s，每个 %5$s EU（每货币 %6$s EU），经 %7$s，原料 %8$s',
        '%1$s：買値 %2$s、作る費用 %3$s、利益 %4$s、1 個あたり %5$s EU（1 コインあたり %6$s EU）、%7$s 経由、材料 %8$s'),
    C + 'cycles': ('Recipe cycles that multiply items: %1$s', '增殖物品的配方循环：%1$s', 'アイテムが増えるレシピ循環：%1$s'),
    C + 'calibrate': (
        'Coins per bucket of UU-matter: %1$s (median of %2$s commodities; %3$s without a UU value)',
        '每桶 UU 物质的货币价值：%1$s（%2$s 个商品的中位数；%3$s 个没有 UU 值）',
        'UU 物質 1 バケツあたりのコイン：%1$s（%2$s 品目の中央値、UU 値なし %3$s）'),
    C + 'calibrate_row': (
        '%1$s: base %2$s, UU %3$s buckets = %4$s coins, ratio %5$s',
        '%1$s：基础价格 %2$s，UU %3$s 桶 = %4$s 货币，比率 %5$s',
        '%1$s：基準価格 %2$s、UU %3$s バケツ = %4$s コイン、比率 %5$s'),
    C + 'ledger_day': ('Day %1$s: created %2$s, destroyed %3$s, net %4$s', '第 %1$s 天：发行 %2$s，销毁 %3$s，净额 %4$s', '%1$s 日目：発行 %2$s、消滅 %3$s、差引 %4$s'),
    C + 'ledger': (
        '%1$s days: created %2$s, destroyed %3$s, net %4$s; sell pressure %5$s items; auction orders %6$s',
        '%1$s 天：发行 %2$s，销毁 %3$s，净额 %4$s；卖压 %5$s 个；集中清算订单 %6$s',
        '%1$s 日間：発行 %2$s、消滅 %3$s、差引 %4$s。売り圧力 %5$s 個、一括清算の注文 %6$s'),
}

CONFIG = {
    'buyItemsPerDay': ('Daily Buy Limit (items, 0 = none)', '每日购买上限（个，0 = 不限）', '1 日の購入上限（個、0 = なし）'),
    'sellDepthPlayer': ('Personal Sell Depth (items)', '个人卖出深度（个）', '個人の売却の深さ（個）'),
    'sellDepthServer': ('Server Sell Depth (items)', '服务器卖出深度（个）', 'サーバーの売却の深さ（個）'),
    'sellExportPerDay': ('Sell Pressure Exported per Day', '每日转出的卖压比例', '1 日に流す売り圧力の割合'),
    'sellAllowancePerDay': ('Daily Coins from Market Sales per Player (0 = none)', '每名玩家每日卖给市场的货币上限（0 = 不限）',
                            '1 人 1 日に市場への売却で受け取れる硬貨（0 = なし）'),
    'sellAllowanceDays': ('Days of Allowance Kept', '额度最多保留天数', '繰り越せる日数'),
    'sellAllowanceInSingleplayer': ('Allowance in Single Player', '单人游戏也使用额度', 'シングルプレイでも上限を使う'),
}
for key, values in CONFIG.items():
    KEYS[F + 'FutureMarket.' + key] = values
    KEYS[F + key] = values


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

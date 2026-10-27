#!/usr/bin/env python3
"""0.1.10.29 language values (en_us / zh_cn / ja_jp): the coin account (orders page, header, settlement coins, messages), the
minimum-price sale (sell / settlement pages, ticket, terminal orders and settlement), the rate limit message. Idempotent; keeps the
line endings of each file. With --check it only reports missing or different keys.
Usage: lang_1129.py <repo root> [--check]
"""
import json
import os
import re
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

G = 'gui.mio_icif.exchange.'
M = 'message.mio_icif.exchange.'
T = 'gui.mio_icif.terminal.'
C = 'gui.mio_icif.card.'

KEYS = {
    # ---------------------------------------------------------------- the coin account
    G + 'header_account': ('Day %1$s · %2$s coins · account %3$s · %4$s mB UU', '第 %1$s 天 · %2$s 货币 · 账户 %3$s · %4$s mB UU',
                           '%1$s 日目 · 硬貨 %2$s · 口座 %3$s · UU %4$s mB'),
    G + 'account_coins': ('Coin account: %s', '货币账户：%s', '硬貨口座：%s'),
    G + 'account_help': (
        'Your coins at the exchange. Sales that do not fit the inventory land here; purchases, buy orders, commission rewards and fees '
        'and futures collateral take from the inventory and this account together (in bank mode from the account first).',
        '你在交易所的货币。物品栏放不下的销售所得存入这里；购买、买单、委托报酬与手续费、期货保证金从物品栏和本账户合计支付（银行模式下优先从账户扣除）。',
        '取引所にあるあなたの硬貨です。インベントリに入りきらない売上はここに入ります。購入・買い注文・依頼の報酬と手数料・先物の証拠金は、インベントリと'
        'この口座の合計から支払います（バンクモードでは口座が先）。'),
    G + 'coin_amount_help': ('Coins to deposit or withdraw (0 = all)', '要存入或取出的货币（0 = 全部）', '入金・出金する硬貨（0 = 全部）'),
    G + 'coin_in': ('Deposit', '存入', '入金'),
    G + 'coin_out': ('Withdraw', '取出', '出金'),
    G + 'bank_on': ('Bank mode: on', '银行模式：开', 'バンクモード：オン'),
    G + 'bank_off': ('Bank mode: off', '银行模式：关', 'バンクモード：オフ'),
    G + 'bank_help': (
        'Bank mode: the coins of sales and payouts go to the coin account instead of the inventory, payments take from the account '
        'first, and Collect leaves the coins there (Withdraw takes them). Off: the inventory first, the account for the rest.',
        '银行模式：销售与支付所得的货币进入货币账户而不是物品栏，付款优先从账户扣除，“领取”时货币留在账户（用“取出”取出）。关闭时：物品栏优先，其余用账户。',
        'バンクモード：売上や支払いの硬貨はインベントリではなく硬貨口座へ入り、支払いは口座から先に引かれ、「受け取る」でも硬貨は口座に残ります（「出金」で'
        '引き出し）。オフ：インベントリが先で、足りない分を口座から。'),
    G + 'pay_from': ('Pays from: %s coins carried + %s on the account', '支付来源：携带 %s 货币 + 账户 %s', '支払い元：手持ち %s 硬貨 + 口座 %s'),
    G + 'settle.coins': ('Coins in the container: %s', '容器中的货币：%s', 'コンテナ内の硬貨：%s'),
    G + 'settle.coins_in': ('Coins to account', '货币存入账户', '硬貨を口座へ'),
    C + 'mail': ('Account: %s coins, %s mB UU-matter, mailbox %s items', '账户：%s 货币，%s mB UU 物质，邮箱 %s 件物品',
                 '口座：硬貨 %s、UU 物質 %s mB、メールボックスの品物 %s 個'),
    T + 'owner_coins': ("Owner's coin account: %s coins", '所有者货币账户：%s 货币', '所有者の硬貨口座：%s 硬貨'),
    T + 'settle_uu_coins': ('UU-matter: %s mB · coins: %s', 'UU 物质：%s mB · 货币：%s', 'UU 物質：%s mB · 硬貨：%s'),
    T + 'uu_in_short': ('UU to account', 'UU 存入账户', 'UU を口座へ'),
    T + 'coins_in_short': ('Coins to account', '货币存入账户', '硬貨を口座へ'),
    T + 'coins_in_help': ('The coins of the container onto your coin account.', '把容器中的货币存入你的货币账户。', 'コンテナの硬貨をあなたの硬貨口座へ入れます。'),
    M + 'coins_deposited': ('%s coins onto the coin account', '%s 货币已存入货币账户', '%s 硬貨を硬貨口座へ入金しました'),
    M + 'coins_withdrawn': ('%s coins taken from the coin account', '已从货币账户取出 %s 货币', '硬貨口座から %s 硬貨を出金しました'),
    M + 'no_coins': ('No coins to deposit', '没有可存入的货币', '入金できる硬貨がありません'),
    M + 'no_account_coins': ('The coin account is empty', '货币账户为空', '硬貨口座は空です'),
    M + 'inventory_full': ('No room in the inventory', '物品栏没有空间', 'インベントリに空きがありません'),
    M + 'bank_mode_on': ('Bank mode on: sales go to the coin account, payments take from it first',
                         '银行模式已开启：销售所得进入货币账户，付款优先从账户扣除', 'バンクモード オン：売上は硬貨口座へ、支払いは口座から先に'),
    M + 'bank_mode_off': ('Bank mode off: sales go to the inventory, payments take from it first',
                          '银行模式已关闭：销售所得进入物品栏，付款优先从物品栏扣除', 'バンクモード オフ：売上はインベントリへ、支払いはインベントリから先に'),
    M + 'too_fast': ('Too many actions at once - wait a moment and try again', '操作过于频繁，请稍候再试', '操作が速すぎます。少し待ってからもう一度どうぞ'),
    # ---------------------------------------------------------------- the minimum-price sale
    G + 'box.min': ('Minimum (%% of the bid)', '最低价（买价的 %%）', '最低価格（買値の %%）'),
    G + 'box.min_coins': ('Minimum (coins per item)', '最低价（每件货币）', '最低単価（1 個あたり硬貨）'),
    G + 'box.min_toggle_help': (
        'Switch the minimum between percent of the bid without pressure and coins per item.',
        '在“无抛压买价的百分比”和“每件货币”之间切换最低价。',
        '最低価格を「売り圧のない買値に対するパーセント」と「1 個あたりの硬貨」で切り替えます。'),
    G + 'box.min_help': (
        'Sell now: of each commodity the most items whose own price is still at least the minimum, in one batch at the summed price '
        '(one payout). Auction: the lowest average. 0 = no minimum.',
        '立即出售：每种商品卖出自身价格仍不低于最低价的最多件数，一次成交并按价格总和一次支付。集中清算：最低平均价。0 = 不设最低价。',
        '今すぐ売る：品目ごとに、1 個ずつの価格が最低価格以上である最大の個数を一括で売り、合計額を一度に支払います。一括清算：最低の平均価格。0 = 最低価格なし。'),
    G + 'box.item_prices': ('last item %s, next %s', '最后一件 %s，下一件 %s', '最後の 1 個 %s、次の 1 個 %s'),
    G + 'box.sell_now_help': (
        'Sells each commodity now in one batch: every item paid at least the minimum, within your daily allowance. The coins go to the '
        'inventory (in bank mode to the coin account). What the market does not take at the minimum stays in the box.',
        '每种商品一次性立即卖出：每件不低于最低价，并在你的每日额度之内。货币放入物品栏（银行模式下存入货币账户）。市场在最低价下不收的部分留在箱中。',
        '品目ごとに今すぐ一括で売ります。どの 1 個も最低価格以上で、1 日の上限の範囲内です。硬貨はインベントリへ（バンクモードでは硬貨口座へ）。'
        '最低価格で市場が引き取らない分はボックスに残ります。'),
    G + 'settle.sell_now_help': (
        'Sells the commodities of the container now, each in one batch: every item paid at least the minimum, within your daily '
        'allowance. What the market does not take at the minimum stays in the container.',
        '立即出售容器中的商品，每种一次成交：每件不低于最低价，并在你的每日额度之内。市场在最低价下不收的部分留在容器中。',
        'コンテナの商品をいますぐ品目ごとに一括で売ります。どの 1 個も最低価格以上で、1 日の上限の範囲内です。最低価格で市場が引き取らない分はコンテナに残ります。'),
    G + 'price_percent': ('Min. price (%% of bid)', '最低价（买价 %%）', '最低価格（買値 %%）'),
    G + 'percent_toggle_help': (
        'Name the limit of a sale in coins (c) or in percent of the bid without pressure (%%). A percent becomes coins with the price '
        'at the time of the sale.',
        '用货币（c）或无抛压买价的百分比（%%）设定出售的限价。百分比按出售时的价格换算为货币。',
        '売りの指値を硬貨（c）または売り圧のない買値に対するパーセント（%%）で指定します。パーセントは売る時点の価格で硬貨に換算されます。'),
    G + 'ticket_estimate': ('Market now: %1$s of %2$s for %3$s coins (avg. %4$s)', '市场即时：%2$s 中 %1$s 件，%3$s 货币（均价 %4$s）',
                            '市場に即時：%2$s 個中 %1$s 個、%3$s 硬貨（平均 %4$s）'),
    G + 'ticket_estimate_help': (
        'Every item the market takes is paid at least %1$s coins (last item %2$s, next %3$s), one payout for the batch. Resting bids of '
        'players are not counted.',
        '市场收购的每件价格不低于 %1$s 货币（最后一件 %2$s，下一件 %3$s），整批一次支付。不含玩家的挂单买价。',
        '市場が引き取る 1 個ごとの価格は %1$s 硬貨以上（最後の 1 個 %2$s、次の 1 個 %3$s）で、まとめて一度に支払われます。プレイヤーの買い注文は含みません。'),
    T + 'min_price_percent': ('Min. %%', '最低 %%', '最低 %%'),
    T + 'order_estimate': ('now: limit %s, sells %s for %s coins', '现在：限价 %s，可卖 %s 件，得 %s 货币', '現在：指値 %s、%s 個を %s 硬貨で売却'),
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
            stray = [k for k, t in KEYS.items() if '%' in re.sub(r'%(?:\d+\$)?([A-Za-z%]|$)', '', t[i])]
            bad += len(stray)
            if stray:
                print(lang, 'stray %:', stray)
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

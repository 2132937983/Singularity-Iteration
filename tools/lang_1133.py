#!/usr/bin/env python3
"""0.1.10.33 language values (en_us / zh_cn / ja_jp): the exchange panel's new texts (short tabs, the status bar's day, the
bounty floor, the band note of a sale, "+N more", the futures limits, commission lots), the bounty and commission messages and
the /mio_icif market day command. Idempotent; keeps the line endings of each file. With --check it only reports missing or
different keys. Usage: lang_1133.py <repo root> [--check]
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
C = 'command.mio_icif.market_tools.'

SHORT = (
    ('Market', '行情', '市場'),
    ('Comms', '委托', '委託'),
    ('Futures', '期货', '先物'),
    ('Orders', '挂单', '注文'),
    ('Treasury', '国库', '国庫'),
    ('Sell', '出售', '売却'),
    ('Bounties', '悬赏', '懸賞'),
    ('Settle', '结算', '決済'),
)

KEYS = {G + 'tab_short.%d' % i: t for i, t in enumerate(SHORT)}
KEYS.update({
    G + 'day_help': (
        'The market\'s day. It goes on with the days that pass in the world; when the world\'s time is set back it still goes '
        'on by one day (records, bounties and orders keep their dates).',
        '市场日。随世界中经过的天数前进；世界时间被调回时仍只前进一天（记录、悬赏和挂单保持各自的日期）。',
        '市場の日付。ワールドで経過した日数だけ進みます。ワールドの時刻が巻き戻されても 1 日進むだけです（記録・懸賞・注文の日付は'
        'そのまま保たれます）。'),
    G + 'bounty_floor_help': (
        'A bounty needs this item: the market sells it at %1$s or more (the bounty\'s reward per item), so buying here to deliver '
        'does not pay.',
        '有悬赏需要此物品：市场以不低于 %1$s（该悬赏每个物品的报酬）的价格出售，在此买入再交付不会获利。',
        '懸賞がこのアイテムを求めています：市場は %1$s 以上（懸賞の 1 個あたりの報酬）で売るため、ここで買って納品しても得には'
        'なりません。'),
    G + 'below_band': ('Under the band (%1$s): sold now to the market only', '低于价格区间（%1$s）：只能立即卖给市场',
                       '値幅（%1$s）未満：市場への即時売却のみ'),
    G + 'below_band_rest': ('Under the band (%1$s): an order cannot rest here', '低于价格区间（%1$s）：不能挂单',
                            '値幅（%1$s）未満：板には出せません'),
    G + 'below_band_help': (
        'Orders in the book stay within %1$s - %2$s. An instant sale to the market may name a lower limit; it is paid what the '
        'market\'s bid pays.',
        '挂单价格须在 %1$s - %2$s 之间。立即卖给市场时限价可以更低，按市场买价成交。',
        '板の注文は %1$s - %2$s の範囲です。市場への即時売却はこれより低い指値でもよく、市場の買値で約定します。'),
    G + 'more': ('+%1$s more', '还有 %1$s 项', '他 %1$s 件'),
    G + 'more_help': ('Scroll the list with the mouse wheel', '用鼠标滚轮滚动列表', 'マウスホイールでリストをスクロールできます'),
    G + 'positions_count': ('Positions %1$s/%2$s · collateral %3$s', '持仓 %1$s/%2$s · 保证金 %3$s', '建玉 %1$s/%2$s · 証拠金 %3$s'),
    G + 'positions_help': (
        'At most %1$s open futures of up to %2$s items each; a position settles on its last day at the market price.',
        '最多 %1$s 份未平仓期货，每份最多 %2$s 个；持仓在最后一天按市场价结算。',
        '未決済の先物は最大 %1$s 件、1 件 %2$s 個まで。建玉は最終日に市場価格で決済されます。'),
    G + 'future_full': ('You already have %1$s open futures', '你已有 %1$s 份未平仓期货', '未決済の先物がすでに %1$s 件あります'),
    G + 'future_no_coins': ('Collateral %1$s coins; you have %2$s (inventory + account)', '需要保证金 %1$s 货币；你有 %2$s（背包 + 账户）',
                            '証拠金 %1$s コインが必要です。所持 %2$s（インベントリ + 口座）'),
    G + 'future_no_quantity': ('Enter a quantity', '请输入数量', '数量を入力してください'),
    G + 'commission_lot_help': ('Delivered in lots of %1$s items; each lot pays %2$s', '按每批 %1$s 个交付；每批报酬 %2$s',
                                '%1$s 個単位で納品；1 単位の報酬 %2$s'),
    M + 'bounty_missing': ('§cNothing to deliver. Still needed: %1$s', '§c没有可交付的物品。仍需：%1$s', '§c納品できるアイテムがありません。残り：%1$s'),
    M + 'bounty_delivered_left': ('§aDelivered %1$s items; %2$s coins paid now. Still needed: %3$s',
                                  '§a已交付 %1$s 个；现在支付 %2$s 货币。仍需：%3$s',
                                  '§a%1$s 個を納品しました。今 %2$s コインを払いました。残り：%3$s'),
    M + 'commission_lot': ('§cThis commission takes whole lots of %1$s items (each pays %2$s), or all that is left.',
                           '§c此委托须按每批 %1$s 个整批交付（每批报酬 %2$s），或交付全部剩余。',
                           '§cこの委託は %1$s 個単位（1 単位の報酬 %2$s）か、残り全部で納品してください。'),
    C + 'day': ('Market day %1$s; world day %2$s (last seen %3$s); latest record day %4$s',
                '市场日 %1$s；世界日 %2$s（上次记录 %3$s）；记录中最晚的日期 %4$s',
                '市場日 %1$s、ワールド日 %2$s（前回確認 %3$s）、記録の最新日 %4$s'),
    C + 'day_repaired': ('Market day repaired: %1$s -> %2$s; %3$s records moved with it.',
                         '市场日已修复：%1$s -> %2$s；%3$s 条记录随之移动。',
                         '市場日を修復しました：%1$s → %2$s。%3$s 件の記録を移動しました。'),
    C + 'day_ok': ('Market day %1$s is not behind any record; nothing to repair.', '市场日 %1$s 不早于任何记录，无需修复。',
                   '市場日 %1$s はどの記録より前ではありません。修復は不要です。'),
})


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

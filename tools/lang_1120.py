#!/usr/bin/env python3
"""0.1.10.20 language keys (en_us / zh_cn / ja_jp): the futures market quotes, daily buy budget and refusal
messages (M1), its new config entries, and the grid zone line of /mio_icif perf (G1 / G2).
Idempotent; keeps the line endings of each file. With --check it only reports missing or different keys.
Usage: lang_1120.py <repo root> [--check]
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

F = 'gui.mio_icif.future.quote.'
M = 'message.mio_icif.future.'
CFG = 'mio_icif.configuration.'

KEYS = {
    F + 'buy': ('Buy: %s coins', '买入：%s 货币', '購入: %s コイン'),
    F + 'sell': ('Sell: %s coins', '卖出：%s 货币', '売却: %s コイン'),
    F + 'sell_none': ('Sell: market full today', '卖出：今日已停止收购', '売却: 本日の買い取り終了'),
    F + 'room': ('Market buys %s more today', '今日还收购 %s 个', '本日の残り買い取り: %s 個'),
    M + 'market_full': ('§cThe market buys only %s more %s today!', '§c今日市场只再收购 %s 个%s！', '§c本日、市場が買い取る%2$sは残り %1$s 個です！'),
    M + 'inventory_full': ('§cNot enough inventory space!', '§c背包空间不足！', '§cインベントリに空きがありません！'),
    CFG + 'FutureMarket.spread': ('Bid-Ask Spread', '买卖价差', '売買スプレッド'),
    CFG + 'spread': ('Bid-Ask Spread', '买卖价差', '売買スプレッド'),
    CFG + 'FutureMarket.systemBuyPerDay': ('Daily Market Buy Budget (items per commodity)', '市场每日收购上限（每种货品的数量）',
                                          '市場の 1 日の買い取り上限（品目ごとの個数）'),
    CFG + 'systemBuyPerDay': ('Daily Market Buy Budget (items per commodity)', '市场每日收购上限（每种货品的数量）',
                              '市場の 1 日の買い取り上限（品目ごとの個数）'),
    CFG + 'FutureMarket.budgetPriceDrop': ('Sale Price Drop Over the Budget', '收购额度用尽时的卖价降幅', '買い取り上限に近づくときの売値の下落'),
    CFG + 'budgetPriceDrop': ('Sale Price Drop Over the Budget', '收购额度用尽时的卖价降幅', '買い取り上限に近づくときの売値の下落'),
    'command.mio_icif.perf.zones': (
        'Grid zones: conductor components rebuilt after edits %1$s, kept by the last rebuilds %2$s; steady windows replayed %3$s',
        '电网分区：编辑后重建的导线连通块 %1$s 个，最近重建时保留 %2$s 个；稳态窗口重放 %3$s 次',
        '電力網ゾーン: 編集後に作り直した導線の連結成分 %1$s 個、直近の再構築で残した成分 %2$s 個、定常ウィンドウの再利用 %3$s 回'),
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
    if bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
